from pathlib import Path
import sys

import joblib
import matplotlib.pyplot as plt
import pandas as pd
import mlflow

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


# ============================================================
# Make project root available for imports
# ============================================================

PROJECT_ROOT = Path(__file__).resolve().parents[1]

if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from app.mlflow_config import configure_mlflow


# ============================================================
# Paths
# ============================================================

DATA_PATH = (
    PROJECT_ROOT
    / "data"
    / "processed"
    / "paysim_features.csv"
)

MODEL_PATH = (
    PROJECT_ROOT
    / "artifacts"
    / "xgboost_model.joblib"
)

REPORT_DIR = (
    PROJECT_ROOT
    / "reports"
    / "metrics"
)

FIGURE_DIR = (
    PROJECT_ROOT
    / "reports"
    / "figures"
)


# ============================================================
# Evaluation
# ============================================================

def main():

    # --------------------------------------------------------
    # 1. Load Dataset
    # --------------------------------------------------------

    print("Loading processed dataset...")

    df = pd.read_csv(DATA_PATH)

    print(
        f"Dataset shape: {df.shape}"
    )


    # --------------------------------------------------------
    # 2. Time-Based Test Split
    # --------------------------------------------------------

    split_step = df["step"].quantile(0.80)

    test_df = df[
        df["step"] > split_step
    ].copy()

    print(
        f"Test rows: {len(test_df):,}"
    )


    # --------------------------------------------------------
    # 3. Load Trained Model
    # --------------------------------------------------------

    print(
        f"Loading model from: {MODEL_PATH}"
    )

    bundle = joblib.load(
        MODEL_PATH
    )

    model = bundle["model"]
    preprocessor = bundle["preprocessor"]
    features = bundle["features"]


    # --------------------------------------------------------
    # 4. Prepare Test Data
    # --------------------------------------------------------

    X_test = test_df[features]

    y_test = test_df["isFraud"]


    # --------------------------------------------------------
    # 5. Apply Saved Preprocessor
    # --------------------------------------------------------

    X_processed = preprocessor.transform(
        X_test
    )


    # --------------------------------------------------------
    # 6. Generate Predictions
    # --------------------------------------------------------

    probabilities = model.predict_proba(
        X_processed
    )[:, 1]

    predictions = (
        probabilities >= 0.5
    ).astype(int)


    # --------------------------------------------------------
    # 7. Calculate Metrics
    # --------------------------------------------------------

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


    # --------------------------------------------------------
    # 8. Print Results
    # --------------------------------------------------------

    print(
        "\n========== XGBoost Evaluation =========="
    )

    print(
        f"Precision : {precision:.4f}"
    )

    print(
        f"Recall    : {recall:.4f}"
    )

    print(
        f"F1        : {f1:.4f}"
    )

    print(
        f"ROC-AUC   : {roc_auc:.4f}"
    )

    print(
        f"PR-AUC    : {pr_auc:.4f}"
    )

    print(
        "=========================================\n"
    )


    # --------------------------------------------------------
    # 9. Classification Report
    # --------------------------------------------------------

    print(
        "Classification Report:\n"
    )

    print(
        classification_report(
            y_test,
            predictions,
            zero_division=0
        )
    )


    # --------------------------------------------------------
    # 10. Create Output Directories
    # --------------------------------------------------------

    REPORT_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    FIGURE_DIR.mkdir(
        parents=True,
        exist_ok=True
    )


    # --------------------------------------------------------
    # 11. Save Metrics
    # --------------------------------------------------------

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

    metrics_path = (
        REPORT_DIR
        / "xgboost_metrics.csv"
    )

    metrics.to_csv(
        metrics_path,
        index=False
    )

    print(
        f"Metrics saved to: {metrics_path}"
    )


    # --------------------------------------------------------
    # 12. Confusion Matrix
    # --------------------------------------------------------

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


    confusion_matrix_path = (
        FIGURE_DIR
        / "xgboost_confusion_matrix.png"
    )

    plt.savefig(
        confusion_matrix_path,
        dpi=150
    )

    plt.close()

    print(
        f"Confusion matrix saved to: "
        f"{confusion_matrix_path}"
    )


    # --------------------------------------------------------
    # 13. Configure MLflow
    # --------------------------------------------------------

    configure_mlflow()


    # --------------------------------------------------------
    # 14. Log Evaluation to MLflow
    # --------------------------------------------------------

    with mlflow.start_run(
        run_name="xgboost-evaluation"
    ):

        mlflow.set_tags({
            "model_type": "XGBoost",
            "dataset": "PaySim",
            "task": "fraud_detection",
            "evaluation_type": "holdout_test"
        })

        mlflow.log_metrics({
            "precision": precision,
            "recall": recall,
            "f1": f1,
            "roc_auc": roc_auc,
            "pr_auc": pr_auc
        })

        mlflow.log_metric(
            "testing_rows",
            len(X_test)
        )

        mlflow.log_metric(
            "fraud_test_samples",
            int(y_test.sum())
        )

        mlflow.log_artifact(
            str(metrics_path)
        )

        mlflow.log_artifact(
            str(confusion_matrix_path)
        )


        print(
            "\nEvaluation results logged to MLflow."
        )


# ============================================================
# Entry Point
# ============================================================

if __name__ == "__main__":
    main()