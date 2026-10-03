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
- The 14-fixture emulator dry-run has not run yet. It is scheduled after P3 archives the exact APK and AndroidTest APK through the required wrapper; structural and semantic verdicts remain unmeasured until then.

## P3 — pending

Wrapper build/archive from a temporary worktree, emulator fixture dry-run and targeted R1–R4 regression. Record APK hashes, artifact parity, test results and C8/C9 handling here. The full instrumented suite is not a valid shortcut because it includes pilot/history and provider opt-in tests.

## P4 stop point

After P3, stop before live readiness. Owner must enter the key in emulator app Settings and provide the endpoint/account fingerprint. Price verification and live-group work are not part of this offline record.
