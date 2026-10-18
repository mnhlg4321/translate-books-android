package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** v4.23 resume fix: the question asked on Start, which job each input continues, and a job that only lacks chunks is not an integrity failure. */
public class ResumePromptTest {
    private static TranslationRepository.JobProgress job(long id, String name, String hash, int total, int done, int error, int unknown, int pending) {
        TranslationRepository.JobProgress p = new TranslationRepository.JobProgress();
        p.id = id; p.fileName = name; p.inputHash = hash; p.total = total; p.done = done; p.error = error; p.unknown = unknown; p.pending = pending;
        return p;
    }

    @Test public void progressLineNamesEveryCount() {
        assertEquals("3/15 chunk xong · 1 lỗi · 11 chưa dịch", ResumePrompt.progressLine(job(1, "a", "h", 15, 3, 1, 0, 11)));
        assertEquals("15/15 chunk xong", ResumePrompt.progressLine(job(1, "a", "h", 15, 15, 0, 0, 0)));
        assertTrue(ResumePrompt.progressLine(job(1, "a", "h", 4, 1, 0, 1, 2)).contains("1 không rõ đã gửi"));
    }

    @Test public void messageListsEachFileAndExplainsBothChoices() {
        String one = ResumePrompt.message(Arrays.asList(job(7, "025_RAW.txt", "h", 15, 3, 1, 0, 11)));
        assertTrue(one.startsWith("Đã có job dở cho tệp này:"));
        assertTrue(one.contains("• 025_RAW.txt — 3/15 chunk xong"));
        assertTrue(one.contains("Tiếp tục job cũ"));
        assertTrue(one.contains("Dịch lại từ đầu"));
        String two = ResumePrompt.message(Arrays.asList(job(7, "a.txt", "h1", 5, 1, 0, 0, 4), job(8, "b.txt", "h2", 5, 2, 0, 0, 3)));
        assertTrue(two.startsWith("Đã có job dở cho 2 tệp:"));
    }

    @Test public void eachInputContinuesTheNewestJobWithItsOwnHashAndUnknownDeliveryIsNeverResumed() {
        List<TranslationRepository.JobProgress> partial = Arrays.asList(
                job(30, "ch2.txt", "H2", 10, 4, 0, 0, 6),
                job(20, "ch2-old.txt", "H2", 10, 1, 0, 0, 9),
                job(10, "ch3.txt", "H3", 10, 4, 0, 1, 5));
        Map<Integer, Long> m = ResumePrompt.chooseJobs(Arrays.asList(0, 1, 2), Arrays.asList("H1", "H2", "H3"), partial);
        assertEquals("chapter 1 has no unfinished job: a new one is created", 1, m.size());
        assertFalse(m.containsKey(0));
        assertEquals(Long.valueOf(30), m.get(1));
        assertFalse("a job with a request of unknown delivery is not resumed automatically", m.containsKey(2));
    }

    @Test public void resumeMapRoundTripsThroughJson() throws Exception {
        Map<Integer, Long> m = new java.util.HashMap<>();
        m.put(0, 5L); m.put(2, 9L);
        JSONObject o = new JSONObject(ResumePrompt.toJson(m));
        assertEquals(5L, o.getLong("0"));
        assertEquals(9L, o.getLong("2"));
        assertEquals("{}", ResumePrompt.toJson(new java.util.HashMap<Integer, Long>()));
    }

    private static TranslationRepository.ChunkRow row(int idx, String status, String text) {
        TranslationRepository.ChunkRow r = new TranslationRepository.ChunkRow();
        r.index = idx; r.status = status; r.translated = text; r.source = "s" + idx; r.startOffset = idx * 2; r.endOffset = idx * 2 + 2;
        r.sourceHash = HashUtil.sha256(r.source);
        return r;
    }

    @Test public void aJobThatOnlyLacksChunksIsPausedNotAnIntegrityFailure() {
        List<TranslationRepository.ChunkRow> rows = new ArrayList<>(Arrays.asList(row(0, "COMPLETED", "one"), row(1, "COMPLETED", "two"), row(2, "PENDING", ""), row(3, "PENDING", "")));
        OutputIntegrityAudit.Result r = OutputIntegrityAudit.audit(rows);
        assertFalse(r.passed());
        assertTrue("pending chunks (blank output) are only incomplete", r.onlyIncomplete());
        assertEquals(2, r.incompleteCount());
        assertEquals("PAUSED", r.status());
    }

    @Test public void incompleteDetectionIgnoresOtherErrorsAndCountsChunks() {
        OutputIntegrityAudit.Result only = new OutputIntegrityAudit.Result();
        only.errors.add("INCOMPLETE_CHUNK:2"); only.errors.add("INCOMPLETE_CHUNK:3");
        assertTrue(only.onlyIncomplete());
        assertEquals(2, only.incompleteCount());
        assertEquals("PAUSED", only.status());
        OutputIntegrityAudit.Result mixed = new OutputIntegrityAudit.Result();
        mixed.errors.add("INCOMPLETE_CHUNK:2"); mixed.errors.add("SOURCE_HASH_MISMATCH:1");
        assertFalse(mixed.onlyIncomplete());
        assertEquals("FAILED_INTEGRITY_CHECK", mixed.status());
        assertFalse(new OutputIntegrityAudit.Result().onlyIncomplete());
        assertEquals("COMPLETED", new OutputIntegrityAudit.Result().status());
    }
}
