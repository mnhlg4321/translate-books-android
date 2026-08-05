package com.ml.tblandroidtxt;

/**
 * Caller-owned selection for an explicit closure event. Trusted profile, pack
 * and evaluation facts are intentionally absent and are re-read by the creator.
 */
public record EditorialClosedRunContextClosureRequest(
        String projectRevisionIdentity,
        String scopeSnapshotIdentity,
        String compatibilityEvaluationId,
        String runKind,
        String phaseIdentity,
        Long sourceRunRowId,
        long closedAt) {
}
