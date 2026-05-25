---
name: update-lessons
description: >
  Maintains the lessons/ folder as a living document of technical decisions,
  patterns, bugs fixed, and architectural choices in the TaskFlow project.
  Trigger: After any implementation, bug fix, architectural decision, library choice,
  or non-obvious pattern is introduced. Run BEFORE closing the task.
license: Apache-2.0
metadata:
  author: gentleman-programming
  version: "1.0"
allowed-tools: Read, Edit, Write, Glob, Grep
---

## When to Use

Trigger this skill immediately after ANY of:

- Bug fixed with a non-obvious root cause
- New architectural pattern introduced
- Library or tool chosen (with tradeoffs)
- Security decision made
- Infrastructure pattern established (DB config, migrations, etc.)
- Testing pattern added or changed
- Spring Boot / JPA / Security behavior explained to resolve an issue
- A "why does this work this way?" moment that took effort to understand

**Do NOT trigger for**: routine CRUD additions, renaming, formatting, dependency upgrades without decisions.

---

## Decision: Update Existing vs Create New Lesson

```
Is the change a deeper explanation of a topic already in lessons/?
  YES → update the relevant existing lesson (add new section or extend examples)
  NO  → create a new lesson file

Did we FIX a bug that belongs to an existing lesson's topic?
  YES → add a "Bug Fixed" section to that lesson (see template below)
  NO  → create new lesson if the bug reveals a new pattern

Is this a minor addition (one code example, one clarification)?
  YES → edit the existing lesson in-place
  NO  → new lesson or major expansion
```

---

## Lesson File Naming

```
lessons/
├── 01-hexagonal-architecture.md
├── 02-domain-model.md
├── 03-ports-and-adapters.md
├── 04-spring-security-jwt.md
├── 05-spring-boot-autoconfig-trap.md
├── 06-jpa-entities-and-flyway.md
├── 07-use-cases-and-transactions.md
├── 08-error-handling.md
├── 09-integration-testing.md
├── 10-jwt-refresh-tokens.md
└── NN-kebab-case-topic.md   ← new lessons continue the sequence
```

New lesson numbers continue from the highest existing number.
Use kebab-case. Be specific: `11-hikaricp-connection-pool.md` not `11-database.md`.

---

## Lesson Structure (required sections)

Every lesson must have these three sections. Non-negotiable.

```markdown
# Lesson NN — Title

## The Concept
[What it is and WHY it exists — motivation first]

## In This Project
[Exact file paths + code snippets from THIS codebase]
[Show the actual implementation, not generic examples]

## What Breaks Without It
[The failure mode — what goes wrong if you skip or misuse this]

## Key Files
[Table: file path | what to study]
```

Optional sections (add when relevant):
- `## Bug Fixed` — for lessons born from a real bug
- `## The Rule to Remember` — one-liner summary of the core insight
- `## Decision: X vs Y` — when a choice was made between alternatives

---

## Bug Fixed Section Template

Use this when the lesson documents a real bug we hit:

```markdown
## Bug Fixed

**Symptom**: [What the developer/test saw — e.g., "401 on all authenticated requests"]

**Root cause**: [The actual technical reason — be specific]

**The fix**:
\`\`\`java
// Before (broken)
...

// After (fixed)
...
\`\`\`

**Why this happens**: [Explain the mechanism — Spring internals, JVM behavior, library quirk]
```

---

## Updating `lessons/README.md`

After adding or significantly changing a lesson, update the index table in `lessons/README.md`:

```markdown
| [NN](NN-topic.md) | Short Title | Core question this lesson answers |
```

The "Core question" column should be a question a developer would actually ask, e.g.:
- "Why did adding `@Component` break all tests?"
- "When do I use `create()` vs `reconstitute()`?"
- "Why does wrong password return 401 and not 404?"

---

## Quality Rules

- **Always use actual code from the project** — no invented examples
- **Include file paths** — `infrastructure/security/JwtService.java` not "the JWT service"
- **Explain the WHY** — not just what the code does, but why it's done this way
- **Show the failure mode** — every lesson must answer "what breaks without this?"
- **Keep it conversational** — write like a senior explaining to a junior, not like a manual

---

## Process

1. Read the changed/added files to understand what happened
2. Determine: update existing lesson OR create new one (use decision tree above)
3. If creating new: pick the next number in sequence, use kebab-case name
4. Write/update the lesson following the required structure
5. Update `lessons/README.md` index if a new lesson was added
6. Confirm with: "Lesson [NN] updated/created: [topic]"

---

## Context: Existing Lessons

| # | File | Topic |
|---|------|-------|
| 01 | `01-hexagonal-architecture.md` | Layer structure, inward dependency rule |
| 02 | `02-domain-model.md` | Aggregates, create() vs reconstitute(), domain events |
| 03 | `03-ports-and-adapters.md` | Repository interfaces, adapters, mappers |
| 04 | `04-spring-security-jwt.md` | Filter chain, JWT validation, SecurityConfig |
| 05 | `05-spring-boot-autoconfig-trap.md` | @Component double-registration bug |
| 06 | `06-jpa-entities-and-flyway.md` | ddl-auto: validate, EnumType.STRING, TIMESTAMPTZ |
| 07 | `07-use-cases-and-transactions.md` | @Transactional boundary, one class per use case |
| 08 | `08-error-handling.md` | GlobalExceptionHandler, 422 vs 400, user enumeration |
| 09 | `09-integration-testing.md` | Testcontainers singleton, HttpClient5, real DB |
| 10 | `10-jwt-refresh-tokens.md` | JWT vs opaque, token rotation, hashed storage |
