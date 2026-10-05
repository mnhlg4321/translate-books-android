package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.List;

/** What one run edits: the two texts, the target language and the optional reference material. */
public record EditInputs(String raw, String draft, String targetLanguage, List<GlossaryEntry> glossary, String pronounCsv) {
    /** One glossary line: {@code source} (as in RAW) maps to {@code target} (as it must appear in the translation). */
    public record GlossaryEntry(String source, String target, String category, String note) {
        public GlossaryEntry {
            source = source == null ? "" : source;
            target = target == null ? "" : target;
            category = category == null ? "" : category;
            note = note == null ? "" : note;
        }
    }

    public EditInputs {
        raw = raw == null ? "" : raw;
        draft = draft == null ? "" : draft;
        targetLanguage = targetLanguage == null || targetLanguage.isBlank() ? "Vietnamese" : targetLanguage.trim();
        glossary = glossary == null ? List.of() : List.copyOf(glossary);
        pronounCsv = pronounCsv == null ? "" : pronounCsv;
    }

    public EditInputs withoutReference() {
        return new EditInputs(raw, draft, targetLanguage, new ArrayList<>(), "");
    }
}
