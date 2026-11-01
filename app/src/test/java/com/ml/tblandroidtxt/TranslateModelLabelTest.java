package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TranslateModelLabelTest {
    @Test public void translateHeaderUsesShortModelAndKeepsLanguagePair() {
        assertEquals("Japanese → Vietnamese • gpt-5.6-luna",
                TranslateModelLabel.header("Japanese", "Vietnamese", "openai/gpt-5.6-luna"));
        assertEquals("Japanese → Vietnamese • claude-haiku-5.5",
                TranslateModelLabel.header("Japanese", "Vietnamese", "anthropic/claude-haiku-5.5"));
    }

    @Test public void unsetModelAndProviderHaveReadableLabels() {
        assertEquals("Japanese → Vietnamese • chưa chọn model",
                TranslateModelLabel.header("Japanese", "Vietnamese", " "));
        assertEquals("Provider: openrouter\nExact model ID: anthropic/claude-haiku-5.5",
                TranslateModelLabel.details("openrouter", "anthropic/claude-haiku-5.5"));
        assertEquals("chưa gửi request", TranslateModelLabel.jobCard(""));
    }
}
