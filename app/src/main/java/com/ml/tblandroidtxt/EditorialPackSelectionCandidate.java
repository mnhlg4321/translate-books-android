package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfile;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;

/** One explicit, project-scoped pack selection candidate. */
public record EditorialPackSelectionCandidate(
        EditorialPackManifest manifest,
        EditorialPackCompatibilityEvaluation evaluation,
        EditorialEngineContractProfile trustedProfile) {
    public EditorialPackSelectionCandidate {
        if (manifest == null || evaluation == null || trustedProfile == null) {
            throw new IllegalArgumentException("selection candidate facts are required");
        }
    }

    public String packId() { return manifest.packId(); }
    public String packVersion() { return manifest.version(); }
    public String canonicalPackHash() { return manifest.canonicalPackHash(); }
    public String trustedProfileId() { return trustedProfile.engineProfileId(); }
    public String trustedProfileVersion() { return trustedProfile.engineProfileVersion(); }
    public String canonicalProfileHash() { return trustedProfile.canonicalProfileHash(); }
    public String machineContractFingerprint() { return trustedProfile.machineContractFingerprint(); }
    public String selectionLabel() {
        return manifest.displayName() + " • v" + manifest.version()
                + " • " + EditorialPackUiModel.shortHashOf(manifest.canonicalPackHash());
    }
}
