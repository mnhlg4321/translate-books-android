package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;

/** Typed, fail-closed result for project/run setup binding. */
public final class EditorialP4BindingResult {
    public enum Code {
        APPENDED,
        ALREADY_EXISTS,
        REQUEST_COLLISION,
        PACK_NOT_SELECTABLE,
        INVALID_INPUT,
        FOREIGN_REFERENCE_MISSING,
        STALE_CHAIN,
        AMBIGUOUS_RESUME,
        PERSISTENCE_FAILURE,
        EXECUTION_DISABLED
    }

    private final Code code;
    private final long projectId;
    private final EditorialP4Binding binding;
    private final String detail;

    private EditorialP4BindingResult(Code code, long projectId, EditorialP4Binding binding, String detail) {
        this.code = code;
        this.projectId = projectId;
        this.binding = binding;
        this.detail = detail == null ? "" : detail;
    }

    public static EditorialP4BindingResult of(Code code, long projectId,
                                               EditorialP4Binding binding, String detail) {
        return new EditorialP4BindingResult(code, projectId, binding, detail);
    }

    public Code code() { return code; }
    public long projectId() { return projectId; }
    public EditorialP4Binding binding() { return binding; }
    public String detail() { return detail; }
    public boolean accepted() { return code == Code.APPENDED || code == Code.ALREADY_EXISTS; }
}
