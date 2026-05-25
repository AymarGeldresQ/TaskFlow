# Skill Registry — TaskFlow

Generated: 2026-05-25

## Project Skills (`.claude/skills/`)

| Skill | Trigger |
|-------|---------|
| `update-lessons` | Bug fixed, architectural decision made, new pattern introduced, library chosen, non-obvious behavior explained |
| `update-readme` | Phase completed, endpoint added/removed, new env var, schema change, new doc file, setup change, security change |

## User Skills (`~/.claude/skills/`)

| Skill | Trigger |
|-------|---------|
| `branch-pr` | Creating a PR or branch from current work |
| `chained-pr` | Splitting large changes into stacked PRs |
| `cognitive-doc-design` | Writing technical documentation |
| `comment-writer` | Writing code comments or docstrings |
| `go-testing` | Go test files, Bubbletea TUI testing |
| `issue-creation` | Creating GitHub issues |
| `judgment-day` | Resolving architectural trade-offs |
| `skill-creator` | Creating new AI skills |
| `work-unit-commits` | Writing structured commits with work units |

## Project Convention Files

| File | Purpose |
|------|---------|
| `CLAUDE.md` | Project instructions, skill auto-load triggers, architecture rules |

## Compact Rules

### update-lessons
Trigger: after any bug fix, architectural decision, non-obvious pattern, or library choice.
1. Check if topic exists in `lessons/` → update existing lesson OR create new `NN-kebab-case.md`
2. Required sections: `## The Concept`, `## In This Project` (real file paths + code), `## What Breaks Without It`, `## Key Files`
3. For bugs: add `## Bug Fixed` with Symptom / Root cause / Fix / Why
4. Update `lessons/README.md` index table if new lesson added

### update-readme
Trigger: phase completed, endpoint changed, new env var, schema change, new doc.
1. Read current `README.md` — update only the section affected
2. Keep phase table status in sync (⬜ Planned / 🔄 In Progress / ✅ Done)
3. Never add speculative sections — only reflect what's actually built

### work-unit-commits
Use conventional commits. No "Co-Authored-By" AI attribution. Never `--no-verify`.

### branch-pr
PR title ≤70 chars. Body: Summary bullets + Test plan checklist.
