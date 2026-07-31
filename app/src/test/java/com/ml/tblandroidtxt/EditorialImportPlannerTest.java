package com.ml.tblandroidtxt;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class EditorialImportPlannerTest {
    @Test public void mapsRawDraftAndSharedAssetsFromOneBatch() {
        EditorialImportPlanner.Result result=EditorialImportPlanner.plan(Arrays.asList(
                new EditorialImportPlanner.Source("Ch 01 RAW.txt","raw", "r"),
                new EditorialImportPlanner.Source("Ch 01 DRAFT.txt","draft", "d"),
                new EditorialImportPlanner.Source("Series Glossary.txt","g", "g"),
                new EditorialImportPlanner.Source("Series Pronoun.txt","p", "p")));
        assertEquals(1,result.readyCount()); assertEquals("Ch 01",result.chapters.get(0).key); assertNotNull(result.glossary); assertNotNull(result.pronoun);
    }
    @Test public void keepsUnpairedFilesVisibleAndBlocked() {
        EditorialImportPlanner.Result result=EditorialImportPlanner.plan(Arrays.asList(new EditorialImportPlanner.Source("Ch 02 RAW.txt","raw", "r")));
        assertEquals(0,result.readyCount()); assertEquals("missing DRAFT",result.chapters.get(0).problem);
    }
    @Test public void rejectsAmbiguousNames() {
        EditorialImportPlanner.Result result=EditorialImportPlanner.plan(Arrays.asList(new EditorialImportPlanner.Source("chapter.txt","x", "x")));
        assertEquals("name must include RAW or DRAFT",result.chapters.get(0).problem);
    }
}
