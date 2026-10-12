package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EditorialEngineProfileResolverTest {
    @Test public void unsupportedLegacyFixtureFailsClosedWhileBootstrapRemainsNonExecutable() {
        EditorialCompatibilityEvaluationResult result = resolver(BundledEditorialEngineContractProfileRegistry.load())
                .resolve(EditorialPackFixtures.valid().manifest());

        assertEquals(EditorialPackCompatibilityClass.BLOCKED, result.outcome());
        assertEquals(EditorialCompatibilityReasonCode.UNSUPPORTED_CONTRACT, result.reasonCode());
        assertTrue(result.blocked());
        assertFalse(result.trustedProfile().isPresent());
        EditorialEngineContractProfile bootstrap = BundledEditorialEngineContractProfileRegistry.load()
                .findByIdentity(TrustedEditorialEngineProfileCatalog.PRODUCTION_PROFILE_ID,
                        TrustedEditorialEngineProfileCatalog.PRODUCTION_PROFILE_VERSION).orElseThrow();
        assertFalse(EditorialEngineProfileResolver.isExecutableContractProfile(bootstrap));
    }

    @Test public void canonicalSafe4ManifestMatchesQualifiedProfileButDoesNotOpenExecution() throws IOException {
        EditorialPackManifest manifest;
        try (InputStream input = getClass().getResourceAsStream("/editorial-p2/editorial-pack.json")) {
            if (input == null) throw new IOException("P2 manifest resource is missing");
            manifest = EditorialPackManifest.parse(input.readAllBytes());
        }
        EditorialCompatibilityEvaluationResult result = resolver(BundledEditorialEngineContractProfileRegistry.load())
                .resolve(manifest);
        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, result.outcome());
        assertEquals(EditorialCompatibilityReasonCode.DATA_COMPATIBLE, result.reasonCode());
        assertFalse(result.blocked());
        assertEquals(TrustedEditorialEngineProfileCatalog.SAFE4_PROFILE_ID,
                result.trustedProfile().orElseThrow().engineProfileId());
        assertTrue(EditorialEngineProfileResolver.isExecutableContractProfile(result.trustedProfile().orElseThrow()));
    }

    @Test public void adapterMapsEvaluatorFactsAndDoesNotUseProfileMetadata() {
        EditorialEngineContractProfile source = BundledEditorialEngineContractProfileRegistry.load().list().get(0);
        EditorialPackManifest manifest = EditorialPackFixtures.valid().manifest();
        EditorialEngineContractProfile executable = executableCopy(source, "profile.one", "2.0.0", "2026-08-05T00:00:00Z", "9".repeat(40));

        EditorialEngineProfile facts = new EditorialEngineProfileAdapter().adapt(executable, manifest);
        assertEquals(executable.engineVersion(), facts.engineVersion());
        assertEquals(new java.util.HashSet<>(executable.implementedCapabilities()), facts.capabilities());
        assertEquals(1, facts.contracts().size());
        EditorialEngineProfile.ContractSupport support = facts.contracts().values().iterator().next();
        assertEquals(manifest.contractVersion(), support.contractVersion());
        assertEquals(manifest.schemaVersion(), support.schemaVersion());
        assertEquals(executable.machineContractFingerprint(), support.machineContractFingerprint());
        assertEquals(new java.util.HashSet<>(executable.implementedCapabilities()), support.supportedCapabilities());
        assertTrue(facts.adapters().isEmpty());
    }

    @Test public void registryHashMismatchFailsClosedWithStableReason() {
        EditorialEngineContractProfile source = BundledEditorialEngineContractProfileRegistry.load().list().get(0);
        EditorialEngineContractProfile tampered = copy(source, source.engineProfileId(), source.engineProfileVersion(),
                source.machineContractFingerprint(), "0".repeat(64), source.createdAt(), source.buildSourceCommit());
        EditorialCompatibilityEvaluationResult result = resolver(registryOf(List.of(tampered)))
                .resolve(EditorialPackFixtures.valid().manifest());

        assertEquals(EditorialCompatibilityReasonCode.PROFILE_HASH_MISMATCH, result.reasonCode());
        assertEquals(EditorialPackCompatibilityClass.BLOCKED, result.outcome());
        assertTrue(result.blocked());
    }

    @Test public void registryMachineFingerprintMismatchFailsClosedWithStableReason() {
        EditorialEngineContractProfile source = BundledEditorialEngineContractProfileRegistry.load().list().get(0);
        EditorialEngineContractProfile machineChanged = copy(source, source.engineProfileId(), source.engineProfileVersion(),
                "1".repeat(64), "0".repeat(64), source.createdAt(), source.buildSourceCommit());
        EditorialEngineContractProfile tampered = copy(source, source.engineProfileId(), source.engineProfileVersion(),
                machineChanged.machineContractFingerprint(),
                EditorialEngineContractProfileCanonicalizer.canonicalProfileHash(machineChanged),
                source.createdAt(), source.buildSourceCommit());
        EditorialCompatibilityEvaluationResult result = resolver(registryOf(List.of(tampered)))
                .resolve(EditorialPackFixtures.valid().manifest());

        assertEquals(EditorialCompatibilityReasonCode.MACHINE_FINGERPRINT_MISMATCH, result.reasonCode());
        assertEquals(EditorialPackCompatibilityClass.BLOCKED, result.outcome());
    }

    @Test public void executableTestProfileRejectsUnsupportedContractWithoutFallback() {
        EditorialEngineContractProfile source = BundledEditorialEngineContractProfileRegistry.load().list().get(0);
        EditorialEngineContractProfile executable = validExecutableCopy(source, "profile.unsupported", "2.0.0",
                "2026-08-05T00:00:00Z", "9".repeat(40));
        Map<String, Object> declarations = new java.util.LinkedHashMap<>(EditorialPackFixtures.valid().manifest().declarations());
        declarations.put("contractVersion", "contract.not-supported.v1");
        declarations.put("canonicalPackHash", "0".repeat(64));
        declarations.put("canonicalPackHash", EditorialPackFixtures.canonicalHashWithout(declarations));
        EditorialPackManifest unsupported = EditorialPackManifest.parse(
                EditorialCanonicalJson.canonicalize(declarations).getBytes(java.nio.charset.StandardCharsets.UTF_8));

        EditorialCompatibilityEvaluationResult result = resolver(registryOf(List.of(executable))).resolve(unsupported);

        assertEquals(EditorialPackCompatibilityClass.BLOCKED, result.outcome());
        assertEquals(EditorialCompatibilityReasonCode.UNSUPPORTED_CONTRACT, result.reasonCode());
        assertTrue(result.blocked());
        assertFalse(result.trustedProfile().isPresent());
    }

    @Test public void duplicateTrustedIdentityFailsClosed() {
        EditorialEngineContractProfile source = BundledEditorialEngineContractProfileRegistry.load().list().get(0);
        EditorialCompatibilityEvaluationResult result = resolver(registryOf(List.of(source, source)))
                .resolve(EditorialPackFixtures.valid().manifest());

        assertEquals(EditorialCompatibilityReasonCode.TRUSTED_REGISTRY_INVALID, result.reasonCode());
        assertTrue(result.blocked());
    }

    @Test public void resultMissingCapabilitiesAndIssuesAreImmutableAndDeterministic() {
        EditorialCompatibilityEvaluationResult result = new EditorialCompatibilityEvaluationResult(
                EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED,
                EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED,
                EditorialCompatibilityReasonCode.MISSING_ENGINE_CAPABILITY,
                true,
                List.of(new EditorialPackIntegrityResult.Issue(EditorialPackValidationCode.MISSING_CAPABILITY,
                        "requiredCapabilities", "missing")),
                new java.util.HashSet<>(List.of("z.capability", "a.capability")),
                "pack-hash", null, null);

        assertEquals(List.of("a.capability", "z.capability"), List.copyOf(result.missingCapabilities()));
        try { result.missingCapabilities().add("mutation"); throw new AssertionError("mutable result"); }
        catch (UnsupportedOperationException expected) { }
        try { result.issues().clear(); throw new AssertionError("mutable issues"); }
        catch (UnsupportedOperationException expected) { }
    }

    private static EditorialEngineProfileResolver resolver(EditorialEngineContractProfileRegistry registry) {
        return new EditorialEngineProfileResolver(registry);
    }

    private static EditorialEngineContractProfileRegistry registryOf(List<EditorialEngineContractProfile> profiles) {
        return new EditorialEngineContractProfileRegistry() {
            @Override public List<EditorialEngineContractProfile> list() { return profiles; }
            @Override public java.util.Optional<EditorialEngineContractProfile> findByCanonicalHash(String hash) { return profiles.stream().filter(p -> p.canonicalProfileHash().equals(hash)).findFirst(); }
            @Override public java.util.Optional<EditorialEngineContractProfile> findByIdentity(String id, String version) { return profiles.stream().filter(p -> p.engineProfileId().equals(id) && p.engineProfileVersion().equals(version)).findFirst(); }
        };
    }

    private static EditorialEngineContractProfile executableCopy(EditorialEngineContractProfile source,
                                                                  String id, String version,
                                                                  String createdAt, String buildCommit) {
        return new EditorialEngineContractProfile(
                source.profileFormat(), source.profileFormatVersion(), id, version, source.engineVersion(),
                "contract.test.v1", "contract.test.v1", List.of("evidence.test.v1"), List.of(),
                new EditorialEngineContractProfile.PhaseGraph(List.of("L1"), List.of()),
                Map.of("L1", new EditorialEngineContractProfile.ContextAllowList(List.of(), List.of())),
                source.evidenceSchemaFingerprints(), source.gateDefinitionFingerprints(), source.releaseArtifactFingerprints(),
                source.implementedCapabilities(), source.explicitlyMissingCapabilities(), source.capabilityEvidence(),
                source.bundledAdapterIds(), source.adapterDescriptors(), source.machineContractFingerprint(),
                source.canonicalProfileHash(), createdAt, buildCommit, source.deprecationPolicy());
    }

    private static EditorialEngineContractProfile validExecutableCopy(EditorialEngineContractProfile source,
                                                                        String id, String version,
                                                                        String createdAt, String buildCommit) {
        EditorialEngineContractProfile draft = new EditorialEngineContractProfile(
                source.profileFormat(), source.profileFormatVersion(), id, version, source.engineVersion(),
                "contract.test.v1", "contract.test.v1", List.of("evidence.test.v1"), List.of(),
                new EditorialEngineContractProfile.PhaseGraph(List.of("L1"), List.of()),
                Map.of("L1", new EditorialEngineContractProfile.ContextAllowList(List.of(), List.of())),
                source.evidenceSchemaFingerprints(), source.gateDefinitionFingerprints(), source.releaseArtifactFingerprints(),
                source.implementedCapabilities(), source.explicitlyMissingCapabilities(), source.capabilityEvidence(),
                source.bundledAdapterIds(), source.adapterDescriptors(), "0".repeat(64), "0".repeat(64),
                createdAt, buildCommit, source.deprecationPolicy());
        String machine = EditorialEngineContractProfileCanonicalizer.machineContractFingerprint(draft);
        EditorialEngineContractProfile withMachine = new EditorialEngineContractProfile(
                draft.profileFormat(), draft.profileFormatVersion(), draft.engineProfileId(), draft.engineProfileVersion(), draft.engineVersion(),
                draft.minimumSupportedContractVersion(), draft.maximumSupportedContractVersion(), draft.supportedSchemaVersions(),
                draft.supportedInputRoles(), draft.supportedPhaseGraph(), draft.contextAllowListByPhase(),
                draft.evidenceSchemaFingerprints(), draft.gateDefinitionFingerprints(), draft.releaseArtifactFingerprints(),
                draft.implementedCapabilities(), draft.explicitlyMissingCapabilities(), draft.capabilityEvidence(),
                draft.bundledAdapterIds(), draft.adapterDescriptors(), machine, "0".repeat(64), draft.createdAt(), draft.buildSourceCommit(), draft.deprecationPolicy());
        String hash = EditorialEngineContractProfileCanonicalizer.canonicalProfileHash(withMachine);
        return new EditorialEngineContractProfile(
                withMachine.profileFormat(), withMachine.profileFormatVersion(), withMachine.engineProfileId(), withMachine.engineProfileVersion(), withMachine.engineVersion(),
                withMachine.minimumSupportedContractVersion(), withMachine.maximumSupportedContractVersion(), withMachine.supportedSchemaVersions(),
                withMachine.supportedInputRoles(), withMachine.supportedPhaseGraph(), withMachine.contextAllowListByPhase(),
                withMachine.evidenceSchemaFingerprints(), withMachine.gateDefinitionFingerprints(), withMachine.releaseArtifactFingerprints(),
                withMachine.implementedCapabilities(), withMachine.explicitlyMissingCapabilities(), withMachine.capabilityEvidence(),
                withMachine.bundledAdapterIds(), withMachine.adapterDescriptors(), machine, hash, withMachine.createdAt(), withMachine.buildSourceCommit(), withMachine.deprecationPolicy());
    }

    private static EditorialEngineContractProfile copy(EditorialEngineContractProfile source,
                                                       String id, String version, String machine, String hash,
                                                       String createdAt, String buildCommit) {
        return new EditorialEngineContractProfile(
                source.profileFormat(), source.profileFormatVersion(), id, version, source.engineVersion(),
                source.minimumSupportedContractVersion(), source.maximumSupportedContractVersion(), source.supportedSchemaVersions(),
                source.supportedInputRoles(), source.supportedPhaseGraph(), source.contextAllowListByPhase(),
                source.evidenceSchemaFingerprints(), source.gateDefinitionFingerprints(), source.releaseArtifactFingerprints(),
                source.implementedCapabilities(), source.explicitlyMissingCapabilities(), source.capabilityEvidence(),
                source.bundledAdapterIds(), source.adapterDescriptors(), machine, hash, createdAt, buildCommit,
                source.deprecationPolicy());
    }
}
