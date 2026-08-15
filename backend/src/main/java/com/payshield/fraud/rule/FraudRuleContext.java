package com.payshield.fraud.rule;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudRuleContext(

        UUID userId,

        UUID transactionId,

        BigDecimal amount,

        Instant transactionTime,

        long transactionsLast5Minutes,

        long transactionsLast1Hour,

        BigDecimal averageTransactionAmount,

        long recentFailedAttempts,

        boolean newDevice,

        boolean locationChanged,

        boolean destinationHighRisk

) {
}