package com.payshield.repository;

import com.payshield.entity.FraudPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FraudPredictionRepository
        extends JpaRepository<FraudPrediction, UUID> {

    Optional<FraudPrediction> findByTransactionId(UUID transactionId);
}