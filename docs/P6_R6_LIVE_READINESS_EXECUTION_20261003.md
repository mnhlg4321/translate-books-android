# P6 R6 live-readiness execution record — 2026-10-03

Scope: owner request `docs/P6_R6_LIVE_READINESS_WORK_REQUEST_20261003.md`. Complete L0–L8 offline and, after those gates pass, run G1 only within Q1's approved USD 1.00 cap; stop and report before G2. The owner answer in §6 closes L9 for the emulator and pilot. Do not read, print, store, or transfer the API key. Do not put the fingerprint in Git or output logs.

## L0 — branch and baseline

- Branch: `feature/v4.18-p5e-runner-repair-20260917`.
- HEAD: `495b30bb6b409c0c3d93a06ac1c2da220260b4ab`, which contains the L9 owner answer and is newer than the request's minimum `fbde77f6`.
- The branch was synchronized with origin. The working tree had pre-existing owner edits and untracked evidence/artifacts in `.idea`, P5E documentation/scripts, `raw/`, `evidence/`, `artifacts/`, and owner backup folders. These were not staged, reset, cleaned, or moved.
- L9: §6 records that the same key is saved in app Settings on `emulator-5554` and pilot `15e84958`, and the supplied fingerprint applies to both. This is owner-provided placement evidence; the runner must still validate emulator Settings before dispatch.
- At L0 completion: no provider call, key read, pilot access, device operation, or database change was performed. No call or spend has been incurred in this live-readiness sequence.
- **L0 PASS.**

## L1 — live fixture runner implementation

- Added a separate `p6_fixture_live=YES` instrumentation gate. Without it the runner uses local fakes; a live run reads `SettingsStore` in process memory and checks the qualified route, nonempty key, and owner-supplied endpoint/account fingerprint before fixture setup or provider construction. Refusal results are fixed codes; neither key nor expected/actual fingerprint is written or printed.
- Live calls use the production ledger-v2 RAW and RECONCILE OpenRouter adapters, production L2/L3 adapters, and the existing `EditorialP6Budgeted*Provider` wrappers. The runner requires a group ID and cap (maximum USD 1.00); its hash-chained group ledger is stored under app-private files and is not removed by test teardown.
- A test-only recorder writes exact rendered prompt bytes for every phase to the private run directory before delegate dispatch. Structural and run metadata distinguish LIVE from FAKE_OFFLINE and include provider call count, usage, known USD, finish reasons, and a cumulative spend-ledger copy. The fingerprint itself is excluded.
- Added synthetic preflight tests for missing expected fingerprint, wrong route, missing key, and mismatched/matching fingerprint. Added an opt-in emulator test for a deliberately wrong fingerprint that reports only `FINGERPRINT_MISMATCH` and zero dispatches.
- `:app:compileDebugAndroidTestJavaWithJavac`: PASS. PowerShell parser check for `scripts/p6/run_group.ps1`: PASS. The emulator preflight test will run in L8 after the final archived production/test APKs are built and installed.
- No API key was read by Codex; the test code only reads it in emulator process memory as required. No provider call, pilot access, or spend occurred.

## L2 — group-level spend guard

- The fixture runner now uses one hash-chained ledger per group ID under the app-specific external evidence directory, with the approved group cap passed explicitly. The file is outside per-test teardown and is shared by fixture calls on the emulator; it contains spend/phase/hash metadata only.
- `scripts/p6/run_group.ps1` prechecks the remaining cap against the mode's per-fixture worst case before instrumentation, retrieves and validates the cumulative ledger before and after each fixture, preserves snapshots under the private run directory, and stops on pending UNKNOWN cost, cap exhaustion, ledger loss, or fixture failure.
- Added `spend_ledger.py`, `verify_spend_ledger.py`, and three host tests covering pre-dispatch cap refusal, UNKNOWN stop, and cumulative spend surviving a copied host snapshot. `test_spend_ledger.py`: **3/3 PASS**. `EditorialP6GroupSpendLedgerTest`: **6/6 PASS**, including distinct ledger instances sharing the same cumulative cap. PowerShell syntax and Python syntax checks pass.
- These tests establish the guard logic; emulator persistence and per-fixture host transfer will be rechecked with the final L8 fake run. No provider calls or spend occurred.

## L3 — committed-contract continuation

- `Inspection` now reports the durable L1 contract revision and whether its state was read successfully. Readback failures fail closed: they offer no run action and show a typed unreadable-state message.
- Added a pure chapter action policy. Only a readable `L1_REQUIRED` state selects the L1 entry point. A committed legacy L1 displays the “create a new binding for v2” notice and may continue only through its stored legacy L2/L3 contract; a committed ledger-v2 L1 continues only through ledger-v2 L2/L3.
- `AppChain` re-inspects after confirmation and before provider construction. The continuation branch constructs no L1 authorization or adapter and calls `runToFinal`; a stale confirmation is refused before dispatch. Ledger-v2 continuation keeps the original USD 0.50 group cap and presents six remaining calls; legacy continuation presents four calls with no final-read.
- Removed `withFreshLedgerL1LifecyclePersistence` and the special `L1_LEDGER` phase allowance.
- Targeted JVM tests: `EditorialChapterRunActionPolicyTest` **5/5**, `EditorialChainBudgetsLedgerTest` **5/5**, `EditorialChapterRunServiceTest` **11/11** (21 total, all pass). `:app:compileDebugAndroidTestJavaWithJavac`: PASS. The instrumented coordinator tests are included in the later emulator verification; no device or provider was used for this package.
- No provider call or spend occurred. **L3 offline implementation and compile gates PASS; emulator runtime evidence remains pending L7/L8.**

## L4 — UNKNOWN after L1 claims

- Added an instrumented RAW interruption test that throws a simulated process-death `AssertionError` immediately after provider dispatch begins and the durable attempt is `CLAIMED`. After closing and reopening the database, the attempt remains `CLAIMED`; the next coordinator run returns `RETRY_PROVIDER_CALL_STATE_UNKNOWN` with zero L1/L2/L3 provider calls.
- Added the corresponding RECONCILE interruption test. It first commits RAW, then simulates process death after RECONCILE claim. After reopen, RAW remains `COMMITTED`, RECONCILE remains `CLAIMED`, and the next run returns `RETRY_PROVIDER_CALL_STATE_UNKNOWN` with zero redispatches.
- `:app:compileDebugAndroidTestJavaWithJavac`: PASS. Runtime execution is queued for the current-source emulator regression in L7/L8 after the required wrapper build. Both tests use only local fake providers; no live provider, device data, or spend was used for this package.
- **L4 instrumentation source/compile PASS; emulator runtime evidence pending L7/L8.**

## L5 — G2 L1_THEN_L2 wiring

- Added `L1_THEN_L2`. An offline run creates and commits `REPORT_L1`, then commits `VI_L2`, in the same temporary database and reports the expected five fake provider calls. Live `L1_THEN_L2` requires the G1 base `RunId`; it reopens the retained app-private database and pack storage, verifies binding/project/pack plus all four normalized source inputs, reads the committed `REPORT_L1`, and dispatches only the three L2 calls. Live `L2_ONLY` is refused by both host and instrumentation.
- Added host fixture selection and selective retention so the eight-fixture G1 base run preserves only `fx-a04`, `fx-a11`, and `fx-a02`; repeat runs can share the G1 group ledger without replacing those source databases. G2 can pass the base G1 `RunId` for exact predecessor reuse. The frozen budget table now describes this wiring while keeping G1 at 24 calls / USD 0.954 and G2 at 24 calls / USD 0.875.
- The verifier checks selected fixtures, provider kind and mode call counts, REPORT_L1, final hashes, spend-ledger integrity, and captured prompt leaks. L1_ONLY no longer requires an L2 edit-prompt file because that phase is not run.
- `:app:compileDebugAndroidTestJavaWithJavac`: PASS; `test_verify_fixture_run.py`: **4/4 PASS**; Python bytecode compile: PASS; `run_group.ps1` parser: PASS; `git diff --check` for this package: PASS. The three-fixture fake emulator run will be exercised with the archived current-source APKs in L8.
- No emulator call, live provider request, API-key read, fingerprint use, or spend occurred in L5.
- **L5 offline implementation and compile gates PASS; emulator dry-run proof pending L8.**

## L6 — host and captured-prompt leak guards

- Added `verify_prompt_inputs.py`; `run_group.ps1` invokes it after preparing the selected transfer files and before pushing them to the emulator. It checks the exact four transfer-source hashes against the frozen manifest, reads the three OpenRouter prompt builders plus the three canonical pack instruction assets, and rejects target IDs, corrected label text not present in visible sources, and label-field names. Diagnostics contain only fixture IDs and fixed error codes.
- The device-side recorder already writes the rendered bytes before delegate dispatch. The host verifier now requires the exact ordered capture set for each mode: L1 phases, L2 discovery/edit/final-read, and L3 reaudit/reconcile/final-read; reused G1 `REPORT_L1` in `L1_THEN_L2` correctly requires L2 prompts only. It rejects missing/extra/empty captures, checks that the legacy L2 prompt copy is byte-identical to the captured `L2_EDIT`, and checks every capture plus `REPORT_L1` for answer leaks. It also distinguishes three billable calls from eight total fake/live pipeline calls in `L3_ONLY`.
- Host tests: `test_verify_fixture_run.py` **9/9 PASS** and `test_verify_prompt_inputs.py` **5/5 PASS**; Python bytecode compile: PASS; `run_group.ps1` parser: PASS. The prompt-input guard passed across all 14 fixture sources and prompt templates; frozen fixture verification reported **14 fixtures, 0 errors**.
- The actual emulator capture round-trip will be exercised during L8 after the wrapper build. No emulator dispatch, live provider request, API-key read, fingerprint use, or spend occurred in L6.
- **L6 guard implementation and host tests PASS; emulator prompt-capture evidence pending L8.**

## L7 — current emulator AndroidTest sweep (PASS)

- The current source `6631abc6ed778ccd84b17784899b3e62b3f413ff` was built in clean temporary worktree `D:\P5E-builds\wt-p6-r6-l7-scroll-20261003` through both required wrappers. Production `4.18-p6.14`/code227, event `build-20261003-123317`, APK SHA-256 `304B9C9C6C7428EB82E2DFDA20A0EB9F50F22D7798B2C41EF8396034EF5F405F`, source ZIP SHA-256 `979626374E612567092BDF5A35E932FBD428A48CAD52CA34A81A2399595D56C6`; app unit tests **339/339** and lint PASS. AndroidTest event `p6-r6r7-6631abc6-20261003-14`, APK SHA-256 `B67D7A1BA4F30BE344409366875A4F5D9491DFFB165E52F842832B6A3F422846`, same source ZIP, signer SHA-256 matches production. Production payload (5 files) and AndroidTest payload (8 files) match between `artifacts/` and `backup/`; both were verified before install. Production and AndroidTest APKs were installed only on `emulator-5554`.
- Earlier wrapper evidence remains recorded: source `57806a6f` code225 app unit `331/331` and lint PASS; source `e2ef0323` code226 app unit/lint PASS. The wrong-route synthetic case was corrected from the approved model to `not-the-approved-model`; the preflight class passed **5/5** on code226 and **5/5** again on current code227.
- The safe emulator sweep ran 31 of 42 instrumented classes in temporary Android user 10 (`Codex-P6-L7-20261003`), isolated from user0 Settings and app data. The initial result was **182 passed, 2 failed**. Both failures are now classified and resolved on current code227: `EditorialPackManagementPageInstrumentedTest#detailRebuildAndActivityRecreationDoNotExposeMutationControls` was caused by display sleep (`isSleeping=true`) and reran **3/3 PASS** with display awake; `V415LibraryScrollInstrumentedTest#usePreservesListScrollForGlossaryAndPronoun` used too few synthetic profiles, so both lists were increased from 12 to 40 and the current-source method reran **1/1 PASS**. No current failure remains unclassified.
- The safe scope excludes 11 provider/account/pilot/main-DB classes: `EditorialP5CLiveL1PilotInstrumentedTest`, `EditorialP5CLiveRecoveryInspectionInstrumentedTest`, `EditorialP5CRealBindingDeviceSetupInstrumentedTest`, `EditorialP5DVol5RawPilotInstrumentedTest`, `EditorialP5EAccountCheckOnlyInstrumentedTest`, `EditorialP5EFreshPilotInstrumentedTest`, `EditorialP5EFreshRawBoundaryInstrumentedTest`, `EditorialP5EFreshRawLiveInstrumentedTest`, `EditorialP5EFreshRawRouteDiagnosticInstrumentedTest`, `EditorialP5EReconcileLiveInstrumentedTest`, and `V48RealApiPromptInstrumentedTest`. `EditorialChapterFinalCoordinator#seedMainDatabaseForEmulatorUiSmoke` received no seed opt-in and assumption-skipped before opening the main DB. Preference-mutating classes ran only in user10. The included P5C fake E2E uses an in-process loopback server/test key.
- Failure inventory: negative-route preflight fixture (wrong synthetic model; repaired, 5/5 PASS); pack-management activity recreation (display sleep; environmental, 3/3 PASS on awake display); library scroll preservation (short synthetic fixture; repaired to 40 entries, 1/1 PASS). Private logs: `D:\P5E-private\p6-runs\l7-regression-57806a6f-20261003-1200`, `D:\P5E-private\p6-runs\l7-preflight-rerun-e2ef0323-20261003`, `D:\P5E-private\p6-runs\l7-pack-management-awake-u10-e2ef0323-20261003`, and `D:\P5E-private\p6-runs\l7-scroll-repair-6631abc6-20261003`.
- C9's original 22 historical failure log was not retained. Commit `d9ccdcda` updated 13 stale schema-v24 expectation sites across six classes, and those classes passed in the current sweep. The exact historical IDs for the other failures cannot be reconstructed and are not inferred from present-day exclusions. This history limitation is recorded separately; all failures from the current sweep are classified and resolved. External provider calls/spend: **0**. No live opt-in, owner fingerprint, account argument, or API key was supplied to these tests.
- Temporary user10 was removed after the reruns; the emulator is back on user0 and `stay_on_while_plugged_in` was restored to its original value `1`. Pilot `15e84958` was not accessed.
- **L7 PASS for current emulator regression and failure classification.**

## L8

Pending. Reuse the verified code227 artifacts for the 14-fixture fake run, then run the opt-in wrong-fingerprint preflight on emulator user0 and require `FINGERPRINT_MISMATCH` with zero provider calls. No live provider call is part of L8.

## G1

Not started. Authorized only after L0–L8 pass; maximum total spend USD 1.00; zero retries or repair calls; stop on UNKNOWN or cap exhaustion. Report separately before G2.

## Current next action

Complete the L8 14-fixture fake run and wrong-fingerprint zero-call check using the verified code227 artifacts.
