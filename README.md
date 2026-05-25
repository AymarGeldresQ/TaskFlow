# TaskFlow

Team-based project management API — think Linear/Jira core, built to production-grade engineering standards.

**Portfolio project** demonstrating full-stack engineering: Hexagonal Architecture, stateless JWT auth, real-integration tests, structured logging, and an Angular frontend (in progress).

---

## What It Does

Users belong to **Teams**. Teams own **Projects**. Projects contain **Tasks** with a state machine. Tasks have **Comments**, **Labels**, and a full **Audit Trail**.

```
User ──< TeamMembership >── Team ──< Project ──< Task
                                                  ├── Comments
                                                  ├── Labels
                                                  └── AuditLog (async events)
```

**Task lifecycle:**
```
BACKLOG → TODO → IN_PROGRESS → IN_REVIEW → DONE
                 ↑__________↓  ↑_________↓
                 (backwards allowed within active states)
                                            ↘ CANCELLED (any active state)
```

---

## Tech Stack

### Backend
| | |
|---|---|
| Java 21 + Spring Boot 3.3.4 | Runtime + framework |
| Spring Security 6 | Stateless JWT auth filter chain |
| Spring Data JPA + Hibernate 6 | ORM with explicit entity/domain separation |
| PostgreSQL 16 | Primary database |
| Flyway 10 | Schema versioning (`ddl-auto: validate`) |
| JJWT 0.12 | JWT generation + validation |
| SpringDoc OpenAPI 2 | Auto-generated Swagger UI |
| Testcontainers 1.20 | Real Postgres in integration tests |
| Apache HttpClient 5 | Test HTTP client (handles 401 cleanly) |
| Logstash Logback Encoder | Structured JSON logging |
| Micrometer + Prometheus | Metrics |
| Checkstyle + SpotBugs | Code quality enforcement |

### Frontend *(in progress)*
| | |
|---|---|
| Angular 17 | SPA framework (standalone components) |
| Angular Material | UI component library |
| TypeScript 5 (strict) | No `any` — ever |
| Jest + Angular Testing Library | Unit + component tests |

### Infrastructure
| | |
|---|---|
| Docker + Docker Compose | Full local stack |
| GitHub Actions | CI (test + lint + coverage) |
| k6 | Performance baseline scripts |
| Prometheus + pgAdmin | Monitoring + DB inspection |

---

## Architecture

Hexagonal (Ports & Adapters) — dependencies point **inward only**:

```
infrastructure/   ← Spring, JPA, HTTP, Security
  └── application/   ← Use cases, DTOs
        └── domain/     ← Business rules, zero framework imports
```

- **`domain/`** — Pure Java. `Task`, `User`, `Team`, domain events, repository interfaces. No Spring annotations.
- **`application/`** — Use cases (`CreateTaskUseCase`, `LoginUseCase`, …). One class per operation. `@Transactional` lives here.
- **`infrastructure/`** — JPA entities, REST controllers, JWT filter, Flyway migrations, adapters.

Architecture Decision Records: [`docs/adr/`](docs/adr/)

---

## Project Structure

```
taskflow/
├── backend/
│   ├── src/main/java/dev/taskflow/
│   │   ├── domain/
│   │   │   ├── model/          Task, User, Team, Project, Comment, Label…
│   │   │   ├── exception/      DomainException, EntityNotFoundException…
│   │   │   ├── event/          TaskStatusChangedEvent, TaskAssignedEvent…
│   │   │   └── port/repository/  TaskRepository, UserRepository… (interfaces)
│   │   ├── application/
│   │   │   ├── usecase/        CreateTaskUseCase, LoginUseCase… (one class per op)
│   │   │   └── dto/            Request/Response records
│   │   └── infrastructure/
│   │       ├── persistence/    JPA entities, adapters, mappers, Spring Data repos
│   │       ├── rest/           Controllers, GlobalExceptionHandler
│   │       ├── security/       JwtAuthenticationFilter, JwtService, SecurityConfig
│   │       └── config/         JwtProperties, OpenApiConfig, CorrelationIdFilter, TaskMetrics
│   ├── src/main/resources/
│   │   ├── db/migration/       V1__init_schema.sql (Flyway)
│   │   └── application.yml     + application-dev.yml
│   ├── src/test/
│   │   ├── java/.../integration/  AbstractIntegrationTest + per-feature tests
│   │   └── resources/application-test.yml
│   ├── build.gradle.kts
│   └── Dockerfile              Multi-stage (eclipse-temurin:21-alpine)
│
├── frontend/                   Angular 17 (in progress)
│   └── Dockerfile              Multi-stage (nginx)
│
├── infra/
│   ├── docker-compose.yml      postgres, pgadmin, backend, frontend, prometheus
│   ├── prometheus/prometheus.yml
│   └── k6/baseline.js          Performance baseline (10 VUs, p95<500ms threshold)
│
├── docs/
│   ├── adr/                    ADR-001, ADR-002, ADR-003
│   ├── taskflow-postman-collection.json
│   └── api-curl-cheatsheet.md
│
├── lessons/                    Living technical documentation
│   ├── README.md               Index of all lessons
│   └── 01…10-*.md
│
├── CLAUDE.md                   AI assistant project instructions
└── PLAN.md                     Full engineering spec + phase tracker
```

---

## Getting Started

### Prerequisites

- Java 21 ([SDKMAN](https://sdkman.io): `sdk install java 21.0.7-zulu`)
- Docker + Docker Compose (or [OrbStack](https://orbstack.dev) on macOS)
- Node 20+ (frontend only)

### Run the Backend Locally

**1. Start the database:**
```bash
cd infra
docker compose up -d postgres
```

**2. Run the backend:**
```bash
cd backend
JWT_SECRET=dev_secret_minimum_32_characters_required \
  ./gradlew bootRun --args='--spring.profiles.active=dev'
```

Server starts at `http://localhost:8080`.

**3. Verify:**
```bash
curl http://localhost:8080/actuator/health
# {"status":"UP","groups":["liveness","readiness"]}
```

### Run the Full Stack (Docker)

```bash
cd infra
JWT_SECRET=dev_secret_minimum_32_characters_required \
  docker compose up -d postgres backend
```

Frontend (when ready):
```bash
docker compose up -d   # includes frontend on :4200
```

Monitoring stack:
```bash
docker compose --profile monitoring up -d
```

| Service | URL |
|---------|-----|
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| pgAdmin | http://localhost:5050 (admin@taskflow.dev / admin) |
| Prometheus | http://localhost:9090 (with monitoring profile) |

---

## Running Tests

### Integration Tests (recommended)
```bash
cd backend
./gradlew test
```

Requires Docker (Testcontainers starts a real Postgres 16 container automatically).

### Single test class
```bash
./gradlew test --tests "dev.taskflow.integration.TaskControllerIntegrationTest"
```

### Single test method
```bash
./gradlew test --tests "dev.taskflow.integration.TeamControllerIntegrationTest\$CreateTeam.createsTeamAndOwnerBecomesOwnerMember"
```

### With coverage report
```bash
./gradlew test jacocoTestReport
# Report: backend/build/reports/jacoco/test/html/index.html
```

### Code quality checks
```bash
./gradlew checkstyleMain   # Google style
./gradlew spotbugsMain     # Static analysis
./gradlew check            # Everything (test + checkstyle + spotbugs + coverage)
```

---

## API Reference

### Postman Collection

Import [`docs/taskflow-postman-collection.json`](docs/taskflow-postman-collection.json) into Postman.

- Pre-configured collection variables: `baseUrl`, `accessToken`, `teamId`, `projectId`, `taskId`, etc.
- Test scripts auto-populate variables on Register/Login/Create responses — just hit Register and every subsequent request is ready.

### cURL Cheat Sheet

See [`docs/api-curl-cheatsheet.md`](docs/api-curl-cheatsheet.md) for copy-paste cURL commands for all 25 endpoints.

### Swagger UI (live)

`http://localhost:8080/swagger-ui/index.html` — interactive docs with Bearer auth.

### Endpoint Summary

```
Auth
  POST  /api/v1/auth/register
  POST  /api/v1/auth/login
  POST  /api/v1/auth/refresh
  POST  /api/v1/auth/logout

Teams
  POST  /api/v1/teams
  GET   /api/v1/teams/{teamId}/members
  POST  /api/v1/teams/{teamId}/members
  DELETE /api/v1/teams/{teamId}/members/{userId}

Projects
  POST  /api/v1/teams/{teamId}/projects
  GET   /api/v1/teams/{teamId}/projects          ?page=0&size=20
  PATCH /api/v1/projects/{projectId}/archive
  POST  /api/v1/projects/{projectId}/labels
  GET   /api/v1/projects/{projectId}/labels

Tasks
  POST  /api/v1/projects/{projectId}/tasks
  GET   /api/v1/projects/{projectId}/tasks       ?status=TODO&page=0&size=20
  PUT   /api/v1/tasks/{taskId}
  PATCH /api/v1/tasks/{taskId}/status
  PATCH /api/v1/tasks/{taskId}/assignee
  POST  /api/v1/tasks/{taskId}/labels/{labelId}
  DELETE /api/v1/tasks/{taskId}/labels/{labelId}
  DELETE /api/v1/tasks/{taskId}

Comments
  POST  /api/v1/tasks/{taskId}/comments
  GET   /api/v1/tasks/{taskId}/comments          ?page=0&size=20
  PATCH /api/v1/comments/{commentId}
  DELETE /api/v1/comments/{commentId}
```

All list endpoints return:
```json
{
  "data": [...],
  "pagination": { "page": 0, "size": 20, "totalElements": 47, "totalPages": 3 }
}
```

All errors return:
```json
{ "code": "TASK_NOT_FOUND", "message": "task with id … does not exist" }
```

---

## Security Model

| Mechanism | Detail |
|-----------|--------|
| Access token | JWT, HMAC-SHA256, 15-minute expiry |
| Refresh token | Opaque UUID, 7-day expiry, SHA-256 hashed in DB |
| Token rotation | Old refresh token deleted on each use |
| Password hashing | BCrypt, cost factor 12 |
| Session | Stateless — no server-side sessions, no cookies |
| Logout | Deletes all refresh tokens for the user |
| Unauthenticated requests | 401 (not 403) |
| User enumeration | Wrong email and wrong password both return 401 with identical message |

See [Lesson 10](lessons/10-jwt-refresh-tokens.md) and [ADR-002](docs/adr/002-jwt-refresh-tokens.md) for the full rationale.

---

## Database Schema

12 tables managed exclusively by Flyway — `ddl-auto: validate`.

```
users               id, email, password_hash, full_name, deleted_at
teams               id, name, slug (unique), owner_id, deleted_at
team_memberships    team_id + user_id (PK), role (OWNER|MEMBER|VIEWER)
projects            id, team_id, name, status (ACTIVE|ARCHIVED), deleted_at
tasks               id, project_id, title, status, priority, assignee_id, due_date, deleted_at
task_labels         task_id + label_id (composite PK)
labels              id, project_id, name, color (#RRGGBB)
comments            id, task_id, author_id, body, edited_at, deleted_at
audit_logs          id, entity_type, entity_id, action, actor_id, payload (JSONB)
refresh_tokens      id, user_id, token_hash (SHA-256), expires_at, revoked_at
```

All timestamps: `TIMESTAMPTZ` stored in UTC. All soft-deletes via `deleted_at IS NULL` filter.

See [`backend/src/main/resources/db/migration/V1__init_schema.sql`](backend/src/main/resources/db/migration/V1__init_schema.sql).

---

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | **Yes** | — | Min 32 chars. Use a random secret in production. |
| `SPRING_DATASOURCE_URL` | No | `jdbc:postgresql://localhost:5432/taskflow` | Postgres JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | No | `taskflow` | |
| `SPRING_DATASOURCE_PASSWORD` | No | `taskflow_dev` | |
| `JWT_ACCESS_EXPIRATION_MS` | No | `900000` (15 min) | Access token TTL in ms |
| `JWT_REFRESH_EXPIRATION_MS` | No | `604800000` (7 days) | Refresh token TTL in ms |

---

## Project Status

| Phase | Status | Notes |
|-------|--------|-------|
| 1 — Foundation | ✅ Done | Java 21, Gradle, Docker, Flyway, CI, ADRs |
| 2 — Domain + Auth | ✅ Done | JWT auth cycle, 37 unit tests, integration tests |
| 3 — Core API | ✅ Done | 72/72 integration tests passing, 25 endpoints |
| 4 — Observability | ✅ Done | Micrometer gauge, MDC correlation ID, structured logging, k6 baseline, Checkstyle+SpotBugs clean |
| 5 — Frontend | ⬜ Planned | Angular, Kanban board, JWT interceptor |
| 6 — Polish | ⬜ Planned | Final k6 run, README diagrams |

---

## Documentation

| Document | Description |
|----------|-------------|
| [`lessons/`](lessons/README.md) | Living technical documentation — architecture, bugs fixed, decisions explained |
| [`docs/adr/`](docs/adr/) | Architecture Decision Records (hexagonal arch, JWT strategy, Flyway) |
| [`docs/api-curl-cheatsheet.md`](docs/api-curl-cheatsheet.md) | cURL commands for all 25 endpoints |
| [`docs/taskflow-postman-collection.json`](docs/taskflow-postman-collection.json) | Importable Postman collection with auto-token scripts |
| [`infra/k6/baseline.js`](infra/k6/baseline.js) | k6 load test — 10 VUs, full CRUD flow, p95 < 500ms threshold |
| [`PLAN.md`](PLAN.md) | Full engineering spec, standards, and phase tracker |

---

## Contributing / Development Notes

- **No Lombok** — explicit getters, constructors, builders. Readable without IDE plugins.
- **Conventional Commits** — `feat:`, `fix:`, `test:`, `chore:`, `docs:`
- **No `@Component` on filters** registered inside `SecurityFilterChain` — causes double-registration. See [Lesson 05](lessons/05-spring-boot-autoconfig-trap.md).
- **`@Transactional` on use cases only** — not on domain, not on controllers.
- **`EnumType.STRING` always** — never `ORDINAL`.
- `./gradlew check` must pass before any commit.
