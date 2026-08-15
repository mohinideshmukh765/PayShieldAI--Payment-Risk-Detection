import joblib
import numpy as np
import pandas as pd

from app.config import (
    XGBOOST_MODEL_PATH,
    ISOLATION_FOREST_MODEL_PATH
)


class ModelService:

    def __init__(self):

        self.xgboost_bundle = None
        self.isolation_bundle = None

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

    def build_features(self, request):

        hour = request.step % 24
        day = request.step // 24

        origin_balance_error = (
            request.old_balance_origin
            - request.amount
            - request.new_balance_origin
        )

        destination_balance_error = (
            request.old_balance_destination
            + request.amount
            - request.new_balance_destination
        )

        amount_to_origin_balance = (
            request.amount
            / (request.old_balance_origin + 1)
        )

        amount_to_destination_balance = (
            request.amount
            / (request.old_balance_destination + 1)
        )

        return pd.DataFrame([{
            "step": request.step,
            "type": request.transaction_type,
            "amount": request.amount,
            "oldbalanceOrg": request.old_balance_origin,
            "newbalanceOrig": request.new_balance_origin,
            "oldbalanceDest": request.old_balance_destination,
            "newbalanceDest": request.new_balance_destination,
            "isFlaggedFraud": request.flagged_fraud,
            "hour": hour,
            "day": day,
            "origin_balance_error": origin_balance_error,
            "destination_balance_error": destination_balance_error,
            "amount_to_origin_balance": amount_to_origin_balance,
            "amount_to_destination_balance": amount_to_destination_balance
        }])

    def predict(self, request):

        features = self.build_features(request)

        # XGBoost
        xgb_bundle = self.xgboost_bundle

        xgb_features = xgb_bundle[
            "preprocessor"
        ].transform(features)

        fraud_probability = (
            xgb_bundle["model"]
            .predict_proba(xgb_features)[0][1]
        )

        # Isolation Forest
        iso_bundle = self.isolation_bundle

        iso_features = iso_bundle[
            "preprocessor"
        ].transform(features)

        isolation_prediction = (
            iso_bundle["model"]
            .predict(iso_features)[0]
        )

        isolation_score = (
            iso_bundle["model"]
            .decision_function(iso_features)[0]
        )

        return {
            "xgboost_probability": float(
                fraud_probability
            ),
            "isolation_forest_score": float(
                isolation_score
            ),
            "anomaly_detected": (
                isolation_prediction == -1
            )
        }