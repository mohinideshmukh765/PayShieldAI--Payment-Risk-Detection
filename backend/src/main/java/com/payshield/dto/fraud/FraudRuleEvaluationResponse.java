package com.payshield.dto.fraud;

import com.payshield.fraud.rule.FraudRuleResult;

import java.util.List;

public record FraudRuleEvaluationResponse(

        List<FraudRuleResult> results,

        int totalRiskPoints,

        long triggeredRules

) {
}