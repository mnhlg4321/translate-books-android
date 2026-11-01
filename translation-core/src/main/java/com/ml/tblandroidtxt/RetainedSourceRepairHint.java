package com.ml.tblandroidtxt;

import java.util.Collection;
import java.util.List;

/** Builds a short user-message correction from Japanese text left in a rejected translation response. */
public final class RetainedSourceRepairHint {
    private static final int MAX_EXCERPT_LINES = 12;
    private static final int MAX_EXCERPT_CODE_POINTS = 96;

    public static final class Generated {
        public final String note;
        public final int lineCount;

        private Generated(String note, int lineCount) {
            this.note = note;
            this.lineCount = lineCount;
        }
    }

    public static Generated fromTranslation(String translatedResponse, Collection<String> keptTargets) {
        List<ResponseValidator.RetainedSourceLine> lines =
                ResponseValidator.retainedSourceLineDetails(translatedResponse, keptTargets);
        if (lines.isEmpty()) return null;

        StringBuilder note = new StringBuilder("# APP-GENERATED RETRY NOTE\n")
                .append("The previous response retained Japanese source text on these lines. Translate or render each listed fragment, including sound effects, in the complete translation. Preserve the other translated lines.\n");
        int shown = Math.min(lines.size(), MAX_EXCERPT_LINES);
        for (int i = 0; i < shown; i++) {
            ResponseValidator.RetainedSourceLine line = lines.get(i);
            note.append("Line ").append(line.lineNumber).append(": ")
                    .append(clip(line.excerpt)).append('\n');
        }
        if (lines.size() > shown) {
            note.append("Other affected line numbers: ");
            for (int i = shown; i < lines.size(); i++) {
                if (i > shown) note.append(", ");
                note.append(lines.get(i).lineNumber);
            }
        }
        return new Generated(note.toString().trim(), lines.size());
    }

    public static PromptPair addToUser(PromptPair prompt, Generated hint) {
        if (prompt == null || hint == null || hint.note.isEmpty()) return prompt;
        return new PromptPair(prompt.system, hint.note + "\n\n" + prompt.user);
    }

    private static String clip(String value) {
        String line = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        int codePoints = line.codePointCount(0, line.length());
        if (codePoints <= MAX_EXCERPT_CODE_POINTS) return line;
        int end = line.offsetByCodePoints(0, MAX_EXCERPT_CODE_POINTS);
        return line.substring(0, end) + "…";
    }

    private RetainedSourceRepairHint() {}
}
