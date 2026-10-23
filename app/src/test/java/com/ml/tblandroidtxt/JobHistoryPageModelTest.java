package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class JobHistoryPageModelTest {
    @Test public void keysetCursorUsesTheOldestIdNotUpdatedAt() {
        List<TranslationRepository.JobSummary> page = new ArrayList<>();
        page.add(job(300)); page.add(job(250)); page.add(job(275));
        assertEquals(250L, JobHistoryPageModel.nextCursor(page));
        assertFalse(JobHistoryPageModel.hasMore(page));
    }

    @Test public void exactlyFiftyRowsRequestsAnotherPage() {
        List<TranslationRepository.JobSummary> page = new ArrayList<>();
        for (int i = 50; i > 0; i--) page.add(job(i));
        assertTrue(JobHistoryPageModel.hasMore(page));
        assertEquals(50, JobHistoryPageModel.append(null, page).size());
    }

    private static TranslationRepository.JobSummary job(long id) {
        TranslationRepository.JobSummary out = new TranslationRepository.JobSummary();
        out.id = id; return out;
    }
}
