# P5C — Recovery and idempotency report

Ngày ghi nhận: `2026-09-08` (+07:00)

## Authorized live boundary

| Boundary | Observed behavior | Evidence |
|---|---|---|
| RAW provider dispatch | Exactly one request, no automatic retry | `P5C_LIVE_RESULT`, `providerCalls=1` |
| Provider no-response/timeout | Typed `RETRY_PROVIDER_CALL_FAILED` after 179,728 ms | `P5C_LIVE_PHASE`, `primaryCalls=1`, `finishReason=NOT_CALLED` |
| RECONCILE after RAW uncertainty | Not dispatched | Live result `reconcile=NOT_RUN` |
| Durable recovery | `RECOVERY_REQUIRED`, no response/report/receipt bytes | `EditorialP5CLiveRecoveryInspectionInstrumentedTest` `1/1` |
| Duplicate external call | Not attempted automatically | Runner exited after the typed stop |

## Verified in app-bound fake E2E

| Boundary | Expected behavior | Evidence |
|---|---|---|
| Missing authorization | Stop before provider; no row | `incompleteAuthorizationStopsBeforeProvider` |
| Source byte drift | `STOP_SOURCE_DRIFT`; no provider; no attempt | `sourceDriftStopsBeforeProviderAndLeavesNoAttempt` |
| First claim | One durable `CLAIMED` row | `EditorialP5CAttemptStore` claim path |
| Store reopen while claimed | `IN_FLIGHT`; do not issue a second call | `claimedAttemptIsInFlightAfterStoreReopenAndRecoveryIsRetryable` |
| Explicit recovery | `RECOVERY_REQUIRED` can be reclaimed once under exact facts | Same recovery test |
| Valid RAW predecessor | Read back from durable store before RECONCILE | Exact-binding E2E assertion |
| Final result | Report, receipt, response identity and metrics commit together | Exact-binding E2E readback |
| Duplicate dispatch after reopen | `ALREADY_COMMITTED`; provider call count remains `0` | Exact-binding E2E replay |

## Not yet verified

The following remain intentionally unclaimed:

- process death while a real provider request is pending;
- final resolution of the timeout's external call state;
- cancellation during a real provider call;
- a provider response or provider rejection receipt from a real adapter;
- live response truncation or one schema-only repair;
- real token/cost/latency and egress measurement.

The engine-local fake suite covers these decision types with deterministic
provider seams (`EditorialP5PilotExecutionBoundaryTest`, `15/15`), but that is
separate from live evidence.

## Safety result

No duplicate semantic call was made after the live timeout. No partial final
report/receipt was accepted, no chapter state was certified, and no project was
rebound. The live attempt retains `executionAllowed=false` and
`NOT_CERTIFIED`; a future retry requires explicit recovery of the external
provider state and a new authorization.
