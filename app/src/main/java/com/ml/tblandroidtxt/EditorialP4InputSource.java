package com.ml.tblandroidtxt;

import java.util.Arrays;

/** Raw source input for P4; the binding service computes length and SHA-256. */
public record EditorialP4InputSource(
        String role,
        String sourceReference,
        byte[] bytes,
        String encoding,
        String schemaStatus,
        long ordinal) {
    public EditorialP4InputSource {
        if (role == null || role.isBlank()) throw new IllegalArgumentException("source role is required");
        if (sourceReference == null || sourceReference.isBlank()) {
            throw new IllegalArgumentException("source reference is required");
        }
        if (bytes == null) throw new IllegalArgumentException("source bytes are required");
        bytes = Arrays.copyOf(bytes, bytes.length);
        if (encoding == null || encoding.isBlank()) throw new IllegalArgumentException("source encoding is required");
        if (schemaStatus == null || schemaStatus.isBlank()) throw new IllegalArgumentException("source schema status is required");
        if (ordinal < 0) throw new IllegalArgumentException("source ordinal cannot be negative");
    }

    @Override public byte[] bytes() { return Arrays.copyOf(bytes, bytes.length); }
}
