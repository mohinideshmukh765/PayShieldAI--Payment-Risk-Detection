# Fraud Rule API

Base path: `/api/v1/fraud/rules`  
Authentication: **Required** — `Authorization: Bearer <JWT>`  
Role: **ANALYST only** (`ROLE_ANALYST`)

---

## Overview

The Fraud Rule API exposes the deterministic rule engine as a **sandbox** for fraud analysts. Analysts can submit any transaction payload and see exactly which of the 5 fraud rules would trigger, along with the total risk points scored.

This is the same rule engine that runs automatically inside `FraudAssessmentService` on every payment.

---

## Endpoints

| Method | Path | Description | Role |
|---|---|---|---|
| `POST` | `/api/v1/fraud/rules/evaluate` | Evaluate all fraud rules against a payload | ANALYST |

---

## The 5 Fraud Rules

| Rule | Trigger Condition | Risk Points |
|---|---|---|
| `LARGE_TRANSACTION` | `amount > ₹50,000` | 30 |
| `HIGH_VELOCITY` | `> 5 transactions in last 5 minutes` OR `> 20 transactions in last 1 hour` | 35 |
| `UNUSUAL_AMOUNT` | `amount > 3× user's average transaction amount` | 20 |
| `ACCOUNT_ACTIVITY_ANOMALY` | `≥ 3 failed attempts in last 24 hours` | 20 |
| `DESTINATION_RISK` | Destination is flagged as high-risk | 10 |

Maximum possible rule score: **115 points**

---

## POST `/api/v1/fraud/rules/evaluate`

Evaluates all 5 fraud rules against the provided context and returns per-rule results plus a total risk score.

### Request Body

```json
{
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "transactionId": "b1e23a4c-8d3f-4b2a-9c1e-5f7d6e8a0b2d",
  "amount": 75000.00,
  "transactionsLast5Minutes": 3,
  "transactionsLast1Hour": 8,
  "averageTransactionAmount": 20000.00,
  "recentFailedAttempts": 1,
  "newDevice": false,
  "locationChanged": false,
  "destinationHighRisk": false
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `userId` | `UUID` | ✅ | User ID being evaluated |
| `transactionId` | `UUID` | ✅ | Transaction ID being evaluated |
| `amount` | `decimal` | ✅ | Transaction amount |
| `transactionsLast5Minutes` | `int` | ✅ | Count of user's transactions in last 5 minutes |
| `transactionsLast1Hour` | `int` | ✅ | Count of user's transactions in last 1 hour |
| `averageTransactionAmount` | `decimal` | ✅ | User's historical average transaction amount |
| `recentFailedAttempts` | `int` | ✅ | Count of failed attempts in last 24 hours |
| `newDevice` | `boolean` | ✅ | Whether this is a new/unrecognized device |
| `locationChanged` | `boolean` | ✅ | Whether the user's location has changed |
| `destinationHighRisk` | `boolean` | ✅ | Whether the destination is on a high-risk list |

### Response — `200 OK`

```json
{
  "evaluations": [
    {
      "ruleType": "LARGE_TRANSACTION",
      "triggered": true,
      "riskPoints": 30,
      "reason": "Amount ₹75,000.00 exceeds threshold of ₹50,000.00"
    },
    {
      "ruleType": "HIGH_VELOCITY",
      "triggered": false,
      "riskPoints": 0,
      "reason": "Velocity within acceptable limits"
    },
    {
      "ruleType": "UNUSUAL_AMOUNT",
      "triggered": true,
      "riskPoints": 20,
      "reason": "Amount is 3.75× the user average of ₹20,000.00"
    },
    {
      "ruleType": "ACCOUNT_ACTIVITY_ANOMALY",
      "triggered": false,
      "riskPoints": 0,
      "reason": "Recent failed attempts within normal range"
    },
    {
      "ruleType": "DESTINATION_RISK",
      "triggered": false,
      "riskPoints": 0,
      "reason": "Destination not flagged as high-risk"
    }
  ],
  "totalRiskPoints": 50,
  "triggeredRuleCount": 2
}
```

| Field | Type | Description |
|---|---|---|
| `evaluations` | `array` | Per-rule result objects |
| `evaluations[].ruleType` | `string` | Rule identifier |
| `evaluations[].triggered` | `boolean` | Whether the rule fired |
| `evaluations[].riskPoints` | `int` | Points contributed (0 if not triggered) |
| `totalRiskPoints` | `int` | Sum of all triggered rule points (0–115) |
| `triggeredRuleCount` | `long` | Number of rules that fired |

### Error Responses

| Status | Scenario |
|---|---|
| `400 Bad Request` | Missing or invalid request fields |
| `401 Unauthorized` | Missing or invalid JWT |
| `403 Forbidden` | Caller does not have `ROLE_ANALYST` |

### cURL Example

```bash
curl -X POST http://localhost:8080/api/v1/fraud/rules/evaluate \
  -H "Authorization: Bearer <ANALYST_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "transactionId": "b1e23a4c-8d3f-4b2a-9c1e-5f7d6e8a0b2d",
    "amount": 75000.00,
    "transactionsLast5Minutes": 3,
    "transactionsLast1Hour": 8,
    "averageTransactionAmount": 20000.00,
    "recentFailedAttempts": 1,
    "newDevice": false,
    "locationChanged": false,
    "destinationHighRisk": false
  }'
```

---

## How Rule Scores Feed Into the Final Decision

Rule points are **one input** into the composite fraud score:

```
Composite Score = (XGBoost_Probability × 100 × 0.60)
               + (Rule_Points × 0.25)
               + (Isolation_Forest_Score × 0.15)
```

The rule sandbox lets analysts understand the rule contribution independently from the ML signals.