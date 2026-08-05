package com.ml.tblandroidtxt.editorial.pack;

/**
 * Optional caller assertions used only for race detection.
 *
 * <p>Every field is explicitly an assertion, never an authoritative source.
 * The resolver must load the corresponding value independently and the
 * service rejects a mismatch. Parent fingerprints are intentionally absent;
 * a parent fingerprint can only come from the resolver's stored parent.</p>
 */
public final class EditorialLineageCallerAssertions {
    private final String canonicalPackHashAssertion;
    private final String trustedProfileIdAssertion;
    private final String trustedProfileVersionAssertion;
    private final String canonicalProfileHashAssertion;
    private final String machineContractFingerprintAssertion;
    private final String contractVersionAssertion;
    private final String schemaVersionAssertion;
    private final String compatibilityEvidenceIdentityAssertion;
    private final EditorialLineageCompatibilityStatus compatibilityStatusAssertion;
    private final String inputManifestFingerprintAssertion;

    public EditorialLineageCallerAssertions(
            String canonicalPackHashAssertion,
            String trustedProfileIdAssertion,
            String trustedProfileVersionAssertion,
            String canonicalProfileHashAssertion,
            String machineContractFingerprintAssertion,
            String contractVersionAssertion,
            String schemaVersionAssertion,
            String compatibilityEvidenceIdentityAssertion,
            EditorialLineageCompatibilityStatus compatibilityStatusAssertion,
            String inputManifestFingerprintAssertion) {
        this.canonicalPackHashAssertion = canonicalPackHashAssertion;
        this.trustedProfileIdAssertion = trustedProfileIdAssertion;
        this.trustedProfileVersionAssertion = trustedProfileVersionAssertion;
        this.canonicalProfileHashAssertion = canonicalProfileHashAssertion;
        this.machineContractFingerprintAssertion = machineContractFingerprintAssertion;
        this.contractVersionAssertion = contractVersionAssertion;
        this.schemaVersionAssertion = schemaVersionAssertion;
        this.compatibilityEvidenceIdentityAssertion = compatibilityEvidenceIdentityAssertion;
        this.compatibilityStatusAssertion = compatibilityStatusAssertion;
        this.inputManifestFingerprintAssertion = inputManifestFingerprintAssertion;
    }

    public static EditorialLineageCallerAssertions none() {
        return new EditorialLineageCallerAssertions(null, null, null, null, null,
                null, null, null, null, null);
    }

    public String canonicalPackHashAssertion() { return canonicalPackHashAssertion; }
    public String trustedProfileIdAssertion() { return trustedProfileIdAssertion; }
    public String trustedProfileVersionAssertion() { return trustedProfileVersionAssertion; }
    public String canonicalProfileHashAssertion() { return canonicalProfileHashAssertion; }
    public String machineContractFingerprintAssertion() { return machineContractFingerprintAssertion; }
    public String contractVersionAssertion() { return contractVersionAssertion; }
    public String schemaVersionAssertion() { return schemaVersionAssertion; }
    public String compatibilityEvidenceIdentityAssertion() { return compatibilityEvidenceIdentityAssertion; }
    public EditorialLineageCompatibilityStatus compatibilityStatusAssertion() { return compatibilityStatusAssertion; }
    public String inputManifestFingerprintAssertion() { return inputManifestFingerprintAssertion; }
}
