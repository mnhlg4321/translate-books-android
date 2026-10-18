# Workspace Snapshot

- Updated: 2026-10-18 (+07:00): F2 clean-archive regression PASS, F3 device test for R, F4 `4.22-dev.3` built and emulator QA PASS (171 PASS / 14 SKIP / 0 FAIL); phone install waits for owner approval.
- Current version: v4.22.0 in development (not released); shipped 4.21.0 / code 271 (tag `v4.21.0`).
- Current branch: `feature/v4.22` in worktree `D:\P5E-builds\wt-v4.22` (from `main` `7c44f3a2`).
- Current commit: `2317fb5b` is the implementation baseline immediately before this snapshot commit (not a self-reference); the dev.3 APK is built from `4032ff39`, `2317fb5b` changes androidTest only. Confirm actual HEAD when resuming.
- Current build: `4.22-dev.3` / code 275 (event `build-20261018-161433`, source `4032ff39`, APK SHA-256 `CC7F669076058D8AAA4843FDADE9D1F3C1021B4C793D67DC42DEEE8B7DC195CB`), in `D:\App Translate Books\artifacts\builds` and `backup\builds`; on emulator-5554 only. Phone `15e84958`: `4.22-dev.2` / code 273 (read only 2026-10-18).
- Current phase: step 05 develop: R and S0 in a dev build, emulator QA done; waiting for owner approval of F5 (phone backup and install), then owner trial F6.
- Completed tasks: U1, U2, U3, P, G, Q1 design, R; F1 archive junctions; S0.1-S0.8; F2 clean archive `4032ff39`; F3 `EditorialRefusedProposalInstrumentedTest`; F4 build, device QA and 16 screenshots (`artifacts/qa/v4.22-dev3-20261018/QA_REPORT.md`); work requests `docs/V422_DEVICE_TRIAL_RELEASE_WORK_REQUEST.md`, `docs/V422_S0_LIBRARY_SAFETY_WORK_REQUEST.md`, proposal `docs/V423_LIBRARY_BATCH_PROPOSAL.md`.
- Pending tasks: F5 phone backup and install only on separate owner approval, F6 owner trial (T1-T13), F7 release 4.22.0, F8 release install; 4.23 S1-S5 after release.
- Known bugs/limits: Editorial does not know who speaks each line; a refused proposal needs the owner's tap; quality of edits, system file picker and notifications on a phone are NOT_MEASURED; G rule (c) skipped with another mid-sentence capitalised name; release builds are not debuggable; a job report prints raw settings JSON (possibly with the key) when it cannot parse it (left for 4.23).
- Regression status: clean archive `4032ff39`: engine 742 / 1 SKIP, app debug, release, benchmark 516 each / 5 SKIP, 0 FAIL, lint 0 errors / 58 warnings, AndroidTest compile PASS; device (emulator-5554, dev.3) 185 discovered / 171 PASS / 14 SKIP / 0 FAIL / 0 INCOMPLETE.
- Semantic progress: unchanged (Kumo ch 013 line 151 repair reachable offline).
- Spend: USD 0; no provider call. Phone touched only read-only on 2026-10-18.
- Next action: ask the owner to approve F5: run-as backup from `4.22-dev.2` on `15e84958`, then install `4.22-dev.3` with `scripts/install-validated.ps1`.
