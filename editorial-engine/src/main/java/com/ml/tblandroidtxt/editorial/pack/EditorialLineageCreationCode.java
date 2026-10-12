package com.ml.tblandroidtxt.editorial.pack;

/**
 * Stable outcomes owned by the pure-JVM creation boundary.
 *
 * <p>Parent, duplicate, reparent, orphan and other record-level outcomes are
 * deliberately not repeated here. They are returned losslessly as
 * {@link EditorialLineageValidationCode} values from the existing validator
 * vocabulary.</p>
 */
public enum EditorialLineageCreationCode {
    READY_TO_APPEND,
    VALIDATION_FAILED,
    LINEAGE_CONTEXT_REQUIRED,
    PROJECT_IDENTITY_REQUIRED,
    INPUT_SCOPE_REQUIRED,
    RUN_EVALUATION_IDENTITY_REQUIRED,
    INPUT_MANIFEST_INCOMPLETE,
    TRUSTED_PROFILE_CONTEXT_MISMATCH,
    COMPATIBILITY_CONTEXT_MISMATCH,
    CALLER_ASSERTION_MISMATCH,
    RESOLVER_FAILURE
}
