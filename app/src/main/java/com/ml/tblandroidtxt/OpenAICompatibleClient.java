package com.ml.tblandroidtxt;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
        default void onCallCreated(long elapsedMillis) { onCallCreated(); }
        default void onRequestBodyStarted() {}
        default void onRequestBodyStarted(long elapsedMillis) { onRequestBodyStarted(); }
        default void onRequestBodySent(long byteCount) {}
        default void onRequestBodySent(long byteCount, long elapsedMillis) {
            onRequestBodySent(byteCount);
        }
        default void onResponseHeaders(int code) {}
        default void onResponseHeaders(int code, String generationId) { onResponseHeaders(code); }
        default void onResponseHeaders(int code, String generationId, String contentType) {
            onResponseHeaders(code, generationId);
        }
        default void onResponseHeaders(int code, String generationId, String contentType,
                                       long elapsedMillis) {
            onResponseHeaders(code, generationId, contentType);
        }
        default void onResponseBodyProgress(long byteCount, long elapsedMillis) {}
        default void onResponseBodyComplete(long byteCount, String providerResponseId) {}
        default void onResponseBodyComplete(long byteCount, String providerResponseId,
                                            long elapsedMillis) {
            onResponseBodyComplete(byteCount, providerResponseId);
        }
        default void onCallCancelled(long elapsedMillis) {}
        default void onCallCancelled(long elapsedMillis, String cancellationSource) {
            onCallCancelled(elapsedMillis);
        }
        default void onCallFailed(String exceptionClass, long elapsedMillis) {}
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient BASE_CLIENT = new OkHttpClient.Builder().build();
    private static final AtomicReference<ActiveCall> ACTIVE_CALL = new AtomicReference<>();
    private static final long MAX_RESPONSE_BODY_BYTES = 8L * 1024L * 1024L;
    private static final long BODY_PROGRESS_BYTES = 32L * 1024L;
    private static final long BODY_PROGRESS_INTERVAL_NANOS =
            TimeUnit.MILLISECONDS.toNanos(250L);

    private static final class ActiveCall {
        private final Call call;
        private final AtomicReference<String> cancellationSource;

        private ActiveCall(Call call, AtomicReference<String> cancellationSource) {
            this.call = call;
            this.cancellationSource = cancellationSource;
        }
    }

    /**
     * Scoped cancellation owner for a pilot call. It is intentionally not
     * connected to the legacy translation-service cancellation slot.
     */
    static final class CallControl {
        private final AtomicReference<Call> call = new AtomicReference<>();
        private final AtomicReference<String> cancellationSource = new AtomicReference<>("");

        void bind(Call value) {
            call.set(value);
            String source = cancellationSource.get();
            if (!source.isEmpty()) value.cancel();
        }

        void cancel(String source) {
            String value = source == null || source.isBlank()
                    ? "PILOT_REQUESTED_CANCEL" : source;
            cancellationSource.compareAndSet("", value);
            Call current = call.get();
            if (current != null) current.cancel();
        }

        void clear(Call value) { call.compareAndSet(value, null); }

        AtomicReference<String> cancellationSource() { return cancellationSource; }

        void reset() {
            call.set(null);
            cancellationSource.set("");
        }
    }

    static long monotonicDeadlineNanosFromNowMillis(long durationMillis) {
        if (durationMillis <= 0) throw new IllegalArgumentException("deadline must be positive");
        long now = System.nanoTime();
        long durationNanos = TimeUnit.MILLISECONDS.toNanos(durationMillis);
        long deadline = now + durationNanos;
        return deadline < now ? Long.MAX_VALUE : deadline;
    }

    static long remainingMillis(long deadlineNanos) {
        if (deadlineNanos <= 0) return Long.MAX_VALUE;
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) return 0L;
        long millis = TimeUnit.NANOSECONDS.toMillis(remainingNanos);
        return Math.max(1L, millis);
    }

    public static void cancelActiveRequests() {
        ActiveCall active = ACTIVE_CALL.getAndSet(null);
        if (active != null) {
            active.cancellationSource.compareAndSet("", "LEGACY_GLOBAL_CANCEL");
            try { active.call.cancel(); } catch (Exception ignored) {}
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
        return chatWithUsage(s, prompt, maxOutputTokens, requestId, observer, true);
    }

    /**
     * P5D callers opt out of the legacy translation-service cancellation slot.
     * A legacy global cancel must never claim or cancel a separately authorized
     * editorial pilot call.
     */
    public static ChatResult chatWithUsage(AppSettings s, PromptPair prompt, int maxOutputTokens,
                                           String requestId, NetworkObserver observer,
                                           boolean registerForLegacyGlobalCancellation) throws Exception {
        return chatWithUsage(s, prompt, maxOutputTokens, requestId, observer,
                registerForLegacyGlobalCancellation, 0L, null);
    }

    /**
     * Executes one call against an absolute monotonic deadline. A zero
     * deadline preserves the legacy settings-based behavior; pilot callers
     * must provide a deadline so OkHttp cannot add an unowned grace period.
     */
    static ChatResult chatWithUsage(AppSettings s, PromptPair prompt, int maxOutputTokens,
                                    String requestId, NetworkObserver observer,
                                    boolean registerForLegacyGlobalCancellation,
                                    long deadlineNanos, CallControl callControl) throws Exception {
        if (s.apiKey == null || s.apiKey.trim().isEmpty()) throw new IllegalArgumentException("API key is empty");
        if (s.model == null || s.model.trim().isEmpty()) throw new IllegalArgumentException("Model is empty");
        String endpoint = AppSettings.normalizeEndpoint(s.baseUrl);
        if (endpoint == null || endpoint.trim().isEmpty()) throw new IllegalArgumentException("Base URL is empty");
        if (endpoint.contains("/api/generate")) throw new IllegalArgumentException("Ollama /api/generate is not implemented in this Android TXT MVP. Use an OpenAI-compatible /v1/chat/completions endpoint.");

        long startedNanos = System.nanoTime();
        boolean boundedDeadline = deadlineNanos > 0;
        long timeoutMillis;
        if (boundedDeadline) {
            timeoutMillis = remainingMillis(deadlineNanos);
            if (timeoutMillis <= 0) throw new SocketTimeoutException("P5D attempt deadline expired");
        } else {
            int timeout = Math.max(10, s.timeoutSeconds);
            timeoutMillis = TimeUnit.SECONDS.toMillis(timeout);
        }
        JSONObject body = buildChatRequestBody(s, prompt, maxOutputTokens);

        OkHttpClient client = BASE_CLIENT.newBuilder()
                .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .callTimeout(boundedDeadline ? timeoutMillis : timeoutMillis + 30_000L,
                        TimeUnit.MILLISECONDS)
                .eventListener(new EventListener(){
                    @Override public void requestBodyStart(Call call){
                        if(observer!=null)observer.onRequestBodyStarted(elapsedMillis(startedNanos));
                    }
                    @Override public void requestBodyEnd(Call call,long byteCount){
                        if(observer!=null)observer.onRequestBodySent(byteCount,
                                elapsedMillis(startedNanos));
                    }
                    @Override public void responseHeadersEnd(Call call,Response response){
                        if(observer!=null)observer.onResponseHeaders(response.code(),
                                safeHeader(response.header("X-Generation-Id")),
                                safeContentType(response.header("Content-Type")),
                                elapsedMillis(startedNanos));
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
        AtomicReference<String> cancellationSource = callControl == null
                ? new AtomicReference<>("") : callControl.cancellationSource();
        ActiveCall active = registerForLegacyGlobalCancellation
                ? new ActiveCall(call, cancellationSource) : null;
        if (active != null) ACTIVE_CALL.set(active);
        if (callControl != null) callControl.bind(call);
        if (observer != null) observer.onCallCreated(elapsedMillis(startedNanos));
        try (Response response = call.execute()) {
            ResponseBody responseBody = response.body();
            ReadBody readBody = readResponseBody(responseBody, observer, startedNanos,
                    deadlineNanos);
            String responseText = readBody.text();
            long bodyBytes = readBody.byteCount();
            long bodyElapsed = elapsedMillis(startedNanos);
            if (observer != null) observer.onResponseBodyComplete(bodyBytes, "", bodyElapsed);
            if (boundedDeadline && System.nanoTime() > deadlineNanos) {
                throw new SocketTimeoutException("P5D attempt deadline expired before validation");
            }
            int code = response.code();
            if (!response.isSuccessful()) throw new ApiHttpException(code, parseRetryAfterMs(response.header("Retry-After")), ApiErrorParser.fromHttp(code, responseText));

            ChatResult r = parseChatResponse(responseText);
            if (observer != null) observer.onResponseBodyComplete(bodyBytes,
                    safeHeader(r.providerResponseId),
                    Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L));
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
                long elapsed = elapsedMillis(startedNanos);
                if (!cancellationSource.get().isBlank() || error instanceof CancellationException) {
                    observer.onCallCancelled(elapsed, cancellationSource.get());
                } else {
                    observer.onCallFailed(safeExceptionClass(error), elapsed);
                }
            }
            throw error;
        } finally {
            if (active != null) ACTIVE_CALL.compareAndSet(active, null);
            if (callControl != null) callControl.clear(call);
        }
    }

    private record ReadBody(String text, long byteCount) { }

    private static ReadBody readResponseBody(ResponseBody responseBody, NetworkObserver observer,
                                             long startedNanos, long deadlineNanos)
            throws IOException {
        if (responseBody == null) return new ReadBody("", 0L);
        long declaredLength = responseBody.contentLength();
        if (declaredLength > MAX_RESPONSE_BODY_BYTES) {
            throw new ResponseBodyTooLargeException();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream(
                (int) Math.min(Math.max(0L, declaredLength), 64L * 1024L));
        long count = 0L;
        long reportedBytes = 0L;
        long lastReportNanos = startedNanos;
        byte[] buffer = new byte[8 * 1024];
        try (InputStream input = responseBody.byteStream()) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (read == 0) continue;
                if (count > MAX_RESPONSE_BODY_BYTES - read) {
                    throw new ResponseBodyTooLargeException();
                }
                output.write(buffer, 0, read);
                count += read;
                long now = System.nanoTime();
                if (observer != null && (reportedBytes == 0L
                        || count - reportedBytes >= BODY_PROGRESS_BYTES
                        || now - lastReportNanos >= BODY_PROGRESS_INTERVAL_NANOS)) {
                    observer.onResponseBodyProgress(count, elapsedMillis(startedNanos));
                    reportedBytes = count;
                    lastReportNanos = now;
                }
                if (deadlineNanos > 0 && now > deadlineNanos) {
                    throw new SocketTimeoutException("P5D attempt deadline expired while reading body");
                }
            }
        }
        if (deadlineNanos > 0 && System.nanoTime() > deadlineNanos) {
            throw new SocketTimeoutException("P5D attempt deadline expired after body read");
        }
        return new ReadBody(new String(output.toByteArray(), StandardCharsets.UTF_8), count);
    }

    private static long elapsedMillis(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    static final class ResponseBodyTooLargeException extends IOException { }

    /**
     * Builds the OpenAI-compatible envelope without silently changing the
     * caller's authorized output cap. Pilot callers validate the cap before
     * dispatch; this boundary rejects an unusable value instead of clamping it.
     */
    static JSONObject buildChatRequestBody(AppSettings s, PromptPair prompt,
                                           int maxOutputTokens) throws Exception {
        if (s == null) throw new IllegalArgumentException("Settings are required");
        if (prompt == null) throw new IllegalArgumentException("Prompt is required");
        if (maxOutputTokens <= 0) {
            throw new IllegalArgumentException("Output token cap is invalid");
        }
        JSONObject body = new JSONObject();
        body.put("model", s.model);
        body.put("temperature", s.temperature);
        body.put("max_tokens", maxOutputTokens);
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", prompt.system));
        messages.put(new JSONObject().put("role", "user").put("content", prompt.user));
        body.put("messages", messages);
        return body;
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

    private static String safeContentType(String value) {
        if (value == null || value.isBlank() || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return "";
        }
        String normalized = value.split(";", 2)[0].trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "application/json", "application/problem+json", "text/event-stream" -> normalized;
            default -> "";
        };
    }

    private static String safeExceptionClass(Throwable error) {
        if (error == null) return "RuntimeException";
        if (error instanceof ResponseBodyTooLargeException) return "ResponseBodyTooLargeException";
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
