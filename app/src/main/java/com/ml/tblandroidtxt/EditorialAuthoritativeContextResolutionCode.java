package com.ml.tblandroidtxt;

/** Stable read-only outcomes for resolving C1 context from authoritative v17 storage. */
public enum EditorialAuthoritativeContextResolutionCode {
    RESOLVED,
    PROJECT_REVISION_REQUIRED,
    INPUT_SCOPE_REQUIRED,
    RUN_CONTEXT_REQUIRED,
    IDENTITY_UNATTESTED,
    TRUSTED_PROFILE_CONTEXT_MISMATCH,
    COMPATIBILITY_CONTEXT_MISMATCH,
    PARENT_NOT_FOUND,
    PARENT_AMBIGUOUS
}
