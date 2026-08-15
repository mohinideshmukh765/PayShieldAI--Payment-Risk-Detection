from pathlib import Path

import joblib
import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder
from sklearn.ensemble import IsolationForest

import mlflow
import mlflow.sklearn

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


    # --------------------------------------------------------
    # 3. Keep Only Legitimate Transactions
    # --------------------------------------------------------
    #
    # Isolation Forest is being used as an unsupervised
    # anomaly detector, so we train it primarily on
    # legitimate transactions.
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
    #
    # Keeps local training practical while retaining a
    # representative sample.
    #

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
    # 5. Select Features
    # --------------------------------------------------------

    X_train = normal_df[FEATURES]


    # --------------------------------------------------------
    # 6. Feature Preprocessing
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
    # 7. Transform Features
    # --------------------------------------------------------

    print(
        "Preprocessing features..."
    )

    X_processed = (
        preprocessor.fit_transform(
            X_train
        )
    )


    # --------------------------------------------------------
    # 8. Create Isolation Forest
    # --------------------------------------------------------

    model = IsolationForest(
        n_estimators=200,
        contamination="auto",
        random_state=42,
        n_jobs=-1
    )


    # --------------------------------------------------------
    # 9. Configure MLflow
    # --------------------------------------------------------

    configure_mlflow()


    # --------------------------------------------------------
    # 10. Start MLflow Run
    # --------------------------------------------------------

    with mlflow.start_run(
        run_name="isolation-forest-paysim"
    ):

        mlflow.set_tags({
            "model_type": "Isolation Forest",
            "dataset": "PaySim",
            "task": "anomaly_detection"
        })


        # ----------------------------------------------------
        # Log Parameters
        # ----------------------------------------------------

        mlflow.log_params({
            "n_estimators": 200,
            "contamination": "auto",
            "random_state": 42,
            "training_samples": len(X_train)
        })


        # ----------------------------------------------------
        # 11. Train Model
        # ----------------------------------------------------

        print(
            "\nTraining Isolation Forest..."
        )

        model.fit(
            X_processed
        )


        # ----------------------------------------------------
        # 12. Save Model Artifact
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
        # 13. Log Model to MLflow
        # ----------------------------------------------------

        mlflow.sklearn.log_model(
            model,
            name="isolation_forest_model"
        )


        # ----------------------------------------------------
        # 14. Log Local Artifact
        # ----------------------------------------------------

        mlflow.log_artifact(
            str(MODEL_PATH)
        )


        # ----------------------------------------------------
        # 15. Print Result
        # ----------------------------------------------------

        print(
            "\nIsolation Forest training completed."
        )

        print(
            f"Model saved to: {MODEL_PATH}"
        )


# ============================================================
# Entry Point
# ============================================================

if __name__ == "__main__":
    main()