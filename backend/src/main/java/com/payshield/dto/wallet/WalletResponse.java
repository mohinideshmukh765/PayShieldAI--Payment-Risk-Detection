package com.payshield.dto.wallet;

import com.payshield.entity.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(
        UUID walletId,
        BigDecimal balance,
        String currency,
        String status
) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus().name()
        );
    }
}