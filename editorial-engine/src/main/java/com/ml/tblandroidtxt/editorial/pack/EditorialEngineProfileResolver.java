package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deterministically selects one active bundled profile and invokes the G2-A
 * evaluator. It never accepts profile identity from the imported pack and has
 * no fallback/default/closest-profile behavior.
 */
public final class EditorialEngineProfileResolver {
    private final EditorialEngineContractProfileRegistry registry;
    private final EditorialEngineContractProfileValidator validator;
    private final EditorialEngineProfileAdapter adapter;
    private final EditorialCompatibilityEvaluator evaluator;

    public EditorialEngineProfileResolver(EditorialEngineContractProfileRegistry registry) {
        this(registry, new EditorialEngineContractProfileValidator(),
                new EditorialEngineProfileAdapter(), new EditorialCompatibilityEvaluator());
    }

    EditorialEngineProfileResolver(EditorialEngineContractProfileRegistry registry,
                                   EditorialEngineContractProfileValidator validator,
                                   EditorialEngineProfileAdapter adapter,
                                   EditorialCompatibilityEvaluator evaluator) {
        if (registry == null || validator == null || adapter == null || evaluator == null) {
            throw new IllegalArgumentException("Resolver dependencies are required");
        }
        this.registry = registry;
        this.validator = validator;
        this.adapter = adapter;
        this.evaluator = evaluator;
    }

    public EditorialCompatibilityEvaluationResult resolve(EditorialPackManifest manifest) {
        if (manifest == null) return blocked(null, EditorialCompatibilityReasonCode.INVALID_PACK,
                "Manifest is required", null, null, EditorialPackCompatibilityClass.INVALID);

        final List<EditorialEngineContractProfile> profiles;
        try {
            profiles = registry.list();
            if (profiles == null) throw new IllegalStateException("Trusted registry returned null");
            validateRegistryProfiles(profiles);
        } catch (EditorialEngineContractProfileRegistryException e) {
            return registryFailure(manifest, e);
        } catch (RuntimeException e) {
            return blocked(manifest, EditorialCompatibilityReasonCode.TRUSTED_REGISTRY_INVALID,
                    "Trusted profile registry is invalid", null, null, EditorialPackCompatibilityClass.BLOCKED);
        }

        List<EditorialEngineContractProfile> active = profiles.stream()
                .filter(profile -> "ACTIVE".equals(profile.deprecationPolicy().state()))
                .sorted(Comparator.comparing(EditorialEngineContractProfile::engineProfileId)
                        .thenComparing(EditorialEngineContractProfile::engineProfileVersion)
                        .thenComparing(EditorialEngineContractProfile::canonicalProfileHash))
                .toList();
        if (active.isEmpty()) return blocked(manifest, EditorialCompatibilityReasonCode.NO_TRUSTED_PROFILE,
                "No active trusted profile is installed", null, null, EditorialPackCompatibilityClass.BLOCKED);

        List<EditorialEngineContractProfile> contractMatches = active.stream()
                .filter(profile -> hasExecutableContract(profile)
                        && supportsContractVersion(profile, manifest.contractVersion()))
                .toList();
        if (contractMatches.isEmpty()) {
            boolean anyExecutable = active.stream().anyMatch(EditorialEngineProfileResolver::hasExecutableContract);
            return blocked(manifest,
                    anyExecutable ? EditorialCompatibilityReasonCode.UNSUPPORTED_CONTRACT
                            : EditorialCompatibilityReasonCode.NO_TRUSTED_PROFILE,
                    anyExecutable ? "No trusted profile supports the imported contract"
                            : "No trusted executable contract profile is installed",
                    null, null, EditorialPackCompatibilityClass.BLOCKED);
        }

        List<EditorialEngineContractProfile> schemaMatches = contractMatches.stream()
                .filter(profile -> profile.supportedSchemaVersions().contains(manifest.schemaVersion()))
                .toList();
        if (schemaMatches.isEmpty()) return blocked(manifest, EditorialCompatibilityReasonCode.UNSUPPORTED_SCHEMA,
                "No trusted profile supports the imported schema", null, null,
                EditorialPackCompatibilityClass.BLOCKED);
        if (schemaMatches.size() > 1) return blocked(manifest,
                EditorialCompatibilityReasonCode.AMBIGUOUS_TRUSTED_PROFILE,
                "Multiple trusted profiles match without an approved priority rule", null, null,
                EditorialPackCompatibilityClass.BLOCKED);

        EditorialEngineContractProfile selected = schemaMatches.get(0);
        final EditorialEngineProfile evaluatorProfile;
        final EditorialCompatibilityResult evaluated;
        try {
            evaluatorProfile = adapter.adapt(selected, manifest);
            evaluated = evaluator.evaluate(manifest, evaluatorProfile);
        } catch (RuntimeException e) {
            return blocked(manifest, EditorialCompatibilityReasonCode.INVALID_PACK,
                    "Compatibility evaluation failed closed", selected, null,
                    EditorialPackCompatibilityClass.INVALID);
        }
        return normalize(manifest, selected, evaluatorProfile, evaluated);
    }

    private EditorialCompatibilityEvaluationResult normalize(EditorialPackManifest manifest,
                                                              EditorialEngineContractProfile selected,
                                                              EditorialEngineProfile evaluatorProfile,
                                                              EditorialCompatibilityResult evaluated) {
        Set<String> missing = new HashSet<>(evaluated.missingCapabilities());
        List<EditorialPackIntegrityResult.Issue> issues = new ArrayList<>(evaluated.issues());
        EditorialPackCompatibilityClass outcome = evaluated.classification();
        EditorialPackCompatibilityClass required = evaluated.requiredClass();
        EditorialCompatibilityReasonCode reason = EditorialCompatibilityReasonCode.BLOCKED;
        if (!missing.isEmpty()) {
            outcome = EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED;
            required = EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED;
            reason = EditorialCompatibilityReasonCode.MISSING_ENGINE_CAPABILITY;
            issues = List.of(new EditorialPackIntegrityResult.Issue(
                    EditorialPackValidationCode.MISSING_CAPABILITY,
                    "requiredCapabilities",
                    "Required engine capabilities are missing: " + String.join(",", new java.util.TreeSet<>(missing))));
        } else if (outcome == EditorialPackCompatibilityClass.DATA_COMPATIBLE) {
            reason = EditorialCompatibilityReasonCode.DATA_COMPATIBLE;
        } else if (outcome == EditorialPackCompatibilityClass.ADAPTER_REQUIRED) {
            reason = EditorialCompatibilityReasonCode.ADAPTER_REQUIRED;
        } else if (outcome == EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED) {
            reason = EditorialCompatibilityReasonCode.MACHINE_FINGERPRINT_MISMATCH;
        } else if (outcome == EditorialPackCompatibilityClass.INVALID) {
            reason = EditorialCompatibilityReasonCode.INVALID_PACK;
        }
        return new EditorialCompatibilityEvaluationResult(outcome, required, reason,
                evaluated.blocked(), issues, missing, manifest.canonicalPackHash(), selected, evaluatorProfile);
    }

    private void validateRegistryProfiles(List<EditorialEngineContractProfile> profiles) {
        Set<String> identities = new HashSet<>();
        Set<String> hashes = new HashSet<>();
        for (EditorialEngineContractProfile profile : profiles) {
            if (profile == null) throw new EditorialEngineContractProfileRegistryException(
                    EditorialEngineContractProfileRegistryException.Code.PROFILE_INVALID,
                    "Trusted profile registry contains a null profile");
            EditorialEngineContractProfileValidationResult validation = validator.validate(profile);
            if (!validation.isValid()) {
                if (validation.hasCode(EditorialEngineContractProfileValidationResult.Code.CANONICAL_HASH_MISMATCH)) {
                    throw new EditorialEngineContractProfileRegistryException(
                            EditorialEngineContractProfileRegistryException.Code.CANONICAL_HASH_MISMATCH,
                            "Trusted profile canonical hash mismatch");
                }
                if (validation.hasCode(EditorialEngineContractProfileValidationResult.Code.MACHINE_FINGERPRINT_MISMATCH)) {
                    throw new EditorialEngineContractProfileRegistryException(
                            EditorialEngineContractProfileRegistryException.Code.MACHINE_FINGERPRINT_MISMATCH,
                            "Trusted profile machine fingerprint mismatch");
                }
                throw new EditorialEngineContractProfileRegistryException(
                        EditorialEngineContractProfileRegistryException.Code.PROFILE_INVALID,
                        "Trusted profile validation failed");
            }
            String identity = profile.engineProfileId() + "\u0000" + profile.engineProfileVersion();
            if (!identities.add(identity)) throw new EditorialEngineContractProfileRegistryException(
                    EditorialEngineContractProfileRegistryException.Code.DUPLICATE_IDENTITY,
                    "Trusted profile identity is duplicated");
            if (!hashes.add(profile.canonicalProfileHash())) throw new EditorialEngineContractProfileRegistryException(
                    EditorialEngineContractProfileRegistryException.Code.DUPLICATE_CANONICAL_HASH,
                    "Trusted profile canonical hash is duplicated");
        }
    }

    private static boolean hasExecutableContract(EditorialEngineContractProfile profile) {
        return profile.minimumSupportedContractVersion() != null
                && profile.maximumSupportedContractVersion() != null
                && !profile.supportedSchemaVersions().isEmpty()
                && !profile.supportedPhaseGraph().phases().isEmpty();
    }

    private static boolean supportsContractVersion(EditorialEngineContractProfile profile, String version) {
        return compareContractVersions(profile.minimumSupportedContractVersion(), version) <= 0
                && compareContractVersions(version, profile.maximumSupportedContractVersion()) <= 0;
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
            int comparison = numericA && numericB ? compareNumericStrings(a, b) : a.compareTo(b);
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

    private EditorialCompatibilityEvaluationResult registryFailure(EditorialPackManifest manifest,
                                                                    EditorialEngineContractProfileRegistryException exception) {
        EditorialCompatibilityReasonCode reason = switch (exception.code()) {
            case CANONICAL_HASH_MISMATCH, PROFILE_ID_MISMATCH, PROFILE_VERSION_MISMATCH,
                    RESOURCE_HASH_MISMATCH -> EditorialCompatibilityReasonCode.PROFILE_HASH_MISMATCH;
            case MACHINE_FINGERPRINT_MISMATCH -> EditorialCompatibilityReasonCode.MACHINE_FINGERPRINT_MISMATCH;
            default -> EditorialCompatibilityReasonCode.TRUSTED_REGISTRY_INVALID;
        };
        return blocked(manifest, reason, "Trusted profile registry rejected its bundled profile",
                null, null, EditorialPackCompatibilityClass.BLOCKED);
    }

    private static EditorialCompatibilityEvaluationResult blocked(EditorialPackManifest manifest,
                                                                   EditorialCompatibilityReasonCode reason,
                                                                   String message,
                                                                   EditorialEngineContractProfile profile,
                                                                   EditorialEngineProfile evaluatorProfile,
                                                                   EditorialPackCompatibilityClass outcome) {
        EditorialPackCompatibilityClass required = outcome == EditorialPackCompatibilityClass.INVALID
                ? EditorialPackCompatibilityClass.INVALID : outcome;
        String hash = manifest == null ? "" : manifest.canonicalPackHash();
        return new EditorialCompatibilityEvaluationResult(outcome, required, reason, true,
                List.of(new EditorialPackIntegrityResult.Issue(
                        issueCode(reason),
                        "trustedProfile",
                        message)), Set.of(), hash, profile, evaluatorProfile);
    }

    private static EditorialPackValidationCode issueCode(EditorialCompatibilityReasonCode reason) {
        return switch (reason) {
            case INVALID_PACK -> EditorialPackValidationCode.INVALID_MANIFEST_FIELD;
            case ADAPTER_REQUIRED -> EditorialPackValidationCode.ADAPTER_REQUIRED;
            case ENGINE_UPGRADE_REQUIRED, MACHINE_FINGERPRINT_MISMATCH -> EditorialPackValidationCode.ENGINE_UPGRADE_REQUIRED;
            case MISSING_ENGINE_CAPABILITY -> EditorialPackValidationCode.MISSING_CAPABILITY;
            default -> EditorialPackValidationCode.UNSUPPORTED_CONTRACT_SCHEMA;
        };
    }
}
