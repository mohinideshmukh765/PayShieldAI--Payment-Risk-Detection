package com.payshield.dto.ml;

import java.math.BigDecimal;

public record FraudPredictionRequest(

        int step,

        String type,

        BigDecimal amount,

        BigDecimal oldbalanceOrg,

        BigDecimal newbalanceOrig,

        BigDecimal oldbalanceDest,

        BigDecimal newbalanceDest,

        int isFlaggedFraud

) {
}