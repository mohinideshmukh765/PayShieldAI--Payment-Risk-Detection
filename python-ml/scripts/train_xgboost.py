from pathlib import Path

import joblib
import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder
from xgboost import XGBClassifier


DATA_PATH = Path(
    "data/processed/paysim_features.csv"
)

MODEL_PATH = Path(
    "artifacts/xgboost_model.joblib"
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

TARGET = "isFraud"


def main():

    print("Loading processed dataset...")

    df = pd.read_csv(DATA_PATH)

    # Time-based split
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

    print(f"Training rows: {len(X_train):,}")
    print(f"Testing rows: {len(X_test):,}")

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

    X_train_processed = preprocessor.fit_transform(
        X_train
    )

    X_test_processed = preprocessor.transform(
        X_test
    )

    fraud_count = y_train.sum()
    normal_count = len(y_train) - fraud_count

    scale_pos_weight = (
        normal_count / fraud_count
    )

    print(
        f"scale_pos_weight: {scale_pos_weight:.2f}"
    )

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

    model.fit(
        X_train_processed,
        y_train
    )

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
        f"Model saved to {MODEL_PATH}"
    )


if __name__ == "__main__":
    main()