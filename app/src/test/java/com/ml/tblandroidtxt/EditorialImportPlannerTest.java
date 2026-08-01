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

    @Test public void fourRoleBundleKeeps005006007ReferencesWithTheirOwnChapter() {
        EditorialImportPlanner.Result result=EditorialImportPlanner.planBundle(Arrays.asList(
                new EditorialImportPlanner.Source("005_RAW_MERCEDES_VOL3.txt","raw-005","raw005"),
                new EditorialImportPlanner.Source("005_RAW_MERCEDES_VOL3 (Vietnamese).txt","draft-005","draft005"),
                new EditorialImportPlanner.Source("005_FINAL_GLOSSARY_MERCEDES_VOL3.csv","glossary-005","glossary005"),
                new EditorialImportPlanner.Source("005_FINAL_QA_PRONOUN_KAKETA_TSUKI_NO_MERCEDES_VOL3.csv","pronoun-005","pronoun005"),
                new EditorialImportPlanner.Source("006_RAW_MERCEDES_VOL3.txt","raw-006","raw006"),
                new EditorialImportPlanner.Source("006_RAW_MERCEDES_VOL3 (Vietnamese).txt","draft-006","draft006"),
                new EditorialImportPlanner.Source("006_FINAL_GLOSSARY_MERCEDES_VOL3.csv","glossary-006","glossary006"),
                new EditorialImportPlanner.Source("006_FINAL_QA_PRONOUN_KAKETA_TSUKI_NO_MERCEDES_VOL3.csv","pronoun-006","pronoun006"),
                new EditorialImportPlanner.Source("007_RAW_MERCEDES_VOL3.txt","raw-007","raw007"),
                new EditorialImportPlanner.Source("007_RAW_MERCEDES_VOL3 (Vietnamese).txt","draft-007","draft007"),
                new EditorialImportPlanner.Source("007_FINAL_GLOSSARY_MERCEDES_VOL3.csv","glossary-007","glossary007"),
                new EditorialImportPlanner.Source("007_FINAL_QA_PRONOUN_KAKETA_TSUKI_NO_MERCEDES_VOL3.csv","pronoun-007","pronoun007")));

        assertEquals(3,result.readyCount());
        assertEquals(3,result.chapters.size());
        assertEquals("005",result.chapters.get(0).key);
        assertEquals("006",result.chapters.get(1).key);
        assertEquals("007",result.chapters.get(2).key);
        assertEquals("glossary-005",result.chapters.get(0).glossary.uri);
        assertEquals("pronoun-005",result.chapters.get(0).pronoun.uri);
        assertEquals(EditorialImportPlanner.ChapterPlan.Status.READY,result.chapters.get(0).status());
        assertEquals("glossary-006",result.chapters.get(1).glossary.uri);
        assertEquals("pronoun-006",result.chapters.get(1).pronoun.uri);
        assertEquals("glossary-007",result.chapters.get(2).glossary.uri);
        assertEquals("pronoun-007",result.chapters.get(2).pronoun.uri);
    }

    @Test public void bundleRequiresAllFourRolesAndMarksDuplicateAsNeedsReview() {
        EditorialImportPlanner.Result result=EditorialImportPlanner.planBundle(Arrays.asList(
                new EditorialImportPlanner.Source("005_RAW.txt","raw","r"),
                new EditorialImportPlanner.Source("005_DRAFT.txt","draft","d"),
                new EditorialImportPlanner.Source("005_FINAL_GLOSSARY.csv","g1","g1"),
                new EditorialImportPlanner.Source("005_FINAL_GLOSSARY_2.csv","g2","g2"),
                new EditorialImportPlanner.Source("005_FINAL_QA_PRONOUN.csv","p","p")));

        assertEquals(0,result.readyCount());
        assertTrue(result.chapters.get(0).problem.contains("multiple GLOSSARY files"));
        assertEquals(EditorialImportPlanner.ChapterPlan.Status.NEEDS_REVIEW,result.chapters.get(0).status());
    }

    @Test public void unclearRoleOrChapterIsUnassignedAndCannotBecomeReady() {
        EditorialImportPlanner.Result result=EditorialImportPlanner.planBundle(Arrays.asList(
                new EditorialImportPlanner.Source("chapter-not-numbered.txt","unknown","x"),
                new EditorialImportPlanner.Source("Series Glossary.csv","glossary","g")));

        assertEquals(0,result.readyCount());
        assertEquals(0,result.chapters.size());
        assertEquals(2,result.unassigned.size());
        assertTrue(result.warnings.get(0).contains("role is unclear"));
        assertTrue(result.warnings.get(1).contains("chapter number/key is unclear"));
    }

    @Test public void csvIsAcceptedOnlyForNamedGlossaryOrPronounRoles() {
        EditorialImportPlanner.Result result=EditorialImportPlanner.planBundle(Arrays.asList(
                new EditorialImportPlanner.Source("005_RAW.txt","raw","r"),
                new EditorialImportPlanner.Source("005_DRAFT.txt","draft","d"),
                new EditorialImportPlanner.Source("005_FINAL_GLOSSARY.csv","g","g"),
                new EditorialImportPlanner.Source("005_FINAL_QA_PRONOUN.csv","p","p")));

        assertEquals(1,result.readyCount());
    }
}
