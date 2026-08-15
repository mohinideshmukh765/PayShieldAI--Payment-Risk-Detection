package com.payshield.dto.auth;

import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String email,
        Set<String> roles
) {
}