# P6 R6/R7 offline execution record — 2026-10-03

Scope: owner request `docs/P6_R6_R7_CODEX_WORK_REQUEST_20261003.md` at `1f1dd8b1`; complete P0–P3 only, then stop at P4 before key entry, fingerprint handling, price lookup or any provider call.

## P0 — baseline

- Branch: `feature/v4.18-p5e-runner-repair-20260917`.
- HEAD: `1f1dd8b1e21197a0b7692938797166387571d206`; this is the requested document commit and its parent is the snapshot's `f11581a3` implementation baseline.
- `git status --short --branch`: branch synchronized with origin; pre-existing owner edits and untracked evidence were present in `.idea`, old P5E docs/scripts, `raw/`, fixture/evidence directories, delivery/build artifacts and owner backup folders. No reset/clean was run. P1 source paths were clean before editing; none of the pre-existing paths were staged.
- Startup documents read in required order: `EDITORIAL_RECOVERY_V4_18.md` §§10–11, `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md`; also `GIT_WORKFLOW.md`, `DEVELOPMENT_WORKFLOW.md`, the requested work request, R0 issue table/fixture manifest, R5 protocol/budget, `HANDOFF.md`, and final-output acceptance.
- Baseline artifact/build facts remain those in the snapshot: v4.18-p6.5/code218 archived and present on emulator-5554; pilot 15e84958 remains on v4.18-p6.2/code215. No device or provider action was performed in P0/P1.

## P1 — product L1 entry point (offline)

- Added `runFromL1` to the existing chapter coordinator. It executes ledger-v2 RAW, reads the committed RAW predecessor back, executes RECONCILE, then continues through the existing L2/L3/final-read stages. Persisted committed stages are reused; a claimed/recovery-required stage is refused by the attempt store before the provider call.
- The chapter action can start when L1 is absent. Its confirmation shows all eight calls, individual output/USD caps, the USD 0.50 G4 cap, and the no-repair/no-retry/UNKNOWN stop rules. L1 RAW and RECONCILE use separate bounded provider adapters and one-use phase authorizations.
- Targeted app JVM tests: `EditorialChainBudgetsLedgerTest` and `EditorialChapterRunServiceTest`, **14/14 PASS**. The first run found one assertion mismatch in the legacy dialog wording; the wording was corrected and the same targeted tests passed on rerun.
- `:app:compileDebugAndroidTestJavaWithJavac`: PASS. The emulator end-to-end run-from-L1 test is added but remains pending the P3 archived build and emulator run.
- No provider calls, key reads by the agent, pilot access, installation or database changes.

## P2 — offline implementation ready; emulator acceptance runs under P3

- Added an opt-in Android instrumented fixture harness with `L1_ONLY`, `L2_ONLY`, `L3_ONLY`, and `CHAIN` modes. Each invocation creates an isolated temporary database and uses `EditorialP4BindingTransactionService.createSetup`, then the production L1/L2/L3 execution and durable stores with local fake providers. L2-only and L3-only predecessors are produced by the production chain; the neutral L1 report has no findings, target labels, `mustContain` data, or corrected text.
- `scripts/p6/run_group.ps1` verifies the frozen private fixture set, builds a runtime manifest containing only fixture id/chapter/file hashes and byte counts, pushes only `RAW.txt`, `DRAFT.txt`, `GLOSSARY.csv`, and `PRONOUN.csv`, runs the opt-in test on `emulator-5554`, then pulls private outputs. `scripts/p6/verify_fixture_run.py` checks the exact stored `REPORT_L1` and production-rendered L2 edit prompt against host-only labels, verifies the spend hash chain, and runs the frozen scorer with separate verdicts. Labels are never transferred to the emulator.
- The product chain now reserves each call's pinned R5 worst-case cost before dispatch into an append-only, per-binding/chapter JSONL ledger under app-private `files/evidence/p6-spend-ledger`. Hash-chain validation, restart-persistent UNKNOWN blocking, group-cap refusal before append/dispatch, and tamper rejection are covered by unit tests. The R5 price basis is explicitly provisional until the owner-controlled P4 repricing; no current price lookup was done.
- Verification so far: engine **340/340 PASS**, app JVM **330/330 PASS**, AndroidTest Java compile PASS, focused spend/budget tests **7/7 PASS**, `verify_fixtures.py` **14/14 hash/leak checks PASS**, Python/PowerShell source parse PASS, `git diff --check` PASS. No provider request or Settings/key access occurred.
- The 14-fixture emulator dry-run was attempted during P3; see its outcome below. Structural and semantic verdicts remain unmeasured while the exact-source replay guard fails.

## P3 — schema corrections, artifacts, and offline emulator regressions (FAILED_REPAIRING)

- C8: chapter 001 is 191 units. `EditorialRawInventory` excludes the `[IMAGE: …]` marker.
- C9 audit: no retained report enumerates the requested 22 historical v24/seed failures. Thirteen current-schema `24` assertions were found and updated to v25 in six isolated disposable-database AndroidTest classes. Pilot/main-database characterization tests remain excluded.
- Production archive: `4.18-p6.10`/code223, event `build-20261003-083922`, source `b16a707c3b54fba9a63dc704b6f151dde80a00b2`, APK SHA-256 `01787A89C8BF3F6E6BE26DA66B478385B961A47A64A680BC6C6CEF0E8C29ED21`; source ZIP SHA-256 `806A30C17831B4FB1326601FC13F09CD0C9937F2538F17EACED98294846A7D9D`. The wrapper passed app JVM `330/330` and lint. The payload is mirrored under `artifacts/builds` and `backup/builds`.
- Latest AndroidTest archive: event `p6-r6r7-0cc10cbd-20261003-06`, source `0cc10cbda0436bde2742687143c07635a67749b3`, APK SHA-256 `1934C2FE120AC1A002116D5804783B2A107F91DA3C678384010D8BD404060B60`, source ZIP SHA-256 `A729EE533F312DC38DF452FCA9E209A2FA94CFC0FE8957A8A80E67A89A6CA5B5`; matching eight-file payloads are in both test-build roots. Signer SHA-256 matches `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`.
- Harness repair: Android scoped storage hid both the host shell's app-external path and `/data/local/tmp` from UiAutomation. Inputs now transfer through host `adb push` into `/data/local/tmp`, are copied with `run-as` to the app-private `files/p6-fixtures/<runId>`, and are read directly by the app context. The test reads the canonical ZIP from instrumentation APK assets and preserves the archive bytes unchanged. A canonical numeric JSON count and CHAIN `REPORT_L1` readback capture were also repaired.
- Host fixture/hash/leak validation: `verify_fixtures.py` passes `14/14`. The safe existing test `EditorialChapterFinalCoordinatorInstrumentedTest#fullLedgerChainStartsAtL1AndReopensWithoutRepeatingAnyStage` passes `1/1` on `emulator-5554`.
- Latest fixture group `2124ef07-8f8c-4c79-9f8f-f24aa75d0a56`: all 14 instrumented fixtures reached P4's exact-source preflight and stopped `STOP_SOURCE_DRIFT`; `providerCalls=0`, `fakeCalls=0`, no output artifact was accepted. Per-fixture temporary databases are deleted in test teardown. No key, Settings read, API request, main/pilot database, or pilot device operation occurred.
- Source-identity diagnostic on `fx-a01` (run `1271e2b3-894d-4dad-9d69-6bba89e855df`) narrowed the mismatch to PRONOUN byte length and SHA only. The host fixture and pushed transfer both match the frozen 455-byte SHA-256 `63e79eebcbfe6bedcea088640339eb7d05aeda75c28fd4a4e17b282b8ed1a49c`; there are no other changed files. An AndroidTest follow-up now reports the expected/current lengths and SHA-256 prefixes without exposing source text, then repeats only `fx-a01` to identify the exact conversion.
- Still required: measure and fix the source replay mismatch; build and archive AndroidTest from the exact corrected commit; pass a one-fixture smoke and the 14-fixture `CHAIN` run with zero real provider calls; run selected safe R1–R4 and v25 schema classes; update `BUILD_STATE.md` and record exact counts/hashes. Keep the historical 22-method mapping gap explicit; do not include main-DB/provider tests in the offline suite.

## P4 stop point

After P3 passes, stop before live readiness. The owner must enter the key in emulator app Settings and provide the endpoint/account fingerprint. Do not perform price lookup or any provider call before the owner-controlled P4 step.
