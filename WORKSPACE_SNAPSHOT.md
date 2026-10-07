# Workspace Snapshot

- Updated: 2026-10-07 (+07:00) after Q1.5 dev/holdout execution and private output export; package stopped.
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; existing release checklist unchanged.
- Current commit: `8294f13a` — implementation baseline immediately before this Q1.5 report/state snapshot commit.
- Current build: `4.18-q1.1`/code `244`, event `build-20261007-223121`, source `41edafa4`; AndroidTest event `q1-androidtest-20261007`; both mirrored and installed only on `emulator-5554`.
- Current phase: EDITORIAL_API_V1 — Q1.5 complete; stopped before further provider/device/G2 work.
- Completed tasks: Q1.1 scorer, strict RAW-aligned normalizer, `EDITORIAL_API_V1.2` Quality Core/guards, N6 replay, A/B manifest, wrapper archive, validated emulator gate, C0/C1/C2 dev matrix, C0/C1 holdout matrix, offline FINAL scoring, ledger verification, and exports `010`, `016`, `022` under `D:\P5E-private\q1-outputs\`.
- Pending tasks: owner review and decision after the holdout quality gate result.
- Known bugs: holdout gate NOT PASS; C1 fix recall `0.383562` with `1/6` positive similarity deltas and `19` farther lines, although added kana/Han is `0`. No UI U1, pilot or chunk-pair file/data was touched.
- Regression status: Q1.4 offline replay, app unit/lint and selected instrumentation PASS; Q1 ledger has 33 calls, 0 pending/UNKNOWN, settled USD `0.2623298605` under the approved USD 2.00 ceiling.
- Next action: owner reads the three private holdout outputs and decides whether to authorize a new scope.
