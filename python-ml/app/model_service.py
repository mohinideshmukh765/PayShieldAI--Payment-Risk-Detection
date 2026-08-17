import joblib
import pandas as pd

from app.config import (
    XGBOOST_MODEL_PATH,
    ISOLATION_FOREST_MODEL_PATH
)


class ModelService:

    def __init__(self):

        self.xgboost_bundle = None
        self.isolation_bundle = None

    # =========================================================
    # Load Models
    # =========================================================

    def load_models(self):

        self.xgboost_bundle = joblib.load(
            XGBOOST_MODEL_PATH
        )

        self.isolation_bundle = joblib.load(
            ISOLATION_FOREST_MODEL_PATH
        )

        return {
            "xgboost": True,
            "isolation_forest": True
        }

    # =========================================================
    # Build Features
    # =========================================================

    def build_features(self, request):

        hour = request.step % 24

        day = request.step // 24

        origin_balance_error = (
            request.oldbalanceOrg
            - request.amount
            - request.newbalanceOrig
        )

        destination_balance_error = (
            request.oldbalanceDest
            + request.amount
            - request.newbalanceDest
        )

        amount_to_origin_balance = (
            request.amount
            / (request.oldbalanceOrg + 1)
        )

        amount_to_destination_balance = (
            request.amount
            / (request.oldbalanceDest + 1)
        )

        return pd.DataFrame([{

            "step": request.step,

            "type": request.type,

            "amount": request.amount,

            "oldbalanceOrg": request.oldbalanceOrg,

            "newbalanceOrig": request.newbalanceOrig,

            "oldbalanceDest": request.oldbalanceDest,

            "newbalanceDest": request.newbalanceDest,

            "isFlaggedFraud": request.isFlaggedFraud,

            "hour": hour,

            "day": day,

            "origin_balance_error":
                origin_balance_error,

            "destination_balance_error":
                destination_balance_error,

            "amount_to_origin_balance":
                amount_to_origin_balance,

            "amount_to_destination_balance":
                amount_to_destination_balance
        }])

    # =========================================================
    # Normalize Isolation Forest Score
    # =========================================================

    def normalize_anomaly_score(self, raw_score):

        # Isolation Forest decision_function:
        #
        # positive -> more normal
        # negative -> more anomalous
        #
        # Convert approximately to 0-100 risk representation.

        score = 50 - (raw_score * 50)

        return max(
            0.0,
            min(100.0, score)
        )

    # =========================================================
    # Prediction
    # =========================================================

    def predict(self, request):

        if (
            self.xgboost_bundle is None
            or self.isolation_bundle is None
        ):
            raise RuntimeError(
                "ML models are not loaded."
            )

        # -----------------------------------------------------
        # Build raw PaySim feature DataFrame
        # -----------------------------------------------------

        features = self.build_features(request)

        # =====================================================
        # XGBoost
        # =====================================================

        xgb_preprocessor = (
            self.xgboost_bundle["preprocessor"]
        )

        xgb_model = (
            self.xgboost_bundle["model"]
        )

        xgb_features = (
            xgb_preprocessor.transform(features)
        )

        xgb_probability = (
            xgb_model
            .predict_proba(xgb_features)[0][1]
        )

        xgb_prediction = int(
            xgb_probability >= 0.5
        )

        # =====================================================
        # Isolation Forest
        # =====================================================

        isolation_preprocessor = (
            self.isolation_bundle["preprocessor"]
        )

        isolation_model = (
            self.isolation_bundle["model"]
        )

        isolation_features = (
            isolation_preprocessor.transform(features)
        )

        raw_score = (
            isolation_model
            .decision_function(
                isolation_features
            )[0]
        )

        isolation_prediction = (
            isolation_model
            .predict(
                isolation_features
            )[0]
        )

        isolation_anomaly = (
            isolation_prediction == -1
        )

        isolation_score = (
            self.normalize_anomaly_score(
                raw_score
            )
        )

        # =====================================================
        # Final Response
        # =====================================================

        return {

            "xgboostProbability":
                float(xgb_probability),

            "xgboostPrediction":
                xgb_prediction,

            "isolationForestScore":
                float(isolation_score),

            "isolationForestAnomaly":
                bool(isolation_anomaly),

            "modelVersion":
                "xgboost-v1-isolation-v1"
        }