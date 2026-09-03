package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** P01-P09 contract characterization/acceptance for source preflight. */
public class EditorialSourcePreflightTest {
    private static final EditorialSourcePreflight PREFLIGHT = new EditorialSourcePreflight();

    @Test public void P01_missingRawIsInputRequired() {
        Map<String, EditorialSourcePreflight.SourceInput> inputs = validInputs();
        inputs.remove(EditorialSafe4Contract.RAW);
        assertResult(EditorialSourcePreflight.Outcome.INPUT_REQUIRED,
                "INPUT_RAW_FILE_MISSING", PREFLIGHT.evaluate(normal(inputs)));
    }

    @Test public void P02_missingDraftIsInputRequired() {
        Map<String, EditorialSourcePreflight.SourceInput> inputs = validInputs();
        inputs.remove(EditorialSafe4Contract.DRAFT);
        assertResult(EditorialSourcePreflight.Outcome.INPUT_REQUIRED,
                "INPUT_DRAFT_FILE_MISSING", PREFLIGHT.evaluate(normal(inputs)));
    }

    @Test public void P03_missingGlossaryIsInputRequired() {
        Map<String, EditorialSourcePreflight.SourceInput> inputs = validInputs();
        inputs.remove(EditorialSafe4Contract.GLOSSARY);
        assertResult(EditorialSourcePreflight.Outcome.INPUT_REQUIRED,
                "INPUT_GLOSSARY_REQUIRED", PREFLIGHT.evaluate(normal(inputs)));
    }

    @Test public void P04_missingPronounInNormalModeIsInputRequired() {
        Map<String, EditorialSourcePreflight.SourceInput> inputs = validInputs();
        inputs.remove(EditorialSafe4Contract.PRONOUN);
        assertResult(EditorialSourcePreflight.Outcome.INPUT_REQUIRED,
                "INPUT_PRONOUN_FILE_MISSING", PREFLIGHT.evaluate(normal(inputs)));
    }

    @Test public void P05_unreadableHandleIsRetryRequired() {
        Map<String, EditorialSourcePreflight.SourceInput> inputs = validInputs();
        inputs.put(EditorialSafe4Contract.RAW,
                EditorialSourcePreflight.SourceInput.unreadable(EditorialSafe4Contract.RAW, "raw-handle", "text.v1"));
        assertResult(EditorialSourcePreflight.Outcome.RETRY_REQUIRED,
                "RETRY_SOURCE_BYTES_UNAVAILABLE", PREFLIGHT.evaluate(normal(inputs)));
    }

    @Test public void P06_invalidGlossarySchemaIsInputRequired() {
        Map<String, EditorialSourcePreflight.SourceInput> inputs = validInputs();
        inputs.put(EditorialSafe4Contract.GLOSSARY,
                EditorialSourcePreflight.SourceInput.readable(EditorialSafe4Contract.GLOSSARY,
                        "glossary", bytes("term\ttarget"), "wrong.schema.v1"));
        assertResult(EditorialSourcePreflight.Outcome.INPUT_REQUIRED,
                "INPUT_GLOSSARY_SCHEMA_INVALID", PREFLIGHT.evaluate(normal(inputs)));
    }

    @Test public void P07_invalidPronounSchemaIsInputRequired() {
        Map<String, EditorialSourcePreflight.SourceInput> inputs = validInputs();
        inputs.put(EditorialSafe4Contract.PRONOUN,
                EditorialSourcePreflight.SourceInput.readable(EditorialSafe4Contract.PRONOUN,
                        "pronoun", bytes("from\ttarget"), "wrong.schema.v1"));
        assertResult(EditorialSourcePreflight.Outcome.INPUT_REQUIRED,
                "INPUT_PRONOUN_SCHEMA_INVALID", PREFLIGHT.evaluate(normal(inputs)));
    }

    @Test public void P08_insufficientEvidencePreservesDraftAfterPreflight() {
        EditorialSourcePreflight.Result result = PREFLIGHT.evaluate(
                EditorialSourcePreflight.Request.normal(validInputs(), false));
        assertEquals(EditorialSourcePreflight.Outcome.PRESERVE_DRAFT, result.outcome());
        assertEquals("PRESERVE_DRAFT_EVIDENCE_INSUFFICIENT", result.reasonCode());
        assertTrue(result.passed());
        assertEquals(0, result.semanticRawReads());
        assertEquals(0, result.providerCalls());
    }

    @Test public void P09_pairContextAbsentContinuesAndAlternateRequiresExplicitChoice() {
        Map<String, EditorialSourcePreflight.SourceInput> inputs = validInputs();
        EditorialSourcePreflight.Result noPair = PREFLIGHT.evaluate(normal(inputs));
        assertEquals(EditorialSourcePreflight.Outcome.PASS, noPair.outcome());
        assertEquals(0, noPair.semanticRawReads());
        inputs.remove(EditorialSafe4Contract.PRONOUN);
        EditorialSourcePreflight.Result alternate = PREFLIGHT.evaluate(
                EditorialSourcePreflight.Request.alternateExplicit(inputs));
        assertEquals(EditorialSourcePreflight.Outcome.PASS, alternate.outcome());
        assertEquals("PREFLIGHT_PASS", alternate.reasonCode());
    }

    private static EditorialSourcePreflight.Request normal(Map<String, EditorialSourcePreflight.SourceInput> inputs) {
        return EditorialSourcePreflight.Request.normal(inputs);
    }

    private static void assertResult(EditorialSourcePreflight.Outcome outcome, String reason,
                                    EditorialSourcePreflight.Result result) {
        assertEquals(outcome, result.outcome());
        assertEquals(reason, result.reasonCode());
        assertTrue(result.downstreamNotEvaluated());
        assertEquals(0, result.semanticRawReads());
        assertEquals(0, result.providerCalls());
    }

    private static Map<String, EditorialSourcePreflight.SourceInput> validInputs() {
        Map<String, EditorialSourcePreflight.SourceInput> inputs = new LinkedHashMap<>();
        inputs.put(EditorialSafe4Contract.RAW, readable(EditorialSafe4Contract.RAW, "raw", "raw text"));
        inputs.put(EditorialSafe4Contract.DRAFT, readable(EditorialSafe4Contract.DRAFT, "draft", "draft text"));
        inputs.put(EditorialSafe4Contract.GLOSSARY, EditorialSourcePreflight.SourceInput.readable(
                EditorialSafe4Contract.GLOSSARY, "glossary", bytes("term\ttarget"), "safe4.full.glossary.v1"));
        inputs.put(EditorialSafe4Contract.PRONOUN, EditorialSourcePreflight.SourceInput.readable(
                EditorialSafe4Contract.PRONOUN, "pronoun", bytes("from\ttarget"), "safe4.full.pronoun.v1"));
        return inputs;
    }

    private static EditorialSourcePreflight.SourceInput readable(String role, String id, String text) {
        return EditorialSourcePreflight.SourceInput.readable(role, id, bytes(text), "text.v1");
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
