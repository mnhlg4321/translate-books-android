# P5E RAW host-preparation evidence — 2026-09-15

> **Historical host-preparation result, superseded for current readiness.** The
> later SQL/collector/golden repair at implementation HEAD `77f060ad4aba61852c2c92f22326c2ed02180e20`
> is recorded in `docs/P5E_SQL_BEHAVIORAL_RESULT_20260916.json`. Do not use
> the old helper/command hashes below as current pins. A4.3, RAW and P6 remain
> closed; the body below is retained as historical evidence.

Historical status at report time: `LOCAL_VALIDATION_FAILED_REPAIR_REQUIRED / OWNER_PACKET_NOT_READY`. See `P5E_READINESS_AUDIT_20260916.md` and `P5E_NEXT_WORK_REQUEST.md`; three SQL defects superseded this report's readiness conclusion. This report's tests remain historical evidence; its file hash changes with this notice.

Historical 2026-09-15 status: `P5E_LOCAL_EVIDENCE_CHAIN_GREEN / OWNER_PACKET_PENDING /
A4.3_NOT_ISSUED / RAW_NOT_RUN / LIVE_ACTIONS_NOT_AUTHORIZED / P6_NOT_READY`

> Historical local closure at input baseline `8c24b7d2` and final probe HEAD `1d0dfe88`:
> H1–H4 are resolved for the offline host boundary. F1 account provenance and
> all live/device actions remain pending owner decision. The historical body
> below is retained as evidence; current hash-bound results are in
> `P5E_LOCAL_EVIDENCE_CHAIN_RESULT_20260915.json`.

This is host-only evidence for the bounded P5/P5E continuation. It is not a
new authorization, a new A4 proposal, or permission to dispatch. No credential,
instrumentation method, device, ADB command, provider call, runtime
authorization, database mutation, settings read, or RECONCILE operation was
performed while producing this evidence.

## Historical local closure claim — H1–H4

The final helper/collector is
`scripts/p5e-raw-live-supervisor.ps1`, SHA-256
`4D68F4BB0B0D31FA6D443439967746C1C83CCB4514EE6D7AF830CF90DBE0EC76`.
The review-only command is
`docs/P5E_RAW_AUTHORIZATION_COMMAND.txt`, SHA-256
`30B50BFEF809225B33901AC130CC5EE83CC8AD76A3D958D987EF19C8FCA50D10`; it was
parsed but not executed. The unchanged manifest remains
`DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501`.

H1 is closed by command-side and helper-side exact regular-file SHA checks
before environment/fingerprint/device access. H2/H4 are closed offline by
the explicit `CollectReadback` `Before`/`After` parameter set and same-event
collector. The collector uses package/APK/certificate readback, a
WAL-aware/read-only SQLite transaction, exact tuple/lineage/attempt/
authorization/lifecycle/artifact fields and immutable source mappings. It
returns typed `NO_CLAIM_OBSERVED`, `EXTERNAL_CALL_STATE_UNKNOWN`,
`RECOVERY_REQUIRED` and `COLLECTOR_TYPED_STOP` outcomes. The After collection
is the recovery path after timeout/process death/device reconnect and does not
refresh authorization or redispatch. Each read-only command is recorded in the
bounded, same-event `COLLECTOR_COMMAND_LOG.jsonl` with operation class,
numeric exit code, launch count and timeout state; argv and captured output are
not written to that log.

H3 is closed at the host contract boundary by
`docs/P5E_PRODUCTION_ARTIFACT_CONTRACT_20260915.json`, SHA-256
`FFE70A70E622706FABFA49D5843310ECD5A283B1CA114E32C636EA26B9FAE4BF`, pinned
to production serializer source SHA-256
`1222B8AC9B79DAFC659DD364F50849DFBA4782C181606A92DA47EBD8C6164E3C`. Report
and receipt have separate required/allowed field sets and pair mappings. The
golden Java test source is present (SHA-256
`8977BC825A3E1DDF73D5D049BD27737755F1B11160CD684EFC185A32460E77BE`) but was
not executed because this request forbids build. The offline synthetic
regression matrix rejects 11 cases: wrong manifest, binding, bundle,
predecessor, swapped bytes, one-byte mutation, extra/missing field, BOM,
invalid UTF-8 and noncanonical bytes.

The concrete field-to-source chain is:

| Required field group | Read-only source/entry point | Transform and gate |
|---|---|---|
| package/APK/certificate | `Get-P5EPackageReadback`; `pm path`; `dumpsys package`; local `apksigner` | exact installed package/version/code/APK/cert; mismatch is typed stop |
| schema/integrity/FK/WAL | `Get-P5EConsistentDatabaseReadback`; `run-as ... sqlite3 -readonly` | one WAL-aware read transaction; main/WAL/SHM hashes and schema/integrity/FK are checked |
| immutable tuple/source projection | `Get-P5EBindingReadback`; `Get-P5EConsistentDatabaseReadback` | exact binding, run, evaluation, pack/profile and RAW/GLOSSARY visible/DRAFT/PRONOUN hidden identities |
| attempt/auth/lifecycle | database query functions `Get-P5EAttemptReadback`, `Get-P5EAuthorizationReadback`, `Get-P5ELifecycleReadback` | exact event/attempt pair; consume is `issued <= consume < expires`; lifecycle is tied to the attempt |
| report/receipt | persisted `report_bytes`/`receipt_bytes` decoded from DB rows | exact bytes → UTF-8/no BOM/canonical/source-derived validators → hash/length; pair atomicity is checked separately |
| atomicity/allowed diff | transaction source/test pins plus before/after lineage | source transaction semantics + observed zero/allowlisted rows; no unrelated writes/deletes/reconciliation/history |
| event/provenance | `EVENT_PLAN.json`, `HOST_RUN_METADATA.json`, `COLLECTOR_COMMAND_LOG.jsonl`, `COLLECTOR_OUTCOME.json` | canonical event directory, allowlisted command class/exit metadata, collector/hash/source mapping and chronology; no cross-event reuse |

Missing/unavailable sources produce typed stop or `UNKNOWN`; no field is
entered as a caller-supplied boolean. The collector never pulls settings
content, credentials, raw endpoint, prompt, request body, model response or a
full database export. The current result is
`docs/P5E_LOCAL_EVIDENCE_CHAIN_RESULT_20260915.json`.

## Historical baseline for prior host-preparation evidence

| Fact | Evidence |
|---|---|
| Workspace | `D:\App Translate Books\App Translate Books-translation-profile` (the parent D1 checkout was not used) |
| Branch | `feature/v4.18-p5e-audit-20260914` |
| HEAD at start of earlier F2/F3 host repair | `0f52d36e516560bb33d294303c70fa1753cb64f9` |
| HEAD at resume of this provenance repair | `31a262a806372dc804a0650c4d65f8012d4f78bb`; clean before local mutation |
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
| `scripts/p5e-raw-live-supervisor.ps1` | `6FABAE1F53973942561EDA52002F052F775570A931EAB65188103E7DEE9955A2` | host dispatcher, redacting capture, synthetic producer, and independent outcome verifier |

The command file still carries the original manifest hash and the immutable
code207/test57EC99 pins. Its hash is deliberately different because the old
command did not prove safe remote-shell transport or durable acceptance.

The prior helper hash `BEEFBB7733EED660B1F59435922D0594FBA1CBDED01B6C0B00482E786E456799`
is retained as the RED input for the provenance review; it is not the current
packet helper. The manifest and command hashes are unchanged.

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

Within this earlier outcome-only matrix, the valid fixture was the only
`RAW_ACCEPTED` result and explicitly returned `p6Ready=false`. These fixtures
were rejected:

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

## F3 — provenance repair and producer-to-verifier proof

The four false accepts from the provenance review were repaired in the helper.
The corrected consume boundary is `issued <= consumed < expires`; the verifier
also binds `before <= claim`, `attempt.created <= attempt.updated <= observed <=
collected`, and permits a post-readback observation after authorization expiry
when the call/commit chronology is otherwise valid. Report and receipt bytes
are read and validated independently, and both must carry the exact
source-derived pack manifest fingerprint
`0353d751924d02ef0928bb6460c4ab894fee7c6324506e62b2972090e519c4da`.
This value is a pack-manifest identity, not the owner account fingerprint.

The exact boundary fixture set is independently recorded as
`issued-1=reject`, `issued=accept`, `expires-1=accept`, `expires=reject` and
`expires+1=reject`. The fixtures align claim/attempt/observation timestamps so
the result tests the authorization upper/lower bound itself.

`Invoke-P5ESyntheticReadbackCollector` is a concrete offline producer entry
point. It consumes only a disposable event directory containing:

```text
HOST_RUN_METADATA.json
collector-input.json
before-snapshot.json
after-snapshot.json
transaction-evidence.json
report.bin
receipt.bin
```

It validates the source input, event/run identity, WAL-aware snapshot marker,
transaction evidence and actual UTF-8/no-BOM serialized artifact bytes; it
then writes `post-readback.json` with hashes, lengths, source paths, collector
identity/hash, timing and separate atomicity evidence. It has no ADB,
instrumentation, provider, real database or credential path. This proves the
producer-to-verifier contract on synthetic content, not that a live device
collector exists on the pinned APK.

### Field → source → transformation → gate

The required root fields of `p5e.raw.readback.v1` are all covered by the
mapping below: `schemaVersion`, `observedAtMillis`, `externalCallState`,
`production`, `test`, `database`, `freshTuple`, `lineageBefore`,
`lineageAfter`, `attempt`, `authorizationReceipt`, `lifecycle`, `artifacts`,
`integrity` and `provenance`. Nested required fields are checked by the same
producer/verifier path, rather than added as unproven placeholders.

The nested required sets are: `production` package/version/versionCode/APK/cert;
`test` package/target/APK/cert/runner/sourceCommit; `database`
before/after/schema/integrity/FK; `freshTuple` project/selector/chapter/
binding/run/evaluation/pack/profile/mode/projection/four sources; both lineage
objects' six counts; `attempt` identity/status/timestamps/artifact hashes and
metrics; `authorizationReceipt` identity/phase/attempt/timing/caps/consume
result; `lifecycle` attempt/stage/byte counts/status/generation/response;
`artifacts.report` and `.receipt` schema/type/identity/manifest/pack/profile/
chapter/phase/predecessor/hash/length/validation; `integrity` allowed-diff,
immutability, atomicity and delete flags; and `provenance` source paths/hashes,
collector identity, event binding, timing and `atomicityEvidence`.

| Field group | Source file/function | Transformation | Acceptance gate |
|---|---|---|---|
| Package/certificate | `collector-input.json` → `production`, `test` | preserve exact package, version/code, APK/certificate/source commit | pinned artifact/package/certificate identity |
| Fresh tuple and four sources | `collector-input.json` → `freshTuple` | preserve project/selector/chapter/binding/run/evaluation, pack/profile, role/visibility, bytes and hashes | exact fresh tuple and immutable source/binding/run/settings projection |
| DB/schema/integrity/FK | `before-snapshot.json`, `after-snapshot.json` | map snapshot SHA-256, schema, integrity, FK, tuple identity | `WAL_AWARE_CONSISTENT`, schema24, `ok`, FK `0`, exact path/source hash |
| Lineage and allowed diff | snapshot `lineage` plus transaction evidence | compare zero-before with allowlisted after pair; do not edit DB | one attempt/receipt/lifecycle/report pair, reconciliation/history `0`, no unrelated write/delete |
| Attempt/authorization/lifecycle | `collector-input.json` templates | bind exact event/run/attempt and replace artifact hash/length with validator output | consumed exact receipt, `COMMITTED` exact attempt, caps/timestamps/lifecycle identity |
| Report/receipt bytes | `report.bin`, `receipt.bin` | real no-BOM UTF-8 JSON parse, schema/identity checks, SHA-256/length | stored bytes valid and cross-linked; validator result is not caller-supplied boolean |
| Atomicity | `transaction-evidence.json` plus before/after | keep transaction semantics, row-pair and byte validation as separate evidence | transaction flags, matching source hash, allowed diff and no delete |
| Provenance/timing | metadata + all six producer files + collector implementation | canonicalize paths, hash every source, record run/claim/consume/attempt/observation/collection times | exact event/file binding, collector identity/hash and chronology |

The tracked probe result is `docs/P5E_PROVENANCE_REVIEW_RESULT.json`, SHA-256
`BCB2DE2BD98C8191EB32CBE8298089ADFB42A8DADF33231A2733B4C272B72D01`. It
records the two accepted controls
(`valid_control`, `late_observation_control`), five exact authorization-boundary
fixtures, rejection of all four repaired mutations, six typed producer stops,
twelve verifier negatives, `P5E_FINGERPRINT_FAILURE_REDACTION=PASS`,
`deviceActions=0`, `providerCalls=0` and `p6Ready=false`. No boolean was
supplied by the owner or copied from `New-P5EValidReadbackFixture`; the
synthetic transaction flags are disposable test inputs, while the producer
creates the input files and the verifier recomputes hashes/validation,
identity, path and chronology from those files.

The producer negative cases are typed stops for missing report/receipt, wrong
event, schema drift, incomplete WAL-consistency marker and invalid validator
output. The verifier negatives cover missing rows, orphan lifecycle, duplicate
attempt, wrong event, schema drift, incomplete snapshot, missing artifact,
modified tuple, wrong source hash and unknown cost. None retries, calls a
provider, or changes a database.

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

QA round one passed after the final helper patch and provenance collector:

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
P5E_PROVENANCE_PROBE=PASS
P5E_FINGERPRINT_FAILURE_REDACTION=PASS
P5E_PRODUCER_TO_VERIFIER=PASS
QA_ROUND_ONE=PASS
```

Adversarial review items were checked against the implementation and source:
expected fingerprint provenance is still explicitly pending; the pipe is
byte-exact only in the repaired path; fingerprint assertion digests are
redacted on success/error/timeout fake captures; exit zero cannot bypass
non-`COMMITTED` acceptance; timeout does not retry; only the allowlisted DB
rows/diffs can be accepted; branch, source commit, artifact pins and
proposal-era HEAD are distinguished; and the future permission must name the
account operation in addition to the RAW/GLOSSARY egress and one-call budget.
A second equivalent review is not required after the targeted corrections; any
new owner input would be a new decision, not a third review loop.

Not done: no actual account fingerprint was computed, no credential was read,
no device or instrumentation run occurred, and the executable collector was
not run against a live device. RAW was not accepted, RECONCILE was not opened,
P5/P5E exit was not claimed, and P6 remains false. F3 is locally GREEN for the
hash-bound host evidence chain; the synthetic producer is regression-only and
does not prove a live installed-package readback.

## Single next action

Owner reviews the final hash-bound packet and supplies trusted expected-
fingerprint provenance plus the exact memory-only account/readback/RAW
permission described in F1. Until that decision is recorded against the final
hashes, do not dispatch A4.3.
