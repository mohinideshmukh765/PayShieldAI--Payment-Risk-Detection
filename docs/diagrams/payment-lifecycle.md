# PayShield AI — Payment Processing & Fraud Assessment Sequence

This sequence diagram traces the full lifecycle of a payment from the moment a user submits `POST /api/v1/payments` through fraud assessment and the final wallet debit (or hold).

---

## Key Design Decisions

1. **Idempotency first** — if an `Idempotency-Key` has already been used, the cached `PaymentResponse` is returned immediately. No duplicate processing.
2. **Available balance check** — the system deducts funds held in `REVIEW` status from the total wallet balance before checking affordability. This prevents overdraft from held funds.
3. **Wallet is debited ONLY on ALLOW** — for `REVIEW` and `BLOCK` decisions, the wallet balance is never reduced. `REVIEW` funds are simply excluded from the "available" calculation.
4. **Parallel fraud signals** — the rule engine and ML service conceptually run as two independent signals that are then combined by `RiskAssessmentService`.

---

## Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor User as Client (User)
    participant PC as PaymentController
    participant PS as PaymentService
    participant WS as WalletService
    participant FS as FraudAssessmentService
    participant RE as FraudRuleEngine
    participant ML as FastAPI Python ML Service
    participant RA as RiskAssessmentService
    participant DB as PostgreSQL Database

    User->>PC: POST /api/v1/payments (Idempotency-Key)
    PC->>PS: createPayment(email, key, request)
    PS->>DB: Check IdempotencyKey
    alt Key already exists
        DB-->>PS: Existing Payment
        PS-->>PC: Cached PaymentResponse
        PC-->>User: 200 OK
    else New Payment
        PS->>DB: Get User & Wallet
        PS->>DB: sumPendingReviewAmountByUserId(userId)
        DB-->>PS: Held Funds in Review
        
        alt Amount > (Wallet Balance - Held Funds)
            PS-->>PC: 400 Bad Request (Insufficient Available Balance)
            PC-->>User: Error Response (Funds Held in Review)
        else Sufficient Available Funds
            PS->>DB: Save Transaction (Status: PENDING)
            PS->>FS: assess(transaction, walletBalance)
            
            par Deterministic Heuristics
                FS->>RE: evaluate(context)
                RE-->>FS: 5 Rule Evaluations & Points
            and Python ML Inference
                FS->>ML: POST /predict (Payload)
                ML-->>FS: XGBoost Probability & IF Score
            end

            FS->>RA: calculateRiskScore(xgbProb, ifScore, rulePoints, highSeverity)
            RA-->>FS: Composite Score (0 - 100) & Decision (ALLOW / REVIEW / BLOCK)
            FS->>DB: Save FraudPrediction & FraudRuleResults
            FS-->>PS: FraudAssessmentResult

            alt Decision == ALLOW
                PS->>WS: debit(userId, amount, txnRef, payment)
                WS->>DB: Update Wallet Balance & Insert DEBIT Ledger
                PS->>DB: Update Transaction (COMPLETED) & Payment (APPROVED)
            else Decision == REVIEW
                PS->>DB: Update Transaction (PENDING) & Payment (REVIEW)
                Note over PS,DB: Wallet NOT debited. Funds held from available balance.
            else Decision == BLOCK
                PS->>DB: Update Transaction (BLOCKED) & Payment (REJECTED)
                Note over PS,DB: Wallet NOT debited.
            end

            PS->>DB: Save IdempotencyKey record
            PS-->>PC: PaymentResponse
            PC-->>User: 200 OK (Payment Result)
        end
    end
```

---

## Payment Status Outcomes

| Fraud Decision | Payment Status | Transaction Status | Wallet Effect |
|---|---|---|---|
| `ALLOW` | `APPROVED` | `COMPLETED` | Debited immediately |
| `REVIEW` | `REVIEW` | `PENDING` | Funds held (not debited); queued for analyst |
| `BLOCK` | `REJECTED` | `BLOCKED` | Not debited |

---

## Composite Risk Score Formula

```
Score = (XGBoost_Prob × 100 × 0.60)
      + (Rule_Points × 0.25)
      + (Isolation_Score × 0.15)

If any HIGH-severity rule fired:
  Score = max(Score, 55)

Decision:
  Score < 45           → ALLOW
  45 ≤ Score < 75      → REVIEW
  Score ≥ 75
    AND (XGBoost=1 OR Anomaly=true) → BLOCK
    AND both ML clean               → REVIEW (downgrade)
```

See [fraud-rule-engine.md](./fraud-rule-engine.md) for the complete flowchart.
