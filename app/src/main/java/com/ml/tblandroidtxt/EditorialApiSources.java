package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.V5SourceIdentity;

import java.util.ArrayList;
import java.util.List;

/** The texts one run edits, as read when the run starts; their SHA-256 values identify the snapshot. */
public final class EditorialApiSources {
    public final String raw;
    public final String draft;
    /** Glossary as a stable text (one entry per line) used for the snapshot digest; empty when not used. */
    public final String glossaryText;
    public final String pronounText;
    public final List<EditInputs.GlossaryEntry> glossary;
    public final String targetLanguage;
    /** Original four source attachments for V5; the E path continues to use the filtered fields above. */
    public final List<EditInputs.OriginalSourceFile> originalSourceFiles;
    /** Chapter ID and series of the V5 chain; both are required before a V5 request is built. */
    public final V5SourceIdentity identity;

    public EditorialApiSources(String raw, String draft, String glossaryText, List<EditInputs.GlossaryEntry> glossary,
                               String pronounText, String targetLanguage) {
        this(raw, draft, glossaryText, glossary, pronounText, targetLanguage, List.of());
    }

    public EditorialApiSources(String raw, String draft, String glossaryText, List<EditInputs.GlossaryEntry> glossary,
                               String pronounText, String targetLanguage,
                               List<EditInputs.OriginalSourceFile> originalSourceFiles) {
        this(raw, draft, glossaryText, glossary, pronounText, targetLanguage, originalSourceFiles, V5SourceIdentity.NONE);
    }

    public EditorialApiSources(String raw, String draft, String glossaryText, List<EditInputs.GlossaryEntry> glossary,
                               String pronounText, String targetLanguage,
                               List<EditInputs.OriginalSourceFile> originalSourceFiles, V5SourceIdentity identity) {
        this.raw = raw == null ? "" : raw;
        this.draft = draft == null ? "" : draft;
        this.glossaryText = glossaryText == null ? "" : glossaryText;
        this.glossary = glossary == null ? List.of() : List.copyOf(glossary);
        this.pronounText = pronounText == null ? "" : pronounText;
        this.targetLanguage = targetLanguage == null || targetLanguage.isBlank() ? "Vietnamese" : targetLanguage;
        this.originalSourceFiles = originalSourceFiles == null ? List.of() : List.copyOf(originalSourceFiles);
        this.identity = identity == null ? V5SourceIdentity.NONE : identity;
    }

    public EditInputs inputs() { return new EditInputs(raw, draft, targetLanguage, glossary, pronounText, originalSourceFiles, identity); }

    public String rawSha256() { return HashUtil.sha256(raw); }

    public String draftSha256() { return HashUtil.sha256(draft); }

    public String glossarySha256() { return glossaryText.isEmpty() ? "" : HashUtil.sha256(glossaryText); }

    public String pronounSha256() { return pronounText.isEmpty() ? "" : HashUtil.sha256(pronounText); }

    public boolean hasGlossary() { return !glossary.isEmpty(); }

    public boolean hasPronoun() { return !pronounText.isBlank(); }

    /** One line per entry: {@code source<TAB>target<TAB>category<TAB>note}. */
    public static String glossaryAsText(List<EditInputs.GlossaryEntry> entries) {
        StringBuilder out = new StringBuilder();
        for (EditInputs.GlossaryEntry entry : entries) {
            out.append(entry.source()).append('\t').append(entry.target()).append('\t').append(entry.category()).append('\t')
                    .append(entry.note().replace('\n', ' ')).append('\n');
        }
        return out.toString();
    }

    /** Which parts of a stored run no longer match what is on disk / in the library now. */
    public static List<String> changedParts(EditorialApiRun run, EditorialApiSources current) {
        List<String> changed = new ArrayList<>();
        if (!run.rawSha256.equals(current.rawSha256())) changed.add("RAW");
        if (!run.draftSha256.equals(current.draftSha256())) changed.add("DRAFT");
        if (!run.glossarySha256.equals(current.glossarySha256())) changed.add("GLOSSARY");
        if (!run.pronounSha256.equals(current.pronounSha256())) changed.add("PRONOUN");
        return changed;
    }
}
