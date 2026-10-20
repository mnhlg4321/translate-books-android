package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Blocking validation applied before any response can become accepted output. */
public final class ResponseValidator {
    public enum Finish { STOP, LENGTH, CONTENT_FILTER, ERROR, CANCELLED, UNKNOWN }

    public static Result validate(String raw, String extracted, String source, String prompt,
                                  String finishReason, int outputTokens, int maxOutputTokens,
                                  boolean markersRequired) {
        ArrayList<String> errors = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        String body = extracted == null ? "" : extracted.trim();
        String rawBody = raw == null ? "" : raw.trim();
        Finish finish = parseFinish(finishReason);
        if (rawBody.isEmpty() || body.isEmpty()) errors.add("EMPTY_RESPONSE");
        if (!rawBody.isEmpty() && rawBody.replace("`", "").trim().isEmpty()) errors.add("MARKDOWN_FENCE_ONLY");
        String lower = body.toLowerCase(Locale.ROOT);
        if (looksLikeRefusal(lower)) errors.add("REFUSAL");
        if (looksLikeProviderError(lower)) errors.add("PROVIDER_ERROR_IN_BODY");
        if (lower.matches("(?s)^\s*(analysis|reasoning|metadata)\s*[:：].*$") && !lower.contains("<translation>")) errors.add("METADATA_ONLY");
        if (markersRequired && !rawBody.toLowerCase(Locale.ROOT).contains("<translation>")) errors.add("MISSING_TRANSLATION_MARKER");
        if (!body.isEmpty() && prompt != null && normalize(body).equals(normalize(prompt))) errors.add("PROMPT_ECHO");
        if (!body.isEmpty() && source != null && expectsTranslation(source) && normalize(body).equals(normalize(source))) errors.add("SOURCE_ECHO");
        if (lower.contains("<system>") || lower.contains("you are a professional literary translator") || lower.contains("system prompt:")) errors.add("SYSTEM_PROMPT_LEAKAGE");
        // Truncation is reported before everything it causes (an unclosed quote, missing lines), so the message names the real cause.
        boolean truncated = false;
        if (finish == Finish.LENGTH) { errors.add("FINISH_REASON_LENGTH"); truncated = true; }
        if (finish == Finish.CONTENT_FILTER || finish == Finish.ERROR || finish == Finish.CANCELLED) errors.add("FINISH_REASON_" + finish.name());
        if (outputTokens > 0 && maxOutputTokens > 0 && outputTokens >= maxOutputTokens) { errors.add("OUTPUT_TOKEN_LIMIT_REACHED"); truncated = true; }
        // An unknown finish reason with plenty of room left is as good as "stop"; it is not evidence of a cut.
        boolean farFromLimit = maxOutputTokens <= 0 || outputTokens < maxOutputTokens * 0.9;
        boolean stopped = finish == Finish.STOP || (finish == Finish.UNKNOWN && farFromLimit);
        if (!body.isEmpty() && looksMidSentence(body) && (!stopped || outputTokens >= Math.max(1, maxOutputTokens - 2))) { errors.add("LIKELY_TRUNCATED_END"); truncated = true; }
        double lineRatio = -1;
        int[] worst = null;
        if (!truncated) {
            lineRatio = lineRatio(source, body);
            if (lineRatio >= 0 && lineRatio < MIN_LINE_RATIO) errors.add("OUTPUT_MISSING_LINES");
            worst = worstBracket(source, body);
            if (worst != null) errors.add("UNCLOSED_BRACKET_OR_DIALOGUE");
            if (looksRepeated(body) && !looksRepeated(source == null ? "" : source)) errors.add("REPEATED_RESPONSE_LOOP");
        }
        if (finish == Finish.UNKNOWN) warnings.add("UNKNOWN_FINISH_REASON");
        Result r = new Result(errors.isEmpty(), finish, errors, warnings);
        r.truncated = truncated;
        r.lineRatio = lineRatio;
        r.outputTokens = outputTokens;
        r.maxOutputTokens = maxOutputTokens;
        if (worst != null) { r.bracketKind = BRACKET_NAMES[worst[0]]; r.bracketDelta = worst[1]; r.rawBracketDelta = worst[2]; }
        return r;
    }

    /** Share of the RAW's non-blank lines the translation must still have; below this the answer is cut or skipped text. */
    static final double MIN_LINE_RATIO = 0.80;
    /** Short chunks have too few lines for a ratio to mean anything. */
    private static final int MIN_RAW_LINES = 5;
    private static final char[][] BRACKETS = {{'\u300c', '\u300d'}, {'\u300e', '\u300f'}, {'(', ')'}, {'[', ']'}};
    private static final String[] BRACKET_NAMES = {"\u300c\u300d", "\u300e\u300f", "()", "[]"};

    /** Non-blank translated lines divided by non-blank RAW lines, or -1 when the RAW is too short to judge. */
    static double lineRatio(String source, String body) {
        if (source == null) return -1;
        int raw = nonBlankLines(source);
        if (raw < MIN_RAW_LINES) return -1;
        return nonBlankLines(body == null ? "" : body) / (double) raw;
    }

    private static int nonBlankLines(String s) {
        int n = 0;
        for (String line : s.split("\\R")) if (!line.trim().isEmpty()) n++;
        return n;
    }

    /**
     * The bracket pair whose imbalance in the translation is not explained by the RAW: {pairIndex, translationDelta, rawDelta}, or null.
     * A translation is refused only when its delta (opens minus closes) is non-zero and has the opposite sign of the RAW's, or is larger.
     * For ( ) and [ ] only a surplus of opens counts, as before.
     */
    static int[] worstBracket(String source, String body) {
        String raw = source == null ? "" : source;
        int[] worst = null;
        for (int k = 0; k < BRACKETS.length; k++) {
            int dt = balance(body, BRACKETS[k][0], BRACKETS[k][1]);
            if (dt == 0) continue;
            int dr = balance(raw, BRACKETS[k][0], BRACKETS[k][1]);
            if (k >= 2 && dt < 0) continue;
            boolean refused = Integer.signum(dt) != Integer.signum(dr) || Math.abs(dt) > Math.abs(dr);
            if (refused && (worst == null || Math.abs(dt) > Math.abs(worst[1]))) worst = new int[]{k, dt, dr};
        }
        return worst;
    }

    /** Result of {@link #repairQuoteStyle}: the text with closing quotes of the wrong style replaced, and whether anything changed. */
    public static final class Repaired {
        public final String text; public final boolean changed;
        Repaired(String text, boolean changed) { this.text = text; this.changed = changed; }
    }

    /**
     * A model that opens a quote with the corner bracket (as the RAW does) but closes it with a curly quote is fixed by a fixed rule. The text
     * is read once, left to right, with a stack of open quotes: a curly closer that meets a curly opener on the stack closes it as usual, a
     * curly closer that meets a corner opener becomes the corner closer. The same for the double corner brackets and single curly quotes.
     * Only the marks change, never the words, and the repair is kept only when it brings the pair to the balance the RAW has.
     */
    public static Repaired repairQuoteStyle(String body, String source) {
        if (body == null || body.isEmpty()) return new Repaired(body == null ? "" : body, false);
        String raw = source == null ? "" : source;
        String out = body;
        boolean changed = false;
        if (raw.indexOf('\u300c') >= 0 || raw.indexOf('\u300d') >= 0) {
            String fixed = closeCurlyAsCorner(out, '\u300c', '\u300d', '\u201c', '\u201d');
            if (!fixed.equals(out) && balance(fixed, '\u300c', '\u300d') == balance(raw, '\u300c', '\u300d')) { out = fixed; changed = true; }
        }
        if (raw.indexOf('\u300e') >= 0 || raw.indexOf('\u300f') >= 0) {
            String fixed = closeCurlyAsCorner(out, '\u300e', '\u300f', '\u2018', '\u2019');
            if (!fixed.equals(out) && balance(fixed, '\u300e', '\u300f') == balance(raw, '\u300e', '\u300f')) { out = fixed; changed = true; }
        }
        return new Repaired(out, changed);
    }

    private static String closeCurlyAsCorner(String text, char open, char close, char curlyOpen, char curlyClose) {
        StringBuilder sb = new StringBuilder(text);
        java.util.ArrayDeque<Character> stack = new java.util.ArrayDeque<>();
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == open || c == curlyOpen) stack.push(c);
            else if (c == close) { if (!stack.isEmpty() && stack.peek() == open) stack.pop(); }
            else if (c == curlyClose && !stack.isEmpty()) {
                if (stack.peek() == curlyOpen) stack.pop();
                else if (stack.peek() == open) { sb.setCharAt(i, close); stack.pop(); }
            }
        }
        return sb.toString();
    }

    private static int count(String s, char c) { int n = 0; for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++; return n; }

    public static Finish parseFinish(String value) {
        String v = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (v.equals("stop") || v.equals("end_turn") || v.equals("completed")) return Finish.STOP;
        if (v.equals("length") || v.equals("max_tokens")) return Finish.LENGTH;
        if (v.equals("content_filter") || v.equals("content-filter")) return Finish.CONTENT_FILTER;
        if (v.equals("error") || v.equals("failed")) return Finish.ERROR;
        if (v.equals("cancelled") || v.equals("canceled")) return Finish.CANCELLED;
        return Finish.UNKNOWN;
    }

    private static boolean expectsTranslation(String source) {
        if (source == null) return false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if ((c >= '\u3040' && c <= '\u30ff') || (c >= '\u4e00' && c <= '\u9fff')) return true;
        }
        return false;
    }

    private static boolean looksLikeRefusal(String s) { return s.startsWith("i cannot") || s.startsWith("i can't") || s.startsWith("sorry, i") || s.contains("cannot assist with"); }
    private static boolean looksLikeProviderError(String s) { return s.startsWith("error:") || s.contains("insufficient credits") || s.contains("invalid api key") || s.contains("rate limit exceeded"); }
    private static int balance(String s, char open, char close) { int n=0; for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c==open)n++;else if(c==close)n--;}return n; }
    private static boolean looksRepeated(String s) {
        String[] lines = s.split("\\R");
        if (lines.length < 4) return false;
        int repeated=0; for(int i=1;i<lines.length;i++) if(!lines[i].trim().isEmpty() && lines[i].trim().equals(lines[i-1].trim())) repeated++;
        return repeated >= 3 || repeated * 2 >= lines.length;
    }
    private static boolean looksMidSentence(String s) {
        if (s.isEmpty()) return false; char c=s.charAt(s.length()-1);
        return Character.isLetterOrDigit(c) && !s.endsWith("。") && !s.endsWith("！") && !s.endsWith("？");
    }
    private static String normalize(String s) { return s == null ? "" : s.replaceAll("\\s+", " ").trim(); }

    public static final class Result {
        public final boolean accepted; public final Finish finish; public final List<String> errors; public final List<String> warnings;
        /** True when the answer was cut (finish length, token limit reached, ends mid-sentence): resending the same request will be cut again. */
        public boolean truncated;
        /** Translated non-blank lines over RAW non-blank lines; -1 when not judged. */
        public double lineRatio = -1;
        public int outputTokens, maxOutputTokens;
        /** The bracket pair that failed, the translation's imbalance and the RAW's; empty/0 when none failed. */
        public String bracketKind = "";
        public int bracketDelta, rawBracketDelta;
        Result(boolean accepted, Finish finish, List<String> errors, List<String> warnings) { this.accepted=accepted;this.finish=finish;this.errors=errors;this.warnings=warnings; }
        public String summary() { return String.join(",", errors); }
        /** What the person reads: the cause first, counts only (never book text). */
        public String readable() {
            StringBuilder sb = new StringBuilder();
            if (truncated) sb.append("B\u1ea3n d\u1ecbch b\u1ecb c\u1eaft (h\u1ebft token \u0111\u1ea7u ra: ").append(outputTokens).append('/').append(maxOutputTokens).append(", finish=").append(finish.name().toLowerCase(Locale.ROOT)).append("). ");
            if (errors.contains("OUTPUT_MISSING_LINES")) sb.append("B\u1ea3n d\u1ecbch thi\u1ebfu d\u00f2ng (").append(Math.round(lineRatio * 100)).append("% s\u1ed1 d\u00f2ng c\u1ee7a RAW). ");
            if (errors.contains("UNCLOSED_BRACKET_OR_DIALOGUE")) sb.append("Ngo\u1eb7c ").append(bracketKind).append(" l\u1ec7ch ").append(bracketDelta).append(" trong b\u1ea3n d\u1ecbch, RAW l\u1ec7ch ").append(rawBracketDelta).append(". ");
            return sb.toString();
        }
    }
    public static final class RejectedResponseException extends Exception {
        public final Result validation;
        public RejectedResponseException(Result result) { super(result.readable() + "Response validation failed: " + result.summary()); validation=result; }
    }
    private ResponseValidator() {}
}
