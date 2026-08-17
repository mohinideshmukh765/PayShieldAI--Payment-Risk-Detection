from pathlib import Path

import joblib
import pandas as pd
import matplotlib.pyplot as plt
import mlflow
import mlflow.sklearn

from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder
from sklearn.ensemble import IsolationForest
from sklearn.metrics import (
    precision_score,
    recall_score,
    f1_score,
    roc_auc_score,
    average_precision_score,
    confusion_matrix,
    ConfusionMatrixDisplay
)

from app.mlflow_config import configure_mlflow


# ============================================================
# Paths
# ============================================================

DATA_PATH = Path(
    "data/processed/paysim_features.csv"
)

MODEL_PATH = Path(
    "artifacts/isolation_forest_model.joblib"
)

METRICS_PATH = Path(
    "reports/metrics/isolation_forest_metrics.csv"
)

CONFUSION_MATRIX_PATH = Path(
    "reports/figures/isolation_forest_confusion_matrix.png"
)


# ============================================================
# Features
# ============================================================

FEATURES = [
    "step",
    "type",
    "amount",
    "oldbalanceOrg",
    "newbalanceOrig",
    "oldbalanceDest",
    "newbalanceDest",
    "isFlaggedFraud",
    "hour",
    "day",
    "origin_balance_error",
    "destination_balance_error",
    "amount_to_origin_balance",
    "amount_to_destination_balance",
]


# ============================================================
# Main Training Pipeline
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
    # 2. Time-Based Training Split
    # --------------------------------------------------------

    split_step = df["step"].quantile(0.80)

    train_df = df[
        df["step"] <= split_step
    ]

    test_df = df[
        df["step"] > split_step
    ]

    print(
        f"Training rows: {len(train_df):,}"
    )

    print(
        f"Testing rows: {len(test_df):,}"
    )


    # --------------------------------------------------------
    # 3. Keep Only Legitimate Transactions
    # --------------------------------------------------------
    #
    # Isolation Forest is trained on legitimate transactions
    # so that fraudulent/anomalous transactions can be detected
    # during evaluation.
    #

    normal_df = train_df[
        train_df["isFraud"] == 0
    ]

    print(
        f"Legitimate training rows: "
        f"{len(normal_df):,}"
    )


    # --------------------------------------------------------
    # 4. Limit Training Size
    # --------------------------------------------------------

    sample_size = min(
        300_000,
        len(normal_df)
    )

    normal_df = normal_df.sample(
        n=sample_size,
        random_state=42
    )

    print(
        f"Sampled rows: {len(normal_df):,}"
    )


    # --------------------------------------------------------
    # 5. Select Training Features
    # --------------------------------------------------------

    X_train = normal_df[FEATURES]


    # --------------------------------------------------------
    # 6. Select Test Features + Labels
    # --------------------------------------------------------

    X_test = test_df[FEATURES]

    y_test = test_df["isFraud"]


    # --------------------------------------------------------
    # 7. Feature Preprocessing
    # --------------------------------------------------------

    categorical_features = [
        "type"
    ]

    numerical_features = [
        feature
        for feature in FEATURES
        if feature not in categorical_features
    ]

    preprocessor = ColumnTransformer(
        transformers=[
            (
                "categorical",
                OneHotEncoder(
                    handle_unknown="ignore"
                ),
                categorical_features
            ),
            (
                "numerical",
                "passthrough",
                numerical_features
            )
        ]
    )


    # --------------------------------------------------------
    # 8. Transform Training Data
    # --------------------------------------------------------

    print(
        "Preprocessing training features..."
    )

    X_train_processed = (
        preprocessor.fit_transform(
            X_train
        )
    )


    # --------------------------------------------------------
    # 9. Transform Test Data
    # --------------------------------------------------------

    print(
        "Preprocessing test features..."
    )

    X_test_processed = (
        preprocessor.transform(
            X_test
        )
    )


    # --------------------------------------------------------
    # 10. Create Isolation Forest
    # --------------------------------------------------------

    model = IsolationForest(
        n_estimators=200,
        contamination="auto",
        random_state=42,
        n_jobs=-1
    )


    # --------------------------------------------------------
    # 11. Configure MLflow
    # --------------------------------------------------------

    configure_mlflow()


    # --------------------------------------------------------
    # 12. Start MLflow Run
    # --------------------------------------------------------

    with mlflow.start_run(
        run_name="isolation-forest-paysim"
    ):

        mlflow.set_tags({
            "model_type": "Isolation Forest",
            "dataset": "PaySim",
            "task": "anomaly_detection",
            "training_strategy": "legitimate_transactions_only"
        })


        # ----------------------------------------------------
        # 13. Log Parameters
        # ----------------------------------------------------

        mlflow.log_params({
            "n_estimators": 200,
            "contamination": "auto",
            "random_state": 42,
            "training_samples": len(X_train),
            "testing_samples": len(X_test),
            "split_step": split_step
        })


        # ----------------------------------------------------
        # 14. Train Model
        # ----------------------------------------------------

        print(
            "\nTraining Isolation Forest..."
        )

        model.fit(
            X_train_processed
        )


        # ----------------------------------------------------
        # 15. Predict Test Data
        # ----------------------------------------------------
        #
        # Isolation Forest:
        #
        #   1  = normal
        #  -1  = anomaly
        #
        # We convert this to:
        #
        #   0 = legitimate
        #   1 = fraud/anomaly
        #

        raw_predictions = model.predict(
            X_test_processed
        )

        predictions = (
            raw_predictions == -1
        ).astype(int)


        # ----------------------------------------------------
        # 16. Calculate Anomaly Score
        # ----------------------------------------------------
        #
        # decision_function:
        #
        # higher = more normal
        # lower  = more anomalous
        #
        # Negating it gives us a score where higher means
        # more anomalous.
        #

        anomaly_scores = -model.decision_function(
            X_test_processed
        )


        # ----------------------------------------------------
        # 17. Calculate Metrics
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
            anomaly_scores
        )

        pr_auc = average_precision_score(
            y_test,
            anomaly_scores
        )

        anomaly_rate = predictions.mean()


        # ----------------------------------------------------
        # 18. Log Metrics to MLflow
        # ----------------------------------------------------

        mlflow.log_metrics({
            "precision": precision,
            "recall": recall,
            "f1": f1,
            "roc_auc": roc_auc,
            "pr_auc": pr_auc,
            "anomaly_rate": anomaly_rate
        })


        # ----------------------------------------------------
        # 19. Create Reports Directories
        # ----------------------------------------------------

        METRICS_PATH.parent.mkdir(
            parents=True,
            exist_ok=True
        )

        CONFUSION_MATRIX_PATH.parent.mkdir(
            parents=True,
            exist_ok=True
        )


        # ----------------------------------------------------
        # 20. Save Metrics CSV
        # ----------------------------------------------------

        metrics_df = pd.DataFrame({
            "metric": [
                "precision",
                "recall",
                "f1",
                "roc_auc",
                "pr_auc",
                "anomaly_rate"
            ],
            "value": [
                precision,
                recall,
                f1,
                roc_auc,
                pr_auc,
                anomaly_rate
            ]
        })

        metrics_df.to_csv(
            METRICS_PATH,
            index=False
        )


        # ----------------------------------------------------
        # 21. Create Confusion Matrix
        # ----------------------------------------------------

        cm = confusion_matrix(
            y_test,
            predictions
        )

        display = ConfusionMatrixDisplay(
            confusion_matrix=cm,
            display_labels=[
                "Legitimate",
                "Fraud"
            ]
        )

        display.plot()

        plt.title(
            "Isolation Forest Confusion Matrix"
        )

        plt.tight_layout()

        plt.savefig(
            CONFUSION_MATRIX_PATH,
            dpi=150
        )

        plt.close()


        # ----------------------------------------------------
        # 22. Log Reports to MLflow
        # ----------------------------------------------------

        mlflow.log_artifact(
            str(METRICS_PATH)
        )

        mlflow.log_artifact(
            str(CONFUSION_MATRIX_PATH)
        )


        # ----------------------------------------------------
        # 23. Save Model
        # ----------------------------------------------------

        MODEL_PATH.parent.mkdir(
            parents=True,
            exist_ok=True
        )

        joblib.dump(
            {
                "model": model,
                "preprocessor": preprocessor,
                "features": FEATURES
            },
            MODEL_PATH
        )


        # ----------------------------------------------------
        # 24. Log Model to MLflow
        # ----------------------------------------------------

        mlflow.sklearn.log_model(
            model,
            name="isolation_forest_model"
        )


        # ----------------------------------------------------
        # 25. Log Local Model Artifact
        # ----------------------------------------------------

        mlflow.log_artifact(
            str(MODEL_PATH)
        )


        # ----------------------------------------------------
        # 26. Print Results
        # ----------------------------------------------------

        print(
            "\n=========================================="
        )

        print(
            "Isolation Forest Evaluation"
        )

        print(
            "=========================================="
        )

        print(
            f"Precision : {precision:.4f}"
        )

        print(
            f"Recall    : {recall:.4f}"
        )

        print(
            f"F1 Score  : {f1:.4f}"
        )

        print(
            f"ROC-AUC   : {roc_auc:.4f}"
        )

        print(
            f"PR-AUC    : {pr_auc:.4f}"
        )

        print(
            f"Anomaly % : {anomaly_rate:.4%}"
        )

        print(
            "\nReports:"
        )

        print(
            f"Metrics CSV: {METRICS_PATH}"
        )

        print(
            f"Confusion Matrix: "
            f"{CONFUSION_MATRIX_PATH}"
        )

        print(
            f"\nModel saved to: {MODEL_PATH}"
        )

        print(
            "\nIsolation Forest training and "
            "evaluation completed successfully."
        )


# ============================================================
# Entry Point
# ============================================================

if __name__ == "__main__":
    main()