import joblib
import numpy as np

from app.config import XGBOOST_MODEL_PATH


class XGBoostModel:

    def __init__(self):
        self.model = None

    def load(self):
        if not XGBOOST_MODEL_PATH.exists():
            return False

        self.model = joblib.load(XGBOOST_MODEL_PATH)
        return True

    def predict_probability(self, features: np.ndarray) -> float:

        if self.model is None:
            raise RuntimeError("XGBoost model is not loaded")

        probability = self.model.predict_proba(features)[0][1]

        return float(probability)