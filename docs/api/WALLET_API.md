# Wallet API

Base path: `/api/v1/wallet`  
Authentication: **Required** — `Authorization: Bearer <JWT>`  
Role: **USER**

---

## Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/wallet` | Get the authenticated user's wallet |
| `GET` | `/api/v1/wallet/transactions` | Get the full wallet ledger |
| `POST` | `/api/v1/wallet/topup` | Add funds to the wallet |

---

## Wallet Statuses

| Status | Meaning |
|---|---|
| `ACTIVE` | Wallet is active and can be used |

---

## GET `/api/v1/wallet`

Returns the authenticated user's wallet including current balance.

### Response — `200 OK`

```json
{
  "id": "9f8e7d6c-5b4a-3210-fedc-ba9876543210",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "balance": 100000.0000,
  "currency": "INR",
  "status": "ACTIVE",
  "createdAt": "2026-08-18T00:00:00Z",
  "updatedAt": "2026-08-18T00:00:00Z"
}
```

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Wallet ID |
| `userId` | `UUID` | Owner user ID |
| `balance` | `decimal` | Current balance (4 decimal precision) |
| `currency` | `string` | Currency code (default: `INR`) |
| `status` | `string` | Wallet status |
| `createdAt` | `ISO-8601` | Wallet creation timestamp |
| `updatedAt` | `ISO-8601` | Last update timestamp |

> **Note on Available Balance:** The displayed balance is the **total** balance. Funds held in REVIEW status payments are deducted from the spendable balance at payment time. The API does not currently expose "available balance" separately — this calculation happens inside `PaymentService`.

### cURL Example

```bash
curl http://localhost:8080/api/v1/wallet \
  -H "Authorization: Bearer <JWT>"
```

---

## GET `/api/v1/wallet/transactions`

Returns the full wallet ledger — all DEBIT and CREDIT entries for the authenticated user's wallet.

### Response — `200 OK`

```json
[
  {
    "id": "1a2b3c4d-5e6f-7890-abcd-ef1234567890",
    "walletId": "9f8e7d6c-5b4a-3210-fedc-ba9876543210",
    "paymentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "type": "DEBIT",
    "amount": 75000.0000,
    "balanceBefore": 100000.0000,
    "balanceAfter": 25000.0000,
    "reference": "TXN-A1B2C3D4E5F60001",
    "createdAt": "2026-08-18T00:00:00Z"
  },
  {
    "id": "2b3c4d5e-6f78-9012-bcde-f12345678901",
    "walletId": "9f8e7d6c-5b4a-3210-fedc-ba9876543210",
    "paymentId": null,
    "type": "CREDIT",
    "amount": 100000.0000,
    "balanceBefore": 0.0000,
    "balanceAfter": 100000.0000,
    "reference": "TOPUP",
    "createdAt": "2026-08-17T23:00:00Z"
  }
]
```

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Ledger entry ID |
| `walletId` | `UUID` | Wallet this entry belongs to |
| `paymentId` | `UUID` | Linked payment ID (null for top-ups) |
| `type` | `string` | `DEBIT` or `CREDIT` |
| `amount` | `decimal` | Transaction amount |
| `balanceBefore` | `decimal` | Wallet balance before this entry |
| `balanceAfter` | `decimal` | Wallet balance after this entry |
| `reference` | `string` | Transaction reference or `TOPUP` |
| `createdAt` | `ISO-8601` | Entry timestamp |

### cURL Example

```bash
curl http://localhost:8080/api/v1/wallet/transactions \
  -H "Authorization: Bearer <JWT>"
```

---

## POST `/api/v1/wallet/topup`

Adds funds to the authenticated user's wallet. Creates a CREDIT ledger entry.

> This endpoint is provided for demo and development purposes, allowing wallet funding without a manual database update.

### Request Body

```json
{
  "amount": 50000
}
```

| Field | Type | Required | Validation |
|---|---|---|---|
| `amount` | `number` | ✅ | Must be greater than 0 |

### Response — `200 OK`

```json
{
  "id": "9f8e7d6c-5b4a-3210-fedc-ba9876543210",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "balance": 150000.0000,
  "currency": "INR",
  "status": "ACTIVE",
  "createdAt": "2026-08-18T00:00:00Z",
  "updatedAt": "2026-08-18T00:05:00Z"
}
```

### Error Responses

| Status | Scenario |
|---|---|
| `400 Bad Request` | `amount` is missing, non-numeric, or ≤ 0 |
| `401 Unauthorized` | Missing or invalid JWT |

### cURL Example

```bash
curl -X POST http://localhost:8080/api/v1/wallet/topup \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{"amount": 50000}'
```
