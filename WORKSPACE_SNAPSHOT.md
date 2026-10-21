# Workspace Snapshot

- Updated: 2026-10-21 (+07:00): W0 of `docs/V423_WORKSPACE_NAV_WORK_REQUEST.md` is implemented on `feature/v4.23`; the workspace model merges live `chapter_sets` with saved runs, the full read-only job query includes `start_session_id` and resumed job ids, and Jobs has a Tập filter applied before the 30-card cap. Targeted `WorkspaceSetModelTest` and `RunGroupsTest` pass. The owner-phone rule remains active; `artifacts/qa/v4.23-dev14-20261021/media/` is untracked and untouched.
- Current version: v4.23.0 in development (not released); contains unreleased v4.22 code; shipped 4.21.0 / code 271 (tag `v4.21.0`).
- Current branch: `feature/v4.23` in worktree `D:\P5E-builds\wt-v4.23` (BASE `c8408207` on `feature/v4.22`).
- Current commit: `c3b9bdfe` is the implementation baseline immediately before this snapshot commit. Confirm actual HEAD when resuming.
- Current build: 4.23-dev.13 / code 291 (APK SHA-256 `64B9824A9E160F9933C3E162FA79713EA6E27F54B4925D2C86327894CB314499`); installed on owner phone `15e84958` on 2026-10-20 and on emulator-5554. No new APK has been built in this W0 session.
- Current phase: W0 complete; W1 in progress. No phone install is authorized without a fresh owner approval in this terminal.
- Completed tasks: startup authority/state read; W0 workspace merge, full job query, deleted-set labeling and Jobs filter; targeted W0 tests.
- Pending tasks: W1 one-gate navigation and left drawer; W1 regression/build `4.23-dev.14` and owner-approved phone QA; W2 Volume page, NavState and owner-approved `4.23-dev.15` phone QA.
- Known bugs/limits: drawer, Volume and `Mở tập` are not implemented yet; phone behavior for this work request is NOT_MEASURED; the prior emulator media directory remains untracked.
- Regression status: targeted W0 tests PASS (`WorkspaceSetModelTest`, `RunGroupsTest`); full archive regression and lint have not yet been rerun after W0. Editorial boundary: no `editorial-engine/` or `Editorial*` file edited.
- Semantic progress: unchanged; spend USD 0 and no provider call.
- Next action: implement W1 `navigateTo`, the button-opened Tập drawer, and translation-side label updates.
