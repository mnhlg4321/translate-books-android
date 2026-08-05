package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialLineageAuthoritativeContext;

import java.util.Optional;

/** Immutable detailed result; the C1 port remains a narrow context-or-null adapter. */
public final class EditorialAuthoritativeContextResolutionResult {
    private final EditorialAuthoritativeContextResolutionCode code;
    private final EditorialLineageAuthoritativeContext context;

    private EditorialAuthoritativeContextResolutionResult(
            EditorialAuthoritativeContextResolutionCode code,
            EditorialLineageAuthoritativeContext context) {
        this.code = code;
        this.context = context;
    }

    public static EditorialAuthoritativeContextResolutionResult resolved(
            EditorialLineageAuthoritativeContext context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        return new EditorialAuthoritativeContextResolutionResult(
                EditorialAuthoritativeContextResolutionCode.RESOLVED, context);
    }

    public static EditorialAuthoritativeContextResolutionResult failure(
            EditorialAuthoritativeContextResolutionCode code) {
        if (code == null || code == EditorialAuthoritativeContextResolutionCode.RESOLVED) {
            throw new IllegalArgumentException("failure code is required");
        }
        return new EditorialAuthoritativeContextResolutionResult(code, null);
    }

    public EditorialAuthoritativeContextResolutionCode code() { return code; }
    public Optional<EditorialLineageAuthoritativeContext> context() { return Optional.ofNullable(context); }
}
