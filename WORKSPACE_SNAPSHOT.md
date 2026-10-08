# Workspace Snapshot

- Updated: 2026-10-08 (+07:00): Q2.5.1 offline hoàn tất; owner duyệt D-Q2b (trần Q2 USD 10.00), chỉ dùng GPT-5.6 luna reasoning medium; cổng chất lượng tối thiểu 4.1.3.
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; existing release checklist unchanged.
- Current commit: d2cec720 — implementation baseline immediately before this Q2.5.1 code/evidence commit.
- Current build: `4.18-q2.1`/code `245`, source `45976adb`, installed only on `emulator-5554`; wrapper archives are mirrored in both artifact roots.
- Current phase: EDITORIAL_API_V1 — Q2.5.1 source-pack attachment and preflight implemented offline; no Q2.5 build/device/provider action yet. U1 deferred; 0/3 chapters accepted; P7 not started.
- Completed tasks: Q2.1 scorer/normalizer/Quality Core, Q2.2 V5_CHAT baseline, Q2.3 wrapper/emulator/manifest, Q2.4 dev scoring, and Q2.5.1 original four-file V5 attachments, single Project Instruction, stop/truncation handling, FINAL extraction, and fail-closed pack preflight.
- Pending tasks: Q2.5.2 minimal-rewrite/case guard, then Q2.5.3 wrapper build and emulator tests, then Q2.5.4 canary 007 followed only on success by frozen dev and holdout matrices under section 7.
- Known bugs: Q2.5.1 has not yet been built or exercised on-device; V5-luna quality is NOT_MEASURED. Q2.4 findings remain historical. Chunk-pair is frozen; pilot untouched.
- Regression status: engine 572/572 PASS; app benchmark/debug/release unit tasks each 439/439 PASS; `scripts/p6` 80/80 PASS; real input pack preflight 8/8 PASS; AndroidTest Java compile PASS. Q2 ledger last reported at USD 0.8375159 / 6.00 cap, with 10 settled logical reservations, 0 pending, 0 UNKNOWN; no Q2.5 call has been made.
- Next action: finish and verify Q2.5.2 offline, then commit and push that package before building.

