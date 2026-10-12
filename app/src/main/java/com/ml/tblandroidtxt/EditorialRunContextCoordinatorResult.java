package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialLineageCreationCode;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageValidationCode;

import java.util.ArrayList;
import java.util.List;

/** Immutable, lossless coordinator result for one explicit command. */
public final class EditorialRunContextCoordinatorResult {
    private final EditorialRunContextCoordinatorStage stage;
    private final EditorialRunContextCoordinatorCode code;
    private final boolean retryable;
    private final String closedRunIdentity;
    private final String lineageIdentity;
    private final String bindingIdentity;
    private final String detail;
    private final EditorialLineageCreationCode lineageCreationCode;
    private final List<EditorialLineageValidationCode> validationCodes;
    private final EditorialIdentityPersistenceCode persistenceCode;

    private EditorialRunContextCoordinatorResult(
            EditorialRunContextCoordinatorStage stage,
            EditorialRunContextCoordinatorCode code,
            boolean retryable,
            String closedRunIdentity,
            String lineageIdentity,
            String bindingIdentity,
            String detail,
            EditorialLineageCreationCode lineageCreationCode,
            List<EditorialLineageValidationCode> validationCodes,
            EditorialIdentityPersistenceCode persistenceCode) {
        this.stage = stage;
        this.code = code;
        this.retryable = retryable;
        this.closedRunIdentity = safe(closedRunIdentity);
        this.lineageIdentity = safe(lineageIdentity);
        this.bindingIdentity = safe(bindingIdentity);
        this.detail = safe(detail);
        this.lineageCreationCode = lineageCreationCode;
        this.validationCodes = List.copyOf(new ArrayList<>(validationCodes == null ? List.of() : validationCodes));
        this.persistenceCode = persistenceCode;
    }

    public static EditorialRunContextCoordinatorResult of(
            EditorialRunContextCoordinatorStage stage,
            EditorialRunContextCoordinatorCode code,
            boolean retryable,
            String closedRunIdentity,
            String lineageIdentity,
            String bindingIdentity,
            String detail,
            EditorialLineageCreationCode lineageCreationCode,
            List<EditorialLineageValidationCode> validationCodes,
            EditorialIdentityPersistenceCode persistenceCode) {
        if (stage == null || code == null) throw new IllegalArgumentException("stage-and-code-required");
        return new EditorialRunContextCoordinatorResult(stage, code, retryable, closedRunIdentity,
                lineageIdentity, bindingIdentity, detail, lineageCreationCode, validationCodes,
                persistenceCode);
    }

    public EditorialRunContextCoordinatorStage stage() { return stage; }
    public EditorialRunContextCoordinatorCode code() { return code; }
    public boolean retryable() { return retryable; }
    public String closedRunIdentity() { return closedRunIdentity; }
    public String lineageIdentity() { return lineageIdentity; }
    public String bindingIdentity() { return bindingIdentity; }
    public String detail() { return detail; }
    public EditorialLineageCreationCode lineageCreationCode() { return lineageCreationCode; }
    public List<EditorialLineageValidationCode> validationCodes() { return validationCodes; }
    public EditorialIdentityPersistenceCode persistenceCode() { return persistenceCode; }

    private static String safe(String value) { return value == null ? "" : value; }
}
