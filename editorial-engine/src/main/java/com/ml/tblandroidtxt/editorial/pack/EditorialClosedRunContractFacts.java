package com.ml.tblandroidtxt.editorial.pack;

/** Immutable pack/evaluator contract facts copied from trusted evaluation evidence. */
public record EditorialClosedRunContractFacts(
        String packContractId,
        String packContractVersion,
        String packSchemaId,
        String packSchemaVersion,
        String evaluatorContractVersion,
        String adapterSetFingerprint,
        String capabilityFingerprint,
        String compatibilityContextFingerprint,
        String engineVersionUsed) {
    public EditorialClosedRunContractFacts {
        EditorialIdentityText.nonEmpty(packContractId, "pack contract id");
        EditorialIdentityText.nonEmpty(packContractVersion, "pack contract version");
        EditorialIdentityText.nonEmpty(packSchemaId, "pack schema id");
        EditorialIdentityText.nonEmpty(packSchemaVersion, "pack schema version");
        EditorialIdentityText.nonEmpty(evaluatorContractVersion, "evaluator contract version");
        EditorialIdentityText.sha256(adapterSetFingerprint, "adapter-set fingerprint");
        EditorialIdentityText.sha256(capabilityFingerprint, "capability fingerprint");
        EditorialIdentityText.sha256(compatibilityContextFingerprint,
                "compatibility-context fingerprint");
        EditorialIdentityText.nonEmpty(engineVersionUsed, "engine version");
    }
}
