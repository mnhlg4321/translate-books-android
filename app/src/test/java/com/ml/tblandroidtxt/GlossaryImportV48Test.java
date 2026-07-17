package com.ml.tblandroidtxt;

import org.junit.Test;

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
}
