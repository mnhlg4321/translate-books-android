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

## P2 — pending

Offline instrumented fixture runner, neutral L2 predecessor/oracle-leak checks, and durable append-only group-spend ledger.

## P3 — pending

Wrapper build/archive from a temporary worktree, emulator fixture dry-run and targeted regression. Record APK hashes, artifact parity, test results and C8/C9 handling here.

## P4 stop point

After P3, stop before live readiness. Owner must enter the key in emulator app Settings and provide the endpoint/account fingerprint. Price verification and live-group work are not part of this offline record.
