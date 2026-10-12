package com.ml.tblandroidtxt;

/** Read-only recovery/status projection. */
public enum EditorialRunContextStatusCode {
    EVENT_UNAVAILABLE,
    EVENT_INVALID,
    EVENT_COLLISION,
    NO_CLOSED_CONTEXT,
    CLOSED_UNBOUND,
    LINEAGE_EXISTS_UNBOUND,
    LINEAGE_BOUND,
    BINDING_CONFLICT,
    CORRUPT_MISMATCH,
    AMBIGUOUS
}
