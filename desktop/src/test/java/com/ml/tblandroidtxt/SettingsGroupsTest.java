package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/** T6 (settings groups save and reopen the same, profile stays clean) and T8 (nothing reaches the provider without a confirmation). */
public class SettingsGroupsTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static String changed(DesktopSettingsModel.Field f, String current) {
        switch (f.kind) {
            case BOOL: return Boolean.parseBoolean(current) ? "false" : "true";
            case CHOICE: {
                for (String[] c : f.choices) if (!c[0].equals(current) && !f.needsConfirm(c[0])) return c[0];
                return current;
            }
            case INT: return String.valueOf(Integer.parseInt(current.isEmpty() ? "0" : current) + 7 + (f.key.equals("maxTokensPerChunk") ? 100 : 0)
                    + (f.key.equals("maxOutputTokens") ? 1000 : 0) + (f.key.equals("timeoutSeconds") ? 20 : 0));
            case DECIMAL: return f.key.equals("temperature") ? "0.7" : f.key.equals("softLimitRatio") ? "0.6" : "1.25";
            default: return f.key.equals("provider") ? "custom" : f.key.equals("baseUrl") ? "https://qa.example.test/v1/chat/completions" : "qa/model-doi";
        }
    }

    @Test public void everyGroupSavesAndReopensTheSameAndTheProfileStaysClean() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        Path dir = tmp.getRoot().toPath().resolve("data");
        java.util.Map<String, String> expected = new java.util.LinkedHashMap<>();
        try (DesktopRuntime rt = DesktopRuntime.open(dir, null)) {
            for (DesktopSettingsModel.Field f : DesktopSettingsModel.fields()) {
                String now = f.get(rt.loadSettings());
                String next = changed(f, now);
                assertNull(f.key + " accepted", rt.applySetting(f.key, next));
                expected.put(f.key, f.get(rt.loadSettings()));
            }
            assertEquals(6, DesktopSettingsModel.groups().size());
            rt.secrets().save("TEST_KEY_NOT_SECRET");
        }
        try (DesktopRuntime rt = DesktopRuntime.open(dir, null)) {
            for (DesktopSettingsModel.Field f : DesktopSettingsModel.fields()) {
                assertEquals(f.key + " after reopen", expected.get(f.key), f.get(rt.loadSettings()));
            }
            AppSettings s = rt.loadSettings();
            s.outputTreeUri = "content://phone/tree";
            s.envUri = "content://phone/env";
            s.editorial.model = "QA_editorial";
            rt.saveSettings(s);
            String json = rt.exportProfileJson();
            JSONObject o = new JSONObject(json);
            for (String forbidden : new String[]{"apiKey", "editorial", "outputTreeUri", "outputUri", "envUri", "instructionUri"}) assertFalse(forbidden, o.has(forbidden));
            assertFalse(json.contains("TEST_KEY_NOT_SECRET") || json.contains("QA_editorial"));
            assertEquals(expected.get("maxTokensPerChunk"), String.valueOf(o.getInt("maxTokensPerChunk")));
        }
    }

    @Test public void aBadValueIsRefusedAndTheOldOneStays() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        try (DesktopRuntime rt = DesktopRuntime.open(tmp.getRoot().toPath().resolve("b"), null)) {
            String before = rt.loadSettings().temperature + "";
            assertNotNull(rt.applySetting("temperature", "9"));
            assertNotNull(rt.applySetting("temperature", "abc"));
            assertNotNull(rt.applySetting("maxAttempts", "0"));
            assertNotNull(rt.applySetting("model", "  "));
            assertEquals(before, rt.loadSettings().temperature + "");
            assertEquals("a comma is accepted as the decimal mark", null, rt.applySetting("temperature", "0,5"));
            assertTrue(DesktopSettingsModel.field("reasoningEffort").needsConfirm("high"));
            assertFalse(DesktopSettingsModel.field("reasoningEffort").needsConfirm("low"));
        }
    }

    @Test public void recommendationAppliesAndUndoesAndNeverTouchesMoneyOrProvider() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        try (DesktopRuntime rt = DesktopRuntime.open(tmp.getRoot().toPath().resolve("r"), null)) {
            AppSettings s = rt.loadSettings();
            s.maxOutputTokens = 2048; s.maxAttempts = 1; s.costLimitUsd = 0.5; s.provider = "custom";
            rt.saveSettings(s);
            assertFalse(rt.recommendationDiffs().isEmpty());
            assertFalse(rt.canUndoRecommendation());
            rt.applyRecommendation();
            AppSettings after = rt.loadSettings();
            assertEquals(8192, after.maxOutputTokens);
            assertEquals(3, after.maxAttempts);
            assertEquals(0.5, after.costLimitUsd, 0);
            assertEquals("custom", after.provider);
            assertTrue(rt.recommendationDiffs().isEmpty());
            assertTrue(rt.canUndoRecommendation());
            rt.undoRecommendation();
            assertEquals(2048, rt.loadSettings().maxOutputTokens);
            assertEquals(1, rt.loadSettings().maxAttempts);
            assertFalse(rt.canUndoRecommendation());
            assertFalse(rt.unlimitedCost());
            rt.setUnlimitedCost(true);
            assertTrue(rt.unlimitedCost());
        }
    }

    @Test public void noProviderRequestIsSentUntilTheConfirmationHasBeenGiven() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        Path root = tmp.getRoot().toPath();
        AtomicInteger requests = new AtomicInteger();
        Path raw = root.resolve("001_QA_raw.txt");
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < 3; p++) { sb.append("甲").append(p); for (int i = 0; i < 84; i++) sb.append('日'); sb.append("。\n\n"); }
        Files.write(raw, sb.toString().getBytes(StandardCharsets.UTF_8));
        try (DesktopRuntime rt = DesktopRuntime.open(root.resolve("g"), null, (settings, prompt, limit, id, observer) -> {
            requests.incrementAndGet();
            OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
            r.content = "<TRANSLATION>T.</TRANSLATION>"; r.finishReason = "stop"; r.usageReported = true;
            r.promptTokens = 10; r.completionTokens = 5; r.totalTokens = 15;
            return r;
        })) {
            rt.requireConfirmation(true);
            rt.secrets().save("TEST_KEY_NOT_SECRET");
            AppSettings s = rt.loadSettings();
            s.model = "fixture-model"; s.reasoningEffort = "off"; s.optimizationPreset = "full"; s.maxTokensPerChunk = 80;
            rt.saveSettings(s);
            rt.importRawFiles(java.util.Arrays.asList(raw));

            assertTrue(rt.testApi().startsWith("Chưa xác nhận"));
            assertFalse(rt.startNew(raw, root.resolve("out").resolve("a.txt"), s, null, null));
            assertFalse(rt.startVolume(rt.newVolumePlan(null), s, null, null, root.resolve("out")));
            assertFalse(rt.resumeJob(1));
            assertFalse(rt.retryFailed(1));
            assertEquals("zero requests before any confirmation", 0, requests.get());
            assertEquals(0, rt.apiTestRequests());

            rt.confirmSpend();
            assertTrue(rt.testApi().startsWith("API OK"));
            assertEquals(1, requests.get());
            assertTrue("the confirmation is used up by one action", rt.testApi().startsWith("Chưa xác nhận"));
            assertEquals(1, requests.get());

            rt.confirmSpend();
            assertTrue(rt.startNew(raw, root.resolve("out").resolve("a.txt"), s, null, null));
            long deadline = System.currentTimeMillis() + 60_000;
            Thread.sleep(200);
            while (rt.isRunning() && System.currentTimeMillis() < deadline) Thread.sleep(20);
            assertTrue(requests.get() > 1);
        }
    }
}
