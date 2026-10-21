package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChapterSetVolumeModelTest {
    @Test public void retranslatedRawInAnotherSetDoesNotCompleteThisSet() {
        ChapterSetStore.SetData live = live("target", row(1, "raw-a"));
        RunGroups.SessionInfo other = info("other-run", "other", 10);
        TranslationRepository.JobSummary job = job(20, "COMPLETED", "other-run", 0, RawUri.of("raw-a"));

        ChapterSetVolumeModel.Volume volume = ChapterSetVolumeModel.build(
                "target", live, Collections.<ChapterSession>emptyList(), Collections.singletonList(other),
                Collections.singletonList(job), Collections.<String, String>emptyMap());

        assertEquals(1, volume.chapters.size());
        assertNull(volume.chapters.get(0).job);
        assertEquals(0, volume.doneCount());
    }

    @Test public void resumedFromAnotherSetUsesCreatingSnapshotAndFileModeUsesUriFallback() {
        ChapterSession old = session("old-run", "other", 10, binding("raw-b", 2, "Old B"));
        RunGroups.SessionInfo oldInfo = info("old-run", "other", 10);
        RunGroups.SessionInfo targetInfo = info("target-run", "target", 20);
        TranslationRepository.JobSummary continued = job(21, "COMPLETED", "old-run", 0, RawUri.of("wrong"));
        targetInfo.resumedJobs.add(21L);

        ChapterSetStore.SetData live = live("target", row(2, "raw-b"), row(3, "raw-c"));
        TranslationRepository.JobSummary fileMode = job(22, "error", "", 0, RawUri.of("raw-c"));
        ChapterSetVolumeModel.Volume volume = ChapterSetVolumeModel.build(
                "target", live, Collections.singletonList(old), Arrays.asList(oldInfo, targetInfo),
                Arrays.asList(continued, fileMode), Collections.<String, String>emptyMap());

        assertEquals(2, volume.doneCount() + volume.errorCount());
        assertEquals(21L, find(volume.chapters, "raw-b").job.id);
        assertEquals(22L, find(volume.chapters, "raw-c").job.id);
    }

    @Test public void errorAndMissingJobRemainDistinct() {
        ChapterSetStore.SetData live = live("target", row(1, "raw-a"), row(2, "raw-b"));
        TranslationRepository.JobSummary error = job(30, "NEEDS_REVIEW", "", 0, RawUri.of("raw-a"));
        ChapterSetVolumeModel.Volume volume = ChapterSetVolumeModel.build(
                "target", live, Collections.<ChapterSession>emptyList(), Collections.<RunGroups.SessionInfo>emptyList(),
                Collections.singletonList(error), Collections.<String, String>emptyMap());

        assertEquals(0, volume.doneCount());
        assertEquals(1, volume.errorCount());
        assertEquals("Lỗi", find(volume.chapters, "raw-a").statusLabel());
        assertEquals("Chưa dịch", find(volume.chapters, "raw-b").statusLabel());
        assertTrue(volume.unfinishedCount() == 2);
    }

    @Test public void deletedSetUnionsAllRunsWhenLaterRunContainsOnlySubset() {
        ChapterSession first = session("first", "gone", 10, binding("raw-a", 1, "A"), binding("raw-b", 2, "B"));
        ChapterSession later = session("later", "gone", 20, binding("raw-b", 2, "B later"));
        ChapterSetVolumeModel.Volume volume = ChapterSetVolumeModel.build(
                "gone", null, Arrays.asList(first, later), Collections.<RunGroups.SessionInfo>emptyList(),
                Collections.<TranslationRepository.JobSummary>emptyList(), Collections.<String, String>emptyMap());

        assertTrue(volume.deleted);
        assertEquals(2, volume.chapters.size());
        assertEquals("B later", find(volume.chapters, "raw-b").rawName);
        assertFalse(find(volume.chapters, "raw-a").finished());
    }

    private static ChapterSetStore.SetData live(String id, ChapterSetStore.Row... rows) {
        ChapterSetStore.SetData data = new ChapterSetStore.SetData();
        data.id = id; data.name = "Live"; data.rows.addAll(Arrays.asList(rows));
        return data;
    }

    private static ChapterSetStore.Row row(int number, String rawId) { return new ChapterSetStore.Row(number, rawId, "", ""); }

    private static ChapterSession session(String id, String setId, long created, ChapterBinding... bindings) {
        ChapterSession session = new ChapterSession(id);
        session.setId = setId; session.setName = "Set " + setId; session.createdAt = created;
        session.chapters.addAll(Arrays.asList(bindings));
        return session;
    }

    private static ChapterBinding binding(String rawId, int chapterNo, String name) {
        ChapterBinding binding = new ChapterBinding();
        binding.rawId = rawId; binding.rawName = name; binding.chapterNo = chapterNo;
        return binding;
    }

    private static RunGroups.SessionInfo info(String id, String setId, long created) {
        RunGroups.SessionInfo info = new RunGroups.SessionInfo();
        info.sessionId = id; info.setId = setId; info.createdAt = created;
        return info;
    }

    private static TranslationRepository.JobSummary job(long id, String status, String session, int ordinal, String inputUri) {
        TranslationRepository.JobSummary job = new TranslationRepository.JobSummary();
        job.id = id; job.status = status; job.startSessionId = session; job.preparedOrdinal = ordinal; job.inputUri = inputUri;
        return job;
    }

    private static ChapterSetVolumeModel.Chapter find(List<ChapterSetVolumeModel.Chapter> chapters, String rawId) {
        for (ChapterSetVolumeModel.Chapter chapter : chapters) if (rawId.equals(chapter.rawId)) return chapter;
        throw new AssertionError("Missing " + rawId);
    }
}
