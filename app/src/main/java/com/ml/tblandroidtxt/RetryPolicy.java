package com.ml.tblandroidtxt;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;

public final class RetryPolicy {
    public enum Kind { NETWORK, RATE_LIMIT, TIMEOUT, EMPTY_RESPONSE, TRUNCATION, VALIDATION, CONTEXT_LENGTH, MALFORMED_OUTPUT, PROVIDER, NON_RETRYABLE }
    public static Kind classify(Throwable error) {
        Throwable root = error;
        while (root != null && root.getCause() != null && root.getCause() != root) root = root.getCause();
        if (root instanceof SocketTimeoutException) return Kind.TIMEOUT;
        if (root instanceof UnknownHostException) return Kind.NETWORK;
        if (root instanceof ApiHttpException) {
            int code = ((ApiHttpException) root).statusCode;
            if (code == 401 || code == 402 || code == 403 || code == 400 || code == 404 || code == 422) return Kind.NON_RETRYABLE;
            if (code == 408) return Kind.TIMEOUT;
            if (code == 409) return Kind.PROVIDER;
            if (code == 413) return Kind.CONTEXT_LENGTH;
            if (code == 429) return Kind.RATE_LIMIT;
            if (code >= 500) return Kind.PROVIDER;
        }
        String m = String.valueOf(root == null ? "" : root.getMessage()).toLowerCase(Locale.ROOT);
        if (m.contains("401") || m.contains("403") || m.contains("auth") || m.contains("api key")) return Kind.NON_RETRYABLE;
        if (m.contains("429") || m.contains("rate limit") || m.contains("quota")) return Kind.RATE_LIMIT;
        if (m.contains("timeout") || m.contains("timed out") || m.contains("408")) return Kind.TIMEOUT;
        if (m.contains("context") || m.contains("too large") || m.contains("413")) return Kind.CONTEXT_LENGTH;
        if (m.contains("empty response") || m.contains("0 tokens") || m.contains("no choices")) return Kind.EMPTY_RESPONSE;
        if (m.contains("finish_reason_length") || m.contains("likely_truncated") || m.contains("token_limit")) return Kind.TRUNCATION;
        if (root instanceof ResponseValidator.RejectedResponseException || m.contains("response validation failed")) return Kind.VALIDATION;
        if (m.contains("missing <translation>") || m.contains("meta/commentary") || m.contains("malformed")) return Kind.MALFORMED_OUTPUT;
        if (m.contains("500") || m.contains("502") || m.contains("503") || m.contains("504")) return Kind.PROVIDER;
        if (m.contains("connect") || m.contains("network") || m.contains("host")) return Kind.NETWORK;
        return Kind.NON_RETRYABLE;
    }
    public static boolean mayRetry(Kind kind, int attempt, int maxAttempts) {
        return attempt < Math.max(1, maxAttempts) && kind != Kind.NON_RETRYABLE && kind != Kind.CONTEXT_LENGTH;
    }
    public static long backoffMs(Kind kind, int attempt) {
        long base = kind == Kind.RATE_LIMIT ? 4000 : kind == Kind.NETWORK || kind == Kind.PROVIDER ? 2000 : 1000;
        long exponential = base * (1L << Math.min(10, Math.max(0, attempt - 1)));
        long jitter = Math.abs((kind.name().hashCode() * 31L + attempt * 997L) % Math.max(1, base / 2));
        return Math.min(60000, exponential + jitter);
    }
    public static long backoffMs(Kind kind,int attempt,long initialMs,long maximumMs){long initial=Math.max(100,initialMs);long exponential=initial*(1L<<Math.min(10,Math.max(0,attempt-1)));long jitter=Math.abs((kind.name().hashCode()*31L+attempt*997L)%Math.max(1,initial/2));return Math.min(Math.max(initial,maximumMs),exponential+jitter);}
    public static long parseRetryAfterMs(String value,long nowMs){if(value==null||value.trim().isEmpty())return 0;try{return Math.max(0,(long)(Double.parseDouble(value.trim())*1000));}catch(Exception ignored){}try{return Math.max(0,java.time.ZonedDateTime.parse(value.trim(),java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()-nowMs);}catch(Exception ignored){return 0;}}
    public static long retryAfterMs(Throwable error) {
        Throwable root=error; while(root!=null&&root.getCause()!=null&&root.getCause()!=root) root=root.getCause();
        return root instanceof ApiHttpException ? ((ApiHttpException)root).retryAfterMs : 0;
    }
    private RetryPolicy() {}
}
