package com.payshield.service;

import com.payshield.dto.transaction.CreateTransactionRequest;
import com.payshield.dto.transaction.TransactionResponse;
import com.payshield.dto.transaction.TransactionSummaryResponse;
import com.payshield.entity.Transaction;
import com.payshield.entity.User;
import com.payshield.entity.enums.TransactionStatus;
import com.payshield.entity.enums.TransactionType;
import com.payshield.entity.Payment;
import com.payshield.entity.Wallet;
import com.payshield.exception.ResourceNotFoundException;
import com.payshield.repository.PaymentRepository;
import com.payshield.repository.TransactionRepository;
import com.payshield.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final WalletService walletService;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            PaymentRepository paymentRepository,
            WalletService walletService
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.walletService = walletService;
    }

    @Transactional
    public TransactionResponse createTransaction(
            String email,
            CreateTransactionRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + email
                        )
                );

        Transaction transaction = Transaction.builder()
                .transactionReference(generateTransactionReference())
                .user(user)
                .transactionType(request.transactionType())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .sourceAccount(request.sourceAccount())
                .destinationAccount(request.destinationAccount())
                .transactionTime(request.transactionTime())
                .status(TransactionStatus.PENDING)
                .build();

        Transaction saved = transactionRepository.save(transaction);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID transactionId) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Transaction not found: " + transactionId
                        )
                );

        return mapToResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionSummaryResponse> getTransactions(
            TransactionStatus status,
            TransactionType type,
            Pageable pageable
    ) {

        Page<Transaction> transactions;

        if (status != null && type != null) {
            transactions = transactionRepository
                    .findByStatusAndTransactionType(
                            status,
                            type,
                            pageable
                    );
        } else if (status != null) {
            transactions = transactionRepository
                    .findByStatus(status, pageable);
        } else if (type != null) {
            transactions = transactionRepository
                    .findByTransactionType(type, pageable);
        } else {
            transactions = transactionRepository.findAll(pageable);
        }

        return transactions.map(this::mapToSummaryResponse);
    }

    @Transactional
    public TransactionResponse updateTransactionStatus(
            UUID transactionId,
            TransactionStatus status
    ) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Transaction not found: " + transactionId
                        )
                );

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                    "Transaction is already " + transaction.getStatus() + " and cannot be modified."
            );
        }

        // Sync with Payment entity if associated payment exists
        var paymentOpt = paymentRepository.findByTransactionId(transaction.getTransactionReference());
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            if (status == TransactionStatus.COMPLETED) {
                // Analyst approved — check user wallet balance before approving
                Wallet wallet = walletService.getUserWallet(payment.getUser().getId());
                if (wallet.getBalance().compareTo(payment.getAmount()) < 0) {
                    throw new IllegalStateException(
                            String.format(
                                    "Cannot approve payment: User wallet balance (₹%.2f) is insufficient for transaction amount (₹%.2f).",
                                    wallet.getBalance(), payment.getAmount()
                            )
                    );
                }

                if (payment.getStatus() == com.payshield.entity.enums.PaymentStatus.REVIEW) {
                    payment.setStatus(com.payshield.entity.enums.PaymentStatus.APPROVED);
                    paymentRepository.save(payment);
                    // Execute wallet debit upon analyst approval
                    walletService.debit(
                            payment.getUser().getId(),
                            payment.getAmount(),
                            transaction.getTransactionReference(),
                            payment
                    );
                }
            } else if (status == TransactionStatus.BLOCKED) {
                payment.setStatus(com.payshield.entity.enums.PaymentStatus.REJECTED);
                paymentRepository.save(payment);
            }
        } else {
            // Standalone transaction without Payment record
            if (status == TransactionStatus.COMPLETED && transaction.getUser() != null) {
                Wallet wallet = walletService.getUserWallet(transaction.getUser().getId());
                if (wallet.getBalance().compareTo(transaction.getAmount()) < 0) {
                    throw new IllegalStateException(
                            String.format(
                                    "Cannot approve transaction: User wallet balance (₹%.2f) is insufficient for transaction amount (₹%.2f).",
                                    wallet.getBalance(), transaction.getAmount()
                            )
                    );
                }
            }
        }

        transaction.setStatus(status);
        Transaction saved = transactionRepository.save(transaction);

        return mapToResponse(saved);
    }

    private String generateTransactionReference() {
        return "TXN-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 16)
                        .toUpperCase();
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        BigDecimal userWalletBalance = null;
        try {
            if (transaction.getUser() != null) {
                userWalletBalance = walletService.getUserWallet(transaction.getUser().getId()).getBalance();
            }
        } catch (Exception ignored) {
        }

        return new TransactionResponse(
                transaction.getId(),
                transaction.getTransactionReference(),
                transaction.getUser().getId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getSourceAccount(),
                transaction.getDestinationAccount(),
                transaction.getTransactionTime(),
                transaction.getStatus(),
                userWalletBalance,
                transaction.getCreatedAt()
        );
    }

    private TransactionSummaryResponse mapToSummaryResponse(
            Transaction transaction
    ) {

        return new TransactionSummaryResponse(
                transaction.getId(),
                transaction.getTransactionReference(),
                transaction.getUser() != null ? transaction.getUser().getId() : null,
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getTransactionTime()
        );
    }
}