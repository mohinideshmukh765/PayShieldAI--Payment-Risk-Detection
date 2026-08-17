# PayShield AI — Backend Service

Spring Boot **4.1** / Java **17** REST API powering the PayShield AI fraud detection platform.

---

## Table of Contents

- [Overview](#overview)
- [Module Breakdown](#module-breakdown)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Running Locally](#running-locally)
- [API Base URLs](#api-base-urls)
- [Security Model](#security-model)
- [Fraud Rule Engine](#fraud-rule-engine)
- [Database Migrations](#database-migrations)
- [Key Dependencies](#key-dependencies)

---

## Overview

The backend exposes a REST API on **port 8080** and orchestrates:

1. **User authentication** — JWT-based stateless auth with RBAC (`ROLE_USER`, `ROLE_ANALYST`)
2. **Payment processing** — Idempotent payment creation with real-time fraud assessment before any wallet debit
3. **Fraud assessment** — Hybrid engine: 5 deterministic rules (25%) + XGBoost (60%) + Isolation Forest (15%)
4. **Analyst desk** — Paginated review queue; approve / reject payments with overdraft prevention
5. **Wallet ledger** — Full debit/credit transaction log per user

---

## Module Breakdown

```
src/main/java/com/payshield/
│
├── controller/                    # REST layer
│   ├── AuthController             # POST /api/v1/auth/register, /login
│   ├── PaymentController          # POST/GET /api/v1/payments
│   ├── TransactionController      # POST/GET/PUT /api/v1/transactions
│   ├── WalletController           # GET /api/v1/wallet, /transactions, POST /topup
│   ├── FraudRuleController        # POST /api/v1/fraud/rules/evaluate  [ANALYST]
│   └── HealthController           # GET /actuator/health
│
├── service/                       # Business logic layer
│   ├── AuthService                # Register, login, JWT generation
│   ├── PaymentService             # Idempotency, balance check, fraud orchestration
│   ├── FraudAssessmentService     # Rule evaluation + ML call + risk scoring
│   ├── RiskAssessmentService      # Composite score formula & decision matrix
│   ├── TransactionService         # Review queue, analyst approve/reject
│   ├── WalletService              # Debit, credit, top-up, ledger
│   └── HealthService
│
├── fraud/rule/                    # Deterministic rule engine
│   ├── FraudRuleEngine            # Evaluates all 5 rules, sums risk points
│   ├── FraudRule                  # Interface: evaluate(context) → FraudRuleEvaluation
│   ├── FraudRuleContext           # Input: userId, amount, velocity, etc.
│   ├── FraudRuleEvaluation        # Output: triggered, riskPoints, ruleType
│   ├── FraudRuleType              # Enum of all rule types
│   └── rules/
│       ├── LargeTransactionRule   # Amount > ₹50,000
│       ├── HighVelocityRule       # > 5 txn in 5 min or > 20 in 1 hr
│       ├── UnusualAmountRule      # > 3× user's average transaction
│       ├── AccountActivityAnomalyRule  # ≥ 3 failed attempts in 24 hr
│       └── DestinationRiskRule    # High-risk destination flag
│
├── entity/                        # JPA entities
│   ├── User, Role, Wallet, WalletTransaction
│   ├── Payment, IdempotencyKey
│   ├── Transaction, FraudRuleResult, FraudPrediction
│   ├── AuditLog
│   └── enums/
│       ├── PaymentStatus          # APPROVED, REVIEW, REJECTED
│       ├── PaymentType            # PAYMENT
│       ├── TransactionStatus      # PENDING, COMPLETED, BLOCKED
│       ├── TransactionType        # PAYMENT
│       ├── UserRole               # USER, ANALYST
│       ├── UserStatus             # ACTIVE, INACTIVE
│       ├── WalletStatus           # ACTIVE
│       └── WalletTransactionType  # DEBIT, CREDIT
│
├── dto/                           # Request / Response DTOs
│   ├── auth/                      # LoginRequest, RegisterRequest, AuthResponse
│   ├── payment/                   # CreatePaymentRequest, PaymentResponse
│   ├── transaction/               # CreateTransactionRequest, TransactionResponse, TransactionSummaryResponse
│   ├── wallet/                    # WalletResponse, WalletTransactionResponse
│   ├── fraud/                     # FraudRuleEvaluationRequest/Response
│   ├── ml/                        # FraudPredictionRequest/Response
│   └── ApiResponse<T>             # Generic wrapper: { success, message, data }
│
├── security/
│   ├── JwtService                 # generateToken, validateToken (HMAC-SHA)
│   ├── JwtAuthenticationFilter    # Extracts JWT from Authorization header
│   └── CustomUserDetailsService   # Loads UserDetails from DB by email
│
├── config/
│   ├── SecurityConfig             # CORS, CSRF off, stateless session, route rules
│   └── FraudRuleConfig            # Registers all 5 rule beans
│
├── client/
│   ├── MLPredictionFeignClient    # OpenFeign interface → POST /predict
│   └── LoggingInterceptor         # Logs outbound ML requests
│
├── repository/                    # Spring Data JPA repositories (10 total)
│
└── exception/
    ├── GlobalExceptionHandler     # Maps exceptions to HTTP status codes
    └── ResourceNotFoundException
```

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 17+ |
| Maven | 3.9+ |
| PostgreSQL | 15+ |
| Python ML Service | Running on port 8000 |

---

## Configuration

### `application.properties`

| Property | Description |
|---|---|
| `spring.datasource.url` | PostgreSQL JDBC URL |
| `spring.datasource.username` | DB username |
| `spring.datasource.password` | DB password |
| `jwt.secret` | HMAC-SHA signing secret (≥ 32 chars) |
| `jwt.expiration` | Token TTL in milliseconds (default: 86400000 = 24 h) |
| `ml.service.url` | Base URL of Python FastAPI service (default: `http://localhost:8000`) |

### Dev profile: `application-dev.properties`

Activate with `-Dspring.profiles.active=dev` for local overrides.

---

## Running Locally

```bash
cd backend

# Run with Maven wrapper
./mvnw spring-boot:run

# Or with a specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Server starts at **http://localhost:8080**.

---

## API Base URLs

| Group | Base Path | Auth Required |
|---|---|---|
| Authentication | `/api/v1/auth` | No |
| Payments | `/api/v1/payments` | Yes (USER) |
| Transactions | `/api/v1/transactions` | Yes (USER/ANALYST) |
| Wallet | `/api/v1/wallet` | Yes (USER) |
| Fraud Rules | `/api/v1/fraud/rules` | Yes (ANALYST only) |
| Analytics | `/api/v1/analytics` | Yes (ANALYST only) |
| Health | `/actuator/health` | No |

Full API reference → [docs/api/](../docs/api/)

---

## Security Model

All protected endpoints require:

```
Authorization: Bearer <JWT>
```

- Tokens are **stateless** (no server-side session)
- Signed with **HMAC-SHA** using a secret from config
- Default expiry: **24 hours**
- Role enforcement: `ROLE_ANALYST` required for `/api/v1/fraud/**` and `/api/v1/analytics/**`

---

## Fraud Rule Engine

Five rules run in sequence inside `FraudRuleEngine.evaluate(context)`:

| Rule | Trigger Condition | Risk Points |
|---|---|---|
| `LargeTransactionRule` | `amount > ₹50,000` | 30 |
| `HighVelocityRule` | `> 5 txn in 5 min` OR `> 20 txn in 1 hr` | 35 |
| `UnusualAmountRule` | `amount > 3× user average` | 20 |
| `AccountActivityAnomalyRule` | `≥ 3 failed attempts in last 24 hr` | 20 |
| `DestinationRiskRule` | Destination marked as high-risk | 10 |

Total rule risk points feed into the composite score at **25% weight**.

---

## Database Migrations

Managed by **Flyway**. Migration scripts live in `src/main/resources/db/`. They run automatically at startup. No manual SQL required.

---

## Key Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-webmvc` | REST API |
| `spring-boot-starter-security` | Auth + RBAC |
| `spring-boot-starter-data-jpa` | ORM / repositories |
| `spring-boot-starter-validation` | Request validation |
| `spring-boot-starter-actuator` | Health endpoint |
| `jjwt-api 0.12.6` | JWT generation & validation |
| `spring-cloud-starter-openfeign` | HTTP client to ML service |
| `flyway-core` | Database migrations |
| `postgresql` | JDBC driver |
| `lombok` | Boilerplate reduction |
