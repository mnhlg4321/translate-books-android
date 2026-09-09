# P5D — Local HTTP harness and code189 validation

Ngày kiểm tra: `2026-09-09` (+07:00)

## Quyết định

```text
LOCAL_TRANSPORT_AND_LIFECYCLE_VERIFIED
HISTORICAL_CANCELLATION_CAUSE_UNRESOLVED
RAW_DIAGNOSTIC_RETRY_READY
NO_PROVIDER_CALL_IN_THIS_VALIDATION
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```

Đây là kết quả kiểm chứng local trên code189. Không có live RAW retry và
không có RECONCILE request trong bước này.

## Baseline and artifact provenance

| Item | Value |
|---|---|
| Workspace | `D:\App Translate Books\App Translate Books-translation-profile` |
| Branch | `feature/v4.18` |
| Source HEAD used to build code189 | `6a310f9b6c5bfbd8f0fee6179f22331524e823c6` |
| Device | `15e84958` / `CPH2691` / API 35 |
| Installed validation package | `4.17-dev.21` / code189 |
| Production APK SHA-256 | `D66C3C816E29403508BF997413998683FDCAD6AC2C74F24DA40E9AF31570860C` |
| Test APK SHA-256 | `30EADECFA740326F5A3036584DB2F8613C90633E0DAF19DCF9472C0001EF2165` |
| Build source snapshot | `e809cfff7a98f6d7a6ffd756b9f0a0c06aa68c69` |
| DB schema readback | v22 |
| Provider calls in this validation | `0` |

Canonical ZIP, Java-control ZIP, profile resource and three authority hashes
were re-read and unchanged:

```text
canonical ZIP  B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987
control ZIP    44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5
profile v2     1B2DB011D59F3E2EF4349AEB0DAA9C54A19B7EFD1E2CA6886BC29B56E4690D62
project        1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD
prompt         D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F
workflow       5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730
```

## VOL5 pilot-state readback

The production database was read through `run-as` into an in-memory SQLite
connection; it was not reset, copied back or modified.

| Fact | Readback |
|---|---|
| Attempt | `157e3517b4b98535392db95f0c93285f08a0ac6b07341a82508ffc36aea9a5f0` |
| Attempt status | `RECOVERY_REQUIRED` |
| Local reason | `RETRY_PROVIDER_CALL_FAILED_UNKNOWN` |
| Response/report/receipt | absent / 0 bytes |
| Prior RAW authorization | consumed (`f39ff4fd…aa212`) |
| Lifecycle row for historical attempt | absent; call predates code189 recorder wiring |
| Local reconciliation row | absent; external classification remains documented separately |
| Execution/certification | disabled / not certified |

The missing lifecycle row is historical evidence, not a pass claim. The new
recorder is verified only on isolated code189 test attempts.

### Source identity comparison

| Role | Binding length/hash | Current device bytes | Result |
|---|---:|---:|---|
| RAW | 23,814 / `A308210E…04504BE` | 23,814 / `A308210E…04504BE` | match |
| DRAFT | 26,462 / `64ADECD8…7F62B5` | 26,462 / `64ADECD8…7F62B5` | match |
| GLOSSARY | 3,249 / `4BC3E2DD…A0314` | 3,249 / `4BC3E2DD…A0314` | match |
| PRONOUN | 452 / `4947FF91…20686` | 455 / `63E79EEB…1A49C` raw; 452 / `4947FF91…20686` after BOM removal | match |

The current 455-byte PRONOUN file is the user-supplied VOL5 source and begins
with the UTF-8 BOM `EF BB BF`. The existing app-owned `stripUtf8Bom` rule removes
exactly those three transport bytes before computing the source identity. The
result is 452 bytes with hash `4947FF9184995BE5F850F2323FBE0A04C67302FB8D5AFB63CF12202B44720686`,
matching the immutable binding. No source rewrite or rebind was needed.

## Harness fix and evidence

The test-only `SlowJsonServer` in
`EditorialP5CExactBindingFakeE2EInstrumentedTest` now owns both listening and
accepted sockets, uses bounded socket/executor waits, propagates server
failures, cancels scheduled delay work, captures redacted thread state at a
deadline, and reports incomplete cleanup. The device foreground keepalive is
test-only; it does not change the production deadline or client timeout.

The final focused results were:

| Layer/test | Result |
|---|---|
| Immediate local HTTP response | `1/1 PASS` |
| 11-second response with legacy cancel | `1/1 PASS` |
| 11-second response without legacy cancel | `1/1 PASS` |
| `EditorialP5CExactBindingFakeE2EInstrumentedTest` | `13/13 PASS` |
| Schema/migration-related device classes | `41/41 PASS` |
| Full instrumentation on code189 | `130 tests, 0 failures`; live provider tests remained opt-in/skipped |
| Engine + app debug unit XML | `396 tests, 0 failures, 0 errors, 0 skipped` |
| External static qualification | `306 PASS / 0 FAIL` |
| `git diff --check` before documentation update | `PASS` |

The immediate and delayed tests each observed exactly one local HTTP request,
HTTP 200, `application/json`, a redacted generation ID and a complete response
body. The adapter wrote `RESPONSE_BODY_COMPLETE` to the isolated v22 test DB;
the row was read back after DB close/reopen with request byte count, content
type, generation ID, empty cancellation source and elapsed time intact.

The first delayed run exposed the device test-process freezer: the timeline
stopped at `DELAY_STARTED`, the server worker was waiting on its bounded latch,
and the client was in the cancellation/read path. Keeping the target activity
foreground made both delayed cases complete. This is a
`SUPPORTED_HYPOTHESIS` for the local harness interruption, not a confirmed
cause of the historical OpenRouter cancellation. The historical actor remains
`UNKNOWN`; the external record only establishes provider-side cancellation.

Existing `executeRaw()` fake-provider tests separately pass the RAW-only path,
read back the committed predecessor and prove that no RECONCILE request is
constructed. The local HTTP tests exercise the real client/adapter/recorder
path but intentionally do not claim a live `REPORT_L1` acceptance.

## Scope and next gate

Production changes are limited to the existing lifecycle/transport owners and
additive v22 migration fields. Test changes are limited to the local harness,
current-schema expectations and the historical recovery assertion. No
authority, pack, profile, UI activation, project binding, provider call,
database reset, uninstall or build metadata change was used in this step.

The local gates now support preparation of one new exact-phase RAW diagnostic
authorization. The historical cancellation actor remains unknown, so this is
not evidence that the original cause was fixed. The old consumed authorization
is not reusable; no RAW retry is issued by this report. A separate local
reconciliation row may still be required by recovery policy before dispatch.
