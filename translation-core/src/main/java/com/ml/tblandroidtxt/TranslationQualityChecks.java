package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;

/** Language-independent risk checks; manual review remains required for semantic quality. */
public final class TranslationQualityChecks {
    public static class Result {
        public final List<String> issues = new ArrayList<>();
        public boolean passed() { return issues.isEmpty(); }
    }
    public static Result inspect(String source, String output, String previousOutput) {
        Result r = new Result(); String src = safe(source), out = safe(output).trim();
        if (out.isEmpty()) { r.issues.add("EMPTY_OUTPUT"); return r; }
        if (out.contains(PromptBuilder.INPUT_IN) || out.contains(PromptBuilder.INPUT_OUT)) r.issues.add("PROMPT_ECHO");
        if (src.length() > 200 && out.length() < Math.max(20, src.length() / 8)) r.issues.add("POSSIBLE_OMISSION_OR_TRUNCATION");
        if (unbalanced(src, out, '(', ')') || unbalanced(src, out, '[', ']') || unbalanced(src, out, '「', '」') || unbalanced(src, out, '『', '』')) r.issues.add("SYMBOL_OR_FORMAT_LOSS");
        String prev = safe(previousOutput).trim();
        if (!prev.isEmpty() && commonBoundary(prev, out) >= 40) r.issues.add("DUPLICATE_BOUNDARY_TEXT");
        return r;
    }
    private static boolean unbalanced(String source, String output, char open, char close) {
        return count(source, open) == count(source, close) && count(source, open) > 0 && count(output, open) != count(output, close);
    }
    private static int count(String s, char c) { int n=0; for(int i=0;i<s.length();i++) if(s.charAt(i)==c)n++; return n; }
    private static int commonBoundary(String a, String b) { int max=Math.min(Math.min(a.length(),b.length()),300); for(int n=max;n>=1;n--) if(a.regionMatches(a.length()-n,b,0,n)) return n; return 0; }
    private static String safe(String s) { return s == null ? "" : s; }
    private TranslationQualityChecks() {}
}
