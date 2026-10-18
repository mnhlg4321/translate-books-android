package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;

/**
 * What a bulk delete may do with the ticked items. Items a translation job still needs are left out of the delete;
 * Active items and items used by a chapter set or an Editorial combination are deleted only after a second confirmation.
 */
final class LibraryDeletePlan {
    static final class Target {
        final String id;
        final String name;
        boolean active;
        final List<String> chapterSets = new ArrayList<>();
        final List<String> editorialCombos = new ArrayList<>();
        final List<String> unfinishedJobs = new ArrayList<>();

        Target(String id, String name) { this.id = id; this.name = name == null || name.trim().isEmpty() ? "(không tên)" : name.trim(); }

        boolean blocked() { return !unfinishedJobs.isEmpty(); }
        boolean risky() { return active || !chapterSets.isEmpty() || !editorialCombos.isEmpty(); }
    }

    final List<Target> deletable = new ArrayList<>();
    final List<Target> blocked = new ArrayList<>();

    static LibraryDeletePlan plan(List<Target> targets) {
        LibraryDeletePlan plan = new LibraryDeletePlan();
        for (Target t : targets) (t.blocked() ? plan.blocked : plan.deletable).add(t);
        return plan;
    }

    List<String> deletableIds() {
        ArrayList<String> ids = new ArrayList<>();
        for (Target t : deletable) ids.add(t.id);
        return ids;
    }

    boolean needsSecondConfirm() {
        for (Target t : deletable) if (t.risky()) return true;
        return false;
    }

    /** First dialog: how many items, and the ones that are left out. */
    String firstMessage() {
        StringBuilder sb = new StringBuilder();
        if (deletable.isEmpty()) sb.append("Không có mục nào xóa được.");
        else sb.append("Xóa ").append(deletable.size()).append(" mục khỏi thư viện của app (tệp gốc trong máy không bị xóa).\nSau khi xóa còn 10 giây để Hoàn tác.");
        if (!blocked.isEmpty()) {
            sb.append("\n\nBỏ qua ").append(blocked.size()).append(" mục vì còn job dịch dở cần đọc lại:");
            for (Target t : blocked) sb.append("\n• ").append(t.name).append(" — job ").append(join(t.unfinishedJobs));
        }
        return sb.toString();
    }

    /** Second dialog: only the risky items among those that will be deleted. */
    String secondMessage() {
        StringBuilder sb = new StringBuilder("Các mục sau đang được dùng. Xóa vẫn được, nhưng:");
        for (Target t : deletable) {
            if (!t.risky()) continue;
            sb.append("\n• ").append(t.name);
            if (t.active) sb.append(" — đang Active (bản dịch kế tiếp sẽ không dùng nó nữa)");
            if (!t.chapterSets.isEmpty()) sb.append(" — nằm trong bộ chương ").append(join(t.chapterSets)).append(" (dòng đó thành \"thiếu\")");
            if (!t.editorialCombos.isEmpty()) sb.append(" — tổ hợp Biên tập ").append(join(t.editorialCombos)).append(" đang tham chiếu");
        }
        return sb.toString();
    }

    private static String join(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(values.get(i));
        }
        return sb.toString();
    }
}
