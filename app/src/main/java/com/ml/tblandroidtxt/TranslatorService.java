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

    private static class CostLimitReachedException extends RuntimeException {
        CostLimitReachedException(String message) { super(message); }
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
    private static final int NOTIFY_ID = 1001;
    private static final AtomicBoolean ACTIVE = new AtomicBoolean(false);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean paused = false;
    private volatile boolean cancelled = false;
    private TranslationRepository repo;
    private TranslationEngine engine;
    /** Chapter-set runs only: what happened to each chapter so far; null otherwise. */
    private volatile ChapterRunOutcome chapterOutcome;
    private final RunCostMeter runCost = new RunCostMeter();
    private final RootChunkAttemptBudget rootAttemptBudget = new RootChunkAttemptBudget(6);
    private final Map<String, String> rootByChunk = new HashMap<>();
    private volatile long sessionStartMs;
    private volatile int setIndex;

    /** A new run starts counting tokens, cost and time from zero. */
    private void resetSession() { runCost.reset(); sessionStartMs = System.currentTimeMillis(); setIndex = 0; }

    private long jobStartMs = 0;
    private int metricTotal = 0;
    private int metricCompleted = 0;
    private int metricFailed = 0;
    private int metricFallbacks = 0;
    private int metricTokens = 0;
    private double metricCost = 0;
    private int metricEstimatedTokens = 0;
    private double metricEstimatedCost = 0;
    private int metricUsageResponses = 0;
    private boolean metricAllUsageReported = true;
    private final AcceptedTranslationPreview acceptedPreview = new AcceptedTranslationPreview();
    private String metricFile = "";
    private String metricPhase = "Preparing";
    private int metricCurrentChunk = 0;
    private int metricGlossaryLocks = -1;
    private int metricPronounLocks = -1;
    private int metricLockChunk = 0;
    private String metricLockPhase = "";
    private int metricLastProgress = -1;
    private String metricLastLog = "";
    private volatile long activeJobId = -1L;
    private volatile boolean benchmarkRun = false;
    private volatile TranslationJobState.State jobState = TranslationJobState.State.IDLE;

    public static boolean isActive() { return ACTIVE.get(); }

    @Override public void onCreate() {
        super.onCreate();
        ObservabilityLog.initialize(this);
        try { PersistedSettingsCleanup.runOnce(getApplicationContext()); }
        catch (Exception ignored) { Log.e("TranslateBooks", "settings_cleanup_failed"); }
        createChannel();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (!ACTIVE.get()) executor.shutdownNow();
        if(repo!=null)repo.close();
    }

    @Override public void onTimeout(int startId,int foregroundServiceType){cancelled=true;paused=false;OpenAICompatibleClient.cancelActiveRequests();if(repo!=null&&activeJobId>0)repo.touchJob(activeJobId,"paused");metricPhase="System timeout";broadcast("paused",-1,"Android stopped the data-sync service; progress was checkpointed and can be resumed safely");releaseRun();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(startId);}

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_PAUSE.equals(action)) {
            if (!ACTIVE.get()) {
                broadcast("paused", -1, "Không có job đang chạy để tạm dừng");
                return START_NOT_STICKY;
            }
            paused = true;
            repoPauseLast();
            setState(TranslationJobState.State.PAUSED, "Paused");
            broadcast("paused", -1, "Đã tạm dừng dịch");
            updateNotification("Tạm dừng", metricCompleted, metricTotal);
            return START_NOT_STICKY;
        }
        if (ACTION_RESUME.equals(action)) {
            if (!ACTIVE.get()) {
                ObservabilityLog.event("resume_decision", "action", "RESUME", "branch", "no_running_job");
                broadcast("info", -1, "Không có job nào đang tạm dừng. Job đang dở (nếu có) nằm ở thẻ \"Job đang dở\" trên màn Dịch hoặc ở tab Jobs: bấm \"Tiếp tục dịch\".");
                return START_NOT_STICKY;
            }
            paused = false;
            setState(TranslationJobState.State.RUNNING, "Running");
            broadcast("running", -1, "Tiếp tục dịch");
            return START_NOT_STICKY;
        }
        if (ACTION_CANCEL.equals(action)) {
            OpenAICompatibleClient.cancelActiveRequests();
            if (!ACTIVE.get()) {
                broadcast("cancelled", -1, "Không có job đang chạy để hủy");
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                return START_NOT_STICKY;
            }
            cancelled = true;
            paused = false;
            metricPhase = "Stopping";
            if (activeJobId > 0 && !benchmarkRun) repo.touchJob(activeJobId, "cancelled");
            broadcast("stopping", -1, "Đang dừng dịch. App sẽ không tạo job mới trong lúc request hiện tại chưa thoát.");
            updateNotification("Đang dừng dịch...", metricCompleted, metricTotal);
            return START_NOT_STICKY;
        }
        if (ACTION_START.equals(action)) {
            if (!acquireRun("Start")) return START_NOT_STICKY;
            cancelled = false; paused = false; activeJobId = -1L; resetSession();
            resetMetrics("", 0);
            metricEstimatedTokens = Math.max(0, intent.getIntExtra(EXTRA_ESTIMATED_TOTAL_TOKENS, 0));
            metricEstimatedCost = Math.max(0, intent.getDoubleExtra(EXTRA_ESTIMATED_TOTAL_COST, 0));
            setState(TranslationJobState.State.VALIDATING, "Validating");
            broadcast("validating", 0, "Validating a fresh translation session");
            if(!promote("Preparing translation",0,0)){releaseRun();return START_NOT_STICKY;}
            executor.submit(() -> { try { if(ensureRuntime())runNewJobs(intent); } finally { releaseRun(); } });
            return START_NOT_STICKY;
        }
        if (ACTION_RESUME_LAST.equals(action)) {
            if (!acquireRun("Resume checkpoint")) return START_NOT_STICKY;
            cancelled = false; paused = false; activeJobId = -1L; resetSession();
            resetMetrics("",0);setState(TranslationJobState.State.VALIDATING,"Validating resume");
            if(!promote("Resuming checkpoint",0,0)){releaseRun();return START_NOT_STICKY;}
            executor.submit(() -> { try { if(ensureRuntime())resumeLastJob(); } finally { releaseRun(); } });
            return START_NOT_STICKY;
        }
        if (ACTION_RESUME_JOB.equals(action)) {
            final long jobId = intent.getLongExtra("jobId", -1L);
            if (!acquireRun("Resume selected job")) return START_NOT_STICKY;
            cancelled = false; paused = false; activeJobId = -1L; resetSession();
            resetMetrics("",0);setState(TranslationJobState.State.VALIDATING,"Validating resume");
            if(!promote("Resuming job #"+jobId,0,0)){releaseRun();return START_NOT_STICKY;}
            executor.submit(() -> { try { if(ensureRuntime())resumeJobById(jobId); } finally { releaseRun(); } });
            return START_NOT_STICKY;
        }
        if (ACTION_RETRY_FAILED.equals(action)) {
            if (!acquireRun("Retry failed chunks")) return START_NOT_STICKY;
            cancelled = false; paused = false; activeJobId = -1L; resetSession();
            resetMetrics("",0);setState(TranslationJobState.State.VALIDATING,"Validating retry");
            if(!promote("Retrying failed chunks",0,0)){releaseRun();return START_NOT_STICKY;}
            executor.submit(() -> { try { if(ensureRuntime())retryFailedChunksOnly(); } finally { releaseRun(); } });
            return START_NOT_STICKY;
        }
        if (ACTION_RETRY_JOB_FAILED.equals(action)) {
            final long jobId = intent.getLongExtra("jobId", -1L);
            if (!acquireRun("Retry selected job")) return START_NOT_STICKY;
            cancelled = false; paused = false; activeJobId = -1L; resetSession();
            resetMetrics("",0);setState(TranslationJobState.State.VALIDATING,"Validating retry");
            if(!promote("Retrying job #"+jobId,0,0)){releaseRun();return START_NOT_STICKY;}
            executor.submit(() -> { try { if(ensureRuntime())retryFailedChunksForJob(jobId); } finally { releaseRun(); } });
            return START_NOT_STICKY;
        }
        if(ACTION_RETRY_CHUNK.equals(action)){final long jobId=intent.getLongExtra("jobId",-1L);final int chunkIdx=intent.getIntExtra("chunkIdx",-1);final String edited=intent.getStringExtra("settings");final boolean confirmedUnknown=intent.getBooleanExtra("confirmDeliveryUnknown",false);if(!acquireRun("Retry/continue chunk"))return START_NOT_STICKY;cancelled=false;paused=false;activeJobId=-1L;if(!promote("Retry/continue chunk "+(chunkIdx+1),0,0)){releaseRun();return START_NOT_STICKY;}executor.submit(()->{try{if(!ensureRuntime())return;if("DELIVERY_UNKNOWN".equalsIgnoreCase(repo.chunkStatus(jobId,chunkIdx))&&!confirmedUnknown){broadcast("paused",-1,"Retry blocked: delivery is unknown and explicit duplicate-charge confirmation is required");stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return;}if(edited!=null&&!edited.isEmpty())repo.applyConfigRevision(jobId,chunkIdx,SettingsStore.withCurrentSecrets(this,SettingsStore.fromJson(edited)));retryFailedChunksForJob(jobId,chunkIdx);}finally{releaseRun();}});return START_NOT_STICKY;}
        if (ACTION_BENCHMARK_JOB.equals(action)) {
            final long jobId=intent.getLongExtra("jobId",-1L); final double budget=intent.getDoubleExtra("budgetUsd",0);
            if(!acquireRun("Live A/B benchmark"))return START_NOT_STICKY;
            cancelled=false;paused=false;activeJobId=jobId;benchmarkRun=true;
            if(!promote("Benchmarking job #"+jobId,0,0)){releaseRun();return START_NOT_STICKY;}
            executor.submit(()->{try{if(ensureRuntime())runLiveBenchmark(jobId,budget);}finally{benchmarkRun=false;releaseRun();}});
            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    private synchronized boolean ensureRuntime(){if(repo!=null&&engine!=null)return true;try{PersistedSettingsCleanup.runOnce(getApplicationContext());ModelCatalog.initialize(this);repo=new TranslationRepository(this);repo.recoverInterruptedState();engine=new TranslationEngine(this,repo,TranslateTestSeam.provider());engine.setSessionCostSupplier(runCost::cost);return true;}catch(Exception e){setState(TranslationJobState.State.FAILED,"Initialization failure");logFailure("runtime initialization",e);broadcast("error",-1,"Translation could not initialize: "+AppValidator.readableError(e));stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return false;}}

    private boolean promote(String text,int done,int total){try{startForeground(NOTIFY_ID,buildNotification(text,done,total));return true;}catch(RuntimeException e){setState(TranslationJobState.State.FAILED,"Foreground service error");logFailure("foreground service promotion",e);broadcast("error",-1,"Android could not start the translation service: "+AppValidator.readableError(e));stopSelf();return false;}}

    private boolean acquireRun(String label) {
        if (ACTIVE.compareAndSet(false, true)) {
            acceptedPreview.beginRun();
            return true;
        }
        metricPhase = "Already running";
        broadcast("running", -1, label + " bị chặn: đang có job dịch khác chạy. Không tạo job mới để tránh dịch lặp/tốn API.");
        return false;
    }

    private void releaseRun() {
        ACTIVE.set(false);
        activeJobId = -1L;
    }

    private void runLiveBenchmark(long jobId,double budgetUsd){
        try{
            metricPhase="A/B Benchmark";broadcast("running",0,"Starting paid Full/Balanced benchmark for job #"+jobId);
            LiveBenchmarkRunner.Result result=LiveBenchmarkRunner.run(this,repo,jobId,budgetUsd,()->cancelled,msg->broadcast("running",-1,msg));
            metricPhase="Benchmark completed";broadcast("done",100,"Benchmark #"+result.sessionId+" completed: "+result.requests+" requests, cost "+CostEstimator.money(result.spentUsd));
        }catch(Exception e){metricPhase="Benchmark error";broadcast(cancelled?"cancelled":"error",-1,"Benchmark stopped: "+AppValidator.readableError(e));}
        finally{stopForeground(STOP_FOREGROUND_REMOVE);NotificationManager nm=getSystemService(NotificationManager.class);if(nm!=null)nm.cancel(NOTIFY_ID);stopSelf();}
    }

    private void finishCancelled(String message) {
        metricPhase = "Cancelled";
        if (activeJobId > 0) repo.touchJob(activeJobId, "cancelled");
        broadcast("cancelled", -1, message == null || message.isEmpty() ? "Đã dừng dịch" : message);
        releaseRun();
        stopForeground(STOP_FOREGROUND_REMOVE);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(NOTIFY_ID);
        stopSelf();
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
            s = applyEffectiveCostLimit(s, intent);
            String validation = AppValidator.validateForTranslation(s);
            if (validation != null) throw new IllegalArgumentException(validation);
            SettingsStore.save(this, ownerSettings);

            setState(TranslationJobState.State.READY, "Ready");
            broadcast("ready", 0, "Input, configuration and output access are valid");
            setState(TranslationJobState.State.RUNNING, "Running");
            broadcast("running", 0, "Batch of " + inputs.size() + " file(s) initiated.");
            for (int f = 0; f < inputs.size(); f++) {
                waitIfPaused();
                if (cancelled) { finishCancelled("Đã hủy batch trước khi chạy file tiếp theo"); return; }
                Uri inputUri = inputs.get(f);
                String fileName = FileUtil.displayName(this, inputUri);
                Uri outUri = fixedOutput;
                if (outUri == null || inputs.size() > 1) outUri = FileUtil.createOutputInTree(this, outputTree, s.buildOutputName(fileName));
                metricPhase = "Translating";
                broadcast("running", Math.round(f * 100f / inputs.size()), "Starting translation for: " + fileName);
                runSingleJob(inputUri, outUri, s, f + 1, inputs.size());
                if (cancelled) { finishCancelled("Đã hủy batch dịch"); return; }
            }
            setState(TranslationJobState.State.COMPLETED, "Completed");
            broadcast("done", 100, "Batch translation completed for all files");
            releaseRun();
            stopForeground(STOP_FOREGROUND_REMOVE);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(NOTIFY_ID);
            stopSelf();
        } catch (CostLimitReachedException e) {
            setState(TranslationJobState.State.PAUSED,"Paused");
            if (activeJobId > 0) repo.touchJob(activeJobId, "paused");
            broadcast("paused", -1, AppValidator.readableError(e) + chapterSummarySuffix());
            releaseRun();
            stopForeground(STOP_FOREGROUND_REMOVE);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(NOTIFY_ID);
            stopSelf();
        } catch (Exception e) {
            if (cancelled) { finishCancelled("Đã hủy dịch"); return; }
            setState(TranslationJobState.State.FAILED, activeJobId > 0 ? "Translation failed" : "Initialization failure");
            if (activeJobId > 0) repo.touchJob(activeJobId, "error");
            logFailure(metricPhase, e);
            if (e instanceof QueueStoppedException) broadcast("error", -1, e.getMessage() + chapterSummarySuffix());
            else broadcast("error", -1, (activeJobId > 0 ? "Lỗi: " : "Lỗi khởi tạo: ") + AppValidator.readableError(e) + remainingSuffix() + chapterSummarySuffix());
            releaseRun();
            stopForeground(STOP_FOREGROUND_REMOVE);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(NOTIFY_ID);
            stopSelf();
        }
    }

    /** " — x xong · y lỗi (…) · z chưa chạy" after a chapter-set run stopped; empty for every other run. */
    private String chapterSummarySuffix(){ChapterRunOutcome o=chapterOutcome;return o==null?"":"\n"+o.summary();}

    private void runPreparedJobs(Intent intent,String batchId) throws Exception {
        PreparedBatch batch=repo.getPreparedBatch(batchId);
        if(batch==null||!batch.ready())throw new IllegalStateException("Prepared chunk plan is missing or stale; select the TXT again");
        String startSessionId=intent.getStringExtra("startSessionId");
        if(startSessionId==null||startSessionId.trim().isEmpty())throw new IllegalArgumentException("Fresh Start is missing its session identity");
        String supplied=intent.getStringExtra("settings");
        AppSettings s=AppValidator.normalize(supplied==null||supplied.trim().isEmpty()?SettingsStore.withCurrentSecrets(this,SettingsStore.fromJson(batch.settingsJson)):SettingsStore.withCurrentSecrets(this,SettingsStore.fromJson(supplied)));
        AppSettings ownerSettings = s.copy();
        s = applyEffectiveCostLimit(s, intent);
        String validation=AppValidator.validateForTranslation(s);if(validation!=null)throw new IllegalArgumentException(validation);
        String outputUriStr=intent.getStringExtra("outputUri"),treeStr=intent.getStringExtra("outputTreeUri");
        Uri fixed=outputUriStr==null||outputUriStr.isEmpty()?null:Uri.parse(outputUriStr);Uri tree=treeStr==null||treeStr.isEmpty()?null:Uri.parse(treeStr);
        if(batch.inputs.size()>1&&tree==null)throw new IllegalArgumentException("Batch mode requires an output folder");
        if(batch.inputs.size()==1&&fixed==null&&tree==null)throw new IllegalArgumentException("Choose an output TXT or folder");
        for(PreparedBatch.Input input:batch.inputs){String error=FileUtil.validateReadable(this,input.uri,"Input "+(input.ordinal+1));if(error!=null)throw new IllegalStateException(error);}
        validateOutputAccess(batch.inputs.size(),fixed,tree);
        chapterOutcome=null;
        final boolean chapterMode=intent.getBooleanExtra("chapterMode",false);
        ChapterSession session=null;
        if(chapterMode){
            session=ChapterSession.read(new java.io.File(getFilesDir(),ChapterSession.DIR),startSessionId);
            ArrayList<String> preparedUris=new ArrayList<>();for(PreparedBatch.Input in:batch.inputs)preparedUris.add(in.uri.toString());
            session.requireMatches(preparedUris);
            chapterOutcome=new ChapterRunOutcome(batch.inputs.size());
        }
        // The shared settings (and the Active Glossary/Pronoun) are saved once and never replaced by a chapter's own copy.
        SettingsStore.save(this,ownerSettings);setState(TranslationJobState.State.READY,"Ready");broadcast("ready",0,"Validated prepared plan "+batch.id+" with "+batch.exactChunkCount()+" chunks");setState(TranslationJobState.State.RUNNING,"Running");
        final java.util.Map<Integer,Long> resumeJobs=parseResumeJobs(intent.getStringExtra("resumeJobs"));
        if(session!=null&&!resumeJobs.isEmpty()){
            // The Jobs tab groups a run's chapters from this snapshot: remember which earlier jobs this run continues.
            for(Long id:resumeJobs.values())if(!session.resumedJobs.contains(id))session.resumedJobs.add(id);
            try{session.write(new java.io.File(getFilesDir(),ChapterSession.DIR));}catch(Exception ignored){}
        }
        for(int i=0;i<batch.inputs.size();i++){
            waitIfPaused();if(cancelled){finishCancelled("Cancelled before the next prepared file");return;}
            PreparedBatch.Input input=batch.inputs.get(i);Uri out=fixed;
            if(out==null||batch.inputs.size()>1)out=deferredOutput(tree,s.buildOutputName(input.displayName));
            ChapterRunOutcome outcome=chapterOutcome;
            final Long resumeId=resumeJobs.get(input.ordinal);
            final int fileNo=i+1,fileTotal=batch.inputs.size();
            if(outcome==null){
                if(resumeId!=null)continueExistingJob(resumeId,fileNo,fileTotal,input.displayName);
                else runSinglePreparedJob(batch,startSessionId,input,out,s,fileNo,fileTotal,input.displayName);
                continue;
            }
            ChapterBinding binding=session.chapters.get(i);
            setIndex=i+1;
            String label="Chương "+(binding.chapterNo>=0?binding.chapterNo:i+1)+" ("+(i+1)+"/"+batch.inputs.size()+") · "+input.displayName;
            try{
                if(resumeId!=null)continueExistingJob(resumeId,fileNo,fileTotal,label);
                else runSinglePreparedJob(batch,startSessionId,input,out,binding.applyTo(s),fileNo,fileTotal,label);
                if(!cancelled)outcome.done(input.displayName);
            }catch(Exception e){
                if(cancelled)throw e;
                String here="Chương "+(binding.chapterNo>=0?binding.chapterNo:i+1);
                outcome.failed(input.displayName,AppValidator.readableError(e),here);
                if(ChapterRunOutcome.continuesQueue(e)){
                    if(activeJobId>0)repo.touchJob(activeJobId,"error");
                    if(outcome.shouldStopQueue())throw new QueueStoppedException(outcome.stopSentence());
                    String next=i+1<session.chapters.size()?"chuyển chương "+(session.chapters.get(i+1).chapterNo>=0?session.chapters.get(i+1).chapterNo:i+2):"hết tập";
                    broadcast("running",percent(i+1,batch.inputs.size()),here+" lỗi, "+next+": "+AppValidator.readableError(e));
                    continue;
                }
                throw e;
            }
        }
        ChapterRunOutcome finished=chapterOutcome;
        setState(TranslationJobState.State.COMPLETED,"Completed");broadcast("done",100,finished==null?"Prepared batch translation completed":finished.summary());releaseRun();stopForeground(STOP_FOREGROUND_REMOVE);NotificationManager nm=getSystemService(NotificationManager.class);if(nm!=null)nm.cancel(NOTIFY_ID);stopSelf();
    }

    /** {"<input ordinal>": <job id>}: inputs that continue an unfinished job (its own settings and output) instead of starting a new one. */
    private static java.util.Map<Integer,Long> parseResumeJobs(String json){
        java.util.HashMap<Integer,Long> out=new java.util.HashMap<>();
        if(json==null||json.trim().isEmpty())return out;
        try{org.json.JSONObject o=new org.json.JSONObject(json);java.util.Iterator<String> keys=o.keys();while(keys.hasNext()){String k=keys.next();out.put(Integer.parseInt(k),o.getLong(k));}}catch(Exception ignored){}
        return out;
    }

    /** Resume compares the stored hash with the file as written (raw) or as the plan normalized it; the matching branch is logged. */
    private boolean inputStillMatches(long jobId,String expectedHash,Uri inputUri)throws Exception{
        InputIdentity.Match m=InputIdentity.match(expectedHash,FileUtil.readText(this,inputUri));
        ObservabilityLog.event("input_identity","job",jobId,"match",m.name());
        return m!=InputIdentity.Match.NONE;
    }

    /** Continues an unfinished job inside a prepared run: translated chunks are kept, the rest (including failed ones) is translated with the job's own settings. */
    private void continueExistingJob(long jobId,int fileNo,int fileTotal,String label)throws Exception{
        TranslationRepository.Job job=repo.getJob(jobId);
        if(job==null)throw new IllegalStateException("Không thấy job #"+jobId+" để tiếp tục");
        AppSettings s=applyEffectiveCostLimit(AppValidator.normalize(SettingsStore.withCurrentSecrets(this,SettingsStore.fromJson(job.settingsJson))),null);
        String validation=AppValidator.validateForTranslation(s);if(validation!=null)throw new IllegalArgumentException(validation);
        activeJobId=job.id;
        String expectedInputHash=repo.getInputHash(job.id);
        if(expectedInputHash!=null&&!expectedInputHash.isEmpty()&&!inputStillMatches(job.id,expectedInputHash,Uri.parse(job.inputUri))){repo.touchJob(job.id,"INPUT_CONFLICT");throw new IllegalStateException("Input file changed since checkpoint; job #"+job.id+" cannot continue");}
        List<TranslationRepository.ChunkRow> rows=repo.getChunkRows(job.id);
        for(TranslationRepository.ChunkRow row:rows)if("DELIVERY_UNKNOWN".equalsIgnoreCase(row.status)){repo.touchJob(job.id,"NEEDS_REVIEW");throw new IllegalStateException("Job #"+job.id+" có request không rõ đã gửi hay chưa; xem ở tab Jobs rồi chọn retry");}
        List<Chunk> chunks=new ArrayList<>();int completed=0,failed=0,pending=0;
        for(TranslationRepository.ChunkRow r:rows){chunks.add(chunkFromRow(r));if(isCompletedStatus(r.status))completed++;else if(isFailedStatus(r.status))failed++;else pending++;}
        if(rows.isEmpty())throw new IllegalArgumentException("Job #"+job.id+" không có chunk checkpoint");
        Chunker.assignParagraphRanges(chunks);
        ObservabilityLog.event("resume_decision","job",job.id,"action","START_CONTINUE","branch",completed>=rows.size()?"already_translated":"continue","done",completed,"error",failed,"pending",pending);
        if(completed>=rows.size()){updatePartialOutput(job.id,Uri.parse(job.outputUri),s);repo.finishJob(job.id);return;}
        repo.touchJob(job.id,"running");
        try{runCost.preloadContinuedJob(job.id,repo.getMetricsSummary(job.id).totalCost);}catch(Exception ignored){}
        resetMetrics(label,rows.size());metricCompleted=completed;metricFailed=failed;metricPhase="Translating";
        broadcast("running",percent(fileNo-1,fileTotal),"Tiếp tục job #"+job.id+": "+label+" (xong "+completed+", lỗi "+failed+", chưa dịch "+pending+")");
        translateJob(job.id,Uri.parse(job.outputUri),s,chunks,fileNo,fileTotal,label);
    }

    private void runSinglePreparedJob(PreparedBatch batch,String startSessionId,PreparedBatch.Input input,Uri outputUri,AppSettings s,int fileNo,int fileTotal,String label)throws Exception{
        List<Chunk> chunks=input.chunks;if(chunks==null||chunks.isEmpty())throw new IllegalStateException("Prepared plan has no chunks for "+input.displayName);
        Chunker.assignParagraphRanges(chunks);
        resetMetrics(input.displayName,chunks.size());setExpectedMetrics(batch.estimate);String settingsHash=HashUtil.settingsHash(s);
        long jobId=repo.createJobFromPrepared(batch.id,startSessionId,input.ordinal,input.uri.toString(),outputUri.toString(),input.displayName,s,chunks,input.inputHash,settingsHash);activeJobId=jobId;repo.touchJob(jobId,"RUNNING");metricPhase="Translating";
        ObservabilityLog.event("prepared_plan_claimed","batch",batch.id,"job",jobId,"inputOrdinal",input.ordinal,"chunks",chunks.size());
        broadcast("running",percent(fileNo-1,fileTotal),"Starting exact prepared plan: "+chunks.size()+" chunks");translateJob(jobId,outputUri,s,chunks,fileNo,fileTotal,label);
    }

    private void resetMetrics(String fileName, int total) {
        jobStartMs = System.currentTimeMillis();
        metricTotal = total;
        metricCompleted = 0;
        metricFailed = 0;
        metricFallbacks = 0;
        metricTokens = 0;
        metricCost = 0;
        metricEstimatedTokens = 0;
        metricEstimatedCost = 0;
        metricUsageResponses = 0;
        metricAllUsageReported = true;
        acceptedPreview.beginFile(fileName, total);
        metricFile = fileName == null ? "" : fileName;
        metricPhase = "Preparing";
        metricCurrentChunk = 0;
        metricGlossaryLocks = -1;
        metricPronounLocks = -1;
        metricLockChunk = 0;
        metricLockPhase = "";
        metricLastProgress = -1;
        metricLastLog = "";
        rootAttemptBudget.reset();
        rootByChunk.clear();
    }

    private void setExpectedMetrics(CostEstimator.Estimate estimate) {
        if (estimate == null) return;
        metricEstimatedTokens = Math.max(0, estimate.totalTokensHigh);
        metricEstimatedCost = estimate.pricingAvailable && !Double.isNaN(estimate.costHigh) ? Math.max(0, estimate.costHigh) : 0;
    }

    private void runSingleJob(Uri inputUri, Uri outputUri, AppSettings s, int fileNo, int fileTotal) throws Exception {
        TranslationEngine.PreparedInput prepared = engine.prepareInput(inputUri, s);
        String fileName = prepared.fileName;
        List<Chunk> chunks = prepared.chunks;
        resetMetrics(fileName, chunks.size());
        // ACTION_START always owns a clean job. Checkpoint reuse is available only through explicit Resume actions.
        TranslationRepository.Job previousDone = null;
        if (previousDone != null) {
            String oldOut = repo.assembleOutput(previousDone.id, s.bilingualOutput);
            FileUtil.writeText(this, outputUri, oldOut);
            resetMetrics(fileName, chunks.size());
            metricCompleted = chunks.size();
            metricPhase = "Already completed";
            acceptedPreview.accept(oldOut, chunks.size(), chunks.size());
            broadcast("done", percent(fileNo, fileTotal), "File này đã dịch xong với cùng nội dung + settings. App đã xuất lại output từ checkpoint, không gọi API/dịch lại: " + fileName);
            updateNotification("Đã có bản dịch, không dịch lại " + fileName, chunks.size(), chunks.size());
            return;
        }
        long jobId = repo.createJob(inputUri.toString(), outputUri.toString(), fileName, s, chunks, prepared.inputHash, prepared.settingsHash);
        activeJobId = jobId;
        repo.touchJob(jobId, "RUNNING");
        metricPhase = "Translating";
        broadcast("running", percent(fileNo - 1, fileTotal), "Đã chia " + fileName + " thành " + chunks.size() + " chunk");
        translateJob(jobId, outputUri, s, chunks, fileNo, fileTotal, fileName);
    }

    private void resumeLastJob() {
        resumeJobById(-1L);
    }

    private void resumeJobById(long selectedJobId) {
        try {
            TranslationRepository.Job job = selectedJobId > 0 ? repo.getJob(selectedJobId) : repo.getLastIncompleteJob();
            if (job == null) {
                broadcast("error", -1, selectedJobId > 0 ? "Không thấy job #" + selectedJobId : "Không có checkpoint để resume");
                releaseRun();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                return;
            }
            if (isCompletedStatus(job.status)) {
                ObservabilityLog.event("resume_decision", "job", job.id, "action", "RESUME", "branch", "already_completed");
                broadcast("done", 100, "Job #" + job.id + " đã hoàn tất, không cần resume: " + job.fileName);
                releaseRun();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                return;
            }
            AppSettings s = applyEffectiveCostLimit(AppValidator.normalize(SettingsStore.withCurrentSecrets(this,SettingsStore.fromJson(job.settingsJson))),null);
            String validation = AppValidator.validateForTranslation(s);
            if (validation != null) throw new IllegalArgumentException(validation);
            activeJobId = job.id;
            String expectedInputHash=repo.getInputHash(job.id);
            if(expectedInputHash!=null&&!expectedInputHash.isEmpty()){
                if(!inputStillMatches(job.id,expectedInputHash,Uri.parse(job.inputUri))){repo.touchJob(job.id,"INPUT_CONFLICT");throw new IllegalStateException("Input file changed since checkpoint; resume blocked");}
            }
            List<TranslationRepository.ChunkRow> rows = repo.getChunkRows(job.id);
            for(TranslationRepository.ChunkRow row:rows)if("DELIVERY_UNKNOWN".equalsIgnoreCase(row.status)){repo.touchJob(job.id,"NEEDS_REVIEW");broadcast("paused",-1,"Job #"+job.id+" has a request with unknown delivery. Review it before choosing retry anyway; it will not be resent automatically.");releaseRun();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return;}
            for(TranslationRepository.ChunkRow r:rows){if("RESPONSE_RECEIVED".equalsIgnoreCase(r.status)&&r.persistedResponse!=null&&!r.persistedResponse.isEmpty()){try{String recovered=PromptBuilder.extractTranslationStrict(r.persistedResponse);ResponseValidator.Result check=ResponseValidator.validate(r.persistedResponse,recovered,r.source,"",r.finishReason,Chunker.approxTokens(recovered),s.maxOutputTokens,false);if(check.accepted){ChunkQa.Metrics qa=ChunkQa.inspect(r.source,recovered,"");repo.commitValidatedChunk(job.id,r.index,recovered,String.join(",",qa.warnings),"PROVIDER_RECOVERED");r.translated=recovered;r.status="COMPLETED";}else{repo.rejectResponse(job.id,r.index,r.attempt,r.persistedResponse,check.summary(),true);r.status="RETRYABLE_ERROR";r.error=check.summary();}}catch(Exception validationError){repo.rejectResponse(job.id,r.index,r.attempt,r.persistedResponse,validationError.getMessage(),true);r.status="RETRYABLE_ERROR";r.error=validationError.getMessage();}}}
            List<Chunk> chunks = new ArrayList<>();
            int completed = 0, failed = 0, pending = 0;
            for (TranslationRepository.ChunkRow r : rows) {
                chunks.add(chunkFromRow(r));
                if (isCompletedStatus(r.status)) completed++;
                else if (isFailedStatus(r.status)) failed++;
                else pending++;
            }
            if (rows.isEmpty()) throw new IllegalArgumentException("Job #" + job.id + " không có chunk checkpoint");
            Chunker.assignParagraphRanges(chunks);
            if (completed >= rows.size()) {
                updatePartialOutput(job.id, Uri.parse(job.outputUri), s);
                repo.finishJob(job.id);
                broadcast("done", 100, "Job #" + job.id + " đã có đủ output, đã đánh dấu hoàn tất");
                releaseRun();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                return;
            }
            ObservabilityLog.event("resume_decision", "job", job.id, "action", "RESUME", "branch", "continue", "done", completed, "error", failed, "pending", pending);
            repo.touchJob(job.id, "running");
            resetMetrics(job.fileName, rows.size());
            metricCompleted = completed;
            metricFailed = failed;
            setState(TranslationJobState.State.READY,"Ready to resume");
            setState(TranslationJobState.State.RUNNING,selectedJobId > 0 ? "Resume selected job" : "Resume");
            broadcast("running", percent(completed, Math.max(1, rows.size())), "Resume job #" + job.id + ": " + job.fileName + " (done=" + completed + ", failed=" + failed + ", pending=" + pending + ")");
            translateJob(job.id, Uri.parse(job.outputUri), s, chunks, 1, 1, job.fileName);
            if (cancelled) { finishCancelled("Đã hủy resume checkpoint"); return; }
            setState(TranslationJobState.State.COMPLETED,"Completed");
            broadcast("done", 100, "Resume checkpoint completed: " + job.fileName);
            releaseRun();
            stopForeground(STOP_FOREGROUND_REMOVE);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(NOTIFY_ID);
            stopSelf();
        } catch (CostLimitReachedException e) {
            setState(TranslationJobState.State.PAUSED,"Paused");
            if (activeJobId > 0) repo.touchJob(activeJobId, "paused");
            broadcast("paused", -1, AppValidator.readableError(e));
            releaseRun();
            stopForeground(STOP_FOREGROUND_REMOVE);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(NOTIFY_ID);
            stopSelf();
        } catch (Exception e) {
            if (cancelled) { finishCancelled("Đã hủy resume checkpoint"); return; }
            setState(TranslationJobState.State.FAILED,"Resume error");
            if (activeJobId > 0) repo.touchJob(activeJobId, "error");
            logFailure(metricPhase,e);
            broadcast("error", -1, "Lỗi resume: " + AppValidator.readableError(e) + remainingSuffix());
            releaseRun();
            stopForeground(STOP_FOREGROUND_REMOVE);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(NOTIFY_ID);
            stopSelf();
        }
    }


    private void retryFailedChunksOnly() {
        retryFailedChunksForJob(-1L);
    }

    private void retryFailedChunksForJob(long selectedJobId) {
        retryFailedChunksForJob(selectedJobId,-1);
    }
    private void retryFailedChunksForJob(long selectedJobId,int targetChunk) {
        try {
            TranslationRepository.Job job = selectedJobId > 0 ? repo.getJob(selectedJobId) : repo.getLastJobWithFailedChunks();
            if (job == null) {
                broadcast("error", -1, selectedJobId > 0 ? "Không thấy job #" + selectedJobId : "Không có chunk lỗi để retry");
                releaseRun();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                return;
            }
            AppSettings s = applyEffectiveCostLimit(AppValidator.normalize(SettingsStore.withCurrentSecrets(this,SettingsStore.fromJson(job.settingsJson))),null);
            String validation = AppValidator.validateForTranslation(s);
            if (validation != null) throw new IllegalArgumentException(validation);
            activeJobId = job.id;
            List<TranslationRepository.ChunkRow> rows = repo.getChunkRows(job.id);
            resetMetrics(job.fileName, rows.size());
            int completed = 0, failedTargets = 0;
            for (TranslationRepository.ChunkRow r : rows) {
                if (isCompletedStatus(r.status)) completed++;
                if ((targetChunk>=0&&r.index==targetChunk)||(targetChunk<0&&isFailedStatus(r.status))) failedTargets++;
            }
            if (failedTargets <= 0 && (targetChunk >= 0 || unfinishedCount(rows) == 0)) {
                ObservabilityLog.event("resume_decision", "job", job.id, "action", "RETRY", "branch", "nothing_to_retry", "done", completed, "total", rows.size());
                broadcast("done", percent(completed, Math.max(1, rows.size())), "Job #" + job.id + " không có chunk lỗi để retry, mọi chunk đã dịch xong");
                releaseRun();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                return;
            }
            repo.touchJob(job.id, "running");
            metricCompleted = completed;
            metricFailed = failedTargets;
            setState(TranslationJobState.State.READY,"Ready to retry");
            setState(TranslationJobState.State.RUNNING,selectedJobId > 0 ? "Retry selected job" : "Retry failed only");
            broadcast("running", percent(completed, Math.max(1, rows.size())), "Retry " + failedTargets + " failed chunk(s) for job #" + job.id + ": " + job.fileName);
            retryFailedRows(job.id, Uri.parse(job.outputUri), s, rows, job.fileName,targetChunk);
            updatePartialOutput(job.id, Uri.parse(job.outputUri), s);
            if (cancelled) { finishCancelled("Đã hủy retry failed chunks"); return; }
            List<TranslationRepository.ChunkRow> afterRetry = repo.getChunkRows(job.id);
            int remaining = unfinishedCount(afterRetry);
            ObservabilityLog.event("resume_decision", "job", job.id, "action", targetChunk >= 0 ? "RETRY_CHUNK" : "RETRY_FAILED", "branch", remaining > 0 ? (targetChunk >= 0 ? "paused_with_remaining" : "continue_remaining") : "finish", "remaining", remaining);
            if (remaining > 0 && targetChunk >= 0) {
                // One chunk was retried on request: do not mark the job finished while chunks are still untranslated.
                repo.touchJob(job.id, "paused");
                setState(TranslationJobState.State.PAUSED, "Paused");
                broadcast("paused", -1, "Đã retry chunk " + (targetChunk + 1) + " của job #" + job.id + ". Còn " + remaining + " chunk chưa dịch: bấm \"Tiếp tục dịch\" để dịch nốt.");
                releaseRun();
                stopForeground(STOP_FOREGROUND_REMOVE);
                NotificationManager nmPaused = getSystemService(NotificationManager.class);
                if (nmPaused != null) nmPaused.cancel(NOTIFY_ID);
                stopSelf();
                return;
            }
            if (remaining > 0) {
                // The failed chunks are fixed; the original run would have translated the rest, so do it now in the same run.
                List<Chunk> restChunks = new ArrayList<>();
                for (TranslationRepository.ChunkRow row : afterRetry) restChunks.add(chunkFromRow(row));
                Chunker.assignParagraphRanges(restChunks);
                broadcast("running", percent(metricCompleted, Math.max(1, afterRetry.size())), "Retry xong; dịch tiếp " + remaining + " chunk còn lại của job #" + job.id);
                metricPhase = "Translating";
                translateJob(job.id, Uri.parse(job.outputUri), s, restChunks, 1, 1, job.fileName);
                if (cancelled) { finishCancelled("Đã hủy khi dịch tiếp sau retry"); return; }
            } else {
                repo.finishJob(job.id);
            }
            setState(TranslationJobState.State.COMPLETED,"Completed");
            broadcast("done", 100, "Retry failed chunks completed: " + job.fileName);
            releaseRun();
            stopForeground(STOP_FOREGROUND_REMOVE);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(NOTIFY_ID);
            stopSelf();
        } catch (CostLimitReachedException e) {
            setState(TranslationJobState.State.PAUSED,"Paused");
            if (activeJobId > 0) repo.touchJob(activeJobId, "paused");
            broadcast("paused", -1, AppValidator.readableError(e));
            releaseRun();
            stopForeground(STOP_FOREGROUND_REMOVE);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(NOTIFY_ID);
            stopSelf();
        } catch (Exception e) {
            if (cancelled) { finishCancelled("Đã hủy retry failed chunks"); return; }
            setState(TranslationJobState.State.FAILED,"Retry error");
            if (activeJobId > 0) repo.touchJob(activeJobId, "error");
            logFailure(metricPhase,e);
            broadcast("error", -1, "Lỗi retry failed chunks: " + AppValidator.readableError(e) + remainingSuffix());
            releaseRun();
            stopForeground(STOP_FOREGROUND_REMOVE);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(NOTIFY_ID);
            stopSelf();
        }
    }


    private void retryFailedRows(long jobId, Uri outputUri, AppSettings s, List<TranslationRepository.ChunkRow> rows, String fileName,int targetChunk) throws Exception {
        ArrayList<Chunk> annotated = new ArrayList<>();
        for (TranslationRepository.ChunkRow row : rows) annotated.add(chunkFromRow(row));
        Chunker.assignParagraphRanges(annotated);
        for (TranslationRepository.ChunkRow r : rows) {
            waitIfPaused();
            if (cancelled) return;
            if(targetChunk>=0){if(r.index!=targetChunk)continue;}else if(!isFailedStatus(r.status))continue;
            enforceCostLimit(s);
            String previous = previousBefore(rows, r.index);
            Chunk chunk = findChunk(annotated, r.index);
            metricPhase = "Retry failed only";
            metricCurrentChunk = r.index + 1;
            broadcast("running", percent(metricCompleted, Math.max(1, metricTotal)), "Retry chunk " + (r.index + 1) + "/" + metricTotal + ": " + fileName);
            try {
                String translated = translateWithRetry(chunk, previous, s);
                if (cancelled) return;
                if (s.refineAfter) {
                    translated = refineWithRetry(chunk, translated, s);
                    if (cancelled) return;
                }
                commitAccepted(jobId, chunk, translated, previous);
                metricCompleted++;
                metricFailed = Math.max(0, metricFailed - 1);
                acceptedPreview.accept(translated, r.index + 1, metricTotal);
                if (s.savePartialOutput) updatePartialOutput(jobId, outputUri, s);
                broadcast("running", percent(metricCompleted, Math.max(1, metricTotal)), "Retry chunk " + (r.index + 1) + " hoàn tất");
            } catch (Exception e) {
                if (cancelled) return;
                repo.markChunkError(jobId, r.index, AppValidator.readableError(e));
                broadcast("error", percent(metricCompleted, Math.max(1, metricTotal)), "Retry chunk " + (r.index + 1) + " vẫn lỗi: " + AppValidator.readableError(e));
                throw e;
            }
        }
    }

    private void translateJob(long jobId, Uri outputUri, AppSettings s, List<Chunk> chunks, int fileNo, int fileTotal, String fileName) throws Exception {
        List<TranslationRepository.ChunkRow> rows = repo.getChunkRows(jobId);
        String previous = findPreviousTranslation(rows);
        int total = chunks.size();
        metricTotal = total;

        for (int i = 0; i < total; i++) {
            waitIfPaused();
            if (cancelled) return;
            metricCurrentChunk = i + 1;

            TranslationRepository.ChunkRow existing = findRow(rows, i);
            if(existing!=null&&"DELIVERY_UNKNOWN".equalsIgnoreCase(existing.status)){repo.touchJob(jobId,"NEEDS_REVIEW");throw new TranslationEngine.DeliveryUnknownException("Chunk "+(i+1)+" delivery is unknown; automatic resend is blocked",null);}
            if (existing != null && isCompletedStatus(existing.status) && existing.translated != null && !existing.translated.isEmpty()) {
                previous = tail(existing.translated, 1500);
                acceptedPreview.accept(existing.translated, i + 1, total);
                if (metricCompleted < i + 1) metricCompleted = i + 1;
                updatePartialOutput(jobId, outputUri, s);
                broadcast("running", percent(metricCompleted, total), "Đã bỏ qua chunk đã dịch " + (i + 1) + "/" + total);
                continue;
            }

            enforceCostLimit(s);
            Chunk chunk = chunks.get(i);
            int fileProgress = percent(i, total);
            int global = fileTotal <= 1 ? fileProgress : Math.min(99, Math.round(((fileNo - 1) + (i / Math.max(1f, total))) * 100f / fileTotal));
            metricPhase = s.refineAfter ? "Translating (1/2)" : "Translating";
            broadcast("running", global, "[" + fileNo + "/" + fileTotal + "] Đang dịch " + fileName + " chunk " + (i + 1) + "/" + total);
            updateNotification("[" + fileNo + "/" + fileTotal + "] " + fileName + " chunk " + (i + 1) + "/" + total, i, total);
            try {
                String translated = translateWithRetry(chunk, previous, s);
                if (cancelled) return;
                if (s.refineAfter) {
                    metricPhase = "Refining (2/2)";
                    broadcast("running", global, "Đang refine chunk " + (i + 1) + "/" + total);
                    translated = refineWithRetry(chunk, translated, s);
                    if (cancelled) return;
                }
                commitAccepted(jobId, chunk, translated, previous);
                metricCompleted = Math.max(metricCompleted, i + 1);
                acceptedPreview.accept(translated, i + 1, total);
                previous = tail(translated, 1500);
                if (s.savePartialOutput) updatePartialOutput(jobId, outputUri, s);
                broadcast("running", global, "Chunk " + (i + 1) + " hoàn tất");
            } catch (Exception e) {
                if (cancelled) return;
                boolean reasoningBound=TranslationEngine.isReasoningBound(e);
                boolean rootAttemptLimit=TranslationEngine.isRootAttemptLimit(e);
                boolean cut=TranslationEngine.shouldSplitAfterFailure(e);
                if (reasoningBound || rootAttemptLimit) {
                    repo.markChunkError(jobId, i, AppValidator.readableError(e));
                    metricFailed++;
                    String reason=reasoningBound
                            ? "Reasoning đã dùng hết giới hạn; chunk " + (i + 1) + " dừng ở cấp chương, không chia nhỏ"
                            : "Chunk gốc đã vượt 6 lượt gọi; dừng ở cấp chương, không chia nhỏ";
                    throw new ChapterLocalException(reason, e);
                }
                if(cut||RetryPolicy.classify(e)==RetryPolicy.Kind.CONTEXT_LENGTH){
                    // A cut answer would be cut again if sent as it is: split the chunk instead (an answer too long for the context is split the same way).
                    List<Chunk> children=Chunker.splitForContextOverflow(chunk,s);
                    if(children.size()<2){
                        if(cut){repo.markChunkError(jobId,i,AppValidator.readableError(e));metricFailed++;throw new ChapterLocalException("Bản dịch bị cắt và chunk "+(i+1)+" không chia nhỏ được nữa",e);}
                        repo.markChunkPermanentError(jobId,i,"Context length exceeded and source cannot be split further");throw new ChapterLocalException("Context length exceeded and source cannot be split further",e);
                    }
                    repo.supersedeAndInsertSubchunks(jobId,chunk,children);
                    broadcast("running",global,(cut?"Bản dịch bị cắt: chunk ":"Context limit: chunk ")+(i+1)+" split into "+children.size()+" ordered subchunks; instructions and locks preserved");
                    List<TranslationRepository.ChunkRow> replacementRows=repo.getChunkRows(jobId);List<Chunk> replacementChunks=new ArrayList<>();for(TranslationRepository.ChunkRow row:replacementRows)replacementChunks.add(chunkFromRow(row));Chunker.assignParagraphRanges(replacementChunks);
                    translateJob(jobId,outputUri,s,replacementChunks,fileNo,fileTotal,fileName);return;
                }
                repo.markChunkError(jobId, i, AppValidator.readableError(e));
                metricFailed++;
                if (s.savePartialOutput) updatePartialOutput(jobId, outputUri, s);
                // In a chapter set a rejected answer belongs to this chapter: the queue goes on, so no "error" is broadcast.
                if (chapterOutcome != null && isRejection(e)) throw new ChapterLocalException(AppValidator.readableError(e), e);
                broadcast("error", global, "Chunk " + (i + 1) + " lỗi: " + AppValidator.readableError(e));
                throw e;
            }
        }
        updatePartialOutput(jobId, outputUri, s);
        OutputIntegrityAudit.Result audit=repo.finishJobWithAudit(jobId);
        if(!audit.passed()){
            if(audit.onlyIncomplete())throw new IllegalStateException("Còn "+audit.incompleteCount()+" chunk chưa dịch");
            throw new ChapterLocalException("Final integrity audit failed: "+audit.errors);
        }
        metricPhase = "Completed";
        broadcast("running", percent(fileNo, fileTotal), "Translation completed: " + fileName);
        updateNotification("Hoàn tất " + fileName, total, total);
    }

    /** True when the failure is a validation verdict on a received answer (not a network, authentication, quota or cost problem). */
    private static boolean isRejection(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) if (t instanceof ResponseValidator.RejectedResponseException) return true;
        return false;
    }

    private String translateWithRetry(Chunk chunk, String previous, AppSettings s) throws Exception {
        return engine.translateWithRetry(activeJobId, chunk, previous, s,
                () -> cancelled,
                this::addUsage,
                runtimeEventSink());
    }

    private String refineWithRetry(Chunk chunk, String draft, AppSettings s) throws Exception {
        return engine.refineWithRetry(activeJobId, chunk, draft, s,
                () -> cancelled,
                this::addUsage,
                runtimeEventSink());
    }

    private TranslationEngine.EventSink runtimeEventSink() {
        return new TranslationEngine.EventSink() {
            @Override public void onEvent(String message) {
                LogStore.append(TranslatorService.this, message);
            }

            @Override public int beforeRequest(Chunk chunk, int attempt) throws Exception {
                String root = rootStableKey(activeJobId, chunk);
                int ordinal = rootAttemptBudget.claim(root);
                if (ordinal < 0) {
                    throw new TranslationEngine.RootAttemptLimitException("Root chunk attempt limit reached: 6");
                }
                LogStore.append(TranslatorService.this, "rootAttemptOrdinal=" + ordinal + " attempt=" + attempt + " chunk=" + (chunk == null ? -1 : chunk.index));
                return ordinal;
            }

            @Override public void onLockUsage(String phase, int oneBasedChunk, int glossaryCount, int pronounCount) {
                metricGlossaryLocks = Math.max(0, glossaryCount);
                metricPronounLocks = Math.max(0, pronounCount);
                metricLockChunk = Math.max(0, oneBasedChunk);
                metricLockPhase = phase == null ? "" : phase;
                broadcast("running", metricLastProgress, null);
            }
        };
    }

    private String rootStableKey(long jobId, Chunk chunk) {
        String stable = chunk == null || chunk.stableId == null ? "" : chunk.stableId;
        String key = jobId + ":" + stable;
        String known = rootByChunk.get(key);
        if (known != null) return known;
        String parent = chunk == null || chunk.parentStableId == null ? "" : chunk.parentStableId;
        String root = parent.isEmpty() ? stable : rootByChunk.get(jobId + ":" + parent);
        if (root == null || root.isEmpty()) root = parent.isEmpty() ? stable : parent;
        rootByChunk.put(key, root);
        return jobId + ":" + root;
    }

    private void addUsage(OpenAICompatibleClient.ChatResult r, AppSettings s) {
        if (r == null) return;
        metricUsageResponses++;
        metricAllUsageReported &= r.usageReported;
        metricTokens += Math.max(0, r.totalTokens);
        double actual = r.providerCostReported ? r.providerCost : ModelCatalog.usageCost(
                ModelCatalog.findModelInfo(s.provider, s.model), Math.max(0, r.promptTokens),
                Math.max(0, r.cachedPromptTokens), Math.max(0, r.completionTokens));
        if (!Double.isNaN(actual)) metricCost += actual;
        runCost.addUsage(Math.max(0, r.totalTokens), actual);
    }

    private void updatePartialOutput(long jobId, Uri outputUri, AppSettings s) throws Exception {
        String assembled=engine.assembleOutput(repo.getChunkRows(jobId),s!=null&&s.bilingualOutput,true);
        if(assembled.isEmpty())return;
        Uri actual=materializeOutput(jobId,outputUri);
        FileUtil.writeTextVerified(this,actual,assembled);
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

    private Uri materializeOutput(long jobId,Uri output) throws Exception {
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

    private void setState(TranslationJobState.State next, String phase) {
        TranslationJobState.State current = jobState;
        if (!TranslationJobState.canTransition(current, next)) {
            LogStore.append(this, "Invalid job-state transition " + current + " -> " + next);
        }
        jobState = next;
        metricPhase = phase == null || phase.isEmpty() ? next.name() : phase;
    }

    private void logFailure(String step, Throwable error) {
        String stack = Log.getStackTraceString(error);
        Log.e("TranslateBooks", step, error);
        LogStore.append(this, "Failure at " + step + ": " + error.getClass().getName() + ": "
                + AppValidator.readableError(error) + "\n" + stack);
        DebugTraceStore.error(this, step, Math.max(0, metricCurrentChunk), error);
    }


    private AppSettings applyEffectiveCostLimit(AppSettings settings, Intent intent) {
        AppSettings s = settings == null ? new AppSettings() : settings;
        double cap = intent == null ? Double.NaN : intent.getDoubleExtra(EXTRA_EFFECTIVE_COST_LIMIT_USD, Double.NaN);
        if (!Double.isNaN(cap)) {
            if (cap < 0) { s.stopOnCostLimit = false; s.costLimitUsd = 0; }
            else { s.stopOnCostLimit = true; s.costLimitUsd = cap; }
        } else if (!(s.stopOnCostLimit && s.costLimitUsd > 0)) {
            // Resume/retry actions do not have a fresh preflight estimate, so retain the minimum safe default.
            s.stopOnCostLimit = true;
            s.costLimitUsd = CostLimitPolicy.MIN_SAFE_CAP_USD;
        }
        return s;
    }

    private void enforceCostLimit(AppSettings s) {
        if (s != null && s.stopOnCostLimit && s.costLimitUsd > 0 && runCost.limitReached(true, s.costLimitUsd)) {
            metricPhase = "Cost limit reached";
            broadcast("paused", -1, "Đã đạt cost limit " + CostEstimator.money(s.costLimitUsd) + " cho cả lượt (" + CostEstimator.money(runCost.cost()) + " đã chi); dừng trước chunk tiếp theo");
            if (activeJobId > 0) repo.touchJob(activeJobId, "paused");
            throw new CostLimitReachedException("Cost limit reached: " + CostEstimator.money(s.costLimitUsd));
        }
    }

    /** Chunks that are not translated yet (anything but completed). */
    private int unfinishedCount(List<TranslationRepository.ChunkRow> rows) {
        int n = 0;
        for (TranslationRepository.ChunkRow r : rows) if (!isCompletedStatus(r.status)) n++;
        return n;
    }

    /** " — còn m chunk chưa dịch" for the job being worked on, or "" when nothing is left or there is no job. */
    private String remainingSuffix() {
        try {
            if (activeJobId <= 0 || repo == null) return "";
            int n = unfinishedCount(repo.getChunkRows(activeJobId));
            return n > 0 ? " — còn " + n + " chunk chưa dịch (bấm \"Tiếp tục dịch\" ở màn Dịch hoặc tab Jobs)" : "";
        } catch (Exception e) {
            return "";
        }
    }

    private boolean hasFailedRows(List<TranslationRepository.ChunkRow> rows) {
        for (TranslationRepository.ChunkRow r : rows) if (isFailedStatus(r.status)) return true;
        return false;
    }

    private String previousBefore(List<TranslationRepository.ChunkRow> rows, int idx) {
        String last = "";
        for (TranslationRepository.ChunkRow r : rows) {
            if (r.index >= idx) break;
            if (isCompletedStatus(r.status) && r.translated != null && !r.translated.isEmpty()) last = r.translated;
        }
        return tail(last, 1500);
    }

    private void waitIfPaused() throws InterruptedException {
        while (paused && !cancelled) Thread.sleep(300);
    }

    private int percent(int done, int total) { return total <= 0 ? 0 : Math.min(100, Math.round(done * 100f / total)); }
    private String tail(String s, int max) { if (s == null) return ""; return s.length() <= max ? s : s.substring(s.length() - max); }

    private long elapsedMs() { return jobStartMs <= 0 ? 0 : System.currentTimeMillis() - jobStartMs; }
    private long remainingMs() {
        return TranslationJobState.remainingMs(jobState, metricCompleted, metricTotal, elapsedMs());
    }

    private String findPreviousTranslation(List<TranslationRepository.ChunkRow> rows) {
        String last = "";
        for (TranslationRepository.ChunkRow r : rows) if (isCompletedStatus(r.status) && r.translated != null && !r.translated.isEmpty()) last = r.translated;
        return tail(last, 1500);
    }

    private TranslationRepository.ChunkRow findRow(List<TranslationRepository.ChunkRow> rows, int idx) {
        for (TranslationRepository.ChunkRow r : rows) if (r.index == idx) return r;
        return null;
    }

    private void commitAccepted(long jobId, Chunk chunk, String translated, String previous) {
        ChunkQa.Metrics qa=ChunkQa.inspect(chunk.mainContent,translated,previous);
        java.util.ArrayList<String> warnings=new java.util.ArrayList<>(qa.warnings);
        if(engine.takeQuoteRepaired())warnings.add("QUOTE_STYLE_REPAIRED");
        repo.commitValidatedChunk(jobId,chunk.index,translated,String.join(",",warnings),"PROVIDER");
    }

    private static Chunk chunkFromRow(TranslationRepository.ChunkRow r) {
        Chunk c=new Chunk(r.index,r.startOffset,r.endOffset,"",r.source,"",r.parentStableId);
        if(r.stableId!=null&&!r.stableId.isEmpty())c.stableId=r.stableId;
        return c;
    }

    private static Chunk findChunk(List<Chunk> chunks, int index) {
        if (chunks != null) for (Chunk chunk : chunks) if (chunk != null && chunk.index == index) return chunk;
        throw new IllegalArgumentException("Missing chunk " + index + " in restored chunk list");
    }

    private static boolean isCompletedStatus(String status) {
        return "done".equalsIgnoreCase(status)||"COMPLETED".equalsIgnoreCase(status)||"COMPLETED_WITH_WARNINGS".equalsIgnoreCase(status);
    }

    private static boolean isFailedStatus(String status) {
        return "error".equalsIgnoreCase(status)||"RETRYABLE_ERROR".equalsIgnoreCase(status)||"PERMANENT_ERROR".equalsIgnoreCase(status);
    }

    private void repoPauseLast() {
        TranslationRepository.Job job = repo.getLastIncompleteJob();
        if (job != null) repo.touchJob(job.id, "paused");
    }

    private void broadcast(String status, int progress, String log) {
        metricLastProgress = progress;
        if (log != null && !log.trim().isEmpty()) metricLastLog = log;
        long elapsed = elapsedMs();
        long remaining = remainingMs();
        RuntimeStateStore.save(this, jobState.name(), status, progress, metricLastLog, metricPhase, metricFile,
                metricTotal, metricCompleted, metricFailed, metricFallbacks,
                metricTokens, metricCost, metricEstimatedTokens, metricEstimatedCost,
                metricUsageResponses > 0 && metricAllUsageReported, elapsed, remaining, acceptedPreview.text,
                metricCurrentChunk, metricGlossaryLocks, metricPronounLocks, metricLockChunk, metricLockPhase,
                acceptedPreview.chunk, activeJobId, acceptedPreview.file, acceptedPreview.total);
        ChapterRunOutcome setOutcome = chapterOutcome;
        int setTotal = setOutcome == null ? 0 : setOutcome.total(), setDone = setOutcome == null ? 0 : setOutcome.doneCount(), setFailed = setOutcome == null ? 0 : setOutcome.failedCount();
        RuntimeStateStore.saveSession(this, runCost.tokens(), runCost.cost(), sessionStartMs, setTotal, setDone, setFailed, setIndex);
        if (log != null && !log.trim().isEmpty()) LogStore.append(this, log);

        Intent i = new Intent(ACTION_PROGRESS);
        i.setPackage(getPackageName());
        i.putExtra(EXTRA_STATUS, status);
        i.putExtra(EXTRA_PROGRESS, progress);
        i.putExtra(EXTRA_LOG, log);
        i.putExtra(EXTRA_PHASE, metricPhase);
        i.putExtra(EXTRA_FILE_NAME, metricFile);
        i.putExtra(EXTRA_TOTAL_CHUNKS, metricTotal);
        i.putExtra(EXTRA_COMPLETED, metricCompleted);
        i.putExtra(EXTRA_FAILED, metricFailed);
        i.putExtra(EXTRA_FALLBACKS, metricFallbacks);
        i.putExtra(EXTRA_TOTAL_TOKENS, metricTokens);
        i.putExtra(EXTRA_TOTAL_COST, metricCost);
        i.putExtra(EXTRA_ESTIMATED_TOTAL_TOKENS, metricEstimatedTokens);
        i.putExtra(EXTRA_ESTIMATED_TOTAL_COST, metricEstimatedCost);
        i.putExtra(EXTRA_PROVIDER_USAGE_COMPLETE, metricUsageResponses > 0 && metricAllUsageReported);
        i.putExtra(EXTRA_ELAPSED_MS, elapsed);
        i.putExtra(EXTRA_REMAINING_MS, remaining);
        i.putExtra(EXTRA_PREVIEW, acceptedPreview.text);
        i.putExtra(EXTRA_CURRENT_CHUNK, metricCurrentChunk);
        i.putExtra(EXTRA_GLOSSARY_LOCKS, metricGlossaryLocks);
        i.putExtra(EXTRA_PRONOUN_LOCKS, metricPronounLocks);
        i.putExtra(EXTRA_LOCK_CHUNK, metricLockChunk);
        i.putExtra(EXTRA_LOCK_PHASE, metricLockPhase);
        i.putExtra(EXTRA_PREVIEW_CHUNK, acceptedPreview.chunk);
        i.putExtra(EXTRA_PREVIEW_FILE, acceptedPreview.file);
        i.putExtra(EXTRA_PREVIEW_TOTAL, acceptedPreview.total);
        i.putExtra(EXTRA_ACTIVE_JOB_ID, activeJobId);
        i.putExtra(EXTRA_STATE, jobState.name());
        i.putExtra(EXTRA_SESSION_TOKENS, runCost.tokens());
        i.putExtra(EXTRA_SESSION_COST, runCost.cost());
        i.putExtra(EXTRA_SESSION_START_MS, sessionStartMs);
        i.putExtra(EXTRA_SET_TOTAL, setTotal);
        i.putExtra(EXTRA_SET_DONE, setDone);
        i.putExtra(EXTRA_SET_FAILED, setFailed);
        i.putExtra(EXTRA_SET_INDEX, setIndex);
        sendBroadcast(i);
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

    private void updateNotification(String text, int done, int total) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIFY_ID, buildNotification(text, done, total));
    }
}
