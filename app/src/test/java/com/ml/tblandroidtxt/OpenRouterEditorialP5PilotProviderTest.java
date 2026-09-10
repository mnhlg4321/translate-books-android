package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class OpenRouterEditorialP5PilotProviderTest {
    @Test public void parsedIdentityIsRetainedForLocalValidation() throws Exception {
        EditorialP5PilotProvider.Request request = request();
        JSONObject root = new JSONObject();
        root.put("reportSchemaVersion", "safe4.full.report-l1.v1");
        root.put("receiptSchemaVersion", "safe4.full.receipt.v1");
        root.put("bindingIdentity", "model-must-not-control-this");
        root.put("manifestFingerprint", "model-must-not-control-this");
        root.put("chapterKey", "001");
        root.put("phase", "L1");
        root.put("bundleIdentity", "model-must-not-control-this");
        root.put("predecessorIdentity", "model-must-not-control-this");
        root.put("stableAnchors", List.of("chapter:001"));
        JSONObject ledger = new JSONObject().put("populationIds", List.of("population:001"));
        ledger.put("entries", List.of(new JSONObject().put("itemId", "population:001")
                .put("disposition", "PROCESSED")
                .put("evidenceRefs", List.of("evidence:local"))));
        root.put("ledger", ledger);
        Map<String, String> gates = new LinkedHashMap<>();
        for (String gate : EditorialSafe4Contract.GATE_IDS) gates.put(gate, "PASS");
        root.put("gates", new JSONObject(gates));
        root.put("preservedInventory", List.of());
        root.put("declaredChanges", List.of());
        root.put("beforeText", "");
        root.put("afterText", "");
        root.put("releaseAttemptCount", 0);
        root.put("disposition", new JSONObject().put("disposition", "CONTINUE")
                .put("reasonCode", "LOCAL_REVIEW").put("phase", "L1")
                .put("blockingGate", "COVERAGE").put("evidenceRefs", List.of())
                .put("affectedScope", "NONE").put("recoveryAction", "No action")
                .put("resumeFrom", "L1"));
        root.put("evidenceRefs", List.of("evidence:local"));
        root.put("modelDeclaredPass", true);

        var output = OpenRouterEditorialP5PilotProvider.parseCanonicalOutput(root.toString(), request);
        assertEquals("model-must-not-control-this", output.bindingIdentity());
        assertEquals("model-must-not-control-this", output.manifestFingerprint());
        assertEquals("model-must-not-control-this", output.bundleIdentity());
        assertTrue(output.modelDeclaredPass());
    }

    @Test public void missingFinishReasonIsNotAcceptedAsComplete() {
        try {
            OpenRouterEditorialP5PilotProvider.requireFinishReason("");
            throw new AssertionError("a missing finish reason must be typed as a response failure");
        } catch (EditorialP5PilotProvider.ProviderFailure expected) {
            assertEquals("RETRY_PROVIDER_RESPONSE_PARSE_FAILED", expected.reasonCode());
        }
    }

    @Test public void compactRawResponseHasNoSourceAndMaterializesAppOwnedFacts() throws Exception {
        EditorialP5PilotProvider.Request request = request();
        JSONObject root = compactRoot(request);
        String rawJson = root.toString();

        var wire = OpenRouterEditorialP5PilotProvider.parseRawOutput(rawJson, request);
        var output = wire.materialize(request);
        assertEquals("raw", output.beforeText());
        assertEquals(output.beforeText(), output.afterText());
        assertEquals("binding", output.bindingIdentity());
        assertEquals("manifest", output.manifestFingerprint());
        assertEquals("bundle", output.bundleIdentity());
        assertTrue(output.declaredChanges().isEmpty());
        assertFalse(rawJson.contains("beforeText"));
        assertFalse(rawJson.contains("afterText"));
        assertFalse(rawJson.contains("raw chapter"));
    }

    @Test public void compactUnknownFieldIsRejectedByLocalStrictParser() throws Exception {
        JSONObject root = compactRoot(request()).put("sourceText", "raw");
        try {
            OpenRouterEditorialP5PilotProvider.parseRawOutput(root.toString(), request());
            throw new AssertionError("unknown compact field must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("unknown or missing"));
        }
    }

    @Test public void compactDeclaredChangesAreRejectedBeforeMaterialization() throws Exception {
        JSONObject root = compactRoot(request());
        root.put("declaredChanges", List.of(new JSONObject().put("lineNumber", 1)));
        try {
            OpenRouterEditorialP5PilotProvider.parseRawOutput(root.toString(), request());
            throw new AssertionError("RAW edits must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("RAW_DECLARED_CHANGES_FORBIDDEN"));
        }
    }

    @Test public void compactSizeAndUnsafeTokenLimitsFailClosed() throws Exception {
        EditorialP5PilotProvider.Request request = request();
        JSONObject tooManyEvidenceRefs = compactRoot(request);
        tooManyEvidenceRefs.put("evidenceRefs", List.of(
                "e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9"));
        try {
            OpenRouterEditorialP5PilotProvider.parseRawOutput(tooManyEvidenceRefs.toString(), request);
            throw new AssertionError("evidence reference limit must be enforced");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("limit"));
        }

        JSONObject unsafeItemId = compactRoot(request);
        JSONObject unsafeFinding = new JSONObject().put("itemId", "population:001\nsource")
                .put("disposition", "PROCESSED")
                .put("evidenceRefs", List.of("evidence:raw"))
                .put("modelDeclaredPass", true);
        unsafeItemId.put("findings", new JSONArray().put(unsafeFinding));
        try {
            OpenRouterEditorialP5PilotProvider.parseRawOutput(unsafeItemId.toString(), request);
            throw new AssertionError("newline in an identifier must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("RAW_WIRE_ITEM_ID_INVALID"));
        }

        JSONObject unsafeDispositionText = compactRoot(request);
        unsafeDispositionText.getJSONObject("disposition")
                .put("reasonCode", "BAD\"QUOTE");
        try {
            OpenRouterEditorialP5PilotProvider.parseRawOutput(unsafeDispositionText.toString(), request);
            throw new AssertionError("JSON escaping must not widen safe text grammar");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("RAW_WIRE_DISPOSITION_TEXT_INVALID"));
        }
    }

    @Test public void compactBodyAboveHardCeilingIsRejectedBeforeMaterialization() throws Exception {
        try {
            OpenRouterEditorialP5PilotProvider.parseRawOutput(
                    "x".repeat(EditorialP5RawWireContract.MAX_WIRE_BYTES + 1), request());
            throw new AssertionError("body above hard ceiling must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("RAW_WIRE_BYTE_LIMIT_EXCEEDED"));
        }
    }

    @Test(expected = RuntimeException.class)
    public void rejectsNonJsonOrPartialResponse() throws Exception {
        OpenRouterEditorialP5PilotProvider.parseOutput("not-json", request());
    }

    private static EditorialP5PilotProvider.Request request() {
        EditorialP5PilotProvider.Request.Context context =
                new EditorialP5PilotProvider.Request.Context(
                        "binding", "run", "manifest", "bundle", "predecessor",
                        List.of("chapter:001"), List.of("population:001"));
        return new EditorialP5PilotProvider.Request("a".repeat(64),
                EditorialP5PilotProvider.CallKind.PRIMARY_SEMANTIC, "openrouter",
                "google/gemini-2.5-flash", "L1_RAW_DISCOVERY", "e".repeat(64),
                Map.of("RAW", "raw".getBytes()),
                new EditorialP5PilotRequest.PackAuthority(Map.of()),
                EditorialP5RawWireContract.SCHEMA_VERSION, "001", "", context);
    }

    private static JSONObject compactRoot(EditorialP5PilotProvider.Request request) throws Exception {
        Map<String, String> gates = new LinkedHashMap<>();
        for (String gate : EditorialSafe4Contract.GATE_IDS) gates.put(gate, "PASS");
        JSONObject finding = new JSONObject().put("itemId", "population:001")
                .put("disposition", "PROCESSED")
                .put("evidenceRefs", List.of("evidence:raw"))
                .put("modelDeclaredPass", true);
        return new JSONObject()
                .put("wireSchemaVersion", EditorialP5RawWireContract.SCHEMA_VERSION)
                .put("attemptIdentity", request.attemptIdentity())
                .put("requestEnvelopeHash", request.requestEnvelopeHash())
                .put("findings", List.of(finding))
                .put("gateObservations", new JSONObject(gates))
                .put("evidenceRefs", List.of("evidence:raw"))
                .put("preservedInventory", List.of())
                .put("declaredChanges", List.of())
                .put("disposition", new JSONObject().put("disposition", "CONTINUE")
                        .put("reasonCode", "LOCAL_REVIEW").put("phase", "L1")
                        .put("blockingGate", "COVERAGE").put("evidenceRefs", List.of())
                        .put("affectedScope", "NONE").put("recoveryAction", "NO_ACTION")
                        .put("resumeFrom", "L1").put("stopClass", "NONE")
                        .put("retryable", false))
                .put("modelDeclaredPass", true);
    }
}
