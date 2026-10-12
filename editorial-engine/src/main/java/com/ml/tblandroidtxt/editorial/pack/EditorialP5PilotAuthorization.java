package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Narrow, explicit authorization for one controlled L1 pilot.
 *
 * <p>This object is deliberately not a global execution switch.  It names one
 * persisted binding, one chapter and one provider/model budget.  Secrets are
 * not part of the object; endpointAccountFingerprint is an app-owned redacted
 * identity only.</p>
 */
public final class EditorialP5PilotAuthorization {
    private final String authorizationId;
    private final String projectBindingIdentity;
    private final String runDeclarationIdentity;
    private final String canonicalPackHash;
    private final String canonicalProfileHash;
    private final String compatibilityEvaluationId;
    private final String chapterKey;
    private final String phase;
    private final String provider;
    private final String model;
    private final String endpointAccountFingerprint;
    private final int maximumPrimarySemanticCalls;
    private final int maximumSchemaRepairCalls;
    private final int maximumNetworkRetries;
    private final int maximumInputTokens;
    private final int maximumOutputTokens;
    private final int maximumTotalTokens;
    private final BigDecimal maximumTotalCost;
    private final long maximumExecutionTimeMillis;
    private final boolean allowChapterToProvider;
    private final boolean allowFullModelResponseStorage;
    private final boolean allowRequestBodyStorage;
    private final String evidenceRedactionPolicy;
    private final String cancellationStopAuthority;
    private final long issuedAtMillis;
    private final long expiresAtMillis;
    private final boolean singleUse;

    public EditorialP5PilotAuthorization(
            String authorizationId,
            String projectBindingIdentity,
            String runDeclarationIdentity,
            String canonicalPackHash,
            String canonicalProfileHash,
            String compatibilityEvaluationId,
            String chapterKey,
            String phase,
            String provider,
            String model,
            String endpointAccountFingerprint,
            int maximumPrimarySemanticCalls,
            int maximumSchemaRepairCalls,
            int maximumNetworkRetries,
            int maximumInputTokens,
            int maximumOutputTokens,
            int maximumTotalTokens,
            BigDecimal maximumTotalCost,
            long maximumExecutionTimeMillis,
            boolean allowChapterToProvider,
            boolean allowFullModelResponseStorage,
            boolean allowRequestBodyStorage,
            String evidenceRedactionPolicy,
            String cancellationStopAuthority,
            long issuedAtMillis,
            long expiresAtMillis,
            boolean singleUse) {
        this.authorizationId = text(authorizationId, "authorization id");
        // Keep malformed values representable so the executor can return a typed,
        // fail-closed mismatch instead of throwing while constructing an authorization.
        this.projectBindingIdentity = text(projectBindingIdentity, "project binding identity");
        this.runDeclarationIdentity = text(runDeclarationIdentity, "run declaration identity");
        this.canonicalPackHash = text(canonicalPackHash, "canonical pack hash");
        this.canonicalProfileHash = text(canonicalProfileHash, "canonical profile hash");
        this.compatibilityEvaluationId = text(compatibilityEvaluationId,
                "compatibility evaluation id");
        this.chapterKey = text(chapterKey, "chapter key");
        this.phase = text(phase, "phase");
        this.provider = text(provider, "provider");
        this.model = text(model, "model");
        this.endpointAccountFingerprint = text(endpointAccountFingerprint,
                "endpoint/account fingerprint");
        if (maximumPrimarySemanticCalls < 0) {
            throw new IllegalArgumentException("maximum primary calls cannot be negative");
        }
        if (maximumSchemaRepairCalls < 0 || maximumSchemaRepairCalls > 1) {
            throw new IllegalArgumentException("maximum schema repair calls must be 0 or 1");
        }
        if (maximumNetworkRetries < 0 || maximumInputTokens < 0
                || maximumOutputTokens < 0 || maximumTotalTokens < 0) {
            throw new IllegalArgumentException("pilot budgets cannot be negative");
        }
        this.maximumPrimarySemanticCalls = maximumPrimarySemanticCalls;
        this.maximumSchemaRepairCalls = maximumSchemaRepairCalls;
        this.maximumNetworkRetries = maximumNetworkRetries;
        this.maximumInputTokens = maximumInputTokens;
        this.maximumOutputTokens = maximumOutputTokens;
        this.maximumTotalTokens = maximumTotalTokens;
        this.maximumTotalCost = Objects.requireNonNull(maximumTotalCost, "maximum total cost")
                .stripTrailingZeros();
        if (this.maximumTotalCost.signum() < 0) {
            throw new IllegalArgumentException("maximum total cost cannot be negative");
        }
        if (maximumExecutionTimeMillis <= 0) {
            throw new IllegalArgumentException("maximum execution time must be positive");
        }
        this.maximumExecutionTimeMillis = maximumExecutionTimeMillis;
        this.allowChapterToProvider = allowChapterToProvider;
        this.allowFullModelResponseStorage = allowFullModelResponseStorage;
        this.allowRequestBodyStorage = allowRequestBodyStorage;
        this.evidenceRedactionPolicy = text(evidenceRedactionPolicy,
                "evidence redaction policy");
        this.cancellationStopAuthority = text(cancellationStopAuthority,
                "cancellation/stop authority");
        if (expiresAtMillis <= issuedAtMillis) {
            throw new IllegalArgumentException("authorization expiry must be after issue time");
        }
        this.issuedAtMillis = issuedAtMillis;
        this.expiresAtMillis = expiresAtMillis;
        this.singleUse = singleUse;
    }

    public String authorizationId() { return authorizationId; }
    public String projectBindingIdentity() { return projectBindingIdentity; }
    public String runDeclarationIdentity() { return runDeclarationIdentity; }
    public String canonicalPackHash() { return canonicalPackHash; }
    public String canonicalProfileHash() { return canonicalProfileHash; }
    public String compatibilityEvaluationId() { return compatibilityEvaluationId; }
    public String chapterKey() { return chapterKey; }
    public String phase() { return phase; }
    public String provider() { return provider; }
    public String model() { return model; }
    public String endpointAccountFingerprint() { return endpointAccountFingerprint; }
    public int maximumPrimarySemanticCalls() { return maximumPrimarySemanticCalls; }
    public int maximumSchemaRepairCalls() { return maximumSchemaRepairCalls; }
    public int maximumNetworkRetries() { return maximumNetworkRetries; }
    public int maximumInputTokens() { return maximumInputTokens; }
    public int maximumOutputTokens() { return maximumOutputTokens; }
    public int maximumTotalTokens() { return maximumTotalTokens; }
    public BigDecimal maximumTotalCost() { return maximumTotalCost; }
    public long maximumExecutionTimeMillis() { return maximumExecutionTimeMillis; }
    public boolean allowChapterToProvider() { return allowChapterToProvider; }
    public boolean allowFullModelResponseStorage() { return allowFullModelResponseStorage; }
    public boolean allowRequestBodyStorage() { return allowRequestBodyStorage; }
    public String evidenceRedactionPolicy() { return evidenceRedactionPolicy; }
    public String cancellationStopAuthority() { return cancellationStopAuthority; }
    public long issuedAtMillis() { return issuedAtMillis; }
    public long expiresAtMillis() { return expiresAtMillis; }
    public boolean singleUse() { return singleUse; }

    public boolean expiredAt(long nowMillis) {
        return nowMillis < issuedAtMillis || nowMillis >= expiresAtMillis;
    }

    private static String text(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }
}
