# 🌐 API Gateway — README

Port: **8080** | Entry point for ALL client requests

---

## What It Does

- Routes requests to the correct microservice
- Validates JWT on all protected routes
- Injects `X-User-Name` and `X-User-Role` headers downstream
- Handles CORS for browser clients

---

## Running via CMD

```bash
cd api-gateway
mvn clean package -DskipTests
java -jar target/api-gateway-1.0.0.jar
```

### With custom service URLs
```bash
java -jar target/api-gateway-1.0.0.jar \
  --AUTH_SERVICE_URL=http://localhost:8081 \
  --DELIVERY_SERVICE_URL=http://localhost:8082 \
  --TRACKING_SERVICE_URL=http://localhost:8083 \
  --ADMIN_SERVICE_URL=http://localhost:8084
```

---

## Routing Table

| Path Pattern              | Upstream Service  | Auth Required |
|---------------------------|-------------------|---------------|
| /api/auth/signup          | Auth :8081        | No            |
| /api/auth/login           | Auth :8081        | No            |
| /api/auth/profile         | Auth :8081        | JWT           |
| /api/deliveries/**        | Delivery :8082    | JWT           |
| GET /api/tracking/{id}    | Tracking :8083    | No            |
| /api/tracking/**          | Tracking :8083    | JWT           |
| /api/admin/**             | Admin :8084       | JWT           |

---

## How JWT Auth Works

```
Client → Gateway (validates JWT) → adds X-User-Name + X-User-Role headers → Microservice
```

The downstream services **trust** these headers and never re-validate the JWT themselves.

---

## Health Check

```bash
curl http://localhost:8080/actuator/health
```

---

## Common Errors

| Code | Meaning                                      |
|------|----------------------------------------------|
| 401  | Missing or invalid JWT token                 |
| 403  | Valid JWT but ROLE_ADMIN required            |
| 503  | Downstream service is down                  |
