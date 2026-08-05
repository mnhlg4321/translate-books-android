# G2-C1B1-B — SQLite v16 Append-Only Lineage Persistence

## Review-stop result

```text
G2-C1B1B_PERSISTENCE: PASS
LINEAGE_CAPABILITY_PROMOTION: NOT_STARTED
SAFE4_EXECUTION_READINESS: BLOCKED
```

This phase implements only the additive SQLite v15 → v16 persistence boundary for
`lineage.exact-parent.v1`. It does not connect the DAO to the importer or runtime,
promote the capability, change a profile, certify a pack, activate/bind a project,
or enable execution.

## Branch and evidence identity

- Start branch: `feature/v4.16-g2-c1b1a`
- Start HEAD: `3f748ca78380d9bc6daa9f6367020874fc5b1ae6`
- C1A snapshot-correction commit: `3fcf039be0185b22567e75f34a970bf75ba5ea6a`
- B1B branch: `feature/v4.16-g2-c1b1b`
- Implementation commits: `c0539b9` (v16 schema/DAO/adapter/tests), `e65b723` (min-SDK adapter fix), `1d6fbae` (canonical immutable manifest entry ordering)
- End implementation baseline: `1d6fbae`; documentation/state commit follows this review evidence
- Protected worktree files: `.idea/compiler.xml`, `.idea/gradle.xml`, `.idea/misc.xml`; untouched, unstaged and uncommitted

Before opening B1B, the stale C1A snapshot Next step was corrected and committed
with evidence commit `3fcf039`; no other snapshot evidence was changed.

## v15 schema survey

The actual v15 source of truth was `TranslationRepository.VER` with
`TranslationRepository` calling `createEditorialPackCompatibilityEvaluationTables`
when upgrading from v14. `onConfigure` enables SQLite foreign keys. SQLiteOpenHelper
owns the upgrade transaction. The new explicit `onDowngrade` rejects downgrades with
`SQLiteException`; it does not drop or rewrite data.

The v15 chain is additive:

- v13 reference-profile tables and v14 pack tables are created by
  `EditorialMigrationSpec.from13To14()`.
- v14 pack tables include `editorial_packs` (primary key `id`; unique pack/version,
  canonical hash and storage key), `editorial_pack_imports` (primary key
  `import_id`, restricted pack foreign key), `editorial_pack_files` (restricted
  pack foreign key, unique role/path), and
  `editorial_pack_compatibility_results` (primary key `id`, restricted import/pack
  foreign keys, unique canonical-pack/engine/fingerprint identity).
- v15 adds `editorial_pack_compatibility_evaluations` with primary key
  `evaluation_id`, restricted import/pack/result foreign keys, trusted profile and
  evaluation-context provenance, and immutable update/delete triggers.
- Existing v15 pack identity is canonical pack hash plus pack id/version and storage
  key; evaluation identity is `evaluation_id` plus its immutable provenance fields.
  No existing v15 column is reused for lineage identity.

No v15 table is altered or rebuilt by B1B. Existing v15 rows are not backfilled,
reevaluated, or converted into roots.

## v16 schema

`EditorialMigrationSpec.from15To16()` adds exactly two tables, three indexes and
four immutability triggers.

### `editorial_lineage_records`

| Column | Constraint/meaning |
|---|---|
| `record_identity` | `TEXT PRIMARY KEY NOT NULL`, lowercase 64-hex identity |
| `record_fingerprint` | `TEXT NOT NULL UNIQUE`, lowercase 64-hex fingerprint |
| `canonical_pack_hash` | required lowercase SHA-256 |
| `trusted_profile_id`, `trusted_profile_version` | required non-empty trusted identity |
| `canonical_profile_hash` | required lowercase SHA-256 |
| `machine_contract_fingerprint` | required lowercase SHA-256 |
| `contract_version`, `schema_version` | required non-empty contract/schema identity |
| `project_identity`, `input_scope_identity`, `run_evaluation_identity` | required lineage scope |
| `input_manifest_version` | required manifest version |
| `input_manifest_fingerprint` | required lowercase SHA-256 |
| `node_kind` | `ROOT` or `CHILD` only |
| `parent_record_identity`, `parent_record_fingerprint` | both null for ROOT; both required for CHILD |
| `created_at` | non-negative audit timestamp; excluded from semantic fingerprint |

The parent identity has a self foreign key with `ON DELETE RESTRICT`. SQLite
checks enforce hash shape, non-empty identity tokens, node-kind/cardinality and
non-negative audit data. The DAO and pure-JVM validator enforce the semantic
invariants before transaction commit.

### `editorial_lineage_input_entries`

| Column | Constraint/meaning |
|---|---|
| `record_identity` | restricted foreign key to the record |
| `role` | required non-empty input role |
| `ordinal` | non-negative ordinal |
| `input_hash` | lowercase 64-hex SHA-256 |
| `byte_count`, `item_count` | non-negative counts |

The primary key is `(record_identity, role, ordinal)`. Entries are read in the
canonical role/ordinal order. Indexes cover pack, run/evaluation and parent lookup.
`BEFORE UPDATE` and `BEFORE DELETE` triggers reject mutation of both tables.

## Transaction and append-only contract

`EditorialLineageDao` exposes append/read operations only. It does not expose update,
delete, reparent, backfill, certification, activation, binding or execution APIs.

For every append the DAO:

1. Reads an existing identity and classifies an exact immutable repeat as
   `ALREADY_EXISTS` without changing `created_at`.
2. Runs `EditorialLineageValidator` through the read-only SQLite validation context.
3. Begins one SQLite transaction, inserts the record with `insertOrThrow`, inserts
   every manifest entry with `insertOrThrow`, and commits only after all entries
   succeed.
4. Rolls back the complete transaction on any entry/constraint failure. It never
   uses `INSERT OR REPLACE`, never manufactures a parent, and never repairs a chain.

Stable DAO boundary results are:

`APPENDED`, `ALREADY_EXISTS`, `DUPLICATE_LINEAGE`, `REPARENT_ATTEMPT`,
`ORPHAN_LINEAGE`, `PARENT_MISMATCH`, `AMBIGUOUS_PARENT`, `INVALID_LINEAGE`, and
`PERSISTENCE_FAILURE`. Raw SQLite errors do not become the external machine reason
code for a normal append outcome.

The database triggers reject direct UPDATE/DELETE attempts, including input-entry
deletion. Parent deletion is additionally blocked by both the immutable trigger and
the restricted foreign key. A failed append cannot leave a record or partial entry
set because record and entries share one transaction.

## Validator-context mapping

`SqliteEditorialLineageValidationContext` is an app-layer, read-only adapter. It
converts DAO rows into immutable engine facts and keeps Android/SQLite types out of
`editorial-engine`. The pure-JVM validator remains the source of truth for:

- exact parent identity and fingerprint;
- duplicate identity, same-run fork and reparent detection;
- ambiguous parent detection;
- cycle/self-parent detection;
- same-run manifest changes;
- cross-pack, profile, project and input-scope rejection;
- stale machine/profile and contract/schema mismatch.

Adapter failures return empty immutable facts, which causes validator failure-closed
behavior; the adapter never invents a parent or permits a fallback.

## Legacy and migration policy

- Fresh databases are created at v16.
- v15 → v16 is additive and transactional.
- Supported earlier chains continue through v14 → v15 → v16 where existing chain
  support applies; no v15 row is changed.
- Existing packs, evaluation history, storage state, `LEGACY_UNATTESTED` behavior
  and QA blocked rows remain unchanged.
- No lineage rows are backfilled, no root is inferred from a missing parent, and no
  bundled profile or immutable payload is opened for inference.
- A migration failure rolls back and leaves the old schema version and old rows.
- Downgrade is explicitly rejected; no destructive downgrade path exists.

After migration, existing packs simply have no lineage evidence until a later,
approved importer/runtime phase appends it.

## Test matrix and actual result

Pure JVM and compile checks:

- `:editorial-engine:clean :editorial-engine:test :app:testDebugUnitTest :app:compileDebugAndroidTestJavaWithJavac --no-daemon`: PASS.
- Final XML counts: editorial-engine `74/74`, app JVM `163/163`; failures `0`, errors `0`, skipped `0`.
- `:app:lintDebug --no-daemon`: PASS, 0 errors and 53 existing warnings.
- `git diff --check`: PASS.

Focused Android instrumentation, run directly against the archived target and
isolated test databases on OnePlus CPH2691 / Android 15:

- `EditorialLineagePersistenceInstrumentedTest`: `11/11` pass, 0 failures/errors/skips.
- `EditorialPackCompatibilityEvaluationInstrumentedTest`: `9/9` pass, 0 failures/errors/skips.
- Coverage includes fresh v16, v15→v16 preservation, rollback, no backfill,
  root/child readback and restart, idempotency, duplicate/reparent/orphan/parent
  mismatch, cross-context rejection, partial rollback, racing duplicate, and SQL
  immutability.

The Gradle connected task was not used as the evidence source because its generic
project-property filter shadowed the Android extension and its unfiltered retry
attempted to install an older default code; it ran zero tests. The direct runner
above is the successful device evidence. The production package was then restored
from the archive before the successful run; no `pm clear`, manual database edit or
candidate file was used.

The device had no existing production database under `run-as` at final inspection,
so preservation of pre-existing user rows is covered by the isolated v15 migration
fixtures, not claimed as a real-data device assertion. Device migration result:
`ISOLATED_MIGRATION_AND_APPEND_QA_PASS`; real existing-data continuity remains
`NOT_APPLICABLE` for this device state.

## Archive-first build evidence

- Version: `4.16-dev.38`, code `100`
- Event: `build-20260805-123935`
- Branch/source commit in `BUILD_INFO.json`: `feature/v4.16-g2-c1b1b` /
  `e65b7235e103280c8631cc054cc40aa679167e4c`
- Artifact: `artifacts/builds/v4.16-dev.38/build-20260805-123935/`
- Backup: `backup/builds/v4.16-dev.38/build-20260805-123935/`
- APK SHA-256: `8E7677020E39A57FD0EE35DD7CBA1812228B781A8B38680A5B75B85C7FA63ED1`
- Source ZIP SHA-256: `ABBFC90107F55655C78F8559EFD90AC36A1D86E47374E23BC83EDED40B93CFAE`
- Artifact/backup parity: all five payload files match by name and SHA-256.
- Provenance: Gradle 9.3.0, AGP 8.7.3, JBR 21.0.10, Java 17 source/target,
  compile/target SDK 35, as recorded in `BUILD_INFO.json`.
- `executionEnabled()` remains `false`; latest accepted artifact for this phase is
  the archived code100 development build, not a product release.

## Scope confirmations

- No importer or runtime call site uses the lineage DAO.
- No capability evidence catalog entry was added or promoted.
- No bundled production profile, profile version, canonical profile hash or machine
  fingerprint was changed.
- No SQLite version beyond v16 was introduced.
- No `DATA_COMPATIBLE`, ready/certified/active/executable state, certification,
  Golden Replay, activation, project binding or model execution was created.
- The eight other SAFE4 capabilities, pronoun/pair, ledger and release artifacts
  remain out of scope.

## Next boundary

The exact next step is `G2-C1B1-C importer/runtime lineage wiring`. It requires a
separate approval and must provide production integration evidence before any
capability promotion review. G2-C1B1-B stops here.
