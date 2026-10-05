# Workspace Snapshot

- Updated: 2026-10-05 (+07:00): Z1-Z4 complete offline (docs/P6_R6_Z1_Z4_EXECUTION_20261005.md). Build `4.18-p6.23`/code236 from 808dd9e9 archived in both roots, installed on emulator-5554 only; preflight 5/5, coordinator 12/12, fake CHAIN 14/14, negative gate 0/1 as expected, 0 provider calls, G1 device ledger untouched. Z5 (full G1 under the Z3 rule) next.
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; continue the existing branch and release checklist.
- Current commit: 808dd9e9 — implementation baseline immediately before this snapshot update; confirm actual HEAD on resume.
- Current build: `4.18-p6.23`/code236, event `build-20261005-190002`, source `808dd9e9`; production APK SHA-256 `B13A59A162C6EC1D7305B571DF50AEFCE8FADFA0EB5D81B316F5DE34052754F6`, AndroidTest `DB313687AC2A9CC847CDDF4EE85975C2D9FDF396F6C7EA7905B0A1288715238F` (event `p6-z4-808dd9e9-20261005-01`). Installed on `emulator-5554` only and archived in both payload roots.
- Current phase: P6 R6 Z4 complete; Z5 live G1 (8 fixtures + 2 repeat rounds under the Z3 rule) about to start.
- Completed tasks: W1, W2, W5, Z1 (classification), Z2 (mutation suite, RAW anchors, strict L2/L3 schemas), Z3 (run rule), Z4 (wrapper build + emulator checks).
- Pending tasks: Z4 wrapper build and emulator checks (preflight, coordinator, fake CHAIN 14/14, negative gate, 0 calls); Z5 full G1 (8 fixtures + 2 repeat rounds) within USD 0.90641910, scored with score_run.py, stop before G2.
- Known bugs: none known in the Z1/Z2 scope. The saved W4 RECONCILE response had a RAW unit number off by two (L321 vs the quote on 323): RAW anchors are now derived from rawQuote like the DRAFT ones.
- Regression status: engine 420 PASS, app unit PASS, androidTest compile PASS, Python 49/49, emulator preflight 5/5, coordinator 12/12, fake CHAIN 14/14, negative gate 0/1 as expected. G1 ledger: 11 settled calls, USD 0.09358090 / 1.00, USD 0.90641910 remaining, 0 pending.
- Next action: Z5: run the full G1 on ledger G1-20261003-ecbf8c55 (fx-a03, fx-a04, fx-a05, fx-a07, fx-a08, fx-a11, fx-a02, fx-a12 + two repeat rounds of fx-a11 and fx-a04) with the Z3 rule, score with score_run.py, report per fixture, stop before G2.
