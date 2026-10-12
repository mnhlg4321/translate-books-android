package com.ml.tblandroidtxt;

/** Immutable typed result; audit timestamps are never exposed as identity facts. */
public final class EditorialIdentityAppendResult<T> {
    private final EditorialIdentityPersistenceCode code;
    private final T value;
    private final String identity;
    private final String detail;

    private EditorialIdentityAppendResult(EditorialIdentityPersistenceCode code,
                                           T value,
                                           String identity,
                                           String detail) {
        this.code = code;
        this.value = value;
        this.identity = identity == null ? "" : identity;
        this.detail = detail == null ? "" : detail;
    }

    public static <T> EditorialIdentityAppendResult<T> of(
            EditorialIdentityPersistenceCode code, T value, String identity, String detail) {
        return new EditorialIdentityAppendResult<>(code, value, identity, detail);
    }

    public EditorialIdentityPersistenceCode code() { return code; }
    public T value() { return value; }
    public String identity() { return identity; }
    public String detail() { return detail; }
}
