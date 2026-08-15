```mermaid
sequenceDiagram
    participant Client
    participant Filter as JWT Filter
    participant Security as SecurityContext
    participant Controller
    participant Service

    Client->>Filter: Request + Bearer JWT
    Filter->>Filter: Validate JWT
    Filter->>Security: Set Authentication
    Filter->>Controller: Continue request
    Controller->>Service: Business operation
    Service-->>Controller: Result
    Controller-->>Client: Response