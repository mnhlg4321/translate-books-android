# Workspace Snapshot

- Updated: 2026-10-18 (+07:00): v4.23 opened from `feature/v4.22` `c8408207` for the Translate/Library work request.
- Current version: v4.23.0 in development (not released); contains unreleased v4.22 code; shipped 4.21.0 / code 271 (tag `v4.21.0`).
- Current branch: `feature/v4.23` in worktree `D:\P5E-builds\wt-v4.23` (BASE `c8408207` on `feature/v4.22`).
- Current commit: `c8408207` is the implementation baseline immediately before this snapshot commit (not a self-reference). Confirm actual HEAD when resuming.
- Current build: none on v4.23 yet; latest numbered build `4.22-dev.3` / code 275 (emulator only). Next: `4.23-dev.1`, code >= 276.
- Current phase: step 05 develop, L1 (Library RAW tab).
- Completed tasks: steps 01-04 (`release_checklists/v4.23.md`), worktree, `local.properties`, archive junctions.
- Pending tasks: L1 RAW tab, L2 multi-select, `4.23-dev.1` build + clean-archive regression + emulator QA + owner report; L3 chapter sets, L4 per-chapter translation, `4.23-dev.2` + QA + owner report; L5 owner trial D1-D7.
- Known bugs/limits: as v4.22 (`feature/v4.22` snapshot); a job report prints raw settings JSON when it cannot parse it.
- Regression status: inherited from v4.22 `4032ff39` clean archive (engine 742 / 1 SKIP, app 516 / 5 SKIP per variant, 0 FAIL); not rerun on v4.23 yet.
- Semantic progress: unchanged.
- Spend: USD 0; no provider call. Phone `15e84958` not touched.
- Next action: implement L1 storage (schema 30, `library_raw`, internal RAW files, `tblraw://` in `FileUtil`).
