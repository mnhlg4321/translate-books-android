package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;

/** Pure keyset-page rules for the Jobs history screen. */
public final class JobHistoryPageModel {
    public static final int PAGE_SIZE = 50;

    private JobHistoryPageModel() {}

    public static long nextCursor(List<TranslationRepositoryCore.JobSummary> page) {
        if (page == null || page.isEmpty()) return -1L;
        long cursor = Long.MAX_VALUE;
        for (TranslationRepositoryCore.JobSummary row : page) if (row != null) cursor = Math.min(cursor, row.id);
        return cursor == Long.MAX_VALUE ? -1L : cursor;
    }

    public static boolean hasMore(List<TranslationRepositoryCore.JobSummary> page) {
        return page != null && page.size() >= PAGE_SIZE;
    }

    public static List<TranslationRepositoryCore.JobSummary> append(
            List<TranslationRepositoryCore.JobSummary> current,
            List<TranslationRepositoryCore.JobSummary> page) {
        ArrayList<TranslationRepositoryCore.JobSummary> out = new ArrayList<>();
        if (current != null) out.addAll(current);
        if (page != null) out.addAll(page);
        return out;
    }
}
