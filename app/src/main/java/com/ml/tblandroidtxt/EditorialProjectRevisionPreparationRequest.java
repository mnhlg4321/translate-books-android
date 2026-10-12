package com.ml.tblandroidtxt;

/** Immutable caller input for one explicit project-revision preparation event. */
public record EditorialProjectRevisionPreparationRequest(
        String canonicalProjectionVersion,
        String projectSemanticKey,
        String projectDefinitionContractVersion,
        String semanticProjectType,
        String scopePolicyFingerprint,
        String workflowPolicyFingerprint,
        Long sourceProjectRowId,
        long createdAt) {
}
