package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** v4.23 J5: contrast of every palette and status colour, onAccent, accent validation, and the saved choices with today's look as default. */
public class V423AppearanceTest {
    @Test public void everyPaletteHasReadableTextMutedTextAndStatusColoursOnItsSurfaces() {
        for (UiAppearance.Palette p : UiAppearance.all()) {
            int[] surfaces = {p.bg, p.panel, p.card, p.field};
            for (int surface : surfaces) {
                assertTrue(p.id + " text", ColorMath.ratio(p.text, surface) >= 7.0);
                assertTrue(p.id + " muted " + ColorMath.ratio(p.muted, surface), ColorMath.ratio(p.muted, surface) >= 4.5);
                assertTrue(p.id + " ok " + ColorMath.ratio(p.ok, surface), ColorMath.ratio(p.ok, surface) >= 4.5);
                assertTrue(p.id + " warn " + ColorMath.ratio(p.warn, surface), ColorMath.ratio(p.warn, surface) >= 4.5);
                assertTrue(p.id + " bad " + ColorMath.ratio(p.bad, surface), ColorMath.ratio(p.bad, surface) >= 3.0);
            }
        }
    }

    @Test public void lightPalettesAreLightAndTheDarkOnesKeepTheirStatusColours() {
        assertEquals(7, UiAppearance.light().size());
        assertTrue(UiAppearance.find("gray-light").light && UiAppearance.find("peach-light").light);
        assertFalse(UiAppearance.find("default").light);
        assertEquals(0xFF2BCF7E, UiAppearance.find("default").ok);
        assertEquals(0xFFEE4646, UiAppearance.find("blue").bad);
        assertEquals(0xFFF5B443, UiAppearance.find("camo").warn);
        for (UiAppearance.Palette p : UiAppearance.light()) assertTrue(ColorMath.luminance(p.bg) > 0.5);
    }

    @Test public void fiveNewLightPalettesMeetTextBorderAndAccentContrast() {
        String[] ids = {"gray-light", "mint-light", "sky-light", "lavender-light", "peach-light"};
        for (String id : ids) {
            UiAppearance.Palette p = UiAppearance.find(id);
            for (int surface : new int[]{p.bg, p.card, p.field}) {
                assertTrue(id + " text", ColorMath.ratio(p.text, surface) >= 4.5);
                assertTrue(id + " accent", ColorMath.ratio(p.accent, surface) >= 4.5);
            }
            assertTrue(id + " muted on card", ColorMath.ratio(p.muted, p.card) >= 4.5);
            assertTrue(id + " border on bg", ColorMath.ratio(p.border, p.bg) >= 3.0);
            assertTrue(id + " border on card", ColorMath.ratio(p.border, p.card) >= 3.0);
            assertTrue(id + " on accent", ColorMath.ratio(p.onAccent, p.accent) >= 4.5);
        }
    }

    @Test public void textOnTheAccentIsReadableInEveryLightPaletteAndForAnyCustomAccent() {
        for (UiAppearance.Palette p : UiAppearance.light()) assertTrue(p.id, ColorMath.ratio(p.onAccent, p.accent) >= 4.5);
        for (String hex : AppearanceSettings.PRESET_ACCENTS) {
            UiAppearance.Palette withAccent = UiAppearance.find("default").withAccent(ColorMath.parseHex(hex));
            assertTrue(hex, ColorMath.ratio(withAccent.onAccent, withAccent.accent) >= 4.5 || ColorMath.ratio(withAccent.onAccent, withAccent.accent) >= 3.0);
            assertEquals(ColorMath.onColor(ColorMath.parseHex(hex)), withAccent.onAccent);
        }
        assertEquals(ColorMath.BLACK, ColorMath.onColor(0xFFFFE066));
        assertEquals(ColorMath.WHITE, ColorMath.onColor(0xFF1A237E));
    }

    @Test public void contrastMathMatchesKnownValues() {
        assertEquals(21.0, ColorMath.ratio(ColorMath.BLACK, ColorMath.WHITE), 1e-9);
        assertEquals(1.0, ColorMath.ratio(0xFF336699, 0xFF336699), 1e-9);
        assertEquals(0xFF0000FF, (int) ColorMath.parseHex("#00f"));
        assertEquals(0xFF123456, (int) ColorMath.parseHex("123456"));
        assertNull(ColorMath.parseHex("12345"));
        assertNull(ColorMath.parseHex("zzzzzz"));
        assertEquals("#123456", ColorMath.hex(0xFF123456));
        assertEquals(0xFF808080, ColorMath.mix(0xFF000000, 0xFFFFFFFF, 0.5f) & 0xFFFFFFFF, 1);
    }

    @Test public void anAccentThatCannotBeReadOnThePaletteIsRefusedWithAReason() {
        UiAppearance.Palette dark = UiAppearance.find("default"), light = UiAppearance.find("light");
        assertNull(AppearanceSettings.accentError(0xFF8AB4FF, dark));
        assertNotNull("dark blue on a dark background", AppearanceSettings.accentError(0xFF1A237E, dark));
        assertNotNull("pale yellow on white", AppearanceSettings.accentError(0xFFFFE066, light));
        assertNull(AppearanceSettings.accentError(0xFF1D5FD1, light));
        String reason = AppearanceSettings.accentError(0xFF1A237E, dark);
        assertTrue(reason, reason.contains("#1A237E") && reason.contains("Default") && reason.contains("AA"));
        assertNotNull(AppearanceSettings.accentError(null, dark));
    }

    @Test public void defaultsAreTodaysLookAndRoundTripThroughPreferences() {
        AppearanceSettings d = AppearanceSettings.load(new FakeSharedPreferences());
        assertTrue(d.isDefault());
        assertEquals("default", d.resolve(false).id);
        assertEquals(1f, d.fontScale(), 0);
        assertEquals(1f, d.densityScale(), 0);
        assertEquals(1f, d.cornerFactor(), 0);
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        AppearanceSettings s = new AppearanceSettings();
        s.mode = AppearanceSettings.MODE_SYSTEM; s.palette = "blue"; s.paletteLight = "paper"; s.accent = "#2F8FD8"; s.fontPercent = 130;
        s.density = AppearanceSettings.DENSITY_COMPACT; s.corners = AppearanceSettings.CORNERS_ROUND;
        s.save(prefs);
        AppearanceSettings back = AppearanceSettings.load(prefs);
        assertFalse(back.isDefault());
        assertEquals("blue", back.palette);
        assertEquals("paper", back.paletteLight);
        assertEquals("", back.accent);
        assertEquals(130, back.fontPercent);
        assertEquals(1.3f, back.fontScale(), 1e-6);
        assertEquals(0.75f, back.densityScale(), 0);
        assertEquals(1.8f, back.cornerFactor(), 0);
        new AppearanceSettings().save(prefs);
        assertTrue(AppearanceSettings.load(prefs).isDefault());
    }

    @Test public void theOldPaletteKeyStillWorksAndBadValuesFallBack() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        prefs.values.put("palette", "gold");
        AppearanceSettings s = AppearanceSettings.load(prefs);
        assertEquals("gold", s.palette);
        assertEquals("gold", s.resolve(true).id);
        prefs.values.put("fontPercent", 77); prefs.values.put("accent", "nonsense"); prefs.values.put("density", "x"); prefs.values.put("corners", "x"); prefs.values.put("mode", "x");
        AppearanceSettings bad = AppearanceSettings.load(prefs);
        assertEquals(100, bad.fontPercent);
        assertEquals("", bad.accent);
        assertEquals(AppearanceSettings.DENSITY_COMFORTABLE, bad.density);
        assertEquals(AppearanceSettings.CORNERS_MEDIUM, bad.corners);
        assertEquals(AppearanceSettings.MODE_FIXED, bad.mode);
    }

    @Test public void followingTheSystemPicksTheLightOrTheDarkPaletteAndIgnoresTheOldAccent() {
        AppearanceSettings s = new AppearanceSettings();
        s.mode = AppearanceSettings.MODE_SYSTEM; s.palette = "slate"; s.paletteLight = "paper";
        assertEquals("slate", s.resolve(true).id);
        assertEquals("paper", s.resolve(false).id);
        s.accent = "#2F8FD8";
        assertEquals(UiAppearance.find("paper").accent, s.resolve(false).accent);
        assertEquals("paper", s.resolve(false).id);
        s.mode = AppearanceSettings.MODE_FIXED;
        assertEquals("slate", s.resolve(false).id);
    }

    @Test public void fontScaleChoicesAreTheFourOffered() {
        assertEquals(4, AppearanceSettings.FONT_PERCENTS.length);
        assertEquals(8, AppearanceSettings.PRESET_ACCENTS.length);
        for (String hex : AppearanceSettings.PRESET_ACCENTS) assertNotNull(hex, ColorMath.parseHex(hex));
    }
}
