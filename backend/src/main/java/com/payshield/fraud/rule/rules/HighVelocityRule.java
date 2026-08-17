package com.payshield.fraud.rule.rules;

import com.payshield.fraud.config.FraudRuleProperties;
import com.payshield.fraud.rule.FraudRule;
import com.payshield.fraud.rule.FraudRuleContext;
import com.payshield.fraud.rule.FraudRuleEvaluation;
import com.payshield.fraud.rule.FraudRuleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HighVelocityRule implements FraudRule {

    private final FraudRuleProperties properties;

    public HighVelocityRule(
            FraudRuleProperties properties
    ) {
        this.properties = properties;
    }

    @Override
    public FraudRuleType getType() {
        return FraudRuleType.HIGH_VELOCITY;
    }

    @Override
    public FraudRuleEvaluation evaluate(
            FraudRuleContext context
    ) {

        boolean triggered =
                context.transactionsLast5Minutes()
                        >= properties
                        .getTransactionsLast5MinutesThreshold()
                        ||
                        context.transactionsLast1Hour()
                                >= properties
                                .getTransactionsLast1HourThreshold();

        long observed =
                Math.max(
                        context.transactionsLast5Minutes(),
                        context.transactionsLast1Hour()
                );

        return new FraudRuleEvaluation(
                getType(),
                triggered,
                triggered
                        ? properties.getVelocityPoints()
                        : 0,
                triggered
                        ? "Transaction frequency exceeds configured velocity threshold"
                        : "Transaction frequency is within normal limits",
                BigDecimal.valueOf(observed),
                BigDecimal.valueOf(
                        Math.min(
                                properties
                                        .getTransactionsLast5MinutesThreshold(),
                                properties
                                        .getTransactionsLast1HourThreshold()
                        )
                )
        );
    }
}