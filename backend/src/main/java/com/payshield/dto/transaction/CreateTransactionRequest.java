package com.payshield.dto.transaction;

import com.payshield.entity.enums.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateTransactionRequest(

        @NotNull(message = "Transaction type is required")
        TransactionType transactionType,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must contain 3 characters")
        String currency,

        @NotBlank(message = "Source account is required")
        @Size(max = 100)
        String sourceAccount,

        @NotBlank(message = "Destination account is required")
        @Size(max = 100)
        String destinationAccount,

        @NotNull(message = "Transaction time is required")
        Instant transactionTime
) {
}