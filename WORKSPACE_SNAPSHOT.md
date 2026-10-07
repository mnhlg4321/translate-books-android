# Workspace Snapshot

- Updated: 2026-10-07 (+07:00) after Q1.3 offline regression; Q1.4–Q1.5 remain pending.
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; existing release checklist unchanged.
- Current commit: `92b3aec4` — implementation baseline immediately before the Q1.3 package snapshot commit.
- Current build: `4.18-api.6`/code `243`, event `build-20261007-065128`, wrapper archive from `81e4d578`; artifacts remain mirrored and installed only on `emulator-5554`.
- Current phase: EDITORIAL_API_V1 — Q1.1–Q1.3 complete offline; U1 UI remains owned by another session.
- Completed tasks: private 28-chapter scorer/manifest; conservative RAW-aligned normalizer; `EDITORIAL_API_V1.2` Quality Core, normalized prompt detections and deterministic source-script guard; engine and app JVM regressions pass. FINAL text stayed private and out of prompts/fixtures.
- Pending tasks: Q1.4 saved-N6 guard replay, A/B pricing manifest, wrapper build and emulator-only Editorial API tests; then approved Q1.5 dev/holdout calls and three private exports.
- Known bugs: N6 hard issues remain measurements only; Q1.2 made no real-chapter repairs under strict alignment. No Q1 provider result exists yet. Pilot, chunk-pair and U1 files are untouched.
- Regression status: engine suite PASS; app `testDebugUnitTest` PASS; provider calls `0`, spend `USD 0`.
- Next action: complete Q1.4 offline replay, model manifest and wrapper/emulator gate.
