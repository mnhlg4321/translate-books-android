package com.ml.tblandroidtxt.editorial.pack;

import java.util.Objects;

/** Immutable machine-readable validation issue. */
public record EditorialLineageValidationIssue(
        EditorialLineageValidationCode code,
        String path,
        String detail) {
    public EditorialLineageValidationIssue {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(detail, "detail");
    }
}
