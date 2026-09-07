# P5C — Recovery and idempotency report

Ngày ghi nhận: `2026-09-07` (+07:00)

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

The following are intentionally not claimed because there was no live external
call:

- process death while a real provider request is pending;
- timeout with external call state unknown;
- cancellation during a real provider call;
- provider rejection/network failure from a real adapter;
- live response truncation or one schema-only repair;
- real token/cost/latency and egress measurement.

The engine-local fake suite covers these decision types with deterministic
provider seams (`EditorialP5PilotExecutionBoundaryTest`, `15/15`), but that is
separate from live evidence.

## Safety result

No duplicate semantic call was made in the fake replay. No partial final
report/receipt was accepted, no chapter state was certified, and no project was
rebound. All P5C results retain `executionAllowed=false` and
`NOT_CERTIFIED`.
