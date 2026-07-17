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
        if (finish == Finish.LENGTH) errors.add("FINISH_REASON_LENGTH");
        if (finish == Finish.CONTENT_FILTER || finish == Finish.ERROR || finish == Finish.CANCELLED) errors.add("FINISH_REASON_" + finish.name());
        if (outputTokens > 0 && maxOutputTokens > 0 && outputTokens >= maxOutputTokens) errors.add("OUTPUT_TOKEN_LIMIT_REACHED");
        if (hasUnclosedPairs(body)) errors.add("UNCLOSED_BRACKET_OR_DIALOGUE");
        if (looksRepeated(body)) errors.add("REPEATED_RESPONSE_LOOP");
        if (!body.isEmpty() && looksMidSentence(body) && (finish != Finish.STOP || outputTokens >= Math.max(1, maxOutputTokens - 2))) errors.add("LIKELY_TRUNCATED_END");
        if (finish == Finish.UNKNOWN) warnings.add("UNKNOWN_FINISH_REASON");
        return new Result(errors.isEmpty(), finish, errors, warnings);
    }

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
    private static boolean hasUnclosedPairs(String s) { return balance(s, '「', '」') != 0 || balance(s, '『', '』') != 0 || balance(s, '(', ')') > 0 || balance(s, '[', ']') > 0; }
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
        Result(boolean accepted, Finish finish, List<String> errors, List<String> warnings) { this.accepted=accepted;this.finish=finish;this.errors=errors;this.warnings=warnings; }
        public String summary() { return String.join(",", errors); }
    }
    public static final class RejectedResponseException extends Exception {
        public final Result validation;
        public RejectedResponseException(Result result) { super("Response validation failed: " + result.summary()); validation=result; }
    }
    private ResponseValidator() {}
}
