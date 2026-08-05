package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContractFacts;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageCallerSelection;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageInputEntry;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageInputManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageRecord;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageIdentity;
import com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshotEntry;
import com.ml.tblandroidtxt.editorial.pack.EditorialRequiredInputRoleContract;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Isolated C2-A creator/resolver coverage. No production composition calls these seams. */
@RunWith(AndroidJUnit4.class)
public final class EditorialAuthoritativeCreatorResolverInstrumentedTest {
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
    private EditorialTrustedClosedRunFacts trustedFacts;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-g2-c1b1c2a-" + UUID.randomUUID() + ".db";
        repository = new TranslationRepository(context, databaseName);
        trustedFacts = new EditorialTrustedClosedRunFacts("trusted-test", "1", PROFILE, MACHINE, facts());
        seedTrustedCompatibleEvidence();
    }

    @After public void tearDown() {
        if (repository != null) repository.close();
        context.deleteDatabase(databaseName);
    }

    @Test public void creatorIsDeterministicIdempotentAndNeverAcceptsPartialScope() {
        EditorialAuthoritativeIdentityCreator creator = creator();
        EditorialProjectRevisionPreparationRequest project = projectRequest();
        EditorialIdentityAppendResult<?> first = creator.prepareProjectRevision(project);
        EditorialIdentityAppendResult<?> second = creator.prepareProjectRevision(project);
        assertEquals(EditorialIdentityPersistenceCode.APPENDED, first.code());
        assertEquals(EditorialIdentityPersistenceCode.ALREADY_EXISTS, second.code());
        String revision = first.identity();

        EditorialInputScopeSnapshotPreparationRequest scope = scopeRequest(revision);
        EditorialIdentityAppendResult<?> snapshot = creator.prepareInputScopeSnapshot(scope);
        assertEquals(EditorialIdentityPersistenceCode.APPENDED, snapshot.code());
        assertEquals(EditorialIdentityPersistenceCode.ALREADY_EXISTS,
                creator.prepareInputScopeSnapshot(scope).code());
        assertEquals(1, count("editorial_input_scope_snapshots"));
        assertEquals(2, count("editorial_input_scope_snapshot_entries"));

        EditorialInputScopeSnapshotPreparationRequest missingCount = new EditorialInputScopeSnapshotPreparationRequest(
                revision, "scope-v1", "chapter/001", roles(), "manifest-v1", List.of(
                new EditorialInputScopeSnapshotEntry("RAW", 0L, RAW, 10L, null),
                new EditorialInputScopeSnapshotEntry("DRAFT", 0L, DRAFT, 20L, 2L)), null, 12L);
        assertEquals(EditorialIdentityPersistenceCode.INPUT_MANIFEST_INCOMPLETE,
                creator.prepareInputScopeSnapshot(missingCount).code());
        assertEquals(1, count("editorial_input_scope_snapshots"));
        assertEquals(2, count("editorial_input_scope_snapshot_entries"));
    }

    @Test public void closeRunUsesOnlyTestTrustedFactsAndDaoOwnsOrdinal() {
        EditorialAuthoritativeIdentityCreator creator = creator();
        String revision = creator.prepareProjectRevision(projectRequest()).identity();
        String scope = creator.prepareInputScopeSnapshot(scopeRequest(revision)).identity();
        EditorialClosedRunContextClosureRequest closure = new EditorialClosedRunContextClosureRequest(
                revision, scope, "test-evaluation", "ROOT", "phase-1", null, 20L);
        EditorialIdentityAppendResult<?> first = creator.closeRunContext(closure);
        EditorialIdentityAppendResult<?> duplicate = creator.closeRunContext(closure);
        assertEquals(EditorialIdentityPersistenceCode.APPENDED, first.code());
        assertEquals(EditorialIdentityPersistenceCode.ALREADY_EXISTS, duplicate.code());
        assertEquals(0L, new EditorialClosedRunContextDao(repository).findByIdentity(first.identity())
                .orElseThrow().runAttemptOrdinal());

        EditorialAuthoritativeIdentityCreator production = new EditorialAuthoritativeIdentityCreator(repository);
        EditorialClosedRunContextClosureRequest nextAttempt = new EditorialClosedRunContextClosureRequest(
                revision, scope, "test-evaluation", "ROOT", "phase-2", null, 21L);
        assertEquals(EditorialIdentityPersistenceCode.TRUSTED_PROFILE_CONTEXT_MISMATCH,
                production.closeRunContext(nextAttempt).code());
        assertEquals(1, count("editorial_closed_run_contexts"));
    }

    @Test public void resolverReadsOnlyStoredFactsAndSurvivesReopen() {
        EditorialAuthoritativeIdentityCreator creator = creator();
        String revision = creator.prepareProjectRevision(projectRequest()).identity();
        String scope = creator.prepareInputScopeSnapshot(scopeRequest(revision)).identity();
        String closed = creator.closeRunContext(new EditorialClosedRunContextClosureRequest(
                revision, scope, "test-evaluation", "ROOT", "phase-1", null, 20L)).identity();
        SqliteEditorialLineageContextResolver resolver = resolver();
        EditorialLineageCallerSelection selection = new EditorialLineageCallerSelection(
                revision, scope, closed, EditorialLineageNodeKind.ROOT, null);
        EditorialAuthoritativeContextResolutionResult resolved = resolver.resolveDetailed(selection);
        assertEquals(EditorialAuthoritativeContextResolutionCode.RESOLVED, resolved.code());
        assertEquals(PACK, resolved.context().orElseThrow().canonicalPackHash());
        assertEquals(closed, resolved.context().orElseThrow().runEvaluationIdentity());
        assertEquals(2, resolved.context().orElseThrow().inputManifest().entries().size());

        repository.close();
        repository = new TranslationRepository(context, databaseName);
        assertEquals(EditorialAuthoritativeContextResolutionCode.RESOLVED,
                resolver().resolveDetailed(selection).code());
    }

    @Test public void resolverFailsClosedForMismatchesAndResolvesExactParentOnly() {
        EditorialAuthoritativeIdentityCreator creator = creator();
        String revision = creator.prepareProjectRevision(projectRequest()).identity();
        String scope = creator.prepareInputScopeSnapshot(scopeRequest(revision)).identity();
        String closed = creator.closeRunContext(new EditorialClosedRunContextClosureRequest(
                revision, scope, "test-evaluation", "ROOT", "phase-1", null, 20L)).identity();
        SqliteEditorialLineageContextResolver resolver = resolver();
        assertEquals(EditorialAuthoritativeContextResolutionCode.INPUT_SCOPE_REQUIRED,
                resolver.resolveDetailed(new EditorialLineageCallerSelection(revision, "0".repeat(64), closed,
                        EditorialLineageNodeKind.ROOT, null)).code());
        assertEquals(EditorialAuthoritativeContextResolutionCode.RUN_CONTEXT_REQUIRED,
                resolver.resolveDetailed(new EditorialLineageCallerSelection(revision, scope, "0".repeat(64),
                        EditorialLineageNodeKind.ROOT, null)).code());
        assertEquals(EditorialAuthoritativeContextResolutionCode.PARENT_NOT_FOUND,
                resolver.resolveDetailed(new EditorialLineageCallerSelection(revision, scope, closed,
                        EditorialLineageNodeKind.CHILD, "9".repeat(64))).code());

        EditorialLineageRecord parent = parentRecord(revision, scope);
        assertEquals(EditorialLineagePersistenceCode.APPENDED,
                new EditorialLineageDao(repository).append(parent, 30L).code());
        EditorialAuthoritativeContextResolutionResult exact = resolver.resolveDetailed(
                new EditorialLineageCallerSelection(revision, scope, closed,
                        EditorialLineageNodeKind.CHILD, parent.recordIdentity()));
        assertEquals(EditorialAuthoritativeContextResolutionCode.RESOLVED, exact.code());
        assertEquals(1, exact.context().orElseThrow().parentCandidates().size());
        assertEquals(parent.recordIdentity(), exact.context().orElseThrow().parentCandidates().get(0).recordIdentity());
    }

    @Test public void retentionPreflightDetectsAuthoritativeProjectAndRunReferences() {
        long projectId = seedMutableProject();
        long chapterId = seedMutableChapter(projectId);
        long runId = seedMutableRun(chapterId);
        EditorialAuthoritativeIdentityCreator creator = creator();
        EditorialProjectRevisionPreparationRequest project = new EditorialProjectRevisionPreparationRequest(
                "project-projection-v1", "semantic/project/retained", "project-definition-v1", "novel",
                PROFILE, MACHINE, projectId, 10L);
        String revision = creator.prepareProjectRevision(project).identity();
        String scope = creator.prepareInputScopeSnapshot(new EditorialInputScopeSnapshotPreparationRequest(
                revision, "scope-v1", "chapter/retained", roles(), "manifest-v1", entries(), chapterId, 11L)).identity();
        assertEquals(EditorialIdentityPersistenceCode.APPENDED, creator.closeRunContext(
                new EditorialClosedRunContextClosureRequest(revision, scope, "test-evaluation", "ROOT", "phase-retained", runId, 20L)).code());
        EditorialIdentityRetentionPreflight preflight = new EditorialIdentityRetentionPreflight(repository);
        assertEquals(EditorialIdentityRetentionCode.AUTHORITATIVE_REFERENCE_PRESENT, preflight.projectDeletion(projectId));
        assertEquals(EditorialIdentityRetentionCode.AUTHORITATIVE_REFERENCE_PRESENT, preflight.runDeletion(runId));
        assertEquals(EditorialIdentityRetentionCode.CLEAR_TO_DELETE, preflight.projectDeletion(projectId + 99));
        assertEquals(EditorialIdentityRetentionCode.INVALID_SELECTION, preflight.runDeletion(-1));
    }

    private EditorialAuthoritativeIdentityCreator creator() {
        return new EditorialAuthoritativeIdentityCreator(repository, (evaluation, contract, schema) ->
                Optional.of(trustedFacts));
    }

    private SqliteEditorialLineageContextResolver resolver() {
        return new SqliteEditorialLineageContextResolver(repository, (evaluation, contract, schema) ->
                Optional.of(trustedFacts));
    }

    private EditorialProjectRevisionPreparationRequest projectRequest() {
        return new EditorialProjectRevisionPreparationRequest("project-projection-v1", "semantic/project/001",
                "project-definition-v1", "novel", PROFILE, MACHINE, null, 10L);
    }

    private EditorialInputScopeSnapshotPreparationRequest scopeRequest(String revision) {
        return new EditorialInputScopeSnapshotPreparationRequest(revision, "scope-v1", "chapter/001", roles(),
                "manifest-v1", entries(), null, 11L);
    }

    private EditorialRequiredInputRoleContract roles() {
        return new EditorialRequiredInputRoleContract("roles-v1", new LinkedHashSet<>(List.of("RAW", "DRAFT")));
    }

    private List<EditorialInputScopeSnapshotEntry> entries() {
        return List.of(new EditorialInputScopeSnapshotEntry("DRAFT", 0L, DRAFT, 20L, 2L),
                new EditorialInputScopeSnapshotEntry("RAW", 0L, RAW, 10L, 1L));
    }

    private EditorialClosedRunContractFacts facts() {
        return new EditorialClosedRunContractFacts("editorial-pack-contract", "contract.v1",
                "editorial-pack-schema", "schema.v1", "evaluator.v1", ADAPTERS, CAPABILITIES, CONTEXT,
                "engine-test");
    }

    private EditorialLineageRecord parentRecord(String revision, String scope) {
        EditorialLineageIdentity identity = new EditorialLineageIdentity(PACK, "trusted-test", "1", PROFILE,
                MACHINE, "contract.v1", "schema.v1", revision, scope, "previous-closed-run",
                new EditorialLineageInputManifest("manifest-v1", List.of(
                        new EditorialLineageInputEntry("RAW", 0, RAW, 10, 1),
                        new EditorialLineageInputEntry("DRAFT", 0, DRAFT, 20, 2))), EditorialLineageNodeKind.ROOT);
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
        ContentValues imported = new ContentValues(); imported.put("import_id", "test-import");
        imported.put("state", "STORED_READY_FOR_CERTIFICATION"); imported.put("pack_id", "test-pack");
        imported.put("pack_version", "1.0.0"); imported.put("canonical_pack_hash", PACK);
        imported.put("staging_path", ""); imported.put("storage_key", "test-storage"); imported.put("storage_moved", 1);
        imported.put("pack_row_id", packId); imported.put("blocked_reason", ""); imported.put("created_at", 1L); imported.put("updated_at", 1L);
        db.insertOrThrow("editorial_pack_imports", null, imported);
        ContentValues compatibility = new ContentValues(); compatibility.put("import_id", "test-import"); compatibility.put("pack_row_id", packId);
        compatibility.put("canonical_pack_hash", PACK); compatibility.put("engine_version_used", "engine-test");
        compatibility.put("machine_contract_fingerprint", MACHINE); compatibility.put("compatibility_class", "DATA_COMPATIBLE");
        compatibility.put("required_class", "DATA_COMPATIBLE"); compatibility.put("blocked_reason", ""); compatibility.put("evaluated_at", 1L);
        long resultId = db.insertOrThrow("editorial_pack_compatibility_results", null, compatibility);
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

    private long seedMutableProject() {
        ContentValues row = new ContentValues(); row.put("series_name", "retained"); row.put("volume_name", "one");
        row.put("workflow_version", "v1"); row.put("workflow_hash", RAW); row.put("output_tree_uri", "");
        row.put("created_at", 1L); row.put("updated_at", 1L);
        return repository.editorialWritableDatabase().insertOrThrow("editorial_projects", null, row);
    }

    private long seedMutableChapter(long projectId) {
        ContentValues row = new ContentValues(); row.put("project_id", projectId); row.put("chapter_key", "chapter");
        row.put("title", ""); row.put("state", "NEW"); row.put("raw_hash", RAW); row.put("created_at", 1L); row.put("updated_at", 1L);
        return repository.editorialWritableDatabase().insertOrThrow("editorial_chapters", null, row);
    }

    private long seedMutableRun(long chapterId) {
        ContentValues row = new ContentValues(); row.put("chapter_id", chapterId); row.put("run_kind", "ROOT");
        row.put("state", "CLOSED"); row.put("provider", ""); row.put("model", ""); row.put("prompt_hash", RAW);
        row.put("workflow_hash", RAW); row.put("input_manifest_json", "{}"); row.put("created_at", 1L); row.put("updated_at", 1L);
        return repository.editorialWritableDatabase().insertOrThrow("editorial_runs", null, row);
    }

    private int count(String table) {
        try (Cursor cursor = repository.editorialReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            assertTrue(cursor.moveToFirst()); return cursor.getInt(0);
        }
    }
}
