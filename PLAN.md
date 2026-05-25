# TaskFlow — Project Plan

**Portfolio project for:** Krell Consulting Full Stack Developer application  
**Stack:** Spring Boot 3 · Angular 17 · PostgreSQL · Docker · GitHub Actions  
**Goal:** Demonstrate production-grade Full Stack engineering — not a tutorial project

---

## What It Is

Team-based project management system (think Linear/Jira core, stripped to essentials).  
Domain: Users belong to Teams, Teams own Projects, Projects contain Tasks with a state machine,
Tasks have Comments and an audit trail.

---

## Engineering Standards (Non-Negotiable)

### Language
- All code, comments, commits, PR bodies, variable names, error messages: **English**
- No Spanish anywhere in the codebase

### Testing
- Backend domain + application layer: **≥ 80% line coverage**
- Integration tests use **Testcontainers** (real PostgreSQL — no H2, no mocks of DB)
- Frontend: **≥ 70% coverage** with Jest + Angular Testing Library
- Every public API endpoint has at least one integration test
- State machine transitions: exhaustive unit tests for valid and invalid paths
- No `@Disabled` tests without a tracked issue comment

### Observability
- Structured JSON logging via Logback (`logstash-logback-encoder`)
- **Correlation ID** on every request — added by filter, propagated through MDC, present in all log lines
- Spring Boot Actuator: `/actuator/health`, `/actuator/metrics`, `/actuator/info`
- Micrometer metrics exposed at `/actuator/prometheus`
- Custom business metric: active tasks per project (gauge)
- No sensitive data (passwords, tokens, PII) in logs — enforced via custom serializer on `User`

### Performance
- **No unbounded queries** — all list endpoints paginated (`page` + `size`, max size = 100)
- **No N+1 queries** — verified with Hibernate statistics in test profile; `@EntityGraph` or `JOIN FETCH` where needed
- **Indexes required:**
  - All foreign key columns
  - `tasks.status`, `tasks.assignee_id`, `tasks.created_at` (common filters)
  - `audit_logs.entity_id + entity_type` (composite)
  - `users.email` (unique login lookup)
- All slow queries (>100ms in dev) annotated with `EXPLAIN ANALYZE` output in a comment
- P95 < 200ms for standard CRUD — verified with k6 baseline script in `infra/k6/`

### Database
- **Flyway only** — `spring.jpa.hibernate.ddl-auto=validate` in all environments
- Every entity has `created_at`, `updated_at` (UTC, non-null), `deleted_at` (nullable, soft delete)
- No `VARCHAR` without explicit length; no `TEXT` for short fields
- Enum values stored as `VARCHAR`, not ordinal integers

### Security
- JWT: access token 15min expiry, refresh token 7 days, stored in `refresh_tokens` table
- Passwords: BCrypt cost factor 12
- CORS: explicit origin whitelist, no `*` in any profile
- All endpoints protected by default — explicit `permitAll()` only for auth routes
- Input validation: `@Valid` on all request bodies; custom `@ValidEnum` for state transitions
- No stack traces in HTTP responses (`server.error.include-stacktrace=never`)

### API Design
- Base path: `/api/v1/`
- Consistent error envelope:
  ```json
  { "code": "TASK_NOT_FOUND", "message": "Task with id X does not exist", "timestamp": "2026-05-23T12:00:00Z" }
  ```
- Consistent pagination envelope:
  ```json
  { "data": [], "pagination": { "page": 0, "size": 20, "totalElements": 100, "totalPages": 5 } }
  ```
- No domain exceptions leaking to HTTP layer — global `@RestControllerAdvice`
- OpenAPI 3.1 spec auto-generated via SpringDoc; available at `/swagger-ui.html`

### Architecture — Backend
- **Hexagonal architecture**: `domain/` has zero Spring/JPA annotations
  - `domain/` — entities, value objects, domain events, repository interfaces, service interfaces
  - `application/` — use cases, DTOs, mappers, application services
  - `infrastructure/` — Spring beans, JPA entities, REST controllers, security config
- Domain events for cross-aggregate side effects (e.g., `TaskStatusChanged` → audit log entry)
- No Lombok — explicit getters/setters/constructors (readable, no annotation magic)
- Repository interfaces defined in `domain/`, implemented in `infrastructure/`

### Architecture — Frontend
- Feature-based folder structure: `features/auth/`, `features/projects/`, `features/tasks/`, etc.
- Angular Signals for local component state; `HttpClient` with typed responses
- No `any` in TypeScript — `strict: true` in `tsconfig.json`
- Angular Material for UI components
- JWT interceptor (auto-attaches Bearer token), 401 interceptor (redirects to login)
- Route guards: `AuthGuard`, `RoleGuard(role)`
- Lazy-loaded feature routes

### Code Quality
- Backend: Checkstyle (Google style) + SpotBugs enforced in CI — build fails on violation
- Frontend: ESLint strict config + Prettier — `lint` step in CI
- No `@SuppressWarnings` without inline comment explaining the exception
- Commits follow Conventional Commits: `feat:`, `fix:`, `test:`, `chore:`, `docs:`

---

## Repository Structure

```
taskflow/
├── backend/                    # Spring Boot 3
│   ├── src/
│   │   ├── main/java/dev/taskflow/
│   │   │   ├── domain/         # pure Java — no framework deps
│   │   │   │   ├── model/      # Task, Project, Team, User, Comment
│   │   │   │   ├── event/      # TaskStatusChanged, TaskAssigned, ...
│   │   │   │   ├── port/       # repository interfaces, service interfaces
│   │   │   │   └── exception/  # DomainException subclasses
│   │   │   ├── application/    # use cases, DTOs, mappers
│   │   │   └── infrastructure/ # Spring, JPA, REST, Security
│   │   │       ├── config/
│   │   │       ├── persistence/
│   │   │       ├── rest/
│   │   │       └── security/
│   │   └── test/
│   │       ├── unit/           # pure domain tests, no Spring
│   │       └── integration/    # @SpringBootTest + Testcontainers
│   ├── build.gradle
│   └── Dockerfile
├── frontend/                   # Angular 17 standalone
│   ├── src/app/
│   │   ├── core/               # interceptors, guards, services singleton
│   │   ├── shared/             # reusable components, pipes, directives
│   │   └── features/
│   │       ├── auth/
│   │       ├── dashboard/
│   │       ├── projects/
│   │       ├── tasks/
│   │       └── teams/
│   └── Dockerfile
├── infra/
│   ├── docker-compose.yml      # full local stack
│   ├── docker-compose.dev.yml  # dev overrides (hot reload)
│   ├── k6/                     # performance baseline scripts
│   └── prometheus/             # scrape config
├── docs/
│   ├── adr/                    # Architecture Decision Records
│   │   ├── 001-hexagonal-architecture.md
│   │   ├── 002-jwt-refresh-tokens.md
│   │   └── 003-flyway-migrations.md
│   └── diagrams/               # C4 or simple ASCII architecture diagrams
├── PLAN.md                     # this file
└── README.md
```

---

## Domain Model

```
User
  id, email (unique), password_hash, full_name, avatar_url
  created_at, updated_at, deleted_at

Team
  id, name, slug (unique), owner_id (→ User)
  created_at, updated_at, deleted_at

TeamMembership
  team_id, user_id, role (OWNER | MEMBER | VIEWER)
  joined_at

Project
  id, team_id (→ Team), name, description, status (ACTIVE | ARCHIVED)
  created_at, updated_at, deleted_at

Task
  id, project_id (→ Project), title, description
  status (BACKLOG | TODO | IN_PROGRESS | IN_REVIEW | DONE | CANCELLED)
  priority (LOW | MEDIUM | HIGH | CRITICAL)
  assignee_id (→ User, nullable)
  due_date (nullable)
  created_by (→ User)
  created_at, updated_at, deleted_at

Label
  id, project_id, name, color (hex)

TaskLabel (join)
  task_id, label_id

Comment
  id, task_id (→ Task), author_id (→ User), body, edited_at (nullable)
  created_at, updated_at, deleted_at

AuditLog
  id, entity_type, entity_id, action, actor_id, payload (jsonb), created_at

RefreshToken
  id, user_id, token_hash, expires_at, revoked_at (nullable), created_at
```

### Task State Machine

```
BACKLOG ──► TODO ──► IN_PROGRESS ──► IN_REVIEW ──► DONE
  │           │           │               │
  └───────────┴───────────┴───────────────┴──► CANCELLED
```

Rules:
- Only task creator, assignee, or team OWNER/MEMBER can transition
- DONE and CANCELLED are terminal states — no transitions out
- Every transition emits `TaskStatusChanged` domain event → `AuditLog` entry

---

## API Endpoints

### Auth
```
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

### Teams
```
POST   /api/v1/teams
GET    /api/v1/teams/{id}
PATCH  /api/v1/teams/{id}
DELETE /api/v1/teams/{id}
GET    /api/v1/teams/{id}/members
POST   /api/v1/teams/{id}/members         # invite
DELETE /api/v1/teams/{id}/members/{userId}
```

### Projects
```
POST   /api/v1/teams/{teamId}/projects
GET    /api/v1/teams/{teamId}/projects    # paginated
GET    /api/v1/projects/{id}
PATCH  /api/v1/projects/{id}
DELETE /api/v1/projects/{id}
GET    /api/v1/projects/{id}/stats        # task counts by status, velocity
```

### Tasks
```
POST   /api/v1/projects/{projectId}/tasks
GET    /api/v1/projects/{projectId}/tasks  # paginated, filterable by status/priority/assignee
GET    /api/v1/tasks/{id}
PATCH  /api/v1/tasks/{id}
DELETE /api/v1/tasks/{id}
POST   /api/v1/tasks/{id}/transitions      # body: { "to": "IN_PROGRESS" }
POST   /api/v1/tasks/{id}/assign           # body: { "userId": "..." }
GET    /api/v1/tasks/{id}/audit            # paginated audit log for this task
```

### Comments
```
POST   /api/v1/tasks/{taskId}/comments
GET    /api/v1/tasks/{taskId}/comments     # paginated
PATCH  /api/v1/comments/{id}
DELETE /api/v1/comments/{id}
```

### Labels
```
POST   /api/v1/projects/{projectId}/labels
GET    /api/v1/projects/{projectId}/labels
DELETE /api/v1/labels/{id}
POST   /api/v1/tasks/{taskId}/labels/{labelId}
DELETE /api/v1/tasks/{taskId}/labels/{labelId}
```

---

## Implementation Phases

### Phase 1 — Foundation
- [x] Backend: Spring Boot 3.3.4 + Gradle 8.10.2 + Java 21 — compiles clean
- [x] Backend: Hexagonal package skeleton (domain, application, infrastructure)
- [x] Backend: Flyway V1 migration — full schema, all indexes, all constraints
- [x] Backend: Dockerfile (multi-stage, eclipse-temurin:21-alpine)
- [x] Backend: Checkstyle (Google style) + SpotBugs config
- [x] Backend: application.yml / application-dev.yml / logback-spring.xml
- [x] Frontend: Angular 21 standalone + strict TS + Angular Material + ESLint + Prettier
- [x] Frontend: Feature-based folder structure (core, shared, features/*)
- [x] Frontend: Dockerfile (multi-stage nginx) + nginx.conf (SPA routing + API proxy)
- [x] Infra: docker-compose.yml (postgres, pgadmin, backend, frontend, prometheus profile)
- [x] CI: GitHub Actions — backend (Checkstyle + SpotBugs + test + coverage) + frontend (lint + test + build)
- [x] Docs: ADR-001 hexagonal, ADR-002 JWT refresh, ADR-003 Flyway

### Phase 2 — Domain Model + Auth
- [x] Domain entities: User, Team, TeamMembership, Project, Task, Comment, Label, AuditLog, RefreshToken
- [x] Task state machine + transition rules (pure Java, zero Spring)
- [x] Domain events: TaskStatusChanged, TaskAssigned, CommentAdded
- [x] Unit tests: state machine (all valid/invalid transitions)
- [x] JWT auth: register, login, refresh, logout
- [x] Spring Security config: stateless, JWT filter, role extraction
- [x] Integration tests: auth endpoints (Testcontainers)

### Phase 3 — Core API
- [x] Teams: create + membership management (add/remove members, role enforcement)
- [x] Projects: create + list (paginated) + archive
- [x] Tasks: create + list (paginated, filterable by status) + update + state machine transitions + assign + soft-delete
- [x] Labels: create + list + attach/detach to tasks
- [x] Comments: add + list (paginated) + edit + soft-delete
- [x] Audit log: async domain event listeners → AuditLog persistence (TaskStatusChanged, TaskAssigned, CommentAdded)
- [x] Global exception handler (`@RestControllerAdvice`) — DomainException, EntityNotFound, Unauthorized, Validation
- [x] Pagination on all list endpoints (`PageResponse<T>` envelope)
- [x] Integration tests: Team, Project, Task endpoints (Testcontainers)
- [x] OpenAPI/Swagger setup (SpringDoc + Bearer auth scheme)
- [x] `JwtUserDetails` — UserDetails with UUID, eliminates per-request user lookup in controllers
- [x] `SpringDomainEventPublisher` — domain port implemented via Spring `ApplicationEventPublisher`

### Phase 4 — Observability + Quality
- [ ] Structured JSON logging (Logback + logstash encoder)
- [ ] Correlation ID filter (MDC)
- [ ] Actuator endpoints: health, metrics, prometheus
- [ ] Custom business metric: active tasks per project
- [ ] Checkstyle + SpotBugs config + CI enforcement
- [ ] Hibernate statistics in test profile — verify no N+1
- [ ] k6 baseline performance script

### Phase 5 — Frontend
- [ ] Auth flow: login + register pages, JWT interceptor, auth guard
- [ ] Dashboard: stats overview (task counts by status, recent activity)
- [ ] Team management: create team, invite members, manage roles
- [ ] Project list + create project
- [ ] Kanban board: tasks by status column, drag-to-transition
- [ ] Task detail: full form, comments, audit timeline, label management
- [ ] Role guard: hide/show actions based on team role
- [ ] Frontend unit tests (Jest + ATL): auth service, task state, guards
- [ ] ESLint strict + Prettier in CI

### Phase 6 — Polish + README
- [ ] README: architecture diagram, setup instructions, design decisions
- [ ] ADR-001: Hexagonal architecture rationale
- [ ] ADR-002: JWT refresh token strategy
- [ ] ADR-003: Flyway over ddl-auto
- [ ] Final k6 run — document P95 baseline
- [ ] `docker compose up` — full stack runs in one command

---

## Progress Tracker

| Phase | Status | Notes |
|---|---|---|
| 1 — Foundation | ✅ Done | Java 21, Gradle 8.10.2, Angular 21, Docker Compose, GitHub Actions CI, Flyway V1 schema, ADRs |
| 2 — Domain + Auth | ✅ Done | 37 unit tests passing (0 failures) · JWT auth full cycle · Testcontainers integration tests ready |
| 3 — Core API | ✅ Done | 90+ files · Teams, Projects, Tasks, Comments, Labels, Audit log · Integration tests |
| 4 — Observability | ⬜ Not started | |
| 5 — Frontend | ⬜ Not started | |
| 6 — Polish | ⬜ Not started | |

Legend: ⬜ Not started · 🔄 In progress · ✅ Done · ❌ Blocked

---

## Tech Stack — Exact Versions

### Backend
| Dependency | Version |
|---|---|
| Java | 21 (LTS) |
| Spring Boot | 3.3.x |
| Spring Security | 6.x (included) |
| Spring Data JPA | 3.x (included) |
| PostgreSQL driver | 42.x |
| Flyway | 10.x |
| JJWT (JWT) | 0.12.x |
| SpringDoc OpenAPI | 2.x |
| Testcontainers | 1.19.x |
| JUnit 5 | 5.x (included) |
| Mockito | 5.x (included) |
| Logback JSON encoder | 7.x |
| Micrometer Prometheus | 1.x (included) |

### Frontend
| Dependency | Version |
|---|---|
| Angular | 17.x |
| Angular Material | 17.x |
| TypeScript | 5.x |
| RxJS | 7.x |
| Jest | 29.x |
| Angular Testing Library | 17.x |
| ESLint | 8.x |
| Prettier | 3.x |

### Infra
| Tool | Purpose |
|---|---|
| Docker + Compose | Local full stack |
| GitHub Actions | CI pipeline |
| k6 | Performance baseline |
| Prometheus (optional) | Metrics scraping |
| pgAdmin | DB inspection in dev |
