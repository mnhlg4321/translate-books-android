# EDITORIAL PACK PLATFORM — G2-C0C-B1 SQLITE V15

Status: **PASS / REVIEW STOP**
Date: 2026-08-05
Branch: `feature/v4.16`
Implementation baseline: `c4c199d89b363033eb6df744b9578d744192ea0b`
Implementation end: `7defbe711bd404cf51927ae2357790f55baa11cb`

The documentation/state commit that records this handoff is intentionally not used as the implementation baseline. No push, merge or tag was performed.

## Scope and boundary

G2-C0C-B1 adds only the SQLite v15 persistence contract, additive migration, immutable evaluation history model/DAO, deterministic context fingerprint and persistence/migration tests. C0C-B2 runtime resolver/importer wiring was not implemented.

The bundled production profile was not changed. It remains non-executable, with only `pack.integrity.sha256.v1` implemented and all nine SAFE4 capabilities explicitly missing. No production `DATA_COMPATIBLE` result, certification, activation, project binding, re-evaluation or execution was created.

Changed files:

- `app/src/main/java/com/ml/tblandroidtxt/EditorialMigrationSpec.java`
- `app/src/main/java/com/ml/tblandroidtxt/TranslationRepository.java`
- `app/src/main/java/com/ml/tblandroidtxt/EditorialPackCompatibilityEvaluation.java`
- `app/src/main/java/com/ml/tblandroidtxt/EditorialPackCompatibilityEvaluationDao.java`
- `app/src/test/java/com/ml/tblandroidtxt/EditorialPackMigrationSpecTest.java`
- `app/src/androidTest/java/com/ml/tblandroidtxt/EditorialPackCompatibilityEvaluationInstrumentedTest.java`
- `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/EditorialCompatibilityEvaluationContext.java`
- `editorial-engine/src/test/java/com/ml/tblandroidtxt/editorial/pack/EditorialCompatibilityEvaluationContextTest.java`
- `WORKSPACE_SNAPSHOT.md`, `BUILD_STATE.md`, `release_checklists/v4.16.md`
- this handoff document

The protected user files `.idea/compiler.xml`, `.idea/gradle.xml` and `.idea/misc.xml` were not modified, staged, stashed or committed.

## v14 schema survey

The v14 source is `EditorialMigrationSpec.from13To14()` and `TranslationRepository` version 14 before this change.

| Table | Primary key / relationships | Uniqueness and immutability | Compatibility evidence before v15 |
|---|---|---|---|
| `editorial_packs` | `id INTEGER PRIMARY KEY AUTOINCREMENT` | Unique `canonical_pack_hash`, unique `storage_key`, unique `(pack_id, version)`, immutable update/delete triggers | `compatibility_class`, `state`, `blocked_reason`, `created_at`, `validated_at`, `engine_version_used` |
| `editorial_pack_files` | `id INTEGER PRIMARY KEY AUTOINCREMENT`; `pack_row_id` → packs `ON DELETE RESTRICT` | Unique `(pack_row_id, declared_role)` and `(pack_row_id, normalized_relative_path)`, immutable update/delete triggers | File role/path/length/hash/storage key; no evaluation provenance |
| `editorial_pack_imports` | `import_id TEXT PRIMARY KEY`; `pack_row_id` → packs `ON DELETE RESTRICT` | `state` check, indexed hash/state; import row is mutable only through the existing importer state flow | `canonical_pack_hash`, `blocked_reason`, `created_at`, `updated_at`, storage movement fields |
| `editorial_pack_compatibility_results` | `id INTEGER PRIMARY KEY AUTOINCREMENT`; `import_id` and nullable `pack_row_id` foreign keys with `ON DELETE RESTRICT` | Unique `(canonical_pack_hash, engine_version_used, machine_contract_fingerprint)`, immutable update/delete triggers | Outcome/required class, machine fingerprint, engine version, blocked reason and `evaluated_at` |

The v14 result table therefore represented one immutable result for its three-column uniqueness tuple. It had no trusted profile identity/version/hash, evaluator contract version, adapter-set fingerprint, capability fingerprint or evaluation-context identity. Adding nullable columns to that table would still not preserve multiple contexts honestly, and making legacy rows appear attested would fabricate provenance.

## v15 design

`TranslationRepository` now reports source version **15**. Fresh creation creates the v14 tables followed by the v15 table. Upgrade `oldVersion < 15` executes `EditorialMigrationSpec.from14To15()`.

The migration creates only:

- `editorial_pack_compatibility_evaluations`, an append-only history table;
- `idx_editorial_pack_compatibility_evaluations_pack` on `(canonical_pack_hash, evaluated_at, id)`;
- `idx_editorial_pack_compatibility_evaluations_import` on `(import_id, evaluated_at)`;
- update/delete rejection triggers on the history table.

The history table columns are:

```text
id                         INTEGER PRIMARY KEY AUTOINCREMENT
evaluation_id              TEXT NOT NULL UNIQUE
import_id                  TEXT NOT NULL
pack_row_id                INTEGER NULL
compatibility_result_id    INTEGER NULL
canonical_pack_hash        TEXT NOT NULL
trusted_profile_id         TEXT NOT NULL
trusted_profile_version    TEXT NOT NULL
canonical_profile_hash     TEXT NOT NULL
engine_version_used        TEXT NOT NULL
machine_contract_fingerprint TEXT NOT NULL
evaluator_contract_version TEXT NOT NULL
adapter_set_fingerprint    TEXT NOT NULL
capability_fingerprint     TEXT NOT NULL
context_fingerprint        TEXT NOT NULL
compatibility_outcome      TEXT NOT NULL with the existing five outcome values
reason_code                TEXT NOT NULL
blocker_details            TEXT NOT NULL DEFAULT ''
evaluated_at               INTEGER NOT NULL
```

`import_id`, `pack_row_id` and `compatibility_result_id` retain `ON DELETE RESTRICT` links to the v14 records. The repeated pack/result fields are intentional immutable audit snapshots in a separate history relation; the v14 table is not altered, rewritten or reinterpreted.

## Persistence contract and identity

New trusted evidence is constructed from `EditorialPackCompatibilityEvaluation` and appended by `EditorialPackCompatibilityEvaluationDao.append(...)`. The public DAO surface contains only append and read operations:

- `append`
- `findByEvaluationId`
- `listByPackHash`
- `listByImportId`
- an explicitly named `listByPackHashIncludingLegacy` read view

There is no update, replace, delete, certify, activate, bind, execution or state-setting API. `insertOrThrow` is used; `INSERT OR REPLACE` is not used. `evaluation_id` is an immutable identity. Two evaluations for the same pack with different contexts coexist. A duplicate identity is rejected without changing the first row.

Each new row must contain:

- pack canonical hash;
- trusted profile ID and version;
- trusted profile canonical hash;
- machine contract fingerprint;
- evaluator/compatibility contract version;
- adapter-set and capability fingerprints;
- canonical evaluation context fingerprint;
- outcome, stable reason code, blocker details and timestamp;
- import identity and optional links to the existing v14 result/pack row.

`blocker_details` is diagnostic text and may contain Unicode. Raw prompt, project, workflow, pack bytes, secrets, UI wording and personal machine paths are not stored in this table.

### Canonical evaluation context

`EditorialCompatibilityEvaluationContext` canonicalizes these semantic fields only:

```text
canonicalPackHash
trustedProfileId
trustedProfileVersion
canonicalProfileHash
engineVersion
machineContractFingerprint
evaluatorContractVersion
adapterSetFingerprint
capabilityFingerprint
```

The canonical JSON uses sorted keys and deterministic UTF-8 serialization. The context fingerprint is SHA-256 of the fixed domain prefix `EDITORIAL_COMPATIBILITY_EVALUATION_CONTEXT_V1` plus that canonical JSON. `evaluatedAt` is stored separately and is not hashed. Timestamp, locale, JSON field order, JSON formatting whitespace, UI wording, raw prompt/project/workflow content, personal paths and secrets cannot affect the fingerprint. No raw context JSON is required in SQLite.

### Legacy-row policy

Migration does not copy or backfill v14 rows. The DAO’s explicit legacy read view maps an existing v14 row to `LEGACY_UNATTESTED`; trusted profile ID/version/hash, evaluator version, adapter/capability fingerprints and context fingerprint are absent. The legacy primary key, state, outcome, reason and timestamp remain unchanged. A legacy row is never called trusted or certified.

## Transaction and rollback behavior

`SQLiteOpenHelper` invokes `onUpgrade` in its upgrade transaction. The v15 helper executes the additive SQL without opening a nested transaction and has no destructive `onDowngrade`. Every v15 statement is idempotent with `IF NOT EXISTS` for structure/index/trigger creation. No resolver, bundled resource or evaluator is called during migration, and no pack state is changed.

The migration rollback test pre-creates a malformed v15 object, verifies the upgrade fails, then reopens the database and confirms that `user_version` remains 14, no v15 index was committed and the legacy pack row remains. This proves the migration does not leave a half-v14/half-v15 schema.

## Evidence and test matrix

Host regression, all through the pinned Wrapper and JDK 21:

- `gradlew.bat --version`: Gradle **9.3.0**, JBR **21.0.10**.
- `:editorial-engine:clean :editorial-engine:test :app:testDebugUnitTest :app:compileDebugAndroidTestJavaWithJavac --no-daemon`: engine **59/59**, app JVM **162/162**, instrumentation Java compilation PASS, 0 failures/errors/skips.
- `:app:lintDebug --no-daemon`: **0 errors**, 53 existing warnings.
- `git diff --check`: PASS.
- `scripts/test-java-toolchain-preflight.ps1`: **4/4** — JDK 21 accepted; Java 8, missing `JAVA_HOME` and invalid path rejected; no system environment or execution policy was changed.

The scoped `EditorialPackCompatibilityEvaluationInstrumentedTest` ran on the archived APK/device and passed **9/9**:

- fresh v15 table/index/trigger contract;
- trusted provenance round-trip and immutable returned collection;
- two different contexts for one pack;
- duplicate identity rejection without replacement;
- v14 legacy mapping as `LEGACY_UNATTESTED`;
- v14→v15 preservation with no provenance backfill;
- v13→v14→v15 and reopen idempotency;
- transaction rollback on migration failure;
- pack storage state unchanged by append.

The instrumented test database is isolated and seeded with the reviewed v14 schema. No SQLite was edited manually, no app data was cleared, and the existing device QA blocked row was not updated or deleted.

## Archive and device evidence

After regression, archive-first build command:

```powershell
scripts/build-and-save.ps1 -Series 4.16-dev
```

Result:

- version `4.16-dev.31`, versionCode `93`;
- event `build-20260805-080909`;
- APK SHA-256 `6522BFF2B7DA8B5B265243B10DEA9E6E936A3C712B28B4173C10EC1DDEBADC0B`;
- source ZIP SHA-256 `66B62FED77C4F275559FB80E02D6BC21CC8C0F2E5032B716AA608444E7E9FADE`;
- artifact/backup parity: PASS for APK, README, `BUILD_INFO.json`, `SHA256SUMS.txt` and exact tracked-source ZIP.

`BUILD_INFO.json` was opened from both payloads and contains Gradle 9.3.0, the distribution URL and official checksum source/value, AGP 8.7.3, JDK/JBR version/vendor/runtime, JDK major policy 21, Java source/target 17, compile/target SDK 35, Git commit/branch, event, APK hash and source ZIP hash. No secret or unnecessary personal path is present.

The exact archived APK was installed as an upgrade without clearing data. Device evidence: **OnePlus CPH2691, Android 15, SDK 35**; installation returned `Success`, package metadata reported `4.16-dev.31`/code93, and a force-stop/cold launch returned `MainActivity` resumed. The scoped v15 migration/persistence tests passed 9/9 on that device.

## Final safety review

- Production profile identity/version remains `com.ml.tblandroidtxt.editorial.engine.bootstrap` / `1.0.0`.
- Canonical profile hash remains `2d4e2f76dc5defcfb98cfd36cec49b0e5454cb3462db93a6b1586f7784eb91b6`.
- Machine contract fingerprint remains `6410f374ce175cbc6fc32484f5cd9635888b9297b01d882923af06ccd4ac1e4c`.
- Production capability remains only `pack.integrity.sha256.v1`; all nine SAFE4 capabilities remain missing.
- `EditorialSafe4Pack.executionEnabled()` remains false.
- C0C-B2 runtime wiring and re-evaluation were not implemented.
- Latest app QA/storage state was not rewritten by migration; no candidate `DBE214...` or `3B2FCC...` was read/imported/activated.
- Final worktree: only the three protected user-owned `.idea/*` files are modified and unstaged; all B1 source/documentation changes are committed. No tag, merge or push.

Decision: **G2-C0C-B1 PASS / REVIEW STOP**. The next proposed step is **G2-C0C-B2**, which has not been implemented.
