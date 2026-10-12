package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.ml.tblandroidtxt.editorial.pack.EditorialAuthoritativeRunDeclaration;
import com.ml.tblandroidtxt.editorial.pack.EditorialAuthoritativeRunDeclarationDraft;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshot;
import com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshotEntry;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4SourceIdentity;
import com.ml.tblandroidtxt.editorial.pack.EditorialProjectRevision;
import com.ml.tblandroidtxt.editorial.pack.EditorialRequiredInputRoleContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Atomic P4 setup owner. It creates only immutable metadata and has no model,
 * provider, certification or execution path.
 */
public final class EditorialP4BindingTransactionService {
    private final TranslationRepository database;
    private final EditorialPackSelectionPolicy selectionPolicy;
    private final EditorialProjectRevisionDao projectRevisions;
    private final EditorialInputScopeSnapshotDao scopeSnapshots;
    private final EditorialAuthoritativeRunDeclarationDao declarations;
    private final EditorialP4BindingDao bindings;

    public EditorialP4BindingTransactionService(TranslationRepository database,
                                                EditorialPackStorageLayout storage) {
        this(database, new EditorialPackSelectionPolicy(database, storage));
    }

    EditorialP4BindingTransactionService(TranslationRepository database,
                                          EditorialPackSelectionPolicy selectionPolicy) {
        this.database = Objects.requireNonNull(database, "database");
        this.selectionPolicy = Objects.requireNonNull(selectionPolicy, "selectionPolicy");
        this.projectRevisions = new EditorialProjectRevisionDao(database);
        this.scopeSnapshots = new EditorialInputScopeSnapshotDao(database);
        this.declarations = new EditorialAuthoritativeRunDeclarationDao(database);
        this.bindings = new EditorialP4BindingDao(database);
    }

    /**
     * Creates a project row, its immutable identity rows, and one initial run
     * declaration as one SQLite transaction. The pack identity is resolved
     * from the registry by the exact user-selected pack id/version.
     */
    public EditorialP4BindingResult createSetup(EditorialP4SetupRequest request) {
        if (request == null || request.createdAt() < 0 || blank(request.attemptRequestSelector())
                || blank(request.seriesName()) || blank(request.volumeName())
                || blank(request.packId()) || blank(request.packVersion())
                || blank(request.scopeKey()) || blank(request.runKind())
                || blank(request.phaseIdentity()) || blank(request.frozenManifestReference())) {
            return EditorialP4BindingResult.of(EditorialP4BindingResult.Code.INVALID_INPUT,
                    -1L, null, "setup-request-required");
        }
        final EditorialPackSelectionCandidate candidate = selectionPolicy
                .resolve(request.packId(), request.packVersion()).orElse(null);
        if (candidate == null) return EditorialP4BindingResult.of(
                EditorialP4BindingResult.Code.PACK_NOT_SELECTABLE, -1L, null,
                "pack-is-not-stored-valid-trusted-data-compatible");
        final List<EditorialP4SourceIdentity> sourceIdentities;
        try {
            sourceIdentities = appComputedIdentities(request.sources());
            validateSourceDecisions(request, sourceIdentities);
        } catch (RuntimeException invalid) {
            return EditorialP4BindingResult.of(EditorialP4BindingResult.Code.INVALID_INPUT,
                    -1L, null, safeMessage(invalid, "source-facts-invalid"));
        }

        Optional<EditorialP4Binding> existing = bindings.findByAttemptRequestSelector(
                request.attemptRequestSelector());
        if (existing.isPresent()) {
            if (sameRequest(existing.get(), request, candidate, sourceIdentities)) {
                return EditorialP4BindingResult.of(EditorialP4BindingResult.Code.ALREADY_EXISTS,
                        projectIdFor(existing.get()), existing.get(), "same-explicit-selector");
            }
            return EditorialP4BindingResult.of(EditorialP4BindingResult.Code.REQUEST_COLLISION,
                    projectIdFor(existing.get()), null, "same-selector-different-binding-facts");
        }

        EditorialProjectRevision revision;
        EditorialInputScopeSnapshot snapshot;
        try {
            revision = buildProjectRevision(request);
            snapshot = buildScopeSnapshot(revision, request, sourceIdentities);
        } catch (RuntimeException invalid) {
            return EditorialP4BindingResult.of(EditorialP4BindingResult.Code.INVALID_INPUT,
                    -1L, null, safeMessage(invalid, "identity-facts-invalid"));
        }

        SQLiteDatabase db = database.editorialWritableDatabase();
        long projectId = -1L;
        try {
            db.beginTransaction();
            projectId = insertProjectRow(db, request);
            EditorialIdentityAppendResult<EditorialProjectRevision> revisionResult =
                    projectRevisions.appendInTransaction(db, revision, projectId, request.createdAt());
            if (!accepted(revisionResult)) return failure(projectId, "project-revision-" + revisionResult.detail());
            EditorialIdentityAppendResult<EditorialInputScopeSnapshot> snapshotResult =
                    scopeSnapshots.appendWithEntriesInTransaction(db, snapshot, null, request.createdAt());
            if (!accepted(snapshotResult)) return failure(projectId, "input-scope-" + snapshotResult.detail());

            EditorialLineageNodeKind nodeKind = request.nodeKind() == null
                    ? EditorialLineageNodeKind.ROOT : request.nodeKind();
            EditorialAuthoritativeRunDeclarationDraft draft = new EditorialAuthoritativeRunDeclarationDraft(
                    request.attemptRequestSelector(), revision.revisionIdentity(),
                    snapshot.scopeSnapshotIdentity(), candidate.evaluation().evaluationId(), request.runKind(),
                    request.phaseIdentity(), snapshot.manifestFingerprint(), request.frozenManifestReference(),
                    nodeKind, request.parentRecordIdentity());
            EditorialIdentityAppendResult<EditorialAuthoritativeRunDeclaration> declarationResult =
                    declarations.appendNewAuthorizedAttemptInTransaction(db, draft);
            if (!accepted(declarationResult)) return failure(projectId,
                    "run-declaration-" + declarationResult.detail());
            EditorialAuthoritativeRunDeclaration declaration = declarationResult.value();
            EditorialP4Binding binding = new EditorialP4Binding(
                    revision.revisionIdentity(), snapshot.scopeSnapshotIdentity(),
                    declaration.declarationIdentity(), candidate.packId(), candidate.packVersion(),
                    candidate.canonicalPackHash(), EditorialPackSelectionPolicy.manifestFingerprint(candidate.manifest()),
                    candidate.trustedProfileId(), candidate.trustedProfileVersion(), candidate.canonicalProfileHash(),
                    candidate.machineContractFingerprint(), candidate.evaluation().evaluationId(),
                    candidate.evaluation().compatibilityOutcome().name(),
                    candidate.evaluation().contextFingerprint().orElseThrow(), candidate.manifest().contractVersion(),
                    candidate.manifest().schemaVersion(),
                    EditorialPackSelectionPolicy.phaseGraphFingerprint(candidate.manifest()),
                    EditorialPackSelectionPolicy.contextAllowListFingerprint(candidate.manifest()),
                    request.sourceMode(), request.glossaryStatus(), request.pronounStatus(),
                    request.pairContextStatus(), request.explicitUserDecisionProvenance(),
                    snapshot.manifestFingerprint(), declaration.runAttemptOrdinal(), request.runKind(),
                    request.phaseIdentity(), sourceIdentities);
            EditorialP4BindingResult bindingResult = bindings.appendInTransaction(db,
                    request.attemptRequestSelector(), projectId, binding, request.createdAt());
            if (!bindingResult.accepted()) return bindingResult;
            db.setTransactionSuccessful();
            return EditorialP4BindingResult.of(bindingResult.code(), projectId,
                    bindingResult.binding(), bindingResult.detail());
        } catch (RuntimeException error) {
            return EditorialP4BindingResult.of(EditorialP4BindingResult.Code.PERSISTENCE_FAILURE,
                    projectId, null, safeMessage(error, "p4-setup-transaction-rolled-back"));
        } finally {
            db.endTransaction();
        }
    }

    /** Exact binding lookup after restart; there is no latest-pack query. */
    public EditorialP4ResumeResult resumeBySelector(String selector,
                                                     List<EditorialP4InputSource> currentSources) {
        Optional<EditorialP4Binding> found = bindings.findByAttemptRequestSelector(selector);
        if (found.isEmpty()) return EditorialP4ResumeResult.of(
                EditorialP4ResumeResult.Code.NOT_FOUND, null, "binding-selector-not-found");
        return verifyResume(found.get(), currentSources);
    }

    /** Resume requires an explicit binding identity; multiple project candidates are never collapsed. */
    public EditorialP4ResumeResult resumeProject(long projectId, String selector,
                                                  List<EditorialP4InputSource> currentSources) {
        if (projectId <= 0) return EditorialP4ResumeResult.of(
                EditorialP4ResumeResult.Code.NOT_FOUND, null, "project-not-found");
        EditorialP4Binding binding;
        if (selector != null && !selector.isBlank()) {
            binding = bindings.findByAttemptRequestSelector(selector).orElse(null);
            if (binding == null || projectIdFor(binding) != projectId) return EditorialP4ResumeResult.of(
                    EditorialP4ResumeResult.Code.NOT_FOUND, null, "exact-project-binding-not-found");
        } else {
            List<EditorialP4Binding> candidates = bindings.listByProject(projectId);
            if (candidates.size() != 1) return EditorialP4ResumeResult.of(
                    candidates.isEmpty() ? EditorialP4ResumeResult.Code.NOT_FOUND
                            : EditorialP4ResumeResult.Code.AMBIGUOUS,
                    null, candidates.isEmpty() ? "project-binding-not-found" : "multiple-resume-candidates");
            binding = candidates.get(0);
        }
        return verifyResume(binding, currentSources);
    }

    public List<EditorialP4Binding> listProjectBindings(long projectId) {
        return bindings.listByProject(projectId);
    }

    private EditorialP4ResumeResult verifyResume(EditorialP4Binding binding,
                                                   List<EditorialP4InputSource> currentSources) {
        try {
            List<EditorialP4SourceIdentity> current = appComputedIdentities(currentSources);
            if (!current.equals(binding.inputs())) return stale(binding, "source-identity-drift");
            EditorialPackSelectionCandidate candidate = selectionPolicy.resolve(
                    binding.packId(), binding.packVersion()).orElse(null);
            if (candidate == null || !candidate.canonicalPackHash().equals(binding.canonicalPackHash())
                    || !EditorialPackSelectionPolicy.manifestFingerprint(candidate.manifest())
                    .equals(binding.manifestFingerprint())
                    || !candidate.trustedProfileId().equals(binding.trustedProfileId())
                    || !candidate.trustedProfileVersion().equals(binding.trustedProfileVersion())
                    || !candidate.canonicalProfileHash().equals(binding.canonicalProfileHash())
                    || !candidate.machineContractFingerprint().equals(binding.machineContractFingerprint())
                    || !candidate.evaluation().evaluationId().equals(binding.compatibilityEvaluationId())
                    || !candidate.evaluation().contextFingerprint().orElse("")
                    .equals(binding.evaluationContextFingerprint())
                    || !candidate.manifest().contractVersion().equals(binding.contractVersion())
                    || !candidate.manifest().schemaVersion().equals(binding.schemaVersion())
                    || !EditorialPackSelectionPolicy.phaseGraphFingerprint(candidate.manifest())
                    .equals(binding.phaseGraphFingerprint())
                    || !EditorialPackSelectionPolicy.contextAllowListFingerprint(candidate.manifest())
                    .equals(binding.contextAllowListFingerprint())
                    || candidate.evaluation().compatibilityOutcome() != EditorialPackCompatibilityClass.DATA_COMPATIBLE) {
                return stale(binding, "pack-profile-or-evaluation-drift");
            }
            EditorialProjectRevision revision = projectRevisions.findByIdentity(
                    binding.projectRevisionIdentity()).orElse(null);
            EditorialInputScopeSnapshot snapshot = scopeSnapshots.findByIdentity(
                    binding.inputScopeSnapshotIdentity()).orElse(null);
            if (revision == null || snapshot == null
                    || !snapshot.projectRevisionIdentity().equals(binding.projectRevisionIdentity())
                    || !snapshot.manifestFingerprint().equals(binding.inputManifestFingerprint())) {
                return stale(binding, "identity-row-missing");
            }
            EditorialAuthoritativeRunDeclaration declaration = declarations.findByDeclarationIdentity(
                    binding.runDeclarationIdentity()).orElse(null);
            if (declaration == null || declaration.runAttemptOrdinal() != binding.runAttemptOrdinal()
                    || !declaration.projectRevisionIdentity().equals(revision.revisionIdentity())
                    || !declaration.inputScopeSnapshotIdentity().equals(snapshot.scopeSnapshotIdentity())
                    || !declaration.compatibilityEvaluationId().equals(binding.compatibilityEvaluationId())
                    || !declaration.phaseIdentity().equals(binding.phaseIdentity())
                    || !declaration.frozenManifestFingerprint().equals(binding.inputManifestFingerprint())) {
                return stale(binding, "run-declaration-chain-drift");
            }
            return EditorialP4ResumeResult.of(EditorialP4ResumeResult.Code.RESTORED,
                    binding, "exact-binding-restored;execution-disabled");
        } catch (RuntimeException error) {
            return stale(binding, safeMessage(error, "binding-readback-invalid"));
        }
    }

    private EditorialP4BindingResult failure(long projectId, String detail) {
        return EditorialP4BindingResult.of(EditorialP4BindingResult.Code.PERSISTENCE_FAILURE,
                projectId, null, detail);
    }

    private long insertProjectRow(SQLiteDatabase db, EditorialP4SetupRequest request) {
        ContentValues row = new ContentValues();
        row.put("series_name", request.seriesName().trim());
        row.put("volume_name", request.volumeName().trim());
        // Legacy columns remain a legacy marker; the P4 binding table is authoritative.
        row.put("workflow_version", EditorialSafe4Pack.VERSION);
        row.put("workflow_hash", EditorialSafe4Pack.PACK_HASH);
        row.put("output_tree_uri", "");
        row.put("created_at", request.createdAt());
        row.put("updated_at", request.createdAt());
        return db.insertOrThrow("editorial_projects", null, row);
    }

    private EditorialProjectRevision buildProjectRevision(EditorialP4SetupRequest request) {
        String semanticKey = blank(request.projectSemanticKey())
                ? request.seriesName().trim() + "/" + request.volumeName().trim()
                : request.projectSemanticKey();
        return new EditorialProjectRevision("p4-project-revision-v1", semanticKey,
                "editorial-project-definition-p4-v1", "editorial-project",
                HashUtil.sha256("p4-scope-policy-v1"), HashUtil.sha256("p4-workflow-policy-v1"));
    }

    private EditorialInputScopeSnapshot buildScopeSnapshot(
            EditorialProjectRevision revision, EditorialP4SetupRequest request,
            List<EditorialP4SourceIdentity> sources) {
        LinkedHashSet<String> required = new LinkedHashSet<>();
        for (EditorialP4SourceIdentity source : sources) {
            if (!"PAIR_CONTEXT".equals(source.role())) required.add(source.role());
        }
        EditorialRequiredInputRoleContract contract = new EditorialRequiredInputRoleContract(
                "p4-input-roles-v1", required);
        ArrayList<EditorialInputScopeSnapshotEntry> entries = new ArrayList<>();
        for (EditorialP4SourceIdentity source : sources) entries.add(new EditorialInputScopeSnapshotEntry(
                source.role(), source.ordinal(), source.sha256(), source.byteLength(), 0L));
        return new EditorialInputScopeSnapshot(revision.revisionIdentity(), "p4-scope-v1",
                request.scopeKey(), contract, "p4-input-manifest-v1", entries);
    }

    private List<EditorialP4SourceIdentity> appComputedIdentities(List<EditorialP4InputSource> sources) {
        if (sources == null || sources.isEmpty()) throw new IllegalArgumentException("source list is required");
        ArrayList<EditorialP4SourceIdentity> result = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (EditorialP4InputSource source : sources) {
            if (source == null) throw new IllegalArgumentException("source is null");
            String key = source.role() + "\u0000" + source.ordinal();
            if (!keys.add(key)) throw new IllegalArgumentException("duplicate source role/ordinal");
            byte[] bytes = source.bytes();
            result.add(new EditorialP4SourceIdentity(source.role(), source.sourceReference(), bytes.length,
                    HashUtil.sha256(bytes), source.encoding(), source.schemaStatus(), source.ordinal()));
        }
        result.sort(Comparator.comparing(EditorialP4SourceIdentity::role)
                .thenComparingLong(EditorialP4SourceIdentity::ordinal));
        return List.copyOf(result);
    }

    private void validateSourceDecisions(EditorialP4SetupRequest request,
                                         List<EditorialP4SourceIdentity> sources) {
        if (!EditorialSafe4Contract.NORMAL_MODE.equals(request.sourceMode())
                && !EditorialSafe4Contract.ALTERNATE_MODE.equals(request.sourceMode())) {
            throw new IllegalArgumentException("source mode must be explicit NORMAL_FOUR_SOURCE or ALTERNATE_EXPLICIT");
        }
        if (blank(request.explicitUserDecisionProvenance())) {
            throw new IllegalArgumentException("explicit user decision provenance is required");
        }
        if (request.sourceMode().equals(EditorialSafe4Contract.ALTERNATE_MODE)
                && !request.explicitUserDecisionProvenance().toUpperCase().contains("USER")) {
            throw new IllegalArgumentException("alternate mode requires user decision provenance");
        }
        requireExactlyAtLeast(sources, "RAW");
        requireExactlyAtLeast(sources, "DRAFT");
        requireExactlyAtLeast(sources, "GLOSSARY");
        if (!"AVAILABLE".equals(request.glossaryStatus()) || !hasRole(sources, "GLOSSARY")) {
            throw new IllegalArgumentException("glossary must be explicitly AVAILABLE");
        }
        long pronouns = countRole(sources, "PRONOUN");
        if ("AVAILABLE".equals(request.pronounStatus()) && pronouns != 1) {
            throw new IllegalArgumentException("AVAILABLE pronoun status requires exactly one source");
        }
        if (("NONE".equals(request.pronounStatus()) || "LEGACY_REJECTED".equals(request.pronounStatus()))
                && pronouns != 0) throw new IllegalArgumentException("non-available pronoun status forbids a source");
        if (!Set.of("AVAILABLE", "NONE", "LEGACY_REJECTED").contains(request.pronounStatus())) {
            throw new IllegalArgumentException("pronoun status is not explicit");
        }
        if (!Set.of("AVAILABLE", "NONE").contains(request.pairContextStatus())) {
            throw new IllegalArgumentException("pair context status is invalid");
        }
        if ("AVAILABLE".equals(request.pairContextStatus()) && !hasRole(sources, "PAIR_CONTEXT")) {
            throw new IllegalArgumentException("available pair context requires its source");
        }
        if ("NONE".equals(request.pairContextStatus()) && hasRole(sources, "PAIR_CONTEXT")) {
            throw new IllegalArgumentException("pair context source requires AVAILABLE status");
        }
    }

    private boolean sameRequest(EditorialP4Binding existing, EditorialP4SetupRequest request,
                                EditorialPackSelectionCandidate candidate,
                                List<EditorialP4SourceIdentity> sources) {
        try {
            EditorialProjectRevision revision = buildProjectRevision(request);
            EditorialInputScopeSnapshot snapshot = buildScopeSnapshot(revision, request, sources);
            EditorialAuthoritativeRunDeclaration declaration = declarations.findByDeclarationIdentity(
                    existing.runDeclarationIdentity()).orElse(null);
            return existing.packId().equals(candidate.packId())
                    && existing.packVersion().equals(candidate.packVersion())
                    && existing.canonicalPackHash().equals(candidate.canonicalPackHash())
                    && existing.manifestFingerprint().equals(
                    EditorialPackSelectionPolicy.manifestFingerprint(candidate.manifest()))
                    && existing.trustedProfileId().equals(candidate.trustedProfileId())
                    && existing.trustedProfileVersion().equals(candidate.trustedProfileVersion())
                    && existing.canonicalProfileHash().equals(candidate.canonicalProfileHash())
                    && existing.machineContractFingerprint().equals(candidate.machineContractFingerprint())
                    && existing.compatibilityEvaluationId().equals(candidate.evaluation().evaluationId())
                    && existing.evaluationContextFingerprint().equals(
                    candidate.evaluation().contextFingerprint().orElse(""))
                    && existing.contractVersion().equals(candidate.manifest().contractVersion())
                    && existing.schemaVersion().equals(candidate.manifest().schemaVersion())
                    && existing.phaseGraphFingerprint().equals(
                    EditorialPackSelectionPolicy.phaseGraphFingerprint(candidate.manifest()))
                    && existing.contextAllowListFingerprint().equals(
                    EditorialPackSelectionPolicy.contextAllowListFingerprint(candidate.manifest()))
                    && existing.projectRevisionIdentity().equals(revision.revisionIdentity())
                    && existing.inputScopeSnapshotIdentity().equals(snapshot.scopeSnapshotIdentity())
                    && existing.inputManifestFingerprint().equals(snapshot.manifestFingerprint())
                    && existing.sourceMode().equals(request.sourceMode())
                    && existing.glossaryStatus().equals(request.glossaryStatus())
                    && existing.pronounStatus().equals(request.pronounStatus())
                    && existing.pairContextStatus().equals(request.pairContextStatus())
                    && existing.explicitUserDecisionProvenance().equals(request.explicitUserDecisionProvenance())
                    && existing.runKind().equals(request.runKind())
                    && existing.phaseIdentity().equals(request.phaseIdentity())
                    && existing.inputs().equals(sources)
                    && declarationMatches(declaration, request, revision, snapshot);
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    private boolean declarationMatches(EditorialAuthoritativeRunDeclaration declaration,
                                       EditorialP4SetupRequest request,
                                       EditorialProjectRevision revision,
                                       EditorialInputScopeSnapshot snapshot) {
        if (declaration == null
                || !declaration.attemptRequestSelector().equals(request.attemptRequestSelector())
                || !declaration.projectRevisionIdentity().equals(revision.revisionIdentity())
                || !declaration.inputScopeSnapshotIdentity().equals(snapshot.scopeSnapshotIdentity())
                || !declaration.runKind().equals(request.runKind())
                || !declaration.phaseIdentity().equals(request.phaseIdentity())
                || !declaration.frozenManifestFingerprint().equals(snapshot.manifestFingerprint())
                || !declaration.frozenManifestReference().equals(request.frozenManifestReference())
                || declaration.nodeKind() != (request.nodeKind() == null
                ? EditorialLineageNodeKind.ROOT : request.nodeKind())) return false;
        if (request.parentRecordIdentity() == null) return declaration.parentRecordIdentity() == null;
        return HashUtil.sha256(request.parentRecordIdentity()).equals(declaration.parentRecordIdentity());
    }

    private boolean projectMatches(EditorialP4Binding binding, EditorialP4SetupRequest request) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT series_name,volume_name FROM editorial_projects WHERE id=?",
                new String[]{String.valueOf(projectIdFor(binding))})) {
            return cursor.moveToFirst() && request.seriesName().trim().equals(cursor.getString(0))
                    && request.volumeName().trim().equals(cursor.getString(1));
        }
    }

    private long projectIdFor(EditorialP4Binding binding) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT project_row_id FROM editorial_p4_bindings WHERE binding_identity=?",
                new String[]{binding.bindingIdentity()})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        }
    }

    private EditorialP4ResumeResult stale(EditorialP4Binding binding, String detail) {
        return EditorialP4ResumeResult.of(EditorialP4ResumeResult.Code.STALE_CHAIN, binding,
                "stale-chain;exact-bytes-or-new-project-required:" + detail);
    }

    private static boolean accepted(EditorialIdentityAppendResult<?> result) {
        return result != null && (result.code() == EditorialIdentityPersistenceCode.APPENDED
                || result.code() == EditorialIdentityPersistenceCode.ALREADY_EXISTS);
    }

    private static boolean hasRole(List<EditorialP4SourceIdentity> sources, String role) {
        return countRole(sources, role) > 0;
    }

    private static long countRole(List<EditorialP4SourceIdentity> sources, String role) {
        return sources.stream().filter(source -> role.equals(source.role())).count();
    }

    private static void requireExactlyAtLeast(List<EditorialP4SourceIdentity> sources, String role) {
        if (!hasRole(sources, role)) throw new IllegalArgumentException(role + " source is required");
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private static String safeMessage(Throwable error, String fallback) {
        return error.getMessage() == null || error.getMessage().isBlank() ? fallback : error.getMessage();
    }
}
