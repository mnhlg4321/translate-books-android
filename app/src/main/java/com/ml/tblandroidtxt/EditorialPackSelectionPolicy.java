package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfile;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackRegistryMetadata;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministic, project-scoped selection policy. It has no global active-pack
 * state and never selects a latest or fallback pack.
 */
public final class EditorialPackSelectionPolicy {
    private final EditorialPackRegistry registry;
    private final EditorialPackCompatibilityEvaluationDao evaluations;
    private final EditorialEngineContractProfileRegistry profiles;

    public EditorialPackSelectionPolicy(EditorialPackRegistry registry,
                                        EditorialPackCompatibilityEvaluationDao evaluations,
                                        EditorialEngineContractProfileRegistry profiles) {
        if (registry == null || evaluations == null || profiles == null) {
            throw new IllegalArgumentException("selection policy dependencies are required");
        }
        this.registry = registry;
        this.evaluations = evaluations;
        this.profiles = profiles;
    }

    public EditorialPackSelectionPolicy(TranslationRepository database,
                                        EditorialPackStorageLayout storage) {
        this(new SqliteEditorialPackRegistry(database, storage),
                new EditorialPackCompatibilityEvaluationDao(database),
                BundledEditorialEngineContractProfileRegistry.load());
    }

    /** Returns only packs that are valid, stored, trusted and DATA_COMPATIBLE. */
    public List<EditorialPackSelectionCandidate> listSelectable() {
        ArrayList<EditorialPackSelectionCandidate> result = new ArrayList<>();
        for (EditorialPackManifest manifest : registry.list()) {
            candidate(manifest).ifPresent(result::add);
        }
        result.sort(Comparator.comparing(EditorialPackSelectionCandidate::packId)
                .thenComparing(EditorialPackSelectionCandidate::packVersion)
                .thenComparing(EditorialPackSelectionCandidate::canonicalPackHash));
        return List.copyOf(result);
    }

    /** Resolves exactly the user-selected identity; no version or hash fallback is allowed. */
    public Optional<EditorialPackSelectionCandidate> resolve(String packId, String packVersion) {
        if (packId == null || packId.isBlank() || packVersion == null || packVersion.isBlank()) {
            return Optional.empty();
        }
        try { return registry.findByIdentity(packId, packVersion).flatMap(this::candidate); }
        catch (RuntimeException error) { return Optional.empty(); }
    }

    private Optional<EditorialPackSelectionCandidate> candidate(EditorialPackManifest manifest) {
        if (manifest == null || manifest.registryMetadata().isEmpty()) return Optional.empty();
        EditorialPackRegistryMetadata metadata = manifest.registryMetadata().get();
        if (metadata.storageState() != EditorialPackRegistryMetadata.StorageState.STORED_READY_FOR_CERTIFICATION
                || metadata.integrityState() != EditorialPackRegistryMetadata.IntegrityState.VALID) {
            return Optional.empty();
        }
        if (!manifest.canonicalPackHash().equals(manifest.calculatedCanonicalPackHash())) {
            return Optional.empty();
        }
        List<EditorialPackCompatibilityEvaluation> history = evaluations.listByPackHash(
                manifest.canonicalPackHash());
        EditorialPackCompatibilityEvaluation evaluation = history.stream()
                .filter(value -> value.attestation() == EditorialPackCompatibilityEvaluation.Attestation.TRUSTED_PROFILE)
                .max(Comparator.comparingLong(EditorialPackCompatibilityEvaluation::evaluatedAt)
                        .thenComparingLong(EditorialPackCompatibilityEvaluation::id))
                .orElse(null);
        if (evaluation == null
                || evaluation.compatibilityOutcome() != EditorialPackCompatibilityClass.DATA_COMPATIBLE
                || !manifest.canonicalPackHash().equals(evaluation.canonicalPackHash())
                || evaluation.trustedProfileId().isEmpty()
                || evaluation.trustedProfileVersion().isEmpty()
                || evaluation.canonicalProfileHash().isEmpty()) return Optional.empty();
        EditorialEngineContractProfile profile = profiles.findByIdentity(
                evaluation.trustedProfileId().get(), evaluation.trustedProfileVersion().get()).orElse(null);
        if (profile == null || !"ACTIVE".equals(profile.deprecationPolicy().state())
                || !EditorialEngineProfileResolver.isExecutableContractProfile(profile)
                || !profile.canonicalProfileHash().equals(evaluation.canonicalProfileHash().get())
                || !profile.machineContractFingerprint().equals(evaluation.machineContractFingerprint())) {
            return Optional.empty();
        }
        Set<String> missing = new HashSet<>(manifest.requiredCapabilities());
        missing.removeAll(profile.implementedCapabilities());
        if (!missing.isEmpty()) return Optional.empty();
        return Optional.of(new EditorialPackSelectionCandidate(manifest, evaluation, profile));
    }

    public static String manifestFingerprint(EditorialPackManifest manifest) {
        if (manifest == null) throw new IllegalArgumentException("manifest is required");
        return EditorialCanonicalJson.sha256Hex(manifest.canonicalJson().getBytes(StandardCharsets.UTF_8));
    }

    public static String phaseGraphFingerprint(EditorialPackManifest manifest) {
        return declarationFingerprint(manifest, "phaseGraph");
    }

    public static String contextAllowListFingerprint(EditorialPackManifest manifest) {
        return declarationFingerprint(manifest, "contextAllowList");
    }

    private static String declarationFingerprint(EditorialPackManifest manifest, String key) {
        Object declaration = manifest.declarations().get(key);
        if (declaration == null) throw new IllegalArgumentException("Manifest declaration is missing: " + key);
        return EditorialCanonicalJson.sha256Hex(EditorialCanonicalJson.canonicalize(declaration)
                .getBytes(StandardCharsets.UTF_8));
    }
}
