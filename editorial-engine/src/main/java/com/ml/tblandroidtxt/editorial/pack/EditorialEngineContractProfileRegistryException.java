package com.ml.tblandroidtxt.editorial.pack;

/** Stable, non-sensitive failure for the bundled trusted-profile boundary. */
public final class EditorialEngineContractProfileRegistryException extends IllegalStateException {
    public enum Code {
        CATALOG_INVALID,
        RESOURCE_MISSING,
        RESOURCE_READ_ERROR,
        RESOURCE_HASH_MISMATCH,
        PROFILE_INVALID,
        PROFILE_ID_MISMATCH,
        PROFILE_VERSION_MISMATCH,
        CANONICAL_HASH_MISMATCH,
        MACHINE_FINGERPRINT_MISMATCH,
        DUPLICATE_IDENTITY,
        DUPLICATE_CANONICAL_HASH
    }

    private final Code code;

    public EditorialEngineContractProfileRegistryException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
