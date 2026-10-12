package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Core translation coordinator extracted from TranslatorService in v2.7.x.
 *
 * This class owns file/config/chunk/prompt/API/refine primitives. The Android
 * service remains responsible only for lifecycle, notification, pause/cancel,
 * repository updates, and broadcasts. Keeping these responsibilities separate
 * prevents future UI changes from breaking translation output.
 */
public class TranslationEngine {
    public static class DeliveryUnknownException extends Exception {
        public DeliveryUnknownException(String message, Throwable cause) { super(message, cause); }
    }
    public interface CancelChecker { boolean isCancelled(); }
    public interface UsageSink { void onUsage(OpenAICompatibleClient.ChatResult result, AppSettings settings); }
    public interface EventSink {
        void onEvent(String message);
        default void onLockUsage(String phase, int oneBasedChunk, int glossaryCount, int pronounCount) {}
    }
    public interface ProviderClient { OpenAICompatibleClient.ChatResult chat(AppSettings settings,PromptPair prompt,int outputLimit,String logicalRequestId,OpenAICompatibleClient.NetworkObserver observer) throws Exception; }

    public static class PreparedInput {
        public Uri inputUri;
        public String inputText;
        public String fileName;
        public List<Chunk> chunks;
        public String inputHash;
        public String settingsHash;
    }

    private final Context appContext;
    private final TranslationRepository metricsRepository;
    private final ProviderClient providerClient;

    public TranslationEngine(Context context) {
        this.appContext = context.getApplicationContext();
        this.metricsRepository = null;
        this.providerClient = OpenAICompatibleClient::chatWithUsage;
    }

    public TranslationEngine(Context context, TranslationRepository repository) {
        this.appContext = context.getApplicationContext();
        this.metricsRepository = repository;
        this.providerClient = OpenAICompatibleClient::chatWithUsage;
    }

    public TranslationEngine(Context context,TranslationRepository repository,ProviderClient providerClient) {
        this.appContext=context.getApplicationContext();this.metricsRepository=repository;
        this.providerClient=providerClient==null?OpenAICompatibleClient::chatWithUsage:providerClient;
    }

    public ArrayList<Uri> readInputUris(Intent intent) throws Exception {
        ArrayList<Uri> inputs = new ArrayList<>();
        if (intent == null) return inputs;

        // Current UI sends a JSON string for backward compatibility with older service code.
        // Also accept StringArrayListExtra so future UI code can pass a typed list safely.
        String json = intent.getStringExtra("inputUris");
        if (json != null && !json.isEmpty()) {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) inputs.add(Uri.parse(arr.getString(i)));
        } else {
            ArrayList<String> many = intent.getStringArrayListExtra("inputUris");
            if (many != null) for (String s : many) if (s != null && !s.isEmpty()) inputs.add(Uri.parse(s));
        }
        if (inputs.isEmpty()) {
            String one = intent.getStringExtra("inputUri");
            if (one != null && !one.isEmpty()) inputs.add(Uri.parse(one));
        }
        return inputs;
    }

    public AppSettings loadExternalConfig(Intent intent, AppSettings settings) throws Exception {
        AppSettings s = settings == null ? new AppSettings() : settings;
        if (intent == null) return AppValidator.normalize(s);
        String yamlUriStr = intent.getStringExtra("yamlUri");
        String envUriStr = intent.getStringExtra("envUri");
        String glossaryUriStr = intent.getStringExtra("glossaryUri");
        String pronounUriStr = intent.getStringExtra("pronounUri");
        if (envUriStr != null && !envUriStr.isEmpty()) {
            String envText = FileUtil.readText(appContext, Uri.parse(envUriStr));
            s = AppSettings.fromEnv(EnvParser.parse(envText), s);
        }
        if (yamlUriStr != null && !yamlUriStr.isEmpty()) {
            Uri yuri = Uri.parse(yamlUriStr);
            CustomInstructions ci = YamlInstructionParser.parse(FileUtil.displayName(appContext, yuri), FileUtil.readText(appContext, yuri));
            if (ci.hasTranslation()) s.translationInstructions = ci.translation;
            if (ci.hasRefinement()) s.refinementInstructions = ci.refinement;
        }
        if (glossaryUriStr != null && !glossaryUriStr.isEmpty()) {
            Uri guri = Uri.parse(glossaryUriStr);
            s.glossaryText = FileUtil.readText(appContext, guri);
        }
        if (pronounUriStr != null && !pronounUriStr.isEmpty()) {
            Uri puri = Uri.parse(pronounUriStr);
            s.pronounText = FileUtil.readText(appContext, puri);
            s.pronounUri = pronounUriStr;
            s.pronounName = FileUtil.displayName(appContext, puri);
        }
        return AppValidator.normalize(s);
    }

    public PreparedInput prepareInput(Uri inputUri, AppSettings settings) throws Exception {
        if (inputUri == null) throw new IllegalArgumentException("Input TXT is empty");
        AppSettings s = AppValidator.normalize(settings);
        String input = FileUtil.readText(appContext, inputUri);
        String fileName = FileUtil.displayName(appContext, inputUri);
        String inputProblem = AppValidator.validateSourceText(input, fileName);
        if (inputProblem != null) throw new IllegalArgumentException(inputProblem);
        List<Chunk> chunks = Chunker.chunkText(input, s);
        if (chunks.isEmpty()) throw new IllegalArgumentException(fileName + ": không tạo được chunk từ file input");

        PreparedInput p = new PreparedInput();
        p.inputUri = inputUri;
        p.inputText = input;
        p.fileName = fileName;
        p.chunks = chunks;
        p.inputHash = HashUtil.sha256(input);
        p.settingsHash = HashUtil.settingsHash(s);
        return p;
    }

    public String translateWithRetry(Chunk chunk, String previous, AppSettings settings,
                                     CancelChecker cancelChecker, UsageSink usageSink, EventSink eventSink) throws Exception {
        return translateWithRetry(-1, chunk, previous, settings, cancelChecker, usageSink, eventSink);
    }

    public String translateWithRetry(long jobId, Chunk chunk, String previous, AppSettings settings,
                                     CancelChecker cancelChecker, UsageSink usageSink, EventSink eventSink) throws Exception {
        AppSettings s = AppValidator.normalize(settings);
        Exception last = null;
        for (int attempt = 1; attempt <= Math.max(1, s.maxAttempts); attempt++) {
            throwIfCancelled(cancelChecker);
            if(attempt>1&&metricsRepository!=null&&jobId>0&&s.stopOnCostLimit&&s.costLimitUsd>0&&metricsRepository.getMetricsSummary(jobId).totalCost>=s.costLimitUsd)throw new IllegalStateException("Cost budget reached; valid received response retained, retry paused");
            AppSettings attemptSettings = s;
            PromptPlan plan = PromptPlan.forTranslation(chunk, previous, attemptSettings);
            AppSettings baselineSettings = s.copy(); baselineSettings.optimizationPreset = "full";
            PromptPlan baselinePlan = PromptPlan.forTranslation(chunk, previous, baselineSettings);
            String logicalRequestId = jobId + ":" + chunk.index + ":translate";
            int outputLimit = "full".equalsIgnoreCase(attemptSettings.optimizationPreset) ? attemptSettings.maxOutputTokens : plan.adaptiveMaxOutputTokens(attemptSettings);
            long started = System.currentTimeMillis();
            OpenAICompatibleClient.ChatResult result = null;
            final boolean[] bodySent = {false};
            final boolean[] responseHeaders = {false};
            final int attemptNo=attempt;
            try {
                if (attempt == 1 && eventSink != null) {
                    eventSink.onLockUsage("Translating", chunk.index + 1,
                            plan.glossaryLockCount, plan.pronounLockCount);
                    eventSink.onEvent("Chunk " + (chunk.index + 1) + " locks: glossary=" + plan.glossaryLockCount
                            + ", pronoun=" + plan.pronounLockCount);
                }
                PromptPair prompt = plan.prompt;
                String requestHash = HashUtil.sha256(prompt.system + "\n" + prompt.user + "\n" + attemptSettings.provider + "\n" + attemptSettings.model);
                if (metricsRepository != null && jobId > 0) metricsRepository.beginChunkAttempt(jobId, chunk, attempt, "translate", requestHash, logicalRequestId + ":" + attempt, attemptSettings);
                ObservabilityLog.event("chunk_request","job",jobId,"stableId",chunk.stableId,"index",chunk.index,"attempt",attempt,"requestHash",requestHash,"provider",attemptSettings.provider,"model",attemptSettings.model);
                DebugTraceStore.request(appContext, "translation attempt " + attempt, chunk.index + 1, prompt, attemptSettings);
                result = providerClient.chat(attemptSettings, prompt, outputLimit, logicalRequestId + ":" + attempt, new OpenAICompatibleClient.NetworkObserver() {
                    @Override public void onRequestBodyStarted() { if (metricsRepository != null && jobId > 0) metricsRepository.markAttemptPhase(jobId,chunk.index,attemptNo,"translate","SEND_STARTED"); }
                    @Override public void onRequestBodySent(long byteCount) { bodySent[0]=true; if (metricsRepository != null && jobId > 0) metricsRepository.markAttemptPhase(jobId,chunk.index,attemptNo,"translate","REQUEST_BODY_SENT"); }
                    @Override public void onResponseHeaders(int code) { responseHeaders[0]=true; if (metricsRepository != null && jobId > 0) metricsRepository.markAttemptPhase(jobId,chunk.index,attemptNo,"translate","RESPONSE_HEADERS"); }
                });
                if (metricsRepository != null && jobId > 0) { metricsRepository.persistReceivedResponse(jobId, chunk.index, attempt,"translate", result); metricsRepository.markValidating(jobId,chunk.index,attempt,"translate"); }
                ObservabilityLog.event("chunk_response","job",jobId,"stableId",chunk.stableId,"attempt",attempt,"responseHash",HashUtil.sha256(result.content),"finishReason",result.finishReason,"usageSource",result.usageReported?"PROVIDER":"ESTIMATED","inputTokens",result.promptTokens,"outputTokens",result.completionTokens,"cachedTokens",result.cachedPromptTokens);
                if (usageSink != null) usageSink.onUsage(result, attemptSettings);
                String out = PromptBuilder.extractTranslationStrict(result.content);
                ResponseValidator.Result validation = ResponseValidator.validate(result.content, out, chunk.mainContent,
                        prompt.system + "\n" + prompt.user, result.finishReason, result.completionTokens, outputLimit, false);
                if (!validation.accepted) throw new ResponseValidator.RejectedResponseException(validation);
                ObservabilityLog.event("chunk_validation","job",jobId,"stableId",chunk.stableId,"accepted",true,"finishReason",validation.finish.name());
                TranslationQualityChecks.Result quality = TranslationQualityChecks.inspect(chunk.mainContent, out, previous);
                if (!quality.passed() && eventSink != null) eventSink.onEvent("Chunk " + (chunk.index + 1) + " quality warning: " + quality.issues);
                recordMetric(jobId, chunk.index, attempt, plan, baselinePlan, logicalRequestId, attemptSettings, outputLimit, started, result, "success", "");
                DebugTraceStore.response(appContext, "translation attempt " + attempt, chunk.index + 1, result, out);
                return out;
            } catch (Exception e) {
                throwIfCancelled(cancelChecker);
                if (result == null && bodySent[0] && !responseHeaders[0]) {
                    String message="Request body was sent but no durable provider response was stored; automatic retry is paused to prevent duplicate billing";
                    if(metricsRepository!=null&&jobId>0)metricsRepository.markDeliveryUnknown(jobId,chunk.index,attempt,"translate",message);
                    throw new DeliveryUnknownException(message,e);
                }
                last = e;
                RetryPolicy.Kind kind = RetryPolicy.classify(e);
                if (metricsRepository != null && jobId > 0 && result != null) metricsRepository.rejectResponse(jobId, chunk.index, attempt,"translate",
                        result.content, e.getMessage(), RetryPolicy.mayRetry(kind, attempt, s.maxAttempts));
                else if(metricsRepository!=null&&jobId>0&&responseHeaders[0])metricsRepository.markAttemptPhase(jobId,chunk.index,attempt,"translate","REJECTED");
                else if(metricsRepository!=null&&jobId>0&&!bodySent[0])metricsRepository.markAttemptFailedBeforeSend(jobId,chunk.index,attempt,"translate",e.getMessage());
                ObservabilityLog.event("chunk_attempt_failed","job",jobId,"stableId",chunk.stableId,"attempt",attempt,"kind",kind.name(),"errorClass",e.getClass().getSimpleName());
                recordMetric(jobId, chunk.index, attempt, plan, baselinePlan, logicalRequestId, attemptSettings, outputLimit, started, result, "failed", kind.name());
                DebugTraceStore.error(appContext, "translation attempt " + attempt, chunk.index + 1, e);
                if (eventSink != null) eventSink.onEvent("Chunk " + (chunk.index + 1) + " lỗi lần " + attempt + ": " + AppValidator.readableError(e));
                if (!RetryPolicy.mayRetry(kind, attempt, s.maxAttempts)) break;
                if((kind==RetryPolicy.Kind.EMPTY_RESPONSE&&!s.retryOnEmpty)||(kind==RetryPolicy.Kind.TRUNCATION&&!s.retryOnTruncation)||(kind==RetryPolicy.Kind.VALIDATION&&!s.retryOnValidationFailure))break;
                long delay = Math.max(RetryPolicy.backoffMs(kind, attempt,s.initialRetryDelayMs,s.maxRetryDelayMs), RetryPolicy.retryAfterMs(e));
                if (eventSink != null) eventSink.onEvent("Retry " + kind + " after " + delay + "ms");
                sleepBackoff(delay, cancelChecker);
            }
        }
        throw last == null ? new RuntimeException("Translation failed") : last;
    }

    public String refineWithRetry(String source, String draft, AppSettings settings,
                                  CancelChecker cancelChecker, UsageSink usageSink, EventSink eventSink) throws Exception {
        return refineWithRetry(-1, -1, source, draft, settings, cancelChecker, usageSink, eventSink);
    }

    /** Kept for 4.2 compatibility tests; runtime context overflow now requires source splitting. */
    @Deprecated
    static AppSettings contextFallbackSettings(AppSettings original) {
        AppSettings fallback=original.copy();fallback.optimizationPreset="balanced";fallback.contextChars=0;
        fallback.maxOutputTokens=Math.max(256,Math.min(original.maxOutputTokens,original.maxTokensPerChunk*2));
        return AppValidator.normalize(fallback);
    }

    public String refineWithRetry(long jobId, int chunkIndex, String source, String draft, AppSettings settings,
                                  CancelChecker cancelChecker, UsageSink usageSink, EventSink eventSink) throws Exception {
        Chunk legacyChunk = new Chunk(chunkIndex, "", source, "");
        legacyChunk.stableId = HashUtil.sha256(source);
        return refineWithRetryInternal(jobId, chunkIndex, legacyChunk, draft, settings,
                cancelChecker, usageSink, eventSink);
    }

    /** Range-aware refinement entry point for runtime translation and retry. */
    public String refineWithRetry(Chunk chunk, String draft, AppSettings settings,
                                  CancelChecker cancelChecker, UsageSink usageSink, EventSink eventSink) throws Exception {
        return refineWithRetry(-1, chunk, draft, settings, cancelChecker, usageSink, eventSink);
    }

    /** Range-aware refinement entry point that keeps the persisted chunk identity. */
    public String refineWithRetry(long jobId, Chunk chunk, String draft, AppSettings settings,
                                  CancelChecker cancelChecker, UsageSink usageSink, EventSink eventSink) throws Exception {
        if (chunk == null) {
            return refineWithRetry(jobId, -1, "", draft, settings, cancelChecker, usageSink, eventSink);
        }
        return refineWithRetryInternal(jobId, chunk.index, chunk, draft, settings,
                cancelChecker, usageSink, eventSink);
    }

    private String refineWithRetryInternal(long jobId, int chunkIndex, Chunk chunk, String draft, AppSettings settings,
                                           CancelChecker cancelChecker, UsageSink usageSink, EventSink eventSink) throws Exception {
        String source = chunk == null || chunk.mainContent == null ? "" : chunk.mainContent;
        AppSettings s = AppValidator.normalize(settings);
        Exception last = null;
        for (int attempt = 1; attempt <= Math.max(1, s.maxAttempts); attempt++) {
            throwIfCancelled(cancelChecker);
            PromptPlan plan = PromptPlan.forRefinement(chunk, draft, s);
            AppSettings baselineSettings = s.copy(); baselineSettings.optimizationPreset = "full";
            PromptPlan baselinePlan = PromptPlan.forRefinement(chunk, draft, baselineSettings);
            String logicalRequestId = jobId + ":" + chunkIndex + ":refine";
            int outputLimit = "full".equalsIgnoreCase(s.optimizationPreset) ? s.maxOutputTokens : plan.adaptiveMaxOutputTokens(s);
            long started = System.currentTimeMillis();
            OpenAICompatibleClient.ChatResult result=null;
            final boolean[] bodySent={false};
            final boolean[] responseHeaders={false};
            final int attemptNo=attempt;
            try {
                if (attempt == 1 && eventSink != null) {
                    eventSink.onLockUsage("Refining", chunkIndex + 1,
                            plan.glossaryLockCount, plan.pronounLockCount);
                    eventSink.onEvent("Refine chunk " + (chunkIndex + 1) + " locks: glossary="
                            + plan.glossaryLockCount + ", pronoun=" + plan.pronounLockCount);
                }
                PromptPair prompt = plan.prompt;
                Chunk durableChunk = chunk;
                String requestHash=HashUtil.sha256(prompt.system+"\n"+prompt.user+"\n"+s.provider+"\n"+s.model);
                if(metricsRepository!=null&&jobId>0)metricsRepository.beginChunkAttempt(jobId,durableChunk,attempt,"refine",requestHash,logicalRequestId+":"+attempt,s);
                DebugTraceStore.request(appContext, "refinement attempt " + attempt, 0, prompt, s);
                result = providerClient.chat(s, prompt, outputLimit, logicalRequestId + ":" + attempt,new OpenAICompatibleClient.NetworkObserver(){
                    @Override public void onRequestBodyStarted(){if(metricsRepository!=null&&jobId>0)metricsRepository.markAttemptPhase(jobId,chunkIndex,attemptNo,"refine","SEND_STARTED");}
                    @Override public void onRequestBodySent(long byteCount){bodySent[0]=true;if(metricsRepository!=null&&jobId>0)metricsRepository.markAttemptPhase(jobId,chunkIndex,attemptNo,"refine","REQUEST_BODY_SENT");}
                    @Override public void onResponseHeaders(int code){responseHeaders[0]=true;if(metricsRepository!=null&&jobId>0)metricsRepository.markAttemptPhase(jobId,chunkIndex,attemptNo,"refine","RESPONSE_HEADERS");}
                });
                if(metricsRepository!=null&&jobId>0){metricsRepository.persistReceivedResponse(jobId,chunkIndex,attempt,"refine",result);metricsRepository.markValidating(jobId,chunkIndex,attempt,"refine");}
                if (usageSink != null) usageSink.onUsage(result, s);
                String out = PromptBuilder.extractTranslationStrict(result.content);
                ResponseValidator.Result validation = ResponseValidator.validate(result.content, out, source,
                        prompt.system + "\n" + prompt.user, result.finishReason, result.completionTokens, outputLimit, false);
                if (!validation.accepted) throw new ResponseValidator.RejectedResponseException(validation);
                TranslationQualityChecks.Result quality = TranslationQualityChecks.inspect(source, out, "");
                if (!quality.passed() && eventSink != null) eventSink.onEvent("Refine quality warning: " + quality.issues);
                recordMetric(jobId, chunkIndex, attempt, plan, baselinePlan, logicalRequestId, s, outputLimit, started, result, "success", "");
                DebugTraceStore.response(appContext, "refinement attempt " + attempt, 0, result, out);
                return out;
            } catch (Exception e) {
                throwIfCancelled(cancelChecker);
                if(result==null&&bodySent[0]&&!responseHeaders[0]){String message="Refinement request was sent but no durable response was stored; automatic retry is paused";if(metricsRepository!=null&&jobId>0)metricsRepository.markDeliveryUnknown(jobId,chunkIndex,attempt,"refine",message);throw new DeliveryUnknownException(message,e);}
                last = e;
                RetryPolicy.Kind kind = RetryPolicy.classify(e);
                if(metricsRepository!=null&&jobId>0&&result!=null)metricsRepository.rejectResponse(jobId,chunkIndex,attempt,"refine",result.content,e.getMessage(),RetryPolicy.mayRetry(kind,attempt,s.maxAttempts));
                else if(metricsRepository!=null&&jobId>0&&responseHeaders[0])metricsRepository.markAttemptPhase(jobId,chunkIndex,attempt,"refine","REJECTED");
                else if(metricsRepository!=null&&jobId>0&&!bodySent[0])metricsRepository.markAttemptFailedBeforeSend(jobId,chunkIndex,attempt,"refine",e.getMessage());
                recordMetric(jobId, chunkIndex, attempt, plan, baselinePlan, logicalRequestId, s, outputLimit, started, result, "failed", kind.name());
                DebugTraceStore.error(appContext, "refinement attempt " + attempt, 0, e);
                if (eventSink != null) eventSink.onEvent("Refine lỗi lần " + attempt + ": " + AppValidator.readableError(e));
                if (!RetryPolicy.mayRetry(kind, attempt, s.maxAttempts)) break;
                if((kind==RetryPolicy.Kind.EMPTY_RESPONSE&&!s.retryOnEmpty)||(kind==RetryPolicy.Kind.TRUNCATION&&!s.retryOnTruncation)||(kind==RetryPolicy.Kind.VALIDATION&&!s.retryOnValidationFailure))break;
                long delay = Math.max(RetryPolicy.backoffMs(kind, attempt,s.initialRetryDelayMs,s.maxRetryDelayMs), RetryPolicy.retryAfterMs(e));
                if (eventSink != null) eventSink.onEvent("Refine retry " + kind + " after " + delay + "ms");
                sleepBackoff(delay, cancelChecker);
            }
        }
        throw last == null ? new RuntimeException("Refinement failed") : last;
    }

    public void updatePartialOutput(TranslationRepository repo, long jobId, Uri outputUri, AppSettings settings) throws Exception {
        String assembled=assembleOutput(repo.getChunkRows(jobId), settings != null && settings.bilingualOutput, true);
        if(!assembled.isEmpty())FileUtil.writeTextVerified(appContext, outputUri, assembled);
    }

    public String assembleOutput(List<TranslationRepository.ChunkRow> rows, boolean bilingual, boolean onlyContiguousDone) {
        StringBuilder sb = new StringBuilder();
        if (rows == null) return "";
        for (TranslationRepository.ChunkRow r : rows) {
            if (r == null) continue;
            boolean complete = "done".equalsIgnoreCase(r.status) || "COMPLETED".equalsIgnoreCase(r.status);
            if (!complete || r.translated == null || r.translated.isEmpty()) {
                if (onlyContiguousDone) break;
                continue;
            }
            if (bilingual) sb.append("[SOURCE]\n").append(r.source).append("\n[TRANSLATION]\n").append(r.translated).append("\n\n");
            else {
                sb.append(r.translated);
                if (!r.translated.endsWith("\n")) sb.append('\n');
            }
        }
        return sb.toString();
    }

    private void sleepBackoff(long total, CancelChecker cancelChecker) throws Exception {
        long slept = 0;
        while (slept < total) {
            throwIfCancelled(cancelChecker);
            long step = Math.min(300, total - slept);
            Thread.sleep(step);
            slept += step;
        }
        throwIfCancelled(cancelChecker);
    }

    private void recordMetric(long jobId, int chunkIndex, int attempt, PromptPlan p, PromptPlan baselinePlan,
                              String logicalRequestId, AppSettings s,
                              int outputLimit, long started, OpenAICompatibleClient.ChatResult result,
                              String outcome, String reason) {
        if (metricsRepository == null || jobId <= 0 || p == null) return;
        ChunkMetric m = new ChunkMetric(); m.jobId = jobId; m.chunkIndex = chunkIndex; m.attempt = attempt;
        m.phase = p.phase; m.preset = s.optimizationPreset; m.provider = s.provider; m.model = s.model;
        m.sourceChars = p.source.length(); m.rawTokens = p.rawTokens; m.systemTokens = p.systemTokens;
        m.instructionTokens = p.instructionTokens; m.glossaryTokens = p.glossaryTokens; m.pronounTokens = p.pronounTokens;
        m.historyTokens = p.historyTokens; m.contextTokens = p.contextTokens; m.estimatedInputTokens = p.totalInputTokens;
        m.maxOutputTokens = outputLimit; m.durationMs = Math.max(0, System.currentTimeMillis() - started);
        m.baselineInputTokens = baselinePlan == null ? p.totalInputTokens : baselinePlan.totalInputTokens;
        m.logicalRequestId = logicalRequestId;
        ModelCatalog.ModelInfo pricing = ModelCatalog.findModelInfo(s.provider, s.model);
        boolean hasPricing = pricing != null && pricing.hasPricing();
        if (result != null) {
            m.usageSource = result.usageReported ? "PROVIDER" : "UNKNOWN_USAGE";
            if (result.usageReported) {
                m.actualInputTokens = result.promptTokens; m.outputTokens = result.completionTokens;
                m.cachedInputTokens = result.cachedPromptTokens;
                if (result.providerCostReported) {
                    m.usageCost = result.providerCost; m.costStatus = "PROVIDER_REPORTED";
                } else if (hasPricing) {
                    m.usageCost = ModelCatalog.usageCost(pricing,result.promptTokens,result.cachedPromptTokens,result.completionTokens);
                    m.costStatus = "USAGE_DERIVED";
                } else m.costStatus = "UNKNOWN_PRICING";
            } else if (result.providerCostReported) {
                m.usageCost = result.providerCost; m.costStatus = "PROVIDER_REPORTED";
            } else m.costStatus = "UNKNOWN_USAGE";
        }
        m.retryTokens = attempt > 1 ? (result == null ? p.totalInputTokens : result.promptTokens + result.completionTokens) : 0;
        m.estimatedCost = CostEstimator.cost(p.totalInputTokens, Math.min(outputLimit, Math.max(1, p.rawTokens)), s);
        m.outcome = outcome; m.retryReason = reason; metricsRepository.recordMetric(m);
        ObservabilityLog.event("actual_usage", "provider", s.provider, "model", s.model,
                "usageSource", m.usageSource, "costSource", m.costStatus,
                "input", m.actualInputTokens, "cached", m.cachedInputTokens,
                "output", m.outputTokens, "retry", m.retryTokens, "cost", m.usageCost);
    }

    private void throwIfCancelled(CancelChecker cancelChecker) throws InterruptedException {
        if (cancelChecker != null && cancelChecker.isCancelled()) throw new InterruptedException("Cancelled");
    }
}
