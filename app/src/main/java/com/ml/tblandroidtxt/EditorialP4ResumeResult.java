package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;

/** Exact-resume result; stale chains are visible but never repaired automatically. */
public final class EditorialP4ResumeResult {
    public enum Code { RESTORED, NOT_FOUND, STALE_CHAIN, AMBIGUOUS }

    private final Code code;
    private final EditorialP4Binding binding;
    private final String detail;

    private EditorialP4ResumeResult(Code code, EditorialP4Binding binding, String detail) {
        this.code = code;
        this.binding = binding;
        this.detail = detail == null ? "" : detail;
    }

    static EditorialP4ResumeResult of(Code code, EditorialP4Binding binding, String detail) {
        return new EditorialP4ResumeResult(code, binding, detail);
    }

    public Code code() { return code; }
    public EditorialP4Binding binding() { return binding; }
    public String detail() { return detail; }
    public boolean executionAllowed() { return false; }
}
