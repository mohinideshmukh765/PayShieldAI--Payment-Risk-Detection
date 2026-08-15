package com.payshield.service;

import com.payshield.dto.payment.CreatePaymentRequest;
import com.payshield.dto.payment.PaymentResponse;
import com.payshield.entity.IdempotencyKey;
import com.payshield.entity.Payment;
import com.payshield.entity.User;
import com.payshield.entity.Wallet;
import com.payshield.entity.enums.PaymentStatus;
import com.payshield.entity.enums.PaymentType;
import com.payshield.repository.IdempotencyKeyRepository;
import com.payshield.repository.PaymentRepository;
import com.payshield.repository.UserRepository;
import com.payshield.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            UserRepository userRepository,
            WalletRepository walletRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.idempotencyKeyRepository =
                idempotencyKeyRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public PaymentResponse createPayment(
            String email,
            String idempotencyKey,
            CreatePaymentRequest request
    ) {

        if (idempotencyKey == null ||
                idempotencyKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Idempotency-Key header is required"
            );
        }

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "User not found"
                        )
                );

        /*
         * Idempotent retry:
         * return the original payment instead
         * of creating another one.
         */
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

        Wallet wallet = walletRepository
                .findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Wallet not found"
                        )
                );

        if (wallet.getBalance()
                .compareTo(request.amount()) < 0) {

            throw new IllegalStateException(
                    "Insufficient wallet balance"
            );
        }

        String currency =
                request.currency() == null ||
                        request.currency().isBlank()
                        ? "INR"
                        : request.currency()
                        .trim()
                        .toUpperCase();

        Payment payment = Payment.builder()
                .transactionId(
                        "TXN-" +
                                UUID.randomUUID()
                )
                .user(user)
                .wallet(wallet)
                .amount(request.amount())
                .currency(currency)
                .paymentType(PaymentType.PAYMENT)
                .status(PaymentStatus.RISK_ANALYSIS)
                .idempotencyKey(idempotencyKey)
                .description(
                        request.description() == null
                                ? null
                                : request.description().trim()
                )
                .build();

        payment = paymentRepository.save(payment);

        IdempotencyKey key =
                IdempotencyKey.builder()
                        .key(idempotencyKey)
                        .user(user)
                        .payment(payment)
                        .build();

        idempotencyKeyRepository.save(key);

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
}