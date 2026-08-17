package com.payshield.dto.ml;

import java.math.BigDecimal;

public record FraudPredictionResponse(

        BigDecimal xgboostProbability,

        int xgboostPrediction,

        BigDecimal isolationForestScore,

        boolean isolationForestAnomaly,

        String modelVersion

) {
}