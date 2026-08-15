package com.payshield.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "fraud_predictions",
        indexes = {
                @Index(
                        name = "idx_predictions_transaction_id",
                        columnList = "transaction_id"
                ),
                @Index(
                        name = "idx_predictions_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "transaction_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_prediction_transaction")
    )
    private Transaction transaction;

    @Column(
            name = "xgboost_probability",
            precision = 6,
            scale = 5
    )
    private BigDecimal xgboostProbability;

    @Column(name = "xgboost_prediction")
    private Boolean xgboostPrediction;

    @Column(
            name = "isolation_score",
            precision = 10,
            scale = 6
    )
    private BigDecimal isolationScore;

    @Column(name = "isolation_anomaly")
    private Boolean isolationAnomaly;

    @Column(
            name = "risk_score",
            precision = 6,
            scale = 2
    )
    private BigDecimal riskScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}