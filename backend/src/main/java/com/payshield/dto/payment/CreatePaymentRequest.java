package com.payshield.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Amount must be greater than zero"
        )
        BigDecimal amount,

        @Size(
                max = 3,
                message = "Currency must contain at most 3 characters"
        )
        String currency,

        @Size(
                max = 500,
                message = "Description cannot exceed 500 characters"
        )
        String description
) {
}