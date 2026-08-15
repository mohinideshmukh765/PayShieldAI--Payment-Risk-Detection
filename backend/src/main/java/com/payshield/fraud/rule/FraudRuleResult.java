package com.payshield.fraud.rule;

import java.math.BigDecimal;

public record FraudRuleResult(

        FraudRuleType ruleType,

        boolean triggered,

        int riskPoints,

        String reason,

        BigDecimal observedValue,

        BigDecimal threshold

) {
}