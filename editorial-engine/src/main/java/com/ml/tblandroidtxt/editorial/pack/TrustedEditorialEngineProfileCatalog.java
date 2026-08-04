package com.ml.tblandroidtxt.editorial.pack;

import java.util.List;
import java.util.Objects;

/**
 * Compile-time trust anchors for the production profile resources.
 *
 * <p>The JSON's self-declared hashes are checked by the C0A validator, but
 * they are not trust roots. This catalog is the independent allow-list and
 * pins the resource bytes, identity, canonical hash and machine projection.</p>
 */
public final class TrustedEditorialEngineProfileCatalog {
    public static final String PRODUCTION_RESOURCE_PATH =
            "editorial/engine-profile/v1/profile.json";
    public static final String PRODUCTION_PROFILE_ID =
            "com.ml.tblandroidtxt.editorial.engine.bootstrap";
    public static final String PRODUCTION_PROFILE_VERSION = "1.0.0";
    public static final String EXPECTED_RESOURCE_SHA256 =
            "1c1f7cec41d1984695475283bf6c9b6318b9fdb5473507f86be701e143f3a660";
    public static final String EXPECTED_CANONICAL_PROFILE_HASH =
            "916ae87dc0b0f429dcf9ae3ffa3fbc76efdf0ebd2c7f631aea572567819b67a3";
    public static final String EXPECTED_MACHINE_CONTRACT_FINGERPRINT =
            "7bcc3d249d66dbe834b764f3bf129752435e50953be47b566c17db9d79cb489b";

    private static final List<TrustAnchor> PRODUCTION = List.of(new TrustAnchor(
            PRODUCTION_RESOURCE_PATH,
            PRODUCTION_PROFILE_ID,
            PRODUCTION_PROFILE_VERSION,
            EXPECTED_RESOURCE_SHA256,
            EXPECTED_CANONICAL_PROFILE_HASH,
            EXPECTED_MACHINE_CONTRACT_FINGERPRINT));

    private TrustedEditorialEngineProfileCatalog() {}

    public static List<TrustAnchor> production() {
        return PRODUCTION;
    }

    public record TrustAnchor(
            String resourcePath,
            String engineProfileId,
            String engineProfileVersion,
            String expectedResourceSha256,
            String expectedCanonicalProfileHash,
            String expectedMachineContractFingerprint) {
        public TrustAnchor {
            Objects.requireNonNull(resourcePath, "resourcePath");
            Objects.requireNonNull(engineProfileId, "engineProfileId");
            Objects.requireNonNull(engineProfileVersion, "engineProfileVersion");
            Objects.requireNonNull(expectedResourceSha256, "expectedResourceSha256");
            Objects.requireNonNull(expectedCanonicalProfileHash, "expectedCanonicalProfileHash");
            Objects.requireNonNull(expectedMachineContractFingerprint, "expectedMachineContractFingerprint");
        }
    }
}
