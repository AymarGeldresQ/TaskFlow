# Lesson 13 — Frontend Architecture Decisions: How We Found and Resolved the Hard Problems

## The Concept

Before writing a single line of Angular code, we ran a structured planning phase (SDD: Spec-Driven Development) that produced a proposal, a spec, and a technical design. Each phase forced us to think through risks explicitly.

The result: we found 12 risks before implementation, resolved 7 of them in design, and corrected a wrong assumption in the spec — all without touching the codebase.

This lesson documents the decisions that matter: WHY we made them, what we rejected, and what breaks if you ignore them.

---

## How We Identified the Risks

The proposal phase produced an initial risk table. But two of the most important findings came from CROSS-REFERENCING the spec assumptions against the actual backend code.

### The State Machine Mismatch (found during spec review)

The proposal defined "5 Kanban columns: BACKLOG → TODO → IN_PROGRESS → IN_REVIEW → DONE." The spec was written with an assumption: "any task can be dragged back to BACKLOG."

Before running the tasks phase, we read the actual backend source:

```java
// backend/domain/model/TaskStatus.java
public boolean canTransitionTo(TaskStatus target) {
    return switch (this) {
        case BACKLOG     -> target == TODO || target == CANCELLED;
        case TODO        -> target == IN_PROGRESS || target == BACKLOG || target == CANCELLED;
        case IN_PROGRESS -> target == IN_REVIEW || target == TODO || target == CANCELLED;
        case IN_REVIEW   -> target == DONE || target == IN_PROGRESS || target == CANCELLED;
        case DONE, CANCELLED -> false;  // ← TERMINAL. No transitions out.
    };
}
```

Two things the proposal got wrong:

1. **"Any → BACKLOG" is false.** Only `TODO → BACKLOG` is valid. `IN_PROGRESS → BACKLOG` is rejected with 422.
2. **`CANCELLED` exists.** The proposal specified 5 columns. The backend has 6 statuses. `CANCELLED` is terminal.

If we had written the frontend Kanban with the proposal assumption, every drag from IN_PROGRESS back to BACKLOG would return 422. The optimistic update would show the task in the wrong column, the rollback would snap it back — a confusing, broken UX.

**The fix**: mirror the exact transitions in `shared/models/task-status.ts` and use them to disable invalid drop zones:

```typescript
// shared/models/task-status.ts
export const TASK_STATUS_TRANSITIONS: Record<TaskStatus, TaskStatus[]> = {
  BACKLOG:     ['TODO', 'CANCELLED'],
  TODO:        ['IN_PROGRESS', 'BACKLOG', 'CANCELLED'],
  IN_PROGRESS: ['IN_REVIEW', 'TODO', 'CANCELLED'],
  IN_REVIEW:   ['DONE', 'IN_PROGRESS', 'CANCELLED'],
  DONE:        [],       // terminal
  CANCELLED:   [],       // terminal
};

export function canTransition(from: TaskStatus, to: TaskStatus): boolean {
  return TASK_STATUS_TRANSITIONS[from].includes(to);
}
```

**What breaks without it**: frontend allows the drag, optimistic update shows wrong column, PATCH returns 422, rollback fires, task snaps back. Broken UX that silently teaches the user your software is unreliable.

**Decision on CANCELLED**: not a 6th drag column. It becomes a "Cancel task" action button inside the task detail panel. Nobody drags to "Cancelled" — they click a button. Drag columns = the 5 forward-flow statuses only.

---

## The Three Hard Technical Decisions (resolved in design)

### Decision 1 — Refresh Token Single-Flight

**The problem**: `authInterceptor` attaches a JWT to every request. When the access token expires, two parallel requests both get 401 simultaneously. Without coordination, both try to refresh → second refresh call uses the already-rotated token → backend returns 401 again → infinite loop or force logout.

**What we considered**:
- Option A: Queue all requests, refresh once, replay. Complex but correct.
- Option B: Ignore the problem, let the second 401 fail. Simple but broken — user gets logged out randomly.
- Option C: `BehaviorSubject` as a flag, queue on `filter(token => token !== null)`. Standard pattern.

**What we chose**: `ReplaySubject<string>(1)` + a `RefreshCoordinator` singleton.

```typescript
// core/auth/refresh-coordinator.ts
@Injectable({ providedIn: 'root' })
export class RefreshCoordinator {
  private refreshing = false;
  private refresh$ = new ReplaySubject<string>(1);

  getOrRefresh(refreshFn: () => Observable<string>): Observable<string> {
    if (!this.refreshing) {
      this.refreshing = true;
      this.refresh$ = new ReplaySubject<string>(1);
      refreshFn().subscribe({
        next: token => { this.refresh$.next(token); this.refreshing = false; },
        error: err  => { this.refresh$.error(err);  this.refreshing = false; },
      });
    }
    return this.refresh$.asObservable();
  }
}
```

Why `ReplaySubject(1)` and not `BehaviorSubject`: `BehaviorSubject` requires an initial value and emits it immediately — subscribers get `null` before the refresh resolves. `ReplaySubject(1)` only emits when we push a value, then replays to late subscribers.

**Why this matters for the portfolio**: this is the difference between "I built an interceptor" and "I understand exactly why parallel 401s break and how to serialize them." It is the single most common auth bug in Angular apps.

---

### Decision 2 — Optimistic Update Race on Drag-and-Drop

**The problem**: user drags task A to TODO, then immediately drags it to IN_PROGRESS before the first PATCH resolves. Two PATCH requests in flight. If they arrive out of order at the server, the board shows IN_PROGRESS but the server stored TODO.

**What we considered**:
- Option A: Per-task request queue — each task has a queue, new PATCH waits for previous one. Correct but adds complexity to the store.
- Option B: `switchMap` per task — new drag cancels the previous PATCH observable on the CLIENT (server still processes it). Simpler. Works because status set is idempotent: whichever PATCH arrives last wins, and the last one is always the correct final state.
- Option C: Debounce — wait 300ms before sending. Loses the "instant" feel of drag-and-drop.

**What we chose**: `switchMap` per task-id.

```typescript
// features/board/board.store.ts (skeleton)
private readonly moveTask$ = new Subject<{ taskId: string; from: TaskStatus; to: TaskStatus }>();

constructor() {
  this.moveTask$.pipe(
    groupBy(e => e.taskId),
    mergeMap(group$ =>
      group$.pipe(
        switchMap(({ taskId, from, to }) => {
          // 1. Optimistic update
          this.applyOptimisticMove(taskId, to);
          const generation = this.getGeneration(taskId);

          return this.taskApiClient.patchStatus(taskId, to).pipe(
            catchError(() => {
              // Rollback only if no newer move has already replaced this one
              if (this.getGeneration(taskId) === generation) {
                this.applyOptimisticMove(taskId, from);
              }
              return EMPTY;
            })
          );
        })
      )
    )
  ).subscribe();
}
```

**Important caveat**: `switchMap` cancels the Observable on the client — the HTTP request may still reach the server and apply. This is ACCEPTED because: (a) the final PATCH supersedes any intermediate one, (b) the server state converges to correct. Add a comment in the code explaining this.

---

### Decision 3 — Token Storage

**The problem**: where do you store the JWT access token and the refresh token in a browser?

| Option | Access token | Refresh token | XSS risk | CSRF risk |
|--------|-------------|---------------|----------|-----------|
| localStorage (both) | ✅ survives refresh | ✅ survives refresh | **HIGH** | Low |
| Memory (both) | ✅ fast | ❌ lost on tab close | Low | Low |
| Memory + httpOnly cookie (refresh) | ✅ fast | ✅ survives refresh | **Low** | Medium |
| Memory + localStorage (refresh) | ✅ fast | ✅ survives refresh | Medium | Low |

**Why httpOnly cookie is ideal but blocked**: the backend's `/auth/refresh` returns the refresh token in the JSON response body — it does NOT set a `Set-Cookie` header. Moving to `httpOnly` requires a backend change. That's a Phase 6 decision.

**What we chose for Phase 5**: access token in memory (Angular signal in `AuthStore`) + refresh token in `localStorage` with rotation.

- Access token in memory: never in the DOM, never accessible to XSS.
- Refresh token in `localStorage`: survives tab close (no re-login after refresh). XSS risk mitigated by Angular's automatic XSS sanitization + strict CSP + no third-party inline scripts.

**What the "phase 6 upgrade path" looks like**: backend adds `Set-Cookie: refreshToken=...; HttpOnly; Secure; SameSite=Strict`. Frontend removes `localStorage.setItem(REFRESH_KEY, ...)`. The `authInterceptor` stays identical (browser sends the cookie automatically). Net change: ~5 lines of frontend code.

---

## In This Project

| File | Decision documented |
|------|-------------------|
| `shared/models/task-status.ts` | `TASK_STATUS_TRANSITIONS` map + `canTransition()` |
| `core/auth/refresh-coordinator.ts` | `ReplaySubject` single-flight |
| `core/auth/token-storage.service.ts` | Memory (access) + localStorage (refresh) |
| `features/board/board.store.ts` | `groupBy` + `switchMap` per task optimistic pattern |
| `.atl/phase5-frontend-risks.md` | Full risk register — all 12 risks with status |

---

## What Breaks Without It

| Skip this | What breaks |
|-----------|-------------|
| State machine map | `IN_PROGRESS → BACKLOG` drag gets 422. Rollback snaps task. Looks like a bug. |
| `CANCELLED` as button (not column) | 6th Kanban column appears. Users drag tasks to "Cancelled" accidentally. |
| Refresh single-flight | Parallel 401s → two refresh calls → second uses rotated token → 401 again → force logout. Random user sessions terminated. |
| Optimistic update generation | Fast drag → wrong column persists on screen after rollback. Board is out of sync with server. |
| Memory for access token | JWT stored in `localStorage` → readable by any injected script → full session takeover. |

---

## The Rule to Remember

The spec is a hypothesis. Verify it against the source of truth before implementation. In this project, the source of truth for task transitions is `domain/model/TaskStatus.java` — not the proposal, not the spec, not your intuition.

---

## Key Files

| File | What to study |
|------|---------------|
| `backend/domain/model/TaskStatus.java` | Ground truth for all valid task transitions |
| `frontend/shared/models/task-status.ts` | Mirror of the backend state machine |
| `frontend/core/auth/refresh-coordinator.ts` | ReplaySubject single-flight pattern |
| `frontend/features/board/board.store.ts` | groupBy + switchMap optimistic update |
| `.atl/phase5-frontend-risks.md` | Full risk register with resolution status |
