package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Chapter card section for a P4-bound Editorial chapter: durable progress (L1 -> L2 -> L3 -> FINAL),
 * typed stop/recovery wording, a viewer for the stored FINAL and a verified TXT export. It reads through
 * {@link EditorialChapterFinalCoordinator#inspect} off the UI thread and never dispatches a provider call;
 * running L2/L3 needs a separate explicit authorization that this panel does not grant.
 */
final class EditorialChapterFinalPanel {
    private final MainActivity a;

    EditorialChapterFinalPanel(MainActivity activity) { a = activity; }

    View build(EditorialRepository.Project project, EditorialRepository.Chapter chapter) {
        LinearLayout box = a.card(10, a.FIELD, a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(8));
        box.addView(a.text("Bản biên tập cuối (L1 → L2 → L3)", 11, a.MUTED, true));
        TextView status = a.text("Đang đọc trạng thái đã lưu…", 11, a.TEXT, false);
        status.setSingleLine(false);
        box.addView(status, a.marginLP(-1, -2, 0, 2, 0, 4));
        LinearLayout actions = a.rowContainer();
        box.addView(actions);
        a.preflightExecutor.submit(() -> {
            EditorialChapterFinalCoordinator.Inspection inspection = inspect(project.id, project.bindingIdentity, chapter.chapterKey);
            a.runOnUiThread(() -> {
                if (a.isFinishing() || a.isDestroyed()) return;
                render(box, status, actions, project, chapter, inspection);
            });
        });
        return box;
    }

    private EditorialChapterFinalCoordinator.Inspection inspect(long projectId, String bindingIdentity, String chapterKey) {
        try (TranslationRepository database = new TranslationRepository(a)) {
            Optional<String> selector = new EditorialP4BindingDao(database).selectorFor(bindingIdentity);
            if (selector.isEmpty()) {
                return new EditorialChapterFinalCoordinator.Inspection(
                        EditorialChapterProgress.derive(false, null, null), null);
            }
            return new EditorialChapterFinalCoordinator(database,
                    new EditorialPackStorageLayout(a.getFilesDir().toPath()))
                    .inspect(projectId, selector.get(), chapterKey);
        } catch (RuntimeException error) {
            return new EditorialChapterFinalCoordinator.Inspection(
                    EditorialChapterProgress.derive(false, null, null), null);
        }
    }

    private void render(LinearLayout box, TextView status, LinearLayout actions, EditorialRepository.Project project,
                        EditorialRepository.Chapter chapter, EditorialChapterFinalCoordinator.Inspection inspection) {
        EditorialChapterProgress.Progress progress = inspection.progress();
        status.setText(EditorialChapterProgress.describe(progress));
        status.setTextColor(progress.finalReady() ? a.GREEN
                : progress.stopClass() == EditorialChapterProgress.StopClass.NONE ? a.TEXT : a.AMBER);
        actions.removeAllViews();
        EditorialL2Execution.Committed finalArtifact = inspection.finalArtifact();
        if (finalArtifact != null) {
            actions.addView(a.primaryButton("Xem bản cuối", v -> showFinal(chapter.chapterKey, finalArtifact)),
                    new LinearLayout.LayoutParams(0, a.dp(44), 1));
            actions.addView(a.space(8, 1));
            actions.addView(a.secondaryButton("Xuất TXT", v -> a.startEditorialFinalExport(project.id,
                    project.bindingIdentity, chapter.chapterKey)), new LinearLayout.LayoutParams(0, a.dp(44), 1));
        } else if (progress.next() == EditorialChapterProgress.NextAction.RUN_STAGE_WITH_AUTHORIZATION) {
            TextView locked = a.text("Chạy L2/L3 đang khóa cho tới khi có cấp phép riêng.", 11, a.MUTED, false);
            locked.setSingleLine(false);
            actions.addView(locked);
        }
    }

    /**
     * Writes the stored FINAL of the chapter to {@code uri} and reads it back. Runs off the UI thread;
     * any failure leaves the stored FINAL untouched and only reports a typed code.
     */
    void exportTo(long projectId, String bindingIdentity, String chapterKey, android.net.Uri uri) {
        a.preflightExecutor.submit(() -> {
            String message;
            try {
                EditorialChapterFinalCoordinator.Inspection inspection = inspect(projectId, bindingIdentity, chapterKey);
                EditorialChapterFinalCoordinator.ExportResult result = EditorialChapterFinalCoordinator.exportTxt(
                        inspection.finalArtifact(),
                        () -> a.getContentResolver().openOutputStream(uri, "wt"),
                        () -> a.getContentResolver().openInputStream(uri));
                message = result.verified()
                        ? "Đã xuất TXT và đọc lại khớp (sha256 " + result.sha256().substring(0, 12) + "…, " + result.byteCount() + " byte)"
                        : "Xuất TXT thất bại: " + result.reasonCode() + ". Bản cuối đã lưu không bị thay đổi.";
            } catch (RuntimeException error) {
                message = "Xuất TXT thất bại: EXPORT_UNEXPECTED_ERROR. Bản cuối đã lưu không bị thay đổi.";
            }
            String shown = message;
            a.runOnUiThread(() -> a.toast(shown));
        });
    }

    private void showFinal(String chapterKey, EditorialL2Execution.Committed finalArtifact) {
        String text = new String(finalArtifact.viL2Bytes(), StandardCharsets.UTF_8);
        TextView body = a.text(text, 13, a.TEXT, false);
        body.setSingleLine(false);
        body.setTextIsSelectable(true);
        int pad = a.dp(16);
        body.setPadding(pad, pad, pad, pad);
        ScrollView scroll = new ScrollView(a);
        scroll.addView(body);
        String sha = finalArtifact.viL2Sha256();
        new AlertDialog.Builder(a).setTitle("Chương " + chapterKey + " • bản biên tập cuối")
                .setMessage("sha256 " + (sha.length() > 12 ? sha.substring(0, 12) + "…" : sha) + " • "
                        + finalArtifact.viL2Bytes().length + " byte")
                .setView(scroll).setPositiveButton("Đóng", null).show();
    }
}
