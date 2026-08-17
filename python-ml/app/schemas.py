from decimal import Decimal

from pydantic import BaseModel


class FraudPredictionRequest(BaseModel):

    step: int
    type: str
    amount: Decimal

    oldbalanceOrg: Decimal
    newbalanceOrig: Decimal

    oldbalanceDest: Decimal
    newbalanceDest: Decimal

    isFlaggedFraud: int


class FraudPredictionResponse(BaseModel):

    xgboostProbability: float
    xgboostPrediction: int

    isolationForestScore: float
    isolationForestAnomaly: bool

    modelVersion: str