package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class V48ImportNavigationInstrumentedTest {
    @Test public void glossarySelectionSurvivesStoreReload() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences preferences = target.getSharedPreferences("glossary_store", Context.MODE_PRIVATE);
        Map<String, ?> original = new HashMap<>(preferences.getAll());
        try {
            preferences.edit().clear().commit();
            clearStoreCache(GlossaryStore.class, "cachedJson", "cachedGlossaries");
            GlossaryStore.Glossary glossary = GlossaryStore.create(target, "v4.8 glossary fixture");
            glossary.terms.add(new GlossaryStore.Term("Magic", "Phép thuật", "term"));
            GlossaryStore.upsert(target, glossary);
            GlossaryStore.setSelectedId(target, glossary.id);

            clearStoreCache(GlossaryStore.class, "cachedJson", "cachedGlossaries");
            GlossaryStore.Glossary restored = GlossaryStore.selected(target);
            assertNotNull(restored);
            assertEquals(glossary.id, restored.id);
            assertTrue(GlossaryStore.selectedPromptText(target).contains("Magic => Phép thuật"));
        } finally {
            restore(preferences, original);
            clearStoreCache(GlossaryStore.class, "cachedJson", "cachedGlossaries");
        }
    }

    @Test public void pronounImportAndActivationSurviveColdStoreReload() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences preferences = target.getSharedPreferences("pronoun_store", Context.MODE_PRIVATE);
        Map<String, ?> original = new HashMap<>(preferences.getAll());
        try {
            preferences.edit().clear().commit();
            clearStoreCache(PronounStore.class, "cachedJson", "cachedProfiles");
            PronounStore.Profile profile = PronounStore.importAndSelectProfile(
                    target, "v4.8 pronoun fixture", "content://fixture/pronoun.csv",
                    "from,to,pronoun\nAlice,Bob,chị/em");
            assertNotNull(profile);

            clearStoreCache(PronounStore.class, "cachedJson", "cachedProfiles");
            PronounStore.Profile restored = PronounStore.selected(target);
            assertNotNull(restored);
            assertEquals(profile.id, restored.id);
            assertEquals(profile.text, PronounStore.selectedPromptText(target));
        } finally {
            restore(preferences, original);
            clearStoreCache(PronounStore.class, "cachedJson", "cachedProfiles");
        }
    }

    @Test public void BackWalksThroughLibraryAndSettingsWithoutFinishingActivity() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                a.switchTab("Glossaries");
                a.onBackPressed();
                assertEquals("Files", a.currentTab);
                a.onBackPressed();
                assertEquals("Translate", a.currentTab);

                a.settingsCategory = "Provider";
                a.switchTab("Settings");
                a.onBackPressed();
                assertEquals("Settings", a.currentTab);
                assertEquals("General", a.settingsCategory);
                a.onBackPressed();
                assertEquals("Translate", a.currentTab);
                assertTrue(!a.isFinishing());
            });
        }
    }

    @Test public void verticalGesturesStayInsideAllFourPrimaryTabs() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                String[] requested = {"Translate", "Jobs", "Library", "Settings"};
                String[] expected = {"Translate", "Jobs", "Files", "Settings"};
                for (int i = 0; i < requested.length; i++) {
                    a.switchTab(requested[i]);
                    View page = a.pageCache.get(expected[i]);
                    ScrollView scroll = findScrollView(page);
                    assertNotNull("missing scroll container for " + requested[i], scroll);
                    dispatchVerticalSwipe(scroll);
                    assertEquals(expected[i], a.currentTab);
                }
            });
        }
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

    private static ScrollView findScrollView(View view) {
        if (view instanceof ScrollView) return (ScrollView) view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            ScrollView found = findScrollView(group.getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }

    private static void dispatchVerticalSwipe(View view) {
        long downTime = SystemClock.uptimeMillis();
        view.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 200, 700, 0));
        view.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_MOVE, 200, 300, 0));
        view.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime + 40, MotionEvent.ACTION_UP, 200, 300, 0));
    }
}
