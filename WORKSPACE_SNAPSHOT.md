# Workspace Snapshot

- Updated: 2026-10-20 (+07:00): J1-J6 of docs/V423_PERF_RUN_UX_WORK_REQUEST.md implemented; clean-archive regression passed; 4.23-dev.12 built and run on emulator-5554; stopped for the owner install approval.
- Current version: v4.23.0 in development (not released); contains unreleased v4.22 code; shipped 4.21.0 / code 271 (tag `v4.21.0`).
- Current branch: `feature/v4.23` in worktree `D:\P5E-builds\wt-v4.23` (BASE `c8408207` on `feature/v4.22`).
- Current commit: `d4d0ad54` is the implementation baseline before this snapshot commit (test-only commits may follow). Confirm actual HEAD when resuming.
- Current build: 4.23-dev.12 / code 290 (event build-20261020-215555, source d4d0ad54, APK SHA-256 CD1FE14368A35A6737F64040683879DB6C65FE0C48D263A2503F006C6ECCF589); installed on emulator-5554 only; owner phone 15e84958 still runs 4.23-dev.11 / code 286 (not touched).
- Current phase: stop point after J5 slice B reached: regression, dev.12 build and emulator QA done; waiting for the owner decision to install on the phone (needs a run-as backup from dev.11 first).
- Completed tasks: steps 01-04; L1-L4; U-series; J1 (SAF probe off the UI thread, async log writer, caches, in-place ticks), J2 + J6 (chapter-local validation failures, run cost cap, stop after 2, continue set, bracket rule against RAW, cut-answer retry or split, quote repair, missing lines, reasoning effort), J3 progress panel, J4 wrap buttons, J5 slices A and B.
- Pending tasks: owner approval to install dev.12 on the phone; trial K1-K8; release decision (v4.22 and v4.23 order).
- Known bugs/limits: Xem tat ca opens Jobs without a filter; no search box in Glossaries/Pronouns; 66 old test jobs left on the emulator by older test classes; every phone behaviour NOT_MEASURED until the owner tries G1-G6.
- Regression status: clean archive d4d0ad54: engine 742 / 1 SKIP; app debug, release, benchmark 647 each / 5 SKIP; 0 FAIL; lint 0 errors / 61 warnings; AndroidTest compile PASS; device QA in artifacts/qa/v4.23-dev12-20261020. Editorial boundary: no Editorial source or test edited.
- Semantic progress: unchanged.
- Spend: USD 0; no provider call. Phone 15e84958: owner approved the install in chat 2026-10-18; backup before install in D:/App Translate Books/App Translate Books-device-backups/v4.23-dev2-code277/20261018-213422 (DB sha256 e3820252...107134 matches phone and archive); 4.23-dev.5 / code 280 installed with install-validated.ps1, first launch without crash.
- Next action: ask the owner to approve installing 4.23-dev.12 on phone 15e84958 (back up with run-as from dev.11 first), then trial K1-K8.
