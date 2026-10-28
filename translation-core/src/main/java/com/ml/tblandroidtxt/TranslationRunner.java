package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The state and control flow of one translation run (translate, retry, resume, pause, cost cap, partial output),
 * moved out of TranslatorService. Android-specific work goes through the ports: {@link RunListener} (progress and
 * notification), {@link DocumentIO}, {@link RunStatePersister}, {@link RuntimeLog}, {@link TraceSink},
 * {@link SecretsResolver} and {@link Clock}. Method bodies below are copied from TranslatorService; only the
 * substitutions of docs/V424_P1B_RUN_LOOP_CORE_WORK_REQUEST.md sections 2.3 and 2.4 were applied.
 */
public class TranslationRunner {
    public static class CostLimitReachedException extends RuntimeException {
        public CostLimitReachedException(String message) { super(message); }
    }

    private final RunListener listener;
    private final DocumentIO documents;
    private final RunStatePersister persister;
    private final RuntimeLog runtimeLog;
    private final TraceSink trace;
    private final SecretsResolver secrets;
    private final Clock clock;
    private TranslationRepositoryCore repo;
    private TranslationEngineCore engine;

    public TranslationRunner(RunListener listener, DocumentIO documents, RunStatePersister persister,
                             RuntimeLog runtimeLog, TraceSink trace, SecretsResolver secrets, Clock clock) {
        this.listener = listener;
        this.documents = documents;
        this.persister = persister;
        this.runtimeLog = runtimeLog;
        this.trace = trace == null ? TraceSink.NONE : trace;
        this.secrets = secrets;
        this.clock = clock == null ? Clock.SYSTEM : clock;
    }

    /** Called once the repository and engine exist (the service creates them lazily). */
    public void attach(TranslationRepositoryCore repo, TranslationEngineCore engine) {
        this.repo = repo;
        this.engine = engine;
    }

    private volatile boolean paused = false;
    private volatile boolean cancelled = false;
    /** Chapter-set runs only: what happened to each chapter so far; null otherwise. */
    private volatile ChapterRunOutcome chapterOutcome;
    private final RunCostMeter runCost = new RunCostMeter();
    private final RootChunkAttemptBudget rootAttemptBudget = new RootChunkAttemptBudget(6);
    private final Map<String, String> rootByChunk = new HashMap<>();
    private volatile long sessionStartMs;
    private volatile long sessionEndMs;
    private volatile long pausedAtMs;
    private volatile long pausedTotalMs;
    private volatile long sessionElapsedStoredMs;
    private volatile int setIndex;

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


    // ---- control and state access for the service / desktop shell ----

    public boolean isPaused() { return paused; }
    public boolean isCancelled() { return cancelled; }
    public void setPaused(boolean value) { paused = value; }
    public void setCancelled(boolean value) { cancelled = value; }
    public long getActiveJobId() { return activeJobId; }
    public void setActiveJobId(long value) { activeJobId = value; }
    public boolean isBenchmarkRun() { return benchmarkRun; }
    public void setBenchmarkRun(boolean value) { benchmarkRun = value; }
    public String getMetricPhase() { return metricPhase; }
    public void setMetricPhase(String value) { metricPhase = value; }
    public ChapterRunOutcome getChapterOutcome() { return chapterOutcome; }
    public void setChapterOutcome(ChapterRunOutcome value) { chapterOutcome = value; }
    public int getSetIndex() { return setIndex; }
    public void setSetIndex(int value) { setIndex = value; }
    public int getMetricCompleted() { return metricCompleted; }
    public int getMetricTotal() { return metricTotal; }
    public void setMetricCompleted(int value) { metricCompleted = value; }
    public AcceptedTranslationPreview getAcceptedPreview() { return acceptedPreview; }

    /** ACTION_START carries the estimate shown before the run. */
    public void setEstimated(int tokens, double cost) {
        metricEstimatedTokens = Math.max(0, tokens);
        metricEstimatedCost = Math.max(0, cost);
    }

    /** The statements each ACTION_* starts with: not cancelled, not paused, no active job; a fresh run also resets the session. */
    public void beginRun(boolean freshSession) {
        cancelled = false;
        paused = false;
        activeJobId = -1L;
        if (freshSession) resetSession();
    }

    public void beginPreviewRun() { acceptedPreview.beginRun(); }

    public double sessionCost() { return runCost.cost(); }

    public java.util.function.DoubleSupplier runCostSupplier() { return runCost::cost; }

    public void pause() {
        paused = true;
        markPausedAt(clock.now());
        repoPauseLast();
        setState(TranslationJobState.State.PAUSED, "Paused");
        publish("paused", -1, "Đã tạm dừng dịch");
        listener.onNotification("Tạm dừng", metricCompleted, metricTotal);
    }

    public void resume() {
        closePause(clock.now());
        paused = false;
        setState(TranslationJobState.State.RUNNING, "Running");
        publish("running", -1, "Tiếp tục dịch");
    }

    /** The active-run branch of ACTION_CANCEL (the caller has already asked the HTTP client to drop requests). */
    public void cancel() {
        cancelled = true;
        paused = false;
        metricPhase = "Stopping";
        if (activeJobId > 0 && !benchmarkRun) repo.touchJob(activeJobId, "cancelled");
        publish("stopping", -1, "Đang dừng dịch. App sẽ không tạo job mới trong lúc request hiện tại chưa thoát.");
        listener.onNotification("Đang dừng dịch...", metricCompleted, metricTotal);
    }

    /** ACTION_RETRY_CHUNK: optionally applies an edited settings snapshot, then retries that chunk. */
    public void retryChunk(long jobId, int chunkIdx, String editedSettingsJson, boolean confirmedUnknown) {
        if ("DELIVERY_UNKNOWN".equalsIgnoreCase(repo.chunkStatus(jobId, chunkIdx)) && !confirmedUnknown) {
            publish("paused", -1, "Retry blocked: delivery is unknown and explicit duplicate-charge confirmation is required");
            listener.onRunEnded();
            return;
        }
        if (editedSettingsJson != null && !editedSettingsJson.isEmpty()) {
            repo.applyConfigRevision(jobId, chunkIdx, secrets.withCurrentSecrets(SettingsJson.fromJson(editedSettingsJson)));
        }
        retryFailedChunksForJob(jobId, chunkIdx);
    }

    /** Computes the numbers once, persists them, and hands the listener an immutable copy (the old broadcast()). */
    public void publish(String status, int progress, String log) {
        listener.onSnapshot(snapshot(status, progress, log));
    }

    public RunSnapshot snapshot(String status, int progress, String log) {
        metricLastProgress = progress;
        if (log != null && !log.trim().isEmpty()) metricLastLog = log;
        long now = clock.now();
        updateSessionMarkers(status, now);
        long elapsed = elapsedMs();
        long remaining = remainingMs();
        persister.save(jobState.name(), status, progress, metricLastLog, metricPhase, metricFile,
                metricTotal, metricCompleted, metricFailed, metricFallbacks,
                metricTokens, metricCost, metricEstimatedTokens, metricEstimatedCost,
                metricUsageResponses > 0 && metricAllUsageReported, elapsed, remaining, acceptedPreview.text,
                metricCurrentChunk, metricGlossaryLocks, metricPronounLocks, metricLockChunk, metricLockPhase,
                acceptedPreview.chunk, activeJobId, acceptedPreview.file, acceptedPreview.total);
        ChapterRunOutcome setOutcome = chapterOutcome;
        int setTotal = setOutcome == null ? 0 : setOutcome.total(), setDone = setOutcome == null ? 0 : setOutcome.doneCount(), setFailed = setOutcome == null ? 0 : setOutcome.failedCount();
        persister.saveSession(runCost.tokens(), runCost.cost(), sessionStartMs,
                sessionEndMs, pausedAtMs, pausedTotalMs, setTotal, setDone, setFailed, setIndex);
        if (log != null && !log.trim().isEmpty()) runtimeLog.append(log);
        return new RunSnapshot(status, progress, log, metricLastLog, metricPhase, metricFile,
                metricTotal, metricCompleted, metricFailed, metricFallbacks,
                metricTokens, metricCost, metricEstimatedTokens, metricEstimatedCost,
                metricUsageResponses > 0 && metricAllUsageReported, elapsed, remaining, acceptedPreview.text,
                metricCurrentChunk, metricGlossaryLocks, metricPronounLocks, metricLockChunk, metricLockPhase,
                acceptedPreview.chunk, acceptedPreview.file, acceptedPreview.total, activeJobId, jobState.name(),
                runCost.tokens(), runCost.cost(), sessionStartMs, sessionEndMs, pausedAtMs, pausedTotalMs,
                setTotal, setDone, setFailed, setIndex);
    }

    public void resetMetrics(String fileName, int total) {
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

    public void setExpectedMetrics(CostEstimator.Estimate estimate) {
        if (estimate == null) return;
        metricEstimatedTokens = Math.max(0, estimate.totalTokensHigh);
        metricEstimatedCost = estimate.pricingAvailable && !Double.isNaN(estimate.costHigh) ? Math.max(0, estimate.costHigh) : 0;
    }

    /** " — x xong · y lỗi (…) · z chưa chạy" after a chapter-set run stopped; empty for every other run. */
    public String chapterSummarySuffix(){ChapterRunOutcome o=chapterOutcome;return o==null?"":"\n"+o.summary();}

    /** Continues an unfinished job inside a prepared run: translated chunks are kept, the rest (including failed ones) is translated with the job's own settings. */
    public void continueExistingJob(long jobId,int fileNo,int fileTotal,String label)throws Exception{
        TranslationRepositoryCore.Job job=repo.getJob(jobId);
        if(job==null)throw new IllegalStateException("Không thấy job #"+jobId+" để tiếp tục");
        AppSettings s=applyEffectiveCostLimit(AppValidator.normalize(secrets.withCurrentSecrets(SettingsJson.fromJson(job.settingsJson))),null);
        String validation=AppValidator.validateForTranslation(s);if(validation!=null)throw new IllegalArgumentException(validation);
        activeJobId=job.id;
        String expectedInputHash=repo.getInputHash(job.id);
        if(expectedInputHash!=null&&!expectedInputHash.isEmpty()&&!inputStillMatches(job.id,expectedInputHash,job.inputUri)){repo.touchJob(job.id,"INPUT_CONFLICT");throw new IllegalStateException("Input file changed since checkpoint; job #"+job.id+" cannot continue");}
        List<TranslationRepositoryCore.ChunkRow> rows=repo.getChunkRows(job.id);
        for(TranslationRepositoryCore.ChunkRow row:rows)if("DELIVERY_UNKNOWN".equalsIgnoreCase(row.status)){repo.touchJob(job.id,"NEEDS_REVIEW");throw new IllegalStateException("Job #"+job.id+" có request không rõ đã gửi hay chưa; xem ở tab Jobs rồi chọn retry");}
        List<Chunk> chunks=new ArrayList<>();int completed=0,failed=0,pending=0;
        for(TranslationRepositoryCore.ChunkRow r:rows){chunks.add(chunkFromRow(r));if(isCompletedStatus(r.status))completed++;else if(isFailedStatus(r.status))failed++;else pending++;}
        if(rows.isEmpty())throw new IllegalArgumentException("Job #"+job.id+" không có chunk checkpoint");
        Chunker.assignParagraphRanges(chunks);
        ObservabilityLog.event("resume_decision","job",job.id,"action","START_CONTINUE","branch",completed>=rows.size()?"already_translated":"continue","done",completed,"error",failed,"pending",pending);
        if(completed>=rows.size()){updatePartialOutput(job.id,job.outputUri,s);repo.finishJob(job.id);return;}
        repo.touchJob(job.id,"running");
        try{runCost.preloadContinuedJob(job.id,repo.getMetricsSummary(job.id).totalCost);}catch(Exception ignored){}
        resetMetrics(label,rows.size());metricCompleted=completed;metricFailed=failed;metricPhase="Translating";
        publish("running",percent(fileNo-1,fileTotal),"Tiếp tục job #"+job.id+": "+label+" (xong "+completed+", lỗi "+failed+", chưa dịch "+pending+")");
        translateJob(job.id,job.outputUri,s,chunks,fileNo,fileTotal,label);
    }

    public void startNewJob(String inputUri, String outputUri, AppSettings s, int fileNo, int fileTotal) throws Exception {
        TranslationEngineCore.PreparedInput prepared = engine.prepareInput(documents, inputUri, s);
        String fileName = prepared.fileName;
        List<Chunk> chunks = prepared.chunks;
        resetMetrics(fileName, chunks.size());
        // ACTION_START always owns a clean job. Checkpoint reuse is available only through explicit Resume actions.
        TranslationRepositoryCore.Job previousDone = null;
        if (previousDone != null) {
            String oldOut = repo.assembleOutput(previousDone.id, s.bilingualOutput);
            documents.writeText(outputUri, oldOut);
            resetMetrics(fileName, chunks.size());
            metricCompleted = chunks.size();
            metricPhase = "Already completed";
            acceptedPreview.accept(oldOut, chunks.size(), chunks.size());
            publish("done", percent(fileNo, fileTotal), "File này đã dịch xong với cùng nội dung + settings. App đã xuất lại output từ checkpoint, không gọi API/dịch lại: " + fileName);
            listener.onNotification("Đã có bản dịch, không dịch lại " + fileName, chunks.size(), chunks.size());
            return;
        }
        long jobId = repo.createJob(inputUri, outputUri, fileName, s, chunks, prepared.inputHash, prepared.settingsHash);
        activeJobId = jobId;
        repo.touchJob(jobId, "RUNNING");
        metricPhase = "Translating";
        publish("running", percent(fileNo - 1, fileTotal), "Đã chia " + fileName + " thành " + chunks.size() + " chunk");
        translateJob(jobId, outputUri, s, chunks, fileNo, fileTotal, fileName);
    }

    public void resumeLastJob() {
        resumeJobById(-1L);
    }

    public void resumeJobById(long selectedJobId) {
        try {
            TranslationRepositoryCore.Job job = selectedJobId > 0 ? repo.getJob(selectedJobId) : repo.getLastIncompleteJob();
            if (job == null) {
                publish("error", -1, selectedJobId > 0 ? "Không thấy job #" + selectedJobId : "Không có checkpoint để resume");
                listener.onRunEnded();
                return;
            }
            if (isCompletedStatus(job.status)) {
                ObservabilityLog.event("resume_decision", "job", job.id, "action", "RESUME", "branch", "already_completed");
                publish("done", 100, "Job #" + job.id + " đã hoàn tất, không cần resume: " + job.fileName);
                listener.onRunEnded();
                return;
            }
            AppSettings s = applyEffectiveCostLimit(AppValidator.normalize(secrets.withCurrentSecrets(SettingsJson.fromJson(job.settingsJson))),null);
            String validation = AppValidator.validateForTranslation(s);
            if (validation != null) throw new IllegalArgumentException(validation);
            activeJobId = job.id;
            String expectedInputHash=repo.getInputHash(job.id);
            if(expectedInputHash!=null&&!expectedInputHash.isEmpty()){
                if(!inputStillMatches(job.id,expectedInputHash,job.inputUri)){repo.touchJob(job.id,"INPUT_CONFLICT");throw new IllegalStateException("Input file changed since checkpoint; resume blocked");}
            }
            List<TranslationRepositoryCore.ChunkRow> rows = repo.getChunkRows(job.id);
            for(TranslationRepositoryCore.ChunkRow row:rows)if("DELIVERY_UNKNOWN".equalsIgnoreCase(row.status)){repo.touchJob(job.id,"NEEDS_REVIEW");publish("paused",-1,"Job #"+job.id+" has a request with unknown delivery. Review it before choosing retry anyway; it will not be resent automatically.");listener.onRunEnded();return;}
            for(TranslationRepositoryCore.ChunkRow r:rows){if("RESPONSE_RECEIVED".equalsIgnoreCase(r.status)&&r.persistedResponse!=null&&!r.persistedResponse.isEmpty()){try{String recovered=PromptBuilder.extractTranslationStrict(r.persistedResponse);PromptPlan recoveryPlan=PromptPlan.forTranslation(new Chunk(r.index,"",r.source,""),"",s);ResponseValidator.Result check=ResponseValidator.validate(r.persistedResponse,recovered,r.source,"",r.finishReason,Chunker.approxTokens(recovered),s.maxOutputTokens,recoveryPlan.glossaryTargets,true);if(check.accepted){ChunkQa.Metrics qa=ChunkQa.inspect(r.source,recovered,"");repo.commitValidatedChunk(job.id,r.index,recovered,String.join(",",qa.warnings),"PROVIDER_RECOVERED");r.translated=recovered;r.status="COMPLETED";}else{repo.rejectResponse(job.id,r.index,r.attempt,r.persistedResponse,check.summary(),true);r.status="RETRYABLE_ERROR";r.error=check.summary();}}catch(Exception validationError){repo.rejectResponse(job.id,r.index,r.attempt,r.persistedResponse,validationError.getMessage(),true);r.status="RETRYABLE_ERROR";r.error=validationError.getMessage();}}}
            List<Chunk> chunks = new ArrayList<>();
            int completed = 0, failed = 0, pending = 0;
            for (TranslationRepositoryCore.ChunkRow r : rows) {
                chunks.add(chunkFromRow(r));
                if (isCompletedStatus(r.status)) completed++;
                else if (isFailedStatus(r.status)) failed++;
                else pending++;
            }
            if (rows.isEmpty()) throw new IllegalArgumentException("Job #" + job.id + " không có chunk checkpoint");
            Chunker.assignParagraphRanges(chunks);
            if (completed >= rows.size()) {
                updatePartialOutput(job.id, job.outputUri, s);
                repo.finishJob(job.id);
                publish("done", 100, "Job #" + job.id + " đã có đủ output, đã đánh dấu hoàn tất");
                listener.onRunEnded();
                return;
            }
            ObservabilityLog.event("resume_decision", "job", job.id, "action", "RESUME", "branch", "continue", "done", completed, "error", failed, "pending", pending);
            repo.touchJob(job.id, "running");
            resetMetrics(job.fileName, rows.size());
            metricCompleted = completed;
            metricFailed = failed;
            setState(TranslationJobState.State.READY,"Ready to resume");
            setState(TranslationJobState.State.RUNNING,selectedJobId > 0 ? "Resume selected job" : "Resume");
            publish("running", percent(completed, Math.max(1, rows.size())), "Resume job #" + job.id + ": " + job.fileName + " (done=" + completed + ", failed=" + failed + ", pending=" + pending + ")");
            translateJob(job.id, job.outputUri, s, chunks, 1, 1, job.fileName);
            if (cancelled) { listener.onCancelledEnd("Đã hủy resume checkpoint"); return; }
            setState(TranslationJobState.State.COMPLETED,"Completed");
            publish("done", 100, "Resume checkpoint completed: " + job.fileName);
            listener.onRunEnded();
        } catch (CostLimitReachedException e) {
            setState(TranslationJobState.State.PAUSED,"Paused");
            if (activeJobId > 0) repo.touchJob(activeJobId, "paused");
            publish("paused", -1, AppValidator.readableError(e));
            listener.onRunEnded();
        } catch (Exception e) {
            if (cancelled) { listener.onCancelledEnd("Đã hủy resume checkpoint"); return; }
            setState(TranslationJobState.State.FAILED,"Resume error");
            if (activeJobId > 0) repo.touchJob(activeJobId, "error");
            logFailure(metricPhase,e);
            publish("error", -1, "Lỗi resume: " + AppValidator.readableError(e) + remainingSuffix());
            listener.onRunEnded();
        }
    }

    public void retryFailedChunksOnly() {
        retryFailedChunksForJob(-1L);
    }

    public void retryFailedChunksForJob(long selectedJobId) {
        retryFailedChunksForJob(selectedJobId,-1);
    }

    public void retryFailedChunksForJob(long selectedJobId,int targetChunk) {
        try {
            TranslationRepositoryCore.Job job = selectedJobId > 0 ? repo.getJob(selectedJobId) : repo.getLastJobWithFailedChunks();
            if (job == null) {
                publish("error", -1, selectedJobId > 0 ? "Không thấy job #" + selectedJobId : "Không có chunk lỗi để retry");
                listener.onRunEnded();
                return;
            }
            AppSettings s = applyEffectiveCostLimit(AppValidator.normalize(secrets.withCurrentSecrets(SettingsJson.fromJson(job.settingsJson))),null);
            String validation = AppValidator.validateForTranslation(s);
            if (validation != null) throw new IllegalArgumentException(validation);
            activeJobId = job.id;
            List<TranslationRepositoryCore.ChunkRow> rows = repo.getChunkRows(job.id);
            resetMetrics(job.fileName, rows.size());
            int completed = 0, failedTargets = 0;
            for (TranslationRepositoryCore.ChunkRow r : rows) {
                if (isCompletedStatus(r.status)) completed++;
                if ((targetChunk>=0&&r.index==targetChunk)||(targetChunk<0&&isFailedStatus(r.status))) failedTargets++;
            }
            if (failedTargets <= 0 && (targetChunk >= 0 || unfinishedCount(rows) == 0)) {
                ObservabilityLog.event("resume_decision", "job", job.id, "action", "RETRY", "branch", "nothing_to_retry", "done", completed, "total", rows.size());
                publish("done", percent(completed, Math.max(1, rows.size())), "Job #" + job.id + " không có chunk lỗi để retry, mọi chunk đã dịch xong");
                listener.onRunEnded();
                return;
            }
            repo.touchJob(job.id, "running");
            metricCompleted = completed;
            metricFailed = failedTargets;
            setState(TranslationJobState.State.READY,"Ready to retry");
            setState(TranslationJobState.State.RUNNING,selectedJobId > 0 ? "Retry selected job" : "Retry failed only");
            publish("running", percent(completed, Math.max(1, rows.size())), "Retry " + failedTargets + " failed chunk(s) for job #" + job.id + ": " + job.fileName);
            retryFailedRows(job.id, job.outputUri, s, rows, job.fileName,targetChunk);
            updatePartialOutput(job.id, job.outputUri, s);
            if (cancelled) { listener.onCancelledEnd("Đã hủy retry failed chunks"); return; }
            List<TranslationRepositoryCore.ChunkRow> afterRetry = repo.getChunkRows(job.id);
            int remaining = unfinishedCount(afterRetry);
            ObservabilityLog.event("resume_decision", "job", job.id, "action", targetChunk >= 0 ? "RETRY_CHUNK" : "RETRY_FAILED", "branch", remaining > 0 ? (targetChunk >= 0 ? "paused_with_remaining" : "continue_remaining") : "finish", "remaining", remaining);
            if (remaining > 0 && targetChunk >= 0) {
                // One chunk was retried on request: do not mark the job finished while chunks are still untranslated.
                repo.touchJob(job.id, "paused");
                setState(TranslationJobState.State.PAUSED, "Paused");
                publish("paused", -1, "Đã retry chunk " + (targetChunk + 1) + " của job #" + job.id + ". Còn " + remaining + " chunk chưa dịch: bấm \"Tiếp tục dịch\" để dịch nốt.");
                listener.onRunEnded();
                return;
            }
            if (remaining > 0) {
                // The failed chunks are fixed; the original run would have translated the rest, so do it now in the same run.
                List<Chunk> restChunks = new ArrayList<>();
                for (TranslationRepositoryCore.ChunkRow row : afterRetry) restChunks.add(chunkFromRow(row));
                Chunker.assignParagraphRanges(restChunks);
                publish("running", percent(metricCompleted, Math.max(1, afterRetry.size())), "Retry xong; dịch tiếp " + remaining + " chunk còn lại của job #" + job.id);
                metricPhase = "Translating";
                translateJob(job.id, job.outputUri, s, restChunks, 1, 1, job.fileName);
                if (cancelled) { listener.onCancelledEnd("Đã hủy khi dịch tiếp sau retry"); return; }
            } else {
                repo.finishJob(job.id);
            }
            setState(TranslationJobState.State.COMPLETED,"Completed");
            publish("done", 100, "Retry failed chunks completed: " + job.fileName);
            listener.onRunEnded();
        } catch (CostLimitReachedException e) {
            setState(TranslationJobState.State.PAUSED,"Paused");
            if (activeJobId > 0) repo.touchJob(activeJobId, "paused");
            publish("paused", -1, AppValidator.readableError(e));
            listener.onRunEnded();
        } catch (Exception e) {
            if (cancelled) { listener.onCancelledEnd("Đã hủy retry failed chunks"); return; }
            setState(TranslationJobState.State.FAILED,"Retry error");
            if (activeJobId > 0) repo.touchJob(activeJobId, "error");
            logFailure(metricPhase,e);
            publish("error", -1, "Lỗi retry failed chunks: " + AppValidator.readableError(e) + remainingSuffix());
            listener.onRunEnded();
        }
    }

    public void retryFailedRows(long jobId, String outputUri, AppSettings s, List<TranslationRepositoryCore.ChunkRow> rows, String fileName,int targetChunk) throws Exception {
        ArrayList<Chunk> annotated = new ArrayList<>();
        for (TranslationRepositoryCore.ChunkRow row : rows) annotated.add(chunkFromRow(row));
        Chunker.assignParagraphRanges(annotated);
        for (TranslationRepositoryCore.ChunkRow r : rows) {
            waitIfPaused();
            if (cancelled) return;
            if(targetChunk>=0){if(r.index!=targetChunk)continue;}else if(!isFailedStatus(r.status))continue;
            enforceCostLimit(s);
            String previous = previousBefore(rows, r.index);
            Chunk chunk = findChunk(annotated, r.index);
            metricPhase = "Retry failed only";
            metricCurrentChunk = r.index + 1;
            publish("running", percent(metricCompleted, Math.max(1, metricTotal)), "Retry chunk " + (r.index + 1) + "/" + metricTotal + ": " + fileName);
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
                publish("running", percent(metricCompleted, Math.max(1, metricTotal)), "Retry chunk " + (r.index + 1) + " hoàn tất");
            } catch (Exception e) {
                if (cancelled) return;
                repo.markChunkError(jobId, r.index, AppValidator.readableError(e));
                publish("error", percent(metricCompleted, Math.max(1, metricTotal)), "Retry chunk " + (r.index + 1) + " vẫn lỗi: " + AppValidator.readableError(e));
                throw e;
            }
        }
    }

    public void translateJob(long jobId, String outputUri, AppSettings s, List<Chunk> chunks, int fileNo, int fileTotal, String fileName) throws Exception {
        List<TranslationRepositoryCore.ChunkRow> rows = repo.getChunkRows(jobId);
        String previous = findPreviousTranslation(rows);
        int total = chunks.size();
        metricTotal = total;

        for (int i = 0; i < total; i++) {
            waitIfPaused();
            if (cancelled) return;
            metricCurrentChunk = i + 1;

            TranslationRepositoryCore.ChunkRow existing = findRow(rows, i);
            if(existing!=null&&"DELIVERY_UNKNOWN".equalsIgnoreCase(existing.status)){repo.touchJob(jobId,"NEEDS_REVIEW");throw new TranslationEngineCore.DeliveryUnknownException("Chunk "+(i+1)+" delivery is unknown; automatic resend is blocked",null);}
            if (existing != null && isCompletedStatus(existing.status) && existing.translated != null && !existing.translated.isEmpty()) {
                previous = tail(existing.translated, 1500);
                acceptedPreview.accept(existing.translated, i + 1, total);
                if (metricCompleted < i + 1) metricCompleted = i + 1;
                updatePartialOutput(jobId, outputUri, s);
                publish("running", percent(metricCompleted, total), "Đã bỏ qua chunk đã dịch " + (i + 1) + "/" + total);
                continue;
            }

            enforceCostLimit(s);
            Chunk chunk = chunks.get(i);
            int fileProgress = percent(i, total);
            int global = fileTotal <= 1 ? fileProgress : Math.min(99, Math.round(((fileNo - 1) + (i / Math.max(1f, total))) * 100f / fileTotal));
            metricPhase = s.refineAfter ? "Translating (1/2)" : "Translating";
            publish("running", global, "[" + fileNo + "/" + fileTotal + "] Đang dịch " + fileName + " chunk " + (i + 1) + "/" + total);
            listener.onNotification("[" + fileNo + "/" + fileTotal + "] " + fileName + " chunk " + (i + 1) + "/" + total, i, total);
            try {
                String translated = translateWithRetry(chunk, previous, s);
                if (cancelled) return;
                if (s.refineAfter) {
                    metricPhase = "Refining (2/2)";
                    publish("running", global, "Đang refine chunk " + (i + 1) + "/" + total);
                    translated = refineWithRetry(chunk, translated, s);
                    if (cancelled) return;
                }
                commitAccepted(jobId, chunk, translated, previous);
                metricCompleted = Math.max(metricCompleted, i + 1);
                acceptedPreview.accept(translated, i + 1, total);
                previous = tail(translated, 1500);
                if (s.savePartialOutput) updatePartialOutput(jobId, outputUri, s);
                publish("running", global, "Chunk " + (i + 1) + " hoàn tất");
            } catch (Exception e) {
                if (cancelled) return;
                boolean reasoningBound=TranslationEngineCore.isReasoningBound(e);
                boolean rootAttemptLimit=TranslationEngineCore.isRootAttemptLimit(e);
                boolean cut=TranslationEngineCore.shouldSplitAfterFailure(e);
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
                    publish("running",global,(cut?"Bản dịch bị cắt: chunk ":"Context limit: chunk ")+(i+1)+" split into "+children.size()+" ordered subchunks; instructions and locks preserved");
                    List<TranslationRepositoryCore.ChunkRow> replacementRows=repo.getChunkRows(jobId);List<Chunk> replacementChunks=new ArrayList<>();for(TranslationRepositoryCore.ChunkRow row:replacementRows)replacementChunks.add(chunkFromRow(row));Chunker.assignParagraphRanges(replacementChunks);
                    translateJob(jobId,outputUri,s,replacementChunks,fileNo,fileTotal,fileName);return;
                }
                repo.markChunkError(jobId, i, AppValidator.readableError(e));
                metricFailed++;
                if (s.savePartialOutput) updatePartialOutput(jobId, outputUri, s);
                // In a chapter set a rejected answer belongs to this chapter: the queue goes on, so no "error" is broadcast.
                if (chapterOutcome != null && isRejection(e)) throw new ChapterLocalException(AppValidator.readableError(e), e);
                publish("error", global, "Chunk " + (i + 1) + " lỗi: " + AppValidator.readableError(e));
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
        publish("running", percent(fileNo, fileTotal), "Translation completed: " + fileName);
        listener.onNotification("Hoàn tất " + fileName, total, total);
    }

    /** True when the failure is a validation verdict on a received answer (not a network, authentication, quota or cost problem). */
    public static boolean isRejection(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) if (t instanceof ResponseValidator.RejectedResponseException) return true;
        return false;
    }

    public String translateWithRetry(Chunk chunk, String previous, AppSettings s) throws Exception {
        return engine.translateWithRetry(activeJobId, chunk, previous, s,
                () -> cancelled,
                this::addUsage,
                runtimeEventSink());
    }

    public String refineWithRetry(Chunk chunk, String draft, AppSettings s) throws Exception {
        return engine.refineWithRetry(activeJobId, chunk, draft, s,
                () -> cancelled,
                this::addUsage,
                runtimeEventSink());
    }

    public TranslationEngineCore.EventSink runtimeEventSink() {
        return new TranslationEngineCore.EventSink() {
            @Override public void onEvent(String message) {
                runtimeLog.append(message);
            }

            @Override public int beforeRequest(Chunk chunk, int attempt) throws Exception {
                String root = rootStableKey(activeJobId, chunk);
                int ordinal = rootAttemptBudget.claim(root);
                if (ordinal < 0) {
                    throw new TranslationEngineCore.RootAttemptLimitException("Root chunk attempt limit reached: 6");
                }
                runtimeLog.append("rootAttemptOrdinal=" + ordinal + " attempt=" + attempt + " chunk=" + (chunk == null ? -1 : chunk.index));
                return ordinal;
            }

            @Override public void onLockUsage(String phase, int oneBasedChunk, int glossaryCount, int pronounCount) {
                metricGlossaryLocks = Math.max(0, glossaryCount);
                metricPronounLocks = Math.max(0, pronounCount);
                metricLockChunk = Math.max(0, oneBasedChunk);
                metricLockPhase = phase == null ? "" : phase;
                publish("running", metricLastProgress, null);
            }
        };
    }

    public String rootStableKey(long jobId, Chunk chunk) {
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

    public void addUsage(OpenAICompatibleClient.ChatResult r, AppSettings s) {
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

    public void updatePartialOutput(long jobId, String outputUri, AppSettings s) throws Exception {
        String assembled=engine.assembleOutput(repo.getChunkRows(jobId),s!=null&&s.bilingualOutput,true);
        if(assembled.isEmpty())return;
        String actual=documents.resolveOutput(jobId,outputUri);
        documents.writeTextVerified(actual,assembled);
    }

    /** Resume compares the stored hash with the file as written (raw) or as the plan normalized it; the matching branch is logged. */
    public boolean inputStillMatches(long jobId,String expectedHash,String inputUri)throws Exception{
        InputIdentity.Match m=InputIdentity.match(expectedHash,documents.readText(inputUri));
        ObservabilityLog.event("input_identity","job",jobId,"match",m.name());
        return m!=InputIdentity.Match.NONE;
    }

    public void setState(TranslationJobState.State next, String phase) {
        TranslationJobState.State current = jobState;
        if (!TranslationJobState.canTransition(current, next)) {
            runtimeLog.append("Invalid job-state transition " + current + " -> " + next);
        }
        jobState = next;
        metricPhase = phase == null || phase.isEmpty() ? next.name() : phase;
    }

    public void logFailure(String step, Throwable error) {
        String stack = runtimeLog.stackTrace(error);
        runtimeLog.error("TranslateBooks", step, error);
        runtimeLog.append("Failure at " + step + ": " + error.getClass().getName() + ": "
                + AppValidator.readableError(error) + "\n" + stack);
        trace.error(step, Math.max(0, metricCurrentChunk), error);
    }

    public AppSettings applyEffectiveCostLimit(AppSettings settings, Double capOrNull) {
        AppSettings s = settings == null ? new AppSettings() : settings;
        double cap = capOrNull == null ? Double.NaN : capOrNull;
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

    public void enforceCostLimit(AppSettings s) {
        if (s != null && s.stopOnCostLimit && s.costLimitUsd > 0 && runCost.limitReached(true, s.costLimitUsd)) {
            metricPhase = "Cost limit reached";
            publish("paused", -1, "Đã đạt cost limit " + CostEstimator.money(s.costLimitUsd) + " cho cả lượt (" + CostEstimator.money(runCost.cost()) + " đã chi); dừng trước chunk tiếp theo");
            if (activeJobId > 0) repo.touchJob(activeJobId, "paused");
            throw new CostLimitReachedException("Cost limit reached: " + CostEstimator.money(s.costLimitUsd));
        }
    }

    /** Chunks that are not translated yet (anything but completed). */
    public int unfinishedCount(List<TranslationRepositoryCore.ChunkRow> rows) {
        int n = 0;
        for (TranslationRepositoryCore.ChunkRow r : rows) if (!isCompletedStatus(r.status)) n++;
        return n;
    }

    /** " — còn m chunk chưa dịch" for the job being worked on, or "" when nothing is left or there is no job. */
    public String remainingSuffix() {
        try {
            if (activeJobId <= 0 || repo == null) return "";
            int n = unfinishedCount(repo.getChunkRows(activeJobId));
            return n > 0 ? " — còn " + n + " chunk chưa dịch (bấm \"Tiếp tục dịch\" ở màn Dịch hoặc tab Jobs)" : "";
        } catch (Exception e) {
            return "";
        }
    }

    public boolean hasFailedRows(List<TranslationRepositoryCore.ChunkRow> rows) {
        for (TranslationRepositoryCore.ChunkRow r : rows) if (isFailedStatus(r.status)) return true;
        return false;
    }

    public String previousBefore(List<TranslationRepositoryCore.ChunkRow> rows, int idx) {
        String last = "";
        for (TranslationRepositoryCore.ChunkRow r : rows) {
            if (r.index >= idx) break;
            if (isCompletedStatus(r.status) && r.translated != null && !r.translated.isEmpty()) last = r.translated;
        }
        return tail(last, 1500);
    }

    public void waitIfPaused() throws InterruptedException {
        while (paused && !cancelled) Thread.sleep(300);
    }

    public int percent(int done, int total) { return total <= 0 ? 0 : Math.min(100, Math.round(done * 100f / total)); }

    public String tail(String s, int max) { if (s == null) return ""; return s.length() <= max ? s : s.substring(s.length() - max); }

    public void markPausedAt(long nowMs) {
        if (sessionStartMs > 0L && pausedAtMs <= 0L) pausedAtMs = Math.max(sessionStartMs, nowMs);
    }

    public void closePause(long nowMs) {
        if (pausedAtMs > 0L) {
            pausedTotalMs += Math.max(0L, nowMs - pausedAtMs);
            pausedAtMs = 0L;
        }
    }

    public void markTerminal(long nowMs) {
        closePause(nowMs);
        if (sessionEndMs <= 0L) sessionEndMs = Math.max(sessionStartMs, nowMs);
    }

    public void updateSessionMarkers(String status, long nowMs) {
        if ("paused".equalsIgnoreCase(status)) markPausedAt(nowMs);
        if ("done".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)
                || "cancelled".equalsIgnoreCase(status)) markTerminal(nowMs);
    }

    public long elapsedMs() {
        long now = clock.now();
        if (sessionStartMs <= 0L) return Math.max(0L, sessionElapsedStoredMs);
        sessionElapsedStoredMs = RunPanelFormatter.runElapsedMs(jobState.name(), now, sessionStartMs,
                sessionEndMs, pausedAtMs, sessionElapsedStoredMs, pausedTotalMs);
        return sessionElapsedStoredMs;
    }

    public long remainingMs() {
        return TranslationJobState.remainingMs(jobState, metricCompleted, metricTotal, elapsedMs());
    }

    public String findPreviousTranslation(List<TranslationRepositoryCore.ChunkRow> rows) {
        String last = "";
        for (TranslationRepositoryCore.ChunkRow r : rows) if (isCompletedStatus(r.status) && r.translated != null && !r.translated.isEmpty()) last = r.translated;
        return tail(last, 1500);
    }

    public TranslationRepositoryCore.ChunkRow findRow(List<TranslationRepositoryCore.ChunkRow> rows, int idx) {
        for (TranslationRepositoryCore.ChunkRow r : rows) if (r.index == idx) return r;
        return null;
    }

    public void commitAccepted(long jobId, Chunk chunk, String translated, String previous) {
        ChunkQa.Metrics qa=ChunkQa.inspect(chunk.mainContent,translated,previous);
        java.util.ArrayList<String> warnings=new java.util.ArrayList<>(qa.warnings);
        if(engine.takeQuoteRepaired())warnings.add("QUOTE_STYLE_REPAIRED");
        repo.commitValidatedChunk(jobId,chunk.index,translated,String.join(",",warnings),"PROVIDER");
    }

    public static Chunk chunkFromRow(TranslationRepositoryCore.ChunkRow r) {
        Chunk c=new Chunk(r.index,r.startOffset,r.endOffset,"",r.source,"",r.parentStableId);
        if(r.stableId!=null&&!r.stableId.isEmpty())c.stableId=r.stableId;
        return c;
    }

    public static Chunk findChunk(List<Chunk> chunks, int index) {
        if (chunks != null) for (Chunk chunk : chunks) if (chunk != null && chunk.index == index) return chunk;
        throw new IllegalArgumentException("Missing chunk " + index + " in restored chunk list");
    }

    public static boolean isCompletedStatus(String status) {
        return "done".equalsIgnoreCase(status)||"COMPLETED".equalsIgnoreCase(status)||"COMPLETED_WITH_WARNINGS".equalsIgnoreCase(status);
    }

    public static boolean isFailedStatus(String status) {
        return "error".equalsIgnoreCase(status)||"RETRYABLE_ERROR".equalsIgnoreCase(status)||"PERMANENT_ERROR".equalsIgnoreCase(status);
    }

    public void repoPauseLast() {
        TranslationRepositoryCore.Job job = repo.getLastIncompleteJob();
        if (job != null) repo.touchJob(job.id, "paused");
    }

    /** A new run starts counting tokens, cost and time from zero. */
    public void resetSession() {
        runCost.reset();
        sessionStartMs = clock.now();
        sessionEndMs = 0L;
        pausedAtMs = 0L;
        pausedTotalMs = 0L;
        sessionElapsedStoredMs = 0L;
        setIndex = 0;
    }
}
