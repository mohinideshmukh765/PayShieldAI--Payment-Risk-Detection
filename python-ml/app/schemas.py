from pydantic import BaseModel, Field


class FraudPredictionRequest(BaseModel):

    amount: float = Field(..., gt=0)

    transactions_last_5_minutes: int = Field(
        default=0,
        ge=0
    )

    transactions_last_1_hour: int = Field(
        default=0,
        ge=0
    )

    average_transaction_amount: float = Field(
        default=0,
        ge=0
    )

    recent_failed_attempts: int = Field(
        default=0,
        ge=0
    )

    new_device: bool = False

    location_changed: bool = False

    destination_high_risk: bool = False


class FraudPredictionResponse(BaseModel):

    xgboost_probability: float

    isolation_forest_score: float

    anomaly_detected: bool