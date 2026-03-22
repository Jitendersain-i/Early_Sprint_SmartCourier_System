# 🔐 Auth Service — README

Port: **8081** | DB Schema: `auth_user`

---

## Running via CMD

```bash
cd auth-service
mvn clean package -DskipTests
java -jar target/auth-service-1.0.0.jar
```

### With custom Oracle URL
```bash
java -jar target/auth-service-1.0.0.jar \
  --spring.datasource.url=jdbc:oracle:thin:@localhost:1521/XEPDB1 \
  --spring.datasource.username=auth_user \
  --spring.datasource.password=auth_pass123
```

### Health check
```bash
curl http://localhost:8081/actuator/health
```

---

## 📬 Postman API Test Data

### 1. Register a Normal User

**POST** `http://localhost:8080/api/auth/signup`

Headers:
```
Content-Type: application/json
```

Body:
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123",
  "fullName": "John Doe",
  "phoneNumber": "+91-9876543210",
  "role": "ROLE_USER"
}
```

Expected Response (200):
```json
{
  "message": "User registered successfully!"
}
```

---

### 2. Register an Admin User

**POST** `http://localhost:8080/api/auth/signup`

Body:
```json
{
  "username": "admin_raj",
  "email": "admin@smartcourier.com",
  "password": "admin123",
  "fullName": "Raj Admin",
  "phoneNumber": "+91-9000000001",
  "role": "ROLE_ADMIN"
}
```

---

### 3. Login (Get JWT Token)

**POST** `http://localhost:8080/api/auth/login`

Body:
```json
{
  "username": "john_doe",
  "password": "password123"
}
```

Expected Response (200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "role": "ROLE_USER"
}
```

> ✅ **Copy the `token` value** — you need it for all protected endpoints.
> In Postman: go to the collection → Authorization tab → Bearer Token → paste it.

---

### 4. Get Profile (Protected)

**GET** `http://localhost:8080/api/auth/profile`

Headers:
```
Authorization: Bearer <YOUR_TOKEN_HERE>
```

Expected Response (200):
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "phoneNumber": "+91-9876543210",
  "role": "ROLE_USER",
  "isActive": true
}
```

---

### ❌ Error Cases to Test

**Duplicate username:**
```json
{ "username": "john_doe", "email": "other@x.com", "password": "abc123" }
```
→ 400: `{ "error": "Username is already taken: john_doe" }`

**Wrong password login:**
```json
{ "username": "john_doe", "password": "wrongpass" }
```
→ 401 Unauthorized

**Missing JWT on protected route:**
→ GET /api/auth/profile with no token
→ 401 Unauthorized from Gateway

---

## 🗄️ Database Tables Created (auto via JPA)

- `USERS` — user accounts
- `ROLES` — ROLE_USER, ROLE_ADMIN, ROLE_DRIVER
- `USER_ROLES` — junction table (M:N)
