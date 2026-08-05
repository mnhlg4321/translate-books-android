package com.ml.tblandroidtxt.editorial.pack;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable, canonical identity of a compatibility evaluation. This context
 * contains only semantic inputs and deliberately excludes time, UI text,
 * locale, raw pack content, machine paths and secrets.
 */
public final class EditorialCompatibilityEvaluationContext {
    public static final String CONTEXT_DOMAIN = "EDITORIAL_COMPATIBILITY_EVALUATION_CONTEXT_V1\n";
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    private final String canonicalPackHash;
    private final String trustedProfileId;
    private final String trustedProfileVersion;
    private final String canonicalProfileHash;
    private final String engineVersion;
    private final String machineContractFingerprint;
    private final String evaluatorContractVersion;
    private final String adapterSetFingerprint;
    private final String capabilityFingerprint;

    public EditorialCompatibilityEvaluationContext(String canonicalPackHash,
                                                   String trustedProfileId,
                                                   String trustedProfileVersion,
                                                   String canonicalProfileHash,
                                                   String engineVersion,
                                                   String machineContractFingerprint,
                                                   String evaluatorContractVersion,
                                                   String adapterSetFingerprint,
                                                   String capabilityFingerprint) {
        this.canonicalPackHash = sha256(canonicalPackHash, "canonicalPackHash");
        this.trustedProfileId = text(trustedProfileId, "trustedProfileId");
        this.trustedProfileVersion = text(trustedProfileVersion, "trustedProfileVersion");
        this.canonicalProfileHash = sha256(canonicalProfileHash, "canonicalProfileHash");
        this.engineVersion = text(engineVersion, "engineVersion");
        this.machineContractFingerprint = sha256(machineContractFingerprint, "machineContractFingerprint");
        this.evaluatorContractVersion = text(evaluatorContractVersion, "evaluatorContractVersion");
        this.adapterSetFingerprint = sha256(adapterSetFingerprint, "adapterSetFingerprint");
        this.capabilityFingerprint = sha256(capabilityFingerprint, "capabilityFingerprint");
    }

    public String canonicalPackHash() { return canonicalPackHash; }
    public String trustedProfileId() { return trustedProfileId; }
    public String trustedProfileVersion() { return trustedProfileVersion; }
    public String canonicalProfileHash() { return canonicalProfileHash; }
    public String engineVersion() { return engineVersion; }
    public String machineContractFingerprint() { return machineContractFingerprint; }
    public String evaluatorContractVersion() { return evaluatorContractVersion; }
    public String adapterSetFingerprint() { return adapterSetFingerprint; }
    public String capabilityFingerprint() { return capabilityFingerprint; }

    /** Canonical semantic JSON; field order is fixed by EditorialCanonicalJson. */
    public String canonicalJson() {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        fields.put("canonicalPackHash", canonicalPackHash);
        fields.put("trustedProfileId", trustedProfileId);
        fields.put("trustedProfileVersion", trustedProfileVersion);
        fields.put("canonicalProfileHash", canonicalProfileHash);
        fields.put("engineVersion", engineVersion);
        fields.put("machineContractFingerprint", machineContractFingerprint);
        fields.put("evaluatorContractVersion", evaluatorContractVersion);
        fields.put("adapterSetFingerprint", adapterSetFingerprint);
        fields.put("capabilityFingerprint", capabilityFingerprint);
        return EditorialCanonicalJson.canonicalize(fields);
    }

    /** SHA-256 of the canonical semantic context, excluding evaluatedAt. */
    public String fingerprint() {
        return EditorialCanonicalJson.sha256Hex(
                (CONTEXT_DOMAIN + canonicalJson()).getBytes(StandardCharsets.UTF_8));
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EditorialCompatibilityEvaluationContext that)) return false;
        return canonicalJson().equals(that.canonicalJson());
    }

    @Override public int hashCode() { return Objects.hash(canonicalJson()); }

    private static String text(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String sha256(String value, String name) {
        String text = text(value, name);
        if (!SHA256.matcher(text).matches()) throw new IllegalArgumentException(name + " must be lowercase SHA-256");
        return text;
    }
}
