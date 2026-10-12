# Editorial Pack Platform — G2-B1

Status: `IMPLEMENTED / REVIEW STOP`
Scope: persistent pack storage, additive SQLite v14 migration and headless import service.
Explicitly out of scope: UI/picker, certification, Golden Replay, project binding, model execution, L1/L2/L3 and release flow.

## Implemented boundary

`:editorial-engine` remains pure JVM. The Android app depends on it only at the import/persistence boundary. Pack input is a collection of already-selected streams; the service never reopens a URI/path and never loads Java/Dex.

Public read API is `EditorialPackRegistry` implemented by `SqliteEditorialPackRegistry`:

- `findByHash`, `findByIdentity` and `list` return only rows whose immutable marker, canonical manifest and all three file bytes revalidate.
- There is no public update, replace, delete, activate or certify operation.

The only write boundary is `EditorialPackImportService`.

## v14 additive schema

The actual database baseline was v13; `TranslationRepository` now uses v14. `EditorialMigrationSpec.from13To14()` creates, without changing legacy tables:

- `editorial_packs`: immutable identity, canonical hash, contract/schema, compatibility, storage key, canonical manifest and validation metadata. Unique `(pack_id, version)` and `canonical_pack_hash` prevent identity collision and overwrite.
- `editorial_pack_files`: exactly the declared role/path, byte length, SHA-256 and immutable storage key; foreign key `ON DELETE RESTRICT`.
- `editorial_pack_imports`: import protocol state, staging/storage locations, move marker and blocked reason.
- `editorial_pack_compatibility_results`: engine version, machine-contract fingerprint, effective/required compatibility class and blocker.

Pack/file/compatibility rows have immutable SQL triggers. No cascade delete is used. The migration is additive and idempotent through `CREATE TABLE/INDEX/TRIGGER IF NOT EXISTS`; legacy project/chapter/run/evidence rows are untouched.

## Storage and TOCTOU protocol

Private app storage is:

```text
<filesDir>/editorial-packs/
  staging/<random-import-uuid>/
  immutable/<lowercase-canonical-pack-sha256>/
```

The importer creates an owner marker, reads each stream once into staging, computes byte length and SHA-256 while writing, forces file contents, closes the stream, and writes a completion marker. All later manifest/integrity/compatibility checks use the in-memory snapshot bytes. Paths are root-level normalized NFC names; absolute paths, traversal, both slash families, Unicode slash variants, duplicate normalized paths, symlinks, extra files, oversized files/packs and compression bombs are rejected.

After validation, the staging directory receives a storage marker and is moved with `ATOMIC_MOVE` to the content-addressed hash directory. An existing complete hash is never overwritten; an incomplete destination is a storage collision. Re-import of the same identity/hash is idempotent. Same `packId/version` with another hash is `IDENTITY_COLLISION` and remains blocked.

Recovery examines importer-owned staging and database rows. It can finalize a provable storage move, clean owned staging left by a blocked/crashed import, and retain unknown complete immutable storage for audit. It never deletes a directory without the importer owner marker.

## G2-B1 state machine

```text
STAGING -> SNAPSHOTTED -> INTEGRITY_VALIDATED -> COMPATIBILITY_EVALUATED
       -> STORED_READY_FOR_CERTIFICATION   (DATA_COMPATIBLE)
       -> STORED_BLOCKED                    (invalid, missing capability,
                                             adapter required or engine upgrade)
```

`STORED_READY_FOR_CERTIFICATION` is not `CERTIFIED` and does not enable execution. No G2-B1 code can create `ACTIVE`, `PROJECT_BOUND`, `EXECUTING` or `RELEASE_READY`.

## Transaction/recovery invariant

1. Create random staging directory and STAGING import row.
2. Snapshot and fsync/close all bytes.
3. Validate integrity and compatibility from the snapshot.
4. Move atomically to the hash directory.
5. In one SQLite transaction insert pack, three file rows, compatibility result and final import state.

If the database transaction fails after the move, storage is retained with a non-final import row for recovery; it is not exposed by the read-only registry. If the filesystem move fails, no pack row is committed. If the process stops before the move, recovery blocks and cleans only owned staging.

## Verification evidence

- Focused G2-B1 JVM: 4 tests (migration invariants and storage path/identity guards), all pass.
- Full pure-JVM regression: `:editorial-engine:test` 19/19 and `:app:testDebugUnitTest` 148/148, 0 failures/errors/skips.
- Android instrumentation source compilation: `:app:compileDebugAndroidTestJavaWithJavac` pass. Connected execution was not possible in this environment because `adb` is unavailable; no device result is claimed.
- `git diff --check` pass before each implementation commit.
- Final archive-first build: `4.16-dev.26`/code88, event `build-20260803-182934`, APK SHA-256 `56E7651EF623DC3A4787C1F6D00DBB52C195B260D1D01F528C706039B7B1DF57`, source ZIP SHA-256 `6D9B14707DC8EF511177EEE13F0B66F3E3EC408E2EFF8DC87E0A50132E4FCDA2`. Artifact and backup payloads are byte-identical; the APK was archived and not installed.

Android instrumentation tests cover fresh schema, v13 upgrade retention, valid import, idempotent re-import, changed-prompt identity collision, post-snapshot source mutation, traversal/duplicate/symlink/hash/length failures, filesystem collision, database failure and orphan recovery. They require a device to produce runtime evidence.

## Immutable pack decisions

The bundled SAFE4/code86 three-file assets and hashes are unchanged and remain guarded by the existing build task. The external `DBE214...` and current `3B2FCC...` identities are not inserted into this registry and cannot replace SAFE4. No capability was marked implemented and `EditorialSafe4Pack.executionEnabled()` remains false.

## Next review stop (G2-B2 proposal only)

After approval, the smallest next increment is a read-only pack-management surface that lists imported states and blockers. It must not activate a pack, bind a project, certify replay or connect model execution until the separate certification/binding design is approved.
