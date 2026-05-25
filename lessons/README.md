# TaskFlow — Lessons

Every lesson is grounded in THIS codebase. Real file paths, real decisions, real bugs we hit and fixed.

## Index

| # | Topic | Core Question |
|---|-------|---------------|
| [01](01-hexagonal-architecture.md) | Hexagonal Architecture | Why is the project structured in layers? What would break if you ignored them? |
| [02](02-domain-model.md) | Domain Model & Aggregates | Why does `Task` have no Spring annotations? What is `create()` vs `reconstitute()`? |
| [03](03-ports-and-adapters.md) | Ports & Adapters (Repository Pattern) | Why does the domain define `TaskRepository` as an interface? |
| [04](04-spring-security-jwt.md) | Spring Security + JWT | How does a Bearer token authenticate a request? What is the filter chain? |
| [05](05-spring-boot-autoconfig-trap.md) | Spring Boot Auto-Config Trap | Why did adding `@Component` to `JwtAuthenticationFilter` break all tests? |
| [06](06-jpa-entities-and-flyway.md) | JPA Entities + Flyway | Why two separate classes for `Task` and `TaskEntity`? Why Flyway instead of `ddl-auto: create`? |
| [07](07-use-cases-and-transactions.md) | Use Cases & `@Transactional` | What is a use case class? Why does `@Transactional` live here, not in the domain? |
| [08](08-error-handling.md) | Error Handling Strategy | Why does wrong password return 401, not 400? How does `GlobalExceptionHandler` work? |
| [09](09-integration-testing.md) | Integration Testing | Why not mock the database? What was the Testcontainers singleton bug we fixed? |
| [10](10-jwt-refresh-tokens.md) | JWT vs Opaque Refresh Tokens | Why is the access token a JWT but the refresh token a UUID? |
| [11](11-observability-metrics-logging.md) | Observability: Metrics, Logging, Correlation IDs | Why does the gauge query the DB at scrape time, not per request? How does MDC propagate a correlation ID through every log line? |
| [12](12-jacoco-coverage-enforcement.md) | JaCoCo Coverage Enforcement | Why did the first CI run fail with 36 violations? What's the difference between a coverage report and a coverage threshold? |
| [13](13-frontend-architecture-decisions.md) | Frontend Architecture Decisions | How did we find the state machine bug before writing code? How does refresh single-flight work? Why access token in memory and refresh in localStorage? |

## How to read

Each file has three sections:
- **The Concept** — what it is and why it exists
- **In This Project** — exact file + code showing how it's applied
- **What Breaks Without It** — the failure mode that teaches you why it matters

Start at 01 and go in order — each lesson builds on the previous.
