package com.payshield.fraud.rule;

import java.math.BigDecimal;

public record FraudRuleEvaluation(

        FraudRuleType ruleType,

        boolean triggered,

        int riskPoints,

        String reason,

        BigDecimal observedValue,

        BigDecimal threshold

) {
}