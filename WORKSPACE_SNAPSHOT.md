# Workspace Snapshot

- Updated: 2026-10-09 (+07:00); C6.4 host fake on private chapter 006 passed; clean-source regression and wrapper/emulator checks remain.
- Current version: active v4.18; release/checklist unchanged.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Current commit: `11cd0140de1a186f593ec39dabc5003bfd544b4d` — implementation baseline immediately before this C6.4 test/evidence commit.
- Current build: `4.18-c1.7` / code `254`, event `build-20261009-193025`, source `ff819dde12eb548e83ab0de43d200bf16d9b795d`, APK SHA-256 `51BEC1455D9E2827AC8ED10336DC77D1AC703947423297450EBF2B8C65A77D40`; archived in `artifacts/` and `backup/`, installed only on `emulator-5554`. C6 source is not yet built.
- Current phase: C6.1–C6.4 offline technical verification. C6.3 ADDRESS coverage is below its gate; C6.5 live is not eligible. U1 remains deferred; no pilot or V5 work.
- Completed tasks: C6.1 engine and C6.2 app integration pushed (`44dcfffe`, `0d581055`); C6.3 eight-chapter measurement is recorded in plan §7. Host fake on real 006 passed twice: 14 calls per run for the 14 flagged chunks, the one no-point chunk made no call; all-keep reassembled byte-exact DRAFT, and second fake changed only one target line; USD 0.
- Pending tasks: run full engine/app and `scripts/p6`/`scripts/chunk` checks on clean source; build by `scripts/build-and-save.ps1` using code >254, mirror identical payload into both required archives; install only `emulator-5554` and run focused pair tests. Then stop before provider because offline ADDRESS coverage is 57.3% (<69%) and conservative fallback bound for 006 is USD 0.555370 (>USD 0.03).
- Known bugs/limits: ADDRESS detector/measurement does not meet the existing 69% threshold. Exact luna price is unavailable in the local model catalog; fallback bound is conservative, not a provider quote. Model quality remains NOT_MEASURED.
- Regression status: targeted engine and 8-chapter measurement/fake tests pass on working tree. Fake emits only counts, no book text. Clean-source regression/build/device evidence is pending.
- Spend: C6 provider calls 0 / USD 0. Existing ledgers unchanged.
- Next action: finish clean-source C6.4 regression, wrapper build, and focused emulator-5554 verification; do not dispatch a live call while either C6 gate is unmet.
