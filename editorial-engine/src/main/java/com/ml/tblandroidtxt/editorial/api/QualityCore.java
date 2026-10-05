package com.ml.tblandroidtxt.editorial.api;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;

import java.nio.charset.StandardCharsets;

/**
 * The quality standard of the 4.1.3 instructions reduced to the rules a model can follow in one reading (plan section
 * 2.1): RAW decides content, completeness, glossary and pronoun discipline, minimal edits, doubt keeps the draft,
 * symbols stay, the output is only the text. It never mentions ledgers, ids, hashes or unit references. The text is
 * pinned by {@link #editSha256()} / {@link #checkSha256()}, which every run records.
 */
public final class QualityCore {
    private static final String RULES =
            "QUALITY STANDARD\n"
            + "1. RAW is the authority. It decides what happens, in what order, who acts and who receives, who speaks and who listens, "
            + "the point of view, numbers and quantities, negation, scope and degree of certainty. The draft must say what RAW says.\n"
            + "2. Completeness. Nothing is added, nothing is missing, no sentence or paragraph is reordered. Narration, dialogue, inner "
            + "thought, tables, numbers and scene breaks are all kept.\n"
            + "3. Glossary entries lock proper names and terms when they do not contradict RAW. Apply them by context, never by blind "
            + "search-and-replace.\n"
            + "4. Pronoun rows are reference, valid inside their scope (from / speaker / target / self / call / scope). Do not translate a "
            + "first-person word the same way everywhere; keep the address between two characters consistent within a scene or phase and "
            + "do not change it unless RAW gives a reason.\n"
            + "5. Edit minimally. Keep what is already correct. Do not rewrite to be different, do not dramatize, do not raise or lower the "
            + "register or the intensity.\n"
            + "6. When you are not sure, keep the draft and say so briefly in the notes.\n"
            + "7. Keep every symbol and frame exactly (「」『』◇ ◆ ＊ ── …… 【】 and the like). Remove technical debris only: metadata lines, "
            + "broken Markdown, U+FFFD, zero-width characters.\n"
            + "8. The output contains only the text. Remarks go outside the text tags.\n";

    private static final String EDIT_ROLE =
            "You are a careful literary editor. You receive a RAW source text and a DRAFT translation of the same chapter into %s, "
            + "optionally with a glossary and pronoun rows. You return the DRAFT corrected so that it follows RAW, changing as little as "
            + "possible.\n\n";

    private static final String CHECK_ROLE =
            "You are an independent reviewer of a %s translation. You read RAW first, then the EDITED text, then the list of passages the "
            + "editor changed (before -> after). You report only real problems, each with an exact quote, and you do not rewrite the "
            + "chapter. Differences of style are not problems.\n\n";

    private QualityCore() { }

    /** Rules shared by both prompts, without any role sentence. */
    public static String rules() { return RULES; }

    public static String editPrompt(String targetLanguage) {
        return String.format(EDIT_ROLE, language(targetLanguage)) + RULES;
    }

    public static String checkPrompt(String targetLanguage) {
        return String.format(CHECK_ROLE, language(targetLanguage)) + RULES;
    }

    public static String editSha256() { return sha(editPrompt("TARGET")); }

    public static String checkSha256() { return sha(checkPrompt("TARGET")); }

    /** Digest of the rules alone: the identity of the quality standard whichever role sentence frames it. */
    public static String rulesSha256() { return sha(RULES); }

    private static String language(String targetLanguage) {
        return targetLanguage == null || targetLanguage.isBlank() ? "Vietnamese" : targetLanguage.trim();
    }

    private static String sha(String text) {
        return EditorialCanonicalJson.sha256Hex(text.getBytes(StandardCharsets.UTF_8));
    }
}
