package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;

/** What happened to each chapter of one chapter-set run, for the closing line "x xong · y lỗi (…) · z chưa chạy". */
final class ChapterRunOutcome {
    private final int total;
    private final List<String> done = new ArrayList<>();
    private final List<String> failed = new ArrayList<>();

    ChapterRunOutcome(int total) { this.total = total; }

    void done(String name) { done.add(name); }

    void failed(String name, String reason) { failed.add(reason == null || reason.trim().isEmpty() ? name : name + ": " + reason.trim()); }

    int doneCount() { return done.size(); }

    int failedCount() { return failed.size(); }

    /** Chapters neither finished nor failed (they were not started because the queue stopped). */
    int notRun() { return Math.max(0, total - done.size() - failed.size()); }

    /** Only a failure that belongs to one chapter lets the queue go on. */
    static boolean continuesQueue(Throwable error) {
        return error instanceof ChapterLocalException;
    }

    /** {@code stoppedAt}: the chapter that was running when the queue stopped, counted as failed by the caller, or null. */
    String summary() {
        StringBuilder sb = new StringBuilder("Bộ chương: ").append(done.size()).append(" xong · ").append(failed.size()).append(" lỗi");
        if (!failed.isEmpty()) {
            sb.append(" (");
            for (int i = 0; i < failed.size(); i++) {
                if (i > 0) sb.append("; ");
                sb.append(failed.get(i));
            }
            sb.append(')');
        }
        sb.append(" · ").append(notRun()).append(" chưa chạy");
        return sb.toString();
    }
}
