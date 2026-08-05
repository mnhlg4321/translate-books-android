package com.ml.tblandroidtxt.editorial.pack;

import java.util.Objects;

/** Immutable request for preparation; it contains no persistence authority. */
public final class EditorialLineageCreationRequest {
    private final EditorialLineageCallerSelection callerSelection;
    private final EditorialLineageCallerAssertions callerAssertions;

    public EditorialLineageCreationRequest(EditorialLineageCallerSelection callerSelection) {
        this(callerSelection, EditorialLineageCallerAssertions.none());
    }

    public EditorialLineageCreationRequest(
            EditorialLineageCallerSelection callerSelection,
            EditorialLineageCallerAssertions callerAssertions) {
        this.callerSelection = Objects.requireNonNull(callerSelection, "callerSelection");
        this.callerAssertions = callerAssertions == null
                ? EditorialLineageCallerAssertions.none() : callerAssertions;
    }

    public EditorialLineageCallerSelection callerSelection() { return callerSelection; }
    public EditorialLineageCallerAssertions callerAssertions() { return callerAssertions; }
}
