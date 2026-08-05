package com.ml.tblandroidtxt;

import java.util.Objects;
import java.util.Optional;

/** Immutable machine result for one lineage append attempt. */
public final class EditorialLineageAppendResult {
    private final EditorialLineagePersistenceCode code;
    private final String recordIdentity;
    private final Long rowId;
    private final String detail;

    private EditorialLineageAppendResult(EditorialLineagePersistenceCode code,
                                         String recordIdentity,
                                         Long rowId,
                                         String detail) {
        this.code = Objects.requireNonNull(code, "code");
        this.recordIdentity = recordIdentity == null ? "" : recordIdentity;
        this.rowId = rowId;
        this.detail = detail == null ? "" : detail;
    }

    static EditorialLineageAppendResult of(EditorialLineagePersistenceCode code,
                                           String recordIdentity,
                                           Long rowId,
                                           String detail) {
        return new EditorialLineageAppendResult(code, recordIdentity, rowId, detail);
    }

    public EditorialLineagePersistenceCode code() { return code; }
    public String recordIdentity() { return recordIdentity; }
    public Optional<Long> rowId() { return Optional.ofNullable(rowId); }
    public String detail() { return detail; }
    public boolean accepted() {
        return code == EditorialLineagePersistenceCode.APPENDED
                || code == EditorialLineagePersistenceCode.ALREADY_EXISTS;
    }
}
