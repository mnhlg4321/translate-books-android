# Editorial v4.18 implementation and verification map

> Scope note (2026-09-14): the baseline and proposed implementation map below describe P0 at code169, not current device/build readiness. Current candidate is code207; use EDITORIAL_RECOVERY_V4_18.md and WORKSPACE_SNAPSHOT.md for actual pins/phase/next action. Do not restart P1 from the historical map.


Status: `P0 MAP COMPLETE / NO PRODUCTION CHANGE`

## 1. Baseline

- Source baseline: v4.17 verified HEAD `921af9256e1b1fe4ab9ac113affa98eec7a1e339`.
- Development branch: `feature/v4.18`.
- Current APK baseline: `4.17-dev.1` / code169.
- Editorial authority target: `V5-SAFE.4.1.3-FULL`.
- Next build code: greater than 169; no build is authorized by this P0 map.

## 2. Reuse-first architecture map

| Existing area | Current value | Planned treatment | Primary verification |
|---|---|---|---|
| `EditorialPackManifest` | Strict canonical manifest v1 with contract/schema/capabilities and three file roles | Reuse unchanged in P1; extend only for a proven 4.1.3 gap | Manifest positive/negative fixtures |
| `EditorialPackIntegrityValidator` | Per-file length/hash and canonical pack hash | Reuse | One-byte, missing/extra file, wrong hash |
| `EditorialPackImportService` | ZIP limits, one-pass snapshot, staging, immutable move and recovery | Reuse | Truncation, zip-slip, duplicate, bomb limits, recovery |
| `EditorialPackStorageLayout` | Content-addressed owned paths | Reuse | Traversal and lowercase hash tests |
| `EditorialCompatibilityEvaluator` | Contract/schema/minimum-engine/capability/adapter negotiation | Reuse | Compatible, adapter, engine-upgrade outcomes |
| `SqliteEditorialPackRegistry` | Persisted pack and compatibility facts | Reuse; no schema change until test proves need | Idempotent import/readback |
| Bundled engine profile v1 | Only integrity capability is trusted | Replace with a new qualified profile or add a new profile version; do not falsely edit capability flags | Exact profile fingerprint/evidence tests |
| `EditorialSafe4Pack` | Hardcoded old V5-SAFE.4 assets and execution lock | Preserve as legacy bootstrap/read-only; stop using its version boolean as authority for imported-pack execution | Legacy and activation-separation tests |
| `EditorialSafe4Workflow` | Useful roles/gates but conflates some phase input rules and old G1-G10 | Adapt behind the new contract only after P1 gap proof | G1-G24 and full-bundle/visibility tests |
| `EditorialRepository` | Project/chapter/input snapshots | Reuse minimal persistence and exact pack binding; no auto-rebind | Restart, stale input and identity tests |
| `EditorialPageFactory` | Setup/import preview with execution disabled | Incrementally expose compatible pack selection and typed preflight; keep Run disabled until P5 | UI state matrix/device tests |
| `MainActivity` composition | Existing SAF/import and Editorial routes | Wire only qualified services; avoid new provider framework | Composition and no-call-before-preflight tests |
| `app/build.gradle` old asset verifier | Protects immutable legacy bundled SAFE4 files | Keep legacy verification; imported 4.1.3 identity is runtime manifest-driven, not another hardcoded asset table | Pre-build and APK asset inspection |

## 3. Minimal new runtime seams

Names are provisional; behavior is fixed before names:

| Responsibility | Preferred seam | Must not do |
|---|---|---|
| Four-source preflight | `EditorialSourcePreflight` | Read semantic RAW or call provider before pass |
| User-selected source mode/status | `EditorialSourceModeResolver` | Infer absent Pronoun silently |
| Bundle-to-phase projection | `EditorialPhaseContextProjector` | Treat hidden assets as missing/forbidden |
| Stable run identity | existing project/run manifest types where sufficient | Let model compute hashes or renumber anchors |
| Typed stop/recovery | `EditorialStopDecision` | Collapse all failures to `BLOCKED` |
| Receipt/equation validation | `EditorialReceiptValidator` | Trust model PASS label |
| Three-turn orchestration | `EditorialThreePassRunner` | Commit partial outputs or loop repair indefinitely |

No new class is created until P1 confirms an existing class cannot own the responsibility cleanly.

## 4. Ordered commit groups

| Group | Scope | Production change allowed? | Exit evidence |
|---|---|---:|---|
| D0 | Plan, ABI, map, checklist, README/state | No | Document consistency and clean diff check |
| D1 | Characterization fixtures/tests and test-only 4.1.3 ZIP | No | Existing platform baseline and gap matrix |
| D2 | Canonical 4.1.3 reference pack/test assets | Test/resource only first | Import/integrity/compatibility PASS |
| D3 | Minimal proven runtime contract changes | Yes, only demonstrated gaps | Preflight/status/visibility/receipt cases PASS |
| D4 | Trusted engine profile and pack certification/read path | Yes | 4.1.3 + synthetic compatible update, unknown-required rejection |
| D5 | Project binding, setup and resume UI | Yes | Restart/stale/side-by-side/device UI evidence |
| D6 | Controlled L1 then L2/L3 execution | Yes | Real chapter pilot, no unexplained false block |
| D7 | Regression, numbered build and QA | Build metadata only after tests | Full regression, archive parity and device QA |

Each production group is a separate commit. Test repairs stay in the same group rather than opening new branches/plans.

## 5. P1 characterization matrix

| Question | Fixture/test | Pass condition |
|---|---|---|
| Can exact 4.1.3 authority fit the existing four-root ZIP? | valid reference ZIP | Import snapshots exactly 4 entries and verifies all hashes |
| Can normal four-source mode be represented? | manifest/input-role fixture | RAW/DRAFT/GLOSSARY/PRONOUN are required before L1 model work |
| Can explicit alternate mode be represented without a new ABI? | NONE/LEGACY fixture | User decision is persisted; no silent inference |
| Is full bundle separate from phase visibility? | G17 | Hidden DRAFT/Pronoun do not trigger forbidden/missing errors |
| Is Pair Context optional? | G18 | Absence never blocks any Pronoun status |
| Does uncertainty preserve rather than block? | G19/G24 | Processed preserve entry is no-canon/no-propagation |
| Are format/truncation errors recoverable? | G20/G22 | REPAIR/RETRY with same manifest and bounded resume |
| Does a compatible future version avoid rebuild? | synthetic 4.1.4 | Same engine profile returns DATA_COMPATIBLE |
| Does unknown behavior fail clearly? | unknown required capability | ENGINE_UPGRADE_REQUIRED |

## 6. Runtime acceptance map

### Preflight

- Exact inventory for the selected mode.
- Byte access proven, not inferred from an attachment handle.
- UTF-8/format/schema validated before semantic work.
- Byte hashes owned by app.
- Preflight failure produces one typed stop receipt; downstream gates are `NOT_EVALUATED`.

### L1

- Raw-first ordering and stable app-owned populations.
- No partial report committed.
- Preserved uncertainty is processed but cannot become canon.

### L2

- Exact predecessor and pack identity.
- Every actual edit maps to a declared change/Error ID.
- VI_L2 and CHANGE_MAP_L2 commit atomically.

### L3

- Independent raw-first reaudit ordering.
- Two adversarial passes.
- App reconstructs actual diffs.
- Five release numbers derive to zero before export.
- FINAL and QA_RECEIPT commit atomically.

## 7. Regression ownership

| Risk | Required regression |
|---|---|
| V4.17 Translation profile regression | Existing Glossary4, Pronoun7, D4 Mercedes and full JVM tests |
| Legacy Editorial data | Old V5-SAFE.4 remains read-only and hash-stable |
| ZIP/import security | Existing import/storage tests plus 4.1.3 payload cases |
| False block | G13-G24, zero-population, zero-edit and typed recovery cases |
| State loss | Process death/reopen between every turn |
| Pack update drift | Side-by-side 4.1.3/compatible-next, no auto-rebind |
| Unknown future contract | ENGINE_UPGRADE_REQUIRED with exact missing capability |
| Provider boundary | Zero provider calls before preflight/certification; one active turn writer |

## 8. Documentation ownership

- `EDITORIAL_RECOVERY_V4_18.md`: only active scope/order/next action.
- `docs/EDITORIAL_PACK_V1_4_1_3_INTEGRATION.md`: stable package/runtime compatibility contract.
- This file: implementation/test ownership map; update only when ownership materially changes.
- `BUILD_STATE.md`: current facts only.
- `WORKSPACE_SNAPSHOT.md`: current handoff only.
- Release checklist: evidence for the 14 release gates.
- Historical Editorial/RSC/IPC documents remain untouched.

## 9. Stop conditions

Stop dependent work and record evidence when:

- CODE169 regression changes before Editorial production work;
- exact 4.1.3 source hash differs;
- current manifest cannot express a requirement without changing its canonical semantics;
- a test demonstrates the existing importer mutates or loses bytes;
- a proposed implementation activates provider/runtime before preflight/certification;
- a database migration is proposed without a failing persistence test;
- a future-pack claim relies on filename/version rather than contract/capabilities.

Local test failures are repaired within the same branch/group; they do not justify another plan or platform.
