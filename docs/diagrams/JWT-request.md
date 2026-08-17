# PayShield AI — JWT Request Lifecycle

This document shows how every protected API request flows through the `JwtAuthenticationFilter` and the Spring Security context.

---

## How JWT Authentication Works

Every request to a protected endpoint must include:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

The `JwtAuthenticationFilter` intercepts all requests **before** they reach a controller.

---

## Request Flow Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as JwtAuthenticationFilter
    participant JwtService
    participant UDS as CustomUserDetailsService
    participant Security as SecurityContextHolder
    participant Controller
    participant Service

    Client->>Filter: HTTP Request + Authorization: Bearer <JWT>
    Filter->>Filter: Extract token from "Bearer " prefix
    Filter->>JwtService: extractUsername(token)
    JwtService-->>Filter: email (subject)
    Filter->>UDS: loadUserByUsername(email)
    UDS-->>Filter: UserDetails (email + roles)
    Filter->>JwtService: isTokenValid(token, userDetails)
    JwtService-->>Filter: true (valid & not expired)
    Filter->>Security: Set UsernamePasswordAuthenticationToken(userDetails, roles)
    Filter->>Controller: Forward request
    Controller->>Service: Execute business logic
    Service-->>Controller: Response data
    Controller-->>Client: HTTP Response
```

---

## Security Context Population

Once the filter validates the token, it sets a `UsernamePasswordAuthenticationToken` into the `SecurityContextHolder`. This allows any downstream code to call:

```java
// In a controller:
@AuthenticationPrincipal UserDetails userDetails
// → userDetails.getUsername() returns the user's email

// In a service:
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String email = authentication.getName();
```

---

## Role Enforcement

Route-level role restrictions are configured in `SecurityConfig`:

| Route Pattern | Required Role |
|---|---|
| `/api/v1/auth/**` | None (public) |
| `/api/v1/fraud/**` | `ROLE_ANALYST` |
| `/api/v1/analytics/**` | `ROLE_ANALYST` |
| All other `/api/v1/**` | Authenticated (any role) |

Method-level security uses `@PreAuthorize`:
```java
@PreAuthorize("hasRole('ANALYST')")
public FraudRuleEvaluationResponse evaluate(...) { ... }
```

---

## Token Validation Logic

`JwtService.isTokenValid()` performs two checks:

1. **Subject match** — `token.subject == userDetails.username`
2. **Not expired** — `token.expiry > now`

```java
public boolean isTokenValid(String token, UserDetails userDetails) {
    String username = extractUsername(token);
    return username.equals(userDetails.getUsername())
        && !isTokenExpired(token);
}
```