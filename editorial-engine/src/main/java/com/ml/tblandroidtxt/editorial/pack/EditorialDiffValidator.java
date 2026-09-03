package com.ml.tblandroidtxt.editorial.pack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Computes changed spans and coverage from actual before/after bytes. */
public final class EditorialDiffValidator {
    public record DeclaredChange(int lineNumber, String beforeHash, String afterHash,
                                 String errorId) {
        public DeclaredChange {
            if (lineNumber < 1) throw new IllegalArgumentException("lineNumber must be positive");
            Objects.requireNonNull(beforeHash, "beforeHash");
            Objects.requireNonNull(afterHash, "afterHash");
            Objects.requireNonNull(errorId, "errorId");
        }
    }

    public record ChangedSpan(int lineNumber, String beforeHash, String afterHash) { }

    public record Result(boolean valid, List<ChangedSpan> actualChangedSpans,
                         List<String> issues) {
        public Result {
            actualChangedSpans = List.copyOf(actualChangedSpans == null ? List.of() : actualChangedSpans);
            issues = List.copyOf(issues == null ? List.of() : issues);
        }
    }

    public Result validate(String before, String after, List<DeclaredChange> declaredChanges,
                           boolean modelDeclaredPass) {
        if (before == null || after == null) return new Result(false, List.of(), List.of("DIFF_INPUT_MISSING"));
        List<ChangedSpan> actual = compute(before, after);
        List<String> issues = new ArrayList<>();
        List<DeclaredChange> declared = declaredChanges == null ? List.of() : declaredChanges;
        if (declared.size() != actual.size()) issues.add("DIFF_DECLARED_COUNT_MISMATCH");
        int count = Math.min(declared.size(), actual.size());
        for (int i = 0; i < count; i++) {
            DeclaredChange expected = declared.get(i);
            ChangedSpan observed = actual.get(i);
            if (expected.lineNumber() != observed.lineNumber()
                    || !expected.beforeHash().equals(observed.beforeHash())
                    || !expected.afterHash().equals(observed.afterHash())) {
                issues.add("DIFF_CHANGED_SPAN_MISMATCH:" + expected.lineNumber());
            }
            if (expected.errorId().isBlank()) issues.add("DIFF_ERROR_MAPPING_MISSING:" + expected.lineNumber());
        }
        // modelDeclaredPass is intentionally not read: only the actual comparison above decides validity.
        return new Result(issues.isEmpty(), actual, issues);
    }

    public List<ChangedSpan> compute(String before, String after) {
        if (before == null || after == null) throw new IllegalArgumentException("Diff inputs are required");
        String[] beforeLines = before.split("\\r?\\n", -1);
        String[] afterLines = after.split("\\r?\\n", -1);
        int count = Math.max(beforeLines.length, afterLines.length);
        List<ChangedSpan> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String beforeLine = i < beforeLines.length ? beforeLines[i] : "";
            String afterLine = i < afterLines.length ? afterLines[i] : "";
            if (!beforeLine.equals(afterLine)) {
                result.add(new ChangedSpan(i + 1, hashLine(beforeLine), hashLine(afterLine)));
            }
        }
        return List.copyOf(result);
    }

    private static String hashLine(String value) {
        return EditorialCanonicalJson.sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }
}
