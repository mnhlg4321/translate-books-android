package com.ml.tblandroidtxt.editorial.pack;

import java.util.Objects;

/** Immutable compatibility provenance selected by the authoritative resolver. */
public record EditorialLineageCompatibilityEvidence(
        String evidenceIdentity,
        EditorialLineageCompatibilityStatus status) {
    public EditorialLineageCompatibilityEvidence {
        Objects.requireNonNull(evidenceIdentity, "evidenceIdentity");
        Objects.requireNonNull(status, "status");
    }
}
