package com.ml.tblandroidtxt.editorial.pack;

import java.util.Objects;

/** One immutable input-role/hash/count entry in a lineage manifest. */
public record EditorialLineageInputEntry(
        String role,
        int ordinal,
        String inputHash,
        long byteCount,
        long itemCount) {
    public EditorialLineageInputEntry {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(inputHash, "inputHash");
    }
}
