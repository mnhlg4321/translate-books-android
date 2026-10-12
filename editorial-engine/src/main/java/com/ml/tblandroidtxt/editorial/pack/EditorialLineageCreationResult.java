package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable, non-persisting result of lineage preparation. */
public final class EditorialLineageCreationResult {
    private final EditorialLineageCreationCode code;
    private final List<EditorialLineageValidationCode> validationCodes;
    private final EditorialLineageRecord record;

    private EditorialLineageCreationResult(
            EditorialLineageCreationCode code,
            List<EditorialLineageValidationCode> validationCodes,
            EditorialLineageRecord record) {
        this.code = Objects.requireNonNull(code, "code");
        this.validationCodes = Collections.unmodifiableList(new ArrayList<>(validationCodes));
        this.record = record;
        if (code == EditorialLineageCreationCode.READY_TO_APPEND && record == null) {
            throw new IllegalArgumentException("Ready result requires a record");
        }
        if (code != EditorialLineageCreationCode.READY_TO_APPEND && record != null) {
            throw new IllegalArgumentException("Failure result cannot contain a record");
        }
    }

    public static EditorialLineageCreationResult ready(EditorialLineageRecord record) {
        return new EditorialLineageCreationResult(EditorialLineageCreationCode.READY_TO_APPEND,
                List.of(EditorialLineageValidationCode.VALID), Objects.requireNonNull(record, "record"));
    }

    public static EditorialLineageCreationResult failure(EditorialLineageCreationCode code) {
        if (code == EditorialLineageCreationCode.READY_TO_APPEND
                || code == EditorialLineageCreationCode.VALIDATION_FAILED) {
            throw new IllegalArgumentException("Use validationFailure for validation failures");
        }
        return new EditorialLineageCreationResult(code, List.of(), null);
    }

    public static EditorialLineageCreationResult validationFailure(
            List<EditorialLineageValidationCode> codes) {
        Objects.requireNonNull(codes, "codes");
        LinkedHashSet<EditorialLineageValidationCode> unique = new LinkedHashSet<>(codes);
        unique.remove(EditorialLineageValidationCode.VALID);
        if (unique.isEmpty()) throw new IllegalArgumentException("Validation failure requires a code");
        return new EditorialLineageCreationResult(EditorialLineageCreationCode.VALIDATION_FAILED,
                List.copyOf(unique), null);
    }

    public EditorialLineageCreationCode code() { return code; }
    public List<EditorialLineageValidationCode> validationCodes() { return validationCodes; }
    public boolean isReadyToAppend() { return code == EditorialLineageCreationCode.READY_TO_APPEND; }
    public Optional<EditorialLineageRecord> record() { return Optional.ofNullable(record); }
}
