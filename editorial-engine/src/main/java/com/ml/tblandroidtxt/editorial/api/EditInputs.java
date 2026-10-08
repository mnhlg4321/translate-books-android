package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.List;

/** What one run edits: the two texts, the target language, optional references and verbatim V5 source attachments. */
public record EditInputs(String raw, String draft, String targetLanguage, List<GlossaryEntry> glossary, String pronounCsv,
                         List<OriginalSourceFile> originalSourceFiles) {
    /** Exact role filename and strict-UTF-8 decoded file contents; no newline/BOM normalization is applied. */
    public record OriginalSourceFile(String name, String content) {
        public OriginalSourceFile {
            name = name == null ? "" : name;
            content = content == null ? "" : content;
        }
    }

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
        originalSourceFiles = originalSourceFiles == null ? List.of() : List.copyOf(originalSourceFiles);
    }

    /** Backwards-compatible constructor for the ordinary E path. */
    public EditInputs(String raw, String draft, String targetLanguage, List<GlossaryEntry> glossary, String pronounCsv) {
        this(raw, draft, targetLanguage, glossary, pronounCsv, List.of());
    }

    public EditInputs withoutReference() {
        return new EditInputs(raw, draft, targetLanguage, new ArrayList<>(), "", originalSourceFiles);
    }
}
