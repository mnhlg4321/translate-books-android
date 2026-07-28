package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.ScrollView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class V415LibraryScrollInstrumentedTest {
    @Test public void usePreservesListScrollForGlossaryAndPronoun() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences glossaryPreferences = target.getSharedPreferences("glossary_store", Context.MODE_PRIVATE);
        SharedPreferences pronounPreferences = target.getSharedPreferences("pronoun_store", Context.MODE_PRIVATE);
        SharedPreferences settingsPreferences = target.getSharedPreferences("settings", Context.MODE_PRIVATE);
        Map<String, ?> originalGlossaries = new HashMap<>(glossaryPreferences.getAll());
        Map<String, ?> originalPronouns = new HashMap<>(pronounPreferences.getAll());
        Map<String, ?> originalSettings = new HashMap<>(settingsPreferences.getAll());
        try {
            glossaryPreferences.edit().clear().commit();
            pronounPreferences.edit().clear().commit();
            clearStoreCache(GlossaryStore.class, "cachedJson", "cachedGlossaries");
            clearStoreCache(PronounStore.class, "cachedJson", "cachedProfiles");

            List<GlossaryStore.Glossary> glossaries = glossaryFixtures();
            GlossaryStore.saveAll(target, glossaries);
            GlossaryStore.setSelectedId(target, glossaries.get(0).id);
            List<PronounStore.Profile> pronouns = pronounFixtures();
            PronounStore.saveAll(target, pronouns);
            PronounStore.setSelectedId(target, pronouns.get(0).id);

            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                assertScrollPreservedForGlossary(scenario, glossaries.get(8));
                assertScrollPreservedForPronoun(scenario, pronouns.get(8));
            }
        } finally {
            restore(glossaryPreferences, originalGlossaries);
            restore(pronounPreferences, originalPronouns);
            restore(settingsPreferences, originalSettings);
            clearStoreCache(GlossaryStore.class, "cachedJson", "cachedGlossaries");
            clearStoreCache(PronounStore.class, "cachedJson", "cachedProfiles");
            assertEquals(originalGlossaries, glossaryPreferences.getAll());
            assertEquals(originalPronouns, pronounPreferences.getAll());
            assertEquals(originalSettings, settingsPreferences.getAll());
        }
    }

    private static void assertScrollPreservedForGlossary(
            ActivityScenario<MainActivity> scenario, GlossaryStore.Glossary selection) {
        AtomicInteger before = new AtomicInteger();
        scenario.onActivity(a -> a.switchTab("Glossaries"));
        waitForIdle();
        scenario.onActivity(a -> {
            ScrollView scroll = a.findScrollView(a.pageCache.get("Glossaries"));
            assertNotNull(scroll);
            scroll.scrollTo(0, 900);
            before.set(scroll.getScrollY());
            assertTrue("glossary fixture must be scrollable", before.get() > 0);
            a.selectGlossary(selection);
        });
        waitForIdle();
        scenario.onActivity(a -> {
            ScrollView scroll = a.findScrollView(a.pageCache.get("Glossaries"));
            assertNotNull(scroll);
            assertEquals(before.get(), scroll.getScrollY());
            assertEquals(selection.id, GlossaryStore.getSelectedId(a));
        });
    }

    private static void assertScrollPreservedForPronoun(
            ActivityScenario<MainActivity> scenario, PronounStore.Profile selection) {
        AtomicInteger before = new AtomicInteger();
        scenario.onActivity(a -> a.switchTab("Pronouns"));
        waitForIdle();
        scenario.onActivity(a -> {
            ScrollView scroll = a.findScrollView(a.pageCache.get("Pronouns"));
            assertNotNull(scroll);
            scroll.scrollTo(0, 900);
            before.set(scroll.getScrollY());
            assertTrue("pronoun fixture must be scrollable", before.get() > 0);
            a.selectPronoun(selection);
        });
        waitForIdle();
        scenario.onActivity(a -> {
            ScrollView scroll = a.findScrollView(a.pageCache.get("Pronouns"));
            assertNotNull(scroll);
            assertEquals(before.get(), scroll.getScrollY());
            assertEquals(selection.id, PronounStore.getSelectedId(a));
        });
    }

    private static List<GlossaryStore.Glossary> glossaryFixtures() {
        ArrayList<GlossaryStore.Glossary> fixtures = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            GlossaryStore.Glossary glossary = new GlossaryStore.Glossary();
            glossary.name = String.format("G%02d", i);
            glossary.terms.add(new GlossaryStore.Term("source" + i, "target" + i, "term"));
            fixtures.add(glossary);
        }
        return fixtures;
    }

    private static List<PronounStore.Profile> pronounFixtures() {
        ArrayList<PronounStore.Profile> fixtures = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            PronounStore.Profile profile = new PronounStore.Profile();
            profile.name = String.format("P%02d", i);
            profile.text = "from,to,pronoun\nAlice,Bob,she/he";
            fixtures.add(profile);
        }
        return fixtures;
    }

    private static void waitForIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void clearStoreCache(Class<?> type, String jsonField, String itemsField) throws Exception {
        Field json = type.getDeclaredField(jsonField);
        Field items = type.getDeclaredField(itemsField);
        json.setAccessible(true);
        items.setAccessible(true);
        json.set(null, null);
        items.set(null, null);
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
