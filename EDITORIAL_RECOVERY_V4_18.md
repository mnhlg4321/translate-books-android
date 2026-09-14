# Editorial Recovery v4.18

Current proposal gate: RAW_AUTHORIZATION_PROPOSAL_PREPARED / OWNER_DECISION_REQUIRED / ENDPOINT_ACCOUNT_FINGERPRINT_PENDING / NOT_READY_FOR_DISPATCH / NO_RUNTIME_AUTHORIZATION_CREATED / NO_LIVE_CALL_PERFORMED.

Status: `ACTIVE / P0_P4_COMPLETE / P5_P5E_INCOMPLETE / A2_FAIL_CLOSED / P5E_9B_A3_1_TECHNICAL_PASS / P5E_9B_A3_1R_DOCUMENTATION_AND_EVIDENCE_PASS / P5E_WORKFLOW_PRETAG_FAIL_CLOSED / P5E_9B_A3_2_MODEL_MISMATCH_OBSERVED_HISTORICAL / P5E_9B_A3_2_PRESERVATION_NOT_PROVEN_HISTORICAL / P5E_9B_A4_MODEL_REMEDIATION_PASS_HISTORICAL / P5E_9B_A4_EXACT_PREFLIGHT_EVIDENCE_CHANNEL_FAILED_HISTORICAL / P5E_9B_A4_1_HOST_CONTRACT_PASS / P5E_9B_A4_1_TEST_ARTIFACT_BUILT_NOT_INSTALLED / P5E_9B_A4_2_EXACT_PREFLIGHT_PASS / FRESH_RAW_EXACT_PREFLIGHT_READY / RAW_AUTHORIZATION_REQUIRED / NO_AUTHORIZATION_CREATED / NO_LIVE_CALL_PERFORMED / PROVIDER_CALLS_ZERO / RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED / EXECUTION_DISABLED / NOT_CERTIFIED / P6_NOT_READY`

This document is the single product and execution authority for the v4.18 Editorial recovery. It supersedes active next actions from the historical v4.16 Editorial/RSC/IPC tracks without deleting or reinterpreting their evidence.

## Current active boundary — P5E.9B-A4.2 exact preflight pass; RAW proposal prepared, not issued

A4.1 was host-only. A4.2 then used the separately approved single-use device
boundary from execution-start HEAD
`005317cd83f107edbf275734cb2977b9929e88ce` on branch
`fix/v4.18-p5e-9b-a4-1`; the AndroidTest source/archive commit remains
`d51b7f3c16bdc482513b9904db07b97daed592d1`. The test package was replaced
exactly once and the exact preflight method ran exactly once. Production was
not installed or modified; no provider/API call, authorization, attempt,
reconciliation, settings write or database write occurred.

### Canonical current pin table

| Pin | Current value | Qualification |
|---|---|---|
| Device | `15e84958` | A4.2 pre/post read-only verification matched |
| Production | `com.ml.tblandroidtxt` / `v4.17-p5e.11` / code `207` | frozen candidate; not rebuilt |
| Production APK SHA-256 | `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD` | frozen artifact/backup fact |
| Production source ZIP SHA-256 | `B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348` | frozen artifact/backup fact |
| Certificate SHA-256 | `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` | frozen candidate fact |
| A4.1 test artifact | `D:\App Translate Books\App Translate Books-translation-profile\artifacts\test-builds\v4.17-p5e.11\a4-1-test-20260914-065532\app-debug-androidTest.apk` | installed once as test package under A4.2; post-install/readback exact |
| A4.1 test APK SHA-256 / bytes | `57EC99A95EE2DC0F1759934C62CEA39E2EC92EB77C3DAF76CFEED28D41A2FDEA` / `1155224` | artifact and backup match |
| A4.1 source/archive commit | `d51b7f3c16bdc482513b9904db07b97daed592d1` | exact tracked-source ZIP in payload |
| A4.1 test certificate/package/target/runner | `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` / `com.ml.tblandroidtxt.test` / `com.ml.tblandroidtxt` / `androidx.test.runner.AndroidJUnitRunner` | build-tool inspection |
| A4.1 source ZIP SHA-256 | `382EC5D12FC786BC358316434692E49A73BD62C2F9DD9AC6BD1BC9274CE0B3FA` | artifact/backup payload |
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
creation and reconciliation creation were all `0`. The single current next
action is owner review of the prepared A4.3 RAW authorization packet derived
from this manifest. It remains RAW-only with one primary call, zero
repair/retry and no RECONCILE. P5/P5E have not exited; execution,
certification and P6 remain
disabled. Historical `HISTORICAL_CODE196_PRESERVATION_FAILED`,
`PILOT_DATA_PRESERVATION_FAILED`, the A3.2 preservation gap and consumed
A2/A3.2 approvals remain unchanged.

## P5E.9B-A4.3 — owner-approval packet prepared, not issued

The fixed-scope owner packet is
docs/P5E_RAW_AUTHORIZATION_APPROVAL_MANIFEST.md, SHA-256
DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501.
The separate command is
docs/P5E_RAW_AUTHORIZATION_COMMAND.txt, SHA-256
31B075935CECA342A249961F8E871410A700478B780A827F25D21DDAEE13BE98.
The narrative proposal is
docs/P5E_RAW_AUTHORIZATION_PROPOSAL.md.

The packet preserves the harness authorization ID and the exact A4.2
identities. It requests only RAW/GLOSSARY egress to the pinned route, one
primary semantic call, zero schema repair, zero network retry and zero
RECONCILE. DRAFT and PRONOUN remain hidden from the model. The test package
and production code207/certificate pins remain unchanged.

endpointAccountFingerprint is not filled with a default or a settings-file
hash. Before any runtime authorization is constructed, the owner must permit
the source-defined in-memory operation:
SettingsStore.load(target).copy(), normalizeEndpoint(settings.baseUrl), then
SHA-256 of UTF-8 endpoint + newline + in-memory settings.apiKey. The credential
must remain in memory and never enter command text, logs or evidence. A
mismatch or unverifiable account stops before authorization creation or
dispatch. This operation has not been performed in this audit, so the packet
is not READY_FOR_APPROVAL or READY_FOR_DISPATCH.

The command computes fresh issuedAt/expiresAt values at owner-approved
dispatch, uses a 240000 ms host observation window, and must be run once only.
The selected live method performs its own preflightOnly checks internally; no
separate instrumentation preflight is permitted.

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

The current next action is owner review of the prepared A4.3 RAW authorization
packet derived from the A4.2 manifest. It remains single-use, RAW-only, one
primary call, zero repair/retry and no RECONCILE. The A4.2 approval is
consumed and not reusable. The endpoint account fingerprint is still pending
the separately permitted memory-only account check; P5/P5E exit, execution,
certification and P6 remain incomplete, and no RAW predecessor/report/receipt
exists.
