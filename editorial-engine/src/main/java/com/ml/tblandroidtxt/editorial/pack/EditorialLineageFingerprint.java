package com.ml.tblandroidtxt.editorial.pack;

import java.util.Objects;

/** Immutable derived fingerprints for a lineage identity and exact parent binding. */
public record EditorialLineageFingerprint(
        String inputManifestFingerprint,
        String recordIdentity,
        String recordFingerprint) {
    public EditorialLineageFingerprint {
        Objects.requireNonNull(inputManifestFingerprint, "inputManifestFingerprint");
        Objects.requireNonNull(recordIdentity, "recordIdentity");
        Objects.requireNonNull(recordFingerprint, "recordFingerprint");
    }

    public static EditorialLineageFingerprint derive(EditorialLineageIdentity identity,
                                                      EditorialLineageParentReference parent) {
        String manifestFingerprint = EditorialLineageCanonicalizer.inputManifestFingerprint(identity.inputManifest());
        String recordIdentity = EditorialLineageCanonicalizer.recordIdentity(identity);
        String recordFingerprint = EditorialLineageCanonicalizer.recordFingerprint(identity, parent, recordIdentity);
        return new EditorialLineageFingerprint(manifestFingerprint, recordIdentity, recordFingerprint);
    }
}
