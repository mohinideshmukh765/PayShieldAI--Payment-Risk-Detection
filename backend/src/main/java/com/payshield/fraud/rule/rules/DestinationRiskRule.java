package com.payshield.fraud.rule.rules;

import com.payshield.fraud.config.FraudRuleProperties;
import com.payshield.fraud.rule.FraudRule;
import com.payshield.fraud.rule.FraudRuleContext;
import com.payshield.fraud.rule.FraudRuleResult;
import com.payshield.fraud.rule.FraudRuleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DestinationRiskRule implements FraudRule {

    private final FraudRuleProperties properties;

    public DestinationRiskRule(
            FraudRuleProperties properties
    ) {
        this.properties = properties;
    }

    @Override
    public FraudRuleType getType() {
        return FraudRuleType.DESTINATION_RISK;
    }

    @Override
    public FraudRuleResult evaluate(
            FraudRuleContext context
    ) {

        boolean triggered =
                context.destinationHighRisk();

        return new FraudRuleResult(
                getType(),
                triggered,
                triggered
                        ? properties.getDestinationRiskPoints()
                        : 0,
                triggered
                        ? "Payment destination is marked as high risk"
                        : "Payment destination is not marked as high risk",
                triggered
                        ? BigDecimal.ONE
                        : BigDecimal.ZERO,
                BigDecimal.ONE
        );
    }
}