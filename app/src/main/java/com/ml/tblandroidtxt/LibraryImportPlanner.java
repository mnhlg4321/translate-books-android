package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converts independently selected files into independent library profiles. */
final class LibraryImportPlanner {
    static final class Source {
        final String name;
        final String text;

        Source(String name, String text) {
            this.name = name == null ? "" : name.trim();
            this.text = text == null ? "" : text;
        }
    }

    static final class Result<T> {
        final List<T> imported;
        final List<String> failed;

        Result(List<T> imported, List<String> failed) {
            this.imported = Collections.unmodifiableList(new ArrayList<>(imported));
            this.failed = Collections.unmodifiableList(new ArrayList<>(failed));
        }
    }

    static Result<GlossaryStore.Glossary> glossaries(List<Source> sources) {
        ArrayList<GlossaryStore.Glossary> imported = new ArrayList<>();
        ArrayList<String> failed = new ArrayList<>();
        if (sources == null) return new Result<>(imported, failed);
        for (Source source : sources) {
            String name = displayName(source, "Glossary");
            String text = source == null ? "" : source.text;
            List<GlossaryStore.Term> terms = GlossaryStore.parseTerms(name, text);
            if (terms.isEmpty()) {
                failed.add(name + ": No valid glossary terms");
                continue;
            }
            GlossaryStore.Glossary glossary = new GlossaryStore.Glossary();
            glossary.name = name;
            GlossaryStore.mergeTerms(glossary, terms);
            imported.add(glossary);
        }
        return new Result<>(imported, failed);
    }

    static Result<PronounStore.Profile> pronouns(List<Source> sources) {
        ArrayList<PronounStore.Profile> imported = new ArrayList<>();
        ArrayList<String> failed = new ArrayList<>();
        if (sources == null) return new Result<>(imported, failed);
        for (Source source : sources) {
            String name = displayName(source, "Pronoun");
            String text = source == null ? "" : source.text;
            PromptContextBuilder.ParseReport report = PromptContextBuilder.validate("", text);
            if (text.trim().isEmpty() || !report.malformedPronounRows.isEmpty()
                    || report.explicitPronouns.isEmpty()) {
                failed.add(name + ": No valid pronoun rules");
                continue;
            }
            PronounStore.Profile profile = new PronounStore.Profile();
            profile.name = name;
            profile.text = text;
            imported.add(profile);
        }
        return new Result<>(imported, failed);
    }

    private static String displayName(Source source, String fallback) {
        return source == null || source.name.isEmpty() ? fallback : source.name;
    }

    private LibraryImportPlanner() {}
}
