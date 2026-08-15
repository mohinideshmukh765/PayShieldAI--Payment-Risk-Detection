from pydantic import BaseModel, Field


class FraudPredictionRequest(BaseModel):

    step: int = Field(..., ge=0)

    transaction_type: str

    amount: float = Field(..., gt=0)

    old_balance_origin: float = Field(
        ..., ge=0
    )

    new_balance_origin: float = Field(
        ..., ge=0
    )

    old_balance_destination: float = Field(
        ..., ge=0
    )

    new_balance_destination: float = Field(
        ..., ge=0
    )

    flagged_fraud: int = Field(
        default=0,
        ge=0,
        le=1
    )


class FraudPredictionResponse(BaseModel):

    xgboost_probability: float

    isolation_forest_score: float

    anomaly_detected: bool