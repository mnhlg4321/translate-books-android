package com.ml.tblandroidtxt.editorial.pack;

/** Version-independent entry point for the reviewed v1 scope-key normalization. */
public final class EditorialScopeKeyCanonicalizer {
    public static final String CANONICAL_VERSION = "EDITORIAL_INPUT_SCOPE_CANONICAL_V1";

    private EditorialScopeKeyCanonicalizer() { }

    public static String normalize(String value) {
        return EditorialIdentityText.scopeKey(value);
    }
}
