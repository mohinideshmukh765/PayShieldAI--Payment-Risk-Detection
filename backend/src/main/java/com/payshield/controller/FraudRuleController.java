package com.payshield.controller;

import com.payshield.dto.fraud.FraudRuleEvaluationRequest;
import com.payshield.dto.fraud.FraudRuleEvaluationResponse;
import com.payshield.fraud.rule.FraudRuleEngine;
import com.payshield.fraud.rule.FraudRuleContext;
import com.payshield.fraud.rule.FraudRuleEvaluation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud/rules")
public class FraudRuleController {

    private final FraudRuleEngine fraudRuleEngine;

    public FraudRuleController(
            FraudRuleEngine fraudRuleEngine
    ) {
        this.fraudRuleEngine = fraudRuleEngine;
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasRole('ANALYST')")
    public FraudRuleEvaluationResponse evaluate(
            @Valid
            @RequestBody
            FraudRuleEvaluationRequest request
    ) {

        FraudRuleContext context =
                new FraudRuleContext(
                        request.userId(),
                        request.transactionId(),
                        request.amount(),
                        Instant.now(),
                        request.transactionsLast5Minutes(),
                        request.transactionsLast1Hour(),
                        request.averageTransactionAmount(),
                        request.recentFailedAttempts(),
                        request.newDevice(),
                        request.locationChanged(),
                        request.destinationHighRisk()
                );

        List<FraudRuleEvaluation> results =
                fraudRuleEngine.evaluate(context);

        int totalRiskPoints =
                fraudRuleEngine.calculateRiskPoints(results);

        long triggeredRules =
                results.stream()
                        .filter(FraudRuleEvaluation::triggered)
                        .count();

        return new FraudRuleEvaluationResponse(
                results,
                totalRiskPoints,
                triggeredRules
        );
    }
}