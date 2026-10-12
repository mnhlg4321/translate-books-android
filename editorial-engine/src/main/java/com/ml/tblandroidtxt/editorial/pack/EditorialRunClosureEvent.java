package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable canonical RUN_CONTEXT_CLOSED event for the v18 authoritative store. */
public final class EditorialRunClosureEvent {
    public static final String IDENTITY_DOMAIN = "EDITORIAL_CLOSURE_EVENT_IDENTITY_V1";
    public static final String FINGERPRINT_DOMAIN = "EDITORIAL_CLOSURE_EVENT_FINGERPRINT_V1";

    private final EditorialAuthoritativeRunDeclaration declaration;
    private final EditorialRunClosureEventDraft draft;
    private final String closureAttestationVersion;
    private final String closureAttestationFingerprint;
    private final long appendedAt;
    private final String identityProjection;
    private final String fingerprintProjection;
    private final String closureEventIdentity;
    private final String closureEventFingerprint;

    private EditorialRunClosureEvent(
            EditorialAuthoritativeRunDeclaration declaration,
            EditorialRunClosureEventDraft draft,
            String closureAttestationVersion,
            String closureAttestationFingerprint,
            long appendedAt) {
        this.declaration = Objects.requireNonNull(declaration, "authoritative declaration");
        this.draft = Objects.requireNonNull(draft, "closure event draft");
        if (!declaration.attemptRequestSelector().equals(draft.attemptRequestSelector())) {
            throw new IllegalArgumentException("closure selector does not resolve declaration");
        }
        this.closureAttestationVersion = EditorialIdentityText.nonEmpty(
                closureAttestationVersion, "closure attestation version");
        this.closureAttestationFingerprint = EditorialIdentityText.sha256(
                closureAttestationFingerprint, "closure attestation fingerprint");
        if (appendedAt < 0) throw new IllegalArgumentException("appended timestamp cannot be negative");
        this.appendedAt = appendedAt;
        this.identityProjection = buildIdentityProjection();
        this.fingerprintProjection = buildFingerprintProjection();
        this.closureEventIdentity = EditorialIdentityText.hash(IDENTITY_DOMAIN, identityProjection);
        this.closureEventFingerprint = EditorialIdentityText.hash(
                FINGERPRINT_DOMAIN, fingerprintProjection);
    }

    /** Called only after declaration/reference/trusted-evidence checks by the event DAO seam. */
    public static EditorialRunClosureEvent allocate(
            EditorialAuthoritativeRunDeclaration declaration,
            EditorialRunClosureEventDraft draft,
            String closureAttestationVersion,
            String closureAttestationFingerprint,
            long appendedAt) {
        return new EditorialRunClosureEvent(declaration, draft, closureAttestationVersion,
                closureAttestationFingerprint, appendedAt);
    }

    /** Reconstructs and verifies stored hashes; declared values are never trusted. */
    public static EditorialRunClosureEvent fromStored(
            String declaredIdentity,
            String declaredFingerprint,
            EditorialAuthoritativeRunDeclaration declaration,
            EditorialRunClosureEventDraft draft,
            String closureAttestationVersion,
            String closureAttestationFingerprint,
            long appendedAt) {
        EditorialRunClosureEvent result = allocate(declaration, draft, closureAttestationVersion,
                closureAttestationFingerprint, appendedAt);
        if (!result.closureEventIdentity.equals(declaredIdentity)
                || !result.closureEventFingerprint.equals(declaredFingerprint)) {
            throw new IllegalArgumentException("stored closure event identity/fingerprint mismatch");
        }
        return result;
    }

    public EditorialRunClosureEventDraft draft() { return draft; }
    public String closureEventContractVersion() { return EditorialRunClosureEventDraft.CONTRACT_VERSION; }
    public String authoritativeRunIdentity() { return declaration.declarationIdentity(); }
    public String projectRevisionIdentity() { return declaration.projectRevisionIdentity(); }
    public String inputScopeSnapshotIdentity() { return declaration.inputScopeSnapshotIdentity(); }
    public String compatibilityEvaluationId() { return declaration.compatibilityEvaluationId(); }
    public String runKind() { return declaration.runKind(); }
    public String phaseIdentity() { return declaration.phaseIdentity(); }
    public String frozenManifestFingerprint() { return declaration.frozenManifestFingerprint(); }
    public String frozenManifestReference() { return declaration.frozenManifestReference(); }
    public EditorialLineageNodeKind nodeKind() { return draft.nodeKind(); }
    public String parentRecordIdentity() { return draft.parentRecordIdentity(); }
    public long runAttemptOrdinal() { return declaration.runAttemptOrdinal(); }
    public EditorialClosureEventEligibility closureEligibility() { return draft.closureEligibility(); }
    public String closureAttestationVersion() { return closureAttestationVersion; }
    public String closureAttestationFingerprint() { return closureAttestationFingerprint; }
    public long appendedAt() { return appendedAt; }
    public String identityProjection() { return identityProjection; }
    public String fingerprintProjection() { return fingerprintProjection; }
    public String canonicalProjection() { return fingerprintProjection; }
    public String closureEventIdentity() { return closureEventIdentity; }
    public String closureEventFingerprint() { return closureEventFingerprint; }

    private String buildIdentityProjection() {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("closureEventContractVersion", closureEventContractVersion());
        projection.put("authoritativeRunIdentity", authoritativeRunIdentity());
        projection.put("projectRevisionIdentity", projectRevisionIdentity());
        projection.put("inputScopeSnapshotIdentity", inputScopeSnapshotIdentity());
        projection.put("compatibilityEvaluationId", compatibilityEvaluationId());
        projection.put("runKind", runKind());
        projection.put("phaseIdentity", phaseIdentity());
        projection.put("frozenManifestFingerprint", frozenManifestFingerprint());
        projection.put("nodeKind", nodeKind().name());
        projection.put("parentRecordIdentity", parentRecordIdentity());
        projection.put("runAttemptOrdinal", BigDecimal.valueOf(runAttemptOrdinal()));
        projection.put("closureEligibility", closureEligibility().name());
        projection.put("closureAttestationVersion", closureAttestationVersion());
        return EditorialCanonicalJson.canonicalize(projection);
    }

    private String buildFingerprintProjection() {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("identityProjection", EditorialCanonicalJson.parse(
                EditorialIdentityText.strictUtf8(identityProjection(), "identity projection")));
        projection.put("frozenManifestReference", frozenManifestReference());
        projection.put("closureAttestationFingerprint", closureAttestationFingerprint());
        return EditorialCanonicalJson.canonicalize(projection);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EditorialRunClosureEvent that)) return false;
        return closureEventIdentity.equals(that.closureEventIdentity)
                && closureEventFingerprint.equals(that.closureEventFingerprint)
                && appendedAt == that.appendedAt;
    }

    @Override public int hashCode() {
        return Objects.hash(closureEventIdentity, closureEventFingerprint, appendedAt);
    }
}
