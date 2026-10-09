package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.ApiPrompt;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;
import com.ml.tblandroidtxt.editorial.api.PairText;

import java.util.List;

/** Minimal request for app-detected points. FINAL and whole-chunk rewriting are deliberately absent. */
public final class TargetedFixPrompt {
    private TargetedFixPrompt() { }

    public static ApiPrompt build(String targetLanguage, String rawBefore, String rawMain, String rawAfter,
                                  String draftBefore, String draftMain, String draftAfter, List<FixPoint> points) {
        if (points == null || points.isEmpty()) throw new IllegalArgumentException("TARGETED_PROMPT_NO_POINTS");
        String language = targetLanguage == null || targetLanguage.isBlank() ? "Vietnamese" : targetLanguage.trim();
        String system = "You edit only the numbered lines in a " + language + " DRAFT. RAW is the authority for meaning. "
                + "The supplied chunk and neighbouring context are read-only evidence; do not rewrite them. "
                + "For every listed id return exactly one line: [id] <replacement DRAFT line>, or [id] = to keep the original. "
                + "For a MISSING point, return one translated line for its insertion position. Do not add explanation, markdown, tags, "
                + "or any unlisted id. A point with ambiguous speaker/addressee or conflicting rules must be [id] =. "
                + "Do not edit any other line.\n";
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
            user.append("RULES: ").append(String.join(" | ", point.rules())).append("\n\n");
            if (point.types().contains(FixPoint.Type.GLOSSARY)) glossary++;
            if (point.types().contains(FixPoint.Type.ADDRESS)) pronouns++;
        }
        user.append("Return one answer line for each id.\n");
        return new ApiPrompt(EditorialApiContract.Step.EDIT, system, user.toString(), glossary, pronouns, PairText.sha256(system));
    }

    private static void appendContext(StringBuilder out, String name, String text) {
        if (text != null && !text.isBlank()) out.append("# ").append(name).append(" (read-only)\n").append(text).append("\n\n");
    }

    private static String value(String value) { return value == null ? "" : value; }
}
