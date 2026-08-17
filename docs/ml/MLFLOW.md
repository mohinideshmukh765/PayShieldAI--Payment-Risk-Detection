# PayShield AI — MLflow Experiment Tracking

MLflow is used to track all machine learning training experiments — logging parameters, metrics, and model artifacts for every training run.

---

## Configuration

MLflow tracking is configured in `app/mlflow_config.py`:

```python
MLFLOW_TRACKING_URI = "http://127.0.0.1:5000"
EXPERIMENT_NAME = "PayShield-Fraud-Detection"

def configure_mlflow():
    mlflow.set_tracking_uri(MLFLOW_TRACKING_URI)
    mlflow.set_experiment(EXPERIMENT_NAME)
```

- **Tracking URI:** Local MLflow server at `http://127.0.0.1:5000`
- **Experiment name:** `PayShield-Fraud-Detection`
- **Backend store:** SQLite database at `python-ml/mlflow.db`
- **Artifact store:** `python-ml/mlartifacts/`

---

## Starting the MLflow UI

```bash
cd python-ml

# Start MLflow tracking server using the local SQLite DB
mlflow ui --backend-store-uri sqlite:///mlflow.db
```

Then open: **http://127.0.0.1:5000**

---

## What Gets Tracked

### XGBoost Training Run (`scripts/train_xgboost.py`)

| Item | Type | Example |
|---|---|---|
| `scale_pos_weight` | Parameter | Class imbalance ratio |
| `n_estimators` | Parameter | Number of trees |
| `max_depth` | Parameter | Tree depth |
| `precision` | Metric | 0.94 |
| `recall` | Metric | 0.87 |
| `f1_score` | Metric | 0.90 |
| `roc_auc` | Metric | 0.98 |
| `pr_auc` | Metric | 0.93 |
| XGBoost bundle | Artifact | `xgboost_model.joblib` |

### Isolation Forest Training Run (`scripts/train_isolation_forest.py`)

| Item | Type | Example |
|---|---|---|
| `contamination` | Parameter | Fraud fraction in training set |
| `n_estimators` | Parameter | Number of trees |
| `anomaly_rate_test` | Metric | Fraction flagged as anomaly on test set |
| Isolation Forest bundle | Artifact | `isolation_forest_model.joblib` |

---

## Model Artifact Paths

Model artifacts are loaded by `ModelService` from:

```python
# app/config.py
ARTIFACTS_DIR = BASE_DIR / "artifacts"
XGBOOST_MODEL_PATH = ARTIFACTS_DIR / "xgboost_model.joblib"
ISOLATION_FOREST_MODEL_PATH = ARTIFACTS_DIR / "isolation_forest_model.joblib"
```

Each artifact is a joblib bundle:

```python
# XGBoost bundle structure
{
    "preprocessor": ColumnTransformer,   # fitted sklearn pipeline
    "model": XGBClassifier               # trained XGBoost model
}

# Isolation Forest bundle structure
{
    "preprocessor": ColumnTransformer,   # fitted sklearn pipeline
    "model": IsolationForest             # trained Isolation Forest model
}
```

---

## Comparing Runs

The MLflow UI allows:
- Comparing metrics across multiple training runs
- Viewing parameter configurations side-by-side
- Downloading artifact bundles from any run
- Viewing logged plots (confusion matrix, ROC curve, PR curve) from `reports/`

---

## Model Versioning in Predictions

Every `POST /predict` response includes a `modelVersion` field:

```json
{
  "modelVersion": "xgboost-v1-isolation-v1"
}
```

This version is also stored in the `fraud_predictions` PostgreSQL table, enabling audit trails that trace which model version produced each prediction.