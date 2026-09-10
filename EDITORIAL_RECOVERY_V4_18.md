# Editorial Recovery v4.18

Status: `ACTIVE / P5E_LOCAL_QA_COMPLETE / ORIGINAL_PILOT_DATA_PRESERVATION_FAILED / FRESH_PILOT_REQUIRES_OWNER_APPROVAL / LIVE_PROVIDER_DISABLED`

This document is the single product and execution authority for the v4.18 Editorial recovery. It supersedes active next actions from the historical v4.16 Editorial/RSC/IPC tracks without deleting or reinterpreting their evidence.

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

Run P1 characterization on the clean `feature/v4.18` branch: build a test-only four-entry 4.1.3 pack fixture and execute the existing manifest/integrity/compatibility/import tests. Do not modify production code until the resulting gap matrix proves a change is required.
