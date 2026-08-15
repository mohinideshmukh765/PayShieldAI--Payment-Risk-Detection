package com.payshield.fraud.rule;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FraudRuleEngine {

    private final List<FraudRule> rules;

    public FraudRuleEngine(
            List<FraudRule> rules
    ) {
        this.rules = rules;
    }

    public List<FraudRuleResult> evaluate(
            FraudRuleContext context
    ) {

        return rules.stream()
                .map(rule -> rule.evaluate(context))
                .toList();
    }

    public int calculateRiskPoints(
            List<FraudRuleResult> results
    ) {

        return results.stream()
                .filter(FraudRuleResult::triggered)
                .mapToInt(FraudRuleResult::riskPoints)
                .sum();
    }
}