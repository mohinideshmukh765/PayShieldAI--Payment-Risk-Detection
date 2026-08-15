package com.payshield.fraud.rule.rules;

import com.payshield.fraud.config.FraudRuleProperties;
import com.payshield.fraud.rule.FraudRule;
import com.payshield.fraud.rule.FraudRuleContext;
import com.payshield.fraud.rule.FraudRuleResult;
import com.payshield.fraud.rule.FraudRuleType;
import org.springframework.stereotype.Component;

@Component
public class LargeTransactionRule implements FraudRule {

    private final FraudRuleProperties properties;

    public LargeTransactionRule(
            FraudRuleProperties properties
    ) {
        this.properties = properties;
    }

    @Override
    public FraudRuleType getType() {
        return FraudRuleType.LARGE_TRANSACTION;
    }

    @Override
    public FraudRuleResult evaluate(
            FraudRuleContext context
    ) {

        boolean triggered =
                context.amount()
                        .compareTo(
                                properties
                                        .getLargeTransactionThreshold()
                        ) > 0;

        return new FraudRuleResult(
                getType(),
                triggered,
                triggered
                        ? properties.getLargeTransactionPoints()
                        : 0,
                triggered
                        ? "Transaction amount exceeds configured large transaction threshold"
                        : "Transaction amount is within normal large-transaction threshold",
                context.amount(),
                properties.getLargeTransactionThreshold()
        );
    }
}