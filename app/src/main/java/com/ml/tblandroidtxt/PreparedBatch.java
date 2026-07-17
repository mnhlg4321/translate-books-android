package com.ml.tblandroidtxt;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/** Durable, exact source/chunk snapshot prepared before a translation job starts. */
public final class PreparedBatch {
    public enum Status { PREPARING, READY, ERROR, CANCELLED }

    public static final class Input {
        public int ordinal;
        public Uri uri;
        public String displayName = "";
        public String inputHash = "";
        public String encoding = "";
        public List<Chunk> chunks = new ArrayList<>();
    }

    public String id = "";
    public String selectionKey = "";
    public String constructionKey = "";
    public String estimateKey = "";
    public String settingsJson = "";
    public Status status = Status.PREPARING;
    public int progress;
    public String error = "";
    public long createdAt;
    public final List<Input> inputs = new ArrayList<>();
    public CostEstimator.Estimate estimate;

    public int exactChunkCount() {
        int total = 0;
        for (Input input : inputs) total += input.chunks == null ? 0 : input.chunks.size();
        return total;
    }

    public boolean ready() {
        return status == Status.READY && !id.isEmpty() && exactChunkCount() > 0 && estimate != null;
    }
}
