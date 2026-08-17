package com.payshield.service;

import com.payshield.dto.payment.CreatePaymentRequest;
import com.payshield.dto.payment.PaymentResponse;
import com.payshield.entity.*;
import com.payshield.entity.enums.PaymentStatus;
import com.payshield.entity.enums.PaymentType;
import com.payshield.entity.enums.TransactionStatus;
import com.payshield.entity.enums.TransactionType;
import com.payshield.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import java.time.Instant;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private  final FraudAssessmentService fraudAssessmentService;
    public PaymentService(
            PaymentRepository paymentRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            UserRepository userRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository, WalletService walletService, FraudAssessmentService fraudAssessmentService
    ) {

        this.paymentRepository = paymentRepository;
        this.idempotencyKeyRepository =
                idempotencyKeyRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
        this.fraudAssessmentService = fraudAssessmentService;
    }

    @Transactional
    public PaymentResponse createPayment(
            String email,
            String idempotencyKey,
            CreatePaymentRequest request
    ) {

        // =====================================================
        // 0. VALIDATE REQUEST
        // =====================================================

        if (idempotencyKey == null ||
                idempotencyKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Idempotency-Key header is required"
            );
        }

        if (request == null ||
                request.amount() == null ||
                request.amount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero"
            );
        }


        // =====================================================
        // 1. FIND USER
        // =====================================================

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "User not found"
                        )
                );


        // =====================================================
        // 2. IDEMPOTENCY CHECK
        // =====================================================

        var existing =
                idempotencyKeyRepository
                        .findByUserIdAndKey(
                                user.getId(),
                                idempotencyKey
                        );

        if (existing.isPresent() &&
                existing.get().getPayment() != null) {

            return PaymentResponse.from(
                    existing.get().getPayment()
            );
        }


        // =====================================================
        // 3. GET WALLET & VALIDATE AVAILABLE BALANCE
        // =====================================================

        Wallet wallet = walletRepository
                .findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Wallet not found"
                        )
                );

        BigDecimal heldInReview = paymentRepository.sumPendingReviewAmountByUserId(user.getId());
        if (heldInReview == null) {
            heldInReview = BigDecimal.ZERO;
        }

        BigDecimal availableBalance = wallet.getBalance().subtract(heldInReview);

        if (request.amount().compareTo(availableBalance) > 0) {
            if (wallet.getBalance().compareTo(request.amount()) >= 0) {
                throw new IllegalStateException(
                        String.format(
                                "Insufficient available balance. Total wallet balance is ₹%.2f, but ₹%.2f is currently held in review for pending payments. Available to spend: ₹%.2f",
                                wallet.getBalance(), heldInReview, availableBalance.max(BigDecimal.ZERO)
                        )
                );
            } else {
                throw new IllegalStateException(
                        String.format(
                                "Insufficient wallet balance. Total balance: ₹%.2f, Required: ₹%.2f",
                                wallet.getBalance(), request.amount()
                        )
                );
            }
        }


        // =====================================================
        // 4. NORMALIZE CURRENCY
        // =====================================================

        String currency =
                request.currency() == null ||
                        request.currency().isBlank()
                        ? "INR"
                        : request.currency()
                        .trim()
                        .toUpperCase();


        // =====================================================
        // 5. CREATE TRANSACTION
        // =====================================================

        Transaction transaction =
                Transaction.builder()
                        .transactionReference(
                                generateTransactionReference()
                        )
                        .user(user)
                        .transactionType(
                                TransactionType.PAYMENT
                        )
                        .amount(request.amount())
                        .currency(currency)
                        .sourceAccount(
                                "WALLET-" +
                                        wallet.getId()
                        )
                        .destinationAccount(
                                request.description() == null
                                        ? "PAYMENT-DESTINATION"
                                        : request.description().trim()
                        )
                        .transactionTime(
                                Instant.now()
                        )
                        .status(
                                TransactionStatus.PENDING
                        )
                        .build();

        transaction =
                transactionRepository.save(transaction);


        // =====================================================
        // 6. FRAUD ASSESSMENT
        // =====================================================

        FraudAssessmentService.FraudAssessmentResult
                assessment =
                fraudAssessmentService.assess(
                        transaction,
                        wallet.getBalance()
                );


        // =====================================================
        // 7. DETERMINE PAYMENT STATUS
        // =====================================================

        PaymentStatus paymentStatus =
                switch (assessment.decision()) {

                    case "BLOCK" ->
                            PaymentStatus.REJECTED;

                    case "REVIEW" ->
                            PaymentStatus.REVIEW;

                    case "ALLOW" ->
                            PaymentStatus.APPROVED;

                    default ->
                            throw new IllegalStateException(
                                    "Unknown fraud decision: "
                                            + assessment.decision()
                            );
                };


        // =====================================================
        // 8. CREATE PAYMENT
        // =====================================================

        Payment payment =
                Payment.builder()
                        .transactionId(
                                transaction
                                        .getTransactionReference()
                        )
                        .user(user)
                        .wallet(wallet)
                        .amount(request.amount())
                        .currency(currency)
                        .paymentType(
                                PaymentType.PAYMENT
                        )
                        .status(paymentStatus)
                        .idempotencyKey(idempotencyKey)
                        .description(
                                request.description() == null
                                        ? null
                                        : request.description().trim()
                        )
                        .build();

        payment =
                paymentRepository.save(payment);


        // =====================================================
        // 9. APPLY FRAUD DECISION
        // =====================================================

        switch (assessment.decision()) {

            case "BLOCK" -> {

                transaction.setStatus(
                        TransactionStatus.BLOCKED
                );

                transactionRepository.save(transaction);
            }

            case "REVIEW" -> {

                transaction.setStatus(
                        TransactionStatus.PENDING
                );

                transactionRepository.save(transaction);
            }

            case "ALLOW" -> {

                /*
                 * Payment has passed fraud assessment.
                 *
                 * Debit wallet only now.
                 *
                 * walletService.debit() also creates the
                 * WalletTransaction ledger entry and links
                 * it to this Payment.
                 */

                walletService.debit(
                        user.getId(),
                        request.amount(),
                        transaction.getTransactionReference(),
                        payment
                );

                /*
                 * Debit succeeded.
                 *
                 * Therefore the transaction can safely be
                 * marked as completed.
                 */

                transaction.setStatus(
                        TransactionStatus.COMPLETED
                );

                transactionRepository.save(transaction);
            }

            default -> throw new IllegalStateException(
                    "Unknown fraud decision: "
                            + assessment.decision()
            );
        }


        // =====================================================
        // 10. SAVE IDEMPOTENCY KEY
        // =====================================================

        IdempotencyKey key =
                IdempotencyKey.builder()
                        .key(idempotencyKey)
                        .user(user)
                        .payment(payment)
                        .build();

        idempotencyKeyRepository.save(key);


        // =====================================================
        // 11. RETURN PAYMENT
        // =====================================================

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(
            String email,
            UUID paymentId
    ) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "User not found"
                        )
                );

        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Payment not found"
                        )
                );

        if (!payment.getUser()
                .getId()
                .equals(user.getId())) {

            throw new IllegalStateException(
                    "Payment does not belong to user"
            );
        }

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPayments(
            String email
    ) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "User not found"
                        )
                );

        return paymentRepository
                .findByUserIdOrderByCreatedAtDesc(
                        user.getId()
                )
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }



    private String generateTransactionReference() {
        return "TXN-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 16)
                        .toUpperCase();
    }
}