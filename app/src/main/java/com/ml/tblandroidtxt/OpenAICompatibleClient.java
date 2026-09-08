package com.ml.tblandroidtxt;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.EventListener;

public class OpenAICompatibleClient {
    public interface NetworkObserver {
        default void onCallCreated() {}
        default void onRequestBodyStarted() {}
        default void onRequestBodySent(long byteCount) {}
        default void onResponseHeaders(int code) {}
        default void onResponseHeaders(int code, String generationId) { onResponseHeaders(code); }
        default void onResponseBodyComplete(long byteCount, String providerResponseId) {}
        default void onCallCancelled(long elapsedMillis) {}
        default void onCallFailed(String exceptionClass, long elapsedMillis) {}
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient BASE_CLIENT = new OkHttpClient.Builder().build();
    private static final AtomicReference<Call> ACTIVE_CALL = new AtomicReference<>();

    public static void cancelActiveRequests() {
        Call call = ACTIVE_CALL.getAndSet(null);
        if (call != null) {
            try { call.cancel(); } catch (Exception ignored) {}
        }
    }

    public static class ChatResult {
        public String content = "";
        public int promptTokens = 0;
        public int completionTokens = 0;
        public int totalTokens = 0;
        public int cachedPromptTokens = 0;
        public boolean usageReported = false;
        public double providerCost = Double.NaN;
        public boolean providerCostReported = false;
        public String finishReason = "";
        public String providerResponseId = "";
    }

    public static String chat(AppSettings s, PromptPair prompt) throws Exception {
        return chatWithUsage(s, prompt).content;
    }

    public static ChatResult chatWithUsage(AppSettings s, PromptPair prompt) throws Exception {
        return chatWithUsage(s, prompt, s == null ? 4096 : s.maxOutputTokens);
    }

    public static ChatResult chatWithUsage(AppSettings s, PromptPair prompt, int maxOutputTokens) throws Exception {
        return chatWithUsage(s, prompt, maxOutputTokens, "");
    }

    public static ChatResult chatWithUsage(AppSettings s, PromptPair prompt, int maxOutputTokens, String requestId) throws Exception {
        return chatWithUsage(s,prompt,maxOutputTokens,requestId,null);
    }

    public static ChatResult chatWithUsage(AppSettings s, PromptPair prompt, int maxOutputTokens, String requestId, NetworkObserver observer) throws Exception {
        if (s.apiKey == null || s.apiKey.trim().isEmpty()) throw new IllegalArgumentException("API key is empty");
        if (s.model == null || s.model.trim().isEmpty()) throw new IllegalArgumentException("Model is empty");
        String endpoint = AppSettings.normalizeEndpoint(s.baseUrl);
        if (endpoint == null || endpoint.trim().isEmpty()) throw new IllegalArgumentException("Base URL is empty");
        if (endpoint.contains("/api/generate")) throw new IllegalArgumentException("Ollama /api/generate is not implemented in this Android TXT MVP. Use an OpenAI-compatible /v1/chat/completions endpoint.");

        JSONObject body = new JSONObject();
        body.put("model", s.model);
        body.put("temperature", s.temperature);
        body.put("max_tokens", Math.max(128, maxOutputTokens));
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", prompt.system));
        messages.put(new JSONObject().put("role", "user").put("content", prompt.user));
        body.put("messages", messages);

        int timeout = Math.max(10, s.timeoutSeconds);
        OkHttpClient client = BASE_CLIENT.newBuilder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .callTimeout(timeout + 30L, TimeUnit.SECONDS)
                .eventListener(new EventListener(){
                    @Override public void requestBodyStart(Call call){if(observer!=null)observer.onRequestBodyStarted();}
                    @Override public void requestBodyEnd(Call call,long byteCount){if(observer!=null)observer.onRequestBodySent(byteCount);}
                    @Override public void responseHeadersEnd(Call call,Response response){
                        if(observer!=null)observer.onResponseHeaders(response.code(),
                                safeHeader(response.header("X-Generation-Id")));
                    }
                })
                .build();

        Request.Builder rb = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + s.apiKey.trim());
        if (endpoint.contains("openrouter.ai")) {
            rb.addHeader("HTTP-Referer", "https://local.tbl.android");
            rb.addHeader("X-Title", "Translate Books with LLMs");
        }
        if (requestId != null && !requestId.trim().isEmpty()) rb.addHeader("X-TBL-Request-ID", requestId.trim());

        Call call = client.newCall(rb.build());
        ACTIVE_CALL.set(call);
        if (observer != null) observer.onCallCreated();
        long startedNanos = System.nanoTime();
        try (Response response = call.execute()) {
            ResponseBody responseBody = response.body();
            String responseText = responseBody == null ? "" : responseBody.string();
            if (observer != null) observer.onResponseBodyComplete(
                    responseText.getBytes(StandardCharsets.UTF_8).length, "");
            int code = response.code();
            if (!response.isSuccessful()) throw new ApiHttpException(code, parseRetryAfterMs(response.header("Retry-After")), ApiErrorParser.fromHttp(code, responseText));

            ChatResult r = parseChatResponse(responseText);
            if (observer != null) observer.onResponseBodyComplete(
                    responseText.getBytes(StandardCharsets.UTF_8).length,
                    safeHeader(r.providerResponseId));
            if (r.promptTokens <= 0) r.promptTokens = Chunker.approxTokens(prompt.system) + Chunker.approxTokens(prompt.user);
            if (r.completionTokens <= 0) r.completionTokens = Chunker.approxTokens(r.content);
            if (r.totalTokens <= 0) r.totalTokens = r.promptTokens + r.completionTokens;
            ObservabilityLog.event("provider_usage", "provider", s.provider, "model", s.model,
                    "source", r.usageReported ? "PROVIDER" : "ESTIMATED",
                    "input", r.promptTokens, "cached", r.cachedPromptTokens,
                    "output", r.completionTokens, "total", r.totalTokens,
                    "providerCost", r.providerCost);
            return r;
        } catch (Exception error) {
            if (observer != null) {
                long elapsed = Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
                if (call.isCanceled() || error instanceof CancellationException) {
                    observer.onCallCancelled(elapsed);
                } else {
                    observer.onCallFailed(safeExceptionClass(error), elapsed);
                }
            }
            throw error;
        } finally {
            ACTIVE_CALL.compareAndSet(call, null);
        }
    }

    static ChatResult parseChatResponse(String responseText) throws Exception {
        JSONObject json = new JSONObject(responseText == null ? "{}" : responseText);
        JSONArray choices = json.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            throw new RuntimeException("No choices returned. Raw response: " + ApiErrorParser.preview(responseText, 900));
        }
        JSONObject first = choices.getJSONObject(0);
        JSONObject message = first.optJSONObject("message");
        String content = message != null ? message.optString("content", "") : first.optString("text", "");
        ChatResult result = new ChatResult();
        result.providerResponseId=json.optString("id","");
        result.content = content == null ? "" : content;
        result.finishReason = first.optString("finish_reason", "");
        JSONObject usage = json.optJSONObject("usage");
        if (usage != null) {
            boolean promptPresent = hasAny(usage, "prompt_tokens", "input_tokens");
            boolean completionPresent = hasAny(usage, "completion_tokens", "output_tokens");
            boolean totalPresent = usage.has("total_tokens") && !usage.isNull("total_tokens");
            result.usageReported = promptPresent || completionPresent || totalPresent;
            result.promptTokens = nonNegativeInt(firstValue(usage, "prompt_tokens", "input_tokens"));
            result.completionTokens = nonNegativeInt(firstValue(usage, "completion_tokens", "output_tokens"));
            result.totalTokens = nonNegativeInt(firstValue(usage, "total_tokens"));
            if (result.totalTokens <= 0 && promptPresent && completionPresent) {
                result.totalTokens = result.promptTokens + result.completionTokens;
            }
            JSONObject details = usage.optJSONObject("prompt_tokens_details");
            if (details == null) details = usage.optJSONObject("input_tokens_details");
            if (details != null) {
                result.cachedPromptTokens = nonNegativeInt(firstValue(details,
                        "cached_tokens", "cache_read_input_tokens", "cached_input_tokens"));
            }
            Object cost = firstValue(usage, "cost", "total_cost", "provider_cost");
            if (cost == null) cost = firstValue(json, "cost", "usage_cost", "provider_cost");
            double parsed = nonNegativeDouble(cost);
            if (!Double.isNaN(parsed)) {
                result.providerCost = parsed;
                result.providerCostReported = true;
            }
        } else {
            double parsed = nonNegativeDouble(firstValue(json, "cost", "usage_cost", "provider_cost"));
            if (!Double.isNaN(parsed)) {
                result.providerCost = parsed;
                result.providerCostReported = true;
            }
        }
        result.cachedPromptTokens = Math.min(result.promptTokens, Math.max(0, result.cachedPromptTokens));
        return result;
    }

    private static boolean hasAny(JSONObject object, String... keys) {
        if (object == null) return false;
        for (String key : keys) if (object.has(key) && !object.isNull(key)) return true;
        return false;
    }

    private static Object firstValue(JSONObject object, String... keys) {
        if (object == null) return null;
        for (String key : keys) if (object.has(key) && !object.isNull(key)) return object.opt(key);
        return null;
    }

    private static int nonNegativeInt(Object raw) {
        if (raw == null || raw == JSONObject.NULL) return 0;
        try { return Math.max(0, (int)Math.min(Integer.MAX_VALUE, Double.parseDouble(String.valueOf(raw).trim()))); }
        catch (Exception ignored) { return 0; }
    }

    private static double nonNegativeDouble(Object raw) {
        if (raw == null || raw == JSONObject.NULL) return Double.NaN;
        try {
            double value = Double.parseDouble(String.valueOf(raw).trim());
            return Double.isFinite(value) && value >= 0 ? value : Double.NaN;
        } catch (Exception ignored) { return Double.NaN; }
    }

    private static long parseRetryAfterMs(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0;
        try { return Math.max(0, Long.parseLong(raw.trim()) * 1000L); } catch (Exception ignored) { return 0; }
    }

    private static String safeHeader(String value) {
        if (value == null || value.isBlank() || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) return "";
        return value.length() > 256 ? value.substring(0, 256) : value;
    }

    private static String safeExceptionClass(Throwable error) {
        if (error == null) return "RuntimeException";
        if (error instanceof UnknownHostException) return "UnknownHostException";
        if (error instanceof ConnectException) return "ConnectException";
        if (error instanceof java.io.IOException && error instanceof javax.net.ssl.SSLException) {
            return error instanceof javax.net.ssl.SSLHandshakeException
                    ? "SSLHandshakeException" : "SSLException";
        }
        if (error instanceof SocketTimeoutException) return "SocketTimeoutException";
        if (error instanceof InterruptedIOException) return "InterruptedIOException";
        if (error instanceof ApiHttpException) return "ApiHttpException";
        if (error instanceof org.json.JSONException) return "JSONException";
        if (error instanceof java.io.IOException) return "IOException";
        if (error instanceof CancellationException) return "CancellationException";
        return "RuntimeException";
    }
}
