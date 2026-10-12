package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialL3Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** JVM-only: nothing here may reach a transport. */
public final class OpenRouterEditorialL3ProviderTest {
    private static final String ATTEMPT = "c".repeat(64);
    private static final String VI_L2 = "dong mot\ndong hai";
    private static final String CANDIDATES_JSON =
            "{\"candidates\":[{\"candidateId\":\"U001\",\"ledger\":\"UNIT\",\"line\":1,\"status\":\"PROCESSED\"}]}";
    private static final String MARKER = "Return only this object:\n";

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

    private static Map<String, byte[]> reauditSources() {
        Map<String, byte[]> map = new LinkedHashMap<>();
        map.put("VI_L2", b(VI_L2));
        map.put(EditorialSafe4Contract.RAW, b("raw line one\nraw line two"));
        map.put(EditorialSafe4Contract.GLOSSARY, b("source,target\nterm,thuat ngu"));
        return map;
    }

    private static Map<String, byte[]> reconcileSources() {
        Map<String, byte[]> map = reauditSources();
        map.put(EditorialSafe4Contract.DRAFT, b("draft one\ndraft two"));
        map.put("REPORT_L1", b("report l1 body"));
        map.put("CHANGE_MAP_L2", b("{\"changes\":[]}"));
        map.put("L3_REAUDIT_CANDIDATES", b(CANDIDATES_JSON));
        return map;
    }

    private static EditorialL2Execution.Provider.Request reaudit() {
        return request(EditorialL3Execution.REAUDIT_PHASE, EditorialL3Execution.REAUDIT_WIRE, reauditSources());
    }

    private static EditorialL2Execution.Provider.Request reconcile() {
        return request(EditorialL3Execution.RECONCILE_PHASE, EditorialL3Execution.RECONCILE_WIRE, reconcileSources());
    }

    private static void assertThrowsMessage(String expected, AppSettings settings,
                                            EditorialL2Execution.Provider.Request request) throws Exception {
        try {
            new OpenRouterEditorialL3Provider(settings).call(request);
            throw new AssertionError("expected " + expected);
        } catch (IllegalStateException error) {
            assertEquals(expected, error.getMessage());
        }
    }

    @Test public void invalidRequestsAreRejectedBeforeTransport() throws Exception {
        AppSettings good = routeSettings("test-only-no-dispatch");
        // wrong phase (L2 phase)
        assertThrowsMessage("P6_L3_REQUEST_INVALID", good,
                request(EditorialL2Execution.PHASE, EditorialL2Execution.WIRE_SCHEMA_VERSION, reauditSources()));
        // re-audit with the wrong schema
        assertThrowsMessage("P6_L3_REQUEST_INVALID", good,
                request(EditorialL3Execution.REAUDIT_PHASE, EditorialL3Execution.RECONCILE_WIRE, reauditSources()));
        // reconcile with the wrong schema
        assertThrowsMessage("P6_L3_REQUEST_INVALID", good,
                request(EditorialL3Execution.RECONCILE_PHASE, EditorialL3Execution.REAUDIT_WIRE, reconcileSources()));
        // re-audit that carries the candidate list
        assertThrowsMessage("P6_L3_REQUEST_INVALID", good,
                request(EditorialL3Execution.REAUDIT_PHASE, EditorialL3Execution.REAUDIT_WIRE, reconcileSources()));
        // reconcile without the candidate list
        assertThrowsMessage("P6_L3_REQUEST_INVALID", good,
                request(EditorialL3Execution.RECONCILE_PHASE, EditorialL3Execution.RECONCILE_WIRE, reauditSources()));
        assertThrowsMessage("P6_L3_REQUEST_INVALID", good, null);
    }

    @Test public void routeMismatchAndBlankKeyAreRejectedBeforeTransport() throws Exception {
        AppSettings wrongModel = routeSettings("test-only-no-dispatch");
        wrongModel.model = "google/gemini-2.5-flash";
        assertThrowsMessage("P6_L3_ROUTE_SETTINGS_MISMATCH", wrongModel, reaudit());
        assertThrowsMessage("P6_L3_ROUTE_SETTINGS_MISMATCH", wrongModel, reconcile());

        AppSettings wrongProvider = routeSettings("test-only-no-dispatch");
        wrongProvider.provider = "openai";
        assertThrowsMessage("P6_L3_ROUTE_SETTINGS_MISMATCH", wrongProvider, reaudit());

        assertThrowsMessage("OPENROUTER_CONFIGURATION_INCOMPLETE", routeSettings(""), reaudit());
        assertThrowsMessage("OPENROUTER_CONFIGURATION_INCOMPLETE", routeSettings("   "), reconcile());
        assertThrowsMessage("OPENROUTER_CONFIGURATION_INCOMPLETE", routeSettings(null), reaudit());
    }

    @Test public void reauditPromptShowsOnlyBlindRolesAndNumbersViL2() {
        PromptPair prompt = OpenRouterEditorialL3Provider.buildPrompt(reaudit());
        assertTrue(prompt.system.contains(OpenRouterEditorialL3Provider.REAUDIT_RULES));
        assertFalse(prompt.system.contains(OpenRouterEditorialL3Provider.RECONCILE_RULES));
        String user = prompt.user;
        assertTrue(user, user.contains("--- VI_L2 ---\nL1|dong mot\nL2|dong hai\n--- END VI_L2 ---"));
        assertTrue(user.contains("--- RAW ---\nraw line one\nraw line two\n--- END RAW ---"));
        assertTrue(user.contains("--- GLOSSARY ---\nsource,target\nterm,thuat ngu\n--- END GLOSSARY ---"));
        assertFalse(user.contains("L1|raw"));
        assertFalse(user.contains("L1|source"));
        assertFalse(user.contains("--- DRAFT ---"));
        assertFalse(user.contains("--- REPORT_L1 ---"));
        assertFalse(user.contains("--- CHANGE_MAP_L2 ---"));
        assertFalse(user.contains("--- L3_REAUDIT_CANDIDATES ---"));
        assertTrue(user.contains("\"attemptIdentity\":\"" + ATTEMPT + "\""));
        assertTrue(user.contains("\"wireSchemaVersion\":\"" + EditorialL3Execution.REAUDIT_WIRE + "\""));
        assertTrue(user.contains("\"phaseActivation\":\"" + EditorialL3Execution.REAUDIT_PHASE + "\""));
        assertTrue(prompt.system.contains("[PROJECT_INSTRUCTION]\nPROJECT-AUTH-TEXT\n[/PROJECT_INSTRUCTION]"));
        assertTrue(prompt.system.contains("[TURN_PROMPT]\nTURN-AUTH-TEXT\n[/TURN_PROMPT]"));
        assertTrue(prompt.system.contains("[WORKFLOW]\nWORKFLOW-AUTH-TEXT\n[/WORKFLOW]"));
    }

    @Test public void reconcilePromptCarriesCandidatesVerbatimAndNumbersViL2() {
        PromptPair prompt = OpenRouterEditorialL3Provider.buildPrompt(reconcile());
        assertTrue(prompt.system.contains(OpenRouterEditorialL3Provider.RECONCILE_RULES));
        assertFalse(prompt.system.contains(OpenRouterEditorialL3Provider.REAUDIT_RULES));
        String user = prompt.user;
        assertTrue(user, user.contains("--- L3_REAUDIT_CANDIDATES ---\n" + CANDIDATES_JSON
                + "\n--- END L3_REAUDIT_CANDIDATES ---"));
        assertTrue(user.contains("--- VI_L2 ---\nL1|dong mot\nL2|dong hai\n--- END VI_L2 ---"));
        assertTrue(user.contains("--- DRAFT ---\ndraft one\ndraft two\n--- END DRAFT ---"));
        assertTrue(user.contains("--- REPORT_L1 ---\nreport l1 body\n--- END REPORT_L1 ---"));
        assertTrue(user.contains("--- CHANGE_MAP_L2 ---\n{\"changes\":[]}\n--- END CHANGE_MAP_L2 ---"));
        assertTrue(user.contains("\"wireSchemaVersion\":\"" + EditorialL3Execution.RECONCILE_WIRE + "\""));
        assertTrue(user.contains("\"attemptIdentity\":\"" + ATTEMPT + "\""));
    }

    private static String sample(PromptPair prompt) {
        return prompt.user.substring(prompt.user.indexOf(MARKER) + MARKER.length()).trim();
    }

    private static Object newCandidate(String id, String ledger, int line, String status) throws Exception {
        Class<?> type = Class.forName("com.ml.tblandroidtxt.editorial.pack.EditorialL3Execution$Candidate");
        Constructor<?> ctor = type.getDeclaredConstructor(String.class, String.class, int.class, String.class);
        ctor.setAccessible(true);
        return ctor.newInstance(id, ledger, line, status);
    }

    private static Object invoke(Method method, Object... args) throws Exception {
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception) throw (Exception) error.getCause();
            throw error;
        }
    }

    private static void assertRejected(Method method, Object... args) throws Exception {
        try {
            invoke(method, args);
            throw new AssertionError("unfilled template must be rejected");
        } catch (RuntimeException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test public void reauditSampleParsesOnlyAfterPlaceholdersAreFilled() throws Exception {
        String sample = sample(OpenRouterEditorialL3Provider.buildPrompt(reaudit()));
        JSONObject object = new JSONObject(sample);
        assertEquals(EditorialL3Execution.REAUDIT_WIRE, object.getString("wireSchemaVersion"));

        Method parse = EditorialL3Execution.class.getDeclaredMethod("parseReaudit",
                byte[].class, String.class, int.class);
        parse.setAccessible(true);
        assertRejected(parse, b(sample), ATTEMPT, 2);

        object.put("attemptIdentity", ATTEMPT);
        JSONObject row = object.getJSONArray("candidates").getJSONObject(0);
        row.put("ledger", "UNIT").put("line", 2).put("status", "PROCESSED");
        List<?> parsed = (List<?>) invoke(parse, b(object.toString()), ATTEMPT, 2);
        assertEquals(1, parsed.size());

        // Only the ledger placeholder left unfilled must also fail.
        row.put("ledger", "UNIT|TG|SR|RC");
        assertRejected(parse, b(object.toString()), ATTEMPT, 2);
    }

    @Test public void reconcileSampleParsesOnlyAfterPlaceholdersAreFilled() throws Exception {
        String sample = sample(OpenRouterEditorialL3Provider.buildPrompt(reconcile()));
        JSONObject object = new JSONObject(sample);
        assertEquals(EditorialL3Execution.RECONCILE_WIRE, object.getString("wireSchemaVersion"));

        Method parse = EditorialL3Execution.class.getDeclaredMethod("parseReconcile",
                byte[].class, String.class, List.class);
        parse.setAccessible(true);
        List<Object> candidates = List.of(newCandidate("U001", "UNIT", 1, "PROCESSED"));
        assertRejected(parse, b(sample), ATTEMPT, candidates);

        object.put("attemptIdentity", ATTEMPT);
        object.getJSONArray("resolutions").getJSONObject(0).put("status", "PROCESSED");
        object.getJSONArray("changes").getJSONObject(0)
                .put("before", "dong mot").put("after", "dong mot sua").put("reason", "fix wording");
        object.getJSONArray("preserved").getJSONObject(0)
                .put("line", 2).put("before", "dong hai").put("evidenceLimit", "no proof");
        object.getJSONArray("adversarialCoverage").getJSONObject(0)
                .put("finding", "checked raw units").put("verdict", "NO_DEFECT");
        object.getJSONArray("adversarialRegression").getJSONObject(0)
                .put("finding", "checked changes").put("verdict", "NO_DEFECT");
        object.getJSONObject("disposition").put("disposition", "CONTINUE")
                .put("reasonCode", "OK").put("stopClass", "NONE");
        Object wire = invoke(parse, b(object.toString()), ATTEMPT, candidates);
        assertNotNull(wire);

        // Unknown candidate id is rejected.
        JSONObject unknown = new JSONObject(object.toString());
        unknown.getJSONArray("resolutions").getJSONObject(0).put("candidateId", "U999");
        assertRejected(parse, b(unknown.toString()), ATTEMPT, candidates);

        // Empty adversarial probes are rejected.
        JSONObject noProbes = new JSONObject(object.toString());
        noProbes.put("adversarialCoverage", new JSONArray());
        assertRejected(parse, b(noProbes.toString()), ATTEMPT, candidates);
    }
}
