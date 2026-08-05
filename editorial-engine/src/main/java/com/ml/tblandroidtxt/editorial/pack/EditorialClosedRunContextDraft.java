package com.ml.tblandroidtxt.editorial.pack;

import java.util.Objects;

/** Closed-run input facts. Attempt ordinal is intentionally absent and DAO-owned. */
public final class EditorialClosedRunContextDraft {
    private final String closedContextVersion;
    private final String projectRevisionIdentity;
    private final String scopeSnapshotIdentity;
    private final String canonicalPackHash;
    private final String compatibilityEvaluationId;
    private final EditorialClosedRunCompatibilityOutcome compatibilityOutcome;
    private final String trustedProfileId;
    private final String trustedProfileVersion;
    private final String canonicalProfileHash;
    private final String machineFingerprint;
    private final EditorialClosedRunContractFacts contractFacts;
    private final String runKind;
    private final String phaseIdentity;
    private final String inputManifestFingerprint;

    public EditorialClosedRunContextDraft(
            String closedContextVersion,
            String projectRevisionIdentity,
            String scopeSnapshotIdentity,
            String canonicalPackHash,
            String compatibilityEvaluationId,
            EditorialClosedRunCompatibilityOutcome compatibilityOutcome,
            String trustedProfileId,
            String trustedProfileVersion,
            String canonicalProfileHash,
            String machineFingerprint,
            EditorialClosedRunContractFacts contractFacts,
            String runKind,
            String phaseIdentity,
            String inputManifestFingerprint) {
        this.closedContextVersion = EditorialIdentityText.nonEmpty(closedContextVersion,
                "closed-run context version");
        this.projectRevisionIdentity = EditorialIdentityText.sha256(projectRevisionIdentity,
                "project revision identity");
        this.scopeSnapshotIdentity = EditorialIdentityText.sha256(scopeSnapshotIdentity,
                "scope snapshot identity");
        this.canonicalPackHash = EditorialIdentityText.sha256(canonicalPackHash,
                "canonical pack hash");
        this.compatibilityEvaluationId = EditorialIdentityText.nonEmpty(
                compatibilityEvaluationId, "compatibility evaluation id");
        this.compatibilityOutcome = Objects.requireNonNull(compatibilityOutcome,
                "compatibility outcome");
        this.trustedProfileId = EditorialIdentityText.nonEmpty(trustedProfileId,
                "trusted profile id");
        this.trustedProfileVersion = EditorialIdentityText.nonEmpty(trustedProfileVersion,
                "trusted profile version");
        this.canonicalProfileHash = EditorialIdentityText.sha256(canonicalProfileHash,
                "canonical profile hash");
        this.machineFingerprint = EditorialIdentityText.sha256(machineFingerprint,
                "machine fingerprint");
        this.contractFacts = Objects.requireNonNull(contractFacts, "contract facts");
        this.runKind = EditorialIdentityText.nonEmpty(runKind, "run kind");
        this.phaseIdentity = EditorialIdentityText.nonEmpty(phaseIdentity, "phase identity");
        this.inputManifestFingerprint = EditorialIdentityText.sha256(inputManifestFingerprint,
                "input manifest fingerprint");
    }

    public String closedContextVersion() { return closedContextVersion; }
    public String projectRevisionIdentity() { return projectRevisionIdentity; }
    public String scopeSnapshotIdentity() { return scopeSnapshotIdentity; }
    public String canonicalPackHash() { return canonicalPackHash; }
    public String compatibilityEvaluationId() { return compatibilityEvaluationId; }
    public EditorialClosedRunCompatibilityOutcome compatibilityOutcome() { return compatibilityOutcome; }
    public String trustedProfileId() { return trustedProfileId; }
    public String trustedProfileVersion() { return trustedProfileVersion; }
    public String canonicalProfileHash() { return canonicalProfileHash; }
    public String machineFingerprint() { return machineFingerprint; }
    public EditorialClosedRunContractFacts contractFacts() { return contractFacts; }
    public String runKind() { return runKind; }
    public String phaseIdentity() { return phaseIdentity; }
    public String inputManifestFingerprint() { return inputManifestFingerprint; }
}
