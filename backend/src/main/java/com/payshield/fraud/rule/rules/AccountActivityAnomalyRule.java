package com.payshield.fraud.rule.rules;

import com.payshield.fraud.config.FraudRuleProperties;
import com.payshield.fraud.rule.FraudRule;
import com.payshield.fraud.rule.FraudRuleContext;
import com.payshield.fraud.rule.FraudRuleEvaluation;
import com.payshield.fraud.rule.FraudRuleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AccountActivityAnomalyRule implements FraudRule {

    private final FraudRuleProperties properties;

    public AccountActivityAnomalyRule(
            FraudRuleProperties properties
    ) {
        this.properties = properties;
    }

    @Override
    public FraudRuleType getType() {
        return FraudRuleType.ACCOUNT_ACTIVITY_ANOMALY;
    }

    @Override
    public FraudRuleEvaluation evaluate(
            FraudRuleContext context
    ) {

        boolean triggered =
                context.recentFailedAttempts()
                        >= properties.getFailedAttemptsThreshold()
                        ||
                        context.newDevice()
                                && context.locationChanged();

        return new FraudRuleEvaluation(
                getType(),
                triggered,
                triggered
                        ? properties.getActivityAnomalyPoints()
                        : 0,
                triggered
                        ? "Suspicious account activity detected"
                        : "No significant account activity anomaly detected",
                BigDecimal.valueOf(
                        context.recentFailedAttempts()
                ),
                BigDecimal.valueOf(
                        properties.getFailedAttemptsThreshold()
                )
        );
    }
}