# Enterprise SaaS Billing & Identity Platform

A **production-ready SaaS backend** built with Spring Boot 3.4.5 and Java 21.
Covers JWT authentication, subscription billing, plan-based access control,
admin management, Docker, and CI/CD — designed for real-world backend engineering.

[![CI/CD Pipeline](https://github.com/abdessalems/Enterprise-SaaS-Billing-Identity-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/abdessalems/Enterprise-SaaS-Billing-Identity-Platform/actions/workflows/ci.yml)

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Phases Roadmap](#phases-roadmap)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [API Reference](#api-reference)
- [Authentication Flow](#authentication-flow)
- [Docker](#docker)
- [CI/CD Pipeline](#cicd-pipeline)
- [Database](#database)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.5 |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| AOP | Spring AOP — plan-based access control |
| Persistence | Spring Data JPA + Hibernate 6 |
| Database | PostgreSQL 16 |
| Connection Pool | HikariCP |
| Validation | Jakarta Bean Validation |
| API Docs | SpringDoc OpenAPI 2.8.9 (Swagger UI) |
| Build Tool | Maven (mvnw wrapper) |
| Boilerplate | Lombok |
| Containerization | Docker + Docker Compose |
| CI/CD | GitHub Actions |

---

## Project Structure

```
src/main/java/com/saas/
├── EnterpriseSaaSApplication.java
│
├── annotation/
│   └── RequiresPlan.java            ← Custom annotation for plan gating
│
├── aspect/
│   └── PlanAccessAspect.java        ← AOP: enforces @RequiresPlan at runtime
│
├── config/
│   ├── AppConfig.java               ← AuthProvider, PasswordEncoder, @EnableScheduling
│   └── SwaggerConfig.java           ← OpenAPI / Swagger UI
│
├── controller/
│   ├── AuthController.java          ← POST /auth/register, POST /auth/login
│   ├── SubscriptionController.java  ← Subscription CRUD endpoints
│   ├── DashboardController.java     ← Plan-gated dashboard endpoints
│   └── AdminController.java        ← Admin-only user & subscription management
│
├── dto/
│   ├── ApiResponse.java
│   ├── auth/                        ← RegisterRequest, LoginRequest, AuthResponse
│   ├── subscription/                ← SubscriptionRequest, SubscriptionResponse
│   └── admin/                       ← AdminUserResponse, AdminSubscriptionResponse
│
├── entity/
│   ├── User.java                    ← JPA entity + UserDetails
│   ├── Role.java                    ← USER / ADMIN / MANAGER
│   ├── Subscription.java            ← Subscription entity
│   ├── Plan.java                    ← FREE / PRO / ENTERPRISE enum
│   └── SubscriptionStatus.java     ← ACTIVE / EXPIRED / CANCELLED
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ErrorResponse.java
│   ├── ResourceNotFoundException.java
│   ├── EmailAlreadyExistsException.java
│   └── PlanAccessException.java    ← 403 when plan is insufficient
│
├── repository/
│   ├── UserRepository.java
│   └── SubscriptionRepository.java ← includes bulk expiry update query
│
├── scheduler/
│   └── SubscriptionExpiryScheduler.java ← Daily job: marks expired subscriptions
│
├── security/
│   ├── SecurityConfig.java          ← @EnableMethodSecurity, JWT filter chain
│   ├── JwtUtils.java
│   ├── JwtAuthFilter.java
│   └── UserDetailsServiceImpl.java
│
└── service/
    ├── AuthService.java
    ├── SubscriptionService.java
    └── AdminService.java
```

---

## Phases Roadmap

| Phase | Feature | Status |
|---|---|---|
| 0 | Project setup — Docker, package structure, error handling | ✅ Done |
| 1 | JWT Authentication — register, login, stateless security | ✅ Done |
| 1.1 | Swagger UI / OpenAPI documentation | ✅ Done |
| 2 | Subscription System — plans, billing, lifecycle | ✅ Done |
| 3 | Business Rules — AOP plan gating, expiry scheduler | ✅ Done |
| 4 | Admin Module — user management, subscription overview | ✅ Done |
| 5 | Docker + GitHub Actions CI/CD pipeline | ✅ Done |
| 6 | Usage tracking + metered billing | 🔜 Planned |
| 7 | Event-driven architecture (Kafka / RabbitMQ) | 🔜 Planned |

---

## Getting Started

### Prerequisites

- Java 21
- Docker Desktop

### 1. Clone the repository

```bash
git clone https://github.com/abdessalems/Enterprise-SaaS-Billing-Identity-Platform.git
cd Enterprise-SaaS-Billing-Identity-Platform
```

### 2. Configure environment

```bash
cp "Enterprise SaaS-Billing-Identity-Platform/.env.example" "Enterprise SaaS-Billing-Identity-Platform/.env"
```

Edit `.env` and fill in your values (see [Environment Variables](#environment-variables)).

### 3. Start everything with Docker Compose

```bash
cd "Enterprise SaaS-Billing-Identity-Platform"
docker compose up -d
```

This starts three containers:
- **saas-postgres** — PostgreSQL 16 on port `5432`
- **saas-app** — Spring Boot app on port `8080`
- **saas-pgadmin** — pgAdmin UI on port `5050`

### 4. Open Swagger UI

```
http://localhost:8080/api/v1/swagger-ui/index.html
```

### Run locally without Docker

```bash
cd "Enterprise SaaS-Billing-Identity-Platform"

# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Requires a local PostgreSQL instance with database `saas_db`.

---

## Environment Variables

Copy `.env.example` to `.env` and set:

| Variable | Description |
|---|---|
| `DB_HOST` | PostgreSQL host (`postgres` for Docker, `localhost` for local) |
| `DB_NAME` | Database name (default: `saas_db`) |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | 256-bit hex secret — generate with `openssl rand -hex 32` |

---

## API Reference

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | None | Create account, returns JWT |
| `POST` | `/api/v1/auth/login` | None | Login, returns JWT |

### Subscriptions

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/subscriptions` | JWT | Create subscription |
| `GET` | `/api/v1/subscriptions/my` | JWT | Get current user's subscription |
| `PUT` | `/api/v1/subscriptions/{id}` | JWT | Update subscription |
| `DELETE` | `/api/v1/subscriptions/{id}` | JWT | Cancel subscription |

### Dashboard (plan-gated)

| Method | Endpoint | Minimum Plan | Description |
|---|---|---|---|
| `GET` | `/api/v1/dashboard/summary` | FREE | Basic summary |
| `GET` | `/api/v1/dashboard/reports` | PRO | Advanced reports |
| `GET` | `/api/v1/dashboard/analytics` | ENTERPRISE | Full analytics |

### Admin (role: ADMIN only)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/admin/users` | List all users with their active plan |
| `GET` | `/api/v1/admin/subscriptions` | List all subscriptions with user info |
| `POST` | `/api/v1/admin/disable-user/{id}` | Disable a user account |

### How to authenticate in Swagger UI

1. Call `POST /auth/register` or `POST /auth/login`
2. Copy the `accessToken` from the response
3. Click **Authorize** (lock icon, top right)
4. Paste your token and click **Authorize**

---

## Authentication Flow

```
Client                          Server
  │                               │
  │── POST /auth/register ────────►│  Validate → BCrypt(12) → Save
  │◄── 201 { accessToken } ───────│  Generate JWT (24h)
  │                               │
  │── POST /auth/login ───────────►│  Authenticate → Verify hash
  │◄── 200 { accessToken } ───────│  Generate JWT (24h)
  │                               │
  │── GET /dashboard/reports ─────►│  JwtAuthFilter → @RequiresPlan(PRO)
  │   Authorization: Bearer <jwt> │  PlanAccessAspect checks subscription
  │◄── 200 / 403 ─────────────────│
```

---

## Docker

### Build the image locally

```bash
cd "Enterprise SaaS-Billing-Identity-Platform"
docker build -t enterprise-saas-platform .
```

### Run with Docker Compose

```bash
docker compose up -d        # start all services
docker compose logs -f app  # follow app logs
docker compose down         # stop all services
```

### Published image

The latest image is automatically published to Docker Hub on every push to `main`:

```bash
docker pull bganda47/enterprise-saas-platform:latest
```

---

## CI/CD Pipeline

GitHub Actions runs on every push to `main` or `develop`.

**Job 1 — Build & Test**
- Starts a PostgreSQL 16 service container
- Runs `mvn clean verify` (build + all tests)
- Uploads Surefire test reports on failure

**Job 2 — Docker Build & Push** *(main branch only)*
- Builds the Docker image
- Pushes to Docker Hub as:
  - `bganda47/enterprise-saas-platform:latest`
  - `bganda47/enterprise-saas-platform:<commit-sha>`

**Required GitHub secrets:**

| Secret | Description |
|---|---|
| `JWT_SECRET` | JWT signing key used during tests |
| `DOCKERHUB_TOKEN` | Docker Hub personal access token |

---

## Database

### pgAdmin

Available at `http://localhost:5050` when using Docker Compose.
- Email: `admin@saas.com`
- Password: `admin`

Connect to the server:
- Host: `postgres`
- Port: `5432`
- Database: `saas_db`
- Username / Password: from your `.env`

### Key tables (auto-created by Hibernate)

| Table | Description |
|---|---|
| `users` | Registered users with roles and status |
| `subscriptions` | User subscriptions with plan, dates, and status |

### Expiry Scheduler

A scheduled job runs daily at midnight and automatically sets all subscriptions
with `endDate < today` and status `ACTIVE` to `EXPIRED`.

---

*Built with Spring Boot 3.4.5 · Java 21 · PostgreSQL 16*
