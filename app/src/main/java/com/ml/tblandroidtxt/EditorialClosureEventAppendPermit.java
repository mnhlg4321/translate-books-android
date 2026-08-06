package com.ml.tblandroidtxt;

/**
 * Package-private trusted-facts receipt. Public callers cannot construct or
 * pass a fabricated eligibility assertion to the closure-event DAO.
 */
final class EditorialClosureEventAppendPermit {
    final String compatibilityEvaluationId;
    final String frozenManifestFingerprint;
    final boolean trustedDataCompatible;
    final String closureAttestationVersion;
    final String closureAttestationFingerprint;

    EditorialClosureEventAppendPermit(String compatibilityEvaluationId,
                                      String frozenManifestFingerprint,
                                      boolean trustedDataCompatible,
                                      String closureAttestationVersion,
                                      String closureAttestationFingerprint) {
        this.compatibilityEvaluationId = compatibilityEvaluationId;
        this.frozenManifestFingerprint = frozenManifestFingerprint;
        this.trustedDataCompatible = trustedDataCompatible;
        this.closureAttestationVersion = closureAttestationVersion;
        this.closureAttestationFingerprint = closureAttestationFingerprint;
    }
}
