# Workspace Snapshot

- Updated: 2026-10-10 (+07:00); resumed C6.4 emulator verification and aligned stale pair instrumentation fixtures with CP-IMPL-6.
- Current version: active v4.18; release/checklist unchanged.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Current commit: `da648d961cdcda188795bb3ef97cec4cf1c46c46` — implementation baseline immediately before this snapshot commit.
- Current build: `4.18-c6.1` / code `255`, event `build-20261009-205035`, source `da648d961cdcda188795bb3ef97cec4cf1c46c46`, APK SHA-256 `8D346623728DF48A9C1B1C3AB70093B3BD5D54C374D4B88F687F0FD23D4906B5`; immutable payload mirrored in `artifacts/` and `backup/`, installed only on `emulator-5554` after exact device-token preflight.
- Current phase: C6.4 device regression repair. C6.3 ADDRESS coverage is below gate; C6.5 live is not eligible. U1 remains deferred; no pilot or V5 work.
- Completed tasks: C6.1 engine (`44dcfffe`), C6.2 app (`0d581055`), C6.3 measurement (`11cd0140`), and C6.4 host fake on private 006 (`9bdf2fed`) are pushed. Fake no-op byte-roundtrip and isolated single-point change both pass with 14/14 flagged chunks and one no-point chunk making no call; zero fake spend. Clean-host engine/app, `scripts/p6`, and `scripts/chunk` regressions pass at `da648d96`.
- Pending tasks: push the focused AndroidTest fixture correction, build code 256 through the wrapper, install only on `emulator-5554`, and rerun pair/API device tests including process-death phases; then write the final C6.4 evidence into plan §7.
- Known bugs/limits: ADDRESS coverage is 57.3%, below required 69%. Conservative fallback estimate for 006 is USD 0.555370, above C6 cap USD 0.03; this is not a confirmed luna quote. Model quality NOT_MEASURED.
- Regression status: clean host engine 665 tests (0 fail/error, 1 opt-in skip), app 468 (0 fail/error, 3 opt-in skips), `scripts/p6` 93/93 and `scripts/chunk` 24/24 PASS. Emulator: API Store 6/6, API flow 3/3, API UI 5/5 PASS. Pair Store 4/6 and Pair UI 2/3 executable cases exposed stale instrumented fixtures that still expected whole-chunk calls; their correction is staged and awaits a fresh wrapper build/device rerun. No product failure is confirmed by those stale expectations.
- Spend: C6 provider calls 0 / USD 0; all existing ledgers unchanged.
- Next action: complete C6.4 by pushing the fixture correction, rebuilding through the wrapper, and rerunning focused emulator tests including process-death recovery.
