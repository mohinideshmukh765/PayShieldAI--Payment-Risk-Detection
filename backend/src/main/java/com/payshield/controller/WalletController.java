package com.payshield.controller;

import com.payshield.dto.wallet.WalletResponse;
import com.payshield.dto.wallet.WalletTransactionResponse;
import com.payshield.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.payshield.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;

    public WalletController(
            WalletService walletService,
            UserRepository userRepository
    ) {
        this.walletService = walletService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public WalletResponse getWallet(
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        var user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "User not found"
                        )
                );

        return walletService.getWallet(user.getId());
    }

    @GetMapping("/transactions")
    public List<WalletTransactionResponse> getTransactions(
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        var user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "User not found"
                        )
                );

        return walletService.getTransactions(user.getId());
    }

    /**
     * Top-up the authenticated user's wallet.
     *
     * POST /api/v1/wallet/topup
     * Body: { "amount": 5000 }
     *
     * Accessible to USER role.  Allows the user to add funds without
     * requiring a manual database update during demos.
     */
    @PostMapping("/topup")
    public ResponseEntity<WalletResponse> topUp(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> body
    ) {

        var user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() ->
                        new IllegalStateException("User not found")
                );

        Object rawAmount = body.get("amount");
        if (rawAmount == null) {
            throw new IllegalArgumentException("amount is required");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(rawAmount.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("amount must be a valid number");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        WalletResponse updated = walletService.topUp(user.getId(), amount);
        return ResponseEntity.ok(updated);
    }
}