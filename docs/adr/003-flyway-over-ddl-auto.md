# ADR-003: Flyway for All Schema Changes

**Status:** Accepted  
**Date:** 2026-05-23

## Context

Spring Boot's `spring.jpa.hibernate.ddl-auto` can automatically create or update the schema.
This is convenient in development but dangerous in production: schema changes happen silently,
rollbacks are not possible, and the migration history is invisible.

## Decision

- `spring.jpa.hibernate.ddl-auto=validate` in ALL environments (dev, test, prod)
- All schema changes via Flyway versioned migrations in `db/migration/`
- Naming: `V{version}__{description}.sql` (e.g., `V2__add_task_position_column.sql`)
- Repeatable migrations (`R__`) only for views and functions, never for table structure
- Flyway runs automatically on startup; if validation fails, the app does not start

## Consequences

**Positive:**
- Full schema history in version control alongside code
- Schema changes are explicit, reviewed in PRs, and reversible (via undo migration or new migration)
- `validate` mode catches drift between JPA entities and actual DB schema at startup

**Negative:**
- More discipline required: every schema change needs a migration file
- Local dev must apply migrations (handled automatically by Flyway on startup)

## Test Strategy

Integration tests use Testcontainers (real PostgreSQL). Flyway runs automatically on test startup,
ensuring migrations are valid and the schema matches JPA entities before any test runs.
This catches migration errors in CI, not in production.
