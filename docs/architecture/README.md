# PayShield AI Architecture

## Architecture Decision

PayShield AI uses a modular application architecture rather than
microservices.

The primary objective is to demonstrate:

- Spring Boot backend engineering
- Payment transaction processing
- Machine learning
- MLflow / MLOps
- PostgreSQL
- AWS
- Docker
- Secure API development

Spring Boot owns the core business domain and transaction lifecycle.

The Python service is isolated specifically because the machine learning
runtime and ecosystem are Python-based.

---

## Core Components

### Spring Boot

Responsible for:

- Authentication
- Transaction management
- Fraud analysis orchestration
- Risk scoring
- Analytics
- Audit logging

### Python ML Service

Responsible for:

- Feature preprocessing
- XGBoost inference
- Isolation Forest inference
- Model loading
- ML prediction APIs

### PostgreSQL

Stores:

- Users
- Transactions
- Fraud predictions
- Risk results
- Audit logs

### MLflow

Tracks:

- Experiments
- Parameters
- Metrics
- Models
- Model versions

### AWS S3

Stores:

- Dataset artifacts
- Model artifacts
- Reports

### AWS RDS

Hosts the production PostgreSQL database.