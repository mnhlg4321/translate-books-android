# P6 R6 wire simplification execution — 2026-10-04

Scope: `docs/P6_R6_WIRE_SIMPLIFICATION_PROPOSAL_20261004.md` at approved §9, on the existing v4.18 continuation branch. Offline packages S1–S4 completed; S5 was resumed on the existing G1 ledger and stopped at its first L1 refusal. No G2 or pilot work was performed.

## S1–S4 and V1 validation

The earlier unit-reference change uses wire v3 with `L<line>` references; the app resolves those references to full `u:<line>:<hash>` identifiers before validation and persistence. S1 adds safe `CODE:path` diagnostics, S2 captures fixture response content without headers or credentials and supports offline parser replay, and S3 derives schema/prompt requirements from the shared field specifications. S4 makes L2/L3 `before` optional: the app derives the source line from its number and records a warning if a supplied substring does not match after NFC/whitespace normalization. The contract revision is V5; v4 attempts are not reused for the new contract.

Offline validation passed: `:editorial-engine:test` 362/362, `:app:testDebugUnitTest` 341/341, and `:app:compileDebugAndroidTestJavaWithJavac`. `scripts/p6/test_verify_fixture_run.py` passed 9/9 after aligning its negative-gate expectation with the new safe path.

Pushed implementation commits: S1 `71ebab19`, S2 `883129d6`, S3 `a4e17a97`, S4 `cc3747ad`. Follow-up test/verifier commits `8a2b11ba` and `c4529088` keep the offline negative-gate check aligned with `L1_COVERAGE_GAP:coverage`.

## V2 emulator validation

The production wrapper archived and installed `4.18-p6.17` / versionCode 230, event `build-20261004-084923`, from source `cc3747ad`. APK SHA-256: `380C6042B6D22E84142ACCB083C3A16740809DF867EC2FD18F84042B4C560721`. The five-file artifact payloads in `artifacts/builds` and `backup/builds` match.

The AndroidTest wrapper event `p6-s4-neg2-8a2b11ba` produced APK SHA-256 `9611FF4A49204F5D7138FEC2DAC30CEE86F4D64495EFBD72891B34CD4889D313`; the eight-file artifact and backup payloads match. It was installed on `emulator-5554` and exercised against the installed production build.

Fake CHAIN run `633dc74d-1154-4246-a2ec-5b920ec40295` completed `STRUCTURAL_VALID 14/14`, with 0 actual provider calls and USD 0. The fake no-edit semantic scores are control output, not model-quality evidence.

Negative-gate run `bb7de2bb-26ad-4da6-aceb-ab36aa4a499c` completed on the emulator and passed `verify_fixture_run.py --expect-invalid-l1`: `STRUCTURAL_VALID 0/1`, expected `L1_COVERAGE_GAP:coverage`, one fake call, 0 actual provider calls, USD 0, no pending reservations. The earlier event `p6-s4-neg-8a2b11ba` is superseded: it packaged the previous APK because the wrapper was invoked from a different working directory. Its unique artifact remains preserved; use `p6-s4-neg2-8a2b11ba` for verification.

## S5 — resumed G1 and stopped at fx-a03

A zero-call fingerprint sentinel reached the deliberately absent runtime manifest, confirming the expected fingerprint matched before provider construction. Its private log is `D:\P5E-private\p6-live-checks\20261004\matching-preflight-sentinel.txt`; no fingerprint value is retained in this report.

Before S5, the emulator’s established group ledger `G1-20261003-ecbf8c55` matched the prior G1 file byte-for-byte: 4 settled calls, USD 0.03222845 / 1.00, 0 pending UNKNOWN, and USD 0.96777155 remaining. S5 run `7b6ac5b8-aea9-44b0-8f92-da72ca0d2e7e` started with `fx-a03`, mode `L1_ONLY`, on that same ledger.

The first phase, `L1_RAW_DISCOVERY`, was rejected. Safe result:

- `P6_L1_PREDECESSOR_FAILED:REPAIR_L1_LEDGER_INVALID`
- phase `L1_RAW_DISCOVERY`; detail **`L1_UNIT_UNKNOWN:coverage.0.from`**
- 1 actual provider call; 21,186 input tokens / 222 output tokens; finish `stop`; settled cost **USD 0.00556275**
- no reconcile, retry, repair, REPORT_L1, or later fixture was run

The response content was captured without headers or credentials at:

`D:\P5E-private\p6-runs\7b6ac5b8-aea9-44b0-8f92-da72ca0d2e7e\results\fx-a03\responses\001-L1_RAW_DISCOVERY.json`

Offline replay through the production parser reproduced `L1_UNIT_UNKNOWN:coverage.0.from`. The response is 330 bytes; its SHA-256 is `28823BCFA24BEBB809DFC5D2283E2F2F69151C1E1147CC00C69F77E8AD925492`. Replay command:

```powershell
.\gradlew.bat --offline :editorial-engine:replayP6WireResponse `
  -Pp6ReplayPhase=L1_RAW_DISCOVERY `
  "-Pp6ReplayResponse=D:\P5E-private\p6-runs\7b6ac5b8-aea9-44b0-8f92-da72ca0d2e7e\results\fx-a03\responses\001-L1_RAW_DISCOVERY.json" `
  "-Pp6ReplayRaw=D:\P5E-private\p6-fixtures\fx-a03\RAW.txt"
```

A typed refusal intentionally makes the Java replay process exit 2, which Gradle reports as a failed task (wrapper exit 1); the emitted `CODE:path` above is the replay result. No provider call occurs during replay.

After S5, the same G1 ledger has 5 settled calls / 10 entries, **USD 0.03779120 / 1.00**, USD 0.96220880 remaining, and 0 pending UNKNOWN. Ledger validation passed. The group stopped at `fx-a03` as required; the remaining G1 fixtures and repeats were not started. No G2 or pilot work occurred.

## Commit/push and next action

The implementation and follow-up verifier commits listed above are pushed to `feature/v4.18-p5e-runner-repair-20260917`; this execution record and the workspace snapshot are committed separately. Next action: review the captured `fx-a03` response offline and decide whether to authorize a new G1 continuation; do not dispatch another provider call before that decision.