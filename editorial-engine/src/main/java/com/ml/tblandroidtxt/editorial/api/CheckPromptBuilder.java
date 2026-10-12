package com.ml.tblandroidtxt.editorial.api;

import java.util.List;

/**
 * Builds the check call: RAW first, then the EDITED text, then only the passages the editor changed (before -> after) so
 * regressions show, then the guard flags. The editor's own notes are never passed on: the check stays independent.
 */
public final class CheckPromptBuilder {
    /** The changed-passage section is bounded; the rest is counted so the model knows it was cut. */
    static final int MAX_CHANGED_SECTION_CHARS = 60_000;

    private CheckPromptBuilder() { }

    public static ApiPrompt build(EditorialApiContract.Step step, String raw, String draft, String edited,
                                  List<EditGuards.Flag> flags, String targetLanguage) {
        return build(step, raw, draft, edited, flags, targetLanguage, List.of(), List.of());
    }

    /**
     * @param glossary the glossary entries whose source occurs in RAW, as the edit received them
     * @param pronounRows the pronoun rows in scope for RAW, as the edit received them
     */
    public static ApiPrompt build(EditorialApiContract.Step step, String raw, String draft, String edited,
                                  List<EditGuards.Flag> flags, String targetLanguage,
                                  List<EditInputs.GlossaryEntry> glossary, List<String> pronounRows) {
        StringBuilder system = new StringBuilder(QualityCore.checkPrompt(targetLanguage));
        system.append("\nCHECK RULES\n")
                .append("Report a problem only when RAW and the EDITED text disagree, when something is missing, added or ")
                .append("reordered, when a number, negation, speaker, listener, pronoun or glossary term is wrong, or when the ")
                .append("edit broke a passage that was right in the draft (REGRESSION). Quote exactly; the app finds your quote ")
                .append("by plain text matching. A suggested fix replaces exactly the edited_quote and nothing else.\n\n")
                .append(CheckSpec.promptDescription());
        if (!glossary.isEmpty() || !pronounRows.isEmpty()) {
            system.append("\nAUTHORITATIVE REFERENCE\nThe glossary and pronoun rows below are the ones the editor was given for this chapter. ")
                    .append("Report a term or form of address in the EDITED text that contradicts them (kind GLOSSARY or PRONOUN); ")
                    .append("do not report a form they do not cover.\n");
            EditPromptBuilder.appendReferences(system, glossary, pronounRows);
        }
        StringBuilder user = new StringBuilder("# RAW\n").append(raw).append("\n\n# EDITED\n").append(edited).append("\n\n");
        user.append("# CHANGED PASSAGES (draft -> edited)\n");
        List<LineDiff.Segment> segments = LineDiff.segments(draft, edited);
        if (segments.isEmpty()) user.append("(none: the edited text equals the draft)\n");
        int shown = 0;
        int used = 0;
        for (LineDiff.Segment segment : segments) {
            String block = "[" + (shown + 1) + "] before: " + oneLine(segment.before()) + "\n    after: " + oneLine(segment.after()) + "\n";
            if (used + block.length() > MAX_CHANGED_SECTION_CHARS) break;
            user.append(block);
            used += block.length();
            shown++;
        }
        if (shown < segments.size()) user.append("(").append(segments.size() - shown).append(" more changed passages not listed)\n");
        user.append("\n# POINTS TO LOOK AT\n");
        if (flags.isEmpty()) user.append("(none)\n");
        for (EditGuards.Flag flag : flags) user.append("- ").append(flag.code().name()).append(": ").append(flag.detail()).append('\n');
        return new ApiPrompt(step, system.toString(), user.toString(), glossary.size(), pronounRows.size(), QualityCore.checkSha256());
    }

    private static String oneLine(String text) {
        return text.isEmpty() ? "(empty)" : text.replace("\n", " ⏎ ");
    }
}
