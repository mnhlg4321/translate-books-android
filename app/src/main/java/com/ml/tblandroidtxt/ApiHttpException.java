package com.ml.tblandroidtxt;

public class ApiHttpException extends RuntimeException {
    public final int statusCode;
    public final long retryAfterMs;
    public ApiHttpException(int statusCode, long retryAfterMs, String message) {
        super(message); this.statusCode = statusCode; this.retryAfterMs = Math.max(0, retryAfterMs);
    }
}
