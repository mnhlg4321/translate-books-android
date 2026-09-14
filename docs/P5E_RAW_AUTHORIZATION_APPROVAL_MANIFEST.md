# P5E.9B-A4.3 — RAW authorization approval manifest

Document status: FIXED_SCOPE_PACKET / OWNER_DECISION_REQUIRED / NOT_ISSUED / NOT_READY_FOR_DISPATCH

This is the fixed-scope packet for one owner decision. It is not a runtime
authorization and it does not authorize a provider call by itself. The file
intentionally does not contain its own SHA-256. After this file is final, its
SHA-256 is computed once and passed as
p5e_owner_approval_manifest_sha256 in the separate command file. If this file
changes, the command hash must be recomputed before any use.

## Provenance and boundary

- Canonical plan: EDITORIAL_RECOVERY_V4_18.md.
- Branch: fix/v4.18-p5e-9b-a4-1.
- HEAD when this proposal was prepared: c1e3ec6eec62e38a1e2f4fdb0d0f151d5efdcfae.
- AndroidTest source/archive commit for the pinned APK:
  d51b7f3c16bdc482513b9904db07b97daed592d1.
- A4.2 execution-start HEAD:
  005317cd83f107edbf275734cb2977b9929e88ce.
- A4.2 host-preparation HEAD:
  90c40c4959004657d527b9a385449589347e10aa.
- The post-documentation commit is reported by the handoff and is not
  embedded in this file as a self-referential identity.
- No new branch or release checklist is created. No APK is rebuilt.
- No instrumentation preflight, model remediation, credential read, runtime
  authorization construction, provider call or RECONCILE operation is performed
  by this documentation task.

The A4.2 evidence pins are recorded as filename plus SHA-256, not as an
unqualified reference:

- D:\P5E-private\a4-2-exact-preflight-device-20260914-185308591\A4-2-EVIDENCE-MANIFEST.md
  SHA-256 7FBBECD3E2A8868D42D34CB3F9F2F8BA8CDAC236B4C7161F6747CBB19EE2474D.
- D:\P5E-private\a4-2-exact-preflight-device-20260914-185308591\instrumentation-stdout-raw.txt
  SHA-256 1FFE572DF3ADEBA6A8AB55061BD75F2F5EEBBC479F8BB0D1EA97DB3957D1577C.
- D:\P5E-private\a4-2-exact-preflight-device-20260914-185308591\PARSED_INSTRUMENTATION_RESULT.txt
  SHA-256 5DFA2C6A1AD42D233B5D4B05AE394E714CA9283544E6CBD3BA5699641A4C560B.
- D:\P5E-private\a4-2-exact-preflight-device-20260914-185308591\SHA256SUMS.txt
  SHA-256 379C3C063AA026FD60D4E416B9E4FA983A731106F1142AEF72428B71A3526173.
- D:\P5E-private\a4-2-exact-preflight-host-prep-20260914-184255833\HOST_PREP_MANIFEST.md
  SHA-256 C6B1CD88C212EA51DA698DF3AA89335FD0C6B7334E53B2617CD0DCFF45945BD6.
- D:\P5E-private\a4-2-exact-preflight-host-prep-20260914-184255833\PLANNED_DEVICE_COMMANDS.txt
  SHA-256 A4DFC0F83AF40413274E6A889BABDFD9ADD8541FB9133146AD78550B630EBC6C.
- D:\P5E-private\a4-2-exact-preflight-host-prep-20260914-184255833\HOST_QA_RESULT.md
  SHA-256 930B359FCD09B765ACBE5BEC0C05758BC105F891A2EFF6E199304B9FEB44F1AD.

The last two preparation hashes above are retained as the preparation-event
pins. They do not authorize a new device operation.

## Fixed authorization identity

phase=L1_RAW_DISCOVERY
projectRowId=2
attemptIdentity=7a5e34287d90055a5f0e7d6bb5c9c459202eadcfc538b9f452d548bea298fd6e
requestIdentity=ae328c3d771112ce73e9e9d6cba0bb951f930042fc6851a31a96f15f7b70ee06
requestEnvelopeHash=5c25e1850c7f70081bd21d67effa2a3a642f410f025ab91cf8044b6ed1bd87f2
canonicalRequestBodySha256=c5920dd842ea92f21d4045c72306a04d59313ac20190c951749fa1b64457c1c2
routeFingerprint=23149071716043a2a4dc7fb7af51073b4de838ba072919bb6fd750bc9e62948c

selector=p5e-fresh-mercedes-vol5-20260911-01
chapterKey=001
bindingIdentity=845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf
runDeclarationIdentity=8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc
compatibilityEvaluationId=3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1
canonicalPackHash=497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d
canonicalProfileHash=beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21

authorizationId=P5E-FRESH-MERCEDES-VOL5-RAW-20260911-01
authorizationIdSha256=0aa82c5897e3df3ec8a7a1586736dbf184b316c66ec165e95e64e8e4832145eb
authorizationIdState=HARNESS_ID_RETAINED_AND_UNUSED_PER_A4_2; DO_NOT_ROTATE_FOR_DATE

The authorization ID is the existing ID required by the live harness. A4.2
readback recorded zero authorization receipts globally and for the fresh
lineage, zero attempts, zero reconciliation rows, zero reconciliation history,
zero lifecycle rows and zero report/receipt rows. Therefore this ID hash has no
consumed row in the pinned baseline. This task does not create or consume it.

## Frozen artifact and device pins

- Device serial: 15e84958.
- Production package: com.ml.tblandroidtxt.
- Production version/code: v4.17-p5e.11 / 207.
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
  size 1155224 bytes.
- Test source ZIP file:
  artifacts\test-builds\v4.17-p5e.11\a4-1-test-20260914-065532\project_source_a4-1-test-20260914-065532.zip
  SHA-256 382EC5D12FC786BC358316434692E49A73BD62C2F9DD9AC6BD1BC9274CE0B3FA.
- Test certificate SHA-256:
  47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155.
- Test package/target/runner:
  com.ml.tblandroidtxt.test / com.ml.tblandroidtxt /
  androidx.test.runner.AndroidJUnitRunner.
- Production and test certificate identity is pinned by the named SHA-256
  values above. The production APK is code207 and is not rebuilt or replaced.
  If the test package already matches the named APK, no reinstall is allowed.

## Source projection and route

- Egress permission requested: RAW and GLOSSARY may be sent to the pinned
  route for this one L1_RAW_DISCOVERY call.
- DRAFT and PRONOUN are hidden from the model in the RAW projection.
- RAW source: 23814 bytes, SHA-256
  a308210eca80557cfa9fec7ed55b2ee3de5c1c4776e59b2b5edbf0efb04504be.
- GLOSSARY source: 3249 bytes, SHA-256
  4bc3e2dd05542aa5ca6b7e5fcac43ed53e9af57060eb69c6fa71e9d0a2ea0314.
- DRAFT hidden source: 26462 bytes, SHA-256
  64adecd8ceccbb13446ef14c494ca9bb1987117c428c5758e7442270ec7f62b5.
- PRONOUN semantic hidden source: 452 bytes, SHA-256
  4947ff9184995be5f850f2323fbe0a04c67302fb8d5afb63cf12202b44720686.
- Wire schema: safe4.raw.discovery.wire.v1.
- JSON schema bytes/hash: 3670 /
  4d4077e8be16ea5ba12664bcdcdc8c449eadb315bb94e82f917b98459aa1b99f.
- Worst-case wire bytes/hard maximum: 2785 / 3584.
- Route provider/model/upstream: openrouter /
  openai/gpt-5.6-luna / openai.
- stream=false, response format=json_schema, strict=true.
- requireParameters=true, allowFallbacks=false, only=[openai],
  dataCollection=deny, plugins=ABSENT.
- Full request-body and full model-response storage: false / false.

## Budget and time contract

The following are harness-enforced caps, not a quote or billing result:

maximumPrimarySemanticCalls=1
maximumSchemaRepairCalls=0
maximumNetworkRetries=0
maximumInputTokens=100000
maximumOutputTokens=4096
maximumTotalTokens=104096
maximumTotalCostUsd=0.05
maximumExecutionTimeMillis=120000

The cost cap is a requested maximum. No provider usage or actual cost is
known at proposal time, and missing usage/cost must not be recorded as zero.
The engine marks cost accounting incomplete when the provider does not return
known cost, and stops before acceptance. The engine rejects input, output,
total-token, estimated/reported-cost or wall-clock overages. The provider
uses a monotonic deadline derived from maximumExecutionTimeMillis, and the
engine repeats a wall-clock guard before durable commit.

The proposed authorization validity window is 180000 milliseconds to cover
the local pre-dispatch checks plus the 120000-millisecond execution cap. It is
not a cost or execution budget. At owner-approved dispatch time only:

issuedAtMillis=DateTimeOffset.Now.ToUnixTimeMilliseconds()
expiresAtMillis=issuedAtMillis+180000

The separate command computes these values immediately before the single
instrumentation invocation. No old timestamp is copied. If the window expires
or the command is not dispatched in that window, stop and obtain a new owner
decision; do not refresh, redispatch or reuse the consumed ID automatically.

## Account verification: owner-controlled second approval part

The valid source for endpointAccountFingerprint is the app's runtime
AppSettings object, loaded by the selected live method as
SettingsStore.load(target).copy(). It is not a settings-file hash and it is
not a default value.

The exact permitted operation, to be performed only after owner approval and
before dispatch, is:

1. In the target app process, load AppSettings from SettingsStore into memory.
2. Require the pinned route to match and require a non-empty API credential.
3. Normalize the endpoint with AppSettings.normalizeEndpoint(settings.baseUrl).
4. Compute SHA-256 over the UTF-8 bytes of endpoint + newline + the in-memory
   credential: endpoint + "\n" + settings.apiKey.
5. Compare that 64-hex result to the owner-supplied expected fingerprint.
6. Keep the credential and intermediate value in memory only; do not print,
   pull, serialize, log or commit the credential, settings content or raw
   endpoint. Only the permitted fingerprint may be passed as an argument.

The selected live method performs this same comparison at its first permitted
credential-read point, before it constructs the runtime authorization. This
audit has not performed the operation. The expected fingerprint is therefore
explicitly pending owner-permitted verification and is not a fake/default
value:

expectedEndpointAccountFingerprint=OWNER_PROCESS_VALUE_AFTER_DEVICE_VERIFICATION

If the account does not match, the credential is unavailable, the route is not
the pinned route, or the fingerprint cannot be verified in the permitted
scope, stop before constructing or consuming authorization and before any
provider dispatch. The current packet is NOT_READY_FOR_DISPATCH because this
mandatory value is not available yet.

## DB write boundary

Before dispatch, the selected live method performs read-only package, settings,
database, exact request, fresh-tuple and UNUSED-lineage checks. No authorization
object is constructed until those checks and the account comparison pass.

Only the following writes are in scope if the owner later approves the exact
packet and the live path reaches them:

| Point | Table/row | Permitted fields or effect |
|---|---|---|
| Atomic claim | editorial_p5c_attempts, one row for attemptIdentity | Exact attempt/request/binding/run/chapter/phase/predecessor/envelope/provider/model identities; status CLAIMED; empty response identity; null report/receipt; empty metrics/recovery fields; created/updated timestamps |
| Atomic claim | editorial_p5d_authorization_receipts, one row for authorizationIdSha256 | Exact phase, attempt/request/binding/run/chapter/provider/model/account fingerprint, issued/expires and all eight budget fields; consumed timestamp/result and consumed attempt identity |
| Transport lifecycle | editorial_p5d_network_lifecycle, one row for the exact attempt | Allowlisted redacted stage, request/response byte counters, HTTP status, bounded exception class, elapsed time, generation/provider-response IDs, content type and cancellation source |
| Recovery outcome | the exact editorial_p5c_attempts row | Status RECOVERY_REQUIRED and a typed recovery reason when provider/timeout/validation/commit fails; no deletion or reset |
| Accepted result | the exact editorial_p5c_attempts row | Status COMMITTED, redacted response identity, app-owned report_bytes, receipt_bytes, metrics_json and updated timestamp only after all contract checks pass |

The attempt and authorization receipt must be created in the existing atomic
claim transaction. A lifecycle row is allowed only after the durable attempt
exists. Report/receipt bytes are app-owned validated artifacts; raw source,
prompt, request body and model response are not stored.

The following are forbidden: any write to bindings, binding inputs, run
declarations, packs, profiles, settings or unrelated data; any
editorial_p5d_reconciliation or editorial_p5d_reconciliation_history write;
deletes, cleanup, database restore, hash-forcing, or redispatch. The baseline
has zero relevant rows, so an unexpected pre-existing row or stale claim is a
stop condition rather than a cleanup target.

## Owner outcome matrix

| Outcome | Required conclusion and action |
|---|---|
| Pre-dispatch pin, credential or expiry check fails | Do not dispatch. Before claim, authorization has not been created or consumed; record that conclusion only from evidence. |
| Provider error, timeout or lost connection | Do not retry. Keep durable attempt/lifecycle state and do not infer whether the request reached the provider. |
| Response exists but schema or semantic validation fails | RAW is not accepted. Do not repair and do not RECONCILE. |
| Test reports OK but durable status is not COMMITTED | Do not record RAW PASS. |
| COMMITTED and stored data/metrics are valid | The RAW predecessor may be accepted according to the contract. |
| Post-check is missing or data is inconsistent | Acceptance is not proven. Do not clean up, restore, or redispatch. |
| RAW accepted | Stop at RAW. Evaluate the next step separately; P6 does not open automatically. |

The post-live database hash is an integrity/readback fact, not an assertion
that it must equal the pre-live hash. Integrity, foreign keys, identity
immutability, permitted row set, atomicity and durable result fields must be
checked instead.

## Owner decision requested

The owner is asked to approve both parts together:

- Fixed pins and one-use permission to send only the RAW/GLOSSARY projection
  for the exact phase, project, chapter, selector, binding, run, evaluation,
  pack/profile, route and artifact identities above.
- One owner-controlled, memory-only account verification using the exact
  fingerprint operation above, followed by at most one dispatch of the
  selected live method.
- Creation of only the attempt, authorization receipt, allowlisted lifecycle
  and committed RAW result records listed above.
- No schema repair, network retry, fallback, response healing, RECONCILE,
  cleanup, restore or automatic redispatch.

Owner decision: PENDING
Runtime authorization: NOT CREATED
Provider dispatch: NOT PERFORMED
