package com.ml.tblandroidtxt;

import android.content.Context;

import java.util.List;

/** Checkpoint facade for v2.7.x. Keeps future checkpoint UI independent from DB implementation. */
public class CheckpointStore {
    private final TranslationRepository repo;

    public CheckpointStore(Context context) { repo = new TranslationRepository(context.getApplicationContext()); }

    public TranslationRepository.Job lastResumableJob() { return repo.getLastIncompleteJob(); }
    public List<TranslationRepository.ChunkRow> rows(long jobId) { return repo.getChunkRows(jobId); }
    public void pause(long jobId) { repo.touchJob(jobId, "paused"); }
    public void markCancelled(long jobId) { repo.touchJob(jobId, "cancelled"); }
}
