package com.ml.tblandroidtxt;

/**
 * Production boundary while no RUN_CONTEXT_CLOSED event exists.
 * It deliberately cannot construct an event from chapter state, import, startup or UI state.
 */
public final class EditorialProductionRunClosureEventResolver
        implements EditorialRunClosureEventResolver {
    @Override public EditorialClosureEventResolutionResult resolve(String closureEventSelector) {
        return EditorialClosureEventResolutionResult.failure(
                EditorialClosureEventResolutionCode.CLOSURE_EVENT_UNAVAILABLE,
                "production-run-context-closed-event-absent");
    }
}
