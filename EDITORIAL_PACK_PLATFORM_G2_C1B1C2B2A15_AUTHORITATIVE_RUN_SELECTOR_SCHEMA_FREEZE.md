# G2-C1B1-C2B2A-1.5 - Authoritative Run Selector and SQLite v18 Schema Freeze

Status: `G2-C1B1C2B2A15_SCHEMA_FREEZE: PASS / REVIEW STOP`

This document freezes the authoritative run-selector architecture and the
future SQLite v18 schema. It is a plan/schema-review artifact only. No
production code, DAO, migration, SQLite row, lifecycle service, event caller,
profile, catalog, lineage promotion or execution change was made.

## 1. Baseline and protected boundary

- Project: `C:\\Users\\ADMIN\\Documents\\App Translate Books`.
- Baseline branch: `feature/v4.16-g2-c1b1c2b2a1`.
- Baseline HEAD before this branch: `6dcfd6f161355332d29a4a9bbfeb7799c2267131`.
- Work branch: `feature/v4.16-g2-c1b1c2b2a15`.
- SQLite source version: v17 (`TranslationRepository.java:15`).
- Retained artifact: `4.16-dev.48`, code110.
- APK SHA-256:
  `B7E07C945602CF65572702DDF19579961CBA0070024DB9C5FFF89585C825F2C3`.
- Source ZIP SHA-256:
  `E5E927A5C7A32501F11555BB3A74A633A76DE4218A58DF8F2C2E9B5551759B23`.
- Baseline JVM evidence: `269/269 PASS`.
- Retained connected evidence: `82/82`, zero failures/errors, one approved
  skip.
- `.idea/compiler.xml`, `.idea/gradle.xml`, and `.idea/misc.xml` were
  pre-existing user-owned changes. They were not edited, staged, stashed or
  committed.

The required workflow and Git instructions were read before branch creation.
The current profile remains non-executable, declares only
`pack.integrity.sha256.v1`, and `EditorialSafe4Pack.executionEnabled()` remains
`false`.

## 2. Decision summary

```text
G2-C1B1C2B2A15_SCHEMA_FREEZE: PASS
AUTHORITATIVE_RUN_IDENTITY_MODEL: DECLARATION
LEGACY_EDITORIAL_RUNS_AUTHORITY: PROHIBITED
LEGACY_RUN_PROVENANCE: REMOVED
RUN_ATTEMPT_ORDINAL_OWNER: DAO
RETRY_VS_NEW_ATTEMPT_CONTRACT: DECIDED
ROOT_CHILD_INTENT_STORAGE: DECIDED
V18_TABLE_COUNT: 2
V18_FINAL_DDL: FROZEN
C2B2A2_SCHEMA_IMPLEMENTATION: NOT_STARTED
LINEAGE_PROMOTION: BLOCKED
SAFE4_EXECUTION_READINESS: BLOCKED
```

The selected model is **Option A: an immutable authoritative run declaration
before closure**. The declaration creates the stable authoritative run
identity and DAO-owned attempt ordinal. A later immutable closure event refers
to that identity. The event is never used as a substitute for a declaration.

## 3. Source audit and the four required answers

### 3.1 Evidence that legacy runs are not authoritative

The legacy table is defined by
`app/src/main/java/com/ml/tblandroidtxt/EditorialMigrationSpec.java:16-21`.
It has an AUTOINCREMENT `id`, mutable `state`, provider/model fields,
legacy prompt/workflow hashes, mutable timestamps and a legacy
`input_manifest_json`. `EditorialRepository.java:145-160` deletes the legacy
runs during project deletion. There is no production `INSERT INTO
editorial_runs`, run declaration owner, authoritative state-transition owner or
closure-event producer. The source audit therefore remains:

```text
PRODUCTION_EDITORIAL_RUN_INSERT: ABSENT
PRODUCTION_EDITORIAL_RUN_STATE_OWNER: ABSENT
PRODUCTION_RUN_CONTEXT_CLOSED_EVENT: ABSENT
LEGACY_EDITORIAL_RUNS_REUSABLE: NO
```

The current v17 field `source_run_row_id` is visible in
`EditorialMigrationSpec.java:85` and `EditorialClosedRunContextDao.java:27,
120,220-223`. It is nullable provenance on a closed-context row, not a
reviewed run authority. It cannot supply immutable attempt identity, frozen
manifest authority, trusted evaluation provenance, ROOT/CHILD intent, exact
parent selection or process-death recovery.

### 3.2 Why an AUTHORITATIVE legacy reference is not permitted

There is no valid reason to retain `source_run_row_id` with
`source_run_selector_authority = AUTHORITATIVE`. That combination in the A1
draft was an unresolved contradiction and is superseded by this freeze.

The final v18 schema has no FK, selector-authority field or authoritative
identity field pointing to `editorial_runs`. The existing nullable v17 field
is not altered, backfilled or reinterpreted; it remains historical v17
provenance only. New v18 declaration/event rows do not depend on it. A future
historical-link requirement would need a separately approved non-authoritative
provenance contract; it is not part of v18.

Therefore:

```text
LEGACY_EDITORIAL_RUNS_AUTHORITY: PROHIBITED
LEGACY_RUN_PROVENANCE: REMOVED from the v18 authoritative selector schema
```

### 3.3 How multiple legitimate attempts are distinguished

`source_run_row_id = NULL` is not an ambiguity under the frozen model because
the authoritative declaration stores a DAO-allocated `run_attempt_ordinal`
and derives `authoritative_run_identity` from the complete declaration
projection. Two legitimate attempts with the same project, scope, evaluation,
run kind, phase and frozen manifest receive different ordinals and therefore
different declaration identities. The ordinal is not a timestamp, row ID or
random UUID.

An explicit retry does not allocate an ordinal. It selects the existing
declaration and requires exact canonical readback. A new authorized attempt
uses a distinct reviewed attempt-request selector and allocates the next
ordinal in the same serialized allocation scope.

### 3.4 When identity is created and who owns it

The identity is created **before closure**, in the run-declaration transaction.
This preserves ROOT/CHILD intent and the exact parent across process death
before closure. A closure event can then be appended later without inventing a
new attempt.

`EditorialAuthoritativeRunDeclarationDao` is the sole owner of the
authoritative ordinal allocation and declaration identity construction. The
existing v17 `EditorialClosedRunContextDao` must not independently allocate a
second authoritative ordinal in A2. Its future transaction-scoped append path
must consume and cross-check the declaration-owned ordinal; it must not accept
an ordinal supplied by an external caller. The v17 closed-context
`run_attempt_ordinal` remains a denormalized readback/cross-check field until a
separately approved implementation changes that adapter boundary.

## 4. Authoritative run declaration contract

### 4.1 Declaration responsibilities

The future production lifecycle owner creates one immutable declaration after
re-reading the project revision, scope snapshot, compatibility evaluation,
frozen manifest and exact parent facts. It does not create compatibility
evidence or choose a latest row. The declaration DAO then:

1. validates the reviewed command and the explicit retry/new-attempt intent;
2. serializes allocation over the allocation scope;
3. allocates the next nonnegative ordinal only for a new authorized attempt;
4. computes declaration identity and fingerprint from canonical bytes;
5. appends and reads back the exact row;
6. maps duplicate/collision/race outcomes to stable result codes.

No public API accepts a caller-provided authoritative ordinal, identity,
fingerprint, timestamp or legacy row ID.

### 4.2 Attempt command contract

The future owner has two explicit, mutually exclusive dispositions:

```text
RETRY_SAME_ATTEMPT
  Input: exact declaration selector
  Effect: read the existing declaration; ordinal and identity are unchanged

NEW_AUTHORIZED_ATTEMPT
  Input: reviewed lifecycle facts plus a stable attempt-request selector
  Effect: allocate exactly one new ordinal for that request selector
```

`attempt_request_selector` is an idempotency selector, not a run identity and
not an ordinal. For a new attempt it must be produced/validated by the future
reviewed lifecycle owner from explicit user/workflow authorization. It may not
be a current timestamp, random UUID, database row ID, display name, URI/path
or caller-supplied precomputed authoritative hash. If a new-attempt command has
no stable reviewed request selector, the operation fails with
`ATTEMPT_REQUEST_SELECTOR_REQUIRED` and writes nothing.

This is the necessary distinction between two legitimate new attempts and a
double-click replay. A double-click repeats the same request selector and
returns the existing declaration. A deliberate new attempt must carry a new
reviewed authorization selector. It cannot be inferred from time, row order or
parent absence.

### 4.3 Allocation key and concurrency

The allocation scope is exactly:

```text
(project_revision_identity,
 input_scope_snapshot_identity,
 compatibility_evaluation_id,
 run_kind,
 phase_identity,
 frozen_manifest_fingerprint)
```

The compatibility evaluation selector is the trusted-context discriminator;
its immutable row supplies pack/profile/machine facts. ROOT/CHILD and exact
parent are semantic declaration intent and are included in the declaration
identity, but not used to create separate ordinal sequences.

The declaration DAO must execute a serialized SQLite write transaction
(`BEGIN IMMEDIATE` or the repository's equivalent) before reading the next
ordinal. It reads the maximum ordinal for the allocation scope, inserts with
the unique allocation index, and exact-reads the result. If two processes
submit the same new-attempt selector, the unique request index makes one row
authoritative and the other returns `DECLARATION_ALREADY_EXISTS` after exact
readback. If two distinct authorized selectors race, SQLite serialization and
the unique `(allocation_scope, run_attempt_ordinal)` index assign distinct
ordinals.

Stable declaration results include at least:

```text
DECLARATION_APPENDED
DECLARATION_ALREADY_EXISTS
DECLARATION_IMMUTABLE_COLLISION
ATTEMPT_REQUEST_SELECTOR_REQUIRED
ATTEMPT_ALLOCATION_CONFLICT
DECLARATION_NOT_FOUND
DECLARATION_SELECTOR_AMBIGUOUS
RUN_INTENT_MISMATCH
ROOT_HAS_PARENT
CHILD_PARENT_REQUIRED
PARENT_NOT_FOUND
PARENT_AMBIGUOUS
FROZEN_MANIFEST_MISMATCH
TRUSTED_CONTEXT_MISMATCH
```

The DAO never exposes `INSERT OR REPLACE`, update, delete, backfill, latest
selection or ordinal injection.

### 4.4 Declaration identity and fingerprint

Identity domain:
`EDITORIAL_AUTHORITATIVE_RUN_DECLARATION_IDENTITY_V1`.

Fingerprint domain:
`EDITORIAL_AUTHORITATIVE_RUN_DECLARATION_FINGERPRINT_V1`.

The canonical identity projection contains, in deterministic field order:

```text
contract_version
attempt_request_selector
project_revision_identity
input_scope_snapshot_identity
compatibility_evaluation_id
run_kind
phase_identity
frozen_manifest_fingerprint
node_kind
parent_record_identity (null for ROOT)
run_attempt_ordinal
```

The fingerprint projection contains the same semantic fields plus
`frozen_manifest_reference`. `created_at` is audit metadata and is excluded
from both projections. Strict UTF-8, deterministic canonical JSON, lowercase
64-hex hashes, explicit null handling and domain separation are mandatory.
Same identity/same canonical bytes is idempotent. Same identity/different
fingerprint is `DECLARATION_IMMUTABLE_COLLISION`; no update or second row is
allowed.

## 5. ROOT/CHILD intent placement and stability

The intent is persisted in **both** the declaration and closure event, with
exact cross-checks. This is intentional duplication of immutable facts, not a
second authority.

| Concern | Owner and rule |
|---|---|
| Create reviewed intent | Future reviewed UI/workflow command, not display text. |
| Verify intent | `EditorialAuthoritativeRunLifecycleService`; exact parent lookup through the current lineage validator. |
| Persist first | Declaration DAO, before closure. |
| Persist closure evidence | Closure-event DAO, only after declaration re-read and cross-check. |
| ROOT | Explicit `ROOT`; parent must be SQL NULL and semantic parent must be absent. |
| CHILD | Explicit `CHILD`; exactly one `parent_record_identity`; zero/multiple/mismatch fails closed. |
| Retry | Exact declaration/event selector only; no parent lookup by latest/order. |
| New attempt | A new reviewed request may choose a new explicit intent, but it never automatically reparents a prior attempt. |
| Current source | No production component currently owns this intent; the future lifecycle API is dormant/fail-closed until one is reviewed. |

`L1_CLOSED` and `L2_CLOSED` remain chapter states in
`EditorialSafe4Workflow.java:30-55,133-145`; they do not create or imply a
declaration/event. MainActivity, ZIP import, startup, page rebuild, model
completion and chapter preparation are not owners.

## 6. Frozen SQLite v18 schema (proposal, not implemented)

### 6.1 Table count and legacy boundary

```text
V18_TABLE_COUNT: 2
V18_FINAL_DDL: FROZEN
```

The two additive tables are:

1. `editorial_authoritative_run_declarations`
2. `editorial_run_closure_events`

There is no `source_run_row_id`, no `source_run_selector_authority`, and no
foreign key to `editorial_runs` in either v18 table. v17 is not altered. Any
existing v17 `source_run_row_id` remains historical v17 data and cannot
authorize a declaration or closure event.

### 6.2 Exact draft DDL

The following is the frozen A2 draft. It is intentionally shown as design
text only; it was not executed.

```sql
CREATE TABLE IF NOT EXISTS editorial_authoritative_run_declarations (
    declaration_identity TEXT PRIMARY KEY NOT NULL
        CHECK(length(declaration_identity)=64
          AND declaration_identity NOT GLOB '*[^0-9a-f]*'),
    declaration_fingerprint TEXT NOT NULL UNIQUE
        CHECK(length(declaration_fingerprint)=64
          AND declaration_fingerprint NOT GLOB '*[^0-9a-f]*'),
    run_declaration_contract_version TEXT NOT NULL
        CHECK(length(trim(run_declaration_contract_version))>0),
    attempt_request_selector TEXT NOT NULL
        CHECK(length(trim(attempt_request_selector))>0),
    project_revision_identity TEXT NOT NULL
        CHECK(length(project_revision_identity)=64
          AND project_revision_identity NOT GLOB '*[^0-9a-f]*'),
    input_scope_snapshot_identity TEXT NOT NULL
        CHECK(length(input_scope_snapshot_identity)=64
          AND input_scope_snapshot_identity NOT GLOB '*[^0-9a-f]*'),
    compatibility_evaluation_id TEXT NOT NULL,
    run_kind TEXT NOT NULL CHECK(length(trim(run_kind))>0),
    phase_identity TEXT NOT NULL CHECK(length(trim(phase_identity))>0),
    frozen_manifest_fingerprint TEXT NOT NULL
        CHECK(length(frozen_manifest_fingerprint)=64
          AND frozen_manifest_fingerprint NOT GLOB '*[^0-9a-f]*'),
    frozen_manifest_reference TEXT NOT NULL
        CHECK(length(trim(frozen_manifest_reference))>0),
    node_kind TEXT NOT NULL CHECK(node_kind IN ('ROOT','CHILD')),
    parent_record_identity TEXT
        CHECK(parent_record_identity IS NULL
           OR (length(parent_record_identity)=64
           AND parent_record_identity NOT GLOB '*[^0-9a-f]*')),
    run_attempt_ordinal INTEGER NOT NULL CHECK(run_attempt_ordinal>=0),
    created_at INTEGER NOT NULL CHECK(created_at>=0),
    CHECK((node_kind='ROOT' AND parent_record_identity IS NULL)
       OR (node_kind='CHILD' AND parent_record_identity IS NOT NULL)),
    FOREIGN KEY(project_revision_identity)
        REFERENCES editorial_project_revisions(revision_identity)
        ON DELETE RESTRICT,
    FOREIGN KEY(input_scope_snapshot_identity)
        REFERENCES editorial_input_scope_snapshots(scope_snapshot_identity)
        ON DELETE RESTRICT,
    FOREIGN KEY(compatibility_evaluation_id)
        REFERENCES editorial_pack_compatibility_evaluations(evaluation_id)
        ON DELETE RESTRICT,
    FOREIGN KEY(parent_record_identity)
        REFERENCES editorial_lineage_records(record_identity)
        ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS
    idx_authoritative_run_declarations_attempt_request
    ON editorial_authoritative_run_declarations(attempt_request_selector);

CREATE UNIQUE INDEX IF NOT EXISTS
    idx_authoritative_run_declarations_allocation
    ON editorial_authoritative_run_declarations(
        project_revision_identity,
        input_scope_snapshot_identity,
        compatibility_evaluation_id,
        run_kind,
        phase_identity,
        frozen_manifest_fingerprint,
        run_attempt_ordinal
    );

CREATE INDEX IF NOT EXISTS
    idx_authoritative_run_declarations_scope
    ON editorial_authoritative_run_declarations(
        project_revision_identity,
        input_scope_snapshot_identity,
        compatibility_evaluation_id,
        run_kind,
        phase_identity,
        frozen_manifest_fingerprint,
        run_attempt_ordinal,
        declaration_identity
    );

CREATE TABLE IF NOT EXISTS editorial_run_closure_events (
    closure_event_identity TEXT PRIMARY KEY NOT NULL
        CHECK(length(closure_event_identity)=64
          AND closure_event_identity NOT GLOB '*[^0-9a-f]*'),
    closure_event_fingerprint TEXT NOT NULL UNIQUE
        CHECK(length(closure_event_fingerprint)=64
          AND closure_event_fingerprint NOT GLOB '*[^0-9a-f]*'),
    closure_event_contract_version TEXT NOT NULL
        CHECK(length(trim(closure_event_contract_version))>0),
    authoritative_run_identity TEXT NOT NULL UNIQUE
        CHECK(length(authoritative_run_identity)=64
          AND authoritative_run_identity NOT GLOB '*[^0-9a-f]*'),
    project_revision_identity TEXT NOT NULL
        CHECK(length(project_revision_identity)=64
          AND project_revision_identity NOT GLOB '*[^0-9a-f]*'),
    input_scope_snapshot_identity TEXT NOT NULL
        CHECK(length(input_scope_snapshot_identity)=64
          AND input_scope_snapshot_identity NOT GLOB '*[^0-9a-f]*'),
    compatibility_evaluation_id TEXT NOT NULL,
    run_kind TEXT NOT NULL CHECK(length(trim(run_kind))>0),
    phase_identity TEXT NOT NULL CHECK(length(trim(phase_identity))>0),
    frozen_manifest_fingerprint TEXT NOT NULL
        CHECK(length(frozen_manifest_fingerprint)=64
          AND frozen_manifest_fingerprint NOT GLOB '*[^0-9a-f]*'),
    frozen_manifest_reference TEXT NOT NULL
        CHECK(length(trim(frozen_manifest_reference))>0),
    node_kind TEXT NOT NULL CHECK(node_kind IN ('ROOT','CHILD')),
    parent_record_identity TEXT
        CHECK(parent_record_identity IS NULL
           OR (length(parent_record_identity)=64
           AND parent_record_identity NOT GLOB '*[^0-9a-f]*')),
    run_attempt_ordinal INTEGER NOT NULL CHECK(run_attempt_ordinal>=0),
    closure_eligibility TEXT NOT NULL
        CHECK(closure_eligibility='ELIGIBLE'),
    closure_attestation_version TEXT NOT NULL
        CHECK(length(trim(closure_attestation_version))>0),
    closure_attestation_fingerprint TEXT NOT NULL
        CHECK(length(closure_attestation_fingerprint)=64
          AND closure_attestation_fingerprint NOT GLOB '*[^0-9a-f]*'),
    appended_at INTEGER NOT NULL CHECK(appended_at>=0),
    CHECK((node_kind='ROOT' AND parent_record_identity IS NULL)
       OR (node_kind='CHILD' AND parent_record_identity IS NOT NULL)),
    FOREIGN KEY(authoritative_run_identity)
        REFERENCES editorial_authoritative_run_declarations(declaration_identity)
        ON DELETE RESTRICT,
    FOREIGN KEY(project_revision_identity)
        REFERENCES editorial_project_revisions(revision_identity)
        ON DELETE RESTRICT,
    FOREIGN KEY(input_scope_snapshot_identity)
        REFERENCES editorial_input_scope_snapshots(scope_snapshot_identity)
        ON DELETE RESTRICT,
    FOREIGN KEY(compatibility_evaluation_id)
        REFERENCES editorial_pack_compatibility_evaluations(evaluation_id)
        ON DELETE RESTRICT,
    FOREIGN KEY(parent_record_identity)
        REFERENCES editorial_lineage_records(record_identity)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_run_closure_events_selector
    ON editorial_run_closure_events(authoritative_run_identity);

CREATE INDEX IF NOT EXISTS idx_run_closure_events_context
    ON editorial_run_closure_events(
        project_revision_identity,
        input_scope_snapshot_identity,
        compatibility_evaluation_id,
        run_kind,
        phase_identity,
        run_attempt_ordinal,
        closure_event_identity
    );

CREATE TRIGGER IF NOT EXISTS trg_authoritative_run_declarations_no_update
BEFORE UPDATE ON editorial_authoritative_run_declarations
BEGIN
    SELECT RAISE(ABORT, 'editorial_authoritative_run_declarations are immutable');
END;

CREATE TRIGGER IF NOT EXISTS trg_authoritative_run_declarations_no_delete
BEFORE DELETE ON editorial_authoritative_run_declarations
BEGIN
    SELECT RAISE(ABORT, 'editorial_authoritative_run_declarations are immutable');
END;

CREATE TRIGGER IF NOT EXISTS trg_run_closure_events_no_update
BEFORE UPDATE ON editorial_run_closure_events
BEGIN
    SELECT RAISE(ABORT, 'editorial_run_closure_events are immutable');
END;

CREATE TRIGGER IF NOT EXISTS trg_run_closure_events_no_delete
BEFORE DELETE ON editorial_run_closure_events
BEGIN
    SELECT RAISE(ABORT, 'editorial_run_closure_events are immutable');
END;
```

SQL checks enforce shape and retention. Cross-row semantic checks remain DAO
or lifecycle-service obligations:

- declaration project/scope compatibility and manifest equality;
- evaluation's trusted pack/profile/machine facts;
- declaration/event exact cross-field equality;
- event `run_attempt_ordinal` equals declaration ordinal;
- exact parent project/scope/contract match through the lineage validator;
- frozen-manifest reference is a stable attestation reference, not a local
  path, SAF URI or content payload;
- one byte of semantic canonical change changes identity/fingerprint;
- no caller-provided identity or fingerprint is trusted without recomputation.

No table stores a mutable lifecycle status. The following statuses are
projections:

```text
DECLARED        = declaration exists; no exact closure event exists
EVENT_APPENDED  = declaration and exact closure event exist
CLOSED_UNBOUND  = EVENT_APPENDED + v17 closed context + no exact binding
LINEAGE_BOUND   = exact v17 binding exists and readback matches
```

`CLOSED_UNBOUND` remains the B1 Option-B projection; it is not a v18 row or
status update.

### 6.3 Closure event identity and replay

Identity domain:
`EDITORIAL_CLOSURE_EVENT_IDENTITY_V1`.

Fingerprint domain:
`EDITORIAL_CLOSURE_EVENT_FINGERPRINT_V1`.

Event identity includes event contract version, authoritative run identity,
project revision, scope snapshot, evaluation selector, run kind, phase, frozen
manifest fingerprint, explicit node kind, exact parent selector, closure
eligibility and attestation version. The fingerprint additionally includes
the stable manifest reference and closure-attestation fingerprint. `appended_at`
is audit metadata and is excluded from both hashes.

The declaration FK and `UNIQUE(authoritative_run_identity)` enforce one event
per authoritative run. The repeated declaration facts are required for exact
readback and DAO cross-check; the declaration remains the ordinal authority.

| Input/result | Required behavior |
|---|---|
| Same event identity and same canonical bytes | `EVENT_ALREADY_EXISTS`; exact readback, unchanged `appended_at`. |
| Same event identity and different bytes | `EVENT_IMMUTABLE_COLLISION`; no update/second row. |
| Same authoritative run, changed ROOT/CHILD | `EVENT_INTENT_COLLISION`; no second event. |
| Same authoritative run, changed parent | `EVENT_PARENT_COLLISION`; no reparent. |
| Zero selector match | `EVENT_NOT_FOUND`; no B1 call. |
| Multiple selector matches | `EVENT_SELECTOR_AMBIGUOUS`; no B1 call. |
| Pack/profile/evaluation/scope mismatch | `EVENT_TRUSTED_CONTEXT_MISMATCH`; no append. |
| ROOT with parent | `ROOT_HAS_PARENT`; no append. |
| CHILD without exact parent | `CHILD_PARENT_REQUIRED`, `PARENT_NOT_FOUND`, `PARENT_AMBIGUOUS` or exact validator code; no append. |

## 7. Transaction boundaries

### 7.1 Declaration transaction

One SQLite transaction owns validation of the durable declaration boundary:

1. resolve the minimal reviewed command;
2. for retry, exact-read the declaration and verify no semantic drift;
3. for a new attempt, re-read project/scope/evaluation/frozen-manifest facts;
4. resolve ROOT/CHILD and exact parent;
5. allocate the DAO ordinal under the exact allocation scope;
6. compute identity/fingerprint, append and exact-read back;
7. commit only after all cross-row checks pass.

Any failure rolls back the new declaration and allocation effect. A previously
existing declaration remains unchanged.

### 7.2 Closure-event transaction

The event transaction is separate from declaration allocation:

1. resolve exactly one declaration selector;
2. re-read declaration, project revision, scope snapshot and evaluation;
3. verify frozen-manifest equality and trusted `DATA_COMPATIBLE` eligibility;
4. verify event ROOT/CHILD and exact parent against the declaration and current
   lineage validator;
5. append event or exact-read an existing event;
6. exact-read all event bytes and commit.

No new attempt ordinal is allocated in this transaction. If eligibility is
blocked or facts mismatch, no event is written.

### 7.3 B1 Option-B transaction remains separate

The approved B1 sequence remains:

```text
resolve immutable closure event
 -> close/read v17 context using declaration-owned ordinal cross-check
 -> C1 validateAndPrepare
 -> one-connection lineage append + binding transaction
 -> exact lineage/binding readback
```

The future v17 adapter must reject a closed context whose ordinal, project,
scope, evaluation, run kind, phase or manifest does not equal the declaration
and event. It may not allocate a replacement ordinal. This keeps the existing
Option-B recovery boundary while removing the possibility of two independent
authoritative ordinal owners.

## 8. Legacy provenance and retention boundary

Final v18 declaration/event rows do not store a legacy run reference. Existing
v17 `source_run_row_id` is neither a selector nor authority and remains
unchanged because v17 history is not rewritten. No v18 FK to `editorial_runs`
is permitted. No backfill may convert a legacy row into a declaration/event.

Before any future project/chapter/run deletion, the lifecycle-aware retention
preflight must check declaration, event, v17 closed-context and lineage/binding
references. The result must be a stable domain code such as
`AUTHORITATIVE_REFERENCE_PRESENT` or `CLEAR_TO_DELETE`; raw
`SQLiteConstraintException` must not reach UI. No cascade, detach, authoritative
delete or reparent is allowed. Delete UX wiring is not part of A1.5 and belongs
to a separately approved retention phase (C2-B2 or an explicitly named phase).

## 9. Future migration requirements and matrix

The future v17->v18 migration must be additive, transactional and zero
backfill:

- create exactly the two tables, their indexes and four immutable triggers;
- do not alter/rebuild v15, v16 or v17 tables;
- do not insert declaration/event rows;
- do not reinterpret or delete `editorial_runs`;
- do not change pack, evaluation, closed-context, lineage or binding rows;
- preserve exact existing row counts and canonical hashes;
- rollback the entire DDL transaction if any table/index/trigger fails;
- reject downgrade rather than destructively rewriting history.

| Migration scenario | Required evidence |
|---|---|
| Fresh v18 | Full chain creates v18 objects empty; no declaration/event backfill. |
| v17 -> v18 | Exactly two new tables, indexes and triggers; v17 rows/counts/hashes unchanged. |
| v13 -> v14 -> v15 -> v16 -> v17 -> v18 | Existing additive chain remains unchanged; v18 adds no rows and does not reinterpret legacy runs. |
| Reopen at v18 | Version guard is idempotent; no duplicate objects or rows. |
| Failed DDL/index/trigger | Whole migration rolls back and the database remains readable as v17. |
| Concurrent new attempts | Serialized DAO allocation gives one ordinal per stable request and distinct ordinals for distinct authorized requests. |
| Immutability/retention | Update/delete trigger rejection and all FK `ON DELETE RESTRICT` checks pass. |
| Process death | Declaration survives before closure; retry selector reads the same declaration; event append/replay cannot alter intent. |

## 10. A2 DAO contract, stable vocabulary and stop conditions

The next implementation phase may create only append/read-only APIs equivalent
to:

```text
EditorialAuthoritativeRunDeclarationDao
  appendNewAuthorizedAttempt(reviewed command)
  findByDeclarationIdentity(selector)
  findByAttemptRequestSelector(selector)
  listByAllocationScope(scope selector)
  allocateNextOrdinalInsideTransaction(scope)

EditorialRunClosureEventDao
  appendOrReadback(exact declaration/event facts)
  findByClosureEventIdentity(selector)
  findByAuthoritativeRunIdentity(selector)
  listByContext(selector)       // deterministic, never "latest"
```

Stable persistence outcomes include:

```text
APPENDED
ALREADY_EXISTS
DUPLICATE_IMMUTABLE_RECORD
IMMUTABLE_COLLISION
NOT_FOUND
AMBIGUOUS
FOREIGN_REFERENCE_MISSING
FOREIGN_REFERENCE_RESTRICTED
TRUSTED_CONTEXT_MISMATCH
PARENT_NOT_FOUND
PARENT_AMBIGUOUS
ATTEMPT_ALLOCATION_CONFLICT
```

The following are explicit A2 stop conditions:

- the implementation needs a third authority for ordinal/identity;
- a caller supplies an ordinal, identity, hash or timestamp as authority;
- the implementation needs to populate or FK `editorial_runs`;
- ROOT/CHILD is inferred from missing parent or chapter state;
- a latest/fallback selector is introduced;
- v17 history must be altered, backfilled or reinterpreted;
- one transaction cannot preserve declaration intent across process death;
- a requested feature requires profile/catalog/capability/execution changes.

## 11. Acceptance matrix

| Acceptance criterion | Frozen answer/evidence |
|---|---|
| No authoritative FK to legacy run | Yes; v18 DDL contains none. |
| Multiple legitimate attempts distinguishable | Yes; declaration-owned ordinal plus immutable declaration identity. |
| Explicit retry does not allocate | Yes; retry selects exact declaration identity. |
| DAO owns ordinal/identity | Yes; `EditorialAuthoritativeRunDeclarationDao`. |
| Double-click is idempotent | Yes; stable attempt-request selector unique constraint. |
| Two distinct authorized attempts are distinct | Yes; distinct reviewed request selectors and serialized next ordinals. |
| ROOT/CHILD cannot drift | Yes; persisted in declaration and event, exact cross-check, no fallback. |
| Event unique selector does not block new attempt | Yes; uniqueness is per authoritative declaration identity; ordinal changes declaration identity. |
| Lossless event/declaration semantics | Yes; two immutable tables retain identity, fingerprint, intent, parent, manifest attestation and audit timestamps. |
| A2 has no unresolved architecture choice | Yes; table count, fields, owner, transactions, errors and migration matrix are frozen here. |

## 12. Verification and review stop

Read-only verification for this documentation phase:

- `:editorial-engine:test`: baseline `105/105` PASS; retained aggregate JVM
  baseline is `269/269` PASS with zero failures/errors/skips.
- `:app:testDebugUnitTest`: baseline `164/164` PASS.
- JDK/toolchain preflight: JBR/JDK 21.0.10, required major 21, PASS.
- `git diff --check`: PASS after documentation edits.
- No APK build/install was run because source code did not change.
- No migration, database row, profile/catalog mutation, production closure,
  lineage/binding row, D: candidate access, caller wiring, promotion,
  certification, activation or execution was performed.

The next exact action is a separately approved implementation of
`C2B2A2_SCHEMA_IMPLEMENTATION` using this frozen two-table declaration/event
contract. Do not begin A2 automatically. `C2B2A3` remains not started.

```text
G2-C1B1C2B2A15_SCHEMA_FREEZE: PASS
AUTHORITATIVE_RUN_IDENTITY_MODEL: DECLARATION
LEGACY_EDITORIAL_RUNS_AUTHORITY: PROHIBITED
LEGACY_RUN_PROVENANCE: REMOVED
RUN_ATTEMPT_ORDINAL_OWNER: DAO
RETRY_VS_NEW_ATTEMPT_CONTRACT: DECIDED
ROOT_CHILD_INTENT_STORAGE: DECIDED
V18_TABLE_COUNT: 2
V18_FINAL_DDL: FROZEN
C2B2A2_SCHEMA_IMPLEMENTATION: NOT_STARTED
LINEAGE_PROMOTION: BLOCKED
SAFE4_EXECUTION_READINESS: BLOCKED
```
