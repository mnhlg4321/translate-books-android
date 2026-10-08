package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;

import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;

import java.util.ArrayList;
import java.util.List;

/**
 * State and actions of the Biên tập tab (plan section 5): list of combos, one combo, the confirmation before anything is
 * sent, progress, and the result. The screens are drawn by {@link EditorialApiPageFactory}; every sentence comes from
 * {@link EditorialApiPresenter}. Runs execute on a worker thread and are stored after every step, so leaving the screen or
 * closing the app never loses or resends anything.
 */
final class EditorialApiUiController {
    enum Screen { LIST, COMBO, CONFIRM, PROGRESS, RESULT, PAIR_CONFIRM, PAIR_RUN, PAIR_RESULT }

    static final int REQ_RAW = 41;
    static final int REQ_DRAFT = 42;
    static final int REQ_EXPORT = 43;
    static final long REQUEST_TIMEOUT_MILLIS = 900_000L;

    /** Test hook: a provider that never reaches the network. */
    static volatile EditorialApiProvider providerOverride;

    private volatile MainActivity a;
    private final android.content.Context appContext;

    Screen screen = Screen.LIST;
    /** The combo being edited or run; a copy, saved with "Tiếp tục". */
    EditorialApiCombo combo = new EditorialApiCombo();
    EditorialApiSources sources;
    EditorialApiPresenter.Confirmation confirmation;
    String error = "";
    String progress = "";
    long runId;
    boolean showDiff;
    /** What the person typed on the combo screen and has not saved yet; kept while a file or reference is chosen. */
    static final class Form {
        final String name;
        final EditorialApiContract.Mode mode;
        final String model;
        final String cap;

        Form(String name, EditorialApiContract.Mode mode, String model, String cap) {
            this.name = name == null ? "" : name;
            this.mode = mode == null ? EditorialApiContract.Mode.QUICK : mode;
            this.model = model == null ? "" : model;
            this.cap = cap == null ? "" : cap;
        }
    }

    /** The last dialog shown, so a device test can choose an item the way a finger would. */
    volatile AlertDialog lastDialog;

    Form form;
    java.util.function.Supplier<Form> formReader;
    boolean technicalOpen;
    boolean legacyOpen;
    List<String> staleParts = new ArrayList<>();
    private long staleCheckedRun = -1L;
    private volatile EditorialApiRunService service;
    private Thread worker;

    /** The chunk-pair half of the tab (rows of a translation job). */
    final EditorialPairUiController pair = new EditorialPairUiController(this);

    EditorialApiUiController(MainActivity activity) {
        a = activity;
        appContext = activity.getApplicationContext();
    }

    void attach(MainActivity activity) { a = activity; }

    void detach(MainActivity activity) { if (a == activity) a = null; }

    android.content.Context appContext() { return appContext; }

    void postUi(Runnable action) { ui(action); }

    void setScreen(Screen next) { screen = next; refresh(); }

    void toast(String message) { MainActivity activity = a; if (activity != null) activity.toast(message); }

    void startActivityForResult(Intent intent, int code) { MainActivity activity = a; if (activity != null) activity.startActivityForResult(intent, code); }

    private void ui(Runnable action) {
        MainActivity activity = a;
        if (activity != null) activity.runOnUiThread(action);
    }

    // ---- navigation ----

    boolean running() { return worker != null && worker.isAlive(); }

    /** Back inside the tab goes one screen up; false = let the app handle it. */
    boolean handleBack() {
        switch (screen) {
            case LIST: return false;
            case PROGRESS: case PAIR_RUN: return true; // leaving needs the cancel button
            case CONFIRM: case PAIR_CONFIRM: screen = Screen.COMBO; break;
            default: screen = Screen.LIST;
        }
        error = "";
        refresh();
        return true;
    }

    void refresh() {
        MainActivity a = this.a;
        if (a == null) return;
        a.invalidatePage("Editorial");
        if ("Editorial".equals(a.currentTab)) a.switchTab("Editorial");
    }

    /** Keeps the combo form's unsaved values before an action rebuilds the screen. */
    void captureForm() {
        java.util.function.Supplier<Form> reader = formReader;
        if (screen == Screen.COMBO && reader != null) form = reader.get();
    }

    void showList() { screen = Screen.LIST; error = ""; form = null; formReader = null; refresh(); }

    void newCombo() {
        combo = new EditorialApiCombo();
        combo.sourceKind = EditorialPairModels.SOURCE_JOB;
        form = null;
        combo.settingsJson = new EditorialApiCombo.Settings().toJson();
        screen = Screen.COMBO;
        error = "";
        refresh();
    }

    void openCombo(long id) {
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(appContext)) {
            EditorialApiCombo stored = store.getCombo(id);
            if (stored == null) { a.toast("Không còn tổ hợp này"); showList(); return; }
            combo = stored;
            form = null;
        }
        screen = Screen.COMBO;
        error = "";
        refresh();
    }

    void duplicate(long id) {
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(appContext)) {
            EditorialApiCombo stored = store.getCombo(id);
            if (stored == null) return;
            combo = stored.copy();
            combo.id = 0;
            combo.name = stored.name + " (bản sao)";
            form = null;
        }
        screen = Screen.COMBO;
        error = "";
        refresh();
    }

    void confirmDelete(long id, String name) {
        lastDialog = new AlertDialog.Builder(a).setTitle("Xóa tổ hợp?")
                .setMessage("“" + name + "” và các kết quả đã lưu của nó sẽ bị xóa. File RAW, DRAFT và thư viện không bị đụng tới.")
                .setPositiveButton("Xóa", (d, w) -> {
                    try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(appContext)) { store.deleteCombo(id); }
                    showList();
                }).setNegativeButton("Giữ lại", null).show();
    }

    // ---- combo screen ----

    void pickFile(boolean raw) {
        captureForm();
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"text/plain", "application/octet-stream"});
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        a.startActivityForResult(i, raw ? REQ_RAW : REQ_DRAFT);
    }

    /** Lets the person take the file from a recent translation: its source as RAW, its output as DRAFT. */
    void pickRecent(boolean raw) {
        captureForm();
        List<TranslationRepository.JobSummary> jobs = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        try (TranslationRepository repository = new TranslationRepository(a)) {
            for (TranslationRepository.JobSummary job : repository.getJobSummaries(30)) {
                String uri = raw ? job.inputUri : job.outputUri;
                if (uri == null || uri.isEmpty()) continue;
                jobs.add(job);
                labels.add(job.fileName == null || job.fileName.isEmpty() ? "(không tên)" : job.fileName);
            }
        }
        if (jobs.isEmpty()) { a.toast("Chưa có bản dịch gần đây"); return; }
        lastDialog = new AlertDialog.Builder(a).setTitle(raw ? "RAW từ bản dịch gần đây" : "DRAFT từ bản dịch gần đây")
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    TranslationRepository.JobSummary job = jobs.get(which);
                    String uri = raw ? job.inputUri : job.outputUri;
                    setFile(raw, Uri.parse(uri), labels.get(which));
                    refresh();
                }).setNegativeButton("Đóng", null).show();
    }

    private void setFile(boolean raw, Uri uri, String name) {
        if (raw) { combo.rawUri = uri.toString(); combo.rawName = name; }
        else { combo.draftUri = uri.toString(); combo.draftName = name; }
    }

    /** Switches the combo to the rows of a translation job (the chunk-pair flow). */
    void pickJob() {
        captureForm();
        final android.content.Context context = appContext;
        new Thread(() -> {
            final List<EditorialPairSourceLoader.JobChoice> jobs = EditorialPairSourceLoader.listJobs(context);
            ui(() -> {
                if (jobs.isEmpty()) { toast("Chưa có job Dịch nào"); return; }
                String[] labels = new String[jobs.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = jobs.get(i).label();
                MainActivity activity = a;
                if (activity == null) return;
                lastDialog = new AlertDialog.Builder(activity).setTitle("Chọn job Dịch").setItems(labels, (d, which) -> {
                    EditorialPairSourceLoader.JobChoice job = jobs.get(which);
                    combo.sourceKind = EditorialPairModels.SOURCE_JOB;
                    combo.jobId = job.id;
                    combo.rawName = job.fileName.isEmpty() ? "Job " + job.id : job.fileName;
                    combo.draftName = "bản dịch của job " + job.id;
                    combo.rawUri = "";
                    combo.draftUri = "";
                    refresh();
                }).setNegativeButton("Đóng", null).show();
            });
        }, "editorial-pair-jobs").start();
    }

    /** Back to two independent files (the whole-chapter flow). */
    void useFiles() {
        captureForm();
        combo.sourceKind = EditorialPairModels.SOURCE_FILES;
        combo.jobId = 0;
        refresh();
    }

    /** Explicitly "no glossary": an empty choice is a valid configuration. */
    void clearGlossary() { captureForm(); combo.glossaryId = ""; refresh(); }

    void clearPronoun() { captureForm(); combo.pronounId = ""; refresh(); }

    void chooseGlossary() {
        captureForm();
        List<GlossaryStore.Glossary> all = GlossaryStore.loadAll(a);
        String[] items = new String[all.size() + 1];
        items[0] = "Không dùng";
        for (int i = 0; i < all.size(); i++) items[i + 1] = all.get(i).name + " (" + all.get(i).count() + " mục)";
        lastDialog = new AlertDialog.Builder(a).setTitle("Glossary").setItems(items, (d, which) -> {
            combo.glossaryId = which == 0 ? "" : all.get(which - 1).id;
            refresh();
        }).setNegativeButton("Đóng", null).show();
    }

    void choosePronoun() {
        captureForm();
        List<PronounStore.Profile> all = PronounStore.loadAll(a);
        String[] items = new String[all.size() + 1];
        items[0] = "Không dùng";
        for (int i = 0; i < all.size(); i++) items[i + 1] = all.get(i).name;
        lastDialog = new AlertDialog.Builder(a).setTitle("Pronoun").setItems(items, (d, which) -> {
            combo.pronounId = which == 0 ? "" : all.get(which - 1).id;
            refresh();
        }).setNegativeButton("Đóng", null).show();
    }

    String glossaryName() {
        if (combo.glossaryId.isEmpty()) return "";
        GlossaryStore.Glossary glossary = GlossaryStore.find(a, combo.glossaryId);
        return glossary == null ? "" : glossary.name;
    }

    String pronounName() {
        if (combo.pronounId.isEmpty()) return "";
        PronounStore.Profile profile = PronounStore.find(a, combo.pronounId);
        return profile == null ? "" : profile.name;
    }

    /** Stores the edited combo (name, mode, model, cap are passed from the fields) and opens the confirmation. */
    void saveAndContinue(String name, EditorialApiContract.Mode mode, String model, String capText) {
        if (!persist(name, mode, model, capText)) return;
        if (EditorialApiCombo.sourceIsJob(combo)) pair.openConfirm(); else prepareConfirmation();
    }

    /** Saves the combo as it stands without reading any source and without sending anything. */
    void saveOnly(String name, EditorialApiContract.Mode mode, String model, String capText) {
        if (!persist(name, mode, model, capText)) return;
        ui(() -> { MainActivity activity = a; if (activity != null) activity.toast("Đã lưu tổ hợp"); });
        showList();
    }

    /** Returns false (the message is in {@code error}) when the form is not complete; the typed values are kept. */
    private boolean persist(String name, EditorialApiContract.Mode mode, String model, String capText) {
        form = new Form(name, mode, model, capText);
        String missing = EditorialApiPresenter.missingForConfirmation(combo);
        if (!missing.isEmpty()) { error = missing; refresh(); return false; }
        EditorialApiCombo.Settings settings = new EditorialApiCombo.Settings();
        settings.mode = mode;
        settings.model = model == null ? "" : model.trim();
        try {
            java.math.BigDecimal cap = new java.math.BigDecimal(capText == null ? "" : capText.trim().replace(',', '.'));
            if (cap.signum() <= 0) throw new NumberFormatException();
            settings.maxUsdPerChapter = cap;
        } catch (NumberFormatException invalid) {
            error = "Trần chi phí phải là một số lớn hơn 0, ví dụ 0.10.";
            refresh();
            return false;
        }
        combo.settingsJson = settings.toJson();
        combo.name = name == null || name.trim().isEmpty()
                ? EditorialApiPresenter.suggestedName(combo, glossaryName(), pronounName()) : name.trim();
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(appContext)) {
            if (combo.id > 0) store.updateCombo(combo); else store.insertCombo(combo);
        }
        error = "";
        form = null;
        return true;
    }

    // ---- confirmation ----

    /** Reads the sources off the UI thread, then shows what is about to be sent. */
    void prepareConfirmation() {
        final EditorialApiCombo snapshot = combo.copy();
        final AppSettings settings = SettingsStore.load(appContext);
        new Thread(() -> {
            try {
                EditorialApiSources loaded = EditorialApiSourceLoader.load(appContext, snapshot, settings.targetLanguage);
                String model = effectiveModel(snapshot, settings);
                EditorialApiModelPricing pricing = new EditorialApiModelPricing(settings.provider);
                EditorialApiPresenter.Confirmation view = EditorialApiPresenter.confirmation(snapshot, loaded, glossaryName(), pronounName(),
                        model, pricing.inputPerToken(model), pricing.outputPerToken(model), pricing.known(model));
                ui(() -> { sources = loaded; confirmation = view; screen = Screen.CONFIRM; error = ""; refresh(); });
            } catch (EditorialApiSourceLoader.SourceException unreadable) {
                ui(() -> { screen = Screen.COMBO; error = unreadable.getMessage(); refresh(); });
            }
        }, "editorial-api-sources").start();
    }

    static String effectiveModel(EditorialApiCombo combo, AppSettings settings) {
        String own = combo.settings().model;
        String model = own.isEmpty() ? settings.model : own;
        return model == null ? "" : model.trim();
    }

    // ---- run ----

    void startRun() {
        if (confirmation == null || sources == null || !confirmation.canRun() || running()) return;
        final AppSettings settings = SettingsStore.load(appContext);
        if (providerOverride == null && (settings.apiKey == null || settings.apiKey.trim().isEmpty())) {
            error = "Chưa có khóa API. Vào Cài đặt để nhập khóa.";
            refresh();
            return;
        }
        final EditorialApiCombo.Settings chosen = combo.settings();
        final String model = effectiveModel(combo, settings);
        final EditorialApiCombo comboCopy = combo.copy();
        final EditorialApiSources runSources = sources;
        final EditorialApiProvider provider = providerOverride != null ? providerOverride : new OpenRouterEditorialApiProvider(settings);
        screen = Screen.PROGRESS;
        progress = "Đang chuẩn bị…";
        error = "";
        staleParts = new ArrayList<>();
        staleCheckedRun = -1L;
        showDiff = false;
        refresh();
        worker = new Thread(() -> {
            SqliteEditorialApiStore store = new SqliteEditorialApiStore(appContext);
            try {
                EditorialApiRunService runService = new EditorialApiRunService(store, provider,
                        new EditorialApiModelPricing(settings.provider), REQUEST_TIMEOUT_MILLIS);
                service = runService;
                EditorialApiRun run = runService.prepare(comboCopy, runSources, model, chosen.mode);
                runId = run.id;
                runService.execute(run.id, chosen.maxUsdPerChapter, settings.targetLanguage, new EditorialApiRunService.Listener() {
                    @Override public void onStep(EditorialApiRun current, EditorialApiContract.Step step, int attempt) {
                        progress = EditorialApiPresenter.progressLine(step, attempt) + "\n" + EditorialApiPresenter.spentLine(current);
                        ui(() -> { if (screen == Screen.PROGRESS) refresh(); });
                    }

                    @Override public void onFinished(EditorialApiRun finished) { }
                });
            } catch (RuntimeException failure) {
                android.util.Log.w("EditorialApi", "run failed: " + failure);
                error = "Không chạy được: " + failure.getClass().getSimpleName();
            } finally {
                store.close();
                service = null;
                ui(() -> { screen = Screen.RESULT; refresh(); });
            }
        }, "editorial-api-run");
        worker.start();
    }

    void cancelRun() {
        EditorialApiRunService active = service;
        if (active != null) active.cancel();
        progress = "Đang hủy…";
    }

    // ---- result ----

    void openResult(long comboId) {
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(appContext)) {
            EditorialApiCombo stored = store.getCombo(comboId);
            if (stored != null && EditorialApiCombo.sourceIsJob(stored)) { combo = stored; pair.openResult(comboId); return; }
            EditorialApiRun latest = store.latestRun(comboId);
            if (stored == null || latest == null) { a.toast("Chưa có kết quả"); return; }
            combo = stored;
            runId = latest.id;
        }
        screen = Screen.RESULT;
        showDiff = false;
        error = "";
        staleParts = new ArrayList<>();
        staleCheckedRun = -1L;
        refresh();
    }

    void toggleDiff() { showDiff = !showDiff; refresh(); }

    void toggleTechnical() { technicalOpen = !technicalOpen; refresh(); }

    void toggleLegacy() { legacyOpen = !legacyOpen; refresh(); }

    void runFromList(long comboId) {
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(appContext)) {
            EditorialApiCombo stored = store.getCombo(comboId);
            if (stored == null) { a.toast("Không còn tổ hợp này"); return; }
            combo = stored;
        }
        error = "";
        if (EditorialApiCombo.sourceIsJob(combo)) pair.openConfirm(); else prepareConfirmation();
    }

    /** Re-run of the open result: the same combo, sources read again from disk and the library. */
    void rerun() {
        error = "";
        prepareConfirmation();
    }

    /** Looks (off the UI thread, once per run) whether the sources changed since the stored run; shows the question if so. */
    void checkStale(EditorialApiRun run) {
        if (run == null || staleCheckedRun == run.id) return;
        staleCheckedRun = run.id;
        staleParts = new ArrayList<>();
        final EditorialApiCombo snapshot = combo.copy();
        final String language = SettingsStore.load(appContext).targetLanguage;
        new Thread(() -> {
            try {
                List<String> changed = EditorialApiSources.changedParts(run, EditorialApiSourceLoader.load(appContext, snapshot, language));
                if (!changed.isEmpty()) ui(() -> { staleParts = changed; if (screen == Screen.RESULT && runId == run.id) refresh(); });
            } catch (EditorialApiSourceLoader.SourceException unreadable) {
                List<String> missing = List.of(unreadable.which);
                ui(() -> { staleParts = missing; if (screen == Screen.RESULT && runId == run.id) refresh(); });
            }
        }, "editorial-api-stale").start();
    }

    void exportFinal() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TITLE, EditorialApiExport.suggestedFileName(combo));
        a.startActivityForResult(i, REQ_EXPORT);
    }

    /** @return true when the request belongs to this tab */
    boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (pair.onActivityResult(requestCode, resultCode, data)) return true;
        if (requestCode != REQ_RAW && requestCode != REQ_DRAFT && requestCode != REQ_EXPORT) return false;
        if (resultCode != android.app.Activity.RESULT_OK || data == null || data.getData() == null) return true;
        Uri uri = data.getData();
        if (requestCode == REQ_EXPORT) {
            final long exportRunId = runId;
            new Thread(() -> {
                String message;
                try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(appContext)) {
                    String hash = EditorialApiExport.exportFinal(appContext, uri, store.getRun(exportRunId));
                    message = "Đã xuất TXT và đọc lại khớp (" + hash.substring(0, 12) + ")";
                } catch (Exception failure) {
                    message = "Không xuất được TXT. Hãy chọn nơi lưu khác.";
                }
                final String shown = message;
                ui(() -> a.toast(shown));
            }, "editorial-api-export").start();
            return true;
        }
        int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        FileUtil.takePersistable(a, uri, takeFlags, true, false);
        setFile(requestCode == REQ_RAW, uri, FileUtil.displayName(a, uri));
        refresh();
        return true;
    }
}
