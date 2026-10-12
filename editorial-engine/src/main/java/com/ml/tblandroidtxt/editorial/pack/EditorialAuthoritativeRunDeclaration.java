package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable authoritative run declaration with DAO-owned ordinal and audit time. */
public final class EditorialAuthoritativeRunDeclaration {
    public static final String IDENTITY_DOMAIN =
            "EDITORIAL_AUTHORITATIVE_RUN_DECLARATION_IDENTITY_V1";
    public static final String FINGERPRINT_DOMAIN =
            "EDITORIAL_AUTHORITATIVE_RUN_DECLARATION_FINGERPRINT_V1";

    private final EditorialAuthoritativeRunDeclarationDraft draft;
    private final long runAttemptOrdinal;
    private final long createdAt;
    private final String identityProjection;
    private final String fingerprintProjection;
    private final String declarationIdentity;
    private final String declarationFingerprint;

    private EditorialAuthoritativeRunDeclaration(
            EditorialAuthoritativeRunDeclarationDraft draft,
            long runAttemptOrdinal,
            long createdAt) {
        this.draft = Objects.requireNonNull(draft, "declaration draft");
        if (runAttemptOrdinal < 0) throw new IllegalArgumentException("run attempt ordinal cannot be negative");
        if (createdAt < 0) throw new IllegalArgumentException("created timestamp cannot be negative");
        this.runAttemptOrdinal = runAttemptOrdinal;
        this.createdAt = createdAt;
        this.identityProjection = buildIdentityProjection();
        this.fingerprintProjection = buildFingerprintProjection();
        this.declarationIdentity = EditorialIdentityText.hash(IDENTITY_DOMAIN, identityProjection);
        this.declarationFingerprint = EditorialIdentityText.hash(
                FINGERPRINT_DOMAIN, fingerprintProjection);
    }

    /** Called only by the transaction-scoped declaration DAO after allocation. */
    public static EditorialAuthoritativeRunDeclaration allocate(
            EditorialAuthoritativeRunDeclarationDraft draft,
            long runAttemptOrdinal,
            long createdAt) {
        return new EditorialAuthoritativeRunDeclaration(draft, runAttemptOrdinal, createdAt);
    }

    /** Reconstructs and verifies a stored row; declared hashes are never trusted. */
    public static EditorialAuthoritativeRunDeclaration fromStored(
            String declaredIdentity,
            String declaredFingerprint,
            EditorialAuthoritativeRunDeclarationDraft draft,
            long runAttemptOrdinal,
            long createdAt) {
        EditorialAuthoritativeRunDeclaration result = allocate(draft, runAttemptOrdinal, createdAt);
        if (!result.declarationIdentity.equals(declaredIdentity)
                || !result.declarationFingerprint.equals(declaredFingerprint)) {
            throw new IllegalArgumentException("stored declaration identity/fingerprint mismatch");
        }
        return result;
    }

    public EditorialAuthoritativeRunDeclarationDraft draft() { return draft; }
    public String runDeclarationContractVersion() { return EditorialAuthoritativeRunDeclarationDraft.CONTRACT_VERSION; }
    public String attemptRequestSelector() { return draft.attemptRequestSelector(); }
    public String projectRevisionIdentity() { return draft.projectRevisionIdentity(); }
    public String inputScopeSnapshotIdentity() { return draft.inputScopeSnapshotIdentity(); }
    public String compatibilityEvaluationId() { return draft.compatibilityEvaluationId(); }
    public String runKind() { return draft.runKind(); }
    public String phaseIdentity() { return draft.phaseIdentity(); }
    public String frozenManifestFingerprint() { return draft.frozenManifestFingerprint(); }
    public String frozenManifestReference() { return draft.frozenManifestReference(); }
    public EditorialLineageNodeKind nodeKind() { return draft.nodeKind(); }
    public String parentRecordIdentity() { return draft.parentRecordIdentity(); }
    public long runAttemptOrdinal() { return runAttemptOrdinal; }
    public long createdAt() { return createdAt; }
    public EditorialAuthoritativeRunAllocationScope allocationScope() { return draft.allocationScope(); }
    public String identityProjection() { return identityProjection; }
    public String fingerprintProjection() { return fingerprintProjection; }
    public String canonicalProjection() { return fingerprintProjection; }
    public String declarationIdentity() { return declarationIdentity; }
    public String declarationFingerprint() { return declarationFingerprint; }

    private String buildIdentityProjection() {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("runDeclarationContractVersion", runDeclarationContractVersion());
        projection.put("attemptRequestSelector", attemptRequestSelector());
        projection.put("projectRevisionIdentity", projectRevisionIdentity());
        projection.put("inputScopeSnapshotIdentity", inputScopeSnapshotIdentity());
        projection.put("compatibilityEvaluationId", compatibilityEvaluationId());
        projection.put("runKind", runKind());
        projection.put("phaseIdentity", phaseIdentity());
        projection.put("frozenManifestFingerprint", frozenManifestFingerprint());
        projection.put("nodeKind", nodeKind().name());
        projection.put("parentRecordIdentity", parentRecordIdentity());
        projection.put("runAttemptOrdinal", BigDecimal.valueOf(runAttemptOrdinal));
        return EditorialCanonicalJson.canonicalize(projection);
    }

    private String buildFingerprintProjection() {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("identityProjection", EditorialCanonicalJson.parse(
                EditorialIdentityText.strictUtf8(identityProjection(), "identity projection")));
        projection.put("frozenManifestReference", frozenManifestReference());
        return EditorialCanonicalJson.canonicalize(projection);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EditorialAuthoritativeRunDeclaration that)) return false;
        return declarationIdentity.equals(that.declarationIdentity)
                && declarationFingerprint.equals(that.declarationFingerprint)
                && createdAt == that.createdAt;
    }

    @Override public int hashCode() {
        return Objects.hash(declarationIdentity, declarationFingerprint, createdAt);
    }
}
