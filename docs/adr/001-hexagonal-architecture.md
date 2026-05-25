# ADR-001: Hexagonal Architecture for Backend

**Status:** Accepted  
**Date:** 2026-05-23

## Context

The backend needs to be testable at the domain level without requiring a Spring context or database.
The domain logic (task state machine, business rules) must be isolated from infrastructure concerns
(HTTP, JPA, security) to enable fast unit tests and clear separation of responsibilities.

## Decision

Adopt Hexagonal Architecture (Ports & Adapters) with three distinct layers:

- **`domain/`** — pure Java. Zero framework dependencies. Contains entities, value objects,
  domain events, repository interfaces (ports), and business rules.
- **`application/`** — orchestrates use cases. Depends on domain only. Contains application
  services, DTOs, and mappers. Spring annotations allowed only at the boundary.
- **`infrastructure/`** — framework glue. Spring beans, JPA entities, REST controllers,
  security configuration, Flyway. Implements the ports defined in the domain.

## Consequences

**Positive:**
- Domain unit tests run in milliseconds with no Spring context
- Infrastructure can be swapped (e.g., replace JPA with JDBC) without touching domain logic
- Business rules are readable and testable in isolation

**Negative:**
- More boilerplate: separate JPA entity classes from domain model classes
- Mappers required between infrastructure and domain layers
- Slightly steeper onboarding for developers unfamiliar with the pattern

## Enforcement

- CI runs `checkstyle` rule: classes in `domain/` package must not import `org.springframework.*`
  or `jakarta.persistence.*`
- Code review checklist includes: "Does this PR add Spring annotations to domain classes?"
