package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.List;

/**
 * What one run edits: the two texts, the target language, optional references, and for V5 the verbatim original attachments
 * with the chain identity the pack needs (ID and series).
 */
public record EditInputs(String raw, String draft, String targetLanguage, List<GlossaryEntry> glossary, String pronounCsv,
                         List<OriginalSourceFile> originalSourceFiles, V5SourceIdentity identity) {
    /**
     * One original attachment. {@code role} (RAW, DRAFT, GLOSSARY or PRONOUN) says what the file is for; {@code name} is the
     * file name the owner gave it and is sent unchanged; {@code content} is the strict-UTF-8 decoded text with no
     * newline or BOM normalization.
     */
    public record OriginalSourceFile(String role, String name, String content) {
        public OriginalSourceFile {
            role = role == null ? "" : role;
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
        identity = identity == null ? V5SourceIdentity.NONE : identity;
    }

    /** Backwards-compatible constructor for the ordinary E path. */
    public EditInputs(String raw, String draft, String targetLanguage, List<GlossaryEntry> glossary, String pronounCsv) {
        this(raw, draft, targetLanguage, glossary, pronounCsv, List.of(), V5SourceIdentity.NONE);
    }

    public EditInputs withoutReference() {
        return new EditInputs(raw, draft, targetLanguage, new ArrayList<>(), "", originalSourceFiles, identity);
    }
}
