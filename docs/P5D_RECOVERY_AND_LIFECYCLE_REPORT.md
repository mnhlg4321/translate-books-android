# P5D.3–P5D.4 — Recovery lifecycle hardening and regression

Ngày kiểm tra: `2026-09-08` (+07:00)

## Kết luận phạm vi

P5D.1/P5D.2 đã hoàn tất ở phạm vi read-only provider audit sau khi người dùng
đăng nhập thủ công. P5D.3 và P5D.4 đã hoàn tất ở phạm vi local/app-owned.
Không có provider/API call nào được thực hiện trong P5D.0–P5D.4.

```text
P5D.1_EXTERNAL_AUDIT_COMPLETE
P5D.2_EXTERNAL_CONFIRMED_CANCELLED
P5D.3_LIFECYCLE_HARDENING_PASS
P5D.4_RECOVERY_REGRESSION_PASS
RETRY_ELIGIBLE_AFTER_NEW_AUTHORIZATION
RECOVERY_REQUIRED
NO_NEW_RETRY_AUTHORIZATION_ISSUED
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```

## Production owners đã thay đổi

- `EditorialP5CAttemptStore`: owner cho lifecycle row, durable authorization
  receipt, immutable reconciliation decision và recovery gate.
- `EditorialP5PilotExecution`: không reclaim `RECOVERY_REQUIRED`; chỉ cho phép
  retry sau reconciliation decision bất biến, new authorization hash và
  duplicate-risk acknowledgement phù hợp.
- `OpenAICompatibleClient`: phát lifecycle event đã khử nội dung, giữ request
  byte count, HTTP status, generation ID, provider response ID và elapsed time;
  không lưu body hay exception message.
- `OpenRouterEditorialP5PilotProvider`: phân loại typed provider failure theo
  DNS/connect/TLS/write/read/call timeout/cancel/HTTP/parse/unknown.
- `TranslationRepository`/`EditorialMigrationSpec`: additive schema v21 với
  `editorial_p5d_network_lifecycle`,
  `editorial_p5d_authorization_receipts` và
  `editorial_p5d_reconciliation`; không sửa authority, UI, profile hay build
  metadata.

## Lifecycle evidence contract

| Evidence | Persisted value | Content policy |
|---|---|---|
| Call stage | `CALL_CREATED`, `REQUEST_BODY_STARTED`, `REQUEST_BODY_SENT`, `RESPONSE_HEADERS_RECEIVED`, `RESPONSE_BODY_COMPLETE`, `CALL_CANCELLED`, `CALL_FAILED` | enum only |
| Request size | byte count | no request body |
| Response facts | HTTP status, generation ID, provider response ID | metadata only |
| Failure | allowlisted exception class and typed reason | no exception message |
| Timing | elapsed milliseconds | scalar only |

The lifecycle row has no response-body column. Authorization IDs are stored as
SHA-256 fingerprints. Reopen/readback tests verify that a consumed receipt
remains consumed and cannot be reclaimed by process recreation.

## Read-only external reconciliation

The authenticated OpenRouter Logs view contained one matching generation for
the P5C RAW dispatch: `GPT-5.6 Luna` / `OpenAI` / `Translate Books with LLMs`
at `Sep 8, 09:29 PM` (+07:00 display), with `17,808` input and `84` output
tokens and displayed cost `$0.00366`. Generation details reported
`Finish reason: cancelled`, streaming `true`, provider HTTP `200`, generation
ID `gen-1788877749-P8b2hBo1TWuduENuKbQ3`, routing `258 ms`, provider `675 ms`,
generation `9.8 s` and total `10.8 s`. I/O logging remained disabled.

This is redacted provider metadata only. The app did not receive a usable
response or usage receipt, so the old attempt remains `RECOVERY_REQUIRED` and
did not produce a report or receipt. The provider-confirmed cancellation makes
a new RAW retry eligible only after a new exact-phase authorization; the old
authorization is consumed and cannot be reused. No RECONCILE authorization is
issued here.

## Recovery gate

An attempt in `RECOVERY_REQUIRED` cannot be claimed by a new authorization
alone. The store requires:

1. one immutable reconciliation classification;
2. an evidence reference and external-state decision;
3. a new authorization fingerprint;
4. an explicit retry-eligibility decision; and
5. duplicate-risk acknowledgement when the state is not provider-confirmed
   failed/cancelled.

Without those facts the execution boundary returns
`STOP_EXTERNAL_CALL_STATE_UNRESOLVED` with `providerCalls=0`.

## Test evidence

- `EditorialP5CExactBindingFakeE2EInstrumentedTest`:
  - recovery-owner tables and columns exist;
  - `RECOVERY_REQUIRED` is not reclaimable without reconciliation;
  - lifecycle and single-use authorization survive DB reopen;
  - immutable reconciliation requires a new authorization and duplicate-risk
    acknowledgement;
  - unallowlisted exception metadata is rejected.
- `EditorialPackMigrationSpecTest.v21AddsOnlyRedactedP5DRecoveryOwners`:
  additive v21 migration contains only the three P5D recovery owners and
  redacted identity fields.
- `EditorialP5PilotExecutionBoundaryTest`:
  unresolved external recovery returns typed stop with zero provider calls.
- Latest device validation (code186, device `15e84958`): XML reports
  `124` test methods, `0` failures, `0` errors and `4` approved skips.
- Latest host validation:
  - `:editorial-engine:test`: `180/180 PASS`;
  - `:app:test`: aggregate `645/645 PASS` across debug/release/benchmark XML.

The device was restored to `4.17-dev.1 / code169` after validation. No attempt
row from the validation package is treated as the original durable P5C row;
the restore removed validation-package data after the recovery evidence had
been exported in redacted form.

## Not yet proven

- A new RAW retry or a live RECONCILE response.
- Provider-issued generation metadata from a successful, app-validated live
  response.
- Final `REPORT_L1`/receipt commit from a real chapter.

Therefore P5D.5–P5D.7 remain closed and no new provider authorization is
issued by this report.

## Later VOL5 follow-up

The statements above are the `2026-09-08` lifecycle baseline. On
`2026-09-09`, after a separate key-restored setup, one exact VOL5 RAW request
was dispatched under a user authorization and OpenRouter later confirmed its
generation as `cancelled`. The local attempt remains `RECOVERY_REQUIRED` with
no response/report/receipt. That later evidence does not satisfy P5D.5 for a
further retry, does not authorize RECONCILE and does not prove live lifecycle
metadata persistence for that attempt; see
`docs/P5D_VOL5_RAW_PROVIDER_RECONCILIATION.md`.

## Code189 local HTTP harness closure — 2026-09-09

The local harness was tightened without changing the production deadline:
listening and accepted sockets are both owned and closed, server/client waits
are bounded and interruptible, scheduled delay work is cancelled, server
failures are surfaced to the assertion, and cleanup reports incomplete
termination. A test-only foreground keepalive was needed on the validation
device because the instrumentation target could be freezer-suspended while a
socket was intentionally quiet.

Code189 evidence:

- installed package: `4.17-dev.21 / code189`, APK SHA-256
  `D66C3C816E29403508BF997413998683FDCAD6AC2C74F24DA40E9AF31570860C`;
- local immediate response `1/1`, delayed-with-legacy-cancel `1/1`, and
  delayed-without-legacy-cancel `1/1`;
- `EditorialP5CExactBindingFakeE2EInstrumentedTest` `13/13 PASS`;
- full device instrumentation `130 tests, 0 failures`, with real provider
  paths still opt-in/skipped;
- schema/migration-related device tests `41/41 PASS`; engine/app debug unit
  XML `396/396 PASS`; external qualification `306/306 PASS`;
- local adapter lifecycle row survived DB close/reopen with request byte count,
  response content type, generation ID, terminal stage and elapsed time.

The first delayed harness attempt stopped at `DELAY_STARTED`; redacted thread
state showed the server worker waiting on its bounded latch and the client in
the read/cancellation path. Keeping the target foreground made the two delayed
cases complete. This is a `SUPPORTED_HYPOTHESIS` for the local harness
interruption only. It does not identify the actor that cancelled the historical
OpenRouter generation, which remains `UNKNOWN`.

The real VOL5 attempt remains historical and was made before code189 recorder
wiring: schema v22 is present, but its attempt has no lifecycle row and the
local reconciliation table has no row. It remains `RECOVERY_REQUIRED` with no
response/report/receipt. The current device PRONOUN source is 455 bytes /
`63E79EEB…1A49C`, while the persisted binding requires 452 bytes /
`4947FF91…20686`; this prevents a new RAW dispatch as `STOP_SOURCE_DRIFT`.
No source was rewritten and no provider call was made in this closure.
