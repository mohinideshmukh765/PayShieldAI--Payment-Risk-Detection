package com.payshield.repository;

import com.payshield.entity.FraudRuleResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FraudRuleResultRepository
        extends JpaRepository<FraudRuleResult, UUID> {

    List<FraudRuleResult> findByTransactionId(UUID transactionId);
}