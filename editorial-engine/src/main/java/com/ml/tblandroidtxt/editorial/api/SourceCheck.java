package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.List;

/**
 * What the app looks at before it spends anything (plan sections 3.2 and 5): sources that are empty or unreadable stop the
 * run, a draft/raw size ratio outside 0.5-4 or a missing reference only warns, and a chapter too long for one call is
 * refused with a plain message. Pure; the texts are passed in.
 */
public final class SourceCheck {
    public enum Problem {
        RAW_EMPTY(true), DRAFT_EMPTY(true), RAW_BROKEN_ENCODING(true), DRAFT_BROKEN_ENCODING(true), TOO_LONG(true),
        LENGTH_RATIO(false), NO_GLOSSARY(false), NO_PRONOUN(false), SAME_TEXT(false);

        private final boolean blocking;

        Problem(boolean blocking) { this.blocking = blocking; }

        public boolean blocking() { return blocking; }
    }

    /** Estimated tokens above which one call is not attempted in this version. */
    public static final long MAX_ESTIMATED_TOKENS = 40_000L;
    public static final double MIN_RATIO = 0.5;
    public static final double MAX_RATIO = 4.0;

    private SourceCheck() { }

    public static List<Problem> check(String raw, String draft, boolean hasGlossary, boolean hasPronoun, int referenceChars) {
        List<Problem> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) out.add(Problem.RAW_EMPTY);
        if (draft == null || draft.isBlank()) out.add(Problem.DRAFT_EMPTY);
        if (raw != null && replacementShare(raw) > 0.001) out.add(Problem.RAW_BROKEN_ENCODING);
        if (draft != null && replacementShare(draft) > 0.001) out.add(Problem.DRAFT_BROKEN_ENCODING);
        if (raw != null && draft != null && !raw.isBlank() && !draft.isBlank()) {
            double ratio = (double) draft.length() / raw.length();
            if (ratio < MIN_RATIO || ratio > MAX_RATIO) out.add(Problem.LENGTH_RATIO);
            if (raw.equals(draft)) out.add(Problem.SAME_TEXT);
            if (estimatedTokens(raw, draft, referenceChars) > MAX_ESTIMATED_TOKENS) out.add(Problem.TOO_LONG);
        }
        if (!hasGlossary) out.add(Problem.NO_GLOSSARY);
        if (!hasPronoun) out.add(Problem.NO_PRONOUN);
        return out;
    }

    public static boolean blocked(List<Problem> problems) {
        for (Problem problem : problems) if (problem.blocking()) return true;
        return false;
    }

    /** Japanese counts about one token per character, Vietnamese about one per two. */
    public static long estimatedTokens(String raw, String draft, int referenceChars) {
        return (long) raw.length() + draft.length() / 2L + referenceChars / 2L;
    }

    private static double replacementShare(String text) {
        if (text.isEmpty()) return 0.0;
        int bad = 0;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\uFFFD') bad++;
        return (double) bad / text.length();
    }
}
