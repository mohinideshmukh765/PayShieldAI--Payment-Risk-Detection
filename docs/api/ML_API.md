# ML Service API

Base URL: `http://localhost:8000` (Python FastAPI service)  
Authentication: **None** (internal service, called by Spring Boot backend only)  
Framework: **FastAPI 0.100+**

---

## Overview

The ML service is a standalone Python FastAPI application that loads two pre-trained models at startup and exposes a `/predict` endpoint. The Spring Boot backend calls this service via **Spring Cloud OpenFeign** (`MLPredictionFeignClient`) on every payment request.

---

## Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | Liveness check |
| `POST` | `/predict` | Score a transaction for fraud |

---

## GET `/health`

Liveness check. Used by the Spring Boot `HealthController` to verify the ML service is available.

### Response — `200 OK`

```json
{
  "status": "UP",
  "service": "payshield-ml"
}
```

### cURL Example

```bash
curl http://localhost:8000/health
```

---

## POST `/predict`

Scores a single payment transaction using both ML models and returns predictions.

### Request Body

```json
{
  "step": 1,
  "type": "PAYMENT",
  "amount": 75000.00,
  "oldbalanceOrg": 100000.00,
  "newbalanceOrig": 25000.00,
  "oldbalanceDest": 0.00,
  "newbalanceDest": 75000.00,
  "isFlaggedFraud": 0
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `step` | `int` | ✅ | Hour step in PaySim simulation (used to derive `hour` and `day` features) |
| `type` | `string` | ✅ | Transaction type (e.g., `PAYMENT`, `TRANSFER`) |
| `amount` | `decimal` | ✅ | Transaction amount |
| `oldbalanceOrg` | `decimal` | ✅ | Sender's balance before the transaction |
| `newbalanceOrig` | `decimal` | ✅ | Sender's balance after the transaction |
| `oldbalanceDest` | `decimal` | ✅ | Receiver's balance before the transaction |
| `newbalanceDest` | `decimal` | ✅ | Receiver's balance after the transaction |
| `isFlaggedFraud` | `int` | ✅ | Pre-existing system fraud flag (`1` = flagged, `0` = clean) |

### Feature Engineering (Internal)

The service internally computes 4 additional features before passing to the models:

| Derived Feature | Formula |
|---|---|
| `hour` | `step % 24` |
| `day` | `step // 24` |
| `origin_balance_error` | `oldbalanceOrg − amount − newbalanceOrig` |
| `destination_balance_error` | `oldbalanceDest + amount − newbalanceDest` |
| `amount_to_origin_balance` | `amount / (oldbalanceOrg + 1)` |
| `amount_to_destination_balance` | `amount / (oldbalanceDest + 1)` |

Total input to models: **14 features**.

### Response — `200 OK`

```json
{
  "xgboostProbability": 0.87,
  "xgboostPrediction": 1,
  "isolationForestScore": 72.4,
  "isolationForestAnomaly": true,
  "modelVersion": "xgboost-v1-isolation-v1"
}
```

| Field | Type | Description |
|---|---|---|
| `xgboostProbability` | `float` | Fraud probability from XGBoost [0.0–1.0] |
| `xgboostPrediction` | `int` | XGBoost binary prediction: `1` = fraud, `0` = legitimate |
| `isolationForestScore` | `float` | Anomaly score normalized to [0–100]; higher = more anomalous |
| `isolationForestAnomaly` | `bool` | `true` if Isolation Forest classifies as anomaly |
| `modelVersion` | `string` | Identifies the currently loaded model versions |

### Isolation Forest Score Normalization

```
normalized_score = 50 − (raw_decision_function × 50)
clamped to [0.0, 100.0]
```

The raw `decision_function` output from sklearn's IsolationForest is:
- **Positive** → more normal
- **Negative** → more anomalous

After normalization: **0 = very normal, 100 = very anomalous**.

### Error Responses

| Status | Scenario |
|---|---|
| `503 Service Unavailable` | Models not loaded (startup failure) |
| `422 Unprocessable Entity` | Invalid request body (Pydantic validation) |

### cURL Example

```bash
curl -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" \
  -d '{
    "step": 1,
    "type": "PAYMENT",
    "amount": 75000.00,
    "oldbalanceOrg": 100000.00,
    "newbalanceOrig": 25000.00,
    "oldbalanceDest": 0.00,
    "newbalanceDest": 75000.00,
    "isFlaggedFraud": 0
  }'
```

---

## Model Loading

On application startup (`@app.on_event("startup")`), `ModelService.load_models()` loads two joblib bundles from the paths configured in `app/config.py`:

- **XGBoost bundle** — contains a preprocessor pipeline + trained XGBoost model
- **Isolation Forest bundle** — contains a preprocessor pipeline + trained IsolationForest model

If loading fails, subsequent calls to `/predict` return `503 Service Unavailable`.

---

## Interactive API Docs

FastAPI auto-generates interactive documentation:

- **Swagger UI** — http://localhost:8000/docs
- **ReDoc** — http://localhost:8000/redoc