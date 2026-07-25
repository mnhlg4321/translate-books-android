package com.ml.tblandroidtxt;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GlossaryImportV48Test {
    @Test public void importedTermsAreMergedAndAvailableToPrompt() {
        GlossaryStore.Glossary glossary = new GlossaryStore.Glossary();
        glossary.name = "Volume 8";

        List<GlossaryStore.Term> first = GlossaryStore.parseTerms(
                "terms.csv",
                "source,target,category\nAlice,Alice,character\nMagic,Phép thuật,term");
        List<GlossaryStore.Term> duplicate = GlossaryStore.parseTerms(
                "more.csv",
                "Alice,Alice,character\nGuild,Hội,term");

        GlossaryStore.mergeTerms(glossary, first);
        GlossaryStore.mergeTerms(glossary, duplicate);

        assertEquals(3, glossary.count());
        String prompt = GlossaryStore.toPromptText(glossary);
        assertTrue(prompt.contains("# Glossary: Volume 8"));
        assertTrue(prompt.contains("Magic => Phép thuật [term]"));
        assertTrue(prompt.contains("Guild => Hội [term]"));
    }

    @Test public void invalidImportDoesNotProduceTerms() {
        assertTrue(GlossaryStore.parseTerms("empty.txt", "not a glossary mapping").isEmpty());
    }

    @Test public void defaultNameAdoptsSingleImportedFileNameExactly() {
        assertEquals(
                "024_FINAL_QA_GLOSSARY.csv",
                GlossaryStore.suggestedImportName(
                        "New glossary",
                        Collections.singletonList("024_FINAL_QA_GLOSSARY.csv")));
    }

    @Test public void customNameIsNeverOverwrittenByImport() {
        assertEquals(
                "Volume 24 master glossary",
                GlossaryStore.suggestedImportName(
                        "Volume 24 master glossary",
                        Collections.singletonList("024_FINAL_QA_GLOSSARY.csv")));
    }

    @Test public void multipleFilesProduceDeterministicEditableName() {
        assertEquals(
                "volume24-characters.csv +2 files",
                GlossaryStore.suggestedImportName(
                        "",
                        Arrays.asList("volume24-characters.csv", "skills.csv", "places.csv")));
    }

    @Test public void failedImportCannotRenamePlaceholder() {
        assertEquals(
                "New glossary",
                GlossaryStore.suggestedImportName("New glossary", Collections.emptyList()));
    }
}
