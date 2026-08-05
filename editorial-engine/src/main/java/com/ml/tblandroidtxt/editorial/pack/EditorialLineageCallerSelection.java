package com.ml.tblandroidtxt.editorial.pack;

/**
 * Caller-owned selectors for one explicit lineage preparation request.
 *
 * <p>Selectors are not semantic identities. The resolver must resolve them to
 * authoritative immutable facts. Null/blank selectors are retained so the
 * service can return a stable fail-closed result rather than throwing during
 * request construction.</p>
 */
public final class EditorialLineageCallerSelection {
    private final String projectSelector;
    private final String inputScopeSelector;
    private final String runEvaluationSelector;
    private final EditorialLineageNodeKind requestedNodeKind;
    private final String selectedParentRecordIdentity;

    public EditorialLineageCallerSelection(
            String projectSelector,
            String inputScopeSelector,
            String runEvaluationSelector,
            EditorialLineageNodeKind requestedNodeKind,
            String selectedParentRecordIdentity) {
        this.projectSelector = projectSelector;
        this.inputScopeSelector = inputScopeSelector;
        this.runEvaluationSelector = runEvaluationSelector;
        this.requestedNodeKind = requestedNodeKind;
        this.selectedParentRecordIdentity = selectedParentRecordIdentity;
    }

    public String projectSelector() { return projectSelector; }
    public String inputScopeSelector() { return inputScopeSelector; }
    public String runEvaluationSelector() { return runEvaluationSelector; }
    public EditorialLineageNodeKind requestedNodeKind() { return requestedNodeKind; }
    public String selectedParentRecordIdentity() { return selectedParentRecordIdentity; }
}
