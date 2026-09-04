package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunCompatibilityOutcome;
import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContext;
import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContextDraft;
import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContractFacts;
import com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshot;
import com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshotEntry;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageIdentity;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageInputEntry;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageInputManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageRecord;
import com.ml.tblandroidtxt.editorial.pack.EditorialProjectRevision;
import com.ml.tblandroidtxt.editorial.pack.EditorialRequiredInputRoleContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialRunLineageBinding;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
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

/** Isolated v17 identity/binding QA; this test has no production caller or importer path. */
@RunWith(AndroidJUnit4.class)
public final class EditorialV17IdentityStoreInstrumentedTest {
    private static final String PACK = "a".repeat(64);
    private static final String PROFILE = "b".repeat(64);
    private static final String MACHINE = "c".repeat(64);
    private static final String RAW = "d".repeat(64);
    private static final String DRAFT = "e".repeat(64);
    private static final String ADAPTERS = "f".repeat(64);
    private static final String CAPABILITIES = "1".repeat(64);
    private static final String CONTEXT = "2".repeat(64);

    private Context context;
    private String databaseName;
    private TranslationRepository repository;
    private EditorialProjectRevision revision;
    private EditorialInputScopeSnapshot snapshot;
    private EditorialClosedRunContext closedRun;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-g2-c1b1c15b-" + UUID.randomUUID() + ".db";
        repository = new TranslationRepository(context, databaseName);
    }

    @After public void tearDown() {
        if (repository != null) repository.close();
        context.deleteDatabase(databaseName);
    }

    @Test public void freshV17SchemaHasFiveStoresAndNoIdentityBackfill() {
        SQLiteDatabase db = repository.editorialWritableDatabase();
        assertEquals(20, db.getVersion());
        for (String table : List.of("editorial_project_revisions", "editorial_input_scope_snapshots",
                "editorial_input_scope_snapshot_entries", "editorial_closed_run_contexts",
                "editorial_run_lineage_bindings")) assertEquals(1, scalarInt(db,
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='" + table + "'"));
        assertEquals(0, scalarInt(db, "SELECT COUNT(*) FROM editorial_project_revisions"));
        assertEquals(0, scalarInt(db, "SELECT COUNT(*) FROM editorial_input_scope_snapshots"));
        assertEquals(0, scalarInt(db, "SELECT COUNT(*) FROM editorial_closed_run_contexts"));
        assertEquals(0, scalarInt(db, "SELECT COUNT(*) FROM editorial_run_lineage_bindings"));
        assertEquals(1, scalarInt(db, "PRAGMA foreign_keys"));
        assertTrigger(db, "trg_editorial_project_revisions_no_update");
        assertTrigger(db, "trg_editorial_input_scope_snapshots_no_delete");
        assertTrigger(db, "trg_editorial_input_scope_snapshot_entries_no_update");
        assertTrigger(db, "trg_editorial_closed_run_contexts_no_delete");
        assertTrigger(db, "trg_editorial_run_lineage_bindings_no_update");
    }

    @Test public void projectSnapshotClosedRunAreAppendOnlyAndRestartReadable() {
        prepareIdentityContext();
        EditorialProjectRevisionDao revisionDao = new EditorialProjectRevisionDao(repository);
        EditorialInputScopeSnapshotDao snapshotDao = new EditorialInputScopeSnapshotDao(repository);
        EditorialClosedRunContextDao runDao = new EditorialClosedRunContextDao(repository);

        assertEquals(EditorialIdentityPersistenceCode.APPENDED,
                revisionDao.append(revision, null, 10L).code());
        assertEquals(EditorialIdentityPersistenceCode.ALREADY_EXISTS,
                revisionDao.append(revision, null, 99L).code());
        assertEquals(EditorialIdentityPersistenceCode.APPENDED,
                snapshotDao.appendWithEntries(snapshot, null, 11L).code());
        assertEquals(EditorialIdentityPersistenceCode.ALREADY_EXISTS,
                snapshotDao.appendWithEntries(snapshot, null, 99L).code());
        EditorialIdentityAppendResult<EditorialClosedRunContext> first = runDao.append(closedDraft(), null, 12L);
        assertEquals(EditorialIdentityPersistenceCode.APPENDED, first.code());
        assertEquals(0L, first.value().runAttemptOrdinal());
        EditorialIdentityAppendResult<EditorialClosedRunContext> duplicate = runDao.append(closedDraft(), null, 99L);
        assertEquals(EditorialIdentityPersistenceCode.ALREADY_EXISTS, duplicate.code());
        assertEquals(0L, duplicate.value().runAttemptOrdinal());
        assertEquals(1, scalarInt(repository.editorialReadableDatabase(),
                "SELECT COUNT(*) FROM editorial_closed_run_contexts"));

        repository.close();
        repository = new TranslationRepository(context, databaseName);
        assertEquals(revision.revisionIdentity(), new EditorialProjectRevisionDao(repository)
                .findByIdentity(revision.revisionIdentity()).orElseThrow().revisionIdentity());
        assertEquals(snapshot.scopeSnapshotIdentity(), new EditorialInputScopeSnapshotDao(repository)
                .findByIdentity(snapshot.scopeSnapshotIdentity()).orElseThrow().scopeSnapshotIdentity());
        assertEquals(first.value().closedRunIdentity(), new EditorialClosedRunContextDao(repository)
                .findByIdentity(first.value().closedRunIdentity()).orElseThrow().closedRunIdentity());
    }

    @Test public void closedRunGateRejectsBlockedLegacyAndMismatchedTrustedContext() {
        prepareIdentityContext();
        new EditorialProjectRevisionDao(repository).append(revision, null, 10L);
        new EditorialInputScopeSnapshotDao(repository).appendWithEntries(snapshot, null, 11L);
        EditorialClosedRunContextDao dao = new EditorialClosedRunContextDao(repository);
        assertEquals(EditorialIdentityPersistenceCode.RUN_CONTEXT_NOT_CLOSED,
                dao.append(draft(EditorialClosedRunCompatibilityOutcome.BLOCKED), null, 1L).code());
        assertEquals(EditorialIdentityPersistenceCode.RUN_CONTEXT_NOT_CLOSED,
                dao.append(draft(EditorialClosedRunCompatibilityOutcome.LEGACY_UNATTESTED), null, 2L).code());
        EditorialClosedRunContextDraft mismatch = new EditorialClosedRunContextDraft(
                "closed-run-v1", revision.revisionIdentity(), snapshot.scopeSnapshotIdentity(), PACK,
                "test-evaluation", EditorialClosedRunCompatibilityOutcome.DATA_COMPATIBLE,
                "trusted-test", "1", "9".repeat(64), MACHINE, facts(), "ROOT", "phase-1",
                snapshot.manifestFingerprint());
        assertEquals(EditorialIdentityPersistenceCode.TRUSTED_PROFILE_CONTEXT_MISMATCH,
                dao.append(mismatch, null, 3L).code());
        assertEquals(0, scalarInt(repository.editorialReadableDatabase(),
                "SELECT COUNT(*) FROM editorial_closed_run_contexts"));
    }

    @Test public void serializedRacingAppendAllocatesOneOrdinalAndOneRow() throws Exception {
        prepareIdentityContext();
        new EditorialProjectRevisionDao(repository).append(revision, null, 10L);
        new EditorialInputScopeSnapshotDao(repository).appendWithEntries(snapshot, null, 11L);
        EditorialClosedRunContextDao dao = new EditorialClosedRunContextDao(repository);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<EditorialIdentityAppendResult<EditorialClosedRunContext>> one = executor.submit(
                    () -> dao.append(closedDraft(), null, 20L));
            Future<EditorialIdentityAppendResult<EditorialClosedRunContext>> two = executor.submit(
                    () -> dao.append(closedDraft(), null, 21L));
            List<EditorialIdentityPersistenceCode> codes = List.of(one.get().code(), two.get().code());
            assertTrue(codes.contains(EditorialIdentityPersistenceCode.APPENDED));
            assertTrue(codes.contains(EditorialIdentityPersistenceCode.ALREADY_EXISTS));
            assertEquals(1, scalarInt(repository.editorialReadableDatabase(),
                    "SELECT COUNT(*) FROM editorial_closed_run_contexts"));
            assertEquals(0L, new EditorialClosedRunContextDao(repository)
                    .listByScope(snapshot.scopeSnapshotIdentity()).get(0).runAttemptOrdinal());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test public void sharedTransactionCommitsAndRollsBackLineageAndBindingAtomically() {
        prepareIdentityContext();
        new EditorialProjectRevisionDao(repository).append(revision, null, 10L);
        new EditorialInputScopeSnapshotDao(repository).appendWithEntries(snapshot, null, 11L);
        closedRun = new EditorialClosedRunContextDao(repository).append(closedDraft(), null, 12L).value();
        EditorialLineageRecord record = lineageRecord("run-lineage-1");
        EditorialRunLineageBinding binding = new EditorialRunLineageBinding(
                "binding-v1", closedRun.closedRunIdentity(), record.recordIdentity(), record.recordFingerprint());
        EditorialLineageAndBindingTransactionService service =
                new EditorialLineageAndBindingTransactionService(repository);
        EditorialLineageAndBindingResult committed = service.appendAndBind(record, binding, 13L, 14L);
        assertTrue(committed.committed());
        assertEquals(1, scalarInt(repository.editorialReadableDatabase(),
                "SELECT COUNT(*) FROM editorial_lineage_records"));
        assertEquals(1, scalarInt(repository.editorialReadableDatabase(),
                "SELECT COUNT(*) FROM editorial_run_lineage_bindings"));

        EditorialClosedRunContext secondRun = new EditorialClosedRunContextDao(repository)
                .append(draft("phase-2", "test-evaluation"), null, 15L).value();
        EditorialLineageRecord newRecord = lineageRecord("run-lineage-2");
        EditorialRunLineageBinding badFingerprint = new EditorialRunLineageBinding(
                "binding-v1", secondRun.closedRunIdentity(), newRecord.recordIdentity(), "9".repeat(64));
        EditorialLineageAndBindingResult rolledBack = service.appendAndBind(
                newRecord, badFingerprint, 16L, 17L);
        assertFalse(rolledBack.committed());
        assertEquals(EditorialIdentityPersistenceCode.BINDING_MISMATCH, rolledBack.code());
        assertEquals(1, scalarInt(repository.editorialReadableDatabase(),
                "SELECT COUNT(*) FROM editorial_lineage_records"));
        assertEquals(1, scalarInt(repository.editorialReadableDatabase(),
                "SELECT COUNT(*) FROM editorial_run_lineage_bindings"));

        EditorialRunLineageBinding sameRunDifferentLineage = new EditorialRunLineageBinding(
                "binding-v1", closedRun.closedRunIdentity(), newRecord.recordIdentity(), newRecord.recordFingerprint());
        EditorialLineageAndBindingResult runMismatch = service.appendAndBind(
                newRecord, sameRunDifferentLineage, 18L, 19L);
        assertFalse(runMismatch.committed());
        assertEquals(1, scalarInt(repository.editorialReadableDatabase(),
                "SELECT COUNT(*) FROM editorial_lineage_records"));
    }

    @Test public void everyV17TableRejectsUpdateAndDelete() {
        prepareIdentityContext();
        new EditorialProjectRevisionDao(repository).append(revision, null, 10L);
        new EditorialInputScopeSnapshotDao(repository).appendWithEntries(snapshot, null, 11L);
        closedRun = new EditorialClosedRunContextDao(repository).append(closedDraft(), null, 12L).value();
        EditorialLineageRecord record = lineageRecord("run-immutable");
        EditorialRunLineageBinding binding = new EditorialRunLineageBinding(
                "binding-v1", closedRun.closedRunIdentity(), record.recordIdentity(), record.recordFingerprint());
        new EditorialLineageAndBindingTransactionService(repository).appendAndBind(record, binding, 13L, 14L);
        SQLiteDatabase db = repository.editorialWritableDatabase();
        assertRejected(() -> db.execSQL("UPDATE editorial_project_revisions SET project_semantic_key='changed'"));
        assertRejected(() -> db.execSQL("DELETE FROM editorial_project_revisions"));
        assertRejected(() -> db.execSQL("UPDATE editorial_input_scope_snapshots SET canonical_scope_key='changed'"));
        assertRejected(() -> db.execSQL("DELETE FROM editorial_input_scope_snapshots"));
        assertRejected(() -> db.execSQL("UPDATE editorial_input_scope_snapshot_entries SET byte_count=99"));
        assertRejected(() -> db.execSQL("DELETE FROM editorial_input_scope_snapshot_entries"));
        assertRejected(() -> db.execSQL("UPDATE editorial_closed_run_contexts SET run_kind='changed'"));
        assertRejected(() -> db.execSQL("DELETE FROM editorial_closed_run_contexts"));
        assertRejected(() -> db.execSQL("UPDATE editorial_run_lineage_bindings SET bound_at=99"));
        assertRejected(() -> db.execSQL("DELETE FROM editorial_run_lineage_bindings"));
    }

    @Test public void v16ToV17IsAdditiveAndDoesNotBackfillIdentityRows() throws Exception {
        repository.close();
        Path path = context.getDatabasePath(databaseName).toPath();
        Files.createDirectories(path.getParent());
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        for (String sql : EditorialMigrationSpec.from13To14()) old.execSQL(sql);
        for (String sql : EditorialMigrationSpec.from14To15()) old.execSQL(sql);
        for (String sql : EditorialMigrationSpec.from15To16()) old.execSQL(sql);
        ContentValues lineage = new ContentValues();
        lineage.put("record_identity", "3".repeat(64)); lineage.put("record_fingerprint", "4".repeat(64));
        lineage.put("canonical_pack_hash", PACK); lineage.put("trusted_profile_id", "legacy");
        lineage.put("trusted_profile_version", "1"); lineage.put("canonical_profile_hash", PROFILE);
        lineage.put("machine_contract_fingerprint", MACHINE); lineage.put("contract_version", "contract");
        lineage.put("schema_version", "schema"); lineage.put("project_identity", "legacy-project");
        lineage.put("input_scope_identity", "legacy-scope"); lineage.put("run_evaluation_identity", "legacy-run");
        lineage.put("input_manifest_version", "manifest"); lineage.put("input_manifest_fingerprint", "5".repeat(64));
        lineage.put("node_kind", "ROOT"); lineage.putNull("parent_record_identity");
        lineage.putNull("parent_record_fingerprint"); lineage.put("created_at", 1L);
        old.insertOrThrow("editorial_lineage_records", null, lineage);
        old.setVersion(16);
        old.close();
        repository = new TranslationRepository(context, databaseName);
        SQLiteDatabase upgraded = repository.editorialReadableDatabase();
        assertEquals(20, upgraded.getVersion());
        assertEquals(1, scalarInt(upgraded, "SELECT COUNT(*) FROM editorial_lineage_records"));
        assertEquals(0, scalarInt(upgraded, "SELECT COUNT(*) FROM editorial_project_revisions"));
        assertEquals(0, scalarInt(upgraded, "SELECT COUNT(*) FROM editorial_input_scope_snapshots"));
        assertEquals(0, scalarInt(upgraded, "SELECT COUNT(*) FROM editorial_closed_run_contexts"));
        assertEquals(0, scalarInt(upgraded, "SELECT COUNT(*) FROM editorial_run_lineage_bindings"));
    }

    @Test public void failedV16ToV17MigrationRollsBackToReadableV16() throws Exception {
        repository.close();
        Path path = context.getDatabasePath(databaseName).toPath();
        Files.createDirectories(path.getParent());
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        for (String sql : EditorialMigrationSpec.from13To14()) old.execSQL(sql);
        for (String sql : EditorialMigrationSpec.from14To15()) old.execSQL(sql);
        for (String sql : EditorialMigrationSpec.from15To16()) old.execSQL(sql);
        old.execSQL("CREATE TABLE editorial_project_revisions (bad INTEGER)");
        old.setVersion(16);
        old.close();
        repository = new TranslationRepository(context, databaseName);
        try {
            repository.editorialWritableDatabase();
            fail("malformed v17 table must fail the v16-to-v17 migration");
        } catch (SQLiteException expected) {
            assertNotNull(expected.getMessage());
        }
        repository.close();
        SQLiteDatabase reopened = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        assertEquals(16, reopened.getVersion());
        assertEquals(1, scalarInt(reopened,
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='editorial_project_revisions'"));
        assertEquals(0, scalarInt(reopened,
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='editorial_input_scope_snapshots'"));
        reopened.close();
    }

    private void prepareIdentityContext() {
        revision = new EditorialProjectRevision("project-projection-v1", "semantic/project/001",
                "project-definition-v1", "novel", PROFILE, MACHINE);
        EditorialRequiredInputRoleContract roles = new EditorialRequiredInputRoleContract(
                "roles-v1", new LinkedHashSet<>(List.of("RAW", "DRAFT")));
        snapshot = new EditorialInputScopeSnapshot(revision.revisionIdentity(), "scope-v1", "chapter/001",
                roles, "manifest-v1", List.of(
                new EditorialInputScopeSnapshotEntry("DRAFT", 0L, DRAFT, 20L, 2L),
                new EditorialInputScopeSnapshotEntry("RAW", 0L, RAW, 10L, 1L)));
        seedTrustedCompatibleEvidence();
    }

    private EditorialClosedRunContextDraft closedDraft() { return draft("phase-1", "test-evaluation"); }

    private EditorialClosedRunContextDraft draft(String phase, String evaluationId) {
        return draft(phase, evaluationId, EditorialClosedRunCompatibilityOutcome.DATA_COMPATIBLE);
    }

    private EditorialClosedRunContextDraft draft(EditorialClosedRunCompatibilityOutcome outcome) {
        return draft("phase-1", "test-evaluation", outcome);
    }

    private EditorialClosedRunContextDraft draft(String phase, String evaluationId,
                                                  EditorialClosedRunCompatibilityOutcome outcome) {
        return new EditorialClosedRunContextDraft("closed-run-v1", revision.revisionIdentity(),
                snapshot.scopeSnapshotIdentity(), PACK, evaluationId,
                outcome, "trusted-test", "1", PROFILE,
                MACHINE, facts(), "ROOT", phase, snapshot.manifestFingerprint());
    }

    private EditorialClosedRunContractFacts facts() {
        return new EditorialClosedRunContractFacts("test-pack", "contract.v1", "test-schema", "schema.v1",
                "evaluator.v1", ADAPTERS, CAPABILITIES, CONTEXT, "engine-test");
    }

    private EditorialLineageRecord lineageRecord(String runIdentity) {
        EditorialLineageInputManifest manifest = new EditorialLineageInputManifest("manifest-v1", List.of(
                new EditorialLineageInputEntry("RAW", 0, RAW, 10, 1),
                new EditorialLineageInputEntry("DRAFT", 0, DRAFT, 20, 2)));
        EditorialLineageIdentity identity = new EditorialLineageIdentity(PACK, "trusted-test", "1", PROFILE,
                MACHINE, "contract.v1", "schema.v1", revision.revisionIdentity(),
                snapshot.scopeSnapshotIdentity(), runIdentity, manifest, EditorialLineageNodeKind.ROOT);
        return EditorialLineageRecord.create(identity, null);
    }

    private void seedTrustedCompatibleEvidence() {
        SQLiteDatabase db = repository.editorialWritableDatabase();
        ContentValues pack = new ContentValues();
        pack.put("pack_id", "test-pack"); pack.put("version", "1.0.0"); pack.put("canonical_pack_hash", PACK);
        pack.put("contract_version", "contract.v1"); pack.put("schema_version", "schema.v1");
        pack.put("minimum_engine_version", "engine-test"); pack.put("compatibility_class", "DATA_COMPATIBLE");
        pack.put("state", "STORED_READY_FOR_CERTIFICATION"); pack.put("storage_key", "test-storage");
        pack.put("manifest_canonical_json", "{}"); pack.put("created_at", 1L); pack.put("validated_at", 1L);
        pack.put("engine_version_used", "engine-test"); pack.put("blocked_reason", "");
        long packId = db.insertOrThrow("editorial_packs", null, pack);
        ContentValues importRow = new ContentValues(); importRow.put("import_id", "test-import");
        importRow.put("state", "STORED_READY_FOR_CERTIFICATION"); importRow.put("pack_id", "test-pack");
        importRow.put("pack_version", "1.0.0"); importRow.put("canonical_pack_hash", PACK);
        importRow.put("staging_path", ""); importRow.put("storage_key", "test-storage"); importRow.put("storage_moved", 1);
        importRow.put("pack_row_id", packId); importRow.put("blocked_reason", ""); importRow.put("created_at", 1L); importRow.put("updated_at", 1L);
        db.insertOrThrow("editorial_pack_imports", null, importRow);
        ContentValues result = new ContentValues(); result.put("import_id", "test-import"); result.put("pack_row_id", packId);
        result.put("canonical_pack_hash", PACK); result.put("engine_version_used", "engine-test");
        result.put("machine_contract_fingerprint", MACHINE); result.put("compatibility_class", "DATA_COMPATIBLE");
        result.put("required_class", "DATA_COMPATIBLE"); result.put("blocked_reason", ""); result.put("evaluated_at", 1L);
        long resultId = db.insertOrThrow("editorial_pack_compatibility_results", null, result);
        ContentValues evaluation = new ContentValues(); evaluation.put("evaluation_id", "test-evaluation");
        evaluation.put("import_id", "test-import"); evaluation.put("pack_row_id", packId); evaluation.put("compatibility_result_id", resultId);
        evaluation.put("canonical_pack_hash", PACK); evaluation.put("trusted_profile_id", "trusted-test");
        evaluation.put("trusted_profile_version", "1"); evaluation.put("canonical_profile_hash", PROFILE);
        evaluation.put("engine_version_used", "engine-test"); evaluation.put("machine_contract_fingerprint", MACHINE);
        evaluation.put("evaluator_contract_version", "evaluator.v1"); evaluation.put("adapter_set_fingerprint", ADAPTERS);
        evaluation.put("capability_fingerprint", CAPABILITIES); evaluation.put("context_fingerprint", CONTEXT);
        evaluation.put("compatibility_outcome", "DATA_COMPATIBLE"); evaluation.put("reason_code", "TEST_ONLY_TRUSTED");
        evaluation.put("blocker_details", ""); evaluation.put("evaluated_at", 2L);
        db.insertOrThrow("editorial_pack_compatibility_evaluations", null, evaluation);
    }

    private int scalarInt(SQLiteDatabase db, String sql) {
        try (Cursor cursor = db.rawQuery(sql, null)) { assertTrue(cursor.moveToFirst()); return cursor.getInt(0); }
    }

    private void assertTrigger(SQLiteDatabase db, String name) {
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='trigger' AND name='" + name + "'"));
    }

    private void assertRejected(Runnable action) {
        try { action.run(); fail("immutable SQL mutation must fail"); }
        catch (SQLiteException expected) { assertNotNull(expected.getMessage()); }
    }
}
