package com.payshield.controller;

import com.payshield.dto.payment.CreatePaymentRequest;
import com.payshield.dto.payment.PaymentResponse;
import com.payshield.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(
            PaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentResponse createPayment(
            @AuthenticationPrincipal UserDetails userDetails,

            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @Valid
            @RequestBody
            CreatePaymentRequest request
    ) {

        return paymentService.createPayment(
                userDetails.getUsername(),
                idempotencyKey,
                request
        );
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID paymentId
    ) {

        return paymentService.getPayment(
                userDetails.getUsername(),
                paymentId
        );
    }

    @GetMapping
    public List<PaymentResponse> getPayments(
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        return paymentService.getPayments(
                userDetails.getUsername()
        );
    }
}