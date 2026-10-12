package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EditorialEngineContractProfileTest {
    private static final String SOURCE_COMMIT = "f62ea5b35a183071374498417b7ed4eef5637f33";
    private static final String ZERO_HASH = "0000000000000000000000000000000000000000000000000000000000000000";
    private final EditorialEngineContractProfileParser parser = new EditorialEngineContractProfileParser();
    private final EditorialEngineContractProfileValidator validator = new EditorialEngineContractProfileValidator();

    @Test public void validNoContractBaselineIsAcceptedWithOnlyPackIntegrity() {
        EditorialEngineContractProfile profile = profile(false, List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1));
        EditorialEngineContractProfileValidationResult result = validator.validate(
                EditorialEngineContractProfileCanonicalizer.canonicalJson(profile).getBytes(StandardCharsets.UTF_8));
        assertTrue(result.issues().toString(), result.isValid());
        assertEquals(profile.canonicalProfileHash(), result.profile().orElseThrow().canonicalProfileHash());
    }

    @Test public void fieldOrderAndWhitespaceDoNotChangeIdentity() {
        EditorialEngineContractProfile profile = profile(true, List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1));
        Map<String, Object> root = EditorialCanonicalJson.parseObject(
                EditorialEngineContractProfileCanonicalizer.canonicalJson(profile).getBytes(StandardCharsets.UTF_8));
        String reordered = rawObject(root, true, true);
        EditorialEngineContractProfileValidationResult result = validator.validate(reordered.getBytes(StandardCharsets.UTF_8));
        assertTrue(result.issues().toString(), result.isValid());
        assertEquals(profile.canonicalProfileHash(), result.profile().orElseThrow().canonicalProfileHash());
        assertEquals(EditorialEngineContractProfileCanonicalizer.canonicalJson(profile),
                EditorialEngineContractProfileCanonicalizer.canonicalJson(result.profile().orElseThrow()));
    }

    @Test public void localeDoesNotAffectCanonicalHashes() {
        EditorialEngineContractProfile profile = profile(true, List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1));
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            String hash = EditorialEngineContractProfileCanonicalizer.canonicalProfileHash(profile);
            String machine = EditorialEngineContractProfileCanonicalizer.machineContractFingerprint(profile);
            Locale.setDefault(Locale.US);
            assertEquals(hash, EditorialEngineContractProfileCanonicalizer.canonicalProfileHash(profile));
            assertEquals(machine, EditorialEngineContractProfileCanonicalizer.machineContractFingerprint(profile));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test public void roundTripPreservesSemanticIdentityAndCollectionsAreImmutable() {
        EditorialEngineContractProfile profile = profile(true, List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1));
        EditorialEngineContractProfile parsed = parser.parse(
                EditorialEngineContractProfileCanonicalizer.canonicalJson(profile).getBytes(StandardCharsets.UTF_8));
        assertEquals(EditorialEngineContractProfileCanonicalizer.canonicalJson(profile),
                EditorialEngineContractProfileCanonicalizer.canonicalJson(parsed));
        assertThrowsUnsupported(() -> parsed.supportedSchemaVersions().add("schema.other"));
        assertThrowsUnsupported(() -> parsed.supportedPhaseGraph().phases().add("L3"));
        assertThrowsUnsupported(() -> parsed.contextAllowListByPhase().put("L3", new EditorialEngineContractProfile.ContextAllowList(List.of(), List.of())));
    }

    @Test public void bomAndInvalidUtf8AreRejected() {
        assertCode(validator.validate(new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf, '{', '}'}),
                EditorialEngineContractProfileValidationResult.Code.PARSE_ERROR);
        assertCode(validator.validate(new byte[]{(byte) 0xc3, (byte) 0x28}),
                EditorialEngineContractProfileValidationResult.Code.PARSE_ERROR);
    }

    @Test public void duplicateUnknownMissingAndWrongTypeFieldsAreRejected() {
        EditorialEngineContractProfile profile = profile(false, List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1));
        String json = EditorialEngineContractProfileCanonicalizer.canonicalJson(profile);
        assertCode(validator.validate((json.substring(0, json.length() - 1) + ",\"engineVersion\":\"x\"}").getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.PARSE_ERROR);

        Map<String, Object> unknown = root(profile);
        unknown.put("unknownField", true);
        assertCode(validator.validate(rawObject(unknown, false, false).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.UNKNOWN_FIELD);

        Map<String, Object> missing = root(profile);
        missing.remove("engineVersion");
        assertCode(validator.validate(rawObject(missing, false, false).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.MISSING_FIELD);

        Map<String, Object> wrongType = root(profile);
        wrongType.put("profileFormatVersion", "1");
        assertCode(validator.validate(rawObject(wrongType, false, false).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.WRONG_TYPE);
    }

    @Test public void hashAndMachineFingerprintAreRecomputed() {
        EditorialEngineContractProfile profile = profile(false, List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1));
        Map<String, Object> badHash = root(profile);
        badHash.put("canonicalProfileHash", ZERO_HASH);
        assertCode(validator.validate(rawObject(badHash, false, false).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.CANONICAL_HASH_MISMATCH);

        Map<String, Object> badMachine = root(profile);
        badMachine.put("machineContractFingerprint", ZERO_HASH);
        assertCode(validator.validate(rawObject(badMachine, false, false).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.MACHINE_FINGERPRINT_MISMATCH);
    }

    @Test public void duplicateCapabilitiesOverlapAndUnknownCapabilityAreRejected() {
        EditorialEngineContractProfile profile = profile(false, List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1));
        Map<String, Object> duplicate = root(profile);
        duplicate.put("implementedCapabilities", List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1,
                EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1));
        assertCode(validator.validate(rehash(duplicate).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.DUPLICATE_VALUE);

        Map<String, Object> overlap = root(profile);
        overlap.put("explicitlyMissingCapabilities", List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1));
        assertCode(validator.validate(rehash(overlap).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.CAPABILITY_OVERLAP);

        Map<String, Object> unknown = root(profile);
        unknown.put("implementedCapabilities", List.of("safe4.not.real.v1"));
        unknown.put("capabilityEvidence", List.of(Map.of(
                "capabilityId", "safe4.not.real.v1",
                "sourceCommit", SOURCE_COMMIT,
                "evidenceFingerprint", hash("fake"),
                "evidenceClass", "implementation-test")));
        assertCode(validator.validate(rehash(unknown).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.UNKNOWN_CAPABILITY);
    }

    @Test public void implementedCapabilityRequiresEvidenceAndProductionOnlyConfirmsIntegrity() {
        EditorialEngineContractProfile profile = profile(false, List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1));
        Map<String, Object> missingEvidence = root(profile);
        missingEvidence.put("capabilityEvidence", List.of());
        assertCode(validator.validate(rehash(missingEvidence).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.MISSING_CAPABILITY_EVIDENCE);

        List<String> allSafe4 = new ArrayList<>(EditorialEngineContractCapabilityEvidenceCatalog.KNOWN_SAFE4_CAPABILITIES);
        Map<String, Object> allImplemented = root(profile);
        allImplemented.put("implementedCapabilities", allSafe4);
        allImplemented.put("explicitlyMissingCapabilities", List.of());
        assertCode(validator.validate(rehash(allImplemented).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.UNKNOWN_CAPABILITY);
    }

    @Test public void phaseContextAndDescriptorConstraintsAreFailClosed() {
        EditorialEngineContractProfile profile = profile(true, List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1));
        Map<String, Object> duplicatePhase = root(profile);
        Map<String, Object> graph = new LinkedHashMap<>((Map<String, Object>) duplicatePhase.get("supportedPhaseGraph"));
        graph.put("phases", List.of("L1", "L1"));
        duplicatePhase.put("supportedPhaseGraph", graph);
        assertCode(validator.validate(rehash(duplicatePhase).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.DUPLICATE_VALUE);

        Map<String, Object> dangling = root(profile);
        Map<String, Object> danglingGraph = new LinkedHashMap<>((Map<String, Object>) dangling.get("supportedPhaseGraph"));
        danglingGraph.put("edges", List.of(Map.of("from", "L1", "to", "UNKNOWN")));
        dangling.put("supportedPhaseGraph", danglingGraph);
        assertCode(validator.validate(rehash(dangling).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.DANGLING_PHASE);

        Map<String, Object> missingContext = root(profile);
        missingContext.put("contextAllowListByPhase", Map.of("L1", Map.of("requiredRoles", List.of("RAW"), "allowedRoles", List.of("RAW", "Glossary"))));
        assertCode(validator.validate(rehash(missingContext).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.INCOMPLETE_CONTEXT);

        Map<String, Object> badFingerprint = root(profile);
        badFingerprint.put("evidenceSchemaFingerprints", List.of(Map.of("id", "evidence.test.v1", "fingerprint", "bad")));
        assertCode(validator.validate(rehash(badFingerprint).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.INVALID_HASH);
    }

    @Test public void contractBoundsAndDeprecationAreValidated() {
        EditorialEngineContractProfile profile = profile(true, List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1));
        Map<String, Object> bounds = root(profile);
        bounds.put("minimumSupportedContractVersion", null);
        assertCode(validator.validate(rehash(bounds).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.CONTRACT_SCHEMA_MISMATCH);

        Map<String, Object> selfReplacement = root(profile);
        selfReplacement.put("deprecationPolicy", Map.of(
                "state", "DEPRECATED",
                "replacementProfileId", profile.engineProfileId(),
                "replacementVersion", profile.engineProfileVersion(),
                "automaticReplacement", false,
                "automaticProjectRebind", false));
        assertCode(validator.validate(rehash(selfReplacement).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.INVALID_DEPRECATION);

        Map<String, Object> automatic = root(profile);
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("state", "ACTIVE");
        policy.put("replacementProfileId", null);
        policy.put("replacementVersion", null);
        policy.put("automaticReplacement", true);
        policy.put("automaticProjectRebind", false);
        automatic.put("deprecationPolicy", policy);
        assertCode(validator.validate(rehash(automatic).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.INVALID_DEPRECATION);
    }

    @Test public void machineFieldsChangeBothFingerprintsButMetadataOnlyChangesProfileHash() {
        EditorialEngineContractProfile profile = profile(true, List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1));
        Map<String, Object> machineChange = root(profile);
        Map<String, Object> graph = new LinkedHashMap<>((Map<String, Object>) machineChange.get("supportedPhaseGraph"));
        graph.put("phases", List.of("L1", "L2", "L3"));
        machineChange.put("supportedPhaseGraph", graph);
        String changedMachineJson = rehash(machineChange);
        EditorialEngineContractProfile changedMachine = parser.parse(changedMachineJson.getBytes(StandardCharsets.UTF_8));
        assertNotEquals(profile.machineContractFingerprint(), changedMachine.machineContractFingerprint());
        assertNotEquals(profile.canonicalProfileHash(), changedMachine.canonicalProfileHash());

        Map<String, Object> metadataChange = root(profile);
        metadataChange.put("createdAt", "2026-08-05T00:00:00+07:00");
        EditorialEngineContractProfile metadataProfile = parser.parse(rehash(metadataChange).getBytes(StandardCharsets.UTF_8));
        assertEquals(profile.machineContractFingerprint(), metadataProfile.machineContractFingerprint());
        assertNotEquals(profile.canonicalProfileHash(), metadataProfile.canonicalProfileHash());
    }

    @Test public void testNamespaceNeedsExplicitTestCatalog() {
        EditorialEngineContractProfile profile = profile(false, List.of("test.synthetic.v1"));
        Map<String, Object> testRoot = root(profile);
        testRoot.put("explicitlyMissingCapabilities", List.of());
        String testJson = rehash(testRoot);
        assertFalse(validator.validate(testJson.getBytes(StandardCharsets.UTF_8)).isValid());
        EditorialEngineContractProfileValidator testValidator = new EditorialEngineContractProfileValidator(
                EditorialEngineContractCapabilityEvidenceCatalog.testOnly(Set.of("test.synthetic.v1")));
        assertTrue(testValidator.validate(testJson.getBytes(StandardCharsets.UTF_8)).isValid());
    }

    @Test public void profileAndCollectionLimitsAreEnforced() {
        byte[] oversized = new byte[EditorialEngineContractProfileParser.MAX_PROFILE_BYTES + 1];
        assertCode(validator.validate(oversized), EditorialEngineContractProfileValidationResult.Code.SIZE_LIMIT);

        EditorialEngineContractProfile profile = profile(false, List.of(EditorialEngineContractCapabilityEvidenceCatalog.PACK_INTEGRITY_SHA256_V1));
        Map<String, Object> oversizedCollection = root(profile);
        List<String> many = new ArrayList<>();
        for (int i = 0; i < EditorialEngineContractProfileParser.MAX_COLLECTION_SIZE + 1; i++) many.add("schema." + i);
        oversizedCollection.put("supportedSchemaVersions", many);
        assertCode(validator.validate(rawObject(oversizedCollection, false, false).getBytes(StandardCharsets.UTF_8)),
                EditorialEngineContractProfileValidationResult.Code.COLLECTION_LIMIT);
    }

    private static EditorialEngineContractProfile profile(boolean executableContract, List<String> implemented) {
        List<String> missing = new ArrayList<>(EditorialEngineContractCapabilityEvidenceCatalog.KNOWN_SAFE4_CAPABILITIES);
        missing.removeAll(implemented);
        List<EditorialEngineContractProfile.InputRole> roles = executableContract
                ? List.of(new EditorialEngineContractProfile.InputRole("RAW", "ONE", true),
                new EditorialEngineContractProfile.InputRole("Glossary", "OPTIONAL", false))
                : List.of();
        EditorialEngineContractProfile.PhaseGraph graph = executableContract
                ? new EditorialEngineContractProfile.PhaseGraph(List.of("L1", "L2"), List.of(
                new EditorialEngineContractProfile.PhaseEdge("L1", "L2")))
                : new EditorialEngineContractProfile.PhaseGraph(List.of(), List.of());
        Map<String, EditorialEngineContractProfile.ContextAllowList> contexts = new LinkedHashMap<>();
        if (executableContract) {
            contexts.put("L1", new EditorialEngineContractProfile.ContextAllowList(List.of("RAW"), List.of("RAW", "Glossary")));
            contexts.put("L2", new EditorialEngineContractProfile.ContextAllowList(List.of("RAW"), List.of("RAW")));
        }
        List<EditorialEngineContractProfile.CapabilityEvidence> evidence = new ArrayList<>();
        for (String capability : implemented) {
            evidence.add(new EditorialEngineContractProfile.CapabilityEvidence(
                    capability, SOURCE_COMMIT, hash(capability + "-evidence"), "implementation-test"));
        }
        return finalized(new EditorialEngineContractProfile(
                EditorialEngineContractProfile.PROFILE_FORMAT,
                EditorialEngineContractProfile.PROFILE_FORMAT_VERSION,
                "com.ml.tblandroidtxt.editorial.engine.test",
                "1.0.0",
                "4.16-dev.29",
                executableContract ? "1.0" : null,
                executableContract ? "1.0" : null,
                executableContract ? List.of("schema.test.v1") : List.of(),
                roles,
                graph,
                contexts,
                executableContract ? List.of(new EditorialEngineContractProfile.FingerprintDescriptor("evidence.test.v1", hash("evidence"))) : List.of(),
                executableContract ? List.of(new EditorialEngineContractProfile.FingerprintDescriptor("gate.test.v1", hash("gate"))) : List.of(),
                executableContract ? List.of(new EditorialEngineContractProfile.FingerprintDescriptor("release.test.v1", hash("release"))) : List.of(),
                implemented,
                missing,
                evidence,
                List.of(),
                List.of(),
                ZERO_HASH,
                ZERO_HASH,
                "2026-08-04T00:00:00+07:00",
                SOURCE_COMMIT,
                new EditorialEngineContractProfile.DeprecationPolicy("ACTIVE", null, null, false, false)));
    }

    private static EditorialEngineContractProfile finalized(EditorialEngineContractProfile draft) {
        String machine = EditorialEngineContractProfileCanonicalizer.machineContractFingerprint(draft);
        EditorialEngineContractProfile withMachine = copy(draft, machine, ZERO_HASH);
        String hash = EditorialEngineContractProfileCanonicalizer.canonicalProfileHash(withMachine);
        return copy(withMachine, machine, hash);
    }

    private static EditorialEngineContractProfile copy(EditorialEngineContractProfile p, String machine, String hash) {
        return new EditorialEngineContractProfile(
                p.profileFormat(), p.profileFormatVersion(), p.engineProfileId(), p.engineProfileVersion(), p.engineVersion(),
                p.minimumSupportedContractVersion(), p.maximumSupportedContractVersion(), p.supportedSchemaVersions(),
                p.supportedInputRoles(), p.supportedPhaseGraph(), p.contextAllowListByPhase(), p.evidenceSchemaFingerprints(),
                p.gateDefinitionFingerprints(), p.releaseArtifactFingerprints(), p.implementedCapabilities(),
                p.explicitlyMissingCapabilities(), p.capabilityEvidence(), p.bundledAdapterIds(), p.adapterDescriptors(),
                machine, hash, p.createdAt(), p.buildSourceCommit(), p.deprecationPolicy());
    }

    private static Map<String, Object> root(EditorialEngineContractProfile profile) {
        return new LinkedHashMap<>(EditorialCanonicalJson.parseObject(
                EditorialEngineContractProfileCanonicalizer.canonicalJson(profile).getBytes(StandardCharsets.UTF_8)));
    }

    private static String rehash(Map<String, Object> root) {
        // Deliberately recompute both declared values so semantic validation reaches the intended negative case.
        EditorialEngineContractProfile parsed = new EditorialEngineContractProfileParser().parse(
                rawObject(root, false, false).getBytes(StandardCharsets.UTF_8));
        String machine = EditorialEngineContractProfileCanonicalizer.machineContractFingerprint(parsed);
        Map<String, Object> withMachine = new LinkedHashMap<>(root);
        withMachine.put("machineContractFingerprint", machine);
        EditorialEngineContractProfile reparsed = new EditorialEngineContractProfileParser().parse(
                rawObject(withMachine, false, false).getBytes(StandardCharsets.UTF_8));
        withMachine.put("canonicalProfileHash", EditorialEngineContractProfileCanonicalizer.canonicalProfileHash(reparsed));
        return rawObject(withMachine, false, false);
    }

    private static String rawObject(Map<String, Object> root, boolean reverse, boolean whitespace) {
        List<String> keys = new ArrayList<>(root.keySet());
        if (reverse) Collections.reverse(keys);
        StringBuilder out = new StringBuilder("{");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) out.append(whitespace ? ", \n\t" : ",");
            String key = keys.get(i);
            out.append(EditorialCanonicalJson.canonicalize(key)).append(whitespace ? " : " : ":");
            out.append(EditorialCanonicalJson.canonicalize(root.get(key)));
        }
        return out.append('}').toString();
    }

    private static String hash(String value) {
        return EditorialCanonicalJson.sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertCode(EditorialEngineContractProfileValidationResult result,
                                   EditorialEngineContractProfileValidationResult.Code expected) {
        if (!result.hasCode(expected)) fail("Expected " + expected + " but got " + result.issues());
    }

    private static void assertThrowsUnsupported(Runnable action) {
        try {
            action.run();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }
}
