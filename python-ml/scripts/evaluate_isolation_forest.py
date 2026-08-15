from pathlib import Path

import joblib
import pandas as pd

from sklearn.metrics import (
    precision_score,
    recall_score,
    f1_score,
    average_precision_score,
    roc_auc_score
)


DATA_PATH = Path(
    "data/processed/paysim_features.csv"
)

MODEL_PATH = Path(
    "artifacts/isolation_forest_model.joblib"
)


def main():

    df = pd.read_csv(DATA_PATH)

    split_step = df["step"].quantile(0.80)

    test_df = df[
        df["step"] > split_step
    ]

    bundle = joblib.load(MODEL_PATH)

    model = bundle["model"]
    preprocessor = bundle["preprocessor"]
    features = bundle["features"]

    X_test = test_df[features]
    y_test = test_df["isFraud"]

    X_processed = preprocessor.transform(
        X_test
    )

    predictions = model.predict(
        X_processed
    )

    # Isolation Forest:
    # -1 = anomaly
    #  1 = normal

    anomaly_predictions = (
        predictions == -1
    ).astype(int)

    scores = -model.decision_function(
        X_processed
    )

    precision = precision_score(
        y_test,
        anomaly_predictions,
        zero_division=0
    )

    recall = recall_score(
        y_test,
        anomaly_predictions,
        zero_division=0
    )

    f1 = f1_score(
        y_test,
        anomaly_predictions,
        zero_division=0
    )

    roc_auc = roc_auc_score(
        y_test,
        scores
    )

    pr_auc = average_precision_score(
        y_test,
        scores
    )

    print("\n===== Isolation Forest Evaluation =====")

    print(f"Precision : {precision:.4f}")
    print(f"Recall    : {recall:.4f}")
    print(f"F1        : {f1:.4f}")
    print(f"ROC-AUC   : {roc_auc:.4f}")
    print(f"PR-AUC    : {pr_auc:.4f}")


if __name__ == "__main__":
    main()