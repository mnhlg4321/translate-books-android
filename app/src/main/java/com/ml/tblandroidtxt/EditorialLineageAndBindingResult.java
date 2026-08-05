package com.ml.tblandroidtxt;

/** Read-only summary of one atomic v16-lineage plus v17-binding operation. */
public final class EditorialLineageAndBindingResult {
    private final EditorialIdentityPersistenceCode code;
    private final EditorialLineageAppendResult lineageResult;
    private final EditorialIdentityAppendResult<?> bindingResult;
    private final boolean committed;
    private final String detail;

    EditorialLineageAndBindingResult(EditorialIdentityPersistenceCode code,
                                      EditorialLineageAppendResult lineageResult,
                                      EditorialIdentityAppendResult<?> bindingResult,
                                      boolean committed,
                                      String detail) {
        this.code = code;
        this.lineageResult = lineageResult;
        this.bindingResult = bindingResult;
        this.committed = committed;
        this.detail = detail == null ? "" : detail;
    }

    public EditorialIdentityPersistenceCode code() { return code; }
    public EditorialLineageAppendResult lineageResult() { return lineageResult; }
    public EditorialIdentityAppendResult<?> bindingResult() { return bindingResult; }
    public boolean committed() { return committed; }
    public String detail() { return detail; }
}
