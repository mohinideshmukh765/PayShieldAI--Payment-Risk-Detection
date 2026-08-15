package com.payshield.service;

import com.payshield.dto.wallet.WalletResponse;
import com.payshield.dto.wallet.WalletTransactionResponse;
import com.payshield.entity.User;
import com.payshield.entity.Wallet;
import com.payshield.entity.WalletTransaction;
import com.payshield.entity.enums.WalletStatus;
import com.payshield.entity.enums.WalletTransactionType;
import com.payshield.repository.WalletRepository;
import com.payshield.repository.WalletTransactionRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository
            walletTransactionRepository;

    public WalletService(
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository
    ) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository =
                walletTransactionRepository;
    }

    @Transactional
    public Wallet createWallet(User user) {

        if (walletRepository.existsByUserId(user.getId())) {
            return walletRepository
                    .findByUserId(user.getId())
                    .orElseThrow();
        }

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .version(0L)
                .build();

        return walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID userId) {

        Wallet wallet = getUserWallet(userId);

        return WalletResponse.from(wallet);
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse>
    getTransactions(UUID userId) {

        Wallet wallet = getUserWallet(userId);

        return walletTransactionRepository
                .findByWalletIdOrderByCreatedAtDesc(wallet.getId())
                .stream()
                .map(WalletTransactionResponse::from)
                .toList();
    }

    @Transactional
    public void credit(
            UUID userId,
            BigDecimal amount,
            String reference
    ) {

        validateAmount(amount);

        Wallet wallet = getUserWallet(userId);

        validateActive(wallet);

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);

        wallet.setBalance(after);

        walletRepository.save(wallet);

        WalletTransaction ledgerEntry =
                WalletTransaction.builder()
                        .wallet(wallet)
                        .type(WalletTransactionType.CREDIT)
                        .amount(amount)
                        .balanceBefore(before)
                        .balanceAfter(after)
                        .reference(reference)
                        .build();

        walletTransactionRepository.save(ledgerEntry);
    }

    @Transactional
    public void debit(
            UUID userId,
            BigDecimal amount,
            String reference
    ) {

        validateAmount(amount);

        Wallet wallet = getUserWallet(userId);

        validateActive(wallet);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Insufficient wallet balance"
            );
        }

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.subtract(amount);

        wallet.setBalance(after);

        walletRepository.save(wallet);

        WalletTransaction ledgerEntry =
                WalletTransaction.builder()
                        .wallet(wallet)
                        .type(WalletTransactionType.DEBIT)
                        .amount(amount)
                        .balanceBefore(before)
                        .balanceAfter(after)
                        .reference(reference)
                        .build();

        walletTransactionRepository.save(ledgerEntry);
    }

    public Wallet getUserWallet(UUID userId) {

        return walletRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Wallet not found"
                        )
                );
    }

    private void validateAmount(BigDecimal amount) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }
    }

    private void validateActive(Wallet wallet) {

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Wallet is not active"
            );
        }
    }
}