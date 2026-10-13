# Workspace Snapshot

- Updated: 2026-10-13 (+07:00): D work request S0 complete, S1 no-retry/call-bound runner compiled.
- Current version: v4.20.0 in development; shipped v4.18.0 (tag `v4.18.0` on `9d02976e`).
- Current branch: `feature/v4.20` in worktree `D:\P5E-builds\wt-v4.20` (from `main` `be2056d8`).
- Current commit: `f7e39015f77fe7eb734925cff4d4ff4619069f53` is the implementation baseline immediately before this S1 snapshot commit; confirm actual HEAD on resume.
- Current build: none for v4.20. Shipped 4.18.0 / code 266 from `82626ac8`, APK SHA-256 `5865F23A1EE15602C5C38B967E34AED22E63728078C7A792F92E8BECBD6AE91B`.
- Current phase: plan D, S1 completed (androidTest compile PASS), S2 build/retention next; C1 production remains exact 739e5c49.
- Completed tasks: plan A/B/C1; S0 clean HEAD=origin=f7e39015; S1 opt-in p6_no_retry and p6_max_calls, metadata noRetry/maxCalls/dispatched/refused, no content gate, AndroidTest compile PASS.
- Pending tasks: S2-S9, wrapper production/test retained in D:\App Translate Books artifact+backup roots, emulator only, fake then one live014 <=10 calls/no retry/cap0.08; reading page/counts; stop for owner reading.
- Known bugs/limits: response logs of earlier runs are not in the cited format, so stored-response replay cannot evaluate C1; the call-name rule of v4.18 (cậu to name) is unchanged; narration errors of chapter 004 are not addressed; translation still produces wrong first-person forms (C2 not chosen).
- Regression status: clean archive `739e5c49`: engine 714 (1 skip), app 472 (3 skip), lint 0 errors / 57 warnings, androidTest compile OK; after the CP-IMPL-16 bump the working tree gives the same counts; clean-archive check of this commit recorded at the next state change.
- Semantic progress: 3/3 historically accepted chapters unchanged; C1 quality NOT_MEASURED.
- Spend: USD 0 for v4.20.
- Next action: perform S2 wrapper builds and byte-verified retention in the workspace archive roots, then S3-S9 per the approved work request.
