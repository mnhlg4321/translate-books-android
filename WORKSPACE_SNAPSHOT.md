# Workspace Snapshot

- Updated: 2026-10-18 (+07:00): L1 RAW tab and L2 multi-select implemented with host tests (533 / 5 SKIP / 0 FAIL); device tests written; `4.23-dev.1` build and QA next.
- Current version: v4.23.0 in development (not released); contains unreleased v4.22 code; shipped 4.21.0 / code 271 (tag `v4.21.0`).
- Current branch: `feature/v4.23` in worktree `D:\P5E-builds\wt-v4.23` (BASE `c8408207` on `feature/v4.22`).
- Current commit: `d2cc1a93` (L1 storage) is the last commit before the L1 UI + L2 commit; this snapshot describes the working tree of that commit. Confirm actual HEAD when resuming.
- Current build: none on v4.23 yet; latest numbered build `4.22-dev.3` / code 275 (emulator only). Next: `4.23-dev.1`, code >= 276.
- Current phase: step 05 develop: L1 and L2 done on host; building `4.23-dev.1`, clean-archive regression and emulator QA, then stop for the owner.
- Completed tasks: steps 01-04; L1 storage (schema 30 `library_raw`/`chapter_sets`/`chapter_set_rows`, `tblraw://` in `FileUtil`, `RawFileStore`, `RawImportPlanner`, library ZIP format 2 with RAW); L1 RAW tab (import, search, view, rename, replace, delete); L2 selection (`LibrarySelectionController`, `LibraryBulkUi`, delete plan with second confirm, 10 s undo, export, Use); `TranslateTestSeam` (test-only provider and output folder).
- Pending tasks: `4.23-dev.1` build, clean-archive regression, emulator QA and screenshots, owner report; L3 chapter sets; L4 per-chapter translation; `4.23-dev.2`; L5 owner trial D1-D7.
- Known bugs/limits: as v4.22 (`feature/v4.22` snapshot); a job report prints raw settings JSON when it cannot parse it.
- Regression status: working tree: app host 533 tests / 5 SKIP / 0 FAIL; androidTest compile PASS; engine, lint, release and benchmark variants not rerun yet. Three Editorial schema-version assertions (`VER = 29` -> `VER = 30`) were bumped in `EditorialApiV26MigrationSpecTest`, `EditorialPairV27MigrationSpecTest`, `EditorialPairV29MigrationSpecTest`; `git diff c8408207 -- editorial-engine app/.../Editorial*` is empty.
- Semantic progress: unchanged.
- Spend: USD 0; no provider call. Phone `15e84958` not touched.
- Next action: commit L1 UI + L2, build `4.23-dev.1` with `scripts/build-and-save.ps1 -Series 4.23-dev -MinimumVersionCode 276`, run the clean-archive regression and emulator QA.
