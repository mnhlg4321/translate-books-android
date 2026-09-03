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
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Trusted-path acceptance and adversarial profile evidence checks for P3B. */
public class EditorialP3BTrustedProfileAcceptanceTest {
    private static final String P2_MANIFEST = "/editorial-p2/editorial-pack.json";

    @Test
    public void canonical413AndSynthetic414UseTheTrustedProfileWithoutOpeningExecution() throws IOException {
        EditorialEngineProfileResolver resolver = new EditorialEngineProfileResolver(
                BundledEditorialEngineContractProfileRegistry.load());
        EditorialPackManifest canonical = p2Manifest();
        EditorialCompatibilityEvaluationResult first = resolver.resolve(canonical);

        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, first.outcome());
        assertEquals(EditorialCompatibilityReasonCode.DATA_COMPATIBLE, first.reasonCode());
        assertEquals(TrustedEditorialEngineProfileCatalog.SAFE4_PROFILE_ID,
                first.trustedProfile().orElseThrow().engineProfileId());
        assertTrue(first.missingCapabilities().isEmpty());
        assertTrue(EditorialEngineProfileResolver.isExecutableContractProfile(
                BundledEditorialEngineContractProfileRegistry.load().findByIdentity(
                        TrustedEditorialEngineProfileCatalog.SAFE4_PROFILE_ID,
                        TrustedEditorialEngineProfileCatalog.SAFE4_PROFILE_VERSION).orElseThrow()));

        EditorialPackManifest future = variant(canonical, root -> {
            root.put("packId", "com.ml.tblandroidtxt.editorial.safe4.full.synthetic414");
            root.put("version", "4.1.4");
            root.put("displayName", "V5-SAFE.4.1.4 synthetic");
            root.put("createdAt", "2026-09-03T00:02:00+07:00");
            byte[] changedPrompt = "synthetic 4.1.4 authority\n".getBytes(StandardCharsets.UTF_8);
            for (Object value : list(root.get("fileRoles"))) {
                Map<String, Object> file = map(value);
                if ("prompt.txt".equals(file.get("path"))) {
                    file.put("byteLength", BigDecimal.valueOf(changedPrompt.length));
                    file.put("sha256", EditorialCanonicalJson.sha256Hex(changedPrompt));
                }
            }
        });
        EditorialCompatibilityEvaluationResult second = resolver.resolve(future);
        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, second.outcome());
        assertEquals(EditorialCompatibilityReasonCode.DATA_COMPATIBLE, second.reasonCode());
        assertEquals(TrustedEditorialEngineProfileCatalog.SAFE4_PROFILE_ID,
                second.trustedProfile().orElseThrow().engineProfileId());
        assertTrue(second.missingCapabilities().isEmpty());
    }

    @Test
    public void missingEvidenceInvalidatesSafe4ProfileAndTrustedResolverFailsClosed() throws IOException {
        EditorialEngineContractProfile source = BundledEditorialEngineContractProfileRegistry.load()
                .findByIdentity(TrustedEditorialEngineProfileCatalog.SAFE4_PROFILE_ID,
                        TrustedEditorialEngineProfileCatalog.SAFE4_PROFILE_VERSION).orElseThrow();
        ArrayList<EditorialEngineContractProfile.CapabilityEvidence> missingOne =
                new ArrayList<>(source.capabilityEvidence());
        missingOne.remove(0);
        EditorialEngineContractProfile withoutEvidence = copy(source, missingOne,
                source.machineContractFingerprint(), "0".repeat(64));
        withoutEvidence = copy(withoutEvidence, missingOne, withoutEvidence.machineContractFingerprint(),
                EditorialEngineContractProfileCanonicalizer.canonicalProfileHash(withoutEvidence));

        EditorialEngineContractProfileValidationResult validation =
                new EditorialEngineContractProfileValidator().validate(withoutEvidence);
        assertFalse(validation.isValid());
        assertTrue(validation.issues().stream().anyMatch(issue ->
                issue.code() == EditorialEngineContractProfileValidationResult.Code.MISSING_CAPABILITY_EVIDENCE
                        || issue.code() == EditorialEngineContractProfileValidationResult.Code.INVALID_DESCRIPTOR));

        EditorialCompatibilityEvaluationResult result = new EditorialEngineProfileResolver(
                registryOf(List.of(withoutEvidence))).resolve(p2Manifest());
        assertEquals(EditorialPackCompatibilityClass.BLOCKED, result.outcome());
        assertEquals(EditorialCompatibilityReasonCode.TRUSTED_REGISTRY_INVALID, result.reasonCode());
        assertTrue(result.blocked());
    }

    @Test
    public void modelIndependentProfileHashAndMachineTamperAreRejected() throws IOException {
        EditorialEngineContractProfile source = BundledEditorialEngineContractProfileRegistry.load()
                .findByIdentity(TrustedEditorialEngineProfileCatalog.SAFE4_PROFILE_ID,
                        TrustedEditorialEngineProfileCatalog.SAFE4_PROFILE_VERSION).orElseThrow();
        EditorialEngineContractProfile wrongMachine = copy(source, source.capabilityEvidence(),
                "1".repeat(64), "0".repeat(64));
        wrongMachine = copy(wrongMachine, wrongMachine.capabilityEvidence(),
                wrongMachine.machineContractFingerprint(),
                EditorialEngineContractProfileCanonicalizer.canonicalProfileHash(wrongMachine));

        EditorialCompatibilityEvaluationResult result = new EditorialEngineProfileResolver(
                registryOf(List.of(wrongMachine))).resolve(p2Manifest());
        assertEquals(EditorialPackCompatibilityClass.BLOCKED, result.outcome());
        assertEquals(EditorialCompatibilityReasonCode.MACHINE_FINGERPRINT_MISMATCH, result.reasonCode());
        assertTrue(result.blocked());
    }

    private static EditorialPackManifest p2Manifest() throws IOException {
        try (InputStream input = EditorialP3BTrustedProfileAcceptanceTest.class
                .getResourceAsStream(P2_MANIFEST)) {
            if (input == null) throw new IOException("P2 manifest is missing");
            return EditorialPackManifest.parse(input.readAllBytes());
        }
    }

    private static EditorialEngineContractProfileRegistry registryOf(List<EditorialEngineContractProfile> profiles) {
        return new EditorialEngineContractProfileRegistry() {
            @Override public List<EditorialEngineContractProfile> list() { return profiles; }
            @Override public Optional<EditorialEngineContractProfile> findByCanonicalHash(String hash) {
                return profiles.stream().filter(profile -> profile.canonicalProfileHash().equals(hash)).findFirst();
            }
            @Override public Optional<EditorialEngineContractProfile> findByIdentity(String id, String version) {
                return profiles.stream().filter(profile -> profile.engineProfileId().equals(id)
                        && profile.engineProfileVersion().equals(version)).findFirst();
            }
        };
    }

    private static EditorialEngineContractProfile copy(EditorialEngineContractProfile source,
                                                       List<EditorialEngineContractProfile.CapabilityEvidence> evidence,
                                                       String machine, String hash) {
        return new EditorialEngineContractProfile(
                source.profileFormat(), source.profileFormatVersion(), source.engineProfileId(),
                source.engineProfileVersion(), source.engineVersion(), source.minimumSupportedContractVersion(),
                source.maximumSupportedContractVersion(), source.supportedSchemaVersions(), source.supportedInputRoles(),
                source.supportedPhaseGraph(), source.contextAllowListByPhase(), source.evidenceSchemaFingerprints(),
                source.gateDefinitionFingerprints(), source.releaseArtifactFingerprints(), source.implementedCapabilities(),
                source.explicitlyMissingCapabilities(), evidence, source.bundledAdapterIds(), source.adapterDescriptors(),
                machine, hash, source.createdAt(), source.buildSourceCommit(), source.deprecationPolicy());
    }

    private static EditorialPackManifest variant(EditorialPackManifest original,
                                                 Consumer<Map<String, Object>> mutation) {
        Map<String, Object> root = map(mutable(original.declarations()));
        mutation.accept(root);
        root.put("canonicalPackHash", "0".repeat(64));
        root.put("canonicalPackHash", EditorialPackFixtures.canonicalHashWithout(root));
        return EditorialPackManifest.parse(EditorialCanonicalJson.canonicalize(root)
                .getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) { return (Map<String, Object>) value; }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) { return (List<Object>) value; }

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
