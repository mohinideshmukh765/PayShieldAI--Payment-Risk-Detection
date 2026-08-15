# PaySim ML Pipeline

## Dataset

PaySim is a synthetic mobile-money transaction dataset
designed for financial fraud detection.

## Target

`isFraud`

- `0` = legitimate
- `1` = fraudulent

## Preprocessing

The pipeline:

1. Loads the raw PaySim CSV.
2. Generates time-based features.
3. Generates balance consistency features.
4. Generates transaction-to-balance ratios.
5. Removes identifier columns.
6. Splits data chronologically.

## Feature Engineering

### Time

- hour
- day

### Balance consistency

- origin_balance_error
- destination_balance_error

### Transaction behavior

- amount_to_origin_balance
- amount_to_destination_balance

## Models

### XGBoost

Supervised binary classification.

The model produces a fraud probability.

Class imbalance is handled using
`scale_pos_weight`.

### Isolation Forest

Unsupervised anomaly detection.

The model is trained primarily on legitimate
transactions and identifies unusual behavior.

## Evaluation

The following metrics are used:

- Precision
- Recall
- F1
- ROC-AUC
- PR-AUC
- Confusion Matrix

Accuracy is not treated as the primary metric because
fraud transactions are highly imbalanced.

## Model Artifacts

Models are serialized using Joblib.

They are stored locally during development.

Production artifact storage will be introduced
with MLflow and AWS S3 in later phases.