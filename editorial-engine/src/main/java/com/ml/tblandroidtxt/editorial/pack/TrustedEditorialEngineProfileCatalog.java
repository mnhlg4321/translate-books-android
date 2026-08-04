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
            "deb0e89a4084a88c137c71ba2aa7a7170f84d9979395ef58866c73529ce601eb";
    public static final String EXPECTED_CANONICAL_PROFILE_HASH =
            "2d4e2f76dc5defcfb98cfd36cec49b0e5454cb3462db93a6b1586f7784eb91b6";
    public static final String EXPECTED_MACHINE_CONTRACT_FINGERPRINT =
            "6410f374ce175cbc6fc32484f5cd9635888b9297b01d882923af06ccd4ac1e4c";

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
