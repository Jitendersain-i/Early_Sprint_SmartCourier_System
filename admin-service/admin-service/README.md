# 🛡️ Admin Service — README

Port: **8084** | DB Schema: `admin_user`

> All endpoints require a JWT with **ROLE_ADMIN**.
> The service verifies this by calling Auth Service via Feign on every request.

---

## Running via CMD

```bash
cd admin-service
mvn clean package -DskipTests
java -jar target/admin-service-1.0.0.jar
```

### With custom URLs
```bash
java -jar target/admin-service-1.0.0.jar \
  --spring.datasource.url=jdbc:oracle:thin:@localhost:1521/XEPDB1 \
  --spring.datasource.username=admin_user \
  --spring.datasource.password=admin_pass123 \
  --feign.auth-service.url=http://localhost:8081 \
  --feign.delivery-service.url=http://localhost:8082 \
  --feign.tracking-service.url=http://localhost:8083
```

### Health check
```bash
curl http://localhost:8084/actuator/health
```

---

## 🔗 Feign Clients (Inter-Service)

| Target           | When                             | Feign Interface           |
|------------------|----------------------------------|---------------------------|
| Auth Service     | Every request (admin role check) | `AuthServiceClient`       |
| Delivery Service | GET /admin/deliveries/{id}       | `DeliveryServiceClient`   |
| Tracking Service | GET/POST tracking + status       | `TrackingServiceClient`   |

---

## 📬 Postman API Test Data

> First, login as an **admin user** (registered with `"role": "ROLE_ADMIN"`):
>
> POST `http://localhost:8080/api/auth/login`
> `{ "username": "admin_raj", "password": "admin123" }`
>
> Copy the token and use it for all requests below.

---

### 1. Get Dashboard

**GET** `http://localhost:8080/api/admin/dashboard`

Headers:
```
Authorization: Bearer {{adminToken}}
```

Expected Response (200):
```json
{
  "totalDeliveries": 0,
  "activeDeliveries": 0,
  "totalHubs": 0,
  "totalRevenue": 0,
  "deliveriesByStatus": [
    { "status": "BOOKED",     "count": 0 },
    { "status": "IN_TRANSIT", "count": 0 },
    { "status": "DELIVERED",  "count": 0 }
  ]
}
```

---

### 2. Get Any Delivery (Feign → Delivery Service)

**GET** `http://localhost:8080/api/admin/deliveries/1`

Headers:
```
Authorization: Bearer {{adminToken}}
```

---

### 3. Get Tracking for Any Delivery (Feign → Tracking Service)

**GET** `http://localhost:8080/api/admin/tracking/1`

Headers:
```
Authorization: Bearer {{adminToken}}
```

---

### 4. Update Delivery Status (Feign → Tracking Service)

**POST** `http://localhost:8080/api/admin/deliveries/status`

Headers:
```
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

Body — mark as delayed:
```json
{
  "deliveryId": 1,
  "status": "DELAYED",
  "location": "Pune Sorting Facility",
  "notes": "Delayed due to weather conditions. Expected next day."
}
```

Body — mark as returned:
```json
{
  "deliveryId": 1,
  "status": "RETURNED",
  "location": "Bengaluru Origin Hub",
  "notes": "Recipient not available after 3 attempts."
}
```

---

### 5. Create a Hub

**POST** `http://localhost:8080/api/admin/hubs`

Headers:
```
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

Body:
```json
{
  "name": "Bengaluru Central Hub",
  "address": "Plot 45, Electronic City Phase 1",
  "city": "Bengaluru",
  "state": "Karnataka",
  "country": "India"
}
```

Expected Response (201):
```json
{
  "id": 1,
  "name": "Bengaluru Central Hub",
  "address": "Plot 45, Electronic City Phase 1",
  "city": "Bengaluru",
  "state": "Karnataka",
  "country": "India",
  "isActive": true,
  "createdAt": "2024-12-25T09:00:00"
}
```

---

### 6. Create More Hubs
```json
{ "name": "Mumbai Airport Hub",  "address": "CSIA Cargo Terminal", "city": "Mumbai",  "state": "Maharashtra", "country": "India" }
{ "name": "Delhi NCR Hub",       "address": "Kundli Industrial Area", "city": "Delhi", "state": "Delhi",       "country": "India" }
{ "name": "Chennai South Hub",   "address": "Sholinganallur IT Park", "city": "Chennai","state": "Tamil Nadu", "country": "India" }
```

---

### 7. Get All Hubs

**GET** `http://localhost:8080/api/admin/hubs`

Headers:
```
Authorization: Bearer {{adminToken}}
```

---

### 8. Revenue Report

**POST** `http://localhost:8080/api/admin/reports/revenue?period=monthly`

Headers:
```
Authorization: Bearer {{adminToken}}
```

---

### 9. Delivery Report

**POST** `http://localhost:8080/api/admin/reports/deliveries?period=weekly`

Headers:
```
Authorization: Bearer {{adminToken}}
```

---

### ❌ Error Cases to Test

**Non-admin JWT accessing admin routes:**
→ Login as `john_doe` (ROLE_USER), use that token
→ GET /api/admin/dashboard
→ 403: `{ "error": "Access denied: ROLE_ADMIN required" }`

**Duplicate hub name:**
→ POST /api/admin/hubs with same name twice
→ 400: `{ "error": "Hub with name already exists: Bengaluru Central Hub" }`

---

## 🗄️ Database Tables Created (auto via JPA)

- `HUBS` — dispatch hub locations
