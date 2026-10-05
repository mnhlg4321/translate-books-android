package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the fixes the check proposed. A fix is applied only when its quote is found exactly once in the current text
 * (whitespace runs compare as one space) and the fix is not empty; everything else stays a "needs review" item and is never
 * guessed at. Issues are applied one after another on the updated text, so two fixes for the same passage cannot both land.
 */
public final class FixApplier {
    public enum ReviewReason { NO_MATCH, AMBIGUOUS, EMPTY_FIX }

    public record Applied(CheckResponseParser.Issue issue) { }

    public record Review(CheckResponseParser.Issue issue, ReviewReason reason) { }

    public record Result(String text, List<Applied> applied, List<Review> review) {
        public Result {
            applied = List.copyOf(applied);
            review = List.copyOf(review);
        }
    }

    private FixApplier() { }

    public static Result apply(String edited, List<CheckResponseParser.Issue> issues) {
        String text = edited;
        List<Applied> applied = new ArrayList<>();
        List<Review> review = new ArrayList<>();
        for (CheckResponseParser.Issue issue : issues) {
            if (issue.fix().isEmpty()) { review.add(new Review(issue, ReviewReason.EMPTY_FIX)); continue; }
            View view = new View(text);
            String quote = collapse(issue.editedQuote());
            if (quote.isEmpty()) { review.add(new Review(issue, ReviewReason.NO_MATCH)); continue; }
            int first = view.normalized.indexOf(quote);
            if (first < 0) { review.add(new Review(issue, ReviewReason.NO_MATCH)); continue; }
            if (view.normalized.indexOf(quote, first + 1) >= 0) { review.add(new Review(issue, ReviewReason.AMBIGUOUS)); continue; }
            int from = view.originalStart[first];
            int to = view.originalEnd[first + quote.length() - 1];
            text = text.substring(0, from) + issue.fix() + text.substring(to);
            applied.add(new Applied(issue));
        }
        return new Result(text, applied, review);
    }

    /** Whitespace runs become one space and the ends are trimmed; the view remembers where each character came from. */
    private static final class View {
        final String normalized;
        final int[] originalStart;
        final int[] originalEnd;

        View(String text) {
            StringBuilder out = new StringBuilder(text.length());
            int[] start = new int[text.length() + 1];
            int[] end = new int[text.length() + 1];
            boolean pendingSpace = false;
            int spaceStart = 0;
            int spaceEnd = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (isSpace(c)) {
                    if (!pendingSpace) { pendingSpace = true; spaceStart = i; }
                    spaceEnd = i + 1;
                    continue;
                }
                if (pendingSpace && out.length() > 0) {
                    start[out.length()] = spaceStart;
                    end[out.length()] = spaceEnd;
                    out.append(' ');
                }
                pendingSpace = false;
                start[out.length()] = i;
                end[out.length()] = i + 1;
                out.append(c);
            }
            normalized = out.toString();
            originalStart = start;
            originalEnd = end;
        }
    }

    static String collapse(String text) {
        return new View(text).normalized;
    }

    private static boolean isSpace(char c) {
        return Character.isWhitespace(c) || c == '　' || c == ' ';
    }
}
