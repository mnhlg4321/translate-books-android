# P5E RAW host-preparation evidence — 2026-09-15

Status: `HOST_PREPARATION_COMPLETE / P5E_CONTINUES / A4.3_NOT_ISSUED /
RAW_NOT_RUN / P6_NOT_READY`

This is host-only evidence for the bounded P5/P5E continuation. It is not a
new authorization, a new A4 proposal, or permission to dispatch. No credential,
instrumentation method, device, ADB command, provider call, runtime
authorization, database mutation, settings read, or RECONCILE operation was
performed while producing this evidence.

## Current baseline

| Fact | Evidence |
|---|---|
| Workspace | `D:\App Translate Books\App Translate Books-translation-profile` (the parent D1 checkout was not used) |
| Branch | `feature/v4.18-p5e-audit-20260914` |
| HEAD at start of host repair | `0f52d36e516560bb33d294303c70fa1753cb64f9` |
| Audit baseline | user-supplied prefix `f8fe433ef454772a1b55dea496a2c4bfd679766` resolves to `f8fe433ef454772a1b55dea496a2c4bfd679766f`; it is an ancestor of the start HEAD |
| Frozen production | code207 / `v4.17-p5e.11`; APK `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD`; source ZIP `B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348` |
| AndroidTest pin | `57EC99A95EE2DC0F1759934C62CEA39E2EC92EB77C3DAF76CFEED28D41A2FDEA`, package `com.ml.tblandroidtxt.test`, runner `androidx.test.runner.AndroidJUnitRunner`, source/archive commit `d51b7f3c16bdc482513b9904db07b97daed592d1` |
| Historical A4.2 state | exact preflight `PASS`, one `OK (1 test)`, terminal `-1`, zero provider/creation counts; this is stored history, not a new device run |
| Current live state | A4.3 `NOT_ISSUED / NOT_READY_FOR_DISPATCH`; RAW not run; P6 `NOT_READY` |

The frozen A4.3 approval manifest was rechecked without modification:

`docs/P5E_RAW_AUTHORIZATION_APPROVAL_MANIFEST.md` →
`DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501`.

The old command hash, which is retained as the audit RED input, was
`31B075935CECA342A249961F8E871410A700478B780A827F25D21DDAEE13BE98`.
The repaired command and helper are:

| File | SHA-256 | Role |
|---|---|---|
| `docs/P5E_RAW_AUTHORIZATION_COMMAND.txt` | `1D9A67693C4C4F64AF182300CDEB963A9FD9D44361EF77E91371F88C92EDA15E` | review-only child-process wrapper; not executed |
| `scripts/p5e-raw-live-supervisor.ps1` | `BEEFBB7733EED660B1F59435922D0594FBA1CBDED01B6C0B00482E786E456799` | host dispatcher, redacting capture, and independent outcome verifier |

The command file still carries the original manifest hash and the immutable
code207/test57EC99 pins. Its hash is deliberately different because the old
command did not prove safe remote-shell transport or durable acceptance.

## Artifact and source checks

The production event `build-20260911-201725` was compared file-by-file with
its backup: `5/5` payload files matched by name and SHA-256. The AndroidTest
event `a4-1-test-20260914-065532` was compared with its backup: `8/8` payload
files matched by name and SHA-256. The payloads include the APK, README,
`BUILD_INFO.json`, checksum manifest and exact tracked-source ZIP as applicable
to each event. `BUILD_INFO.json` was read for event/version/source metadata.
The test metadata says `installed=false`, `providerCalls=0`,
`authorizationCreated=0`, `attemptCreated=0` and `reconciliationCreated=0` at
build time; `installed=false` is not treated as a device-state failure.

The pinned live source was checked against `d51b7f3c16bdc482513b9904db07b97daed592d1`:

```text
git diff --quiet d51b7f3c16bdc482513b9904db07b97daed592d1 -- app/src/androidTest/java/com/ml/tblandroidtxt/EditorialP5EFreshRawLiveInstrumentedTest.java
PINNED_TEST_SOURCE_DIFF=0
CURRENT_ANDROID_SOURCE_DIFF=0
```

There was no production-source, schema, migration, pack/profile, prompt,
model/route, budget, input-identity or pilot-data change.

## F2 — RED to GREEN transport proof

The old path was reproduced with only a fake PowerShell process. PowerShell
`Start-Process` passed the unquoted legacy remote tokens to the fake process;
the simulated ADB join and POSIX parser then treated the pipe characters in
`p5e_cancellation_stop_authority` as shell operators. The parsed authority was
not one byte-exact value. This is a host construction defect, not a device
observation.

The repaired path uses `ProcessStartInfo.ArgumentList` for the child process and
POSIX single-quotes every token after `adb shell`, including the entire
pipe-delimited stop authority. The same fake process captured the final process
argv, then the ADB-join/shell parser saw zero operators and the exact authority
string. No fake executable can invoke ADB or a provider.

Self-test evidence:

```text
P5E_TRANSPORT_PIPE_RED=PASS
P5E_TRANSPORT_PIPE_FIXTURE=RED_TO_GREEN_PASS
P5E_TRANSPORT_PROCESS_ARGV_GREEN=PASS
P5E_PROVIDER_DEVICE_ACTIONS=0
```

The argument contract extracts the selected live method's requirements from
source and checks exact class/method, serial, all required key-value pairs,
no duplicate/extra opt-in, hashes, numbers, the 180000 ms expiry relation and
the unmodified stop-authority bytes. The source-derived contract contains 40
keys including the explicit `p5e_fresh_raw_live` opt-in (39 are explicit
required values inside the selected method).

Required key set:

```text
p5e_fresh_raw_live
p5e_authorization_id
p5e_authorization_id_hash
p5e_owner_approval_manifest_sha256
p5e_expected_attempt_identity
p5e_expected_request_identity
p5e_expected_request_envelope_hash
p5e_expected_canonical_request_body_sha256
p5e_expected_route_fingerprint
p5e_expected_endpoint_account_fingerprint
p5e_expected_project_row_id
p5e_expected_device_signature_token
p5e_expected_provider
p5e_expected_model
p5e_expected_upstream_provider
p5e_expected_phase
p5e_expected_selector
p5e_expected_chapter_key
p5e_expected_binding
p5e_expected_run
p5e_expected_evaluation
p5e_expected_pack_hash
p5e_expected_profile_hash
p5e_expected_schema_version
p5e_expected_production_version_code
p5e_expected_production_apk_sha256
p5e_expected_certificate_sha256
p5e_expected_db_sha256
p5e_maximum_primary_semantic_calls
p5e_maximum_schema_repair_calls
p5e_maximum_network_retries
p5e_maximum_input_tokens
p5e_maximum_output_tokens
p5e_maximum_total_tokens
p5e_maximum_total_cost_usd
p5e_maximum_execution_time_ms
p5e_authorization_issued_at_ms
p5e_authorization_expires_at_ms
p5e_evidence_redaction_policy
p5e_cancellation_stop_authority
```

Negative fixtures all rejected: missing remote quote, changed pipe value,
missing fingerprint, wrong body hash, duplicate argument, different class and
stale expiry. Result: `P5E_REQUIRED_ARGUMENT_FIXTURES=8_PASS` including the
green fixture.

## F3 — bounded supervisor and independent acceptance

The production dispatch path has one child-process launch at most and uses the
required units: host observation `240000` ms, authorization validity `180000`
ms and execution deadline `120000` ms. The offline fake process covered:

| Fake outcome | Numeric evidence | Required handling |
|---|---|---|
| success | launch/dispatch `1`, exit `0`, timeout `false` | observation only; post-readback still required |
| nonzero | launch/dispatch `1`, exit `7` | external state unresolved; no retry |
| timeout | launch/dispatch `1`, timeout `true` | external state unresolved; no expiry refresh or redispatch |
| failure before launch | launch/dispatch `0`, outcome `FAILED_BEFORE_LAUNCH` | no external call is claimed; no retry |

Output and error capture is redaction-checked for API-key, bearer,
endpoint/URL-shaped values on success, error and timeout fixtures. The helper
does not echo the argument list. Real dispatch uses hidden/no-window
`ProcessStartInfo`; it does not combine `NoNewWindow` with `WindowStyle`.

The verifier is independent of the process exit code. It requires the exact
class and method, exactly `OK (1 test)`, exactly one terminal
`INSTRUMENTATION_CODE: -1`, no failure markers, valid host metadata and a
post-readback object with schema `p5e.raw.readback.v1`. It checks only the
source/schema-derived allowlist: exact attempt and consumed authorization,
`COMMITTED` lifecycle, valid report/receipt/metrics, calls `1/0/0`, known cost
within the `$0.05` cap, valid token/deadline fields, schema/receipt validation,
the four-source tuple, integrity/FK, and zero reconciliation/history.
It does not impose A4.2's unrelated 67-field preflight schema.

The valid fixture was the only `RAW_ACCEPTED` result and explicitly returned
`p6Ready=false`. These fixtures were rejected:

```text
OK but RECOVERY_REQUIRED
OK but post-readback missing
COMMITTED but receipt missing
unknown cost
duplicate attempt
lifecycle without attempt
unrelated write
```

Result: `P5E_OUTCOME_FIXTURES=8_PASS`, counting the one valid fixture and the
seven rejection fixtures. No fixture is device or provider evidence.

## F1 — source account path and the one missing owner decision

The selected source path is viable and was not executed. In
`EditorialP5EFreshRawLiveInstrumentedTest`, the first credential-dependent
operation loads `SettingsStore.load(target).copy()`, verifies the fresh route,
requires a non-empty in-memory `settings.apiKey`, computes the fingerprint and
compares it to the expected argument. The helper method defines the exact
operation as:

```text
SHA-256(UTF-8(normalizeEndpoint(settings.baseUrl) + "\n" + settings.apiKey))
```

`AppSettings.normalizeEndpoint` trims the endpoint, removes one trailing slash,
and appends `/chat/completions` to a `/v1` endpoint. The live source emits the
actual fingerprint as lowercase hex; the host accepts A–F and normalizes the
owner-supplied expected value to lowercase in process memory. The selected
method has no credential/fingerprint stdout, stderr or evidence write.

Expected and actual are separate facts: the app computes actual from the
runtime settings; the owner must provide the expected value's trusted
provenance. No settings file hash, default, fabricated 64-hex value, old
harness, reflection, shell pull, logcat or preference edit was used.

The one remaining owner decision is therefore precise: provide a provenance
for the expected 64-hex endpoint/account fingerprint and approve this exact
memory-only account check immediately before any future dispatch, without
sending the credential to chat. The credential, endpoint text and intermediate
values must stay process-only and must not be logged or serialized. If this
decision/input is absent or the actual value does not match, the helper must
stop before runtime authorization construction and provider dispatch. No
AndroidTest change is needed for this path; host-only work cannot manufacture
the owner-approved expected value.

## QA and boundary result

QA round one passed after the final helper patch:

```text
PARSE_ONLY=scripts/p5e-raw-live-supervisor.ps1:PASS
PARSE_ONLY=docs/P5E_RAW_AUTHORIZATION_COMMAND.txt:PASS
FORBIDDEN_EXECUTION_PATTERNS=PASS
AUDIT_BASELINE_ANCESTOR=PASS
PINNED_TEST_SOURCE_DIFF=0
CURRENT_ANDROID_SOURCE_DIFF=0
PRODUCTION_PAYLOAD_PARITY=5/5:PASS
TEST_PAYLOAD_PARITY=8/8:PASS
P5E_HOST_SELFTEST=PASS
QA_ROUND_ONE=PASS
```

Adversarial review items were checked against the implementation and source:
expected fingerprint provenance is still explicitly pending; the pipe is
byte-exact only in the repaired path; sensitive shapes are redacted on
success/error/timeout; exit zero cannot bypass non-`COMMITTED` acceptance;
timeout does not retry; only the allowlisted DB rows/diffs can be accepted;
branch, source commit, artifact pins and proposal-era HEAD are distinguished;
and the future permission must name the account operation in addition to the
RAW/GLOSSARY egress and one-call budget. A second equivalent review is not
required after the targeted corrections; any new owner input would be a new
decision, not a third review loop.

Not done: no actual account fingerprint was computed, no credential was read,
no device or instrumentation run occurred, no post-live readback exists, RAW
was not accepted, RECONCILE was not opened, P5/P5E exit was not claimed, and
P6 remains false.

## Single next action

Owner supplies trusted expected-fingerprint provenance and decides whether to
permit the exact memory-only account verification described in F1. Until that
decision is recorded against the final hashes, do not dispatch A4.3.
