# Phase 5 — Follow-up Items (post-verify warnings)

Identified by `sdd-verify`. Non-blocking — Phase 5 archived clean. Address in Phase 6 or a follow-up PR.

---

## W3 — NFR-5: No lint rule enforcing presentational component purity

**Issue**: `task-detail-panel.component.ts` injects `AuthStore` and `LabelsStore` directly. NFR-5 says presentational components (`*-page` excluded) MUST NOT inject stores. No ESLint rule currently enforces this.
**Where**: `eslint.config.js` + `features/tasks/task-detail-panel/task-detail-panel.component.ts`
**Fix**: Add ESLint `no-restricted-imports` pattern targeting `**/components/**/*.ts` that blocks imports from `*.store.ts`. Then refactor `task-detail-panel` to receive data via `@Input()` from `BoardPageComponent`.
**Effort**: ~1h (rule + refactor)

---

## W4a — Scenario B4 (cancel task) untested

**Issue**: `task-detail-panel.component.ts` has a "Cancel task" button but no spec test verifies the confirmation dialog → PATCH CANCELLED → task removed from board flow.
**Where**: `features/tasks/task-detail-panel/task-detail-panel.component.spec.ts` (file doesn't exist yet)
**Fix**: Create spec, test: confirm dialog appears → on confirm calls `TaskDetailStore.cancelTask()` → task removed from `BoardStore.tasks`.
**Effort**: ~30min

---

## W4b — Scenario C2 (OWNER/ADMIN UI controls hidden for VIEWER) untested

**Issue**: `team-detail-page.component.ts` hides invite/remove buttons for non-OWNER/ADMIN users but no test verifies the DOM state for a VIEWER.
**Where**: `features/teams/team-detail/team-detail-page.component.spec.ts`
**Fix**: Create spec with a mocked VIEWER role in `TeamMembershipStore` → render page → assert invite button absent from DOM.
**Effort**: ~30min

---

## W4c — Scenario D1 (label toggle optimistic + rollback) undertested

**Issue**: `label-selector.component.spec.ts` tests the toggle emission but not the optimistic-then-rollback path through `TaskDetailStore`.
**Where**: `features/tasks/task-detail.store.spec.ts` — extend existing file
**Fix**: Add 2 tests: `toggleLabel success` (chip stays selected) and `toggleLabel rollback` (API fails → chip reverts + error signal set).
**Effort**: ~20min

---

## W4d — Scenario E1 (logout clears all state) untested

**Issue**: `AuthStore.logout()` clears tokens and navigates to `/login` but no test verifies the full sequence: token signal null + localStorage cleared + navigation.
**Where**: `core/auth/auth.store.spec.ts` (file doesn't exist yet)
**Fix**: Create spec, test: call `logout()` → `accessToken()` is null + `localStorage.getItem('tf.refresh')` is null + router navigated to `/login`.
**Effort**: ~20min

---

## W2 — RefreshCoordinator mutex deviation (cosmetic)

**Issue**: Design spec described a `refreshing: boolean` flag. Implementation uses `refresh$ !== null` as the mutex. Functionally identical — noted for documentation accuracy.
**Where**: `core/auth/refresh-coordinator.service.ts`
**Fix**: Either rename to align with implementation OR update lesson 13 to reflect the actual pattern. No behavioral change needed.
**Effort**: 5min (docs only)

---

## Total estimated effort: ~2h

All items are test-coverage or minor architectural consistency fixes. Zero breaking changes. Can be done as a single follow-up PR before Phase 6 starts.
