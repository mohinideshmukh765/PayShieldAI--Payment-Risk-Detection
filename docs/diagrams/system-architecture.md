# PayShield AI — End-to-End System Architecture

This diagram shows the full tier-by-tier architecture of the PayShield AI platform: how the React frontend communicates with the Spring Boot backend, how the backend delegates ML inference to the Python FastAPI service, and how all data is persisted in PostgreSQL.

---

## Architecture Diagram

```mermaid
graph TB
    subgraph Client_Layer ["Client Tier (React 18 + Vite)"]
        UI_User["User Dashboard<br/>(Wallet, Payments, Ledger)"]
        UI_Analyst["Analyst Risk Desk<br/>(Inspection, Solvency Check, Sandbox)"]
    end

    subgraph Spring_Boot ["Core Backend Tier (Spring Boot 4.1 + Java 17)"]
        Security["Spring Security + JWT Filter<br/>(RBAC: ROLE_USER, ROLE_ANALYST)"]
        
        subgraph Controllers ["REST API Controllers"]
            AuthCtrl["AuthController"]
            PayCtrl["PaymentController"]
            WallCtrl["WalletController"]
            TxCtrl["TransactionController"]
            RuleCtrl["FraudRuleController"]
        end

        subgraph Business_Services ["Core Domain Services"]
            PaySvc["PaymentService<br/>(Idempotency & Available Balance)"]
            FraudSvc["FraudAssessmentService<br/>(Orchestration)"]
            RiskSvc["RiskAssessmentService<br/>(Composite Scoring)"]
            RuleEng["FraudRuleEngine<br/>(5 Deterministic Rules)"]
            TxSvc["TransactionService<br/>(Queue & Analyst Override)"]
            WallSvc["WalletService<br/>(Ledger & Debit/Credit)"]
        end

        FeignClient["MLPredictionFeignClient<br/>(HTTP / Spring Cloud OpenFeign)"]
    end

    subgraph ML_Tier ["Machine Learning Inference Tier (Python 3.10 + FastAPI)"]
        FastAPI["FastAPI /predict Endpoint"]
        XGBoost["XGBoost Supervised Classifier<br/>(Fraud Probability)"]
        IsoForest["Isolation Forest Unsupervised<br/>(Anomaly Detection)"]
        MLflow["MLflow Model Registry & Tracking"]
    end

    subgraph Storage_Tier ["Database & Persistence Tier (PostgreSQL)"]
        DB_Users[("users & roles")]
        DB_Wallets[("wallets & wallet_transactions")]
        DB_Payments[("payments & idempotency_keys")]
        DB_Tx[("transactions & fraud_rule_results")]
        DB_Pred[("fraud_predictions & audit_logs")]
    end

    %% Client to Security
    UI_User --> Security
    UI_Analyst --> Security
    Security --> Controllers

    %% Controllers to Services
    AuthCtrl --> DB_Users
    PayCtrl --> PaySvc
    WallCtrl --> WallSvc
    TxCtrl --> TxSvc
    RuleCtrl --> RuleEng

    %% Internal Orchestration
    PaySvc --> WallSvc
    PaySvc --> FraudSvc
    FraudSvc --> RuleEng
    FraudSvc --> FeignClient
    FraudSvc --> RiskSvc
    FeignClient --> FastAPI

    %% ML Execution
    FastAPI --> XGBoost
    FastAPI --> IsoForest
    MLflow -.-> FastAPI

    %% Database Operations
    PaySvc --> DB_Payments
    PaySvc --> DB_Tx
    FraudSvc --> DB_Pred
    FraudSvc --> DB_Tx
    WallSvc --> DB_Wallets
    TxSvc --> DB_Tx
    TxSvc --> DB_Payments
    TxSvc --> WallSvc
```

---

## Tier Descriptions

### Client Tier — React 18 + Vite

Two distinct UIs are served from the same React SPA:

- **User Dashboard** — Wallet balance, payment creation, payment history, debit/credit ledger
- **Analyst Risk Desk** — Paginated review queue, transaction inspection with live solvency banner, approve/reject controls, fraud rule sandbox

All API calls include a `Bearer <JWT>` header. The React frontend reads the user's role from the decoded token to conditionally render the correct view.

---

### Core Backend Tier — Spring Boot 4.1 + Java 17

**Security layer** (`JwtAuthenticationFilter` + `SecurityConfig`):
- Extracts and validates JWT from every request
- Enforces RBAC: `ROLE_USER` for payments/wallet, `ROLE_ANALYST` for fraud rules and analytics
- Stateless — no server-side session

**Controllers** map HTTP verbs to service calls. There are 5 REST controllers:
- `AuthController` — register + login
- `PaymentController` — create + get payments
- `WalletController` — balance + ledger + top-up
- `TransactionController` — review queue + analyst status update
- `FraudRuleController` — analyst sandbox for rule evaluation

**Business Services** contain all domain logic. Key orchestration:
- `PaymentService` — idempotency check → balance check → fraud assessment → conditional wallet debit
- `FraudAssessmentService` — runs rule engine + calls ML service + calculates composite score
- `RiskAssessmentService` — applies weighted formula (60/25/15) and decision matrix
- `WalletService` — transactional debit/credit with optimistic locking (version field)
- `TransactionService` — analyst review queue with overdraft prevention on approval

**ML Client** (`MLPredictionFeignClient`) — Spring Cloud OpenFeign HTTP client. Calls `POST http://localhost:8000/predict` and maps the JSON response to `FraudPredictionResponse`.

---

### ML Inference Tier — Python 3.10 + FastAPI

Two models run in sequence for every `/predict` call:

1. **XGBoost** — supervised binary classifier trained on PaySim dataset. Outputs fraud probability `[0.0, 1.0]`.
2. **Isolation Forest** — unsupervised anomaly detector. Outputs a normalized anomaly score `[0, 100]`.

Both models were trained offline using scripts in `python-ml/scripts/` and saved as joblib bundles. MLflow tracked all experiments.

---

### Database & Persistence Tier — PostgreSQL

**10 tables** managed by Flyway migrations:

| Table Group | Tables |
|---|---|
| Identity | `users`, `roles`, `user_roles` |
| Wallet | `wallets`, `wallet_transactions` |
| Payments | `payments`, `idempotency_keys` |
| Fraud | `transactions`, `fraud_rule_results`, `fraud_predictions` |
| Audit | `audit_logs` |

See [database-erd.md](./database-erd.md) for the full ERD.