# Editorial Recovery v4.18

> Current work package — 2026-09-28: `OFFLINE_REPAIR_BEHAVIOR_PASS / LIVE_PACKET_NOT_READY / A43_OUTER_PROCESS_TREE_AND_AUTHORITY_GATE_CLOSURE_REQUIRED / 0_BLOCKER / 1_HIGH / 2_MEDIUM / 2_LOW / CLOSED_CONSUMED_NON_REUSABLE / NOT_AUTHORIZED / NOT_DISPATCHED / P6_NOT_READY`. PM-path/capture behavior remains offline PASS (`36/36`, binding `262/262`, regression `175/175`, DB `56/56`), but independent final Luna review found a live-boundary HIGH: Windows PowerShell 5.1 lacks `Process.Kill(Boolean)`, so the current fallback proves only parent termination, not descendant termination. Review: `docs/P5E_A43_PM_PATH_CAPTURE_REPAIR_FINAL_LUNA_REVIEW_20260928.md`. Next work request/provenance: `docs/P5E_A43_PREAUTH_RUNTIME_GUARD_CLOSURE_NEXT_WORK_REQUEST_20260928.md` and `docs/P5E_A43_PREAUTH_RUNTIME_GUARD_CLOSURE_PROVENANCE_20260928.json`; work-request QA `29/29 PASS`, Luna plan review `0 BLOCKER / 0 HIGH / 0 MEDIUM`.

> Repaired packet pins: manifest `C08C3F6D8EE1B802B0D68AB1FF0302E82655D17C72B06619752B65396C11ADC3`, command `C641F6A01C01209AD08FB820871E575895DC998A4CB6AD15769F1D2222BA4CD5`, helper `959F2BBDC2EF163F00F3A56900B903DF529FDCDD9024AD6CEE906A01E080A8F0`, exporter `D8783B31F9141458CA397915664CA79B07D3161A5E0F0D3B4365C5C65EA41D06`, bridge `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111`, certificate `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`, serial `15e84958`.

> The offline repair parses PM output in memory into a fail-closed safe enum and separates helper process exit from stdout/stderr drain status, timeout and bounded capture. No raw output, package-private path, argv, provider payload, credential or expected digest was retained. Final audit supersedes the earlier conditional review for live readiness: `1 HIGH` process-tree containment, `2 MEDIUM` durable dependency/authority gates and `2 LOW` capture-cap/severity metadata findings must remain visible.

> Closed historical A4.3 event outcome — 2026-09-26: `OWNER_DECISION_RECEIVED_AND_CONSUMED / A4_3_PRE_DISPATCH_COLLECTOR_STOP / COLLECTOR_TYPED_STOP / P5E_COLLECTOR_BINDING_TUPLE_MISMATCH / RAW_NOT_DISPATCHED / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`. The new event ran exactly once; no provider, credential, DB-write, RAW or redispatch action occurred. Result: `docs/P5E_A43_EVENT_RESULT_20260926.json`. This does not authorize the current offline repair or a new event.

> Historical consumed-packet audit — 2026-09-26: the approved packet was verified and invoked once. Manifest `669C54049920C49344D2FB55533EFA9FA9F87E933A6A18DE5FA7215F1146D147`, helper `8A0509743403B28F7C074BE37DD7D08D41A1B9C0E56AC70D8A43024803CDF434`, exporter `D8783B31F9141458CA397915664CA79B07D3161A5E0F0D3B4365C5C65EA41D06`, bridge `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111` and command `A2EF2BA90F07D3F4D2517E7F1541EA752615F6BCF61E304E579E301A8D5D3D08` matched. The old manifest/command, decision and 2026-09-25 event remain consumed and are not fallback.

> Current DB readback prerequisite — 2026-09-26: `OFFLINE_DB_HOST_READBACK_REPAIR_PASS` remains a prerequisite and is bound by `docs/P5E_DB_HOST_READBACK_REPAIR_QA_20260925.json` at `56/56 PASS`. The binding-tuple repair consumed only a synthetic copy of the captured export; the closed event and its evidence remain immutable and were not reused. A4.3, RAW acceptance, P5 exit and P6 remain not ready.

> The consumed live packet remains closed and non-reusable. Redacted evidence proves only that `pm path com.ml.tblandroidtxt` returned nonzero; it does not prove package absence, device/USB failure or ADB authorization failure. Wrapper code `125` was a secondary output-drain status and did not replace the collector typed stop; the repaired offline wrapper preserves both statuses.

> Current Next action: execute only the offline `A43_OUTER_PROCESS_TREE_AND_AUTHORITY_GATE_CLOSURE` work request. Add verified tree termination, machine-enforced owner/expected-value pre-event gates, bounded capture and durable exact dependency provenance; do not request authorization, execute the current command, open an event, use ADB or open P6.

> Superseded DB diagnosis — 2026-09-25: the two exit-`1` presence probes were valid `ABSENT` results for WAL/SHM and did not stop the collector. The actual blocker was `database-consistent-read-transaction`, which launched once and exited `1`; its native reason is unavailable in the redacted evidence. The bounded offline work package named in this diagnosis is complete; A4.3/RAW/P5 exit/P6 remain closed.

> Current version-contract repair: collector Android versionName expected is corrected to 4.17-p5e.11, independently confirmed by immutable BUILD_INFO and offline inspection of the hash-matched pulled production APK. Release label v4.17-p5e.11 remains a label, not Android versionName. Companion QA is `PASS` (collector version 5/5); see docs/P5E_PACKAGE_VERSION_NEXT_WORK_REQUEST_20260925.md and companion provenance. The new event below is terminal and no retry occurred.

> Historical toolchain owner handoff: docs/P5E_PACKAGE_VERSION_NEXT_WORK_REQUEST_20260925.md bound the version-corrected working-tree packet. The owner decision was received and consumed by one new event; no new account provenance is needed and prior events remain closed.

> Offline launch audit 2026-09-25: the bounded RAW toolchain repair is complete. One explicit SDK/build-tools 35.0.0 contract now resolves absolute ADB and Java+apksigner.jar paths before event creation, and the same paths are bound through Before/Dispatch/After. Targeted Windows PowerShell 5.1 QA passed after closing timeout/capture, legacy-plan dispatch, and expected-value boundary findings; no ADB, signer, device, provider, DB or credential action was performed. The historical native cause remains unproven and the closed event is not reopened.

> Historical owner decision/result — 2026-09-25: the owner authorized exactly one new A4.3 event on serial `15e84958` under the version-corrected hash-bound packet. Event `raw-live-20260925-093707011-cc71e9e18029485c8e2411698c88f586` ran once and stopped before dispatch; the decision is consumed and no retry or redispatch is permitted.

> A4.3 live outcome — 2026-09-25: `A4_3_PRE_DISPATCH_COLLECTOR_STOP / COLLECTOR_TYPED_STOP / RAW_NOT_DISPATCHED / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`. The Before collector stopped with typed detail `P5E_COLLECTOR_ADB_NONZERO`; no provider or credential read, live method dispatch, or device mutation occurred.

> Current offline identity repair — 2026-09-24: `ACCOUNT_IDENTITY_LIFECYCLE_REPAIRED_OFFLINE` remains PASS. The repaired account-only runner now has a terminal typed `MATCH`; no further offline parser work is authorized without a new failing case.

> Current event disposition — 2026-09-25: event `D:\P5E-private\raw-live-20260925-093707011-cc71e9e18029485c8e2411698c88f586` is `CLOSED_A43_PRE_DISPATCH_COLLECTOR_STOP / COLLECTOR_TYPED_STOP`. Fourteen read-only Before commands each launched once with bounded capture; 11 exited `0` and 3 exited `1` (`database-wal-presence`, `database-shm-presence`, `database-consistent-read-transaction`). `readOnlyCommandCount=14`, provider calls `0`, credential reads `0`, device mutations `0`, redispatches `0`; no After collector or live method dispatch was reached. Preserve this event and do not retry, reuse or rename it. Earlier events remain immutable history.

> Superseded P5E state before DB repair: `HOST_SIGNATURE_LAYOUT_ADAPTER_OFFLINE_PASS / PREFLIGHT_PASS_7_READ_ONLY_CALLS / ACCOUNT_CHECK_COMPLETED_MATCH / ACCOUNT_RUNNER_COMPLETED_MATCH / A43_OFFLINE_PACKET_TECHNICAL_GATES_PASS / PACKAGE_VERSION_REPAIR_OFFLINE_PASS / A43_OWNER_DECISION_RECEIVED_AND_CONSUMED / A43_PRE_DISPATCH_COLLECTOR_STOP / COLLECTOR_TYPED_STOP / P5E_COLLECTOR_ADB_NONZERO / RAW_NOT_DISPATCHED / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`. The event did not establish a RAW predecessor or acceptance.

> Live evidence: event `D:\P5E-private\raw-live-20260925-093707011-cc71e9e18029485c8e2411698c88f586`; `EVENT_PLAN.json` SHA-256 `4F43A09D4717827708ABF8932975CDF168C6014A3E57FDF1BC338110D0EFCB95`; `COLLECTOR_OUTCOME.json` SHA-256 `0DF2BA4435DAC7AAB38D7493E64FD1707F8C44B8A7C9BE1ED4ADA1A9DEFC35C1`; `COLLECTOR_COMMAND_LOG.jsonl` SHA-256 `E0FBF7EB667586D97D7CFD314A090AF914D92848A946DB0E4D6685EA1BC5BE40`. Pulled production/test APK hashes matched the pinned `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD` / `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8`; the two presence results mean WAL/SHM were absent, while the redacted receipt does not identify the native reason for the consistent-read exit `1`.

> The approved owner decision is consumed by this terminal one-event STOP. The version-contract repair remains closed offline; do not retry or reuse this event, and do not open P6. Any future live event would require a new owner decision after an offline diagnosis of the typed ADB nonzero gate.

> Offline repair result — 2026-09-25: `OFFLINE_TOOLCHAIN_REPAIR_PASS / PACKAGE_VERSION_REPAIR_PASS / A43_OWNER_DECISION_CONSUMED / A43_PRE_DISPATCH_COLLECTOR_STOP / P5E_COLLECTOR_ADB_NONZERO / RAW_NOT_DISPATCHED / P6_NOT_READY`. Current packet provenance is `docs/P5E_PACKAGE_VERSION_REPAIR_PROVENANCE_20260925.json`; targeted QA is `docs/P5E_PACKAGE_VERSION_TOOLCHAIN_QA_20260925.json`. The consumed decision and all terminal events are not reusable.

> Superseded Next action: implement and QA the offline DB binary-export plus host-readback contract; completed by the hash-bound PASS packet above. Do not touch the device or open a live event.

> The prior route event `MISMATCH` remains historical and unchanged in `docs/P5E_ACCOUNT_IDENTITY_EVENT_RESULT_20260924.json`; the older signature-layout-adapter `NOT_PROVEN` receipt remains unchanged in `docs/P5E_ACCOUNT_EVENT_RESULT_20260924.json`.


> Historical pre-event P5E execution state 2026-09-23 at baseline `7a64b71acb1dafa32cdfe3d99c13e353589b19ab` is retained below for provenance; it is superseded by the current event disposition above.

Historical pre-event proposal gate: AUTHORIZED_LOCAL_WORK_BY_CANONICAL_P5E_SCOPE / F2_TRANSPORT_QUALIFIED / F3_LOCAL_BEHAVIORAL_GATE_GREEN / OWNER_ACCOUNT_CHECK_SCOPE_RECEIVED / ACCOUNT_TEST_INSTALLER_QUALIFIED_OFFLINE / ACCOUNT_TEST_CHECKONLY_PASS / TEST_PACKAGE_REPLACEMENT_PASS / HOST_RUNNER_REPAIR_OFFLINE_PASS / EXPECTED_PROVENANCE_ACCEPTED_FOR_REVIEW / EXPECTED_VALUE_LOADER_OFFLINE_PASS / EXPECTED_VALUE_PROCESS_SHAPE_PASS_AT_EVENT_START / ACCOUNT_CHECK_PREFLIGHT_DEVICE_STOP / ACCOUNT_CHECK_NOT_EXECUTED / F1_EXPECTED_ENDPOINT_ACCOUNT_FINGERPRINT_PENDING / A4_3_NOT_ISSUED / LIVE_ACTIONS_NOT_AUTHORIZED / RAW_NOT_RUN / NO_RUNTIME_AUTHORIZATION_CREATED / NO_LIVE_CALL_PERFORMED / P6_NOT_READY.
Status: `ACTIVE / P0_P4_COMPLETE / P5_P5E_INCOMPLETE / A2_FAIL_CLOSED / P5E_9B_A3_1_TECHNICAL_PASS / P5E_9B_A3_1R_DOCUMENTATION_AND_EVIDENCE_PASS / P5E_WORKFLOW_PRETAG_FAIL_CLOSED / P5E_9B_A3_2_MODEL_MISMATCH_OBSERVED_HISTORICAL / P5E_9B_A3_2_PRESERVATION_NOT_PROVEN_HISTORICAL / P5E_9B_A4_MODEL_REMEDIATION_PASS_HISTORICAL / P5E_9B_A4_EXACT_PREFLIGHT_EVIDENCE_CHANNEL_FAILED_HISTORICAL / P5E_9B_A4_1_HOST_CONTRACT_PASS / P5E_9B_A4_1_TEST_ARTIFACT_BUILD_HISTORICAL / P5E_9B_A4_2_EXACT_PREFLIGHT_PASS / P5E_9B_A4_3_HOST_FIXTURES_PASS_HISTORICAL / F2_TRANSPORT_QUALIFIED / F3_LOCAL_BEHAVIORAL_GATE_GREEN / HOST_RUNNER_REPAIR_OFFLINE_PASS / EXPECTED_PROVENANCE_ACCEPTED_FOR_REVIEW / EXPECTED_VALUE_LOADER_OFFLINE_PASS / EXPECTED_VALUE_PROCESS_SHAPE_PASS_AT_EVENT_START / ACCOUNT_CHECK_PREFLIGHT_DEVICE_STOP / F1_EXPECTED_ENDPOINT_ACCOUNT_FINGERPRINT_PENDING / A4_3_NOT_ISSUED / RAW_NOT_RUN / NO_RUNTIME_AUTHORIZATION_CREATED / NO_LIVE_CALL_PERFORMED / PROVIDER_CALLS_ZERO / RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED / EXECUTION_DISABLED / NOT_CERTIFIED / P6_NOT_READY`

This document is the single product and execution authority for the v4.18 Editorial recovery. It supersedes active next actions from the historical v4.16 Editorial/RSC/IPC tracks without deleting or reinterpreting their evidence.

## Historical pre-event boundary — P5E local behavioral gate green; expected Process value pending at that checkpoint

The final local repair ran the real `Get-P5EConsistentDatabaseReadback` SQL through a read-only SQLite bridge over six disposable DDL fixtures. Schema `24`, LINEAGE `17`, INPUT `7`, nullable CLAIMED/RECOVERY rows, exact COMMITTED golden report/receipt bytes, lineage/reconciliation source mapping and fail-closed parser mutations all have concrete results. The targeted production serializer JVM test ran on JBR `21.0.10`; its bytes passed the host validator and the required identity/byte mutation matrix was rejected. Helper self-test, SQL boundary probe and supervisor failure/timeout checks pass. See `docs/P5E_SQL_BEHAVIORAL_RESULT_20260916.json`.

This closes only the local behavioral gate. Owner provenance metadata is now
accepted for review, and the owner has reported loading the expected value;
the agent has not read it and the current host `Process` lifetime is unknown.
This does not create runtime authorization, issue A4.3, run RAW, establish a
RAW predecessor or open P6. The exact-serial `CheckOnly` and one approved
test-package replacement are complete with no production operation; the
account check had not launched at that checkpoint. A separately bounded
follow-on is now prepared; readback/RAW decisions remain separate.

The owner has now approved only one memory-only account check with output
`MATCH`/`MISMATCH` and no key/fingerprint/endpoint logging, provider call, DB
write or RAW dispatch. The pinned live method is not a safe account-only
entry point: it continues from comparison into DB/preflight and `dispatchRaw`.
The separately qualified test-only account method and host runner are now built
in replacement event `p5e-account-check-20260916-01`. The exact-serial
`CheckOnly` passed and one replacement of `com.ml.tblandroidtxt.test` passed;
the installed APK was pulled back with the exact requested hash and certificate.
No production package operation, provider call, DB write or RAW dispatch
occurred. The account check was not launched in that checkpoint; no
`MATCH`/`MISMATCH` is claimed by the host repair.

The post-STOP host-repair work package is now complete locally, including the
source-derived signature-layout adapter. The bounded preflight is
`scripts/p5e-account-check-device-preflight.ps1` SHA-256
`09A33DC74820A1AA18B3EE47AA96862DC0AFA03067D5B46B433B74E4EB1AFA22`; its
Windows PowerShell 5.1 synthetic QA is `189/189`, recorded in
`docs/P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_QA_20260924_LAYOUT.json` (SHA-256
`86A13ADBB27AABAC3D409DCBE65B47FDCDB8DF00CCD10DE992DB4B14ECC67E50`). It
keeps legacy parsing, adds the source-derived AOSP wrapper with separate
current/past diagnostics, rejects malformed/conflicting forms, and retains
both full pulled-APK identity gates. The one-attempt command is
`scripts/p5e-account-check-device-command.ps1` SHA-256
`B79A6C7AD05DE30249DEBAA6C3E1E4FFBD0326754CFD3F14AFC0B69E265F1298`; its
synthetic chain QA is `28/28`, recorded in
`docs/P5E_ACCOUNT_CHECK_DEVICE_COMMAND_QA_20260924_LAYOUT.json` (SHA-256
`EB8A9BBA81F1F08419810B52C9BFBDACE4AE2669E0C52C1AA5B963A100597895`). The
two source guards also pass `12/12` against sandbox junction fixtures. This
proves host orchestration and qualified AOSP-derived compatibility only; it
does not prove the device layout, produce `MATCH`/`MISMATCH`, or open A4.3,
RAW, P5 exit or P6.

The owner screenshot exposed a loader contract defect: the interactive loader
prompted for `EnvironmentName` despite its intended canonical default, so its
PASS signal did not populate the variable consumed by the command. The loader
was repaired without reading a real value. Its new SHA-256 is
`1D6C1DEA14E70001F81DB841668969A51DB15417436E742BA018837BFA587A9D` and its
new synthetic QA is `27/27`, recorded in
`docs/P5E_EXPECTED_VALUE_LOADER_QA_20260924.json` (SHA-256
`933A7780700F3562106992BFF3F801B841AB09227C4A2664D192685541914E21`). The
old owner event is terminal; the current adapter packet names a separate new
event directory that remains unused and uncreated.

The owner subsequently ran the approved single-use command. It passed the
canonical Process presence/shape gate and the source-derived AOSP wrapper
metadata path for both packages, completed seven read-only ADB calls and
passed the full pulled-APK identity gates. Exactly one account runner process
and one instrumentation attempt then ran. The process exited zero with
terminal lifecycle success, one result token, bounded capture and redaction
pass, but strict class/method identity counts were invalid:
`ACCOUNT_CHECK_CLASS_IDENTITY_COUNT_INVALID` and
`ACCOUNT_CHECK_METHOD_IDENTITY_COUNT_INVALID`. The typed outcome is therefore
`ACCOUNT_CHECK_NOT_PROVEN_STOP`, not `MATCH` or `MISMATCH`; no provider call,
DB write, RAW dispatch, install attempt or new credential read occurred. The
event is closed and its hashes are recorded in
`docs/P5E_ACCOUNT_EVENT_RESULT_20260924.json`. No automatic retry is
authorized. The prior two-call `UNSUPPORTED_LAYOUT` event remains historical
and its receipts are unchanged.

QA freeze is complete on these final hashes: round 1 executed the source query,
SQLite fixture collector/parser, production golden serializer bridge and full
mutation/timeout matrix; round 2 read the result independently and rejected
wrong identity/event, partial artifact, recovery/unknown and redispatch cases.
Both rounds pass with `failures=[]`, action counts `0` and `P6_READY=false`; no
third review was opened for unchanged input.

## Historical A4.2 and 2026-09-15 local closure record (readiness superseded)

A4.1 was host-only. A4.2 then used the separately approved single-use device
boundary from execution-start HEAD
`005317cd83f107edbf275734cb2977b9929e88ce` on branch
`fix/v4.18-p5e-9b-a4-1`; the AndroidTest source/archive commit remains
`d51b7f3c16bdc482513b9904db07b97daed592d1`. The test package was replaced
exactly once and the exact preflight method ran exactly once. Production was
not installed or modified; no provider/API call, authorization, attempt,
reconciliation, settings write or database write occurred.

The current host-only continuation is on branch
`feature/v4.18-p5e-audit-20260914`. The earlier F2/F3 repair started from
`0f52d36e516560bb33d294303c70fa1753cb64f9`; this provenance repair resumed at
actual HEAD `1d0dfe8854eddf99e3bc73478a3343ec8f9484c3` before the final docs-only
refresh. This branch is the
audit/repair continuation, not a new release branch; the pinned AndroidTest
source and production code207 artifact remain unchanged. The host-preparation
commit is `c2c79a19842f551fc752a53328024aab8ddb529d`.

### Historical local P5E evidence-chain result

H1–H4 are closed at the host boundary without touching the device: the command
and helper both fail closed on the exact helper SHA-256 before sensitive or
external access; the helper reads the source-pinned production serializer
contract with separate report and receipt field sets and exact persisted-byte
checks; `CollectReadback` is an explicit `Before`/`After` executable mode with
WAL-aware/read-only collection, same-event path binding and typed
`NO_CLAIM_OBSERVED`, `EXTERNAL_CALL_STATE_UNKNOWN`, `RECOVERY_REQUIRED` and
`COLLECTOR_TYPED_STOP` outcomes; and the command's After collection remains
available after timeout or process death without redispatch.

The source-derived contract is
`docs/P5E_PRODUCTION_ARTIFACT_CONTRACT_20260915.json` (SHA-256
`FFE70A70E622706FABFA49D5843310ECD5A283B1CA114E32C636EA26B9FAE4BF`) and its
serializer source is pinned to SHA-256
`1222B8AC9B79DAFC659DD364F50849DFBA4782C181606A92DA47EBD8C6164E3C`.
The current helper/collector SHA-256 is
`4D68F4BB0B0D31FA6D443439967746C1C83CCB4514EE6D7AF830CF90DBE0EC76`; the
review-only command SHA-256 is
`30B50BFEF809225B33901AC130CC5EE83CC8AD76A3D958D987EF19C8FCA50D10`. The
11-case artifact mutation matrix rejects wrong manifest/binding/bundle/
predecessor, swapped bytes, one-byte mutation, extra/missing field, BOM,
invalid UTF-8 and noncanonical bytes. The fixture source is labelled
`HOST_SYNTHETIC_SHAPE_REGRESSION_ONLY`; the production-serializer golden test
source exists but was not executed because this request forbids build.

The final local result is
`docs/P5E_LOCAL_EVIDENCE_CHAIN_RESULT_20260915.json`. Existing production
code207 and AndroidTest 57EC99 artifact/source pins remain unchanged; no build,
install, ADB, instrumentation, provider, credential, runtime authorization or
live DB readback occurred. F1 owner provenance is still `PENDING`, so this
does not issue A4.3 or establish RAW/P5 exit. The one next action is owner
review of this exact hash-bound packet and the separate account/readback/RAW
permission decision; until then, do not dispatch.

The current host-preparation document is
`docs/P5E_RAW_HOST_PREPARATION_20260915.md`, SHA-256
`4A0E678ED298F4FF879062C9FD5F81A383D235F1C27ED4EAE7CA15563DEB6797`.

### Canonical current pin table

| Pin | Current value | Qualification |
|---|---|---|
| Device | `15e84958` | A4.2 pre/post read-only verification matched |
| Production | `com.ml.tblandroidtxt` / `v4.17-p5e.11` / code `207` | frozen candidate; not rebuilt |
| Production APK SHA-256 | `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD` | frozen artifact/backup fact |
| Production source ZIP SHA-256 | `B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348` | frozen artifact/backup fact |
| Certificate SHA-256 | `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` | frozen candidate fact |
| Selected AndroidTest artifact | `D:\App Translate Books\App Translate Books-translation-profile\artifacts\test-builds\v4.17-p5e.11\p5e-account-check-20260916-01\app-debug-androidTest.apk` | selected immutable artifact; artifact/backup bytes match |
| Selected test APK SHA-256 / bytes | `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8` / `1155788` | selected account-replacement bundle; not a live identity claim |
| Selected test source/archive commit | `9e5ffb7819bfb91dcb8ed9e25c901ab10aa48390` | exact tracked-source ZIP in selected payload |
| RAW source contract commit | `d51b7f3c16bdc482513b9904db07b97daed592d1` | separate source contract for RAW live method; not whole-bundle authority |
| Selected test certificate/package/target/runner | `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` / `com.ml.tblandroidtxt.test` / `com.ml.tblandroidtxt` / `androidx.test.runner.AndroidJUnitRunner` | BUILD_INFO/package contract |
| Selected source ZIP / BUILD_INFO SHA-256 | `5029E2AE955E980CEB1246D3ACA19F6B4E008EAA2E5E71360C5BAE305D820C8F` / `772F32E23AEF3537FEF00DACE8E4B9B994448BFEE2071150CF0E0540DDCCC5A9` | artifact/backup payload |
| DB / schema | `3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391` / `24` | A4.2 WAL-aware pre/post readback matched; integrity `ok`, FK `0` |
| Data classification | `RECONSTRUCTED_ONLY` | not code196 recovery |
| Fresh selector / chapter | `p5e-fresh-mercedes-vol5-20260911-01` / `001` | frozen fresh tuple |
| Fresh binding / run | `845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf` / `8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc` | frozen fresh tuple |
| Evaluation | `3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1` | official fresh binding record |
| Pack / profile | `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d` / `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21` | frozen fresh tuple |

The prior A4 artifact `DC0E6790C1D82F3C7D3102711C929F8DC0CA2D380314E4EB77F1EEA46C41AC2B` is retained outside Git as
`SUPERSEDED_NOT_INSTALLED_INVALID_STATUS_MAPPING`: its manifest carried the
field but `redactedPreflightStatus()` did not emit it. It is not used or
installed. The A4.1 correction adds the actual mapping immediately after
`attemptCreated`, and the host contract test reads the real emitter source
against the parser's required/allowed/strict-boolean sets. The contract covers
missing, duplicate, wrong-prefix, invalid-boolean and absent-emitter mappings.

Host QA passed with JDK `21.0.10`, pinned Android SDK `android-35`, Gradle
`9.3.0`: targeted parser tests, emitter-contract tests, AndroidTest Java
compilation, production/schema/pack/profile/wire/version diff guard, secret
scan and `git diff --check`. The test-only archive mirrors the exact payload
under `artifacts/test-builds/` and `backup/test-builds/`; production was not
rebuilt. The A4.2 owner-approved run later replaced only the test package once;
the production package was not operated on, and provider calls, authorization,
attempt and reconciliation creation remained `0`.

The prior exact-preflight result remains historical evidence: it reported
`OK (1 test)` and terminal `-1`, but the old status channel lacked
`p5e.preflight.v2.reconciliationCreated`; its acceptance was correctly
rejected. A4.1 fixes the host contract only; it does not convert that result
into exact-preflight readiness or authorize a rerun.

The A4.2 device event is
`D:\P5E-private\a4-2-exact-preflight-device-20260914-185308591`; its
`A4-2-EVIDENCE-MANIFEST.md` records the raw instrumentation, host-parser result,
and WAL-aware before/after readback. The evidence manifest SHA-256 is
`7FBBECD3E2A8868D42D34CB3F9F2F8BA8CDAC236B4C7161F6747CBB19EE2474D`, and the
raw instrumentation SHA-256 is
`1FFE572DF3ADEBA6A8AB55061BD75F2F5EEBBC479F8BB0D1EA97DB3957D1577C`.
The result was one `OK (1 test)` with terminal `-1`; all four route flags and
the conjunction were `true`, the parser accepted the complete status, and
post-run package/settings/database/tuple/lineage preservation matched.

The A4.2 single-use approval is consumed and is not reusable. Its only device
mutation was one `adb install -r` replacement of the test package; production
package operations, provider/API calls, authorization creation, attempt
creation and reconciliation creation were all `0`. The later local H1–H4
repair closed the host evidence chain without changing the device pins. The
historical next action was one owner decision against the final hash-bound packet. The historical
packet remains RAW-only with one primary call, zero repair/retry and no
RECONCILE; it is not an authorization.
P5/P5E have not exited; execution,
certification and P6 remain
disabled. Historical `HISTORICAL_CODE196_PRESERVATION_FAILED`,
`PILOT_DATA_PRESERVATION_FAILED`, the A3.2 preservation gap and consumed
A2/A3.2 approvals remain unchanged.

## P5E.9B-A4.3 — owner-authorized event terminal pre-dispatch collector STOP

The local repair packet passed offline review and the owner decision was received. The single live event then stopped in the Before collector at `pm-path-production-before`, before any live method or provider dispatch. Current status is `A43_OFFLINE_PACKET_TECHNICAL_GATES_PASS / A43_OWNER_DECISION_RECEIVED / A43_PRE_DISPATCH_COLLECTOR_STOP / COLLECTOR_TYPED_STOP / RAW_NOT_DISPATCHED / P6_NOT_READY`.

The fixed-scope owner packet is
docs/P5E_RAW_AUTHORIZATION_APPROVAL_MANIFEST.md, SHA-256
412790E2E55A8289FF170D3EE93B683553468ADE252EF5C565D833599E5F5EA3.
The separate command is
docs/P5E_RAW_AUTHORIZATION_COMMAND.txt, SHA-256
51A71D47BBFC91DE19658FEEA120CF419ECD0F2FFD77562D74B83C73F9A98AB5.
The narrative proposal is
docs/P5E_RAW_AUTHORIZATION_PROPOSAL.md.
The current root-runtime host supervisor is
scripts/p5e-raw-live-supervisor.ps1, SHA-256
CB9C07312E025DA94D8DB4840B7600CE9A3613DB75CC0E0E4F28EB059E00901F.
The selected production/test artifact, source-archive and BUILD_INFO pairs are
recorded in the manifest and `docs/P5E_A43_OFFLINE_QA_20260924.json`; the
selected test source bundle is commit `9e5ffb7819bfb91dcb8ed9e25c901ab10aa48390`,
while the separate RAW source contract remains `d51b7f3c16bdc482513b9904db07b97daed592d1`.
The preparation evidence is
docs/P5E_RAW_HOST_PREPARATION_20260915.md, SHA-256
4A0E678ED298F4FF879062C9FD5F81A383D235F1C27ED4EAE7CA15563DEB6797.

The source-derived production artifact contract is
docs/P5E_PRODUCTION_ARTIFACT_CONTRACT_20260915.json, SHA-256
FFE70A70E622706FABFA49D5843310ECD5A283B1CA114E32C636EA26B9FAE4BF.
The current local evidence result is
docs/P5E_SQL_BEHAVIORAL_RESULT_20260916.json, SHA-256
C3B7B7C7B86580CF56A810E4ECBA623A54B45F123B92C8A7753484EC54A45B48.

The account-only replacement source is
app/src/androidTest/java/com/ml/tblandroidtxt/EditorialP5EAccountCheckOnlyInstrumentedTest.java,
SHA-256 `2F4BF9AD27CF5DF93D89456767423271907598EA209A0AD6E4C27599BC20063C`;
its host runner is `scripts/p5e-account-check.ps1`, SHA-256
`96E6B3B449D00B75989D3AD4E9403EA9510E504FBE90A53D6825E72E09B71E65`.
The current root-runtime RAW helper pin is
`CB9C07312E025DA94D8DB4840B7600CE9A3613DB75CC0E0E4F28EB059E00901F`;
it is host-only and no RAW command is executed in this package.
The dedicated test-package installer is
`scripts/p5e-install-account-check-test.ps1`, SHA-256
`21AADE819DB83464E96C0BB6AC28CB42FB26AB916D5905AD13CED37C15FC786B`;
its offline parser/self-test and local APK package/target/runner/certificate
inspection pass, and its bounded CheckOnly/replacement path has now run with
the exact serial. Updated QA evidence is
`docs/P5E_ACCOUNT_TEST_INSTALLER_QA_20260917.json`, SHA-256
`041760CD298DA06928D35D41321CF497D7DB49C25A0608D35609370884138519`. The
CheckOnly result is
`D:\P5E-private\p5e-account-check-install-checkonly-20260917-113519515-c1d4240c97d145d28939340dd8904ea0\ACCOUNT_TEST_INSTALL_RESULT.json`;
the replacement result and installed APK are under
`D:\P5E-private\p5e-account-check-install-replacement-20260917-113601720-80675f1e5c4746da97f0a884ee319472`.
The wrapper-built replacement AndroidTest APK is
`058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8`, source
ZIP `5029E2AE955E980CEB1246D3ACA19F6B4E008EAA2E5E71360C5BAE305D820C8F`,
event `p5e-account-check-20260916-01`, and it was installed once with exact
installed-byte/certificate readback. The selected test bundle is also the
immutable AndroidTest input for the offline A4.3 packet; the root RAW helper
remains a separate host runtime authority.

The local account-check preparation/preflight result is
`docs/P5E_ACCOUNT_CHECK_LOCAL_RESULT_20260916.json`, SHA-256
`3FBE39142BA2DAA16AF6F5301271525E71FE1871CF6C14F0DF2A816346AA53EF`. It records PowerShell parse,
hash-gate and redaction/fake-process checks plus the current CheckOnly,
replacement and account-stop outcomes; it contains no credential or account
fingerprint value.

The approval manifest is frozen at
412790E2E55A8289FF170D3EE93B683553468ADE252EF5C565D833599E5F5EA3.
The production serializer source remains
editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/EditorialP5PilotExecution.java,
SHA-256 `1222B8AC9B79DAFC659DD364F50849DFBA4782C181606A92DA47EBD8C6164E3C`.
The local golden report/receipt byte hashes are
`739E83EB04F941C4690FA5C49FED1AA31B8D861C77FE92829BFA1062EEDB3848` and
`3FCED2263D18BDDA7FC03ECE0F0FE85972E03B008DDE5FB74651790A48F3281A`.

The prior F3 result remains historical synthetic evidence. The current local
chain additionally has the executable offline SQLite collector bridge, exact
helper hash gate, source-derived production report/receipt golden bridge and
bounded `COLLECTOR_COMMAND_LOG.jsonl` contract. F1 expected-value Process
loading and the bounded account-only event are complete for the current
account predicate, but they do not authorize A4.3 by themselves.

The packet preserves the harness authorization ID and the exact A4.2
identities. It requests only RAW/GLOSSARY egress to the pinned route, one
primary semantic call, zero schema repair, zero network retry and zero
RECONCILE. DRAFT and PRONOUN remain hidden from the model. The test package
and production code207/certificate pins remain unchanged.

endpointAccountFingerprint is not filled with a default or a settings-file
hash. For the separately scoped account check, the owner-controlled runner
must receive an independently sourced expected value through its process-only
channel before any device process is created. The test then performs the
source-defined in-memory operation:
SettingsStore.load(target).copy(), normalizeEndpoint(settings.baseUrl), then
SHA-256 of UTF-8 endpoint + newline + in-memory settings.apiKey. The credential
must remain in memory and never enter command text, logs or evidence. A
mismatch or unverifiable account returns `MISMATCH` and does not construct
authorization or dispatch. The route-corrected account-only event performed
this operation once and returned `MATCH`; its command, preflight and runner
receipts are preserved in the current event result document. The packet remains
for owner review of the separate A4.3 conditions, not for automatic RAW
dispatch; no provider call, DB write or RAW operation occurred.

Targeted offline QA is
`docs/P5E_A43_OFFLINE_QA_20260924.json`, SHA-256
`D124711219DFC1FD205CDB9205C394E2DA4CB3569827CA6C9D3B0A592EEC7050`.
The final host-only provenance result is
`D:\P5E-private\p5e-a43-offline-provenance-qa-20260924-04\RESULT.json`,
SHA-256 `5C327F8265567734A744F5A43E770A76C5C52B51B6D4378E45CE2E4B1C0E5A3D`;
the final PrepareEvent plan is
`D:\P5E-private\p5e-a43-offline-prepare-qa-20260924-04\EVENT_PLAN.json`,
SHA-256 `9A87EAEB728994A281143DF068ABDBCB5D8F373B40E2EF56798A8DADCABFCAD6`.
Both are synthetic/host-only and recorded zero device, provider and credential actions.

The repaired command invokes the supervisor as one child process. It computes
fresh issuedAt/expiresAt values at owner-approved dispatch, uses a 240000 ms
host observation window, and must be run once only. The supervisor uses
180000 ms authorization validity and 120000 ms execution deadline, captures
only redacted streams, and never retries or redispatches. A zero process exit
does not establish RAW acceptance; the independent post-readback verifier is
required.
The selected live method performs its own preflightOnly checks internally; no
separate instrumentation preflight is permitted. F2/F3 host evidence and the
source-derived argument list are recorded in the preparation evidence above.

The single next action is offline diagnosis of the concrete
`pm-path-production-before` collector launch failure using the preserved event
evidence. No retry or redispatch of this event is permitted, and P6 remains
closed.

## Historical A4 exact-preflight evidence-channel blocker

The A4 owner-authorized device sequence used the current fresh binding and
production code207 without touching the production package. Read-only baseline
verified device `15e84958`, production SHA-256
`2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD`, the
matching certificate/signature, schema v24, DB SHA-256
`3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391`,
integrity `ok`, FK violations `0`, the exact fresh selector/binding/run/
evaluation/pack/profile tuple and zero fresh lineage. No app PID or active job
was observed, so no force-stop was needed.

### Historical A4 pin table

| Pin | Current value | Qualification |
|---|---|---|
| Device | `15e84958` | A4 readback |
| Production | `com.ml.tblandroidtxt` / `v4.17-p5e.11` / code `207` | A4 readback |
| Production APK SHA-256 | `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD` | A4 readback; artifact and backup match |
| Certificate SHA-256 | `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` | A4 readback |
| Device signature token | `abebea4b` | A4 readback |
| Installed test APK SHA-256 | `5D248FFD33F52AC649966C4135773708C7CA747CE34C28BB763B40EC1FE81467` | A4 readback after one replacement |
| Historical corrected test APK SHA-256 | `DC0E6790C1D82F3C7D3102711C929F8DC0CA2D380314E4EB77F1EEA46C41AC2B` | `SUPERSEDED_NOT_INSTALLED_INVALID_STATUS_MAPPING` |
| DB / schema | `3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391` / `24` | A4 readback |
| Data classification | `RECONSTRUCTED_ONLY` | not code196 recovery |
| Fresh selector / chapter | `p5e-fresh-mercedes-vol5-20260911-01` / `001` | A4 readback |
| Fresh binding / run | `845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf` / `8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc` | A4 readback |
| Evaluation | `3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1` | official fresh binding record |
| Pack / profile | `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d` / `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21` | A4 readback |

The test package was replaced exactly once with the host-built A4 artifact
`5D248FFD33F52AC649966C4135773708C7CA747CE34C28BB763B40EC1FE81467`.
The model-remediation method then ran exactly once and reported a committed
model correction, successful readback, preserved non-model settings and route
match. The settings file was already present; only its hashes were retained:
`C2FC2DC71F3687E09F6D3899A99F397C3B75AD05B402D176367C03A0280E8062` before
and `4A0AA4564B62D5F9852A108B1D7991DA21AEF46DCDA4ACBC485E6E45CD3214F9`
after. No settings content or credential was read or logged.

The exact-preflight method ran exactly once. Its raw output reported one test
`OK (1 test)`, terminal instrumentation code `-1`, provider/model/endpoint/
route all `true`, a valid conjunction, DB preservation `true` and provider
calls `0`. The host parser rejected the output because the versioned status
channel omitted required `p5e.preflight.v2.reconciliationCreated`; therefore
this is `P5E_9B_A4_EXACT_PREFLIGHT_EVIDENCE_CHANNEL_FAILED`, not an exact
preflight acceptance. The DB after remediation and preflight remained the
expected SHA/schema with zero attempts, authorization receipts,
reconciliation, history, lifecycle and report/receipt bytes. No rerun is
permitted in this one-shot window.

The device evidence manifest is `SHA256SUMS.txt` with SHA-256
`4C902DDEAEC7554C42EDB65F53ECC73403ECB45F4CE0A8D0BF74DB3312844EF0`.
The raw instrumentation output and parser result are retained in that private
event; the parser separates test success, output parse, route match, DB
preservation and exact acceptance, with only exact acceptance failing.

The test-only emitter correction adding that missing `false` field is committed
at `911fb355a2148feb8c7ec4b60a843c547596bf56`. A new host-only artifact is
available at
`D:\P5E-private\a4-fresh-raw-model-preflight-statusfix-20260913-202218809\app-debug-androidTest.apk`
with SHA-256
`DC0E6790C1D82F3C7D3102711C929F8DC0CA2D380314E4EB77F1EEA46C41AC2B`; it is
`SUPERSEDED_NOT_INSTALLED_INVALID_STATUS_MAPPING`, has not been installed or
device-verified, and must not be used. The currently installed test
package remains the prior A4 artifact; a separate owner approval is required
before any replacement or preflight rerun.
The corrected artifact manifest `SHA256SUMS.txt` has SHA-256
`E333AC3A3FC21A808F69EDFC71ACD9710629C012C6DCE091C19B7F86ED282109`.

The historical next action at that time was to obtain that narrowly scoped
approval. It must not
authorize a provider call, RAW authorization, attempt, reconciliation,
production-package operation or automatic retry. P5/P5E have not exited;
execution, certification and P6 remain disabled. Historical
`HISTORICAL_CODE196_PRESERVATION_FAILED`, `PILOT_DATA_PRESERVATION_FAILED`,
the A3.2 immediate-preservation gap and consumed A2/A3.2 approvals remain
unchanged.

## Historical A3.2C delayed read-only closure blocked

P0-P4 are complete as historical phase evidence. P5/P5E have not exited: no
accepted RAW predecessor, REPORT_L1 or receipt exists, and execution,
certification and P6 remain disabled. A2 ended fail-closed with
`P5E_9B_A2_FRESH_RAW_ROUTE_PRECONDITION_FAILED`; its single-run approval is
consumed and is not reusable.

A3.1/A3.1R and the A3.2 host preparation are retained as historical evidence.
The owner-approved A3.2 run performed one test-package replacement and one
diagnostic method invocation. The diagnostic emitted exactly four redacted
instrumentation-status values: `providerMatch=true`, `modelMatch=false`,
`endpointMatch=true`, `routeMatch=false`; the conjunction was valid and the
test result was `OK (1 test)`. This is an observed route signal, not a valid
authorization or exact-preflight acceptance.

The pre-run read-only checks passed: production code207, the A2 test artifact,
DB schema v24/hash, integrity/FK state, fresh tuple and zero lineage counts
matched their pins. The test package was then replaced once with the A3R
artifact and read back with its exact hash. The redacted evidence event is
`D:\P5E-private\fresh-raw-route-diagnostic-a3.2-run-20260912-012928513` with
manifest SHA-256
`12898E7A7DFFDCBEEF4E2E0BF6794E88C90EDE4E6DAD15D0147C9710ABF137CB`.
During the first post-run read-only sequence the device became unavailable to
ADB, so settings-after, DB-after and complete lineage preservation could not be
verified. A later A3.2C one-shot availability check found the device online but
the production process active at PID `420`; it stopped before package, settings
or DB readback and did not force-stop the process. No retry or workaround was
performed. The current PreTag result remains `FAIL` at `Step 05`; the earlier
`Step 09` result is historical A3.1 evidence only.

The historical next action at that time was a separately authorized read-only
window after both production and test processes were idle; this result did not
authorize an automatic retry, force-stop or select A4. The historical
`HISTORICAL_CODE196_PRESERVATION_FAILED` and `PILOT_DATA_PRESERVATION_FAILED`
conclusions remain unchanged. P5/P5E have not exited, and execution,
certification and P6 remain disabled.

## 1. Locked product decision

- Build the next Editorial workflow from the device-verified v4.17 development baseline, commit `921af9256e1b1fe4ab9ac113affa98eec7a1e339`, APK `4.17-dev.1` / code169.
- Use `V5-SAFE.4.1.3-FULL` as the first quality authority and reference pack.
- Develop on one branch: `feature/v4.18`.
- Preserve the v4.17 Translation behavior and its Glossary/Pronoun compatibility. Editorial input contracts remain separate from Translation profile contracts.
- Reuse the existing Editorial ZIP importer, manifest parser, immutable storage and compatibility evaluator. Do not build a second pack platform.
- Keep Editorial packs data-only. Imported packs may change authority text and declarations within a supported contract; they may not ship executable code or scripts.
- A running L1-L3 chain is pinned to one exact canonical pack hash. Installing a newer pack never rewrites or silently upgrades an existing project/run.

The v4.18 release is a controlled continuation from the verified v4.17 recovery head rather than from the older `main`. This owner-directed baseline exception is limited to this release and must be recorded in the release checklist.

## 2. Source authority

External source release:

`D:\Ebooks\1. Prompt cac the loai\4.BIÊN TẬP\BIEN_TAP_V5_SAFE_4_1_3_FULL_RELEASE`

Runtime authority files:

| Role | Source file | Bytes | SHA-256 |
|---|---|---:|---|
| `PROJECT_INSTRUCTION` | `CHATGPT/PROJECT_INSTRUCTION_BIEN_TAP_V5_SAFE_4_1_3_FULL.txt` | 9,485 | `1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD` |
| `TURN_PROMPT` | `CHATGPT/PROMPT_DAU_CHAT_3_LUOT_V5_SAFE_4_1_3_FULL.txt` | 8,852 | `D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F` |
| `WORKFLOW` | `CHATGPT/WORKFLOW_BIEN_TAP_3_LUOT_V5_SAFE_4_1_3_FULL.txt` | 34,917 | `5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730` |

The release's static qualification was rerun on `2026-09-03`: `306 PASS / 0 FAIL`, baseline words `5308`, release words `7050`, exact retained lines `223/311`. This is structural/contract evidence only. The release itself records `realChapterPilot=NOT_RUN`, `modelBehaviorBenchmark=NOT_RUN`, `androidIntegration=NOT_PERFORMED` and `apkBuild=NOT_PERFORMED`.

The files under `APP`, `COMMON` and `TESTS` are design-time specification and qualification inputs. They are not executable Android pack payload and must not be executed after import.

## 3. Authority order

1. Repository safety and Git/build workflow.
2. This document for v4.18 product scope and ordered work.
3. `docs/EDITORIAL_PACK_V1_4_1_3_INTEGRATION.md` for the pack ABI and compatibility boundary.
4. `docs/EDITORIAL_V4_18_IMPLEMENTATION_MAP.md` for file ownership and verification mapping.
5. `BUILD_STATE.md` and `WORKSPACE_SNAPSHOT.md` for current facts and exact next action.
6. `release_checklists/v4.18-editorial-v5-safe-4-1-3.md` for evidence gates.
7. The exact 4.1.3 Project/Workflow authority for editorial quality.
8. Older Editorial/RSC/IPC documents as historical reference only.

## 4. Pre-write adversarial review

The following questions were checked before any project file was changed:

| Question | Finding | Decision |
|---|---|---|
| Is the original workspace safe to modify? | No. `D:\App Translate Books` is on the older D1 branch and contains user-owned `.idea` changes. | Leave it untouched. |
| Is the moved v4.17 checkout valid? | Its Git pointer still referenced the old C: location. | Repair with `git worktree repair`, then require clean status and HEAD `921af92`. |
| Should v4.16 Editorial R3 be merged? | It is infrastructure-heavy, incomplete and not part of the verified CODE169 product. | Preserve as history; reuse only individually justified code/tests. |
| Is a new pack installer required? | No. CODE169 already has secure ZIP snapshotting, canonical manifest validation, content-addressed storage and compatibility evaluation. | Extend/qualify the existing Pack Manifest v1 path. |
| Should all APP JSON files be put in the imported ZIP? | No. Current importer intentionally accepts one manifest plus exactly three root authority files. | Compile APP semantics into a versioned runtime contract and tests. |
| Can every future Editorial version avoid an APK rebuild? | No. Only packs expressible by already supported contracts/capabilities can. | Unknown required capability returns `ENGINE_UPGRADE_REQUIRED`. |
| Is 4.1.3 production proven? | No; only static/parity qualification exists. | Require real-chapter pilot, model behavior evidence and Android QA before activation. |
| Can model text decide PASS/BLOCKED? | No. This caused false-block risk. | App owns preflight, identity, state, equations and typed recovery; model supplies semantic judgments. |

Optimization result: keep one existing manifest format, one three-pass runtime contract and one active release plan. Do not introduce a marketplace, arbitrary workflow language, downloadable code, V23 activation or a second persistence platform.

## 5. Product workflow

The first usable workflow is deliberately chapter-by-chapter:

```text
import/select one compatible Editorial pack
  -> create/open one Editorial chapter
  -> select RAW + original DRAFT + five-column GLOSSARY + seven-column PRONOUN
  -> deterministic source preflight and immutable manifest
  -> L1 REPORT_L1
  -> L2 VI_L2 + CHANGE_MAP_L2
  -> L3 FINAL_QA + QA_RECEIPT
  -> local export after derived release gates pass
```

Normal mode requires all four sources. Only an explicit user action may select an alternate source mode. Pair Context remains optional and never becomes required merely because it exists.

## 6. Fixed safety boundary

### In scope

- Package 4.1.3 as a valid existing `editorial-pack.json` plus exactly three UTF-8 root authority files.
- Add a trusted engine profile/contract descriptor for the supported 4.1.3 semantics.
- Deterministic four-source preflight before any model call.
- Separate full-bundle validity from phase visibility.
- App-owned hashes, stable anchors, declared populations, actual diff and receipt validation.
- Typed `INPUT_REQUIRED`, `REPAIR_REQUIRED`, `RETRY_REQUIRED` and `CONTENT_BLOCKED` outcomes with recovery/resume.
- `PRESERVE_DRAFT` as processed, no-canon, no-propagation when semantic evidence is insufficient after preflight passes.
- Side-by-side pack installation and exact project/run pinning.
- One chapter and one active writer/run at a time for the first release.
- Real chapter pilot and device QA before execution is enabled.

### Explicitly out of scope

- Arbitrary executable code or scripts from a pack.
- Online pack marketplace, silent updates or remote auto-download.
- Automatic project rebind or in-place pack overwrite.
- Arbitrary phase/DAG interpreter beyond the supported three-pass contract.
- Whole-volume dispatch, automatic chapter matching or cross-device sync.
- RSC/Relation-Speaker inference, V23 activation and historical IPC/CP6 work.
- New database migration until a focused persistence test proves the current schema cannot support exact pack/project/run identity.
- Cryptographic publisher signatures for the first local-import milestone; content hashes and explicit local user import are required. Signature support remains a later trust enhancement.

## 7. Ordered implementation sequence

Only one phase may be in progress.

### P0_DOCS — baseline, contract and map

Status: `COMPLETE` for document preparation; no production source or APK change.

- Repair and verify the moved v4.17 worktree.
- Create `feature/v4.18` from exact verified CODE169 HEAD.
- Record the 4.1.3 source hashes and rerun its static qualification.
- Freeze the Pack Manifest v1 reuse decision, compatibility boundary, implementation map and release checklist.
- Update current README/state/snapshot.

Exit: all active documents agree on baseline, branch, pack, scope, phase and exact next action.

### P1_CHARACTERIZATION — prove the existing platform boundary

- Run focused existing manifest, integrity, compatibility, ZIP import, storage and project-binding tests without production changes.
- Create a test-only 4.1.3 reference ZIP using the existing four-root-entry format.
- Determine whether current manifest fields can represent normal four-source mode, explicit alternate mode, phase visibility, nine gates and G1-G24 without schema expansion.
- Record only concrete gaps. Do not change the manifest parser speculatively.

Exit: baseline tests pass and every required 4.1.3 behavior maps to an existing field/capability or one documented minimal gap.

### P2_REFERENCE_PACK — create and validate the importable 4.1.3 pack

- Generate a canonical `editorial-pack.json` and exact authority payload.
- Validate root paths, UTF-8/no-BOM policy, byte lengths, per-file hashes and canonical pack hash.
- Add negative fixtures for one-byte drift, missing/extra entry, path traversal, duplicate role and wrong contract.
- Keep the legacy bundled V5-SAFE.4 asset unchanged/read-only.

Exit: 4.1.3 imports into immutable storage and an identical re-import is idempotent; no execution is enabled.

### P3_RUNTIME_CONTRACT — deterministic control layer

- Implement only gaps proven in P1 for preflight, source modes, phase projection, typed stop/recovery, preserve semantics and receipt validation.
- Publish a trusted engine profile with exact contract/schema/capabilities and evidence.
- Keep unknown required capabilities fail-closed as `ENGINE_UPGRADE_REQUIRED`.
- Keep model output unable to directly mutate state or self-certify PASS.

Exit: G1-G24, nine preflight decisions and false-block cases pass with fake/model fixtures; provider calls remain disabled.

### P4_BINDING_AND_RESUME — user import and immutable selection

- Expose ZIP import and compatibility result in the Editorial UI.
- Bind new project/run creation to exact `packId`, version and canonical hash.
- Preserve side-by-side versions; never auto-rebind.
- Prove process-death recovery and stale-chain invalidation.

Exit: 4.1.3 and a synthetic compatible 4.1.4 install on the same APK without rebuild and remain independently selectable.

### P5_L1_PILOT — one real L1 vertical slice

- Run preflight before model access.
- Execute one controlled L1 on representative real chapters.
- Parse and validate REPORT_L1/receipt; repair formatting once without semantic reinterpretation.
- Measure false stops, preserve use, token/context size and truncation behavior.

Exit: no unexplained or unrecoverable false block; every stop has exact evidence and recovery.

### P6_L2_L3 — complete the three-pass chain

- Add atomic L2 result/change-map commit and L3 final/receipt commit.
- Reconstruct actual diff in app code.
- Enforce five release numbers and no-regression before export.
- Resume truncated output with stable anchors only when qualification proves it necessary.

Exit: at least three representative chapters complete L1-L3 or stop only for a proven content conflict.

### P7_REGRESSION_BUILD_QA — numbered development artifact

- Run focused Editorial suites, full JVM regression, lint and all preserved v4.17 Translation tests.
- Build only through `scripts/build-and-save.ps1`; next Android versionCode must be greater than 169.
- Verify immutable artifact/backup/source parity.
- Perform clean-install, import, process-death, retry, stale-input, side-by-side version and export QA on device.

Exit: evidence-backed development build; no public release claim until tag/archive/release gates pass.

## 8. Definition of done

The release is not complete merely because 4.1.3 imports. It is complete only when:

1. The same APK imports 4.1.3 and a compatible synthetic later pack without rebuild.
2. Unknown required behavior is rejected as `ENGINE_UPGRADE_REQUIRED`, not generic BLOCKED.
3. Project/run identity remains pinned across restart and pack updates.
4. Input/transport/schema errors stop before semantic model work and have recovery actions.
5. Formatting/truncation failures do not become semantic conflicts.
6. Real chapter L1-L3 evidence demonstrates no unexplained false hard-block.
7. Translation behavior from CODE169 passes regression.
8. A numbered build greater than code169 is archived and device-QA verified.

## 9. Exact next action

Historical override, 2026-09-25: `OFFLINE_TOOLCHAIN_REPAIR_PASS / NEW_OWNER_DECISION_REQUIRED / RAW_NOT_DISPATCHED / P6_NOT_READY`.
The final host repair packet is bound by
`docs/P5E_COLLECTOR_LAUNCH_REPAIR_PROVENANCE_20260925.json`, with targeted
Windows PowerShell 5.1 QA in `docs/P5E_RAW_TOOLCHAIN_REPAIR_QA_20260925.json`.
The prior owner decision was consumed by the terminal pre-dispatch event; do not
reuse that decision or event. No device execution, A4.3 dispatch, provider call,
DB write or P6 action is authorized by this offline repair. The following A4
checkpoint narrative is historical and does not override this next action.

The following A4 checkpoint text is historical evidence only.

The host-only A4.1 group corrected the actual status emitter and verified it
against the parser contract. A4.2 then used the immutable AndroidTest artifact
`D:\App Translate Books\App Translate Books-translation-profile\artifacts\test-builds\v4.17-p5e.11\a4-1-test-20260914-065532\app-debug-androidTest.apk`
with SHA-256
`57EC99A95EE2DC0F1759934C62CEA39E2EC92EB77C3DAF76CFEED28D41A2FDEA`; the
identical backup is under `backup\test-builds\v4.17-p5e.11\a4-1-test-20260914-065532`.
It was replaced once as the test package and read back exactly during A4.2.

A4.2 evidence is retained in
`D:\P5E-private\a4-2-exact-preflight-device-20260914-185308591` with
manifest SHA-256
`7FBBECD3E2A8868D42D34CB3F9F2F8BA8CDAC236B4C7161F6747CBB19EE2474D` and raw
instrumentation SHA-256
`1FFE572DF3ADEBA6A8AB55061BD75F2F5EEBBC479F8BB0D1EA97DB3957D1577C`.
The run produced one `OK (1 test)`, terminal `-1`, accepted complete status,
all route/preservation conditions true and zero provider/creation flags. DB,
settings, tuple and lineage readback matched before and after.

The historical next action at the A4.2 checkpoint was owner review of the prepared A4.3 RAW authorization
packet derived from the A4.2 manifest. It remains single-use, RAW-only, one
primary call, zero repair/retry and no RECONCILE. The A4.2 approval is
consumed and not reusable. The endpoint account fingerprint is still pending
the separately permitted memory-only account check; P5/P5E exit, execution,
certification and P6 remain incomplete, and no RAW predecessor/report/receipt
exists.
