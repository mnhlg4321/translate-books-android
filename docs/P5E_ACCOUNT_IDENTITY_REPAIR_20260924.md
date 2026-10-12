# Account identity lifecycle repair — 2026-09-24

Status: OFFLINE_REPAIRED; closed device event remains NOT_PROVEN. No new device event is authorized or executed by this repair.

## Root cause and correction
The previous parser required exactly one class and one method line in the entire output. AndroidJUnitRunner emits both fields in its START (code 1) and FINISH (code 0) bundles. The account test independently emits its result-only bundle with code 0. The old synthetic success fixture omitted START and merged identity/result, so it did not model the producer contract.

The parser now validates three ordered bundles: expected class/method START, one exact MATCH or MISMATCH result-only bundle, expected class/method FINISH. Then it requires one OK (1 test) summary and terminal -1. Duplicate fields, extra/missing/reordered bundles, conflicting identities, wrong result case/value, negative status, repeated results, failure markers and truncated completion fail closed. Identity is checked in both lifecycle bundles; this is not deduplication or a relaxed count-only check.

Source basis: app/src/androidTest/java/com/ml/tblandroidtxt/EditorialP5EAccountCheckOnlyInstrumentedTest.java sends the result-only status. AndroidX InstrumentationResultPrinter testStarted/testFinished sends the two identity bundles: https://raw.githubusercontent.com/android/android-test/main/runner/android_junit_runner/java/androidx/test/internal/runner/listener/InstrumentationResultPrinter.java . This upstream reference is main, not an assertion that the exact installed runner source was rebuilt. Device stdout/stderr was not read; this repair reproduces the contract defect offline and does not retroactively reinterpret the closed receipt.

## Validation
- Runner transport/parser regression under Windows PowerShell 5.1: PASS. Fixed an additional test-only .NET Contains overload incompatibility with that host.
- Full command → preflight fake executable → actual account runner → parser → receipt: 38/38 PASS. MATCH, MISMATCH and wrong-identity/NOT_PROVEN paths each preserve one launch and reject a repeat event.
- Helper self-test PASS, including transport, bounded process, outcome fixtures and redaction.
- Independent Luna lifecycle matrix 25/25 PASS and old-parser RED → repaired-parser GREEN: see P5E_ACCOUNT_IDENTITY_LIFECYCLE_QA_20260924.json.
- No Android source changes, APK build/install, real ADB/device/provider/DB/RAW action. Existing closed event and receipts are preserved.

## Integration and next action
The command now pins the repaired helper. Old authorization packets and historical RAW helper pins remain historical and fail closed against changed bytes; they are not approvals to use this revision. No event path is created or silently changed.

Next action: review the qualified repaired command/helper hashes for a separately approved account-only event. Do not repair the same parser again or ask for key/provenance metadata again without a new concrete failure. P6 remains closed until account and subsequent P5 criteria are actually satisfied.
Luna final challenge found trailing nonblank data after terminal was accepted; this was fixed and the independent matrix rerun PASS. The RED baseline is pinned to commit 55061312f40f367b933d0c2db4866335cd36a8ed so the regression remains reproducible after committing this fix.
