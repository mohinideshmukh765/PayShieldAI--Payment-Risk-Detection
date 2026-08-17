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

    public List<FraudRuleEvaluation> evaluate(
            FraudRuleContext context
    ) {

        return rules.stream()
                .map(rule -> rule.evaluate(context))
                .toList();
    }

    public int calculateRiskPoints(
            List<FraudRuleEvaluation> results
    ) {

        return results.stream()
                .filter(FraudRuleEvaluation::triggered)
                .mapToInt(FraudRuleEvaluation::riskPoints)
                .sum();
    }
}