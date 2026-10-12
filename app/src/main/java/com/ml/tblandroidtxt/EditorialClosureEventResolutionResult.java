package com.ml.tblandroidtxt;

import java.util.Objects;
import java.util.Optional;

/** Immutable closure-event resolution result without raw exceptions. */
public final class EditorialClosureEventResolutionResult {
    private final EditorialClosureEventResolutionCode code;
    private final EditorialRunClosureEvent event;
    private final String detail;

    private EditorialClosureEventResolutionResult(EditorialClosureEventResolutionCode code,
                                                   EditorialRunClosureEvent event,
                                                   String detail) {
        this.code = Objects.requireNonNull(code, "code");
        this.event = event;
        this.detail = detail == null ? "" : detail;
        if (code == EditorialClosureEventResolutionCode.RESOLVED && event == null) {
            throw new IllegalArgumentException("resolved closure event is required");
        }
        if (code != EditorialClosureEventResolutionCode.RESOLVED && event != null) {
            throw new IllegalArgumentException("failure cannot contain closure event");
        }
    }

    public static EditorialClosureEventResolutionResult resolved(EditorialRunClosureEvent event) {
        return new EditorialClosureEventResolutionResult(
                EditorialClosureEventResolutionCode.RESOLVED,
                Objects.requireNonNull(event, "event"), "resolved");
    }

    public static EditorialClosureEventResolutionResult failure(
            EditorialClosureEventResolutionCode code, String detail) {
        if (code == null || code == EditorialClosureEventResolutionCode.RESOLVED) {
            throw new IllegalArgumentException("failure code is required");
        }
        return new EditorialClosureEventResolutionResult(code, null, detail);
    }

    public EditorialClosureEventResolutionCode code() { return code; }
    public Optional<EditorialRunClosureEvent> event() { return Optional.ofNullable(event); }
    public String detail() { return detail; }
}
