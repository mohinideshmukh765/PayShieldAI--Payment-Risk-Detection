# PayShield AI — Documentation Index

Complete reference for the PayShield AI fraud detection platform.

---

## API Reference

| Document | Endpoints | Auth |
|---|---|---|
| [AUTHENTICATION_API.md](./api/AUTHENTICATION_API.md) | `POST /register`, `POST /login` | Public |
| [PAYMENT_API.md](./api/PAYMENT_API.md) | `POST /payments`, `GET /payments/{id}`, `GET /payments` | USER |
| [TRANSACTION_API.md](./api/TRANSACTION_API.md) | `POST /transactions`, `GET /transactions`, `GET /transactions/{id}`, `PUT /transactions/{id}/status` | USER / ANALYST |
| [WALLET_API.md](./api/WALLET_API.md) | `GET /wallet`, `GET /wallet/transactions`, `POST /wallet/topup` | USER |
| [FRAUD_RULE_API.md](./api/FRAUD_RULE_API.md) | `POST /fraud/rules/evaluate` | ANALYST only |
| [ML_API.md](./api/ML_API.md) | `GET /health`, `POST /predict` | Internal |
| [ANALYTICS_API.md](./api/ANALYTICS_API.md) | Analytics endpoints | ANALYST only |

---

## Architecture Diagrams

| Document | Description |
|---|---|
| [system-architecture.md](./diagrams/system-architecture.md) | Full end-to-end system diagram (React → Spring Boot → ML → PostgreSQL) |
| [payment-lifecycle.md](./diagrams/payment-lifecycle.md) | Payment creation sequence with fraud assessment |
| [fraud-rule-engine.md](./diagrams/fraud-rule-engine.md) | Hybrid fraud engine decision flowchart |
| [database-erd.md](./diagrams/database-erd.md) | Complete entity relationship diagram (10 tables) |
| [analyst-approval-flow.md](./diagrams/analyst-approval-flow.md) | Analyst review, approve, and reject sequence |
| [authentication-sequence.md](./diagrams/authentication-sequence.md) | JWT registration & login flow |
| [JWT-request.md](./diagrams/JWT-request.md) | JWT request lifecycle |

---

## ML Documentation

| Document | Description |
|---|---|
| [ML_ARCHITECTURE.MD](./ml/ML_ARCHITECTURE.MD) | XGBoost + Isolation Forest model architecture |
| [PAYSIM_PIPELINE.md](./ml/PAYSIM_PIPELINE.md) | PaySim dataset preprocessing pipeline |
| [MLFLOW.md](./ml/MLFLOW.md) | MLflow experiment tracking setup |

---

## Architecture Docs

| Document | Description |
|---|---|
| [architecture/README.md](./architecture/README.md) | Backend component overview |
| [architecture/backend-architecture.md](./architecture/backend-architecture.md) | Layered architecture detail |
