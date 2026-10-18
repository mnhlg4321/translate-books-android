# Workspace Snapshot

- Updated: 2026-10-18 (+07:00): F1 done (build archive linked, QA media committed); S0.1-S0.6 and S0.8 implemented with host tests; device test for S0 written, not run.
- Current version: v4.22.0 in development (not released); shipped 4.21.0 / code 271 (tag `v4.21.0`).
- Current branch: `feature/v4.22` in worktree `D:\P5E-builds\wt-v4.22` (from `main` `7c44f3a2`).
- Current commit: `1ee90be4` is the implementation baseline immediately before this snapshot commit (not a self-reference); this commit carries the S0 code and tests. Confirm actual HEAD when resuming.
- Current build: `4.22-dev.1` / code 274 (event `build-20261015-090335`, source `e8083883`) contains neither R nor S0. Phone `15e84958`: `4.22-dev.2` / code 273, DEBUGGABLE (read-only `dumpsys` 2026-10-18).
- Current phase: step 05 develop: R and S0 done in source; next F2 clean-archive regression, F3, F4 dev build `4.22-dev.3` (code 275) and emulator QA, then owner install approval and trial.
- Completed tasks: U1, U2, U3, P, G, Q1 design, R (see earlier state in BUILD_STATE.md); F1 (archive junctions, media `1ee90be4`); S0.1-S0.6, S0.8 and the S0.7 test code; work requests `docs/V422_DEVICE_TRIAL_RELEASE_WORK_REQUEST.md` (F1-F9, trial sheet T1-T13), `docs/V422_S0_LIBRARY_SAFETY_WORK_REQUEST.md` (S0.1-S0.8) and proposal `docs/V423_LIBRARY_BATCH_PROPOSAL.md` (reviewed by a critic agent against the code).
- Pending tasks: S0.7 device run (in F4), F2 clean-archive regression, F3 device test for R, F4 dev build and emulator QA, F5 phone install only on separate owner approval, F6 owner trial, F7 release.
- Known bugs/limits: Editorial does not know who speaks each line; a refused proposal needs the owner's tap; quality of edits, system file picker and notifications on a phone are NOT_MEASURED; G rule (c) skipped with another mid-sentence capitalised name; release builds are not debuggable; a job report prints raw settings JSON (possibly with the key) when it cannot parse it (left for 4.23).
- Regression status: working tree with S0: engine 742 / 1 SKIP, app debug 516 / 5 SKIP, 0 FAIL, lint 0 errors / 58 warnings, AndroidTest compile PASS (not yet a clean archive; release/benchmark variants not rerun). Last clean archive: `48860789`.
- Semantic progress: unchanged (Kumo ch 013 line 151 repair reachable offline).
- Spend: USD 0; no provider call. Phone touched only read-only on 2026-10-18.
- Next action: F2 of `docs/V422_DEVICE_TRIAL_RELEASE_WORK_REQUEST.md` (clean-archive regression of HEAD), then F3 and F4.
