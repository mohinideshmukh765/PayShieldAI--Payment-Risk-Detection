# PayShield AI — Analyst Review & Decision Flow

This sequence diagram documents how a fraud analyst interacts with the review queue — from loading the list of pending transactions through inspecting an individual transaction (with live wallet solvency check) to making an approve or reject decision.

---

## Analyst Workflow Overview

1. **Load Review Queue** — `GET /api/v1/transactions?status=PENDING` — paginated list of all transactions waiting for human review
2. **Inspect Transaction** — `GET /api/v1/transactions/{id}` — full details including the user's **live wallet balance** (solvency banner)
3. **Make Decision**:
   - **Approve** → `PUT /api/v1/transactions/{id}/status?status=COMPLETED` — debit wallet + release held funds
   - **Reject** → `PUT /api/v1/transactions/{id}/status?status=BLOCKED` — cancel payment + release held funds

---

## Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor Analyst as Fraud Analyst
    participant TC as TransactionController
    participant TS as TransactionService
    participant WS as WalletService
    participant DB as PostgreSQL Database

    Analyst->>TC: GET /api/v1/transactions (Review Queue)
    TC->>TS: getTransactions(status=PENDING, size=100)
    TS->>DB: Query Transactions
    DB-->>TS: Page of TransactionSummaryResponse (with userId)
    TS-->>TC: 200 OK
    TC-->>Analyst: Transactions List

    Analyst->>TC: GET /api/v1/transactions/{id} (Inspect)
    TC->>TS: getTransaction(id)
    TS->>DB: Find Transaction & User Wallet
    DB-->>TS: Transaction + Live Wallet Balance
    TS-->>TC: TransactionResponse (including userWalletBalance)
    TC-->>Analyst: Detailed Inspection View (Solvency Banner & Sandbox)

    alt Decision: APPROVE (status=COMPLETED)
        Analyst->>TC: PUT /api/v1/transactions/{id}/status?status=COMPLETED
        TC->>TS: updateTransactionStatus(id, COMPLETED)
        TS->>DB: Check Transaction is PENDING
        TS->>WS: getUserWallet(userId)
        WS-->>TS: Current Wallet Balance
        
        alt Wallet Balance < Payment Amount
            TS-->>TC: 400 Bad Request ("Insufficient wallet balance to approve")
            TC-->>Analyst: Overdraft Prevention Error Alert
        else Wallet Balance >= Payment Amount
            TS->>DB: Update Payment (Status: APPROVED)
            TS->>WS: debit(userId, amount, txnRef, payment)
            WS->>DB: Update Wallet & Insert DEBIT Ledger
            TS->>DB: Update Transaction (Status: COMPLETED)
            TS-->>TC: Updated TransactionResponse
            TC-->>Analyst: 200 OK ("Payment Approved & Debited")
        end

    else Decision: REJECT (status=BLOCKED)
        Analyst->>TC: PUT /api/v1/transactions/{id}/status?status=BLOCKED
        TC->>TS: updateTransactionStatus(id, BLOCKED)
        TS->>DB: Update Payment (Status: REJECTED)
        TS->>DB: Update Transaction (Status: BLOCKED)
        Note over TS,DB: Held funds released back to user available balance.
        TS-->>TC: Updated TransactionResponse
        TC-->>Analyst: 200 OK ("Payment Blocked & Rejected")
    end
```

---

## Solvency Check — Overdraft Prevention

When an analyst approves a transaction, `TransactionService` checks the user's **current** wallet balance before calling `WalletService.debit()`. This prevents a scenario where:

- User had ₹100,000 in their wallet at the time of payment
- Multiple payments went to `REVIEW`
- By the time the analyst approves, the wallet balance has dropped below the payment amount

If `walletBalance < paymentAmount`, the approval is rejected with a `400 Bad Request` and a clear error message:

```json
{
  "success": false,
  "message": "Insufficient wallet balance to approve this transaction. Current balance: ₹20,000.00, Required: ₹75,000.00",
  "data": null
}
```

---

## Held Funds Mechanics

When a payment is in `REVIEW` status, the funds are **not physically held** — the wallet balance is unchanged. Instead, `PaymentService` queries `sumPendingReviewAmountByUserId()` before each new payment to calculate:

```
Available Balance = Wallet Balance − Sum of REVIEW payment amounts
```

When an analyst **rejects** (BLOCKED), the REVIEW payment is removed from this sum, so the funds become available again automatically — no explicit release is needed.
