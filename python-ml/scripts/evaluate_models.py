from pathlib import Path

import joblib
import matplotlib.pyplot as plt
import pandas as pd

from sklearn.metrics import (
    average_precision_score,
    classification_report,
    confusion_matrix,
    ConfusionMatrixDisplay,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score
)


DATA_PATH = Path(
    "data/processed/paysim_features.csv"
)

MODEL_PATH = Path(
    "artifacts/xgboost_model.joblib"
)

REPORT_DIR = Path(
    "reports/metrics"
)

FIGURE_DIR = Path(
    "reports/figures"
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

    probabilities = model.predict_proba(
        X_processed
    )[:, 1]

    predictions = (
        probabilities >= 0.5
    ).astype(int)

    precision = precision_score(
        y_test,
        predictions,
        zero_division=0
    )

    recall = recall_score(
        y_test,
        predictions,
        zero_division=0
    )

    f1 = f1_score(
        y_test,
        predictions,
        zero_division=0
    )

    roc_auc = roc_auc_score(
        y_test,
        probabilities
    )

    pr_auc = average_precision_score(
        y_test,
        probabilities
    )

    print("\n===== XGBoost Evaluation =====")

    print(f"Precision : {precision:.4f}")
    print(f"Recall    : {recall:.4f}")
    print(f"F1        : {f1:.4f}")
    print(f"ROC-AUC   : {roc_auc:.4f}")
    print(f"PR-AUC    : {pr_auc:.4f}")

    print("\nClassification Report")

    print(
        classification_report(
            y_test,
            predictions,
            zero_division=0
        )
    )

    REPORT_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    FIGURE_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    metrics = pd.DataFrame({
        "metric": [
            "precision",
            "recall",
            "f1",
            "roc_auc",
            "pr_auc"
        ],
        "value": [
            precision,
            recall,
            f1,
            roc_auc,
            pr_auc
        ]
    })

    metrics.to_csv(
        REPORT_DIR / "xgboost_metrics.csv",
        index=False
    )

    cm = confusion_matrix(
        y_test,
        predictions
    )

    display = ConfusionMatrixDisplay(
        confusion_matrix=cm
    )

    display.plot()

    plt.title(
        "XGBoost Confusion Matrix"
    )

    plt.tight_layout()

    plt.savefig(
        FIGURE_DIR / "xgboost_confusion_matrix.png"
    )

    plt.close()


if __name__ == "__main__":
    main()