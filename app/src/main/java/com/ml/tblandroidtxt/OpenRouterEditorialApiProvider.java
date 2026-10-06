package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.CheckSpec;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

/**
 * EDITORIAL_API_V1 calls over the existing OpenAI-compatible transport. The edit call asks for plain text; the check call
 * asks for the strict JSON schema generated from {@link CheckSpec}. Reasoning stays minimal as in P6. What the response
 * says about the model and route, the finish reason, the usage and the cost are returned to be recorded with the run.
 */
public final class OpenRouterEditorialApiProvider implements EditorialApiProvider {
    public static final String CHECK_SCHEMA_NAME = "editorial_api_check_v1";
    private static final String REASONING = "minimal";

    private final AppSettings settings;
    private final OpenAICompatibleClient.CallControl control = new OpenAICompatibleClient.CallControl();

    public OpenRouterEditorialApiProvider(AppSettings settings) {
        if (settings == null) throw new IllegalArgumentException("settings are required");
        this.settings = settings.copy();
    }

    @Override public void cancel() { control.cancel("EDITORIAL_API_USER_CANCEL"); }

    /** Output ceiling for the edit call: the draft's size in tokens (generous for Vietnamese) times 1.6, at least 4096. */
    public static int editMaxOutputTokens(int draftChars) {
        long tokens = (long) Math.ceil(draftChars / 2.0 * 1.6);
        return (int) Math.min(32_000L, Math.max(4096L, tokens));
    }

    @Override public EditorialApiFlow.StepResponse call(EditorialApiFlow.Request request, String model, int maxOutputTokens, long timeoutMillis) {
        try {
            AppSettings s = settings.copy();
            if (model != null && !model.trim().isEmpty()) s.model = model.trim();
            if (s.apiKey == null || s.apiKey.trim().isEmpty()) return EditorialApiFlow.StepResponse.failure("API_KEY_MISSING");
            PromptPair pair = new PromptPair(request.prompt().system(), request.prompt().user());
            JSONObject responseFormat;
            try {
                responseFormat = request.json() ? checkResponseFormat() : null;
            } catch (JSONException building) {
                return EditorialApiFlow.StepResponse.failure(describe(building, s.apiKey)); // nothing was sent
            }
            JSONObject preferences = EditorialP5EFreshRawRoutingPolicy.matches(s) ? EditorialP5EFreshRawRoutingPolicy.providerPreferences() : null;
            long deadline = timeoutMillis > 0 ? OpenAICompatibleClient.monotonicDeadlineNanosFromNowMillis(timeoutMillis) : 0L;
            OpenAICompatibleClient.ChatResult result = OpenAICompatibleClient.chatWithUsage(s, pair, maxOutputTokens,
                    "editorial-api-" + request.step().name().toLowerCase(java.util.Locale.ROOT) + "-" + request.attempt(), null,
                    false, deadline, control, responseFormat, preferences, REASONING);
            BigDecimal cost = BigDecimal.ZERO;
            boolean known = false;
            if (result.providerCostReported && Double.isFinite(result.providerCost) && result.providerCost >= 0) {
                cost = BigDecimal.valueOf(result.providerCost);
                known = true;
            } else {
                double estimate = ModelCatalog.usageCost(ModelCatalog.findModelInfo(s.provider, s.model),
                        result.promptTokens, result.cachedPromptTokens, result.completionTokens);
                if (Double.isFinite(estimate) && estimate >= 0) { cost = BigDecimal.valueOf(estimate); known = true; }
            }
            String servedModel = result.responseModel.isEmpty() ? s.model : result.responseModel;
            return new EditorialApiFlow.StepResponse(result.content, result.finishReason, result.promptTokens,
                    result.completionTokens, cost, known, servedModel, result.responseProvider, "");
        } catch (Exception failure) {
            String reason = describe(failure, settings.apiKey);
            return notDispatched(failure) ? EditorialApiFlow.StepResponse.failure(reason) : EditorialApiFlow.StepResponse.unknownOutcome(reason);
        }
    }

    /**
     * True only when the failure proves the request never reached the provider (bad local settings, no route, a 4xx rejection
     * before any generation). Everything else after sending - timeouts, lost connections, 5xx, unreadable bodies, a cancel -
     * may already have been billed, so its charge is unknown and the step is not repeated automatically.
     */
    static boolean notDispatched(Exception failure) {
        if (failure instanceof IllegalArgumentException) return true;
        if (failure instanceof java.net.UnknownHostException || failure instanceof java.net.ConnectException
                || failure instanceof java.net.NoRouteToHostException || failure instanceof javax.net.ssl.SSLHandshakeException) return true;
        if (failure instanceof ApiHttpException) {
            int code = ((ApiHttpException) failure).statusCode;
            return code >= 400 && code < 500 && code != 408;
        }
        return false;
    }

    static JSONObject checkResponseFormat() throws JSONException {
        return new JSONObject().put("type", "json_schema").put("json_schema", new JSONObject()
                .put("name", CHECK_SCHEMA_NAME).put("strict", true)
                .put("schema", new JSONObject(EditorialCanonicalJson.canonicalize(CheckSpec.schema()))));
    }

    /** A short, key-free reason: the exception type and the first line of its message. */
    static String describe(Exception failure) { return describe(failure, ""); }

    static String describe(Exception failure, String apiKey) {
        String message = failure.getMessage() == null ? "" : failure.getMessage().replace('\n', ' ');
        if (apiKey != null && apiKey.trim().length() >= 6) message = message.replace(apiKey.trim(), "[key]");
        message = message.replaceAll("(?i)bearer\\s+\\S+", "Bearer [key]").replaceAll("sk-[A-Za-z0-9_-]{8,}", "[key]");
        if (message.length() > 160) message = message.substring(0, 160);
        String type = failure.getClass().getSimpleName();
        return message.isEmpty() ? type : type + ": " + message;
    }
}
