package com.ml.tblandroidtxt;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Repository facade for Job Manager screens/actions. */
public class JobStore {
    private final Context context;
    private final TranslationRepository repo;

    public JobStore(Context context) {
        this.context = context.getApplicationContext();
        repo = new TranslationRepository(this.context);
        if(!TranslatorService.isActive())repo.recoverInterruptedState();
    }

    public List<TranslationRepository.JobSummary> recentSummaries(int limit) { return repo.getJobSummaries(limit); }
    public TranslationRepository.JobSummary summary(long jobId) { return repo.getJobSummary(jobId); }
    public List<TranslationRepository.ChunkRow> chunks(long jobId) { return repo.getChunkRows(jobId); }
    public TranslationRepository.Job lastIncomplete() { return repo.getLastIncompleteJob(); }
    public TranslationRepository.Job get(long jobId) { return repo.getJob(jobId); }
    public TranslationRepository.Job lastWithFailedChunks() { return repo.getLastJobWithFailedChunks(); }
    public TranslationRepository.MetricsSummary metrics(long jobId) { return repo.getMetricsSummary(jobId); }
    public TranslationRepository.TokenCalibration calibration(long jobId){return repo.getTokenCalibration(jobId);}
    public List<TranslationRepository.AttemptRow> attempts(long jobId,int idx){return repo.getAttemptRows(jobId,idx);}
    public void manualReplace(long jobId,TranslationRepository.ChunkRow row,String text) throws Exception {
        ResponseValidator.Result validation=ResponseValidator.validate(text,text,row.source,"","stop",0,0,false);
        if(!validation.accepted)throw new IllegalArgumentException("Manual text failed structural validation: "+validation.summary());
        ChunkQa.Metrics qa=ChunkQa.inspect(row.source,text,"");repo.commitManualReplacement(jobId,row.index,text,String.join(",",qa.warnings));
        TranslationRepository.Job job=repo.getJob(jobId);if(job!=null&&job.outputUri!=null&&!job.outputUri.isEmpty()){AppSettings s=SettingsStore.fromJson(job.settingsJson);String assembled=repo.assembleOutput(jobId,s.bilingualOutput);if(!assembled.isEmpty())FileUtil.writeTextVerified(context,android.net.Uri.parse(job.outputUri),assembled);}
    }
    public TranslationRepository.BenchmarkSummary latestBenchmark(long jobId){return repo.latestBenchmark(jobId);}
    public List<TranslationRepository.BenchmarkResultRow> benchmarkResults(long sessionId){return repo.benchmarkResults(sessionId);}
    public void delete(long jobId) { repo.deleteJob(jobId); }
    public int firstRunnableChunk(long jobId){return repo.firstRunnableChunk(jobId);}
    public int applyRevision(long jobId,int firstChunk,AppSettings settings){return repo.applyConfigRevision(jobId,firstChunk,settings);}

    public boolean hasFailedChunks(long jobId) {
        for (TranslationRepository.ChunkRow r : repo.getChunkRows(jobId)) if ("error".equalsIgnoreCase(r.status)||"RETRYABLE_ERROR".equalsIgnoreCase(r.status)||"PERMANENT_ERROR".equalsIgnoreCase(r.status)) return true;
        return false;
    }

    public boolean hasRunnableChunks(long jobId) {
        for (TranslationRepository.ChunkRow r : repo.getChunkRows(jobId)) {
            if (!"done".equalsIgnoreCase(r.status)&&!"COMPLETED".equalsIgnoreCase(r.status)) return true;
        }
        return false;
    }

    public String exportJobBundle(long jobId) {
        TranslationRepository.Job job = repo.getJob(jobId);
        TranslationRepository.JobSummary s = repo.getJobSummary(jobId);
        List<TranslationRepository.ChunkRow> rows = repo.getChunkRows(jobId);
        StringBuilder out = new StringBuilder();
        out.append("TBL Android TXT Job Report\n");
        out.append("Generated: ").append(dateTime(System.currentTimeMillis())).append("\n");
        out.append("App version: ").append(AppBuildInfo.exportVersionLine()).append("\n\n");
        if (job == null) {
            out.append("Job #").append(jobId).append(" not found.\n");
            return out.toString();
        }
        out.append("== JOB ==\n");
        out.append("id: ").append(job.id).append("\n");
        out.append("file: ").append(empty(job.fileName, "unknown")).append("\n");
        out.append("status: ").append(empty(job.status, "unknown")).append("\n");
        out.append("sourceHash: ").append(empty(repo.getInputHash(jobId), "unknown")).append("\n");
        if (s != null) {
            out.append("language: ").append(empty(s.sourceLanguage, "?")).append(" -> ").append(empty(s.targetLanguage, "?")).append("\n");
            out.append("created: ").append(dateTime(s.createdAt)).append("\n");
            out.append("updated: ").append(dateTime(s.updatedAt)).append("\n");
            out.append("chunks: total=").append(s.totalChunks)
                    .append(", done=").append(s.doneChunks)
                    .append(", failed=").append(s.failedChunks)
                    .append(", pending=").append(s.pendingChunks).append("\n");
            out.append("inputUri: ").append(empty(s.inputUri, "")).append("\n");
            out.append("outputUri: ").append(empty(s.outputUri, "")).append("\n");
        } else {
            out.append("inputUri: ").append(empty(job.inputUri, "")).append("\n");
            out.append("outputUri: ").append(empty(job.outputUri, "")).append("\n");
        }
        out.append("\n== SETTINGS SNAPSHOT ==\n");
        try {
            AppSettings app = SettingsStore.fromJson(job.settingsJson);
            out.append("provider: ").append(empty(app.provider, "")).append("\n");
            out.append("baseUrl: ").append(empty(app.baseUrl, "")).append("\n");
            out.append("model: ").append(empty(app.model, "")).append("\n");
            out.append("sourceLanguage: ").append(empty(app.sourceLanguage, "")).append("\n");
            out.append("targetLanguage: ").append(empty(app.targetLanguage, "")).append("\n");
            out.append("chunkTokens: ").append(app.maxTokensPerChunk).append("\n");
            out.append("refineAfter: ").append(app.refineAfter).append("\n");
            out.append("bilingualOutput: ").append(app.bilingualOutput).append("\n");
            out.append("savePartialOutput: ").append(app.savePartialOutput).append("\n");
            out.append("configurationHash: ").append(HashUtil.settingsHash(app)).append("\n");
            out.append("retry: attempts=").append(app.maxAttempts).append(", initialMs=").append(app.initialRetryDelayMs).append(", maxMs=").append(app.maxRetryDelayMs).append("\n");
            out.append("overlap: enabled=").append(app.contextOverlapEnabled).append(", chars=").append(app.contextChars).append("\n");
        } catch (Exception e) {
            out.append("Could not parse settings: ").append(e.getMessage()).append("\n");
            out.append(empty(job.settingsJson, "")).append("\n");
        }
        OutputIntegrityAudit.Result integrity=OutputIntegrityAudit.audit(rows);
        out.append("\n== INTEGRITY AUDIT ==\nstatus: ").append(integrity.status()).append("\nerrors: ").append(integrity.errors).append("\nwarnings: ").append(integrity.warnings).append("\noutputHash: ").append(HashUtil.sha256(repo.assembleOutput(jobId,false))).append("\n");
        out.append("\n== CHUNK STATUS ==\n");
        TranslationRepository.MetricsSummary metrics = repo.getMetricsSummary(jobId);
        if (metrics.attempts > 0) {
            out.append("\n== TOKEN / COST METRICS ==\n");
            out.append("rawTokens: ").append(metrics.rawTokens).append("\n");
            out.append("systemTokens: ").append(metrics.systemTokens).append("\n");
            out.append("instructionTokens: ").append(metrics.instructionTokens).append("\n");
            out.append("glossaryTokens: ").append(metrics.glossaryTokens).append("\n");
            out.append("pronounTokens: ").append(metrics.pronounTokens).append("\n");
            out.append("historyContextTokens: ").append(metrics.contextTokens).append("\n");
            out.append("outputTokens: ").append(metrics.outputTokens).append("\n");
            out.append("retryTokens: ").append(metrics.retryTokens).append("\n");
            out.append("estimatedSavedInputTokensVsFull: ").append(metrics.estimatedSavedInputTokens()).append("\n");
            out.append("cachedInputTokens: ").append(metrics.cachedInputTokens).append("\n");
            out.append("uncachedInputTokens: ").append(metrics.uncachedInputTokens).append("\n");
            out.append("actualInputTokens: ").append(metrics.actualInputTokens).append("\n");
            out.append("providerUsageRows: ").append(metrics.providerUsageRows).append("\n");
            out.append("estimatedUsageRows: ").append(metrics.estimatedUsageRows).append("\n");
            out.append("unknownCostRows: ").append(metrics.unknownCostRows).append("\n");
            out.append("totalActualCost(provider-reported-or-usage-derived): ").append(CostEstimator.money(metrics.totalCost)).append("\n");
            out.append("attempts: ").append(metrics.attempts).append("; successes: ").append(metrics.successes).append("\n");
        }
        for (TranslationRepository.ChunkRow r : rows) {
            out.append("#").append(r.index + 1).append(" • ").append(empty(r.status, "unknown"));
            if (r.error != null && !r.error.trim().isEmpty()) out.append(" • ERROR: ").append(oneLine(r.error, 1000));
            out.append("\n");
            out.append("SRC: ").append(oneLine(r.source, 500)).append("\n");
            if (r.translated != null && !r.translated.trim().isEmpty()) out.append("OUT: ").append(oneLine(r.translated, 500)).append("\n");
            out.append("\n");
        }
        out.append("\n== RUNTIME LOG TAIL ==\n");
        TranslationRepository.BenchmarkSummary benchmark=repo.latestBenchmark(jobId);
        if(benchmark!=null){
            out.append("\n== LATEST LIVE A/B BENCHMARK ==\n");
            out.append("session: ").append(benchmark.id).append(" status: ").append(benchmark.status).append(" model: ").append(benchmark.model).append("\n");
            out.append("budget: ").append(benchmark.budgetUsd).append(" spent: ").append(benchmark.spentUsd).append(" samples: ").append(benchmark.sampleCount).append("\n");
            for(TranslationRepository.BenchmarkResultRow r:repo.benchmarkResults(benchmark.id)){
                out.append("chunk ").append(r.chunkIndex+1).append(" label ").append(r.blindLabel).append(" pipeline ").append(r.pipeline)
                        .append(" input/output/cached ").append(r.inputTokens).append('/').append(r.outputTokens).append('/').append(r.cachedTokens)
                        .append(" cost ").append(r.costUsd).append(" durationMs ").append(r.durationMs).append(" finish ").append(r.finishReason)
                        .append(" usageSource ").append(r.usageSource).append(" costStatus ").append(r.costStatus)
                        .append(" quality ").append(r.qualityIssues).append("\nOUTPUT:\n").append(r.output).append("\n\n");
            }
        }
        out.append(tail(LogStore.read(context), 12000)).append("\n");
        out.append("\n== API DEBUG TRACE TAIL ==\n");
        out.append(tail(DebugTraceStore.read(context), 12000)).append("\n");
        return out.toString();
    }

    /** Machine-readable report intentionally excludes API keys and document bodies. */
    public String exportJobJson(long jobId) {
        try {
            TranslationRepository.Job job=repo.getJob(jobId); if(job==null)throw new IllegalArgumentException("Job not found");
            TranslationRepository.JobSummary summary=repo.getJobSummary(jobId); JSONObject root=new JSONObject();
            root.put("schema","translate-books-job-report-v1");root.put("generatedAt",System.currentTimeMillis());root.put("appVersion",AppBuildInfo.exportVersionLine());
            JSONObject j=new JSONObject();j.put("id",job.id);j.put("fileName",job.fileName);j.put("status",job.status);j.put("inputUri",job.inputUri);j.put("outputUri",job.outputUri);j.put("inputHash",repo.getInputHash(jobId));j.put("preparedPlan",repo.preparedBatchIdentity(jobId));
            if(summary!=null){j.put("createdAt",summary.createdAt);j.put("updatedAt",summary.updatedAt);j.put("totalChunks",summary.totalChunks);j.put("doneChunks",summary.doneChunks);j.put("failedChunks",summary.failedChunks);j.put("pendingChunks",summary.pendingChunks);}root.put("job",j);
            AppSettings settings=SettingsStore.fromJson(job.settingsJson);JSONObject cfg=new JSONObject();cfg.put("provider",settings.provider);cfg.put("model",settings.model);cfg.put("sourceLanguage",settings.sourceLanguage);cfg.put("targetLanguage",settings.targetLanguage);cfg.put("maxTokensPerChunk",settings.maxTokensPerChunk);cfg.put("maxOutputTokens",settings.maxOutputTokens);cfg.put("settingsHash",HashUtil.settingsHash(settings));root.put("settings",cfg);
            List<TranslationRepository.ChunkRow> rows=repo.getChunkRows(jobId);OutputIntegrityAudit.Result audit=OutputIntegrityAudit.audit(rows);JSONObject integrity=new JSONObject();integrity.put("status",audit.status());integrity.put("errors",new JSONArray(audit.errors));integrity.put("warnings",new JSONArray(audit.warnings));integrity.put("outputHash",HashUtil.sha256(repo.assembleOutput(jobId,false)));root.put("integrity",integrity);
            JSONArray chunks=new JSONArray();for(TranslationRepository.ChunkRow row:rows){JSONObject c=new JSONObject();c.put("index",row.index);c.put("stableId",row.stableId);c.put("startOffset",row.startOffset);c.put("endOffset",row.endOffset);c.put("status",row.status);c.put("attempt",row.attempt);c.put("sourceHash",row.sourceHash);c.put("responseHash",row.responseHash);c.put("resultSource",row.resultSource);c.put("warnings",row.validationWarnings);c.put("error",row.error);JSONArray attempts=new JSONArray();for(TranslationRepository.AttemptRow x:repo.getAttemptRows(jobId,row.index)){JSONObject a=new JSONObject();a.put("attempt",x.attempt);a.put("phase",x.phase);a.put("status",x.status);a.put("networkState",x.networkState);a.put("logicalRequestId",x.logicalRequestId);a.put("providerResponseId",x.providerResponseId);a.put("requestHash",x.requestHash);a.put("responseHash",x.responseHash);a.put("provider",x.provider);a.put("model",x.model);a.put("inputTokens",x.inputTokens);a.put("cachedTokens",x.cachedTokens);a.put("outputTokens",x.outputTokens);a.put("actualCost",x.actualCost);a.put("usageSource",x.usageSource);a.put("validationResult",x.validationResult);attempts.put(a);}c.put("attempts",attempts);chunks.put(c);}root.put("chunks",chunks);
            return root.toString(2);
        } catch(Exception e){throw new IllegalStateException("Could not create JSON report: "+e.getMessage(),e);}
    }

    private static String dateTime(long t) {
        return t <= 0 ? "—" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(t));
    }
    private static String empty(String s, String def) { return s == null || s.trim().isEmpty() ? def : s.trim(); }
    private static String oneLine(String s, int max) {
        String v = s == null ? "" : s.replace('\r', ' ').replace('\n', ' ').trim();
        return v.length() <= max ? v : v.substring(0, max) + " ...";
    }
    private static String tail(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(s.length() - max);
    }
    public boolean hasFailedChunks(){return repo.getLastJobWithFailedChunks()!=null;}
    public void close(){repo.close();}
}
