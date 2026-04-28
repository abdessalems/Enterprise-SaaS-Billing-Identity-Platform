# Enterprise SaaS Billing & Identity Platform

A **production-ready SaaS backend** built with Spring Boot 3.4.5 and Java 21.
Covers JWT authentication, multi-tenancy, subscription billing, usage tracking,
event-driven architecture, Docker, and CI/CD — designed for real-world backend engineering.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [API Documentation (Swagger)](#api-documentation-swagger)
- [Authentication Flow](#authentication-flow)
- [Database](#database)
- [Git Workflow](#git-workflow)
- [Phases Roadmap](#phases-roadmap)
- [Phase 0 — Project Setup](#phase-0--project-setup)
- [Phase 1 — JWT Authentication](#phase-1--jwt-authentication)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.5 |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| Persistence | Spring Data JPA + Hibernate 6 |
| Database | PostgreSQL 16 |
| Connection Pool | HikariCP |
| Validation | Jakarta Bean Validation |
| API Docs | SpringDoc OpenAPI 2.8.9 (Swagger UI) |
| Build Tool | Maven (mvnw wrapper) |
| Dev Tools | Spring Boot DevTools |
| Boilerplate | Lombok |
| Containerization | Docker + Docker Compose |

---

## Project Structure

```
src/main/java/com/saas/
├── EnterpriseSaaSApplication.java   ← Spring Boot entry point
│
├── config/
│   ├── AppConfig.java               ← AuthenticationProvider, AuthenticationManager, PasswordEncoder
│   └── SwaggerConfig.java           ← OpenAPI / Swagger UI configuration
│
├── controller/
│   └── AuthController.java          ← POST /auth/register, POST /auth/login
│
├── dto/
│   ├── ApiResponse.java             ← Generic success/error wrapper
│   └── auth/
│       ├── RegisterRequest.java     ← Registration payload (validated)
│       ├── LoginRequest.java        ← Login payload (validated)
│       └── AuthResponse.java        ← JWT token response
│
├── entity/
│   ├── User.java                    ← JPA entity + UserDetails implementation
│   └── Role.java                    ← USER / ADMIN / MANAGER enum
│
├── exception/
│   ├── GlobalExceptionHandler.java  ← @RestControllerAdvice — centralised error handling
│   ├── ErrorResponse.java           ← Uniform error JSON shape
│   ├── ResourceNotFoundException.java   ← 404
│   └── EmailAlreadyExistsException.java ← 409 Conflict
│
├── repository/
│   └── UserRepository.java          ← findByEmail, existsByEmail
│
├── security/
│   ├── SecurityConfig.java          ← Filter chain — stateless, CSRF off, JWT filter
│   ├── JwtUtils.java                ← Generate / validate / extract JWT claims
│   ├── JwtAuthFilter.java           ← OncePerRequestFilter — Bearer token → SecurityContext
│   └── UserDetailsServiceImpl.java  ← Load user by email from DB
│
├── service/
│   └── AuthService.java             ← register() + login() business logic
│
└── util/
    └── DateUtils.java               ← UTC-aware date helpers
```

---

## Getting Started

### Prerequisites

- Java 21
- Maven (or use the included `mvnw` wrapper)
- PostgreSQL 16 running locally **or** Docker

### 1. Clone the repository

```bash
git clone https://github.com/abdessalems/Enterprise-SaaS-Billing-Identity-Platform.git
cd Enterprise-SaaS-Billing-Identity-Platform/Enterprise\ SaaS-Billing-Identity-Platform
```

### 2. Start the database

**Option A — Docker Compose (recommended)**
```bash
docker compose up -d
```
This starts PostgreSQL 16 on port `5432` and pgAdmin on port `5050`.

**Option B — Local PostgreSQL**

Make sure PostgreSQL is running, then create the database:
```sql
CREATE DATABASE saas_db;
```

### 3. Run the application

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
.\mvnw.cmd spring-boot:run
```

The app starts on **`http://localhost:8080/api/v1`**

---

## Environment Variables

All variables have sensible defaults for local development.
Override them in production via environment variables or a secrets manager.

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `JWT_SECRET` | *(256-bit Base64 key)* | JWT signing secret — **change in production** |

Example:
```bash
export DB_USERNAME=myuser
export DB_PASSWORD=mypassword
export JWT_SECRET=your-256-bit-base64-secret
./mvnw spring-boot:run
```

---

## API Documentation (Swagger)

Once the app is running, open:

```
http://localhost:8080/api/v1/swagger-ui/index.html
```

### How to authenticate in Swagger UI

1. Call **`POST /auth/register`** or **`POST /auth/login`**
2. Copy the `accessToken` from the response
3. Click **Authorize** (lock icon, top right)
4. Paste your token and click **Authorize**
5. All protected endpoints now work directly from the UI

---

## Authentication Flow

```
Client                          Server
  │                               │
  │── POST /auth/register ────────►│  Validate input
  │   { firstName, lastName,      │  Check email not taken
  │     email, password }         │  Hash password (BCrypt 12)
  │                               │  Save User to DB
  │◄── 201 { accessToken } ───────│  Generate JWT (24h)
  │                               │
  │── POST /auth/login ───────────►│  Authenticate credentials
  │   { email, password }         │  Load user from DB
  │                               │  Verify BCrypt hash
  │◄── 200 { accessToken } ───────│  Generate JWT (24h)
  │                               │
  │── GET /protected ─────────────►│  JwtAuthFilter extracts Bearer token
  │   Authorization: Bearer <jwt> │  Validate signature + expiry
  │                               │  Set SecurityContext
  │◄── 200 { data } ──────────────│  Handle request
```

### JWT Details

| Property | Value |
|---|---|
| Algorithm | HMAC-SHA256 |
| Expiration | 24 hours |
| Library | JJWT 0.12.6 |
| Location | `Authorization: Bearer <token>` header |

---

## Database

### Connection (HikariCP)

```yaml
url: jdbc:postgresql://localhost:5432/saas_db
max-pool-size: 10
min-idle: 5
connection-timeout: 20s
```

### Users Table (auto-created by Hibernate)

```sql
CREATE TABLE users (
    id                      UUID PRIMARY KEY,
    first_name              VARCHAR(255) NOT NULL,
    last_name               VARCHAR(255) NOT NULL,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    password                VARCHAR(255) NOT NULL,
    role                    VARCHAR(255) NOT NULL CHECK (role IN ('USER','ADMIN','MANAGER')),
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired     BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked      BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP
);
```

### pgAdmin

Available at `http://localhost:5050` when using Docker Compose.
- Email: `admin@saas.com`
- Password: `admin`

---

## Git Workflow

This project follows **Git Flow**:

```
main          ← stable, production-ready releases only
develop       ← integration branch, all features merge here
feat/*        ← one branch per feature / phase
hotfix/*      ← urgent production fixes branched off main
```

### Release tags

| Tag | Description |
|---|---|
| `v0.1.0` | Phase 0 + Phase 1 — project setup and JWT authentication |
| `v1.1.0` | Swagger UI / OpenAPI documentation |

### Branch workflow per feature

```bash
# 1 — create feature branch from develop
git checkout develop
git checkout -b feat/phase-X-description

# 2 — develop, commit
git add <files>
git commit -m "feat(phase-X): ..."

# 3 — push and merge to develop
git push origin feat/phase-X-description
git checkout develop
git merge --no-ff feat/phase-X-description
git push origin develop

# 4 — release to main with tag
git checkout main
git merge --no-ff develop
git tag -a vX.X.X -m "Release description"
git push origin main --tags
```

---

## Phases Roadmap

| Phase | Feature | Status |
|---|---|---|
| 0 | Project setup, Docker, package structure | ✅ Done |
| 1 | JWT Authentication (register / login) | ✅ Done |
| 1.1 | Swagger UI / OpenAPI docs | ✅ Done |
| 2 | Tenant / Organization model (multi-tenancy) | 🔜 Next |
| 3 | User management (profile, roles, CRUD) | 🔜 Planned |
| 4 | Billing — Plan, Subscription, Invoice entities | 🔜 Planned |
| 5 | Email verification + password reset | 🔜 Planned |
| 6 | Usage tracking + metered billing | 🔜 Planned |
| 7 | Event-driven architecture (Kafka / RabbitMQ) | 🔜 Planned |
| 8 | CI/CD pipeline (GitHub Actions) | 🔜 Planned |

---

## Phase 0 — Project Setup

**Branch:** `main` | **Tag:** `v0.1.0`

### What was built

- Clean `pom.xml` with Spring Boot 3.4.5, Java 21, and all required starters
- `application.yml` replacing `application.properties` — HikariCP pool tuning, JPA/Hibernate settings, structured logging
- `docker-compose.yml` — PostgreSQL 16 Alpine + pgAdmin with health checks, named volumes, and a dedicated bridge network
- Full `com.saas` package tree bootstrapped: `config`, `controller`, `service`, `repository`, `entity`, `dto`, `security`, `exception`, `util`
- `ApiResponse<T>` — generic success/error HTTP response wrapper
- `ErrorResponse` — uniform error JSON shape
- `GlobalExceptionHandler` — `@RestControllerAdvice` with handlers for validation, 404, 409, 401, and generic 500
- `ResourceNotFoundException` — structured 404 with resource/field/value pattern
- `DateUtils` — UTC-aware date formatting helpers

---

## Phase 1 — JWT Authentication

**Branch:** `main` | **Tag:** `v0.1.0`

### What was built

#### Entities & Persistence

- **`User`** — JPA entity that implements `UserDetails`
  - UUID primary key (generated by PostgreSQL)
  - `firstName`, `lastName`, `email` (unique), `password` (BCrypt)
  - `Role` enum: `USER`, `ADMIN`, `MANAGER`
  - `@Builder.Default` flags: `enabled`, `accountNonExpired`, `accountNonLocked`, `credentialsNonExpired`
  - `createdAt` / `updatedAt` via Hibernate `@CreationTimestamp` / `@UpdateTimestamp`
  - Password excluded from `@ToString`
- **`UserRepository`** — `findByEmail()` + `existsByEmail()`

#### Security

- **`JwtUtils`** — JJWT 0.12 API
  - `generateToken(UserDetails)` — signs with HMAC-SHA256, 24h expiry
  - `extractUsername(token)` — reads subject claim
  - `isTokenValid(token, userDetails)` — validates signature + expiry + username match
- **`JwtAuthFilter`** — `OncePerRequestFilter`
  - Extracts `Authorization: Bearer <token>` header
  - Validates JWT and sets `SecurityContext`
- **`UserDetailsServiceImpl`** — loads user by email from `UserRepository`
- **`AppConfig`** — `DaoAuthenticationProvider` + `AuthenticationManager` + `BCryptPasswordEncoder(cost=12)`
- **`SecurityConfig`** — stateless session, CSRF disabled, JWT filter before `UsernamePasswordAuthenticationFilter`

#### API Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | None | Create account, returns JWT |
| `POST` | `/api/v1/auth/login` | None | Login, returns JWT |

#### Request / Response Examples

**Register**
```json
POST /api/v1/auth/register
{
  "firstName": "Abdessalem",
  "lastName": "Saadaoui",
  "email": "abdessalemsaa@gmail.com",
  "password": "secret123"
}
```
```json
HTTP 201
{
  "success": true,
  "message": "Account created successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000
  },
  "timestamp": "2026-04-28T18:14:55"
}
```

**Login**
```json
POST /api/v1/auth/login
{
  "email": "abdessalemsaa@gmail.com",
  "password": "secret123"
}
```
```json
HTTP 200
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000
  },
  "timestamp": "2026-04-28T18:14:55"
}
```

#### Error Responses

| Scenario | Status | Message |
|---|---|---|
| Email already registered | `409 Conflict` | Account already exists with email: ... |
| Wrong password | `401 Unauthorized` | Invalid email or password |
| Validation failure | `400 Bad Request` | Validation failed + field errors list |
| Missing/invalid JWT | `403 Forbidden` | — |

---

*Built with Spring Boot 3.4.5 · Java 21 · PostgreSQL 16*
