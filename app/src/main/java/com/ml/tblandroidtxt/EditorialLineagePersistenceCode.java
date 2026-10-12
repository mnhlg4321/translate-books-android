package com.ml.tblandroidtxt;

/** Stable DAO boundary outcomes for append-only lineage persistence. */
public enum EditorialLineagePersistenceCode {
    APPENDED,
    ALREADY_EXISTS,
    DUPLICATE_LINEAGE,
    REPARENT_ATTEMPT,
    ORPHAN_LINEAGE,
    PARENT_MISMATCH,
    AMBIGUOUS_PARENT,
    INVALID_LINEAGE,
    PERSISTENCE_FAILURE
}
