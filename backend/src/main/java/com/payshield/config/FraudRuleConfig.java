package com.payshield.config;

import com.payshield.fraud.config.FraudRuleProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FraudRuleProperties.class)
public class FraudRuleConfig {
}