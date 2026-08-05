package com.ml.tblandroidtxt.editorial.pack;

/** Stable, machine-readable reasons for trusted-profile compatibility decisions. */
public enum EditorialCompatibilityReasonCode {
    DATA_COMPATIBLE,
    NO_TRUSTED_PROFILE,
    TRUSTED_REGISTRY_INVALID,
    PROFILE_HASH_MISMATCH,
    MACHINE_FINGERPRINT_MISMATCH,
    AMBIGUOUS_TRUSTED_PROFILE,
    UNSUPPORTED_CONTRACT,
    UNSUPPORTED_SCHEMA,
    ADAPTER_REQUIRED,
    MISSING_ENGINE_CAPABILITY,
    ENGINE_UPGRADE_REQUIRED,
    INVALID_PACK,
    BLOCKED,
    COMPATIBILITY_PERSISTENCE_FAILURE
}
