package com.payshield.controller;

import com.payshield.dto.ApiResponse;
import com.payshield.dto.transaction.CreateTransactionRequest;
import com.payshield.dto.transaction.TransactionResponse;
import com.payshield.dto.transaction.TransactionSummaryResponse;
import com.payshield.entity.enums.TransactionStatus;
import com.payshield.entity.enums.TransactionType;
import com.payshield.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        TransactionResponse response =
                transactionService.createTransaction(
                        email,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(
                                true,
                                "Transaction created successfully",
                                response
                        )
                );
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable UUID transactionId
    ) {

        TransactionResponse response =
                transactionService.getTransaction(transactionId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Transaction retrieved successfully",
                        response
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionSummaryResponse>>>
    getTransactions(

            @RequestParam(required = false)
            TransactionStatus status,

            @RequestParam(required = false)
            TransactionType type,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        if (page < 0) {
            page = 0;
        }

        if (size < 1 || size > 100) {
            size = 20;
        }

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        Page<TransactionSummaryResponse> response =
                transactionService.getTransactions(
                        status,
                        type,
                        pageable
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Transactions retrieved successfully",
                        response
                )
        );
    }

    @PutMapping("/{transactionId}/status")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransactionStatus(
            @PathVariable UUID transactionId,
            @RequestParam TransactionStatus status
    ) {

        TransactionResponse response =
                transactionService.updateTransactionStatus(
                        transactionId,
                        status
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Transaction status updated to " + status,
                        response
                )
        );
    }
}