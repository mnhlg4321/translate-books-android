package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic JSON projection and hash calculator for contract profiles. */
public final class EditorialEngineContractProfileCanonicalizer {
    public static final String PROFILE_HASH_DOMAIN = "EDITORIAL_ENGINE_PROFILE_CANONICAL_HASH_V1\n";
    public static final String MACHINE_CONTRACT_DOMAIN = "EDITORIAL_ENGINE_MACHINE_CONTRACT_FINGERPRINT_V1\n";

    private EditorialEngineContractProfileCanonicalizer() {}

    public static String canonicalJson(EditorialEngineContractProfile profile) {
        return EditorialCanonicalJson.canonicalize(toMap(profile, true));
    }

    public static String canonicalJsonWithoutProfileHash(EditorialEngineContractProfile profile) {
        return EditorialCanonicalJson.canonicalize(toMap(profile, false));
    }

    public static String canonicalProfileHash(EditorialEngineContractProfile profile) {
        byte[] payload = (PROFILE_HASH_DOMAIN + canonicalJsonWithoutProfileHash(profile))
                .getBytes(StandardCharsets.UTF_8);
        return EditorialCanonicalJson.sha256Hex(payload);
    }

    public static String machineContractFingerprint(EditorialEngineContractProfile profile) {
        if (profile != null && EditorialSafe4Contract.CONTRACT_VERSION.equals(profile.minimumSupportedContractVersion())
                && EditorialSafe4Contract.CONTRACT_VERSION.equals(profile.maximumSupportedContractVersion())) {
            return EditorialSafe4Contract.machineContractFingerprint();
        }
        byte[] payload = (MACHINE_CONTRACT_DOMAIN
                + EditorialCanonicalJson.canonicalize(machineContractProjection(profile)))
                .getBytes(StandardCharsets.UTF_8);
        return EditorialCanonicalJson.sha256Hex(payload);
    }

    /** Returns the exact semantic projection used by machineContractFingerprint. */
    public static Map<String, Object> machineContractProjection(EditorialEngineContractProfile profile) {
        if (profile != null && EditorialSafe4Contract.CONTRACT_VERSION.equals(profile.minimumSupportedContractVersion())
                && EditorialSafe4Contract.CONTRACT_VERSION.equals(profile.maximumSupportedContractVersion())) {
            return new LinkedHashMap<>(EditorialSafe4Contract.machineContractProjection());
        }
        Map<String, Object> all = toMap(profile, false);
        all.remove("profileFormat");
        all.remove("profileFormatVersion");
        all.remove("engineProfileId");
        all.remove("engineProfileVersion");
        all.remove("engineVersion");
        all.remove("canonicalProfileHash");
        all.remove("createdAt");
        all.remove("buildSourceCommit");
        all.remove("deprecationPolicy");
        all.remove("machineContractFingerprint");
        return all;
    }

    private static Map<String, Object> toMap(EditorialEngineContractProfile profile, boolean includeHash) {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("profileFormat", profile.profileFormat());
        root.put("profileFormatVersion", BigDecimal.valueOf(profile.profileFormatVersion()));
        root.put("engineProfileId", profile.engineProfileId());
        root.put("engineProfileVersion", profile.engineProfileVersion());
        root.put("engineVersion", profile.engineVersion());
        root.put("minimumSupportedContractVersion", profile.minimumSupportedContractVersion());
        root.put("maximumSupportedContractVersion", profile.maximumSupportedContractVersion());
        root.put("supportedSchemaVersions", sortedStrings(profile.supportedSchemaVersions()));
        root.put("supportedInputRoles", inputRoles(profile.supportedInputRoles()));
        root.put("supportedPhaseGraph", phaseGraph(profile.supportedPhaseGraph()));
        root.put("contextAllowListByPhase", contextAllowLists(profile.contextAllowListByPhase()));
        root.put("evidenceSchemaFingerprints", fingerprints(profile.evidenceSchemaFingerprints()));
        root.put("gateDefinitionFingerprints", fingerprints(profile.gateDefinitionFingerprints()));
        root.put("releaseArtifactFingerprints", fingerprints(profile.releaseArtifactFingerprints()));
        root.put("implementedCapabilities", sortedStrings(profile.implementedCapabilities()));
        root.put("explicitlyMissingCapabilities", sortedStrings(profile.explicitlyMissingCapabilities()));
        root.put("capabilityEvidence", capabilityEvidence(profile.capabilityEvidence()));
        root.put("bundledAdapterIds", sortedStrings(profile.bundledAdapterIds()));
        root.put("adapterDescriptors", adapters(profile.adapterDescriptors()));
        root.put("machineContractFingerprint", profile.machineContractFingerprint());
        root.put("createdAt", profile.createdAt());
        root.put("buildSourceCommit", profile.buildSourceCommit());
        root.put("deprecationPolicy", deprecationPolicy(profile.deprecationPolicy()));
        if (includeHash) root.put("canonicalProfileHash", profile.canonicalProfileHash());
        return root;
    }

    private static List<String> sortedStrings(List<String> values) {
        ArrayList<String> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        return sorted;
    }

    private static List<Object> inputRoles(List<EditorialEngineContractProfile.InputRole> values) {
        ArrayList<EditorialEngineContractProfile.InputRole> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.comparing(EditorialEngineContractProfile.InputRole::role)
                .thenComparing(EditorialEngineContractProfile.InputRole::cardinality)
                .thenComparing(EditorialEngineContractProfile.InputRole::required));
        ArrayList<Object> result = new ArrayList<>(sorted.size());
        for (EditorialEngineContractProfile.InputRole item : sorted) {
            result.add(Map.of("role", item.role(), "cardinality", item.cardinality(), "required", item.required()));
        }
        return result;
    }

    private static Map<String, Object> phaseGraph(EditorialEngineContractProfile.PhaseGraph graph) {
        ArrayList<Object> edges = new ArrayList<>(graph.edges().size());
        for (EditorialEngineContractProfile.PhaseEdge edge : graph.edges()) {
            edges.add(Map.of("from", edge.from(), "to", edge.to()));
        }
        return Map.of("phases", new ArrayList<>(graph.phases()), "edges", edges);
    }

    private static Map<String, Object> contextAllowLists(
            Map<String, EditorialEngineContractProfile.ContextAllowList> values) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, EditorialEngineContractProfile.ContextAllowList> entry : values.entrySet()) {
            EditorialEngineContractProfile.ContextAllowList item = entry.getValue();
            result.put(entry.getKey(), Map.of(
                    "requiredRoles", sortedStrings(item.requiredRoles()),
                    "allowedRoles", sortedStrings(item.allowedRoles())));
        }
        return result;
    }

    private static List<Object> fingerprints(List<EditorialEngineContractProfile.FingerprintDescriptor> values) {
        ArrayList<EditorialEngineContractProfile.FingerprintDescriptor> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.comparing(EditorialEngineContractProfile.FingerprintDescriptor::id)
                .thenComparing(EditorialEngineContractProfile.FingerprintDescriptor::fingerprint));
        ArrayList<Object> result = new ArrayList<>(sorted.size());
        for (EditorialEngineContractProfile.FingerprintDescriptor item : sorted) {
            result.add(Map.of("id", item.id(), "fingerprint", item.fingerprint()));
        }
        return result;
    }

    private static List<Object> capabilityEvidence(List<EditorialEngineContractProfile.CapabilityEvidence> values) {
        ArrayList<EditorialEngineContractProfile.CapabilityEvidence> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.comparing(EditorialEngineContractProfile.CapabilityEvidence::capabilityId)
                .thenComparing(EditorialEngineContractProfile.CapabilityEvidence::sourceCommit)
                .thenComparing(EditorialEngineContractProfile.CapabilityEvidence::evidenceFingerprint)
                .thenComparing(EditorialEngineContractProfile.CapabilityEvidence::evidenceClass));
        ArrayList<Object> result = new ArrayList<>(sorted.size());
        for (EditorialEngineContractProfile.CapabilityEvidence item : sorted) {
            result.add(Map.of(
                    "capabilityId", item.capabilityId(),
                    "sourceCommit", item.sourceCommit(),
                    "evidenceFingerprint", item.evidenceFingerprint(),
                    "evidenceClass", item.evidenceClass()));
        }
        return result;
    }

    private static List<Object> adapters(List<EditorialEngineContractProfile.AdapterDescriptor> values) {
        ArrayList<EditorialEngineContractProfile.AdapterDescriptor> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.comparing(EditorialEngineContractProfile.AdapterDescriptor::adapterId)
                .thenComparing(EditorialEngineContractProfile.AdapterDescriptor::adapterVersion));
        ArrayList<Object> result = new ArrayList<>(sorted.size());
        for (EditorialEngineContractProfile.AdapterDescriptor item : sorted) {
            result.add(Map.of(
                    "adapterId", item.adapterId(),
                    "adapterVersion", item.adapterVersion(),
                    "sourceContractVersion", item.sourceContractVersion(),
                    "sourceSchemaVersion", item.sourceSchemaVersion(),
                    "sourceMachineContractFingerprint", item.sourceMachineContractFingerprint(),
                    "requiredCapabilities", sortedStrings(item.requiredCapabilities())));
        }
        return result;
    }

    private static Map<String, Object> deprecationPolicy(EditorialEngineContractProfile.DeprecationPolicy policy) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("state", policy.state());
        result.put("replacementProfileId", policy.replacementProfileId());
        result.put("replacementVersion", policy.replacementVersion());
        result.put("automaticReplacement", policy.automaticReplacement());
        result.put("automaticProjectRebind", policy.automaticProjectRebind());
        return result;
    }
}
