# Workspace Snapshot

- Updated: 2026-10-07 (+07:00) after Q1.1 offline scorer; Q1.2–Q1.5 remain pending.
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; existing release checklist unchanged.
- Current commit: `1834294b` — implementation baseline immediately before the Q1.1 package snapshot commit.
- Current build: `4.18-api.6`/code `243`, event `build-20261007-065128`, wrapper archive from `81e4d578`; artifacts remain mirrored and installed only on `emulator-5554`.
- Current phase: EDITORIAL_API_V1 — Q1.1 scorer complete offline; U1 UI remains owned by another session.
- Completed tasks: private 28-chapter input manifest; Q1.1 scorer and synthetic tests; DRAFT baseline and N6 001–003 measurements; FINAL text stayed private and out of prompts/fixtures.
- Pending tasks: Q1.2 RAW-aligned normalizer; Q1.3 Quality Core v2/guards; Q1.4 offline replay/manifest/wrapper/emulator gate; Q1.5 approved dev and holdout calls after Q1.4 PASS; owner review of three private holdout outputs.
- Known bugs: N6 hard issues remain measurements only; no Q1 runtime/device/provider result exists yet. Pilot, chunk-pair and U1 files are untouched by this package.
- Regression status: `py -3 -m unittest scripts/p6/test_score_vs_final.py` PASS (3/3); provider calls `0`, spend `USD 0`.
- Next action: implement and measure Q1.2 `RawAlignedNormalizer` offline.
