package com.ml.tblandroidtxt.editorial.pack;

import java.util.Objects;

/**
 * Immutable semantic request for a new authoritative run declaration.
 * Identity, fingerprint, ordinal and timestamp are deliberately absent.
 */
public final class EditorialAuthoritativeRunDeclarationDraft {
    public static final String CONTRACT_VERSION = "editorial-authoritative-run-declaration-v1";

    private final String attemptRequestSelector;
    private final String projectRevisionIdentity;
    private final String inputScopeSnapshotIdentity;
    private final String compatibilityEvaluationId;
    private final String runKind;
    private final String phaseIdentity;
    private final String frozenManifestFingerprint;
    private final String frozenManifestReference;
    private final EditorialLineageNodeKind nodeKind;
    private final String parentRecordIdentity;

    public EditorialAuthoritativeRunDeclarationDraft(
            String attemptRequestSelector,
            String projectRevisionIdentity,
            String inputScopeSnapshotIdentity,
            String compatibilityEvaluationId,
            String runKind,
            String phaseIdentity,
            String frozenManifestFingerprint,
            String frozenManifestReference,
            EditorialLineageNodeKind nodeKind,
            String parentRecordIdentity) {
        this.attemptRequestSelector = EditorialAttemptRequestSelector.validate(attemptRequestSelector);
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
    }

    public String attemptRequestSelector() { return attemptRequestSelector; }
    public String projectRevisionIdentity() { return projectRevisionIdentity; }
    public String inputScopeSnapshotIdentity() { return inputScopeSnapshotIdentity; }
    public String compatibilityEvaluationId() { return compatibilityEvaluationId; }
    public String runKind() { return runKind; }
    public String phaseIdentity() { return phaseIdentity; }
    public String frozenManifestFingerprint() { return frozenManifestFingerprint; }
    public String frozenManifestReference() { return frozenManifestReference; }
    public EditorialLineageNodeKind nodeKind() { return nodeKind; }
    public String parentRecordIdentity() { return parentRecordIdentity; }

    public EditorialAuthoritativeRunAllocationScope allocationScope() {
        return new EditorialAuthoritativeRunAllocationScope(projectRevisionIdentity,
                inputScopeSnapshotIdentity, compatibilityEvaluationId, runKind,
                phaseIdentity, frozenManifestFingerprint);
    }

    private static String opaqueReference(String value, String field) {
        String checked = EditorialIdentityText.nonEmpty(value, field);
        if (checked.contains("/") || checked.contains("\\") || checked.contains("://")) {
            throw new IllegalArgumentException(field + " must be an opaque attestation reference");
        }
        return checked;
    }
}
