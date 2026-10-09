# Workspace Snapshot

- Updated: 2026-10-09 (+07:00); records offline C6.1–C6.3 evidence and the pending C6.4 device/build check.
- Current version: active v4.18; release/checklist unchanged.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Current commit: `0d581055f718577977d5097905cc34b2a2979d8b` — implementation baseline immediately before the C6.3 snapshot/package commit.
- Current build: `4.18-c1.7` / code `254`, event `build-20261009-193025`, source `ff819dde12eb548e83ab0de43d200bf16d9b795d`, APK SHA-256 `51BEC1455D9E2827AC8ED10336DC77D1AC703947423297450EBF2B8C65A77D40`; archived in `artifacts/` and `backup/`, installed only on `emulator-5554`. C6 source commits are not yet in an APK.
- Current phase: C6.1–C6.3 implementation and offline measurement; C6.4 fake/build/emulator pending; C6.5 is gated. U1 remains deferred; no pilot or V5 work.
- Completed tasks: C6.1 five-layer engine and C6.2 app integration pushed (`44dcfffe`, `0d581055`). No-point chunks do not dispatch provider requests. Opt-in private-corpus measurement covers 004–008/011/014/017: 85 chunks, 64 with points, 346 points. ADDRESS coverage 98/171 = 57.3% (below 69%); glossary 16/18 = 88.9%; kana 10/10 = 100%; missing 0. Corpus contents and FINAL remain outside Git.
- Pending tasks: finish C6.4: clean-source host regression, fake chapter 006 exact-byte reassembly plus one-point-only edit, wrapper build with archive mirrors, and focused tests only on `emulator-5554`. C6.5 cannot proceed unless the offline gate is met and a current estimate is within USD 0.03.
- Known bugs/limits: ADDRESS coverage currently misses the 69% threshold. The app's conservative fallback estimate for 006 is USD 0.555370, not confirmed luna pricing and above the C6 cap. Live quality NOT_MEASURED; no C6 provider calls.
- Regression status: targeted `TargetedFixPipelineTest` PASS; opt-in 8-chapter measurement PASS as a measurement run. Full clean-source C6.4 suite not yet run. `git diff --check` must pass before commit.
- Spend: C6 package so far 0 provider calls / USD 0. Existing prior ledgers unchanged.
- Next action: complete the authorized C6.4 fake chapter 006 and wrapper/emulator verification; do not dispatch live unless the offline and budget gates both pass.
