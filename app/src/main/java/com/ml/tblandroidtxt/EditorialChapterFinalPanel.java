package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Chapter card section for a P4-bound Editorial chapter: durable progress (L1 -> L2 -> L3 -> FINAL),
 * typed stop/recovery wording, a viewer for the stored FINAL and a verified TXT export. It reads through
 * {@link EditorialChapterFinalCoordinator#inspect} off the UI thread. The run button opens an authorization
 * dialog that shows the exact per-phase caps; only "allow" reaches {@link EditorialChapterRunService}, which
 * refuses anything but a never-attempted stage. A stage left unknown or in recovery is shown and never retried.
 */
final class EditorialChapterFinalPanel {
    private static final ExecutorService RUN_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "editorial-chain-run");
        thread.setDaemon(false);
        return thread;
    });

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
        refresh(box, status, actions, project, chapter);
        return box;
    }

    private void refresh(LinearLayout box, TextView status, LinearLayout actions, EditorialRepository.Project project,
                         EditorialRepository.Chapter chapter) {
        a.preflightExecutor.submit(() -> {
            EditorialChapterFinalCoordinator.Inspection inspection = inspect(project.id, project.bindingIdentity, chapter.chapterKey);
            a.runOnUiThread(() -> {
                if (a.isFinishing() || a.isDestroyed()) return;
                render(box, status, actions, project, chapter, inspection);
            });
        });
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
        String lockKey = EditorialChapterRunService.lockKey(project.id, chapter.chapterKey);
        boolean running = EditorialChapterRunService.isRunning(lockKey);
        status.setText(running ? EditorialChapterProgress.describeRunning(progress)
                : EditorialChapterProgress.describe(progress));
        if (running) {
            // Poll the durable rows from the UI thread's timer; the single-thread preflight executor is never blocked.
            box.postDelayed(() -> { if (!a.isFinishing() && !a.isDestroyed()) refresh(box, status, actions, project, chapter); },
                    4_000L);
        }
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
        } else if (!running && (progress.next() == EditorialChapterProgress.NextAction.RUN_STAGE_WITH_AUTHORIZATION
                || progress.next() == EditorialChapterProgress.NextAction.L1_REQUIRED)) {
            actions.addView(a.primaryButton("Chạy L1 → L2 → L3 (cần cấp phép)",
                    v -> confirmRun(box, status, actions, project, chapter)),
                    new LinearLayout.LayoutParams(-1, a.dp(44)));
        }
    }

    private void confirmRun(LinearLayout box, TextView status, LinearLayout actions, EditorialRepository.Project project,
                            EditorialRepository.Chapter chapter) {
        EditorialChainBudgets budgets = EditorialChainBudgets.fullLedgerRecommended();
        String message = "Chương " + chapter.chapterKey + " sẽ được gửi tới OpenRouter (" + EditorialP5EFreshRawRoutingPolicy.MODEL
                + ") trong tối đa 8 call theo thứ tự L1 → L2 → L3. L1_RAW_DISCOVERY chỉ thấy RAW + GLOSSARY; "
                + "L1_RECONCILE thấy RAW + DRAFT + GLOSSARY + PRONOUN và candidates của L1_RAW_DISCOVERY. "
                + "L2/L3 tiếp tục theo ranh giới nguồn của từng phase, gồm final-read trên đúng byte đã dựng.\n\n"
                + budgets.describe() + "\n\nTrần toàn chuỗi: USD 0.50. Không repair, không retry; trạng thái UNKNOWN sẽ dừng và không gọi lại. "
                + "Cấp phép này chỉ dùng cho chương đang mở.";
        TextView body = a.text(message, 13, a.TEXT, false);
        body.setSingleLine(false);
        int pad = a.dp(20);
        body.setPadding(pad, pad / 2, pad, 0);
        ScrollView scroll = new ScrollView(a);
        scroll.addView(body);
        new AlertDialog.Builder(a).setTitle("Cấp phép gọi nhà cung cấp").setView(scroll)
                .setPositiveButton("Cho phép chạy một lần", (d, w) -> startRun(box, status, actions, project, chapter, budgets))
                .setNegativeButton("Hủy", null).show();
    }

    private void startRun(LinearLayout box, TextView status, LinearLayout actions, EditorialRepository.Project project,
                          EditorialRepository.Chapter chapter, EditorialChainBudgets budgets) {
        String lockKey = EditorialChapterRunService.lockKey(project.id, chapter.chapterKey);
        EditorialChapterRunService.Chain chain = new AppChain(project, chapter.chapterKey);
        actions.removeAllViews();
        status.setText("Đang bắt đầu…");
        // A chain takes minutes: it gets its own thread, never the app's single-thread preflight executor.
        RUN_EXECUTOR.submit(() -> {
            EditorialChapterRunService.Outcome outcome = EditorialChapterRunService.run(lockKey, chain, budgets, true);
            a.runOnUiThread(() -> {
                if (a.isFinishing() || a.isDestroyed()) return;
                a.toast(outcome.started() ? "Kết quả chạy: " + outcome.reasonCode() : "Không chạy: " + outcome.reasonCode());
                refresh(box, status, actions, project, chapter);
            });
        });
        // Show the running state (and start polling) once the worker has had time to take the chapter lock.
        box.postDelayed(() -> { if (!a.isFinishing() && !a.isDestroyed()) refresh(box, status, actions, project, chapter); },
                800L);
    }

    /** The real chain: durable coordinator plus the OpenRouter adapters built from the saved settings. */
    private final class AppChain implements EditorialChapterRunService.Chain {
        private final EditorialRepository.Project project;
        private final String chapterKey;

        AppChain(EditorialRepository.Project project, String chapterKey) {
            this.project = project;
            this.chapterKey = chapterKey;
        }

        @Override public EditorialChapterFinalCoordinator.Inspection inspect() {
            return EditorialChapterFinalPanel.this.inspect(project.id, project.bindingIdentity, chapterKey);
        }

        @Override public String preflightIssue() {
            AppSettings settings = SettingsStore.load(a);
            if (!EditorialP5EFreshRawRoutingPolicy.matches(settings)) return "P6_ROUTE_SETTINGS_MISMATCH";
            if (settings.apiKey == null || settings.apiKey.trim().isEmpty()) return "OPENROUTER_CONFIGURATION_INCOMPLETE";
            return null;
        }

        @Override public EditorialChapterFinalCoordinator.Result run(EditorialChainBudgets budgets) {
            AppSettings settings = SettingsStore.load(a);
            try (TranslationRepository database = new TranslationRepository(a)) {
                Optional<String> selector = new EditorialP4BindingDao(database).selectorFor(project.bindingIdentity);
                if (selector.isEmpty()) {
                    return new EditorialChapterFinalCoordinator.Result(EditorialChapterFinalCoordinator.Stage.L1_INCOMPLETE,
                            false, "INPUT_BINDING_SELECTOR_MISSING", null, 0);
                }
                EditorialP4Binding binding = new EditorialP4BindingDao(database)
                        .findByAttemptRequestSelector(selector.get()).orElse(null);
                if (binding == null) {
                    return new EditorialChapterFinalCoordinator.Result(EditorialChapterFinalCoordinator.Stage.L1_INCOMPLETE,
                            false, "INPUT_BINDING_NOT_FOUND", null, 0);
                }
                String keyFingerprint = EditorialCanonicalJson.sha256Hex(
                        settings.apiKey.getBytes(StandardCharsets.UTF_8));
                String endpointAccountFingerprint = EditorialCanonicalJson.sha256Hex(
                        (AppSettings.normalizeEndpoint(settings.baseUrl) + "|" + keyFingerprint)
                                .getBytes(StandardCharsets.UTF_8));
                EditorialP6L1Authorizations.Pair l1Authorization = EditorialP6L1Authorizations.create(
                        binding, chapterKey, endpointAccountFingerprint, budgets, System.currentTimeMillis());
                EditorialP5PilotProvider l1Provider = new OpenRouterEditorialP6L1Provider(
                        OpenRouterEditorialP5PilotProvider.withFreshRawLifecyclePersistence(
                                settings, budgets.l1Raw().maximumOutputTokens(), database),
                        OpenRouterEditorialP5PilotProvider.withFreshReconcileLifecyclePersistence(
                                settings, budgets.l1Reconcile().maximumOutputTokens(), database));
                String groupId = EditorialCanonicalJson.sha256Hex((project.bindingIdentity + "|" + chapterKey + "|"
                        + binding.runDeclarationIdentity() + "|P6-GROUP-V1").getBytes(StandardCharsets.UTF_8));
                Path spendPath = a.getFilesDir().toPath().resolve("evidence").resolve("p6-spend-ledger")
                        .resolve(groupId + ".jsonl");
                EditorialP6GroupSpendLedger spendLedger = new EditorialP6GroupSpendLedger(
                        spendPath, groupId, budgets.chainMaximumCost());
                return new EditorialChapterFinalCoordinator(database, new EditorialPackStorageLayout(a.getFilesDir().toPath()))
                        .runFromL1(project.id, selector.get(), chapterKey, budgets,
                                l1Authorization.raw(), l1Authorization.reconcile(),
                                new EditorialP6BudgetedL1Provider(l1Provider, spendLedger, budgets),
                                new EditorialP6BudgetedPhaseProvider(new OpenRouterEditorialL2Provider(settings), spendLedger, budgets, true),
                                new EditorialP6BudgetedPhaseProvider(new OpenRouterEditorialL3Provider(settings), spendLedger, budgets, false));
            }
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
        String sha = finalArtifact.viL2Sha256();
        TextView header = a.text("sha256 " + (sha.length() > 12 ? sha.substring(0, 12) + "…" : sha) + " • "
                + finalArtifact.viL2Bytes().length + " byte", 12, a.MUTED, false);
        header.setPadding(pad, pad / 2, pad, 0);
        // A long chapter must not push the dialog buttons off screen: the text gets a bounded scroll area.
        ScrollView scroll = new ScrollView(a);
        scroll.addView(body);
        int maxHeight = (int) (a.getResources().getDisplayMetrics().heightPixels * 0.55f);
        LinearLayout content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(header);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, maxHeight));
        new AlertDialog.Builder(a).setTitle("Chương " + chapterKey + " • bản biên tập cuối")
                .setView(content).setPositiveButton("Đóng", null).show();
    }
}
