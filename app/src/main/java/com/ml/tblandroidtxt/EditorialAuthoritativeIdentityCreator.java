package com.ml.tblandroidtxt;

import android.database.Cursor;

import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunCompatibilityOutcome;
import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContext;
import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContextDraft;
import com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshot;
import com.ml.tblandroidtxt.editorial.pack.EditorialProjectRevision;

import java.util.Objects;
import java.util.Optional;

/**
 * Explicit production creator for v17 identity evidence. It is deliberately
 * not composed into importer, startup, UI, project preparation or lineage append paths.
 */
public final class EditorialAuthoritativeIdentityCreator {
    public static final String CLOSED_CONTEXT_VERSION = "editorial-closed-run-context-v1";
    private final TranslationRepository database;
    private final EditorialProjectRevisionDao projectRevisions;
    private final EditorialInputScopeSnapshotDao scopeSnapshots;
    private final EditorialClosedRunContextDao closedRuns;
    private final EditorialPackCompatibilityEvaluationDao evaluations;
    private final EditorialTrustedClosedRunFactsResolver trustedFacts;

    public EditorialAuthoritativeIdentityCreator(TranslationRepository database) {
        this(database, new BundledEditorialTrustedClosedRunFactsResolver());
    }

    EditorialAuthoritativeIdentityCreator(TranslationRepository database,
                                          EditorialTrustedClosedRunFactsResolver trustedFacts) {
        this.database = Objects.requireNonNull(database, "database");
        this.trustedFacts = Objects.requireNonNull(trustedFacts, "trustedFacts");
        this.projectRevisions = new EditorialProjectRevisionDao(database);
        this.scopeSnapshots = new EditorialInputScopeSnapshotDao(database);
        this.closedRuns = new EditorialClosedRunContextDao(database);
        this.evaluations = new EditorialPackCompatibilityEvaluationDao(database);
    }

    public EditorialIdentityAppendResult<EditorialProjectRevision> prepareProjectRevision(
            EditorialProjectRevisionPreparationRequest request) {
        if (request == null) return invalid("project-request-required");
        try {
            EditorialProjectRevision revision = new EditorialProjectRevision(
                    request.canonicalProjectionVersion(), request.projectSemanticKey(),
                    request.projectDefinitionContractVersion(), request.semanticProjectType(),
                    request.scopePolicyFingerprint(), request.workflowPolicyFingerprint());
            return projectRevisions.append(revision, request.sourceProjectRowId(), request.createdAt());
        } catch (RuntimeException invalid) {
            return invalid("project-semantic-facts-invalid");
        }
    }

    public EditorialIdentityAppendResult<EditorialInputScopeSnapshot> prepareInputScopeSnapshot(
            EditorialInputScopeSnapshotPreparationRequest request) {
        if (request == null) return invalid("scope-request-required");
        try {
            EditorialInputScopeSnapshot snapshot = new EditorialInputScopeSnapshot(
                    request.projectRevisionIdentity(), request.scopeCanonicalVersion(), request.scopeKey(),
                    request.requiredRoleContract(), request.manifestVersion(), request.entries());
            return scopeSnapshots.appendWithEntries(snapshot, request.sourceChapterRowId(), request.createdAt());
        } catch (RuntimeException invalid) {
            return EditorialIdentityAppendResult.of(EditorialIdentityPersistenceCode.INPUT_MANIFEST_INCOMPLETE,
                    null, "", "scope-semantic-facts-invalid");
        }
    }

    public EditorialIdentityAppendResult<EditorialClosedRunContext> closeRunContext(
            EditorialClosedRunContextClosureRequest request) {
        if (request == null || request.closedAt() < 0) return invalid("closed-run-request-required");
        Optional<EditorialPackCompatibilityEvaluation> evaluation = evaluations.findByEvaluationId(
                request.compatibilityEvaluationId());
        if (evaluation.isEmpty()) return result(EditorialIdentityPersistenceCode.COMPATIBILITY_CONTEXT_MISMATCH,
                "compatibility-evaluation-not-found");
        Optional<PackFacts> pack = findPack(evaluation.get().canonicalPackHash());
        if (pack.isEmpty()) return result(EditorialIdentityPersistenceCode.COMPATIBILITY_CONTEXT_MISMATCH,
                "pack-not-found-or-ambiguous");
        Optional<EditorialTrustedClosedRunFacts> facts = trustedFacts.resolve(
                evaluation.get(), pack.get().contractVersion, pack.get().schemaVersion);
        if (facts.isEmpty()) return result(EditorialIdentityPersistenceCode.TRUSTED_PROFILE_CONTEXT_MISMATCH,
                "trusted-executable-profile-required");
        try {
            EditorialTrustedClosedRunFacts trust = facts.get();
            EditorialClosedRunContextDraft draft = new EditorialClosedRunContextDraft(
                    CLOSED_CONTEXT_VERSION, request.projectRevisionIdentity(), request.scopeSnapshotIdentity(),
                    evaluation.get().canonicalPackHash(), evaluation.get().evaluationId(),
                    EditorialClosedRunCompatibilityOutcome.DATA_COMPATIBLE, trust.trustedProfileId(),
                    trust.trustedProfileVersion(), trust.canonicalProfileHash(), trust.machineFingerprint(),
                    trust.contractFacts(), request.runKind(), request.phaseIdentity(),
                    requiredManifestFingerprint(request.scopeSnapshotIdentity()));
            return closedRuns.append(draft, request.sourceRunRowId(), request.closedAt());
        } catch (MissingScopeException missing) {
            return result(EditorialIdentityPersistenceCode.INPUT_SCOPE_REQUIRED, "scope-snapshot-not-found");
        } catch (RuntimeException invalid) {
            return result(EditorialIdentityPersistenceCode.RUN_CONTEXT_NOT_CLOSED,
                    "closed-run-semantic-facts-invalid");
        }
    }

    private String requiredManifestFingerprint(String identity) {
        return scopeSnapshots.findByIdentity(identity)
                .orElseThrow(MissingScopeException::new).manifestFingerprint();
    }

    private Optional<PackFacts> findPack(String canonicalPackHash) {
        if (canonicalPackHash == null || canonicalPackHash.isBlank()) return Optional.empty();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT contract_version,schema_version FROM editorial_packs WHERE canonical_pack_hash=?",
                new String[]{canonicalPackHash})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            PackFacts facts = new PackFacts(cursor.getString(0), cursor.getString(1));
            if (cursor.moveToNext()) return Optional.empty();
            return Optional.of(facts);
        }
    }

    private static <T> EditorialIdentityAppendResult<T> invalid(String detail) {
        return EditorialIdentityAppendResult.of(EditorialIdentityPersistenceCode.INVALID_IMMUTABLE_RECORD,
                null, "", detail);
    }

    private static <T> EditorialIdentityAppendResult<T> result(EditorialIdentityPersistenceCode code,
                                                                 String detail) {
        return EditorialIdentityAppendResult.of(code, null, "", detail);
    }

    private record PackFacts(String contractVersion, String schemaVersion) { }
    private static final class MissingScopeException extends RuntimeException { }
}
