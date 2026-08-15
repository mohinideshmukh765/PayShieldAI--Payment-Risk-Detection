from pathlib import Path

import pandas as pd


RAW_DATA = Path(
    "data/raw/paysim_dataset.csv"
)

PROCESSED_DATA = Path(
    "data/processed/paysim_features.csv"
)


def create_features(df: pd.DataFrame) -> pd.DataFrame:

    df = df.copy()

    # Time features
    df["hour"] = df["step"] % 24
    df["day"] = df["step"] // 24

    # Origin balance consistency
    df["origin_balance_error"] = (
        df["oldbalanceOrg"]
        - df["amount"]
        - df["newbalanceOrig"]
    )

    # Destination balance consistency
    df["destination_balance_error"] = (
        df["oldbalanceDest"]
        + df["amount"]
        - df["newbalanceDest"]
    )

    # Amount relative to origin balance
    df["amount_to_origin_balance"] = (
        df["amount"]
        / (df["oldbalanceOrg"] + 1)
    )

    # Amount relative to destination balance
    df["amount_to_destination_balance"] = (
        df["amount"]
        / (df["oldbalanceDest"] + 1)
    )

    return df


def main():

    print("Loading PaySim dataset...")

    df = pd.read_csv(RAW_DATA)

    print(f"Rows loaded: {len(df):,}")

    df = create_features(df)

    PROCESSED_DATA.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    df.to_csv(
        PROCESSED_DATA,
        index=False
    )

    print(
        f"Processed dataset saved to: {PROCESSED_DATA}"
    )


if __name__ == "__main__":
    main()