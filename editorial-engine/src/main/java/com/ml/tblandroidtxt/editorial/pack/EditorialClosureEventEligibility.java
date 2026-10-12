package com.ml.tblandroidtxt.editorial.pack;

/** Immutable closure eligibility vocabulary; only ELIGIBLE may be persisted by the DAO gate. */
public enum EditorialClosureEventEligibility {
    ELIGIBLE,
    INELIGIBLE,
    CANCELLED,
    FAILED,
    STALE,
    INVALID
}
