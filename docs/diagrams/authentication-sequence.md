```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant Security as Spring Security
    participant DB as PostgreSQL

    Client->>AuthController: POST /auth/login
    AuthController->>AuthService: LoginRequest
    AuthService->>Security: Authenticate credentials
    Security->>DB: Find user by email
    DB-->>Security: User + BCrypt hash + roles
    Security->>Security: Verify password
    Security-->>AuthService: Authentication successful
    AuthService->>AuthService: Generate JWT
    AuthService-->>AuthController: AuthResponse
    AuthController-->>Client: JWT Access Token