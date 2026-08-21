# Restaurant Management

Hệ thống quản lý nhà hàng (đặt bàn, thực đơn, đơn hàng, ca làm, thanh toán CASH / VNPAY) — **backend** Spring Boot multi-module và **frontend** Next.js.

**Tác giả:** HUY K3 · **Backend version:** `4.0.1`

---

## Tính năng chính

| Khu vực | Mô tả |
|--------|--------|
| **Người dùng** | Đăng ký (email verify), đăng nhập JWT, phân quyền RBAC |
| **Thực đơn** | Category, MenuItem (filter / phân trang) |
| **Bàn & đặt bàn** | Table status, Reservation + kiểm tra availability theo slot |
| **Đơn hàng** | Order / OrderItem — `DINE_IN`, `DELIVERY`; scheduler hết hạn PENDING / CONFIRMED chưa thanh toán |
| **Ca làm** | Shift cho nhân viên |
| **Thanh toán** | CASH tại quầy; VNPAY (sandbox/prod) qua `/payments/vnpay/*` |
| **Staff UI** | Trang quản trị trong Next.js (`/staff/...`) theo role |

### Vai trò (`UserRole`)

`ADMIN` · `MANAGER` · `CASHIER` · `CUSTOMER`

### Trạng thái đơn (`OrderStatus`)

`PENDING` → `CONFIRMED` → `PREPARING` → `COMPLETED` (hoặc `CANCELLED`)

---

## Tech stack

### Backend

- Java **17**, Spring Boot **3.5**, Maven multi-module
- Spring Web, Data JPA, Validation, Mail, AOP, Security (JWT)
- PostgreSQL + **Flyway**
- Redis (cache / blacklist token)
- Resilience4j (rate limiter, circuit breaker)
- Kafka (tùy chọn; tắt trên profile `prod` mặc định)
- Lombok, ModelMapper, JJWT

### Frontend

- **Next.js 16** (App Router), React 19, TypeScript, Tailwind CSS 4
- Proxy API: `/api/proxy/*` → `BACKEND_URL` (xem `frontend/next.config.ts`)

---

## Cấu trúc thư mục

```
Restaurant_Management/
├── backend/                 # Maven reactor
│   ├── app/                 # Spring Boot app chính (WAR executable) — chạy API
│   ├── common/              # Entities, models, enums, Flyway migrations, i18n
│   ├── security/            # JWT filter, SecurityConfig, blacklist
│   ├── handle_exceptions/   # Exception handlers dùng chung
│   ├── logging/             # Logging module
│   ├── app-reactive/        # Thử nghiệm WebFlux (phụ, không phải entry chính)
│   ├── docs/                # Roadmap, DB design, VNPAY
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                # Next.js customer + staff UI
├── ENTITIES_DESIGN.txt      # Bản thiết kế entity (tham khảo)
└── README.md
```

### Module backend

| Module | Vai trò |
|--------|---------|
| `app` | Controllers, services, schedulers — **entrypoint** |
| `common` | Domain shared + `db/migration` |
| `security` | JWT + method security (`@PreAuthorize`) |
| `handle_exceptions` | Lỗi nghiệp vụ / HTTP thống nhất |
| `logging` | Cấu hình log |
| `app-reactive` | Prototype reactive (không dùng cho production chính) |

Luồng code: **Controller → Service → Repository**.

---

## Yêu cầu môi trường

| Thành phần | Gợi ý |
|------------|--------|
| JDK | 17+ |
| Maven | 3.9+ (hoặc dùng `backend/mvnw`) |
| Node.js | 20+ (khuyến nghị) |
| pnpm / npm | Frontend |
| PostgreSQL | Local (mặc định JDBC port `5435`, DB `restaurent-management`) |
| Redis | Local `6379` (token / cache) |
| Kafka | Tùy chọn — có thể tắt bằng `KAFKA_ENABLED=false` |

---

## Chạy nhanh (local)

### 1. Database & Redis

Đảm bảo PostgreSQL và Redis đang chạy. JDBC mặc định:

```text
jdbc:postgresql://localhost:5435/restaurent-management
user/password: postgres / postgres
```

Flyway chạy migration khi start app (`spring.jpa.hibernate.ddl-auto=validate`).

### 2. Backend

Tạo file bí mật local (đã gitignore), ví dụ `backend/app/src/main/resources/application-local.properties`:

```properties
jwt.secret=your-long-random-secret-at-least-32-chars
# MAIL_USERNAME / MAIL_PASSWORD nếu cần verify email
# VNPAY_TMN_CODE / VNPAY_HASH_SECRET nếu test cổng thanh toán
```

Hoặc set biến môi trường tương đương (`JWT_SECRET`, `DB_*`, `REDIS_*`, …).

```bash
cd backend
./mvnw -pl app -am spring-boot:run
# Windows: mvnw.cmd -pl app -am spring-boot:run
```

API mặc định: [http://localhost:8080](http://localhost:8080)

### 3. Frontend

```bash
cd frontend
cp .env.example .env.local
# BACKEND_URL=http://localhost:8080

pnpm install   # hoặc npm install
pnpm dev       # hoặc npm run dev
```

UI: [http://localhost:3000](http://localhost:3000)

Trình duyệt gọi `/api/proxy/...`; Next rewrite sang backend.

---

## Biến môi trường quan trọng (backend)

| Biến | Mặc định / ghi chú |
|------|---------------------|
| `PORT` | `8080` |
| `DB_JDBC_URL` | `jdbc:postgresql://localhost:5435/restaurent-management` |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` |
| `JWT_SECRET` | Bắt buộc (hoặc `jwt.secret` trong local props) |
| `JWT_EXPIRATION` | `3600000` (ms) |
| `APP_FRONTEND_URL` | `http://localhost:3000` (link verify email) |
| `MAIL_*` | SMTP (Gmail…); để trống nếu chưa dùng register verify |
| `VNPAY_*` | TMN code, hash secret, return/IPN URL — xem docs VNPAY |
| `KAFKA_ENABLED` | `true` local; profile `prod` tắt listener |
| `ORDER_PENDING_EXPIRY_MINUTES` | `5` |
| `ORDER_CONFIRMED_UNPAID_EXPIRY_MINUTES` | `30` |

Profile production: `SPRING_PROFILES_ACTIVE=prod` (Dockerfile đã set).

---

## API overview

Base URL: `http://localhost:8080`

### Public (không JWT)

| Method | Path |
|--------|------|
| `POST` | `/users/register` |
| `POST` | `/users/login` |
| `PUT` | `/users/public/verify` |
| `POST` | `/users/public/resendVerificationToken` |
| `GET` | `/menu-items/filters` |
| `GET` | `/payments/vnpay/return` |
| `GET`/`POST` | `/payments/vnpay/ipn` |

Các endpoint khác cần header `Authorization: Bearer <accessToken>`.

### Nhóm resource chính

| Prefix | Domain |
|--------|--------|
| `/users` | Auth, profile, admin filter users |
| `/categories` | Danh mục |
| `/menu-items` | Món |
| `/tables` | Bàn |
| `/reservations` | Đặt bàn (customer + admin) |
| `/orders` | Đơn hàng |
| `/order-items` | Dòng món trong đơn |
| `/shifts` | Ca làm |
| `/payments` | Thanh toán CASH / quản lý |
| `/payments/vnpay` | Khởi tạo & callback VNPAY |

Chi tiết quyền theo `@PreAuthorize` trong từng controller; luồng VNPAY: [`backend/docs/PAYMENT_VNPAY.md`](backend/docs/PAYMENT_VNPAY.md).

---

## Frontend routes (tóm tắt)

| Path | Mục đích |
|------|----------|
| `/`, `/menu`, `/about` | Landing / thực đơn |
| `/login`, `/register`, `/verify` | Auth |
| `/cart`, `/checkout`, `/orders` | Giỏ & đơn khách |
| `/reservations` | Đặt bàn khách |
| `/payment/vnpay-return` | Redirect sau VNPAY |
| `/account` | Tài khoản khách |
| `/staff/*` | Bảng điều khiển nhân viên (users, tables, orders, payments, …) |

---

## Docker (backend)

Build context **phải** là thư mục `backend/`:

```bash
cd backend
docker build -t restaurant-management-api .
docker run --rm -p 8080:8080 \
  -e DB_JDBC_URL=... \
  -e DB_USERNAME=... \
  -e DB_PASSWORD=... \
  -e JWT_SECRET=... \
  -e REDIS_HOST=... \
  restaurant-management-api
```

Image chạy WAR executable với profile `prod`, port `8080`.

---

## Tài liệu thêm

| Tài liệu | Nội dung |
|----------|----------|
| [`backend/docs/Roadmap.md`](backend/docs/Roadmap.md) | Tiến độ module API đã test |
| [`backend/docs/database/00_README.md`](backend/docs/database/00_README.md) | Thiết kế DB theo domain |
| [`backend/docs/PAYMENT_VNPAY.md`](backend/docs/PAYMENT_VNPAY.md) | Thanh toán VNPAY |
| [`backend/docs/PAYMENT_HELPER_METHODS.md`](backend/docs/PAYMENT_HELPER_METHODS.md) | Helper payment |
| [`ENTITIES_DESIGN.txt`](ENTITIES_DESIGN.txt) | Bản thiết kế entity gốc |

---

## Ghi chú phát triển

- **Không commit** `application-local.properties`, `.env`, secret VNPAY / JWT.
- Hard delete hạn chế — ưu tiên đổi `status` / lifecycle.
- Filter API dùng `@RequestParam` + `Pageable`, không bọc `Map` tùy tiện.
- `app-reactive` là module phụ; phát triển nghiệp vụ chính trên `backend/app` + `common`.

---

## License

Private / học tập — theo chính sách của tác giả dự án.
