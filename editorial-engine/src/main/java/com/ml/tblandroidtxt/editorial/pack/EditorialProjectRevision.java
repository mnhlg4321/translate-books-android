package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Canonical v1 project-definition projection and its derived immutable identity. */
public final class EditorialProjectRevision {
    public static final String IDENTITY_DOMAIN = "EDITORIAL_PROJECT_REVISION_IDENTITY_V1";
    public static final String FINGERPRINT_DOMAIN = "EDITORIAL_PROJECT_REVISION_FINGERPRINT_V1";

    private final String canonicalProjectionVersion;
    private final String projectSemanticKey;
    private final String projectDefinitionContractVersion;
    private final String semanticProjectType;
    private final String scopePolicyFingerprint;
    private final String workflowPolicyFingerprint;
    private final String canonicalProjection;
    private final String projectDefinitionFingerprint;
    private final String revisionIdentity;

    public EditorialProjectRevision(
            String canonicalProjectionVersion,
            String projectSemanticKey,
            String projectDefinitionContractVersion,
            String semanticProjectType,
            String scopePolicyFingerprint,
            String workflowPolicyFingerprint) {
        this.canonicalProjectionVersion = EditorialIdentityText.nonEmpty(
                canonicalProjectionVersion, "canonical project projection version");
        this.projectSemanticKey = EditorialIdentityText.nonEmpty(projectSemanticKey,
                "project semantic key");
        this.projectDefinitionContractVersion = EditorialIdentityText.nonEmpty(
                projectDefinitionContractVersion, "project-definition contract version");
        this.semanticProjectType = EditorialIdentityText.optionalText(semanticProjectType,
                "semantic project type");
        this.scopePolicyFingerprint = EditorialIdentityText.sha256(scopePolicyFingerprint,
                "scope-policy fingerprint");
        this.workflowPolicyFingerprint = EditorialIdentityText.sha256(workflowPolicyFingerprint,
                "workflow-policy fingerprint");
        this.canonicalProjection = buildCanonicalProjection();
        this.projectDefinitionFingerprint = EditorialIdentityText.hash(
                FINGERPRINT_DOMAIN, canonicalProjection);
        this.revisionIdentity = EditorialIdentityText.hash(IDENTITY_DOMAIN, canonicalProjection);
    }

    public String canonicalProjectionVersion() { return canonicalProjectionVersion; }
    public String projectSemanticKey() { return projectSemanticKey; }
    public String projectDefinitionContractVersion() { return projectDefinitionContractVersion; }
    public String semanticProjectType() { return semanticProjectType; }
    public String scopePolicyFingerprint() { return scopePolicyFingerprint; }
    public String workflowPolicyFingerprint() { return workflowPolicyFingerprint; }
    public String canonicalProjection() { return canonicalProjection; }
    public String projectDefinitionFingerprint() { return projectDefinitionFingerprint; }
    public String revisionIdentity() { return revisionIdentity; }

    private String buildCanonicalProjection() {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("canonicalProjectionVersion", canonicalProjectionVersion);
        projection.put("projectDefinitionContractVersion", projectDefinitionContractVersion);
        projection.put("projectSemanticKey", projectSemanticKey);
        if (semanticProjectType != null) projection.put("semanticProjectType", semanticProjectType);
        projection.put("scopePolicyFingerprint", scopePolicyFingerprint);
        projection.put("workflowPolicyFingerprint", workflowPolicyFingerprint);
        return EditorialCanonicalJson.canonicalize(projection);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EditorialProjectRevision that)) return false;
        return revisionIdentity.equals(that.revisionIdentity)
                && canonicalProjection.equals(that.canonicalProjection);
    }

    @Override public int hashCode() { return Objects.hash(revisionIdentity, canonicalProjection); }
}
