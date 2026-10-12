# EDITORIAL PACK PLATFORM — G2-C1B1-C0 LINEAGE CREATION BOUNDARY

## Review-stop result

```text
G2-C1B1C0_BOUNDARY_PLAN: PASS
LINEAGE_RUNTIME_WIRING: NOT_STARTED
LINEAGE_CAPABILITY_PROMOTION: NOT_STARTED
SAFE4_EXECUTION_READINESS: BLOCKED
```

This is a survey and design handoff only. No importer or runtime caller was
changed, no SQLite migration was added, no lineage row was created, and no
profile, evidence catalog, pack state, certification, activation, binding or
execution state was changed.

## 1. Baseline and scope

- Start branch: `feature/v4.16-g2-c1b1b`.
- Start HEAD: `79ff13260d4864900082494ef6c628a943e1478f`.
- Review branch: `feature/v4.16-g2-c1b1c0`.
- End implementation baseline: `79ff13260d4864900082494ef6c628a943e1478f`; this phase changes documentation only. The final documentation commit is recorded by the phase checklist after it is created.
- SQLite source version: v16.
- Latest accepted development artifact: `4.16-dev.39`, code101, event `build-20260805-124714`.
- Production package on the inspected device: code101.
- Production evidence catalog: only `pack.integrity.sha256.v1`.
- `EditorialSafe4Pack.executionEnabled()`: `false`.
- Canonical profile hash remains `2d4e2f76dc5defcfb98cfd36cec49b0e5454cb3462db93a6b1586f7784eb91b6`.
- Machine fingerprint remains `6410f374ce175cbc6fc32484f5cd9635888b9297b01d882923af06ccd4ac1e4c`.
- The only pre-existing worktree changes are the user-owned `.idea/compiler.xml`, `.idea/gradle.xml` and `.idea/misc.xml`; they were not edited, staged, stashed or committed.
- No candidate file or database under `D:` was read.

The required read-only regression passed after the branch was created:

| Command | Result |
|---|---|
| `:editorial-engine:test` | 74 passed, 0 failed, 0 errors, 0 skipped |
| `:app:testDebugUnitTest` | 163 passed, 0 failed, 0 errors, 0 skipped |
| Combined JVM total | 237 passed, 0 failed, 0 errors, 0 skipped |
| `git diff --check` | PASS |

No APK build or installation was performed. The latest artifact remains code101.

## 2. Evidence source audit

### Pack import and compatibility

`EditorialPackImportService.importPack(Collection<EditorialPackImportEntry>)`
and `importZip(InputStream)` are pack-storage operations. An
`EditorialPackImportEntry` contains a normalized path, stream, declared and
compressed lengths, and a symbolic-link flag. It does not contain project,
chapter/input-scope, run/evaluation, frozen project-input manifest, or parent
checkpoint context.

The importer does have authoritative pack facts after integrity validation:
canonical pack hash, pack ID/version, contract/schema declaration and the
trusted compatibility evaluation. `editorial_packs` and the v15
`editorial_pack_compatibility_evaluations` table preserve those facts. The
import-generated UUID is an operational import identity only; it is not a
project identity or lineage run identity.

The importer transaction therefore ends at immutable pack storage and
compatibility evidence. It must not be extended to create a root merely
because a parent is absent.

### Project, chapter and input snapshot

The v10-to-v11 migration and `EditorialRepository` currently provide:

- `editorial_projects`: AUTOINCREMENT `id`, series/volume, workflow version/hash,
  output URI and timestamps, with a unique `(series_name, volume_name)` index.
- `editorial_chapters`: AUTOINCREMENT `id`, `project_id`, chapter key/title,
  state, `raw_hash` and timestamps, with a unique `(project_id, chapter_key)`
  index and a project foreign key.
- `editorial_assets`: AUTOINCREMENT `id`, `chapter_id`, role, source URI,
  display name, SHA-256, byte size and content, with a unique role per chapter.
- `EditorialRepository.createChapter(...)`: one transaction that requires the
  RAW, DRAFT and GLOSSARY roles, accepts optional PRONOUN and PAIR_CONTEXT,
  computes hashes and stores the chapter asset snapshot.

The chapter asset transaction is the closest existing preparation boundary for
an exact input snapshot, but it is not yet a lineage boundary. Project IDs and
chapter IDs are SQLite local keys, not portable semantic identities. Project
series/volume identity is mutable through `updateProjectIdentity`. Asset
storage has a role/hash snapshot but not the v1 lineage manifest shape
`role + ordinal + hash + byte count + item count`.

### Run/evaluation and lineage

`editorial_runs` exists with AUTOINCREMENT `id`, `chapter_id`, run kind/state,
provider/model, prompt/workflow hashes, an input-manifest JSON field and audit
timestamps. No production run-creation API or caller was found. The v15
`evaluation_id` such as `importId:compatibility:v1` identifies compatibility
evaluation evidence, not an editorial run/checkpoint.

SQLite v16 already contains the additive
`editorial_lineage_records`/`editorial_lineage_input_entries` append-only
boundary, and the B1-A pure-JVM model/validator verifies exact parent context.
The DAO is unused by importer/runtime. It is storage and validation context,
not an authoritative source for project/run identity.

### Trust and execution boundary

The bundled trusted profile registry is authoritative for trusted profile ID,
profile version, canonical profile hash and machine contract fingerprint. The
v15 compatibility evaluation is the durable provenance for the resolver's
selected trusted facts. A pack declaration or caller-supplied fingerprint may
be compared, but may not replace those sources. The current profile is
non-executable, the catalog still contains only pack integrity, and execution
remains blocked.

## 3. Creation-boundary decision

### A — ZIP pack import: no lineage creation

ZIP import has enough context to:

1. snapshot and integrity-validate the pack;
2. compute the canonical pack hash;
3. resolve the trusted profile and compatibility context; and
4. persist immutable pack/import/compatibility evidence.

It does not have a stable project identity, chapter/input-scope identity,
closed project input manifest, run/evaluation identity or exact parent. The
authoritative result is therefore:

```text
pack stored + compatibility evidence persisted
lineage not created
pack remains blocked when compatibility is blocked
```

The following events do not create lineage: a duplicate pack import, process
restart, opening the UI, compatibility reevaluation, migration, or importing
the same bytes again. Pack/import and later lineage/run transactions remain
separate semantic events.

### B — Project/chapter preparation: context preparation, not creation

Project/chapter preparation may establish the facts needed later, but it must
not create a root until all of the following have been closed and addressable:

- an immutable, stable project identity;
- an immutable input-scope identity for the chapter;
- the exact input manifest, including role, ordinal, hash, byte count and item
  count;
- trusted pack/profile/contract/schema context from the database and registry;
- a stable run/evaluation identity; and
- an explicit creation event.

The current `createProject`/`createChapter` APIs do not satisfy that contract.
They prepare local rows and assets only. Missing parent is never converted to
ROOT at this boundary.

### C — Run/evaluation creation: authoritative lineage boundary

The authoritative creation event is **run/evaluation context closure**, after
the input snapshot is frozen and before model execution. The future service
must create exactly one explicitly requested node at this event:

- `ROOT`: the request explicitly says ROOT and contains no parent reference.
- `CHILD`: the request explicitly says CHILD and selects exactly one existing
  parent checkpoint whose identity and fingerprint are read and verified by the
  service.

The event is not “a call site that happens to be convenient”. It is the
transition from a prepared project/chapter context to a closed run/evaluation
context. Activation, certification and execution do not create lineage by
themselves. A later model run or checkpoint may create another child only when
it has a new immutable run identity and an explicit exact parent.

The resulting future state sequence is:

```text
PACK_STORED / COMPATIBILITY_EVALUATED
        |
        v
PROJECT_CHAPTER_PREPARED      (no lineage row)
        |
        v
RUN_CONTEXT_CLOSED            (immutable manifest + run identity)
        |
        +-- explicit ROOT  --> validate --> append ROOT
        |
        +-- explicit CHILD --> resolve parent --> validate --> append CHILD
        |
        v
LINEAGE_BOUND_TO_RUN          (future additive binding, not implemented here)
```

## 4. Field-by-field authoritative identity mapping

| Lineage field | Authoritative source | Present now? | Stable/content-addressed? | Missing or required seam |
|---|---|---:|---:|---|
| Canonical pack hash | Integrity-validated `EditorialPackManifest.canonicalPackHash`; persisted in `editorial_packs.canonical_pack_hash` and v15 evaluation | Yes | Yes, SHA-256 | Runtime must re-read and verify, never accept a caller substitute |
| Trusted profile ID/version | Bundled trusted profile registry selected by the compatibility resolver; v15 evaluation columns are the durable copy | Yes | Yes within the trusted registry | Reject absent/ambiguous/stale evaluation; caller cannot author it |
| Canonical profile hash | Resolved trusted profile canonical hash; v15 evaluation | Yes | Yes, SHA-256 | Must match the immutable trust anchor |
| Machine fingerprint | Resolved trusted profile machine contract fingerprint; v15 evaluation | Yes | Yes, SHA-256 | Must match the trusted profile and engine context; pack-derived fingerprint is not a substitute |
| Contract/schema identity | Integrity-validated pack manifest contract/schema plus exact trusted resolver bounds | Yes as pack facts | Stable for the evaluated context | Current profile has no executable contract; lineage creation remains blocked until a future reviewed context is compatible |
| Project identity | Intended future immutable project identity derived from an immutable project definition/revision, resolved from a project row | No | Current AUTOINCREMENT `editorial_projects.id` is not semantic; series/volume can change | Additive stable identity/revision seam; changing semantic project identity must create a new identity, not mutate a lineage source |
| Input-scope identity | Intended future immutable chapter/scope identity derived from project identity, canonical chapter key/scope and the exact asset manifest | Partly | Current AUTOINCREMENT chapter ID and `raw_hash` alone are insufficient | Additive immutable scope identity and frozen snapshot; include all required roles, not only RAW |
| Run/evaluation identity | Intended future run-creation service and a closed immutable run/evaluation record | No production creator | Current `editorial_runs.id` is a local key; v15 compatibility `evaluation_id` has different semantics | Add explicit stable run identity and creation seam; do not reuse import ID or database row ID |
| Input manifest | Future frozen project/chapter snapshot reconstructed from authoritative asset rows and run context; B1-A canonical manifest is role/ordinal/hash/count based | Partly | Existing assets have role/hash/size/content; no ordinal/item-count contract | Define immutable manifest version and all entries before validation; source URI/UI text are not semantic fingerprint inputs |
| Parent identity/fingerprint | Existing v16 lineage DAO read context after an explicit parent selection; parent row must already exist | Storage only | Yes when read from the immutable row | No current run/checkpoint chooses a parent; future service must reject missing, ambiguous, stale or cross-context parent |

### Identity rules

SQLite AUTOINCREMENT IDs may be used as local lookup handles inside a future
resolver, but not as portable lineage identity. Pack ID/version is not project
identity, import ID is not run identity, a missing parent is not a root, and a
random UUID is not an answer to an identity ambiguity. If a proposed semantic
tuple cannot distinguish two records, the service must return
`LINEAGE_IDENTITY_REVIEW_REQUIRED` (the existing pure-JVM validator code) and
stop without writing a row.

## 5. Root and child machine contracts

### Root

A root append is permitted only when the service has reconstructed all of the
following from authoritative sources:

- exact canonical pack hash;
- trusted profile ID/version, canonical profile hash and machine fingerprint;
- compatible contract/schema identity and non-blocked compatibility evidence;
- stable project identity;
- stable input-scope identity;
- stable run/evaluation identity;
- complete immutable input manifest and its fingerprint; and
- explicit `nodeKind = ROOT`.

The parent fields must be absent and the service must not infer ROOT from a
failed parent lookup. The root record includes the full pack/profile/project/
scope/run/manifest context so it is independently verifiable.

### Child

A child append is permitted only when:

- `nodeKind = CHILD` is explicit;
- exactly one existing parent is selected;
- the parent identity is found and its stored fingerprint matches exactly;
- the child and parent have the same canonical pack hash, trusted profile
  context, contract/schema, project identity, input-scope identity and source
  manifest fingerprint;
- the child has a new valid run/checkpoint identity; and
- the pure-JVM validator accepts the complete tuple.

Reparenting, orphan insertion, cross-pack/profile/project/scope linkage,
silent source/input-hash changes, and disallowed forks fail closed. Cycles and
self-parenting are rejected. No adapter may repair a damaged parent chain or
invent a parent.

## 6. Future runtime service boundary

No production classes are added in C0. The following is the proposed seam for
the next implementation phase:

```java
interface EditorialLineageRuntimeService {
    EditorialLineageCreationResult create(
        EditorialLineageCreationRequest request);
}

interface EditorialLineageContextResolver {
    EditorialLineageAuthoritativeContext resolve(
        EditorialLineageCallerSelection selection);
}
```

The names are design names, not current source additions. The request should
contain only caller-owned selections:

- project selector;
- chapter/input-scope selector;
- existing run/evaluation selection or a request to close a new run context;
- explicit ROOT or CHILD kind; and
- for CHILD, the selected parent record identity (not a caller-authored parent
  fingerprint).

The resolver/service must read and verify, rather than trust, the following:

- canonical pack hash and stored pack/import state;
- trusted profile ID/version/hash and machine fingerprint from the registry and
  v15 evaluation;
- exact contract/schema compatibility;
- immutable project and chapter/scope identity;
- frozen input asset snapshot and canonical manifest;
- run/evaluation identity and closed snapshot; and
- parent record, fingerprint and lineage-context matches.

A caller-supplied trusted hash, profile fingerprint or input manifest is at
most a consistency assertion. It can never be the source of truth; mismatch
returns a stable failure and performs no write. The service has no filesystem,
network, raw-pack-content, model, Java/Dex loading or permissive fallback
path.

## 7. Future transaction boundary

One lineage creation transaction is proposed as:

1. Resolve the caller's project/chapter/run selection without trusting caller
   identity facts.
2. Resolve the immutable pack and v15 compatibility evaluation; reject blocked,
   ambiguous, stale or mismatched trusted context.
3. Reconstruct and freeze the exact input manifest, including required roles,
   ordinals, hashes and counts.
4. Resolve an explicit ROOT or exact existing parent; never synthesize one.
5. Run the pure-JVM `EditorialLineageValidator` against immutable facts.
6. Append the lineage record and all input entries through the v16 append-only
   DAO in one transaction.
7. If and only if a reviewed additive binding schema exists, append the run/
   evaluation-to-lineage binding in that same semantic transaction.
8. Commit all lineage facts, or roll them all back. A missing context, failed
   input entry or persistence error leaves no partial lineage record.

Pack import/storage and lineage/run creation must not be one transaction:
they are distinct semantic events and the import API lacks the required
context. A future run/evaluation binding also needs a separate additive schema
review. The current `editorial_runs` row is mutable/deletable and has no
lineage foreign key or production creator. If a safe link cannot be added
without mutating that history, use a separately reviewed append-only binding
table keyed by stable run/evaluation identity and lineage record identity. No
such migration is part of C0.

## 8. Stable fail-closed outcomes

The next service must expose one canonical machine result vocabulary. It must
reuse the existing B1-A/B1-B lineage meaning rather than introduce a second
enum with duplicate meanings. The required result set is:

| Required result | Meaning at the boundary |
|---|---|
| `LINEAGE_CONTEXT_REQUIRED` | The caller did not select a complete authoritative context |
| `PROJECT_IDENTITY_REQUIRED` | No stable immutable project identity exists |
| `INPUT_SCOPE_REQUIRED` | No stable immutable chapter/input-scope identity exists |
| `RUN_EVALUATION_IDENTITY_REQUIRED` | No closed stable run/evaluation identity exists |
| `INPUT_MANIFEST_INCOMPLETE` | Required role, ordinal, hash, byte count or item count is absent/inconsistent |
| `TRUSTED_PROFILE_CONTEXT_MISMATCH` | Caller/context differs from the trusted registry/evaluation facts |
| `COMPATIBILITY_CONTEXT_MISMATCH` | Pack contract/schema/profile/engine context is not the exact evaluated context |
| `MISSING_PARENT` | Child request has no parent selection |
| `PARENT_MISMATCH` | Selected parent identity/fingerprint/context does not match |
| `ORPHAN_LINEAGE` | Parent is not present in the immutable validation context |
| `AMBIGUOUS_PARENT` | More than one parent fact could satisfy the selection |
| `DUPLICATE_LINEAGE` | Same identity is associated with different fingerprint/content |
| `REPARENT_ATTEMPT` | Existing identity is being attached to a different parent |
| `LINEAGE_PERSISTENCE_FAILURE` | The append transaction failed and was rolled back |

The current code already has `MISSING_PARENT`, `PARENT_MISMATCH`,
`ORPHAN_LINEAGE`, `AMBIGUOUS_PARENT`, `DUPLICATE_LINEAGE` and
`REPARENT_ATTEMPT` in the lineage vocabulary. B1-B has `PERSISTENCE_FAILURE`,
which is the current persistence spelling of the proposed
`LINEAGE_PERSISTENCE_FAILURE`. The first seven context outcomes require a
single reviewed extension/mapping of that existing result vocabulary in a
future phase; C0 adds no enum and creates no duplicate source of truth.

No UI wording, model output, `PASS`, `CLOSED`, `SAFE`, `UNKNOWN`, empty string,
placeholder or blocked fake row is a lineage result. Missing context means no
lineage creation.

## 9. Events that must not create lineage

| Event | Allowed result |
|---|---|
| ZIP import only | Store pack and compatibility evidence; no lineage |
| Opening or navigating the UI | No lineage |
| Process restart | Read existing immutable rows only |
| Duplicate pack import | Stable duplicate/import result; no lineage |
| Compatibility reevaluation | New compatibility evidence only; no lineage |
| SQLite migration | Schema change only; no backfill/root synthesis |
| Certification or activation | No lineage unless a new run/checkpoint is explicitly created |
| Pack state change | No lineage; capability/promotion remains separate |
| Missing parent | Fail closed; never implicit ROOT |

## 10. Device QA limitation

G2-C1B1-B provided only:

```text
ISOLATED_MIGRATION_AND_APPEND_QA_PASS
```

The device did not contain a production database at final inspection. That
result is not real-data continuity evidence and must not be relabeled as such.
The future device phase must use a fixture/context with a documented source of
every identity field, must not manually edit SQLite, and must not clear app
data to hide a migration or runtime failure. Existing user-data preservation
can be claimed only when such data actually exists and is inspected without
destructive cleanup.

## 11. Phase decomposition after C0

The phases below are proposals only; none is started by this handoff.

### G2-C1B1-C1 — explicit lineage runtime context/service seam

- Scope: one canonical request/result/context contract and resolver ports;
  authoritative trusted-fact lookup rules; pure-JVM fail-closed tests.
- Dependency: B1-A model/validator and B1-B v16 append/read boundary.
- Expected files/modules: future engine context/request/result interfaces and
  focused tests; no importer call site and no production row creation.
- Evidence/tests: reject caller-authored profile/machine facts, placeholders,
  absent project/scope/run context and incomplete manifest; verify explicit
  ROOT/CHILD semantics and stable result mapping.
- Migration: none in this phase.
- Build/device QA: JVM regression only; no APK; no device mutation.
- Review stop: approve the seam and result vocabulary before any caller is
  connected.
- Rollback: revert the seam/test commit; no persisted state exists.
- Acceptance: a service cannot construct an authoritative context without all
  required fields and cannot write on any failure.
- Remains locked: importer, project/run callers, lineage persistence calls,
  catalog/profile promotion, certification, activation and execution.

### G2-C1B1-C1.5 — stable project/scope/run identity schema review (if required)

- Scope: separate additive design/implementation for immutable project identity,
  input-scope snapshot identity, closed run identity and safe lineage binding.
- Dependency: C1 contract; existing AUTOINCREMENT and mutable project APIs are
  not sufficient.
- Migration: must be additive, transactional, no backfill, no reevaluation and
  no automatic identity for existing rows. Existing projects require explicit
  re-preparation under the reviewed identity contract.
- Review stop: approve schema and deletion/retention semantics before C2.

### G2-C1B1-C2 — project/chapter/run caller integration

- Scope: integrate explicit project/chapter preparation and run-context closure
  with the service; create ROOT or CHILD only at the authoritative event.
- Dependency: C1 and any approved C1.5 identity seam.
- Evidence: end-to-end immutable manifest reconstruction, exact parent checks,
  duplicate/reparent/orphan/cross-context negatives and rollback.
- Explicit non-scope: ZIP importer remains separate; no implicit root on import.
- Review stop: runtime integration evidence review; capability remains
  unpromoted.

### G2-C1B1-C3 — device import/project/run/root-child QA

- Scope: upgrade without clearing data, create a documented fixture/context,
  execute root/child creation and restart/readback checks.
- Evidence boundary: isolated fixtures are clearly labeled; real-data
  continuity is reported only if a real production database is present.
- Review stop: device evidence audit; no production profile/catalog change.

### G2-C1B1-D — production evidence-catalog promotion review

- Scope: only after implementation, persistence, importer/runtime integration,
  fixed fixtures, negative tests, regression and device evidence are complete.
- Certification boundary: promotion is not certification, Golden Replay,
  activation, binding or execution.
- Review stop: a separate approval is required; this phase is not started here.

## 12. Exactly one smallest next step

The single smallest next step is:

```text
G2-C1B1-C1 — implement and test the explicit lineage runtime
context/service seam, without connecting importer or creating lineage rows.
```

This is smaller and safer than caller integration because it resolves the
current identity ambiguity first. It does not begin automatically in C0.

## 13. Final boundary assertions

- No importer, repository, project/chapter/run caller or lineage DAO wiring was
  changed.
- No migration or database row was created or backfilled.
- SQLite remains v16.
- The production profile and canonical trust anchors are unchanged.
- The production evidence catalog still contains only
  `pack.integrity.sha256.v1`; `lineage.exact-parent.v1` is not promoted.
- No compatibility history, QA blocked row, pack state or old evaluation was
  reevaluated.
- `EditorialSafe4Pack.executionEnabled()` remains `false`.
- No APK was built or installed; latest accepted artifact remains code101.
- `.idea/compiler.xml`, `.idea/gradle.xml` and `.idea/misc.xml` remain untouched,
  unstaged and uncommitted.
- Review stop: do not start G2-C1B1-C1 until separately approved.
