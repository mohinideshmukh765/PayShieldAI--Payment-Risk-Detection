package com.payshield.dto.transaction;

import com.payshield.entity.enums.TransactionStatus;
import com.payshield.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String transactionReference,
        UUID userId,
        TransactionType transactionType,
        BigDecimal amount,
        String currency,
        String sourceAccount,
        String destinationAccount,
        Instant transactionTime,
        TransactionStatus status,
        BigDecimal userWalletBalance,
        Instant createdAt
) {
}