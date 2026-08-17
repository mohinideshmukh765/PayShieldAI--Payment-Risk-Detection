package com.payshield.service;

import com.payshield.client.MLPredictionClient;
import com.payshield.client.MLPredictionFeignClient;
import com.payshield.dto.ml.FraudPredictionRequest;
import com.payshield.dto.ml.FraudPredictionResponse;
import com.payshield.entity.FraudPrediction;
import com.payshield.entity.FraudRuleResult;
import com.payshield.entity.Transaction;
import com.payshield.entity.Wallet;
import com.payshield.fraud.rule.FraudRuleContext;
import com.payshield.fraud.rule.FraudRuleEngine;
import com.payshield.fraud.rule.FraudRuleEvaluation;
import com.payshield.repository.FraudPredictionRepository;
import com.payshield.repository.FraudRuleResultRepository;
import com.payshield.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class FraudAssessmentService {

    private final FraudRuleEngine fraudRuleEngine;
    private final FraudRuleResultRepository fraudRuleResultRepository;
    private final FraudPredictionRepository fraudPredictionRepository;
    // change field type and constructor param from MLPredictionClient to MLPredictionFeignClient
    private final MLPredictionFeignClient mlPredictionClient;    private final RiskAssessmentService riskAssessmentService;

    private final TransactionRepository transactionRepository;
    public FraudAssessmentService(
            FraudRuleEngine fraudRuleEngine,
            FraudRuleResultRepository fraudRuleResultRepository,
            FraudPredictionRepository fraudPredictionRepository,
            MLPredictionFeignClient mlPredictionClient,
            RiskAssessmentService riskAssessmentService, TransactionRepository transactionRepository
    ) {
        this.fraudRuleEngine = fraudRuleEngine;
        this.fraudRuleResultRepository =
                fraudRuleResultRepository;
        this.fraudPredictionRepository =
                fraudPredictionRepository;
        this.mlPredictionClient = mlPredictionClient;

        this.riskAssessmentService =
                riskAssessmentService;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public FraudAssessmentResult assess(
            Transaction transaction,
            BigDecimal oldBalance
    ) {

        // =====================================================
        // 1. FRAUD RULES
        // =====================================================

        FraudRuleContext context =
                new FraudRuleContext(
                        transaction.getUser().getId(),
                        transaction.getId(),
                        transaction.getAmount(),
                        transaction.getTransactionTime(),

                        transactionRepository.countByUserIdSince(
                                transaction.getUser().getId(),
                                transaction.getTransactionTime().minus(Duration.ofMinutes(5))
                        ),
                        transactionRepository.countByUserIdSince(
                                transaction.getUser().getId(),
                                transaction.getTransactionTime().minus(Duration.ofHours(1))
                        ),
                        resolveAverageAmount(transaction),
                        transactionRepository.countRecentFailedAttempts(
                                transaction.getUser().getId(),
                                transaction.getTransactionTime().minus(Duration.ofHours(24))
                        ),
                        false,   // newDevice — needs device-fingerprint data; leave false until that exists
                        false,   // locationChanged — needs IP/geo data; leave false until that exists
                        false    // destinationHighRisk — needs a destination denylist; leave false until that exists
                );
        List<FraudRuleEvaluation> evaluations =
                fraudRuleEngine.evaluate(context);

        System.out.println("\n========== FRAUD RULES ==========");

        for (FraudRuleEvaluation evaluation : evaluations) {

            System.out.println(
                    "Rule: "
                            + evaluation.ruleType().name()
                            + " | Triggered: "
                            + evaluation.triggered()
            );
        }

        System.out.println(
                "Rule Score: "
                        + fraudRuleEngine.calculateRiskPoints(evaluations)
        );

        System.out.println("=================================\n");

        int ruleScore =
                fraudRuleEngine.calculateRiskPoints(
                        evaluations
                );

        // =====================================================
        // 2. SAVE RULE RESULTS
        // =====================================================

        saveRuleResults(
                transaction,
                evaluations
        );

        // =====================================================
        // 3. BUILD ML REQUEST
        // =====================================================

        FraudPredictionRequest mlRequest =
                buildMLRequest(
                        transaction,
                        oldBalance
                );

        // =====================================================
        // 4. CALL PYTHON ML SERVICE
        // =====================================================

        FraudPredictionResponse mlResponse =
                mlPredictionClient.predict(
                        mlRequest
                );

        System.out.println("\n========== ML RESPONSE ==========");
        System.out.println(
                "XGBoost Probability: "
                        + mlResponse.xgboostProbability()
        );

        System.out.println(
                "XGBoost Prediction: "
                        + mlResponse.xgboostPrediction()
        );

        System.out.println(
                "Isolation Forest Score: "
                        + mlResponse.isolationForestScore()
        );

        System.out.println(
                "Isolation Forest Anomaly: "
                        + mlResponse.isolationForestAnomaly()
        );

        System.out.println("=================================\n");

        // =====================================================
        // 5. CALCULATE FINAL RISK
        // =====================================================

        boolean anyHighSeverityRuleTriggered =
                evaluations.stream()
                        .anyMatch(e -> e.triggered() && ruleIsHighSeverity(e));

        double riskScore =
                riskAssessmentService.calculateRiskScore(
                        mlResponse.xgboostProbability().doubleValue(),
                        mlResponse.isolationForestScore().doubleValue(),
                        ruleScore,
                        anyHighSeverityRuleTriggered
                );


        String riskLevel =
                riskAssessmentService.determineRiskLevel(
                        riskScore
                );

        String decision =
                riskAssessmentService.determineDecision(
                        riskLevel,
                        mlResponse.xgboostPrediction(),
                        mlResponse.isolationForestAnomaly()
                );

        // =====================================================
        // 6. SAVE FRAUD PREDICTION
        // =====================================================

        FraudPrediction prediction =
                FraudPrediction.builder()
                        .transaction(transaction)
                        .xgboostProbability(
                                mlResponse
                                        .xgboostProbability()
                        )
                        .xgboostPrediction(
                                mlResponse
                                        .xgboostPrediction() == 1
                        )
                        .isolationScore(
                                mlResponse
                                        .isolationForestScore()
                        )
                        .isolationAnomaly(
                                mlResponse
                                        .isolationForestAnomaly()
                        )
                        .riskScore(
                                BigDecimal.valueOf(
                                        riskScore
                                )
                        )
                        .riskLevel(riskLevel)
                        .modelVersion(
                                mlResponse.modelVersion()
                        )
                        .createdAt(Instant.now())
                        .build();

        fraudPredictionRepository.save(
                prediction
        );

        return new FraudAssessmentResult(
                evaluations,
                ruleScore,
                mlResponse,
                riskScore,
                riskLevel,
                decision
        );
    }

    // =========================================================
    // SAVE RULE RESULTS
    // =========================================================

    private void saveRuleResults(
            Transaction transaction,
            List<FraudRuleEvaluation> evaluations
    ) {

        List<FraudRuleResult> entities =
                evaluations.stream()
                        .map(result ->
                                FraudRuleResult.builder()
                                        .transaction(transaction)
                                        .ruleName(
                                                result.ruleType()
                                                        .name()
                                        )
                                        .ruleResult(
                                                result.triggered()
                                        )
                                        .severity(
                                                result.triggered()
                                                        ? "HIGH"
                                                        : "LOW"
                                        )
                                        .build()
                        )
                        .toList();

        fraudRuleResultRepository.saveAll(
                entities
        );
    }

    // =========================================================
    // BUILD ML REQUEST
    // =========================================================

    private FraudPredictionRequest buildMLRequest(
            Transaction transaction,
            BigDecimal oldBalance
    ) {

        BigDecimal amount =
                transaction.getAmount();

        BigDecimal newBalance =
                oldBalance.subtract(amount);

        return new FraudPredictionRequest(
                1,
                "PAYMENT",
                amount,
                oldBalance,
                newBalance.max(BigDecimal.ZERO),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0
        );
    }

    public record FraudAssessmentResult(

            List<FraudRuleEvaluation> ruleEvaluations,

            int ruleScore,

            FraudPredictionResponse mlPrediction,

            double riskScore,

            String riskLevel,

            String decision

    ) {
    }

    private BigDecimal resolveAverageAmount(Transaction transaction) {
        BigDecimal avg = transactionRepository.findAverageAmountByUserId(
                transaction.getUser().getId()
        );
        return avg == null ? transaction.getAmount() : avg;
    }

    private boolean ruleIsHighSeverity(FraudRuleEvaluation e) {
        return e.triggered(); // matches your existing HIGH/LOW logic in saveRuleResults
    }
}