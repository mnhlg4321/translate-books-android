package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Provider boundary for P5.  Implementations are injected; the contract does
 * not select a provider, contain credentials, or permit a provider to mutate
 * application state.
 */
public interface EditorialP5PilotProvider {
    enum CallKind { PRIMARY_SEMANTIC, SCHEMA_REPAIR }

    record Request(String attemptIdentity, CallKind callKind, String provider, String model,
                   String phase, String requestEnvelopeHash,
                   Map<String, byte[]> visibleSources,
                   EditorialP5PilotRequest.PackAuthority authority,
                   String outputSchemaId, String chapterKey,
                   String priorSemanticFingerprint) {
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

    Response call(Request request) throws Exception;

    private static String text(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }
}
