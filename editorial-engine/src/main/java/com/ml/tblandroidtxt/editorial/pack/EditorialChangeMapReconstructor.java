package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * App-owned reconstruction of an edited text from its base text and a declared
 * change map (L2: DRAFT to VI_L2; L3: VI_L2 to FINAL).
 *
 * <p>The model never supplies the edited text or any hash. It declares
 * line-anchored replacements; the app checks every anchor against the exact
 * base bytes, applies the closed changes, recomputes the actual diff and
 * materializes the canonical change map. Dialogue changes without a complete
 * Speaker Proof and changes touching a protected line are reverted to the base
 * line (workflow: "edit thiếu proof phải hoàn nguyên"), never silently kept.
 * Malformed or stale declarations reject the whole set as REPAIR_REQUIRED.</p>
 */
public final class EditorialChangeMapReconstructor {
    public enum Kind {
        L2_DRAFT_TO_VI_L2("safe4.full.change-map-l2.v1", "CHANGE_MAP_L2", "DRAFT", "VI_L2"),
        L3_VI_L2_TO_FINAL("safe4.full.qa-change-map.v1", "QA_CHANGE_MAP", "VI_L2", "FINAL");

        private final String schemaVersion;
        private final String artifactType;
        private final String baseRole;
        private final String outputRole;

        Kind(String schemaVersion, String artifactType, String baseRole, String outputRole) {
            this.schemaVersion = schemaVersion;
            this.artifactType = artifactType;
            this.baseRole = baseRole;
            this.outputRole = outputRole;
        }

        public String schemaVersion() { return schemaVersion; }
        public String artifactType() { return artifactType; }
        public String baseRole() { return baseRole; }
        public String outputRole() { return outputRole; }
    }

    public enum DeclaredStatus { CLOSED, REVERTED }

    public enum Status { ACCEPTED, REPAIR_REQUIRED, INPUT_REQUIRED }

    public static final String REVERT_SPEAKER_PROOF_MISSING = "SPEAKER_PROOF_MISSING";
    public static final String REVERT_PROTECTED_SPAN_TOUCHED = "PROTECTED_SPAN_TOUCHED";
    public static final String REVERT_DECLARED = "DECLARED_REVERTED";

    public record SpeakerProof(String speaker, String listener, String anchorBefore,
                               String anchorAfter) {
        public boolean complete() {
            return !blank(speaker) && !blank(listener) && !blank(anchorBefore) && !blank(anchorAfter);
        }
    }

    public record ChangeRow(String changeId, String errorId, int lineNumber, String before,
                            String after, String reason, boolean dialogue,
                            SpeakerProof speakerProof, DeclaredStatus status) { }

    public record PreservedRow(String preserveId, int lineNumber, String before,
                               String evidenceLimit) { }

    public record Request(Kind kind, byte[] baseBytes, String predecessorIdentity,
                          List<ChangeRow> changes, List<PreservedRow> preserved,
                          Set<Integer> protectedLineNumbers) {
        public Request {
            Objects.requireNonNull(kind, "kind");
            changes = changes == null ? List.of() : List.copyOf(changes);
            preserved = preserved == null ? List.of() : List.copyOf(preserved);
            protectedLineNumbers = protectedLineNumbers == null
                    ? Set.of() : Set.copyOf(protectedLineNumbers);
        }
    }

    public record AppliedChange(ChangeRow row, String beforeHash, String afterHash) { }

    public record RevertedChange(ChangeRow row, String revertReason) { }

    public record Result(Status status, List<String> issues, byte[] outputBytes,
                         String baseSha256, String outputSha256,
                         List<AppliedChange> applied, List<RevertedChange> reverted,
                         List<PreservedRow> preserved, List<Integer> actualChangedLines,
                         byte[] changeMapBytes) {
        public Result {
            issues = List.copyOf(issues == null ? List.of() : issues);
            applied = List.copyOf(applied == null ? List.of() : applied);
            reverted = List.copyOf(reverted == null ? List.of() : reverted);
            preserved = List.copyOf(preserved == null ? List.of() : preserved);
            actualChangedLines = List.copyOf(actualChangedLines == null ? List.of() : actualChangedLines);
            outputBytes = outputBytes == null ? null : outputBytes.clone();
            changeMapBytes = changeMapBytes == null ? null : changeMapBytes.clone();
        }

        public boolean accepted() { return status == Status.ACCEPTED; }
        @Override public byte[] outputBytes() { return outputBytes == null ? null : outputBytes.clone(); }
        @Override public byte[] changeMapBytes() { return changeMapBytes == null ? null : changeMapBytes.clone(); }
    }

    /** Line content plus its original terminator, so untouched bytes are reproduced exactly. */
    private record Line(String content, String terminator) { }

    public Result reconstruct(Request request) {
        Objects.requireNonNull(request, "request");
        if (request.baseBytes() == null) {
            return failed(Status.INPUT_REQUIRED, List.of("BASE_TEXT_MISSING"), "");
        }
        if (!EditorialP5RawWireContract.token(request.predecessorIdentity(), 128)) {
            return failed(Status.INPUT_REQUIRED, List.of("PREDECESSOR_IDENTITY_INVALID"), "");
        }
        String baseSha256 = EditorialCanonicalJson.sha256Hex(request.baseBytes());
        String baseText;
        try {
            baseText = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(request.baseBytes())).toString();
        } catch (CharacterCodingException error) {
            return failed(Status.INPUT_REQUIRED, List.of("BASE_TEXT_NOT_UTF8"), baseSha256);
        }
        List<Line> lines = split(baseText);

        List<String> issues = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Map<Integer, String> closedLineOwners = new HashMap<>();
        for (ChangeRow row : request.changes()) {
            String id = row == null ? "" : row.changeId();
            if (row == null) {
                issues.add("CHANGE_ROW_MISSING");
                continue;
            }
            if (!EditorialP5RawWireContract.token(row.changeId(), EditorialP5RawWireContract.MAX_ID_LENGTH)) {
                issues.add("CHANGE_ID_INVALID");
                continue;
            }
            if (!ids.add(row.changeId())) issues.add("CHANGE_ID_DUPLICATE:" + id);
            if (!EditorialP5RawWireContract.token(row.errorId(), EditorialP5RawWireContract.MAX_ID_LENGTH)) {
                issues.add("CHANGE_ERROR_ID_INVALID:" + id);
            }
            if (row.status() == null) issues.add("CHANGE_STATUS_MISSING:" + id);
            if (blank(row.reason())) issues.add("CHANGE_REASON_MISSING:" + id);
            if (row.after() == null || row.after().indexOf('\n') >= 0 || row.after().indexOf('\r') >= 0) {
                issues.add("CHANGE_AFTER_INVALID:" + id);
            }
            if (row.lineNumber() < 1 || row.lineNumber() > lines.size()) {
                issues.add("CHANGE_LINE_OUT_OF_RANGE:" + id);
                continue;
            }
            if (!lines.get(row.lineNumber() - 1).content().equals(row.before())) {
                issues.add("CHANGE_ANCHOR_MISMATCH:" + id);
            }
            if (row.status() == DeclaredStatus.CLOSED) {
                if (Objects.equals(row.before(), row.after())) issues.add("CHANGE_NO_OP:" + id);
                String owner = closedLineOwners.putIfAbsent(row.lineNumber(), row.changeId());
                if (owner != null) issues.add("CHANGE_LINE_CONFLICT:" + owner + "," + id);
            }
        }
        Set<String> preserveIds = new HashSet<>();
        for (PreservedRow row : request.preserved()) {
            if (row == null || !EditorialP5RawWireContract.token(row.preserveId(),
                    EditorialP5RawWireContract.MAX_ID_LENGTH)) {
                issues.add("PRESERVE_ID_INVALID");
                continue;
            }
            String id = row.preserveId();
            if (!preserveIds.add(id) || ids.contains(id)) issues.add("PRESERVE_ID_DUPLICATE:" + id);
            if (blank(row.evidenceLimit())) issues.add("PRESERVE_EVIDENCE_LIMIT_MISSING:" + id);
            if (row.lineNumber() < 1 || row.lineNumber() > lines.size()) {
                issues.add("PRESERVE_LINE_OUT_OF_RANGE:" + id);
                continue;
            }
            if (!lines.get(row.lineNumber() - 1).content().equals(row.before())) {
                issues.add("PRESERVE_ANCHOR_MISMATCH:" + id);
            }
        }
        if (!issues.isEmpty()) return failed(Status.REPAIR_REQUIRED, issues, baseSha256);

        List<AppliedChange> applied = new ArrayList<>();
        List<RevertedChange> reverted = new ArrayList<>();
        TreeMap<Integer, String> replacements = new TreeMap<>();
        for (ChangeRow row : request.changes()) {
            if (row.status() == DeclaredStatus.REVERTED) {
                reverted.add(new RevertedChange(row, REVERT_DECLARED));
            } else if (row.dialogue() && (row.speakerProof() == null || !row.speakerProof().complete())) {
                reverted.add(new RevertedChange(row, REVERT_SPEAKER_PROOF_MISSING));
            } else if (request.protectedLineNumbers().contains(row.lineNumber())) {
                reverted.add(new RevertedChange(row, REVERT_PROTECTED_SPAN_TOUCHED));
            } else {
                replacements.put(row.lineNumber(), row.after());
                applied.add(new AppliedChange(row, lineHash(row.before()), lineHash(row.after())));
            }
        }
        for (PreservedRow row : request.preserved()) {
            if (replacements.containsKey(row.lineNumber())) {
                issues.add("PRESERVE_LINE_CHANGED:" + row.preserveId());
            }
        }
        if (!issues.isEmpty()) return failed(Status.REPAIR_REQUIRED, issues, baseSha256);

        StringBuilder out = new StringBuilder(baseText.length() + 64);
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            out.append(replacements.getOrDefault(i + 1, line.content())).append(line.terminator());
        }
        String outputText = out.toString();
        byte[] outputBytes = outputText.getBytes(StandardCharsets.UTF_8);

        // Defensive cross-check with the independent diff: every actual changed
        // line must be exactly one applied change and nothing else.
        List<Integer> actual = new ArrayList<>();
        for (EditorialDiffValidator.ChangedSpan span : new EditorialDiffValidator().compute(baseText, outputText)) {
            actual.add(span.lineNumber());
        }
        if (!actual.equals(new ArrayList<>(replacements.keySet()))) {
            return failed(Status.REPAIR_REQUIRED, List.of("DIFF_RECONSTRUCTION_MISMATCH"), baseSha256);
        }
        String outputSha256 = EditorialCanonicalJson.sha256Hex(outputBytes);
        byte[] changeMap = changeMapBytes(request, baseSha256, outputSha256, applied, reverted, actual);
        return new Result(Status.ACCEPTED, List.of(), outputBytes, baseSha256, outputSha256,
                applied, reverted, request.preserved(), actual, changeMap);
    }

    private static byte[] changeMapBytes(Request request, String baseSha256, String outputSha256,
                                         List<AppliedChange> applied, List<RevertedChange> reverted,
                                         List<Integer> actual) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", request.kind().schemaVersion());
        root.put("artifactType", request.kind().artifactType());
        root.put("predecessorIdentity", request.predecessorIdentity());
        root.put("baseRole", request.kind().baseRole());
        root.put("baseSha256", baseSha256);
        root.put("outputRole", request.kind().outputRole());
        root.put("outputSha256", outputSha256);
        List<Object> rows = new ArrayList<>();
        for (AppliedChange change : applied) {
            Map<String, Object> row = rowMap(change.row());
            row.put("beforeHash", change.beforeHash());
            row.put("afterHash", change.afterHash());
            row.put("status", "CLOSED");
            rows.add(row);
        }
        for (RevertedChange change : reverted) {
            Map<String, Object> row = rowMap(change.row());
            row.put("status", "REVERTED");
            row.put("revertReason", change.revertReason());
            rows.add(row);
        }
        rows.sort((a, b) -> ((String) ((Map<?, ?>) a).get("changeId"))
                .compareTo((String) ((Map<?, ?>) b).get("changeId")));
        root.put("changes", rows);
        List<Object> preserved = new ArrayList<>();
        for (PreservedRow row : request.preserved()) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("preserveId", row.preserveId());
            value.put("lineNumber", BigDecimal.valueOf(row.lineNumber()));
            value.put("beforeHash", lineHash(row.before()));
            value.put("evidenceLimit", row.evidenceLimit());
            preserved.add(value);
        }
        root.put("preserved", preserved);
        List<Object> changedLines = new ArrayList<>();
        for (Integer line : actual) changedLines.add(BigDecimal.valueOf(line));
        root.put("actualChangedLines", changedLines);
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("applied", BigDecimal.valueOf(applied.size()));
        counts.put("reverted", BigDecimal.valueOf(reverted.size()));
        counts.put("preserved", BigDecimal.valueOf(request.preserved().size()));
        counts.put("actualChangedLines", BigDecimal.valueOf(actual.size()));
        counts.put("unaccountedChangedLines", BigDecimal.ZERO);
        counts.put("protectedSpanRegressions", BigDecimal.ZERO);
        counts.put("dialogueChangesWithoutProof", BigDecimal.ZERO);
        root.put("counts", counts);
        return EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, Object> rowMap(ChangeRow row) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("changeId", row.changeId());
        value.put("errorId", row.errorId());
        value.put("lineNumber", BigDecimal.valueOf(row.lineNumber()));
        value.put("reason", row.reason());
        value.put("dialogue", row.dialogue());
        if (row.speakerProof() != null && row.speakerProof().complete()) {
            Map<String, Object> proof = new LinkedHashMap<>();
            proof.put("speaker", row.speakerProof().speaker());
            proof.put("listener", row.speakerProof().listener());
            proof.put("anchorBefore", row.speakerProof().anchorBefore());
            proof.put("anchorAfter", row.speakerProof().anchorAfter());
            value.put("speakerProof", proof);
        }
        return value;
    }

    private static List<Line> split(String text) {
        List<Line> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                boolean crlf = i > start && text.charAt(i - 1) == '\r';
                lines.add(new Line(text.substring(start, crlf ? i - 1 : i), crlf ? "\r\n" : "\n"));
                start = i + 1;
            }
        }
        lines.add(new Line(text.substring(start), ""));
        return lines;
    }

    private static String lineHash(String value) {
        return EditorialCanonicalJson.sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    private static Result failed(Status status, List<String> issues, String baseSha256) {
        return new Result(status, new ArrayList<>(new TreeSet<>(issues)), null, baseSha256, "",
                List.of(), List.of(), List.of(), List.of(), null);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
