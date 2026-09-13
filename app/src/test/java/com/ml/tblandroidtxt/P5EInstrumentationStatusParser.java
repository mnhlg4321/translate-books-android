package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Host-only parser for the redacted A4 exact-preflight instrumentation status.
 * It intentionally treats the runner's final instrumentation code {@code -1}
 * as the success sentinel; intermediate status codes are not terminal results.
 */
final class P5EInstrumentationStatusParser {
    static final String PREFIX = "p5e.preflight.v2.";
    static final String MANIFEST_VERSION = "p5e.9b.a4.redacted-preflight.v2";

    private static final Set<String> REQUIRED = Set.of(
            "manifestVersion", "testSuccess", "outputParse", "providerMatch",
            "modelMatch", "endpointMatch", "routeMatch", "conjunctionValid",
            "dbPreservation", "exactAcceptance", "providerCalls",
            "authorizationCreated", "attemptCreated", "reconciliationCreated");

    private static final Set<String> ALLOWED = Set.of(
            "manifestVersion", "execution", "productionPackage", "productionVersion",
            "productionVersionCode", "productionApkSha256", "productionCertificateSha256",
            "testPackage", "testApkSha256", "testSourceCommit", "testRunner", "dbSha256",
            "schemaVersion", "projectRowId", "selector", "chapterKey", "bindingIdentity",
            "runDeclarationIdentity", "compatibilityEvaluationId", "canonicalPackHash",
            "canonicalProfileHash", "attemptIdentity", "requestIdentity", "requestEnvelopeHash",
            "canonicalHttpRequestBodySha256", "canonicalHttpRequestBodyBytes", "jsonSchemaSha256",
            "jsonSchemaBytes", "wireSchemaVersion", "worstCaseWireBytes", "maximumWireBytes",
            "outputTokenCap", "contextSizeBytes", "sourceProjection", "sources",
            "packAuthorityRequired", "routeFingerprint", "provider", "model", "upstreamProvider",
            "stream", "responseFormat", "strict", "reasoningEffort", "requireParameters",
            "allowFallbacks", "only", "dataCollection", "plugins", "providerCalls",
            "dbCountsBefore", "dbCountsAfter", "lineageCounts", "authorizationCreated",
            "attemptCreated", "requestBodyStored", "fullModelResponseStored", "testSuccess",
            "outputParse", "providerMatch", "modelMatch", "endpointMatch", "routeMatch",
            "conjunctionValid", "dbPreservation", "exactAcceptance", "reconciliationCreated");

    private static final Set<String> BOOLEAN_FIELDS = Set.of(
            "testSuccess", "outputParse", "providerMatch", "modelMatch",
            "endpointMatch", "routeMatch", "conjunctionValid", "dbPreservation",
            "exactAcceptance", "authorizationCreated", "attemptCreated",
            "reconciliationCreated");

    private static final Set<String> INTEGER_ZERO_FIELDS = Set.of("providerCalls");

    private P5EInstrumentationStatusParser() { }

    static ParseResult parse(String raw) {
        List<String> errors = new ArrayList<>();
        Map<String, String> values = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) errors.add("OUTPUT_MISSING");

        int terminalCode = Integer.MIN_VALUE;
        if (raw != null) {
            for (String line : raw.split("\\R", -1)) {
                String statusPrefix = "INSTRUMENTATION_STATUS: ";
                if (line.startsWith(statusPrefix)) {
                    String payload = line.substring(statusPrefix.length());
                    int separator = payload.indexOf('=');
                    if (separator <= 0) {
                        errors.add("STATUS_MALFORMED");
                        continue;
                    }
                    String key = payload.substring(0, separator);
                    String value = payload.substring(separator + 1);
                    if (!key.startsWith(PREFIX)) continue; // standard runner key
                    String field = key.substring(PREFIX.length());
                    if (!ALLOWED.contains(field)) {
                        errors.add("UNKNOWN_FIELD:" + field);
                    } else if (values.put(field, value) != null) {
                        errors.add("DUPLICATE_FIELD:" + field);
                    }
                } else if (line.startsWith("INSTRUMENTATION_CODE:")) {
                    String value = line.substring("INSTRUMENTATION_CODE:".length()).trim();
                    try {
                        terminalCode = Integer.parseInt(value);
                    } catch (NumberFormatException error) {
                        errors.add("TERMINAL_CODE_MALFORMED");
                    }
                }
            }
        }

        for (String field : REQUIRED) {
            if (!values.containsKey(field)) errors.add("MISSING_FIELD:" + field);
        }
        for (String field : BOOLEAN_FIELDS) {
            String value = values.get(field);
            if (value != null && !"true".equals(value) && !"false".equals(value)) {
                errors.add("BOOLEAN_INVALID:" + field);
            }
        }
        for (String field : INTEGER_ZERO_FIELDS) {
            String value = values.get(field);
            if (value != null && !"0".equals(value)) errors.add("ZERO_INVALID:" + field);
        }
        if (!MANIFEST_VERSION.equals(values.get("manifestVersion"))) {
            errors.add("MANIFEST_VERSION_INVALID");
        }

        boolean testSuccess = "OK (1 test)".equals(findLine(raw, "OK (1 test)"))
                && "true".equals(values.get("testSuccess"))
                && !containsFailure(raw);
        boolean outputParseSuccess = errors.isEmpty();
        boolean providerMatch = "true".equals(values.get("providerMatch"));
        boolean modelMatch = "true".equals(values.get("modelMatch"));
        boolean endpointMatch = "true".equals(values.get("endpointMatch"));
        boolean routeMatch = "true".equals(values.get("routeMatch"));
        boolean conjunction = providerMatch && modelMatch && endpointMatch;
        boolean conjunctionValid = "true".equals(values.get("conjunctionValid"))
                && conjunction == routeMatch;
        boolean dbPreservation = "true".equals(values.get("dbPreservation"));
        boolean exactAcceptance = "true".equals(values.get("exactAcceptance"));
        boolean terminalSuccess = terminalCode == -1;
        boolean accepted = terminalSuccess && testSuccess && outputParseSuccess
                && conjunctionValid && dbPreservation && exactAcceptance
                && "0".equals(values.get("providerCalls"))
                && "false".equals(values.get("authorizationCreated"))
                && "false".equals(values.get("attemptCreated"))
                && "false".equals(values.get("reconciliationCreated"));

        return new ParseResult(accepted, terminalSuccess, testSuccess, outputParseSuccess,
                routeMatch, conjunctionValid, dbPreservation, exactAcceptance,
                terminalCode, values, errors);
    }

    private static String findLine(String raw, String expected) {
        if (raw == null) return null;
        for (String line : raw.split("\\R", -1)) if (expected.equals(line.trim())) return line.trim();
        return null;
    }

    private static boolean containsFailure(String raw) {
        if (raw == null) return true;
        String lower = raw.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("failures!!!") || lower.contains("there was 1 failure")
                || lower.contains("assumptionviolation") || lower.contains("skipped");
    }

    static final class ParseResult {
        final boolean accepted;
        final boolean terminalSuccess;
        final boolean testSuccess;
        final boolean outputParseSuccess;
        final boolean routeMatch;
        final boolean conjunctionValid;
        final boolean dbPreservation;
        final boolean exactAcceptance;
        final int terminalCode;
        final Map<String, String> values;
        final List<String> errors;

        private ParseResult(boolean accepted, boolean terminalSuccess, boolean testSuccess,
                            boolean outputParseSuccess, boolean routeMatch,
                            boolean conjunctionValid, boolean dbPreservation,
                            boolean exactAcceptance, int terminalCode,
                            Map<String, String> values, List<String> errors) {
            this.accepted = accepted;
            this.terminalSuccess = terminalSuccess;
            this.testSuccess = testSuccess;
            this.outputParseSuccess = outputParseSuccess;
            this.routeMatch = routeMatch;
            this.conjunctionValid = conjunctionValid;
            this.dbPreservation = dbPreservation;
            this.exactAcceptance = exactAcceptance;
            this.terminalCode = terminalCode;
            this.values = Map.copyOf(values);
            this.errors = List.copyOf(errors);
        }
    }
}
