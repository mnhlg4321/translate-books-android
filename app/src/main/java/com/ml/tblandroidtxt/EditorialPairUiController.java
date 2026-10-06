package com.ml.tblandroidtxt;

import android.content.Intent;
import android.net.Uri;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The chunk-pair half of the Biên tập tab: preview of the pairs, the run with its progress and cancel, the result with the
 * per-pair decisions, and export. It shares the screen state, the combo and the refresh of {@link EditorialApiUiController}.
 * Runs execute on a worker thread against the stored snapshot; leaving the screen or closing the app loses nothing and
 * resends nothing (the journal decides on the next open).
 */
final class EditorialPairUiController {
    static final int REQ_EXPORT = 44;
    static final long REQUEST_TIMEOUT_MILLIS = 900_000L;

    private final EditorialApiUiController parent;

    EditorialPairPreview preview;
    EditorialPairSource source;
    EditorialPairSourceLoader.References references;
    String sourceError = "";
    long runId;
    String progress = "";
    boolean detailsOpen;
    final Set<String> expanded = new HashSet<>();
    private volatile EditorialPairRunService service;
    private Thread worker;

    EditorialPairUiController(EditorialApiUiController parent) { this.parent = parent; }

    boolean running() { return worker != null && worker.isAlive(); }

    // ---- preview

    /** Reads the job and the references off the UI thread, then shows the pairs and why the run can or cannot start. */
    void openConfirm() {
        final EditorialApiCombo snapshot = parent.combo.copy();
        new Thread(() -> {
            try {
                EditorialPairSourceLoader.References refs = EditorialPairSourceLoader.loadReferences(parent.appContext(), snapshot);
                EditorialPairSource loaded = EditorialApiCombo.sourceIsJob(snapshot)
                        ? EditorialPairSourceLoader.loadJob(parent.appContext(), snapshot.jobId)
                        : EditorialPairSource.unmappedFiles(snapshot.rawName + " + " + snapshot.draftName);
                EditorialPairPreview view = EditorialPairPreview.of(loaded, EditorialPairSnapshot.glossaryFrom(refs.glossaryText), refs.pronounText, null);
                parent.postUi(() -> {
                    source = loaded;
                    references = refs;
                    preview = view;
                    sourceError = "";
                    parent.setScreen(EditorialApiUiController.Screen.PAIR_CONFIRM);
                });
            } catch (EditorialApiSourceLoader.SourceException unreadable) {
                parent.postUi(() -> { sourceError = unreadable.getMessage(); parent.setScreen(EditorialApiUiController.Screen.COMBO); parent.error = sourceError; parent.refresh(); });
            }
        }, "editorial-pair-preview").start();
    }

    // ---- run

    void startRun() {
        if (preview == null || source == null || !preview.runnable() || running()) return;
        final AppSettings settings = SettingsStore.load(parent.appContext());
        if (EditorialApiUiController.providerOverride == null && (settings.apiKey == null || settings.apiKey.trim().isEmpty())) {
            parent.error = "Chưa có khóa API. Vào Cài đặt để nhập khóa.";
            parent.refresh();
            return;
        }
        final EditorialApiCombo combo = parent.combo.copy();
        final String model = EditorialApiUiController.effectiveModel(combo, settings);
        if (model.isEmpty()) { parent.error = "Chưa chọn model. Vào Cài đặt để chọn model."; parent.refresh(); return; }
        final BigDecimal cap = combo.settings().maxUsdPerChapter;
        final EditorialPairSource runSource = source;
        final EditorialPairSourceLoader.References refs = references;
        final EditorialApiProvider provider = EditorialApiUiController.providerOverride != null
                ? EditorialApiUiController.providerOverride : new OpenRouterEditorialApiProvider(settings);
        progress = "Đang chuẩn bị…";
        parent.error = "";
        parent.setScreen(EditorialApiUiController.Screen.PAIR_RUN);
        launch(null, combo, runSource, refs, provider, settings, model, cap);
    }

    /** Runs the pairs that were not sent yet; the pairs already received or unknown are left exactly as they are. */
    void resumeRun() {
        if (running() || runId <= 0) return;
        final AppSettings settings = SettingsStore.load(parent.appContext());
        final EditorialApiProvider provider = EditorialApiUiController.providerOverride != null
                ? EditorialApiUiController.providerOverride : new OpenRouterEditorialApiProvider(settings);
        progress = "Đang chạy tiếp…";
        parent.setScreen(EditorialApiUiController.Screen.PAIR_RUN);
        launch(runId, null, null, null, provider, settings, null, null);
    }

    private void launch(final Long existingRunId, final EditorialApiCombo combo, final EditorialPairSource runSource,
                        final EditorialPairSourceLoader.References refs, final EditorialApiProvider provider, final AppSettings settings,
                        final String model, final BigDecimal cap) {
        worker = new Thread(() -> {
            SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(parent.appContext());
            try {
                EditorialPairRunService runService = new EditorialPairRunService(store, provider, new EditorialApiModelPricing(settings.provider), REQUEST_TIMEOUT_MILLIS);
                service = runService;
                long id;
                if (existingRunId != null) {
                    id = existingRunId;
                } else {
                    PairRun run = runService.prepare(combo.id, runSource, refs.glossaryText, refs.pronounText, null, model, settings.targetLanguage, cap,
                            EditorialPairModels.ARM_CHUNK);
                    id = run.id;
                }
                runId = id;
                runService.execute(id, new EditorialPairRunService.Listener() {
                    @Override public void onPair(PairRun run, PairItem item) {
                        progress = "Cặp " + item.ordinal + ": đang gửi và chờ kết quả…";
                        parent.postUi(() -> { if (parent.screen == EditorialApiUiController.Screen.PAIR_RUN) parent.refresh(); });
                    }

                    @Override public void onFinished(PairRun run) { }
                });
            } catch (EditorialPairRunService.PrepareException blocked) {
                parent.error = "Chưa chạy được: " + EditorialPairPresenter.blocker(blocked.blockers.get(0));
            } catch (RuntimeException failure) {
                android.util.Log.w("EditorialPair", "run failed: " + failure);
                parent.error = "Không chạy được: " + failure.getClass().getSimpleName();
            } finally {
                store.close();
                service = null;
                parent.postUi(() -> { parent.setScreen(runId > 0 ? EditorialApiUiController.Screen.PAIR_RESULT : EditorialApiUiController.Screen.PAIR_CONFIRM); });
            }
        }, "editorial-pair-run");
        worker.start();
    }

    void cancelRun() {
        EditorialPairRunService active = service;
        if (active != null) active.cancel();
        progress = "Đang dừng sau cặp hiện tại…";
    }

    // ---- result

    void openResult(long comboId) {
        try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(parent.appContext())) {
            PairRun latest = store.latestRun(comboId);
            if (latest == null) { parent.toast("Chưa có kết quả"); return; }
            runId = latest.id;
            EditorialPairRunService reader = new EditorialPairRunService(store, new FailingProvider(), null, 1L);
            if (reader.needsRecovery(runId)) reader.recover(runId); // reads the journal, never sends
        }
        expanded.clear();
        parent.error = "";
        parent.setScreen(EditorialApiUiController.Screen.PAIR_RESULT);
    }

    void toggle(String pairId) {
        if (!expanded.remove(pairId)) expanded.add(pairId);
        parent.refresh();
    }

    void toggleDetails() { detailsOpen = !detailsOpen; parent.refresh(); }

    void resolve(String pairId, boolean accept) {
        try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(parent.appContext())) {
            new EditorialPairRunService(store, new FailingProvider(), null, 1L).resolveWarning(runId, pairId, accept);
        }
        parent.refresh();
    }

    // ---- export

    void exportNow() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/plain");
        String name = "editorial.txt";
        try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(parent.appContext())) {
            EditorialPairRunService.ExportPlan plan = new EditorialPairRunService(store, new FailingProvider(), null, 1L).exportPlan(runId);
            if (plan != null) name = EditorialPairPresenter.exportName(parent.combo.name, plan);
        }
        i.putExtra(Intent.EXTRA_TITLE, name);
        parent.startActivityForResult(i, REQ_EXPORT);
    }

    boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_EXPORT) return false;
        if (resultCode != android.app.Activity.RESULT_OK || data == null || data.getData() == null) return true;
        final Uri uri = data.getData();
        final long exportRunId = runId;
        new Thread(() -> {
            String message;
            try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(parent.appContext())) {
                EditorialPairRunService.ExportPlan plan = new EditorialPairRunService(store, new FailingProvider(), null, 1L).exportPlan(exportRunId);
                if (plan == null || plan.text.isEmpty()) throw new IllegalStateException("NOTHING_TO_EXPORT");
                FileUtil.writeTextVerified(parent.appContext(), uri, plan.text);
                String back = HashUtil.sha256(FileUtil.readText(parent.appContext(), uri));
                if (!back.equals(HashUtil.sha256(plan.text))) throw new IllegalStateException("EXPORT_READBACK_MISMATCH");
                message = "Đã xuất và đọc lại khớp. " + plan.label;
            } catch (Exception failure) {
                message = "Không xuất được. Hãy chọn nơi lưu khác.";
            }
            final String shown = message;
            parent.postUi(() -> parent.toast(shown));
        }, "editorial-pair-export").start();
        return true;
    }

    /** A provider for the screens that only read: if anything ever tried to send, this fails loudly. */
    private static final class FailingProvider implements EditorialApiProvider {
        @Override public com.ml.tblandroidtxt.editorial.api.EditorialApiFlow.StepResponse call(
                com.ml.tblandroidtxt.editorial.api.EditorialApiFlow.Request request, String model, int maxOutputTokens, long timeoutMillis) {
            throw new IllegalStateException("A read-only screen must never send a request");
        }

        @Override public void cancel() { }
    }
}
