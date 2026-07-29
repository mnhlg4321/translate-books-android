package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class V415SettingsPersistenceInstrumentedTest {
    @Test public void lazyPhoneSectionsHydrateBeforeTheyCanPersistDefaults() {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences preferences = target.getSharedPreferences("settings", Context.MODE_PRIVATE);
        Map<String, ?> original = new HashMap<>(preferences.getAll());
        AppSettings fixture = SettingsStore.load(target);
        fixture.provider = "v415-provider";
        fixture.baseUrl = "https://v415.example.test/chat";
        fixture.apiKey = "v415-test-key";
        fixture.model = "v415/model";
        fixture.chunkMode = "char";
        fixture.maxTokensPerChunk = 777;
        fixture.maxCharsPerChunk = 8888;
        fixture.softLimitRatio = 0.67f;
        fixture.contextChars = 321;
        fixture.timeoutSeconds = 654;
        fixture.maxAttempts = 7;
        fixture.temperature = 0.42f;
        fixture.maxOutputTokens = 3456;
        fixture.glossaryInjectLimit = 91;
        fixture.pronounInjectLimit = 37;
        fixture.translationInstructions = "v415 translation instructions";
        fixture.refinementInstructions = "v415 refinement instructions";
        SettingsStore.save(target, fixture);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                a.settingsCategory = "General";
                a.settingsSectionExpanded = true;
                a.switchTab("Settings");

                View settingsPage = a.pageCache.get("Settings");
                View providerHeader = findClickableAncestorWithExactText(settingsPage, "Provider");
                assertNotNull(providerHeader);
                assertTrue(providerHeader.performClick());
                assertEquals("v415-provider", a.providerField.getText().toString());
                assertEquals("https://v415.example.test/chat", a.baseUrlField.getText().toString());
                assertEquals("v415-test-key", a.apiKeyField.getText().toString());
                assertEquals("v415/model", a.modelField.getText().toString());
                assertFixtureStillStored(a);

                View promptHeader = findClickableAncestorWithExactText(settingsPage, "Prompt");
                assertNotNull(promptHeader);
                assertTrue(promptHeader.performClick());
                assertFixtureStillStored(a);

                View performanceHeader = findClickableAncestorWithExactText(settingsPage, "Performance");
                assertNotNull(performanceHeader);
                assertTrue(performanceHeader.performClick());
                assertEquals("char", a.chunkModeField.getText().toString());
                assertEquals("777", a.maxTokensField.getText().toString());
                assertEquals("8888", a.maxCharsField.getText().toString());
                assertEquals("0.67", a.softRatioField.getText().toString());
                assertEquals("321", a.contextField.getText().toString());
                assertEquals("654", a.timeoutField.getText().toString());
                assertEquals("7", a.attemptsField.getText().toString());
                assertEquals("0.42", a.tempField.getText().toString());
                assertEquals("3456", a.maxOutputField.getText().toString());
                assertEquals("91", a.glossaryLimitField.getText().toString());
                assertEquals("37", a.pronounLimitField.getText().toString());
                assertFixtureStillStored(a);

                a.refreshCurrentPage();
                assertEquals("char", a.chunkModeField.getText().toString());
                assertEquals("777", a.maxTokensField.getText().toString());
                assertFixtureStillStored(a);
            });
        } finally {
            restore(preferences, original);
            assertEquals(original, new HashMap<>(preferences.getAll()));
        }
    }

    private static void assertFixtureStillStored(Context context) {
        AppSettings stored = SettingsStore.load(context);
        assertEquals("v415-provider", stored.provider);
        assertEquals("https://v415.example.test/chat", stored.baseUrl);
        assertEquals("v415-test-key", stored.apiKey);
        assertEquals("v415/model", stored.model);
        assertEquals("char", stored.chunkMode);
        assertEquals(777, stored.maxTokensPerChunk);
        assertEquals(8888, stored.maxCharsPerChunk);
        assertEquals(0.67f, stored.softLimitRatio, 0.0001f);
        assertEquals(321, stored.contextChars);
        assertEquals(654, stored.timeoutSeconds);
        assertEquals(7, stored.maxAttempts);
        assertEquals(0.42f, stored.temperature, 0.0001f);
        assertEquals(3456, stored.maxOutputTokens);
        assertEquals(91, stored.glossaryInjectLimit);
        assertEquals(37, stored.pronounInjectLimit);
        assertEquals("v415 translation instructions", stored.translationInstructions);
        assertEquals("v415 refinement instructions", stored.refinementInstructions);
    }

    private static View findClickableAncestorWithExactText(View root, String expected) {
        TextView label = findText(root, expected);
        if (label == null) return null;
        ViewParent parent = label.getParent();
        while (parent instanceof View) {
            View candidate = (View) parent;
            if (candidate.isClickable()) return candidate;
            parent = candidate.getParent();
        }
        return null;
    }

    private static TextView findText(View root, String expected) {
        if (root instanceof TextView && expected.contentEquals(((TextView) root).getText())) {
            return (TextView) root;
        }
        if (!(root instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            TextView found = findText(group.getChildAt(i), expected);
            if (found != null) return found;
        }
        return null;
    }

    private static void restore(SharedPreferences preferences, Map<String, ?> values) {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) editor.putString(entry.getKey(), (String) value);
            else if (value instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) value);
            else if (value instanceof Integer) editor.putInt(entry.getKey(), (Integer) value);
            else if (value instanceof Long) editor.putLong(entry.getKey(), (Long) value);
            else if (value instanceof Float) editor.putFloat(entry.getKey(), (Float) value);
        }
        editor.commit();
    }
}
