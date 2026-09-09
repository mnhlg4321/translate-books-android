# P5D — Validation report

Ngày kiểm tra: `2026-09-08` (+07:00)

## Current decision

```text
P5D_DOCUMENTATION_BASELINE_CONSISTENT
P5D_EXTERNAL_AUDIT_COMPLETE
EXTERNAL_CONFIRMED_CANCELLED
P5D_LIFECYCLE_HARDENING_PASS
P5D_REGRESSION_PASS
NEW_RAW_AUTHORIZATION_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```

P5D chưa hoàn tất exit gate. P5D.1/P5D.2 đã có bằng chứng OpenRouter
authenticated, read-only và phân loại được external cancellation. P5D.5 chưa
được cấp authorization mới; P5D.6/P5D.7 chưa được thực hiện.

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

## Next gate

The next action is a new exact-phase, single-use RAW recovery authorization
after the user explicitly acknowledges that the cancelled external generation
already has provider-side usage/cost metadata and a second semantic attempt
may repeat work and incur new billing. The consumed P5C authorization must not
be reused. P5D.6 must stop after a successful RAW predecessor commit and ask
for a separate RECONCILE authorization; execution remains
`EXECUTION_DISABLED / NOT_CERTIFIED`.

## Current VOL5 follow-up (2026-09-09)

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

The code189 recorder path persisted and read back content type, generation ID,
request byte count, terminal lifecycle stage and elapsed time after DB reopen
on isolated test databases. The historical VOL5 attempt itself still has no
lifecycle or local reconciliation row because it predates the recorder wiring;
its durable state remains `RECOVERY_REQUIRED` with no response/report/receipt.

The current PRONOUN transport file is `455` bytes / `63E79EEB…1A49C` because it
contains a three-byte UTF-8 BOM. The existing app-owned BOM removal produces
`452` bytes / `4947FF91…20686`, exactly matching the persisted binding. No
source rewrite or rebind was needed. The local gates therefore permit
preparation of a new diagnostic RAW authorization, but the historical
cancellation actor remains unknown and no call is issued here.

## Controlled RAW diagnostic preflight — 2026-09-09

The exact recovery/authorization fixtures were rerun on code189 without
touching the VOL5 pilot database or calling a provider:

- `EditorialP5CExactBindingFakeE2EInstrumentedTest`: `13/13 PASS`.
- `EditorialP5CLiveRecoveryInspectionInstrumentedTest`: `1/1 PASS`.
- `EditorialP5PilotExecutionBoundaryTest`: `16/16 PASS` with JDK 17+.
- Provider calls in this preflight: `0`.

The detailed identity table and the unissued authorization draft are in
[`docs/P5D_RAW_DIAGNOSTIC_PREFLIGHT.md`](P5D_RAW_DIAGNOSTIC_PREFLIGHT.md).
The recovery gate remains closed because the prior authorization is consumed
and the historical attempt has no reconciliation row. No placeholder retry
decision was persisted.
