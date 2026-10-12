package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable closed context. It can only be materialized with an allocated ordinal. */
public final class EditorialClosedRunContext {
    public static final String IDENTITY_DOMAIN = "EDITORIAL_CLOSED_RUN_IDENTITY_V1";
    public static final String FINGERPRINT_DOMAIN = "EDITORIAL_CLOSED_RUN_FINGERPRINT_V1";

    private final EditorialClosedRunContextDraft draft;
    private final long runAttemptOrdinal;
    private final String canonicalProjection;
    private final String closedRunFingerprint;
    private final String closedRunIdentity;

    private EditorialClosedRunContext(EditorialClosedRunContextDraft draft, long runAttemptOrdinal) {
        this.draft = Objects.requireNonNull(draft, "closed-run draft");
        if (runAttemptOrdinal < 0) throw new IllegalArgumentException("run attempt ordinal cannot be negative");
        if (draft.compatibilityOutcome() != EditorialClosedRunCompatibilityOutcome.DATA_COMPATIBLE) {
            throw new IllegalArgumentException("closed-run context requires DATA_COMPATIBLE");
        }
        this.runAttemptOrdinal = runAttemptOrdinal;
        this.canonicalProjection = buildCanonicalProjection();
        this.closedRunFingerprint = EditorialIdentityText.hash(FINGERPRINT_DOMAIN, canonicalProjection);
        this.closedRunIdentity = EditorialIdentityText.hash(IDENTITY_DOMAIN, canonicalProjection);
    }

    public static EditorialClosedRunContext allocate(EditorialClosedRunContextDraft draft,
                                                      long runAttemptOrdinal) {
        return new EditorialClosedRunContext(draft, runAttemptOrdinal);
    }

    public EditorialClosedRunContextDraft draft() { return draft; }
    public String closedContextVersion() { return draft.closedContextVersion(); }
    public String projectRevisionIdentity() { return draft.projectRevisionIdentity(); }
    public String scopeSnapshotIdentity() { return draft.scopeSnapshotIdentity(); }
    public String canonicalPackHash() { return draft.canonicalPackHash(); }
    public String compatibilityEvaluationId() { return draft.compatibilityEvaluationId(); }
    public EditorialClosedRunCompatibilityOutcome compatibilityOutcome() { return draft.compatibilityOutcome(); }
    public String trustedProfileId() { return draft.trustedProfileId(); }
    public String trustedProfileVersion() { return draft.trustedProfileVersion(); }
    public String canonicalProfileHash() { return draft.canonicalProfileHash(); }
    public String machineFingerprint() { return draft.machineFingerprint(); }
    public EditorialClosedRunContractFacts contractFacts() { return draft.contractFacts(); }
    public String runKind() { return draft.runKind(); }
    public String phaseIdentity() { return draft.phaseIdentity(); }
    public long runAttemptOrdinal() { return runAttemptOrdinal; }
    public String inputManifestFingerprint() { return draft.inputManifestFingerprint(); }
    public String canonicalProjection() { return canonicalProjection; }
    public String closedRunFingerprint() { return closedRunFingerprint; }
    public String closedRunIdentity() { return closedRunIdentity; }

    private String buildCanonicalProjection() {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("closedContextVersion", closedContextVersion());
        projection.put("projectRevisionIdentity", projectRevisionIdentity());
        projection.put("scopeSnapshotIdentity", scopeSnapshotIdentity());
        projection.put("canonicalPackHash", canonicalPackHash());
        projection.put("compatibilityEvaluationId", compatibilityEvaluationId());
        projection.put("compatibilityOutcome", compatibilityOutcome().name());
        projection.put("trustedProfileId", trustedProfileId());
        projection.put("trustedProfileVersion", trustedProfileVersion());
        projection.put("canonicalProfileHash", canonicalProfileHash());
        projection.put("machineFingerprint", machineFingerprint());
        projection.put("contractFacts", contractFactsMap());
        projection.put("runKind", runKind());
        projection.put("phaseIdentity", phaseIdentity());
        projection.put("runAttemptOrdinal", BigDecimal.valueOf(runAttemptOrdinal));
        projection.put("inputManifestFingerprint", inputManifestFingerprint());
        return EditorialCanonicalJson.canonicalize(projection);
    }

    private Map<String, Object> contractFactsMap() {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("packContractId", contractFacts().packContractId());
        facts.put("packContractVersion", contractFacts().packContractVersion());
        facts.put("packSchemaId", contractFacts().packSchemaId());
        facts.put("packSchemaVersion", contractFacts().packSchemaVersion());
        facts.put("evaluatorContractVersion", contractFacts().evaluatorContractVersion());
        facts.put("adapterSetFingerprint", contractFacts().adapterSetFingerprint());
        facts.put("capabilityFingerprint", contractFacts().capabilityFingerprint());
        facts.put("compatibilityContextFingerprint", contractFacts().compatibilityContextFingerprint());
        facts.put("engineVersionUsed", contractFacts().engineVersionUsed());
        return facts;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EditorialClosedRunContext that)) return false;
        return closedRunIdentity.equals(that.closedRunIdentity)
                && canonicalProjection.equals(that.canonicalProjection);
    }

    @Override public int hashCode() { return Objects.hash(closedRunIdentity, canonicalProjection); }
}
