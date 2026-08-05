# EDITORIAL PACK PLATFORM — G2-C1B1-C1.5-A
# AUTHORITATIVE IDENTITY SCHEMA PLAN

## 1. Review-stop status

This document is the survey and schema design handoff for authoritative identity. It does not implement SQLite v17, a DAO, a migration, a runtime caller, a lineage row, a profile update, or capability promotion.

```text
G2-C1B1C15A_SCHEMA_PLAN: PASS
SQLITE_V17_IMPLEMENTATION: NOT_STARTED
LINEAGE_RUNTIME_CALLER_WIRING: NOT_STARTED
LINEAGE_CAPABILITY_PROMOTION: NOT_STARTED
SAFE4_EXECUTION_READINESS: BLOCKED
AUTHORITATIVE_IDENTITY_SCHEMA: REQUIRED
```

Baseline verified before opening this branch:

- Source branch before branch creation: `feature/v4.16-g2-c1b1c1`.
- Source HEAD before branch creation: `63d77ac267671f88ee2c9ebc31ebc8e575ec74cd`.
- Working branch for this review: `feature/v4.16-g2-c1b1c15a`.
- Implementation HEAD remains `63d77ac267671f88ee2c9ebc31ebc8e575ec74cd`; this phase is documentation-only.
- SQLite source version: v16.
- Latest accepted artifact: `4.16-dev.39`, Android version code `101`.
- Prior regression baseline: `:editorial-engine:test` 95/95 and `:app:testDebugUnitTest` 163/163; `executionEnabled()` remains `false`.
- Production evidence catalog still contains only `pack.integrity.sha256.v1`.
- The three user-owned `.idea/*` changes were observed before and after branch creation and were not edited, staged, stashed, or committed.

The final documentation commit is recorded by the phase checklist and final Git verification. It is intentionally not used as a semantic identity or as a self-referential `Current commit` value in the snapshot.

## 2. Scope and non-goals

The only subject is the future v17 schema boundary for:

1. immutable project revision identity;
2. immutable input-scope snapshot and complete role manifest;
3. immutable closed run/evaluation identity;
4. append-only run-to-lineage binding.

This phase does not:

- change `TranslationRepository`, `EditorialMigrationSpec`, or any database version;
- create a production DAO, canonicalizer, migration, row, or binding;
- connect `EditorialLineageRuntimeService` to a caller, importer, project repository, run repository, or SQLite;
- backfill old projects, chapters, assets, runs, evaluations, packs, or lineage;
- modify the bundled profile, evidence catalog, `EditorialSafe4Pack`, or `executionEnabled()`;
- certify, activate, bind a project, run a model, or run an APK/device migration.

The existing production profile remains non-executable and SAFE4 execution readiness remains `BLOCKED`.

## 3. Existing schema audit

### 3.1 v10 to v16 migration chain

The source of truth is `app/src/main/java/com/ml/tblandroidtxt/TranslationRepository.java`:

- `VER = 16`.
- `onCreate` creates the ordinary tables, editorial tables, pack tables, v15 compatibility-evaluation tables, and v16 lineage tables.
- `onUpgrade` applies `from10To11`, `from11To12`, `from12To13`, `from13To14`, `from14To15`, and `from15To16` when the old version is below the corresponding boundary.
- `onConfigure` enables SQLite foreign-key enforcement.
- `onDowngrade` throws `SQLiteException`; it does not destructively rewrite a database.
- `EditorialMigrationSpec` is the inspectable SQL source for those six editorial migration steps. Existing upgrade work is transaction-owned by `SQLiteOpenHelper`; v17 must retain that rollback behavior.

The v10-to-v11 editorial tables are defined in `EditorialMigrationSpec.from10To11()`:

| Table | Actual v16 shape relevant to identity | Identity conclusion |
|---|---|---|
| `editorial_projects` | `id INTEGER PRIMARY KEY AUTOINCREMENT`; `series_name`, `volume_name`, `workflow_version`, `workflow_hash`, `output_tree_uri`, `created_at`, `updated_at`; unique `(series_name, volume_name)` | The integer is a local lookup key. `series_name` and `volume_name` are mutable application fields, not a reviewed content-addressed project revision. `output_tree_uri` is machine-local path/URI data. |
| `editorial_chapters` | `id INTEGER PRIMARY KEY AUTOINCREMENT`; `project_id`; `chapter_key`; `title`; `state`; `raw_hash`; timestamps; FK to project | The integer is local. The row has one raw hash, not a complete role/ordinal/count manifest. `title`, `state`, and timestamps do not establish immutable scope identity. |
| `editorial_assets` | `id INTEGER PRIMARY KEY AUTOINCREMENT`; `chapter_id`; `role`; `source_uri`; `display_name`; `sha256`; `size_bytes`; `content`; `created_at`; unique `(chapter_id, role)` | This is a mutable/current asset store. URI, display name, and content are not a stable snapshot contract; role is not an ordered manifest; item count is absent. |
| `editorial_runs` | `id INTEGER PRIMARY KEY AUTOINCREMENT`; `chapter_id`; `run_kind`; `state`; provider/model; prompt/workflow hashes; `input_manifest_json`; timestamps; FK to chapter | The integer is local. The source has no production closed-context creator. `state` is mutable and `input_manifest_json` is legacy JSON without the reviewed version/role/ordinal/SHA-256/byte-count/item-count contract. |
| `editorial_scenes`, `editorial_gates`, `editorial_evidence` | Run-local rows keyed by local `run_id` | These are run outputs/evidence, not an authoritative closed-run identity source. |

`from11To12()` adds `editorial_project_assets` with an autoincrement ID and mutable project-level role/content/URI/display/size/hash fields. `from12To13()` adds `editorial_project_reference_profiles` with an autoincrement ID, mutable active selection and replace/delete behavior; it also copies legacy project assets into reference profiles. Neither table is an immutable project revision or a complete input-scope snapshot.

`from13To14()` adds the pack/import tables. `editorial_packs.canonical_pack_hash` is a durable pack identity and `editorial_pack_imports.import_id` is an operational import identity. Neither is a project identity or run identity. Pack rows and compatibility result rows are immutable, but import is intentionally outside the project/run lineage state machine.

`from14To15()` adds `editorial_pack_compatibility_evaluations`. Its `evaluation_id` is a stable evaluation-evidence identity and the row snapshots trusted profile ID/version/hash, machine fingerprint, pack hash, contract/evaluator/context fingerprints and outcome. It is authoritative for the compatibility evaluation it records, but it is not a run identity: it has no project revision, input-scope snapshot, closed-run attempt, or run-to-lineage binding.

`from15To16()` adds:

- `editorial_lineage_records`, keyed by `record_identity`, with immutable semantic fields and a self FK for exact parent identity;
- `editorial_lineage_input_entries`, keyed by `(record_identity, role, ordinal)`;
- hash/count checks, `ROOT`/`CHILD` checks, `ON DELETE RESTRICT`, indexes, and update/delete rejection triggers.

The v16 lineage tables are a persistence boundary, not an authoritative source for the missing project/chapter/run identities. They are not linked to mutable project/chapter/run rows or v15 evaluation rows. G2-C1B1-A/B therefore cannot manufacture the missing source facts.

### 3.2 Repository and API evidence

`app/src/main/java/com/ml/tblandroidtxt/EditorialRepository.java` confirms:

- `Project`, `Chapter`, and `AssetSnapshot` are mutable application objects;
- `createProject()` inserts an autoincrement project row;
- `createChapter()` inserts a local chapter row and current asset rows in one transaction;
- `updateProjectIdentity()` changes `series_name`, `volume_name`, and `updated_at` in place;
- reference-profile selection uses update/replace semantics;
- `deleteProject()` explicitly deletes runs, assets, chapters, project assets, reference profiles, and the project;
- no production method creates or closes an `editorial_runs` row. Source search finds the run table DDL and deletion path, but no production closed-run creator.

`app/src/main/java/com/ml/tblandroidtxt/EditorialAssetManifest.java` only validates required roles and nonempty SHA-256 values. It does not provide manifest version, ordinal, byte count, item count, a canonical projection, or an immutable scope row. Its current required role set is RAW, DRAFT, and GLOSSARY, with PRONOUN and PAIR_CONTEXT allowed conditionally; the reviewed future role contract must remain the authority rather than a new schema default.

`EditorialPackImportService.importPack(Collection<EditorialPackImportEntry>)` and `importZip(InputStream)` receive pack entries/path/stream/length information only. They do not receive project identity, chapter/input-scope identity, complete input manifest, closed-run identity, or exact parent. Pack import therefore cannot enter the lineage state machine.

### 3.3 Explicit audit conclusions

The following are facts, not inferred identities:

- AUTOINCREMENT IDs are local lookup/provenance keys only.
- Current project, chapter, asset, project-asset, and reference-profile rows are mutable or deletable.
- `editorial_runs` has no production closed-context creator and its old manifest JSON is insufficient.
- There is no immutable project revision.
- There is no complete immutable input-scope snapshot.
- There is no stable closed-run semantic identity.
- There is no run-to-lineage binding.
- A v16 lineage field named `project_identity`, `input_scope_identity`, or `run_evaluation_identity` must not be populated from a local row ID, pack ID, import ID, compatibility evaluation ID, empty string, `UNKNOWN`, placeholder, or random UUID.
- Existing rows must be treated as `IDENTITY_UNATTESTED` until an explicit new preparation/closure flow produces the v17 facts.

## 4. Identity state machine

The proposed state machine is event-bound and append-only:

```text
MUTABLE_PROJECT_PREPARATION
        |
        | append reviewed semantic project definition
        v
IMMUTABLE_PROJECT_REVISION
        |
        | append complete role/ordinal/hash/count snapshot
        v
IMMUTABLE_INPUT_SCOPE_SNAPSHOT
        |
        | close a run from exact trusted context and frozen snapshot
        v
RUN_CONTEXT_CLOSED
        |
        | C1 pure-JVM prepare/validate only
        v
LINEAGE_READY_TO_APPEND
        |
        | future v16 lineage append + v17 binding in one transaction
        v
LINEAGE_APPENDED_AND_BOUND
```

Rules:

1. `MUTABLE_PROJECT_PREPARATION` is the existing UI/repository preparation area. It cannot produce lineage.
2. A semantic change produces a new immutable project revision. It never updates an old revision.
3. A changed asset/input produces a new scope snapshot tied to a new or existing reviewed project revision as appropriate. It never edits an old snapshot.
4. Only `RUN_CONTEXT_CLOSED` is allowed to call the C1 service. The service does not close a run, snapshot inputs, choose a parent, or persist a row.
5. `LINEAGE_READY_TO_APPEND` is a transient reviewable result, not a database state and not evidence promotion.
6. `LINEAGE_APPENDED_AND_BOUND` is a future persistence result. It is not certification, activation, project binding, or execution.
7. ZIP import, UI opening, process restart, duplicate pack import, compatibility reevaluation, migration, and later certification/activation without a new run/checkpoint do not enter this state machine.
8. Missing parent never implies `ROOT`; `ROOT` and `CHILD` remain explicit at the lineage creation boundary.

## 5. Proposed v17 additive schema

The following is a design, not SQL executed in this phase. The implementation phase must use the project naming and mapper conventions, preserve SQLite v16, and create the tables only through a transactional `from16To17()` boundary.

No new table uses AUTOINCREMENT as a semantic key. Integer references below are nullable local provenance only and are never included in a semantic fingerprint.

### 5.1 `editorial_project_revisions`

Proposed columns:

| Column | Type/constraint | Meaning |
|---|---|---|
| `revision_identity` | `TEXT PRIMARY KEY NOT NULL`; lowercase 64-hex SHA-256 | Content-addressed project revision identity. |
| `revision_canonical_version` | `TEXT NOT NULL` | Version of the canonical project-definition projection. |
| `project_definition_fingerprint` | `TEXT NOT NULL UNIQUE`; lowercase 64-hex SHA-256 | Domain-separated semantic project-definition fingerprint. |
| `project_definition_canonical` | `TEXT NOT NULL` | Strict UTF-8 canonical semantic projection; no field-order/whitespace ambiguity. It is not raw UI JSON. |
| `source_project_row_id` | nullable `INTEGER`, FK `editorial_projects(id) ON DELETE RESTRICT` | Optional local provenance/lookup reference only; never semantic identity. |
| `created_at` | `INTEGER NOT NULL CHECK(created_at >= 0)` | Audit metadata only; excluded from both semantic hashes. |

The semantic projection must contain the exact reviewed project definition required by the contract. The first implementation must not silently treat current `series_name`/`volume_name` as the whole identity. The projection may include a stable project key, semantic project type, chapter/scope policy, and workflow policy identity only after the contract review decides those are semantic. It must exclude `output_tree_uri`, local paths, UI labels, timestamps, row IDs, and transient state.

### 5.2 `editorial_input_scope_snapshots`

Proposed columns:

| Column | Type/constraint | Meaning |
|---|---|---|
| `scope_snapshot_identity` | `TEXT PRIMARY KEY NOT NULL`; lowercase 64-hex SHA-256 | Content-addressed immutable scope snapshot identity. |
| `project_revision_identity` | `TEXT NOT NULL`, FK project revision `ON DELETE RESTRICT` | Exact project revision used by the scope. |
| `scope_canonical_version` | `TEXT NOT NULL` | Version of scope-key and manifest projection. |
| `canonical_scope_key` | `TEXT NOT NULL` | Normalized chapter/input-scope key; not a display title or URI. |
| `required_roles_canonical` | `TEXT NOT NULL` | Canonical set from the reviewed input-role contract. |
| `required_roles_fingerprint` | `TEXT NOT NULL`; lowercase 64-hex SHA-256 | Fingerprint of the reviewed required-role contract. |
| `manifest_version` | `TEXT NOT NULL` | Version of the input manifest contract. |
| `manifest_fingerprint` | `TEXT NOT NULL`; lowercase 64-hex SHA-256 | Fingerprint of the complete entries plus manifest contract facts. |
| `source_chapter_row_id` | nullable `INTEGER`, FK `editorial_chapters(id) ON DELETE RESTRICT` | Optional local provenance only; never semantic identity. |
| `created_at` | `INTEGER NOT NULL CHECK(created_at >= 0)` | Audit metadata only; excluded from semantic hashes. |

The snapshot has a required one-to-many relationship with `editorial_input_scope_snapshot_entries`. The snapshot is not complete unless all reviewed required roles and all required entry facts are present. A missing role, SHA-256, ordinal, byte count, or item count returns `INPUT_MANIFEST_INCOMPLETE`; it does not create a partial snapshot.

`item_count = 0` is not an unknown sentinel. Zero is accepted only when the authoritative source explicitly proves a real zero. Unknown is missing and fails validation.

### 5.3 `editorial_input_scope_snapshot_entries`

Proposed columns:

| Column | Type/constraint | Meaning |
|---|---|---|
| `scope_snapshot_identity` | `TEXT NOT NULL`, FK snapshot `ON DELETE RESTRICT` | Parent snapshot. |
| `role` | `TEXT NOT NULL CHECK(length(trim(role)) > 0)` | Contract role code, not display text. |
| `ordinal` | `INTEGER NOT NULL CHECK(ordinal >= 0)` | Semantic order within a role where the contract gives order meaning. |
| `input_sha256` | `TEXT NOT NULL`; lowercase 64-hex SHA-256 | Authoritative input bytes identity. |
| `byte_count` | `INTEGER NOT NULL CHECK(byte_count >= 0)` | Authoritative byte count. |
| `item_count` | `INTEGER NOT NULL CHECK(item_count >= 0)` | Authoritative item count; zero is not “unknown”. |

Primary key: `(scope_snapshot_identity, role, ordinal)`. This prevents duplicate role/ordinal entries. There is no URI, display name, raw content, or personal filesystem path in the semantic snapshot.

### 5.4 `editorial_closed_run_contexts`

Proposed columns:

| Column | Type/constraint | Meaning |
|---|---|---|
| `closed_run_identity` | `TEXT PRIMARY KEY NOT NULL`; lowercase 64-hex SHA-256 | Content-addressed identity of one closed run context. |
| `closed_run_fingerprint` | `TEXT NOT NULL UNIQUE`; lowercase 64-hex SHA-256 | Domain-separated fingerprint of the canonical closed-context projection. |
| `closed_context_version` | `TEXT NOT NULL` | Version of the closed-run identity contract. |
| `project_revision_identity` | `TEXT NOT NULL`, FK revision `ON DELETE RESTRICT` | Exact immutable project revision. |
| `scope_snapshot_identity` | `TEXT NOT NULL`, FK snapshot `ON DELETE RESTRICT` | Exact immutable input scope. |
| `canonical_pack_hash` | `TEXT NOT NULL`; lowercase 64-hex SHA-256 | Exact pack bytes context. |
| `trusted_profile_id` | `TEXT NOT NULL` | Trusted profile identity from the trusted registry/evaluation. |
| `trusted_profile_version` | `TEXT NOT NULL` | Trusted profile version from the same source. |
| `canonical_profile_hash` | `TEXT NOT NULL`; lowercase 64-hex SHA-256 | Canonical trusted profile hash. |
| `machine_contract_fingerprint` | `TEXT NOT NULL`; lowercase 64-hex SHA-256 | Exact engine machine-contract context. |
| `contract_id` / `contract_version` | `TEXT NOT NULL` | Contract identity used by the run. |
| `schema_id` / `schema_version` | `TEXT NOT NULL` | Schema identity used by the run. |
| `compatibility_evaluation_id` | `TEXT NOT NULL`, FK v15 evaluation `ON DELETE RESTRICT` | Exact compatibility evidence row. This is not the run identity. |
| `compatibility_context_fingerprint` | `TEXT NOT NULL`; lowercase 64-hex SHA-256 | Context fingerprint copied from the trusted evaluation and cross-checked. |
| `compatibility_outcome` | `TEXT NOT NULL` with reviewed v15 vocabulary | Stored for readback; the closure service must reject blocked/invalid context and any outcome not allowed by the approved contract. |
| `run_kind` | `TEXT NOT NULL` | Semantic run kind, not a UI state. |
| `phase_identity` | `TEXT NOT NULL` | Semantic phase/checkpoint identity when required by the contract. |
| `run_attempt_ordinal` | `INTEGER NOT NULL CHECK(run_attempt_ordinal >= 0)` | Deterministic semantic attempt number allocated by the authoritative run creator; not a row ID and not a random UUID. |
| `input_manifest_fingerprint` | `TEXT NOT NULL`; lowercase 64-hex SHA-256 | Must equal the referenced scope snapshot manifest fingerprint. |
| `source_run_row_id` | nullable `INTEGER`, FK `editorial_runs(id) ON DELETE RESTRICT` | Optional local provenance only; never semantic identity. |
| `closed_at` | `INTEGER NOT NULL CHECK(closed_at >= 0)` | Audit closure timestamp; excluded from semantic hashes. |

The combination of scope, run kind, phase, and `run_attempt_ordinal` must be unique. `run_attempt_ordinal` is necessary to distinguish two semantically separate attempts with identical inputs without importing an ID, using a database row ID, or inventing a random UUID. Allocation must be serialized by the future authoritative run creator.

The table contains only closed contexts. An OPEN row must not be inserted here and then updated to CLOSED. A mutable preparation row and this immutable closed row are separate boundaries.

### 5.5 `editorial_run_lineage_bindings`

Proposed columns:

| Column | Type/constraint | Meaning |
|---|---|---|
| `binding_identity` | `TEXT PRIMARY KEY NOT NULL`; lowercase 64-hex SHA-256 | Content-addressed binding identity. |
| `closed_run_identity` | `TEXT NOT NULL`, FK closed run `ON DELETE RESTRICT` | Exact closed run. |
| `lineage_record_identity` | `TEXT NOT NULL`, FK v16 lineage record `ON DELETE RESTRICT` | Exact lineage record. |
| `lineage_record_fingerprint` | `TEXT NOT NULL`; lowercase 64-hex SHA-256 | Exact fingerprint read from the v16 record and compared before insert. |
| `binding_contract_version` | `TEXT NOT NULL` | Version of the one-run/one-lineage binding contract. |
| `bound_at` | `INTEGER NOT NULL CHECK(bound_at >= 0)` | Audit timestamp; excluded from semantic binding identity. |

Required unique constraints:

- `UNIQUE(closed_run_identity)` — one closed run binds to at most one lineage record;
- `UNIQUE(lineage_record_identity)` — one lineage record is not rebound to another closed run;
- `UNIQUE(closed_run_identity, lineage_record_identity, lineage_record_fingerprint)` — exact idempotency lookup.

The binding DAO may append only after lineage DAO returns `APPENDED` or exact `ALREADY_EXISTS` and a readback verifies the exact record fingerprint. Same run/different lineage returns a stable binding mismatch; it never updates or replaces a binding.

### 5.6 Indexes and database protection

The implementation should add indexes for:

- revision source project and revision identity;
- snapshot `(project_revision_identity, canonical_scope_key, created_at)`;
- snapshot entries `(scope_snapshot_identity, role, ordinal)` (the primary key already covers the exact lookup);
- closed context `(project_revision_identity, scope_snapshot_identity, closed_at)`;
- closed context `compatibility_evaluation_id`;
- binding `closed_run_identity` and `lineage_record_identity`.

Every new table requires `BEFORE UPDATE` and `BEFORE DELETE` triggers that raise a stable SQLite error. Every FK uses `ON DELETE RESTRICT`; no immutable identity or evidence row is cascade-deleted. No `ON UPDATE CASCADE` is used. SQL checks may validate nonempty fields, lowercase 64-hex hashes, allowed node/status vocabulary and nonnegative counts, but cross-row semantic checks remain DAO/service validation responsibilities.

## 6. Canonical identity and fingerprint contract

The implementation must define four separate domain families, with no fingerprint field included in its own input:

```text
EDITORIAL_PROJECT_REVISION_IDENTITY_V1
EDITORIAL_INPUT_SCOPE_IDENTITY_V1
EDITORIAL_CLOSED_RUN_IDENTITY_V1
EDITORIAL_RUN_LINEAGE_BINDING_IDENTITY_V1
```

If a separate `*_fingerprint` is retained, it uses a corresponding explicit fingerprint domain and is computed from the same semantic projection without the identity/fingerprint field. The identity and fingerprint must not be confused by a version string alone.

Common rules:

- encode domain tag, canonical version, field names, lengths and values using strict UTF-8;
- reject a BOM at parser/resource boundaries;
- use deterministic scalar encoding and Unicode/whitespace rules defined by the contract, not locale defaults;
- canonicalize field order independently of source JSON order and whitespace;
- sort set-like roles by the contract’s stable code-point order;
- sort entries by `(role, ordinal)`; preserve ordinal order because it is semantic within a role;
- preserve phase order only when the phase contract says order has semantic meaning;
- exclude timestamps, UI text, local paths/URIs, AUTOINCREMENT row IDs, SQL insertion order and machine-local storage keys;
- one byte changed in the semantic project definition, manifest input hash/count, trusted profile context, or parent/binding fact must change the relevant hash;
- never read raw book/prompt content during identity resolution when an authoritative hash and manifest already exist;
- reject missing facts instead of serializing empty/default/`UNKNOWN` values.

### 6.1 Project revision projection

The first reviewed projection is conceptually:

```text
canonical_version
project_semantic_key
project_semantic_type
workflow_policy_identity (only if semantics require it)
ordered semantic project rules
```

The exact semantic definition is stored in `project_definition_canonical` and hashed. `series_name` and `volume_name` are not automatically the whole identity: they are included only if the contract decides they are semantic names after normalization. `output_tree_uri`, display labels, UI title, row ID, timestamps, and content not required by the project contract are excluded.

### 6.2 Input-scope projection

The scope projection contains:

```text
scope_canonical_version
project_revision_identity
canonical_scope_key
required_roles_canonical
manifest_version
sorted(role, ordinal, input_sha256, byte_count, item_count) entries
```

`manifest_fingerprint` covers the complete manifest projection. The snapshot identity covers the project revision, scope key, role-contract identity, manifest version and manifest fingerprint. Raw URI, display name, content text, source path, insertion order and database IDs are excluded.

### 6.3 Closed-run projection

The closed-run projection contains:

```text
closed_context_version
project_revision_identity
scope_snapshot_identity
canonical_pack_hash
trusted_profile_id
trusted_profile_version
canonical_profile_hash
machine_contract_fingerprint
contract_id/version
schema_id/version
compatibility_evaluation_id
compatibility_context_fingerprint
approved compatibility outcome
run_kind
phase_identity
run_attempt_ordinal
input_manifest_fingerprint
```

It excludes `closed_at`, `source_run_row_id`, UI text, import ID, compatibility-evaluation local row ID, and any mutable open-run fields. `compatibility_evaluation_id` identifies the evidence source; it does not replace the closed run identity.

### 6.4 Binding projection

The binding projection contains:

```text
binding_contract_version
closed_run_identity
lineage_record_identity
lineage_record_fingerprint
```

It excludes `bound_at` and database row IDs. A changed lineage fingerprint, parent, manifest, run, or binding contract produces a different binding identity and is not an update of the old binding.

No canonicalizer is implemented in C1.5-A.

## 7. Field-by-field authoritative mapping

| Lineage field | Authoritative source today / future source | Present? | Stable/content-addressed? | Missing or required decision |
|---|---|---:|---:|---|
| Canonical pack hash | `editorial_packs.canonical_pack_hash`, copied into trusted v15 evaluation | Yes | Yes, SHA-256 | Future resolver must cross-check the pack row, evaluation row and trusted registry. |
| Trusted profile ID/version | Trusted profile registry snapshot and v15 `editorial_pack_compatibility_evaluations` | Trusted rows: yes; legacy rows: no | Yes only for `TRUSTED_PROFILE` evidence | Never use `LEGACY_UNATTESTED` as authoritative profile facts. |
| Canonical profile hash | Trusted registry/evaluation snapshot | Trusted rows: yes | Yes, SHA-256 | Must match the immutable profile trust anchor; caller cannot author it. |
| Machine contract fingerprint | Compatibility evaluation context and engine trust context | Yes in trusted v15 evaluation | Yes, context-addressed | Must match the engine context that closes the run. |
| Contract/schema identity | Pack row contract/schema plus trusted evaluation/evaluator contract | Partially yes | Stable when reviewed and hashed | Final v17 field mapping must distinguish pack contract, evaluator contract and engine schema; do not collapse them into one version string. |
| Project identity | Current `editorial_projects.id`, names, workflow hash | No | No; row ID/local fields are mutable | Need an explicit project semantic key and immutable `editorial_project_revisions` row. |
| Input-scope identity | `editorial_chapters.id`/key/raw hash and current asset rows | No | No; incomplete role manifest and mutable rows | Need immutable scope key, snapshot, roles, ordinals, SHA-256, byte count and item count. |
| Run/evaluation identity | v15 `evaluation_id` exists; `editorial_runs.id` is local/open/mutable | No closed run identity | Evaluation ID is not a run identity | Need immutable closed-run identity and deterministic attempt ordinal. Compatibility evaluation remains a linked evidence source only. |
| Input manifest | Legacy `editorial_runs.input_manifest_json` and `EditorialAssetManifest` role/hash checks | Incomplete | No complete immutable contract | Need v17 snapshot and entry tables; required roles must come from reviewed contract. |
| Parent identity/fingerprint | v16 lineage record/self-FK and future explicit parent resolver | Persistence: yes; creation source: no | Yes for stored exact lineage | Future service must resolve exact existing parent; missing parent never means root. |
| Project revision identity | New `editorial_project_revisions.revision_identity` | No | Planned content-addressed SHA-256 | Requires semantic project-definition review. |
| Scope snapshot identity | New `editorial_input_scope_snapshots.scope_snapshot_identity` | No | Planned content-addressed SHA-256 | Requires stable scope key and complete manifest. |
| Closed run identity | New `editorial_closed_run_contexts.closed_run_identity` | No | Planned content-addressed SHA-256 | Requires an authoritative closure creator and deterministic attempt ordinal. |
| Run-lineage binding identity | New `editorial_run_lineage_bindings.binding_identity` | No | Planned content-addressed SHA-256 | Requires shared append transaction with v16 lineage DAO. |

## 8. Root/child and closure boundary

### 8.1 What may create each identity

- Project revision: explicit project-preparation operation after the semantic project definition is reviewed and frozen. It does not create lineage.
- Input-scope snapshot: explicit scope-preparation operation after project revision and all required inputs are immutable for the snapshot. It does not create lineage.
- Closed run: explicit authoritative run/evaluation close operation after exact project revision, scope snapshot, pack/profile trust context, compatibility evidence, contract/schema and deterministic attempt identity are resolved. It does not silently update an OPEN row.
- Root/child lineage: only after `RUN_CONTEXT_CLOSED`; C1 prepares the record and later persistence appends it. Root/child selection is explicit, and a child requires exact stored parent identity/fingerprint.
- Binding: only after lineage append returns `APPENDED` or exact `ALREADY_EXISTS` and exact readback matches.

### 8.2 Events that must not create an identity or lineage row

- ZIP/pack import alone;
- opening or refreshing the UI;
- process restart;
- duplicate pack import;
- compatibility reevaluation;
- SQLite migration;
- an existing open run becoming visible in the UI;
- certification or activation that creates no new run/checkpoint;
- missing context, blocked compatibility, ambiguous parent, or failed validation.

Failure means “no new record”, not a blocked placeholder and not a fabricated root.

## 9. Retention, deletion, and immutable protection

Recommended v17 retention policy:

1. All five new tables use `ON DELETE RESTRICT` and update/delete rejection triggers.
2. Semantic facts are stored independently of mutable UI payloads. Nullable `source_*_row_id` fields are provenance only and are not in any identity projection.
3. The conservative first implementation keeps a referenced local project/chapter/run row from being deleted while its immutable revision/snapshot/closed context is retained. This prevents the audit chain from losing its declared provenance. It does not cascade-delete immutable evidence.
4. Existing `EditorialRepository.deleteProject()` therefore must be reviewed before v17 implementation: once a source row is referenced, the operation must fail closed and surface a stable retention result, or a separately approved detach/provenance policy must be designed. C1.5-A does not change that method.
5. Mutable project edits may change current UI fields but cannot mutate a revision, snapshot, closed context or binding. A semantic change requires a new immutable row.
6. Deleting an app/database during uninstall is physical data loss, not an API-level lineage delete and not a valid way to remove evidence.
7. No cascade delete is permitted for lineage, identity, compatibility or binding evidence.

User approval is required before implementation for the strict local-FK retention choice versus a future explicit provenance-detach operation. No detach is assumed here.

## 10. Future transaction boundary

The future implementation must keep semantic events separate:

1. Resolve and validate the authoritative project definition, then append one project revision.
2. Resolve the immutable input source and complete manifest; append snapshot plus all entries in one transaction. Any missing/invalid entry rolls back the entire snapshot.
3. Resolve trusted pack/profile/engine context and compatibility evidence; close the run by appending one immutable closed-context row. An OPEN row is not updated into this table.
4. C1 resolves the closed context and prepares a root/child record with pure-JVM validation. No write occurs in C1.
5. Future persistence opens one SQLite transaction, appends the v16 lineage record and every input entry, then appends the v17 run-lineage binding. The binding is allowed only after exact `APPENDED`/`ALREADY_EXISTS` plus fingerprint readback.
6. Any lineage or binding failure rolls back the shared transaction. A failure cannot report lineage creation complete.

The existing v16 lineage DAO owns its own transaction today. To make step 5 atomic, the smallest future API seam is a transaction-scoped append method that accepts the already-open `SQLiteDatabase`, or one application-level `appendLineageAndBind(...)` transaction service that delegates both row mappers against the same connection. The implementation must not nest two independent DAO transactions and claim atomicity.

Pack import/compatibility evaluation and project/run closure are different semantic events and must not be joined into one large transaction merely for convenience.

## 11. Future DAO/API proposal

No API below is added in this phase. Names are proposals subject to existing conventions:

- `EditorialProjectRevisionDao.append(...)` / `findByIdentity(...)` / `findBySourceProjectRow(...)`;
- `EditorialInputScopeSnapshotDao.appendWithEntries(...)` / `findByIdentity(...)` / `findLatestForScope(...)`;
- `EditorialClosedRunContextDao.append(...)` / `findByIdentity(...)` / `findByAttempt(...)`;
- `EditorialRunLineageBindingDao.appendInTransaction(...)` / `findByClosedRunIdentity(...)`;
- an app-side `EditorialLineageContextResolver` adapter that reads the four v17 DAO boundaries, v15 trusted evaluation, v16 lineage parent facts and the reviewed registry;
- a transaction application service that exposes append-and-bind, not separate caller-controlled update/delete/reparent operations.

There must be no update, delete, replace, backfill, reparent, certify, activate, project-bind, or execute API for these immutable records. C1 must continue to distrust caller assertions and re-read trusted facts through the resolver.

## 12. Stable machine outcomes

The future implementation must reuse the existing reviewed C1/lineage/DAO vocabulary or provide a lossless boundary mapping; it must not introduce a second enum with the same machine meaning. At minimum, the schema/service boundary needs stable outcomes for:

```text
IDENTITY_UNATTESTED
PROJECT_REVISION_REQUIRED
INPUT_SCOPE_REQUIRED
INPUT_MANIFEST_INCOMPLETE
RUN_CONTEXT_REQUIRED
RUN_CONTEXT_NOT_CLOSED
TRUSTED_PROFILE_CONTEXT_MISMATCH
COMPATIBILITY_CONTEXT_MISMATCH
DUPLICATE_IMMUTABLE_RECORD
IMMUTABLE_RECORD_MUTATION
BINDING_MISMATCH
LINEAGE_PERSISTENCE_FAILURE
```

Existing lineage validation codes remain authoritative for parent/duplicate/orphan/reparent/cross-context/cycle failures. Existing v15 `LEGACY_UNATTESTED` remains a read-only legacy classification and must not be rewritten or promoted.

## 13. Legacy and migration policy

The future v16→v17 migration must be additive and transactional:

- create only the new tables, indexes, checks and immutability triggers;
- do not alter or rebuild v16 or earlier tables;
- do not add authoritative columns to mutable project/chapter/run rows;
- do not backfill a project revision, scope snapshot, closed run, binding or lineage root for an existing row;
- do not read raw stored content to infer a new identity;
- do not open the bundled profile to backfill profile facts;
- do not reevaluate packs or rewrite `LEGACY_UNATTESTED`;
- do not modify the QA blocked row or any existing pack storage state;
- on migration failure, roll back the new schema as one transaction;
- preserve existing `onConfigure` foreign-key enforcement and non-destructive downgrade rejection;
- existing rows remain `IDENTITY_UNATTESTED`/equivalent until a user- or system-authorized explicit prepare-and-close flow creates new facts.

Supported earlier chains, if retained by implementation, are tested as `v13→v14→v15→v16→v17` and any currently supported lower chain. No older row becomes authoritative by looking complete.

## 14. Future C1.5-B migration/test matrix

This is an acceptance matrix for the next implementation phase, not evidence produced by C1.5-A.

| Area | Required case | Expected result |
|---|---|---|
| Migration | Fresh v17 database | All five new tables/indexes/triggers exist; zero identity rows. |
| Migration | v16→v17 | Additive success; every v16 row count/hash/state unchanged; zero backfill. |
| Migration | Each supported earlier chain→v17 | Same additive result and rollback behavior. |
| Migration | Failure injected mid-migration | Whole v17 addition rolls back; v16 remains readable. |
| Legacy | Existing project/chapter/asset/run/evaluation rows | No revision/snapshot/closed-run/binding/lineage rows created; legacy status unchanged. |
| Revision | Same canonical project projection twice | Stable identity; exact duplicate is idempotent and timestamp unchanged. |
| Revision | One semantic byte changed | New identity; old revision unchanged. |
| Snapshot | Same sorted manifest in different order/whitespace | Same identity. |
| Snapshot | Missing role/hash/count/ordinal or count unknown | `INPUT_MANIFEST_INCOMPLETE`; no partial row. |
| Snapshot | One input hash/byte/item value changed | New manifest/snapshot identity; old snapshot unchanged. |
| Closed run | Same authoritative context/attempt | Stable identity; exact duplicate idempotency. |
| Closed run | OPEN context, stale profile, blocked compatibility, mismatched manifest | Fail closed; no closed row. |
| Closed run | Same context with a different deterministic attempt ordinal | Different closed-run identity; no overwrite. |
| Immutability | UPDATE/DELETE on each new table | Rejected by trigger and API boundary. |
| Referential integrity | Delete referenced revision/scope/closed run/lineage row | Rejected by `ON DELETE RESTRICT`; no cascade. |
| Transaction | Partial snapshot entries, lineage append, or binding failure | Whole shared transaction rolls back; no partial record. |
| Duplicate | Same identity/same bytes | Stable `ALREADY_EXISTS` behavior; audit timestamps unchanged. |
| Duplicate | Same identity/different bytes | Stable duplicate/mismatch failure; no replacement. |
| Concurrency | Racing append of same revision/snapshot/closed run/binding | At most one row; loser gets deterministic duplicate result and exact readback. |
| Binding | APPENDED lineage + exact binding | One immutable binding. |
| Binding | `ALREADY_EXISTS` lineage + exact binding | Binding allowed only after exact fingerprint readback. |
| Binding | Same run/different lineage, reparent, fingerprint mismatch | Stable binding/parent mismatch; no update or second binding. |
| Readback | Restart/reopen database | Exact identities, fingerprints, entries and binding reconstruct unchanged. |
| Safety | No importer call site | Source/test scan proves only future explicit caller may invoke DAO. |

## 15. Device and continuity boundary

C1.5-A performs no device QA. The prior B1-B result is limited to isolated migration and append behavior. The approved wording remains:

```text
ISOLATED_MIGRATION_AND_APPEND_QA_PASS
```

It must not be relabeled real-data continuity. The inspected device did not contain a pre-existing production database at the relevant QA point, so no preservation claim may be made for user data that never existed there.

The future device matrix must:

- upgrade v16 to v17 without clearing app data;
- prove existing rows, pack/evaluation history and QA blocked row are unchanged;
- prove no identity or lineage backfill occurs;
- use an isolated, clearly sourced fixture/context for append and binding tests;
- verify restart/readback and update/delete restrictions;
- avoid manual SQLite mutation, candidate files from `D:`, and `pm clear` as a way to hide migration/runtime errors.

## 16. Decisions requiring approval before implementation

1. Approve the exact semantic project-definition projection, including whether series/volume are semantic identifiers and which workflow/project rules belong in it.
2. Approve the stable project semantic key source; current project row ID, pack ID, import ID and display names are rejected.
3. Approve canonical scope-key normalization and the reviewed required-role/ordinal/count contract. Current `EditorialAssetManifest` is insufficient by itself.
4. Approve the deterministic `run_attempt_ordinal` allocation/closure authority and its race-handling contract.
5. Approve strict `ON DELETE RESTRICT` retention for local provenance references, or separately authorize a future explicit provenance-detach design. No detach is assumed here.
6. Approve the shared SQLite transaction seam for atomic v16 lineage append plus v17 binding.
7. Approve the allowed compatibility outcome for a closed run. A blocked or legacy-unattested evaluation can never close a lineage context; an adapter outcome requires separately certified adapter evidence.

These are schema gates, not permission to promote `lineage.exact-parent.v1` or enable execution.

## 17. Exact next step

The single smallest next step is:

```text
G2-C1B1-C1.5-B — implement and test the additive SQLite v16→v17 schema/DAO boundary after the decisions above are approved.
```

Do not skip to C2 caller integration, importer wiring, capability promotion, certification, activation, project binding, or execution.

## 18. Review-stop conclusion

The plan passes because it rejects local AUTOINCREMENT IDs as semantic identities, supplies immutable project revision/scope/closed-run/binding designs, defines canonical projections and separate SHA-256 domains, preserves legacy rows without backfill, defines retention and transaction boundaries, and provides the required future migration/test/device matrix.

No source, database, profile, runtime, importer, APK, or production evidence changed in this phase.
