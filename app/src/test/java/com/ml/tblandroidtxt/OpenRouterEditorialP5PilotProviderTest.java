package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.json.JSONObject;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
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

        var output = OpenRouterEditorialP5PilotProvider.parseOutput(root.toString(), request);
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
                "safe4.full.report-l1.v1", "001", "", context);
    }
}
