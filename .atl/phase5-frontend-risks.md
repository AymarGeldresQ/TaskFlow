# Phase 5 — Frontend Risks

Identified during proposal. Review before starting each slice.

## 🔴 High Impact

### R1 — Refresh token single-flight correctness
**Risk**: Two parallel requests get 401 simultaneously → two concurrent refresh calls → second call uses already-rotated token → backend rejects → loop or logout.
**Where**: `core/auth/refresh.interceptor.ts`
**Mitigation**: Implement single-flight with a shared `Observable` (BehaviorSubject or ReplaySubject). Spike this FIRST in design before writing any interceptor.
**Tests required**: 3 scenarios — parallel 401s, refresh-returns-401, logout-during-refresh.
**Status**: ⬜ Not addressed

---

### R2 — API contract drift (frontend models vs backend DTOs)
**Risk**: Backend returns a field name change or adds a required field → frontend silently breaks with `undefined`.
**Where**: `shared/models/` — all DTO interfaces
**Mitigation**: Maintain `shared/models` by hand for Phase 5. If drift appears more than twice, generate from OpenAPI in Phase 6.
**Status**: ⬜ Not addressed

---

## 🟡 Medium Impact

### R3 — Optimistic update race on drag-and-drop
**Risk**: User drags task fast → two PATCH /status requests in flight → out-of-order responses → board shows wrong column.
**Where**: `features/board/board.store.ts`
**Mitigation**: `groupBy(taskId) + mergeMap(group$ => group$.pipe(switchMap(...)))` with a `generations` Map. New drag bumps generation — stale rollback checks `getGeneration(taskId) === generation` before reverting.
**Resolved in**: PR-4. Test 7 in `board.store.spec.ts` explicitly verifies superseded-move scenario.
**Status**: ✅ Resolved — PR-4

---

### R4 — State machine drift (frontend allows invalid transitions)
**Risk**: Frontend enables dragging BACKLOG → DONE. Backend rejects with 422. Bad UX + silent data inconsistency.
**Where**: `shared/models/task-status.ts` — transitions map
**Mitigation**: Mirror exact backend transitions below. Disable invalid drop zones. Rollback on 422.

**Verified transitions** (`domain/model/TaskStatus.java`):
```
BACKLOG     → TODO, CANCELLED
TODO        → IN_PROGRESS, BACKLOG, CANCELLED
IN_PROGRESS → IN_REVIEW, TODO, CANCELLED
IN_REVIEW   → DONE, IN_PROGRESS, CANCELLED
DONE        → (terminal — no transitions)
CANCELLED   → (terminal — no transitions)
```

**⚠️ Spec assumption broken**: spec wrote "any→BACKLOG valid" — FALSE. Only TODO→BACKLOG is valid.
**⚠️ Proposal gap**: proposal defined 5 columns but backend has 6 statuses (includes CANCELLED). Decision needed: show CANCELLED as 6th column OR handle via action button (not drag). Recommend: action button — nobody drags to "Cancelled".

**Status**: ✅ Verified against backend — update spec before tasks

---

### R5 — Vitest + @testing-library/angular + Angular 21 incompatibility
**Risk**: Test framework versions don't fully support a new Angular 21 API → test suite can't run.
**Where**: `tsconfig.spec.json`, `angular.json` test builder
**Mitigation**: Scaffold already compiles. First test written during design proves it works. Fallback: Karma for affected suite only.
**Status**: ⬜ Not addressed

---

### R6 — Token storage strategy
**Risk**: Storing access token in memory = lost on refresh/tab close. Storing in localStorage = XSS risk. Wrong choice affects UX and security.
**Where**: `core/auth/token-storage.ts`
**Mitigation**: Decide in design (document in a lesson). Default plan: access in memory + refresh in httpOnly cookie OR localStorage with short TTL. Do not re-litigate after decision.
**Status**: ⬜ Not addressed

---

## 🟢 Low Impact (watch but don't block)

### R7 — Kanban polish scope creep
**Risk**: Animations, virtual scroll, multi-select drag, keyboard reordering — each one "just 2 hours."
**Where**: `features/board/`
**Mitigation**: Enforced. Board shipped with: drag works, optimistic rollback works, filters work. Nothing else added.
**Status**: ✅ Contained — PR-4

---

### R8 — Initial bundle > 500 KB
**Risk**: Importing too much in the shell chunk inflates initial load.
**Where**: `app.routes.ts` — lazy boundaries
**Mitigation**: All features lazy-loaded. Budget revised to 400kB warning / 500kB error — the original 200kB was below Angular + Material 21 baseline (~305kB uncompressed). Build passes clean.
**Status**: ✅ Resolved — PR-6

---

---

## Risks identified during Design

### R9 — `localStorage` refresh token XSS-readable
**Risk**: Refresh token in `localStorage` accessible to any JS running on the page.
**Where**: `core/auth/token-storage.service.ts`
**Mitigation**: Strict CSP + Angular sanitization + no third-party scripts. Documented in ADR-003 as accepted trade-off. Phase 6 candidate: move to `httpOnly` cookie via backend `Set-Cookie`.
**Status**: ❌ Accepted for Phase 5 — revisit in Phase 6

---

### R10 — CDK drag predicate signature
**Risk**: Design assumes `cdkDropListEnterPredicate` accepts `(drag, drop) => boolean` for blocking invalid transitions. If Angular CDK 21 changed the signature, transition enforcement breaks silently.
**Where**: `features/board/board-column.component.ts`
**Mitigation**: Verified in PR-4. Signature is `(drag: CdkDrag<Task>): boolean` — CDK 21 accepts single-param. `canDrop = (drag) => canTransition(drag.data.status, this.status())`.
**Resolved in**: PR-4. `board-column.component.spec.ts` verifies IN_REVIEW→BACKLOG=false and TODO→IN_PROGRESS=true.
**Status**: ✅ Resolved — PR-4

---

### R11 — `roleGuard` needs `TeamMembershipStore` pre-loaded on deep link
**Risk**: User lands directly on `/teams/:teamId/settings` — guard fires before store is populated → guard fails or allows incorrectly.
**Where**: `core/auth/role.guard.ts`, `core/layout/shell.ts` route
**Mitigation**: Pre-load active team membership inside Shell parent route (resolves before children mount). Document as apply constraint.
**Status**: ⬜ Not addressed

---

### R12 — `switchMap` cancels client but not server-side PATCH
**Risk**: Fast drag → switchMap cancels first PATCH on client → server still processes it → intermediate state applied then immediately overwritten by second PATCH. Acceptable because status set is idempotent, but confusing in logs.
**Where**: `features/board/board.store.ts` — `moveTask()`
**Mitigation**: Accepted behavior. Add comment in code + note in lesson.
**Status**: ❌ Accepted — document in code + lesson

---

---

## Risks found during Apply — PR-1

### R13 — `provideAnimationsAsync` deprecated in Angular 21
**Risk**: Angular ESLint marks `provideAnimationsAsync` as `@deprecated`. Angular v23 will remove it.
**Where**: `src/app/app.config.ts`
**Mitigation**: Suppressed with `eslint-disable-line` for now — no replacement exists yet. Phase 6: migrate when Angular recommends an alternative.
**Status**: ❌ Accepted for Phase 5 — revisit in Phase 6

---

### R14 — Double `clearAuth()` on refresh failure
**Risk**: `refreshInterceptor` calls `AuthStore.clearAuth()` on refresh failure AND `RefreshCoordinator` already called it via `logout()`. Harmless double-clear but confusing in debugging.
**Where**: `core/auth/interceptors/refresh.interceptor.ts`, `core/auth/refresh-coordinator.service.ts`
**Mitigation**: Accepted — idempotent, no side effects. Add a comment in refresh.interceptor.ts.
**Status**: ❌ Accepted — document in code

---

## Status Legend

| Symbol | Meaning |
|--------|---------|
| ⬜ | Not addressed yet |
| 🔄 | In progress / spiked |
| ✅ | Resolved — update with how |
| ❌ | Accepted / out of scope — note why |
