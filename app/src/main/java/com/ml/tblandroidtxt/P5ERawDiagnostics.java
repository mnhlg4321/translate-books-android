package com.ml.tblandroidtxt;

import android.util.Log;

import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotResult;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Privacy-safe, device-local diagnostics for the fresh P5E RAW path. It records rule codes, sizes,
 * token and cost numbers, finish reason and typed stop facts only. Model text, prompts, request
 * bodies, keys and exception messages that are not fixed code vocabulary are never logged. Every
 * logging call is best effort and cannot change the result of the call it observes.
 */
final class P5ERawDiagnostics {
    static final String TAG = "P5E_RAW";
    private static final int MAX_VALUE = 120;
    private static final int MAX_REFS = 16;
    private static final Pattern SAFE_MESSAGE = Pattern.compile("[A-Za-z0-9_ :.\\-]{1,120}");

    private P5ERawDiagnostics() { }

    /** Keeps only vocabulary-like characters and bounds the length. */
    static String token(String value) {
        if (value == null) return "null";
        String cleaned = value.replaceAll("[^A-Za-z0-9_./:@+-]", "_");
        return cleaned.length() > MAX_VALUE ? cleaned.substring(0, MAX_VALUE) : cleaned;
    }

    /**
     * Class name plus, only for our own fixed-code exceptions, the message. JSON and other library
     * messages can quote the parsed text, so they are suppressed.
     */
    static String describe(Throwable error) {
        if (error == null) return "none";
        String name = token(error.getClass().getSimpleName());
        boolean ours = error instanceof IllegalArgumentException || error instanceof IllegalStateException;
        String message = error.getMessage();
        if (ours && message != null && SAFE_MESSAGE.matcher(message).matches()) {
            return name + ":" + token(message);
        }
        return name + ":message_suppressed";
    }

    static void chat(String finishReason, int contentBytes, int promptTokens, int completionTokens,
                     int reasoningTokens, int totalTokens, boolean providerCostReported,
                     boolean costKnown, Object cost) {
        write(Log.INFO, "chat finish=" + token(finishReason) + " contentBytes=" + contentBytes
                + " prompt=" + promptTokens + " completion=" + completionTokens
                + " reasoning=" + reasoningTokens + " total=" + totalTokens
                + " providerCostReported=" + providerCostReported + " costKnown=" + costKnown
                + " cost=" + token(String.valueOf(cost)));
    }

    static void parseRejected(Throwable error, int contentBytes) {
        write(Log.WARN, "parse_rejected error=" + describe(error) + " contentBytes=" + contentBytes);
    }

    static void stop(String reason) {
        write(Log.WARN, "dispatch_stop reason=" + token(reason));
    }

    static void dispatch(EditorialP5CExactBindingExecution.Result result) {
        if (result == null) return;
        StringBuilder line = new StringBuilder("dispatch status=").append(token(String.valueOf(result.status())))
                .append(" reason=").append(token(result.reasonCode()))
                .append(" providerCalls=").append(result.providerCalls());
        EditorialP5PilotResult raw = result.rawResult();
        if (raw != null) {
            line.append(" outcome=").append(token(String.valueOf(raw.outcome())))
                    .append(" rawReason=").append(token(raw.reasonCode()));
            EditorialP5PilotResult.StopReceipt stop = raw.stopReceipt();
            if (stop != null) {
                line.append(" stopClass=").append(token(String.valueOf(stop.stopClass())))
                        .append(" gate=").append(token(stop.blockingGate()))
                        .append(" retryable=").append(stop.retryable());
                List<String> refs = stop.evidenceRefs();
                line.append(" refs=").append(refs.size());
                for (int i = 0; i < refs.size() && i < MAX_REFS; i++) {
                    line.append(i == 0 ? " [" : ",").append(token(refs.get(i)));
                    if (i == refs.size() - 1 || i == MAX_REFS - 1) line.append(']');
                }
            }
            EditorialP5PilotResult.Metrics m = raw.metrics();
            if (m != null) {
                line.append(" finish=").append(token(m.finishReason()))
                        .append(" in=").append(m.inputTokens()).append(" out=").append(m.outputTokens())
                        .append(" reasoning=").append(m.reasoningTokens())
                        .append(" total=").append(m.totalTokens())
                        .append(" reportedCost=").append(token(String.valueOf(m.actualReportedCost())))
                        .append(" estimatedCost=").append(token(String.valueOf(m.estimatedCost())))
                        .append(" costComplete=").append(m.costAccountingComplete())
                        .append(" truncated=").append(m.truncated())
                        .append(" schemaOk=").append(m.schemaValidationPassed())
                        .append(" receiptOk=").append(m.receiptValidationPassed())
                        .append(" latencyMs=").append(m.latencyMillis());
            }
        }
        write(Log.INFO, line.toString());
    }

    private static void write(int priority, String message) {
        try {
            Log.println(priority, TAG, message);
        } catch (Throwable ignored) {
            // JVM unit tests and restricted environments have no logger; diagnostics are optional.
        }
    }
}
