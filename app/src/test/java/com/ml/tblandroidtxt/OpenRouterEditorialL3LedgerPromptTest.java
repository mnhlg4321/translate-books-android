package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialFinalRead;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialL3Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;
import com.ml.tblandroidtxt.editorial.pack.EditorialRawInventory;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.junit.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** L3 and L2-discovery prompts of the ledger contract. Synthetic text only. */
public final class OpenRouterEditorialL3LedgerPromptTest {
    private static final String ATTEMPT = "a".repeat(64);
    private static final String RAW = "王は城に入った。\n騎士が言った。";

    private static byte[] b(String value) { return value.getBytes(StandardCharsets.UTF_8); }

    private static EditorialL2Execution.Provider.Request request(String phase, String schema, Map<String, byte[]> sources) {
        Map<EditorialPackFileRole, byte[]> authority = new LinkedHashMap<>();
        authority.put(EditorialPackFileRole.PROJECT_INSTRUCTION, b("PROJECT-AUTH-TEXT"));
        authority.put(EditorialPackFileRole.TURN_PROMPT, b("TURN-AUTH-TEXT"));
        authority.put(EditorialPackFileRole.WORKFLOW, b("WORKFLOW-AUTH-TEXT"));
        return new EditorialL2Execution.Provider.Request(ATTEMPT, phase, schema, sources,
                new EditorialP5PilotRequest.PackAuthority(authority), "001", 4000, 60_000L);
    }

    private static boolean valid(EditorialL2Execution.Provider.Request request) throws Exception {
        Method m = OpenRouterEditorialL3Provider.class.getDeclaredMethod("validPhase", EditorialL2Execution.Provider.Request.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(null, request);
    }

    private static Map<String, byte[]> reauditSources() {
        Map<String, byte[]> map = new LinkedHashMap<>();
        map.put(EditorialSafe4Contract.RAW, b(RAW));
        map.put(EditorialSafe4Contract.GLOSSARY, b("term,target"));
        map.put("VI_L2", b("Vua vao thanh.\nHiep si noi."));
        return map;
    }

    @Test public void reauditV2ShowsUnitIdsAndNumberedViL2AndAsksForCoverageRanges() throws Exception {
        EditorialL2Execution.Provider.Request request = request(EditorialL3Execution.REAUDIT_PHASE,
                EditorialL3Execution.REAUDIT_WIRE_V3, reauditSources());
        assertTrue(valid(request));
        PromptPair prompt = OpenRouterEditorialL3Provider.buildPrompt(request);
        EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(b(RAW));
        assertTrue(prompt.user.contains("L" + inventory.units().get(0).line() + "|王は城に入った。"));
        assertTrue(prompt.user.contains("L1|Vua vao thanh."));
        assertTrue(prompt.system.contains("coverage: ordered, contiguous, non-overlapping ranges"));
        assertTrue(prompt.user.contains(EditorialL3Execution.REAUDIT_WIRE_V3));
        assertTrue(prompt.user.contains("\"viLine\""));
    }

    @Test public void reconcileV2AsksForAnchoredProbesCarriedResolutionsAndOperations() throws Exception {
        Map<String, byte[]> sources = reauditSources();
        sources.put(EditorialSafe4Contract.DRAFT, b("Vua vao.\nHiep si."));
        sources.put("REPORT_L1", b("{}"));
        sources.put("CHANGE_MAP_L2", b("{}"));
        sources.put(OpenRouterEditorialL3Provider.CANDIDATES_ROLE, b("{\"candidates\":[]}"));
        sources.put(OpenRouterEditorialL3Provider.CARRIED_ROLE, b("{\"defects\":[]}"));
        EditorialL2Execution.Provider.Request request = request(EditorialL3Execution.RECONCILE_PHASE,
                EditorialL3Execution.RECONCILE_WIRE_V3, sources);
        assertTrue(valid(request));
        PromptPair prompt = OpenRouterEditorialL3Provider.buildPrompt(request);
        assertTrue(prompt.system.contains("carriedResolutions has EXACTLY one row per entry"));
        assertTrue(prompt.system.contains("probes: at least 3 COVERAGE and 3 REGRESSION, each anchored"));
        assertTrue(prompt.system.contains("MERGE_WITH_NEXT"));
        assertTrue(prompt.user.contains("\"probes\""));
        assertTrue(prompt.user.contains("\"carriedResolutions\""));
        assertFalse(prompt.user.contains("adversarialCoverage"));
        // the legacy reconcile request still uses the legacy example
        PromptPair legacy = OpenRouterEditorialL3Provider.buildPrompt(request(EditorialL3Execution.RECONCILE_PHASE,
                EditorialL3Execution.RECONCILE_WIRE, sources));
        assertTrue(legacy.user.contains("adversarialCoverage"));
        assertFalse(legacy.user.contains("carriedResolutions"));
    }

    @Test public void finalReadOfFinalIsAcceptedOnlyWithTargetProbesAndRaw() throws Exception {
        byte[] target = b("Dong mot\nDong hai\nDong ba\nDong bon\n");
        Map<String, byte[]> sources = new LinkedHashMap<>();
        sources.put(EditorialSafe4Contract.RAW, b("一\n二\n三\n四"));
        sources.put(EditorialFinalRead.TARGET_ROLE, target);
        sources.put(EditorialFinalRead.PROBES_ROLE, EditorialFinalRead.probeBlock(target));
        EditorialL2Execution.Provider.Request request = request(EditorialFinalRead.L3_PHASE, EditorialFinalRead.WIRE, sources);
        assertTrue(valid(request));
        PromptPair prompt = OpenRouterEditorialL3Provider.buildPrompt(request);
        assertTrue(prompt.user.contains("L1|Dong mot"));
        assertTrue(prompt.system.contains("READ_TARGET is the exact text the app built"));
        sources.remove(EditorialFinalRead.PROBES_ROLE);
        assertFalse(valid(request(EditorialFinalRead.L3_PHASE, EditorialFinalRead.WIRE, sources)));
        // the L2 read phase is not an L3 phase
        Map<String, byte[]> again = new LinkedHashMap<>();
        again.put(EditorialSafe4Contract.RAW, b("一"));
        again.put(EditorialFinalRead.TARGET_ROLE, target);
        again.put(EditorialFinalRead.PROBES_ROLE, EditorialFinalRead.probeBlock(target));
        assertFalse(valid(request(EditorialFinalRead.L2_PHASE, EditorialFinalRead.WIRE, again)));
    }

    @Test public void l2DiscoveryV2ShowsUnitIdsAndTheCoverageExample() {
        Map<String, byte[]> sources = new LinkedHashMap<>();
        sources.put(EditorialSafe4Contract.RAW, b(RAW));
        sources.put(EditorialSafe4Contract.GLOSSARY, b("term,target"));
        EditorialL2Execution.Provider.Request request = request(EditorialL2Execution.DISCOVERY_PHASE,
                EditorialL2Execution.DISCOVERY_WIRE_V3, sources);
        assertTrue(OpenRouterEditorialL2Provider.validRequest(request));
        PromptPair prompt = OpenRouterEditorialL2Provider.buildPrompt(request);
        EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(b(RAW));
        assertTrue(prompt.user.contains("L" + inventory.units().get(1).line() + "|騎士が言った。"));
        assertTrue(prompt.user.contains("\"coverage\""));
        assertTrue(prompt.system.contains("candidates are sparse"));
        assertTrue(prompt.user.contains("L1|王は"));
        assertFalse(prompt.user.matches("(?s).*u:[0-9]+:[0-9a-f]{8}.*"));
    }
}
