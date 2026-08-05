package com.ml.tblandroidtxt.editorial.pack;

import java.util.Objects;

/** Optional read-only trust anchor supplied by a future composition root. */
public record EditorialLineageTrustContext(
        String trustedProfileId,
        String trustedProfileVersion,
        String canonicalProfileHash,
        String machineContractFingerprint) {
    public EditorialLineageTrustContext {
        Objects.requireNonNull(trustedProfileId, "trustedProfileId");
        Objects.requireNonNull(trustedProfileVersion, "trustedProfileVersion");
        Objects.requireNonNull(canonicalProfileHash, "canonicalProfileHash");
        Objects.requireNonNull(machineContractFingerprint, "machineContractFingerprint");
    }
}
