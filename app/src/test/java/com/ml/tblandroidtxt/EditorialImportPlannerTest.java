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
        assertEquals(1,result.readyCount()); assertEquals("01",result.chapters.get(0).key); assertNotNull(result.glossary); assertNotNull(result.pronoun);
    }
    @Test public void keepsUnpairedFilesVisibleAndBlocked() {
        EditorialImportPlanner.Result result=EditorialImportPlanner.plan(Arrays.asList(new EditorialImportPlanner.Source("Ch 02 RAW.txt","raw", "r")));
        assertEquals(0,result.readyCount()); assertEquals("missing DRAFT",result.chapters.get(0).problem);
    }
    @Test public void rejectsAmbiguousNames() {
        EditorialImportPlanner.Result result=EditorialImportPlanner.plan(Arrays.asList(new EditorialImportPlanner.Source("chapter.txt","x", "x")));
        assertEquals("name must include RAW or DRAFT",result.chapters.get(0).problem);
    }
    @Test public void explicitPickersDoNotRequireRawDraftTokensOrTxtExtension() {
        EditorialImportPlanner.Result result=EditorialImportPlanner.plan(
                Arrays.asList(new EditorialImportPlanner.Source("005_RAW_MERCEDES_VOL3 (Vietnamese).md","raw","r")),
                Arrays.asList(new EditorialImportPlanner.Source("005_DRAFT_MERCEDES_VOL3 (Vietnamese).doc","draft","d")));
        assertEquals(1,result.readyCount());
        assertEquals("005",result.chapters.get(0).key);
    }

    @Test public void canonicalChapterNumberPairsDifferentZeroPadding() {
        EditorialImportPlanner.Result result=EditorialImportPlanner.plan(
                Arrays.asList(new EditorialImportPlanner.Source("005_RAW.txt","raw","r")),
                Arrays.asList(new EditorialImportPlanner.Source("5_DRAFT.txt","draft","d")));
        assertEquals(1,result.readyCount());
        assertEquals("005",result.chapters.get(0).key);
        assertEquals("number:5",result.chapters.get(0).normalizedKey);
    }

    @Test public void numericChaptersSortBeforeAndByNumberNotFilenameText() {
        EditorialImportPlanner.Result result=EditorialImportPlanner.plan(
                Arrays.asList(new EditorialImportPlanner.Source("10_RAW.txt","10raw","r10"),new EditorialImportPlanner.Source("2_RAW.txt","2raw","r2")),
                Arrays.asList(new EditorialImportPlanner.Source("10_DRAFT.txt","10draft","d10"),new EditorialImportPlanner.Source("2_DRAFT.txt","2draft","d2")));
        assertEquals("2",result.chapters.get(0).key);
        assertEquals("10",result.chapters.get(1).key);
    }

    @Test public void sameSelectedFileCannotBecomeBothRoles() {
        EditorialImportPlanner.Result result=EditorialImportPlanner.plan(
                Arrays.asList(new EditorialImportPlanner.Source("005.txt","content://same","r")),
                Arrays.asList(new EditorialImportPlanner.Source("005.txt","content://same","r")));
        assertEquals(0,result.readyCount());
        assertEquals("RAW and DRAFT use the same file",result.chapters.get(0).problem);
    }
}
