package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContractFacts;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfile;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;

import java.util.Optional;

/** Production resolver: accepts only the exact executable bundled profile matching v15 evidence. */
public final class BundledEditorialTrustedClosedRunFactsResolver
        implements EditorialTrustedClosedRunFactsResolver {
    private final EditorialEngineContractProfileRegistry registry;

    public BundledEditorialTrustedClosedRunFactsResolver() {
        this(BundledEditorialEngineContractProfileRegistry.load());
    }

    BundledEditorialTrustedClosedRunFactsResolver(EditorialEngineContractProfileRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("registry is required");
        this.registry = registry;
    }

    @Override public Optional<EditorialTrustedClosedRunFacts> resolve(
            EditorialPackCompatibilityEvaluation evaluation,
            String packContractVersion,
            String packSchemaVersion) {
        if (evaluation == null
                || evaluation.attestation() != EditorialPackCompatibilityEvaluation.Attestation.TRUSTED_PROFILE
                || evaluation.compatibilityOutcome() != EditorialPackCompatibilityClass.DATA_COMPATIBLE
                || evaluation.trustedProfileId().isEmpty()
                || evaluation.trustedProfileVersion().isEmpty()
                || evaluation.canonicalProfileHash().isEmpty()
                || evaluation.evaluatorContractVersion().isEmpty()
                || evaluation.adapterSetFingerprint().isEmpty()
                || evaluation.capabilityFingerprint().isEmpty()
                || evaluation.contextFingerprint().isEmpty()) return Optional.empty();
        Optional<EditorialEngineContractProfile> profile = registry.findByIdentity(
                evaluation.trustedProfileId().get(), evaluation.trustedProfileVersion().get());
        if (profile.isEmpty() || !EditorialEngineProfileResolver.isExecutableContractProfile(profile.get())) {
            return Optional.empty();
        }
        EditorialEngineContractProfile stored = profile.get();
        if (!stored.canonicalProfileHash().equals(evaluation.canonicalProfileHash().get())
                || !stored.machineContractFingerprint().equals(evaluation.machineContractFingerprint())
                || !stored.engineVersion().equals(evaluation.engineVersionUsed())) return Optional.empty();
        return Optional.of(new EditorialTrustedClosedRunFacts(
                stored.engineProfileId(), stored.engineProfileVersion(), stored.canonicalProfileHash(),
                stored.machineContractFingerprint(), new EditorialClosedRunContractFacts(
                        "editorial-pack-contract", packContractVersion,
                        "editorial-pack-schema", packSchemaVersion,
                        evaluation.evaluatorContractVersion().get(), evaluation.adapterSetFingerprint().get(),
                        evaluation.capabilityFingerprint().get(), evaluation.contextFingerprint().get(),
                        evaluation.engineVersionUsed())));
    }
}
