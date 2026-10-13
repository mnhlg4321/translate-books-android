package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.ApiPrompt;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;
import com.ml.tblandroidtxt.editorial.api.PairText;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Minimal request for app-detected points. FINAL and whole-chunk rewriting are deliberately absent. */
public final class TargetedFixPrompt {
    /**
     * Floor of the output ceiling, the same as the whole-chunk edit and the Performance MAX OUTPUT default: a reasoning model
     * spends output tokens on reasoning before the answer (C6.5: luna medium used all of a 760-token ceiling and returned no
     * content). The answer estimate below only raises the ceiling for very long points.
     */
    private static final int MIN_OUTPUT_TOKENS = 4096;
    private static final int MAX_OUTPUT_TOKENS = 32_000;

    private TargetedFixPrompt() { }

    public static ApiPrompt build(String targetLanguage, String rawBefore, String rawMain, String rawAfter,
                                  String draftBefore, String draftMain, String draftAfter, List<FixPoint> points) {
        if (points == null || points.isEmpty()) throw new IllegalArgumentException("TARGETED_PROMPT_NO_POINTS");
        String language = targetLanguage == null || targetLanguage.isBlank() ? "Vietnamese" : targetLanguage.trim();
        String system = "You edit only the numbered lines in a " + language + " DRAFT. RAW is the authority for meaning. "
                + "The supplied chunk and neighbouring context are read-only evidence; do not rewrite them. "
                + "For every listed id return exactly one line: [id] <replacement DRAFT line>, or [id] = to keep the original. "
                + "For a MISSING point, return one translated line for its insertion position. Do not add explanation, markdown, tags, "
                + "or any unlisted id. A point whose rules conflict must be [id] =. "
                + "Do not edit any other line. Preserve the DRAFT dialogue quotes, frames and special punctuation exactly; they are book content. "
                + "Never import a source-language full stop into a DRAFT that does not have it. "
                + "ADDRESS points list numbered ROWS (R1, R2, ...): pronoun rows in scope, each one speaker addressing one listener. "
                + "Several rows do not make a point ambiguous. Decide from RAW (the line, the reply, speech markers, names in the exchange and the "
                + "RAW context) who speaks this line and to whom, then take the one row whose speaker and listener match: use its self for the "
                + "speaker's own references and its call only for that listener. To change address words answer [id] R<n> <replacement line> "
                + "with the number of that row; an address change without a row number is discarded. Answer [id] = when the line already "
                + "follows that row, when RAW does not establish who speaks to whom, or when no row matches them; never reverse the direction "
                + "or borrow another speaker's row. "
                + "For ADDRESS-only points change only the evidenced address forms, preserving all other wording and order. "
                + "Call names: when a DRAFT word addressed to the listener (cậu, bạn, mày, anh, chị, em) stands where a row lists a personal name as call, "
                + "and the RAW dialogue around the line shows that the row's target is the person spoken to (a reply to that person, a vocative, or the person "
                + "named in the same exchange), replace that word with the row's call name; do not keep the generic word the row replaces. "
                + "Keep [id] = only when the person spoken to is really not established. "
                + "If RAW does not establish the roles or a concrete error, return [id] =.\n";
        StringBuilder user = new StringBuilder();
        appendContext(user, "RAW CONTEXT BEFORE", rawBefore);
        user.append("# RAW CHUNK (read-only; use as evidence)\n").append(value(rawMain)).append("\n\n");
        appendContext(user, "RAW CONTEXT AFTER", rawAfter);
        appendContext(user, "DRAFT CONTEXT BEFORE", draftBefore);
        user.append("# DRAFT CHUNK (read-only except numbered targets below)\n").append(value(draftMain)).append("\n\n");
        appendContext(user, "DRAFT CONTEXT AFTER", draftAfter);
        user.append("# NUMBERED FIX POINTS\n");
        int glossary = 0;
        int pronouns = 0;
        for (FixPoint point : points) {
            user.append('[').append(point.id()).append("] TYPES: ").append(point.typesText()).append('\n');
            user.append("RAW: ").append(point.rawText()).append('\n');
            if (point.insertion()) user.append("DRAFT: (missing; insert after nonblank line ").append(point.draftLineIndex() + 1).append(")\n");
            else user.append("DRAFT: ").append(point.draftText()).append('\n');
            appendRowsAndRules(user, point);
            if (point.types().contains(FixPoint.Type.GLOSSARY)) glossary++;
            if (point.types().contains(FixPoint.Type.ADDRESS)) pronouns++;
        }
        user.append("Return one answer line for each id.\n");
        return new ApiPrompt(EditorialApiContract.Step.EDIT, system, user.toString(), glossary, pronouns, PairText.sha256(system));
    }

    /** Output ceiling (and reservation basis): at least {@link #MIN_OUTPUT_TOKENS}, more only when the flagged lines are very long. */
    public static int maxOutputTokens(List<FixPoint> points) {
        if (points == null || points.isEmpty()) throw new IllegalArgumentException("TARGETED_PROMPT_NO_POINTS");
        long characters = 0;
        for (FixPoint point : points) characters += Math.max(point.draftText().length(), point.rawText().length()) + 48L;
        long tokens = (long) Math.ceil(characters / 2.0 * 1.6);
        return (int) Math.max(MIN_OUTPUT_TOKENS, Math.min(MAX_OUTPUT_TOKENS, tokens));
    }

    /**
     * An ADDRESS point with pronoun-row context lists each row once, numbered as the parser and the edit lock count them, and folds the
     * per-row ADDRESS rules into the address words to check. Other points keep their rules as written.
     */
    private static void appendRowsAndRules(StringBuilder out, FixPoint point) {
        if (!point.types().contains(FixPoint.Type.ADDRESS) || point.addressRows().isEmpty()) {
            out.append("RULES: ").append(String.join(" | ", point.rules())).append("\n\n");
            return;
        }
        out.append("ROWS:\n");
        for (int i = 0; i < point.addressRows().size(); i++) {
            AddressRow row = point.addressRows().get(i);
            out.append('R').append(i + 1).append(" | ").append(row.from()).append(" | ").append(row.speaker()).append(" → ").append(row.target())
                    .append(" | self=").append(row.self()).append(" | call=").append(row.call());
            if (!row.note().isEmpty()) out.append(" | ").append(row.note());
            out.append('\n');
        }
        List<String> rules = new ArrayList<>();
        Set<String> words = new LinkedHashSet<>();
        for (String rule : point.rules()) {
            int at = rule.startsWith("ADDRESS:") ? rule.indexOf(FixPointFinder.ADDRESS_OUTSIDE_MARKER) : -1;
            if (at < 0) {
                if (!rule.startsWith("ADDRESS:")) rules.add(rule);
                continue;
            }
            int start = at + FixPointFinder.ADDRESS_OUTSIDE_MARKER.length();
            int end = rule.indexOf(';', start);
            words.add(rule.substring(start, end < 0 ? rule.length() : end).trim());
        }
        if (!words.isEmpty()) rules.add("ADDRESS: DRAFT address words to check against the speaker's row: " + String.join(", ", words));
        out.append("RULES: ").append(String.join(" | ", rules)).append("\n\n");
    }

    private static void appendContext(StringBuilder out, String name, String text) {
        if (text != null && !text.isBlank()) out.append("# ").append(name).append(" (read-only)\n").append(text).append("\n\n");
    }

    private static String value(String value) { return value == null ? "" : value; }
}
