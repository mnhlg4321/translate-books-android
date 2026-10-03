package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;

import java.math.BigDecimal;
import java.util.UUID;

/** Creates one-use, phase-specific L1 consent records after the owner confirms the full chain dialog. */
final class EditorialP6L1Authorizations {
    record Pair(EditorialP5PilotAuthorization raw, EditorialP5PilotAuthorization reconcile) { }

    private EditorialP6L1Authorizations() { }

    static Pair create(EditorialP4Binding binding, String chapterKey, String endpointAccountFingerprint,
                       EditorialChainBudgets budgets, long issuedAtMillis) {
        if (binding == null || chapterKey == null || chapterKey.isBlank()
                || endpointAccountFingerprint == null || endpointAccountFingerprint.isBlank()
                || budgets == null || !budgets.includesL1()) {
            throw new IllegalArgumentException("P6 L1 authorization scope is incomplete");
        }
        long expiry = issuedAtMillis + 15 * 60 * 1000L;
        return new Pair(authorization(binding, chapterKey, endpointAccountFingerprint,
                        "L1_RAW_DISCOVERY", budgets.l1Raw(), issuedAtMillis, expiry),
                authorization(binding, chapterKey, endpointAccountFingerprint,
                        "L1_RECONCILE", budgets.l1Reconcile(), issuedAtMillis, expiry));
    }

    private static EditorialP5PilotAuthorization authorization(EditorialP4Binding binding, String chapterKey,
                                                                String fingerprint, String phase,
                                                                EditorialL2Execution.Budget budget,
                                                                long issuedAtMillis, long expiresAtMillis) {
        int inputTokens = (budget.maximumInputBytes() + 1) / 2;
        return new EditorialP5PilotAuthorization("p6-" + phase.toLowerCase(java.util.Locale.ROOT)
                + "-" + UUID.randomUUID(), binding.bindingIdentity(), binding.runDeclarationIdentity(),
                binding.canonicalPackHash(), binding.canonicalProfileHash(), binding.compatibilityEvaluationId(),
                chapterKey, phase, EditorialP5EFreshRawRoutingPolicy.PROVIDER,
                EditorialP5EFreshRawRoutingPolicy.MODEL, fingerprint, 1, 0, 0,
                inputTokens, budget.maximumOutputTokens(), inputTokens + budget.maximumOutputTokens(),
                budget.maximumCost(), budget.maximumExecutionTimeMillis(), true, false, false,
                "HASH_ONLY", "USER", issuedAtMillis, expiresAtMillis, true);
    }
}
