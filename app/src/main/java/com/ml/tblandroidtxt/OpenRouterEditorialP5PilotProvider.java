package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialDiffValidator;
import com.ml.tblandroidtxt.editorial.pack.EditorialLedgerValidator;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5L1Output;
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
    private final Map<String, EditorialP5L1Output> parsedOutputs = new HashMap<>();

    public OpenRouterEditorialP5PilotProvider(AppSettings settings, int maximumOutputTokens) {
        this(settings, maximumOutputTokens, null);
    }

    /** Optional app-owned recorder for redacted P5D lifecycle evidence. */
    public OpenRouterEditorialP5PilotProvider(AppSettings settings, int maximumOutputTokens,
                                              NetworkLifecycleRecorder lifecycleRecorder) {
        if (settings == null) throw new IllegalArgumentException("OpenRouter settings are required");
        this.settings = settings.copy();
        if (maximumOutputTokens <= 0) {
            throw new IllegalArgumentException("Output token cap is invalid");
        }
        this.maximumOutputTokens = maximumOutputTokens;
        this.lifecycleRecorder = lifecycleRecorder;
    }

    /** Creates the one P5D live adapter with durable, redacted lifecycle evidence. */
    public static OpenRouterEditorialP5PilotProvider withLifecyclePersistence(
            AppSettings settings, int maximumOutputTokens, TranslationRepository database) {
        EditorialP5CAttemptStore store = new EditorialP5CAttemptStore(
                java.util.Objects.requireNonNull(database, "database"));
        return new OpenRouterEditorialP5PilotProvider(settings, maximumOutputTokens,
                store::recordNetworkLifecycle);
    }

    @FunctionalInterface
    public interface NetworkLifecycleRecorder {
        void record(String attemptIdentity, EditorialP5CAttemptStore.NetworkLifecycleEvent event);
    }

    public boolean configured() {
        return settings.provider != null && settings.provider.toLowerCase(java.util.Locale.ROOT)
                .contains("openrouter")
                && settings.apiKey != null && !settings.apiKey.trim().isEmpty()
                && settings.model != null && !settings.model.trim().isEmpty()
                && settings.baseUrl != null && !settings.baseUrl.trim().isEmpty();
    }

    @Override public synchronized Response call(Request request) throws Exception {
        if (request == null || request.context() == null) {
            return invalidResponse("REQUEST_CONTEXT_MISSING", new byte[0]);
        }
        if (!configured()) throw new IllegalStateException("OPENROUTER_CONFIGURATION_INCOMPLETE");

        PromptPair prompt = buildPrompt(request);
        OpenRouterLifecycleObserver lifecycle = new OpenRouterLifecycleObserver(
                request.attemptIdentity(), lifecycleRecorder);
        OpenAICompatibleClient.ChatResult result;
        try {
            result = OpenAICompatibleClient.chatWithUsage(settings, prompt, maximumOutputTokens,
                    request.attemptIdentity(), lifecycle, false);
        } catch (Exception error) {
            throw new ProviderFailure(failureReason(error, lifecycle), error);
        }
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
        }
        BigDecimal cost = reportedOrEstimated(result, request);
        String responseId = result.providerResponseId == null || result.providerResponseId.isBlank()
                ? "openrouter-" + EditorialCanonicalJson.sha256Hex(responseBytes)
                : result.providerResponseId;
        boolean complete = result.finishReason == null || !isTruncation(result.finishReason);
        return new Response(responseId, responseBytes,
                blank(result.finishReason) ? "stop" : result.finishReason,
                complete, result.promptTokens, result.completionTokens, result.totalTokens,
                cost, output, schemaValid);
    }

    private static String failureReason(Throwable error, OpenRouterLifecycleObserver lifecycle) {
        Throwable root = error;
        while (root.getCause() != null && root != root.getCause()) root = root.getCause();
        if (lifecycle.cancelled || root instanceof CancellationException) {
            return "RETRY_PROVIDER_CANCELLED";
        }
        if (root instanceof ApiHttpException) return "RETRY_PROVIDER_HTTP_ERROR";
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

        private OpenRouterLifecycleObserver(String attemptIdentity,
                                             NetworkLifecycleRecorder recorder) {
            this.attemptIdentity = attemptIdentity;
            this.recorder = recorder;
        }

        @Override public void onCallCreated() {
            emit(EditorialP5CAttemptStore.LifecycleStage.CALL_CREATED, "", 0L);
        }

        @Override public void onRequestBodyStarted() {
            bodyStarted = true;
            emit(EditorialP5CAttemptStore.LifecycleStage.REQUEST_BODY_STARTED, "", 0L);
        }

        @Override public void onRequestBodySent(long byteCount) {
            bodySent = true;
            requestBodyBytes = Math.max(0L, byteCount);
            emit(EditorialP5CAttemptStore.LifecycleStage.REQUEST_BODY_SENT, "", 0L);
        }

        @Override public void onResponseHeaders(int code, String generationId) {
            onResponseHeaders(code, generationId, "");
        }

        @Override public void onResponseHeaders(int code, String generationId,
                                                String contentType) {
            headersReceived = true;
            httpStatus = code;
            this.generationId = generationId == null ? "" : generationId;
            this.responseContentType = contentType == null ? "" : contentType;
            emit(EditorialP5CAttemptStore.LifecycleStage.RESPONSE_HEADERS_RECEIVED, "", 0L);
        }

        @Override public void onResponseBodyComplete(long byteCount, String providerResponseId) {
            bodyComplete = true;
            if (providerResponseId != null && !providerResponseId.isBlank()) {
                this.providerResponseId = providerResponseId;
            }
            emit(EditorialP5CAttemptStore.LifecycleStage.RESPONSE_BODY_COMPLETE, "", 0L);
        }

        @Override public void onResponseBodyComplete(long byteCount, String providerResponseId,
                                                      long elapsedMillis) {
            bodyComplete = true;
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
                            requestBodyBytes, httpStatus, responseContentType, exceptionClass,
                            elapsedMillis, generationId, providerResponseId, cancellationSource));
        }
    }

    private static BigDecimal reportedOrEstimated(OpenAICompatibleClient.ChatResult result,
                                                   Request request) {
        if (result.providerCostReported && Double.isFinite(result.providerCost)
                && result.providerCost >= 0) return BigDecimal.valueOf(result.providerCost);
        ModelCatalog.ModelInfo model = ModelCatalog.findModelInfo(request.provider(), request.model());
        double estimate = ModelCatalog.usageCost(model, result.promptTokens,
                result.cachedPromptTokens, result.completionTokens);
        return Double.isFinite(estimate) && estimate >= 0
                ? BigDecimal.valueOf(estimate) : BigDecimal.ZERO;
    }

    private static Response invalidResponse(String reason, byte[] bytes) {
        return new Response("openrouter-schema-" + reason.toLowerCase(java.util.Locale.ROOT),
                bytes, "schema_invalid", true, 0, 0, 0, BigDecimal.ZERO, null, false);
    }

    private PromptPair buildPrompt(Request request) {
        StringBuilder system = new StringBuilder();
        system.append("You are an untrusted SAFE4 L1 analysis assistant. The app is the authority.\n")
                .append("Return exactly one JSON object and no Markdown or commentary.\n")
                .append("Do not declare certification, change state, choose a pack, or invent hashes.\n")
                .append("For L1_RAW_DISCOVERY, use only the visible RAW and GLOSSARY blocks.\n")
                .append("For L1_RECONCILE, use only the visible blocks supplied below.\n")
                .append("The JSON must contain the exact identity values from the envelope, all gate IDs,")
                .append(" an exhaustive ledger, and disposition metadata.\n\n")
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
        user.append("APP-OWNED REQUEST ENVELOPE (copy identity values exactly):\n")
                .append(EditorialCanonicalJson.canonicalize(envelope))
                .append("\n\nVISIBLE SOURCE BLOCKS. Do not infer or request hidden roles:\n");
        ArrayList<String> roles = new ArrayList<>(request.visibleSources().keySet());
        Collections.sort(roles);
        for (String role : roles) {
            user.append("\n--- ").append(role).append(" ---\n")
                    .append(new String(request.visibleSources().get(role), StandardCharsets.UTF_8))
                    .append("\n--- END ").append(role).append(" ---\n");
        }
        user.append("\nReturn this object shape exactly. beforeText and afterText must be present;")
                .append(" for discovery use equal text and an empty declaredChanges array when no edit is made.\n")
                .append("{\"reportSchemaVersion\":\"safe4.full.report-l1.v1\",\"receiptSchemaVersion\":\"safe4.full.receipt.v1\",\n")
                .append("\"bindingIdentity\":\"...\",\"manifestFingerprint\":\"...\",\"chapterKey\":\"...\",\"phase\":\"L1\",\n")
                .append("\"bundleIdentity\":\"...\",\"predecessorIdentity\":\"...\",\"stableAnchors\":[...],\n")
                .append("\"ledger\":{\"populationIds\":[...],\"entries\":[{\"itemId\":\"...\",\"disposition\":\"PROCESSED|PRESERVE_DRAFT|NOT_EVALUATED\",\"evidenceRefs\":[\"...\"],\"modelDeclaredPass\":false}]},\n")
                .append("\"gates\":{\"ARTIFACT_IDENTITY\":\"PASS|NOT_APPLICABLE\",...},\n")
                .append("\"preservedInventory\":[],\"declaredChanges\":[],\"beforeText\":\"\",\"afterText\":\"\",\n")
                .append("\"releaseAttemptCount\":0,\"disposition\":{\"disposition\":\"CONTINUE|PRESERVE_DRAFT|STOP\",\"reasonCode\":\"...\",\"phase\":\"L1\",\"blockingGate\":\"...\",\"evidenceRefs\":[],\"affectedScope\":\"...\",\"recoveryAction\":\"...\",\"resumeFrom\":\"...\",\"retryable\":false},\n")
                .append("\"evidenceRefs\":[\"...\"],\"modelDeclaredPass\":false}\n");
        return new PromptPair(system.toString(), user.toString());
    }

    private static String authority(Request request,
                                    com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole role) {
        byte[] bytes = request.authority().bytes(role);
        return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
    }

    /** Strict typed parser used by the production adapter and parser tests. */
    public static EditorialP5L1Output parseOutput(String raw, Request request)
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
        EditorialP5PilotProvider.Request.Context context = request.context();
        return new EditorialP5L1Output(
                root.getString("reportSchemaVersion"), root.getString("receiptSchemaVersion"),
                context.bindingIdentity(), context.manifestFingerprint(), root.getString("chapterKey"),
                root.getString("phase"), context.bundleIdentity(), context.predecessorIdentity(),
                stableAnchors, new EditorialLedgerValidator.Request(populationIds, entries), gates,
                strings(root.getJSONArray("preservedInventory"), "preservedInventory"), changes,
                root.getString("beforeText"), root.getString("afterText"),
                root.getInt("releaseAttemptCount"), disposition,
                Set.copyOf(strings(root.getJSONArray("evidenceRefs"), "evidenceRefs")),
                root.optBoolean("modelDeclaredPass", false));
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
}
