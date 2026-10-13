# Workspace Snapshot

- Updated: 2026-10-13 (+07:00): v4.20 opened; plan step C (C1 row citation) done offline; step D waits for a spend authority.
- Current version: v4.20.0 in development; shipped v4.18.0 (tag `v4.18.0` on `9d02976e`).
- Current branch: `feature/v4.20` in worktree `D:\P5E-builds\wt-v4.20` (from `main` `be2056d8`).
- Current commit: `be2056d8` is the implementation baseline immediately before this snapshot commit (not a self-reference).
- Current build: none for v4.20. Shipped 4.18.0 / code 266 from `82626ac8`, APK SHA-256 `5865F23A1EE15602C5C38B967E34AED22E63728078C7A792F92E8BECBD6AE91B`.
- Current phase: checklist step 05 (develop); plan step C done offline, D pending.
- Completed tasks: plan steps A and B; C1 implemented in `editorial-engine` with `AddressRowCitationTest` and updated tests.
- Pending tasks: owner spend authority for the D package (014, one run, max 10 calls, cap USD 0.08); then wrapper build, emulator fake run, one live run, owner reading page; ship or revert C1.
- Known bugs/limits: response logs of earlier runs are not in the cited format, so stored-response replay cannot evaluate C1; the call-name rule of v4.18 (cậu to name) is unchanged; narration errors of chapter 004 are not addressed; translation still produces wrong first-person forms (C2 not chosen).
- Regression status: working tree engine 714 (1 skip), app 472 (3 skip), 0 failures, androidTest compile OK; clean-archive rerun of the committed source recorded in BUILD_STATE after this commit.
- Semantic progress: 3/3 historically accepted chapters unchanged; C1 quality NOT_MEASURED.
- Spend: USD 0 for v4.20.
- Next action: owner grants or refuses the D package (`PRODUCT_DIRECTION_PLAN.md` section 7).
