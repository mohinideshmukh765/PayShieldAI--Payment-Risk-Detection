package com.payshield.controller;

import com.payshield.dto.wallet.WalletResponse;
import com.payshield.dto.wallet.WalletTransactionResponse;
import com.payshield.service.WalletService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.payshield.repository.UserRepository;

import java.util.List;

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
}