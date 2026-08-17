# Authentication API

Base path: `/api/v1/auth`  
Authentication: **None required** (public endpoints)

---

## Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a new user account |
| `POST` | `/api/v1/auth/login` | Log in and receive a JWT |

---

## POST `/api/v1/auth/register`

Creates a new user account and returns a JWT token.

### Request Body

```json
{
  "name": "Mohin Deshmukh",
  "email": "mohin@example.com",
  "password": "securePassword123"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | `string` | ✅ | Full name of the user |
| `email` | `string` | ✅ | Unique email address |
| `password` | `string` | ✅ | Plaintext password (hashed with BCrypt on server) |

### Response — `201 Created`

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400000
  }
}
```

| Field | Type | Description |
|---|---|---|
| `token` | `string` | JWT bearer token |
| `expiresIn` | `long` | Token expiry in milliseconds (default 24 h) |

### Error Responses

| Status | Scenario |
|---|---|
| `400 Bad Request` | Missing or invalid fields |
| `409 Conflict` | Email already registered |

### cURL Example

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mohin Deshmukh",
    "email": "mohin@example.com",
    "password": "securePassword123"
  }'
```

---

## POST `/api/v1/auth/login`

Authenticates an existing user and returns a JWT token.

### Request Body

```json
{
  "email": "mohin@example.com",
  "password": "securePassword123"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `email` | `string` | ✅ | Registered email address |
| `password` | `string` | ✅ | Plaintext password |

### Response — `200 OK`

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400000
  }
}
```

### Error Responses

| Status | Scenario |
|---|---|
| `400 Bad Request` | Missing fields |
| `401 Unauthorized` | Invalid credentials |

### cURL Example

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "mohin@example.com",
    "password": "securePassword123"
  }'
```

---

## Using the Token

Include the JWT in the `Authorization` header for all protected endpoints:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Tokens are **stateless** (no server-side session) and expire after the configured TTL (default: 24 hours).

---

## Response Wrapper

All endpoints return responses in the standard `ApiResponse<T>` wrapper:

```json
{
  "success": true,
  "message": "...",
  "data": { ... }
}
```
