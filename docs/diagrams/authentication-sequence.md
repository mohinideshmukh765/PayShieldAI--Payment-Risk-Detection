# PayShield AI — JWT Authentication Sequence

This diagram covers both the **registration** and **login** flows that produce a JWT, and how that token is used on subsequent requests.

---

## Registration Flow

When a new user registers, the backend:
1. Validates the request
2. Hashes the password with BCrypt (cost factor 12)
3. Creates the `users` record and a linked `wallets` record (zero balance)
4. Assigns `ROLE_USER` by default
5. Generates and returns a JWT immediately — no separate login step required

---

## Authentication Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant AuthController
    participant AuthService
    participant Security as Spring Security
    participant JwtService
    participant DB as PostgreSQL

    Note over Client,DB: --- Registration ---
    Client->>AuthController: POST /api/v1/auth/register {name, email, password}
    AuthController->>AuthService: register(RegisterRequest)
    AuthService->>DB: Check email uniqueness
    AuthService->>Security: BCryptPasswordEncoder.encode(password)
    Security-->>AuthService: passwordHash
    AuthService->>DB: INSERT users (name, email, passwordHash, status=ACTIVE)
    AuthService->>DB: INSERT wallets (userId, balance=0, currency=INR)
    AuthService->>DB: Assign ROLE_USER
    AuthService->>JwtService: generateToken(userDetails)
    JwtService-->>AuthService: Signed JWT (HMAC-SHA, 24h TTL)
    AuthService-->>AuthController: AuthResponse {token, expiresIn}
    AuthController-->>Client: 201 Created {token, expiresIn}

    Note over Client,DB: --- Login ---
    Client->>AuthController: POST /api/v1/auth/login {email, password}
    AuthController->>AuthService: login(LoginRequest)
    AuthService->>Security: authenticate(email, password)
    Security->>DB: SELECT user WHERE email = ?
    DB-->>Security: User + BCrypt hash + roles
    Security->>Security: BCrypt.checkpw(password, hash)
    Security-->>AuthService: Authentication object
    AuthService->>JwtService: generateToken(userDetails)
    JwtService-->>AuthService: Signed JWT
    AuthService-->>AuthController: AuthResponse {token, expiresIn}
    AuthController-->>Client: 200 OK {token, expiresIn}
```

---

## JWT Structure

The JWT is signed with **HMAC-SHA** using a secret key from `application.properties`.

**Claims:**
- `sub` — user email (used as username throughout the system)
- `iat` — issued-at timestamp
- `exp` — expiry timestamp (default: 24 hours after issuance)

---

## Authenticated Request Flow

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as JwtAuthenticationFilter
    participant JwtService
    participant Security as SecurityContext
    participant Controller
    participant Service

    Client->>Filter: Request + "Authorization: Bearer <JWT>"
    Filter->>JwtService: extractUsername(token)
    JwtService-->>Filter: email (subject claim)
    Filter->>Filter: Load UserDetails from DB
    Filter->>JwtService: isTokenValid(token, userDetails)
    JwtService-->>Filter: true / false
    alt Token valid
        Filter->>Security: Set UsernamePasswordAuthenticationToken
        Filter->>Controller: Continue chain
        Controller->>Service: Business operation
        Service-->>Controller: Result
        Controller-->>Client: 200 OK Response
    else Token invalid or expired
        Filter-->>Client: 401 Unauthorized
    end
```

---

## Token Expiry & Security Notes

| Property | Value |
|---|---|
| Algorithm | HMAC-SHA (key-length dependent) |
| Default TTL | 24 hours (configurable via `jwt.expiration`) |
| Subject claim | User email |
| Session type | **Stateless** — no server-side session |
| Password hashing | BCrypt, cost factor 12 |

There is no token refresh endpoint — users must re-login after expiry.