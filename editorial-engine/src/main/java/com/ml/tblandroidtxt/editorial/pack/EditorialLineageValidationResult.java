package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable, deterministically ordered lineage validation result. */
public final class EditorialLineageValidationResult {
    private static final Comparator<EditorialLineageValidationIssue> ISSUE_ORDER =
            Comparator.comparingInt((EditorialLineageValidationIssue issue) -> issue.code().ordinal())
                    .thenComparing(EditorialLineageValidationIssue::path)
                    .thenComparing(EditorialLineageValidationIssue::detail);

    private final List<EditorialLineageValidationIssue> issues;

    private EditorialLineageValidationResult(List<EditorialLineageValidationIssue> issues) {
        ArrayList<EditorialLineageValidationIssue> ordered = new ArrayList<>(issues);
        ordered.sort(ISSUE_ORDER);
        this.issues = Collections.unmodifiableList(ordered);
    }

    public static EditorialLineageValidationResult valid() {
        return new EditorialLineageValidationResult(List.of());
    }

    public static EditorialLineageValidationResult invalid(List<EditorialLineageValidationIssue> issues) {
        Objects.requireNonNull(issues, "issues");
        if (issues.isEmpty()) throw new IllegalArgumentException("Invalid result requires issues");
        return new EditorialLineageValidationResult(issues);
    }

    public boolean isValid() { return issues.isEmpty(); }

    public EditorialLineageValidationCode code() {
        return isValid() ? EditorialLineageValidationCode.VALID : issues.get(0).code();
    }

    public List<EditorialLineageValidationIssue> issues() { return issues; }

    public List<EditorialLineageValidationCode> failureCodes() {
        Set<EditorialLineageValidationCode> unique = new LinkedHashSet<>();
        for (EditorialLineageValidationIssue issue : issues) unique.add(issue.code());
        return List.copyOf(unique);
    }
}
