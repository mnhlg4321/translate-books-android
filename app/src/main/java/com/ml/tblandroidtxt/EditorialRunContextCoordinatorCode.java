package com.ml.tblandroidtxt;

/** Stable coordinator result vocabulary; no raw SQLite exception crosses this boundary. */
public enum EditorialRunContextCoordinatorCode {
    COMMAND_INVALID,
    CLOSURE_EVENT_UNAVAILABLE,
    CLOSURE_EVENT_INVALID,
    CLOSURE_EVENT_COLLISION,
    CLOSE_REJECTED,
    CLOSED_UNBOUND,
    LINEAGE_VALIDATION_REJECTED,
    LINEAGE_PERSISTENCE_FAILED,
    BINDING_CONFLICT,
    LINEAGE_BOUND,
    ALREADY_BOUND,
    STATUS_UNAVAILABLE,
    STATUS_AMBIGUOUS,
    STATUS_CORRUPT_MISMATCH
}
