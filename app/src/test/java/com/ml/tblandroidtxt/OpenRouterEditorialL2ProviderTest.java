package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialChangeMapReconstructor;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** JVM-only: nothing here may reach a transport. */
public final class OpenRouterEditorialL2ProviderTest {
    private static final String ATTEMPT = "a".repeat(64);
    private static final String PREDECESSOR = "b".repeat(64);

    private static AppSettings routeSettings(String apiKey) {
        AppSettings settings = new AppSettings();
        settings.provider = EditorialP5EFreshRawRoutingPolicy.PROVIDER;
        settings.model = EditorialP5EFreshRawRoutingPolicy.MODEL;
        settings.baseUrl = AppSettings.defaultBaseUrl(EditorialP5EFreshRawRoutingPolicy.PROVIDER);
        settings.apiKey = apiKey;
        return settings;
    }

    private static byte[] b(String value) { return value.getBytes(StandardCharsets.UTF_8); }

    private static EditorialL2Execution.Provider.Request request(String phase, String schema,
                                                                  Map<String, byte[]> sources) {
        Map<EditorialPackFileRole, byte[]> authority = new LinkedHashMap<>();
        authority.put(EditorialPackFileRole.PROJECT_INSTRUCTION, b("PROJECT-AUTH-TEXT"));
        authority.put(EditorialPackFileRole.TURN_PROMPT, b("TURN-AUTH-TEXT"));
        authority.put(EditorialPackFileRole.WORKFLOW, b("WORKFLOW-AUTH-TEXT"));
        return new EditorialL2Execution.Provider.Request(ATTEMPT, phase, schema, sources,
                new EditorialP5PilotRequest.PackAuthority(authority), "001", 4000, 60_000L);
    }

    private static EditorialL2Execution.Provider.Request validRequest(Map<String, byte[]> sources) {
        return request(EditorialL2Execution.PHASE, EditorialL2Execution.WIRE_SCHEMA_VERSION, sources);
    }

    private static Map<String, byte[]> sources() {
        Map<String, byte[]> map = new LinkedHashMap<>();
        // Deliberately unsorted insertion order.
        map.put("REPORT_L1", b("report l1 body"));
        map.put(EditorialSafe4Contract.RAW, b("raw line one\nraw line two"));
        map.put(EditorialSafe4Contract.GLOSSARY, b("term,target"));
        map.put(EditorialSafe4Contract.DRAFT, b("dong mot\ndong hai\n"));
        map.put(EditorialL2Execution.CANDIDATES_BLOCK,
                b("{\"candidates\":[{\"candidateId\":\"U001\",\"ledger\":\"UNIT\",\"line\":1}]}"));
        return map;
    }

    private static Map<String, byte[]> discoverySources() {
        Map<String, byte[]> map = new LinkedHashMap<>();
        map.put(EditorialSafe4Contract.RAW, b("raw line one\nraw line two"));
        map.put(EditorialSafe4Contract.GLOSSARY, b("term,target"));
        return map;
    }

    private static EditorialL2Execution.Provider.Request discoveryRequest(Map<String, byte[]> sources) {
        return request(EditorialL2Execution.DISCOVERY_PHASE, EditorialL2Execution.DISCOVERY_WIRE, sources);
    }

    private static void assertThrowsMessage(String expected, AppSettings settings,
                                            EditorialL2Execution.Provider.Request request) throws Exception {
        try {
            new OpenRouterEditorialL2Provider(settings).call(request);
            throw new AssertionError("expected " + expected);
        } catch (IllegalStateException error) {
            assertEquals(expected, error.getMessage());
        }
    }

    @Test public void wrongPhaseOrSchemaIsRejectedBeforeTransport() throws Exception {
        AppSettings good = routeSettings("test-only-no-dispatch");
        assertThrowsMessage("P6_L2_REQUEST_INVALID", good,
                request("L1_RECONCILE", EditorialL2Execution.WIRE_SCHEMA_VERSION, sources()));
        assertThrowsMessage("P6_L2_REQUEST_INVALID", good,
                request(EditorialL2Execution.PHASE, "safe4.l2.edit.wire.v0", sources()));
        assertThrowsMessage("P6_L2_REQUEST_INVALID", good, null);
    }

    @Test public void editWithoutTheCandidateBlockAndImpureDiscoveryAreRejectedBeforeTransport() throws Exception {
        AppSettings good = routeSettings("test-only-no-dispatch");
        Map<String, byte[]> noBlock = sources();
        noBlock.remove(EditorialL2Execution.CANDIDATES_BLOCK);
        assertThrowsMessage("P6_L2_REQUEST_INVALID", good, validRequest(noBlock));

        // Discovery is blind: a DRAFT, REPORT_L1, PRONOUN, VI_L2 or candidate block makes the request invalid.
        for (String hidden : List.of(EditorialSafe4Contract.DRAFT, "REPORT_L1", EditorialSafe4Contract.PRONOUN, "VI_L2",
                EditorialL2Execution.CANDIDATES_BLOCK)) {
            Map<String, byte[]> leaky = discoverySources();
            leaky.put(hidden, b("hidden"));
            assertThrowsMessage("P6_L2_REQUEST_INVALID", good, discoveryRequest(leaky));
        }
        Map<String, byte[]> noRaw = discoverySources();
        noRaw.remove(EditorialSafe4Contract.RAW);
        assertThrowsMessage("P6_L2_REQUEST_INVALID", good, discoveryRequest(noRaw));
        assertThrowsMessage("P6_L2_REQUEST_INVALID", good,
                request(EditorialL2Execution.DISCOVERY_PHASE, EditorialL2Execution.WIRE_SCHEMA_VERSION, discoverySources()));
        // A valid discovery request reaches the next gate (route), not the request gate.
        AppSettings wrongModel = routeSettings("test-only-no-dispatch");
        wrongModel.model = "google/gemini-2.5-flash";
        assertThrowsMessage("P6_L2_ROUTE_SETTINGS_MISMATCH", wrongModel, discoveryRequest(discoverySources()));
    }

    @Test public void discoveryPromptShowsNumberedRawOnlyAndItsSampleParses() throws Exception {
        PromptPair prompt = OpenRouterEditorialL2Provider.buildPrompt(discoveryRequest(discoverySources()));
        assertTrue(prompt.system.contains(OpenRouterEditorialL2Provider.DISCOVERY_RULES));
        assertFalse(prompt.system.contains(OpenRouterEditorialL2Provider.WIRE_FORMAT_RULES));
        assertTrue(prompt.user.contains("--- RAW ---\nL1|raw line one\nL2|raw line two\n--- END RAW ---"));
        assertTrue(prompt.user.contains("--- GLOSSARY ---\nterm,target\n--- END GLOSSARY ---"));
        assertFalse(prompt.user.contains("--- DRAFT ---"));
        assertFalse(prompt.user.contains("--- REPORT_L1 ---"));
        assertTrue(prompt.user.contains("\"phaseActivation\":\"" + EditorialL2Execution.DISCOVERY_PHASE + "\""));

        String marker = "Return only this object:\n";
        JSONObject object = new JSONObject(prompt.user.substring(prompt.user.indexOf(marker) + marker.length()).trim());
        assertEquals(EditorialL2Execution.DISCOVERY_WIRE, object.getString("wireSchemaVersion"));
        object.put("attemptIdentity", ATTEMPT);
        object.getJSONArray("candidates").getJSONObject(0).put("ledger", "UNIT");
        Method parse = EditorialL2Execution.class.getDeclaredMethod("parseDiscovery", byte[].class, String.class, int.class);
        parse.setAccessible(true);
        assertEquals(1, ((List<?>) parse.invoke(null, b(object.toString()), ATTEMPT, 2)).size());
        try {
            parse.invoke(null, b(new JSONObject(prompt.user.substring(prompt.user.indexOf(marker) + marker.length()).trim()).toString()),
                    ATTEMPT, 2);
            throw new AssertionError("unfilled placeholders must be rejected");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            assertTrue(expected.getCause() instanceof RuntimeException);
        }
    }

    @Test public void editPromptStatesTheResolutionRules() {
        PromptPair prompt = OpenRouterEditorialL2Provider.buildPrompt(validRequest(sources()));
        assertTrue(prompt.system.contains(OpenRouterEditorialL2Provider.RESOLUTION_RULES));
        assertTrue(prompt.user.contains("--- L2_RAW_CANDIDATES ---"));
        assertTrue(prompt.user.contains("\"resolutions\":[{\"candidateId\":\"U001\""));
    }

    @Test public void routeMismatchAndBlankKeyAreRejectedBeforeTransport() throws Exception {
        AppSettings wrongModel = routeSettings("test-only-no-dispatch");
        wrongModel.model = "google/gemini-2.5-flash";
        assertThrowsMessage("P6_L2_ROUTE_SETTINGS_MISMATCH", wrongModel, validRequest(sources()));

        AppSettings wrongProvider = routeSettings("test-only-no-dispatch");
        wrongProvider.provider = "openai";
        assertThrowsMessage("P6_L2_ROUTE_SETTINGS_MISMATCH", wrongProvider, validRequest(sources()));

        assertThrowsMessage("OPENROUTER_CONFIGURATION_INCOMPLETE", routeSettings(""), validRequest(sources()));
        assertThrowsMessage("OPENROUTER_CONFIGURATION_INCOMPLETE", routeSettings("   "), validRequest(sources()));
        assertThrowsMessage("OPENROUTER_CONFIGURATION_INCOMPLETE", routeSettings(null), validRequest(sources()));
    }

    @Test public void promptNumbersOnlyDraftAndSortsRoles() {
        PromptPair prompt = OpenRouterEditorialL2Provider.buildPrompt(validRequest(sources()));
        String user = prompt.user;
        assertTrue(user, user.contains("--- DRAFT ---\nL1|dong mot\nL2|dong hai\nL3|\n--- END DRAFT ---"));
        assertTrue(user.contains("--- RAW ---\nL1|raw line one\nL2|raw line two\n--- END RAW ---"));
        assertTrue(user.contains("--- GLOSSARY ---\nterm,target\n--- END GLOSSARY ---"));
        assertTrue(user.contains("--- REPORT_L1 ---\nreport l1 body\n--- END REPORT_L1 ---"));
        assertFalse(user.contains("L1|report"));
        assertFalse(user.contains("L1|term"));

        int draft = user.indexOf("--- DRAFT ---");
        int glossary = user.indexOf("--- GLOSSARY ---");
        int raw = user.indexOf("--- RAW ---");
        int report = user.indexOf("--- REPORT_L1 ---");
        assertTrue(draft >= 0 && draft < glossary && glossary < raw && raw < report);
        assertFalse(user.contains("VI_L2"));
    }

    @Test public void envelopeAndSystemPromptCarryIdentityWireAndAuthority() {
        PromptPair prompt = OpenRouterEditorialL2Provider.buildPrompt(validRequest(sources()));
        assertTrue(prompt.user.contains("\"attemptIdentity\":\"" + ATTEMPT + "\""));
        assertTrue(prompt.user.contains("\"wireSchemaVersion\":\"" + EditorialL2Execution.WIRE_SCHEMA_VERSION + "\""));
        assertTrue(prompt.user.contains("\"phaseActivation\":\"" + EditorialL2Execution.PHASE + "\""));
        assertTrue(prompt.system.contains(OpenRouterEditorialL2Provider.WIRE_FORMAT_RULES));
        assertTrue(prompt.system.contains("[PROJECT_INSTRUCTION]\nPROJECT-AUTH-TEXT\n[/PROJECT_INSTRUCTION]"));
        assertTrue(prompt.system.contains("[TURN_PROMPT]\nTURN-AUTH-TEXT\n[/TURN_PROMPT]"));
        assertTrue(prompt.system.contains("[WORKFLOW]\nWORKFLOW-AUTH-TEXT\n[/WORKFLOW]"));
    }

    @Test public void hiddenRoleNeverAppearsUnlessSuppliedAsVisible() {
        Map<String, byte[]> map = sources();
        PromptPair prompt = OpenRouterEditorialL2Provider.buildPrompt(validRequest(map));
        assertFalse(prompt.user.contains("--- VI_L2 ---"));
        assertFalse(prompt.system.contains("VI_L2 body"));
        // The adapter renders exactly the supplied roles and nothing else.
        int blocks = 0;
        Matcher m = Pattern.compile("--- ([A-Z0-9_]+) ---\n").matcher(prompt.user);
        while (m.find()) blocks++;
        assertEquals(map.size(), blocks);
    }

    private static final String[] DRAFTS = {
            "one\ntwo\nthree\n",
            "one\r\ntwo\r\nthree\r\n",
            "one\ntwo\nunterminated last",
            "one\r\ntwo\r\nunterminated last",
            "first\n\nthird after empty\n",
            "\n\nonly empties after\n",
            "Xin ch\u00e0o, \u0111\u00e2y l\u00e0 d\u00f2ng ti\u1ebfng Vi\u1ec7t\n"
                    + "\u201cAnh \u0111\u1ebfn mu\u1ed9n\u201d, c\u00f4 n\u00f3i.\r\n"
                    + "\n"
                    + "K\u1ebft th\u00fac kh\u00f4ng c\u00f3 xu\u1ed1ng d\u00f2ng",
            "single",
            ""
    };

    @Test public void numberedLinesMatchTheAppReconstructorForEveryDraftShape() {
        for (String draft : DRAFTS) {
            String numbered = OpenRouterEditorialL2Provider.numbered(draft);
            String[] shown = numbered.split("\n", -1);
            for (String entry : shown) {
                Matcher m = Pattern.compile("^L(\\d+)\\|(.*)$", Pattern.DOTALL).matcher(entry);
                assertTrue(entry, m.matches());
                int n = Integer.parseInt(m.group(1));
                String text = m.group(2);
                EditorialChangeMapReconstructor.ChangeRow row = new EditorialChangeMapReconstructor.ChangeRow(
                        "C001", "E001", n, text, text + " edited", "fix wording", false, null,
                        EditorialChangeMapReconstructor.DeclaredStatus.CLOSED);
                EditorialChangeMapReconstructor.Result result = new EditorialChangeMapReconstructor()
                        .reconstruct(new EditorialChangeMapReconstructor.Request(
                                EditorialChangeMapReconstructor.Kind.L2_DRAFT_TO_VI_L2, b(draft),
                                PREDECESSOR, List.of(row), List.of(), Set.of()));
                String label = "draft=" + draft.replace("\r", "\\r").replace("\n", "\\n") + " line=" + n
                        + " issues=" + result.issues();
                assertTrue(label, result.accepted());
                assertEquals(label, List.of(n), result.actualChangedLines());
            }
        }
    }

    @Test public void numberedLineCountEqualsAppLineCount() {
        // L<n> labels must be a contiguous 1..N run with no extra or missing line.
        for (String draft : DRAFTS) {
            String[] shown = OpenRouterEditorialL2Provider.numbered(draft).split("\n", -1);
            for (int i = 0; i < shown.length; i++) {
                assertTrue(shown[i], shown[i].startsWith("L" + (i + 1) + "|"));
            }
        }
    }

    @Test public void promptSampleObjectParsesThroughTheStrictWireParser() throws Exception {
        PromptPair prompt = OpenRouterEditorialL2Provider.buildPrompt(validRequest(sources()));
        String marker = "Return only this object:\n";
        String sample = prompt.user.substring(prompt.user.indexOf(marker) + marker.length()).trim();
        JSONObject object = new JSONObject(sample); // the template itself is valid JSON
        assertEquals(EditorialL2Execution.WIRE_SCHEMA_VERSION, object.getString("wireSchemaVersion"));

        object.put("attemptIdentity", ATTEMPT);
        object.getJSONArray("resolutions").getJSONObject(0).put("status", "PROCESSED");
        JSONObject change = object.getJSONArray("changes").getJSONObject(0);
        change.put("before", "dong mot").put("after", "dong mot sua").put("reason", "fix wording");
        JSONObject preserved = object.getJSONArray("preserved").getJSONObject(0);
        preserved.put("line", 2).put("before", "dong hai").put("evidenceLimit", "no proof");
        object.getJSONObject("disposition").put("disposition", "CONTINUE")
                .put("reasonCode", "OK").put("stopClass", "NONE");

        List<EditorialL2Execution.Candidate> known = List.of(new EditorialL2Execution.Candidate("U001", "UNIT", 1));
        Method parse = EditorialL2Execution.class.getDeclaredMethod("parseEditWire", byte[].class, String.class, List.class);
        parse.setAccessible(true);
        Object editWire = parse.invoke(null, b(object.toString()), ATTEMPT, known);
        Method rows = editWire.getClass().getDeclaredMethod("rows");
        rows.setAccessible(true);
        Object wire = rows.invoke(editWire);
        Method changes = wire.getClass().getDeclaredMethod("changes");
        changes.setAccessible(true);
        assertEquals(1, ((List<?>) changes.invoke(wire)).size());
        Method resolutions = editWire.getClass().getDeclaredMethod("resolutions");
        resolutions.setAccessible(true);
        assertEquals(Map.of("U001", "PROCESSED"), resolutions.invoke(editWire));

        // Same sample with the unsubstituted placeholders must NOT pass (the model has to fill it in).
        JSONObject raw = new JSONObject(sample);
        try {
            parse.invoke(null, b(raw.toString()), ATTEMPT, known);
            throw new AssertionError("unfilled placeholders must be rejected");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            assertTrue(expected.getCause() instanceof RuntimeException);
        }
    }

    @Test public void generatedWiresAreSentWithAStrictSchemaAndLegacyWiresKeepJsonObject() throws Exception {
        JSONObject strict = OpenRouterEditorialL2Provider.responseFormat(
                request(EditorialL2Execution.PHASE, EditorialL2Execution.WIRE_SCHEMA_VERSION_V3, sources()));
        assertEquals("json_schema", strict.getString("type"));
        JSONObject schema = strict.getJSONObject("json_schema");
        assertEquals("safe4_l2_edit_strict", schema.getString("name"));
        assertTrue(schema.getBoolean("strict"));
        JSONObject root = schema.getJSONObject("schema");
        assertFalse(root.getBoolean("additionalProperties"));
        assertTrue(root.getJSONObject("properties").has("findingResolutions"));
        JSONObject legacy = OpenRouterEditorialL2Provider.responseFormat(
                request(EditorialL2Execution.PHASE, EditorialL2Execution.WIRE_SCHEMA_VERSION, sources()));
        assertEquals("json_object", legacy.getString("type"));
        assertFalse(legacy.has("json_schema"));
    }
}
