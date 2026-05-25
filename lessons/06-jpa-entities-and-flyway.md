# Lesson 06 — JPA Entities + Flyway

## Why Two Separate Classes for Task?

Most Spring tutorials put `@Entity` on the domain model directly. This project intentionally keeps them separate. Lesson 01 covered the architecture reason. Here's the practical JPA reason.

## JPA Requires Certain Concessions

To work with JPA, an entity class MUST:
1. Have a no-arg constructor (can be `protected`)
2. Have `@Entity`, `@Id`, etc. annotations
3. Play nicely with Hibernate's proxying (no `final` fields, no records for relationships)

Look at `TaskEntity`:

```java
// infrastructure/persistence/entity/TaskEntity.java
@Entity
@Table(name = "tasks")
public class TaskEntity {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)   // store "TODO" not 1
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    protected TaskEntity() {}  // required by JPA — Hibernate needs this
```

The `protected TaskEntity() {}` constructor must exist for Hibernate to instantiate the entity when loading from the DB. You'd never call this yourself — that's why it's `protected`, not `public`.

`@Enumerated(EnumType.STRING)` is critical. The default is `EnumType.ORDINAL`, which stores the enum position as an integer (0, 1, 2...). If you ever reorder the enum values, your data silently becomes wrong. `STRING` stores `"TODO"`, `"IN_PROGRESS"` etc. — always correct regardless of enum ordering.

## Why Flyway Instead of `ddl-auto: create`?

This project uses:
```yaml
# application.yml
jpa:
  hibernate:
    ddl-auto: validate
```

And Flyway migrations in `src/main/resources/db/migration/`.

**`ddl-auto: create`** would have Hibernate generate the schema from your entities on startup. It's fine for demos. In production, it's dangerous:

- **It drops and recreates tables.** Every restart. All your data is gone.
- Actually, `create` drops. `update` tries to alter. But `update` doesn't handle all cases — it can't drop columns, can't change column types safely, and doesn't run in a transaction.
- You can never know exactly what SQL Hibernate generated.
- Database-specific features (partial indexes, custom constraints) are not supported.

**`ddl-auto: validate`** tells Hibernate: "don't touch the schema — just verify that your entities match what's in the database. Throw an error if they don't." This means:
- Schema changes must be made explicitly via migration files
- You always know exactly what SQL ran
- Migrations run in transactions (rollback on failure)
- Works with any schema management tool your team uses

### The Migration File

```sql
-- db/migration/V1__init_schema.sql
-- All timestamps stored in UTC (TIMESTAMPTZ)
-- Soft deletes via deleted_at (null = not deleted)

CREATE TABLE tasks (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID         NOT NULL REFERENCES projects (id),
    title        VARCHAR(500) NOT NULL,
    description  TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'BACKLOG',
    priority     VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    assignee_id  UUID         REFERENCES users (id),
    due_date     DATE,
    created_by   UUID         NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ
);
```

Flyway versions files as `V1__description.sql`, `V2__description.sql`, etc. On startup, it reads the `flyway_schema_history` table to see which migrations have run, then executes any new ones in order. Each runs in a transaction.

### Soft Deletes via `deleted_at`

Every table has a `deleted_at TIMESTAMPTZ` column. When something is "deleted", only this timestamp is set — the row stays in the DB.

```java
// domain/model/Task.java
public void softDelete() {
    this.deletedAt = Instant.now();
    this.updatedAt = Instant.now();
}
```

All queries filter on `deleted_at IS NULL`:
```java
// infrastructure/persistence/repository/TaskJpaRepository.java
Optional<TaskEntity> findByIdAndDeletedAtIsNull(UUID id);
Page<TaskEntity> findByProjectIdAndDeletedAtIsNull(UUID projectId, Pageable pageable);
```

Why soft deletes?
- **Auditability**: you can see what was deleted and when
- **Recovery**: restore accidentally deleted data by clearing `deleted_at`
- **Foreign keys**: other records referencing a soft-deleted record don't break
- **Event sourcing**: useful if you later want to replay history

## TIMESTAMPTZ vs TIMESTAMP

Notice the SQL uses `TIMESTAMPTZ` (timestamp with time zone), not `TIMESTAMP`.

- `TIMESTAMP` stores local time without timezone info — ambiguous
- `TIMESTAMPTZ` normalizes to UTC on write, returns UTC on read

Combined with the application config:
```yaml
jpa:
  properties:
    hibernate:
      jdbc:
        time_zone: UTC
```

Every timestamp is unambiguously UTC throughout the system. No timezone bugs where 9pm on Sunday looks like midnight Monday in another timezone.

In Java, `Instant` maps to `TIMESTAMPTZ` correctly. Don't use `LocalDateTime` for timestamps that represent a moment in time.

## Key Files

| File | What to study |
|------|---------------|
| `infrastructure/persistence/entity/TaskEntity.java` | `@Enumerated(STRING)`, `protected` constructor |
| `infrastructure/persistence/repository/TaskJpaRepository.java` | Method naming conventions for query generation |
| `src/main/resources/db/migration/V1__init_schema.sql` | Schema design, constraints, indexes |
| `src/main/resources/application.yml` | `ddl-auto: validate`, HikariCP config |
