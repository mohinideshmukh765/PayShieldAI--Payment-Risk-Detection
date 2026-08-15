# Fraud Rule Engine API

## Base URL

http://localhost:8080

## Authentication

Requires JWT authentication.

Required roles:

- ANALYST
- ADMIN

USER does not have access.

---

## Evaluate Fraud Rules

### Endpoint

POST /api/v1/fraud/rules/evaluate

### Headers

Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

### Request

{
"userId": "USER_UUID",
"amount": 85000,
"transactionsLast5Minutes": 7,
"transactionsLast1Hour": 24,
"averageTransactionAmount": 12000,
"recentFailedAttempts": 2,
"newDevice": true,
"locationChanged": true,
"destinationHighRisk": true
}

### Expected Behavior

The rule engine evaluates:

- Large transaction
- High velocity
- Unusual amount
- Account activity anomaly
- Destination risk

### Example Response

{
"results": [
{
"ruleType": "LARGE_TRANSACTION",
"triggered": true,
"riskPoints": 25
},
{
"ruleType": "HIGH_VELOCITY",
"triggered": true,
"riskPoints": 25
}
],
"totalRiskPoints": 100,
"triggeredRules": 5
}

---

## Authorization

USER:

HTTP 403 Forbidden

ANALYST:

HTTP 200 OK

ADMIN:

HTTP 200 OK

---

## Important

This endpoint currently evaluates deterministic rules only.

It does not call the Python ML service.

XGBoost and Isolation Forest are introduced in later phases.