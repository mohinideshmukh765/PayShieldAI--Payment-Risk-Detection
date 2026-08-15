package com.payshield.fraud.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "fraud.rules")
public class FraudRuleProperties {

    private BigDecimal largeTransactionThreshold =
            new BigDecimal("50000");

    private BigDecimal unusualAmountMultiplier =
            new BigDecimal("3.0");

    private long transactionsLast5MinutesThreshold = 5;

    private long transactionsLast1HourThreshold = 20;

    private long failedAttemptsThreshold = 3;

    private int largeTransactionPoints = 25;

    private int velocityPoints = 25;

    private int unusualAmountPoints = 20;

    private int activityAnomalyPoints = 15;

    private int destinationRiskPoints = 30;
}