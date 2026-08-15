import numpy as np

from app.models.xgboost_model import XGBoostModel
from app.models.isolation_forest_model import IsolationForestModel


class ModelService:

    def __init__(self):

        self.xgboost = XGBoostModel()
        self.isolation_forest = IsolationForestModel()

    def load_models(self):

        xgboost_loaded = self.xgboost.load()
        isolation_loaded = self.isolation_forest.load()

        return {
            "xgboost": xgboost_loaded,
            "isolation_forest": isolation_loaded
        }

    def build_features(self, request):

        return np.array([[
            request.amount,
            request.transactions_last_5_minutes,
            request.transactions_last_1_hour,
            request.average_transaction_amount,
            request.recent_failed_attempts,
            int(request.new_device),
            int(request.location_changed),
            int(request.destination_high_risk)
        ]])

    def predict(self, request):

        features = self.build_features(request)

        xgboost_probability = (
            self.xgboost.predict_probability(features)
        )

        isolation_score, anomaly_detected = (
            self.isolation_forest.predict(features)
        )

        return {
            "xgboost_probability": xgboost_probability,
            "isolation_forest_score": isolation_score,
            "anomaly_detected": anomaly_detected
        }