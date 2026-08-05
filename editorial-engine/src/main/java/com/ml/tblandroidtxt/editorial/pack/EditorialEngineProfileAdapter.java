package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts the reviewed C0A profile model into the older G2-A evaluator facts.
 * Profile metadata is deliberately not copied into evaluator facts. Contract
 * support is materialized only for the incoming contract/schema after the
 * profile's approved bounds and schema allow-list have been checked.
 */
public final class EditorialEngineProfileAdapter {
    public EditorialEngineProfile adapt(EditorialEngineContractProfile profile,
                                        EditorialPackManifest manifest) {
        if (profile == null || manifest == null) {
            throw new IllegalArgumentException("Profile and manifest are required");
        }

        List<EditorialEngineProfile.ContractSupport> contracts = new ArrayList<>();
        if (supportsContractAndSchema(profile, manifest.contractVersion(), manifest.schemaVersion())) {
            contracts.add(new EditorialEngineProfile.ContractSupport(
                    manifest.contractVersion(),
                    manifest.schemaVersion(),
                    profile.machineContractFingerprint(),
                    new HashSet<>(profile.implementedCapabilities())));
        }

        List<EditorialEngineProfile.AdapterSupport> adapters = new ArrayList<>();
        Set<String> bundledAdapterIds = new HashSet<>(profile.bundledAdapterIds());
        for (EditorialEngineContractProfile.AdapterDescriptor descriptor : profile.adapterDescriptors()) {
            adapters.add(new EditorialEngineProfile.AdapterSupport(
                    descriptor.adapterId(),
                    descriptor.sourceContractVersion(),
                    descriptor.sourceSchemaVersion(),
                    descriptor.sourceMachineContractFingerprint(),
                    new HashSet<>(descriptor.requiredCapabilities()),
                    bundledAdapterIds.contains(descriptor.adapterId())));
        }

        return new EditorialEngineProfile(
                profile.engineVersion(),
                new HashSet<>(profile.implementedCapabilities()),
                contracts,
                adapters);
    }

    static boolean supportsContractAndSchema(EditorialEngineContractProfile profile,
                                              String contractVersion,
                                              String schemaVersion) {
        if (profile.minimumSupportedContractVersion() == null
                || profile.maximumSupportedContractVersion() == null
                || contractVersion == null
                || schemaVersion == null
                || profile.supportedSchemaVersions().stream().noneMatch(schemaVersion::equals)) {
            return false;
        }
        return compareContractVersions(profile.minimumSupportedContractVersion(), contractVersion) <= 0
                && compareContractVersions(contractVersion, profile.maximumSupportedContractVersion()) <= 0;
    }

    private static int compareContractVersions(String left, String right) {
        String[] leftParts = left.split("[._-]");
        String[] rightParts = right.split("[._-]");
        int count = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < count; i++) {
            String a = i < leftParts.length ? leftParts[i] : "";
            String b = i < rightParts.length ? rightParts[i] : "";
            boolean numericA = a.matches("[0-9]+");
            boolean numericB = b.matches("[0-9]+");
            int comparison;
            if (numericA && numericB) comparison = compareNumericStrings(a, b);
            else comparison = a.compareTo(b);
            if (comparison != 0) return comparison;
        }
        return 0;
    }

    private static int compareNumericStrings(String left, String right) {
        String a = left.replaceFirst("^0+(?!$)", "");
        String b = right.replaceFirst("^0+(?!$)", "");
        if (a.length() != b.length()) return Integer.compare(a.length(), b.length());
        return a.compareTo(b);
    }
}
