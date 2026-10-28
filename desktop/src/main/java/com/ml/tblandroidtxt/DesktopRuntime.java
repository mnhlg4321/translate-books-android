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
    private final FileRuntimeLog log;
    private final FileModelCatalogCache catalogCache;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "tbl-run");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile UiEvents ui;

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
        this.raws = new DesktopRawStore(sql, this.dataDir.resolve(DesktopRawStore.DIR).toFile());
        this.log = new FileRuntimeLog(this.dataDir.resolve("logs"));
        this.catalogCache = new FileModelCatalogCache(this.dataDir.resolve("catalog").resolve("openrouter_model_catalog_v42.json").toFile());
        ObservabilityLog.install(log);
        loadCatalogFromCacheOnly();

        RunListener forwarding = new RunListener() {
            @Override public void onSnapshot(RunSnapshot snapshot) { UiEvents t = ui; if (t != null) t.onSnapshot(snapshot); }
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
        if (!claim()) return false;
        executor.submit(() -> runNew(input, output, base, capUsd, estimate));
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

    public boolean resumeJob(long jobId) {
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

    public int importPronouns(List<Path> files) throws Exception {
        int imported = 0;
        for (Path f : files) {
            String text = TextDecoding.decode(Files.readAllBytes(f)).text;
            if (PronounStoreCore.importProfile(pronounPrefs, stripExtension(f.getFileName().toString()), PathDocumentIO.refOf(f), text) != null) imported++;
        }
        return imported;
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    // ---- RAW library ----

    public DesktopRawStore raws() { return raws; }

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
