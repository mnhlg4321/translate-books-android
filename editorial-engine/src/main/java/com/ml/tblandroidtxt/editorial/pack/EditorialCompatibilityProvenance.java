package com.ml.tblandroidtxt.editorial.pack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic semantic fingerprints used by the SQLite v15 evaluation context. */
public final class EditorialCompatibilityProvenance {
    private EditorialCompatibilityProvenance() {}

    public static String adapterSetFingerprint(EditorialEngineContractProfile profile) {
        if (profile == null) throw new IllegalArgumentException("profile is required");
        List<Map<String, Object>> descriptors = new ArrayList<>();
        profile.adapterDescriptors().stream()
                .sorted(Comparator.comparing(EditorialEngineContractProfile.AdapterDescriptor::adapterId)
                        .thenComparing(EditorialEngineContractProfile.AdapterDescriptor::adapterVersion))
                .forEach(adapter -> {
                    LinkedHashMap<String, Object> value = new LinkedHashMap<>();
                    value.put("adapterId", adapter.adapterId());
                    value.put("adapterVersion", adapter.adapterVersion());
                    value.put("sourceContractVersion", adapter.sourceContractVersion());
                    value.put("sourceSchemaVersion", adapter.sourceSchemaVersion());
                    value.put("sourceMachineContractFingerprint", adapter.sourceMachineContractFingerprint());
                    value.put("requiredCapabilities", adapter.requiredCapabilities().stream().sorted().toList());
                    descriptors.add(value);
                });
        return fingerprint("EDITORIAL_ADAPTER_SET_FINGERPRINT_V1\n", descriptors);
    }

    public static String capabilityFingerprint(EditorialEngineContractProfile profile) {
        if (profile == null) throw new IllegalArgumentException("profile is required");
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("implementedCapabilities", profile.implementedCapabilities().stream().sorted().toList());
        value.put("explicitlyMissingCapabilities", profile.explicitlyMissingCapabilities().stream().sorted().toList());
        value.put("capabilityEvidence", profile.capabilityEvidence().stream()
                .sorted(Comparator.comparing(EditorialEngineContractProfile.CapabilityEvidence::capabilityId))
                .map(evidence -> {
                    LinkedHashMap<String, Object> item = new LinkedHashMap<>();
                    item.put("capabilityId", evidence.capabilityId());
                    item.put("sourceCommit", evidence.sourceCommit());
                    item.put("evidenceFingerprint", evidence.evidenceFingerprint());
                    item.put("evidenceClass", evidence.evidenceClass());
                    return item;
                }).toList());
        return fingerprint("EDITORIAL_CAPABILITY_FINGERPRINT_V1\n", value);
    }

    private static String fingerprint(String domain, Object value) {
        return EditorialCanonicalJson.sha256Hex((domain + EditorialCanonicalJson.canonicalize(value))
                .getBytes(StandardCharsets.UTF_8));
    }
}
