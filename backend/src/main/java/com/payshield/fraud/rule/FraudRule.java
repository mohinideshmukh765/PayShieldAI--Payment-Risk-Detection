package com.payshield.fraud.rule;

public interface FraudRule {

    FraudRuleType getType();

    FraudRuleEvaluation evaluate(FraudRuleContext context);
}