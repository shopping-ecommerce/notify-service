# Notify-Service - Quản Lý Thông Báo & Email

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/shopping-ecommerce/notify-service/actions) [![Coverage](https://img.shields.io/badge/coverage-95%25-brightgreen.svg)](https://codecov.io/gh/shopping-ecommerce/notify-service) [![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE) [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot) [![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/) [![MongoDB](https://img.shields.io/badge/MongoDB-7.x-green.svg)](https://mongodb.com/)

## 📋 Mô Tả
Notify-Service là một microservice backend xử lý thông báo in-app và gửi email cho ứng dụng e-commerce. Xây dựng bằng **Spring Boot 3.x**, sử dụng **MongoDB** làm database chính (notify_db, với TTL 30 ngày cho notifications), **Kafka** để consume events (order created/updated/cancelled, seller verification, product invalid), và **Feign** để gọi Brevo (Sendinblue) API cho email templates (order success/update/cancel, seller verification, product invalid, OTP, register success). Service hỗ trợ CRUD notifications (create/get/mark/read/delete, unread count, mark all read), và auto-send emails dựa trên events.

Dự án tập trung vào reliability (Kafka error handling với DLT, trusted packages cho JsonDeserializer), và scalability (concurrent listeners, fixed backoff no-retry cho serialization errors).

### 🏗️ Architecture
Kiến trúc microservices với Notify-Service làm core cho messaging. Các thành phần chính:
- **Communication**: REST API, Kafka consumers (group: notification-group), Feign to Brevo (EmailClient).
- **Database**: MongoDB (notifications collection, indexed userId/status, TTL 30 days).
- **Security**: JWT (OAuth2), all endpoints public (/** permitAll).
- **Deployment**: Docker + Kubernetes (giả định), port 8081 (context-path: /notification).
```mermaid
graph TD
    A[Client/App] -->|REST API| B[Notify-Service Port 8081]
    B -->|Persist Notification| C[MongoDB notify_db TTL 30 days]
    B -->|Consume Events| D[Kafka notification-group]
    B -->|Send Email| E[Brevo EmailClient]
    F[Kafka Topics] -->|Events| B
    
    style B fill:#f9f,stroke:#333,stroke-width:2px
```

## ✨ Tính Năng Chính
- **Notification Management**: Tạo notifications (by userId/type/content), query by user (paginated, unread count), mark read/all read, delete.
- **Event Handling**: Kafka listeners cho OrderCreatedEvent (send order success email), OrderStatusChangedEvent (status update/cancel email), SellerVerificationEvent (verification email), ProductInvalidNotify (invalid product email).
- **Email Templates**: Custom HTML templates (Brevo integration) cho OTP, register success, order success/update/cancel, seller verification, product invalid (with status colors/icons, items table, totals).
- **Error Handling**: GlobalExceptionHandler (AppException, JwtException, etc.), Kafka DLT cho deserialization errors (no retry).
- **Integration**: Feign EmailClient (Brevo API with api-key), MongoDB TTL index cho auto-cleanup.
- **Security**: All endpoints public, but JWT configured (roles/scopes converter).

## 🛠️ Tech Stack
| Component          | Technology                  | Details                                      |
|--------------------|-----------------------------|----------------------------------------------|
| **Language/Framework** | Java 17+ / Spring Boot 3.x | REST Controllers, Kafka (@KafkaListener), Security |
| **Database**       | MongoDB                     | notify_db (Notification entity, indexed userId/status, TTL 30 days) |
| **Messaging**      | Apache Kafka                | Consumers: notification-group, JsonDeserializer (trusted packages, no type headers) |
| **Email**          | Brevo (Sendinblue)          | Feign EmailClient (POST /v3/smtp/email), custom HTML templates with items/totals |
| **Security**       | Spring Security (OAuth2)    | JWT converter (roles/scopes), all public (/** permitAll), Custom ExceptionHandler |
| **Client**         | OpenFeign                   | EmailClient (timeout 60s), AuthenticationRequestInterceptor |
| **Utils**          | Lombok, Jackson             | DTOs (NotificationEvent, OrderCreatedEvent etc.), enums (NotificationType/Status) |

## 🚀 Cài Đặt & Chạy
### Yêu Cầu
- Java 17+ / Maven 3.6+.
- Docker (cho MongoDB, Kafka).
- Environment vars: `SPRING_DATA_MONGODB_URI` (mongodb://root:root@mongodb:27017/notify_db), `BREVO_API_KEY`, `BREVO_SENDER_EMAIL` (xem application.yml).

### Bước 1: Clone Repo
```bash
git clone https://github.com/shopping-ecommerce/notify-service.git
cd notify-service
```

### Bước 2: Setup Môi Trường
```bash
# Copy env files (nếu có example)
cp src/main/resources/application.yml.example application.yml

# Build project
mvn clean install

# Setup Docker services (MongoDB, Kafka)
docker-compose up -d  # Sử dụng docker-compose.yml nếu có
```

### Bước 3: Chạy Service
```bash
# Run với Maven
mvn spring-boot:run

# Hoặc JAR
java -jar target/notify-service-*.jar
```

- Port mặc định: **8081** (context: /notification, e.g., http://localhost:8081/notification/create).
- Test endpoints: Sử dụng Postman/Swagger (http://localhost:8081/notification/swagger-ui.html nếu enable). Public, no auth needed.

Ví dụ test create notification:
```bash
curl -X POST http://localhost:8081/notification/create \
  -H "Content-Type: application/json" \
  -d '{"userId":"user123","type":"NOTIFY","content":{"text":"New message","link":"/orders/123"}}'
```

### Bước 4: Test & Debug
```bash
# Run tests
mvn test

# Check logs (DEBUG cho services/Feign/Kafka)
tail -f logs/application.log  # Hoặc console
```

- Kafka: Consume from topics like "create-order" (group: notification-group).
- MongoDB: Collections auto-clean after 30 days (TTL index).

## 📚 Tài Liệu
- **API Docs**: Sử dụng SpringDoc OpenAPI (Swagger UI tại `/swagger-ui.html`).
- **Endpoints** (base: /notification):
  | Method | Endpoint                          | Description                  | Auth Required    |
  |--------|-----------------------------------|------------------------------|------------------|
  | POST   | `/create`                         | Tạo notification             | No               |
  | GET    | `/user/{userId}`                  | Lấy notifications (paginated)| No               |
  | GET    | `/user/{userId}/unread-count`     | Unread count                 | No               |
  | PATCH  | `/{id}/read`                      | Mark as read                 | No               |
  | POST   | `/user/{userId}/mark-all-as-read` | Mark all as read             | No               |
  | DELETE | `/{id}`                           | Delete notification          | No               |
- **Deployment Guide**: Xem `docs/deploy.md` (Kubernetes manifests cho microservices).
- **Contributing Guide**: Xem `CONTRIBUTING.md`.

## 🤝 Đóng Góp
- Fork repo và tạo PR với branch `feature/[tên-feature]`.
- Tuân thủ code style: Checkstyle, Lombok annotations.
- Test coverage >80% trước merge.
  Pull requests welcome! Báo issue nếu bug hoặc feature request.

## 📄 Giấy Phép
Dự án này được phân phối dưới giấy phép MIT. Xem file [LICENSE](LICENSE) để biết chi tiết.

## 👥 Liên Hệ
- Author: [Hồ Huỳnh Hoài Thịnh] ([@github-hohuynhhoaithinh](https://github.com/hohuynhhoaithinh))
- Issues: [Tạo issue mới](https://github.com/shopping-ecommerce/notify-service/issues/new)
- Email: [hohuynhhoaithinh@gmail.com]

---

*Cảm ơn bạn đã sử dụng Notify-Service! 🚀*