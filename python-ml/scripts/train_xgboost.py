from pathlib import Path

import joblib
import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder
from xgboost import XGBClassifier

import mlflow
import mlflow.xgboost

from sklearn.metrics import (
    precision_score,
    recall_score,
    f1_score,
    roc_auc_score,
    average_precision_score
)

from app.mlflow_config import configure_mlflow


# ============================================================
# Paths
# ============================================================

DATA_PATH = Path(
    "data/processed/paysim_features.csv"
)

MODEL_PATH = Path(
    "artifacts/xgboost_model.joblib"
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

TARGET = "isFraud"


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
    # 2. Time-Based Train/Test Split
    # --------------------------------------------------------

    split_step = df["step"].quantile(0.80)

    train_df = df[
        df["step"] <= split_step
    ]

    test_df = df[
        df["step"] > split_step
    ]

    X_train = train_df[FEATURES]
    y_train = train_df[TARGET]

    X_test = test_df[FEATURES]
    y_test = test_df[TARGET]

    print(
        f"Training rows: {len(X_train):,}"
    )

    print(
        f"Testing rows: {len(X_test):,}"
    )


    # --------------------------------------------------------
    # 3. Feature Preprocessing
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
    # 4. Transform Features
    # --------------------------------------------------------

    X_train_processed = (
        preprocessor.fit_transform(X_train)
    )

    X_test_processed = (
        preprocessor.transform(X_test)
    )


    # --------------------------------------------------------
    # 5. Handle Class Imbalance
    # --------------------------------------------------------

    fraud_count = y_train.sum()

    normal_count = (
        len(y_train) - fraud_count
    )

    if fraud_count == 0:
        raise RuntimeError(
            "Training dataset contains no fraud samples."
        )

    scale_pos_weight = (
        normal_count / fraud_count
    )

    print(
        f"Fraud samples: {fraud_count:,}"
    )

    print(
        f"Normal samples: {normal_count:,}"
    )

    print(
        f"scale_pos_weight: "
        f"{scale_pos_weight:.2f}"
    )


    # --------------------------------------------------------
    # 6. Create XGBoost Model
    # --------------------------------------------------------

    model = XGBClassifier(
        n_estimators=300,
        max_depth=6,
        learning_rate=0.08,
        subsample=0.8,
        colsample_bytree=0.8,
        objective="binary:logistic",
        eval_metric="aucpr",
        scale_pos_weight=scale_pos_weight,
        random_state=42,
        n_jobs=-1
    )


    # --------------------------------------------------------
    # 7. Configure MLflow
    # --------------------------------------------------------

    configure_mlflow()


    # --------------------------------------------------------
    # 8. Start MLflow Run
    # --------------------------------------------------------

    with mlflow.start_run(
        run_name="xgboost-paysim"
    ):

        mlflow.set_tags({
            "model_type": "XGBoost",
            "dataset": "PaySim",
            "task": "fraud_detection"
        })


        # ----------------------------------------------------
        # Log Hyperparameters
        # ----------------------------------------------------

        mlflow.log_params({
            "n_estimators": 300,
            "max_depth": 6,
            "learning_rate": 0.08,
            "subsample": 0.8,
            "colsample_bytree": 0.8,
            "scale_pos_weight": scale_pos_weight,
            "random_state": 42
        })


        # ----------------------------------------------------
        # 9. Train Model
        # ----------------------------------------------------

        print(
            "\nTraining XGBoost model..."
        )

        model.fit(
            X_train_processed,
            y_train
        )


        # ----------------------------------------------------
        # 10. Predictions
        # ----------------------------------------------------

        probabilities = (
            model.predict_proba(
                X_test_processed
            )[:, 1]
        )

        predictions = (
            probabilities >= 0.5
        ).astype(int)


        # ----------------------------------------------------
        # 11. Evaluation Metrics
        # ----------------------------------------------------

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


        # ----------------------------------------------------
        # 12. Log Metrics
        # ----------------------------------------------------

        mlflow.log_metrics({
            "precision": precision,
            "recall": recall,
            "f1": f1,
            "roc_auc": roc_auc,
            "pr_auc": pr_auc
        })

        mlflow.log_metric(
            "training_rows",
            len(X_train)
        )

        mlflow.log_metric(
            "testing_rows",
            len(X_test)
        )


        # ----------------------------------------------------
        # 13. Save Model with MLflow
        # ----------------------------------------------------

        mlflow.xgboost.log_model(
            model,
            name="xgboost_model"
        )


        # ----------------------------------------------------
        # 14. Save Local Model Artifact
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
        # 15. Log Local Artifact to MLflow
        # ----------------------------------------------------

        mlflow.log_artifact(
            str(MODEL_PATH)
        )


        # ----------------------------------------------------
        # 16. Print Results
        # ----------------------------------------------------

        print(
            "\n========== MODEL RESULTS =========="
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
            "===================================\n"
        )

        print(
            f"Model saved to: {MODEL_PATH}"
        )


# ============================================================
# Entry Point
# ============================================================

if __name__ == "__main__":
    main()