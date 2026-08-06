package com.ml.tblandroidtxt.editorial.pack;

import java.util.Objects;

/** Frozen semantic scope used by the declaration DAO for ordinal allocation. */
public final class EditorialAuthoritativeRunAllocationScope {
    private final String projectRevisionIdentity;
    private final String inputScopeSnapshotIdentity;
    private final String compatibilityEvaluationId;
    private final String runKind;
    private final String phaseIdentity;
    private final String frozenManifestFingerprint;

    public EditorialAuthoritativeRunAllocationScope(
            String projectRevisionIdentity,
            String inputScopeSnapshotIdentity,
            String compatibilityEvaluationId,
            String runKind,
            String phaseIdentity,
            String frozenManifestFingerprint) {
        this.projectRevisionIdentity = EditorialIdentityText.sha256(
                projectRevisionIdentity, "project revision identity");
        this.inputScopeSnapshotIdentity = EditorialIdentityText.sha256(
                inputScopeSnapshotIdentity, "input scope snapshot identity");
        this.compatibilityEvaluationId = EditorialIdentityText.nonEmpty(
                compatibilityEvaluationId, "compatibility evaluation identity");
        this.runKind = EditorialIdentityText.nonEmpty(runKind, "run kind");
        this.phaseIdentity = EditorialIdentityText.nonEmpty(phaseIdentity, "phase identity");
        this.frozenManifestFingerprint = EditorialIdentityText.sha256(
                frozenManifestFingerprint, "frozen manifest fingerprint");
    }

    public String projectRevisionIdentity() { return projectRevisionIdentity; }
    public String inputScopeSnapshotIdentity() { return inputScopeSnapshotIdentity; }
    public String compatibilityEvaluationId() { return compatibilityEvaluationId; }
    public String runKind() { return runKind; }
    public String phaseIdentity() { return phaseIdentity; }
    public String frozenManifestFingerprint() { return frozenManifestFingerprint; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EditorialAuthoritativeRunAllocationScope that)) return false;
        return projectRevisionIdentity.equals(that.projectRevisionIdentity)
                && inputScopeSnapshotIdentity.equals(that.inputScopeSnapshotIdentity)
                && compatibilityEvaluationId.equals(that.compatibilityEvaluationId)
                && runKind.equals(that.runKind)
                && phaseIdentity.equals(that.phaseIdentity)
                && frozenManifestFingerprint.equals(that.frozenManifestFingerprint);
    }

    @Override public int hashCode() {
        return Objects.hash(projectRevisionIdentity, inputScopeSnapshotIdentity,
                compatibilityEvaluationId, runKind, phaseIdentity, frozenManifestFingerprint);
    }
}
