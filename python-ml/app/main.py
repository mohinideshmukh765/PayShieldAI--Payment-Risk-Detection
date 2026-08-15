from fastapi import FastAPI, HTTPException

from app.model_service import ModelService
from app.schemas import (
    FraudPredictionRequest,
    FraudPredictionResponse
)


app = FastAPI(
    title="PayShield AI ML Service",
    description="Machine learning service for payment fraud detection",
    version="1.0.0"
)


model_service = ModelService()


@app.on_event("startup")
def startup_event():

    model_status = model_service.load_models()

    print("ML Model Status:")
    print(model_status)


@app.get("/health")
def health():

    return {
        "status": "UP",
        "service": "payshield-ml"
    }


@app.post(
    "/predict",
    response_model=FraudPredictionResponse
)
def predict(request: FraudPredictionRequest):

    try:

        result = model_service.predict(request)

        return result

    except RuntimeError as exception:

        raise HTTPException(
            status_code=503,
            detail=str(exception)
        )