package com.ml.tblandroidtxt;

import android.content.Context;
import android.net.Uri;

import com.ml.tblandroidtxt.editorial.api.EditInputs;

import java.util.ArrayList;
import java.util.List;

/** Reads the four sources of a combo: SAF text files and the Library glossary / pronoun profiles. */
final class EditorialApiSourceLoader {
    /** A source that cannot be read; the message is for the person, in plain words. */
    static final class SourceException extends Exception {
        final String which;

        SourceException(String which, String message) {
            super(message);
            this.which = which;
        }
    }

    private EditorialApiSourceLoader() { }

    static EditorialApiSources load(Context context, EditorialApiCombo combo, String targetLanguage) throws SourceException {
        String raw = read(context, combo.rawUri, "RAW");
        String draft = read(context, combo.draftUri, "DRAFT");
        List<EditInputs.GlossaryEntry> entries = new ArrayList<>();
        String glossaryText = "";
        if (!combo.glossaryId.isEmpty()) {
            GlossaryStore.Glossary glossary = GlossaryStore.find(context, combo.glossaryId);
            if (glossary == null) throw new SourceException("GLOSSARY", "Glossary đã chọn không còn trong thư viện.");
            for (GlossaryStore.Term term : glossary.terms) {
                entries.add(new EditInputs.GlossaryEntry(term.source, term.target, term.category, term.note));
            }
            glossaryText = EditorialApiSources.glossaryAsText(entries);
        }
        String pronounText = "";
        if (!combo.pronounId.isEmpty()) {
            PronounStore.Profile profile = PronounStore.find(context, combo.pronounId);
            if (profile == null) throw new SourceException("PRONOUN", "Pronoun đã chọn không còn trong thư viện.");
            pronounText = profile.text == null ? "" : profile.text;
        }
        return new EditorialApiSources(raw, draft, glossaryText, entries, pronounText, targetLanguage);
    }

    private static String read(Context context, String uri, String which) throws SourceException {
        if (uri == null || uri.isEmpty()) throw new SourceException(which, "Chưa chọn file " + which + ".");
        try {
            return FileUtil.readText(context, Uri.parse(uri));
        } catch (Exception unreadable) {
            throw new SourceException(which, "Không đọc được file " + which + ". Hãy chọn lại file.");
        }
    }
}
