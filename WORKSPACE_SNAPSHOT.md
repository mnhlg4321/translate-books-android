# Workspace Snapshot

- Updated: 2026-10-13 (+07:00): plan steps A and B done; owner chose P1 (wrong first-person self); fix option C1/C2 pending.
- Current version: v4.18.0 shipped (tag `v4.18.0` on `9d02976e`); no release lifecycle open.
- Current branch: `fix/v4.18-product-plan` (plan-only branch, merged into `main`); work happens in clean worktrees under `D:\P5E-builds\`.
- Current commit: `9252c183` (`main`) is the implementation baseline immediately before this snapshot commit (not a self-reference).
- Current build: production 4.18.0 / code 266 from `82626ac8`, APK SHA-256 `5865F23A1EE15602C5C38B967E34AED22E63728078C7A792F92E8BECBD6AE91B`. Frozen v4.19 dev build 4.19-x5.1 / code 267 archived in `artifacts/` and `backup/` of the workspace, not a release.
- Current phase: plan step B done; waiting for the owner to choose fix option C1 (Editorial row citation) or C2 (translation prompt grouped by speaker), or to switch to P2.
- Completed tasks: v4.18.0 released; v4.19 frozen with its evidence preserved; plan approved.
- Pending tasks: owner choice C1/C2/P2; then step C offline, step D one acceptance run with a separate spend authority.
- Known bugs/limits: wrong first-person self arises in translation although the right row is in the prompt (122/122 correct words are a covering row self; 50 from another speaker row, 26 outside all rows, 8 other listener, 38 narration in 004 only); Editorial flags 70/84 dialogue cases but keeps them because several rows apply and the prompt says keep when ambiguous; no component attributes speaker/listener per line.
- Regression status: no code change on `main` since v4.18.0; no test or build run for this plan commit.
- Semantic progress: 3/3 historically accepted chapters (006, 008, 011) with recorded limits; no new measurement.
- Spend: USD 0 since the frozen v4.19 run (USD 0.0029583).
- Next action: owner chooses C1, C2 or P2 (`PRODUCT_DIRECTION_PLAN.md` section 6).
