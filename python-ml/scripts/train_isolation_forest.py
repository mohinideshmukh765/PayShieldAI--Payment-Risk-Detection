from pathlib import Path

import joblib
import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder
from sklearn.ensemble import IsolationForest


DATA_PATH = Path(
    "data/processed/paysim_features.csv"
)

MODEL_PATH = Path(
    "artifacts/isolation_forest_model.joblib"
)


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


def main():

    df = pd.read_csv(DATA_PATH)

    split_step = df["step"].quantile(0.80)

    train_df = df[
        df["step"] <= split_step
    ]

    # Train anomaly detector on legitimate transactions
    normal_df = train_df[
        train_df["isFraud"] == 0
    ]

    # Keep training practical on a local machine
    normal_df = normal_df.sample(
        n=min(300_000, len(normal_df)),
        random_state=42
    )

    X_train = normal_df[FEATURES]

    categorical_features = ["type"]

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

    X_processed = preprocessor.fit_transform(
        X_train
    )

    model = IsolationForest(
        n_estimators=200,
        contamination="auto",
        random_state=42,
        n_jobs=-1
    )

    model.fit(X_processed)

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

    print(
        f"Isolation Forest saved to {MODEL_PATH}"
    )


if __name__ == "__main__":
    main()