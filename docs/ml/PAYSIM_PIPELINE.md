# PayShield AI — PaySim Data Pipeline

## Dataset

**PaySim** is a synthetic mobile-money transaction dataset designed for financial fraud detection research. It simulates real transaction behavior and includes labeled fraud cases, making it ideal for training supervised models.

The dataset is placed in `python-ml/data/` (not committed to git due to size).

---

## Target Variable

| Value | Label |
|---|---|
| `0` | Legitimate transaction |
| `1` | Fraudulent transaction |

Fraud transactions are **highly imbalanced** — fraud represents a very small fraction of all transactions. This is addressed explicitly in model training.

---

## Data Preparation — `scripts/prepare_data.py`

The `prepare_data.py` script:

1. Loads the raw PaySim CSV from `data/`
2. Generates all engineered features (see below)
3. Removes identifier columns (`nameOrig`, `nameDest`) — not used for modeling
4. Splits data **chronologically** (not randomly) to simulate real deployment — train on past, test on future

---

## Feature Engineering

All features are computed in both `scripts/prepare_data.py` (training) and `app/model_service.py` (inference), ensuring consistency.

### Time Features

| Feature | Formula | Rationale |
|---|---|---|
| `hour` | `step % 24` | Fraud patterns vary by time of day |
| `day` | `step // 24` | Day-of-simulation patterns |

### Balance Consistency Features

| Feature | Formula | Rationale |
|---|---|---|
| `origin_balance_error` | `oldbalanceOrg − amount − newbalanceOrig` | Discrepancy reveals accounting anomalies common in fraud |
| `destination_balance_error` | `oldbalanceDest + amount − newbalanceDest` | Destination-side accounting anomaly |

### Transaction Behavior Features

| Feature | Formula | Rationale |
|---|---|---|
| `amount_to_origin_balance` | `amount / (oldbalanceOrg + 1)` | Proportion of sender's balance being transferred |
| `amount_to_destination_balance` | `amount / (oldbalanceDest + 1)` | Proportion relative to destination balance |

---

## Full Feature Set (14 features)

| Feature | Type |
|---|---|
| `step` | Numeric (raw) |
| `type` | Categorical (raw) — one-hot encoded in preprocessor |
| `amount` | Numeric (raw) |
| `oldbalanceOrg` | Numeric (raw) |
| `newbalanceOrig` | Numeric (raw) |
| `oldbalanceDest` | Numeric (raw) |
| `newbalanceDest` | Numeric (raw) |
| `isFlaggedFraud` | Binary (raw) |
| `hour` | Numeric (derived) |
| `day` | Numeric (derived) |
| `origin_balance_error` | Numeric (derived) |
| `destination_balance_error` | Numeric (derived) |
| `amount_to_origin_balance` | Numeric (derived) |
| `amount_to_destination_balance` | Numeric (derived) |

---

## Training — XGBoost (`scripts/train_xgboost.py`)

- **Model:** `XGBClassifier`
- **Class imbalance handling:** `scale_pos_weight` parameter — set to `(n_negatives / n_positives)` ratio
- **Preprocessor:** `ColumnTransformer` — `OneHotEncoder` for `type`, `StandardScaler` for numerics
- **Output artifact:** `artifacts/xgboost_bundle.pkl` — `{"preprocessor": ..., "model": ...}`
- **MLflow logging:** Parameters, metrics, and the bundle are logged to the MLflow tracking server

---

## Training — Isolation Forest (`scripts/train_isolation_forest.py`)

- **Model:** `IsolationForest` (sklearn)
- **Training data:** Primarily legitimate transactions — anomalies are detected relative to the normal distribution
- **Contamination:** Configured as the known fraud proportion in training data
- **Preprocessor:** Same `ColumnTransformer` structure as XGBoost
- **Output artifact:** `artifacts/isolation_forest_bundle.pkl` — `{"preprocessor": ..., "model": ...}`
- **MLflow logging:** Contamination parameter, anomaly rate on test set, and the bundle

---

## Evaluation — `scripts/evaluate_models.py`

Both models are evaluated after training. Accuracy is explicitly **not** the primary metric due to class imbalance.

### Metrics Used

| Metric | Why |
|---|---|
| **Precision** | % of flagged transactions that are actually fraud |
| **Recall** | % of actual fraud transactions caught |
| **F1 Score** | Harmonic mean of precision and recall |
| **ROC-AUC** | Overall discriminative ability |
| **PR-AUC** | Performance under class imbalance |
| **Confusion Matrix** | True/false positive/negative breakdown |

### Isolation Forest Specific — `scripts/evaluate_isolation_forest.py`

Evaluates how well the unsupervised model aligns with known fraud labels, even though it was not trained on them.

---

## Model Artifacts

Models are serialized using **joblib** and stored in `python-ml/artifacts/`. They are loaded at FastAPI startup by `ModelService.load_models()`.

The artifact paths are configured in `app/config.py`:

```python
XGBOOST_MODEL_PATH = "artifacts/xgboost_bundle.pkl"
ISOLATION_FOREST_MODEL_PATH = "artifacts/isolation_forest_bundle.pkl"
```