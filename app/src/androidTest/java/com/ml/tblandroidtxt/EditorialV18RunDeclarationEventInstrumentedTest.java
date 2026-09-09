package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.EditorialAuthoritativeRunDeclaration;
import com.ml.tblandroidtxt.editorial.pack.EditorialAuthoritativeRunDeclarationDraft;
import com.ml.tblandroidtxt.editorial.pack.EditorialClosureEventEligibility;
import com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshot;
import com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshotEntry;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageRecord;
import com.ml.tblandroidtxt.editorial.pack.EditorialProjectRevision;
import com.ml.tblandroidtxt.editorial.pack.EditorialRequiredInputRoleContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialRunClosureEvent;
import com.ml.tblandroidtxt.editorial.pack.EditorialRunClosureEventDraft;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Isolated v18 declaration/event store QA; no lifecycle caller or B1 wiring is used. */
@RunWith(AndroidJUnit4.class)
public final class EditorialV18RunDeclarationEventInstrumentedTest {
    private static final String PACK = "a".repeat(64);
    private static final String PROFILE = "b".repeat(64);
    private static final String MACHINE = "c".repeat(64);
    private static final String RAW = "d".repeat(64);
    private static final String DRAFT = "e".repeat(64);
    private static final String ATTESTATION = "f".repeat(64);
    private static final String PARENT = "1".repeat(64);
    private static final String OTHER_PARENT = "2".repeat(64);

    private Context context;
    private String databaseName;
    private TranslationRepository repository;
    private EditorialProjectRevision revision;
    private EditorialInputScopeSnapshot snapshot;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-g2-c1b1c2b2a2-" + UUID.randomUUID() + ".db";
        repository = new TranslationRepository(context, databaseName);
    }

    @After public void tearDown() {
        if (repository != null) repository.close();
        context.deleteDatabase(databaseName);
    }

    @Test public void freshV18HasExactlyTwoNewStoresZeroRowsAndNoLegacyForeignKey() {
        SQLiteDatabase db = repository.editorialWritableDatabase();
        assertEquals(22, db.getVersion());
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='editorial_authoritative_run_declarations'"));
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='editorial_run_closure_events'"));
        assertEquals(0, scalarInt(db, "SELECT COUNT(*) FROM editorial_authoritative_run_declarations"));
        assertEquals(0, scalarInt(db, "SELECT COUNT(*) FROM editorial_run_closure_events"));
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='trigger' AND name='trg_authoritative_run_declarations_no_update'"));
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='trigger' AND name='trg_authoritative_run_declarations_no_delete'"));
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='trigger' AND name='trg_run_closure_events_no_update'"));
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='trigger' AND name='trg_run_closure_events_no_delete'"));
        String ddl = scalarText(db, "SELECT sql FROM sqlite_master WHERE type='table' AND name='editorial_run_closure_events'");
        assertFalse(ddl.contains("editorial_runs"));
        assertFalse(ddl.contains("source_run_row_id"));
        assertEquals(1, scalarInt(db, "PRAGMA foreign_keys"));
    }

    @Test public void declarationAppendRetryCollisionDistinctOrdinalsAndRestartReadback() {
        prepareContext();
        EditorialAuthoritativeRunDeclarationDao dao = new EditorialAuthoritativeRunDeclarationDao(
                repository, () -> 100L);
        EditorialIdentityAppendResult<EditorialAuthoritativeRunDeclaration> first =
                dao.appendNewAuthorizedAttempt(declarationDraft("reviewed-attempt-one", "phase-1",
                        EditorialLineageNodeKind.ROOT, null));
        assertEquals(EditorialIdentityPersistenceCode.APPENDED, first.code());
        assertEquals(0L, first.value().runAttemptOrdinal());
        assertEquals(EditorialIdentityPersistenceCode.ALREADY_EXISTS,
                dao.appendNewAuthorizedAttempt(declarationDraft("reviewed-attempt-one", "phase-1",
                        EditorialLineageNodeKind.ROOT, null)).code());
        assertEquals(EditorialIdentityPersistenceCode.ALREADY_EXISTS,
                dao.retrySameAttempt(first.value().declarationIdentity()).code());
        assertEquals(EditorialIdentityPersistenceCode.ATTEMPT_REQUEST_COLLISION,
                dao.appendNewAuthorizedAttempt(declarationDraft("reviewed-attempt-one", "phase-2",
                        EditorialLineageNodeKind.ROOT, null)).code());
        EditorialIdentityAppendResult<EditorialAuthoritativeRunDeclaration> second =
                dao.appendNewAuthorizedAttempt(declarationDraft("reviewed-attempt-two", "phase-1",
                        EditorialLineageNodeKind.ROOT, null));
        assertEquals(EditorialIdentityPersistenceCode.APPENDED, second.code());
        assertEquals(1L, second.value().runAttemptOrdinal());
        assertEquals(List.of(0L, 1L), dao.listByAllocationScope(first.value().allocationScope())
                .stream().map(EditorialAuthoritativeRunDeclaration::runAttemptOrdinal).toList());
        assertEquals(100L, scalarLong(repository.editorialReadableDatabase(),
                "SELECT created_at FROM editorial_authoritative_run_declarations WHERE declaration_identity='"
                        + first.value().declarationIdentity() + "'"));
        repository.close();
        repository = new TranslationRepository(context, databaseName);
        assertEquals(first.value().declarationFingerprint(), new EditorialAuthoritativeRunDeclarationDao(repository)
                .findByDeclarationIdentity(first.value().declarationIdentity()).orElseThrow().declarationFingerprint());
    }

    @Test public void concurrentSameSelectorCreatesOneRowAndConcurrentDistinctSelectorsGetDistinctOrdinals()
            throws Exception {
        prepareContext();
        EditorialAuthoritativeRunDeclarationDraft same = declarationDraft(
                "concurrent-same-request", "phase-1", EditorialLineageNodeKind.ROOT, null);
        EditorialAuthoritativeRunDeclarationDao dao = new EditorialAuthoritativeRunDeclarationDao(
                repository, () -> 101L);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<EditorialIdentityAppendResult<EditorialAuthoritativeRunDeclaration>> one = executor.submit(
                    () -> dao.appendNewAuthorizedAttempt(same));
            Future<EditorialIdentityAppendResult<EditorialAuthoritativeRunDeclaration>> two = executor.submit(
                    () -> dao.appendNewAuthorizedAttempt(same));
            List<EditorialIdentityPersistenceCode> sameCodes = List.of(one.get().code(), two.get().code());
            assertTrue(sameCodes.contains(EditorialIdentityPersistenceCode.APPENDED));
            assertTrue(sameCodes.contains(EditorialIdentityPersistenceCode.ALREADY_EXISTS));
            assertEquals(1, count("editorial_authoritative_run_declarations"));
            Future<EditorialIdentityAppendResult<EditorialAuthoritativeRunDeclaration>> three = executor.submit(
                    () -> dao.appendNewAuthorizedAttempt(declarationDraft("concurrent-three", "phase-1",
                            EditorialLineageNodeKind.ROOT, null)));
            Future<EditorialIdentityAppendResult<EditorialAuthoritativeRunDeclaration>> four = executor.submit(
                    () -> dao.appendNewAuthorizedAttempt(declarationDraft("concurrent-four", "phase-1",
                            EditorialLineageNodeKind.ROOT, null)));
            assertEquals(EditorialIdentityPersistenceCode.APPENDED, three.get().code());
            assertEquals(EditorialIdentityPersistenceCode.APPENDED, four.get().code());
            Set<Long> ordinals = new HashSet<>(List.of(three.get().value().runAttemptOrdinal(),
                    four.get().value().runAttemptOrdinal()));
            assertEquals(Set.of(1L, 2L), ordinals);
            assertEquals(3, count("editorial_authoritative_run_declarations"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test public void missingReferenceFailsWithoutConsumingOrdinalOrWritingPartialRow() {
        EditorialAuthoritativeRunDeclarationDao dao = new EditorialAuthoritativeRunDeclarationDao(
                repository, () -> 1L);
        EditorialAuthoritativeRunDeclarationDraft missing = new EditorialAuthoritativeRunDeclarationDraft(
                "retryable-missing-reference", "3".repeat(64), "4".repeat(64), "missing-evaluation",
                "translate", "phase-1", "5".repeat(64), "attestation-v1",
                EditorialLineageNodeKind.ROOT, null);
        assertEquals(EditorialIdentityPersistenceCode.FOREIGN_REFERENCE_MISSING,
                dao.appendNewAuthorizedAttempt(missing).code());
        assertEquals(0, count("editorial_authoritative_run_declarations"));
        prepareContext();
        EditorialIdentityAppendResult<EditorialAuthoritativeRunDeclaration> appended =
                dao.appendNewAuthorizedAttempt(declarationDraft("retryable-missing-reference", "phase-1",
                        EditorialLineageNodeKind.ROOT, null));
        assertEquals(EditorialIdentityPersistenceCode.APPENDED, appended.code());
        assertEquals(0L, appended.value().runAttemptOrdinal());
    }

    @Test public void exactEventAppendReplayCrossChecksAndRestartReadback() {
        prepareContext();
        EditorialAuthoritativeRunDeclaration declaration = declarationDao().appendNewAuthorizedAttempt(
                declarationDraft("event-request-root", "phase-1", EditorialLineageNodeKind.ROOT, null)).value();
        EditorialRunClosureEventDraft eventDraft = eventDraft(declaration, EditorialLineageNodeKind.ROOT,
                null);
        EditorialRunClosureEventDao events = new EditorialRunClosureEventDao(repository, () -> 200L);
        EditorialClosureEventAppendPermit permit = permit(snapshot.manifestFingerprint(), ATTESTATION);
        EditorialIdentityAppendResult<EditorialRunClosureEvent> first = events.append(eventDraft, permit);
        assertEquals(EditorialIdentityPersistenceCode.APPENDED, first.code());
        long appendedAt = first.value().appendedAt();
        EditorialIdentityAppendResult<EditorialRunClosureEvent> replay =
                new EditorialRunClosureEventDao(repository, () -> 999L).append(eventDraft, permit);
        assertEquals(EditorialIdentityPersistenceCode.ALREADY_EXISTS, replay.code());
        assertEquals(appendedAt, replay.value().appendedAt());
        assertEquals(1, count("editorial_run_closure_events"));
        assertEquals(first.value().closureEventFingerprint(), events
                .findByAuthoritativeRunIdentity(declaration.declarationIdentity()).orElseThrow()
                .closureEventFingerprint());
        repository.close();
        repository = new TranslationRepository(context, databaseName);
        assertEquals(first.value().closureEventIdentity(), new EditorialRunClosureEventDao(repository)
                .findByClosureEventIdentity(first.value().closureEventIdentity()).orElseThrow()
                .closureEventIdentity());
    }

    @Test public void eventTrustIntentParentManifestAndOrdinalFailuresWriteNothing() {
        prepareContext();
        EditorialAuthoritativeRunDeclaration declaration = declarationDao().appendNewAuthorizedAttempt(
                declarationDraft("event-request-collisions", "phase-1", EditorialLineageNodeKind.ROOT, null)).value();
        EditorialRunClosureEventDao events = new EditorialRunClosureEventDao(repository, () -> 1L);
        EditorialRunClosureEventDraft root = eventDraft(declaration, EditorialLineageNodeKind.ROOT, null);
        assertEquals(EditorialIdentityPersistenceCode.TRUSTED_CONTEXT_MISMATCH,
                events.append(root, null).code());
        assertEquals(EditorialIdentityPersistenceCode.EVENT_INTENT_COLLISION,
                events.append(eventDraft(declaration, EditorialLineageNodeKind.CHILD, PARENT),
                        permit(snapshot.manifestFingerprint(), ATTESTATION)).code());
        assertEquals(EditorialIdentityPersistenceCode.FROZEN_MANIFEST_MISMATCH,
                events.append(eventDraft(declaration, EditorialLineageNodeKind.ROOT, null),
                        permit("9".repeat(64), ATTESTATION)).code());
        assertEquals(EditorialIdentityPersistenceCode.TRUSTED_CONTEXT_MISMATCH,
                events.append(eventDraft(declaration, EditorialLineageNodeKind.ROOT, null),
                        permitWithEvaluation("other-evaluation", snapshot.manifestFingerprint(), ATTESTATION)).code());
        assertEquals(0, count("editorial_run_closure_events"));
    }

    @Test public void childExactParentSucceedsAndParentChangeCollides() {
        prepareContext();
        insertLineageParent(PARENT, revision.revisionIdentity(), snapshot.scopeSnapshotIdentity());
        EditorialAuthoritativeRunDeclaration child = declarationDao().appendNewAuthorizedAttempt(
                declarationDraft("event-request-child", "phase-child", EditorialLineageNodeKind.CHILD, PARENT)).value();
        EditorialRunClosureEventDao events = new EditorialRunClosureEventDao(repository, () -> 10L);
        EditorialClosureEventAppendPermit permit = permit(snapshot.manifestFingerprint(), ATTESTATION);
        assertEquals(EditorialIdentityPersistenceCode.APPENDED, events.append(eventDraft(child,
                EditorialLineageNodeKind.CHILD, PARENT), permit).code());
        assertEquals(EditorialIdentityPersistenceCode.EVENT_PARENT_COLLISION, events.append(eventDraft(child,
                EditorialLineageNodeKind.CHILD, OTHER_PARENT), permit).code());
        assertEquals(1, count("editorial_run_closure_events"));
    }

    @Test public void declarationAndEventUpdateDeleteAreRejectedAndForeignRetentionIsRestrictive() {
        prepareContext();
        EditorialAuthoritativeRunDeclaration declaration = declarationDao().appendNewAuthorizedAttempt(
                declarationDraft("immutable-request", "phase-1", EditorialLineageNodeKind.ROOT, null)).value();
        EditorialRunClosureEventDao events = new EditorialRunClosureEventDao(repository, () -> 5L);
        EditorialRunClosureEvent event = events.append(eventDraft(declaration, EditorialLineageNodeKind.ROOT,
                null),
                permit(snapshot.manifestFingerprint(), ATTESTATION)).value();
        SQLiteDatabase db = repository.editorialWritableDatabase();
        assertRejected(() -> db.execSQL("UPDATE editorial_authoritative_run_declarations SET run_kind='changed'"));
        assertRejected(() -> db.execSQL("DELETE FROM editorial_authoritative_run_declarations"));
        assertRejected(() -> db.execSQL("UPDATE editorial_run_closure_events SET run_kind='changed'"));
        assertRejected(() -> db.execSQL("DELETE FROM editorial_run_closure_events"));
        assertNotNull(event);
        assertEquals(1, count("editorial_authoritative_run_declarations"));
        assertEquals(1, count("editorial_run_closure_events"));
    }

    @Test public void v17ToV18IsAdditiveZeroBackfillAndPreservesRows() throws Exception {
        repository.close();
        Path path = context.getDatabasePath(databaseName).toPath();
        createVersionedSchema(path, 17);
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        seedV17Rows(old);
        old.close();
        repository = new TranslationRepository(context, databaseName);
        SQLiteDatabase upgraded = repository.editorialReadableDatabase();
        assertEquals(22, upgraded.getVersion());
        assertEquals(1, scalarInt(upgraded, "SELECT COUNT(*) FROM editorial_project_revisions"));
        assertEquals(1, scalarInt(upgraded, "SELECT COUNT(*) FROM editorial_input_scope_snapshots"));
        assertEquals(0, scalarInt(upgraded, "SELECT COUNT(*) FROM editorial_authoritative_run_declarations"));
        assertEquals(0, scalarInt(upgraded, "SELECT COUNT(*) FROM editorial_run_closure_events"));
        assertEquals("legacy-revision", scalarText(upgraded,
                "SELECT project_semantic_key FROM editorial_project_revisions"));
    }

    @Test public void fullV13ToV18ChainCreatesEmptyV18Stores() throws Exception {
        repository.close();
        Path path = context.getDatabasePath(databaseName).toPath();
        createVersionedSchema(path, 13);
        repository = new TranslationRepository(context, databaseName);
        SQLiteDatabase upgraded = repository.editorialReadableDatabase();
        assertEquals(22, upgraded.getVersion());
        assertEquals(1, scalarInt(upgraded, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='editorial_authoritative_run_declarations'"));
        assertEquals(1, scalarInt(upgraded, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='editorial_run_closure_events'"));
        assertEquals(0, count("editorial_authoritative_run_declarations"));
        assertEquals(0, count("editorial_run_closure_events"));
    }

    @Test public void malformedV18MigrationRollsBackToReadableV17() throws Exception {
        repository.close();
        Path path = context.getDatabasePath(databaseName).toPath();
        createVersionedSchema(path, 17);
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        old.execSQL("CREATE TABLE editorial_authoritative_run_declarations (bad INTEGER)");
        old.setVersion(17);
        old.close();
        repository = new TranslationRepository(context, databaseName);
        try {
            repository.editorialWritableDatabase();
            fail("malformed v18 declaration table must fail migration");
        } catch (SQLiteException expected) {
            assertNotNull(expected.getMessage());
        }
        repository.close();
        SQLiteDatabase reopened = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        assertEquals(17, reopened.getVersion());
        assertEquals(0, scalarInt(reopened, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='editorial_run_closure_events'"));
        reopened.close();
    }

    private EditorialAuthoritativeRunDeclarationDao declarationDao() {
        return new EditorialAuthoritativeRunDeclarationDao(repository, () -> 100L);
    }

    private EditorialAuthoritativeRunDeclarationDraft declarationDraft(
            String request, String phase, EditorialLineageNodeKind node, String parent) {
        return new EditorialAuthoritativeRunDeclarationDraft(request, revision.revisionIdentity(),
                snapshot.scopeSnapshotIdentity(), "test-evaluation", "translate", phase,
                snapshot.manifestFingerprint(), "test-frozen-manifest-attestation", node, parent);
    }

    private EditorialRunClosureEventDraft eventDraft(
            EditorialAuthoritativeRunDeclaration declaration,
            EditorialLineageNodeKind node, String parent) {
        return new EditorialRunClosureEventDraft(declaration.attemptRequestSelector(), node, parent,
                EditorialClosureEventEligibility.ELIGIBLE);
    }

    private EditorialClosureEventAppendPermit permit(String manifest, String fingerprint) {
        return permitWithEvaluation("test-evaluation", manifest, fingerprint);
    }

    private EditorialClosureEventAppendPermit permitWithEvaluation(
            String evaluation, String manifest, String fingerprint) {
        return new EditorialClosureEventAppendPermit(evaluation, manifest, true,
                "closure-attestation-v1", fingerprint);
    }

    private void prepareContext() {
        revision = new EditorialProjectRevision("project-projection-v1", "semantic/project/001",
                "project-definition-v1", "novel", PROFILE, MACHINE);
        EditorialRequiredInputRoleContract roles = new EditorialRequiredInputRoleContract(
                "roles-v1", new LinkedHashSet<>(List.of("RAW", "DRAFT")));
        snapshot = new EditorialInputScopeSnapshot(revision.revisionIdentity(), "scope-v1",
                "chapter/001", roles, "manifest-v1", List.of(
                new EditorialInputScopeSnapshotEntry("DRAFT", 0L, DRAFT, 20L, 2L),
                new EditorialInputScopeSnapshotEntry("RAW", 0L, RAW, 10L, 1L)));
        assertEquals(EditorialIdentityPersistenceCode.APPENDED,
                new EditorialProjectRevisionDao(repository).append(revision, null, 10L).code());
        assertEquals(EditorialIdentityPersistenceCode.APPENDED,
                new EditorialInputScopeSnapshotDao(repository).appendWithEntries(snapshot, null, 11L).code());
        seedTrustedCompatibleEvidence();
    }

    private void insertLineageParent(String identity, String projectIdentity, String scopeIdentity) {
        ContentValues row = new ContentValues();
        row.put("record_identity", identity);
        row.put("record_fingerprint", "9".repeat(64));
        row.put("canonical_pack_hash", PACK);
        row.put("trusted_profile_id", "trusted-test");
        row.put("trusted_profile_version", "1");
        row.put("canonical_profile_hash", PROFILE);
        row.put("machine_contract_fingerprint", MACHINE);
        row.put("contract_version", "contract.v1");
        row.put("schema_version", "schema.v1");
        row.put("project_identity", projectIdentity);
        row.put("input_scope_identity", scopeIdentity);
        row.put("run_evaluation_identity", "parent-run");
        row.put("input_manifest_version", "manifest-v1");
        row.put("input_manifest_fingerprint", snapshot.manifestFingerprint());
        row.put("node_kind", "ROOT");
        row.putNull("parent_record_identity");
        row.putNull("parent_record_fingerprint");
        row.put("created_at", 1L);
        repository.editorialWritableDatabase().insertOrThrow("editorial_lineage_records", null, row);
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
        ContentValues imported = new ContentValues(); imported.put("import_id", "test-import");
        imported.put("state", "STORED_READY_FOR_CERTIFICATION"); imported.put("pack_id", "test-pack");
        imported.put("pack_version", "1.0.0"); imported.put("canonical_pack_hash", PACK);
        imported.put("staging_path", ""); imported.put("storage_key", "test-storage"); imported.put("storage_moved", 1);
        imported.put("pack_row_id", packId); imported.put("blocked_reason", ""); imported.put("created_at", 1L); imported.put("updated_at", 1L);
        db.insertOrThrow("editorial_pack_imports", null, imported);
        ContentValues compatibility = new ContentValues(); compatibility.put("import_id", "test-import");
        compatibility.put("pack_row_id", packId); compatibility.put("canonical_pack_hash", PACK);
        compatibility.put("engine_version_used", "engine-test"); compatibility.put("machine_contract_fingerprint", MACHINE);
        compatibility.put("compatibility_class", "DATA_COMPATIBLE"); compatibility.put("required_class", "DATA_COMPATIBLE");
        compatibility.put("blocked_reason", ""); compatibility.put("evaluated_at", 1L);
        long resultId = db.insertOrThrow("editorial_pack_compatibility_results", null, compatibility);
        ContentValues evaluation = new ContentValues(); evaluation.put("evaluation_id", "test-evaluation");
        evaluation.put("import_id", "test-import"); evaluation.put("pack_row_id", packId); evaluation.put("compatibility_result_id", resultId);
        evaluation.put("canonical_pack_hash", PACK); evaluation.put("trusted_profile_id", "trusted-test");
        evaluation.put("trusted_profile_version", "1"); evaluation.put("canonical_profile_hash", PROFILE);
        evaluation.put("engine_version_used", "engine-test"); evaluation.put("machine_contract_fingerprint", MACHINE);
        evaluation.put("evaluator_contract_version", "evaluator.v1"); evaluation.put("adapter_set_fingerprint", "6".repeat(64));
        evaluation.put("capability_fingerprint", "7".repeat(64)); evaluation.put("context_fingerprint", "8".repeat(64));
        evaluation.put("compatibility_outcome", "DATA_COMPATIBLE"); evaluation.put("reason_code", "TEST_ONLY_TRUSTED");
        evaluation.put("blocker_details", ""); evaluation.put("evaluated_at", 2L);
        db.insertOrThrow("editorial_pack_compatibility_evaluations", null, evaluation);
    }

    private void createVersionedSchema(Path path, int version) throws Exception {
        Files.createDirectories(path.getParent());
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        for (String sql : EditorialMigrationSpec.from10To11()) old.execSQL(sql);
        for (String sql : EditorialMigrationSpec.from11To12()) old.execSQL(sql);
        for (String sql : EditorialMigrationSpec.from12To13()) old.execSQL(sql);
        if (version >= 14) for (String sql : EditorialMigrationSpec.from13To14()) old.execSQL(sql);
        if (version >= 15) for (String sql : EditorialMigrationSpec.from14To15()) old.execSQL(sql);
        if (version >= 16) for (String sql : EditorialMigrationSpec.from15To16()) old.execSQL(sql);
        if (version >= 17) for (String sql : EditorialMigrationSpec.from16To17()) old.execSQL(sql);
        old.setVersion(version);
        old.close();
    }

    private void seedV17Rows(SQLiteDatabase db) {
        ContentValues revisionRow = new ContentValues();
        revisionRow.put("revision_identity", "9".repeat(64));
        revisionRow.put("revision_canonical_version", "project-v1");
        revisionRow.put("project_semantic_key", "legacy-revision");
        revisionRow.put("project_definition_contract_version", "project-definition-v1");
        revisionRow.putNull("semantic_project_type");
        revisionRow.put("scope_policy_fingerprint", "a".repeat(64));
        revisionRow.put("workflow_policy_fingerprint", "b".repeat(64));
        revisionRow.put("project_definition_fingerprint", "c".repeat(64));
        revisionRow.put("project_definition_canonical", "{}");
        revisionRow.putNull("source_project_row_id"); revisionRow.put("created_at", 1L);
        db.insertOrThrow("editorial_project_revisions", null, revisionRow);
        ContentValues scopeRow = new ContentValues();
        scopeRow.put("scope_snapshot_identity", "8".repeat(64));
        scopeRow.put("project_revision_identity", "9".repeat(64));
        scopeRow.put("scope_canonical_version", "scope-v1"); scopeRow.put("canonical_scope_key", "legacy-scope");
        scopeRow.put("required_roles_canonical", "{\"contractVersion\":\"roles-v1\",\"requiredRoles\":[\"RAW\"]}");
        scopeRow.put("required_roles_fingerprint", "d".repeat(64)); scopeRow.put("manifest_version", "manifest-v1");
        scopeRow.put("manifest_fingerprint", "e".repeat(64)); scopeRow.putNull("source_chapter_row_id");
        scopeRow.put("created_at", 2L);
        db.insertOrThrow("editorial_input_scope_snapshots", null, scopeRow);
    }

    private int count(String table) { return scalarInt(repository.editorialReadableDatabase(), "SELECT COUNT(*) FROM " + table); }

    private static int scalarInt(SQLiteDatabase db, String sql) {
        try (Cursor cursor = db.rawQuery(sql, null)) { assertTrue(cursor.moveToFirst()); return cursor.getInt(0); }
    }

    private static long scalarLong(SQLiteDatabase db, String sql) {
        try (Cursor cursor = db.rawQuery(sql, null)) { assertTrue(cursor.moveToFirst()); return cursor.getLong(0); }
    }

    private static String scalarText(SQLiteDatabase db, String sql) {
        try (Cursor cursor = db.rawQuery(sql, null)) { assertTrue(cursor.moveToFirst()); return cursor.getString(0); }
    }

    private static void assertRejected(Runnable operation) {
        try {
            operation.run();
            fail("immutable/FK operation must be rejected");
        } catch (SQLiteException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
