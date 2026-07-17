package com.ml.tblandroidtxt;

import android.content.Context;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class V48RealApiPromptInstrumentedTest {
    @Test public void twoRealChunksCarryGlossaryAndPronounIntoProviderPrompt() throws Exception {
        Bundle arguments = InstrumentationRegistry.getArguments();
        Assume.assumeTrue("real API test is opt-in", "true".equalsIgnoreCase(arguments.getString("realApi")));

        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppSettings settings = SettingsStore.load(target).copy();
        assertFalse("A configured API key is required on the device", safe(settings.apiKey).isEmpty());
        assertFalse("A configured model is required on the device", safe(settings.model).isEmpty());
        assertFalse("A configured endpoint is required on the device", safe(settings.baseUrl).isEmpty());

        settings.sourceLanguage = "English";
        settings.targetLanguage = "Vietnamese";
        settings.glossaryText = "source,target,category\nMagic,Phép thuật,term\nAlice,Alice,character\nBob,Bob,character";
        settings.pronounText = "from,to,pronoun\nAlice,Bob,chị/em";
        settings.maxAttempts = 1;
        settings.maxOutputTokens = 256;
        settings.refineAfter = false;
        settings.optimizationPreset = "balanced";

        List<Chunk> chunks = Arrays.asList(
                new Chunk(0, "", "Alice tells Bob that Magic has started.", ""),
                new Chunk(1, "", "Bob answers Alice and asks about Magic again.", ""));
        AtomicInteger requests = new AtomicInteger();
        TranslationEngine.ProviderClient verifiedRealClient = (s, prompt, outputLimit, requestId, observer) -> {
            String payload = safe(prompt.system) + "\n" + safe(prompt.user);
            assertTrue(payload.contains("# GLOSSARY LOCKS"));
            assertTrue(payload.contains("Magic"));
            assertTrue(payload.contains("Phép thuật"));
            assertTrue(payload.contains("# PRONOUN LOCKS"));
            assertTrue(payload.contains("Alice"));
            assertTrue(payload.contains("Bob"));
            assertTrue(payload.contains("chị/em"));
            requests.incrementAndGet();
            return OpenAICompatibleClient.chatWithUsage(s, prompt, outputLimit, requestId, observer);
        };

        TranslationEngine engine = new TranslationEngine(target, null, verifiedRealClient);
        String previous = "";
        for (Chunk chunk : chunks) {
            String translated = engine.translateWithRetry(chunk, previous, settings, () -> false, (usage, s) -> {}, event -> {});
            assertFalse(translated.trim().isEmpty());
            previous = translated;
        }
        assertEquals(2, requests.get());
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
