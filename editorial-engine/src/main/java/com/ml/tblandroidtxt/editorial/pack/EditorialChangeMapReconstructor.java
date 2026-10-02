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
 * line-anchored operations; the app checks every anchor against the exact
 * base bytes, applies the closed changes, recomputes the actual diff and
 * materializes the canonical change map. Dialogue changes without a complete
 * Speaker Proof and changes touching a protected line are reverted to the base
 * line (workflow: "edit thiếu proof phải hoàn nguyên"), never silently kept.
 * Malformed or stale declarations reject the whole set as REPAIR_REQUIRED.</p>
 *
 * <p>Operations (ledger contract): {@code REPLACE} (default, one whole line), {@code INSERT_AFTER n}
 * (a new line after base line n, n = 0 inserts first; {@code before} is the text of line n),
 * {@code DELETE n} ({@code after} is empty) and {@code MERGE_WITH_NEXT n} (lines n and n+1 become the
 * single line {@code after}). A change set with only {@code REPLACE} reproduces the original behaviour and
 * the original change-map bytes. No operation edits inside a line by offset.</p>
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

    public enum Op { REPLACE, INSERT_AFTER, DELETE, MERGE_WITH_NEXT }

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
                            SpeakerProof speakerProof, DeclaredStatus status, Op op) {
        public ChangeRow {
            if (op == null) op = Op.REPLACE;
        }

        /** The original single-line replacement row. */
        public ChangeRow(String changeId, String errorId, int lineNumber, String before,
                         String after, String reason, boolean dialogue,
                         SpeakerProof speakerProof, DeclaredStatus status) {
            this(changeId, errorId, lineNumber, before, after, reason, dialogue, speakerProof, status, Op.REPLACE);
        }
    }

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
                         byte[] changeMapBytes, List<Integer> lineMap) {
        public Result {
            issues = List.copyOf(issues == null ? List.of() : issues);
            applied = List.copyOf(applied == null ? List.of() : applied);
            reverted = List.copyOf(reverted == null ? List.of() : reverted);
            preserved = List.copyOf(preserved == null ? List.of() : preserved);
            actualChangedLines = List.copyOf(actualChangedLines == null ? List.of() : actualChangedLines);
            outputBytes = outputBytes == null ? null : outputBytes.clone();
            changeMapBytes = changeMapBytes == null ? null : changeMapBytes.clone();
            lineMap = List.copyOf(lineMap == null ? List.of() : lineMap);
        }

        public boolean accepted() { return status == Status.ACCEPTED; }
        @Override public byte[] outputBytes() { return outputBytes == null ? null : outputBytes.clone(); }
        @Override public byte[] changeMapBytes() { return changeMapBytes == null ? null : changeMapBytes.clone(); }
    }

    /** Line content plus its original terminator, so untouched bytes are reproduced exactly. */
    private record Line(String content, String terminator) { }

    /** One applied operation as the line map needs it. */
    record OpRef(Op op, int line) { }

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
        Map<String, String> owners = new HashMap<>();
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
            Op op = row.op();
            int size = lines.size();
            boolean inRange = switch (op) {
                case INSERT_AFTER -> row.lineNumber() >= 0 && row.lineNumber() <= size;
                case MERGE_WITH_NEXT -> row.lineNumber() >= 1 && row.lineNumber() < size;
                default -> row.lineNumber() >= 1 && row.lineNumber() <= size;
            };
            if (!inRange) {
                issues.add("CHANGE_LINE_OUT_OF_RANGE:" + id);
                continue;
            }
            String anchor = row.lineNumber() == 0 ? "" : lines.get(row.lineNumber() - 1).content();
            if (!anchor.equals(row.before())) {
                issues.add("CHANGE_ANCHOR_MISMATCH:" + id);
            }
            if (op == Op.DELETE && row.after() != null && !row.after().isEmpty()) {
                issues.add("CHANGE_DELETE_AFTER_NOT_EMPTY:" + id);
            }
            if ((op == Op.INSERT_AFTER || op == Op.MERGE_WITH_NEXT) && row.after() != null && row.after().isBlank()) {
                issues.add("CHANGE_AFTER_REQUIRED:" + id);
            }
            if (row.status() == DeclaredStatus.CLOSED) {
                if (op == Op.REPLACE && Objects.equals(row.before(), row.after())) issues.add("CHANGE_NO_OP:" + id);
                for (String key : ownedKeys(op, row.lineNumber())) {
                    String owner = owners.putIfAbsent(key, row.changeId());
                    if (owner != null) issues.add("CHANGE_LINE_CONFLICT:" + owner + "," + id);
                }
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
        for (ChangeRow row : request.changes()) {
            if (row.status() == DeclaredStatus.REVERTED) {
                reverted.add(new RevertedChange(row, REVERT_DECLARED));
            } else if (row.dialogue() && (row.speakerProof() == null || !row.speakerProof().complete())) {
                reverted.add(new RevertedChange(row, REVERT_SPEAKER_PROOF_MISSING));
            } else if (touchesProtected(row, request.protectedLineNumbers())) {
                reverted.add(new RevertedChange(row, REVERT_PROTECTED_SPAN_TOUCHED));
            } else {
                applied.add(new AppliedChange(row, lineHash(row.before()), lineHash(row.after())));
            }
        }
        Set<Integer> touched = new TreeSet<>();
        for (AppliedChange change : applied) {
            ChangeRow row = change.row();
            switch (row.op()) {
                case REPLACE, DELETE -> touched.add(row.lineNumber());
                case MERGE_WITH_NEXT -> {
                    touched.add(row.lineNumber());
                    touched.add(row.lineNumber() + 1);
                }
                case INSERT_AFTER -> { }
            }
        }
        for (PreservedRow row : request.preserved()) {
            if (touched.contains(row.lineNumber())) {
                issues.add("PRESERVE_LINE_CHANGED:" + row.preserveId());
            }
        }
        if (!issues.isEmpty()) return failed(Status.REPAIR_REQUIRED, issues, baseSha256);

        boolean structural = false;
        for (AppliedChange change : applied) structural |= change.row().op() != Op.REPLACE;

        String eol = lines.size() > 1 || !lines.get(0).terminator().isEmpty() ? firstTerminator(lines) : "\n";
        List<OpRef> opRefs = new ArrayList<>();
        Map<Integer, String> replace = new HashMap<>();
        Set<Integer> deleted = new HashSet<>();
        Map<Integer, String> merged = new HashMap<>();
        Map<Integer, String> insertAfter = new HashMap<>();
        for (AppliedChange change : applied) {
            ChangeRow row = change.row();
            opRefs.add(new OpRef(row.op(), row.lineNumber()));
            switch (row.op()) {
                case REPLACE -> replace.put(row.lineNumber(), row.after());
                case DELETE -> deleted.add(row.lineNumber());
                case MERGE_WITH_NEXT -> merged.put(row.lineNumber(), row.after());
                case INSERT_AFTER -> insertAfter.put(row.lineNumber(), row.after());
            }
        }

        List<Line> out = new ArrayList<>(lines.size() + insertAfter.size());
        if (insertAfter.containsKey(0)) out.add(new Line(insertAfter.get(0), eol));
        for (int number = 1; number <= lines.size(); number++) {
            Line line = lines.get(number - 1);
            if (merged.containsKey(number)) {
                out.add(new Line(merged.get(number), lines.get(number).terminator()));
                number++;
            } else if (deleted.contains(number)) {
                // the line and its terminator disappear
            } else {
                out.add(new Line(replace.getOrDefault(number, line.content()), line.terminator()));
            }
            if (insertAfter.containsKey(number)) {
                Line anchor = out.isEmpty() ? null : out.get(out.size() - 1);
                if (anchor != null && anchor.terminator().isEmpty()) {
                    out.set(out.size() - 1, new Line(anchor.content(), eol));
                    out.add(new Line(insertAfter.get(number), ""));
                } else {
                    out.add(new Line(insertAfter.get(number), anchor == null ? eol : anchor.terminator()));
                }
            }
        }
        StringBuilder text = new StringBuilder(baseText.length() + 64);
        for (Line line : out) text.append(line.content()).append(line.terminator());
        String outputText = text.toString();
        byte[] outputBytes = outputText.getBytes(StandardCharsets.UTF_8);

        List<Integer> actual = new ArrayList<>();
        if (!structural) {
            // Defensive cross-check with the independent diff: every actual changed
            // line must be exactly one applied change and nothing else.
            for (EditorialDiffValidator.ChangedSpan span : new EditorialDiffValidator().compute(baseText, outputText)) {
                actual.add(span.lineNumber());
            }
            if (!actual.equals(new ArrayList<>(replace.keySet().stream().sorted().toList()))) {
                return failed(Status.REPAIR_REQUIRED, List.of("DIFF_RECONSTRUCTION_MISMATCH"), baseSha256);
            }
        } else {
            // Positional diff cannot follow inserted or removed lines: replay the declared operations on the
            // line hashes with a separate code path and require the very same line sequence.
            List<HashOp> hashOps = new ArrayList<>();
            for (AppliedChange change : applied) {
                hashOps.add(new HashOp(change.row().op(), change.row().lineNumber(), change.afterHash()));
            }
            List<String> expected = hashReplay(lineHashes(lines), hashOps);
            List<String> observed = lineHashes(split(outputText));
            if (!expected.equals(observed)) {
                return failed(Status.REPAIR_REQUIRED, List.of("DIFF_RECONSTRUCTION_MISMATCH"), baseSha256);
            }
            Set<Integer> changed = new TreeSet<>(touched);
            for (Integer anchor : insertAfter.keySet()) changed.add(Math.max(1, anchor));
            actual.addAll(changed);
        }
        String outputSha256 = EditorialCanonicalJson.sha256Hex(outputBytes);
        byte[] changeMap = changeMapBytes(request, baseSha256, outputSha256, applied, reverted, actual, structural);
        return new Result(Status.ACCEPTED, List.of(), outputBytes, baseSha256, outputSha256,
                applied, reverted, request.preserved(), actual, changeMap, computeLineMap(lines.size(), opRefs));
    }

    private static boolean touchesProtected(ChangeRow row, Set<Integer> protectedLines) {
        return switch (row.op()) {
            case MERGE_WITH_NEXT -> protectedLines.contains(row.lineNumber()) || protectedLines.contains(row.lineNumber() + 1);
            // an insertion between two lines of one protected span lands inside that span
            case INSERT_AFTER -> protectedLines.contains(row.lineNumber()) && protectedLines.contains(row.lineNumber() + 1);
            default -> protectedLines.contains(row.lineNumber());
        };
    }

    private static List<String> ownedKeys(Op op, int line) {
        return switch (op) {
            case REPLACE, DELETE -> List.of("L" + line);
            case MERGE_WITH_NEXT -> List.of("L" + line, "L" + (line + 1), "S" + line);
            case INSERT_AFTER -> List.of("S" + line);
        };
    }

    /**
     * Base line number (index + 1) to the output line number that carries its content or its replacement;
     * 0 when the line was deleted. Both lines of a merge map to the merged output line.
     */
    static List<Integer> computeLineMap(int baseLineCount, List<OpRef> ops) {
        Set<Integer> deleted = new HashSet<>();
        Set<Integer> mergeStarts = new HashSet<>();
        Set<Integer> insertSlots = new HashSet<>();
        for (OpRef op : ops) {
            switch (op.op()) {
                case DELETE -> deleted.add(op.line());
                case MERGE_WITH_NEXT -> mergeStarts.add(op.line());
                case INSERT_AFTER -> insertSlots.add(op.line());
                case REPLACE -> { }
            }
        }
        List<Integer> map = new ArrayList<>(baseLineCount);
        for (int i = 0; i < baseLineCount; i++) map.add(0);
        int outputLine = insertSlots.contains(0) ? 1 : 0;
        for (int number = 1; number <= baseLineCount; number++) {
            if (mergeStarts.contains(number) && number < baseLineCount) {
                outputLine++;
                map.set(number - 1, outputLine);
                map.set(number, outputLine);
                number++;
            } else if (deleted.contains(number)) {
                map.set(number - 1, 0);
            } else {
                outputLine++;
                map.set(number - 1, outputLine);
            }
            if (insertSlots.contains(number)) outputLine++;
        }
        return List.copyOf(map);
    }

    /**
     * Line map of a persisted change map (CLOSED rows only); no text is needed because the map keeps the
     * operation and the base line of every applied change.
     */
    public static List<Integer> lineMapFromChangeMap(int baseLineCount, byte[] changeMapBytes) {
        List<OpRef> ops = new ArrayList<>();
        Map<String, Object> map = EditorialCanonicalJson.parseObject(changeMapBytes);
        for (Object value : EditorialCanonicalJson.array(map.get("changes"), "changes")) {
            Map<String, Object> row = EditorialCanonicalJson.object(value, "change");
            if (!"CLOSED".equals(row.get("status"))) continue;
            Object opName = row.get("op");
            Op op = opName == null ? Op.REPLACE : Op.valueOf((String) opName);
            ops.add(new OpRef(op, ((BigDecimal) row.get("lineNumber")).intValueExact()));
        }
        return computeLineMap(baseLineCount, ops);
    }

    /** SHA-256 of each line's content, in order, using the same line splitting as the reconstruction. */
    public static List<String> lineHashes(byte[] bytes) {
        return lineHashes(split(new String(bytes, StandardCharsets.UTF_8)));
    }

    private static List<String> lineHashes(List<Line> lines) {
        List<String> hashes = new ArrayList<>(lines.size());
        for (Line line : lines) hashes.add(lineHash(line.content()));
        return hashes;
    }

    /** One applied operation at the hash level: what a persisted change map keeps of a CLOSED row. */
    private record HashOp(Op op, int line, String afterHash) { }

    /**
     * True when {@code outputBytes} is exactly {@code baseBytes} with the CLOSED rows of the persisted change map
     * applied, judged on line hashes only (the map keeps operation, base line and hash of the new line). Used to
     * re-verify a committed stage and to account for every line between DRAFT, VI_L2 and FINAL.
     */
    public static boolean replayMatches(byte[] baseBytes, byte[] outputBytes, byte[] changeMapBytes) {
        try {
            List<HashOp> ops = new ArrayList<>();
            Map<String, Object> map = EditorialCanonicalJson.parseObject(changeMapBytes);
            for (Object value : EditorialCanonicalJson.array(map.get("changes"), "changes")) {
                Map<String, Object> row = EditorialCanonicalJson.object(value, "change");
                if (!"CLOSED".equals(row.get("status"))) continue;
                Object opName = row.get("op");
                ops.add(new HashOp(opName == null ? Op.REPLACE : Op.valueOf((String) opName),
                        ((BigDecimal) row.get("lineNumber")).intValueExact(), (String) row.get("afterHash")));
            }
            return hashReplay(lineHashes(baseBytes), ops).equals(lineHashes(outputBytes));
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    /** Second, text-free application of the declared operations to line hashes (cross-check only). */
    private static List<String> hashReplay(List<String> baseHashes, List<HashOp> ops) {
        Map<Integer, String> replace = new HashMap<>();
        Set<Integer> deleted = new HashSet<>();
        Map<Integer, String> merged = new HashMap<>();
        Map<Integer, String> inserted = new HashMap<>();
        for (HashOp op : ops) {
            switch (op.op()) {
                case REPLACE -> replace.put(op.line(), op.afterHash());
                case DELETE -> deleted.add(op.line());
                case MERGE_WITH_NEXT -> merged.put(op.line(), op.afterHash());
                case INSERT_AFTER -> inserted.put(op.line(), op.afterHash());
            }
        }
        List<String> out = new ArrayList<>();
        if (inserted.containsKey(0)) out.add(inserted.get(0));
        int number = 1;
        while (number <= baseHashes.size()) {
            if (merged.containsKey(number)) {
                out.add(merged.get(number));
                // the merge owns the slot after its first line; a slot after the second line is still free
                number++;
                if (inserted.containsKey(number)) out.add(inserted.get(number));
                number++;
                continue;
            }
            if (!deleted.contains(number)) out.add(replace.getOrDefault(number, baseHashes.get(number - 1)));
            if (inserted.containsKey(number)) out.add(inserted.get(number));
            number++;
        }
        return out;
    }

    private static byte[] changeMapBytes(Request request, String baseSha256, String outputSha256,
                                         List<AppliedChange> applied, List<RevertedChange> reverted,
                                         List<Integer> actual, boolean structural) {
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
        if (structural) {
            int inserts = 0, deletes = 0, merges = 0;
            for (AppliedChange change : applied) {
                switch (change.row().op()) {
                    case INSERT_AFTER -> inserts++;
                    case DELETE -> deletes++;
                    case MERGE_WITH_NEXT -> merges++;
                    case REPLACE -> { }
                }
            }
            Map<String, Object> structure = new LinkedHashMap<>();
            structure.put("insertAfter", BigDecimal.valueOf(inserts));
            structure.put("delete", BigDecimal.valueOf(deletes));
            structure.put("mergeWithNext", BigDecimal.valueOf(merges));
            counts.put("structuralOps", structure);
        }
        root.put("counts", counts);
        return EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, Object> rowMap(ChangeRow row) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("changeId", row.changeId());
        value.put("errorId", row.errorId());
        value.put("lineNumber", BigDecimal.valueOf(row.lineNumber()));
        if (row.op() != Op.REPLACE) value.put("op", row.op().name());
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

    private static String firstTerminator(List<Line> lines) {
        for (Line line : lines) if (!line.terminator().isEmpty()) return line.terminator();
        return "\n";
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
                List.of(), List.of(), List.of(), List.of(), null, List.of());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
