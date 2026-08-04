package com.ml.tblandroidtxt.editorial.pack;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Immutable machine-readable result of trusted profile validation. */
public final class EditorialEngineContractProfileValidationResult {
    public enum Code {
        INPUT_NULL,
        SIZE_LIMIT,
        PARSE_ERROR,
        UNKNOWN_FIELD,
        MISSING_FIELD,
        WRONG_TYPE,
        COLLECTION_LIMIT,
        STRING_LIMIT,
        INVALID_FORMAT,
        INVALID_ID,
        INVALID_VERSION,
        INVALID_HASH,
        INVALID_FINGERPRINT,
        DUPLICATE_VALUE,
        CAPABILITY_OVERLAP,
        UNKNOWN_CAPABILITY,
        MISSING_CAPABILITY_EVIDENCE,
        INVALID_CAPABILITY_EVIDENCE,
        DANGLING_PHASE,
        INCOMPLETE_CONTEXT,
        CONTRACT_SCHEMA_MISMATCH,
        INVALID_DESCRIPTOR,
        INVALID_ADAPTER,
        INVALID_DEPRECATION,
        CANONICAL_HASH_MISMATCH,
        MACHINE_FINGERPRINT_MISMATCH
    }

    public record Issue(Code code, String path, String message) {
        public Issue {
            if (code == null || path == null || message == null) throw new NullPointerException("Validation issue fields are required");
        }
    }

    private final EditorialEngineContractProfile profile;
    private final List<Issue> issues;

    private EditorialEngineContractProfileValidationResult(
            EditorialEngineContractProfile profile, List<Issue> issues) {
        this.profile = profile;
        this.issues = Collections.unmodifiableList(List.copyOf(issues));
    }

    public static EditorialEngineContractProfileValidationResult valid(EditorialEngineContractProfile profile) {
        return new EditorialEngineContractProfileValidationResult(profile, List.of());
    }

    public static EditorialEngineContractProfileValidationResult invalid(List<Issue> issues) {
        return new EditorialEngineContractProfileValidationResult(null, issues);
    }

    public boolean isValid() { return issues.isEmpty(); }
    public Optional<EditorialEngineContractProfile> profile() { return Optional.ofNullable(profile); }
    public List<Issue> issues() { return issues; }

    public boolean hasCode(Code code) {
        return issues.stream().anyMatch(issue -> issue.code() == code);
    }
}
