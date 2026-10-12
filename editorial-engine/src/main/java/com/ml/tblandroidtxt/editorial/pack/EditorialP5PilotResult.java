package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Typed outcome, recovery evidence and bounded metrics for one P5 attempt. */
public final class EditorialP5PilotResult {
    public enum Outcome { COMMITTED, PRESERVE_DRAFT, STOP, ALREADY_COMMITTED }

    public enum StopClass {
        AUTHORIZATION_REQUIRED,
        AUTHORIZATION_EXPIRED,
        AUTHORIZATION_USED,
        BINDING_MISMATCH,
        STALE_CHAIN,
        INPUT_REQUIRED,
        RETRY_REQUIRED,
        REPAIR_REQUIRED,
        CONTENT_BLOCKED,
        BUDGET_EXCEEDED,
        PHASE_NOT_L1,
        CONCURRENT_WRITER,
        PROVIDER_ERROR,
        VALIDATION_FAILED,
        EXECUTION_DISABLED
    }

    public record StopReceipt(StopClass stopClass, String reasonCode, String phase,
                              String blockingGate, List<String> evidenceRefs,
                              String affectedScope, String recoveryAction,
                              String resumeFrom, boolean retryable) {
        public StopReceipt {
            Objects.requireNonNull(stopClass, "stop class");
            text(reasonCode, "reason code");
            text(phase, "phase");
            text(blockingGate, "blocking gate");
            evidenceRefs = List.copyOf(evidenceRefs == null ? List.of() : evidenceRefs);
            text(affectedScope, "affected scope");
            text(recoveryAction, "recovery action");
            text(resumeFrom, "resume from");
            if (stopClass == StopClass.CONTENT_BLOCKED && evidenceRefs.isEmpty()) {
                throw new IllegalArgumentException("content block requires evidence");
            }
        }
    }

    public record Metrics(
            int providerCallsBeforePreflight,
            int primaryCalls,
            int repairCalls,
            int networkRetries,
            int inputTokens,
            int outputTokens,
            int reasoningTokens,
            int totalTokens,
            BigDecimal estimatedCost,
            BigDecimal actualReportedCost,
            int requestContextSize,
            String finishReason,
            boolean truncated,
            boolean schemaValidationPassed,
            boolean receiptValidationPassed,
            int preserveDraftCount,
            int findingCount,
            int falseStopCount,
            long latencyMillis,
            boolean costAccountingComplete) {
        /** Source-compatible constructor for pre-P5E metric producers. */
        public Metrics(int providerCallsBeforePreflight, int primaryCalls, int repairCalls,
                       int networkRetries, int inputTokens, int outputTokens, int totalTokens,
                       BigDecimal estimatedCost, BigDecimal actualReportedCost,
                       int requestContextSize, String finishReason, boolean truncated,
                       boolean schemaValidationPassed, boolean receiptValidationPassed,
                       int preserveDraftCount, int findingCount, int falseStopCount,
                       long latencyMillis) {
            this(providerCallsBeforePreflight, primaryCalls, repairCalls, networkRetries,
                    inputTokens, outputTokens, 0, totalTokens, estimatedCost,
                    actualReportedCost, requestContextSize, finishReason, truncated,
                    schemaValidationPassed, receiptValidationPassed, preserveDraftCount,
                    findingCount, falseStopCount, latencyMillis, true);
        }

        public Metrics {
            if (providerCallsBeforePreflight < 0 || primaryCalls < 0 || repairCalls < 0
                    || networkRetries < 0 || inputTokens < 0 || outputTokens < 0
                    || reasoningTokens < 0 || totalTokens < 0 || requestContextSize < 0 || preserveDraftCount < 0
                    || findingCount < 0 || falseStopCount < 0 || latencyMillis < 0) {
                throw new IllegalArgumentException("pilot metrics cannot be negative");
            }
            estimatedCost = Objects.requireNonNull(estimatedCost, "estimated cost");
            actualReportedCost = Objects.requireNonNull(actualReportedCost, "actual reported cost");
            if (estimatedCost.signum() < 0 || actualReportedCost.signum() < 0) {
                throw new IllegalArgumentException("pilot cost cannot be negative");
            }
            finishReason = finishReason == null ? "NOT_CALLED" : finishReason;
        }

        public static Metrics empty() {
            return new Metrics(0, 0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO,
                    BigDecimal.ZERO, 0, "NOT_CALLED", false, false, false,
                    0, 0, 0, 0L, true);
        }
    }

    public static final class CommittedResult {
        private final String attemptIdentity;
        private final String requestIdentity;
        private final String responseIdentity;
        private final byte[] reportBytes;
        private final byte[] receiptBytes;
        private final Metrics metrics;
        private final EditorialP5L1Output output;

        public CommittedResult(String attemptIdentity, String requestIdentity,
                               String responseIdentity, byte[] reportBytes,
                               byte[] receiptBytes, Metrics metrics,
                               EditorialP5L1Output output) {
            this(attemptIdentity, requestIdentity, responseIdentity, reportBytes,
                    receiptBytes, metrics, output, false);
        }

        private CommittedResult(String attemptIdentity, String requestIdentity,
                                String responseIdentity, byte[] reportBytes,
                                byte[] receiptBytes, Metrics metrics,
                                EditorialP5L1Output output, boolean persistedReadback) {
            this.attemptIdentity = text(attemptIdentity, "attempt identity");
            this.requestIdentity = text(requestIdentity, "request identity");
            this.responseIdentity = text(responseIdentity, "response identity");
            this.reportBytes = reportBytes == null ? new byte[0] : reportBytes.clone();
            this.receiptBytes = receiptBytes == null ? new byte[0] : receiptBytes.clone();
            if (this.reportBytes.length == 0 || this.receiptBytes.length == 0) {
                throw new IllegalArgumentException("committed result requires report and receipt bytes");
            }
            this.metrics = Objects.requireNonNull(metrics, "metrics");
            if (!persistedReadback) this.output = Objects.requireNonNull(output, "output");
            else this.output = output;
        }

        /**
         * Rehydrates only the durable, redacted result envelope. Semantic
         * output is intentionally absent after process restart; callers must
         * use the persisted report/receipt and identities, never re-call a
         * provider to reconstruct it.
         */
        public static CommittedResult persisted(String attemptIdentity, String requestIdentity,
                                                String responseIdentity, byte[] reportBytes,
                                                byte[] receiptBytes, Metrics metrics) {
            return new CommittedResult(attemptIdentity, requestIdentity, responseIdentity,
                    reportBytes, receiptBytes, metrics, null, true);
        }

        public String attemptIdentity() { return attemptIdentity; }
        public String requestIdentity() { return requestIdentity; }
        public String responseIdentity() { return responseIdentity; }
        public byte[] reportBytes() { return reportBytes.clone(); }
        public byte[] receiptBytes() { return receiptBytes.clone(); }
        public Metrics metrics() { return metrics; }
        public EditorialP5L1Output output() { return output; }
    }

    private final Outcome outcome;
    private final String reasonCode;
    private final String requestIdentity;
    private final CommittedResult committedResult;
    private final StopReceipt stopReceipt;
    private final Metrics metrics;

    private EditorialP5PilotResult(Outcome outcome, String reasonCode, String requestIdentity,
                                   CommittedResult committedResult, StopReceipt stopReceipt,
                                   Metrics metrics) {
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.reasonCode = text(reasonCode, "reason code");
        this.requestIdentity = requestIdentity == null ? "" : requestIdentity;
        this.committedResult = committedResult;
        this.stopReceipt = stopReceipt;
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        if (outcome == Outcome.STOP && stopReceipt == null) {
            throw new IllegalArgumentException("STOP requires a stop receipt");
        }
        if (outcome != Outcome.STOP && stopReceipt != null) {
            throw new IllegalArgumentException("only STOP carries a stop receipt");
        }
        if (outcome == Outcome.COMMITTED && committedResult == null) {
            throw new IllegalArgumentException("committed outcomes require a committed result");
        }
    }

    public Outcome outcome() { return outcome; }
    public String reasonCode() { return reasonCode; }
    public String requestIdentity() { return requestIdentity; }
    public CommittedResult committedResult() { return committedResult; }
    public StopReceipt stopReceipt() { return stopReceipt; }
    public Metrics metrics() { return metrics; }

    static EditorialP5PilotResult stopped(String requestIdentity, StopClass stopClass,
                                           String reasonCode, String phase,
                                           String blockingGate, List<String> evidenceRefs,
                                           String affectedScope, String recoveryAction,
                                           String resumeFrom, boolean retryable,
                                           Metrics metrics) {
        StopReceipt receipt = new StopReceipt(stopClass, reasonCode, phase, blockingGate,
                evidenceRefs, affectedScope, recoveryAction, resumeFrom, retryable);
        return new EditorialP5PilotResult(Outcome.STOP, reasonCode, requestIdentity,
                null, receipt, metrics);
    }

    static EditorialP5PilotResult committed(String requestIdentity, CommittedResult result,
                                             Metrics metrics) {
        return new EditorialP5PilotResult(Outcome.COMMITTED, "L1_COMMITTED",
                requestIdentity, result, null, metrics);
    }

    static EditorialP5PilotResult preserved(String requestIdentity, CommittedResult result,
                                             String reasonCode, Metrics metrics) {
        return new EditorialP5PilotResult(Outcome.PRESERVE_DRAFT, reasonCode,
                requestIdentity, result, null, metrics);
    }

    static EditorialP5PilotResult preserveWithoutProvider(String requestIdentity,
                                                           String reasonCode, Metrics metrics) {
        return new EditorialP5PilotResult(Outcome.PRESERVE_DRAFT, reasonCode,
                requestIdentity, null, null, metrics);
    }

    static EditorialP5PilotResult alreadyCommitted(String requestIdentity,
                                                   CommittedResult result) {
        return new EditorialP5PilotResult(Outcome.ALREADY_COMMITTED, "L1_ALREADY_COMMITTED",
                requestIdentity, result, null, result.metrics());
    }

    private static String text(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }
}
