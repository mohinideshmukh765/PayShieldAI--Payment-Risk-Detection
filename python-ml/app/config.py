from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent.parent

ARTIFACTS_DIR = BASE_DIR / "artifacts"

XGBOOST_MODEL_PATH = ARTIFACTS_DIR / "xgboost_model.joblib"
ISOLATION_FOREST_MODEL_PATH = ARTIFACTS_DIR / "isolation_forest_model.joblib"