package com.ml.tblandroidtxt.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ml.tblandroidtxt.MemoryKeyValueStore;
import com.ml.tblandroidtxt.UiAppearanceCore;

import org.junit.Test;

public class DesktopThemeTest {
    @Test public void theFiveThemesAreTheSharedLightFiveAndTheStyleCarriesEveryToken() {
        assertEquals(5, DesktopTheme.themes().size());
        UiAppearanceCore.Palette peach = DesktopTheme.theme("peach-light");
        String style = DesktopTheme.style(peach, 100);
        for (String token : new String[]{"-tbl-bg: #FBEFEA", "-tbl-surface: #FFFAF8", "-tbl-raised: #F4E0D8", "-tbl-text: #2B1A15", "-tbl-text2: #5E443B",
                "-tbl-divider: #EBD0C5", "-tbl-outline: #9A7062", "-tbl-accent: #A63A1E", "-tbl-on-accent: #FFFFFF", "-tbl-ok: #2D6A1E",
                "-tbl-warn: #7A5000", "-tbl-err: #A52222", "-fx-font-size: 13.00px"}) {
            assertTrue(token + " in " + style, style.contains(token));
        }
        assertTrue(DesktopTheme.style(peach, 130).contains("-fx-font-size: 16.90px"));
        assertTrue(DesktopTheme.style(peach, 90).contains("-fx-font-size: 11.70px"));
    }

    @Test public void choiceIsSavedAndUnknownValuesFallBackToTheDefaults() {
        MemoryKeyValueStore prefs = new MemoryKeyValueStore();
        assertEquals(DesktopTheme.DEFAULT_THEME_ID, DesktopTheme.savedThemeId(prefs));
        assertEquals(100, DesktopTheme.savedFontPercent(prefs));
        DesktopTheme.save(prefs, "sky-light", 115);
        assertEquals("sky-light", DesktopTheme.savedThemeId(prefs));
        assertEquals(115, DesktopTheme.savedFontPercent(prefs));
        DesktopTheme.save(prefs, "no-such-theme", 120);
        assertEquals(DesktopTheme.DEFAULT_THEME_ID, DesktopTheme.savedThemeId(prefs));
        assertEquals("120 rounds to the nearest offered size", 115, DesktopTheme.savedFontPercent(prefs));
        prefs.edit().putString("font_percent", "garbage").commit();
        assertEquals(100, DesktopTheme.savedFontPercent(prefs));
    }
}
