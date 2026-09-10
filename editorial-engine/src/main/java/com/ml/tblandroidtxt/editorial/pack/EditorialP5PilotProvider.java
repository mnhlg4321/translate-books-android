package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provider boundary for P5.  Implementations are injected; the contract does
 * not select a provider, contain credentials, or permit a provider to mutate
 * application state.
 */
public interface EditorialP5PilotProvider {
    enum CallKind { PRIMARY_SEMANTIC, SCHEMA_REPAIR }

    /**
     * Provider adapters use this typed boundary to preserve a redacted,
     * allowlisted transport classification without exposing exception text.
     */
    final class ProviderFailure extends Exception {
        private final String reasonCode;

        public ProviderFailure(String reasonCode) {
            super(reasonCode);
            if (reasonCode == null || reasonCode.isBlank()) {
                throw new IllegalArgumentException("provider failure reason code is required");
            }
            this.reasonCode = reasonCode;
        }

        public ProviderFailure(String reasonCode, Throwable cause) {
            this(reasonCode);
            initCause(cause);
        }

        public String reasonCode() { return reasonCode; }
    }

    record Request(String attemptIdentity, CallKind callKind, String provider, String model,
                   String phase, String requestEnvelopeHash,
                   Map<String, byte[]> visibleSources,
                   EditorialP5PilotRequest.PackAuthority authority,
                   String outputSchemaId, String chapterKey,
                   String priorSemanticFingerprint, Context context) {
        /**
         * Compatibility constructor for existing fake providers. Production
         * providers that render the full request envelope use the context-rich
         * constructor emitted by EditorialP5PilotExecution.
         */
        public Request(String attemptIdentity, CallKind callKind, String provider, String model,
                       String phase, String requestEnvelopeHash,
                       Map<String, byte[]> visibleSources,
                       EditorialP5PilotRequest.PackAuthority authority,
                       String outputSchemaId, String chapterKey,
                       String priorSemanticFingerprint) {
            this(attemptIdentity, callKind, provider, model, phase, requestEnvelopeHash,
                    visibleSources, authority, outputSchemaId, chapterKey,
                    priorSemanticFingerprint, null);
        }

        public Request {
            attemptIdentity = text(attemptIdentity, "attempt identity");
            Objects.requireNonNull(callKind, "call kind");
            provider = text(provider, "provider");
            model = text(model, "model");
            phase = text(phase, "phase");
            requestEnvelopeHash = text(requestEnvelopeHash, "request envelope hash");
            LinkedHashMap<String, byte[]> copy = new LinkedHashMap<>();
            if (visibleSources != null) {
                for (Map.Entry<String, byte[]> entry : visibleSources.entrySet()) {
                    copy.put(text(entry.getKey(), "visible source role"),
                            entry.getValue() == null ? null : entry.getValue().clone());
                }
            }
            visibleSources = java.util.Collections.unmodifiableMap(copy);
            Objects.requireNonNull(authority, "authority");
            outputSchemaId = text(outputSchemaId, "output schema id");
            chapterKey = text(chapterKey, "chapter key");
            priorSemanticFingerprint = priorSemanticFingerprint == null ? "" : priorSemanticFingerprint;
        }

        @Override public Map<String, byte[]> visibleSources() {
            LinkedHashMap<String, byte[]> copy = new LinkedHashMap<>();
            for (Map.Entry<String, byte[]> entry : visibleSources.entrySet()) {
                copy.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().clone());
            }
            return java.util.Collections.unmodifiableMap(copy);
        }

        /** Exact app-owned identity facts required by a real provider adapter. */
        public record Context(String bindingIdentity, String runDeclarationIdentity,
                              String manifestFingerprint, String bundleIdentity,
                              String predecessorIdentity, List<String> stableAnchors,
                              List<String> populationIds) {
            public Context {
                bindingIdentity = text(bindingIdentity, "binding identity");
                runDeclarationIdentity = text(runDeclarationIdentity, "run declaration identity");
                manifestFingerprint = text(manifestFingerprint, "manifest fingerprint");
                bundleIdentity = text(bundleIdentity, "bundle identity");
                predecessorIdentity = text(predecessorIdentity, "predecessor identity");
                stableAnchors = List.copyOf(stableAnchors == null ? List.of() : stableAnchors);
                populationIds = List.copyOf(populationIds == null ? List.of() : populationIds);
            }
        }
    }

    record Response(String responseId, byte[] responseBytes, String finishReason,
                    boolean transportComplete, int inputTokens, int outputTokens,
                    int totalTokens, BigDecimal reportedCost,
                    EditorialP5L1Output output, boolean schemaValid) {
        public Response {
            responseId = text(responseId, "response id");
            responseBytes = responseBytes == null ? new byte[0] : responseBytes.clone();
            finishReason = text(finishReason, "finish reason");
            if (inputTokens < 0 || outputTokens < 0 || totalTokens < 0) {
                throw new IllegalArgumentException("response usage cannot be negative");
            }
            reportedCost = Objects.requireNonNull(reportedCost, "reported cost");
            if (reportedCost.signum() < 0) throw new IllegalArgumentException("reported cost cannot be negative");
        }

        @Override public byte[] responseBytes() { return responseBytes.clone(); }
    }

    /**
     * Gives a provider one monotonic attempt window shared by primary and any
     * explicitly authorized schema-repair call. Existing fake providers keep
     * the source-compatible no-op default.
     */
    default void beginAttempt(long maximumExecutionTimeMillis) { }

    Response call(Request request) throws Exception;

    private static String text(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }
}
