package com.ml.tblandroidtxt;

import java.io.InputStream;

/** One already-selected source stream. The importer reads it once and never reopens its path/URI. */
public record EditorialPackImportEntry(String path, InputStream stream, long declaredLength, long compressedLength, boolean symbolicLink) {
    public EditorialPackImportEntry {
        if (path == null || path.isBlank()) throw new IllegalArgumentException("Pack entry path is required");
        if (stream == null) throw new IllegalArgumentException("Pack entry stream is required");
    }

    public EditorialPackImportEntry(String path, InputStream stream) {
        this(path, stream, -1L, -1L, false);
    }

    public EditorialPackImportEntry(String path, InputStream stream, long compressedLength, boolean symbolicLink) {
        this(path, stream, -1L, compressedLength, symbolicLink);
    }
}
