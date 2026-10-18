# Workspace Snapshot

- Updated: 2026-10-18 (+07:00): owner added S0 (library data safety, library export/import, profile export without API key) to 4.22.0; work requests written; library/batch features S1-S5 moved to 4.23.
- Current version: v4.22.0 in development (not released); shipped 4.21.0 / code 271 (tag `v4.21.0`).
- Current branch: `feature/v4.22` in worktree `D:\P5E-builds\wt-v4.22` (from `main` `7c44f3a2`).
- Current commit: `db71bef5` is the implementation baseline immediately before this snapshot commit (not a self-reference). Confirm actual HEAD when resuming.
- Current build: `4.22-dev.1` / code 274 (event `build-20261015-090335`, source `e8083883`) contains neither R nor S0. Phone `15e84958`: `4.22-dev.2` / code 273, DEBUGGABLE (read-only `dumpsys` 2026-10-18).
- Current phase: step 05 develop: R done in source; S0 starting; then dev build `4.22-dev.3`, emulator QA, owner install approval and trial.
- Completed tasks: U1, U2, U3, P, G, Q1 design, R (see earlier state in BUILD_STATE.md); work requests `docs/V422_DEVICE_TRIAL_RELEASE_WORK_REQUEST.md` (F1-F9, trial sheet T1-T13), `docs/V422_S0_LIBRARY_SAFETY_WORK_REQUEST.md` (S0.1-S0.8) and proposal `docs/V423_LIBRARY_BATCH_PROPOSAL.md` (reviewed by a critic agent against the code).
- Pending tasks: F1 (link the build archive so the next build is `4.22-dev.3`, code >= 275; commit QA media), S0.1-S0.8, F2 clean-archive regression, F3 device test for R, F4 dev build and emulator QA, F5 phone install only on separate owner approval, F6 owner trial, F7 release.
- Known bugs/limits: Editorial does not know who speaks each line; a refused proposal needs the owner's tap; quality of edits, system file picker and notifications on a phone are NOT_MEASURED; G rule (c) skipped with another mid-sentence capitalised name; release builds are not debuggable; until S0 lands, glossary saves are asynchronous, a corrupt library store reads as empty, bulk import changes the Active profile, there is no library export, and "Export profile" writes the API key; a job report prints raw settings JSON (possibly with the key) when it cannot parse it (left for 4.23).
- Regression status: clean archive of `48860789`: engine 742 / 1 SKIP, app 492 / 5 SKIP per variant, 0 FAIL, lint 0 errors / 58 warnings, AndroidTest compile PASS. No regression since (docs only).
- Semantic progress: unchanged (Kumo ch 013 line 151 repair reachable offline).
- Spend: USD 0; no provider call. Phone touched only read-only on 2026-10-18.
- Next action: F1 of `docs/V422_DEVICE_TRIAL_RELEASE_WORK_REQUEST.md`, then S0.1-S0.8 of `docs/V422_S0_LIBRARY_SAFETY_WORK_REQUEST.md`.
