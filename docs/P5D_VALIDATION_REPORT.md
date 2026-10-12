# P5D — Validation report (historical sections superseded by P5E)

Ngày cập nhật: `2026-09-10` (+07:00); các mục trước là evidence lịch sử.

## Decision recorded before the P5E preservation incident

The decision block below is retained as dated historical evidence. It is not
the current pilot gate. The current P5E decision is
`DB_REOPEN_STALE_CLAIM_RECOVERY_VERIFIED`,
`PILOT_DATA_PRESERVATION_GATE_FAILED_ORIGINAL_CODE196_DB_LOST_RECONSTRUCTED_ONLY`,
`P5E_LIVE_PREP_BLOCKED_PILOT_DATA_PRESERVATION` and
`RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED`; no RAW authorization or provider
call is permitted from this document.

```text
P5D_DOCUMENTATION_BASELINE_CONSISTENT
P5D_EXTERNAL_AUDIT_COMPLETE
P5D_LIFECYCLE_HARDENING_PASS
P5D_REGRESSION_PASS
P5D_DEADLINE_BODY_READ_HARDENING_PASS
END_TO_END_DEADLINE_VERIFIED
STALLED_BODY_RECOVERY_VERIFIED
DB_REOPEN_STALE_CLAIM_RECOVERY_VERIFIED
NO_LATE_COMMIT
NO_AUTOMATIC_REDISPATCH
PILOT_DATA_PRESERVATION_STATUS_AT_COLLECTION_ONLY
NO_CURRENT_RAW_RETRY_AUTHORIZATION
LOCAL_TRANSPORT_AND_LIFECYCLE_VERIFIED
HISTORICAL_CANCELLATION_CAUSE_UNRESOLVED
RAW_DIAGNOSTIC_ATTEMPT_COMPLETE
RAW_DIAGNOSTIC_STOPPED_RETRY_OUTPUT_TRUNCATED
RAW_PREDECESSOR_NOT_COMMITTED
RECONCILE_AUTHORIZATION_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```

P5D chưa hoàn tất exit gate. P5D.1/P5D.2 đã có bằng chứng OpenRouter
authenticated, read-only và phân loại được external cancellation. P5D.5 và
P5D.6 đã được thực hiện cho đúng một RAW diagnostic attempt; attempt dừng
typed ở `RETRY_OUTPUT_TRUNCATED`, không có predecessor hợp lệ và P5D.7 chưa
được thực hiện.

Phần kiểm chứng mới nhất ngày `2026-09-09` được ghi ở cuối báo cáo. Kết luận
mới nhất là `LOCAL_TRANSPORT_AND_LIFECYCLE_VERIFIED` và
`HISTORICAL_CANCELLATION_CAUSE_UNRESOLVED`. Các local gate đủ để chuẩn bị một
RAW diagnostic authorization, nhưng chưa có authorization mới và không gọi
provider.

## Baseline and immutable identity

| Item | Value |
|---|---|
| Workspace | `D:\App Translate Books\App Translate Books-translation-profile` |
| Branch | `feature/v4.18` |
| Implementation baseline before the code189 validation snapshot | `6a310f9b6c5bfbd8f0fee6179f22331524e823c6` |
| Device | `15e84958`, `CPH2691`, API 35 |
| Installed validation APK | `4.17-dev.21 / code189` |
| Validation APK SHA-256 | `D66C3C816E29403508BF997413998683FDCAD6AC2C74F24DA40E9AF31570860C` |
| Canonical ZIP SHA-256 | `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987` |
| Java control ZIP SHA-256 | `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5` |
| Profile resource SHA-256 | `1B2DB011D59F3E2EF4349AEB0DAA9C54A19B7EFD1E2CA6886BC29B56E4690D62` |
| Canonical profile hash | `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21` |

Authority hashes remain unchanged: project `1727AE...1A26AD`, prompt
`D25757...7CD754`, workflow `5DB6B4...505730` (full values remain in
`BUILD_STATE.md`).

## P5D.1–P5D.2 read-only external audit

After manual authentication, the bounded OpenRouter Logs view showed one row
matching the old P5C dispatch: `Sep 8, 09:29 PM` (+07:00 display), model
`GPT-5.6 Luna`, provider `OpenAI`, app `Translate Books with LLMs`, and `17,808`
input tokens. Generation details showed generation ID
`gen-1788877749-P8b2hBo1TWuduENuKbQ3`, `Finish reason: cancelled`, streaming
`true`, provider HTTP `200`, `17,808 / 84` input/output tokens, displayed cost
`$0.00366`, routing `258 ms`, provider `675 ms`, generation `9.8 s` and total
`10.8 s`. Request/response I/O logging remained disabled.

The app-side evidence remains one dispatched RAW request for selector
`p5c-real-mercedes-vol4-001`, no usable response/usage receipt in the app, no
retry and no RECONCILE. The provider-side generation is therefore classified
as:

```text
EXTERNAL_CONFIRMED_CANCELLED
RETRY_ELIGIBLE: YES_AFTER_NEW_EXACT_PHASE_AUTHORIZATION
PROVIDER_CALLS_IN_P5D.0-P5D.4: 0
```

The provider-side cost is recorded as metadata, but this is not a successful
L1 result because the app never obtained a response it could validate and
commit. No prompt, chapter text, request body, response body, API key or secret
was exported. The redacted record is
`docs/P5D_PROVIDER_RECONCILIATION_RECORD.md`.

## Commands and results

| Command/evidence | Result |
|---|---|
| `gradlew.bat :editorial-engine:test --no-daemon` | `180/180 PASS` |
| `gradlew.bat :app:test --no-daemon` | aggregate `645/645 PASS`; debug/release/benchmark XML; `0` failures/errors |
| Code186 `connectedDebugAndroidTest` on `15e84958` | `124` test methods, `0` failures, `0` errors, `4` approved skips |
| P5D focused lifecycle/recovery tests | PASS; see `P5D_RECOVERY_AND_LIFECYCLE_REPORT.md` |
| Authenticated OpenRouter Logs audit | One matching generation; `cancelled`, metadata-only; no I/O logging |
| External qualification | historical `306/306 PASS`; not rerun as part of P5D |
| Provider/API calls during P5D.0–P5D.4 | `0` |
| `git diff --check` | PASS before documentation commit |

Compared with the prior code184 device baseline (`118` test methods), code186
contains `+6` test methods. The increase is P5D lifecycle/recovery coverage and
schema/migration regression coverage; the four approved skips remain opt-in or
require validation-package data that was removed during baseline restore.

## Scope guard

P5D changes are limited to the existing attempt/recovery/provider owners,
additive v21 recovery tables, direct tests and documentation. No authority,
canonical pack, trusted profile, UI activation, project binding, provider
framework, build metadata or database owner outside the approved P5D recovery
scope was changed. No provider call was made while the external state was
unresolved.

## Historical next gate before the VOL5 diagnostic follow-up

At this earlier checkpoint, the next action was a new exact-phase, single-use
RAW recovery authorization after the user acknowledged the cancelled external
generation and duplicate billing risk. That authorization was later issued
and consumed by the diagnostic attempt recorded below. The current next action
is no automatic retry and no RECONCILE; execution remains
`EXECUTION_DISABLED / NOT_CERTIFIED`.

## Historical VOL5 follow-up before the diagnostic attempt (2026-09-09)

This addendum records a later independent VOL5/chapter001 attempt after the
validation package key was restored. The earlier missing-key gate remains
historical. The exact persisted binding was re-read, one RAW request was
dispatched under the bounded user authorization, and the durable row ended
`RECOVERY_REQUIRED` with local reason
`RETRY_PROVIDER_CALL_FAILED_UNKNOWN`, no response identity and zero
`REPORT_L1`/receipt bytes.

Authenticated OpenRouter Logs metadata matched generation
`gen-1788910936-DHfTNOyDlU3f3PJOAvqb`: `cancelled`, provider HTTP `200`,
`23,674/90` tokens, displayed cost `$0.00484`, and no I/O logging. The
external classification is `EXTERNAL_CONFIRMED_CANCELLED`. The consumed
authorization cannot be reused; another RAW dispatch needs a new exact-phase
single-use authorization with duplicate-work/billing-risk acknowledgement.
RECONCILE remains unauthorized and the P5D exit gate is still incomplete.

Full redacted evidence is in
`docs/P5D_VOL5_RAW_PROVIDER_RECONCILIATION.md`.

## Controlled RAW diagnostic attempt — 2026-09-09

The exact-phase authorization was approved and consumed once for the
persisted VOL5/chapter001 binding. The app dispatched exactly one RAW primary
request through the lifecycle-persisting adapter; schema repair and automatic
network retry were both `0`. OpenRouter returned HTTP `200` with a complete
JSON transport body, but the output ended at the authorized `2,048` token cap
with `finish_reason=length`. The app therefore returned the typed stop
`RETRY_OUTPUT_TRUNCATED` and did not commit partial `REPORT_L1` or a receipt.

App metrics were `20,327` input tokens, `2,048` output tokens, `22,375` total
tokens, reported cost `$0.0075392` and local latency `20,590 ms`. The durable
row remained `RECOVERY_REQUIRED`; lifecycle readback after execution recorded
`RESPONSE_BODY_COMPLETE`, HTTP `200`, `application/json`, request bytes
`85,068` and the redacted generation identity. Provider metadata independently
matched the same generation, request and response identities. No provider
call followed, no RECONCILE request was made and the consumed authorization
cannot be reused.

The full redacted evidence, source-identity preservation and gate separation
(`SAFETY_STOP_PASS`, `TRANSPORT_PASS`, `RAW_ACCEPTANCE_PASS`) are recorded in
[`docs/P5D_RAW_DIAGNOSTIC_ATTEMPT_REPORT.md`](P5D_RAW_DIAGNOSTIC_ATTEMPT_REPORT.md).

## Code189 local HTTP harness addendum — 2026-09-09

See `docs/P5D_LOCAL_HTTP_HARNESS_REPORT.md` for the detailed baseline,
timeline and source-identity table. The current decision is:

```text
LOCAL_TRANSPORT_AND_LIFECYCLE_VERIFIED
HISTORICAL_CANCELLATION_CAUSE_UNRESOLVED
RAW_DIAGNOSTIC_RETRY_READY
```

The bounded local harness passed immediate, 11-second delayed-with-legacy-cancel
and 11-second delayed-without-legacy-cancel cases (`1/1` each). The complete
fake E2E class passed `13/13`; code189 full instrumentation passed `130` tests
with `0` failures. Engine/app debug unit XML reported `396` tests with `0`
failures/errors/skips. Static qualification remained `306/306 PASS`. No
provider call was made.

These are the pre-dispatch code189 harness results. The later single approved
diagnostic call and its outcome are recorded in the dedicated attempt section
above and in `P5D_RAW_DIAGNOSTIC_ATTEMPT_REPORT.md`.

The code189 recorder path persisted and read back content type, generation ID,
request byte count, terminal lifecycle stage and elapsed time after DB reopen
on isolated test databases. The historical VOL5 attempt itself still has no
lifecycle or local reconciliation row because it predates the recorder wiring;
its durable state remains `RECOVERY_REQUIRED` with no response/report/receipt.

The current PRONOUN transport file is `455` bytes / `63E79EEB…1A49C` because it
contains a three-byte UTF-8 BOM. The existing app-owned BOM removal produces
`452` bytes / `4947FF91…20686`, exactly matching the persisted binding. No
source rewrite or rebind was needed. The diagnostic attempt preserved this
identity and the historical cancellation actor remains unknown.

## Controlled RAW diagnostic preflight — 2026-09-09

The exact recovery/authorization fixtures were rerun on code189 without
touching the VOL5 pilot database or calling a provider:

- `EditorialP5CExactBindingFakeE2EInstrumentedTest`: `13/13 PASS`.
- `EditorialP5CLiveRecoveryInspectionInstrumentedTest`: `1/1 PASS`.
- `EditorialP5PilotExecutionBoundaryTest`: `16/16 PASS` with JDK 17+.
- Provider calls in this preflight: `0`.

The detailed identity table and pre-dispatch authorization snapshot are in
[`docs/P5D_RAW_DIAGNOSTIC_PREFLIGHT.md`](P5D_RAW_DIAGNOSTIC_PREFLIGHT.md).
The snapshot's recovery gate was subsequently passed with the exact approved
authorization; the single diagnostic attempt then stopped at
`RETRY_OUTPUT_TRUNCATED`. No further retry or placeholder decision is
permitted.

## Code196 deadline/body-read/recovery hardening — 2026-09-10

This was the hardening result at collection time, separate from the historical
live attempts. It is now historical because the later connected-installer
incident removed the original code196 package data; the current P5E gate is
therefore not a preservation pass. Production change
`d39bca7dcd16a64d6a97006d71e17f994653c065` was
built and installed as validation APK `4.17-dev.28 / code196` using
`scripts/build-and-save.ps1` and `adb install -r` on device `15e84958`.

| Evidence | Result |
|---|---|
| Production APK | `artifacts/builds/v4.17-dev.28/build-20260910-190649/TranslateBooks-v4.17-dev.28-code196.apk` |
| Production APK SHA-256 | `85345086FBD76FA78133EE54741CA7631EBA91EB4761401080EC10BA1D35042A` |
| Focused test APK SHA-256 | `292F30A50302423E695571BB28E95514504F06F174361762919C35FE6D1704DE` |
| Device package | `com.ml.tblandroidtxt`, `versionCode=196`, `versionName=4.17-dev.28` |
| Database | schema v24; readback present at collection time; later original data unavailable |
| Exact stalled-body executeRaw, short deadline | `1/1 PASS`, `2.069s`, typed timeout + recovery |
| Exact stalled-body executeRaw, 300-second deadline | `1/1 PASS`, `301.501s`, typed timeout + recovery, no host force-stop |
| Local server dispatch count | `1` request in the final cases |
| Focused P1–P5C/importer/migration regression | `92/92 PASS` |
| VOL5 read-only readback | `1/1 PASS` |
| Host engine | `183/183 PASS` |
| App unit | `222/222 PASS` per debug/release/benchmark variant |
| External qualification | `306 PASS / 0 FAIL` |
| High-confidence secret scan | PASS; no credential/private key/request body/chapter source in tracked files |
| Provider/API calls in this hardening validation | `0` |
| `git diff --check` | PASS |

The test-only local server sends headers and a body prefix, then stalls. The
app-owned path passes the exact persisted binding through the RAW adapter and
recorder, uses a scoped monotonic deadline, returns
`RETRY_PROVIDER_CALL_TIMEOUT`, persists `RECOVERY_REQUIRED`, and cleans up
boundedly. DB-reopen/stale-claim coverage reclassifies an expired claim and
refuses redispatch; the retained evidence does not prove two independent app process
invocations. Response-body progress is stored as byte count only; no body, key
or exception message is persisted. The delayed local HTTP cases still verify
that the legacy global cancellation slot does not cancel RAW.

The code196 run proves the local gates, not a live provider result. The
historical cancellation actor remains `UNKNOWN`; the acceptance generation
`gen-1788967700-RgJDCWrZsNZ4VAAWmlj8` remains externally unresolved and has no
validated body/report/receipt. The prior confirmed-cancelled generation had
displayed cost `$0.00484`; the separate truncated diagnostic had displayed
`$0.0075392`. Neither is a successful L1 result, and no `$0` inference is
allowed for the unresolved acceptance attempt.

## Historical next-action note — superseded

The historical local decision was later superseded by the P5E data-preservation
gate. The current decision is:

```text
P5E_LIVE_PREP_BLOCKED_PILOT_DATA_PRESERVATION
RAW_AUTHORIZATION_NOT_PREPARED
RECONCILE_AUTHORIZATION_REQUIRED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```

No preflight or authorization may be prepared until the original-data gate is
restored or the owner explicitly approves a disjoint fresh-pilot design. A
future authorization, if separately approved, must reference the reconciled
code191 generation, current exact binding/source hashes, compact wire schema,
one primary call, zero schema repair, zero automatic retry and explicit
duplicate/billing risk. This report does not issue that authorization and does
not call the provider. RECONCILE remains closed until a valid RAW predecessor
is committed and read back.
