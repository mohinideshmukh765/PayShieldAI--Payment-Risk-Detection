# PayShield AI — ML API

Base URL:

http://localhost:8000

## Health

GET /health

## Fraud Prediction

POST /predict

### Request

```json
{
  "step": 700,
  "transaction_type": "TRANSFER",
  "amount": 50000,
  "old_balance_origin": 60000,
  "new_balance_origin": 10000,
  "old_balance_destination": 5000,
  "new_balance_destination": 55000,
  "flagged_fraud": 0
}