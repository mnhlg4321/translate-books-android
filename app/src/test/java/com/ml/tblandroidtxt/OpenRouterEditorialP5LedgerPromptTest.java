package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialL1Ledger;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialRawInventory;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The ledger-contract prompt and response format. Text is synthetic. */
public final class OpenRouterEditorialP5LedgerPromptTest {
    private static final String RAW = "王は城に入った。\n\n「踏破した。」\n";
    private static final String DRAFT = "Vua vao thanh.\n\n\"Da chinh phuc.\"\n";

    private static EditorialP5PilotProvider.Request request(String phase, String schema, boolean withCandidates) {
        Map<String, byte[]> sources = new LinkedHashMap<>();
        sources.put(EditorialSafe4Contract.RAW, RAW.getBytes(StandardCharsets.UTF_8));
        sources.put(EditorialSafe4Contract.GLOSSARY, "source,target\n".getBytes(StandardCharsets.UTF_8));
        if ("L1_RECONCILE".equals(phase)) {
            sources.put(EditorialSafe4Contract.DRAFT, DRAFT.getBytes(StandardCharsets.UTF_8));
            sources.put(EditorialSafe4Contract.PRONOUN, "from,speaker,target\n".getBytes(StandardCharsets.UTF_8));
            if (withCandidates) {
                sources.put("L1_RAW_CANDIDATES", "candidateId\tledger\tunitId\tnote\nc1\tTG\tu:3:aaaaaaaa\tcontrast\n"
                        .getBytes(StandardCharsets.UTF_8));
            }
        }
        EditorialP5PilotProvider.Request.Context context = new EditorialP5PilotProvider.Request.Context(
                "binding", "run", "manifest", "bundle", "predecessor", List.of("chapter:001"), List.of("population:001"));
        return new EditorialP5PilotProvider.Request("a".repeat(64), EditorialP5PilotProvider.CallKind.PRIMARY_SEMANTIC,
                "openrouter", "model", phase, "e".repeat(64), sources,
                new EditorialP5PilotRequest.PackAuthority(Map.of()), schema, "001", "", context);
    }

    @Test public void rawPromptNumbersUnitsWithAppIdsAndStatesTheTask() {
        PromptPair prompt = OpenRouterEditorialP5PilotProvider.buildLedgerPrompt(
                request("L1_RAW_DISCOVERY", EditorialL1Ledger.RAW_WIRE, false));
        EditorialRawInventory.Inventory inv = EditorialRawInventory.build(RAW.getBytes(StandardCharsets.UTF_8));
        assertEquals(2, inv.units().size());
        assertTrue(prompt.user.contains(inv.units().get(0).id() + "|王は城に入った。"));
        assertTrue(prompt.user.contains(inv.units().get(1).id() + "|「踏破した。」"));
        assertTrue(prompt.user.contains("TASK (L1 RAW discovery)"));
        assertTrue(prompt.user.contains(EditorialL1Ledger.RAW_WIRE));
        assertTrue(prompt.user.contains("a".repeat(64)));
        assertFalse(prompt.user.contains("TASK (L1 RECONCILE)"));
        // a blank line has no unit id and is not shown as a unit
        assertFalse(prompt.user.contains("u:2:"));
    }

    @Test public void reconcilePromptShowsNumberedDraftCandidatesAndNoCap() {
        PromptPair prompt = OpenRouterEditorialP5PilotProvider.buildLedgerPrompt(
                request("L1_RECONCILE", EditorialL1Ledger.RECONCILE_WIRE, true));
        assertTrue(prompt.user.contains("D1|Vua vao thanh."));
        assertTrue(prompt.user.contains("D2|"));
        assertTrue(prompt.user.contains("D3|\"Da chinh phuc.\""));
        assertTrue(prompt.user.contains("--- L1_RAW_CANDIDATES ---"));
        assertTrue(prompt.user.contains("c1\tTG\tu:3:aaaaaaaa"));
        assertTrue(prompt.user.contains("there is no cap of four"));
        assertTrue(prompt.user.contains("findings <=" + EditorialL1Ledger.MAX_FINDINGS_PER_CALL));
        assertTrue(prompt.user.contains("TASK (L1 RECONCILE)"));
    }

    @Test public void responseFormatIsStrictAndEveryKeyIsRequired() throws Exception {
        for (String schema : new String[] {EditorialL1Ledger.RAW_WIRE, EditorialL1Ledger.RECONCILE_WIRE}) {
            JSONObject format = OpenRouterEditorialP5PilotProvider.ledgerResponseFormat(schema);
            assertEquals("json_schema", format.getString("type"));
            JSONObject inner = format.getJSONObject("json_schema");
            assertTrue(inner.getBoolean("strict"));
            JSONObject root = inner.getJSONObject("schema");
            assertFalse(root.getBoolean("additionalProperties"));
            assertEquals(root.getJSONObject("properties").length(), root.getJSONArray("required").length());
        }
        assertEquals(EditorialL1Ledger.RAW_SCHEMA_NAME, OpenRouterEditorialP5PilotProvider
                .ledgerResponseFormat(EditorialL1Ledger.RAW_WIRE).getJSONObject("json_schema").getString("name"));
    }

    @Test public void onlyTheTwoLedgerSchemasSelectTheLedgerPath() {
        assertTrue(OpenRouterEditorialP5PilotProvider.isLedgerSchema(EditorialL1Ledger.RAW_WIRE));
        assertTrue(OpenRouterEditorialP5PilotProvider.isLedgerSchema(EditorialL1Ledger.RECONCILE_WIRE));
        assertFalse(OpenRouterEditorialP5PilotProvider.isLedgerSchema("safe4.raw.discovery.wire.v1"));
        assertFalse(OpenRouterEditorialP5PilotProvider.isLedgerSchema(""));
    }
}
