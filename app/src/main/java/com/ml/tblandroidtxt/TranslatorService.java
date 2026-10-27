package com.ml.tblandroidtxt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TranslatorService extends Service {
    /** The run stopped because too many chapters in a row failed their validation; the message is the sentence shown to the person. */
    private static class QueueStoppedException extends IllegalStateException {
        QueueStoppedException(String message) { super(message); }
    }

    public static final String ACTION_START = "com.ml.tblandroidtxt.START";
    public static final String ACTION_RESUME_LAST = "com.ml.tblandroidtxt.RESUME_LAST";
    public static final String ACTION_RETRY_FAILED = "com.ml.tblandroidtxt.RETRY_FAILED";
    public static final String ACTION_RESUME_JOB = "com.ml.tblandroidtxt.RESUME_JOB";
    public static final String ACTION_RETRY_JOB_FAILED = "com.ml.tblandroidtxt.RETRY_JOB_FAILED";
    public static final String ACTION_RETRY_CHUNK = "com.ml.tblandroidtxt.RETRY_CHUNK";
    public static final String ACTION_BENCHMARK_JOB = "com.ml.tblandroidtxt.BENCHMARK_JOB";
    public static final String ACTION_PAUSE = "com.ml.tblandroidtxt.PAUSE";
    public static final String ACTION_RESUME = "com.ml.tblandroidtxt.RESUME";
    public static final String ACTION_CANCEL = "com.ml.tblandroidtxt.CANCEL";
    public static final String ACTION_PROGRESS = "com.ml.tblandroidtxt.PROGRESS";
    public static final String EXTRA_LOG = "log";
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_PHASE = "phase";
    public static final String EXTRA_FILE_NAME = "fileName";
    public static final String EXTRA_TOTAL_CHUNKS = "totalChunks";
    public static final String EXTRA_COMPLETED = "completed";
    public static final String EXTRA_FAILED = "failed";
    public static final String EXTRA_FALLBACKS = "fallbacks";
    public static final String EXTRA_TOTAL_TOKENS = "totalTokens";
    public static final String EXTRA_TOTAL_COST = "totalCost";
    public static final String EXTRA_ESTIMATED_TOTAL_TOKENS = "estimatedTotalTokens";
    public static final String EXTRA_ESTIMATED_TOTAL_COST = "estimatedTotalCost";
    public static final String EXTRA_PROVIDER_USAGE_COMPLETE = "providerUsageComplete";
    public static final String EXTRA_ELAPSED_MS = "elapsedMs";
    public static final String EXTRA_REMAINING_MS = "remainingMs";
    public static final String EXTRA_PREVIEW = "preview";
    public static final String EXTRA_CURRENT_CHUNK = "currentChunk";
    public static final String EXTRA_GLOSSARY_LOCKS = "glossaryLocks";
    public static final String EXTRA_PRONOUN_LOCKS = "pronounLocks";
    public static final String EXTRA_LOCK_CHUNK = "lockChunk";
    public static final String EXTRA_LOCK_PHASE = "lockPhase";
    public static final String EXTRA_PREVIEW_CHUNK = "previewChunk";
    public static final String EXTRA_PREVIEW_FILE = "previewFile";
    public static final String EXTRA_PREVIEW_TOTAL = "previewTotal";
    /** Totals of the whole run (every chapter, retries included), not of the job being worked on. */
    public static final String EXTRA_SESSION_TOKENS = "sessionTokens";
    public static final String EXTRA_SESSION_COST = "sessionCost";
    public static final String EXTRA_SESSION_START_MS = "sessionStartMs";
    public static final String EXTRA_SESSION_END_MS = "sessionEndMs";
    public static final String EXTRA_PAUSED_AT_MS = "pausedAtMs";
    public static final String EXTRA_PAUSED_TOTAL_MS = "pausedTotalMs";
    /** Positive value is the cap for this run; -1 is the explicit unlimited toggle. */
    public static final String EXTRA_EFFECTIVE_COST_LIMIT_USD = "effectiveCostLimitUsd";
    /** Chapter-set runs only: chapters in the run, finished, failed, and the 1-based chapter being worked on (all 0 otherwise). */
    public static final String EXTRA_SET_TOTAL = "setTotal";
    public static final String EXTRA_SET_DONE = "setDone";
    public static final String EXTRA_SET_FAILED = "setFailed";
    public static final String EXTRA_SET_INDEX = "setIndex";
    public static final String EXTRA_ACTIVE_JOB_ID = "activeJobId";
    public static final String EXTRA_STATE = "jobState";

    private static final String CHANNEL_ID = "translation";
    static final int NOTIFY_ID = 1001;
    private static final AtomicBoolean ACTIVE = new AtomicBoolean(false);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TranslationRepository repo;
    private TranslationEngine engine;
    private TranslationRunner runner;

    public static boolean isActive() { return ACTIVE.get(); }

    @Override public void onCreate() {
        super.onCreate();
        runner = new TranslationRunner(new ServiceRunListener(this), new AndroidDocumentIO(this), new AndroidRunStatePersister(this),
                new AndroidRuntimeLog(this), new AndroidTraceSink(this), snapshot -> SettingsStore.withCurrentSecrets(this, snapshot), Clock.SYSTEM);
        ObservabilityLog.install(new AndroidObservabilitySink(this));
        try { PersistedSettingsCleanup.runOnce(getApplicationContext()); }
        catch (Exception ignored) { Log.e("TranslateBooks", "settings_cleanup_failed"); }
        createChannel();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (!ACTIVE.get()) executor.shutdownNow();
        if(repo!=null)repo.close();
    }

    @Override public void onTimeout(int startId,int foregroundServiceType){runner.setCancelled(true);runner.setPaused(false);OpenAICompatibleClient.cancelActiveRequests();if(repo!=null&&runner.getActiveJobId()>0)repo.touchJob(runner.getActiveJobId(),"paused");runner.setMetricPhase("System timeout");runner.setState(TranslationJobState.State.PAUSED,"System timeout");runner.markPausedAt(System.currentTimeMillis());runner.publish("paused",-1,"Android stopped the data-sync service; progress was checkpointed and can be resumed safely");releaseRun();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(startId);}

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_PAUSE.equals(action)) {
            if (!ACTIVE.get()) {
                runner.publish("paused", -1, "Không có job đang chạy để tạm dừng");
                return START_NOT_STICKY;
            }
            runner.pause();
            return START_NOT_STICKY;
        }
        if (ACTION_RESUME.equals(action)) {
            if (!ACTIVE.get()) {
                ObservabilityLog.event("resume_decision", "action", "RESUME", "branch", "no_running_job");
                runner.publish("info", -1, "Không có job nào đang tạm dừng. Job đang dở (nếu có) nằm ở thẻ \"Job đang dở\" trên màn Dịch hoặc ở tab Jobs: bấm \"Tiếp tục dịch\".");
                return START_NOT_STICKY;
            }
            runner.resume();
            return START_NOT_STICKY;
        }
        if (ACTION_CANCEL.equals(action)) {
            OpenAICompatibleClient.cancelActiveRequests();
            if (!ACTIVE.get()) {
                runner.publish("cancelled", -1, "Không có job đang chạy để hủy");
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                return START_NOT_STICKY;
            }
            runner.cancel();
            return START_NOT_STICKY;
        }
        if (ACTION_START.equals(action)) {
            if (!acquireRun("Start")) return START_NOT_STICKY;
            runner.beginRun(true);
            runner.resetMetrics("", 0);
            runner.setEstimated(intent.getIntExtra(EXTRA_ESTIMATED_TOTAL_TOKENS, 0), intent.getDoubleExtra(EXTRA_ESTIMATED_TOTAL_COST, 0));
            runner.setState(TranslationJobState.State.VALIDATING, "Validating");
            runner.publish("validating", 0, "Validating a fresh translation session");
            if(!promote("Preparing translation",0,0)){releaseRun();return START_NOT_STICKY;}
            executor.submit(() -> { try { if(ensureRuntime())runNewJobs(intent); } finally { releaseRun(); } });
            return START_NOT_STICKY;
        }
        if (ACTION_RESUME_LAST.equals(action)) {
            if (!acquireRun("Resume checkpoint")) return START_NOT_STICKY;
            runner.beginRun(true);
            runner.resetMetrics("",0);runner.setState(TranslationJobState.State.VALIDATING,"Validating resume");
            if(!promote("Resuming checkpoint",0,0)){releaseRun();return START_NOT_STICKY;}
            executor.submit(() -> { try { if(ensureRuntime())runner.resumeLastJob(); } finally { releaseRun(); } });
            return START_NOT_STICKY;
        }
        if (ACTION_RESUME_JOB.equals(action)) {
            final long jobId = intent.getLongExtra("jobId", -1L);
            if (!acquireRun("Resume selected job")) return START_NOT_STICKY;
            runner.beginRun(true);
            runner.resetMetrics("",0);runner.setState(TranslationJobState.State.VALIDATING,"Validating resume");
            if(!promote("Resuming job #"+jobId,0,0)){releaseRun();return START_NOT_STICKY;}
            executor.submit(() -> { try { if(ensureRuntime())runner.resumeJobById(jobId); } finally { releaseRun(); } });
            return START_NOT_STICKY;
        }
        if (ACTION_RETRY_FAILED.equals(action)) {
            if (!acquireRun("Retry failed chunks")) return START_NOT_STICKY;
            runner.beginRun(true);
            runner.resetMetrics("",0);runner.setState(TranslationJobState.State.VALIDATING,"Validating retry");
            if(!promote("Retrying failed chunks",0,0)){releaseRun();return START_NOT_STICKY;}
            executor.submit(() -> { try { if(ensureRuntime())runner.retryFailedChunksOnly(); } finally { releaseRun(); } });
            return START_NOT_STICKY;
        }
        if (ACTION_RETRY_JOB_FAILED.equals(action)) {
            final long jobId = intent.getLongExtra("jobId", -1L);
            if (!acquireRun("Retry selected job")) return START_NOT_STICKY;
            runner.beginRun(true);
            runner.resetMetrics("",0);runner.setState(TranslationJobState.State.VALIDATING,"Validating retry");
            if(!promote("Retrying job #"+jobId,0,0)){releaseRun();return START_NOT_STICKY;}
            executor.submit(() -> { try { if(ensureRuntime())runner.retryFailedChunksForJob(jobId); } finally { releaseRun(); } });
            return START_NOT_STICKY;
        }
        if(ACTION_RETRY_CHUNK.equals(action)){final long jobId=intent.getLongExtra("jobId",-1L);final int chunkIdx=intent.getIntExtra("chunkIdx",-1);final String edited=intent.getStringExtra("settings");final boolean confirmedUnknown=intent.getBooleanExtra("confirmDeliveryUnknown",false);if(!acquireRun("Retry/continue chunk"))return START_NOT_STICKY;runner.beginRun(false);if(!promote("Retry/continue chunk "+(chunkIdx+1),0,0)){releaseRun();return START_NOT_STICKY;}executor.submit(()->{try{if(!ensureRuntime())return;runner.retryChunk(jobId,chunkIdx,edited,confirmedUnknown);}finally{releaseRun();}});return START_NOT_STICKY;}
        if (ACTION_BENCHMARK_JOB.equals(action)) {
            final long jobId=intent.getLongExtra("jobId",-1L); final double budget=intent.getDoubleExtra("budgetUsd",0);
            if(!acquireRun("Live A/B benchmark"))return START_NOT_STICKY;
            runner.setCancelled(false);runner.setPaused(false);runner.setActiveJobId(jobId);runner.setBenchmarkRun(true);
            if(!promote("Benchmarking job #"+jobId,0,0)){releaseRun();return START_NOT_STICKY;}
            executor.submit(()->{try{if(ensureRuntime())runLiveBenchmark(jobId,budget);}finally{runner.setBenchmarkRun(false);releaseRun();}});
            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    private synchronized boolean ensureRuntime(){if(repo!=null&&engine!=null)return true;try{PersistedSettingsCleanup.runOnce(getApplicationContext());ModelCatalog.initialize(new AndroidModelCatalogCache(this));repo=new TranslationRepository(this);repo.recoverInterruptedState();engine=new TranslationEngine(this,repo,TranslateTestSeam.provider());runner.attach(repo,engine);engine.setSessionCostSupplier(runner.runCostSupplier());return true;}catch(Exception e){runner.setState(TranslationJobState.State.FAILED,"Initialization failure");runner.logFailure("runtime initialization",e);runner.publish("error",-1,"Translation could not initialize: "+AppValidator.readableError(e));stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return false;}}

    private boolean promote(String text,int done,int total){try{startForeground(NOTIFY_ID,buildNotification(text,done,total));return true;}catch(RuntimeException e){runner.setState(TranslationJobState.State.FAILED,"Foreground service error");runner.logFailure("foreground service promotion",e);runner.publish("error",-1,"Android could not start the translation service: "+AppValidator.readableError(e));stopSelf();return false;}}

    private boolean acquireRun(String label) {
        if (ACTIVE.compareAndSet(false, true)) {
            runner.beginPreviewRun();
            return true;
        }
        runner.setMetricPhase("Already running");
        runner.publish("running", -1, label + " bị chặn: đang có job dịch khác chạy. Không tạo job mới để tránh dịch lặp/tốn API.");
        return false;
    }

    void releaseRun() {
        ACTIVE.set(false);
        runner.setActiveJobId(-1L);
    }

    private void runLiveBenchmark(long jobId,double budgetUsd){
        try{
            runner.setMetricPhase("A/B Benchmark");runner.publish("running",0,"Starting paid Full/Balanced benchmark for job #"+jobId);
            LiveBenchmarkRunner.Result result=LiveBenchmarkRunner.run(this,repo,jobId,budgetUsd,()->runner.isCancelled(),msg->runner.publish("running",-1,msg));
            runner.setMetricPhase("Benchmark completed");runner.publish("done",100,"Benchmark #"+result.sessionId+" completed: "+result.requests+" requests, cost "+CostEstimator.money(result.spentUsd));
        }catch(Exception e){runner.setMetricPhase("Benchmark error");if(runner.isCancelled())runner.setState(TranslationJobState.State.CANCELLED,"Cancelled");else runner.setState(TranslationJobState.State.FAILED,"Benchmark error");runner.publish(runner.isCancelled()?"cancelled":"error",-1,"Benchmark stopped: "+AppValidator.readableError(e));}
        finally{stopForeground(STOP_FOREGROUND_REMOVE);NotificationManager nm=getSystemService(NotificationManager.class);if(nm!=null)nm.cancel(NOTIFY_ID);stopSelf();}
    }

    void finishCancelled(String message) {
        runner.setMetricPhase("Cancelled");
        if (runner.getActiveJobId() > 0) repo.touchJob(runner.getActiveJobId(), "cancelled");
        runner.setState(TranslationJobState.State.CANCELLED, "Cancelled");
        runner.publish("cancelled", -1, message == null || message.isEmpty() ? "Đã dừng dịch" : message);
        releaseRun();
        stopForeground(STOP_FOREGROUND_REMOVE);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(NOTIFY_ID);
        stopSelf();
    }

    /** The cost cap carried by the Intent, or null when it has none (the old applyEffectiveCostLimit read it inline). */
    private static Double capFrom(Intent intent) {
        return intent == null ? null : intent.getDoubleExtra(EXTRA_EFFECTIVE_COST_LIMIT_USD, Double.NaN);
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void runNewJobs(Intent intent) {
        try {
            String preparedBatchId=intent.getStringExtra("preparedBatchId");
            if(preparedBatchId!=null&&!preparedBatchId.trim().isEmpty()){
                runPreparedJobs(intent,preparedBatchId.trim());
                return;
            }
            ArrayList<Uri> inputs = engine.readInputUris(intent);
            if (inputs.isEmpty()) throw new IllegalArgumentException("No input TXT files");
            String outputUriStr = intent.getStringExtra("outputUri");
            String outputTreeUriStr = intent.getStringExtra("outputTreeUri");
            Uri fixedOutput = outputUriStr == null || outputUriStr.isEmpty() ? null : Uri.parse(outputUriStr);
            Uri outputTree = outputTreeUriStr == null || outputTreeUriStr.isEmpty() ? null : Uri.parse(outputTreeUriStr);
            if (inputs.size() > 1 && outputTree == null) throw new IllegalArgumentException("Batch mode cần chọn output folder");
            if (inputs.size() == 1 && fixedOutput == null && outputTree == null) throw new IllegalArgumentException("Chưa chọn output TXT hoặc output folder");

            validateOutputAccess(inputs.size(), fixedOutput, outputTree);

            String suppliedSettings=intent.getStringExtra("settings");
            AppSettings s = suppliedSettings==null||suppliedSettings.trim().isEmpty()?SettingsStore.load(this):SettingsStore.withCurrentSecrets(this,SettingsStore.fromJson(suppliedSettings));
            s = engine.loadExternalConfig(intent, s);
            if (s.glossaryText == null || s.glossaryText.trim().isEmpty()) s.glossaryText = GlossaryStore.selectedPromptText(this);
            s = AppValidator.normalize(s);
            AppSettings ownerSettings = s.copy();
            s = runner.applyEffectiveCostLimit(s, capFrom(intent));
            String validation = AppValidator.validateForTranslation(s);
            if (validation != null) throw new IllegalArgumentException(validation);
            SettingsStore.save(this, ownerSettings);

            runner.setState(TranslationJobState.State.READY, "Ready");
            runner.publish("ready", 0, "Input, configuration and output access are valid");
            runner.setState(TranslationJobState.State.RUNNING, "Running");
            runner.publish("running", 0, "Batch of " + inputs.size() + " file(s) initiated.");
            for (int f = 0; f < inputs.size(); f++) {
                runner.waitIfPaused();
                if (runner.isCancelled()) { finishCancelled("Đã hủy batch trước khi chạy file tiếp theo"); return; }
                Uri inputUri = inputs.get(f);
                String fileName = FileUtil.displayName(this, inputUri);
                Uri outUri = fixedOutput;
                if (outUri == null || inputs.size() > 1) outUri = FileUtil.createOutputInTree(this, outputTree, s.buildOutputName(fileName));
                runner.setMetricPhase("Translating");
                runner.publish("running", Math.round(f * 100f / inputs.size()), "Starting translation for: " + fileName);
                runSingleJob(inputUri, outUri, s, f + 1, inputs.size());
                if (runner.isCancelled()) { finishCancelled("Đã hủy batch dịch"); return; }
            }
            runner.setState(TranslationJobState.State.COMPLETED, "Completed");
            runner.publish("done", 100, "Batch translation completed for all files");
            releaseRun();
            stopForeground(STOP_FOREGROUND_REMOVE);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(NOTIFY_ID);
            stopSelf();
        } catch (TranslationRunner.CostLimitReachedException e) {
            runner.setState(TranslationJobState.State.PAUSED,"Paused");
            if (runner.getActiveJobId() > 0) repo.touchJob(runner.getActiveJobId(), "paused");
            runner.publish("paused", -1, AppValidator.readableError(e) + runner.chapterSummarySuffix());
            releaseRun();
            stopForeground(STOP_FOREGROUND_REMOVE);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(NOTIFY_ID);
            stopSelf();
        } catch (Exception e) {
            if (runner.isCancelled()) { finishCancelled("Đã hủy dịch"); return; }
            runner.setState(TranslationJobState.State.FAILED, runner.getActiveJobId() > 0 ? "Translation failed" : "Initialization failure");
            if (runner.getActiveJobId() > 0) repo.touchJob(runner.getActiveJobId(), "error");
            runner.logFailure(runner.getMetricPhase(), e);
            if (e instanceof QueueStoppedException) runner.publish("error", -1, e.getMessage() + runner.chapterSummarySuffix());
            else runner.publish("error", -1, (runner.getActiveJobId() > 0 ? "Lỗi: " : "Lỗi khởi tạo: ") + AppValidator.readableError(e) + runner.remainingSuffix() + runner.chapterSummarySuffix());
            releaseRun();
            stopForeground(STOP_FOREGROUND_REMOVE);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(NOTIFY_ID);
            stopSelf();
        }
    }


    private void runPreparedJobs(Intent intent,String batchId) throws Exception {
        PreparedBatch batch=repo.getPreparedBatch(batchId);
        if(batch==null||!batch.ready())throw new IllegalStateException("Prepared chunk plan is missing or stale; select the TXT again");
        String startSessionId=intent.getStringExtra("startSessionId");
        if(startSessionId==null||startSessionId.trim().isEmpty())throw new IllegalArgumentException("Fresh Start is missing its session identity");
        String supplied=intent.getStringExtra("settings");
        AppSettings s=AppValidator.normalize(supplied==null||supplied.trim().isEmpty()?SettingsStore.withCurrentSecrets(this,SettingsStore.fromJson(batch.settingsJson)):SettingsStore.withCurrentSecrets(this,SettingsStore.fromJson(supplied)));
        AppSettings ownerSettings = s.copy();
        s = runner.applyEffectiveCostLimit(s, capFrom(intent));
        String validation=AppValidator.validateForTranslation(s);if(validation!=null)throw new IllegalArgumentException(validation);
        String outputUriStr=intent.getStringExtra("outputUri"),treeStr=intent.getStringExtra("outputTreeUri");
        Uri fixed=outputUriStr==null||outputUriStr.isEmpty()?null:Uri.parse(outputUriStr);Uri tree=treeStr==null||treeStr.isEmpty()?null:Uri.parse(treeStr);
        if(batch.inputs.size()>1&&tree==null)throw new IllegalArgumentException("Batch mode requires an output folder");
        if(batch.inputs.size()==1&&fixed==null&&tree==null)throw new IllegalArgumentException("Choose an output TXT or folder");
        for(PreparedBatch.Input input:batch.inputs){String error=FileUtil.validateReadable(this,input.uri,"Input "+(input.ordinal+1));if(error!=null)throw new IllegalStateException(error);}
        validateOutputAccess(batch.inputs.size(),fixed,tree);
        runner.setChapterOutcome(null);
        final boolean chapterMode=intent.getBooleanExtra("chapterMode",false);
        ChapterSession session=null;
        if(chapterMode){
            session=ChapterSession.read(new java.io.File(getFilesDir(),ChapterSession.DIR),startSessionId);
            ArrayList<String> preparedUris=new ArrayList<>();for(PreparedBatch.Input in:batch.inputs)preparedUris.add(in.uri.toString());
            session.requireMatches(preparedUris);
            runner.setChapterOutcome(new ChapterRunOutcome(batch.inputs.size()));
        }
        // The shared settings (and the Active Glossary/Pronoun) are saved once and never replaced by a chapter's own copy.
        SettingsStore.save(this,ownerSettings);runner.setState(TranslationJobState.State.READY,"Ready");runner.publish("ready",0,"Validated prepared plan "+batch.id+" with "+batch.exactChunkCount()+" chunks");runner.setState(TranslationJobState.State.RUNNING,"Running");
        final java.util.Map<Integer,Long> resumeJobs=parseResumeJobs(intent.getStringExtra("resumeJobs"));
        if(session!=null&&!resumeJobs.isEmpty()){
            // The Jobs tab groups a run's chapters from this snapshot: remember which earlier jobs this run continues.
            for(Long id:resumeJobs.values())if(!session.resumedJobs.contains(id))session.resumedJobs.add(id);
            try{session.write(new java.io.File(getFilesDir(),ChapterSession.DIR));}catch(Exception ignored){}
        }
        for(int i=0;i<batch.inputs.size();i++){
            runner.waitIfPaused();if(runner.isCancelled()){finishCancelled("Cancelled before the next prepared file");return;}
            PreparedBatch.Input input=batch.inputs.get(i);Uri out=fixed;
            if(out==null||batch.inputs.size()>1)out=deferredOutput(tree,s.buildOutputName(input.displayName));
            ChapterRunOutcome outcome=runner.getChapterOutcome();
            final Long resumeId=resumeJobs.get(input.ordinal);
            final int fileNo=i+1,fileTotal=batch.inputs.size();
            if(outcome==null){
                if(resumeId!=null)runner.continueExistingJob(resumeId,fileNo,fileTotal,input.displayName);
                else runSinglePreparedJob(batch,startSessionId,input,out,s,fileNo,fileTotal,input.displayName);
                continue;
            }
            ChapterBinding binding=session.chapters.get(i);
            runner.setSetIndex(i+1);
            String label="Chương "+(binding.chapterNo>=0?binding.chapterNo:i+1)+" ("+(i+1)+"/"+batch.inputs.size()+") · "+input.displayName;
            try{
                if(resumeId!=null)runner.continueExistingJob(resumeId,fileNo,fileTotal,label);
                else runSinglePreparedJob(batch,startSessionId,input,out,binding.applyTo(s),fileNo,fileTotal,label);
                if(!runner.isCancelled())outcome.done(input.displayName);
            }catch(Exception e){
                if(runner.isCancelled())throw e;
                String here="Chương "+(binding.chapterNo>=0?binding.chapterNo:i+1);
                outcome.failed(input.displayName,AppValidator.readableError(e),here);
                if(ChapterRunOutcome.continuesQueue(e)){
                    if(runner.getActiveJobId()>0)repo.touchJob(runner.getActiveJobId(),"error");
                    if(outcome.shouldStopQueue())throw new QueueStoppedException(outcome.stopSentence());
                    String next=i+1<session.chapters.size()?"chuyển chương "+(session.chapters.get(i+1).chapterNo>=0?session.chapters.get(i+1).chapterNo:i+2):"hết tập";
                    runner.publish("running",runner.percent(i+1,batch.inputs.size()),here+" lỗi, "+next+": "+AppValidator.readableError(e));
                    continue;
                }
                throw e;
            }
        }
        ChapterRunOutcome finished=runner.getChapterOutcome();
        runner.setState(TranslationJobState.State.COMPLETED,"Completed");runner.publish("done",100,finished==null?"Prepared batch translation completed":finished.summary());releaseRun();stopForeground(STOP_FOREGROUND_REMOVE);NotificationManager nm=getSystemService(NotificationManager.class);if(nm!=null)nm.cancel(NOTIFY_ID);stopSelf();
    }

    /** {"<input ordinal>": <job id>}: inputs that continue an unfinished job (its own settings and output) instead of starting a new one. */
    private static java.util.Map<Integer,Long> parseResumeJobs(String json){
        java.util.HashMap<Integer,Long> out=new java.util.HashMap<>();
        if(json==null||json.trim().isEmpty())return out;
        try{org.json.JSONObject o=new org.json.JSONObject(json);java.util.Iterator<String> keys=o.keys();while(keys.hasNext()){String k=keys.next();out.put(Integer.parseInt(k),o.getLong(k));}}catch(Exception ignored){}
        return out;
    }



    private void runSinglePreparedJob(PreparedBatch batch,String startSessionId,PreparedBatch.Input input,Uri outputUri,AppSettings s,int fileNo,int fileTotal,String label)throws Exception{
        List<Chunk> chunks=input.chunks;if(chunks==null||chunks.isEmpty())throw new IllegalStateException("Prepared plan has no chunks for "+input.displayName);
        Chunker.assignParagraphRanges(chunks);
        runner.resetMetrics(input.displayName,chunks.size());runner.setExpectedMetrics(batch.estimate);String settingsHash=HashUtil.settingsHash(s);
        long jobId=repo.createJobFromPrepared(batch.id,startSessionId,input.ordinal,input.uri.toString(),outputUri.toString(),input.displayName,s,chunks,input.inputHash,settingsHash);runner.setActiveJobId(jobId);repo.touchJob(jobId,"RUNNING");runner.setMetricPhase("Translating");
        ObservabilityLog.event("prepared_plan_claimed","batch",batch.id,"job",jobId,"inputOrdinal",input.ordinal,"chunks",chunks.size());
        runner.publish("running",runner.percent(fileNo-1,fileTotal),"Starting exact prepared plan: "+chunks.size()+" chunks");runner.translateJob(jobId,outputUri.toString(),s,chunks,fileNo,fileTotal,label);
    }



    private void runSingleJob(Uri inputUri, Uri outputUri, AppSettings s, int fileNo, int fileTotal) throws Exception {
        TranslationEngine.PreparedInput prepared = engine.prepareInput(inputUri, s);
        String fileName = prepared.fileName;
        List<Chunk> chunks = prepared.chunks;
        runner.resetMetrics(fileName, chunks.size());
        // ACTION_START always owns a clean job. Checkpoint reuse is available only through explicit Resume actions.
        TranslationRepository.Job previousDone = null;
        if (previousDone != null) {
            String oldOut = repo.assembleOutput(previousDone.id, s.bilingualOutput);
            FileUtil.writeText(this, outputUri, oldOut);
            runner.resetMetrics(fileName, chunks.size());
            runner.setMetricCompleted(chunks.size());
            runner.setMetricPhase("Already completed");
            runner.getAcceptedPreview().accept(oldOut, chunks.size(), chunks.size());
            runner.publish("done", runner.percent(fileNo, fileTotal), "File này đã dịch xong với cùng nội dung + settings. App đã xuất lại output từ checkpoint, không gọi API/dịch lại: " + fileName);
            updateNotification("Đã có bản dịch, không dịch lại " + fileName, chunks.size(), chunks.size());
            return;
        }
        long jobId = repo.createJob(inputUri.toString(), outputUri.toString(), fileName, s, chunks, prepared.inputHash, prepared.settingsHash);
        runner.setActiveJobId(jobId);
        repo.touchJob(jobId, "RUNNING");
        runner.setMetricPhase("Translating");
        runner.publish("running", runner.percent(fileNo - 1, fileTotal), "Đã chia " + fileName + " thành " + chunks.size() + " chunk");
        runner.translateJob(jobId, outputUri.toString(), s, chunks, fileNo, fileTotal, fileName);
    }
















    private static Uri deferredOutput(Uri tree,String name){
        if(tree==null)throw new IllegalArgumentException("Output folder is missing");
        // Keep the tree URI as a single query parameter.  The former implementation
        // encoded it into the scheme-specific part and decoded it twice on retrieval,
        // corrupting document ids such as primary%3ADownload%2FP0Output.
        return new Uri.Builder().scheme("tbl-pending-output").authority("output")
                .appendQueryParameter("tree",tree.toString())
                .appendQueryParameter("name",name==null?"translation.txt":name).build();
    }

    Uri materializeOutput(long jobId,Uri output) throws Exception {
        if(jobId>0){TranslationRepository.Job job=repo.getJob(jobId);if(job!=null&&job.outputUri!=null&&!job.outputUri.isEmpty()){Uri stored=Uri.parse(job.outputUri);if(!"tbl-pending-output".equals(stored.getScheme()))return stored;output=stored;}}
        if(output==null)throw new IllegalArgumentException("Output destination is missing");
        if(!"tbl-pending-output".equals(output.getScheme()))return output;
        String treeValue=output.getQueryParameter("tree");
        String name=output.getQueryParameter("name");
        if(treeValue==null||treeValue.trim().isEmpty())throw new IllegalArgumentException("Deferred output destination is invalid");
        Uri tree=Uri.parse(treeValue);
        java.io.File testDir=TranslateTestSeam.outputDir();
        Uri actual;
        if(testDir!=null){testDir.mkdirs();java.io.File target=new java.io.File(testDir,FileUtil.sanitizeOutputName(name));int n=2;while(target.exists()){String base=FileUtil.sanitizeOutputName(name);target=new java.io.File(testDir,base.substring(0,base.length()-4)+"_v"+(n++)+".txt");}actual=Uri.fromFile(target);}
        else actual=FileUtil.createOutputInTree(this,tree,name==null?"translation.txt":name);
        LogStore.append(this,"OUTPUT_MATERIALIZED tree="+tree+", file="+actual);
        if(jobId>0)repo.updateJobOutputUri(jobId,actual.toString());
        return actual;
    }

    private void validateOutputAccess(int inputCount, Uri fixedOutput, Uri outputTree) {
        if (TranslateTestSeam.outputDir() != null) return;
        String error;
        if (inputCount > 1 || fixedOutput == null) error = FileUtil.validateTreeWritable(this, outputTree, "Output folder");
        else error = FileUtil.validateWritable(this, fixedOutput, "Single output TXT");
        if (error != null) throw new IllegalStateException(error);
        LogStore.append(this,"OUTPUT_PREFLIGHT_OK uri="+(outputTree!=null?outputTree:fixedOutput)+", inputCount="+inputCount);
    }


























    private void createChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Translation", NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(ch);
    }

    private Notification buildNotification(String text, int done, int total) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        boolean ongoing = total == 0 || done < total;
        Notification.Builder b = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Translate Books with LLMs")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentIntent(pi)
                .setOnlyAlertOnce(true)
                .setAutoCancel(!ongoing)
                .setOngoing(ongoing);
        if (total > 0) b.setProgress(total, done, false);
        return b.build();
    }

    void updateNotification(String text, int done, int total) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIFY_ID, buildNotification(text, done, total));
    }
}
