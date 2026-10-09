# Workspace Snapshot

- Updated: 2026-10-09 (+07:00), offline §6.1 RAW-adjudicated address/completeness controls and regression.
- Current version: active v4.18; release/checklist unchanged.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Current commit: `a14272ab1eb4c74c154afa7493af1d432536d74d` is the implementation baseline immediately before this snapshot/report commit; that commit records CP-IMPL-4 and the §6.1 QA package.
- Current build: unchanged `4.18-c1.6`/253, source `57978e836a4cf13f2308fd8e6541a3732a0b66fa`, event `build-20261009-183051`; this work produced no APK and made no emulator/device change.
- Current phase: C1.6 offline quality measurement complete; CP-IMPL-4 is committed but has not been built/installed and its model adherence is not measured. U1 deferred; 0/3 chapters accepted; P7 not started.
- Completed tasks: C1.1–C1.4 alignment/source/UI/runner and C1.5 live 007 remain as previously recorded. Added offline scorer and synthetic controls under `scripts/chunk/`; 9/9 Python tests pass. Private RAW adjudication of two repeated address occurrences in 007/chunk 005 found 2 wrong fixes; one clean direct vocative stayed unchanged. Synthetic completeness scorer distinguishes restored/remaining/new omissions and boundary leakage. FINAL similarity is not an input to the address score.
- Pending tasks: owner reviews §6.1 and decides whether to authorize exactly one live EDIT for 007/chunk 005, under a new USD 0.01 cap. No live authority is inferred from the old C1 ledger.
- Known bugs/limits: CP-IMPL-4 model adherence NOT_MEASURED; completeness of the real 007 response NOT_MEASURED; other 27 changed lines remain UNADJUDICATED. The real annotation is a selected historical CP-IMPL-3 case, not a full-chapter benchmark.
- Regression status: clean archive of baseline `a14272ab` plus exact 6-file overlay (source equal to this commit): `:editorial-engine:test` 644 tests (0 failures/errors, 1 opt-in skip); `:app:testDebugUnitTest` 457 tests (0 failures/errors, 1 opt-in skip); Python 9/9; `git diff --check` PASS. The app pair-service suite confirms one EDIT per chunk. No APK build, device/emulator or provider call; private evidence is `D:\P5E-private\chunk-quality-20261009\`.
- Spend: old C1 ledger remains 17 calls / USD 0.04044430 settled, pending 0, remaining USD 0.00955570; this offline work added 0 provider calls and USD 0.
- Next action: owner reviews the offline result and approves or declines the single proposed 007/chunk 005 live call.
