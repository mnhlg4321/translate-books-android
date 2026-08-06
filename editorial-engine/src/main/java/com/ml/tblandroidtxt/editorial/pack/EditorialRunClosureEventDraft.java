package com.ml.tblandroidtxt.editorial.pack;

import java.util.Objects;

/** Immutable semantic closure-event request; identity, fingerprint and audit time are absent. */
public final class EditorialRunClosureEventDraft {
    public static final String CONTRACT_VERSION = "editorial-run-closure-event-v1";

    private final String authoritativeRunIdentity;
    private final String projectRevisionIdentity;
    private final String inputScopeSnapshotIdentity;
    private final String compatibilityEvaluationId;
    private final String runKind;
    private final String phaseIdentity;
    private final String frozenManifestFingerprint;
    private final String frozenManifestReference;
    private final EditorialLineageNodeKind nodeKind;
    private final String parentRecordIdentity;
    private final long runAttemptOrdinal;
    private final EditorialClosureEventEligibility closureEligibility;
    private final String closureAttestationVersion;
    private final String closureAttestationFingerprint;

    public EditorialRunClosureEventDraft(
            String authoritativeRunIdentity,
            String projectRevisionIdentity,
            String inputScopeSnapshotIdentity,
            String compatibilityEvaluationId,
            String runKind,
            String phaseIdentity,
            String frozenManifestFingerprint,
            String frozenManifestReference,
            EditorialLineageNodeKind nodeKind,
            String parentRecordIdentity,
            long runAttemptOrdinal,
            EditorialClosureEventEligibility closureEligibility,
            String closureAttestationVersion,
            String closureAttestationFingerprint) {
        this.authoritativeRunIdentity = EditorialIdentityText.sha256(
                authoritativeRunIdentity, "authoritative run identity");
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
        this.frozenManifestReference = opaqueReference(frozenManifestReference,
                "frozen manifest reference");
        this.nodeKind = Objects.requireNonNull(nodeKind, "node kind");
        if (nodeKind == EditorialLineageNodeKind.ROOT && parentRecordIdentity != null) {
            throw new IllegalArgumentException("root parent is forbidden");
        }
        if (nodeKind == EditorialLineageNodeKind.CHILD && parentRecordIdentity == null) {
            throw new IllegalArgumentException("child parent is required");
        }
        this.parentRecordIdentity = parentRecordIdentity == null ? null
                : EditorialIdentityText.sha256(parentRecordIdentity, "parent record identity");
        if (runAttemptOrdinal < 0) throw new IllegalArgumentException("run attempt ordinal cannot be negative");
        this.runAttemptOrdinal = runAttemptOrdinal;
        this.closureEligibility = Objects.requireNonNull(closureEligibility, "closure eligibility");
        this.closureAttestationVersion = EditorialIdentityText.nonEmpty(
                closureAttestationVersion, "closure attestation version");
        this.closureAttestationFingerprint = EditorialIdentityText.sha256(
                closureAttestationFingerprint, "closure attestation fingerprint");
    }

    public String authoritativeRunIdentity() { return authoritativeRunIdentity; }
    public String projectRevisionIdentity() { return projectRevisionIdentity; }
    public String inputScopeSnapshotIdentity() { return inputScopeSnapshotIdentity; }
    public String compatibilityEvaluationId() { return compatibilityEvaluationId; }
    public String runKind() { return runKind; }
    public String phaseIdentity() { return phaseIdentity; }
    public String frozenManifestFingerprint() { return frozenManifestFingerprint; }
    public String frozenManifestReference() { return frozenManifestReference; }
    public EditorialLineageNodeKind nodeKind() { return nodeKind; }
    public String parentRecordIdentity() { return parentRecordIdentity; }
    public long runAttemptOrdinal() { return runAttemptOrdinal; }
    public EditorialClosureEventEligibility closureEligibility() { return closureEligibility; }
    public String closureAttestationVersion() { return closureAttestationVersion; }
    public String closureAttestationFingerprint() { return closureAttestationFingerprint; }

    private static String opaqueReference(String value, String field) {
        String checked = EditorialIdentityText.nonEmpty(value, field);
        if (checked.contains("/") || checked.contains("\\") || checked.contains("://")) {
            throw new IllegalArgumentException(field + " must be an opaque attestation reference");
        }
        return checked;
    }
}
