package com.ml.tblandroidtxt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Everything a scenario needs: schema-backed repository, scripted provider, fake clock, recording ports, temp-dir documents. */
final class RunnerHarness {
    /** One scripted provider reaction. */
    interface Step {
        OpenAICompatibleClient.ChatResult apply(AppSettings settings, PromptPair prompt, int outputLimit) throws Exception;
    }

    static final class Call {
        final String effort;
        final int outputLimit;
        final String system;
        final String user;
        Call(String effort, int outputLimit, String system, String user) {
            this.effort = effort; this.outputLimit = outputLimit; this.system = system; this.user = user;
        }
    }

    final TestDb testDb;
    final TranslationRepositoryCore repo;
    final TranslationEngineCore engine;
    final TranslationRunner runner;
    final AtomicLong time = new AtomicLong(10_000L);
    final List<Step> script = new ArrayList<>();
    final List<Call> calls = new ArrayList<>();
    final List<RunSnapshot> snapshots = new ArrayList<>();
    final List<String> notifications = new ArrayList<>();
    final List<String> cancelledEnds = new ArrayList<>();
    final List<String> logLines = new ArrayList<>();
    final List<long[]> slept = new ArrayList<>();
    int runEnded;
    final Path dir;

    RunnerHarness(Path dir) throws Exception {
        this.dir = dir;
        testDb = TestDb.openWithSchema();
        repo = new TranslationRepositoryCore(testDb.db) { };
        engine = new TranslationEngineCore(repo, (settings, prompt, outputLimit, requestId, observer) -> {
            Call call = new Call(settings.reasoningEffort, outputLimit, prompt.system, prompt.user);
            Step step;
            synchronized (RunnerHarness.this) {
                calls.add(call);
                if (script.isEmpty()) throw new AssertionError("provider called more often than scripted (call " + calls.size() + ")");
                step = script.remove(0);
            }
            return step.apply(settings, prompt, outputLimit);
        });
        engine.setClock(time::get);
        engine.setSleeper(millis -> slept.add(new long[]{millis}));
        RunListener listener = new RunListener() {
            @Override public void onSnapshot(RunSnapshot snapshot) { synchronized (RunnerHarness.this) { snapshots.add(snapshot); } }
            @Override public void onNotification(String text, int done, int total) { synchronized (RunnerHarness.this) { notifications.add(text + "|" + done + "/" + total); } }
            @Override public void onRunEnded() { synchronized (RunnerHarness.this) { runEnded++; } }
            @Override public void onCancelledEnd(String message) { synchronized (RunnerHarness.this) { cancelledEnds.add(message); } }
        };
        DocumentIO documents = new DocumentIO() {
            private Path file(String ref) { return dir.resolve(ref.replace("mem://", "")); }
            @Override public String readText(String ref) throws IOException { return new String(Files.readAllBytes(file(ref)), StandardCharsets.UTF_8); }
            @Override public String displayName(String ref) { return file(ref).getFileName().toString(); }
            @Override public void writeTextVerified(String ref, String text) throws IOException { writeText(ref, text); }
            @Override public void writeText(String ref, String text) throws IOException { Files.write(file(ref), text.getBytes(StandardCharsets.UTF_8)); }
            @Override public String resolveOutput(long jobId, String ref) { return ref; }
        };
        RunStatePersister persister = new RunStatePersister() {
            @Override public void save(String jobState, String status, int progress, String log, String phase, String fileName,
                                       int totalChunks, int completed, int failed, int fallbacks, int totalTokens, double totalCost,
                                       int estimatedTotalTokens, double estimatedTotalCost, boolean providerUsageComplete,
                                       long elapsedMs, long remainingMs, String preview, int currentChunk, int glossaryLocks,
                                       int pronounLocks, int lockChunk, String lockPhase, int previewChunk, long activeJobId,
                                       String previewFile, int previewTotal) { }
            @Override public void saveSession(long sessionTokens, double sessionCost, long sessionStartMs, long sessionEndMs,
                                              long pausedAtMs, long pausedTotalMs, int setTotal, int setDone, int setFailed, int setIndex) { }
        };
        RuntimeLog log = new RuntimeLog() {
            @Override public void append(String message) { synchronized (RunnerHarness.this) { logLines.add(message); } }
            @Override public void error(String tag, String message, Throwable error) { synchronized (RunnerHarness.this) { logLines.add("E " + tag + " " + message); } }
            @Override public String stackTrace(Throwable error) { return error.getClass().getName(); }
        };
        SecretsResolver secrets = snapshot -> { snapshot.apiKey = "TEST_KEY_NOT_SECRET"; return snapshot; };
        runner = new TranslationRunner(listener, documents, persister, log, TraceSink.NONE, secrets, time::get);
        runner.attach(repo, engine);
        engine.setSessionCostSupplier(runner.runCostSupplier());
    }

    void close() throws Exception { testDb.close(); }

    static AppSettings settings() {
        AppSettings s = new AppSettings();
        s.apiKey = "TEST_KEY_NOT_SECRET";
        s.model = "fixture-model";
        s.reasoningEffort = "off";
        return s;
    }

    /** Chunks "A1.", "A2.", ... with contiguous offsets, which is what the integrity audit expects. */
    static List<Chunk> chunks(String... sources) {
        ArrayList<Chunk> list = new ArrayList<>();
        int at = 0;
        for (int i = 0; i < sources.length; i++) {
            list.add(new Chunk(i, at, at + sources[i].length(), "", sources[i], "", ""));
            at += sources[i].length();
        }
        return list;
    }

    /** Creates the job the way the service does for a fresh start and primes the runner for it. */
    long startJob(AppSettings s, List<Chunk> chunks) {
        long id = repo.createJob("mem://in.txt", "mem://out.txt", "book.txt", s, chunks, "", "");
        runner.beginRun(true);
        runner.resetMetrics("book.txt", chunks.size());
        runner.setActiveJobId(id);
        repo.touchJob(id, "RUNNING");
        return id;
    }

    // ---- scripted answers ----

    static OpenAICompatibleClient.ChatResult answer(String text) {
        OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
        r.content = "<TRANSLATION>" + text + "</TRANSLATION>";
        r.finishReason = "stop";
        r.usageReported = true;
        r.promptTokens = 10;
        r.completionTokens = 5;
        r.totalTokens = 15;
        return r;
    }

    /** The source between <INPUT> and </INPUT> with its leading letter changed from A to B ("A2." -> "B2."). */
    static String translate(PromptPair prompt) {
        String user = prompt.user;
        int from = user.indexOf("<INPUT>\n") + "<INPUT>\n".length();
        int to = user.indexOf("\n</INPUT>", from);
        return "B" + user.substring(from, to).substring(1);
    }

    static Step ok() { return (s, p, limit) -> answer(translate(p)); }

    static Step okWithCost(double cost) {
        return (s, p, limit) -> {
            OpenAICompatibleClient.ChatResult r = answer(translate(p));
            r.providerCostReported = true;
            r.providerCost = cost;
            return r;
        };
    }

    static Step fail(Exception e) { return (s, p, limit) -> { throw e; }; }

    static Step content(String raw, String finish, int completion, int reasoning) {
        return (s, p, limit) -> {
            OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
            r.content = raw;
            r.finishReason = finish;
            r.usageReported = true;
            r.completionTokens = completion;
            r.reasoningTokens = reasoning;
            r.totalTokens = completion;
            return r;
        };
    }

    synchronized List<String> statuses() {
        ArrayList<String> out = new ArrayList<>();
        for (RunSnapshot s : snapshots) out.add(s.status);
        return out;
    }

    synchronized List<Integer> progresses() {
        ArrayList<Integer> out = new ArrayList<>();
        for (RunSnapshot s : snapshots) out.add(s.progress);
        return out;
    }

    String chunkStatus(long jobId, int idx) { return repo.chunkStatus(jobId, idx); }

    String output() throws IOException {
        Path f = dir.resolve("out.txt");
        return Files.exists(f) ? new String(Files.readAllBytes(f), StandardCharsets.UTF_8) : null;
    }

    String jobStatus(long jobId) {
        for (TranslationRepositoryCore.JobSummary s : repo.getJobSummaries(10)) if (s.id == jobId) return s.status;
        return null;
    }
}
