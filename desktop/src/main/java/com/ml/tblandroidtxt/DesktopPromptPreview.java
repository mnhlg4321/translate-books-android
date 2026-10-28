package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;

/**
 * "Xem trước prompt": exactly what the app would send for one chunk. It calls {@link PromptPlan#forTranslation}, the same call the engine
 * makes, with the same normalised settings, so the text equals the real request (checked byte for byte against the golden files).
 * The Glossary / Pronoun lines that match the chunk come from the same matcher the prompt used.
 */
public final class DesktopPromptPreview {
    public static final class Result {
        public final int chunkCount;
        /** 0-based. */
        public final int index;
        public final String system;
        public final String user;
        public final List<String> glossaryLines;
        public final List<String> pronounLines;
        public final String matchSummary;
        public final int inputTokens;

        Result(int chunkCount, int index, String system, String user, List<String> glossaryLines, List<String> pronounLines, String matchSummary, int inputTokens) {
            this.chunkCount = chunkCount; this.index = index; this.system = system; this.user = user;
            this.glossaryLines = glossaryLines; this.pronounLines = pronounLines; this.matchSummary = matchSummary; this.inputTokens = inputTokens;
        }

        /** The text of the "Sao chép" button: both prompts, as sent. */
        public String copyText() { return "[system]\n" + system + "\n\n[user]\n" + user; }
    }

    private DesktopPromptPreview() { }

    /** @param settings already normalised and carrying the Glossary / Pronoun / instruction texts; @param previous the accepted translation of the chunk before, or "" */
    public static Result build(String rawText, AppSettings settings, int index, String previous) {
        List<Chunk> chunks = Chunker.chunkText(rawText, settings);
        if (chunks.isEmpty()) throw new IllegalArgumentException("Không có chunk nào để xem trước");
        int i = Math.max(0, Math.min(index, chunks.size() - 1));
        Chunk chunk = chunks.get(i);
        PromptPlan plan = PromptPlan.forTranslation(chunk, i == 0 ? "" : (previous == null ? "" : previous), settings);
        String before = chunk.contextBefore == null ? "" : chunk.contextBefore;
        PromptContextBuilder.MatchReport match = PromptContextBuilder.match(settings.glossaryText, settings.pronounText, chunk.mainContent,
                before + "\n" + chunk.mainContent, chunk.paragraphStart, chunk.paragraphEnd, settings);
        return new Result(chunks.size(), i, plan.prompt.system, plan.prompt.user, new ArrayList<>(match.matchedGlossaryLines),
                new ArrayList<>(match.matchedPronounLines), match.asPreviewText(), plan.totalInputTokens);
    }

    public static int chunkCount(String rawText, AppSettings settings) { return Chunker.chunkText(rawText, settings).size(); }
}
