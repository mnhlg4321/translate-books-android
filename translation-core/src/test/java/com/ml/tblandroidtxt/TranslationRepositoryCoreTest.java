package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** TranslationRepositoryCore over sqlite-jdbc with the real v30 schema; expectations are written by hand. */
public class TranslationRepositoryCoreTest {
    private TestDb testDb;
    private TranslationRepositoryCore repo;

    @Before public void open() throws Exception {
        testDb = TestDb.openWithSchema();
        repo = new TranslationRepositoryCore(testDb.db) {};
    }

    @After public void close() throws Exception { testDb.close(); }

    private static AppSettings settings() {
        AppSettings s = new AppSettings();
        s.sourceLanguage = "Japanese";
        s.targetLanguage = "Vietnamese";
        s.model = "test/model";
        return s;
    }

    private static List<Chunk> twoChunks() {
        ArrayList<Chunk> list = new ArrayList<>();
        list.add(new Chunk(0, 0, 5, "", "alpha", "", ""));
        list.add(new Chunk(1, 5, 9, "", "beta", "", ""));
        return list;
    }

    private long count(String sql) {
        try (DbCursor c = testDb.db.rawQuery(sql, null)) { return c.moveToFirst() ? c.getLong(0) : -1; }
    }

    @Test public void createJobThenProgressThroughChunkStates() {
        long id = repo.createJob("in://a", "out://a", "a.txt", settings(), twoChunks(), "inhash", "sethash");
        assertTrue(id > 0);

        List<TranslationRepositoryCore.JobSummary> list = repo.getJobSummaries(10);
        assertEquals(1, list.size());
        TranslationRepositoryCore.JobSummary s = list.get(0);
        assertEquals(id, s.id);
        assertEquals("a.txt", s.fileName);
        assertEquals("READY", s.status);
        assertEquals("Japanese", s.sourceLanguage);
        assertEquals("Vietnamese", s.targetLanguage);
        assertEquals("in://a", s.inputUri);
        assertEquals("inhash", s.inputHash);
        assertEquals(2, s.totalChunks);
        assertEquals(0, s.doneChunks);
        assertEquals(2, s.pendingChunks);

        List<TranslationRepositoryCore.ChunkRow> rows = repo.getChunkRows(id);
        assertEquals(2, rows.size());
        assertEquals("alpha", rows.get(0).source);
        assertEquals("PENDING", rows.get(0).status);
        assertEquals(5, rows.get(1).startOffset);
        assertEquals(9, rows.get(1).endOffset);
        assertEquals("", rows.get(0).translated);

        repo.markChunkDone(id, 0, "một");
        rows = repo.getChunkRows(id);
        assertEquals("COMPLETED", rows.get(0).status);
        assertEquals("một", rows.get(0).translated);
        assertEquals("PENDING", rows.get(1).status);
        assertEquals(1, repo.firstRunnableChunk(id));
        assertEquals("COMPLETED", repo.chunkStatus(id, 0));

        s = repo.getJobSummaries(10).get(0);
        assertEquals(1, s.doneChunks);
        assertEquals(1, s.pendingChunks);
        assertEquals("running", s.status);

        repo.markChunkError(id, 1, "boom");
        assertEquals("RETRYABLE_ERROR", repo.chunkStatus(id, 1));
        s = repo.getJobSummaries(10).get(0);
        assertEquals(1, s.failedChunks);
        assertEquals("error", s.status);

        List<TranslationRepositoryCore.JobProgress> progress = repo.getIncompleteJobs(Collections.singleton("inhash"), 5);
        assertEquals(1, progress.size());
        assertEquals(id, progress.get(0).id);
        assertEquals(2, progress.get(0).total);
        assertEquals(1, progress.get(0).done);
        assertEquals(1, progress.get(0).error);
        assertEquals(1, progress.get(0).remaining());

        assertEquals("một\n", repo.assembleOutput(id, false));
        assertNotNull(repo.getJob(id));
        assertEquals("inhash", repo.getInputHash(id));
    }

    @Test public void configRevisionsAreRecordedAndJobPointsAtTheLatest() {
        long id = repo.createJob("in://a", "out://a", "a.txt", settings(), twoChunks(), "h", "s");
        assertEquals(1, count("SELECT COUNT(*) FROM config_revisions WHERE job_id=" + id));
        AppSettings changed = settings();
        changed.model = "other/model";
        int revision = repo.applyConfigRevision(id, 1, changed);
        assertEquals(2, revision);
        assertEquals(2, count("SELECT COUNT(*) FROM config_revisions WHERE job_id=" + id));
        assertEquals(2, count("SELECT config_revision FROM jobs WHERE id=" + id));
        assertEquals(1, count("SELECT first_chunk_idx FROM config_revisions WHERE job_id=" + id + " AND revision=2"));
    }

    @Test public void attemptLifecycleKeepsAcceptedTranslation() {
        long id = repo.createJob("in://a", "out://a", "a.txt", settings(), twoChunks(), "h", "s");
        Chunk chunk = twoChunks().get(0);
        repo.beginChunkAttempt(id, chunk, 1, "reqhash", settings());
        assertEquals("REQUESTING", repo.chunkStatus(id, 0));
        assertEquals(1, count("SELECT COUNT(*) FROM chunk_attempts WHERE job_id=" + id));
        repo.commitValidatedChunk(id, 0, "kết quả", "", "PROVIDER");
        assertEquals("COMPLETED", repo.chunkStatus(id, 0));
        repo.beginChunkAttempt(id, chunk, 2, "reqhash2", settings());
        assertEquals("a completed chunk stays completed while a candidate is requested", "COMPLETED", repo.chunkStatus(id, 0));
    }

    @Test public void deleteJobRemovesJobChunksAndRevisions() {
        long id = repo.createJob("in://a", "out://a", "a.txt", settings(), twoChunks(), "h", "s");
        repo.deleteJob(id);
        assertEquals(0, count("SELECT COUNT(*) FROM jobs"));
        assertEquals(0, count("SELECT COUNT(*) FROM chunks"));
        assertEquals(0, count("SELECT COUNT(*) FROM config_revisions"));
        assertNull(repo.getJob(id));
    }

    @Test public void failedCreateJobRollsBackEverything() {
        ArrayList<Chunk> duplicate = new ArrayList<>();
        duplicate.add(new Chunk(0, 0, 5, "", "alpha", "", ""));
        duplicate.add(new Chunk(0, 5, 9, "", "beta", "", ""));
        try {
            repo.createJob("in://a", "out://a", "a.txt", settings(), duplicate, "h", "s");
            fail("duplicate (job_id, idx) must violate the primary key");
        } catch (RuntimeException expected) {
            // like SQLiteConstraintException on Android
        }
        assertEquals(0, count("SELECT COUNT(*) FROM jobs"));
        assertEquals(0, count("SELECT COUNT(*) FROM chunks"));
        assertEquals(0, count("SELECT COUNT(*) FROM config_revisions"));
    }

    @Test public void metricsAreSummedPerJob() {
        long id = repo.createJob("in://a", "out://a", "a.txt", settings(), twoChunks(), "h", "s");
        ChunkMetric m = new ChunkMetric();
        m.jobId = id;
        m.chunkIndex = 0;
        m.phase = "translate";
        m.attempt = 1;
        m.rawTokens = 10;
        m.outputTokens = 20;
        m.outcome = "success";
        m.usageCost = 0.5;
        m.costStatus = "USAGE_DERIVED";
        m.usageSource = "PROVIDER";
        repo.recordMetric(m);
        repo.recordMetric(m);
        TranslationRepositoryCore.MetricsSummary summary = repo.getMetricsSummary(id);
        assertEquals(20, summary.rawTokens);
        assertEquals(40, summary.outputTokens);
        assertEquals(2, summary.attempts);
        assertEquals(2, summary.successes);
        assertEquals(1.0d, summary.totalCost, 1e-9);
    }
}
