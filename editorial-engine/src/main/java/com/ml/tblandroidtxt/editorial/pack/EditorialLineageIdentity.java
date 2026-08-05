package com.ml.tblandroidtxt.editorial.pack;

import java.util.Objects;

/**
 * Stable semantic identity for one lineage node. Parent identity is carried by
 * {@link EditorialLineageParentReference} on the record so reparenting cannot
 * silently create a new node identity.
 */
public final class EditorialLineageIdentity {
    public static final String LINEAGE_CONTRACT = "lineage.exact-parent.v1";

    private final String canonicalPackHash;
    private final String trustedProfileId;
    private final String trustedProfileVersion;
    private final String canonicalProfileHash;
    private final String machineContractFingerprint;
    private final String contractVersion;
    private final String schemaVersion;
    private final String projectIdentity;
    private final String inputScopeIdentity;
    private final String runEvaluationIdentity;
    private final EditorialLineageInputManifest inputManifest;
    private final EditorialLineageNodeKind nodeKind;

    public EditorialLineageIdentity(
            String canonicalPackHash,
            String trustedProfileId,
            String trustedProfileVersion,
            String canonicalProfileHash,
            String machineContractFingerprint,
            String contractVersion,
            String schemaVersion,
            String projectIdentity,
            String inputScopeIdentity,
            String runEvaluationIdentity,
            EditorialLineageInputManifest inputManifest,
            EditorialLineageNodeKind nodeKind) {
        this.canonicalPackHash = Objects.requireNonNull(canonicalPackHash, "canonicalPackHash");
        this.trustedProfileId = Objects.requireNonNull(trustedProfileId, "trustedProfileId");
        this.trustedProfileVersion = Objects.requireNonNull(trustedProfileVersion, "trustedProfileVersion");
        this.canonicalProfileHash = Objects.requireNonNull(canonicalProfileHash, "canonicalProfileHash");
        this.machineContractFingerprint = Objects.requireNonNull(machineContractFingerprint, "machineContractFingerprint");
        this.contractVersion = Objects.requireNonNull(contractVersion, "contractVersion");
        this.schemaVersion = Objects.requireNonNull(schemaVersion, "schemaVersion");
        this.projectIdentity = Objects.requireNonNull(projectIdentity, "projectIdentity");
        this.inputScopeIdentity = Objects.requireNonNull(inputScopeIdentity, "inputScopeIdentity");
        this.runEvaluationIdentity = Objects.requireNonNull(runEvaluationIdentity, "runEvaluationIdentity");
        this.inputManifest = Objects.requireNonNull(inputManifest, "inputManifest");
        this.nodeKind = Objects.requireNonNull(nodeKind, "nodeKind");
    }

    public String canonicalPackHash() { return canonicalPackHash; }
    public String trustedProfileId() { return trustedProfileId; }
    public String trustedProfileVersion() { return trustedProfileVersion; }
    public String canonicalProfileHash() { return canonicalProfileHash; }
    public String machineContractFingerprint() { return machineContractFingerprint; }
    public String contractVersion() { return contractVersion; }
    public String schemaVersion() { return schemaVersion; }
    public String projectIdentity() { return projectIdentity; }
    public String inputScopeIdentity() { return inputScopeIdentity; }
    public String runEvaluationIdentity() { return runEvaluationIdentity; }
    public EditorialLineageInputManifest inputManifest() { return inputManifest; }
    public EditorialLineageNodeKind nodeKind() { return nodeKind; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EditorialLineageIdentity that)) return false;
        return canonicalPackHash.equals(that.canonicalPackHash)
                && trustedProfileId.equals(that.trustedProfileId)
                && trustedProfileVersion.equals(that.trustedProfileVersion)
                && canonicalProfileHash.equals(that.canonicalProfileHash)
                && machineContractFingerprint.equals(that.machineContractFingerprint)
                && contractVersion.equals(that.contractVersion)
                && schemaVersion.equals(that.schemaVersion)
                && projectIdentity.equals(that.projectIdentity)
                && inputScopeIdentity.equals(that.inputScopeIdentity)
                && runEvaluationIdentity.equals(that.runEvaluationIdentity)
                && inputManifest.equals(that.inputManifest)
                && nodeKind == that.nodeKind;
    }

    @Override public int hashCode() {
        return Objects.hash(canonicalPackHash, trustedProfileId, trustedProfileVersion,
                canonicalProfileHash, machineContractFingerprint, contractVersion,
                schemaVersion, projectIdentity, inputScopeIdentity,
                runEvaluationIdentity, inputManifest, nodeKind);
    }
}
