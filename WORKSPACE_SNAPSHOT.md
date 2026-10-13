# Workspace Snapshot

- Updated: 2026-10-13 (+07:00): D package approved; contract revision CP-IMPL-16; detailed work request written.
- Current version: v4.20.0 in development; shipped v4.18.0 (tag `v4.18.0` on `9d02976e`).
- Current branch: `feature/v4.20` in worktree `D:\P5E-builds\wt-v4.20` (from `main` `be2056d8`).
- Current commit: `739e5c49` is the implementation baseline immediately before this snapshot commit (not a self-reference).
- Current build: none for v4.20. Shipped 4.18.0 / code 266 from `82626ac8`, APK SHA-256 `5865F23A1EE15602C5C38B967E34AED22E63728078C7A792F92E8BECBD6AE91B`.
- Current phase: checklist step 05; plan step D ready to execute per `docs/V420_D_014_WORK_REQUEST.md`.
- Completed tasks: plan steps A and B; C1 implemented in `editorial-engine` with `AddressRowCitationTest` and updated tests.
- Pending tasks: work request S0-S9: runner no-retry mode, wrapper build stored in the workspace archives, emulator install, fake run, one live run on 014 (cap USD 0.08, max 10 calls), reading page, counts; then the owner decides ship or drop C1.
- Known bugs/limits: response logs of earlier runs are not in the cited format, so stored-response replay cannot evaluate C1; the call-name rule of v4.18 (cậu to name) is unchanged; narration errors of chapter 004 are not addressed; translation still produces wrong first-person forms (C2 not chosen).
- Regression status: clean archive `739e5c49`: engine 714 (1 skip), app 472 (3 skip), lint 0 errors / 57 warnings, androidTest compile OK; after the CP-IMPL-16 bump the working tree gives the same counts; clean-archive check of this commit recorded at the next state change.
- Semantic progress: 3/3 historically accepted chapters unchanged; C1 quality NOT_MEASURED.
- Spend: USD 0 for v4.20.
- Next action: execute `docs/V420_D_014_WORK_REQUEST.md` from S0; stop for the owner reading after S9.
