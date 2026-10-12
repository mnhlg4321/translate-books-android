package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityEvaluationContext;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;

import java.util.Optional;
import java.util.regex.Pattern;

/** Immutable persistence snapshot for one compatibility evaluation. */
public final class EditorialPackCompatibilityEvaluation {
    private static final Pattern REASON_CODE = Pattern.compile("[A-Z0-9_]{1,128}");

    public enum Attestation { TRUSTED_PROFILE, LEGACY_UNATTESTED }

    private final long id;
    private final String evaluationId;
    private final String importId;
    private final Long packRowId;
    private final Long compatibilityResultId;
    private final String canonicalPackHash;
    private final String trustedProfileId;
    private final String trustedProfileVersion;
    private final String canonicalProfileHash;
    private final String engineVersionUsed;
    private final String machineContractFingerprint;
    private final String evaluatorContractVersion;
    private final String adapterSetFingerprint;
    private final String capabilityFingerprint;
    private final String contextFingerprint;
    private final EditorialPackCompatibilityClass compatibilityOutcome;
    private final String reasonCode;
    private final String blockerDetails;
    private final long evaluatedAt;
    private final Attestation attestation;

    private EditorialPackCompatibilityEvaluation(long id, String evaluationId, String importId,
                                                 Long packRowId, Long compatibilityResultId,
                                                 String canonicalPackHash, String trustedProfileId,
                                                 String trustedProfileVersion, String canonicalProfileHash,
                                                 String engineVersionUsed, String machineContractFingerprint,
                                                 String evaluatorContractVersion, String adapterSetFingerprint,
                                                 String capabilityFingerprint, String contextFingerprint,
                                                 EditorialPackCompatibilityClass compatibilityOutcome,
                                                 String reasonCode, String blockerDetails, long evaluatedAt,
                                                 Attestation attestation) {
        this.id = id;
        this.evaluationId = required(evaluationId, "evaluationId");
        this.importId = required(importId, "importId");
        this.packRowId = packRowId;
        this.compatibilityResultId = compatibilityResultId;
        this.canonicalPackHash = required(canonicalPackHash, "canonicalPackHash");
        this.trustedProfileId = trustedProfileId;
        this.trustedProfileVersion = trustedProfileVersion;
        this.canonicalProfileHash = canonicalProfileHash;
        this.engineVersionUsed = required(engineVersionUsed, "engineVersionUsed");
        this.machineContractFingerprint = required(machineContractFingerprint, "machineContractFingerprint");
        this.evaluatorContractVersion = evaluatorContractVersion;
        this.adapterSetFingerprint = adapterSetFingerprint;
        this.capabilityFingerprint = capabilityFingerprint;
        this.contextFingerprint = contextFingerprint;
        this.compatibilityOutcome = required(compatibilityOutcome, "compatibilityOutcome");
        this.reasonCode = requiredReasonCode(reasonCode);
        this.blockerDetails = blockerDetails == null ? "" : blockerDetails;
        if (evaluatedAt < 0) throw new IllegalArgumentException("evaluatedAt must not be negative");
        this.evaluatedAt = evaluatedAt;
        this.attestation = required(attestation, "attestation");
        if (attestation == Attestation.TRUSTED_PROFILE) {
            required(trustedProfileId, "trustedProfileId");
            required(trustedProfileVersion, "trustedProfileVersion");
            required(canonicalProfileHash, "canonicalProfileHash");
            required(evaluatorContractVersion, "evaluatorContractVersion");
            required(adapterSetFingerprint, "adapterSetFingerprint");
            required(capabilityFingerprint, "capabilityFingerprint");
            required(contextFingerprint, "contextFingerprint");
        }
    }

    public static EditorialPackCompatibilityEvaluation trusted(
            String evaluationId, String importId, Long packRowId, Long compatibilityResultId,
            EditorialCompatibilityEvaluationContext context,
            EditorialPackCompatibilityClass compatibilityOutcome, String reasonCode,
            String blockerDetails, long evaluatedAt) {
        if (context == null) throw new IllegalArgumentException("context is required");
        return new EditorialPackCompatibilityEvaluation(-1L, evaluationId, importId, packRowId,
                compatibilityResultId, context.canonicalPackHash(), context.trustedProfileId(),
                context.trustedProfileVersion(), context.canonicalProfileHash(), context.engineVersion(),
                context.machineContractFingerprint(), context.evaluatorContractVersion(),
                context.adapterSetFingerprint(), context.capabilityFingerprint(), context.fingerprint(),
                compatibilityOutcome, reasonCode, blockerDetails, evaluatedAt, Attestation.TRUSTED_PROFILE);
    }

    static EditorialPackCompatibilityEvaluation persisted(long id, String evaluationId, String importId,
                                                           Long packRowId, Long compatibilityResultId,
                                                           String canonicalPackHash, String trustedProfileId,
                                                           String trustedProfileVersion, String canonicalProfileHash,
                                                           String engineVersionUsed, String machineContractFingerprint,
                                                           String evaluatorContractVersion, String adapterSetFingerprint,
                                                           String capabilityFingerprint, String contextFingerprint,
                                                           EditorialPackCompatibilityClass outcome, String reasonCode,
                                                           String blockerDetails, long evaluatedAt) {
        return new EditorialPackCompatibilityEvaluation(id, evaluationId, importId, packRowId,
                compatibilityResultId, canonicalPackHash, trustedProfileId, trustedProfileVersion,
                canonicalProfileHash, engineVersionUsed, machineContractFingerprint, evaluatorContractVersion,
                adapterSetFingerprint, capabilityFingerprint, contextFingerprint, outcome, reasonCode,
                blockerDetails, evaluatedAt, Attestation.TRUSTED_PROFILE);
    }

    static EditorialPackCompatibilityEvaluation legacy(long id, String importId, Long packRowId,
                                                        String canonicalPackHash, String engineVersionUsed,
                                                        String machineContractFingerprint,
                                                        EditorialPackCompatibilityClass outcome,
                                                        String blockerDetails, long evaluatedAt) {
        return new EditorialPackCompatibilityEvaluation(id, "legacy-v14-" + id, importId, packRowId, id,
                canonicalPackHash, null, null, null, engineVersionUsed, machineContractFingerprint,
                null, null, null, null, outcome, "LEGACY_UNATTESTED", blockerDetails, evaluatedAt,
                Attestation.LEGACY_UNATTESTED);
    }

    public long id() { return id; }
    public String evaluationId() { return evaluationId; }
    public String importId() { return importId; }
    public Optional<Long> packRowId() { return Optional.ofNullable(packRowId); }
    public Optional<Long> compatibilityResultId() { return Optional.ofNullable(compatibilityResultId); }
    public String canonicalPackHash() { return canonicalPackHash; }
    public Optional<String> trustedProfileId() { return Optional.ofNullable(trustedProfileId); }
    public Optional<String> trustedProfileVersion() { return Optional.ofNullable(trustedProfileVersion); }
    public Optional<String> canonicalProfileHash() { return Optional.ofNullable(canonicalProfileHash); }
    public String engineVersionUsed() { return engineVersionUsed; }
    public String machineContractFingerprint() { return machineContractFingerprint; }
    public Optional<String> evaluatorContractVersion() { return Optional.ofNullable(evaluatorContractVersion); }
    public Optional<String> adapterSetFingerprint() { return Optional.ofNullable(adapterSetFingerprint); }
    public Optional<String> capabilityFingerprint() { return Optional.ofNullable(capabilityFingerprint); }
    public Optional<String> contextFingerprint() { return Optional.ofNullable(contextFingerprint); }
    public EditorialPackCompatibilityClass compatibilityOutcome() { return compatibilityOutcome; }
    public String reasonCode() { return reasonCode; }
    public String blockerDetails() { return blockerDetails; }
    public long evaluatedAt() { return evaluatedAt; }
    public Attestation attestation() { return attestation; }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static <T> T required(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String requiredReasonCode(String value) {
        String result = required(value, "reasonCode");
        if (!REASON_CODE.matcher(result).matches()) throw new IllegalArgumentException("reasonCode must be stable uppercase code");
        return result;
    }
}
