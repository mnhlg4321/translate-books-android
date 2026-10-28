package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/** The five light themes against the U6 table of docs/V423_UX_OVERHAUL_WORK_REQUEST.md section 7 (colour codes typed here from that table). */
public class UiAppearanceCoreTest {
    // name, id, bg, surface, raised, text, text2, divider, outline, accent, onAccent, ok, warn, err
    private static final String[][] U6 = {
            {"Xám nhạt", "gray-light", "#F1F2F4", "#FFFFFF", "#E7E9ED", "#16191F", "#4B5463", "#D5D9E0", "#6F7888", "#2457B8", "#FFFFFF", "#14703A", "#8A5300", "#C0262D"},
            {"Bạc hà", "mint-light", "#E9F5EE", "#F7FCF9", "#D9EDE2", "#10231A", "#3C5747", "#C4DDCF", "#5F8A74", "#0B7358", "#FFFFFF", "#14703A", "#8A5300", "#B8232B"},
            {"Trời nhạt", "sky-light", "#EAF2FA", "#F7FAFD", "#DCE8F4", "#0F1C2B", "#3F5268", "#C5D6E8", "#5F7EA0", "#1A5BB0", "#FFFFFF", "#14703A", "#8A5300", "#BE2A31"},
            {"Oải hương", "lavender-light", "#F0EDF8", "#FAF8FE", "#E3DEF2", "#1D1830", "#50476B", "#D3CBE8", "#776D9C", "#5A3DB0", "#FFFFFF", "#14703A", "#8A5300", "#B8232F"},
            {"Hồng đào", "peach-light", "#FBEFEA", "#FFFAF8", "#F4E0D8", "#2B1A15", "#5E443B", "#EBD0C5", "#9A7062", "#A63A1E", "#FFFFFF", "#2D6A1E", "#7A5000", "#A52222"},
    };

    private static String hex(int argb) { return ColorMath.hex(argb); }

    @Test public void theFiveLightThemesCarryTheU6CodesInTableOrder() {
        List<UiAppearanceCore.Palette> five = UiAppearanceCore.lightFive();
        assertEquals(5, five.size());
        for (int i = 0; i < 5; i++) {
            String[] row = U6[i];
            UiAppearanceCore.Palette p = five.get(i);
            assertEquals(row[0], p.name);
            assertEquals(row[1], p.id);
            assertTrue(p.light);
            assertEquals(row[0] + " bg", row[2], hex(p.bg));
            assertEquals(row[0] + " surface -> panel", row[3], hex(p.panel));
            assertEquals(row[0] + " raised -> card", row[4], hex(p.card));
            assertEquals(row[0] + " raised -> field", row[4], hex(p.field));
            assertEquals(row[0] + " text", row[5], hex(p.text));
            assertEquals(row[0] + " text2 -> muted", row[6], hex(p.muted));
            assertEquals(row[0] + " divider", row[7], hex(UiAppearanceCore.divider(p)));
            assertEquals(row[0] + " outline -> border", row[8], hex(p.border));
            assertEquals(row[0] + " accent", row[9], hex(p.accent));
            assertEquals(row[0] + " accentAlt = accent", row[9], hex(p.accentAlt));
            assertEquals(row[0] + " onAccent", row[10], hex(p.onAccent));
            assertEquals(row[0] + " ok", row[11], hex(p.ok));
            assertEquals(row[0] + " warn", row[12], hex(p.warn));
            assertEquals(row[0] + " err -> bad", row[13], hex(p.bad));
        }
    }

    @Test public void contrastMeetsTheU6Thresholds() {
        for (UiAppearanceCore.Palette p : UiAppearanceCore.lightFive()) {
            String n = p.name;
            assertTrue(n + " text on bg", ColorMath.ratio(p.text, p.bg) >= 4.5);
            assertTrue(n + " text on panel", ColorMath.ratio(p.text, p.panel) >= 4.5);
            assertTrue(n + " text on card", ColorMath.ratio(p.text, p.card) >= 4.5);
            assertTrue(n + " border on bg", ColorMath.ratio(p.border, p.bg) >= 3);
            assertTrue(n + " border on card", ColorMath.ratio(p.border, p.card) >= 3);
            assertTrue(n + " onAccent on accent", ColorMath.ratio(p.onAccent, p.accent) >= 4.5);
            assertTrue(n + " accent on bg", ColorMath.ratio(p.accent, p.bg) >= 4.5);
            assertTrue(n + " accent on card", ColorMath.ratio(p.accent, p.card) >= 4.5);
            assertTrue(n + " accent on field", ColorMath.ratio(p.accent, p.field) >= 4.5);
            assertTrue(n + " muted on card", ColorMath.ratio(p.muted, p.card) >= 4.5);
            assertTrue(n + " muted on bg", ColorMath.ratio(p.muted, p.bg) >= 4.5);
            assertTrue(n + " ok on panel", ColorMath.ratio(p.ok, p.panel) >= 4.5);
            assertTrue(n + " warn on panel", ColorMath.ratio(p.warn, p.panel) >= 4.5);
            assertTrue(n + " bad on panel", ColorMath.ratio(p.bad, p.panel) >= 4.5);
        }
    }

    @Test public void lookupFallsBackToDefaultAndTheListsKeepTheirSizes() {
        assertEquals("default", UiAppearanceCore.find("missing").id);
        assertEquals(7, UiAppearanceCore.light().size());
        assertEquals(15, UiAppearanceCore.dark().size());
    }
}
