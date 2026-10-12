package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContractFacts;

/** Immutable trusted facts returned only by a reviewed profile/contract resolver. */
public record EditorialTrustedClosedRunFacts(
        String trustedProfileId,
        String trustedProfileVersion,
        String canonicalProfileHash,
        String machineFingerprint,
        EditorialClosedRunContractFacts contractFacts) {
}
