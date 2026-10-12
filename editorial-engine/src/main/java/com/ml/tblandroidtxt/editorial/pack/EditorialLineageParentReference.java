package com.ml.tblandroidtxt.editorial.pack;

import java.util.Objects;

/** Exact parent node identity and fingerprint; both are required for a child. */
public record EditorialLineageParentReference(
        String parentRecordIdentity,
        String parentRecordFingerprint) {
    public EditorialLineageParentReference {
        Objects.requireNonNull(parentRecordIdentity, "parentRecordIdentity");
        Objects.requireNonNull(parentRecordFingerprint, "parentRecordFingerprint");
    }
}
