# Workspace Snapshot

- Updated: `2026-09-03` (+07:00).
- Current phase/status: `P2_COMPLETE / P3A_GAP012_COMPLETE / IMPORT_ACCEPTANCE_PASS / NOT_RUNNABLE / NOT_CERTIFIED`.
- Current version/build: device restored to verified baseline `4.17-dev.1` / code169. P3A validation APK `4.17-dev.2`/code170 was archived and used only for the importer test run; no v4.18 release build or source build-metadata change.
- Current branch/worktree: `feature/v4.18` / `D:\App Translate Books\App Translate Books-translation-profile`.
- Current commit baseline: `1090661140a4f970c4bd033e6314fc28f6b5ab04`, the P2 Android acceptance baseline immediately before the P3A change/documentation commit; not self-referential.
- P0/P1 checkpoint: `aef7da1` (`chore(editorial): checkpoint P0 P1 characterization evidence`).
- Current build: `TranslateBooks-v4.17-dev.1-code169.apk`, SHA-256 `3C3AAEF1A7D47F39A7B5A5FF8AEDF77347D255AF728B908180AA142CA2B276D1`; artifact/backup parity preserved.
- Active authority: `EDITORIAL_RECOVERY_V4_18.md`.
- Editorial source: `V5-SAFE.4.1.3-FULL`; exact Project/Prompt/Workflow hashes are recorded in the plan and Pack v1 integration contract.
- Completed: repaired the moved v4.17 Git worktree; verified clean CODE169 baseline; created `feature/v4.18`; reran external static qualification `306 PASS / 0 FAIL`; audited existing Pack Manifest v1/import/storage/compatibility code; completed P1 characterization and P2A frozen reference pack; isolated the P2 negative-fixture registry in test setup; fixed only `EditorialPackImportService` for GAP-012 by draining the current stream before EOCD validation; completed P2B acceptance. On `15e84958`/API 35, focused importer `13/13`, P1 `7/7`, P2 `3/3`, and full instrumentation `103/103` pass. Canonical/control readback is exact, re-import is idempotent, synthetic 4.1.4 is side-by-side, and security negatives remain fail-closed. No provider/API call was made.
- Design decision: reuse the existing four-root-entry Pack Manifest v1. Imported runtime payload is `editorial-pack.json` plus exact Project/Prompt/Workflow files. External APP/COMMON/TESTS content is design-time specification/evidence and is never executed by Android.
- Pending: P3B trusted runtime contract/profile with failing contract tests for remaining gaps; then P4 binding/resume UX, P5 real L1 pilot, P6 L2/L3, P7 regression/build/device QA/release gates. Do not open execution from importer acceptance.
- Known bugs/limitations: 4.1.3 has no real-chapter/model/provider proof; current engine profile supports only integrity and Editorial execution remains disabled. GAP-012 is resolved for valid Pack Manifest v1 transport. Trusted runtime contract/profile, certification and execution remain unimplemented.
- Regression status: v4.18 engine suite `125/125` pass; app unit suite `210/210` pass. On `15e84958`/API 35, focused importer `13/13`, P1 `7/7`, P2 `3/3`, and full instrumentation `103/103` pass; real API is skipped by explicit opt-in, provider/API calls `0`. Preserved CODE169 artifact/device evidence remains baseline; external pack static suite passed `306/306` on `2026-09-03`. Test count remains 103 before/after P3A.
- Protected state: `D:\App Translate Books` D1 checkout and its three user-owned `.idea` changes remain untouched.
- Exact next action: hand off only `P3B — trusted runtime contract/profile` with the remaining failing contract tests. Keep `NOT_RUNNABLE / NOT_CERTIFIED`; P2B/importer success is not execution evidence.

This is current-only state; Git history preserves prior snapshots.
