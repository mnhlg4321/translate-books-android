package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;

import java.nio.charset.StandardCharsets;

/** Redacted pre-dispatch gate for the opt-in P6 fixture live runner. */
final class EditorialP6FixtureLivePreflight {
    static final String MATCH = "MATCH";
    static final String EXPECTED_FINGERPRINT_REQUIRED = "P6_LIVE_EXPECTED_FINGERPRINT_REQUIRED";
    static final String ROUTE_MISMATCH = "P6_LIVE_ROUTE_SETTINGS_MISMATCH";
    static final String KEY_MISSING = "P6_LIVE_KEY_NOT_CONFIGURED";
    static final String FINGERPRINT_MISMATCH = "P6_LIVE_FINGERPRINT_MISMATCH";

    private EditorialP6FixtureLivePreflight() { }

    /** Returns a fixed code only. Neither key nor expected/actual fingerprint leaves this process. */
    static String check(AppSettings settings, String expectedFingerprint) {
        if (!isSha256(expectedFingerprint)) return EXPECTED_FINGERPRINT_REQUIRED;
        if (!EditorialP5EFreshRawRoutingPolicy.matches(settings)) return ROUTE_MISMATCH;
        if (settings.apiKey == null || settings.apiKey.trim().isEmpty()) return KEY_MISSING;
        String endpoint = AppSettings.normalizeEndpoint(settings.baseUrl);
        String actual = EditorialCanonicalJson.sha256Hex(
                (endpoint + "\n" + settings.apiKey).getBytes(StandardCharsets.UTF_8));
        return expectedFingerprint.equalsIgnoreCase(actual) ? MATCH : FINGERPRINT_MISMATCH;
    }

    private static boolean isSha256(String value) {
        return value != null && value.matches("[0-9a-fA-F]{64}");
    }
}
