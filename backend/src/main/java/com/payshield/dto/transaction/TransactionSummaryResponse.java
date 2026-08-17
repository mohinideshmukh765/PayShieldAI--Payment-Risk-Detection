package com.payshield.dto.transaction;

import com.payshield.entity.enums.TransactionStatus;
import com.payshield.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionSummaryResponse(
        UUID id,
        String transactionReference,
        UUID userId,
        TransactionType transactionType,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        Instant transactionTime
) {
}