package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable facts returned by an authoritative context resolver.
 *
 * <p>The resolver is the provenance boundary: pack/profile/compatibility
 * fields come from trusted storage and registry facts, project/scope/run fields
 * come from the future immutable identity records, the manifest comes from a
 * closed snapshot, and parent candidates come from the read-only lineage
 * context. This value never writes or opens persistence.</p>
 */
public final class EditorialLineageAuthoritativeContext {
    private final String canonicalPackHash;
    private final EditorialLineageTrustContext trustContext;
    private final String contractVersion;
    private final String schemaVersion;
    private final EditorialLineageCompatibilityEvidence compatibilityEvidence;
    private final String projectIdentity;
    private final String inputScopeIdentity;
    private final String runEvaluationIdentity;
    private final EditorialLineageRunContextState runContextState;
    private final EditorialLineageInputManifest inputManifest;
    private final Set<String> requiredInputRoles;
    private final EditorialLineageValidationContext validationContext;
    private final List<EditorialLineageRecord> parentCandidates;

    public EditorialLineageAuthoritativeContext(
            String canonicalPackHash,
            EditorialLineageTrustContext trustContext,
            String contractVersion,
            String schemaVersion,
            EditorialLineageCompatibilityEvidence compatibilityEvidence,
            String projectIdentity,
            String inputScopeIdentity,
            String runEvaluationIdentity,
            EditorialLineageRunContextState runContextState,
            EditorialLineageInputManifest inputManifest,
            Set<String> requiredInputRoles,
            EditorialLineageValidationContext validationContext,
            List<EditorialLineageRecord> parentCandidates) {
        this.canonicalPackHash = canonicalPackHash;
        this.trustContext = trustContext;
        this.contractVersion = contractVersion;
        this.schemaVersion = schemaVersion;
        this.compatibilityEvidence = compatibilityEvidence;
        this.projectIdentity = projectIdentity;
        this.inputScopeIdentity = inputScopeIdentity;
        this.runEvaluationIdentity = runEvaluationIdentity;
        this.runContextState = runContextState;
        this.inputManifest = inputManifest;
        this.requiredInputRoles = requiredInputRoles == null
                ? Set.of() : Collections.unmodifiableSet(Set.copyOf(requiredInputRoles));
        this.validationContext = validationContext;
        ArrayList<EditorialLineageRecord> parents = new ArrayList<>(
                parentCandidates == null ? List.of() : parentCandidates);
        this.parentCandidates = Collections.unmodifiableList(parents);
    }

    public String canonicalPackHash() { return canonicalPackHash; }
    public EditorialLineageTrustContext trustContext() { return trustContext; }
    public String contractVersion() { return contractVersion; }
    public String schemaVersion() { return schemaVersion; }
    public EditorialLineageCompatibilityEvidence compatibilityEvidence() { return compatibilityEvidence; }
    public String projectIdentity() { return projectIdentity; }
    public String inputScopeIdentity() { return inputScopeIdentity; }
    public String runEvaluationIdentity() { return runEvaluationIdentity; }
    public EditorialLineageRunContextState runContextState() { return runContextState; }
    public EditorialLineageInputManifest inputManifest() { return inputManifest; }
    public Set<String> requiredInputRoles() { return requiredInputRoles; }
    public EditorialLineageValidationContext validationContext() { return validationContext; }
    public List<EditorialLineageRecord> parentCandidates() { return parentCandidates; }
}
