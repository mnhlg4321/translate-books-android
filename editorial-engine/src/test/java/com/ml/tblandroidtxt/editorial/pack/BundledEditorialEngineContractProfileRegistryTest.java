package com.ml.tblandroidtxt.editorial.pack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public class BundledEditorialEngineContractProfileRegistryTest {
    private static final String PATH = TrustedEditorialEngineProfileCatalog.PRODUCTION_RESOURCE_PATH;
    private static final String ID = TrustedEditorialEngineProfileCatalog.PRODUCTION_PROFILE_ID;
    private static final String VERSION = TrustedEditorialEngineProfileCatalog.PRODUCTION_PROFILE_VERSION;
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    public void productionProfileParsesAndValidates() throws Exception {
        byte[] bytes = productionBytes();
        EditorialEngineContractProfileValidationResult result =
                new EditorialEngineContractProfileValidator().validate(bytes);

        assertTrue(result.isValid());
        assertTrue(result.profile().isPresent());
        assertEquals(ID, result.profile().get().engineProfileId());
        assertEquals(List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1),
                result.profile().get().implementedCapabilities());
        assertEquals(9, result.profile().get().explicitlyMissingCapabilities().size());
    }

    @Test
    public void canonicalAndMachineHashesMatchIndependentAnchors() throws Exception {
        BundledEditorialEngineContractProfileRegistry registry =
                BundledEditorialEngineContractProfileRegistry.load();
        EditorialEngineContractProfile profile = registry.list().get(0);

        assertEquals(TrustedEditorialEngineProfileCatalog.EXPECTED_CANONICAL_PROFILE_HASH,
                profile.canonicalProfileHash());
        assertEquals(TrustedEditorialEngineProfileCatalog.EXPECTED_MACHINE_CONTRACT_FINGERPRINT,
                profile.machineContractFingerprint());
        assertEquals(TrustedEditorialEngineProfileCatalog.EXPECTED_RESOURCE_SHA256,
                EditorialCanonicalJson.sha256Hex(productionBytes()));
    }

    @Test
    public void lookupByHashAndIdentityIsExact() {
        BundledEditorialEngineContractProfileRegistry registry =
                BundledEditorialEngineContractProfileRegistry.load();
        EditorialEngineContractProfile profile = registry.list().get(0);

        assertSame(profile, registry.findByCanonicalHash(profile.canonicalProfileHash()).get());
        assertSame(profile, registry.findByIdentity(profile.engineProfileId(), profile.engineProfileVersion()).get());
        assertEquals(Optional.empty(), registry.findByCanonicalHash("not-a-hash"));
        assertEquals(Optional.empty(), registry.findByIdentity(ID, "9.9.9"));
    }

    @Test
    public void orderingAndReturnedValuesAreImmutable() {
        BundledEditorialEngineContractProfileRegistry first =
                BundledEditorialEngineContractProfileRegistry.load();
        BundledEditorialEngineContractProfileRegistry second =
                BundledEditorialEngineContractProfileRegistry.load();

        assertEquals(first.list(), second.list());
        assertThrows(UnsupportedOperationException.class, () -> first.list().add(first.list().get(0)));
        EditorialEngineContractProfile profile = first.list().get(0);
        assertThrows(UnsupportedOperationException.class,
                () -> profile.implementedCapabilities().add("test.mutation"));
        assertThrows(UnsupportedOperationException.class,
                () -> profile.contextAllowListByPhase().put("test.mutation", null));
    }

    @Test
    public void missingResourceFailsClosed() {
        RegistryFailure failure = failure(() -> BundledEditorialEngineContractProfileRegistry.loadForTest(
                new ClassLoader(null) {}, TrustedEditorialEngineProfileCatalog.production()));
        assertEquals(EditorialEngineContractProfileRegistryException.Code.RESOURCE_MISSING, failure.code);
    }

    @Test
    public void tamperedResourceBytesFailClosed() throws Exception {
        byte[] tampered = new String(productionBytes(), StandardCharsets.UTF_8)
                .replace("4.16-dev.30", "4.16-dev.31")
                .getBytes(StandardCharsets.UTF_8);
        RegistryFailure failure = failure(() -> BundledEditorialEngineContractProfileRegistry.loadForTest(
                resourceLoader(PATH, tampered), TrustedEditorialEngineProfileCatalog.production()));
        assertEquals(EditorialEngineContractProfileRegistryException.Code.RESOURCE_HASH_MISMATCH, failure.code);
    }

    @Test
    public void canonicalHashAnchorMismatchFailsClosed() throws Exception {
        byte[] bytes = productionBytes();
        TrustedEditorialEngineProfileCatalog.TrustAnchor anchor =
                anchor(PATH, bytes, ID, VERSION, ZERO_HASH,
                        TrustedEditorialEngineProfileCatalog.EXPECTED_MACHINE_CONTRACT_FINGERPRINT);
        RegistryFailure failure = failure(() -> BundledEditorialEngineContractProfileRegistry.loadForTest(
                resourceLoader(PATH, bytes), List.of(anchor)));
        assertEquals(EditorialEngineContractProfileRegistryException.Code.CANONICAL_HASH_MISMATCH, failure.code);
    }

    @Test
    public void machineFingerprintAnchorMismatchFailsClosed() throws Exception {
        byte[] bytes = productionBytes();
        TrustedEditorialEngineProfileCatalog.TrustAnchor anchor =
                anchor(PATH, bytes, ID, VERSION,
                        TrustedEditorialEngineProfileCatalog.EXPECTED_CANONICAL_PROFILE_HASH, ZERO_HASH);
        RegistryFailure failure = failure(() -> BundledEditorialEngineContractProfileRegistry.loadForTest(
                resourceLoader(PATH, bytes), List.of(anchor)));
        assertEquals(EditorialEngineContractProfileRegistryException.Code.MACHINE_FINGERPRINT_MISMATCH, failure.code);
    }

    @Test
    public void invalidSchemaFailsClosed() throws Exception {
        byte[] invalid = new String(productionBytes(), StandardCharsets.UTF_8)
                .replace("{\n  \"profileFormat\"", "{\n  \"unknownField\": true,\n  \"profileFormat\"")
                .getBytes(StandardCharsets.UTF_8);
        RegistryFailure failure = failure(() -> BundledEditorialEngineContractProfileRegistry.loadForTest(
                resourceLoader(PATH, invalid), List.of(anchor(PATH, invalid, ID, VERSION,
                        TrustedEditorialEngineProfileCatalog.EXPECTED_CANONICAL_PROFILE_HASH,
                        TrustedEditorialEngineProfileCatalog.EXPECTED_MACHINE_CONTRACT_FINGERPRINT))));
        assertEquals(EditorialEngineContractProfileRegistryException.Code.PROFILE_INVALID, failure.code);
    }

    @Test
    public void bomFailsClosed() throws Exception {
        byte[] source = productionBytes();
        byte[] invalid = new byte[source.length + 3];
        invalid[0] = (byte) 0xef;
        invalid[1] = (byte) 0xbb;
        invalid[2] = (byte) 0xbf;
        System.arraycopy(source, 0, invalid, 3, source.length);
        RegistryFailure failure = failure(() -> BundledEditorialEngineContractProfileRegistry.loadForTest(
                resourceLoader(PATH, invalid), List.of(anchor(PATH, invalid, ID, VERSION,
                        TrustedEditorialEngineProfileCatalog.EXPECTED_CANONICAL_PROFILE_HASH,
                        TrustedEditorialEngineProfileCatalog.EXPECTED_MACHINE_CONTRACT_FINGERPRINT))));
        assertEquals(EditorialEngineContractProfileRegistryException.Code.PROFILE_INVALID, failure.code);
    }

    @Test
    public void missingCapabilityEvidenceFailsClosed() throws Exception {
        String json = new String(productionBytes(), StandardCharsets.UTF_8);
        String marker = "  \"capabilityEvidence\": [";
        int start = json.indexOf(marker);
        int end = json.indexOf("  \"bundledAdapterIds\"", start);
        assertTrue(start >= 0 && end > start);
        String invalid = json.substring(0, start)
                + "  \"capabilityEvidence\": [],\n"
                + json.substring(end);
        byte[] bytes = invalid.getBytes(StandardCharsets.UTF_8);
        RegistryFailure failure = failure(() -> BundledEditorialEngineContractProfileRegistry.loadForTest(
                resourceLoader(PATH, bytes), List.of(anchor(PATH, bytes, ID, VERSION,
                        TrustedEditorialEngineProfileCatalog.EXPECTED_CANONICAL_PROFILE_HASH,
                        TrustedEditorialEngineProfileCatalog.EXPECTED_MACHINE_CONTRACT_FINGERPRINT))));
        assertEquals(EditorialEngineContractProfileRegistryException.Code.PROFILE_INVALID, failure.code);
    }

    @Test
    public void duplicateIdentityInCatalogFailsClosed() throws Exception {
        byte[] bytes = productionBytes();
        TrustedEditorialEngineProfileCatalog.TrustAnchor first = anchor(PATH, bytes, ID, VERSION,
                TrustedEditorialEngineProfileCatalog.EXPECTED_CANONICAL_PROFILE_HASH,
                TrustedEditorialEngineProfileCatalog.EXPECTED_MACHINE_CONTRACT_FINGERPRINT);
        TrustedEditorialEngineProfileCatalog.TrustAnchor duplicate = anchor("duplicate.json", bytes, ID, VERSION,
                TrustedEditorialEngineProfileCatalog.EXPECTED_CANONICAL_PROFILE_HASH,
                TrustedEditorialEngineProfileCatalog.EXPECTED_MACHINE_CONTRACT_FINGERPRINT);
        RegistryFailure failure = failure(() -> BundledEditorialEngineContractProfileRegistry.loadForTest(
                resourceLoader(PATH, bytes, "duplicate.json", bytes), List.of(first, duplicate)));
        assertEquals(EditorialEngineContractProfileRegistryException.Code.DUPLICATE_IDENTITY, failure.code);
    }

    @Test
    public void duplicateCanonicalHashInCatalogFailsClosed() throws Exception {
        byte[] bytes = productionBytes();
        TrustedEditorialEngineProfileCatalog.TrustAnchor first = anchor(PATH, bytes, ID, VERSION,
                TrustedEditorialEngineProfileCatalog.EXPECTED_CANONICAL_PROFILE_HASH,
                TrustedEditorialEngineProfileCatalog.EXPECTED_MACHINE_CONTRACT_FINGERPRINT);
        TrustedEditorialEngineProfileCatalog.TrustAnchor duplicate = anchor("duplicate.json", bytes, ID + ".other", VERSION,
                TrustedEditorialEngineProfileCatalog.EXPECTED_CANONICAL_PROFILE_HASH,
                TrustedEditorialEngineProfileCatalog.EXPECTED_MACHINE_CONTRACT_FINGERPRINT);
        RegistryFailure failure = failure(() -> BundledEditorialEngineContractProfileRegistry.loadForTest(
                resourceLoader(PATH, bytes, "duplicate.json", bytes), List.of(first, duplicate)));
        assertEquals(EditorialEngineContractProfileRegistryException.Code.DUPLICATE_CANONICAL_HASH, failure.code);
    }

    @Test
    public void profileOutsideAllowListIsNotLoaded() throws Exception {
        TrackingLoader loader = new TrackingLoader("outside/profile.json", productionBytes());
        RegistryFailure failure = failure(() -> BundledEditorialEngineContractProfileRegistry.loadForTest(
                loader, TrustedEditorialEngineProfileCatalog.production()));
        assertEquals(EditorialEngineContractProfileRegistryException.Code.RESOURCE_MISSING, failure.code);
        assertEquals(Set.of(PATH), loader.requested);
    }

    @Test
    public void catalogAndResourceIdentityMustMatch() throws Exception {
        byte[] bytes = productionBytes();
        TrustedEditorialEngineProfileCatalog.TrustAnchor anchor = anchor(PATH, bytes, ID + ".wrong", VERSION,
                TrustedEditorialEngineProfileCatalog.EXPECTED_CANONICAL_PROFILE_HASH,
                TrustedEditorialEngineProfileCatalog.EXPECTED_MACHINE_CONTRACT_FINGERPRINT);
        RegistryFailure failure = failure(() -> BundledEditorialEngineContractProfileRegistry.loadForTest(
                resourceLoader(PATH, bytes), List.of(anchor)));
        assertEquals(EditorialEngineContractProfileRegistryException.Code.PROFILE_ID_MISMATCH, failure.code);
    }

    @Test
    public void testFixtureNamespaceIsNotInProductionRegistry() {
        BundledEditorialEngineContractProfileRegistry registry =
                BundledEditorialEngineContractProfileRegistry.load();
        assertEquals(1, registry.list().size());
        assertFalse(registry.list().get(0).engineProfileId().contains(".test"));
        assertFalse(registry.list().get(0).implementedCapabilities().stream().anyMatch(id -> id.startsWith("test.")));
    }

    @Test
    public void publicRegistryHasNoMutationOrActivationApi() {
        Set<String> forbidden = Set.of("add", "importProfile", "update", "replace", "delete", "remove",
                "certify", "activate", "setDefault", "bindProject", "execute");
        for (Method method : BundledEditorialEngineContractProfileRegistry.class.getMethods()) {
            assertFalse("forbidden registry API: " + method.getName(), forbidden.contains(method.getName()));
        }
        for (Method method : EditorialEngineContractProfileRegistry.class.getMethods()) {
            assertFalse("forbidden interface API: " + method.getName(), forbidden.contains(method.getName()));
        }
    }

    private static byte[] productionBytes() throws Exception {
        try (InputStream input = BundledEditorialEngineContractProfileRegistry.class.getClassLoader()
                .getResourceAsStream(PATH)) {
            assertNotNull(input);
            return input.readAllBytes();
        }
    }

    private static TrustedEditorialEngineProfileCatalog.TrustAnchor anchor(
            String path, byte[] bytes, String id, String version, String canonical, String machine) {
        return new TrustedEditorialEngineProfileCatalog.TrustAnchor(path, id, version,
                EditorialCanonicalJson.sha256Hex(bytes), canonical, machine);
    }

    private static ClassLoader resourceLoader(String path1, byte[] bytes1, String path2, byte[] bytes2) {
        TrackingLoader loader = new TrackingLoader();
        loader.resources.put(path1, bytes1);
        loader.resources.put(path2, bytes2);
        return loader;
    }

    private static ClassLoader resourceLoader(String path, byte[] bytes) {
        return new TrackingLoader(path, bytes);
    }

    private static RegistryFailure failure(ThrowingAction action) {
        try {
            action.run();
        } catch (EditorialEngineContractProfileRegistryException e) {
            return new RegistryFailure(e.code());
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception", e);
        }
        throw new AssertionError("Expected registry failure");
    }

    private interface ThrowingAction {
        void run() throws Exception;
    }

    private record RegistryFailure(EditorialEngineContractProfileRegistryException.Code code) {}

    private static final class TrackingLoader extends ClassLoader {
        private final java.util.Map<String, byte[]> resources = new java.util.HashMap<>();
        private final Set<String> requested = new HashSet<>();

        private TrackingLoader() {
            super(null);
        }

        private TrackingLoader(String path, byte[] bytes) {
            this();
            resources.put(path, bytes);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            requested.add(name);
            byte[] bytes = resources.get(name);
            return bytes == null ? null : new ByteArrayInputStream(bytes);
        }
    }

    private static void assertThrows(Class<? extends Throwable> type, ThrowingAction action) {
        try {
            action.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Unexpected exception type: " + error.getClass().getName(), error);
        }
        throw new AssertionError("Expected " + type.getName());
    }
}
