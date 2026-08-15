```mermaid
sequenceDiagram
    participant Client
    participant API as Spring Boot API
    participant Service as Transaction Service
    participant DB as PostgreSQL

    Client->>API: POST /transactions
    API->>API: Validate request
    API->>Service: Create transaction
    Service->>DB: Find user
    DB-->>Service: User
    Service->>DB: Save transaction
    DB-->>Service: Transaction
    Service-->>API: TransactionResponse
    API-->>Client: 201 Created