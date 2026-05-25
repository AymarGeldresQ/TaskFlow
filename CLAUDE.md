# TaskFlow — Project Instructions

## Skills (Auto-load based on context)

When you detect any of these contexts, IMMEDIATELY read the corresponding skill file BEFORE writing any code or documentation.

| Context | Read this file |
|---------|---------------|
| Bug fixed, architectural decision made, new pattern introduced, library chosen, non-obvious behavior explained | `.claude/skills/update-lessons/SKILL.md` |
| Phase completed, endpoint added/removed, new env var, schema change, new doc file, setup change, security change | `.claude/skills/update-readme/SKILL.md` |

## Project Structure

```
taskflow/
├── backend/          Spring Boot 3.3.4, Java 21, Hexagonal Architecture
├── frontend/         Angular (in progress)
├── infra/            Docker Compose, Prometheus
├── docs/             API docs, Postman collection
└── lessons/          Living documentation — updated after every significant change
```

## Backend Stack

- Java 21, Spring Boot 3.3.4, Spring Security 6
- PostgreSQL 16 + Flyway migrations
- JWT (access) + Opaque UUID (refresh) authentication
- Testcontainers for integration tests

## Architecture Rules

- Domain layer: zero Spring/JPA imports — pure Java
- Use cases: one class per operation, `@Transactional` here only
- Filters: never `@Component` on a filter registered via `addFilterBefore()`
- Enums stored as STRING in JPA — never ORDINAL

## Lessons Folder Contract

`lessons/` is a living document. After ANY significant implementation, bug fix, or decision:
1. Read `.claude/skills/update-lessons/SKILL.md`
2. Follow its process to update or create a lesson
3. Update `lessons/README.md` index if a new lesson was added

This is not optional — it's part of the definition of done for this project.
