import joblib
import numpy as np

from app.config import ISOLATION_FOREST_MODEL_PATH


class IsolationForestModel:

    def __init__(self):
        self.model = None

    def load(self):
        if not ISOLATION_FOREST_MODEL_PATH.exists():
            return False

        self.model = joblib.load(
            ISOLATION_FOREST_MODEL_PATH
        )

        return True

    def predict(self, features: np.ndarray):

        if self.model is None:
            raise RuntimeError(
                "Isolation Forest model is not loaded"
            )

        prediction = self.model.predict(features)[0]

        score = self.model.decision_function(features)[0]

        anomaly_detected = prediction == -1

        return float(score), bool(anomaly_detected)