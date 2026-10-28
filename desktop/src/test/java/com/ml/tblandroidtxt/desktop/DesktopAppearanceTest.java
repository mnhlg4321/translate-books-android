package com.ml.tblandroidtxt.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ml.tblandroidtxt.MemoryKeyValueStore;
import com.ml.tblandroidtxt.UiAppearanceCore;

import org.junit.Test;

public class DesktopAppearanceTest {
    @Test public void defaultsRoundTripAndAnythingUnknownFallsBack() {
        MemoryKeyValueStore prefs = new MemoryKeyValueStore();
        DesktopAppearance a = DesktopAppearance.load(prefs);
        assertTrue(a.isDefault());
        a.mode = DesktopAppearance.MODE_SYSTEM;
        a.paletteDark = "violet";
        a.paletteLight = "paper";
        a.palette = "peach-light";
        a.fontPercent = 115;
        a.density = DesktopAppearance.DENSITY_COMPACT;
        a.corners = DesktopAppearance.CORNERS_ROUND;
        a.save(prefs);
        DesktopAppearance b = DesktopAppearance.load(prefs);
        assertEquals("system", b.mode);
        assertEquals("violet", b.paletteDark);
        assertEquals("paper", b.paletteLight);
        assertEquals("peach-light", b.palette);
        assertEquals(115, b.fontPercent);
        assertEquals("compact", b.density);
        assertEquals("round", b.corners);
        assertFalse(b.isDefault());
        prefs.edit().putString("palette", "no-such").putString("fontPercent", "x").putString("density", "??").putString("corners", "??").commit();
        DesktopAppearance c = DesktopAppearance.load(prefs);
        assertEquals(DesktopAppearance.DEFAULT_PALETTE, c.palette);
        assertEquals(100, c.fontPercent);
        assertEquals("comfortable", c.density);
        assertEquals("medium", c.corners);
    }

    @Test public void theFirstPcBuildChoicesStillLoad() {
        MemoryKeyValueStore prefs = new MemoryKeyValueStore();
        prefs.edit().putString("theme", "sky-light").putString("font_percent", "130").commit();
        DesktopAppearance a = DesktopAppearance.load(prefs);
        assertEquals("sky-light", a.palette);
        assertEquals(130, a.fontPercent);
    }

    @Test public void fixedModeUsesThePaletteAndSystemModePicksLightOrDark() {
        DesktopAppearance a = DesktopAppearance.defaults();
        a.palette = "graphite";
        assertEquals("graphite", a.resolve(false).id);
        assertEquals("graphite", a.resolve(true).id);
        a.mode = DesktopAppearance.MODE_SYSTEM;
        a.paletteLight = "mint-light";
        a.paletteDark = "coffee";
        assertEquals("mint-light", a.resolve(false).id);
        assertEquals("coffee", a.resolve(true).id);
    }

    @Test public void theCoreHasFifteenDarkPalettesAndTheStyleCarriesTheirTokens() {
        assertEquals(15, DesktopAppearance.darkPalettes().size());
        assertEquals(7, DesktopAppearance.lightPalettes().size());
        for (UiAppearanceCore.Palette p : DesktopAppearance.darkPalettes()) {
            String style = DesktopTheme.style(p, 100);
            assertTrue(p.id, style.contains("-tbl-bg: #") && style.contains("-tbl-accent: #") && style.contains("-tbl-text: #"));
        }
        DesktopAppearance a = DesktopAppearance.defaults();
        a.density = DesktopAppearance.DENSITY_COMPACT;
        a.corners = DesktopAppearance.CORNERS_SQUARE;
        assertEquals(java.util.Arrays.asList("density-compact", "corners-square"), a.rootClasses());
        assertEquals(4, DesktopAppearance.FONT_PERCENTS.length);
    }
}
