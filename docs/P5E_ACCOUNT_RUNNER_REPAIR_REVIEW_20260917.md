# P5E host runner repair review — 2026-09-17

## Decision

The bounded host-only repair is complete and locally green. The account-device
branch remains closed because independent expected provenance is not available
to this work package. The five gates remain separate:

| Gate | Current status |
|---|---|
| Host runner repair | `HOST_RUNNER_REPAIR_OFFLINE_PASS` |
| Expected provenance | `EXPECTED_PROVENANCE_UNAVAILABLE_STOP / EXPECTED_SOURCE_PENDING` |
| Account boundary | `ACCOUNT_CHECK_NOT_EXECUTED` |
| A4.3 / RAW / P5 | `A4.3_NOT_ISSUED / RAW_NOT_RUN / P5_EXIT_NOT_CLAIMED` |
| P6 | `P6_NOT_READY` |

## Source review

`scripts/p5e-account-check.ps1` now pins the complete component
`com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner`, verifies
the runner/helper/APK hashes before reading expected or creating an ADB
process, and passes expected only as a UTF-8/LF stdin line to a remote `sh`
child. The expected value is absent from ADB argv and removed from the ADB
child Process environment. User/Machine environment, file, clipboard and
transcript channels are not used.

`scripts/p5e-raw-live-supervisor.ps1` carries the full component identity for
future RAW command construction, retains `-LibraryOnly`, and bounds the
post-timeout stdout/stderr task waits. This changes the future helper pin; it
does not authorize or execute RAW and does not update historical RAW evidence.

The parser requires one exact class, one exact method, one exact
`p5e.account.result=MATCH|MISMATCH`, one terminal `INSTRUMENTATION_CODE: -1`,
one `OK (1 test)`, zero failure markers and exit code zero. Missing, duplicate,
suffix, wrong-identity, terminal-failure, non-zero, timeout and redaction
conditions are not interpreted as `MISMATCH`.

The Java account-only source was not changed and remains pinned at
`2F4BF9AD27CF5DF93D89456767423271907598EA209A0AD6E4C27599BC20063C`.

Transport scope is precise: the expected digest is absent from the host ADB
argv and child environment, then reaches Android shell by stdin and is supplied
to instrumentation as a temporary extra. It is not raw credential. A claim that
the digest is absent from every device-side command argument would be inaccurate;
if that exposure is unacceptable, the device branch must stay closed.

## Offline evidence

`docs/P5E_ACCOUNT_RUNNER_REPAIR_QA_20260917.json` is `PASS` with 37/37
assertions. It covers source ordering, component construction, command-shape
rejection, missing/malformed expected, parser terminal/identity cases,
process-only inheritance with synthetic shape, exact stdin bytes, child-env
clearing, non-zero process, bounded timeout, no retry and digest-leak
redaction. It used five fake local process launches and zero ADB/device/
provider/database/RAW operations. No owner value was read.

The previous RED audit remains unchanged in
`docs/P5E_ACCOUNT_RUNNER_AUDIT_RESULT_20260917.json`; its four unexpected
acceptances are historical evidence of the defect repaired here. The prior
CheckOnly/replacement evidence remains unchanged, including replacement
`installAttempts=1`, CheckOnly `installAttempts=0`, production untouched and
the replacement APK hash `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8`.

## Provenance stop

No expected fingerprint, API key, endpoint or actual device fingerprint is
stored in this branch. The owner has not yet supplied metadata proving an
independent record, authority, account/project mapping and verification time.
Until that metadata exists and maps to the exact account/endpoint scope, the
stop is `EXPECTED_PROVENANCE_UNAVAILABLE_STOP`; no device launch is permitted.
An expected value must never be reconstructed from the device actual.

The owner response format is in
`docs/P5E_OWNER_LOCAL_INPUT_GUIDE_20260917.md`. It requests metadata only and
does not request a secret.

## Scope ledger

This work package did not install, uninstall, clear data, downgrade, build an
APK, invoke the account runner, invoke the RAW supervisor, call a provider or
endpoint, read/write the database, create authorization, retry or redispatch.
The next account event, if ever admitted, requires a separate follow-on after
the provenance and transport gates are both satisfied; `MATCH` would still
prove equality only and would not issue A4.3 or make P6 ready.
