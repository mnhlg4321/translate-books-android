package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialDiffValidator;
import com.ml.tblandroidtxt.editorial.pack.EditorialLedgerValidator;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5L1Output;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireResponse;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;
import com.ml.tblandroidtxt.editorial.pack.EditorialStopDecision;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

/**
 * The one P5C provider adapter for the already supported OpenRouter client.
 * It converts the app-owned request envelope to a bounded chat request and
 * converts model JSON to typed data; it never lets model text mutate state.
 */
public final class OpenRouterEditorialP5PilotProvider implements EditorialP5PilotProvider {
    private static final String REPORT_SCHEMA = "safe4.full.report-l1.v1";

    private final AppSettings settings;
    private final int maximumOutputTokens;
    private final NetworkLifecycleRecorder lifecycleRecorder;
    private final boolean freshRawRouting;
    /** The single L1 phase a fresh-route adapter may dispatch; empty when not fresh. */
    private final String freshPhase;
    private final Map<String, EditorialP5L1Output> parsedOutputs = new ConcurrentHashMap<>();
    private final OpenAICompatibleClient.CallControl rawCallControl =
            new OpenAICompatibleClient.CallControl();
    private final AtomicLong attemptDeadlineNanos = new AtomicLong(0L);

    public OpenRouterEditorialP5PilotProvider(AppSettings settings, int maximumOutputTokens) {
        this(settings, maximumOutputTokens, null);
    }

    /** Optional app-owned recorder for redacted P5D lifecycle evidence. */
    public OpenRouterEditorialP5PilotProvider(AppSettings settings, int maximumOutputTokens,
                                              NetworkLifecycleRecorder lifecycleRecorder) {
        this(settings, maximumOutputTokens, lifecycleRecorder, false);
    }

    private OpenRouterEditorialP5PilotProvider(AppSettings settings, int maximumOutputTokens,
                                               NetworkLifecycleRecorder lifecycleRecorder,
                                               boolean freshRawRouting) {
        this(settings, maximumOutputTokens, lifecycleRecorder,
                freshRawRouting ? "L1_RAW_DISCOVERY" : "");
    }

    private OpenRouterEditorialP5PilotProvider(AppSettings settings, int maximumOutputTokens,
                                               NetworkLifecycleRecorder lifecycleRecorder,
                                               String freshPhase) {
        if (settings == null) throw new IllegalArgumentException("OpenRouter settings are required");
        this.settings = settings.copy();
        if (maximumOutputTokens <= 0) {
            throw new IllegalArgumentException("Output token cap is invalid");
        }
        this.maximumOutputTokens = maximumOutputTokens;
        this.lifecycleRecorder = lifecycleRecorder;
        this.freshPhase = freshPhase;
        this.freshRawRouting = !freshPhase.isEmpty();
    }

    /** Creates the one P5D live adapter with durable, redacted lifecycle evidence. */
    public static OpenRouterEditorialP5PilotProvider withLifecyclePersistence(
            AppSettings settings, int maximumOutputTokens, TranslationRepository database) {
        EditorialP5CAttemptStore store = new EditorialP5CAttemptStore(
                java.util.Objects.requireNonNull(database, "database"));
        return new OpenRouterEditorialP5PilotProvider(settings, maximumOutputTokens,
                store::recordNetworkLifecycle);
    }

    /**
     * Creates the fresh RAW-only adapter. It is not used by the preflight
     * runner; a future live runner must obtain a separate authorization before
     * invoking the existing executeRaw entry point.
     */
    public static OpenRouterEditorialP5PilotProvider forFreshRaw(
            AppSettings settings, int maximumOutputTokens) {
        return new OpenRouterEditorialP5PilotProvider(settings, maximumOutputTokens,
                null, true);
    }

    /**
     * Fresh-route adapter for the separately authorized M4 RECONCILE call. It
     * dispatches only L1_RECONCILE on the qualified route; RAW stays refused.
     */
    public static OpenRouterEditorialP5PilotProvider withFreshReconcileLifecyclePersistence(
            AppSettings settings, int maximumOutputTokens, TranslationRepository database) {
        EditorialP5CAttemptStore store = new EditorialP5CAttemptStore(
                java.util.Objects.requireNonNull(database, "database"));
        return new OpenRouterEditorialP5PilotProvider(settings, maximumOutputTokens,
                store::recordNetworkLifecycle, "L1_RECONCILE");
    }

    /** Fresh RAW adapter with the same redacted lifecycle recorder as P5D. */
    public static OpenRouterEditorialP5PilotProvider withFreshRawLifecyclePersistence(
            AppSettings settings, int maximumOutputTokens, TranslationRepository database) {
        EditorialP5CAttemptStore store = new EditorialP5CAttemptStore(
                java.util.Objects.requireNonNull(database, "database"));
        return new OpenRouterEditorialP5PilotProvider(settings, maximumOutputTokens,
                store::recordNetworkLifecycle, true);
    }

    /**
     * Builds the exact fresh RAW request locally for a zero-call preflight.
     * This method only renders JSON; it does not validate an API key, create
     * an attempt, record lifecycle state or execute an HTTP call.
     */
    static JSONObject buildFreshRawRequestBodyForPreflight(
            AppSettings settings, Request request, int maximumOutputTokens) throws Exception {
        if (!EditorialP5EFreshRawRoutingPolicy.matches(settings)) {
            throw new IllegalArgumentException("P5E_FRESH_RAW_ROUTE_SETTINGS_MISMATCH");
        }
        if (request == null || request.context() == null
                || !"L1_RAW_DISCOVERY".equals(request.phase())
                || request.callKind() != EditorialP5PilotProvider.CallKind.PRIMARY_SEMANTIC
                || !EditorialP5EFreshRawRoutingPolicy.PROVIDER.equalsIgnoreCase(request.provider())
                || !EditorialP5EFreshRawRoutingPolicy.MODEL.equals(request.model())) {
            throw new IllegalArgumentException("P5E_FRESH_RAW_ROUTE_REQUEST_MISMATCH");
        }
        OpenRouterEditorialP5PilotProvider renderer =
                new OpenRouterEditorialP5PilotProvider(settings, maximumOutputTokens, null, true);
        return OpenAICompatibleClient.buildChatRequestBody(settings,
                renderer.buildPrompt(request), maximumOutputTokens, rawResponseFormat(),
                EditorialP5EFreshRawRoutingPolicy.providerPreferences(),
                EditorialP5RawWireContract.REASONING_POLICY);
    }

    @FunctionalInterface
    public interface NetworkLifecycleRecorder {
        void record(String attemptIdentity, EditorialP5CAttemptStore.NetworkLifecycleEvent event);
    }

    public boolean configured() {
        boolean base = settings.provider != null && settings.provider.toLowerCase(java.util.Locale.ROOT)
                .contains("openrouter")
                && settings.apiKey != null && !settings.apiKey.trim().isEmpty()
                && settings.model != null && !settings.model.trim().isEmpty()
                && settings.baseUrl != null && !settings.baseUrl.trim().isEmpty();
        return base && (!freshRawRouting || EditorialP5EFreshRawRoutingPolicy.matches(settings));
    }

    @Override public void beginAttempt(long maximumExecutionTimeMillis) {
        attemptDeadlineNanos.set(OpenAICompatibleClient.monotonicDeadlineNanosFromNowMillis(
                maximumExecutionTimeMillis));
        rawCallControl.reset();
    }

    /** Test/owner hook for an explicit pilot cancellation, not the legacy global cancel. */
    void cancelRawCall() { rawCallControl.cancel("PILOT_REQUESTED_CANCEL"); }

    @Override public Response call(Request request) throws Exception {
        if (request == null || request.context() == null) {
            return invalidResponse("REQUEST_CONTEXT_MISSING", new byte[0]);
        }
        if (freshRawRouting && (!freshPhase.equals(request.phase())
                || request.callKind() != EditorialP5PilotProvider.CallKind.PRIMARY_SEMANTIC
                || !EditorialP5EFreshRawRoutingPolicy.PROVIDER.equalsIgnoreCase(request.provider())
                || !EditorialP5EFreshRawRoutingPolicy.MODEL.equals(request.model()))) {
            throw new IllegalStateException("P5E_FRESH_RAW_ROUTE_PHASE_OR_MODEL_INVALID");
        }
        if (!configured()) throw new IllegalStateException("OPENROUTER_CONFIGURATION_INCOMPLETE");

        PromptPair prompt = buildPrompt(request);
        OpenRouterLifecycleObserver lifecycle = new OpenRouterLifecycleObserver(
                request.attemptIdentity(), lifecycleRecorder);
        OpenAICompatibleClient.ChatResult result;
        long deadlineNanos = attemptDeadlineNanos.get();
        try {
            boolean rawDiscovery = isL1WirePhase(request.phase());
            if (freshRawRouting) {
                result = OpenAICompatibleClient.chatWithUsage(settings, prompt, maximumOutputTokens,
                        request.attemptIdentity(), lifecycle, false, deadlineNanos, rawCallControl,
                        rawResponseFormat(), EditorialP5EFreshRawRoutingPolicy.providerPreferences(),
                        EditorialP5RawWireContract.REASONING_POLICY);
            } else {
                result = OpenAICompatibleClient.chatWithUsage(settings, prompt, maximumOutputTokens,
                        request.attemptIdentity(), lifecycle, false, deadlineNanos, rawCallControl,
                        rawDiscovery ? rawResponseFormat() : null,
                        rawDiscovery, rawDiscovery ? EditorialP5RawWireContract.REASONING_POLICY : "");
            }
        } catch (Exception error) {
            throw new ProviderFailure(failureReason(error, lifecycle, deadlineNanos), error);
        }
        requireFinishReason(result.finishReason);
        byte[] responseBytes = result.content.getBytes(StandardCharsets.UTF_8);
        EditorialP5L1Output output = null;
        boolean schemaValid = false;
        try {
            output = parseOutput(result.content, request);
            schemaValid = true;
            parsedOutputs.put(request.attemptIdentity(), output);
        } catch (RuntimeException | JSONException invalid) {
            // A response that cannot be represented as the typed contract is
            // returned as schema-invalid. The engine decides whether a single
            // safe repair is possible; this adapter never invents semantics.
            P5ERawDiagnostics.parseRejected(invalid, responseBytes.length);
        }
        CostResolution cost = reportedOrEstimated(result, request);
        P5ERawDiagnostics.chat(result.finishReason, responseBytes.length, result.promptTokens,
                result.completionTokens, result.reasoningTokens, result.totalTokens,
                result.providerCostReported, cost.known(), cost.value());
        String responseId = result.providerResponseId == null || result.providerResponseId.isBlank()
                ? "openrouter-" + EditorialCanonicalJson.sha256Hex(responseBytes)
                : result.providerResponseId;
        boolean complete = !isTruncation(result.finishReason);
        return new Response(responseId, responseBytes,
                result.finishReason,
                complete, result.promptTokens, result.completionTokens, result.reasoningTokens,
                result.totalTokens, cost.value(), output, schemaValid,
                result.providerCostReported, cost.known());
    }

    private static String failureReason(Throwable error, OpenRouterLifecycleObserver lifecycle,
                                        long deadlineNanos) {
        Throwable root = error;
        while (root.getCause() != null && root != root.getCause()) root = root.getCause();
        if (lifecycle.cancelled || root instanceof CancellationException) {
            return "RETRY_PROVIDER_CANCELLED";
        }
        if (deadlineNanos > 0 && System.nanoTime() >= deadlineNanos) {
            return "RETRY_PROVIDER_CALL_TIMEOUT";
        }
        if (root instanceof ApiHttpException) return "RETRY_PROVIDER_HTTP_ERROR";
        if (root instanceof OpenAICompatibleClient.ResponseBodyTooLargeException) {
            return "RETRY_PROVIDER_RESPONSE_TOO_LARGE";
        }
        if (root instanceof UnknownHostException) return "RETRY_PROVIDER_DNS_FAILED";
        if (root instanceof SSLHandshakeException) return "RETRY_PROVIDER_TLS_FAILED";
        if (root instanceof SSLException) return "RETRY_PROVIDER_TLS_FAILED";
        if (root instanceof ConnectException) return "RETRY_PROVIDER_CONNECT_FAILED";
        if (root instanceof SocketTimeoutException || root instanceof InterruptedIOException) {
            if (lifecycle.bodyStarted && !lifecycle.bodySent) {
                return "RETRY_PROVIDER_WRITE_TIMEOUT";
            }
            if (lifecycle.bodySent && !lifecycle.headersReceived) {
                return "RETRY_PROVIDER_READ_TIMEOUT";
            }
            return "RETRY_PROVIDER_CALL_TIMEOUT";
        }
        if (root instanceof JSONException || lifecycle.bodyComplete) {
            return "RETRY_PROVIDER_RESPONSE_PARSE_FAILED";
        }
        return "RETRY_PROVIDER_CALL_FAILED_UNKNOWN";
    }

    private static final class OpenRouterLifecycleObserver implements OpenAICompatibleClient.NetworkObserver {
        private final String attemptIdentity;
        private final NetworkLifecycleRecorder recorder;
        private EditorialP5CAttemptStore.LifecycleStage lastStage;
        private boolean bodyStarted;
        private boolean bodySent;
        private boolean headersReceived;
        private boolean bodyComplete;
        private boolean cancelled;
        private long requestBodyBytes;
        private int httpStatus = -1;
        private String responseContentType = "";
        private String generationId = "";
        private String providerResponseId = "";
        private String cancellationSource = "";
        private long responseBodyBytes;

        private OpenRouterLifecycleObserver(String attemptIdentity,
                                             NetworkLifecycleRecorder recorder) {
            this.attemptIdentity = attemptIdentity;
            this.recorder = recorder;
        }

        @Override public void onCallCreated() {
            emit(EditorialP5CAttemptStore.LifecycleStage.CALL_CREATED, "", 0L);
        }

        @Override public void onCallCreated(long elapsedMillis) {
            emit(EditorialP5CAttemptStore.LifecycleStage.CALL_CREATED, "", elapsedMillis);
        }

        @Override public void onRequestBodyStarted() {
            bodyStarted = true;
            emit(EditorialP5CAttemptStore.LifecycleStage.REQUEST_BODY_STARTED, "", 0L);
        }

        @Override public void onRequestBodyStarted(long elapsedMillis) {
            bodyStarted = true;
            emit(EditorialP5CAttemptStore.LifecycleStage.REQUEST_BODY_STARTED, "", elapsedMillis);
        }

        @Override public void onRequestBodySent(long byteCount) {
            bodySent = true;
            requestBodyBytes = Math.max(0L, byteCount);
            emit(EditorialP5CAttemptStore.LifecycleStage.REQUEST_BODY_SENT, "", 0L);
        }

        @Override public void onRequestBodySent(long byteCount, long elapsedMillis) {
            bodySent = true;
            requestBodyBytes = Math.max(0L, byteCount);
            emit(EditorialP5CAttemptStore.LifecycleStage.REQUEST_BODY_SENT, "", elapsedMillis);
        }

        @Override public void onResponseHeaders(int code, String generationId) {
            onResponseHeaders(code, generationId, "");
        }

        @Override public void onResponseHeaders(int code, String generationId,
                                                String contentType) {
            onResponseHeaders(code, generationId, contentType, 0L);
        }

        @Override public void onResponseHeaders(int code, String generationId,
                                                String contentType, long elapsedMillis) {
            headersReceived = true;
            httpStatus = code;
            this.generationId = generationId == null ? "" : generationId;
            this.responseContentType = contentType == null ? "" : contentType;
            emit(EditorialP5CAttemptStore.LifecycleStage.RESPONSE_HEADERS_RECEIVED, "",
                    elapsedMillis);
        }

        @Override public void onResponseBodyProgress(long byteCount, long elapsedMillis) {
            responseBodyBytes = Math.max(responseBodyBytes, Math.max(0L, byteCount));
            emit(EditorialP5CAttemptStore.LifecycleStage.RESPONSE_BODY_PROGRESS, "",
                    elapsedMillis);
        }

        @Override public void onResponseBodyComplete(long byteCount, String providerResponseId) {
            onResponseBodyComplete(byteCount, providerResponseId, 0L);
        }

        @Override public void onResponseBodyComplete(long byteCount, String providerResponseId,
                                                      long elapsedMillis) {
            bodyComplete = true;
            responseBodyBytes = Math.max(responseBodyBytes, Math.max(0L, byteCount));
            if (providerResponseId != null && !providerResponseId.isBlank()) {
                this.providerResponseId = providerResponseId;
            }
            emit(EditorialP5CAttemptStore.LifecycleStage.RESPONSE_BODY_COMPLETE, "", elapsedMillis);
        }

        @Override public void onCallCancelled(long elapsedMillis) {
            onCallCancelled(elapsedMillis, "");
        }

        @Override public void onCallCancelled(long elapsedMillis, String cancellationSource) {
            cancelled = true;
            this.cancellationSource = cancellationSource == null ? "" : cancellationSource;
            emit(EditorialP5CAttemptStore.LifecycleStage.CALL_CANCELLED, "", elapsedMillis);
        }

        @Override public void onCallFailed(String exceptionClass, long elapsedMillis) {
            emit(EditorialP5CAttemptStore.LifecycleStage.CALL_FAILED, exceptionClass, elapsedMillis);
        }

        private void emit(EditorialP5CAttemptStore.LifecycleStage stage,
                          String exceptionClass, long elapsedMillis) {
            lastStage = stage;
            if (recorder != null) recorder.record(attemptIdentity,
                    new EditorialP5CAttemptStore.NetworkLifecycleEvent(stage,
                            requestBodyBytes, responseBodyBytes, httpStatus, responseContentType,
                            exceptionClass, elapsedMillis, generationId, providerResponseId,
                            cancellationSource));
        }
    }

    private record CostResolution(BigDecimal value, boolean known) { }

    private static CostResolution reportedOrEstimated(OpenAICompatibleClient.ChatResult result,
                                                       Request request) {
        if (result.providerCostReported && Double.isFinite(result.providerCost)
                && result.providerCost >= 0) {
            return new CostResolution(BigDecimal.valueOf(result.providerCost), true);
        }
        ModelCatalog.ModelInfo model = ModelCatalog.findModelInfo(request.provider(), request.model());
        double estimate = ModelCatalog.usageCost(model, result.promptTokens,
                result.cachedPromptTokens, result.completionTokens);
        return Double.isFinite(estimate) && estimate >= 0
                ? new CostResolution(BigDecimal.valueOf(estimate), true)
                : new CostResolution(BigDecimal.ZERO, false);
    }

    private static Response invalidResponse(String reason, byte[] bytes) {
        return new Response("openrouter-schema-" + reason.toLowerCase(java.util.Locale.ROOT),
                bytes, "schema_invalid", true, 0, 0, 0, 0, BigDecimal.ZERO,
                null, false, false, false);
    }

    /**
     * Exact syntax the app enforces on the compact wire object. A live response was rejected with
     * finding.evidenceRefs_contains_invalid_token because this contract was never stated to the
     * model; keep these rules in sync with {@link EditorialP5RawWireContract}.
     */
    static final String RAW_WIRE_FORMAT_RULES =
            "Syntax rules enforced by the app (a violation rejects the whole response):\n"
            + "- itemId and every evidenceRefs entry: 1-48 ASCII characters matching [A-Za-z0-9][A-Za-z0-9._:/-]*; "
            + "never spaces, quotes, brackets, accents or other symbols (invalid: \"RAW line 12\"; valid: \"chapter:001\", \"evidence:raw\").\n"
            + "- findings contains exactly one entry per populationIds value, itemId copied exactly; a PROCESSED entry needs 1-2 evidenceRefs tokens.\n"
            + "- gateObservations lists every gate ID in the envelope with PASS or NOT_APPLICABLE; declaredChanges is [].\n"
            + "- disposition.phase is L1; blockingGate is one gate ID or NONE; for CONTINUE or PRESERVE_DRAFT use stopClass NONE and retryable false.\n"
            + "- reasonCode, affectedScope, recoveryAction, resumeFrom: at most 32 characters, start with a letter or digit, then only letters, digits, spaces and . _ : / ; ( ) -.\n";

    /** Both L1 phases use the compact wire; only the materialized base text differs. */
    static boolean isL1WirePhase(String phase) {
        return "L1_RAW_DISCOVERY".equals(phase) || "L1_RECONCILE".equals(phase);
    }

    private PromptPair buildPrompt(Request request) {
        boolean rawDiscovery = "L1_RAW_DISCOVERY".equals(request.phase());
        boolean reconcile = "L1_RECONCILE".equals(request.phase());
        StringBuilder system = new StringBuilder();
        system.append("You are an untrusted SAFE4 L1 analysis assistant. The app is the authority.\n")
                .append("Return exactly one JSON object and no Markdown or commentary.\n")
                .append("Do not declare certification, change state, choose a pack, or invent hashes.\n")
                .append("For L1_RAW_DISCOVERY, use only the visible RAW and GLOSSARY blocks.\n")
                .append("For L1_RECONCILE, use only the visible blocks supplied below.\n")
                .append(reconcile
                        ? "RECONCILE is a compact wire response. Never return source text, beforeText, afterText, canonical pack/profile bytes, or app-owned identities except the two replay echoes required below. declaredChanges must be an empty array; L1 never edits the draft.\n"
                        : rawDiscovery
                        ? "RAW discovery is a compact wire response. Never return source text, beforeText, afterText, canonical pack/profile bytes, or app-owned identities except the two replay echoes required below. declaredChanges must be an empty array.\n"
                        : "The JSON must contain the exact identity values from the envelope, all gate IDs, an exhaustive ledger, and disposition metadata.\n")
                .append("\n")
                .append("[PROJECT_INSTRUCTION]\n")
                .append(authority(request, com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole.PROJECT_INSTRUCTION))
                .append("\n[/PROJECT_INSTRUCTION]\n[TURN_PROMPT]\n")
                .append(authority(request, com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole.TURN_PROMPT))
                .append("\n[/TURN_PROMPT]\n[WORKFLOW]\n")
                .append(authority(request, com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole.WORKFLOW))
                .append("\n[/WORKFLOW]");

        Map<String, Object> envelope = new TreeMap<>();
        EditorialP5PilotProvider.Request.Context context = request.context();
        envelope.put("contractVersion", EditorialSafe4Contract.CONTRACT_VERSION);
        envelope.put("receiptSchemaVersion", EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION);
        envelope.put("reportSchemaVersion", REPORT_SCHEMA);
        envelope.put("callKind", request.callKind().name());
        envelope.put("phaseActivation", request.phase());
        envelope.put("attemptIdentity", request.attemptIdentity());
        envelope.put("requestEnvelopeHash", request.requestEnvelopeHash());
        envelope.put("bindingIdentity", context.bindingIdentity());
        envelope.put("runDeclarationIdentity", context.runDeclarationIdentity());
        envelope.put("manifestFingerprint", context.manifestFingerprint());
        envelope.put("bundleIdentity", context.bundleIdentity());
        envelope.put("predecessorIdentity", context.predecessorIdentity());
        envelope.put("chapterKey", request.chapterKey());
        envelope.put("stableAnchors", context.stableAnchors());
        envelope.put("populationIds", context.populationIds());
        envelope.put("outputPhase", "L1");
        envelope.put("priorSemanticFingerprint", request.priorSemanticFingerprint());
        envelope.put("gateIds", EditorialSafe4Contract.GATE_IDS);

        StringBuilder user = new StringBuilder();
        user.append("APP-OWNED REQUEST ENVELOPE (identities are context; only the explicitly permitted replay echoes may be returned):\n")
                .append(EditorialCanonicalJson.canonicalize(envelope))
                .append("\n\nVISIBLE SOURCE BLOCKS. Do not infer or request hidden roles:\n");
        ArrayList<String> roles = new ArrayList<>(request.visibleSources().keySet());
        Collections.sort(roles);
        for (String role : roles) {
            user.append("\n--- ").append(role).append(" ---\n")
                    .append(new String(request.visibleSources().get(role), StandardCharsets.UTF_8))
                    .append("\n--- END ").append(role).append(" ---\n");
        }
        if (rawDiscovery || reconcile) {
            user.append(reconcile
                    ? "\nReturn only this compact wire object. The app materializes beforeText=afterText from the exact pinned DRAFT bytes and builds the final REPORT_L1/receipt itself.\n"
                    : "\nReturn only this compact wire object. The app materializes beforeText=afterText from the exact pinned RAW bytes and builds the final REPORT_L1/receipt itself.\n")
                    .append("Hard limits: findings<=").append(EditorialP5RawWireContract.MAX_FINDINGS)
                    .append(", evidenceRefs<=").append(EditorialP5RawWireContract.MAX_EVIDENCE_REFS)
                    .append(", preservedInventory<=").append(EditorialP5RawWireContract.MAX_PRESERVED_ITEMS)
                    .append(", each ID/ref<=").append(EditorialP5RawWireContract.MAX_REF_LENGTH)
                    .append("; no chapter-sized free-form value.\n")
                    .append(RAW_WIRE_FORMAT_RULES)
                    .append("{\"wireSchemaVersion\":\"safe4.raw.discovery.wire.v1\",\"attemptIdentity\":\"<exact replay echo>\",\"requestEnvelopeHash\":\"<exact replay echo>\",\"findings\":[{\"itemId\":\"...\",\"disposition\":\"PROCESSED|PRESERVE_DRAFT|NOT_EVALUATED\",\"evidenceRefs\":[\"...\"],\"modelDeclaredPass\":false}],\n")
                    .append("\"gateObservations\":{\"ARTIFACT_IDENTITY\":\"PASS|NOT_APPLICABLE\",...},\"evidenceRefs\":[\"...\"],\"preservedInventory\":[],\"declaredChanges\":[],\n")
                    .append("\"disposition\":{\"disposition\":\"CONTINUE|PRESERVE_DRAFT|STOP\",\"reasonCode\":\"...\",\"phase\":\"L1\",\"blockingGate\":\"...\",\"evidenceRefs\":[],\"affectedScope\":\"...\",\"recoveryAction\":\"...\",\"resumeFrom\":\"...\",\"stopClass\":\"NONE|INPUT_REQUIRED|REPAIR_REQUIRED|RETRY_REQUIRED|CONTENT_BLOCKED\",\"retryable\":false},\"modelDeclaredPass\":false}\n");
        } else {
            user.append("\nReturn this object shape exactly. beforeText and afterText must be present; for discovery use equal text and an empty declaredChanges array when no edit is made.\n")
                    .append("{\"reportSchemaVersion\":\"safe4.full.report-l1.v1\",\"receiptSchemaVersion\":\"safe4.full.receipt.v1\",\n")
                    .append("\"bindingIdentity\":\"...\",\"manifestFingerprint\":\"...\",\"chapterKey\":\"...\",\"phase\":\"L1\",\n")
                    .append("\"bundleIdentity\":\"...\",\"predecessorIdentity\":\"...\",\"stableAnchors\":[...],\n")
                    .append("\"ledger\":{\"populationIds\":[...],\"entries\":[{\"itemId\":\"...\",\"disposition\":\"PROCESSED|PRESERVE_DRAFT|NOT_EVALUATED\",\"evidenceRefs\":[\"...\"],\"modelDeclaredPass\":false}]},\n")
                    .append("\"gates\":{\"ARTIFACT_IDENTITY\":\"PASS|NOT_APPLICABLE\",...},\n")
                    .append("\"preservedInventory\":[],\"declaredChanges\":[],\"beforeText\":\"\",\"afterText\":\"\",\n")
                    .append("\"releaseAttemptCount\":0,\"disposition\":{\"disposition\":\"CONTINUE|PRESERVE_DRAFT|STOP\",\"reasonCode\":\"...\",\"phase\":\"L1\",\"blockingGate\":\"...\",\"evidenceRefs\":[],\"affectedScope\":\"...\",\"recoveryAction\":\"...\",\"resumeFrom\":\"...\",\"retryable\":false},\n")
                    .append("\"evidenceRefs\":[\"...\"],\"modelDeclaredPass\":false}\n");
        }
        return new PromptPair(system.toString(), user.toString());
    }

    private static String authority(Request request,
                                    com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole role) {
        byte[] bytes = request.authority().bytes(role);
        return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
    }

    private static JSONObject rawResponseFormat() throws JSONException {
        JSONObject schema = new JSONObject(EditorialCanonicalJson.canonicalize(
                EditorialP5RawWireContract.jsonSchema()));
        return new JSONObject()
                .put("type", "json_schema")
                .put("json_schema", new JSONObject()
                        .put("name", EditorialP5RawWireContract.SCHEMA_NAME)
                        .put("strict", true)
                        .put("schema", schema));
    }

    /** Routes RAW discovery to the compact wire parser; final output remains typed. */
    public static EditorialP5L1Output parseOutput(String raw, Request request)
            throws JSONException {
        if (request != null && isL1WirePhase(request.phase())) {
            return parseRawOutput(raw, request).materialize(request);
        }
        return parseCanonicalOutput(raw, request);
    }

    /** Strict parser for the unchanged full canonical artifact shape. */
    public static EditorialP5L1Output parseCanonicalOutput(String raw, Request request)
            throws JSONException {
        if (raw == null || raw.isBlank() || request == null || request.context() == null) {
            throw new IllegalArgumentException("typed L1 response is incomplete");
        }
        JSONObject root = new JSONObject(raw.trim());
        JSONArray anchors = root.getJSONArray("stableAnchors");
        JSONObject ledgerObject = root.getJSONObject("ledger");
        List<String> stableAnchors = strings(anchors, "stableAnchors");
        List<String> populationIds = strings(ledgerObject.getJSONArray("populationIds"), "populationIds");
        ArrayList<EditorialLedgerValidator.Entry> entries = new ArrayList<>();
        JSONArray rawEntries = ledgerObject.getJSONArray("entries");
        for (int i = 0; i < rawEntries.length(); i++) {
            JSONObject item = rawEntries.getJSONObject(i);
            entries.add(new EditorialLedgerValidator.Entry(item.getString("itemId"),
                    item.getString("disposition"), strings(item.getJSONArray("evidenceRefs"), "evidenceRefs"),
                    item.optBoolean("modelDeclaredPass", false)));
        }
        JSONObject rawGates = root.getJSONObject("gates");
        Map<String, String> gates = new TreeMap<>();
        java.util.Iterator<String> gateKeys = rawGates.keys();
        while (gateKeys.hasNext()) {
            String key = gateKeys.next();
            gates.put(key, rawGates.getString(key));
        }
        ArrayList<EditorialDiffValidator.DeclaredChange> changes = new ArrayList<>();
        JSONArray rawChanges = root.getJSONArray("declaredChanges");
        for (int i = 0; i < rawChanges.length(); i++) {
            JSONObject change = rawChanges.getJSONObject(i);
            changes.add(new EditorialDiffValidator.DeclaredChange(change.getInt("lineNumber"),
                    change.getString("beforeHash"), change.getString("afterHash"),
                    change.getString("errorId")));
        }
        JSONObject rawDisposition = root.getJSONObject("disposition");
        EditorialStopDecision.Decision disposition = decision(rawDisposition);
        return new EditorialP5L1Output(
                root.getString("reportSchemaVersion"), root.getString("receiptSchemaVersion"),
                root.getString("bindingIdentity"), root.getString("manifestFingerprint"),
                root.getString("chapterKey"), root.getString("phase"),
                root.getString("bundleIdentity"), root.getString("predecessorIdentity"),
                stableAnchors, new EditorialLedgerValidator.Request(populationIds, entries), gates,
                strings(root.getJSONArray("preservedInventory"), "preservedInventory"), changes,
                root.getString("beforeText"), root.getString("afterText"),
                root.getInt("releaseAttemptCount"), disposition,
                Set.copyOf(strings(root.getJSONArray("evidenceRefs"), "evidenceRefs")),
                root.optBoolean("modelDeclaredPass", false));
    }

    /**
     * Parses only the bounded RAW wire DTO. It intentionally has no fields
     * capable of carrying chapter text or final app-owned identities.
     */
    public static EditorialP5RawWireResponse parseRawOutput(String raw, Request request)
            throws JSONException {
        if (raw == null || raw.isBlank() || request == null || request.context() == null) {
            throw new IllegalArgumentException("compact RAW response is incomplete");
        }
        byte[] rawBytes = raw.getBytes(StandardCharsets.UTF_8);
        if (rawBytes.length > EditorialP5RawWireContract.MAX_WIRE_BYTES) {
            throw new IllegalArgumentException("RAW_WIRE_BYTE_LIMIT_EXCEEDED");
        }
        Map<String, Object> root = EditorialCanonicalJson.parseObject(rawBytes);
        requireKeys(root, Set.of("wireSchemaVersion", "attemptIdentity", "requestEnvelopeHash",
                "findings", "gateObservations", "evidenceRefs", "preservedInventory",
                "declaredChanges", "disposition", "modelDeclaredPass"), "RAW wire");
        String schema = mapString(root, "wireSchemaVersion");
        if (!EditorialP5RawWireContract.SCHEMA_VERSION.equals(schema)) {
            throw new IllegalArgumentException("RAW_WIRE_SCHEMA_INVALID");
        }
        if (!EditorialP5RawWireContract.SCHEMA_VERSION.equals(request.outputSchemaId())) {
            throw new IllegalArgumentException("RAW_WIRE_REQUEST_SCHEMA_MISMATCH");
        }
        String attemptIdentity = mapString(root, "attemptIdentity");
        String requestEnvelopeHash = mapString(root, "requestEnvelopeHash");
        if (!attemptIdentity.equals(request.attemptIdentity())) {
            throw new IllegalArgumentException("RAW wire attempt identity mismatch");
        }
        if (!requestEnvelopeHash.equals(request.requestEnvelopeHash())) {
            throw new IllegalArgumentException("RAW wire request envelope mismatch");
        }

        List<Object> findingValues = mapArray(root, "findings");
        if (findingValues.size() > EditorialP5RawWireContract.MAX_FINDINGS) {
            throw new IllegalArgumentException("RAW_WIRE_FINDINGS_LIMIT_EXCEEDED");
        }
        ArrayList<EditorialLedgerValidator.Entry> findings = new ArrayList<>();
        for (Object value : findingValues) {
            Map<String, Object> item = mapObject(value, "findings[]");
            requireKeys(item, Set.of("itemId", "disposition", "evidenceRefs", "modelDeclaredPass"),
                    "finding");
            String itemId = mapString(item, "itemId");
            if (!EditorialP5RawWireContract.token(itemId,
                    EditorialP5RawWireContract.MAX_ID_LENGTH)) {
                throw new IllegalArgumentException("RAW_WIRE_ITEM_ID_INVALID");
            }
            String findingDisposition = mapString(item, "disposition");
            if (!Set.of("PROCESSED", "PRESERVE_DRAFT", "NOT_EVALUATED")
                    .contains(findingDisposition)) {
                throw new IllegalArgumentException("RAW_WIRE_FINDING_DISPOSITION_INVALID");
            }
            List<String> itemEvidence = mapTokens(mapArray(item, "evidenceRefs"),
                    EditorialP5RawWireContract.MAX_ENTRY_EVIDENCE_REFS, "finding.evidenceRefs");
            findings.add(new EditorialLedgerValidator.Entry(itemId, findingDisposition,
                    itemEvidence, mapBoolean(item, "modelDeclaredPass")));
        }

        Map<String, Object> gateValues = mapObject(root, "gateObservations");
        requireKeys(gateValues, Set.copyOf(EditorialSafe4Contract.GATE_IDS), "gateObservations");
        TreeMap<String, String> gates = new TreeMap<>();
        for (String gate : EditorialSafe4Contract.GATE_IDS) {
            String value = mapString(gateValues, gate);
            if (!"PASS".equals(value) && !"NOT_APPLICABLE".equals(value)) {
                throw new IllegalArgumentException("RAW_WIRE_GATE_STATUS_INVALID:" + gate);
            }
            gates.put(gate, value);
        }
        List<String> evidenceRefs = mapTokens(mapArray(root, "evidenceRefs"),
                EditorialP5RawWireContract.MAX_EVIDENCE_REFS, "evidenceRefs");
        List<String> preserved = mapTokens(mapArray(root, "preservedInventory"),
                EditorialP5RawWireContract.MAX_PRESERVED_ITEMS, "preservedInventory");
        List<Object> changes = mapArray(root, "declaredChanges");
        if (!changes.isEmpty()) throw new IllegalArgumentException("RAW_DECLARED_CHANGES_FORBIDDEN");

        EditorialStopDecision.Decision disposition = rawDecision(
                mapObject(root, "disposition"));
        return new EditorialP5RawWireResponse(schema, attemptIdentity, requestEnvelopeHash,
                findings, gates, evidenceRefs, preserved, changes.size(), disposition,
                mapBoolean(root, "modelDeclaredPass"));
    }

    private static EditorialStopDecision.Decision rawDecision(Map<String, Object> value) {
        requireKeys(value, Set.of("disposition", "reasonCode", "phase", "blockingGate",
                "evidenceRefs", "affectedScope", "recoveryAction", "resumeFrom", "stopClass",
                "retryable"), "disposition");
        String disposition = mapString(value, "disposition");
        String reason = mapSafeText(value, "reasonCode");
        String phase = mapString(value, "phase");
        if (!EditorialP5RawWireContract.FINAL_PHASE.equals(phase)) {
            throw new IllegalArgumentException("RAW_WIRE_DISPOSITION_PHASE_INVALID");
        }
        String gate = mapString(value, "blockingGate");
        if (!EditorialP5RawWireContract.gateOrNone(gate)) {
            throw new IllegalArgumentException("RAW_WIRE_BLOCKING_GATE_INVALID");
        }
        List<String> evidence = mapTokens(mapArray(value, "evidenceRefs"),
                EditorialP5RawWireContract.MAX_DISPOSITION_EVIDENCE_REFS,
                "disposition.evidenceRefs");
        String affected = mapSafeText(value, "affectedScope");
        String recovery = mapSafeText(value, "recoveryAction");
        String resume = mapSafeText(value, "resumeFrom");
        String stopClass = mapString(value, "stopClass");
        boolean retryable = mapBoolean(value, "retryable");
        if ("CONTINUE".equals(disposition) || "PRESERVE_DRAFT".equals(disposition)) {
            if (!"NONE".equals(stopClass) || retryable) {
                throw new IllegalArgumentException("RAW_WIRE_NONSTOP_DISPOSITION_INVALID");
            }
            return new EditorialStopDecision.Decision(
                    "CONTINUE".equals(disposition)
                            ? EditorialStopDecision.Disposition.CONTINUE
                            : EditorialStopDecision.Disposition.PRESERVE_DRAFT,
                    null, reason, phase, gate, evidence, affected, recovery, resume);
        }
        if (!"STOP".equals(disposition) || "NONE".equals(stopClass)) {
            throw new IllegalArgumentException("RAW_WIRE_DISPOSITION_INVALID");
        }
        EditorialStopDecision.StopClass typedStop = EditorialStopDecision.StopClass.valueOf(stopClass);
        EditorialStopDecision.StopReceipt receipt = new EditorialStopDecision.StopReceipt(
                typedStop, reason, phase, gate, evidence, affected, recovery, resume, retryable);
        return new EditorialStopDecision.Decision(EditorialStopDecision.Disposition.STOP, receipt,
                reason, phase, gate, evidence, affected, recovery, resume);
    }

    private static List<String> mapTokens(List<Object> values, int maximum, String label) {
        if (values.size() > maximum) throw new IllegalArgumentException(label + " limit exceeded");
        ArrayList<String> result = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof String)
                    || !EditorialP5RawWireContract.token((String) value,
                    EditorialP5RawWireContract.MAX_REF_LENGTH)) {
                throw new IllegalArgumentException(label + " contains invalid token");
            }
            result.add((String) value);
        }
        return List.copyOf(result);
    }

    private static String mapSafeText(Map<String, Object> object, String key) {
        String value = mapString(object, key);
        if (!EditorialP5RawWireContract.safeText(value)) {
            throw new IllegalArgumentException("RAW_WIRE_DISPOSITION_TEXT_INVALID:" + key);
        }
        return value;
    }

    private static Map<String, Object> mapObject(Map<String, Object> object, String key) {
        return mapObject(object.get(key), key);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapObject(Object value, String label) {
        if (!(value instanceof Map)) throw new IllegalArgumentException(label + " must be object");
        return (Map<String, Object>) value;
    }

    private static List<Object> mapArray(Map<String, Object> object, String key) {
        Object value = object.get(key);
        if (!(value instanceof List)) throw new IllegalArgumentException(key + " must be array");
        @SuppressWarnings("unchecked") List<Object> result = (List<Object>) value;
        return result;
    }

    private static String mapString(Map<String, Object> object, String key) {
        Object value = object.get(key);
        if (!(value instanceof String)) throw new IllegalArgumentException(key + " must be string");
        return (String) value;
    }

    private static boolean mapBoolean(Map<String, Object> object, String key) {
        Object value = object.get(key);
        if (!(value instanceof Boolean)) throw new IllegalArgumentException(key + " must be boolean");
        return (Boolean) value;
    }

    private static void requireKeys(Map<String, Object> object, Set<String> expected, String label) {
        if (!object.keySet().equals(expected)) {
            throw new IllegalArgumentException(label + " contains unknown or missing fields");
        }
    }

    private static EditorialStopDecision.Decision decision(JSONObject value) throws JSONException {
        String disposition = value.getString("disposition");
        String reason = value.getString("reasonCode");
        String phase = value.getString("phase");
        String gate = value.getString("blockingGate");
        List<String> evidence = strings(value.getJSONArray("evidenceRefs"), "disposition.evidenceRefs");
        String affected = value.getString("affectedScope");
        String recovery = value.getString("recoveryAction");
        String resume = value.getString("resumeFrom");
        if ("CONTINUE".equals(disposition)) {
            return new EditorialStopDecision.Decision(EditorialStopDecision.Disposition.CONTINUE,
                    null, reason, phase, gate, evidence, affected, recovery, resume);
        }
        if ("PRESERVE_DRAFT".equals(disposition)) {
            return new EditorialStopDecision.Decision(EditorialStopDecision.Disposition.PRESERVE_DRAFT,
                    null, reason, phase, gate, evidence, affected, recovery, resume);
        }
        if (!"STOP".equals(disposition)) throw new IllegalArgumentException("disposition invalid");
        EditorialStopDecision.StopClass stopClass = EditorialStopDecision.StopClass.valueOf(
                value.getString("stopClass"));
        boolean retryable = value.optBoolean("retryable", false);
        EditorialStopDecision.StopReceipt receipt = new EditorialStopDecision.StopReceipt(
                stopClass, reason, phase, gate, evidence, affected, recovery, resume, retryable);
        return new EditorialStopDecision.Decision(EditorialStopDecision.Disposition.STOP, receipt,
                reason, phase, gate, evidence, affected, recovery, resume);
    }

    private static List<String> strings(JSONArray array, String label) throws JSONException {
        if (array == null) throw new IllegalArgumentException(label + " missing");
        ArrayList<String> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String value = array.getString(i);
            if (value.isBlank()) throw new IllegalArgumentException(label + " contains blank");
            values.add(value);
        }
        return List.copyOf(values);
    }

    private static boolean isTruncation(String finishReason) {
        String value = finishReason == null ? "" : finishReason.toLowerCase(java.util.Locale.ROOT);
        return "length".equals(value) || "max_tokens".equals(value)
                || "max_output_tokens".equals(value);
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    static void requireFinishReason(String finishReason)
            throws EditorialP5PilotProvider.ProviderFailure {
        if (blank(finishReason)) {
            throw new EditorialP5PilotProvider.ProviderFailure(
                    "RETRY_PROVIDER_RESPONSE_PARSE_FAILED");
        }
    }
}
