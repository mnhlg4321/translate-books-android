# P5E.9B-A4.3 — One-run RAW authorization proposal

> Current provenance review (2026-09-15, resumed HEAD 9beafef8): F2 transport remains qualified by the prior host evidence. F3 prior fixtures passed, but four new synthetic provenance/timing/identity cases falsely return RAW_ACCEPTED; readback producer provenance and assertion fingerprint redaction also need closure. The next action is the bounded local repair and provenance work in docs/P5E_NEXT_WORK_REQUEST.md. F1 owner input remains pending; owner input alone is not sufficient for dispatch. A4.3 NOT_ISSUED / NOT_READY_FOR_DISPATCH; P5/P5E incomplete; P6_NOT_READY. Prior next-action/completion prose below is historical where it conflicts with this review. Evidence: docs/P5E_PROVENANCE_REVIEW_20260915.md.

Proposal status: OWNER_REVIEW_REQUEST_PREPARED / HOST_FIXTURES_PASS_F3_REPAIR_REQUIRED /
OWNER_DECISION_REQUIRED / NOT_ISSUED / NOT_READY_FOR_DISPATCH

This packet requests owner approval for exactly one L1_RAW_DISCOVERY run. It
does not create a runtime authorization, consume an authorization ID, read the
device credential, call a provider, or open RECONCILE. The mandatory account
fingerprint is intentionally pending the owner-controlled verification
described below, so this packet is not marked READY_FOR_APPROVAL or
READY_FOR_DISPATCH.

The fixed-scope file whose hash is passed to the live harness is
docs/P5E_RAW_AUTHORIZATION_APPROVAL_MANIFEST.md. Its hash is kept separate from
this proposal and from the command; the manifest contains no self-hash. The
single command is in docs/P5E_RAW_AUTHORIZATION_COMMAND.txt.
Pinned approval-manifest SHA-256:
DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501.
Pinned command SHA-256:
1D9A67693C4C4F64AF182300CDEB963A9FD9D44361EF77E91371F88C92EDA15E.
Host supervisor SHA-256:
BEEFBB7733EED660B1F59435922D0594FBA1CBDED01B6C0B00482E786E456799.

## Baseline and evidence

Canonical plan: EDITORIAL_RECOVERY_V4_18.md
Current host-repair branch: feature/v4.18-p5e-audit-20260914
Host-repair HEAD before this evidence group: 0f52d36e516560bb33d294303c70fa1753cb64f9
Host-preparation commit: c2c79a19842f551fc752a53328024aab8ddb529d
HEAD that created this proposal: c1e3ec6eec62e38a1e2f4fdb0d0f151d5efdcfae
AndroidTest source/archive commit: d51b7f3c16bdc482513b9904db07b97daed592d1
A4.2 execution-start HEAD: 005317cd83f107edbf275734cb2977b9929e88ce
A4.2 host-preparation HEAD: 90c40c4959004657d527b9a385449589347e10aa

The implementation/source commit of the pinned test APK, the HEAD that
created this proposal, and the post-documentation commit are distinct facts.
The post-documentation commit is the commit produced by this documentation
group and is reported in the handoff; it is not placed inside the hashed
manifest or used as an input to its own hash.

A4.2 is the checkpoint. The evidence is pinned by filename plus SHA-256:

| Named file | SHA-256 |
|---|---|
| D:\P5E-private\a4-2-exact-preflight-device-20260914-185308591\A4-2-EVIDENCE-MANIFEST.md | 7FBBECD3E2A8868D42D34CB3F9F2F8BA8CDAC236B4C7161F6747CBB19EE2474D |
| D:\P5E-private\a4-2-exact-preflight-device-20260914-185308591\instrumentation-stdout-raw.txt | 1FFE572DF3ADEBA6A8AB55061BD75F2F5EEBBC479F8BB0D1EA97DB3957D1577C |
| D:\P5E-private\a4-2-exact-preflight-device-20260914-185308591\PARSED_INSTRUMENTATION_RESULT.txt | 5DFA2C6A1AD42D233B5D4B05AE394E714CA9283544E6CBD3BA5699641A4C560B |
| D:\P5E-private\a4-2-exact-preflight-device-20260914-185308591\SHA256SUMS.txt | 379C3C063AA026FD60D4E416B9E4FA983A731106F1142AEF72428B71A3526173 |
| D:\P5E-private\a4-2-exact-preflight-host-prep-20260914-184255833\HOST_PREP_MANIFEST.md | C6B1CD88C212EA51DA698DF3AA89335FD0C6B7334E53B2617CD0DCFF45945BD6 |
| D:\P5E-private\a4-2-exact-preflight-host-prep-20260914-184255833\PLANNED_DEVICE_COMMANDS.txt | A4DFC0F83AF40413274E6A889BABDFD9ADD8541FB9133146AD78550B630EBC6C |
| D:\P5E-private\a4-2-exact-preflight-host-prep-20260914-184255833\HOST_QA_RESULT.md | 930B359FCD09B765ACBE5BEC0C05758BC105F891A2EFF6E199304B9FEB44F1AD |

A4.2 result: one exact-preflight test, OK (1 test), terminal -1, parser
accepted, route flags/conjunction true, complete preservation true, provider
calls 0, authorization/attempt/reconciliation creation false, lineage UNUSED,
and zero relevant rows before and after. No RAW predecessor, REPORT_L1,
receipt, runtime authorization or provider result exists.

The bounded host-preparation evidence is
`docs/P5E_RAW_HOST_PREPARATION_20260915.md`, SHA-256
`C54E55446629ADC161882C51333B25BD21633277C6A4B1020D5265E93DA11E3C`.
It records the F2 RED-to-GREEN process-argv/remote-shell proof, the source
required-argument contract, the F3 one-launch supervisor and independent
readback verifier fixtures, the artifact parity checks and the unresolved F1
owner decision. It is evidence for preparation only; it does not change the
manifest hash, authorize dispatch or establish a RAW predecessor.

## Exact identity requested

The following values are copied from the pinned A4.2 raw evidence and are not
retyped from an older template:

| Field | Exact value |
|---|---|
| phase | L1_RAW_DISCOVERY |
| projectRowId | 2 |
| attemptIdentity | 7a5e34287d90055a5f0e7d6bb5c9c459202eadcfc538b9f452d548bea298fd6e |
| requestIdentity | ae328c3d771112ce73e9e9d6cba0bb951f930042fc6851a31a96f15f7b70ee06 |
| requestEnvelopeHash | 5c25e1850c7f70081bd21d67effa2a3a642f410f025ab91cf8044b6ed1bd87f2 |
| canonicalRequestBodySha256 | c5920dd842ea92f21d4045c72306a04d59313ac20190c951749fa1b64457c1c2 |
| routeFingerprint | 23149071716043a2a4dc7fb7af51073b4de838ba072919bb6fd750bc9e62948c |
| selector | p5e-fresh-mercedes-vol5-20260911-01 |
| chapterKey | 001 |
| bindingIdentity | 845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf |
| runDeclarationIdentity | 8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc |
| compatibilityEvaluationId | 3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1 |
| canonicalPackHash | 497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d |
| canonicalProfileHash | beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21 |
| authorizationId | P5E-FRESH-MERCEDES-VOL5-RAW-20260911-01 |
| authorizationIdSha256 | 0aa82c5897e3df3ec8a7a1586736dbf184b316c66ec165e95e64e8e4832145eb |

The ID remains the harness-required ID even though its date is historical. A4.2
global and fresh-lineage counts show no authorization receipt for this hash,
no attempt, no lifecycle, no reconciliation/history and no report/receipt.
Do not change it only to attach a new date, and do not claim it is consumed.

## APK, certificate and database pins

- Device: 15e84958.
- Production package/version/code: com.ml.tblandroidtxt /
  v4.17-p5e.11 / 207.
- Production APK file:
  artifacts\builds\v4.17-p5e.11\build-20260911-201725\TranslateBooks-v4.17-p5e.11-code207.apk
  SHA-256 2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD.
- Production source ZIP file:
  artifacts\builds\v4.17-p5e.11\build-20260911-201725\project_source_build-20260911-201725.zip
  SHA-256 B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348.
- Production certificate SHA-256:
  47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155.
- Test APK file:
  artifacts\test-builds\v4.17-p5e.11\a4-1-test-20260914-065532\app-debug-androidTest.apk
  SHA-256 57EC99A95EE2DC0F1759934C62CEA39E2EC92EB77C3DAF76CFEED28D41A2FDEA,
  1155224 bytes.
- Test source ZIP file:
  artifacts\test-builds\v4.17-p5e.11\a4-1-test-20260914-065532\project_source_a4-1-test-20260914-065532.zip
  SHA-256 382EC5D12FC786BC358316434692E49A73BD62C2F9DD9AC6BD1BC9274CE0B3FA.
- Test certificate SHA-256:
  47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155.
- Test package/target/runner:
  com.ml.tblandroidtxt.test / com.ml.tblandroidtxt /
  androidx.test.runner.AndroidJUnitRunner.

The production APK is retained exactly as code207. The test APK is the A4.2
artifact and is retained exactly. A read-only package check must confirm those
identities; if the test package already matches, no reinstall is allowed.

## Data egress and route permission

The requested egress is limited to the phase projection
RAW_AND_GLOSSARY_VISIBLE_DRAFT_AND_PRONOUN_HIDDEN:

- RAW: 23814 bytes, SHA-256
  a308210eca80557cfa9fec7ed55b2ee3de5c1c4776e59b2b5edbf0efb04504be.
- GLOSSARY: 3249 bytes, SHA-256
  4bc3e2dd05542aa5ca6b7e5fcac43ed53e9af57060eb69c6fa71e9d0a2ea0314.
- DRAFT: 26462 bytes, SHA-256
  64adecd8ceccbb13446ef14c494ca9bb1987117c428c5758e7442270ec7f62b5;
  hidden from the model.
- PRONOUN semantic: 452 bytes, SHA-256
  4947ff9184995be5f850f2323fbe0a04c67302fb8d5afb63cf12202b44720686;
  hidden from the model.

Owner permission requested: send only RAW and GLOSSARY to the pinned route for
one primary L1_RAW_DISCOVERY request. The request body is not copied into this
packet or evidence. Full request-body and full model-response storage remain
false.

Pinned route facts are provider=openrouter,
model=openai/gpt-5.6-luna, upstreamProvider=openai,
routeFingerprint=23149071716043a2a4dc7fb7af51073b4de838ba072919bb6fd750bc9e62948c,
stream=false, response format json_schema, strict=true,
requireParameters=true, allowFallbacks=false, only=[openai],
dataCollection=deny, plugins=ABSENT. Wire schema is
safe4.raw.discovery.wire.v1; worst-case wire/hard maximum is 2785/3584 bytes;
JSON schema is 3670 bytes with SHA-256
4d4077e8be16ea5ba12664bcdcdc8c449eadb315bb94e82f917b98459aa1b99f.

## Budget and expiry

These are source/harness caps, not a promise of price or a billing result:

maximumPrimarySemanticCalls=1
maximumSchemaRepairCalls=0
maximumNetworkRetries=0
maximumInputTokens=100000
maximumOutputTokens=4096
maximumTotalTokens=104096
maximumTotalCostUsd=0.05
maximumExecutionTimeMillis=120000

The code enforces these limits in the selected live method and the existing
engine. The engine checks input/output/total tokens, estimated plus reported
cost and elapsed wall-clock time. The provider creates a monotonic deadline
from 120000 ms; the engine performs a second wall-clock check immediately
before durable commit. Provider usage/cost is not known at proposal time.
Unknown cost makes costAccountingComplete false and stops before acceptance; it
must not be recorded as 0.

The proposed authorization window is 180000 ms. The command computes fresh
values immediately before dispatch:

issuedAtMillis=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
expiresAtMillis=issuedAtMillis+180000

These are not copied from A4.2 or any historical document. A stale or expired
window is a stop condition, with no refresh, reuse or redispatch.

## Legitimate account fingerprint and owner gate

The legitimate source is the target app's runtime settings object:
SettingsStore.load(target).copy(), followed by
AppSettings.normalizeEndpoint(settings.baseUrl) in the selected live method.
The fingerprint is SHA-256 of the UTF-8 bytes of:

endpoint + newline + in-memory settings.apiKey

This is not a hash of the settings file, not a default, and not a fabricated
fingerprint. This audit has not read the device credential and has not
performed the fingerprint operation.

Owner approval has two explicit parts:

1. Approve the fixed pins, RAW/GLOSSARY egress, budgets, one-use identity and
   allowed DB writes in this packet.
2. Separately permit the device-side account check immediately before dispatch:
   load settings in memory, verify the pinned route and non-empty credential,
   normalize the endpoint, compute the fingerprint in memory, compare it to
   the expected 64-hex value, and pass only that fingerprint to the command.

The credential, settings content, endpoint text and intermediate values must
not be printed, pulled, serialized, logged or committed. The expected
fingerprint is supplied to the command through a process-only owner variable;
it is never a credential. If the owner cannot verify it in the permitted
scope, or the account does not match, stop before constructing/consuming
authorization and before provider dispatch.

## Runtime checks and command

Use only:

EditorialP5EFreshRawLiveInstrumentedTest#authorizedFreshRawRunsOnlyWhenExplicitlyOptedIn

The command file contains every required live-method argument: live opt-in,
authorization ID and hash, owner packet hash, attempt/request/envelope/body/
route/account fingerprints, project row, device token, provider/model/upstream,
phase and fresh tuple, production code/APK/certificate/DB pins, all eight
budget fields, fresh issued/expires times, evidence redaction and stop
authority. It does not add a separate instrumentation preflight invocation and
does not merely mutate the A4.2 preflight command.

Inside that live method, preflightOnly is recomputed before authorization
construction; package, DB, settings, source projection, request body,
request/envelope identities and UNUSED lineage are checked. dispatchRaw is the
only live path. The source checks the existing test package out-of-band by
the pinned artifact; no reinstall is allowed when identity still matches.

The host observation deadline is 240000 ms, long enough to observe the
120000-ms provider deadline and durable result. If the host supervisor reaches
that deadline, preserve the observed durable state, treat external call state
as unresolved, and do not retry or redispatch.

The host implementation and its offline proof are in
`scripts/p5e-raw-live-supervisor.ps1`, SHA-256
`BEEFBB7733EED660B1F59435922D0594FBA1CBDED01B6C0B00482E786E456799`.
The repaired command wrapper is
`docs/P5E_RAW_AUTHORIZATION_COMMAND.txt`, SHA-256
`1D9A67693C4C4F64AF182300CDEB963A9FD9D44361EF77E91371F88C92EDA15E`.
Host-preparation evidence is
`docs/P5E_RAW_HOST_PREPARATION_20260915.md`, SHA-256
`C54E55446629ADC161882C51333B25BD21633277C6A4B1020D5265E93DA11E3C`.
The evidence records F2 RED-to-GREEN transport through fake process argv and
the ADB/shell model, source-derived required arguments, F3 numeric outcomes
and independent readback acceptance. The valid fixture is the only
`RAW_ACCEPTED` case and keeps `P6_READY=false`; no live result is claimed.

## Allowed DB effects and acceptance

The exact allowed table effects are:

- One atomic insertion into editorial_p5c_attempts and one consumed
  editorial_p5d_authorization_receipts row when the exact attempt is claimed.
- Redacted, allowlisted lifecycle metadata in
  editorial_p5d_network_lifecycle only after that attempt exists.
- A typed RECOVERY_REQUIRED update to the exact attempt on provider,
  timeout, validation or commit failure.
- A COMMITTED update to the exact attempt with response identity,
  app-owned report_bytes, receipt_bytes and metrics_json only after all
  validation and integrity checks pass.

No binding, source, run, pack/profile, settings or unrelated row may change.
No editorial_p5d_reconciliation or editorial_p5d_reconciliation_history write
is allowed. No delete, cleanup, restore, hash-forcing or DB reset is allowed.
The post-live DB hash need not equal the pre-live hash; integrity, FK,
identity immutability, permitted row set, atomicity and durable result fields
must be checked.

| Outcome | Acceptance decision |
|---|---|
| Pre-dispatch pin/credential/expiry failure | No dispatch; before claim, authorization is not created or consumed. |
| Provider error, timeout or lost connection | No retry; preserve durable state and do not infer whether the request was sent. |
| Response with schema/semantic validation failure | RAW not accepted; no repair and no RECONCILE. |
| Test OK but durable state not COMMITTED | Do not record RAW PASS. |
| COMMITTED with valid stored data and metrics | RAW predecessor may be accepted under the contract. |
| Missing post-check or inconsistency | Acceptance not proven; no cleanup or redispatch. |
| RAW accepted | Stop at RAW; evaluate the next step separately and keep P6 closed. |

## Pre-dispatch QA and owner decision

Before any owner-approved dispatch, verify:

- This proposal and command contain no credential, raw source, raw request,
  raw response or raw prompt.
- Every evidence pin names its file and its SHA-256.
- The production code207 APK/certificate and the A4.2 test APK SHA are exact.
- The fixed identity table matches the A4.2 raw evidence.
- The command names only the authorized live method and has no preflight-only
  invocation.
- All hashes are 64 hex characters; token/byte/millisecond units are not
  conflated; issued/expires are fresh and expires is greater than issued.
- The host self-test and both transport layers pass offline; success, nonzero,
  timeout and pre-launch failures have numeric outcomes and no redispatch.
- The independent verifier rejects missing post-readback, non-`COMMITTED`
  state, missing receipt, unknown cost, duplicate attempt, orphan lifecycle
  and unrelated writes.
- The account check succeeds in the permitted scope and the lineage remains
  UNUSED immediately before authorization construction.
- No runtime authorization or provider call occurs until the owner approves.

Owner decision requested: after reviewing the final hashes, approve the fixed
scope and the separate memory-only account verification, subject to the
fail-closed conditions above. The host preparation does not itself grant this
permission.

Owner decision: PENDING / HOST_FIXTURES_PASS_F3_REPAIR_REQUIRED
endpointAccountFingerprint: PENDING_OWNER_VERIFICATION
Runtime authorization: NOT CREATED
Provider dispatch: NOT PERFORMED
