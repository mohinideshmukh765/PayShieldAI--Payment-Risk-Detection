# Payment API

Base path: `/api/v1/payments`  
Authentication: **Required** — `Authorization: Bearer <JWT>`  
Role: **USER**

---

## Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/payments` | Create a new payment |
| `GET` | `/api/v1/payments/{paymentId}` | Get a single payment by ID |
| `GET` | `/api/v1/payments` | Get all payments for the authenticated user |

---

## Payment Statuses

| Status | Meaning |
|---|---|
| `APPROVED` | Fraud engine scored low risk → wallet debited immediately |
| `REVIEW` | Fraud engine flagged for review → funds held, queued for analyst |
| `REJECTED` | Fraud engine blocked → wallet not debited |

---

## POST `/api/v1/payments`

Creates a new payment. Triggers the full fraud assessment pipeline before any wallet debit occurs.

### Required Header

```
Idempotency-Key: <unique-string>
```

The `Idempotency-Key` header ensures retried requests do not result in duplicate payments. Supply a unique UUID or string per payment attempt.

### Request Body

```json
{
  "amount": 75000.00,
  "currency": "INR",
  "description": "Vendor payment — Invoice #1042"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `amount` | `decimal` | ✅ | Payment amount (must be > 0) |
| `currency` | `string` | ❌ | ISO 4217 currency code (default: `INR`) |
| `description` | `string` | ❌ | Free-text description (max 500 chars) |

### Fraud Assessment

Before creating the payment, the service:
1. Checks the idempotency key (returns cached response if duplicate)
2. Validates available wallet balance (total balance minus funds held in review)
3. Creates a `PENDING` transaction record
4. Runs **5 deterministic fraud rules** and calls the **Python ML service**
5. Calculates a composite risk score and makes an ALLOW / REVIEW / BLOCK decision
6. Debits wallet **only** if decision is `ALLOW`

### Response — `200 OK`

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "transactionId": "TXN-A1B2C3D4E5F60001",
  "amount": 75000.00,
  "currency": "INR",
  "paymentType": "PAYMENT",
  "status": "REVIEW",
  "description": "Vendor payment — Invoice #1042",
  "idempotencyKey": "my-unique-key-001",
  "createdAt": "2026-08-18T00:00:00Z",
  "updatedAt": "2026-08-18T00:00:00Z"
}
```

### Error Responses

| Status | Scenario |
|---|---|
| `400 Bad Request` | Amount ≤ 0 or missing Idempotency-Key |
| `400 Bad Request` | Insufficient available balance (with breakdown of held funds) |
| `401 Unauthorized` | Missing or invalid JWT |
| `503 Service Unavailable` | Python ML service is unavailable |

### Error Example — Insufficient Available Balance

```json
{
  "success": false,
  "message": "Insufficient available balance. Total wallet balance is ₹100,000.00, but ₹80,000.00 is currently held in review for pending payments. Available to spend: ₹20,000.00",
  "data": null
}
```

### cURL Example

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer <JWT>" \
  -H "Idempotency-Key: pay-$(uuidgen)" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 75000.00,
    "currency": "INR",
    "description": "Vendor payment"
  }'
```

---

## GET `/api/v1/payments/{paymentId}`

Retrieves a single payment by its UUID. Only the payment owner can access it.

### Path Parameter

| Parameter | Type | Description |
|---|---|---|
| `paymentId` | `UUID` | Payment ID |

### Response — `200 OK`

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "transactionId": "TXN-A1B2C3D4E5F60001",
  "amount": 75000.00,
  "currency": "INR",
  "paymentType": "PAYMENT",
  "status": "APPROVED",
  "description": "Vendor payment",
  "idempotencyKey": "my-unique-key-001",
  "createdAt": "2026-08-18T00:00:00Z",
  "updatedAt": "2026-08-18T00:00:00Z"
}
```

### Error Responses

| Status | Scenario |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT |
| `404 Not Found` | Payment not found |
| `403 Forbidden` | Payment belongs to a different user |

### cURL Example

```bash
curl http://localhost:8080/api/v1/payments/3fa85f64-5717-4562-b3fc-2c963f66afa6 \
  -H "Authorization: Bearer <JWT>"
```

---

## GET `/api/v1/payments`

Returns all payments for the authenticated user, ordered by creation date (newest first).

### Response — `200 OK`

```json
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "transactionId": "TXN-A1B2C3D4E5F60001",
    "amount": 75000.00,
    "currency": "INR",
    "paymentType": "PAYMENT",
    "status": "APPROVED",
    "description": "Vendor payment",
    "idempotencyKey": "my-unique-key-001",
    "createdAt": "2026-08-18T00:00:00Z",
    "updatedAt": "2026-08-18T00:00:00Z"
  }
]
```

### cURL Example

```bash
curl http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer <JWT>"
```
