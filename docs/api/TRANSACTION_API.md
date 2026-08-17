# Transaction API

Base path: `/api/v1/transactions`  
Authentication: **Required** — `Authorization: Bearer <JWT>`  
Role: **USER** (create/get own) | **ANALYST** (get all, update status)

---

## Endpoints

| Method | Path | Description | Role |
|---|---|---|---|
| `POST` | `/api/v1/transactions` | Create a transaction record | USER |
| `GET` | `/api/v1/transactions` | Get paginated list (with filters) | USER / ANALYST |
| `GET` | `/api/v1/transactions/{transactionId}` | Get a single transaction detail | USER / ANALYST |
| `PUT` | `/api/v1/transactions/{transactionId}/status` | Update transaction status (analyst review) | ANALYST |

---

## Transaction Statuses

| Status | Meaning |
|---|---|
| `PENDING` | Created; waiting for fraud assessment result or analyst review |
| `COMPLETED` | Fraud cleared or analyst approved; wallet debited |
| `BLOCKED` | Fraud engine blocked or analyst rejected |

---

## Transaction Types

| Type | Meaning |
|---|---|
| `PAYMENT` | A payment transaction |

---

## POST `/api/v1/transactions`

Creates a raw transaction record. In normal payment flow this is called internally by `PaymentService`. This endpoint can also be used to create standalone transaction records.

### Request Body

```json
{
  "transactionType": "PAYMENT",
  "amount": 50000.00,
  "currency": "INR",
  "sourceAccount": "WALLET-3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "destinationAccount": "PAYMENT-DESTINATION",
  "transactionTime": "2026-08-18T00:00:00Z"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `transactionType` | `enum` | ✅ | `PAYMENT` |
| `amount` | `decimal` | ✅ | Transaction amount (must be > 0) |
| `currency` | `string` | ❌ | ISO 4217 code (default: `INR`) |
| `sourceAccount` | `string` | ✅ | Source account identifier |
| `destinationAccount` | `string` | ✅ | Destination account identifier |
| `transactionTime` | `ISO-8601` | ✅ | Timestamp of the transaction |

### Response — `201 Created`

```json
{
  "success": true,
  "message": "Transaction created successfully",
  "data": {
    "id": "b1e23a4c-8d3f-4b2a-9c1e-5f7d6e8a0b2d",
    "transactionReference": "TXN-A1B2C3D4E5F60001",
    "transactionType": "PAYMENT",
    "amount": 50000.00,
    "currency": "INR",
    "sourceAccount": "WALLET-3fa85f64...",
    "destinationAccount": "PAYMENT-DESTINATION",
    "transactionTime": "2026-08-18T00:00:00Z",
    "status": "PENDING",
    "createdAt": "2026-08-18T00:00:00Z"
  }
}
```

### cURL Example

```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionType": "PAYMENT",
    "amount": 50000.00,
    "sourceAccount": "WALLET-abc",
    "destinationAccount": "VENDOR-XYZ",
    "transactionTime": "2026-08-18T00:00:00Z"
  }'
```

---

## GET `/api/v1/transactions`

Returns a paginated list of transactions. Analysts use this as the **Review Queue** by filtering `status=PENDING`.

### Query Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `status` | `enum` | — | Filter by `PENDING`, `COMPLETED`, or `BLOCKED` |
| `type` | `enum` | — | Filter by transaction type (e.g., `PAYMENT`) |
| `page` | `int` | `0` | Zero-based page number |
| `size` | `int` | `20` | Page size (max: 100) |

Results are sorted by `createdAt` descending (newest first).

### Response — `200 OK`

```json
{
  "success": true,
  "message": "Transactions retrieved successfully",
  "data": {
    "content": [
      {
        "id": "b1e23a4c-8d3f-4b2a-9c1e-5f7d6e8a0b2d",
        "transactionReference": "TXN-A1B2C3D4E5F60001",
        "transactionType": "PAYMENT",
        "amount": 50000.00,
        "currency": "INR",
        "status": "PENDING",
        "createdAt": "2026-08-18T00:00:00Z"
      }
    ],
    "totalElements": 47,
    "totalPages": 3,
    "size": 20,
    "number": 0
  }
}
```

### cURL — Analyst Review Queue

```bash
curl "http://localhost:8080/api/v1/transactions?status=PENDING&size=100" \
  -H "Authorization: Bearer <ANALYST_JWT>"
```

---

## GET `/api/v1/transactions/{transactionId}`

Returns full details of a single transaction, including the user's live wallet balance (used by analysts to check solvency before approval).

### Path Parameter

| Parameter | Type | Description |
|---|---|---|
| `transactionId` | `UUID` | Transaction ID |

### Response — `200 OK`

```json
{
  "success": true,
  "message": "Transaction retrieved successfully",
  "data": {
    "id": "b1e23a4c-8d3f-4b2a-9c1e-5f7d6e8a0b2d",
    "transactionReference": "TXN-A1B2C3D4E5F60001",
    "transactionType": "PAYMENT",
    "amount": 50000.00,
    "currency": "INR",
    "sourceAccount": "WALLET-3fa85f64...",
    "destinationAccount": "VENDOR-XYZ",
    "transactionTime": "2026-08-18T00:00:00Z",
    "status": "PENDING",
    "createdAt": "2026-08-18T00:00:00Z",
    "userWalletBalance": 100000.00
  }
}
```

The `userWalletBalance` field shows the user's **current** wallet balance, enabling analysts to check solvency before approving.

### cURL Example

```bash
curl http://localhost:8080/api/v1/transactions/b1e23a4c-8d3f-4b2a-9c1e-5f7d6e8a0b2d \
  -H "Authorization: Bearer <ANALYST_JWT>"
```

---

## PUT `/api/v1/transactions/{transactionId}/status`

**ANALYST ONLY.** Updates the status of a PENDING transaction (approve or reject).

### Path Parameter

| Parameter | Type | Description |
|---|---|---|
| `transactionId` | `UUID` | Transaction ID |

### Query Parameter

| Parameter | Type | Required | Values | Description |
|---|---|---|---|---|
| `status` | `enum` | ✅ | `COMPLETED` or `BLOCKED` | New status to apply |

### Approval Logic (`status=COMPLETED`)

Before approving, the service:
1. Verifies the transaction is currently `PENDING`
2. Fetches the user's **current** wallet balance
3. If balance < payment amount → returns `400 Bad Request` (overdraft prevention)
4. If balance ≥ amount → debits wallet, creates ledger entry, marks payment `APPROVED`, transaction `COMPLETED`

### Rejection Logic (`status=BLOCKED`)

1. Marks payment `REJECTED`
2. Marks transaction `BLOCKED`
3. Funds that were held in review are **released back** to available balance (no actual debit occurred)

### Response — `200 OK`

```json
{
  "success": true,
  "message": "Transaction status updated to COMPLETED",
  "data": {
    "id": "b1e23a4c-8d3f-4b2a-9c1e-5f7d6e8a0b2d",
    "status": "COMPLETED",
    ...
  }
}
```

### Error Responses

| Status | Scenario |
|---|---|
| `400 Bad Request` | Insufficient wallet balance to approve |
| `400 Bad Request` | Transaction is not in PENDING state |
| `401 Unauthorized` | Missing or invalid JWT |
| `403 Forbidden` | Not an ANALYST role |
| `404 Not Found` | Transaction not found |

### cURL — Approve

```bash
curl -X PUT "http://localhost:8080/api/v1/transactions/b1e23a4c-8d3f-4b2a-9c1e-5f7d6e8a0b2d/status?status=COMPLETED" \
  -H "Authorization: Bearer <ANALYST_JWT>"
```

### cURL — Reject

```bash
curl -X PUT "http://localhost:8080/api/v1/transactions/b1e23a4c-8d3f-4b2a-9c1e-5f7d6e8a0b2d/status?status=BLOCKED" \
  -H "Authorization: Bearer <ANALYST_JWT>"
```
