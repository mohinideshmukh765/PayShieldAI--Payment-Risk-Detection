package com.payshield.dto;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {
}