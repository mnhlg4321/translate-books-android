package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable input-role manifest used by the lineage identity tuple. */
public final class EditorialLineageInputManifest {
    private final String manifestVersion;
    private final List<EditorialLineageInputEntry> entries;

    public EditorialLineageInputManifest(String manifestVersion,
                                         List<EditorialLineageInputEntry> entries) {
        this.manifestVersion = Objects.requireNonNull(manifestVersion, "manifestVersion");
        this.entries = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(entries, "entries")));
    }

    public String manifestVersion() { return manifestVersion; }

    public List<EditorialLineageInputEntry> entries() { return entries; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EditorialLineageInputManifest that)) return false;
        return manifestVersion.equals(that.manifestVersion) && entries.equals(that.entries);
    }

    @Override public int hashCode() { return Objects.hash(manifestVersion, entries); }
}
