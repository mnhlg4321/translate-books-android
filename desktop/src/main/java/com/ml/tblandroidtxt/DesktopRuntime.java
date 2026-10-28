package com.ml.tblandroidtxt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;

/**
 * Wires the shared core to the PC: data folder, SQLite database, prefs files, DPAPI key, log, engine and runner. The JavaFX
 * layer only talks to this class. It lives in the core package because the repository constructor and the store helpers are
 * package-private there.
 */
public final class DesktopRuntime implements AutoCloseable {
    /** DesktopRuntime sits between the runner and the UI: it tracks "a run is active" and forwards every event. */
    public interface UiEvents extends RunListener { }

    public final Path dataDir;
    private final Path prefsDir;
    private final JdbcSqlDb sql;
    private final DesktopRepository repo;
    private final TranslationEngineCore engine;
    private final TranslationRunner runner;
    private final JsonFileKeyValueStore settingsPrefs;
    private final JsonFileKeyValueStore glossaryPrefs;
    private final JsonFileKeyValueStore pronounPrefs;
    private final DpapiSecrets secrets;
    private final DesktopRawStore raws;
    private final JsonFileKeyValueStore appearancePrefs;
    private final DesktopHistory history;
    private final FileRuntimeLog log;
    private final FileModelCatalogCache catalogCache;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "tbl-run");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile UiEvents ui;
    private final TranslationEngineCore.ProviderClient testProvider;
    private volatile int apiTestRequests;
    private volatile boolean requireConfirmation;
    private final java.util.concurrent.atomic.AtomicInteger confirmations = new java.util.concurrent.atomic.AtomicInteger();
    private volatile String recommendationUndoJson;
    private volatile String volumeSessionId;
    private volatile int volumeOrdinal;
    private volatile long volumeLastJob;
    private volatile long lastJobId;

    /** Default data folder: TBL_DATA_DIR when set (tests, screenshots), else %LOCALAPPDATA%\TranslateBooks. */
    public static Path defaultDataDir() {
        String override = System.getenv("TBL_DATA_DIR");
        if (override != null && !override.trim().isEmpty()) return Paths.get(override.trim());
        String local = System.getenv("LOCALAPPDATA");
        return Paths.get(local == null || local.isEmpty() ? System.getProperty("user.home") : local, "TranslateBooks");
    }

    public static DesktopRuntime open(Path dataDir, UiEvents ui) throws Exception {
        return new DesktopRuntime(dataDir, ui, null);
    }

    /** Test seam: a scripted provider instead of the network client. */
    public static DesktopRuntime open(Path dataDir, UiEvents ui, TranslationEngineCore.ProviderClient provider) throws Exception {
        return new DesktopRuntime(dataDir, ui, provider);
    }

    private DesktopRuntime(Path dataDir, UiEvents uiEvents, TranslationEngineCore.ProviderClient provider) throws Exception {
        this.dataDir = dataDir.toAbsolutePath();
        this.ui = uiEvents;
        this.testProvider = provider;
        this.prefsDir = this.dataDir.resolve("prefs");
        Files.createDirectories(this.dataDir.resolve("catalog"));
        Files.createDirectories(this.dataDir.resolve("logs"));
        Files.createDirectories(prefsDir);
        this.sql = DesktopSchema.open(this.dataDir.resolve("db").resolve("tbl_android_txt.db"));
        this.repo = new DesktopRepository(sql);
        this.settingsPrefs = new JsonFileKeyValueStore(prefsDir, "settings");
        this.glossaryPrefs = new JsonFileKeyValueStore(prefsDir, GlossaryStoreCore.PREF);
        this.pronounPrefs = new JsonFileKeyValueStore(prefsDir, PronounStoreCore.PREF);
        this.secrets = new DpapiSecrets(this.dataDir.resolve("secrets"));
        this.appearancePrefs = new JsonFileKeyValueStore(prefsDir, "appearance");
        this.history = new DesktopHistory(repo, this.dataDir.resolve(ChapterSession.DIR));
        this.raws = new DesktopRawStore(sql, this.dataDir.resolve(DesktopRawStore.DIR).toFile());
        this.log = new FileRuntimeLog(this.dataDir.resolve("logs"));
        this.catalogCache = new FileModelCatalogCache(this.dataDir.resolve("catalog").resolve("openrouter_model_catalog_v42.json").toFile());
        ObservabilityLog.install(log);
        loadCatalogFromCacheOnly();

        RunListener forwarding = new RunListener() {
            @Override public void onSnapshot(RunSnapshot snapshot) { attachVolumeJob(); if (snapshot.activeJobId > 0) lastJobId = snapshot.activeJobId; UiEvents t = ui; if (t != null) t.onSnapshot(snapshot); }
            @Override public void onNotification(String text, int done, int total) { UiEvents t = ui; if (t != null) t.onNotification(text, done, total); }
            @Override public void onRunEnded() { running.set(false); UiEvents t = ui; if (t != null) t.onRunEnded(); }
            @Override public void onCancelledEnd(String message) { running.set(false); UiEvents t = ui; if (t != null) t.onCancelledEnd(message); }
        };
        this.runner = new TranslationRunner(forwarding, new PathDocumentIO(null, raws), new JsonRunStatePersister(new JsonFileKeyValueStore(prefsDir, "runtime_state")),
                log, TraceSink.NONE, secrets, Clock.SYSTEM);
        this.engine = new TranslationEngineCore(repo, provider);
        runner.attach(repo, engine);
        engine.setSessionCostSupplier(runner.runCostSupplier());
    }

    public void setUi(UiEvents next) { this.ui = next; }

    /** Loads the cached model list only; the network is touched only when the person presses the update button. */
    private void loadCatalogFromCacheOnly() {
        try {
            String text = catalogCache.load();
            if (text == null || text.isEmpty()) return;
            JSONObject root = new JSONObject(text);
            long retrievedAt = root.optLong("retrieved_at", 0);
            List<ModelCatalog.ModelInfo> models = ModelCatalog.parseOpenRouterResponse(root.optString("response", "{}"), "openrouter-cache", retrievedAt);
            if (!models.isEmpty()) ModelCatalog.installForTests(models, ModelCatalog.LoadingState.IDLE);
        } catch (Exception e) {
            log.append("catalog cache unreadable: " + e.getClass().getSimpleName());
        }
    }

    /** User action only ("Cập nhật danh sách model"). */
    public void refreshCatalog() { ModelCatalog.refreshAsync(catalogCache); }

    public List<ModelCatalog.ModelInfo> models() { return ModelCatalog.availableModelInfos("openrouter"); }

    // ---- settings ----

    public AppSettings loadSettings() {
        AppSettings s = SettingsJson.fromJson(settingsPrefs.getString("settings_json", "{}"));
        s.apiKey = "";
        return s;
    }

    public void saveSettings(AppSettings s) throws IOException {
        if (!settingsPrefs.edit().putString("settings_json", SettingsJson.toPersistedJson(s)).commit()) throw new IOException("Could not save settings");
    }

    /** Stores one setting as the person typed it (the page saves on every edit). Returns null on success, else the reason; nothing is stored on a refusal. */
    public String applySetting(String key, String text) throws IOException {
        DesktopSettingsModel.Field f = DesktopSettingsModel.field(key);
        if (f == null) return "Mục cài đặt không có: " + key;
        AppSettings s = loadSettings();
        String problem = f.set(s, text);
        if (problem != null) return problem;
        saveSettings(s);
        return null;
    }

    /** "Cài đặt khuyến nghị": the settings that differ from the safe recommendation. */
    public List<String[]> recommendationDiffs() { return DesktopSettingsModel.recommendationDiffs(loadSettings()); }

    /** Applies the recommendation and remembers what it replaced, so {@link #undoRecommendation} can put it back. */
    public void applyRecommendation() throws IOException {
        AppSettings now = loadSettings();
        recommendationUndoJson = SettingsJson.toPersistedJson(now);
        saveSettings(DesktopSettingsModel.applyRecommendation(now));
    }

    public boolean canUndoRecommendation() { return recommendationUndoJson != null; }

    public void undoRecommendation() throws IOException {
        if (recommendationUndoJson == null) return;
        AppSettings back = SettingsJson.fromJson(recommendationUndoJson);
        back.apiKey = "";
        recommendationUndoJson = null;
        saveSettings(back);
    }

    /** "Không giới hạn": this app run's cost cap is dropped (kept in the settings file, not in the shared profile). */
    public boolean unlimitedCost() { return "true".equals(settingsPrefs.getString("unlimited_cost", "false")); }

    public void setUnlimitedCost(boolean on) { settingsPrefs.edit().putString("unlimited_cost", on ? "true" : "false").commit(); }

    /** Cap passed to a run: -1 (unlimited) when the person chose so, else null (the settings' own cap or the safe minimum). */
    private Double capOverride(Double capUsd) { return capUsd == null && unlimitedCost() ? Double.valueOf(-1.0) : capUsd; }

    /**
     * The window turns this on: every action that can send a request to the provider (start, resume, retry, "Kiểm tra API") then works only
     * right after {@link #confirmSpend()}, which the window calls when the person pressed the OK of the "sẽ gọi provider" dialog.
     */
    public void requireConfirmation(boolean on) { requireConfirmation = on; }

    /** Records that the person confirmed one provider-calling action; the next such action may go ahead, once. */
    public void confirmSpend() { confirmations.incrementAndGet(); }

    private boolean allowedToCallProvider() {
        if (!requireConfirmation) return true;
        while (true) {
            int n = confirmations.get();
            if (n <= 0) return false;
            if (confirmations.compareAndSet(n, n - 1)) return true;
        }
    }

    /**
     * "Kiểm tra API": one tiny request to the provider, so it spends a little. The page asks first; this method never runs by itself.
     * Returns a one-line result with no key and no text from the provider.
     */
    public String testApi() {
        if (!allowedToCallProvider()) return "Chưa xác nhận: không gửi request nào.";
        apiTestRequests++;
        try {
            AppSettings s = settingsForRun(loadSettings());
            PromptPair p = new PromptPair("You are a connection tester. Reply only OK.", "Reply only: OK");
            OpenAICompatibleClient.ChatResult r = testProvider != null ? testProvider.chat(s, p, 64, "api-test", null) : OpenAICompatibleClient.chatWithUsage(s, p);
            return "API OK · " + r.totalTokens + " token";
        } catch (Exception e) {
            return "API lỗi: " + AppValidator.readableError(e);
        }
    }

    /** Number of provider requests made by {@link #testApi} in this session (the tests count it). */
    public int apiTestRequests() { return apiTestRequests; }

    /** "Chẩn đoán model": what the local catalog knows about the chosen model. No request is made. */
    public String modelDiagnostics() {
        AppSettings s = loadSettings();
        ModelCatalog.ModelInfo m = ModelCatalog.findModelInfo(s.provider, s.model);
        StringBuilder sb = new StringBuilder("Provider: ").append(s.provider).append("\nModel: ").append(s.model);
        if (m == null) sb.append("\nCatalog chưa có model này (bấm \"Làm mới catalog\" để tải, cần mạng).");
        else sb.append("\nTìm thấy trong catalog: ").append(m.id);
        return sb.toString();
    }

    public DpapiSecrets secrets() { return secrets; }

    public String instructionPath() { return settingsPrefs.getString("instruction_path", ""); }

    public void setInstructionPath(String path) { settingsPrefs.edit().putString("instruction_path", path == null ? "" : path).commit(); }

    /** Settings for a run: stored settings + selected Glossary/Pronoun text + the instruction file + the API key. */
    public AppSettings settingsForRun(AppSettings base) throws Exception {
        AppSettings s = base.copy();
        s.glossaryText = GlossaryStoreCore.selectedPromptText(glossaryPrefs);
        s.pronounText = PronounStoreCore.selectedPromptText(pronounPrefs);
        String instruction = instructionPath();
        if (!instruction.isEmpty() && Files.exists(Paths.get(instruction))) {
            CustomInstructions ci = YamlInstructionParser.parse(Paths.get(instruction).getFileName().toString(),
                    TextDecoding.decode(Files.readAllBytes(Paths.get(instruction))).text);
            if (ci.hasTranslation()) s.translationInstructions = ci.translation;
            if (ci.hasRefinement()) s.refinementInstructions = ci.refinement;
        }
        s.apiKey = secrets.load();
        return AppValidator.normalize(s);
    }

    // ---- prompt preview, translated text, log ----

    /** The prompt of one chunk of a file, as the app would send it now. {@code index} is 0-based. */
    public DesktopPromptPreview.Result previewPrompt(Path raw, AppSettings base, int index, String previous) throws Exception {
        AppSettings s = settingsForRun(base);
        String text = TextDecoding.decode(Files.readAllBytes(raw)).text;
        String problem = AppValidator.validateSourceText(text, raw.getFileName().toString());
        if (problem != null) throw new IllegalArgumentException(problem);
        return DesktopPromptPreview.build(text, s, index, previous);
    }

    public int previewChunkCount(Path raw, AppSettings base) throws Exception {
        AppSettings s = settingsForRun(base);
        String text = TextDecoding.decode(Files.readAllBytes(raw)).text;
        String problem = AppValidator.validateSourceText(text, raw.getFileName().toString());
        if (problem != null) throw new IllegalArgumentException(problem);
        return DesktopPromptPreview.chunkCount(text, s);
    }

    /** The job of the most recent run in this session (0 when none ran yet). */
    public long lastJobId() { return lastJobId; }

    /** Chunks of a job with the source and the accepted translation, for the in-app reader. Never logged. */
    public List<TranslationRepositoryCore.ChunkRow> chunkRows(long jobId) { return repo.getChunkRows(jobId); }

    public String logText() { return log.readAll(); }

    public void exportLog(Path target) throws IOException { Files.write(target, log.readAll().getBytes(StandardCharsets.UTF_8)); }

    public boolean clearLog() { return log.clear(); }

    // ---- estimate ----

    public CostEstimator.Estimate estimate(Path raw, AppSettings base) throws Exception {
        AppSettings s = settingsForRun(base);
        String text = TextDecoding.decode(Files.readAllBytes(raw)).text;
        String problem = AppValidator.validateSourceText(text, raw.getFileName().toString());
        if (problem != null) throw new IllegalArgumentException(problem);
        return CostEstimator.estimatePreparedChunks(Chunker.chunkText(text, s), s);
    }

    // ---- run control ----

    public boolean isRunning() { return running.get(); }

    public TranslationRunner runner() { return runner; }

    public List<TranslationRepositoryCore.JobSummary> jobs() { return repo.getJobSummaries(200); }

    /** Spend recorded for a job, or NaN when the job has no usage rows. */
    public static double jobCost(TranslationRepositoryCore.JobSummary job) { return job.usage == null ? Double.NaN : job.usage.costUsd; }

    /** One run at a time. Returns false when a run is already active. */
    private boolean claim() { return running.compareAndSet(false, true); }

    public boolean startNew(Path input, Path output, AppSettings base, Double capUsd, CostEstimator.Estimate estimate) {
        if (!allowedToCallProvider()) return false;
        if (!claim()) return false;
        final Double cap = capOverride(capUsd);
        executor.submit(() -> runNew(input, output, base, cap, estimate));
        return true;
    }

    private void runNew(Path input, Path output, AppSettings base, Double capUsd, CostEstimator.Estimate estimate) {
        try {
            runner.beginRun(true);
            runner.beginPreviewRun();
            runner.resetMetrics("", 0);
            if (estimate != null) runner.setEstimated(estimate.totalTokensHigh, estimate.pricingAvailable && !Double.isNaN(estimate.costHigh) ? estimate.costHigh : 0);
            runner.setState(TranslationJobState.State.VALIDATING, "Validating");
            runner.publish("validating", 0, "Validating a fresh translation session");
            AppSettings s = settingsForRun(base);
            s = runner.applyEffectiveCostLimit(s, capUsd);
            String validation = AppValidator.validateForTranslation(s);
            if (validation != null) throw new IllegalArgumentException(validation);
            runner.setState(TranslationJobState.State.READY, "Ready");
            runner.publish("ready", 0, "Input, configuration and output access are valid");
            runner.setState(TranslationJobState.State.RUNNING, "Running");
            runner.publish("running", 0, "Batch of 1 file(s) initiated.");
            runner.setMetricPhase("Translating");
            runner.publish("running", 0, "Starting translation for: " + input.getFileName());
            runner.startNewJob(PathDocumentIO.refOf(input), PathDocumentIO.refOf(output), s, 1, 1);
            if (runner.isCancelled()) {
                finishCancelled("Đã hủy dịch");
                return;
            }
            runner.setState(TranslationJobState.State.COMPLETED, "Completed");
            runner.publish("done", 100, "Translation completed: " + input.getFileName());
            endRun();
        } catch (TranslationRunner.CostLimitReachedException e) {
            runner.setState(TranslationJobState.State.PAUSED, "Paused");
            if (runner.getActiveJobId() > 0) repo.touchJob(runner.getActiveJobId(), "paused");
            runner.publish("paused", -1, AppValidator.readableError(e) + runner.chapterSummarySuffix());
            endRun();
        } catch (Exception e) {
            if (runner.isCancelled()) {
                finishCancelled("Đã hủy dịch");
                return;
            }
            runner.setState(TranslationJobState.State.FAILED, runner.getActiveJobId() > 0 ? "Translation failed" : "Initialization failure");
            if (runner.getActiveJobId() > 0) repo.touchJob(runner.getActiveJobId(), "error");
            runner.logFailure(runner.getMetricPhase(), e);
            runner.publish("error", -1, (runner.getActiveJobId() > 0 ? "Lỗi: " : "Lỗi khởi tạo: ") + AppValidator.readableError(e)
                    + runner.remainingSuffix() + runner.chapterSummarySuffix());
            endRun();
        }
    }

    private void finishCancelled(String message) {
        runner.setMetricPhase("Cancelled");
        if (runner.getActiveJobId() > 0) repo.touchJob(runner.getActiveJobId(), "cancelled");
        runner.setState(TranslationJobState.State.CANCELLED, "Cancelled");
        runner.publish("cancelled", -1, message);
        UiEvents t = ui;
        running.set(false);
        if (t != null) t.onCancelledEnd(message);
    }

    private void endRun() {
        UiEvents t = ui;
        running.set(false);
        if (t != null) t.onRunEnded();
    }

    /** Takes a job off the "Đang làm" list; nothing is deleted (chunks, tokens and cost stay in the history). */
    public void dismissJob(long jobId) { repo.dismissJob(jobId); }

    /** The newest job that can be continued (the "Tiếp tục gần nhất" button), or null. */
    public TranslationRepositoryCore.JobSummary latestResumable() {
        for (TranslationRepositoryCore.JobSummary j : jobs()) if (DesktopHistory.canResume(j) && DesktopHistory.isUnfinished(j)) return j;
        return null;
    }

    public boolean resumeJob(long jobId) {
        if (!allowedToCallProvider()) return false;
        if (!claim()) return false;
        executor.submit(() -> {
            try {
                runner.beginRun(true);
                runner.resetMetrics("", 0);
                runner.setState(TranslationJobState.State.VALIDATING, "Validating resume");
                runner.resumeJobById(jobId);
            } finally {
                running.set(false);
            }
        });
        return true;
    }

    public boolean retryFailed(long jobId) {
        if (!allowedToCallProvider()) return false;
        if (!claim()) return false;
        executor.submit(() -> {
            try {
                runner.beginRun(true);
                runner.resetMetrics("", 0);
                runner.setState(TranslationJobState.State.VALIDATING, "Validating retry");
                runner.retryFailedChunksForJob(jobId);
            } finally {
                running.set(false);
            }
        });
        return true;
    }

    public void pause() { if (running.get()) runner.pause(); }

    public void resume() { if (running.get()) runner.resume(); }

    public void cancel() {
        OpenAICompatibleClient.cancelActiveRequests();
        if (running.get()) runner.cancel();
    }

    // ---- library ----

    public List<GlossaryStoreCore.Glossary> glossaries() { return GlossaryStoreCore.loadAll(glossaryPrefs); }

    public List<PronounStoreCore.Profile> pronouns() { return PronounStoreCore.loadAll(pronounPrefs); }

    public String selectedGlossaryId() { return GlossaryStoreCore.getSelectedId(glossaryPrefs); }

    public String selectedPronounId() { return PronounStoreCore.getSelectedId(pronounPrefs); }

    public void selectGlossary(String id) { GlossaryStoreCore.setSelectedId(glossaryPrefs, id); }

    public void selectPronoun(String id) { PronounStoreCore.setSelectedId(pronounPrefs, id); }

    public boolean deleteGlossary(String id) { return GlossaryStoreCore.delete(glossaryPrefs, id); }

    public GlossaryStoreCore.Glossary glossary(String id) { return GlossaryStoreCore.find(glossaryPrefs, id); }

    /** A new empty Glossary, or null when the store is locked because it could not be read. */
    public GlossaryStoreCore.Glossary createGlossary(String name) { return GlossaryStoreCore.create(glossaryPrefs, name); }

    public boolean saveGlossary(GlossaryStoreCore.Glossary g) { return GlossaryStoreCore.upsert(glossaryPrefs, g); }

    public boolean glossaryStoreLocked() { return GlossaryStoreCore.isCorrupt(glossaryPrefs); }

    public boolean pronounStoreLocked() { return PronounStoreCore.isCorrupt(pronounPrefs); }

    /** The Pronoun text that is in use for translation ("" when none). */
    public String selectedPronounText() { return PronounStoreCore.selectedPromptText(pronounPrefs); }

    /** The Glossary prompt text that is in use for translation ("" when none). */
    public String selectedGlossaryText() { return GlossaryStoreCore.selectedPromptText(glossaryPrefs); }

    public boolean deletePronoun(String id) { return PronounStoreCore.delete(pronounPrefs, id); }

    public int importGlossaries(List<Path> files) throws Exception {
        int imported = 0;
        for (Path f : files) {
            String text = TextDecoding.decode(Files.readAllBytes(f)).text;
            GlossaryStoreCore.Glossary g = GlossaryStoreCore.create(glossaryPrefs, stripExtension(f.getFileName().toString()));
            if (g == null) continue;
            GlossaryStoreCore.mergeTerms(g, GlossaryStoreCore.parseTerms(f.getFileName().toString(), text));
            if (GlossaryStoreCore.upsert(glossaryPrefs, g)) imported++;
        }
        return imported;
    }

    /** Pronoun files that were imported and the reason each refused file was left out (same rule as the phone: no valid rules, no import). */
    public static final class PronounImport {
        public int imported;
        public final List<String> failed = new ArrayList<>();
    }

    public int importPronouns(List<Path> files) throws Exception { return importPronounFiles(files).imported; }

    public PronounImport importPronounFiles(List<Path> files) throws Exception {
        PronounImport out = new PronounImport();
        for (Path f : files) {
            String text = TextDecoding.decode(Files.readAllBytes(f)).text;
            String name = stripExtension(f.getFileName().toString());
            LibraryImportPlanner.Result<PronounStoreCore.Profile> checked = LibraryImportPlanner.pronouns(
                    java.util.Collections.singletonList(new LibraryImportPlanner.Source(name, text)));
            if (checked.imported.isEmpty()) { out.failed.add(checked.failed.get(0)); continue; }
            if (PronounStoreCore.importProfile(pronounPrefs, name, PathDocumentIO.refOf(f), text) != null) out.imported++;
        }
        return out;
    }

    public PronounStoreCore.Profile pronoun(String id) { return PronounStoreCore.find(pronounPrefs, id); }

    public boolean savePronoun(PronounStoreCore.Profile p) { return PronounStoreCore.upsert(pronounPrefs, p); }

    public boolean renamePronoun(String id, String name) {
        PronounStoreCore.Profile p = pronoun(id);
        if (p == null || name == null || name.trim().isEmpty()) return false;
        p.name = name.trim();
        return savePronoun(p);
    }

    /** "Thay file": the profile keeps its id and takes the text of another file, and becomes the one in use, like on the phone. Null on success, else the reason. */
    public String replacePronounFile(String id, Path file) throws Exception {
        String text = TextDecoding.decode(Files.readAllBytes(file)).text;
        String name = stripExtension(file.getFileName().toString());
        LibraryImportPlanner.Result<PronounStoreCore.Profile> checked = LibraryImportPlanner.pronouns(
                java.util.Collections.singletonList(new LibraryImportPlanner.Source(name, text)));
        if (checked.imported.isEmpty()) return checked.failed.get(0);
        PronounStoreCore.Profile p = pronoun(id);
        if (p == null) return "Hồ sơ Pronoun không còn tồn tại";
        p.uri = "";
        p.text = text;
        if (p.name == null || p.name.startsWith("New pronoun")) p.name = name;
        return PronounStoreCore.saveAndSelect(pronounPrefs, p) ? null : "Không ghi được hồ sơ Pronoun";
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    // ---- RAW library ----

    public DesktopRawStore raws() { return raws; }

    public DesktopHistory history() { return history; }

    /** Theme and text size of the window. */
    public KeyValueStore appearancePrefs() { return appearancePrefs; }

    /** Reads TXT files like the phone does and adds them to the RAW library; returns {added, skipped}. */
    public int[] importRawFiles(List<Path> files) throws Exception {
        List<DesktopRawStore.Picked> picked = new ArrayList<>();
        for (Path f : files) {
            TextDecoding.Result d = TextDecoding.decode(Files.readAllBytes(f));
            picked.add(new DesktopRawStore.Picked(f.getFileName().toString(), d.text, d.encoding));
        }
        RawImportPlanner.Plan plan = raws.add(picked);
        return new int[]{plan.added(), plan.skipped()};
    }

    public DesktopRawBulk rawBulk() { return new DesktopRawBulk(raws); }

    // ---- chapter sets (stored in the shared chapter_sets tables) ----

    List<ComboDraft.RawRef> rawRefs() {
        List<ComboDraft.RawRef> out = new ArrayList<>();
        for (DesktopRawStore.Item i : raws.list()) out.add(new ComboDraft.RawRef(i.id, i.originalName, i.chapterNo));
        return out;
    }

    List<ChapterSetMatcher.Named> glossaryNames() {
        List<ChapterSetMatcher.Named> out = new ArrayList<>();
        for (GlossaryStoreCore.Glossary g : glossaries()) out.add(new ChapterSetMatcher.Named(g.id, g.name));
        return out;
    }

    List<ChapterSetMatcher.Named> pronounNames() {
        List<ChapterSetMatcher.Named> out = new ArrayList<>();
        for (PronounStoreCore.Profile p : pronouns()) out.add(new ChapterSetMatcher.Named(p.id, p.name));
        return out;
    }

    /** One line of the "Thư viện · Tập" list. */
    public static final class SetCard {
        public final String id;
        public final String name;
        public final int chapters;
        public final String summary;
        public final String range;
        public final int yellow;
        public final int red;
        SetCard(String id, String name, int chapters, String summary, String range, int yellow, int red) {
            this.id = id; this.name = name; this.chapters = chapters; this.summary = summary; this.range = range; this.yellow = yellow; this.red = red;
        }
    }

    public List<SetCard> setCards() {
        List<SetCard> out = new ArrayList<>();
        for (ChapterSetStoreCore.SetInfo info : ChapterSetStoreCore.listSets(sql)) {
            DesktopSetEditor d = DesktopSetEditor.open(this, info.id);
            int green = 0, yellow = 0, red = 0;
            List<Integer> chapters = new ArrayList<>();
            if (d != null) for (VolumePlan.RowView r : d.rows()) {
                chapters.add(r.chapterNo);
                if (r.status.equals("GREEN")) green++; else if (r.status.equals("YELLOW")) yellow++; else red++;
            }
            String range = ComboDraft.chapterRange(chapters);
            out.add(new SetCard(info.id, info.name, info.chapters, ComboDraft.summaryOf(info.chapters, green, yellow, red) + (range.isEmpty() ? "" : " · " + range), range, yellow, red));
        }
        return out;
    }

    public DesktopSetEditor newSet(java.util.Collection<String> rawIds) { return DesktopSetEditor.create(this, rawIds); }

    public DesktopSetEditor openSet(String id) { return DesktopSetEditor.open(this, id); }

    /** RAW that appear only in the given sets (so they would be orphaned by deleting those sets). */
    public List<String> rawOnlyInSets(java.util.Collection<String> setIds) {
        java.util.LinkedHashSet<String> mine = new java.util.LinkedHashSet<>(), others = new java.util.LinkedHashSet<>();
        for (ChapterSetStoreCore.SetInfo info : ChapterSetStoreCore.listSets(sql)) {
            ChapterSetStoreCore.SetData data = ChapterSetStoreCore.get(sql, info.id);
            if (data == null) continue;
            for (ChapterSetStoreCore.Row r : data.rows) (setIds.contains(info.id) ? mine : others).add(r.rawId);
        }
        mine.removeAll(others);
        mine.removeIf(id -> raws.find(id) == null);
        return new ArrayList<>(mine);
    }

    /** Deletes the sets; with {@code alsoRaw} the RAW that belonged only to them go too (RAW needed by an unfinished job stay). Returns the number of RAW removed. */
    public int deleteSets(java.util.Collection<String> setIds, boolean alsoRaw) {
        List<String> orphaned = alsoRaw ? rawOnlyInSets(setIds) : new ArrayList<String>();
        ChapterSetStoreCore.delete(sql, new ArrayList<>(setIds));
        int removed = 0;
        for (String id : orphaned) {
            if (!raws.unfinishedJobNames(id).isEmpty()) continue;
            raws.delete(new ArrayList<>(java.util.Collections.singletonList(id)));
            removed++;
        }
        return removed;
    }

    public boolean renameSet(String id, String name) {
        String n = name == null ? "" : name.trim();
        ChapterSetStoreCore.SetData data = ChapterSetStoreCore.get(sql, id);
        if (data == null || n.isEmpty()) return false;
        data.name = n;
        ChapterSetStoreCore.save(sql, data);
        return true;
    }

    /** A volume plan loaded from a saved set, or null when the set is gone. The run records the set's id in its session file. */
    public VolumePlan volumePlanFromSet(String setId) {
        DesktopSetEditor d = DesktopSetEditor.open(this, setId);
        if (d == null) return null;
        ChapterSetMatcher.rate(d.rowsForRun());
        return new VolumePlan(setId, d.name(), d.rowsForRun(), glossaries(), pronouns());
    }

    /** Package-private door to the database for the PC store bridges and their tests. */
    SqlDb db() { return sql; }

    /** Replaces the text of a RAW with another file. Returns null on success, else the reason in Vietnamese. */
    public String replaceRaw(String id, Path file) throws Exception {
        TextDecoding.Result d = TextDecoding.decode(Files.readAllBytes(file));
        java.util.Set<String> others = raws.hashes();
        DesktopRawStore.Item current = raws.find(id);
        if (current != null) others.remove(current.inputHash);
        List<RawImportPlanner.Candidate> one = new ArrayList<>();
        one.add(new RawImportPlanner.Candidate(file.getFileName().toString(), d.text, d.encoding));
        RawImportPlanner.Entry entry = RawImportPlanner.plan(one, others).entries.get(0);
        if (entry.verdict != RawImportPlanner.Verdict.ADD) return entry.reason;
        raws.replace(id, entry);
        return null;
    }

    // ---- settings profile shared with the phone ----

    /** What an imported profile would change: the settings it produces and only the shared keys that differ. */
    public static final class ProfilePreview {
        public final AppSettings imported;
        public final List<String[]> differences;
        ProfilePreview(AppSettings imported, List<String[]> differences) { this.imported = imported; this.differences = differences; }
    }

    private static final String PROFILE_UNDO_KEY = "profile_undo_json";

    public String exportProfileJson() throws Exception { return SettingsJson.toExportJson(loadSettings()); }

    public ProfilePreview previewProfile(String json) throws Exception {
        AppSettings current = loadSettings();
        AppSettings imported = SettingsJson.fromImportedProfile(json, current);
        return new ProfilePreview(imported, SettingsJson.differences(current, imported));
    }

    /** Saves the imported settings; the previous ones are kept for {@link #undoProfile()}. */
    public void applyProfile(ProfilePreview preview) throws IOException {
        String before = SettingsJson.toPersistedJson(loadSettings());
        saveSettings(preview.imported);
        settingsPrefs.edit().putString(PROFILE_UNDO_KEY, before).commit();
    }

    public boolean canUndoProfile() { return !settingsPrefs.getString(PROFILE_UNDO_KEY, "").isEmpty(); }

    public void undoProfile() throws IOException {
        String before = settingsPrefs.getString(PROFILE_UNDO_KEY, "");
        if (before.isEmpty()) return;
        AppSettings s = SettingsJson.fromJson(before);
        s.apiKey = "";
        saveSettings(s);
        settingsPrefs.edit().remove(PROFILE_UNDO_KEY).commit();
    }

    // ---- library archive (the phone's tbl-library ZIP) ----

    /** A read archive with what importing it would do; counts only. */
    public static final class LibraryPreview {
        final LibraryArchive.Contents contents;
        final LibraryArchive.MergePlan plan;
        LibraryPreview(LibraryArchive.Contents contents, LibraryArchive.MergePlan plan) { this.contents = contents; this.plan = plan; }
        public int glossariesInFile() { return contents.glossaries.size(); }
        public int pronounsInFile() { return contents.pronouns.size(); }
        public int rawsInFile() { return contents.raws.size(); }
        public int addGlossaries() { return plan.addGlossaries.size(); }
        public int addPronouns() { return plan.addPronouns.size(); }
        public int addRaws() { return plan.addRaws.size(); }
        public int existing() { return plan.existing; }
        public int duplicate() { return plan.duplicate; }
        /** Same id, different content: added under a new id and name. */
        public int renamed() { return plan.renamed; }
        /** Items whose integrity code did not match; they are never imported. */
        public int bad() { return plan.bad.size(); }
        public int addedCount() { return plan.addedCount(); }
    }

    /** Reads a library ZIP and plans the import against what is stored now. Throws IllegalArgumentException with a readable reason. */
    public LibraryPreview previewLibrary(Path zip) throws Exception {
        LibraryArchive.Contents contents;
        try (java.io.InputStream in = Files.newInputStream(zip)) { contents = LibraryArchive.read(in); }
        List<LibraryArchive.RawEntry> currentRaws = new java.util.ArrayList<>();
        for (DesktopRawStore.Item item : raws.list()) {
            LibraryArchive.RawEntry e = new LibraryArchive.RawEntry();
            e.id = item.id; e.inputHash = item.inputHash;
            currentRaws.add(e);
        }
        String date = java.time.LocalDate.now().toString();
        return new LibraryPreview(contents, LibraryArchive.plan(contents, glossaries(), pronouns(), currentRaws, date));
    }

    /** Adds the planned items; false when a store refused the write (nothing is half-applied for Glossary/Pronoun). */
    public boolean applyLibrary(LibraryPreview preview) throws IOException {
        LibraryArchive.MergePlan plan = preview.plan;
        boolean ok = true;
        if (!plan.addGlossaries.isEmpty()) {
            List<GlossaryStoreCore.Glossary> all = new java.util.ArrayList<>(plan.addGlossaries);
            all.addAll(glossaries());
            ok = GlossaryStoreCore.saveAll(glossaryPrefs, all);
        }
        if (ok && !plan.addPronouns.isEmpty()) {
            List<PronounStoreCore.Profile> all = new java.util.ArrayList<>(plan.addPronouns);
            all.addAll(pronouns());
            ok = PronounStoreCore.saveAll(pronounPrefs, all);
        }
        if (ok && !plan.addRaws.isEmpty()) raws.addEntries(plan.addRaws);
        log.append("Library import: added glossaries=" + plan.addGlossaries.size() + ", pronouns=" + plan.addPronouns.size()
                + ", raws=" + plan.addRaws.size() + ", existing=" + plan.existing + ", duplicate=" + plan.duplicate + ", bad=" + plan.bad.size());
        return ok;
    }

    /** Writes the whole library as the phone's ZIP and reads it back to check; returns {glossaries, pronouns, raws}. */
    public int[] exportLibrary(Path zip) throws Exception {
        List<GlossaryStoreCore.Glossary> g = glossaries();
        List<PronounStoreCore.Profile> p = pronouns();
        if (GlossaryStoreCore.isCorrupt(glossaryPrefs) || PronounStoreCore.isCorrupt(pronounPrefs)) throw new IllegalStateException("Dữ liệu Glossary/Pronoun đang bị khóa vì đọc lỗi");
        List<LibraryArchive.RawEntry> r = raws.entries();
        byte[] bytes = LibraryArchive.build(g, p, r, selectedGlossaryId(), selectedPronounId(), APP_VERSION, System.currentTimeMillis());
        if (zip.toAbsolutePath().getParent() != null) Files.createDirectories(zip.toAbsolutePath().getParent());
        Files.write(zip, bytes);
        LibraryArchive.Contents check;
        try (java.io.InputStream in = Files.newInputStream(zip)) { check = LibraryArchive.read(in); }
        if (check.glossaries.size() != g.size() || check.pronouns.size() != p.size() || check.raws.size() != r.size() || !check.bad.isEmpty()) {
            throw new IllegalStateException("Tệp đọc lại không khớp");
        }
        return new int[]{g.size(), p.size(), r.size()};
    }

    public static String suggestedLibraryFileName() {
        return LibraryArchive.suggestedFileName(new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.ROOT).format(new java.util.Date()));
    }

    public static final String APP_VERSION = "4.24-pc";

    // ---- whole volume (chapter queue) ----

    /** What a folder scan found and did; the note explains what was and was not scanned. */
    public static final class FolderImport {
        public int added, skipped;
        public String note = "";
    }

    /** The folder rule shown next to every folder picker: only files directly inside the chosen folder are read. */
    public static final String FOLDER_RULE = "App chỉ đọc các tệp nằm trực tiếp trong thư mục đã chọn, không quét thư mục con.";

    private static List<Path> filesIn(Path dir, String... extensions) throws IOException {
        List<Path> out = new ArrayList<>();
        try (java.util.stream.Stream<Path> list = Files.list(dir)) {
            list.filter(Files::isRegularFile).sorted().forEach(f -> {
                String n = f.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                for (String ext : extensions) if (n.endsWith(ext)) { out.add(f); return; }
            });
        }
        return out;
    }

    private static String subfolderNote(Path dir, boolean foundAny) {
        if (foundAny || !Files.isDirectory(dir.resolve("chapters"))) return "";
        return "Thư mục này có thư mục con chapters/ nhưng không có tệp ở ngay trong nó. " + FOLDER_RULE + " Hãy chọn chính thư mục chapters.";
    }

    /** Adds the TXT files of a folder to the RAW library (duplicates by content are skipped). */
    public FolderImport importRawFolder(Path dir) throws Exception {
        List<Path> files = filesIn(dir, ".txt");
        FolderImport r = new FolderImport();
        if (!files.isEmpty()) {
            int[] counts = importRawFiles(files);
            r.added = counts[0];
            r.skipped = counts[1];
        }
        r.note = subfolderNote(dir, !files.isEmpty());
        return r;
    }

    /** Adds the CSV/TXT Glossary files of a folder; a file whose name is already a Glossary is skipped. */
    public FolderImport importGlossaryFolder(Path dir) throws Exception {
        List<Path> files = filesIn(dir, ".csv", ".txt");
        java.util.Set<String> have = new java.util.HashSet<>();
        for (GlossaryStoreCore.Glossary g : glossaries()) have.add(g.name);
        List<Path> fresh = new ArrayList<>();
        FolderImport r = new FolderImport();
        for (Path f : files) {
            if (have.contains(stripExtension(f.getFileName().toString()))) r.skipped++; else fresh.add(f);
        }
        r.added = importGlossaries(fresh);
        r.note = subfolderNote(dir, !files.isEmpty());
        return r;
    }

    public FolderImport importPronounFolder(Path dir) throws Exception {
        List<Path> files = filesIn(dir, ".csv", ".txt");
        java.util.Set<String> have = new java.util.HashSet<>();
        for (PronounStoreCore.Profile p : pronouns()) have.add(p.name);
        List<Path> fresh = new ArrayList<>();
        FolderImport r = new FolderImport();
        for (Path f : files) {
            if (have.contains(stripExtension(f.getFileName().toString()))) r.skipped++; else fresh.add(f);
        }
        r.added = importPronouns(fresh);
        r.note = subfolderNote(dir, !files.isEmpty());
        return r;
    }

    /** The confirmation table for the chosen RAW ids (all RAW in the library when null), in chapter order. */
    public VolumePlan newVolumePlan(List<String> rawIds) {
        List<DesktopRawStore.Item> chosen = new ArrayList<>();
        for (DesktopRawStore.Item item : raws.list()) if (rawIds == null || rawIds.contains(item.id)) chosen.add(item);
        return new VolumePlan(chosen, glossaries(), pronouns());
    }

    /** Sum of the per-chapter estimates, each with its own Glossary/Pronoun text. */
    public CostEstimator.Estimate estimateVolume(VolumePlan plan, AppSettings base) throws Exception {
        AppSettings s = settingsForRun(base);
        CostEstimator.Estimate total = new CostEstimator.Estimate();
        total.pricingAvailable = true;
        for (ChapterBinding binding : plan.bindings()) {
            DesktopRawStore.Item item = raws.find(binding.rawId);
            if (item == null) throw new IllegalStateException("RAW của " + binding.rawName + " đã bị xóa khỏi thư viện");
            AppSettings sc = binding.applyTo(s);
            CostEstimator.Estimate e = CostEstimator.estimatePreparedChunks(Chunker.chunkText(raws.text(item.id), sc), sc);
            total.chunks += e.chunks; total.sourceTokens += e.sourceTokens;
            total.inputTokensLow += e.inputTokensLow; total.inputTokensHigh += e.inputTokensHigh;
            total.outputTokensLow += e.outputTokensLow; total.outputTokensHigh += e.outputTokensHigh;
            total.totalTokensLow += e.totalTokensLow; total.totalTokensHigh += e.totalTokensHigh;
            total.costLow += e.costLow; total.costHigh += e.costHigh;
            total.noCacheCostLow += e.noCacheCostLow; total.noCacheCostHigh += e.noCacheCostHigh;
            total.pricingAvailable &= e.pricingAvailable;
        }
        if (!total.pricingAvailable) { total.costLow = Double.NaN; total.costHigh = Double.NaN; }
        return total;
    }

    public boolean startVolume(VolumePlan plan, AppSettings base, Double capUsd, CostEstimator.Estimate estimate, Path outputDir) {
        if (plan.hasRed() || plan.included() == 0) return false;
        if (!allowedToCallProvider()) return false;
        if (!claim()) return false;
        final Double cap = capOverride(capUsd);
        executor.submit(() -> runVolume(plan, base, cap, estimate, outputDir));
        return true;
    }

    private void runVolume(VolumePlan plan, AppSettings base, Double capUsd, CostEstimator.Estimate estimate, Path outputDir) {
        try {
            runner.beginRun(true);
            runner.beginPreviewRun();
            runner.resetMetrics("", 0);
            if (estimate != null) runner.setEstimated(estimate.totalTokensHigh, estimate.pricingAvailable && !Double.isNaN(estimate.costHigh) ? estimate.costHigh : 0);
            runner.setState(TranslationJobState.State.VALIDATING, "Validating");
            runner.publish("validating", 0, "Validating a fresh translation session");
            AppSettings s = settingsForRun(base);
            s = runner.applyEffectiveCostLimit(s, capUsd);
            String validation = AppValidator.validateForTranslation(s);
            if (validation != null) throw new IllegalArgumentException(validation);
            List<ChapterBinding> bindings = plan.bindings();
            final String sessionId = java.util.UUID.randomUUID().toString();
            ChapterSession session = new ChapterSession(sessionId);
            session.setName = plan.name();
            session.setId = plan.setId();
            session.chapters.addAll(bindings);
            java.io.File sessionDir = dataDir.resolve(ChapterSession.DIR).toFile();
            Files.createDirectories(sessionDir.toPath());
            session.write(sessionDir);
            final ArrayList<ChapterQueue.Task> tasks = new ArrayList<>();
            for (ChapterBinding binding : bindings) {
                DesktopRawStore.Item item = raws.find(binding.rawId);
                if (item == null) throw new IllegalStateException("RAW của " + binding.rawName + " đã bị xóa khỏi thư viện");
                tasks.add(new ChapterQueue.Task(item.originalName, binding, null, item));
            }
            Files.createDirectories(outputDir);
            runner.setChapterOutcome(new ChapterRunOutcome(tasks.size()));
            runner.setState(TranslationJobState.State.READY, "Ready");
            runner.publish("ready", 0, "Input, configuration and output access are valid");
            runner.setState(TranslationJobState.State.RUNNING, "Running");
            runner.publish("running", 0, "Batch of " + tasks.size() + " file(s) initiated.");
            runner.setMetricPhase("Translating");
            final AppSettings baseSettings = s;
            volumeSessionId = sessionId;
            volumeLastJob = 0;
            boolean ended = ChapterQueue.run(runner, repo, tasks, baseSettings, (task, settings, fileNo, fileTotal, label) -> {
                DesktopRawStore.Item item = (DesktopRawStore.Item) task.payload;
                Path out = outputDir.resolve(baseSettings.buildOutputName(item.originalName));
                volumeOrdinal = fileNo - 1;
                try {
                    runner.startNewJob(item.uri(), PathDocumentIO.refOf(out), settings, fileNo, fileTotal);
                } finally {
                    attachVolumeJob();
                }
            });
            if (!ended) {
                finishCancelled("Đã hủy dịch");
                return;
            }
            endRun();
        } catch (TranslationRunner.CostLimitReachedException e) {
            runner.setState(TranslationJobState.State.PAUSED, "Paused");
            if (runner.getActiveJobId() > 0) repo.touchJob(runner.getActiveJobId(), "paused");
            runner.publish("paused", -1, AppValidator.readableError(e) + runner.chapterSummarySuffix());
            endRun();
        } catch (Exception e) {
            if (runner.isCancelled()) {
                finishCancelled("Đã hủy dịch");
                return;
            }
            runner.setState(TranslationJobState.State.FAILED, runner.getActiveJobId() > 0 ? "Translation failed" : "Initialization failure");
            if (runner.getActiveJobId() > 0) repo.touchJob(runner.getActiveJobId(), "error");
            runner.logFailure(runner.getMetricPhase(), e);
            if (e instanceof ChapterQueue.QueueStoppedException) runner.publish("error", -1, e.getMessage() + runner.chapterSummarySuffix());
            else runner.publish("error", -1, (runner.getActiveJobId() > 0 ? "Lỗi: " : "Lỗi khởi tạo: ") + AppValidator.readableError(e)
                    + runner.remainingSuffix() + runner.chapterSummarySuffix());
            endRun();
        } finally {
            volumeSessionId = null;
        }
    }

    /** Links the job being translated to the run (once per job), so the history can group the chapters of one run. */
    private void attachVolumeJob() {
        String session = volumeSessionId;
        if (session == null) return;
        long id = runner.getActiveJobId();
        if (id <= 0 || id == volumeLastJob) return;
        volumeLastJob = id;
        try { repo.attachRunSession(id, session, volumeOrdinal); } catch (RuntimeException e) { log.append("could not link job " + id + " to its run"); }
    }

    // ---- counts for the headless live check (numbers only, never text) ----

    public static int jobCalls(TranslationRepositoryCore.JobSummary job) { return job.usage == null ? 0 : job.usage.calls; }

    /** Chunk status -> how many chunks of the job have it. */
    public java.util.Map<String, Integer> chunkStatusCounts(long jobId) {
        java.util.TreeMap<String, Integer> out = new java.util.TreeMap<>();
        for (TranslationRepositoryCore.ChunkRow r : repo.getChunkRows(jobId)) out.merge(String.valueOf(r.status), 1, Integer::sum);
        return out;
    }

    /** Provider finish reason -> number of attempts that ended with it. */
    public java.util.Map<String, Integer> finishReasonCounts(long jobId) {
        java.util.TreeMap<String, Integer> out = new java.util.TreeMap<>();
        for (TranslationRepositoryCore.ChunkRow r : repo.getChunkRows(jobId)) {
            for (TranslationRepositoryCore.AttemptRow a : repo.getAttemptRows(jobId, r.index)) out.merge(a.finishReason == null || a.finishReason.isEmpty() ? "(none)" : a.finishReason, 1, Integer::sum);
        }
        return out;
    }

    public AppSettings normalizedForEstimate(AppSettings base) throws Exception { return settingsForRun(base); }

    public List<String> logTail(int lines) {
        try {
            Path f = dataDir.resolve("logs").resolve("tbl_runtime.log");
            if (!Files.exists(f)) return new ArrayList<>();
            List<String> all = Files.readAllLines(f, StandardCharsets.UTF_8);
            return new ArrayList<>(all.subList(Math.max(0, all.size() - lines), all.size()));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    @Override public void close() {
        executor.shutdownNow();
        sql.close();
    }
}
