# P5E.9B-A4.3 — One-run RAW authorization proposal

> Current work package — 2026-09-28: `OFFLINE_PACKET_REVIEWABLE / LUNA_PASS / 0_BLOCKER / 0_HIGH / 0_MEDIUM / 0_LOW / OWNER_DECISION_NOT_REQUESTED / NOT_DISPATCHED / P6_NOT_READY / CLOSED_EVENTS_NON_REUSABLE`. The prior event and every older decision/packet remain closed/consumed. The new packet is reviewable after offline atomicity, expected-value isolation, closed-event and archive-clean PASS; it is not an authorization request.

> Current packet: manifest `D8F5BECE61116217E8F9C0108AE08624A16C6736A2E9DE04F4BF73E0CEEFD5DE`, command `52B961DEC64429B786970D47083F94067E739AD9A9A9D5E988B3F8C8198F246D`, helper `4DAD6E30928DD0D78396BDDFA57AFA1053E75481524D2EA05659227869C57444`, serial `15e84958`, provenance `254925DB44DD7693EA8948129C00E26EC1F0B69FA90412ECE8C16E09FD57B476`, Luna review `1A83DF584E85980EE9C4B6D7DA089C1E078FF54905E6DBBA9C65DC72C80A95B4`. Final QA is atomicity/environment `28/28`, main `21/21`, binding `262/262`, regression `175/175`, DB `56/56`, archive-clean PASS.

> Closed historical A4.3 outcome — 2026-09-26: the explicit owner decision was received and consumed by exactly one event. The new packet stopped in the read-only Before collector with `COLLECTOR_TYPED_STOP / P5E_COLLECTOR_BINDING_TUPLE_MISMATCH`; RAW was not dispatched, P5 exit was not claimed and P6 remains not ready. Result: `docs/P5E_A43_EVENT_RESULT_20260926.json`. This does not authorize the current offline repair or a new event.

> Historical consumed-packet audit — 2026-09-26: this original proposal and its manifest/command remain historical and consumed. The separate post-repair packet was verified at execution with manifest `669C54049920C49344D2FB55533EFA9FA9F87E933A6A18DE5FA7215F1146D147`, helper `8A0509743403B28F7C074BE37DD7D08D41A1B9C0E56AC70D8A43024803CDF434`, exporter `D8783B31F9141458CA397915664CA79B07D3161A5E0F0D3B4365C5C65EA41D06`, bridge `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111` and command `A2EF2BA90F07D3F4D2517E7F1541EA752615F6BCF61E304E579E301A8D5D3D08`.

> Current DB readback prerequisite — 2026-09-26: `OFFLINE_DB_HOST_READBACK_REPAIR_PASS` is bound by `docs/P5E_DB_HOST_READBACK_REPAIR_QA_20260925.json` at `56/56 PASS`. The old proposal, manifest/command and 2026-09-25 decision remain consumed; no live event is authorized by this work.

> Current Next action: owner review of the exact offline packet only. Do not create or consume authorization, execute the live command, create an event, use ADB/device/provider/credential/DB writes, build/install, retry/redispatch, dispatch RAW or open P6.

> Superseded DB diagnosis — 2026-09-25: WAL/SHM exit `1` are valid absence results. The actual stop was the direct Android `sqlite3` consistent-read command. This proposal remains consumed and is not executable; the offline binary-export/host-readback repair is now complete. No new key, account check or owner live decision was requested for that repair.

> Current version-contract repair: collector Android versionName expected is corrected to 4.17-p5e.11, independently confirmed by immutable BUILD_INFO and offline inspection of the hash-matched pulled production APK. Release label v4.17-p5e.11 remains a label, not Android versionName. Companion QA is `PASS` (collector version 5/5); the new event below is terminal and no retry occurred.

> Historical owner handoff: this proposal carried the version-corrected packet. The owner decision was received and consumed by the one new event below; offline packet and account MATCH remain closed, and no account rerun or new credential provenance is requested. No authorization or live execution remains reusable from this document.

> Superseded packet integration — 2026-09-25: ACCOUNT_MATCH_EVIDENCE_VERIFIED / OFFLINE_TOOLCHAIN_REPAIR_PASS / PACKAGE_VERSION_REPAIR_OFFLINE_PASS / A43_OWNER_DECISION_RECEIVED_AND_CONSUMED / A4.3_PRE_DISPATCH_COLLECTOR_STOP / COLLECTOR_TYPED_STOP / P5E_COLLECTOR_ADB_NONZERO / RAW_NOT_DISPATCHED / P5_EXIT_NOT_CLAIMED / P6_NOT_READY. The selected replacement bundle, root helper, resolver, manifest and command were aligned offline; the new event stopped in the Before collector before runtime authorization, allowlisted writes or RAW/GLOSSARY egress. Do not repeat the account event, retry the A4.3 event or reuse its decision. Historical hashes below remain historical only.

> Historical closed-event result — 2026-09-25: event `D:\P5E-private\raw-live-20260925-093707011-cc71e9e18029485c8e2411698c88f586` ran once on serial `15e84958`. Fourteen read-only Before commands launched once with bounded capture; 11 exited `0` and 3 exited `1` (`database-wal-presence`, `database-shm-presence`, `database-consistent-read-transaction`); `readOnlyCommandCount=14`, provider calls `0`, credential reads `0`, device mutations `0`, redispatches `0`. The first two exits are valid `ABSENT` results; `COLLECTOR_OUTCOME.json` stopped on the consistent-read command with detail `P5E_COLLECTOR_ADB_NONZERO`. No After collector, live method or RAW acceptance was reached, and the redacted receipt does not identify the native reason for that command.

> Historical host launch repair closure — 2026-09-25: Before, Dispatch and After used one explicit SDK/toolchain contract. Resolver `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8`, helper `4BF9E361AA2CD535C819EAE1D6F315F8DC929B989FE30285EAA98C9A0E98B31C`, command `D0F1462725CA51DD789480D4BBEC87FE1AC8AC2BBB5024B9101A74B67E14403B`, manifest `6C33FFA3742340E9099B83A676538B5D32D1942276CC3C9643E98054CE6E4EA4`, and companion QA `docs/P5E_PACKAGE_VERSION_TOOLCHAIN_QA_20260925.json` are historical pins. QA reported zero real ADB/signer/provider/device/credential actions, bounded timeout capture, legacy-dispatch rejection, verifier environment isolation and no redispatch. This repair does not prove device readiness or RAW acceptance.

> Historical execution audit 2026-09-17 at c222b13c: TEST_PACKAGE_REPLACEMENT_PASS / ACCOUNT_RUNNER_REPAIR_REQUIRED / EXPECTED_SOURCE_PENDING / ACCOUNT_CHECK_NOT_EXECUTED / A4.3_NOT_ISSUED / RAW_NOT_RUN / P6_NOT_READY. Installed test APK evidence is retained. Source-derived offline probe reproduced four false acceptances and found the incomplete instrumentation component. Next action is the bounded host repair and trusted-input feasibility work in docs/P5E_NEXT_WORK_REQUEST_20260917.md; no reinstall or device execution. See docs/P5E_EXECUTION_AUDIT_20260917.md and docs/P5E_ACCOUNT_RUNNER_AUDIT_RESULT_20260917.json. SQL/golden PASS remains limited to its tested scope.

Historical proposal status (superseded above): LOCAL_BEHAVIORAL_GATE_GREEN / F1_PROVENANCE_PENDING /
OWNER_DECISION_PENDING / A4_3_NOT_ISSUED / RAW_NOT_RUN /
LIVE_ACTIONS_NOT_AUTHORIZED / P6_NOT_READY

The collector/SQL local result and the account MATCH receipts are retained. This
proposal did not create a reusable runtime authorization or grant a second
device/provider event. The one approved event reached only the read-only Before
collector and stopped on the typed ADB nonzero; no freshness readback,
allowlisted write scope or RAW acceptance was established.

The fixed-scope files for the consumed historical packet were
`docs/P5E_A43_FINAL_EXECUTABLE_APPROVAL_MANIFEST_20260926.md` and
`docs/P5E_A43_FINAL_EXECUTABLE_COMMAND_20260926.txt`. Their SHA-256 values are
`23AF3DFAA81F50484187AFD183EA56454EBE38022C67DA2B963245A42D230053` and
`C84355B912DCF3BB59D52004EC80E06CE8B37FC75B5ABE083ECCA95D64C89BA3`.
The historical helper was pinned to
`17CC1C19BF4F6B1B71A77100D710375BDBCFDA7AC82D4508634B68E857DEFD2E`; the
exporter is `D8783B31F9141458CA397915664CA79B07D3161A5E0F0D3B4365C5C65EA41D06`;
the SQLite bridge is
`4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111`; and the
certificate identity is
`47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`.
The historical authorization request, including the APK/source/certificate
and serial `15e84958` pins, was
`docs/P5E_A43_FINAL_EXECUTABLE_OWNER_AUTHORIZATION_REQUEST_20260926.md`.
All earlier manifest/command/helper values in this historical proposal remain
historical and consumed; they are not fallback inputs. The repaired offline
packet and its exact pins are recorded at the top of this proposal, but no new
authorization request is created by this work package.

## Historical owner decision — RECEIVED AND CONSUMED, not reusable

Local evidence result: `docs/P5E_PACKAGE_VERSION_TOOLCHAIN_QA_20260925.json`, SHA-256
`893D1E4888D1905D50EAB6594AF6C29DF2EE79DA244EAA72C9943A2A2296D3E8`, recorded after the package-version
 repair QA. The historical consumed packet refs are manifest `6C33FFA3…6E4EA4`, command
`D0F14627…E14403B`, helper `4BF9E361…E98B31C`, resolver
`C0AE7D43…D5C3C8`, and the prior A4.3 QA remains historical evidence.
The historical consumed manifest/command/root helper/resolver are
`6C33FFA3…6E4EA4` / `D0F14627…E14403B` / `4BF9E361…E98B31C` /
`C0AE7D43…D5C3C8`; artifact contract is
`FFE70A70…A4BF`;
production serializer is `1222B8AC…64E3C`; the targeted JVM test source is
`3D7C7A39…31A5`; frozen production/test APK pins remain code207 /
`2CCBB844…800FD` and `058BE851…158E8`; selected AndroidTest source archive is
`5029E2AE…20C8F` from source commit `9e5ffb78…`. The local diff does not
change production source, schema/migration, route/model, pack/profile,
prompt, budget or input identities; it repairs only host toolchain launch,
dependency pins and redacted diagnostics.

Earlier local result: `docs/P5E_A43_OFFLINE_QA_20260924.json`, SHA-256
`D124711219DFC1FD205CDB9205C394E2DA4CB3569827CA6C9D3B0A592EEC7050`, recorded
after the targeted packet QA. The manifest/command/root helper are
historical `412790E2…5EA3` / `51A71D47…8AB5` / `CB9C0731…901F`; artifact contract is
`FFE70A70…A4BF`;
production serializer is `1222B8AC…64E3C`; the targeted JVM test source is
`3D7C7A39…31A5`; frozen production/test APK pins remain code207 /
`2CCBB844…800FD` and `058BE851…158E8`; selected AndroidTest source archive is
`5029E2AE…20C8F` from source commit `9e5ffb78…`. The local diff does not change production
source, schema/migration, route/model,
pack/profile, prompt, budget or input identities; it repairs only host
pin/path orchestration and provenance checks.

QA freeze: round 1 executed the real SQL/collector/parser/verifier chain and
targeted JVM golden bridge; round 2 independently rejected identity/event,
partial-artifact, recovery/unknown and timeout-redispatch counterexamples.
Both pass on the hashes above with zero failures and zero external actions.

The route-corrected account event on serial `15e84958` is closed with
`ACCOUNT_CHECK_COMPLETED_MATCH`, seven read-only preflight attempts, one runner
launch and exit `0`. The command, preflight and runner receipt hashes are
`C00F4116…6BCE81`, `C0604F6A…4779C7` and `529B082D…9702C9`; provider calls,
DB writes and RAW dispatches are all zero. This evidence proves only the
account-only event and does not authorize RAW or readback collection.

Account-check replacement pins are separate from the RAW/A4 test pin:

| Item | SHA-256 / status |
|---|---|
| Test-only source `EditorialP5EAccountCheckOnlyInstrumentedTest` | `2F4BF9AD27CF5DF93D89456767423271907598EA209A0AD6E4C27599BC20063C` |
| Host runner `scripts/p5e-account-check.ps1` | `0722A243C92724D59AFB7CF4B6DE674024F9BAE4FD76A712DF0210AF25B036F3` |
| Dedicated account-test installer | `21AADE819DB83464E96C0BB6AC28CB42FB26AB916D5905AD13CED37C15FC786B`; CheckOnly and one replacement PASS |
| Installer QA result | `docs/P5E_ACCOUNT_TEST_INSTALLER_QA_20260917.json`, SHA-256 `041760CD298DA06928D35D41321CF497D7DB49C25A0608D35609370884138519`; certificate-case repair and offline guard evidence |
| Replacement AndroidTest APK, event `p5e-account-check-20260916-01` | `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8`, installed once; exact readback |
| Replacement source ZIP | `5029E2AE955E980CEB1246D3ACA19F6B4E008EAA2E5E71360C5BAE305D820C8F` |
| Local account-check result | `docs/P5E_ACCOUNT_CHECK_LOCAL_RESULT_20260916.json`, SHA-256 `3FBE39142BA2DAA16AF6F5301271525E71FE1871CF6C14F0DF2A816346AA53EF`; CheckOnly/replacement PASS; account check blocked by missing expected value |
| Current RAW command/root helper pins | `D0F14627…E14403B` / `4BF9E361…E98B31C`; resolver `C0AE7D43…D5C3C8`; nested helper `364A6AA2…6FFE7` is historical only |

The live gate results and future prerequisites have these separate statuses:

- process-only expected account fingerprint for any future live dispatch —
  `OWNER_PROCESS_VALUE_REQUIRED_AT_DISPATCH`; the account receipt is not a
  substitute for fresh live verification, and no API key/raw endpoint is sent
  in chat or Git;
- the source-defined memory-only account operation
  `SHA256(UTF8(normalizeEndpoint(baseUrl) + "\n" + apiKey))`, with actual computed
  only on the approved device and compared to the trusted expected value; the
  operation scope is `RECEIVED`, but the expected value is still missing;
- read-only package/certificate/SQLite/WAL-aware collection for the exact
  serial and event — fourteen Before commands launched once with bounded
  capture; 11 exited `0`, two presence probes returned exit `1` meaning
  `ABSENT`, and the consistent-read command exited `1`; the collector stopped
  with `P5E_COLLECTOR_ADB_NONZERO`, and the redacted receipt does not identify
  that command's native reason;
- exactly one RAW/GLOSSARY dispatch to the pinned route: primary `1`, repair
  `0`, retry `0`, fallback/RECONCILE off, DRAFT/PRONOUN hidden; caps
  input/output/total `100000/4096/104096`, cost `USD0.05`, execution/auth/host
  windows `120000/180000/240000 ms` — `BLOCKED_BY_BEFORE_COLLECTOR_STOP`.

The event did not reach the durable authorization/attempt/lifecycle/report/
receipt write scope or the allowlisted RAW artifacts. For any future event, the
exact fresh tuple and lineage must be unused before dispatch; afterwards the
collector must prove the exact attempt/receipt pair, valid COMMITTED artifacts
and metrics, reconciliation/history `0`, immutable source/binding/run/settings
identities and no unrelated write/delete. Any timeout, nonzero, USB loss,
missing post-readback, unknown cost, recovery or redaction failure is
`UNKNOWN`/`RECOVERY_REQUIRED` or `ACCEPTANCE_NOT_PROVEN`, with no expiry refresh
and no redispatch.

The historical owner decision was received for exactly one event and is now consumed by
the terminal Before-collector STOP above; account approval did not substitute
for the A4.3 decision. No live action remains issued by this document. The
previous next action was an offline diagnosis of
`P5E_COLLECTOR_ADB_NONZERO` for `database-wal-presence`, `database-shm-presence`
and `database-consistent-read-transaction` from the typed contract/evidence
only. Any future live event requires a new owner decision; no retry,
redispatch, account rerun or P6 transition is allowed.

## Historical baseline and evidence

Everything in this section is retained historical evidence; it is not the
current command, helper, artifact or owner-decision pin.

## Historical 2026-09-15 local closure claims — superseded for readiness

The final local chain binds the unchanged manifest, frozen production code207
and AndroidTest 57EC99 pins to the review-only command and helper/collector.
The helper hash is checked by the command and again before environment/device
access. `CollectReadback` has explicit `Before` and `After` modes and performs
only allowlisted read-only package/APK/certificate, WAL-aware SQLite and
metadata/lineage queries. It emits typed `NO_CLAIM_OBSERVED`,
`EXTERNAL_CALL_STATE_UNKNOWN`, `RECOVERY_REQUIRED` or `COLLECTOR_TYPED_STOP`
outcomes; the same event's After collection remains the recovery path after a
timeout, exception or reconnect and never redispatches.
The collector also writes a bounded same-event `COLLECTOR_COMMAND_LOG.jsonl`
containing only allowlisted operation class, numeric exit code, launch count
and timeout state; it does not write argv or command output.

Report and receipt are separate source-derived contracts. The contract file is
`docs/P5E_PRODUCTION_ARTIFACT_CONTRACT_20260915.json`, SHA-256
`FFE70A70E622706FABFA49D5843310ECD5A283B1CA114E32C636EA26B9FAE4BF`, pinned
to serializer source SHA-256
`1222B8AC9B79DAFC659DD364F50849DFBA4782C181606A92DA47EBD8C6164E3C`.
The production serializer golden test source is present at
`editorial-engine/src/test/java/com/ml/tblandroidtxt/editorial/pack/EditorialP5PilotExecutionBoundaryTest.java`
(SHA-256 `8977BC825A3E1DDF73D5D049BD27737755F1B11160CD684EFC185A32460E77BE`),
but it was not executed because this request forbids build. The offline host
probe still rejects 11 explicit bytes/shape mutations; those cases are marked
`HOST_SYNTHETIC_SHAPE_REGRESSION_ONLY`, not production golden evidence.

The historical technical result is
`docs/P5E_LOCAL_EVIDENCE_CHAIN_RESULT_20260915.json`. It records H1–H4
resolved assertions, self-test/probe/typed-stop evidence and zero device,
provider and credential actions. This local result does not establish a live
installed-package fact, RAW acceptance, P5 exit or P6 readiness. Its historical
SHA-256 is
`ECD61953C8E9C4E4539EC5B2B5865EDBC08D686E37F4C4743D67084887D8E725`.

The historical host-preparation document is
`docs/P5E_RAW_HOST_PREPARATION_20260915.md`, SHA-256
`4A0E678ED298F4FF879062C9FD5F81A383D235F1C27ED4EAE7CA15563DEB6797`.

Canonical plan: EDITORIAL_RECOVERY_V4_18.md
Historical host-repair branch: feature/v4.18-p5e-audit-20260914
Historical host-repair HEAD before this evidence group: 0f52d36e516560bb33d294303c70fa1753cb64f9
Historical host-preparation commit: c2c79a19842f551fc752a53328024aab8ddb529d
Historical HEAD that created this proposal: c1e3ec6eec62e38a1e2f4fdb0d0f151d5efdcfae
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

The earlier bounded host-preparation evidence is retained as historical input:
`docs/P5E_RAW_HOST_PREPARATION_20260915.md`, SHA-256
`728E1F3986A1C7E921CD9DD5B3199BD8F1B36E8699EF9050C4306D38DABF5703`.
It records the F2 RED-to-GREEN process-argv/remote-shell proof, the source
required-argument contract, the F3 one-launch supervisor, the repaired
producer-to-verifier provenance result and the unresolved F1 owner decision.
The hash above is not the current packet hash. The current local result is the
hash-bound evidence for preparation only; it does not change the manifest hash,
authorize dispatch or establish a RAW predecessor.

## Historical local F3 fixture evidence (superseded as current narrative)

The current helper adds `Invoke-P5ESyntheticReadbackCollector`, which consumes
only disposable `collector-input.json`, WAL-aware `before-snapshot.json` and
`after-snapshot.json`, `transaction-evidence.json`, `report.bin` and
`receipt.bin`, then emits `post-readback.json`. It validates actual serialized
bytes and computes their hashes/lengths; it does not accept caller-supplied
`validationPassed`, `atomicClaim` or `allowedDiff` values as evidence. The
collector and verifier bind event id, run identity, canonical paths, all source
hashes, collector implementation hash, attempt/auth/lifecycle timestamps and
the source mapping `p5e.raw.readback.source-map.v1`.

This historical fixture used
`0353d751924d02ef0928bb6460c4ab894fee7c6324506e62b2972090e519c4da` as its
input-scope/legacy manifest value; that value is not the current pack-manifest
authority. The production pack identity is
`3e88503e312db8da351ca574820c98216ab6fd3fa233e35aedb0db379e50013a`, as
defined by `EditorialP5PilotRequest.manifestFingerprint()`; current report and
receipt validators must use that pack value while retaining the separate
input-scope value. It is distinct from `endpointAccountFingerprint`, which
remains pending owner provenance. The consume gate is `issued <= consumed <
expires`; post-readback may be later than expiry when the durable chronology is
valid. The tracked result
`docs/P5E_PROVENANCE_REVIEW_RESULT.json` records two accepted controls, all
four repaired mutations rejected, producer and verifier negative fixtures
rejected, redaction success, `deviceActions=0`, `providerCalls=0` and
`p6Ready=false`.

This is synthetic offline producer evidence only. It is not a device DB
collector, does not read settings or credentials, and does not authorize an
instrumentation/provider call. A future live run must still prove the same
mapping from an owner-permitted read-only device snapshot for the exact event.

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

## Owner provenance input — not an authorization

This template remains non-secret and unapproved until the owner supplies a
trusted source and an exact scope. No API key, raw endpoint or actual
fingerprint belongs in chat, Git or this proposal.

| Field | Current value / required input |
|---|---|
| Decision | `NOT_APPROVED` |
| Owner/authorized operator | `NOT_PROVIDED` — responsible person for account and scope |
| Account/project label | `NOT_PROVIDED` — non-secret distinguishing label |
| Credential reference | `NOT_PROVIDED` — label/version only, never credential value |
| Expected source type | `NOT_PROVIDED` — trusted prior record or separately approved enrollment |
| Source reference + created/verified time | `NOT_PROVIDED` — non-secret reference, time and verification method |
| Algorithm/version | source-defined SHA-256/UTF-8/`normalizeEndpoint`/newline; exact implementation pinned |
| Account mapping attestation | `NOT_PROVIDED` — basis that the credential reference maps to the approved account/project |
| Fingerprint transport | `NOT_PROVIDED` — owner-controlled process-only lowercase 64-hex value; no chat/log/Git |
| Credential rotation since verification | `UNKNOWN` — reverify after endpoint/key rotation |
| Approved account operation | `NOT_APPROVED` — exact device-only load/normalize/hash/compare and redaction |
| Approved data egress | `NOT_APPROVED` — RAW/GLOSSARY only; primary 1, repair 0, retry 0, no RECONCILE |
| Approved artifact/code/command/helper refs | `PENDING_OWNER_DECISION` — manifest `412790E2E55A8289FF170D3EE93B683553468ADE252EF5C565D833599E5F5EA3`, command `51A71D47BBFC91DE19658FEEA120CF419ECD0F2FFD77562D74B83C73F9A98AB5`, root helper `CB9C07312E025DA94D8DB4840B7600CE9A3613DB75CC0E0E4F28EB059E00901F`, contract `FFE70A70E622706FABFA49D5843310ECD5A283B1CA114E32C636EA26B9FAE4BF`, serializer `1222B8AC9B79DAFC659DD364F50849DFBA4782C181606A92DA47EBD8C6164E3C`, QA `D124711219DFC1FD205CDB9205C394E2DA4CB3569827CA6C9D3B0A592EEC7050`; owner must approve these exact final refs or stop |
| Validity and stop conditions | `PENDING_DECISION` — expiry, mismatch, unavailable device, unknown outcome, no redispatch |

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

Historical 2026-09-15 host evidence below retains its original pins. The
current implementation and behavioral evidence are in
`scripts/p5e-raw-live-supervisor.ps1`, SHA-256
`364A6AA2C52A90E7AD20F28EC6C46A0EAD1BA39E8909727BEA7396287896FFE7`.
The repaired command wrapper is
`docs/P5E_RAW_AUTHORIZATION_COMMAND.txt`, SHA-256
`47044AB73C0B76A00E3E40A85D6E893B0F94C015F6332036484EA5ABB5FA55AB`.
The separate production report/receipt contract is
`docs/P5E_PRODUCTION_ARTIFACT_CONTRACT_20260915.json`, SHA-256
`FFE70A70E622706FABFA49D5843310ECD5A283B1CA114E32C636EA26B9FAE4BF`;
serializer source SHA-256 is
`1222B8AC9B79DAFC659DD364F50849DFBA4782C181606A92DA47EBD8C6164E3C`.
Current local result is `docs/P5E_SQL_BEHAVIORAL_RESULT_20260916.json`, SHA-256
`C3B7B7C7B86580CF56A810E4ECBA623A54B45F123B92C8A7753484EC54A45B48`.
The evidence records F2 RED-to-GREEN transport through fake process argv and
the ADB/shell model, source-derived required arguments, F3 numeric outcomes,
the executable same-event collector path, separate artifact contract and
independent readback acceptance. The current local result is
`docs/P5E_LOCAL_EVIDENCE_CHAIN_RESULT_20260915.json`; its synthetic 11-case
artifact matrix is explicitly regression-only, `p6Ready=false`, and no live
result is claimed.
The probe wrapper SHA-256 is
`1BDE695E13C7A84AB48A44B6074EA9441F0A363AF43FD20448B2995FCE9DAD45`.

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

## Conditional pre-dispatch checklist — owner decision still required

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

Owner decision request status is `PENDING`; this document is the final local
hash-bound packet for review, not an authorization. The historical host
preparation and this audit do not grant permission. The owner must separately
confirm the trusted expected fingerprint source/account mapping and permit the
exact memory-only account check, read-only collection and RAW/GLOSSARY egress.
No dispatch occurs before that decision.

Owner decision: PENDING / NOT_APPROVED
endpointAccountFingerprint: PENDING_OWNER_VERIFICATION
Runtime authorization: NOT CREATED
Provider dispatch: NOT PERFORMED
