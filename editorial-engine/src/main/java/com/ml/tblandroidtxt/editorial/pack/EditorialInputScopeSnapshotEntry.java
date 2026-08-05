package com.ml.tblandroidtxt.editorial.pack;

/** One complete required-input fact; null counts/ordinal mean unknown and fail closed. */
public record EditorialInputScopeSnapshotEntry(
        String role,
        Long ordinal,
        String inputSha256,
        Long byteCount,
        Long itemCount) {
    public EditorialInputScopeSnapshotEntry {
        if (role == null) throw new IllegalArgumentException("input role is required");
        if (inputSha256 == null) throw new IllegalArgumentException("input SHA-256 is required");
    }
}
