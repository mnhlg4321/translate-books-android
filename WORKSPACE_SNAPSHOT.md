# Workspace Snapshot

- Updated: 2026-10-05 (+07:00): offline G1 semantic adjudication and normalization review complete in `0d9b5507` (docs/P6_R6_G1_SEMANTIC_ADJUDICATION_20261005.md). All 12 outputs and 17 finding rows are accounted for; 25 seeded-target opportunities yielded 10 semantic detections and 15 misses. Normalization observed 17 duplicate-reference removals and 13 invalid speaker side-record drops; zero DRAFT re-anchors, protected-span adjustments or list truncation. A concrete 24-call G2 proposal is recorded; G2 not started.
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; continue the existing branch and release checklist.
- Current commit: 0d9b5507 — implementation/documentation baseline immediately before this snapshot update; confirm actual HEAD on resume.
- Current build: `4.18-p6.24`/code237, event `build-20261005-191113`, source `3b570135`; production APK SHA-256 `8CE099A7AD5BBD78FA681C6C500412FB4B6F10C5C7E44607764F5ABEBF173193`, AndroidTest `DB313687AC2A9CC847CDDF4EE85975C2D9FDF396F6C7EA7905B0A1288715238F` (event `p6-z5b-3b570135-20261005-01`). Installed on `emulator-5554` only and archived in both payload roots.
- Current phase: P6 R6 Z5 and section-7 offline adjudication complete; G1 gate (>= 6/8 structural) passed 8/8; waiting for a separate G2 decision.
- Completed tasks: W1, W2, W5, Z1, Z2, Z3, Z4, Z5, retrospective target/finding adjudication, normalization impact review, detection/repair split, concrete G2 proposal.
- Pending tasks: Owner decision on G2 proposal only; no pilot/G3/G4/R7 action authorized.
- Known bugs: temporal oracle-probe bypass was repaired and its 54/54 Python regression remains recorded. Semantic findings include 2 confirmed additional defects, 3 false positives, 1 preference and 1 unresolved relation; L1-only repair is NOT_MEASURED. G1 model/route/source metadata are absent from run metadata and must be captured before any G2 dispatch.
- Regression status: clean historical baseline 006434da engine 422/422, app unit 342/342, androidTest compile PASS, Python 54/54; this package ran no code tests because it changed documentation only. Offline audit verified 12/12 response hashes, wire/contract revisions and normalization totals. G1 ledger: 41 settled calls, USD 0.28859445 / 1.00, USD 0.71140555 remaining, 0 pending.
- Next action: Owner decision on `docs/P6_R6_G2_PROPOSAL_20261005.md`; no live dispatch.
