package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Computes effective compatibility from trusted installed facts, never from filenames/version alone. */
public final class EditorialCompatibilityEvaluator {
    public EditorialCompatibilityResult evaluate(EditorialPackManifest manifest, EditorialEngineProfile profile) {
        if (manifest == null || profile == null) {
            return result(EditorialPackCompatibilityClass.INVALID, EditorialPackCompatibilityClass.INVALID, true,
                    List.of(issue(EditorialPackValidationCode.INVALID_MANIFEST_FIELD, "", "Manifest and engine profile are required")), Set.of(), "");
        }
        String fingerprint = manifest.machineContractFingerprint();
        EditorialEngineProfile.ContractKey key = new EditorialEngineProfile.ContractKey(manifest.contractVersion(), manifest.schemaVersion());
        EditorialEngineProfile.ContractSupport contract = profile.contracts().get(key);
        if (contract == null) {
            return result(EditorialPackCompatibilityClass.BLOCKED, EditorialPackCompatibilityClass.BLOCKED, true,
                    List.of(issue(EditorialPackValidationCode.UNSUPPORTED_CONTRACT_SCHEMA, "contractVersion/schemaVersion", "No trusted contract descriptor is installed")), Set.of(), fingerprint);
        }
        if (compareVersions(profile.engineVersion(), manifest.minimumEngineVersion()) < 0) {
            return result(EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, true,
                    List.of(issue(EditorialPackValidationCode.MINIMUM_ENGINE_TOO_NEW, "minimumEngineVersion", "Installed engine does not meet the pack minimum")), Set.of(), fingerprint);
        }

        if (fingerprint.equals(contract.machineContractFingerprint())) {
            Set<String> missing = missing(manifest.requiredCapabilities(), profile.capabilities());
            if (!missing.isEmpty()) return blocked(EditorialPackCompatibilityClass.DATA_COMPATIBLE, missing, fingerprint,
                    EditorialPackValidationCode.MISSING_CAPABILITY, "Required engine capabilities are missing");
            EditorialPackCompatibilityClass declared = manifest.declaredCompatibilityClass();
            if (declared != EditorialPackCompatibilityClass.DATA_COMPATIBLE) return invalidDeclaration(declared, EditorialPackCompatibilityClass.DATA_COMPATIBLE, fingerprint);
            return result(EditorialPackCompatibilityClass.DATA_COMPATIBLE, EditorialPackCompatibilityClass.DATA_COMPATIBLE, false, List.of(), Set.of(), fingerprint);
        }

        List<EditorialEngineProfile.AdapterSupport> knownAdapters = new ArrayList<>();
        for (EditorialEngineProfile.AdapterSupport adapter : profile.adapters()) {
            if (adapter.sourceContractVersion().equals(manifest.contractVersion())
                    && adapter.sourceSchemaVersion().equals(manifest.schemaVersion())
                    && adapter.sourceMachineContractFingerprint().equals(fingerprint)) knownAdapters.add(adapter);
        }
        if (!knownAdapters.isEmpty()) {
            EditorialEngineProfile.AdapterSupport selected = knownAdapters.stream().filter(EditorialEngineProfile.AdapterSupport::installed).findFirst().orElse(knownAdapters.get(0));
            Set<String> missing = new HashSet<>(manifest.requiredCapabilities());
            missing.addAll(selected.requiredCapabilities());
            missing.removeAll(profile.capabilities());
            if (!missing.isEmpty()) return blocked(EditorialPackCompatibilityClass.ADAPTER_REQUIRED, missing, fingerprint,
                    EditorialPackValidationCode.MISSING_CAPABILITY, "Adapter or engine capabilities are missing");
            if (!selected.installed()) return result(EditorialPackCompatibilityClass.ADAPTER_REQUIRED, EditorialPackCompatibilityClass.ADAPTER_REQUIRED, true,
                    List.of(issue(EditorialPackValidationCode.ADAPTER_REQUIRED, "adapterId", "A trusted adapter is known but not installed")), Set.of(), fingerprint);
            if (manifest.declaredCompatibilityClass() != EditorialPackCompatibilityClass.ADAPTER_REQUIRED) return invalidDeclaration(manifest.declaredCompatibilityClass(), EditorialPackCompatibilityClass.ADAPTER_REQUIRED, fingerprint);
            return result(EditorialPackCompatibilityClass.ADAPTER_REQUIRED, EditorialPackCompatibilityClass.ADAPTER_REQUIRED, false, List.of(), Set.of(), fingerprint);
        }

        if (manifest.declaredCompatibilityClass() != EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED) {
            return invalidDeclaration(manifest.declaredCompatibilityClass(), EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, fingerprint);
        }
        return result(EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, true,
                List.of(issue(EditorialPackValidationCode.ENGINE_UPGRADE_REQUIRED, "machineContractFingerprint", "Machine contract is not representable by the installed engine")), Set.of(), fingerprint);
    }

    private static EditorialCompatibilityResult invalidDeclaration(EditorialPackCompatibilityClass declared, EditorialPackCompatibilityClass computed, String fingerprint) {
        return result(EditorialPackCompatibilityClass.INVALID, computed, true,
                List.of(issue(EditorialPackValidationCode.DECLARED_CLASS_MISMATCH, "compatibilityClass", "Declared " + declared + " does not match computed " + computed)), Set.of(), fingerprint);
    }

    private static EditorialCompatibilityResult blocked(EditorialPackCompatibilityClass required, Set<String> missing, String fingerprint,
                                                         EditorialPackValidationCode code, String message) {
        return result(EditorialPackCompatibilityClass.BLOCKED, required, true,
                List.of(issue(code, "requiredCapabilities", message + ": " + missing)), missing, fingerprint);
    }

    private static EditorialCompatibilityResult result(EditorialPackCompatibilityClass classification, EditorialPackCompatibilityClass required,
                                                        boolean blocked, List<EditorialPackIntegrityResult.Issue> issues,
                                                        Set<String> missing, String fingerprint) {
        return new EditorialCompatibilityResult(classification, required, blocked, issues, missing, fingerprint);
    }

    private static EditorialPackIntegrityResult.Issue issue(EditorialPackValidationCode code, String path, String message) {
        return new EditorialPackIntegrityResult.Issue(code, path, message);
    }

    private static Set<String> missing(Set<String> required, Set<String> installed) {
        HashSet<String> result = new HashSet<>(required);
        result.removeAll(installed);
        return result;
    }

    static int compareVersions(String left, String right) {
        String[] a = left.split("\\.");
        String[] b = right.split("\\.");
        int count = Math.max(a.length, b.length);
        for (int i = 0; i < count; i++) {
            int av = i < a.length ? numericPart(a[i]) : 0;
            int bv = i < b.length ? numericPart(b[i]) : 0;
            if (av != bv) return Integer.compare(av, bv);
        }
        return 0;
    }

    private static int numericPart(String value) {
        if (value.isEmpty() || !value.chars().allMatch(Character::isDigit)) throw new IllegalArgumentException("Invalid engine version: " + value);
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { throw new IllegalArgumentException("Engine version is too large", e); }
    }
}
