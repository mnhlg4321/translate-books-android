package com.ml.tblandroidtxt;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class P5EInstrumentationStatusParserTest {
    @Test
    public void acceptsCompleteStatusWithTerminalMinusOneAndStandardRunnerKeys() {
        P5EInstrumentationStatusParser.ParseResult result =
                P5EInstrumentationStatusParser.parse(successOutput());
        assertTrue(result.accepted);
        assertTrue(result.terminalSuccess);
        assertTrue(result.testSuccess);
        assertTrue(result.outputParseSuccess);
        assertTrue(result.routeMatch);
        assertTrue(result.conjunctionValid);
        assertTrue(result.dbPreservation);
        assertTrue(result.exactAcceptance);
    }

    @Test
    public void acceptsStatusFieldsInAnyOrder() {
        List<String> lines = new ArrayList<>(List.of(successOutput().split("\\R")));
        Collections.reverse(lines);
        P5EInstrumentationStatusParser.ParseResult result =
                P5EInstrumentationStatusParser.parse(String.join("\n", lines));
        assertTrue(result.accepted);
    }

    @Test
    public void rejectsMissingField() {
        P5EInstrumentationStatusParser.ParseResult result =
                P5EInstrumentationStatusParser.parse(successOutput()
                        .replace("INSTRUMENTATION_STATUS: p5e.preflight.v2.exactAcceptance=true\n", ""));
        assertFalse(result.outputParseSuccess);
        assertFalse(result.accepted);
    }

    @Test
    public void rejectsDuplicateField() {
        P5EInstrumentationStatusParser.ParseResult result =
                P5EInstrumentationStatusParser.parse(successOutput()
                        .replace("INSTRUMENTATION_CODE: -1", "INSTRUMENTATION_STATUS: p5e.preflight.v2.routeMatch=true\nINSTRUMENTATION_CODE: -1"));
        assertFalse(result.outputParseSuccess);
        assertFalse(result.accepted);
    }

    @Test
    public void rejectsInvalidBooleanType() {
        P5EInstrumentationStatusParser.ParseResult result =
                P5EInstrumentationStatusParser.parse(successOutput()
                        .replace("p5e.preflight.v2.routeMatch=true", "p5e.preflight.v2.routeMatch=TRUE"));
        assertFalse(result.outputParseSuccess);
        assertFalse(result.accepted);
    }

    @Test
    public void rejectsTruncatedOutputWithoutTerminal() {
        String output = successOutput();
        P5EInstrumentationStatusParser.ParseResult result =
                P5EInstrumentationStatusParser.parse(output.substring(0, output.length() / 2));
        assertFalse(result.terminalSuccess);
        assertFalse(result.accepted);
    }

    @Test
    public void rejectsFailureAndSkipEvenIfTerminalCodeIsMinusOne() {
        P5EInstrumentationStatusParser.ParseResult failure =
                P5EInstrumentationStatusParser.parse(successOutput()
                        .replace("OK (1 test)", "FAILURES!!!") + "\nThere was 1 failure\n");
        assertFalse(failure.testSuccess);
        assertFalse(failure.accepted);

        P5EInstrumentationStatusParser.ParseResult skipped =
                P5EInstrumentationStatusParser.parse(successOutput()
                        .replace("OK (1 test)", "OK (0 tests)") + "\nAssumptionViolatedException\n");
        assertFalse(skipped.testSuccess);
        assertFalse(skipped.accepted);
    }

    @Test
    public void rejectsUnknownNamespacedFieldButIgnoresStandardRunnerStatus() {
        P5EInstrumentationStatusParser.ParseResult standard =
                P5EInstrumentationStatusParser.parse(successOutput()
                        .replace("INSTRUMENTATION_STATUS: p5e.preflight.v2.manifestVersion=",
                                "INSTRUMENTATION_STATUS: class=com.ml.tblandroidtxt.Test\n"
                                        + "INSTRUMENTATION_STATUS: p5e.preflight.v2.manifestVersion="));
        assertTrue(standard.accepted);

        P5EInstrumentationStatusParser.ParseResult unknown =
                P5EInstrumentationStatusParser.parse(successOutput()
                        .replace("INSTRUMENTATION_CODE: -1",
                                "INSTRUMENTATION_STATUS: p5e.preflight.v2.unexpected=true\n"
                                        + "INSTRUMENTATION_CODE: -1"));
        assertFalse(unknown.outputParseSuccess);
        assertFalse(unknown.accepted);
    }

    @Test
    public void rejectsFalseConjunctionAndSeparatesRouteFromExactAcceptance() {
        String output = successOutput()
                .replace("p5e.preflight.v2.modelMatch=true", "p5e.preflight.v2.modelMatch=false")
                .replace("p5e.preflight.v2.routeMatch=true", "p5e.preflight.v2.routeMatch=false")
                .replace("p5e.preflight.v2.conjunctionValid=true", "p5e.preflight.v2.conjunctionValid=true")
                .replace("p5e.preflight.v2.exactAcceptance=true", "p5e.preflight.v2.exactAcceptance=false");
        P5EInstrumentationStatusParser.ParseResult result =
                P5EInstrumentationStatusParser.parse(output);
        assertTrue(result.outputParseSuccess);
        assertFalse(result.routeMatch);
        assertTrue(result.conjunctionValid);
        assertFalse(result.exactAcceptance);
        assertFalse(result.accepted);
    }

    private static String successOutput() {
        return String.join("\n",
                "INSTRUMENTATION_STATUS: class=com.ml.tblandroidtxt.EditorialP5EFreshRawLiveInstrumentedTest",
                "INSTRUMENTATION_STATUS: test=freshRawExactPreflightRunsOnlyWhenExplicitlyOptedIn",
                "INSTRUMENTATION_STATUS: p5e.preflight.v2.manifestVersion="
                        + P5EInstrumentationStatusParser.MANIFEST_VERSION,
                "INSTRUMENTATION_STATUS: p5e.preflight.v2.testSuccess=true",
                "INSTRUMENTATION_STATUS: p5e.preflight.v2.outputParse=true",
                "INSTRUMENTATION_STATUS: p5e.preflight.v2.providerMatch=true",
                "INSTRUMENTATION_STATUS: p5e.preflight.v2.modelMatch=true",
                "INSTRUMENTATION_STATUS: p5e.preflight.v2.endpointMatch=true",
                "INSTRUMENTATION_STATUS: p5e.preflight.v2.routeMatch=true",
                "INSTRUMENTATION_STATUS: p5e.preflight.v2.conjunctionValid=true",
                "INSTRUMENTATION_STATUS: p5e.preflight.v2.dbPreservation=true",
                "INSTRUMENTATION_STATUS: p5e.preflight.v2.exactAcceptance=true",
                "INSTRUMENTATION_STATUS: p5e.preflight.v2.providerCalls=0",
                "INSTRUMENTATION_STATUS: p5e.preflight.v2.authorizationCreated=false",
                "INSTRUMENTATION_STATUS: p5e.preflight.v2.attemptCreated=false",
                "INSTRUMENTATION_STATUS: p5e.preflight.v2.reconciliationCreated=false",
                "INSTRUMENTATION_STATUS_CODE: 0",
                "INSTRUMENTATION_CODE: -1",
                "OK (1 test)");
    }
}
