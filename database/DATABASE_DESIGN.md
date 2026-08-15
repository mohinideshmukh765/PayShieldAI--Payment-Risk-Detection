# PayShield AI Database Design

## Database

PostgreSQL is used as the relational database because the application
requires transactional consistency, relational constraints, structured
financial data, and reliable persistence.

## Core Tables

### users

Stores application users and their authorization state.

### transactions

Stores payment transactions processed by PayShield AI.

### fraud_predictions

Stores machine learning predictions associated with transactions.

### fraud_rule_results

Stores individual fraud detection rule outcomes.

### audit_logs

Stores security and business actions for traceability.

## Financial Data

Transaction amounts use PostgreSQL numeric/decimal types and Java
BigDecimal to avoid floating-point precision problems.

## Identifiers

UUIDs are used as primary identifiers to avoid exposing sequential
database IDs through public APIs.