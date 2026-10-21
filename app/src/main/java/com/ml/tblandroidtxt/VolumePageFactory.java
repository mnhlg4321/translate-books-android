package com.ml.tblandroidtxt;

import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The translation-side page for one saved or still-live workspace. */
final class VolumePageFactory {
    private final MainActivity a;

    VolumePageFactory(MainActivity activity) { a = activity; }

    View build(String setId) {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);

        ChapterSetWorkspaceStore.Snapshot snapshot = a.cachedWorkspace;
        if (snapshot == null) {
            root.addView(a.sectionCard("▣", "Tập"));
            root.addView(a.text("Đang tải danh sách tập…", 14, a.MUTED, false));
            a.refreshJobCacheAsync(false);
            return scroll;
        }
        WorkspaceSetModel.Entry entry = snapshot.set(setId);
        if (entry == null) {
            LinearLayout missing = a.sectionCard("▣", "Không tìm thấy tập");
            missing.addView(a.text("Tập này không còn trong dữ liệu đã lưu.", 14, a.MUTED, false));
            missing.addView(a.primaryButton("Về Thư viện", v -> a.navigateTo("Files", "", "Volume")), a.marginLP(-1, -2, 0, 12, 0, 0));
            root.addView(missing);
            return scroll;
        }

        ChapterSetVolumeModel.Volume volume = ChapterSetVolumeModel.build(
                setId, snapshot.liveData.get(setId), snapshot.sessions, snapshot.sessionInfos,
                snapshot.jobsForSet(setId), snapshot.rawNames);
        root.addView(header(entry, volume));
        View action = primaryAction(entry, volume, snapshot.rawNames.keySet());
        if (action != null) root.addView(action, a.marginLP(-1, -2, 0, 0, 0, 4));
        root.addView(chapterList(volume));
        return scroll;
    }

    private View header(WorkspaceSetModel.Entry entry, ChapterSetVolumeModel.Volume volume) {
        LinearLayout card = a.sectionCard("▣", entry.label());
        String summary = volume.chapters.size() + " chương · " + volume.doneCount() + " xong";
        if (volume.errorCount() > 0) summary += " · " + volume.errorCount() + " lỗi";
        int unfinished = volume.unfinishedCount();
        if (unfinished > 0) summary += " · " + unfinished + " chưa dịch";
        TextView meta = a.text(summary, 13, a.MUTED, false);
        meta.setSingleLine(false);
        card.addView(meta, a.marginLP(-1, -2, 0, 0, 0, 0));
        return card;
    }

    private View primaryAction(WorkspaceSetModel.Entry entry,
                               ChapterSetVolumeModel.Volume volume,
                               Set<String> availableRawIds) {
        if (volume.deleted) {
            Button button = a.primaryButton("Xem trong Công việc", v -> a.navigateTo("Jobs", entry.id, "Volume"));
            button.setContentDescription("Xem tập trong Công việc");
            return button;
        }
        if (volume.hasActiveJob()) {
            Button button = a.primaryButton("Xem tiến trình", v -> a.navigateTo("Jobs", entry.id, "Volume"));
            button.setContentDescription("Xem tiến trình của tập");
            return button;
        }
        if (volume.errorCount() > 0) {
            Button button = a.primaryButton("Xem " + volume.errorCount() + " chương lỗi", v -> a.navigateTo("Jobs", entry.id, "Volume"));
            button.setContentDescription("Xem các chương lỗi của tập");
            return button;
        }
        if (volume.unfinishedCount() > 0 && volume.hasUsableRawForUnfinished(availableRawIds)) {
            Button button = a.primaryButton("Dịch tiếp", v -> a.continueChapterSet(entry.id));
            button.setContentDescription("Dịch tiếp tập");
            return button;
        }
        return null;
    }

    private View chapterList(ChapterSetVolumeModel.Volume volume) {
        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, a.dp(8), 0, a.dp(20));
        if (volume.chapters.isEmpty()) {
            list.addView(a.text("Chưa có chương trong tập này.", 14, a.MUTED, false));
            return list;
        }
        for (ChapterSetVolumeModel.Chapter chapter : volume.chapters) {
            list.addView(chapterRow(chapter), a.marginLP(-1, -2, 0, 0, 0, 8));
        }
        return list;
    }

    private View chapterRow(final ChapterSetVolumeModel.Chapter chapter) {
        LinearLayout card = a.card(12, a.FIELD, a.BORDER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        String number = chapter.chapterNo >= 0 ? "Ch." + chapter.chapterNo : "Ch.?";
        String name = chapter.rawName == null || chapter.rawName.trim().isEmpty() ? chapter.rawId : chapter.rawName;
        TextView title = a.text(number + "  " + name, 14, a.TEXT, true);
        title.setSingleLine(false);
        card.addView(title);
        String state = chapter.statusLabel();
        if (chapter.job != null) state += " · " + chapter.job.doneChunks + "/" + chapter.job.totalChunks + " chunk";
        TextView meta = a.text(state, 12, chapter.failed() ? a.RED : a.MUTED, false);
        meta.setSingleLine(false);
        card.addView(meta, a.marginLP(-1, -2, 0, 4, 0, 0));
        if (chapter.failed() && chapter.job != null) {
            Button retry = a.tinyButton("Retry", a.RED, v -> a.retryJobFailedChunks(chapter.job.id));
            retry.setContentDescription("Retry chương " + number);
            card.addView(retry, a.marginLP(-1, -2, 0, 6, 0, 0));
        }
        if (chapter.job != null) {
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> a.openJobDetails(chapter.job.id));
        }
        return card;
    }
}
