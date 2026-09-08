# P5D — Validation report

Ngày kiểm tra: `2026-09-08` (+07:00)

## Current decision

```text
P5D_DOCUMENTATION_BASELINE_CONSISTENT
P5D_EXTERNAL_AUDIT_ATTEMPTED
EXTERNAL_STATE_REMAINS_UNKNOWN
P5D_LIFECYCLE_HARDENING_PASS
P5D_REGRESSION_PASS
NO_RETRY_AUTHORIZATION
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```

P5D không hoàn tất exit gate. P5D.1/P5D.2 đang chờ authenticated,
read-only OpenRouter Activity evidence; P5D.5/P5D.6/P5D.7 chưa được phép
thực hiện.

## Baseline and immutable identity

| Item | Value |
|---|---|
| Workspace | `D:\App Translate Books\App Translate Books-translation-profile` |
| Branch | `feature/v4.18` |
| Implementation baseline before this report | `e9f0b7907c27e88feb209dd23361b254377828dc` |
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

The bounded OpenRouter Activity page for the 2026-09-08 request and model
`openai/gpt-5.6-luna` redirected to sign-in in the available browser session.
No authenticated request/generation metadata was inspected. Input/output
logging was not enabled and no prompt, chapter text, response, API key or
secret was exported.

The app-side evidence remains one dispatched RAW request for selector
`p5c-real-mercedes-vol4-001`, no response/usage receipt, no retry and no
RECONCILE. The persisted classification is:

```text
EXTERNAL_STATE_REMAINS_UNKNOWN
RETRY_ELIGIBLE: NO
PROVIDER_CALLS_IN_P5D.0-P5D.4: 0
```

This is not a finding of “not sent” and is not a `$0` billing conclusion. The
redacted record is `docs/P5D_PROVIDER_RECONCILIATION_RECORD.md`.

## Commands and results

| Command/evidence | Result |
|---|---|
| `gradlew.bat :editorial-engine:test --no-daemon` | `180/180 PASS` |
| `gradlew.bat :app:test --no-daemon` | aggregate `645/645 PASS`; debug/release/benchmark XML; `0` failures/errors |
| Code186 `connectedDebugAndroidTest` on `15e84958` | `124` test methods, `0` failures, `0` errors, `4` approved skips |
| P5D focused lifecycle/recovery tests | PASS; see `P5D_RECOVERY_AND_LIFECYCLE_REPORT.md` |
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

The next action is manual, read-only authentication to the already-open
OpenRouter Activity page so the bounded audit can record redacted metadata or
retain `EXTERNAL_STATE_REMAINS_UNKNOWN`. Only after that classification and
P5D.3/P5D.4 evidence may the user issue a new exact-phase, single-use RAW
authorization. It must not reuse the consumed P5C authorization. RAW must stop
before RECONCILE and remain `EXECUTION_DISABLED / NOT_CERTIFIED`.
