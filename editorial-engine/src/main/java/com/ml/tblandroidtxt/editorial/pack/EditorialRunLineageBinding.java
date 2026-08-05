package com.ml.tblandroidtxt.editorial.pack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable exact binding between one closed run and one v16 lineage record. */
public final class EditorialRunLineageBinding {
    public static final String IDENTITY_DOMAIN = "EDITORIAL_RUN_LINEAGE_BINDING_IDENTITY_V1";
    public static final String FINGERPRINT_DOMAIN = "EDITORIAL_RUN_LINEAGE_BINDING_FINGERPRINT_V1";

    private final String bindingContractVersion;
    private final String closedRunIdentity;
    private final String lineageRecordIdentity;
    private final String lineageRecordFingerprint;
    private final String canonicalProjection;
    private final String bindingFingerprint;
    private final String bindingIdentity;

    public EditorialRunLineageBinding(
            String bindingContractVersion,
            String closedRunIdentity,
            String lineageRecordIdentity,
            String lineageRecordFingerprint) {
        this.bindingContractVersion = EditorialIdentityText.nonEmpty(bindingContractVersion,
                "binding contract version");
        this.closedRunIdentity = EditorialIdentityText.sha256(closedRunIdentity,
                "closed-run identity");
        this.lineageRecordIdentity = EditorialIdentityText.sha256(lineageRecordIdentity,
                "lineage record identity");
        this.lineageRecordFingerprint = EditorialIdentityText.sha256(lineageRecordFingerprint,
                "lineage record fingerprint");
        this.canonicalProjection = buildCanonicalProjection();
        this.bindingFingerprint = EditorialIdentityText.hash(FINGERPRINT_DOMAIN, canonicalProjection);
        this.bindingIdentity = EditorialIdentityText.hash(IDENTITY_DOMAIN, canonicalProjection);
    }

    public String bindingContractVersion() { return bindingContractVersion; }
    public String closedRunIdentity() { return closedRunIdentity; }
    public String lineageRecordIdentity() { return lineageRecordIdentity; }
    public String lineageRecordFingerprint() { return lineageRecordFingerprint; }
    public String canonicalProjection() { return canonicalProjection; }
    public String bindingFingerprint() { return bindingFingerprint; }
    public String bindingIdentity() { return bindingIdentity; }

    private String buildCanonicalProjection() {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("bindingContractVersion", bindingContractVersion);
        projection.put("closedRunIdentity", closedRunIdentity);
        projection.put("lineageRecordIdentity", lineageRecordIdentity);
        projection.put("lineageRecordFingerprint", lineageRecordFingerprint);
        return EditorialCanonicalJson.canonicalize(projection);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EditorialRunLineageBinding that)) return false;
        return bindingIdentity.equals(that.bindingIdentity)
                && canonicalProjection.equals(that.canonicalProjection);
    }

    @Override public int hashCode() { return Objects.hash(bindingIdentity, canonicalProjection); }
}
