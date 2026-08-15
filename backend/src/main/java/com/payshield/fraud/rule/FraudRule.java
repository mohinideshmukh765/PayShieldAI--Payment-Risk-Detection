package com.payshield.fraud.rule;

public interface FraudRule {

    FraudRuleType getType();

    FraudRuleResult evaluate(FraudRuleContext context);
}