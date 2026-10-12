# Workspace Snapshot

- Updated: 2026-10-12 (+07:00), owner approved the small live (006 chunk 5, USD 0.01); selector emitter added; build/install/run in progress.
- Current version: active v4.18; release/checklist unchanged.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Current commit: `5e34c5806cb1e1cfe33e6b19aecfe9a6cce55a4a` is the implementation baseline immediately before the selector-emitter commit and this snapshot commit; confirm actual `HEAD` when resuming.
- Current build: `4.18-c6.4`/code `258` from source `12143ce9ca785e90219f5d94ef00b78c5ce0e5c4`, APK SHA-256 657EB07E43FEB0B7C36C8FC0D0485CF01D51E60111E4EBD74429A8CF463BF281; test APK `c6-6-test-20261010` (source `31612b54`); installed only on `emulator-5554`. No newer build: CP-IMPL-7/8 are source-only.
- Current phase: C6 wrong-edit repair, offline part done. `AddressEditGuard` (scope, name, plural audience, speaker-to-target direction from the full pronoun table, row consistency) plus the coordinator's CP-IMPL-7 patch. 0/3 accepted, P7 not started.
- Completed tasks: C6.1 to C6.6 as before (plan §7). Offline lock: faithful replay of the stored C6B answers through the real finder, no provider call: 9 SAI rejected, 11 correct edits still FIXED with identical lines (dialogue marks and symbols kept), 54 KEPT unchanged, no other text changed. Engine 686 PASS (1 opt-in skip, 16 new synthetic tests), app 1381 PASS (5 skip), androidTest sources compile. Runner LUNA input price corrected to 0.25/M.
- Pending tasks: build 4.18-c6.5 from the committed HEAD via the wrapper, install only emulator-5554, device fake selector proof, one live call on 006 chunk 5 (ledger C6C, cap USD 0.01), adjudicate its 7 points, record in plan §7; isolated clean-source test run by the coordinator.
- Known bugs/limits: owner FINAL edits to 5:1 and 5:2 (SÓT) would be blocked by the third-person rule; 13 pronoun misses stay SÓT; model behaviour under CP-IMPL-8 unmeasured; only chapter 006 replayed; `V5ChatEditorialApiProvider` and host test fixtures still price luna input at 0.20/M; equal line count does not prove no missing meaning.
- Regression status: working-tree run, not a clean archive. Engine 686, app 1381, androidTest compile PASS; Python suites untouched. Evidence D:/P5E-private/c6-guard-20261012/. No new APK/device tests.
- Spend: 0 provider calls, USD 0 this session; C6B ledger unchanged (14 calls, USD 0.02788237, pending 0).
- Next action: build 4.18-c6.5 from the committed HEAD via the wrapper, install only emulator-5554, prove the selector with the fake provider, then make exactly one live call on 006 chunk 5 (luna medium, no retry, ledger C6C, cap USD 0.01, stop on any selector refusal) and adjudicate its 7 points.
