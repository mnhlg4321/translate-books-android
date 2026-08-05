package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.EditorialLineageFingerprint;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageIdentity;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageInputEntry;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageInputManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageParentReference;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Device-side SQLite v16/v17 lineage and authoritative identity persistence contract. */
@RunWith(AndroidJUnit4.class)
public final class EditorialLineagePersistenceInstrumentedTest {
    private static final String PACK = "a".repeat(64);
    private static final String PROFILE_HASH = "b".repeat(64);
    private static final String MACHINE = "c".repeat(64);
    private static final String RAW_HASH = "d".repeat(64);
    private static final String DRAFT_HASH = "e".repeat(64);

    private Context context;
    private String databaseName;
    private TranslationRepository repository;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-g2c1b1b-" + UUID.randomUUID() + ".db";
        repository = new TranslationRepository(context, databaseName);
    }

    @After public void tearDown() {
        if (repository != null) repository.close();
        context.deleteDatabase(databaseName);
    }

    @Test public void freshSchemaIsV17WithLineageTablesIndexesAndTriggers() {
        SQLiteDatabase db = repository.editorialWritableDatabase();
        assertEquals(17, db.getVersion());
        assertTable(db, "editorial_project_revisions");
        assertTable(db, "editorial_input_scope_snapshots");
        assertTable(db, "editorial_input_scope_snapshot_entries");
        assertTable(db, "editorial_closed_run_contexts");
        assertTable(db, "editorial_run_lineage_bindings");
        assertTable(db, "editorial_lineage_records");
        assertTable(db, "editorial_lineage_input_entries");
        assertIndex(db, "idx_editorial_lineage_records_pack");
        assertIndex(db, "idx_editorial_lineage_records_run");
        assertIndex(db, "idx_editorial_lineage_records_parent");
        assertTrigger(db, "trg_editorial_lineage_records_no_update");
        assertTrigger(db, "trg_editorial_lineage_records_no_delete");
        assertTrigger(db, "trg_editorial_lineage_input_entries_no_update");
        assertTrigger(db, "trg_editorial_lineage_input_entries_no_delete");
        assertTrue(scalarInt(db, "PRAGMA foreign_keys") == 1);
        assertColumn(db, "editorial_lineage_records", "trusted_profile_id");
        assertColumn(db, "editorial_lineage_records", "parent_record_fingerprint");
        assertColumn(db, "editorial_lineage_input_entries", "item_count");
    }

    @Test public void validRootAndChildAppendReadbackAndRestartExactly() {
        EditorialLineageDao dao = new EditorialLineageDao(repository);
        EditorialLineageRecord root = root("run-root");
        EditorialLineageAppendResult rootResult = dao.append(root, 100L);
        assertEquals(EditorialLineagePersistenceCode.APPENDED, rootResult.code());
        assertEquals(root, dao.findByRecordIdentity(root.recordIdentity()).orElseThrow());

        EditorialLineageRecord child = child("run-child", root, baseManifest());
        assertEquals(EditorialLineagePersistenceCode.APPENDED, dao.append(child, 200L).code());
        assertEquals(child, dao.findByRecordIdentity(child.recordIdentity()).orElseThrow());
        assertEquals(1, dao.listChildrenByParentIdentity(root.recordIdentity()).size());

        repository.close();
        repository = new TranslationRepository(context, databaseName);
        EditorialLineageDao reopened = new EditorialLineageDao(repository);
        assertEquals(root, reopened.findByRecordIdentity(root.recordIdentity()).orElseThrow());
        assertEquals(child, reopened.findByRecordIdentity(child.recordIdentity()).orElseThrow());
        assertEquals(2, countRows("editorial_lineage_records"));
        assertEquals(4, countRows("editorial_lineage_input_entries"));
    }

    @Test public void sameRecordIsAlreadyExistsAndAuditTimestampIsNotRewritten() {
        EditorialLineageDao dao = new EditorialLineageDao(repository);
        EditorialLineageRecord root = root("run-idempotent");
        assertEquals(EditorialLineagePersistenceCode.APPENDED, dao.append(root, 111L).code());
        EditorialLineageAppendResult second = dao.append(
                EditorialLineageRecord.create(root.identity(), null), 999L);
        assertEquals(EditorialLineagePersistenceCode.ALREADY_EXISTS, second.code());
        assertEquals(111L, scalarLong("SELECT created_at FROM editorial_lineage_records WHERE record_identity='" + root.recordIdentity() + "'"));
        assertEquals(1, countRows("editorial_lineage_records"));
        assertEquals(root.recordFingerprint(), EditorialLineageRecord.create(root.identity(), null).recordFingerprint());
    }

    @Test public void duplicateFingerprintContentAndReparentFailWithStableCodes() {
        EditorialLineageDao dao = new EditorialLineageDao(repository);
        EditorialLineageRecord root = root("run-parent");
        EditorialLineageRecord otherRoot = root("run-other-parent");
        dao.append(root, 1L);
        dao.append(otherRoot, 2L);
        EditorialLineageRecord child = child("run-duplicate", root, baseManifest());
        assertEquals(EditorialLineagePersistenceCode.APPENDED, dao.append(child, 3L).code());

        EditorialLineageRecord changedFingerprint = EditorialLineageRecord.fromDeclared(
                child.identity(), child.parentReference(), new EditorialLineageFingerprint(
                        child.inputManifestFingerprint(), child.recordIdentity(), mutate(child.recordFingerprint())));
        assertEquals(EditorialLineagePersistenceCode.DUPLICATE_LINEAGE, dao.append(changedFingerprint, 4L).code());

        EditorialLineageRecord reparented = childWith(otherRoot, child.identity());
        assertEquals(EditorialLineagePersistenceCode.REPARENT_ATTEMPT, dao.append(reparented, 5L).code());
        assertEquals(3, countRows("editorial_lineage_records"));
    }

    @Test public void missingParentAndWrongParentFingerprintFailClosed() {
        EditorialLineageDao dao = new EditorialLineageDao(repository);
        EditorialLineageIdentity childIdentity = identity("run-orphan", EditorialLineageNodeKind.CHILD,
                baseManifest(), PACK, "profile.safe4", "1.0.0", PROFILE_HASH, MACHINE, "project-1", "chapter-1");
        EditorialLineageRecord missing = EditorialLineageRecord.create(childIdentity,
                new EditorialLineageParentReference("f".repeat(64), "f".repeat(64)));
        assertEquals(EditorialLineagePersistenceCode.ORPHAN_LINEAGE, dao.append(missing, 1L).code());

        EditorialLineageRecord root = root("run-existing-parent");
        dao.append(root, 2L);
        EditorialLineageRecord mismatch = EditorialLineageRecord.create(childIdentity,
                new EditorialLineageParentReference(root.recordIdentity(), mutate(root.recordFingerprint())));
        assertEquals(EditorialLineagePersistenceCode.PARENT_MISMATCH, dao.append(mismatch, 3L).code());
        assertEquals(1, countRows("editorial_lineage_records"));
    }

    @Test public void crossContextAndDuplicateManifestAreRejectedBeforeInsert() {
        EditorialLineageDao dao = new EditorialLineageDao(repository);
        EditorialLineageRecord root = root("run-context-parent");
        dao.append(root, 1L);
        assertEquals(EditorialLineagePersistenceCode.INVALID_LINEAGE, dao.append(childWith(root,
                identity("run-cross-pack", EditorialLineageNodeKind.CHILD, baseManifest(), mutate(PACK),
                        "profile.safe4", "1.0.0", PROFILE_HASH, MACHINE, "project-1", "chapter-1")), 2L).code());
        assertEquals(EditorialLineagePersistenceCode.INVALID_LINEAGE, dao.append(childWith(root,
                identity("run-cross-profile", EditorialLineageNodeKind.CHILD, baseManifest(), PACK,
                        "profile.other", "1.0.0", "f".repeat(64), MACHINE, "project-1", "chapter-1")), 3L).code());
        assertEquals(EditorialLineagePersistenceCode.INVALID_LINEAGE, dao.append(childWith(root,
                identity("run-cross-project", EditorialLineageNodeKind.CHILD, baseManifest(), PACK,
                        "profile.safe4", "1.0.0", PROFILE_HASH, MACHINE, "project-2", "chapter-1")), 4L).code());
        assertEquals(EditorialLineagePersistenceCode.INVALID_LINEAGE, dao.append(childWith(root,
                identity("run-cross-scope", EditorialLineageNodeKind.CHILD, baseManifest(), PACK,
                        "profile.safe4", "1.0.0", PROFILE_HASH, MACHINE, "project-1", "chapter-2")), 5L).code());

        EditorialLineageInputManifest duplicateManifest = new EditorialLineageInputManifest("manifest-v1", List.of(
                new EditorialLineageInputEntry("RAW", 0, RAW_HASH, 10, 1),
                new EditorialLineageInputEntry("RAW", 0, DRAFT_HASH, 20, 1)));
        EditorialLineageRecord duplicateManifestRoot = EditorialLineageRecord.create(
                identity("run-duplicate-manifest", EditorialLineageNodeKind.ROOT, duplicateManifest), null);
        assertEquals(EditorialLineagePersistenceCode.INVALID_LINEAGE,
                dao.append(duplicateManifestRoot, 6L).code());
        assertEquals(1, countRows("editorial_lineage_records"));
    }

    @Test public void partialInputFailureRollsBackRecordAndEntries() {
        SQLiteDatabase db = repository.editorialWritableDatabase();
        db.execSQL("CREATE TRIGGER test_lineage_input_failure BEFORE INSERT ON editorial_lineage_input_entries "
                + "BEGIN SELECT RAISE(ABORT, 'lineage input failure'); END");
        EditorialLineageAppendResult result = new EditorialLineageDao(repository).append(root("run-rollback"), 1L);
        assertEquals(EditorialLineagePersistenceCode.PERSISTENCE_FAILURE, result.code());
        assertEquals(0, countRows("editorial_lineage_records"));
        assertEquals(0, countRows("editorial_lineage_input_entries"));
        db.execSQL("DROP TRIGGER test_lineage_input_failure");
    }

    @Test public void updateDeleteAndParentDeleteAreBlockedByDatabase() {
        EditorialLineageDao dao = new EditorialLineageDao(repository);
        EditorialLineageRecord root = root("run-immutable-root");
        EditorialLineageRecord child = child("run-immutable-child", root, baseManifest());
        dao.append(root, 1L);
        dao.append(child, 2L);
        SQLiteDatabase db = repository.editorialWritableDatabase();
        assertSqliteRejected(() -> db.execSQL("UPDATE editorial_lineage_records SET project_identity='changed' WHERE record_identity='" + root.recordIdentity() + "'"));
        assertSqliteRejected(() -> db.execSQL("UPDATE editorial_lineage_input_entries SET byte_count=99 WHERE record_identity='" + root.recordIdentity() + "'"));
        assertSqliteRejected(() -> db.execSQL("DELETE FROM editorial_lineage_input_entries WHERE record_identity='" + root.recordIdentity() + "'"));
        assertSqliteRejected(() -> db.execSQL("DELETE FROM editorial_lineage_records WHERE record_identity='" + root.recordIdentity() + "'"));
        assertEquals(2, countRows("editorial_lineage_records"));
        assertEquals(4, countRows("editorial_lineage_input_entries"));
    }

    @Test public void v15ToV17PreservesRowsAndCreatesNoIdentityBackfill() throws Exception {
        repository.close();
        Path path = prepareOldDatabase(15);
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        seedV15Rows(old);
        old.setVersion(15);
        old.close();

        repository = new TranslationRepository(context, databaseName);
        SQLiteDatabase db = repository.editorialReadableDatabase();
        assertEquals(17, db.getVersion());
        assertEquals(1, countRows("editorial_packs"));
        assertEquals(1, countRows("editorial_pack_compatibility_results"));
        assertEquals(1, countRows("editorial_pack_compatibility_evaluations"));
        assertEquals(0, countRows("editorial_lineage_records"));
        assertEquals(0, countRows("editorial_lineage_input_entries"));
        assertEquals("STORED_BLOCKED|legacy-storage|legacy blocked",
                scalarString("SELECT state || '|' || storage_key || '|' || blocked_reason FROM editorial_packs"));
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM editorial_pack_compatibility_evaluations WHERE evaluation_id='legacy-evaluation'"));
    }

    @Test public void failedV15ToV17MigrationRollsBackAndKeepsVersion15() throws Exception {
        repository.close();
        Path path = prepareOldDatabase(15);
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        seedV15Rows(old);
        old.execSQL("CREATE TABLE editorial_lineage_records (bad INTEGER)");
        old.setVersion(15);
        old.close();
        repository = new TranslationRepository(context, databaseName);
        try {
            repository.editorialWritableDatabase();
            fail("malformed v17 table must fail migration");
        } catch (SQLiteException expected) { }
        repository.close();
        SQLiteDatabase reopened = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        assertEquals(15, reopened.getVersion());
        assertEquals(1, scalarInt(reopened, "SELECT COUNT(*) FROM editorial_packs"));
        assertEquals(0, scalarInt(reopened, "SELECT COUNT(*) FROM sqlite_master WHERE name='idx_editorial_lineage_records_pack'"));
        reopened.close();
    }

    @Test public void racingDuplicateAppendLeavesExactlyOneImmutableRecord() throws Exception {
        EditorialLineageRecord root = root("run-race");
        EditorialLineageDao dao = new EditorialLineageDao(repository);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<EditorialLineageAppendResult> first = executor.submit(() -> dao.append(root, 10L));
            Future<EditorialLineageAppendResult> second = executor.submit(() -> dao.append(
                    EditorialLineageRecord.create(root.identity(), null), 20L));
            List<EditorialLineagePersistenceCode> codes = List.of(first.get().code(), second.get().code());
            assertTrue(codes.contains(EditorialLineagePersistenceCode.APPENDED));
            assertTrue(codes.contains(EditorialLineagePersistenceCode.ALREADY_EXISTS));
            assertEquals(1, countRows("editorial_lineage_records"));
        } finally {
            executor.shutdownNow();
        }
    }

    private EditorialLineageRecord root(String runIdentity) {
        return EditorialLineageRecord.create(identity(runIdentity, EditorialLineageNodeKind.ROOT, baseManifest()), null);
    }

    private EditorialLineageRecord child(String runIdentity, EditorialLineageRecord parent,
                                         EditorialLineageInputManifest manifest) {
        return childWith(parent, identity(runIdentity, EditorialLineageNodeKind.CHILD, manifest));
    }

    private EditorialLineageRecord childWith(EditorialLineageRecord parent, EditorialLineageIdentity identity) {
        return EditorialLineageRecord.create(identity,
                new EditorialLineageParentReference(parent.recordIdentity(), parent.recordFingerprint()));
    }

    private EditorialLineageIdentity identity(String runIdentity, EditorialLineageNodeKind kind,
                                               EditorialLineageInputManifest manifest) {
        return identity(runIdentity, kind, manifest, PACK, "profile.safe4", "1.0.0", PROFILE_HASH,
                MACHINE, "project-1", "chapter-1");
    }

    private EditorialLineageIdentity identity(String runIdentity, EditorialLineageNodeKind kind,
                                               EditorialLineageInputManifest manifest, String pack,
                                               String profileId, String profileVersion, String profileHash,
                                               String machine, String project, String scope) {
        return new EditorialLineageIdentity(pack, profileId, profileVersion, profileHash, machine,
                "editorial.contract.v1", "editorial.schema.v1", project, scope, runIdentity, manifest, kind);
    }

    private EditorialLineageInputManifest baseManifest() {
        return new EditorialLineageInputManifest("manifest-v1", List.of(
                new EditorialLineageInputEntry("RAW", 0, RAW_HASH, 100, 1),
                new EditorialLineageInputEntry("DRAFT", 0, DRAFT_HASH, 120, 1)));
    }

    private String mutate(String value) {
        return (value.charAt(0) == 'a' ? 'b' : 'a') + value.substring(1);
    }

    private Path prepareOldDatabase(int version) throws Exception {
        Path path = context.getDatabasePath(databaseName).toPath();
        Files.createDirectories(path.getParent());
        Files.deleteIfExists(path);
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        if (version >= 14) for (String sql : EditorialMigrationSpec.from13To14()) old.execSQL(sql);
        if (version >= 15) for (String sql : EditorialMigrationSpec.from14To15()) old.execSQL(sql);
        old.setVersion(version);
        old.close();
        return path;
    }

    private void seedV15Rows(SQLiteDatabase db) {
        ContentValues pack = new ContentValues();
        pack.put("pack_id", "legacy-pack"); pack.put("version", "1.0.0"); pack.put("canonical_pack_hash", "9".repeat(64));
        pack.put("contract_version", "contract.v1"); pack.put("schema_version", "schema.v1"); pack.put("minimum_engine_version", "4.16");
        pack.put("compatibility_class", "BLOCKED"); pack.put("state", "STORED_BLOCKED"); pack.put("storage_key", "legacy-storage");
        pack.put("manifest_canonical_json", "{}"); pack.put("created_at", 10L); pack.put("validated_at", 11L);
        pack.put("engine_version_used", "4.16-dev.36"); pack.put("blocked_reason", "legacy blocked");
        long packId = db.insertOrThrow("editorial_packs", null, pack);
        ContentValues importRow = new ContentValues(); importRow.put("import_id", "legacy-import"); importRow.put("state", "STORED_BLOCKED");
        importRow.put("pack_id", "legacy-pack"); importRow.put("pack_version", "1.0.0"); importRow.put("canonical_pack_hash", "9".repeat(64));
        importRow.put("staging_path", ""); importRow.put("storage_key", "legacy-storage"); importRow.put("storage_moved", 1);
        importRow.put("pack_row_id", packId); importRow.put("blocked_reason", "legacy blocked"); importRow.put("created_at", 10L); importRow.put("updated_at", 11L);
        db.insertOrThrow("editorial_pack_imports", null, importRow);
        ContentValues compatibility = new ContentValues(); compatibility.put("import_id", "legacy-import"); compatibility.put("pack_row_id", packId);
        compatibility.put("canonical_pack_hash", "9".repeat(64)); compatibility.put("engine_version_used", "4.16-dev.36");
        compatibility.put("machine_contract_fingerprint", MACHINE); compatibility.put("compatibility_class", "BLOCKED");
        compatibility.put("required_class", "ENGINE_UPGRADE_REQUIRED"); compatibility.put("blocked_reason", "legacy blocked"); compatibility.put("evaluated_at", 12L);
        long compatibilityId = db.insertOrThrow("editorial_pack_compatibility_results", null, compatibility);
        ContentValues evaluation = new ContentValues(); evaluation.put("evaluation_id", "legacy-evaluation"); evaluation.put("import_id", "legacy-import");
        evaluation.put("pack_row_id", packId); evaluation.put("compatibility_result_id", compatibilityId); evaluation.put("canonical_pack_hash", "9".repeat(64));
        evaluation.put("trusted_profile_id", "profile.safe4"); evaluation.put("trusted_profile_version", "1.0.0"); evaluation.put("canonical_profile_hash", PROFILE_HASH);
        evaluation.put("engine_version_used", "4.16-dev.36"); evaluation.put("machine_contract_fingerprint", MACHINE); evaluation.put("evaluator_contract_version", "editorial-compatibility-v1");
        evaluation.put("adapter_set_fingerprint", "8".repeat(64)); evaluation.put("capability_fingerprint", "7".repeat(64)); evaluation.put("context_fingerprint", "6".repeat(64));
        evaluation.put("compatibility_outcome", "BLOCKED"); evaluation.put("reason_code", "LEGACY_TEST"); evaluation.put("blocker_details", "legacy evidence"); evaluation.put("evaluated_at", 13L);
        db.insertOrThrow("editorial_pack_compatibility_evaluations", null, evaluation);
    }

    private int countRows(String table) {
        return scalarInt(repository.editorialReadableDatabase(), "SELECT COUNT(*) FROM " + table);
    }

    private int scalarInt(SQLiteDatabase db, String sql) {
        try (Cursor cursor = db.rawQuery(sql, null)) { assertTrue(cursor.moveToFirst()); return cursor.getInt(0); }
    }

    private long scalarLong(String sql) {
        try (Cursor cursor = repository.editorialReadableDatabase().rawQuery(sql, null)) { assertTrue(cursor.moveToFirst()); return cursor.getLong(0); }
    }

    private String scalarString(String sql) {
        try (Cursor cursor = repository.editorialReadableDatabase().rawQuery(sql, null)) { assertTrue(cursor.moveToFirst()); return cursor.getString(0); }
    }

    private void assertTable(SQLiteDatabase db, String name) {
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='" + name + "'"));
    }

    private void assertIndex(SQLiteDatabase db, String name) {
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='" + name + "'"));
    }

    private void assertTrigger(SQLiteDatabase db, String name) {
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='trigger' AND name='" + name + "'"));
    }

    private void assertColumn(SQLiteDatabase db, String table, String column) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (cursor.moveToNext()) if (column.equals(cursor.getString(1))) return;
        }
        fail("missing column " + table + "." + column);
    }

    private void assertSqliteRejected(Runnable operation) {
        try {
            operation.run();
            fail("SQLite mutation must be rejected");
        } catch (SQLiteException expected) { }
    }
}
