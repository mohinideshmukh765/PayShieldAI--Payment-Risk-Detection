package com.payshield.dto.fraud;

import com.payshield.fraud.rule.FraudRuleEvaluation;

import java.util.List;

public record FraudRuleEvaluationResponse(

        List<FraudRuleEvaluation> results,

        int totalRiskPoints,

        long triggeredRules

) {
}