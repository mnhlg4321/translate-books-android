package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The structural gate of one chunk-pair candidate (package CP-OFFLINE-2, section 4). It decides only whether a response is
 * trustworthy enough to merge and hand over without a person looking; it never says that the meaning is right or wrong.
 * Every threshold is a suspicion fence. A change in the number of lines never blocks by itself (it is a warning); a block
 * rests on lost or added text, a lost marker, a broken envelope or identity. Integer arithmetic only. Pure.
 */
public final class StructuralGate {
    public enum Status { PASS, WARN, BLOCK }

    public enum Severity { WARN, BLOCK }

    public record Code(String gate, String code, Severity severity, String detail) { }

    /**
     * @param draftRange     the trimmed DRAFT text of the pair
     * @param candidateRaw   the text between the EDITED tags exactly as sent, or {@code null} when there is none
     * @param parseStatus    result of {@link EditResponseParser#parse}
     * @param expectedRequestId / responseRequestId what the app sent with the request and what came back tagged to it
     * @param hardLimitChars fixture hard limit on candidate letters, {@code 0} = none
     */
    public record Input(String draftRange, String candidateRaw, EditResponseParser.Status parseStatus, String finishReason,
                        String expectedRequestId, String responseRequestId, int hardLimitChars) { }

    public record Result(Status status, List<Code> codes, String candidate, int draftLetters, int candidateLetters, int draftLines,
                         int candidateLines, boolean verbatim, boolean reflowOnly, String internalLayoutFingerprint) {
        public Result {
            codes = List.copyOf(codes);
        }

        public boolean has(String code) {
            for (Code c : codes) if (c.code().equals(code)) return true;
            return false;
        }

        public List<String> warnCodes() {
            List<String> out = new ArrayList<>();
            for (Code c : codes) if (c.severity() == Severity.WARN) out.add(c.code());
            return out;
        }

        public List<String> blockCodes() {
            List<String> out = new ArrayList<>();
            for (Code c : codes) if (c.severity() == Severity.BLOCK) out.add(c.code());
            return out;
        }
    }

    /** Characters that make a line a boundary marker when the whole line is made of them. */
    static final String MARKER_CHARS = "◆◇■□●○★☆※＊*─━―＝=~〜-・";
    private static final int MARKER_MAX_LENGTH = 16;
    private static final java.util.regex.Pattern LABEL = java.util.regex.Pattern.compile(
            java.util.regex.Pattern.quote(PairContract.LABEL_OPEN) + "[0-9]{3}" + java.util.regex.Pattern.quote(PairContract.LABEL_CLOSE));

    private StructuralGate() { }

    public static Result check(Input in) {
        List<Code> codes = new ArrayList<>();
        String draft = PairText.normalize(in.draftRange());
        int d = PairText.letters(draft);
        int draftLines = PairText.nonBlankLines(draft);

        // IDENTITY
        if (in.expectedRequestId() != null && in.responseRequestId() != null && !in.expectedRequestId().equals(in.responseRequestId())) {
            codes.add(new Code("IDENTITY", "PAIR_ID_MISMATCH", Severity.BLOCK, "response tagged to another request"));
        }
        // ENVELOPE
        if (in.parseStatus() != EditResponseParser.Status.OK) {
            codes.add(new Code("ENVELOPE", in.parseStatus() == null ? "NO_PARSE" : in.parseStatus().name(), Severity.BLOCK, "not a usable EDITED answer"));
        }
        String finish = in.finishReason() == null ? "" : in.finishReason().trim();
        if (!finish.isEmpty() && !finish.equalsIgnoreCase("stop")) {
            codes.add(new Code("ENVELOPE", "FINISH_REASON", Severity.BLOCK, finish));
        }
        String body = in.candidateRaw() == null ? "" : PairText.normalize(in.candidateRaw());
        String trimmed = PairText.trim(body);
        if (!trimmed.equals(body) && !trimmed.isEmpty()) {
            String lead = body.substring(0, PairText.leadingWhitespace(body));
            String trail = body.substring(PairText.trailingWhitespaceStart(body));
            codes.add(new Code("BOUNDARY", "BOUNDARY_WS_TRIMMED", Severity.WARN, PairText.sha256(lead + "\u0001" + trail)));
        }
        // META
        List<EditGuards.Flag> meta = new ArrayList<>();
        String candidate = trimmed.isEmpty() ? "" : EditGuards.stripMeta(trimmed, meta);
        for (EditGuards.Flag f : meta) {
            codes.add(new Code("META", f.code().name(), f.code() == EditGuards.Code.META_UNSEPARABLE ? Severity.BLOCK : Severity.WARN, f.detail()));
        }
        if (LABEL.matcher(candidate).find()) {
            codes.add(new Code("META", "PARAGRAPH_LABEL_LEAK", Severity.BLOCK, "RAW paragraph label copied into EDITED"));
        }
        candidate = PairText.trim(candidate);
        int c = PairText.letters(candidate);
        int candidateLines = PairText.nonBlankLines(candidate);

        boolean verbatim = !candidate.isEmpty() && candidate.equals(draft);
        boolean reflowOnly = false;
        String layout = layoutFingerprint(candidate);
        if (c == 0) {
            if (in.parseStatus() == EditResponseParser.Status.OK || in.candidateRaw() != null) {
                codes.add(new Code("SIZE", "CANDIDATE_EMPTY", Severity.BLOCK, "no text"));
            }
        } else if (!verbatim) {
            // SIZE on letters
            if (in.hardLimitChars() > 0 && c > in.hardLimitChars()) {
                codes.add(new Code("SIZE", "HARD_LIMIT", Severity.BLOCK, c + " letters"));
            }
            if (d >= PairContract.RATIO_MIN_CHARS) {
                if (2L * c < d) codes.add(new Code("SIZE", "CHARS_LOSS", Severity.BLOCK, c + " of " + d + " letters"));
                else if ((long) c > 2L * d) codes.add(new Code("SIZE", "CHARS_GROWTH", Severity.BLOCK, c + " of " + d + " letters"));
                else if (5L * c < 4L * d) codes.add(new Code("SIZE", "CHARS_LOSS_WARN", Severity.WARN, c + " of " + d + " letters"));
                else if (4L * c > 5L * d) codes.add(new Code("SIZE", "CHARS_GROWTH_WARN", Severity.WARN, c + " of " + d + " letters"));
            } else if (c > PairContract.SHORT_MAX_CANDIDATE_CHARS) {
                codes.add(new Code("SIZE", "CHARS_GROWTH", Severity.BLOCK, c + " letters for a short range"));
            }
            // line count: a warning, never a block
            int delta = Math.abs(candidateLines - draftLines);
            boolean lineWarn = draftLines >= 1 && draftLines <= 5 ? delta >= 1 : delta >= Math.max(2, (int) Math.ceil(0.10 * draftLines));
            if (lineWarn) codes.add(new Code("SIZE", "LINE_DELTA", Severity.WARN, draftLines + " -> " + candidateLines + " lines"));
            // markers
            markers(draft, candidate, codes);
            // layout: only the way lines are broken differs
            boolean sameLetters = PairText.lettersOnly(candidate).equals(PairText.lettersOnly(draft));
            String draftLayout = layoutFingerprint(draft);
            if (sameLetters && (candidateLines != draftLines || !layout.equals(draftLayout))) {
                reflowOnly = true;
                codes.add(new Code("LAYOUT", "REFLOW_ONLY", Severity.WARN, "same letters, different line breaks"));
            } else if (!sameLetters && candidateLines == draftLines && !layout.equals(draftLayout)) {
                codes.add(new Code("LAYOUT", "BLANK_RUN_CHANGED", Severity.WARN, "blank lines inside the text changed"));
            }
        }
        Status status = Status.PASS;
        for (Code code : codes) {
            if (code.severity() == Severity.BLOCK) { status = Status.BLOCK; break; }
            status = Status.WARN;
        }
        return new Result(status, codes, candidate, d, c, draftLines, candidateLines, verbatim, reflowOnly, layout);
    }

    private static void markers(String draft, String candidate, List<Code> codes) {
        List<String> expected = markerLines(draft);
        List<String> actual = markerLines(candidate);
        if (expected.equals(actual)) return;
        if (!expected.isEmpty() && actual.size() < expected.size()) {
            Set<Integer> relevant = new LinkedHashSet<>();
            for (String line : expected) line.codePoints().forEach(relevant::add);
            if (restricted(draft, relevant).equals(restricted(candidate, relevant))) {
                codes.add(new Code("LAYOUT", "MARKER_NOT_OWN_LINE", Severity.WARN, "markers kept in order but not on their own line"));
                return;
            }
        }
        codes.add(new Code("LAYOUT", "MARKER_MISMATCH", Severity.BLOCK, expected.size() + " -> " + actual.size() + " marker lines"));
    }

    static List<String> markerLines(String text) {
        List<String> out = new ArrayList<>();
        for (String line : text.split("\n", -1)) {
            String t = PairText.trim(line);
            if (t.isEmpty() || t.length() > MARKER_MAX_LENGTH) continue;
            boolean all = true;
            for (int i = 0; i < t.length(); ) {
                int cp = t.codePointAt(i);
                if (MARKER_CHARS.indexOf(cp) < 0 && !PairText.isWhitespace(cp)) { all = false; break; }
                i += Character.charCount(cp);
            }
            if (all) out.add(t);
        }
        return out;
    }

    private static String restricted(String text, Set<Integer> chars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (chars.contains(cp)) sb.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    /**
     * Shape of the text without its words: the length of every run of line breaks between two lines with content and the
     * positions of marker lines among the lines with content. Never contains text.
     */
    public static String layoutFingerprint(String text) {
        StringBuilder shape = new StringBuilder();
        String[] lines = text.split("\n", -1);
        int blankRun = 0;
        int content = 0;
        boolean seen = false;
        List<String> markers = markerLines(text);
        int markerIndex = 0;
        for (String line : lines) {
            if (PairText.isBlank(line)) { blankRun++; continue; }
            if (seen) shape.append(blankRun + 1).append(',');
            seen = true;
            blankRun = 0;
            String t = PairText.trim(line);
            if (markerIndex < markers.size() && t.equals(markers.get(markerIndex))) { shape.append('M').append(content).append(','); markerIndex++; }
            content++;
        }
        return PairText.sha256(shape.toString());
    }
}
