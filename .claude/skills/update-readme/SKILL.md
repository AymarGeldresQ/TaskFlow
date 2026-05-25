---
name: update-readme
description: >
  Maintains README.md as the accurate public-facing entry point for the TaskFlow project.
  Trigger: After any change that affects setup commands, endpoints, tech stack, project status,
  new environment variables, new documentation, or completion of a phase.
license: Apache-2.0
metadata:
  author: gentleman-programming
  version: "1.0"
allowed-tools: Read, Edit, Write, Glob, Grep
---

## When to Use

Trigger this skill when ANY of these change:

| Change | README section affected |
|--------|------------------------|
| Phase completed or started | `## Project Status` |
| New endpoint added or removed | `## API Reference → Endpoint Summary` |
| New environment variable | `## Environment Variables` |
| New table in DB schema | `## Database Schema` |
| New dependency added | `## Tech Stack` |
| New doc file created (ADR, lesson, etc.) | `## Documentation` |
| Setup steps changed (new prerequisite, command changed) | `## Getting Started` |
| New security mechanism or change | `## Security Model` |
| Project structure changed (new folder, moved files) | `## Project Structure` |
| Engineering rule added to CLAUDE.md | `## Contributing / Development Notes` |

**Do NOT trigger for**: internal refactors with no external impact, test additions, renaming internal classes, log changes.

---

## Decision: Which Section to Update

```
Did a phase move from planned → done?
  → Update ## Project Status table

Did the endpoint list change (added, removed, path changed)?
  → Update ## API Reference → Endpoint Summary

Did we add a new env var or change a default?
  → Update ## Environment Variables

Did we add a table or change schema meaningfully?
  → Update ## Database Schema

Did we add a new file to docs/ or lessons/?
  → Update ## Documentation table

Did setup steps change (new prereq, new command, port changed)?
  → Update ## Getting Started

Multiple sections need updating?
  → Update all of them in one pass
```

---

## Rules for Editing README.md

- **Accuracy over completeness** — only document what currently works and exists
- **Commands must be copy-paste runnable** — test them mentally before writing
- **Endpoint list must match the codebase** — verify against controllers if unsure
- **Phase status** — only mark ✅ Done when all items in that phase are shipped and tested
- **No future plans in present tense** — "in progress" sections must say *(in progress)* or be in the Planned phase row
- **Keep the tone neutral and professional** — this is a portfolio README, not a blog post
- **Don't remove existing accurate content** — update in place, don't rewrite sections that are still correct

---

## Phase Status Values

| Symbol | Meaning |
|--------|---------|
| ✅ Done | All items shipped, tested, working |
| 🔄 In Progress | Actively being implemented |
| ⬜ Planned | Not started |
| ❌ Blocked | Blocked by dependency |

---

## Process

1. Read `README.md` to understand current state
2. Identify which sections need updating (use decision tree above)
3. Read the affected source files if needed to get accurate details
4. Edit only the sections that changed — don't rewrite unaffected content
5. Confirm with: "README updated: [list of sections changed]"

---

## What README.md Must Always Have (non-negotiable)

- Accurate `## Project Status` table
- Working `## Getting Started` commands (run locally + Docker)
- Accurate `## Endpoint Summary` (path + method only, no descriptions)
- Current `## Environment Variables` table
- Links to Postman collection and curl cheat sheet in `## Documentation`
- At least one working `curl` example in Getting Started verify step

---

## Context: Current README Sections

```
## What It Does          — domain model + task lifecycle diagram
## Tech Stack            — backend, frontend, infra tables
## Architecture          — hexagonal diagram + layer descriptions
## Project Structure     — directory tree
## Getting Started       — prereqs, run locally, docker, service URLs
## Running Tests         — gradle commands, coverage, quality checks
## API Reference         — postman, curl, swagger, endpoint list
## Security Model        — token types, rotation, bcrypt, session
## Database Schema       — table list, timestamps, soft deletes
## Environment Variables — table with defaults
## Project Status        — phase tracker with symbols
## Documentation         — links table
## Contributing          — dev rules (no Lombok, conventional commits, etc.)
```
