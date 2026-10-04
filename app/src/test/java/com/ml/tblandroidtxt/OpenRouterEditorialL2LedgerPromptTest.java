package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialFinalRead;
import com.ml.tblandroidtxt.editorial.pack.EditorialFieldSpec;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** L2 prompts of the ledger contract (edit v2 and final read). Synthetic text only. */
public final class OpenRouterEditorialL2LedgerPromptTest {
    private static final String ATTEMPT = "a".repeat(64);

    private static byte[] b(String value) { return value.getBytes(StandardCharsets.UTF_8); }

    private static EditorialL2Execution.Provider.Request request(String phase, String schema, Map<String, byte[]> sources) {
        Map<EditorialPackFileRole, byte[]> authority = new LinkedHashMap<>();
        authority.put(EditorialPackFileRole.PROJECT_INSTRUCTION, b("PROJECT-AUTH-TEXT"));
        authority.put(EditorialPackFileRole.TURN_PROMPT, b("TURN-AUTH-TEXT"));
        authority.put(EditorialPackFileRole.WORKFLOW, b("WORKFLOW-AUTH-TEXT"));
        return new EditorialL2Execution.Provider.Request(ATTEMPT, phase, schema, sources,
                new EditorialP5PilotRequest.PackAuthority(authority), "001", 4000, 60_000L);
    }

    private static Map<String, byte[]> editSources() {
        Map<String, byte[]> map = new LinkedHashMap<>();
        map.put("REPORT_L1", b("{\"findings\":[]}"));
        map.put(EditorialSafe4Contract.RAW, b("王は城に入った。\n騎士が言った。"));
        map.put(EditorialSafe4Contract.GLOSSARY, b("term,target"));
        map.put(EditorialSafe4Contract.DRAFT, b("Vua vao thanh.\nHiep si noi.\n"));
        map.put(EditorialL2Execution.CANDIDATES_BLOCK, b("{\"candidates\":[]}"));
        return map;
    }

    private static Map<String, byte[]> readSources() {
        Map<String, byte[]> map = new LinkedHashMap<>();
        byte[] target = b("Dong mot\nDong hai\nDong ba\nDong bon\n");
        map.put(EditorialSafe4Contract.RAW, b("一\n二\n三\n四"));
        map.put(EditorialSafe4Contract.GLOSSARY, b("term,target"));
        map.put(EditorialFinalRead.TARGET_ROLE, target);
        map.put(EditorialFinalRead.PROBES_ROLE, EditorialFinalRead.probeBlock(target));
        return map;
    }

    @Test public void v2EditRequestIsAcceptedAndCarriesTheLedgerRulesAndTheV2Example() {
        EditorialL2Execution.Provider.Request request = request(EditorialL2Execution.PHASE,
                EditorialL2Execution.WIRE_SCHEMA_VERSION_V3, editSources());
        assertTrue(OpenRouterEditorialL2Provider.validRequest(request));
        PromptPair prompt = OpenRouterEditorialL2Provider.buildPrompt(request);
        assertTrue(prompt.system.contains("findingResolutions must contain EXACTLY ONE row per finding errorId"));
        assertTrue(prompt.system.contains("INSERT_AFTER"));
        assertTrue(prompt.system.contains("MERGE_WITH_NEXT"));
        assertTrue(prompt.system.contains("MUST changes[].reason length 1.." + EditorialFieldSpec.MAX_MODEL_TEXT));
        assertTrue(prompt.system.contains("MAY omit changes[].op; when present its value MUST ∈ {DELETE|INSERT_AFTER|MERGE_WITH_NEXT|REPLACE}"));
        assertTrue(prompt.system.contains("MAY omit changes[].before"));
        assertTrue(prompt.system.contains("mismatch is a warning"));
        assertFalse(prompt.user.contains("\"line\":1,\"before\":\"...\",\"after\":\"...\""));
        assertTrue(prompt.system.contains("occurrences must contain one {unitId, ref} for EVERY unit"));
        assertTrue(prompt.user.contains(EditorialL2Execution.WIRE_SCHEMA_VERSION_V3));
        assertTrue(prompt.user.contains("\"findingResolutions\""));
        assertTrue(prompt.user.contains("L1|Vua vao thanh."));
        // the legacy request is unchanged: no ledger rules, v1 example
        PromptPair legacy = OpenRouterEditorialL2Provider.buildPrompt(request(EditorialL2Execution.PHASE,
                EditorialL2Execution.WIRE_SCHEMA_VERSION, editSources()));
        assertFalse(legacy.system.contains("findingResolutions"));
        assertFalse(legacy.user.contains("findingResolutions"));
        assertTrue(legacy.user.contains(EditorialL2Execution.WIRE_SCHEMA_VERSION));
        assertTrue(legacy.user.contains("\"line\":1,\"before\":\"...\""));
    }

    @Test public void finalReadRequestNeedsTheTargetTheProbesAndRawAndShowsNumberedLines() {
        EditorialL2Execution.Provider.Request request = request(EditorialFinalRead.L2_PHASE, EditorialFinalRead.WIRE, readSources());
        assertTrue(OpenRouterEditorialL2Provider.validRequest(request));
        PromptPair prompt = OpenRouterEditorialL2Provider.buildPrompt(request);
        assertTrue(prompt.user.contains("L1|Dong mot"));
        assertTrue(prompt.user.contains("L4|Dong bon"));
        assertTrue(prompt.user.contains("targetSha256"));
        assertTrue(prompt.user.contains(EditorialFinalRead.WIRE));
        assertTrue(prompt.system.contains("last " + EditorialFinalRead.TAIL_LENGTH + " characters"));
        assertTrue(prompt.system.contains("MUST readSha256 length 64..64"));
        assertTrue(prompt.system.contains("MUST probeTails[].tail length 1.." + EditorialFieldSpec.MAX_FINAL_READ_TAIL));
        Map<String, byte[]> noProbes = readSources();
        noProbes.remove(EditorialFinalRead.PROBES_ROLE);
        assertFalse(OpenRouterEditorialL2Provider.validRequest(request(EditorialFinalRead.L2_PHASE, EditorialFinalRead.WIRE, noProbes)));
        Map<String, byte[]> noTarget = readSources();
        noTarget.remove(EditorialFinalRead.TARGET_ROLE);
        assertFalse(OpenRouterEditorialL2Provider.validRequest(request(EditorialFinalRead.L2_PHASE, EditorialFinalRead.WIRE, noTarget)));
        // a read must not be dispatched under the edit schema, nor an edit under the read schema
        assertFalse(OpenRouterEditorialL2Provider.validRequest(request(EditorialFinalRead.L2_PHASE,
                EditorialL2Execution.WIRE_SCHEMA_VERSION_V3, readSources())));
        assertFalse(OpenRouterEditorialL2Provider.validRequest(request(EditorialL2Execution.PHASE,
                EditorialFinalRead.WIRE, editSources())));
    }
}
