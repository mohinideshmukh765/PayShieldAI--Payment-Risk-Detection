# PayShield AI — Entity Relationship Diagram

```mermaid
erDiagram

    USERS {
        uuid id PK
        varchar name
        varchar email UK
        varchar password_hash
        varchar status
        timestamp created_at
        timestamp updated_at
    }

    ROLES {
        bigint id PK
        varchar name UK
    }

    USER_ROLES {
        uuid user_id PK, FK
        bigint role_id PK, FK
    }

    TRANSACTIONS {
        uuid id PK
        varchar transaction_reference UK
        uuid user_id FK
        varchar transaction_type
        numeric amount
        varchar currency
        varchar source_account
        varchar destination_account
        timestamp transaction_time
        varchar status
        timestamp created_at
    }

    FRAUD_RULE_RESULTS {
        uuid id PK
        uuid transaction_id FK
        varchar rule_name
        boolean rule_result
        varchar severity
        timestamp created_at
    }

    FRAUD_PREDICTIONS {
        uuid id PK
        uuid transaction_id PK, FK, UK
        numeric xgboost_probability
        boolean xgboost_prediction
        numeric isolation_score
        boolean isolation_anomaly
        numeric risk_score
        varchar risk_level
        varchar model_version
        timestamp created_at
    }

    AUDIT_LOGS {
        uuid id PK
        uuid user_id FK
        varchar action
        varchar entity_type
        varchar entity_id
        varchar ip_address
        timestamp created_at
    }

    USERS ||--o{ USER_ROLES : "has"
    ROLES ||--o{ USER_ROLES : "assigned through"

    USERS ||--o{ TRANSACTIONS : "creates"

    TRANSACTIONS ||--o{ FRAUD_RULE_RESULTS : "evaluated by"

    TRANSACTIONS ||--o| FRAUD_PREDICTIONS : "has prediction"

    USERS ||--o{ AUDIT_LOGS : "generates"