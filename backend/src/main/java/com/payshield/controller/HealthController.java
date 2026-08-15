package com.payshield.controller;

import com.payshield.dto.ApiResponse;
import com.payshield.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/api/v1/health")
    public ApiResponse<String> health() {

        return new ApiResponse<>(
                true,
                "PayShield AI backend is running",
                healthService.getStatus()
        );
    }
}