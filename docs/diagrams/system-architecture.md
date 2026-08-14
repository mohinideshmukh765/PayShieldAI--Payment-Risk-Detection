# System Architecture

```mermaid
flowchart TD

    USER[User] --> FRONTEND[React Frontend]

    FRONTEND --> BACKEND[Spring Boot API]

    BACKEND --> AUTH[Spring Security + JWT]

    BACKEND --> DB[(PostgreSQL)]

    BACKEND --> ML[Python ML API]

    ML --> XGB[XGBoost]
    ML --> IF[Isolation Forest]

    XGB --> MLFLOW[MLflow]
    IF --> MLFLOW

    MLFLOW --> S3[AWS S3]

    DB --> RDS[AWS RDS]