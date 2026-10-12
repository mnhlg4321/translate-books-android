package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContext;
import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContractFacts;
import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContextDraft;
import com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshot;
import com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshotEntry;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageInputEntry;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageInputManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageIdentity;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageRecord;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageRuntimeService;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageValidationCode;
import com.ml.tblandroidtxt.editorial.pack.EditorialProjectRevision;
import com.ml.tblandroidtxt.editorial.pack.EditorialRequiredInputRoleContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialRunLineageBinding;

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

/** Isolated B1 coverage. No production caller or event source is composed here. */
@RunWith(AndroidJUnit4.class)
public final class EditorialRunContextCoordinatorInstrumentedTest {
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
        databaseName = "editorial-g2-c1b1c2b1-" + UUID.randomUUID() + ".db";
        repository = new TranslationRepository(context, databaseName);
        trustedFacts = new EditorialTrustedClosedRunFacts("trusted-test", "1", PROFILE, MACHINE, facts());
        seedTrustedCompatibleEvidence();
    }

    @After public void tearDown() {
        if (repository != null) repository.close();
        context.deleteDatabase(databaseName);
    }

    @Test public void productionResolverFailsClosedAndPerformsZeroWrites() {
        EditorialRunContextCoordinator coordinator = new EditorialRunContextCoordinator(repository);
        EditorialRunContextCoordinatorResult result = coordinator.coordinate(
                new EditorialRunContextCommand("production-event-selector"));
        assertEquals(EditorialRunContextCoordinatorCode.CLOSURE_EVENT_UNAVAILABLE, result.code());
        assertEquals(0, count("editorial_closed_run_contexts"));
        assertEquals(0, count("editorial_lineage_records"));
        assertEquals(0, count("editorial_run_lineage_bindings"));
        assertEquals(EditorialRunContextStatusCode.EVENT_UNAVAILABLE,
                coordinator.inspect(new EditorialRunContextCommand("production-event-selector")).code());
    }

    @Test public void rootHappyPathIsIdempotentAcrossCoordinatorRestart() {
        Fixture fixture = fixture("root-phase");
        EditorialRunClosureEvent event = event(fixture, EditorialLineageNodeKind.ROOT, null,
                "root-phase");
        EditorialRunContextCoordinator first = coordinator(event, 100L);

        EditorialRunContextCoordinatorResult appended = first.coordinate(
                new EditorialRunContextCommand(event.eventIdentity()));
        assertEquals(EditorialRunContextCoordinatorCode.LINEAGE_BOUND, appended.code());
        assertFalse(appended.closedRunIdentity().isEmpty());
        assertFalse(appended.lineageIdentity().isEmpty());
        assertFalse(appended.bindingIdentity().isEmpty());
        long closedAt = scalarLong("SELECT closed_at FROM editorial_closed_run_contexts");
        long lineageAt = scalarLong("SELECT created_at FROM editorial_lineage_records");
        long bindingAt = scalarLong("SELECT bound_at FROM editorial_run_lineage_bindings");

        EditorialRunContextCoordinatorResult replay = coordinator(event, 999L).coordinate(
                new EditorialRunContextCommand(event.eventIdentity()));
        assertEquals(EditorialRunContextCoordinatorCode.ALREADY_BOUND, replay.code());
        assertEquals(closedAt, scalarLong("SELECT closed_at FROM editorial_closed_run_contexts"));
        assertEquals(lineageAt, scalarLong("SELECT created_at FROM editorial_lineage_records"));
        assertEquals(bindingAt, scalarLong("SELECT bound_at FROM editorial_run_lineage_bindings"));
        assertEquals(1, count("editorial_closed_run_contexts"));
        assertEquals(1, count("editorial_lineage_records"));
        assertEquals(1, count("editorial_run_lineage_bindings"));

        EditorialRunContextStatus status = coordinator(event, 1000L).inspect(
                new EditorialRunContextCommand(event.eventIdentity()));
        assertEquals(EditorialRunContextStatusCode.LINEAGE_BOUND, status.code());
        assertEquals(appended.bindingIdentity(), status.bindingIdentity());
    }

    @Test public void invalidChildLeavesClosedUnboundWithoutLineageOrBinding() {
        Fixture fixture = fixture("orphan-phase");
        EditorialRunClosureEvent event = event(fixture, EditorialLineageNodeKind.CHILD,
                "9".repeat(64), "orphan-phase");
        EditorialRunContextCoordinator coordinator = coordinator(event, 200L);

        EditorialRunContextCoordinatorResult result = coordinator.coordinate(
                new EditorialRunContextCommand(event.eventIdentity()));
        assertEquals(EditorialRunContextCoordinatorCode.LINEAGE_VALIDATION_REJECTED, result.code());
        assertEquals(com.ml.tblandroidtxt.editorial.pack.EditorialLineageCreationCode.LINEAGE_CONTEXT_REQUIRED,
                result.lineageCreationCode());
        assertEquals(1, count("editorial_closed_run_contexts"));
        assertEquals(0, count("editorial_lineage_records"));
        assertEquals(0, count("editorial_run_lineage_bindings"));
        assertEquals(EditorialRunContextStatusCode.CLOSED_UNBOUND,
                coordinator.inspect(new EditorialRunContextCommand(event.eventIdentity())).code());
    }

    @Test public void childUsesOnlyExactParentAndConflictingBindingFailsClosed() {
        Fixture parentFixture = fixture("parent-phase");
        EditorialClosedRunContext parentClosed = parentFixture.creator.closeRunContext(
                new EditorialClosedRunContextClosureRequest(parentFixture.revision.revisionIdentity(),
                        parentFixture.scope.scopeSnapshotIdentity(), "test-evaluation", "editorial-run",
                        "parent-phase", null, 300L)).value();
        EditorialLineageRecord parent = parentRecord(parentFixture.revision, parentFixture.scope, parentClosed);
        assertEquals(EditorialLineagePersistenceCode.APPENDED,
                new EditorialLineageDao(repository).append(parent, 301L).code());

        Fixture childFixture = fixture("child-phase");
        EditorialRunClosureEvent childEvent = event(childFixture, EditorialLineageNodeKind.CHILD,
                parent.recordIdentity(), "child-phase");
        EditorialRunContextCoordinatorResult childResult = coordinator(childEvent, 302L).coordinate(
                new EditorialRunContextCommand(childEvent.eventIdentity()));
        assertEquals(EditorialRunContextCoordinatorCode.LINEAGE_BOUND, childResult.code());

        EditorialRunClosureEvent wrongParentEvent = event(childFixture, EditorialLineageNodeKind.CHILD,
                "8".repeat(64), "child-phase");
        EditorialRunContextCoordinatorResult wrongParent = coordinator(wrongParentEvent, 303L).coordinate(
                new EditorialRunContextCommand(wrongParentEvent.eventIdentity()));
        assertEquals(EditorialRunContextCoordinatorCode.BINDING_CONFLICT,
                wrongParent.code());
        assertEquals(2, count("editorial_closed_run_contexts"));
        assertEquals(2, count("editorial_lineage_records"));
        assertEquals(1, count("editorial_run_lineage_bindings"));
    }

    @Test public void eventCollisionCannotChangeRootChildOrParentAndWritesNothing() {
        Fixture fixture = fixture("collision-phase");
        EditorialRunClosureEvent canonical = event(fixture, EditorialLineageNodeKind.ROOT,
                null, "collision-phase");
        EditorialRunClosureEvent tampered = EditorialRunClosureEvent.fromDeclared(
                canonical.eventIdentity(), canonical.eventFingerprint(),
                canonical.projectRevisionSelector(), canonical.inputScopeSelector(),
                canonical.compatibilityEvaluationSelector(), canonical.runKind(), canonical.phaseIdentity(),
                canonical.sourceRunRowIdSelector(), EditorialLineageNodeKind.CHILD, "7".repeat(64),
                canonical.frozenManifestFingerprint(), canonical.frozenManifestReference(),
                canonical.eligibility());
        EditorialRunContextCoordinator coordinator = coordinator(tampered, 400L);
        EditorialRunContextCoordinatorResult result = coordinator.coordinate(
                new EditorialRunContextCommand(canonical.eventIdentity()));
        assertEquals(EditorialRunContextCoordinatorCode.CLOSURE_EVENT_COLLISION, result.code());
        assertEquals(0, count("editorial_closed_run_contexts"));
        assertEquals(0, count("editorial_lineage_records"));
        assertEquals(0, count("editorial_run_lineage_bindings"));
    }

    @Test public void commandAndEventContractRejectInvalidRootParent() {
        Fixture fixture = fixture("invalid-root-phase");
        try {
            event(fixture, EditorialLineageNodeKind.ROOT, "6".repeat(64), "invalid-root-phase");
            throw new AssertionError("root parent must be rejected");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
        EditorialRunContextCoordinatorResult invalid = coordinator(null, 500L).coordinate(
                new EditorialRunContextCommand(" "));
        assertEquals(EditorialRunContextCoordinatorCode.COMMAND_INVALID, invalid.code());
        assertEquals(0, count("editorial_closed_run_contexts"));
    }

    @Test public void statusIdentifiesLineageWithoutBindingWithoutWriting() {
        Fixture fixture = fixture("status-phase");
        EditorialRunClosureEvent event = event(fixture, EditorialLineageNodeKind.ROOT, null,
                "status-phase");
        EditorialClosedRunContext closed = fixture.creator.closeRunContext(
                new EditorialClosedRunContextClosureRequest(fixture.revision.revisionIdentity(),
                        fixture.scope.scopeSnapshotIdentity(), "test-evaluation", "editorial-run",
                        "status-phase", null, 600L)).value();
        assertNotNull(closed);
        EditorialRunContextStatus before = coordinator(event, 601L).inspect(
                new EditorialRunContextCommand(event.eventIdentity()));
        assertEquals(EditorialRunContextStatusCode.CLOSED_UNBOUND, before.code());
        assertEquals(1, count("editorial_closed_run_contexts"));
        assertEquals(0, count("editorial_lineage_records"));
    }

    private EditorialRunContextCoordinator coordinator(EditorialRunClosureEvent event, long timestamp) {
        EditorialRunClosureEventResolver resolver = selector -> event == null
                ? EditorialClosureEventResolutionResult.failure(
                        EditorialClosureEventResolutionCode.CLOSURE_EVENT_UNAVAILABLE, "fixture-empty")
                : EditorialClosureEventResolutionResult.resolved(event);
        EditorialAuthoritativeIdentityCreator creator = new EditorialAuthoritativeIdentityCreator(
                repository, (evaluation, contract, schema) -> Optional.of(trustedFacts));
        SqliteEditorialLineageContextResolver contextResolver = new SqliteEditorialLineageContextResolver(
                repository, (evaluation, contract, schema) -> Optional.of(trustedFacts));
        return new EditorialRunContextCoordinator(repository, resolver, creator, contextResolver,
                new EditorialLineageRuntimeService(contextResolver),
                new EditorialLineageAndBindingTransactionService(repository), () -> timestamp);
    }

    private Fixture fixture(String phase) {
        EditorialAuthoritativeIdentityCreator creator = new EditorialAuthoritativeIdentityCreator(
                repository, (evaluation, contract, schema) -> Optional.of(trustedFacts));
        EditorialProjectRevision revision = creator.prepareProjectRevision(
                new EditorialProjectRevisionPreparationRequest("project-projection-v1",
                        "semantic/project/coordinator", "project-definition-v1", "novel",
                        PROFILE, MACHINE, null, 10L)).value();
        EditorialInputScopeSnapshot scope = creator.prepareInputScopeSnapshot(
                new EditorialInputScopeSnapshotPreparationRequest(revision.revisionIdentity(), "scope-v1",
                        "chapter/coordinator", roles(), "manifest-v1", entries(), null, 11L)).value();
        return new Fixture(creator, revision, scope, phase);
    }

    private EditorialRunClosureEvent event(Fixture fixture,
                                            EditorialLineageNodeKind nodeKind,
                                            String parent,
                                            String phase) {
        return EditorialRunClosureEvent.create(fixture.revision.revisionIdentity(),
                fixture.scope.scopeSnapshotIdentity(), "test-evaluation", "editorial-run", phase, null,
                nodeKind, parent, fixture.scope.manifestFingerprint(), "test-only-frozen-manifest-attestation",
                EditorialClosureEventEligibility.ELIGIBLE);
    }

    private EditorialLineageRecord parentRecord(EditorialProjectRevision revision,
                                                com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshot scope,
                                                EditorialClosedRunContext closed) {
        EditorialLineageInputManifest manifest = new EditorialLineageInputManifest("manifest-v1", List.of(
                new EditorialLineageInputEntry("RAW", 0, RAW, 10, 1),
                new EditorialLineageInputEntry("DRAFT", 0, DRAFT, 20, 2)));
        EditorialLineageIdentity identity = new EditorialLineageIdentity(PACK, "trusted-test", "1", PROFILE,
                MACHINE, "contract.v1", "schema.v1", revision.revisionIdentity(),
                scope.scopeSnapshotIdentity(), closed.closedRunIdentity(), manifest,
                EditorialLineageNodeKind.ROOT);
        return EditorialLineageRecord.create(identity, null);
    }

    private EditorialRequiredInputRoleContract roles() {
        return new EditorialRequiredInputRoleContract("roles-v1", new LinkedHashSet<>(List.of("RAW", "DRAFT")));
    }

    private List<EditorialInputScopeSnapshotEntry> entries() {
        return List.of(new EditorialInputScopeSnapshotEntry("DRAFT", 0L, DRAFT, 20L, 2L),
                new EditorialInputScopeSnapshotEntry("RAW", 0L, RAW, 10L, 1L));
    }

    private EditorialClosedRunContractFacts facts() {
        return new EditorialClosedRunContractFacts("test-pack", "contract.v1", "test-schema", "schema.v1",
                "evaluator.v1", ADAPTERS, CAPABILITIES, CONTEXT, "engine-test");
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
        evaluation.put("evaluator_contract_version", "evaluator.v1"); evaluation.put("adapter_set_fingerprint", ADAPTERS);
        evaluation.put("capability_fingerprint", CAPABILITIES); evaluation.put("context_fingerprint", CONTEXT);
        evaluation.put("compatibility_outcome", "DATA_COMPATIBLE"); evaluation.put("reason_code", "TEST_ONLY_TRUSTED");
        evaluation.put("blocker_details", ""); evaluation.put("evaluated_at", 2L);
        db.insertOrThrow("editorial_pack_compatibility_evaluations", null, evaluation);
    }

    private int count(String table) {
        try (Cursor cursor = repository.editorialReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + table, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    private long scalarLong(String sql) {
        try (Cursor cursor = repository.editorialReadableDatabase().rawQuery(sql, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getLong(0);
        }
    }

    private record Fixture(EditorialAuthoritativeIdentityCreator creator,
                           EditorialProjectRevision revision,
                           com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshot scope,
                           String phase) { }
}
