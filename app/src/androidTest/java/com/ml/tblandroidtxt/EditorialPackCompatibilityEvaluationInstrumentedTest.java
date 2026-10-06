package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityEvaluationContext;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

/** Device-side persistence contract and migration evidence for SQLite v15/v17. */
@RunWith(AndroidJUnit4.class)
public class EditorialPackCompatibilityEvaluationInstrumentedTest {
    private static final String PACK_HASH = "a".repeat(64);
    private static final String PROFILE_HASH = "b".repeat(64);
    private static final String MACHINE_FINGERPRINT = "c".repeat(64);
    private static final String ADAPTER_FINGERPRINT = "d".repeat(64);
    private static final String CAPABILITY_FINGERPRINT = "e".repeat(64);

    private Context context;
    private String databaseName;
    private TranslationRepository repository;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-g2c-b1-" + UUID.randomUUID() + ".db";
        repository = new TranslationRepository(context, databaseName);
    }

    @After public void tearDown() {
        if (repository != null) repository.close();
        context.deleteDatabase(databaseName);
    }

    @Test public void freshSchemaIsV25WithImmutableV15EvaluationHistory() {
        SQLiteDatabase db = repository.editorialWritableDatabase();
        assertEquals(26, db.getVersion());
        assertTable(db, "editorial_pack_compatibility_evaluations");
        assertIndex(db, "idx_editorial_pack_compatibility_evaluations_pack");
        assertIndex(db, "idx_editorial_pack_compatibility_evaluations_import");
        assertColumn(db, "editorial_pack_compatibility_evaluations", "trusted_profile_id");
        assertColumn(db, "editorial_pack_compatibility_evaluations", "trusted_profile_version");
        assertColumn(db, "editorial_pack_compatibility_evaluations", "canonical_profile_hash");
        assertColumn(db, "editorial_pack_compatibility_evaluations", "evaluator_contract_version");
        assertColumn(db, "editorial_pack_compatibility_evaluations", "context_fingerprint");
        assertColumn(db, "editorial_pack_compatibility_evaluations", "reason_code");
        assertTrigger(db, "trg_editorial_pack_compatibility_evaluations_no_update");
        assertTrigger(db, "trg_editorial_pack_compatibility_evaluations_no_delete");
    }

    @Test public void trustedEvaluationRoundTripsAndCollectionsAreImmutable() {
        Seed seed = seedV14Rows(repository.editorialWritableDatabase(), "blocked");
        EditorialPackCompatibilityEvaluation expected = trusted("evaluation-1", seed.importId, null, seed.compatibilityId, 10L);
        EditorialPackCompatibilityEvaluationDao dao = new EditorialPackCompatibilityEvaluationDao(repository);

        long rowId = dao.append(expected);
        EditorialPackCompatibilityEvaluation actual = dao.findByEvaluationId("evaluation-1").orElseThrow();
        assertEquals(rowId, actual.id());
        assertEquals(EditorialPackCompatibilityEvaluation.Attestation.TRUSTED_PROFILE, actual.attestation());
        assertEquals(PROFILE_HASH, actual.canonicalProfileHash().orElseThrow());
        assertEquals("profile.current", actual.trustedProfileId().orElseThrow());
        assertEquals("1.0.0", actual.trustedProfileVersion().orElseThrow());
        assertEquals(expected.contextFingerprint().orElseThrow(), actual.contextFingerprint().orElseThrow());
        assertEquals("ENGINE_UPGRADE_REQUIRED", actual.compatibilityOutcome().name());
        assertEquals("MISSING_ENGINE_CAPABILITY", actual.reasonCode());
        assertEquals("unicode blocker: thiếu SAFE4 • 日本語", actual.blockerDetails());
        List<EditorialPackCompatibilityEvaluation> values = dao.listByImportId(seed.importId);
        assertEquals(1, values.size());
        try { values.clear(); fail("DAO collections must be immutable"); }
        catch (UnsupportedOperationException expectedException) { }
    }

    @Test public void differentContextsForOnePackRemainAppendOnly() {
        Seed seed = seedV14Rows(repository.editorialWritableDatabase(), "context");
        EditorialPackCompatibilityEvaluationDao dao = new EditorialPackCompatibilityEvaluationDao(repository);
        dao.append(trusted("evaluation-a", seed.importId, null, null, 100L));
        dao.append(trustedWithAdapter("evaluation-b", seed.importId, "f".repeat(64), 101L));
        List<EditorialPackCompatibilityEvaluation> values = dao.listByPackHash(PACK_HASH);
        assertEquals(2, values.size());
        assertEquals(List.of("evaluation-a", "evaluation-b"), values.stream().map(EditorialPackCompatibilityEvaluation::evaluationId).toList());
        assertNotEquals(values.get(0).contextFingerprint(), values.get(1).contextFingerprint());
    }

    @Test public void sameContextDoesNotMakeEvaluationIdsInterchangeable() {
        Seed seed = seedV14Rows(repository.editorialWritableDatabase(), "same-context");
        EditorialPackCompatibilityEvaluationDao dao = new EditorialPackCompatibilityEvaluationDao(repository);
        dao.append(trusted("evaluation-a", seed.importId, null, null, 100L));
        dao.append(trusted("evaluation-b", seed.importId, null, null, 101L));

        EditorialPackCompatibilityEvaluation first = dao.findByEvaluationId("evaluation-a")
                .orElseThrow();
        EditorialPackCompatibilityEvaluation second = dao.findByEvaluationId("evaluation-b")
                .orElseThrow();
        assertEquals(first.contextFingerprint(), second.contextFingerprint());
        assertEquals(seed.importId, first.importId());
        assertEquals(seed.importId, second.importId());
        assertNotEquals(first.evaluationId(), second.evaluationId());
    }

    @Test public void duplicateEvaluationIdentityIsRejectedWithoutReplacingEvidence() {
        Seed seed = seedV14Rows(repository.editorialWritableDatabase(), "duplicate");
        EditorialPackCompatibilityEvaluationDao dao = new EditorialPackCompatibilityEvaluationDao(repository);
        dao.append(trusted("same-id", seed.importId, null, null, 1L));
        try {
            dao.append(trustedWithAdapter("same-id", seed.importId, "f".repeat(64), 2L));
            fail("duplicate identity must be rejected");
        } catch (SQLiteConstraintException expected) { }
        EditorialPackCompatibilityEvaluation stored = dao.findByEvaluationId("same-id").orElseThrow();
        assertEquals(1L, stored.evaluatedAt());
        assertEquals(1, dao.listByPackHash(PACK_HASH).size());
    }

    @Test public void legacyV14RowsAreVisibleOnlyAsUnattested() {
        Seed seed = seedV14Rows(repository.editorialWritableDatabase(), "legacy");
        List<EditorialPackCompatibilityEvaluation> values = new EditorialPackCompatibilityEvaluationDao(repository)
                .listByPackHashIncludingLegacy(PACK_HASH);
        assertEquals(1, values.size());
        EditorialPackCompatibilityEvaluation legacy = values.get(0);
        assertEquals(EditorialPackCompatibilityEvaluation.Attestation.LEGACY_UNATTESTED, legacy.attestation());
        assertEquals(Optional.empty(), legacy.trustedProfileId());
        assertEquals(Optional.empty(), legacy.canonicalProfileHash());
        assertEquals(Optional.empty(), legacy.contextFingerprint());
        assertEquals("LEGACY_UNATTESTED", legacy.reasonCode());
        assertEquals("legacy QA blocked • lý do", legacy.blockerDetails());
        assertEquals(seed.compatibilityId, legacy.compatibilityResultId().orElseThrow().longValue());
    }

    @Test public void migrationV14ToV17PreservesRowsAndDoesNotBackfillProvenance() throws Exception {
        repository.close();
        Seed expected;
        Path path = prepareOldDatabase(14);
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        expected = seedV14Rows(old, "migration");
        old.setVersion(14);
        old.close();

        repository = new TranslationRepository(context, databaseName);
        SQLiteDatabase db = repository.editorialReadableDatabase();
        assertEquals(26, db.getVersion());
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM editorial_packs"));
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM editorial_pack_compatibility_results"));
        assertEquals(0, scalarInt(db, "SELECT COUNT(*) FROM editorial_pack_compatibility_evaluations"));
        try (Cursor cursor = db.rawQuery("SELECT state,blocked_reason FROM editorial_packs WHERE id=?", new String[]{String.valueOf(expected.packId)})) {
            assertTrue(cursor.moveToFirst());
            assertEquals("STORED_BLOCKED", cursor.getString(0));
            assertEquals("migration blocked", cursor.getString(1));
        }
        EditorialPackCompatibilityEvaluation legacy = new EditorialPackCompatibilityEvaluationDao(repository)
                .listByPackHashIncludingLegacy(PACK_HASH).get(0);
        assertEquals(EditorialPackCompatibilityEvaluation.Attestation.LEGACY_UNATTESTED, legacy.attestation());
        assertEquals(Optional.empty(), legacy.trustedProfileId());
        assertEquals(expected.compatibilityId, legacy.id());
    }

    @Test public void migrationChainV13ToV14ToV15ToV17AndReopenIsIdempotent() throws Exception {
        repository.close();
        Path path = prepareOldDatabase(13);
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        old.setVersion(13);
        old.close();
        repository = new TranslationRepository(context, databaseName);
        assertEquals(26, repository.editorialReadableDatabase().getVersion());
        assertTable(repository.editorialReadableDatabase(), "editorial_packs");
        assertTable(repository.editorialReadableDatabase(), "editorial_pack_compatibility_evaluations");
        repository.close();
        repository = new TranslationRepository(context, databaseName);
        assertEquals(26, repository.editorialReadableDatabase().getVersion());
        assertEquals(1, scalarInt(repository.editorialReadableDatabase(), "SELECT COUNT(*) FROM sqlite_master WHERE name='editorial_pack_compatibility_evaluations'"));
    }

    @Test public void failedV14ToV15MigrationRollsBackSchemaVersionAndLegacyRows() throws Exception {
        repository.close();
        Path path = prepareOldDatabase(14);
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        Seed expected = seedV14Rows(old, "rollback");
        old.execSQL("CREATE TABLE editorial_pack_compatibility_evaluations (id INTEGER PRIMARY KEY)");
        old.setVersion(14);
        old.close();
        repository = new TranslationRepository(context, databaseName);
        try { repository.editorialWritableDatabase(); fail("malformed v15 object must fail migration"); }
        catch (SQLiteException expectedException) { }
        repository.close();
        SQLiteDatabase reopened = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        assertEquals(14, reopened.getVersion());
        assertEquals(1, scalarInt(reopened, "SELECT COUNT(*) FROM editorial_packs WHERE id=" + expected.packId));
        assertEquals(0, scalarInt(reopened, "SELECT COUNT(*) FROM sqlite_master WHERE name='idx_editorial_pack_compatibility_evaluations_pack'"));
        reopened.close();
    }

    @Test public void appendDoesNotChangePackStorageState() {
        Seed seed = seedV14Rows(repository.editorialWritableDatabase(), "state");
        SQLiteDatabase db = repository.editorialReadableDatabase();
        String before = scalarString(db, "SELECT state || '|' || storage_key FROM editorial_packs WHERE id=" + seed.packId);
        new EditorialPackCompatibilityEvaluationDao(repository).append(trusted("state-evaluation", seed.importId, seed.packId, seed.compatibilityId, 1L));
        assertEquals(before, scalarString(db, "SELECT state || '|' || storage_key FROM editorial_packs WHERE id=" + seed.packId));
    }

    private EditorialPackCompatibilityEvaluation trusted(String evaluationId, String importId, Long packRowId, Long resultId, long timestamp) {
        return trustedWithAdapter(evaluationId, importId, ADAPTER_FINGERPRINT, timestamp, packRowId, resultId);
    }

    private EditorialPackCompatibilityEvaluation trustedWithAdapter(String evaluationId, String importId, String adapter, long timestamp) {
        return trustedWithAdapter(evaluationId, importId, adapter, timestamp, null, null);
    }

    private EditorialPackCompatibilityEvaluation trustedWithAdapter(String evaluationId, String importId, String adapter, long timestamp, Long packRowId, Long resultId) {
        EditorialCompatibilityEvaluationContext context = new EditorialCompatibilityEvaluationContext(
                PACK_HASH, "profile.current", "1.0.0", PROFILE_HASH, "4.16-dev.30", MACHINE_FINGERPRINT,
                "editorial-compatibility-v1", adapter, CAPABILITY_FINGERPRINT);
        return EditorialPackCompatibilityEvaluation.trusted(evaluationId, importId, packRowId, resultId, context,
                EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, "MISSING_ENGINE_CAPABILITY",
                "unicode blocker: thiếu SAFE4 • 日本語", timestamp);
    }

    private Seed seedV14Rows(SQLiteDatabase db, String suffix) {
        ContentValues pack = new ContentValues();
        pack.put("pack_id", "pack." + suffix); pack.put("version", "1.0.0"); pack.put("canonical_pack_hash", PACK_HASH);
        pack.put("contract_version", "contract.v1"); pack.put("schema_version", "schema.v1"); pack.put("minimum_engine_version", "4.16");
        pack.put("compatibility_class", "BLOCKED"); pack.put("state", "STORED_BLOCKED"); pack.put("storage_key", "storage/" + suffix);
        pack.put("manifest_canonical_json", "{}"); pack.put("created_at", 1L); pack.put("validated_at", 2L);
        pack.put("engine_version_used", "4.16-dev.30"); pack.put("blocked_reason", "migration blocked");
        long packId = db.insertOrThrow("editorial_packs", null, pack);
        ContentValues importRow = new ContentValues(); importRow.put("import_id", "import." + suffix); importRow.put("state", "STORED_BLOCKED");
        importRow.put("pack_id", "pack." + suffix); importRow.put("pack_version", "1.0.0"); importRow.put("canonical_pack_hash", PACK_HASH);
        importRow.put("staging_path", ""); importRow.put("storage_key", "storage/" + suffix); importRow.put("storage_moved", 1);
        importRow.put("pack_row_id", packId); importRow.put("blocked_reason", "migration blocked"); importRow.put("created_at", 1L); importRow.put("updated_at", 2L);
        String importId = "import." + suffix; db.insertOrThrow("editorial_pack_imports", null, importRow);
        ContentValues result = new ContentValues(); result.put("import_id", importId); result.put("pack_row_id", packId); result.put("canonical_pack_hash", PACK_HASH);
        result.put("engine_version_used", "4.16-dev.30"); result.put("machine_contract_fingerprint", MACHINE_FINGERPRINT);
        result.put("compatibility_class", "BLOCKED"); result.put("required_class", "ENGINE_UPGRADE_REQUIRED"); result.put("blocked_reason", "legacy QA blocked • lý do"); result.put("evaluated_at", 3L);
        long compatibilityId = db.insertOrThrow("editorial_pack_compatibility_results", null, result);
        return new Seed(packId, importId, compatibilityId);
    }

    private Path prepareOldDatabase(int version) throws Exception {
        Path path = context.getDatabasePath(databaseName).toPath();
        Files.createDirectories(path.getParent());
        Files.deleteIfExists(path);
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        if (version >= 14) for (String sql : EditorialMigrationSpec.from13To14()) old.execSQL(sql);
        old.setVersion(version); old.close();
        return path;
    }

    private static int scalarInt(SQLiteDatabase db, String sql) {
        try (Cursor cursor = db.rawQuery(sql, null)) { assertTrue(cursor.moveToFirst()); return cursor.getInt(0); }
    }

    private static String scalarString(SQLiteDatabase db, String sql) {
        try (Cursor cursor = db.rawQuery(sql, null)) { assertTrue(cursor.moveToFirst()); return cursor.getString(0); }
    }

    private static void assertTable(SQLiteDatabase db, String name) {
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='" + name + "'"));
    }

    private static void assertIndex(SQLiteDatabase db, String name) {
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='" + name + "'"));
    }

    private static void assertTrigger(SQLiteDatabase db, String name) {
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='trigger' AND name='" + name + "'"));
    }

    private static void assertColumn(SQLiteDatabase db, String table, String column) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (cursor.moveToNext()) if (column.equals(cursor.getString(1))) return;
        }
        fail("missing column " + table + "." + column);
    }

    private record Seed(long packId, String importId, long compatibilityId) { }
}
