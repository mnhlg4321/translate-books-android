package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** P1 compatibility matrix and future-pack characterization. */
public class EditorialP1CompatibilityCharacterizationTest {
    private static final String FIXTURE_RESOURCE = "/editorial-p1/editorial-pack.json";
    private final EditorialCompatibilityEvaluator evaluator = new EditorialCompatibilityEvaluator();

    @Test public void supportedContractAndCapabilitiesAreDataCompatible() throws IOException {
        EditorialPackManifest manifest = fixtureManifest();
        EditorialCompatibilityResult result = evaluator.evaluate(manifest, exactProfile(manifest));

        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, result.classification());
        assertFalse(result.blocked());
        assertTrue(result.usableWithoutApk());
        assertTrue(result.missingCapabilities().isEmpty());
    }

    @Test public void matrixSeparatesAdapterEngineUpgradeAndInvalidDeclarations() throws IOException {
        EditorialPackManifest base = fixtureManifest();
        EditorialPackManifest changed = variant(base, root -> {
            Map<String, Object> graph = map(root.get("phaseGraph"));
            graph.put("profile", "safe4.full.three-pass.v2");
            root.put("compatibilityClass", "ADAPTER_REQUIRED");
        });

        EditorialEngineProfile adapterProfile = new EditorialEngineProfile(
                "4.17.0",
                base.requiredCapabilities(),
                List.of(new EditorialEngineProfile.ContractSupport(
                        changed.contractVersion(), changed.schemaVersion(), base.machineContractFingerprint(), Set.of())),
                List.of(new EditorialEngineProfile.AdapterSupport(
                        "safe4.full.adapter.v1", changed.contractVersion(), changed.schemaVersion(),
                        changed.machineContractFingerprint(), Set.of(), false)));
        EditorialCompatibilityResult adapter = evaluator.evaluate(changed, adapterProfile);
        assertEquals(EditorialPackCompatibilityClass.ADAPTER_REQUIRED, adapter.classification());
        assertTrue(adapter.blocked());
        assertEquals(EditorialPackValidationCode.ADAPTER_REQUIRED, adapter.primaryCode());

        EditorialPackManifest upgradeManifest = variant(base, root -> {
            Map<String, Object> graph = map(root.get("phaseGraph"));
            graph.put("profile", "safe4.full.three-pass.v2");
            root.put("compatibilityClass", "ENGINE_UPGRADE_REQUIRED");
        });
        EditorialEngineProfile noAdapterProfile = new EditorialEngineProfile(
                "4.17.0", base.requiredCapabilities(),
                List.of(new EditorialEngineProfile.ContractSupport(
                        upgradeManifest.contractVersion(), upgradeManifest.schemaVersion(),
                        base.machineContractFingerprint(), Set.of())), List.of());
        EditorialCompatibilityResult upgrade = evaluator.evaluate(upgradeManifest, noAdapterProfile);
        assertEquals(EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, upgrade.classification());
        assertTrue(upgrade.blocked());
        assertEquals(EditorialPackValidationCode.ENGINE_UPGRADE_REQUIRED, upgrade.primaryCode());

        EditorialPackManifest dishonest = variant(base, root -> root.put("compatibilityClass", "ENGINE_UPGRADE_REQUIRED"));
        EditorialCompatibilityResult invalid = evaluator.evaluate(dishonest, exactProfile(dishonest));
        assertEquals(EditorialPackCompatibilityClass.INVALID, invalid.classification());
        assertEquals(EditorialPackValidationCode.DECLARED_CLASS_MISMATCH, invalid.primaryCode());
    }

    @Test public void syntheticCompatible414ChangesOnlyIdentityAndAuthorityBytes() throws IOException {
        EditorialPackManifest base = fixtureManifest();
        byte[] changedPrompt = "Synthetic 4.1.4 authority bytes\n".getBytes(StandardCharsets.UTF_8);
        EditorialPackManifest future = variant(base, root -> {
            root.put("packId", "com.ml.tblandroidtxt.editorial.safe4.full.next");
            root.put("version", "4.1.4");
            root.put("displayName", "Biên tập V5-SAFE.4.1.4-FULL (synthetic)");
            root.put("createdAt", "2026-09-03T00:01:00+07:00");
            List<Object> files = list(root.get("fileRoles"));
            for (Object value : files) {
                Map<String, Object> file = map(value);
                if ("prompt.txt".equals(file.get("path"))) {
                    file.put("byteLength", BigDecimal.valueOf(changedPrompt.length));
                    file.put("sha256", EditorialCanonicalJson.sha256Hex(changedPrompt));
                }
            }
        });

        assertEquals(base.contractVersion(), future.contractVersion());
        assertEquals(base.schemaVersion(), future.schemaVersion());
        assertEquals(base.machineContractFingerprint(), future.machineContractFingerprint());
        assertNotEquals(base.packId(), future.packId());
        assertNotEquals(base.version(), future.version());
        assertNotEquals(base.canonicalPackHash(), future.canonicalPackHash());

        EditorialCompatibilityResult result = evaluator.evaluate(future, exactProfile(base));
        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, result.classification());
        assertFalse(result.blocked());
    }

    @Test public void unknownRequiredCapabilityIsEngineUpgradeInTrustedResolverButLegacySeamDiffers() throws IOException {
        EditorialPackManifest base = fixtureManifest();
        EditorialPackManifest unknown = variant(base, root -> {
            List<Object> capabilities = list(root.get("requiredCapabilities"));
            capabilities.add("future.required.capability.v1");
        });

        // The legacy evaluator seam still reports a generic BLOCKED result for
        // a missing capability. This is evidence about that seam, not a claim
        // that production compatibility is currently correct.
        EditorialCompatibilityResult legacy = evaluator.evaluate(unknown,
                profileFor(unknown, base.requiredCapabilities()));
        assertEquals(EditorialPackCompatibilityClass.BLOCKED, legacy.classification());
        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, legacy.requiredClass());
        assertTrue(legacy.missingCapabilities().contains("future.required.capability.v1"));

        // The production trusted resolver normalizes missing required behavior
        // to the fail-closed, user-visible upgrade result.
        EditorialCompatibilityEvaluationResult trusted = new EditorialEngineProfileResolver(
                BundledEditorialEngineContractProfileRegistry.load()).resolve(unknown);
        assertEquals(EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, trusted.outcome());
        assertTrue(trusted.blocked());
        assertTrue(trusted.missingCapabilities().contains("future.required.capability.v1"));
        assertEquals(EditorialCompatibilityReasonCode.MISSING_ENGINE_CAPABILITY, trusted.reasonCode());
    }

    @Test public void trustedProfileMakesFixtureDataCompatibleButDoesNotCertifyExecution() throws IOException {
        EditorialPackManifest manifest = fixtureManifest();
        EditorialCompatibilityEvaluationResult result = new EditorialEngineProfileResolver(
                BundledEditorialEngineContractProfileRegistry.load()).resolve(manifest);

        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, result.outcome());
        assertEquals(EditorialCompatibilityReasonCode.DATA_COMPATIBLE, result.reasonCode());
        assertFalse(result.blocked());
        assertTrue(result.trustedProfile().isPresent());
        assertTrue(EditorialEngineProfileResolver.isExecutableContractProfile(
                result.trustedProfile().orElseThrow()));
        assertTrue(result.missingCapabilities().isEmpty());
    }

    private static EditorialEngineProfile exactProfile(EditorialPackManifest manifest) {
        return profileFor(manifest, manifest.requiredCapabilities());
    }

    private static EditorialEngineProfile profileFor(EditorialPackManifest manifest,
                                                     Set<String> installedCapabilities) {
        return new EditorialEngineProfile("4.17.0", installedCapabilities,
                List.of(new EditorialEngineProfile.ContractSupport(
                        manifest.contractVersion(), manifest.schemaVersion(),
                        manifest.machineContractFingerprint(), Set.of())), List.of());
    }

    private static EditorialPackManifest fixtureManifest() throws IOException {
        try (InputStream input = EditorialP1CompatibilityCharacterizationTest.class
                .getResourceAsStream(FIXTURE_RESOURCE)) {
            if (input == null) throw new IOException("P1 manifest fixture is missing: " + FIXTURE_RESOURCE);
            return EditorialPackManifest.parse(input.readAllBytes());
        }
    }

    private static EditorialPackManifest variant(EditorialPackManifest original,
                                                 Consumer<Map<String, Object>> mutation) {
        Map<String, Object> root = mutableMap(original.declarations());
        mutation.accept(root);
        root.put("canonicalPackHash", "0".repeat(64));
        root.put("canonicalPackHash", EditorialPackFixtures.canonicalHashWithout(root));
        byte[] bytes = EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8);
        return EditorialPackManifest.parse(bytes);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return (List<Object>) value;
    }

    private static Map<String, Object> mutableMap(Map<String, Object> original) {
        return map(mutable(original));
    }

    private static Object mutable(Object value) {
        if (value instanceof Map<?, ?> source) {
            LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                copy.put((String) entry.getKey(), mutable(entry.getValue()));
            }
            return copy;
        }
        if (value instanceof List<?> source) {
            ArrayList<Object> copy = new ArrayList<>(source.size());
            for (Object item : source) copy.add(mutable(item));
            return copy;
        }
        return value;
    }
}
