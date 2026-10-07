# Workspace Snapshot

- Updated: 2026-10-07 (+07:00) after Q1.4 offline replay and manifest preparation; wrapper/emulator gate and Q1.5 remain pending.
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; existing release checklist unchanged.
- Current commit: `c67477c6` — implementation baseline immediately before the Q1.4 preparation package snapshot commit.
- Current build: `4.18-api.6`/code `243`, event `build-20261007-065128`, wrapper archive from `81e4d578`; artifacts remain mirrored and installed only on `emulator-5554`.
- Current phase: EDITORIAL_API_V1 — Q1.1–Q1.4 offline code/manifest preparation complete; Q1.4 build/device gate not yet run.
- Completed tasks: scorer, strict RAW-aligned normalizer, `EDITORIAL_API_V1.2` Quality Core/guards, N6-shaped production-guard replay test, and A/B manifest with C0/C1/C2 pricing/estimates. FINAL text stays private and out of prompts/fixtures.
- Pending tasks: wrapper build with code >243, emulator-5554 install and Editorial API instrumentation; then approved Q1.5 dev/holdout calls and three private exports.
- Known bugs: N6 hard issues remain measurements only; Q1.2 made no real-chapter repairs under strict alignment. No Q1 provider result exists yet. Pilot, chunk-pair and U1 files are untouched.
- Regression status: engine and app JVM tests pass; Q1 replay 2/2; provider calls `0`, spend `USD 0`.
- Next action: commit/push Q1.4 preparation, then build with the wrapper and run the emulator gate.
