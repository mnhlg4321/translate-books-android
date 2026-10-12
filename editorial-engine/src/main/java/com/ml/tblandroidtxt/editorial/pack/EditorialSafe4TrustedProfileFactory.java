package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** Builds the qualified SAFE4 profile only from code-owned contract descriptors. */
public final class EditorialSafe4TrustedProfileFactory {
    public static final String PROFILE_ID = "com.ml.tblandroidtxt.editorial.engine.safe4.full";
    public static final String PROFILE_VERSION = "2.0.0";
    public static final String ENGINE_VERSION = "4.18.0";
    public static final String EVIDENCE_CLASS = "p3b-contract";

    private static final Pattern COMMIT = Pattern.compile("[0-9a-f]{40,64}");

    private EditorialSafe4TrustedProfileFactory() { }

    public static EditorialEngineContractProfile create(String sourceCommit, String createdAt) {
        if (sourceCommit == null || !COMMIT.matcher(sourceCommit).matches()) {
            throw new IllegalArgumentException("sourceCommit must be an existing lowercase hexadecimal commit");
        }
        if (createdAt == null || createdAt.isBlank()) throw new IllegalArgumentException("createdAt is required");
        EditorialEngineContractCapabilityEvidenceCatalog catalog =
                EditorialEngineContractCapabilityEvidenceCatalog.production();
        List<EditorialEngineContractProfile.CapabilityEvidence> evidence = new ArrayList<>();
        for (String capability : EditorialSafe4Contract.IMPLEMENTED_CAPABILITY_IDS) {
            EditorialEngineContractCapabilityEvidenceCatalog.EvidenceDescriptor descriptor =
                    catalog.evidenceDescriptor(capability).orElseThrow();
            evidence.add(new EditorialEngineContractProfile.CapabilityEvidence(
                    capability, sourceCommit, descriptor.evidenceFingerprint(), EVIDENCE_CLASS));
        }

        Map<String, EditorialEngineContractProfile.ContextAllowList> contexts = contexts();
        List<EditorialEngineContractProfile.InputRole> roles = List.of(
                new EditorialEngineContractProfile.InputRole(EditorialSafe4Contract.RAW, "ONE", true),
                new EditorialEngineContractProfile.InputRole(EditorialSafe4Contract.DRAFT, "ONE", true),
                new EditorialEngineContractProfile.InputRole(EditorialSafe4Contract.GLOSSARY, "ONE", true),
                new EditorialEngineContractProfile.InputRole(EditorialSafe4Contract.PRONOUN, "ONE", true),
                new EditorialEngineContractProfile.InputRole(EditorialSafe4Contract.PAIR_CONTEXT, "OPTIONAL", false),
                new EditorialEngineContractProfile.InputRole("REPORT_L1", "OPTIONAL", false),
                new EditorialEngineContractProfile.InputRole("VI_L2", "OPTIONAL", false),
                new EditorialEngineContractProfile.InputRole("CHANGE_MAP_L2", "OPTIONAL", false));
        EditorialEngineContractProfile.PhaseGraph phaseGraph = new EditorialEngineContractProfile.PhaseGraph(
                EditorialSafe4Contract.PHASES,
                EditorialSafe4Contract.PHASE_EDGES.stream()
                        .map(edge -> new EditorialEngineContractProfile.PhaseEdge(edge.get(0), edge.get(1))).toList());

        EditorialEngineContractProfile draft = new EditorialEngineContractProfile(
                EditorialEngineContractProfile.PROFILE_FORMAT,
                EditorialEngineContractProfile.PROFILE_FORMAT_VERSION,
                PROFILE_ID, PROFILE_VERSION, ENGINE_VERSION,
                EditorialSafe4Contract.CONTRACT_VERSION, EditorialSafe4Contract.CONTRACT_VERSION,
                List.of(EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION), roles, phaseGraph, contexts,
                descriptors("safe4.full.evidence.schema.v1", EditorialSafe4Contract.EVIDENCE_SCHEMA_IDS),
                descriptors("safe4.full.gate.v1", EditorialSafe4Contract.GATE_IDS),
                descriptors("safe4.full.release-artifact.v1", EditorialSafe4Contract.RELEASE_ARTIFACT_ROLES),
                EditorialSafe4Contract.IMPLEMENTED_CAPABILITY_IDS, List.of(), evidence, List.of(), List.of(),
                "0".repeat(64), "0".repeat(64), createdAt, sourceCommit,
                new EditorialEngineContractProfile.DeprecationPolicy("ACTIVE", null, null, false, false));

        String machineFingerprint = EditorialEngineContractProfileCanonicalizer.machineContractFingerprint(draft);
        EditorialEngineContractProfile withMachine = copyWithHashes(draft, machineFingerprint, "0".repeat(64));
        String profileHash = EditorialEngineContractProfileCanonicalizer.canonicalProfileHash(withMachine);
        return copyWithHashes(withMachine, machineFingerprint, profileHash);
    }

    public static String canonicalJson(String sourceCommit, String createdAt) {
        return EditorialEngineContractProfileCanonicalizer.canonicalJson(create(sourceCommit, createdAt));
    }

    private static EditorialEngineContractProfile copyWithHashes(EditorialEngineContractProfile source,
                                                                  String machine, String hash) {
        return new EditorialEngineContractProfile(
                source.profileFormat(), source.profileFormatVersion(), source.engineProfileId(),
                source.engineProfileVersion(), source.engineVersion(), source.minimumSupportedContractVersion(),
                source.maximumSupportedContractVersion(), source.supportedSchemaVersions(), source.supportedInputRoles(),
                source.supportedPhaseGraph(), source.contextAllowListByPhase(), source.evidenceSchemaFingerprints(),
                source.gateDefinitionFingerprints(), source.releaseArtifactFingerprints(), source.implementedCapabilities(),
                source.explicitlyMissingCapabilities(), source.capabilityEvidence(), source.bundledAdapterIds(),
                source.adapterDescriptors(), machine, hash, source.createdAt(), source.buildSourceCommit(),
                source.deprecationPolicy());
    }

    private static List<EditorialEngineContractProfile.FingerprintDescriptor> descriptors(String namespace,
                                                                                            List<String> ids) {
        List<EditorialEngineContractProfile.FingerprintDescriptor> result = new ArrayList<>();
        for (String id : ids) result.add(new EditorialEngineContractProfile.FingerprintDescriptor(id,
                EditorialSafe4Contract.descriptorFingerprint(namespace, id)));
        return List.copyOf(result);
    }

    private static Map<String, EditorialEngineContractProfile.ContextAllowList> contexts() {
        Map<String, EditorialEngineContractProfile.ContextAllowList> result = new LinkedHashMap<>();
        result.put("L1_SOURCE_PREFLIGHT", context(List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                EditorialSafe4Contract.GLOSSARY, EditorialSafe4Contract.PRONOUN),
                List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                        EditorialSafe4Contract.GLOSSARY, EditorialSafe4Contract.PRONOUN)));
        result.put("L1_RAW_DISCOVERY", context(List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.GLOSSARY),
                List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.GLOSSARY)));
        result.put("L1_RECONCILE", context(List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                EditorialSafe4Contract.GLOSSARY), List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                EditorialSafe4Contract.GLOSSARY, EditorialSafe4Contract.PRONOUN, EditorialSafe4Contract.PAIR_CONTEXT)));
        result.put("L2_RAW_DISCOVERY", context(List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.GLOSSARY),
                List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.GLOSSARY)));
        result.put("L2_EDIT", context(List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                EditorialSafe4Contract.GLOSSARY, "REPORT_L1"), List.of(EditorialSafe4Contract.RAW,
                EditorialSafe4Contract.DRAFT, EditorialSafe4Contract.GLOSSARY, "REPORT_L1",
                EditorialSafe4Contract.PRONOUN, EditorialSafe4Contract.PAIR_CONTEXT)));
        result.put("L3_RAW_FIRST_REAUDIT", context(List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.GLOSSARY,
                "VI_L2"), List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.GLOSSARY, "VI_L2")));
        result.put("L3_RECONCILE", context(List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                EditorialSafe4Contract.GLOSSARY, "REPORT_L1", "VI_L2", "CHANGE_MAP_L2"), List.of(
                EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT, EditorialSafe4Contract.GLOSSARY,
                "REPORT_L1", "VI_L2", "CHANGE_MAP_L2", EditorialSafe4Contract.PRONOUN,
                EditorialSafe4Contract.PAIR_CONTEXT)));
        return result;
    }

    private static EditorialEngineContractProfile.ContextAllowList context(List<String> required, List<String> allowed) {
        return new EditorialEngineContractProfile.ContextAllowList(required, allowed);
    }
}
