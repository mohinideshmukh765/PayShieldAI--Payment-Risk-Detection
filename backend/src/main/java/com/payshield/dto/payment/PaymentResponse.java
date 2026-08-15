package com.payshield.dto.payment;

import com.payshield.entity.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        String transactionId,
        BigDecimal amount,
        String currency,
        String paymentType,
        String status,
        String description,
        Instant createdAt,
        Instant updatedAt
) {

    public static PaymentResponse from(Payment payment) {

        return new PaymentResponse(
                payment.getId(),
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentType().name(),
                payment.getStatus().name(),
                payment.getDescription(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}