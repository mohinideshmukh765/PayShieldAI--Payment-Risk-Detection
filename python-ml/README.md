# PayShield AI — Python ML Inference Service

FastAPI-based machine learning service that scores payment transactions for fraud using **XGBoost** (supervised) and **Isolation Forest** (unsupervised). Tracked and versioned with **MLflow**.

---

## Table of Contents

- [Overview](#overview)
- [Models](#models)
- [Feature Engineering](#feature-engineering)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup](#setup)
- [Training the Models](#training-the-models)
- [Running the Service](#running-the-service)
- [API Endpoints](#api-endpoints)
- [Prediction Response](#prediction-response)
- [MLflow Tracking](#mlflow-tracking)

---

## Overview

The ML service exposes two endpoints:

| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | Liveness check |
| `POST` | `/predict` | Score a payment transaction |

The Spring Boot backend calls `POST /predict` via **Spring Cloud OpenFeign** on every payment request. The ML service runs separately on **port 8000**.

---

## Models

### XGBoost — Supervised Classifier

- **Task** — Binary fraud classification
- **Input** — PaySim transaction features (engineered from raw fields)
- **Output** — `xgboostProbability` (0.0–1.0), `xgboostPrediction` (0 or 1)
- **Threshold** — probability ≥ 0.5 → prediction = 1 (fraud)
- **Weight in composite score** — **60%**

### Isolation Forest — Unsupervised Anomaly Detector

- **Task** — Detect anomalous transactions without labels
- **Input** — Same engineered feature set
- **Output** — `isolationForestScore` (0–100, higher = more anomalous), `isolationForestAnomaly` (bool)
- **Score normalization** — `score = 50 − (raw_decision_function × 50)`, clamped to [0, 100]
- **Weight in composite score** — **15%**

---

## Feature Engineering

The `ModelService.build_features()` method computes 4 derived features from the raw PaySim input fields:

| Feature | Formula |
|---|---|
| `hour` | `step % 24` |
| `day` | `step // 24` |
| `origin_balance_error` | `oldbalanceOrg − amount − newbalanceOrig` |
| `destination_balance_error` | `oldbalanceDest + amount − newbalanceDest` |
| `amount_to_origin_balance` | `amount / (oldbalanceOrg + 1)` |
| `amount_to_destination_balance` | `amount / (oldbalanceDest + 1)` |

These are combined with the raw fields (`step`, `type`, `amount`, `oldbalanceOrg`, `newbalanceOrig`, `oldbalanceDest`, `newbalanceDest`, `isFlaggedFraud`) for a total of **14 features**.

---

## Project Structure

```
python-ml/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI app — /health and /predict routes
│   ├── schemas.py           # Pydantic models: FraudPredictionRequest, FraudPredictionResponse
│   ├── model_service.py     # Model loading, feature engineering, XGBoost + IF inference
│   ├── config.py            # XGBOOST_MODEL_PATH, ISOLATION_FOREST_MODEL_PATH
│   ├── mlflow_config.py     # MLflow tracking URI
│   └── models/
│       ├── xgboost_model.py        # XGBoost model wrapper
│       └── isolation_forest_model.py  # Isolation Forest model wrapper
│
├── scripts/
│   ├── prepare_data.py            # Load & preprocess PaySim CSV
│   ├── train_xgboost.py           # Train, evaluate, save XGBoost bundle
│   ├── train_isolation_forest.py  # Train, evaluate, save Isolation Forest bundle
│   ├── evaluate_models.py         # Full evaluation report (both models)
│   └── evaluate_isolation_forest.py  # Isolation Forest-specific evaluation
│
├── data/                    # PaySim dataset CSV (not committed to git)
├── artifacts/               # Saved joblib model bundles (output of training)
├── mlartifacts/             # MLflow artifact store
├── reports/                 # Evaluation report outputs
├── tests/                   # Test suite
└── requirements.txt
```

---

## Prerequisites

| Tool | Version |
|---|---|
| Python | 3.10+ |
| pip | latest |

---

## Setup

```bash
cd python-ml

# Create virtual environment
python -m venv .venv

# Activate (Windows)
.venv\Scripts\activate

# Activate (macOS/Linux)
source .venv/bin/activate

# Install dependencies
pip install fastapi uvicorn pydantic numpy pandas scikit-learn xgboost joblib matplotlib seaborn mlflow
```

---

## Training the Models

Place the PaySim dataset CSV inside `data/` before running training scripts.

```bash
# 1. Prepare & split data
python -m scripts.prepare_data

# 2. Train XGBoost (saves bundle to artifacts/)
python -m scripts.train_xgboost

# 3. Train Isolation Forest (saves bundle to artifacts/)
python -m scripts.train_isolation_forest

# 4. Evaluate both models
python -m scripts.evaluate_models
```

Each training script logs parameters, metrics, and the model artifact to **MLflow**.

---

## Running the Service

```bash
# Development (auto-reload)
uvicorn app.main:app --reload --port 8000

# Production
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

On startup, `ModelService.load_models()` loads both joblib bundles from `artifacts/`.

---

## API Endpoints

### `GET /health`

**No auth required.**

```json
{
  "status": "UP",
  "service": "payshield-ml"
}
```

---

### `POST /predict`

**Called by Spring Boot backend via Feign client.**

**Request Body:**

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

| Field | Type | Description |
|---|---|---|
| `step` | `int` | Hour step in PaySim simulation (maps to time of day/day) |
| `type` | `string` | Transaction type (e.g., `PAYMENT`, `TRANSFER`) |
| `amount` | `decimal` | Transaction amount |
| `oldbalanceOrg` | `decimal` | Sender balance before transaction |
| `newbalanceOrig` | `decimal` | Sender balance after transaction |
| `oldbalanceDest` | `decimal` | Receiver balance before transaction |
| `newbalanceDest` | `decimal` | Receiver balance after transaction |
| `isFlaggedFraud` | `int` | System flag: 1 if pre-flagged, 0 otherwise |

---

## Prediction Response

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
| `xgboostProbability` | `float` | Fraud probability [0.0–1.0] |
| `xgboostPrediction` | `int` | 1 = fraud, 0 = legitimate |
| `isolationForestScore` | `float` | Anomaly score [0–100], higher = more anomalous |
| `isolationForestAnomaly` | `bool` | `true` if classified as anomaly |
| `modelVersion` | `string` | Identifies the loaded model versions |

---

## MLflow Tracking

MLflow tracks all training experiments. The SQLite tracking DB is at `mlflow.db`.

```bash
# Launch MLflow UI
mlflow ui --backend-store-uri sqlite:///mlflow.db
```

Open **http://localhost:5000** to browse runs, compare metrics, and inspect artifacts.

Each training run logs:
- **Parameters** — model hyperparameters, feature config
- **Metrics** — precision, recall, F1, ROC-AUC (XGBoost); contamination, anomaly rate (Isolation Forest)
- **Artifacts** — saved joblib model bundles
