package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/** F3: verify the stable prompt prefix and the response-route diagnostic offline. */
public class RuntimeFixF3Test {
    @Test public void threeAdjacentChunksShareLongPrefixBeforeLocks() {
        AppSettings settings = new AppSettings();
        settings.translationInstructions = repeated("Giữ nguyên quy tắc dịch cố định này. ", 150);
        settings.glossaryText = "source,target,category,note\nLOCK,Giữ nguyên,global,\n";
        List<PromptPlan> plans = new ArrayList<>();
        plans.add(PromptPlan.translation(new Chunk(0, "", "Đoạn thứ nhất.", ""), "", settings));
        plans.add(PromptPlan.translation(new Chunk(1, "", "Đoạn thứ hai.", ""), "", settings));
        plans.add(PromptPlan.translation(new Chunk(2, "", "Đoạn thứ ba.", ""), "", settings));

        String marker = "# GLOSSARY LOCKS";
        int lockStart = plans.get(0).prompt.system.indexOf(marker);
        assertTrue(lockStart > 0);
        assertTrue(Chunker.approxTokens(plans.get(0).prompt.system.substring(0, lockStart)) >= 1_024);
        String prefix = plans.get(0).prompt.system.substring(0, lockStart);
        for (PromptPlan plan : plans) {
            assertEquals(prefix, plan.prompt.system.substring(0, lockStart));
        }
    }

    @Test public void chunkResponseEventCarriesRouteOnlyWithoutSecretOrHeader() {
        OpenAICompatibleClient.ChatResult response = new OpenAICompatibleClient.ChatResult();
        response.responseProvider = "offline-route-a";
        String line = ObservabilityLog.formatEvent("chunk_response",
                "job", 7, "responseHash", "abc123", "responseProvider", response.responseProvider,
                "inputTokens", 100, "cachedTokens", 25);
        assertTrue(line.contains("event=chunk_response"));
        assertTrue(line.contains("responseProvider=offline-route-a"));
        assertFalse(line.contains("apiKey"));
        assertFalse(line.toLowerCase().contains("authorization"));
        assertFalse(line.toLowerCase().contains("bearer"));
    }

    private static String repeated(String value, int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) out.append(value);
        return out.toString();
    }
}
