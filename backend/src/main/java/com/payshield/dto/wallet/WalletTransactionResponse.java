package com.payshield.dto.wallet;

import com.payshield.entity.WalletTransaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID id,
        UUID paymentId,
        String type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String reference,
        Instant createdAt
) {

    public static WalletTransactionResponse from(
            WalletTransaction transaction
    ) {

        return new WalletTransactionResponse(
                transaction.getId(),
                transaction.getPayment() != null
                        ? transaction.getPayment().getId()
                        : null,
                transaction.getType().name(),
                transaction.getAmount(),
                transaction.getBalanceBefore(),
                transaction.getBalanceAfter(),
                transaction.getReference(),
                transaction.getCreatedAt()
        );
    }
}