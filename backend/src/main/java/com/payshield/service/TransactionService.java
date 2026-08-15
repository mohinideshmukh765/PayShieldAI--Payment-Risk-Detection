package com.payshield.service;

import com.payshield.dto.transaction.CreateTransactionRequest;
import com.payshield.dto.transaction.TransactionResponse;
import com.payshield.dto.transaction.TransactionSummaryResponse;
import com.payshield.entity.Transaction;
import com.payshield.entity.User;
import com.payshield.entity.enums.TransactionStatus;
import com.payshield.entity.enums.TransactionType;
import com.payshield.exception.ResourceNotFoundException;
import com.payshield.repository.TransactionRepository;
import com.payshield.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
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

    private String generateTransactionReference() {
        return "TXN-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 16)
                        .toUpperCase();
    }

    private TransactionResponse mapToResponse(Transaction transaction) {

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
                transaction.getCreatedAt()
        );
    }

    private TransactionSummaryResponse mapToSummaryResponse(
            Transaction transaction
    ) {

        return new TransactionSummaryResponse(
                transaction.getId(),
                transaction.getTransactionReference(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getTransactionTime()
        );
    }
}