package com.ml.tblandroidtxt.editorial.pack;

import java.util.Objects;

/**
 * Immutable closure-event intent request. Authoritative identities, fingerprints,
 * ordinal and audit time are deliberately absent; the DAO resolves them from
 * the exact declaration and trusted append permit.
 */
public final class EditorialRunClosureEventDraft {
    public static final String CONTRACT_VERSION = "editorial-run-closure-event-v1";

    private final String attemptRequestSelector;
    private final EditorialLineageNodeKind nodeKind;
    private final String parentRecordIdentity;
    private final EditorialClosureEventEligibility closureEligibility;

    public EditorialRunClosureEventDraft(
            String attemptRequestSelector,
            EditorialLineageNodeKind nodeKind,
            String parentRecordIdentity,
            EditorialClosureEventEligibility closureEligibility) {
        this.attemptRequestSelector = EditorialAttemptRequestSelector.validate(
                attemptRequestSelector);
        this.nodeKind = Objects.requireNonNull(nodeKind, "node kind");
        if (nodeKind == EditorialLineageNodeKind.ROOT && parentRecordIdentity != null) {
            throw new IllegalArgumentException("root parent is forbidden");
        }
        if (nodeKind == EditorialLineageNodeKind.CHILD && parentRecordIdentity == null) {
            throw new IllegalArgumentException("child parent is required");
        }
        this.parentRecordIdentity = parentRecordIdentity == null ? null
                : EditorialIdentityText.sha256(parentRecordIdentity, "parent record identity");
        if (closureEligibility == null) {
            throw new IllegalArgumentException("closure eligibility is required");
        }
        this.closureEligibility = closureEligibility;
    }

    public String attemptRequestSelector() { return attemptRequestSelector; }
    public EditorialLineageNodeKind nodeKind() { return nodeKind; }
    public String parentRecordIdentity() { return parentRecordIdentity; }
    public EditorialClosureEventEligibility closureEligibility() { return closureEligibility; }
}
