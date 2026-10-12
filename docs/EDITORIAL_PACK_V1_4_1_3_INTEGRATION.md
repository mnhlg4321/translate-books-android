# Editorial Pack Manifest v1 — V5-SAFE.4.1.3-FULL integration contract

> Scope note (2026-09-14): this is the frozen P0 ABI design, not a current implementation-status report. P0-P4 results and current P5E readiness are governed by EDITORIAL_RECOVERY_V4_18.md. Its generic repair/retry mappings do not override the current RAW one-shot cap of zero repairs and zero retries.


Status: `P0 CONTRACT FROZEN / IMPLEMENTATION NOT STARTED`

## 1. Decision

V4.18 reuses the existing `com.ml.tblandroidtxt.editorial-pack` manifest version 1 and ZIP importer. It does not create a second package format.

An installable pack remains exactly four root entries:

```text
editorial-pack.json
project.txt
prompt.txt
workflow.txt
```

All entries are root-relative. Directory entries, absolute paths, alternate separators, `..`, duplicate paths/roles, symbolic links, unlisted files and executable payloads are invalid.

The external `APP`, `COMMON` and `TESTS` files define implementation requirements and qualification vectors. They are not copied into an Android import ZIP and are never executed by the app.

## 2. Why this is the minimum sufficient design

CODE169 already contains:

- canonical JSON parsing and duplicate-key rejection;
- a manifest with contract/schema/minimum-engine/capability declarations;
- exactly three typed file roles;
- byte-length, encoding and SHA-256 validation;
- canonical pack identity;
- ZIP count/size/compression/path/symlink controls;
- one-pass stream snapshotting;
- content-addressed immutable storage;
- compatibility evaluation and `ENGINE_UPGRADE_REQUIRED`;
- idempotent import and no automatic project rebind.

Replacing this with a new multi-file `.tbepack` ABI would duplicate proven work and increase migration/test scope. The 4.1.3 APP specification should instead be represented by a trusted engine contract and capabilities implemented once in the APK.

## 3. 4.1.3 role mapping

| Pack role | ZIP path | Exact external source | SHA-256 |
|---|---|---|---|
| `PROJECT_INSTRUCTION` | `project.txt` | `CHATGPT/PROJECT_INSTRUCTION_BIEN_TAP_V5_SAFE_4_1_3_FULL.txt` | `1727ae173f2cfd530eb818cae69e0d3fadc59c35e3b5b6d478704a02091a26ad` |
| `TURN_PROMPT` | `prompt.txt` | `CHATGPT/PROMPT_DAU_CHAT_3_LUOT_V5_SAFE_4_1_3_FULL.txt` | `d25757d1a6bdddd5962a3b178b9ef850727573ae0c34867ec8f4b8450c7cd754f` |
| `WORKFLOW` | `workflow.txt` | `CHATGPT/WORKFLOW_BIEN_TAP_3_LUOT_V5_SAFE_4_1_3_FULL.txt` | `5db6b4f6509313f106499113537d2880bc6d2ff663859239dafb285557505730` |

The three authority files remain byte-exact. The importer does not rewrite BOM/newlines or derive identity from normalized display text.

## 4. Proposed manifest identity

The exact final manifest is generated in P2 after P1 characterization. P0 freezes these semantic values:

| Field | Proposed value |
|---|---|
| `manifestFormat` | `com.ml.tblandroidtxt.editorial-pack` |
| `manifestVersion` | `1` |
| `packId` | `com.ml.tblandroidtxt.editorial.safe4.full` |
| `version` | `4.1.3` |
| `displayName` | `Biên tập V5-SAFE.4.1.3-FULL` |
| `contractVersion` | `safe4.full.three-pass.v1` |
| `schemaVersion` | `safe4.full.receipt.v1` |
| `minimumEngineVersion` | determined from the v4.18 engine profile, never from the app marketing version |
| `compatibilityClass` | `DATA_COMPATIBLE` only after the exact trusted profile is implemented and qualified |
| `adapterId` | omitted when the installed engine natively represents the contract; otherwise an exact bundled adapter ID |
| `migrationPolicy` | automatic project upgrade `false`; project rebind `false` |

The current canonical hash algorithm remains authoritative. `canonicalPackHash` identifies the canonical manifest declarations; each authority file is independently hash-bound through `fileRoles`.

## 5. Runtime capability boundary

The trusted v4.18 engine profile should use stable semantic capabilities rather than pack-version checks. Proposed required capability set:

- `pack.integrity.sha256.v1`
- `source.preflight.safe4-full.v1`
- `bundle.phase-visibility.safe4-full.v1`
- `status.glossary-pronoun.safe4-full.v1`
- `ledger.exhaustive.safe4-full.v1`
- `preserve.draft.safe4-full.v1`
- `stop.typed.safe4-full.v1`
- `diff.change-coverage.v1`
- `qa.l3-two-adversarial.v1`
- `release.safe4-full.v1`
- `replay.safe4.g1-g24.v1`

`resume.shard.safe4-full.v1` is optional until a real chapter pilot proves output truncation requires it. It must not become an MVP dependency merely because the external release contains a shard specification.

Compatibility rules:

| Pack condition | Result |
|---|---|
| Exact contract/schema and all required capabilities supported | `DATA_COMPATIBLE` |
| Exact known adapter exists but is not installed | `ADAPTER_REQUIRED` |
| Unknown required capability, phase or machine contract | `ENGINE_UPGRADE_REQUIRED` |
| Hash/schema/path/manifest invalid | `INVALID` |
| Valid pack with incomplete qualification evidence | stored but not executable/certified |

No filename or display version alone can grant compatibility.

## 6. Input and source-mode contract

Normal L1 requires exactly one each of:

- RAW;
- original DRAFT;
- five-column GLOSSARY;
- FINAL PRONOUN-CSV/3.1 seven-column PRONOUN.

An alternate mode is selected only by an explicit user action before L1. The app records effective Glossary/Pronoun status and origin in the immutable run manifest. Absence never silently becomes `AVAILABLE` or `NONE`.

Full-bundle validation and phase visibility are separate operations:

1. Validate inventory, byte access, encoding/schema and byte identity for the selected mode.
2. Pin the immutable run manifest.
3. Build the phase projection.
4. Validate only the assets required to be visible in that phase.

A hidden DRAFT/PRONOUN/PAIR_CONTEXT is not missing or forbidden. Pair Context is optional for every Pronoun status and never gains authority from mere presence.

## 7. Three-pass transport

The imported `prompt.txt` remains the complete three-turn authority. The app does not parse prose headings to invent a new workflow. It supplies a stable app-owned phase activation envelope containing:

- `RUN_PHASE=L1|L2|L3`;
- exact manifest reference;
- selected phase projection;
- stable anchors/populations;
- expected output schema identifier.

Project and Workflow are loaded in full as required by 4.1.3. Whether provider conversation state is retained or the app sends a complete stateless context is a transport decision; output identity and predecessor validation must be identical in both cases.

## 8. False-block prevention

The app, not the model, owns:

- source preflight;
- byte hashes and immutable identity;
- status resolution for deterministic/user-selected cases;
- full-bundle versus phase visibility;
- stable unit/anchor IDs;
- declared population totals;
- actual DRAFT-to-L2 and L2-to-FINAL diffs;
- receipt schema/equation validation;
- state transition and recovery class.

The model owns semantic risk, relation/speaker evidence, fidelity, naturalness and proposed edits. A literal model label `PASS` or `BLOCKED` cannot directly commit or halt runtime state.

Decision mapping:

| Condition | Runtime class | Recovery |
|---|---|---|
| Missing/invalid source or predecessor | `INPUT_REQUIRED` | replace/select exact source and rerun preflight |
| Complete semantic result rendered in invalid schema | `REPAIR_REQUIRED` | one bounded schema-only repair |
| Source bytes unavailable or output truncated | `RETRY_REQUIRED` | retry/resume same manifest and scope |
| Proven unresolved fidelity/relation/change/protected-span conflict | `CONTENT_BLOCKED` | correct or revert exact affected population |
| Semantic evidence insufficient but unchanged draft is safe | non-stop `PRESERVE_DRAFT` | record evidence limit; no canon/propagation |

Generic naked `BLOCKED`, unlimited repair loops and partial turn commits are forbidden.

## 9. Import, storage and update behavior

1. Import through Android SAF.
2. Snapshot source stream once into owned staging.
3. Validate ZIP limits and root entries.
4. Parse canonical manifest and verify all three files.
5. Evaluate against the bundled trusted engine profile.
6. Move atomically to immutable content-addressed storage.
7. Store side-by-side by identity/hash.
8. Require explicit project selection; do not auto-activate or auto-rebind.

A running chain pins `packId`, version, canonical pack hash, contract/schema and the three file hashes. A later pack begins a new L1 chain. If a referenced pack is unavailable, the project remains inspectable/read-only rather than silently switching versions.

## 10. No-rebuild guarantee and limit

The same APK may import a later pack without rebuild when the later pack:

- uses manifest version 1;
- remains on `safe4.full.three-pass.v1` and a supported receipt schema;
- requests only installed capabilities;
- changes only authority bytes/declarations allowed by the existing contract;
- passes integrity, compatibility and local qualification.

A rebuild is required when a pack introduces a required new input parser, execution phase, output protocol, anchor/reducer algorithm, provider tool, database primitive or security capability. The expected user-facing result is `ENGINE_UPGRADE_REQUIRED`, not crash or generic block.

## 11. P1 proof obligations

Before production code changes:

- create a test-only four-entry 4.1.3 ZIP;
- run existing manifest/integrity/compatibility/import/storage tests;
- prove whether existing `inputRoles`, `pronounPolicy`, `phaseGraph`, `contextAllowList`, `evidenceSchemas`, `gateDefinitions`, `releaseArtifacts` and `goldenReplayCases` fully represent 4.1.3;
- list any unrepresentable rule with a concrete fixture;
- prove a synthetic 4.1.4 text-only update is data-compatible;
- prove an unknown required capability becomes `ENGINE_UPGRADE_REQUIRED`.

Only demonstrated gaps may expand the manifest/runtime contract.
