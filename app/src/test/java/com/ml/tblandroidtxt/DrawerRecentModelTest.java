package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class DrawerRecentModelTest {
    @Test public void recentMixIncludesSingleChapterAndKeepsActiveFirst() {
        WorkspaceSetModel.Entry set = new WorkspaceSetModel.Entry();
        set.id = "set-a"; set.name = "Tập A"; set.updatedAt = 10;
        TranslationRepository.JobSummary single = job(9, "009.txt", 30);
        java.util.List<DrawerRecentModel.Item> items = DrawerRecentModel.build(
                Collections.singletonList(set), Collections.singletonList(single),
                Collections.<RunGroups.SessionInfo>emptyList(), "", 9, 3);
        assertEquals(2, items.size());
        assertFalse(items.get(0).set);
        assertEquals(9L, items.get(0).jobId);
        assertTrue(items.get(1).set);
    }

    @Test public void activeSetIsFirstEvenWhenAnOlderSingleJobExists() {
        WorkspaceSetModel.Entry set = new WorkspaceSetModel.Entry();
        set.id = "set-a"; set.name = "Tập A"; set.updatedAt = 10;
        TranslationRepository.JobSummary single = job(9, "009.txt", 30);
        java.util.List<DrawerRecentModel.Item> items = DrawerRecentModel.build(
                Collections.singletonList(set), Collections.singletonList(single),
                Collections.<RunGroups.SessionInfo>emptyList(), "set-a", -1L, 3);
        assertTrue(items.get(0).set);
        assertEquals("set-a", items.get(0).id);
    }

    @Test public void twoJobsForTheSameInputCollapseToTheNewestSingleItem() {
        TranslationRepository.JobSummary older = job(7, "005.txt", 40);
        older.inputHash = "same-input";
        TranslationRepository.JobSummary newer = job(9, "005.txt", 80);
        newer.inputHash = "same-input";
        java.util.List<DrawerRecentModel.Item> items = DrawerRecentModel.build(
                Collections.<WorkspaceSetModel.Entry>emptyList(), Arrays.asList(older, newer),
                Collections.<RunGroups.SessionInfo>emptyList(), "", -1L, 3);
        assertEquals(1, items.size());
        assertEquals(9L, items.get(0).jobId);
    }

    @Test public void activeOlderJobStillWinsItsInputGroup() {
        TranslationRepository.JobSummary older = job(7, "005.txt", 40);
        older.inputHash = "same-input";
        TranslationRepository.JobSummary newer = job(9, "005.txt", 80);
        newer.inputHash = "same-input";
        java.util.List<DrawerRecentModel.Item> items = DrawerRecentModel.build(
                Collections.<WorkspaceSetModel.Entry>emptyList(), Arrays.asList(older, newer),
                Collections.<RunGroups.SessionInfo>emptyList(), "", 7L, 3);
        assertEquals(1, items.size());
        assertEquals(7L, items.get(0).jobId);
        assertTrue(items.get(0).active);
    }

    private static TranslationRepository.JobSummary job(long id, String name, long updated) {
        TranslationRepository.JobSummary out = new TranslationRepository.JobSummary();
        out.id = id; out.fileName = name; out.updatedAt = updated; out.status = "done";
        return out;
    }
}
