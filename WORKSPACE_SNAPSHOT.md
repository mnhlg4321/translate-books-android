# Workspace Snapshot

- Updated: 2026-10-18 (+07:00): dev.1 (L1+L2) built, clean-archive regression PASS, device QA found 4 schema-version assertions (29 -> 30) in two Editorial device tests; fixed in test code; L3/L4 code in progress.
- Current version: v4.23.0 in development (not released); contains unreleased v4.22 code; shipped 4.21.0 / code 271 (tag `v4.21.0`).
- Current branch: `feature/v4.23` in worktree `D:\P5E-builds\wt-v4.23` (BASE `c8408207` on `feature/v4.22`).
- Current commit: `d2cc1a93` (L1 storage) is the last commit before the L1 UI + L2 commit; this snapshot describes the working tree of that commit. Confirm actual HEAD when resuming.
- Current build: `4.23-dev.1` / code 276 (event `build-20261018-164751`, source `3848ebeb`, APK SHA-256 `22B24F0C6BC8CF32258DF7D9CA70B3AFA796EFD0BE564F41D3EB53114753799E`), emulator-5554 only; test APK event `v423-dev1-test-20261018`.
- Current phase: step 05 develop: L1 and L2 done on host; building `4.23-dev.1`, clean-archive regression and emulator QA, then stop for the owner.
- Completed tasks: steps 01-04; L1 storage (schema 30 `library_raw`/`chapter_sets`/`chapter_set_rows`, `tblraw://` in `FileUtil`, `RawFileStore`, `RawImportPlanner`, library ZIP format 2 with RAW); L1 RAW tab (import, search, view, rename, replace, delete); L2 selection (`LibrarySelectionController`, `LibraryBulkUi`, delete plan with second confirm, 10 s undo, export, Use); `TranslateTestSeam` (test-only provider and output folder).
- Pending tasks: `4.23-dev.1` build, clean-archive regression, emulator QA and screenshots, owner report; L3 chapter sets; L4 per-chapter translation; `4.23-dev.2`; L5 owner trial D1-D7.
- Known bugs/limits: as v4.22 (`feature/v4.22` snapshot); a job report prints raw settings JSON when it cannot parse it.
- Regression status: working tree: app host 533 tests / 5 SKIP / 0 FAIL; androidTest compile PASS; engine, lint, release and benchmark variants not rerun yet. Three Editorial schema-version assertions (`VER = 29` -> `VER = 30`) were bumped in `EditorialApiV26MigrationSpecTest`, `EditorialPairV27MigrationSpecTest`, `EditorialPairV29MigrationSpecTest`; `git diff c8408207 -- editorial-engine app/.../Editorial*` is empty.
- Semantic progress: unchanged.
- Spend: USD 0; no provider call. Phone `15e84958` not touched.
- Next action: rebuild the dev.1 test APK from a clean worktree after the test fix, rerun the failed device groups and restart phases by method, take screenshots, write the dev.1 QA report and stop for the owner.
