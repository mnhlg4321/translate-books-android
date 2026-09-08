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

## Baseline and immutable identity

| Item | Value |
|---|---|
| Workspace | `D:\App Translate Books\App Translate Books-translation-profile` |
| Branch | `feature/v4.18` |
| Implementation/documentation baseline before this reconciliation update | `e2019ee38217cf79b0ca0e995831582d0f6ae18d` |
| Device | `15e84958`, restored to `4.17-dev.1 / code169` |
| Baseline APK SHA-256 | `3C3AAEF1A7D47F39A7B5A5FF8AEDF77347D255AF728B908180AA142CA2B276D1` |
| Latest validation APK | `4.17-dev.18 / code186` |
| Validation APK SHA-256 | `A32CD2B379D13CCEE6D7FAB7E0512A1A73587175CE92C32C1CDCD3707245D10C` |
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
