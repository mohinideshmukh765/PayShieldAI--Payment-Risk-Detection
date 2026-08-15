package com.payshield.dto.fraud;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record FraudRuleEvaluationRequest(

        UUID transactionId,

        @NotNull
        UUID userId,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,

        long transactionsLast5Minutes,

        long transactionsLast1Hour,

        BigDecimal averageTransactionAmount,

        long recentFailedAttempts,

        boolean newDevice,

        boolean locationChanged,

        boolean destinationHighRisk
) {
}