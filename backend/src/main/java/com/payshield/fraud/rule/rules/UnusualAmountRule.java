package com.payshield.fraud.rule.rules;

import com.payshield.fraud.config.FraudRuleProperties;
import com.payshield.fraud.rule.FraudRule;
import com.payshield.fraud.rule.FraudRuleContext;
import com.payshield.fraud.rule.FraudRuleEvaluation;
import com.payshield.fraud.rule.FraudRuleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class UnusualAmountRule implements FraudRule {

    private final FraudRuleProperties properties;

    public UnusualAmountRule(
            FraudRuleProperties properties
    ) {
        this.properties = properties;
    }

    @Override
    public FraudRuleType getType() {
        return FraudRuleType.UNUSUAL_AMOUNT;
    }

    @Override
    public FraudRuleEvaluation evaluate(
            FraudRuleContext context
    ) {

        BigDecimal average =
                context.averageTransactionAmount();

        if (average == null ||
                average.compareTo(BigDecimal.ZERO) <= 0) {

            return new FraudRuleEvaluation(
                    getType(),
                    false,
                    0,
                    "Insufficient transaction history to determine unusual amount",
                    context.amount(),
                    null
            );
        }

        BigDecimal threshold =
                average.multiply(
                        properties.getUnusualAmountMultiplier()
                );

        boolean triggered =
                context.amount()
                        .compareTo(threshold) > 0;

        return new FraudRuleEvaluation(
                getType(),
                triggered,
                triggered
                        ? properties.getUnusualAmountPoints()
                        : 0,
                triggered
                        ? "Transaction amount is significantly higher than user's historical average"
                        : "Transaction amount is consistent with user's historical average",
                context.amount(),
                threshold
        );
    }
}