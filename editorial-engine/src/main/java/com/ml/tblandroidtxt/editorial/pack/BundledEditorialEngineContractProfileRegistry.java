package com.ml.tblandroidtxt.editorial.pack;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Production registry backed only by the fixed, source-bundled profile
 * resources named by {@link TrustedEditorialEngineProfileCatalog}.
 *
 * <p>There is deliberately no fallback, classpath scan, external loader,
 * persistence hook, default selector or mutation API. Construction is the
 * trust boundary: every profile must pass the C0A parser/validator and every
 * independent catalog anchor before this immutable registry is returned.</p>
 */
public final class BundledEditorialEngineContractProfileRegistry
        implements EditorialEngineContractProfileRegistry {
    private static final Comparator<TrustedEditorialEngineProfileCatalog.TrustAnchor> ANCHOR_ORDER =
            Comparator.comparing(TrustedEditorialEngineProfileCatalog.TrustAnchor::engineProfileId)
                    .thenComparing(TrustedEditorialEngineProfileCatalog.TrustAnchor::engineProfileVersion)
                    .thenComparing(TrustedEditorialEngineProfileCatalog.TrustAnchor::expectedCanonicalProfileHash)
                    .thenComparing(TrustedEditorialEngineProfileCatalog.TrustAnchor::resourcePath);
    private static final Comparator<EditorialEngineContractProfile> PROFILE_ORDER =
            Comparator.comparing(EditorialEngineContractProfile::engineProfileId)
                    .thenComparing(EditorialEngineContractProfile::engineProfileVersion)
                    .thenComparing(EditorialEngineContractProfile::canonicalProfileHash);

    private final List<EditorialEngineContractProfile> profiles;

    private BundledEditorialEngineContractProfileRegistry(List<EditorialEngineContractProfile> profiles) {
        this.profiles = List.copyOf(profiles);
    }

    /** Loads only the production catalog through this class's own classloader. */
    public static BundledEditorialEngineContractProfileRegistry load() {
        ClassLoader loader = BundledEditorialEngineContractProfileRegistry.class.getClassLoader();
        if (loader == null) loader = ClassLoader.getSystemClassLoader();
        return loadFrom(loader, TrustedEditorialEngineProfileCatalog.production());
    }

    @Override
    public List<EditorialEngineContractProfile> list() {
        return profiles;
    }

    @Override
    public Optional<EditorialEngineContractProfile> findByCanonicalHash(String canonicalProfileHash) {
        if (canonicalProfileHash == null) return Optional.empty();
        return profiles.stream()
                .filter(profile -> canonicalProfileHash.equals(profile.canonicalProfileHash()))
                .findFirst();
    }

    @Override
    public Optional<EditorialEngineContractProfile> findByIdentity(
            String engineProfileId, String engineProfileVersion) {
        if (engineProfileId == null || engineProfileVersion == null) return Optional.empty();
        return profiles.stream()
                .filter(profile -> engineProfileId.equals(profile.engineProfileId())
                        && engineProfileVersion.equals(profile.engineProfileVersion()))
                .findFirst();
    }

    /* Package-private injection exists only for pure-JVM negative tests. */
    static BundledEditorialEngineContractProfileRegistry loadForTest(
            ClassLoader loader, List<TrustedEditorialEngineProfileCatalog.TrustAnchor> anchors) {
        return loadFrom(loader, anchors);
    }

    private static BundledEditorialEngineContractProfileRegistry loadFrom(
            ClassLoader loader, List<TrustedEditorialEngineProfileCatalog.TrustAnchor> anchors) {
        if (loader == null || anchors == null || anchors.isEmpty()) {
            throw failure(EditorialEngineContractProfileRegistryException.Code.CATALOG_INVALID);
        }

        List<TrustedEditorialEngineProfileCatalog.TrustAnchor> orderedAnchors = new ArrayList<>(anchors);
        Set<String> anchorIdentities = new HashSet<>();
        Set<String> anchorHashes = new HashSet<>();
        for (TrustedEditorialEngineProfileCatalog.TrustAnchor anchor : orderedAnchors) {
            if (anchor == null || anchor.resourcePath().isBlank()
                    || anchor.engineProfileId().isBlank() || anchor.engineProfileVersion().isBlank()
                    || anchor.expectedResourceSha256().isBlank()
                    || anchor.expectedCanonicalProfileHash().isBlank()
                    || anchor.expectedMachineContractFingerprint().isBlank()) {
                throw failure(EditorialEngineContractProfileRegistryException.Code.CATALOG_INVALID);
            }
            if (!anchorIdentities.add(identityKey(anchor.engineProfileId(), anchor.engineProfileVersion()))) {
                throw failure(EditorialEngineContractProfileRegistryException.Code.DUPLICATE_IDENTITY);
            }
            if (!anchorHashes.add(anchor.expectedCanonicalProfileHash())) {
                throw failure(EditorialEngineContractProfileRegistryException.Code.DUPLICATE_CANONICAL_HASH);
            }
        }
        orderedAnchors.sort(ANCHOR_ORDER);

        EditorialEngineContractProfileValidator validator = new EditorialEngineContractProfileValidator();
        List<EditorialEngineContractProfile> loaded = new ArrayList<>(orderedAnchors.size());
        Set<String> profileIdentities = new HashSet<>();
        Set<String> profileHashes = new HashSet<>();
        for (TrustedEditorialEngineProfileCatalog.TrustAnchor anchor : orderedAnchors) {
            byte[] bytes = readResource(loader, anchor.resourcePath());
            if (!EditorialCanonicalJson.sha256Hex(bytes).equals(anchor.expectedResourceSha256())) {
                throw failure(EditorialEngineContractProfileRegistryException.Code.RESOURCE_HASH_MISMATCH);
            }
            EditorialEngineContractProfileValidationResult result = validator.validate(bytes);
            if (!result.isValid() || result.profile().isEmpty()) {
                throw failure(EditorialEngineContractProfileRegistryException.Code.PROFILE_INVALID);
            }
            EditorialEngineContractProfile profile = result.profile().get();
            if (!anchor.engineProfileId().equals(profile.engineProfileId())) {
                throw failure(EditorialEngineContractProfileRegistryException.Code.PROFILE_ID_MISMATCH);
            }
            if (!anchor.engineProfileVersion().equals(profile.engineProfileVersion())) {
                throw failure(EditorialEngineContractProfileRegistryException.Code.PROFILE_VERSION_MISMATCH);
            }
            if (!anchor.expectedCanonicalProfileHash().equals(profile.canonicalProfileHash())) {
                throw failure(EditorialEngineContractProfileRegistryException.Code.CANONICAL_HASH_MISMATCH);
            }
            if (!anchor.expectedMachineContractFingerprint().equals(profile.machineContractFingerprint())) {
                throw failure(EditorialEngineContractProfileRegistryException.Code.MACHINE_FINGERPRINT_MISMATCH);
            }
            if (!profileIdentities.add(identityKey(profile.engineProfileId(), profile.engineProfileVersion()))) {
                throw failure(EditorialEngineContractProfileRegistryException.Code.DUPLICATE_IDENTITY);
            }
            if (!profileHashes.add(profile.canonicalProfileHash())) {
                throw failure(EditorialEngineContractProfileRegistryException.Code.DUPLICATE_CANONICAL_HASH);
            }
            loaded.add(profile);
        }
        loaded.sort(PROFILE_ORDER);
        return new BundledEditorialEngineContractProfileRegistry(loaded);
    }

    private static byte[] readResource(ClassLoader loader, String resourcePath) {
        try (InputStream input = loader.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw failure(EditorialEngineContractProfileRegistryException.Code.RESOURCE_MISSING);
            }
            return input.readAllBytes();
        } catch (EditorialEngineContractProfileRegistryException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw failure(EditorialEngineContractProfileRegistryException.Code.RESOURCE_READ_ERROR);
        }
    }

    private static String identityKey(String id, String version) {
        return id + "\u0000" + version;
    }

    private static EditorialEngineContractProfileRegistryException failure(
            EditorialEngineContractProfileRegistryException.Code code) {
        return new EditorialEngineContractProfileRegistryException(code, code.name());
    }
}
