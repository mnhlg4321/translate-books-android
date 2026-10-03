package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;

import java.util.Objects;

/** Routes the two ledger-v2 L1 phases to phase-bounded OpenRouter adapters. */
final class OpenRouterEditorialP6L1Provider implements EditorialP5PilotProvider {
    private final EditorialP5PilotProvider raw;
    private final EditorialP5PilotProvider reconcile;

    OpenRouterEditorialP6L1Provider(EditorialP5PilotProvider raw,
                                    EditorialP5PilotProvider reconcile) {
        this.raw = Objects.requireNonNull(raw, "raw provider");
        this.reconcile = Objects.requireNonNull(reconcile, "reconcile provider");
    }

    @Override public void beginAttempt(long maximumExecutionTimeMillis) {
        raw.beginAttempt(maximumExecutionTimeMillis);
        reconcile.beginAttempt(maximumExecutionTimeMillis);
    }

    @Override public Response call(Request request) throws Exception {
        if (request == null) throw new IllegalArgumentException("L1 request is required");
        return switch (request.phase()) {
            case "L1_RAW_DISCOVERY" -> raw.call(request);
            case "L1_RECONCILE" -> reconcile.call(request);
            default -> throw new IllegalStateException("P6_L1_PHASE_NOT_ALLOWED");
        };
    }
}
