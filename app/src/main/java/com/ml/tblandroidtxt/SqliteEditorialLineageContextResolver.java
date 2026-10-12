package com.ml.tblandroidtxt;

import android.database.Cursor;

import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContext;
import com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshot;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageAuthoritativeContext;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageCompatibilityEvidence;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageCompatibilityStatus;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageContextResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageInputEntry;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageInputManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageRecord;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageRunContextState;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageTrustContext;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageCallerSelection;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialProjectRevision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** SQLite-backed read-only C1 resolver. It never chooses a latest row or trusts caller provenance. */
public final class SqliteEditorialLineageContextResolver implements EditorialLineageContextResolver {
    private final TranslationRepository database;
    private final EditorialProjectRevisionDao projects;
    private final EditorialInputScopeSnapshotDao scopes;
    private final EditorialClosedRunContextDao closedRuns;
    private final EditorialPackCompatibilityEvaluationDao evaluations;
    private final EditorialLineageDao lineages;
    private final EditorialTrustedClosedRunFactsResolver trustedFacts;

    public SqliteEditorialLineageContextResolver(TranslationRepository database) {
        this(database, new BundledEditorialTrustedClosedRunFactsResolver());
    }

    SqliteEditorialLineageContextResolver(TranslationRepository database,
                                          EditorialTrustedClosedRunFactsResolver trustedFacts) {
        this.database = Objects.requireNonNull(database, "database");
        this.trustedFacts = Objects.requireNonNull(trustedFacts, "trustedFacts");
        this.projects = new EditorialProjectRevisionDao(database);
        this.scopes = new EditorialInputScopeSnapshotDao(database);
        this.closedRuns = new EditorialClosedRunContextDao(database);
        this.evaluations = new EditorialPackCompatibilityEvaluationDao(database);
        this.lineages = new EditorialLineageDao(database);
    }

    @Override public EditorialLineageAuthoritativeContext resolve(EditorialLineageCallerSelection selection) {
        return resolveDetailed(selection).context().orElse(null);
    }

    public EditorialAuthoritativeContextResolutionResult resolveDetailed(
            EditorialLineageCallerSelection selection) {
        if (selection == null || blank(selection.projectSelector())) {
            return EditorialAuthoritativeContextResolutionResult.failure(
                    EditorialAuthoritativeContextResolutionCode.PROJECT_REVISION_REQUIRED);
        }
        Optional<EditorialProjectRevision> project = projects.findByIdentity(selection.projectSelector());
        if (project.isEmpty()) return fail(EditorialAuthoritativeContextResolutionCode.PROJECT_REVISION_REQUIRED);
        if (blank(selection.inputScopeSelector())) return fail(EditorialAuthoritativeContextResolutionCode.INPUT_SCOPE_REQUIRED);
        Optional<EditorialInputScopeSnapshot> scope = scopes.findByIdentity(selection.inputScopeSelector());
        if (scope.isEmpty() || !project.get().revisionIdentity().equals(scope.get().projectRevisionIdentity())) {
            return fail(EditorialAuthoritativeContextResolutionCode.INPUT_SCOPE_REQUIRED);
        }
        if (blank(selection.runEvaluationSelector())) return fail(EditorialAuthoritativeContextResolutionCode.RUN_CONTEXT_REQUIRED);
        Optional<EditorialClosedRunContext> closed = closedRuns.findByIdentity(selection.runEvaluationSelector());
        if (closed.isEmpty() || !project.get().revisionIdentity().equals(closed.get().projectRevisionIdentity())
                || !scope.get().scopeSnapshotIdentity().equals(closed.get().scopeSnapshotIdentity())
                || !scope.get().manifestFingerprint().equals(closed.get().inputManifestFingerprint())) {
            return fail(EditorialAuthoritativeContextResolutionCode.RUN_CONTEXT_REQUIRED);
        }
        Optional<EditorialPackCompatibilityEvaluation> evaluation = evaluations.findByEvaluationId(
                closed.get().compatibilityEvaluationId());
        Optional<PackFacts> pack = findPack(closed.get().canonicalPackHash());
        if (evaluation.isEmpty() || pack.isEmpty()) return fail(EditorialAuthoritativeContextResolutionCode.IDENTITY_UNATTESTED);
        if (evaluation.get().attestation() != EditorialPackCompatibilityEvaluation.Attestation.TRUSTED_PROFILE
                || evaluation.get().compatibilityOutcome() != EditorialPackCompatibilityClass.DATA_COMPATIBLE) {
            return fail(EditorialAuthoritativeContextResolutionCode.COMPATIBILITY_CONTEXT_MISMATCH);
        }
        Optional<EditorialTrustedClosedRunFacts> expected = trustedFacts.resolve(
                evaluation.get(), pack.get().contractVersion, pack.get().schemaVersion);
        if (expected.isEmpty() || !matches(closed.get(), expected.get())) {
            return fail(EditorialAuthoritativeContextResolutionCode.TRUSTED_PROFILE_CONTEXT_MISMATCH);
        }
        List<EditorialLineageRecord> parents = resolveExactParent(selection.selectedParentRecordIdentity());
        if (!blank(selection.selectedParentRecordIdentity()) && parents.isEmpty()) {
            return fail(EditorialAuthoritativeContextResolutionCode.PARENT_NOT_FOUND);
        }
        if (parents.size() > 1) return fail(EditorialAuthoritativeContextResolutionCode.PARENT_AMBIGUOUS);
        EditorialLineageInputManifest manifest = new EditorialLineageInputManifest(scope.get().manifestVersion(),
                toLineageEntries(scope.get().entries()));
        return EditorialAuthoritativeContextResolutionResult.resolved(new EditorialLineageAuthoritativeContext(
                closed.get().canonicalPackHash(), new EditorialLineageTrustContext(
                        closed.get().trustedProfileId(), closed.get().trustedProfileVersion(),
                        closed.get().canonicalProfileHash(), closed.get().machineFingerprint()),
                closed.get().contractFacts().packContractVersion(), closed.get().contractFacts().packSchemaVersion(),
                new EditorialLineageCompatibilityEvidence(closed.get().compatibilityEvaluationId(),
                        EditorialLineageCompatibilityStatus.COMPATIBLE),
                project.get().revisionIdentity(), scope.get().scopeSnapshotIdentity(), closed.get().closedRunIdentity(),
                EditorialLineageRunContextState.CLOSED, manifest,
                scope.get().requiredRoleContract().requiredRoleSet(),
                new SqliteEditorialLineageValidationContext(lineages), parents));
    }

    private List<EditorialLineageRecord> resolveExactParent(String recordIdentity) {
        if (blank(recordIdentity)) return List.of();
        Optional<EditorialLineageRecord> parent = lineages.findByRecordIdentity(recordIdentity);
        return parent.isPresent() ? Collections.singletonList(parent.get()) : Collections.emptyList();
    }

    private Optional<PackFacts> findPack(String packHash) {
        if (blank(packHash)) return Optional.empty();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT contract_version,schema_version FROM editorial_packs WHERE canonical_pack_hash=?",
                new String[]{packHash})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            PackFacts facts = new PackFacts(cursor.getString(0), cursor.getString(1));
            return cursor.moveToNext() ? Optional.empty() : Optional.of(facts);
        }
    }

    private static boolean matches(EditorialClosedRunContext closed, EditorialTrustedClosedRunFacts expected) {
        return closed.trustedProfileId().equals(expected.trustedProfileId())
                && closed.trustedProfileVersion().equals(expected.trustedProfileVersion())
                && closed.canonicalProfileHash().equals(expected.canonicalProfileHash())
                && closed.machineFingerprint().equals(expected.machineFingerprint())
                && closed.contractFacts().equals(expected.contractFacts());
    }

    private static List<EditorialLineageInputEntry> toLineageEntries(
            List<com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshotEntry> entries) {
        ArrayList<EditorialLineageInputEntry> result = new ArrayList<>();
        for (com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshotEntry entry : entries) {
            if (entry.ordinal() == null || entry.ordinal() > Integer.MAX_VALUE
                    || entry.byteCount() == null || entry.itemCount() == null) {
                throw new IllegalArgumentException("stored input entry is not a lineage-safe manifest fact");
            }
            result.add(new EditorialLineageInputEntry(entry.role(), entry.ordinal().intValue(), entry.inputSha256(),
                    entry.byteCount(), entry.itemCount()));
        }
        return List.copyOf(result);
    }

    private static EditorialAuthoritativeContextResolutionResult fail(
            EditorialAuthoritativeContextResolutionCode code) {
        return EditorialAuthoritativeContextResolutionResult.failure(code);
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private record PackFacts(String contractVersion, String schemaVersion) { }
}
