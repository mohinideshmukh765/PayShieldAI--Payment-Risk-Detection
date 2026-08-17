# Analytics API — Consolidated Endpoint Reference

Base path: `/api/v1/analytics`  
Authentication: **Required** — `Authorization: Bearer <JWT>`  
Role: **ANALYST only** (`ROLE_ANALYST`)

> **Note:** The `/api/v1/analytics` route group is reserved for analyst-facing analytics and reporting. The endpoints below reflect the complete REST surface currently available in PayShield AI.

---

## Complete Endpoint Directory

| Category | Method | Endpoint | Role | Description | Reference |
|---|---|---|---|---|---|
| **Auth** | `POST` | `/api/v1/auth/register` | Public | Register a new user & wallet | [AUTHENTICATION_API.md](./AUTHENTICATION_API.md) |
| **Auth** | `POST` | `/api/v1/auth/login` | Public | Authenticate & receive JWT | [AUTHENTICATION_API.md](./AUTHENTICATION_API.md) |
| **Wallet** | `GET` | `/api/v1/wallet` | `ROLE_USER` | Get current wallet balance | [WALLET_API.md](./WALLET_API.md) |
| **Wallet** | `POST` | `/api/v1/wallet/topup` | `ROLE_USER` | Add demo funds to wallet | [WALLET_API.md](./WALLET_API.md) |
| **Wallet** | `GET` | `/api/v1/wallet/transactions` | `ROLE_USER` | View full debit/credit ledger | [WALLET_API.md](./WALLET_API.md) |
| **Payments** | `POST` | `/api/v1/payments` | `ROLE_USER` | Create payment with fraud check | [PAYMENT_API.md](./PAYMENT_API.md) |
| **Payments** | `GET` | `/api/v1/payments` | `ROLE_USER` | List user's payment history | [PAYMENT_API.md](./PAYMENT_API.md) |
| **Payments** | `GET` | `/api/v1/payments/{id}` | `ROLE_USER` | Get single payment details | [PAYMENT_API.md](./PAYMENT_API.md) |
| **Transactions** | `POST` | `/api/v1/transactions` | `ROLE_USER` | Create a transaction record | [TRANSACTION_API.md](./TRANSACTION_API.md) |
| **Transactions** | `GET` | `/api/v1/transactions` | `ROLE_USER` / `ROLE_ANALYST` | Paginated list (filterable) | [TRANSACTION_API.md](./TRANSACTION_API.md) |
| **Transactions** | `GET` | `/api/v1/transactions/{id}` | `ROLE_USER` / `ROLE_ANALYST` | Detail view with wallet solvency | [TRANSACTION_API.md](./TRANSACTION_API.md) |
| **Transactions** | `PUT` | `/api/v1/transactions/{id}/status` | `ROLE_ANALYST` | Approve (`COMPLETED`) or Reject (`BLOCKED`) | [TRANSACTION_API.md](./TRANSACTION_API.md) |
| **Fraud Rules** | `POST` | `/api/v1/fraud/rules/evaluate` | `ROLE_ANALYST` | Rule engine sandbox evaluation | [FRAUD_RULE_API.md](./FRAUD_RULE_API.md) |
| **ML Service** | `GET` | `http://localhost:8000/health` | Internal | FastAPI liveness check | [ML_API.md](./ML_API.md) |
| **ML Service** | `POST` | `http://localhost:8000/predict` | Internal (Spring Boot) | XGBoost + Isolation Forest inference | [ML_API.md](./ML_API.md) |
| **Health** | `GET` | `/actuator/health` | Public | Spring Boot actuator health | — |
| **Health** | `GET` | `/actuator/info` | Public | Application info | — |

---

## Route Security Summary

| Path Pattern | Required Role | Notes |
|---|---|---|
| `/api/v1/auth/**` | None | Public registration & login |
| `/api/v1/payments/**` | `ROLE_USER` | Authenticated users only |
| `/api/v1/wallet/**` | `ROLE_USER` | Authenticated users only |
| `/api/v1/transactions/**` | `ROLE_USER` or `ROLE_ANALYST` | PUT status requires ANALYST |
| `/api/v1/fraud/**` | `ROLE_ANALYST` | Analyst-only sandbox |
| `/api/v1/analytics/**` | `ROLE_ANALYST` | Reserved for analytics |
| `/actuator/health` | None | Public liveness probe |
| `/actuator/info` | None | Public info |

---

## Standard Response Envelope

All Spring Boot API endpoints return the `ApiResponse<T>` wrapper:

```json
{
  "success": true,
  "message": "Human-readable status message",
  "data": { }
}
```

On error:

```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

---

## Common HTTP Status Codes

| Code | Meaning |
|---|---|
| `200 OK` | Request successful |
| `201 Created` | Resource created |
| `400 Bad Request` | Validation failure or business rule violation |
| `401 Unauthorized` | Missing or expired JWT |
| `403 Forbidden` | Insufficient role |
| `404 Not Found` | Resource does not exist |
| `503 Service Unavailable` | ML service is unreachable |
