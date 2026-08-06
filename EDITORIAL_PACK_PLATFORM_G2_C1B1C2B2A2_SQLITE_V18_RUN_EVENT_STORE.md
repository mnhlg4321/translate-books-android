# G2-C1B1-C2B2A-2 — SQLite v18 Run Declaration/Event Store

Status: `G2-C1B1C2B2A2_SCHEMA_DAO: PASS`

This handoff records the A2 implementation only. It stops at the immutable run declaration and closure-event storage boundary. It does not implement `EditorialAuthoritativeRunLifecycleService`, a production closure-event resolver composition, B1 caller wiring, production run creation, capability promotion, certification, activation, project binding or execution.

## Baseline and end state

- Baseline branch: `feature/v4.16-g2-c1b1c2b2a15`
- Baseline HEAD: `4d0b9a0ce906bdbad381a2a4e0aaaf67ac3ee2e9`
- Baseline SQLite: v17
- Work branch: `feature/v4.16-g2-c1b1c2b2a2`
- Implementation HEAD before this documentation group: `293e77be3354e3f88d97295c0dae85716c33c085`
- Source version: `TranslationRepository.VER = 18`
- Protected files: `.idea/compiler.xml`, `.idea/gradle.xml`, `.idea/misc.xml` remained user-owned, unchanged by this work, unstaged and uncommitted.

Implementation commits:

1. `fe2f4f8` — pure-JVM authoritative declaration and closure-event models/canonicalization/tests.
2. `6fc5f89` — v17→v18 migration/spec, append/read-only DAOs, result vocabulary and isolated instrumentation coverage.
3. `293e77b` — closure-event request-boundary tightening: selector/intent-only draft, authoritative declaration derivation, trusted permit cross-check, and focused test updates.

## Frozen identity contract

The implementation uses the frozen domains exactly:

- Declaration identity: `EDITORIAL_AUTHORITATIVE_RUN_DECLARATION_IDENTITY_V1`
- Declaration fingerprint: `EDITORIAL_AUTHORITATIVE_RUN_DECLARATION_FINGERPRINT_V1`
- Closure-event identity: `EDITORIAL_CLOSURE_EVENT_IDENTITY_V1`
- Closure-event fingerprint: `EDITORIAL_CLOSURE_EVENT_FINGERPRINT_V1`

Canonical projections use strict UTF-8, domain-separated SHA-256, deterministic canonical JSON and explicit null. Hash values are lowercase 64-hex. Identity/fingerprint inputs are independent of locale, timezone, insertion order, row ID, timestamp, local path, SAF URI, book content and prompt body.

`EditorialAuthoritativeRunDeclarationDraft` contains semantic input only. It does not accept declaration identity, declaration fingerprint, attempt ordinal or timestamp. `EditorialRunClosureEventDraft` is even narrower: it contains the reviewed attempt selector, explicit ROOT/CHILD intent, exact parent selector where CHILD is used, and closure eligibility. It does not accept authoritative run identity, project/scope/evaluation identity, manifest fingerprint/reference, closure-event identity/fingerprint, ordinal or timestamp.

The event DAO resolves the exact declaration by the unique attempt selector. The declaration supplies project, scope, evaluation, run kind, phase, manifest, authoritative run identity and ordinal. A package-private `EditorialClosureEventAppendPermit` supplies trusted compatibility/attestation facts; a public caller cannot fabricate a trusted append permit. Event identity repeats the declaration context plus event contract, explicit node intent, exact parent, ordinal, eligibility and attestation version. Event fingerprint adds the frozen-manifest reference and closure-attestation fingerprint. `createdAt` and `appendedAt` are audit metadata only.

One-byte semantic changes invalidate the applicable identity/fingerprint. Changing only manifest reference or attestation fingerprint changes the fingerprint as specified. Exact replay with a different clock preserves the stored identity/fingerprint and timestamp.

## v18 schema

Migration creates exactly two additive tables and does not alter or rebuild v15, v16 or v17 tables.

### `editorial_authoritative_run_declarations`

Fields and constraints:

- `declaration_identity TEXT PRIMARY KEY NOT NULL`, lowercase 64-hex check.
- `declaration_fingerprint TEXT NOT NULL UNIQUE`, lowercase 64-hex check.
- `run_declaration_contract_version TEXT NOT NULL`.
- `attempt_request_selector TEXT NOT NULL`.
- `project_revision_identity TEXT NOT NULL`, FK to `editorial_project_revisions(revision_identity)` with `ON DELETE RESTRICT`.
- `input_scope_snapshot_identity TEXT NOT NULL`, FK to `editorial_input_scope_snapshots(scope_snapshot_identity)` with `ON DELETE RESTRICT`.
- `compatibility_evaluation_id TEXT NOT NULL`, FK to `editorial_pack_compatibility_evaluations(evaluation_id)` with `ON DELETE RESTRICT`.
- `run_kind TEXT NOT NULL` and `phase_identity TEXT NOT NULL`.
- `frozen_manifest_fingerprint TEXT NOT NULL`, lowercase 64-hex check.
- `frozen_manifest_reference TEXT NOT NULL`.
- `node_kind TEXT NOT NULL CHECK(node_kind IN ('ROOT','CHILD'))`.
- `parent_record_identity TEXT NULL`, exact lineage FK with `ON DELETE RESTRICT`.
- `run_attempt_ordinal INTEGER NOT NULL CHECK(run_attempt_ordinal >= 0)`.
- `created_at INTEGER NOT NULL CHECK(created_at >= 0)`.
- Structural check: ROOT requires NULL parent; CHILD requires a non-NULL parent.

Indexes:

- `idx_authoritative_run_declarations_attempt_request` unique on `attempt_request_selector`.
- `idx_authoritative_run_declarations_allocation` unique on the allocation scope plus `run_attempt_ordinal`.
- `idx_authoritative_run_declarations_scope` deterministic allocation-scope lookup.

### `editorial_run_closure_events`

Fields and constraints:

- `closure_event_identity TEXT PRIMARY KEY NOT NULL`, lowercase 64-hex check.
- `closure_event_fingerprint TEXT NOT NULL UNIQUE`, lowercase 64-hex check.
- `closure_event_contract_version TEXT NOT NULL`.
- `authoritative_run_identity TEXT NOT NULL UNIQUE`, FK to declaration identity with `ON DELETE RESTRICT`; this enforces one event per declaration.
- Exact cross-check fields: project revision, input scope, compatibility evaluation, run kind, phase, frozen-manifest fingerprint, node kind, parent and attempt ordinal.
- `frozen_manifest_reference TEXT NOT NULL`.
- `closure_eligibility TEXT NOT NULL CHECK(closure_eligibility = 'ELIGIBLE')`.
- `closure_attestation_version TEXT NOT NULL`.
- `closure_attestation_fingerprint TEXT NOT NULL`, lowercase 64-hex check.
- `appended_at INTEGER NOT NULL CHECK(appended_at >= 0)`.
- All identity/reference foreign keys use `ON DELETE RESTRICT`; there is no FK to legacy `editorial_runs` and no `source_run_row_id` in either v18 table.

Indexes:

- `idx_run_closure_events_selector` on `authoritative_run_identity`.
- `idx_run_closure_events_context` on deterministic context and ordinal ordering.

Triggers:

- `trg_authoritative_run_declarations_no_update` and `trg_authoritative_run_declarations_no_delete`.
- `trg_run_closure_events_no_update` and `trg_run_closure_events_no_delete`.

The migration has no mutable lifecycle status. `CLOSED_UNBOUND` remains a derived projection from a closed context/event with no exact binding; it is not persisted in v18. Existing v17 rows are not backfilled, reinterpreted or reevaluated.

## Migration and transaction behavior

`EditorialMigrationSpec.from17To18()` is the single additive DDL source. `TranslationRepository.onCreate()` creates the frozen v18 stores for fresh databases, and `onUpgrade()` invokes the v18 DDL only when `oldVersion < 18`. Android's SQLiteOpenHelper upgrade transaction covers the complete upgrade; any DDL/index/trigger failure rolls back the upgrade. Downgrade remains rejected.

The migration contract is:

- v17→v18: create exactly the two stores, five indexes and four triggers.
- Fresh v18: create empty stores with zero declaration/event rows.
- Earlier chain: v13→v14→v15→v16→v17→v18, with v18 stores empty.
- Failure injection: leave the database readable at v17 and do not leave partial v18 stores.
- Existing v17 row counts and stored canonical fields remain unchanged.
- No legacy `editorial_runs` FK, row insert, backfill or state rewrite.

Declaration append is one serialized SQLite transaction. The DAO first resolves the unique selector; exact semantic replay returns `ALREADY_EXISTS`, while same selector with different facts returns `ATTEMPT_REQUEST_COLLISION`. For a new authorized attempt, it validates all required foreign references, calculates `MAX(run_attempt_ordinal)+1` within the transaction allocation scope, creates the immutable model, inserts it, and performs exact readback before commit. The caller cannot supply identity, fingerprint, ordinal or timestamp. A failed transaction writes no partial declaration and consumes no committed ordinal.

Closure-event append is a separate transaction. It resolves exactly one declaration by selector, verifies the package-private trusted permit, rechecks evaluation and manifest facts against the declaration, validates ROOT/CHILD and exact parent, derives the event model, inserts it, and performs exact readback. Same declaration/event bytes return `ALREADY_EXISTS`; ROOT/CHILD or parent changes return `EVENT_INTENT_COLLISION`/`EVENT_PARENT_COLLISION`; mismatched trusted evaluation or manifest is rejected. A2 does not call the B1 coordinator after append.

## DAO and result boundary

`EditorialAuthoritativeRunDeclarationDao` exposes append, exact retry/read, find-by-identity, find-by-selector and deterministic allocation-scope listing. It exposes no update, delete, replace, backfill, latest or fallback operation.

`EditorialRunClosureEventDao` exposes package-bound append using the trusted permit, exact find-by-event-identity, find-by-authoritative-run-identity and deterministic context listing. It exposes no update, delete, replace, latest, fallback or automatic retry operation. Raw SQLite exceptions are mapped to stable persistence results and are not the UI/caller contract.

The stable vocabulary includes `APPENDED`, `ALREADY_EXISTS`, `DUPLICATE_IMMUTABLE_RECORD`, `IMMUTABLE_COLLISION`, `NOT_FOUND`, `AMBIGUOUS`, `FOREIGN_REFERENCE_MISSING`, `TRUSTED_CONTEXT_MISMATCH`, `FROZEN_MANIFEST_MISMATCH`, `ROOT_HAS_PARENT`, `CHILD_PARENT_REQUIRED`, `PARENT_NOT_FOUND`, `PARENT_AMBIGUOUS`, `ATTEMPT_REQUEST_SELECTOR_REQUIRED`, `ATTEMPT_REQUEST_COLLISION`, `ATTEMPT_ALLOCATION_CONFLICT`, `EVENT_INTENT_COLLISION`, `EVENT_PARENT_COLLISION`, `EVENT_DECLARATION_MISMATCH`, `ORDINAL_MISMATCH` and `PERSISTENCE_FAILURE`.

## Test and verification matrix

Pure JVM and app unit verification after the final source change:

- `:editorial-engine:test`: **116/116 PASS**, failures/errors/skips `0/0/0`.
- `:app:testDebugUnitTest`: **166/166 PASS**, failures/errors/skips `0/0/0`.
- Aggregate JVM: **282/282 PASS**.
- `:app:compileDebugAndroidTestJavaWithJavac`: PASS.
- `:app:lintDebug`: PASS, 0 errors / 53 existing warnings.
- `git diff --check`: PASS.
- Wrapper/toolchain: pinned Gradle **9.3.0** offline preflight PASS; JBR/OpenJDK **21.0.10**, source/target compatibility 17, AGP **8.7.3**, compile/target SDK **35**.

The isolated instrumentation class `EditorialV18RunDeclarationEventInstrumentedTest` passed **11/11** on the connected OnePlus CPH2691 / Android 15 device after installing the exact final archive. Coverage includes fresh v18, v17→v18, the full earlier migration chain, malformed migration rollback, declaration retry/collision, distinct and concurrent allocation, event replay/restart, trusted/context/manifest checks, child exact-parent behavior, immutability/FK retention and zero-backfill assertions.

No production database declaration/event row was created. Test databases were isolated and test-owned. The production database was empty at inspection; therefore `REAL_DATA_CONTINUITY: NOT_CLAIMED`.

## Build and artifact evidence

Archive-first build used `scripts/build-and-save.ps1 -Series 4.16-dev` from implementation commit `293e77be3354e3f88d97295c0dae85716c33c085`.

- Version: `4.16-dev.50`, Android code `112`.
- Event: `build-20260806-172210`.
- Artifact: `artifacts/builds/v4.16-dev.50/build-20260806-172210/`.
- Backup: `backup/builds/v4.16-dev.50/build-20260806-172210/`.
- APK SHA-256: `114451EC5FF27A2ACD0C75D4C5BC2EA5B8FE474AA13CA3EBDDD10F31934E6AF4`.
- Source ZIP SHA-256: `A88158C2B739DBAD887291D0BE0746F30939759F2EEA19F3F7F4AA8CD3AE603E`.
- Five-file artifact/backup parity: PASS.
- Device install: exact code112 archived APK installed with `adb install -r` over observed code111; no `pm clear`.

## Safety and deferred work

- `editorial_runs` remains legacy/mutable provenance only; v18 does not reference it.
- No lifecycle owner, production closure-event owner, production event resolver composition, MainActivity/UI/startup/importer/chapter-state caller or B1 caller wiring was added.
- No production `DATA_COMPATIBLE`, declaration/event, closed-run, lineage or binding row was created.
- Production profile/catalog and profile bytes remain unchanged; current profile still declares only `pack.integrity.sha256.v1` and is non-executable.
- `EditorialSafe4Pack.executionEnabled()` remains `false`.
- No migration beyond v18, certification, activation, project binding, capability promotion, Pronoun/Pair or model execution was started.
- Retention remains enforced by `ON DELETE RESTRICT` and immutable triggers; deletion UX integration is deferred to a separately approved lifecycle phase.

## Final review-stop status

```text
G2-C1B1C2B2A2_SCHEMA_DAO: PASS
SQLITE_SOURCE_VERSION: 18
V18_TABLE_COUNT: 2
AUTHORITATIVE_RUN_DECLARATION_MODEL: IMPLEMENTED
CLOSURE_EVENT_MODEL: IMPLEMENTED
DECLARATION_DAO: IMPLEMENTED
EVENT_DAO: IMPLEMENTED
DAO_OWNS_ATTEMPT_ORDINAL: YES
RETRY_NEW_ATTEMPT_CONTRACT: PROVEN
LEGACY_EDITORIAL_RUNS_AUTHORITY: PROHIBITED
ZERO_BACKFILL: PROVEN
DEVICE_MIGRATION_QA: PASS
REAL_DATA_CONTINUITY: NOT_CLAIMED
C2B2A3_LIFECYCLE_IMPLEMENTATION: NOT_STARTED
LINEAGE_PROMOTION: BLOCKED
SAFE4_EXECUTION_READINESS: BLOCKED
```

Exactly one next action: review and separately approve A3 lifecycle/event-owner implementation. Do not start A3 automatically from this A2 review stop.
