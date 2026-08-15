# PayShield AI — Authentication API

Authentication and authorization APIs for the PayShield AI payment fraud detection platform.

Base URL:

```text
http://localhost:8080/api/v1

1. Register User

Creates a new PayShield AI user.
A wallet is automatically created for the user after successful registration.

curl -X POST http://localhost:8080/api/v1/auth/register \
-H "Content-Type: application/json" \
-d '{
  "name": "Mohini",
  "email": "mohini@example.com",
  "password": "Password@123"
}'

2. Login

Authenticates an existing user and generates a JWT.

curl -X POST http://localhost:8080/api/v1/auth/login \
-H "Content-Type: application/json" \
-d '{
  "email": "mohini@example.com",
  "password": "Password@123"
}'

3. Using the JWT

Protected APIs require the JWT in the Authorization header.

curl http://localhost:8080/api/v1/wallet \
-H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

