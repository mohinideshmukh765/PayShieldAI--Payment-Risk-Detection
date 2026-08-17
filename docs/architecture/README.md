# PayShield AI — Architectural Blueprint

PayShield AI is engineered with a **hybrid enterprise architecture** consisting of a Spring Boot core, a Python machine learning inference service, a reactive React frontend, and a PostgreSQL database.

---

## Architectural Principles

1. **Clean Separation of Concerns**
   - **Spring Boot 4.1 (Core Platform):** Owns payment processing, idempotency keys, wallet balances, double-entry ledgers, rule evaluation, and user authentication.
   - **Python FastAPI (ML Inference Engine):** Owns the supervised XGBoost model and unsupervised Isolation Forest anomaly detector.
   - **React 18 + Vite (Client Tier):** Provides tailored interfaces for `USER` and `ANALYST` roles.

2. **Two-Role Security Model (`USER` & `ANALYST`)**
   - Direct RBAC via Spring Security with stateless HMAC-SHA JWT tokens.
   - `ROLE_USER` — wallet, payments, own transaction history
   - `ROLE_ANALYST` — review queue, approve/reject, fraud rule sandbox

3. **Idempotency & Double-Spending Protection**
   - Client-generated `Idempotency-Key` headers prevent duplicate charges on network retry.
   - Dynamic **Available to Spend** balance checking locks pending review funds, eliminating overdraft risks.

4. **Hybrid Risk Scoring (ML + Heuristic Rules)**
   - 5 heuristic rules provide deterministic baseline protection (25% weight).
   - XGBoost provides supervised fraud probability trained on PaySim (60% weight).
   - Isolation Forest provides unsupervised anomaly detection (15% weight).

5. **Analyst Override with Overdraft Prevention**
   - Payments scored as REVIEW are held for analyst inspection.
   - The system re-checks wallet solvency at approval time to prevent approving payments the user can no longer afford.

---

## System Component Diagram

```
┌────────────────────────────────────────────────────────────┐
│                  React 18 Frontend Client                  │
│  - User Wallet & Payments Hub                              │
│  - Analyst Fraud & Risk Review Desk                        │
└────────────────────────────┬───────────────────────────────┘
                             │ HTTP / REST / JWT
┌────────────────────────────▼───────────────────────────────┐
│           Spring Boot 4.1 Enterprise Core (:8080)          │
│  - Spring Security (JWT Filter & Role Validation)          │
│  - PaymentService  (Idempotency & Available Balance)       │
│  - FraudAssessmentService (Rule Engine + ML Orchestration) │
│  - RiskAssessmentService  (Composite Scoring & Decision)   │
│  - WalletService  (Optimistic Locking & Double-Entry)      │
│  - TransactionService (Analyst Override & Overdraft Check) │
│  - MLPredictionFeignClient (OpenFeign → FastAPI)           │
└──────────────────┬──────────────────────────┬──────────────┘
                   │ SQL (Flyway migrations)   │ REST HTTP
┌──────────────────▼──────────────┐ ┌─────────▼──────────────┐
│     PostgreSQL Database         │ │ Python FastAPI (:8000)  │
│ - users, roles, user_roles      │ │ - XGBoost Classifier    │
│ - wallets, wallet_transactions  │ │ - Isolation Forest      │
│ - payments, idempotency_keys    │ │ - Feature Engineering   │
│ - transactions                  │ │ - MLflow Tracking       │
│ - fraud_rule_results            │ └────────────────────────┘
│ - fraud_predictions             │
│ - audit_logs                    │
└─────────────────────────────────┘
```

---

## Key Design Decisions

### Why Separate ML Service?

The Python ML service is deployed independently for several reasons:
- Python has the best ML ecosystem (scikit-learn, XGBoost, MLflow)
- ML model updates don't require redeploying the Java backend
- The inference service can be scaled independently
- MLflow integration is native in Python

### Why Optimistic Locking on Wallets?

The `wallets` table uses a `@Version` column to implement optimistic locking. This prevents concurrent debit operations (e.g., two payments approved simultaneously) from corrupting the balance. If two transactions try to update the same wallet row simultaneously, one will fail with an `OptimisticLockException` and the operation retried.

### Why Idempotency Keys?

Payment APIs are called over unreliable networks. A client may retry a request if it doesn't receive a response (due to timeout, network drop, etc.). Without idempotency, a retry creates a duplicate charge. The `Idempotency-Key` header + the `idempotency_keys` table ensure that retried requests return the exact same response without reprocessing.

---

## Related Documents

- [system-architecture.md](../diagrams/system-architecture.md) — Full Mermaid architecture diagram
- [payment-lifecycle.md](../diagrams/payment-lifecycle.md) — Payment sequence diagram
- [fraud-rule-engine.md](../diagrams/fraud-rule-engine.md) — Fraud engine flowchart
- [backend-architecture.md](./backend-architecture.md) — Backend layered architecture
- [database-erd.md](../diagrams/database-erd.md) — Complete database ERD