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

## L7 — current emulator AndroidTest sweep (FAILED_REPAIRING)

- First wrapper build used clean temporary worktree `D:\P5E-builds\wt-p6-r6-live-l7-20261003`, source `57806a6f32347c6183bdac66a5441220989ccdd5`: production `4.18-p6.12`/code225, event `build-20261003-115734`, APK SHA-256 `9DC483656DEE23D828D0644E21156E753F49FFC8E9C13738D1F02B96AA3F8677`, source ZIP SHA-256 `D7D6770C8C76EABBDC8AB11FA5FE8E7B19A44BC02E948B43550F9351C2841C32`; app unit `331/331` and lint PASS. AndroidTest event `p6-r6r7-57806a6f-20261003-12`, APK SHA-256 `9CC64BF2408911EDD1ABD57E862160C98EFB5AD47850A45BA0ACC84E3BEE6665`, same source ZIP and matching production signer. Both payloads were mirrored in `artifacts/` and `backup/`, verified, then installed only on `emulator-5554`.
- The user0 safe sweep ran 26 of 42 classes: **172 passed, 1 failed**. `EditorialP6FixtureLivePreflightInstrumentedTest#wrongRouteIsRejectedBeforeProviderConstruction` used the approved model in the negative-route fixture. Source changed to `not-the-approved-model`; on wrapper build `4.18-p6.13`/code226 from commit `e2ef03238de55be3511f6038d47650abc16b35c1`, the preflight class reran **5/5 PASS**. Event `build-20261003-121134`; production APK SHA-256 `0ED218EEF0F458ACE30C5B8F22A5091D92062F014D8EEC5E76F19790F604FD5F`, source ZIP SHA-256 `59C26BBFBE0BB0CB0883C219AD57B26498D14AC635A9AD98B6102444E9F68176`; AndroidTest event `p6-r6r7-e2ef0323-20261003-13`, APK SHA-256 `59F1A72B2670B986D1B045968025BB284EF88066E4A9643E96CBE2703C9DBD59`. Wrapper app unit/lint passed; both artifact payloads were mirrored and verified.
- To safely run preference-mutating/fake-seed classes without the owner’s Settings data or main DB, created temporary emulator user 10 (`Codex-P6-L7-20261003`), with no API key. The 31-class user10 sweep produced **182 passed, 2 failed**. `EditorialPackManagementPageInstrumentedTest#detailRebuildAndActivityRecreationDoNotExposeMutationControls` failed while the emulator display was asleep (`isSleeping=true`); with display kept awake, the class reran **3/3 PASS**. `V415LibraryScrollInstrumentedTest#usePreservesListScrollForGlossaryAndPronoun` exposed a synthetic fixture too short to scroll; both fixture lists are now 40 entries, but this source repair still needs its wrapper rebuild and targeted rerun. L7 remains `FAILED_REPAIRING` until that rerun passes.
- The safe scope excludes 11 provider/account/pilot/main-DB classes: `EditorialP5CLiveL1PilotInstrumentedTest`, `EditorialP5CLiveRecoveryInspectionInstrumentedTest`, `EditorialP5CRealBindingDeviceSetupInstrumentedTest`, `EditorialP5DVol5RawPilotInstrumentedTest`, `EditorialP5EAccountCheckOnlyInstrumentedTest`, `EditorialP5EFreshPilotInstrumentedTest`, `EditorialP5EFreshRawBoundaryInstrumentedTest`, `EditorialP5EFreshRawLiveInstrumentedTest`, `EditorialP5EFreshRawRouteDiagnosticInstrumentedTest`, `EditorialP5EReconcileLiveInstrumentedTest`, and `V48RealApiPromptInstrumentedTest`. The `EditorialChapterFinalCoordinator#seedMainDatabaseForEmulatorUiSmoke` selector received no seed opt-in and assumption-skipped before opening the app DB. Settings-mutating tests ran only in temporary user10. The P5C fake E2E suite uses an in-process loopback server/test key; no live arguments were supplied.
- Current failure inventory and classification: wrong-route fixture, repaired and 5/5 PASS; pack-management activity-recreation failure, environmental sleep, rerun 3/3 PASS; scroll-preservation fixture, synthetic test data was insufficient, repaired to 40 entries and rerun pending. Logs: `D:\P5E-private\p6-runs\l7-regression-57806a6f-20261003-1200`, `D:\P5E-private\p6-runs\l7-preflight-rerun-e2ef0323-20261003`, and `D:\P5E-private\p6-runs\l7-pack-management-awake-u10-e2ef0323-20261003`.
- C9's original 22 historical failure log was not found. Commit `d9ccdcda` updated 13 stale schema-v24 expectation sites across six classes, and those classes passed in the current sweep. Exact historical IDs for the remaining failures cannot be reconstructed from retained logs; they are not inferred from current exclusions. No external provider calls or spend occurred; the tests passed no live opt-in, fingerprint, or account arguments.

## L8

Pending. Rebuild the 40-entry scroll fixture source through both required wrappers, rerun `V415LibraryScrollInstrumentedTest` and the sleep-affected UI class while the display stays awake, then complete the emulator regression, 14-fixture fake run, and wrong-fingerprint zero-call check.

## G1

Not started. Authorized only after L0–L8 pass; maximum total spend USD 1.00; zero retries or repair calls; stop on UNKNOWN or cap exhaustion. Report separately before G2.

## Current next action

Build the current L7 source as production/test artifacts and rerun the scroll and sleep-affected instrumented classes on the isolated emulator user.
