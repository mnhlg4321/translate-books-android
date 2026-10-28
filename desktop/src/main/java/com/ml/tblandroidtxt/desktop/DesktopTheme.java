package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.ColorMath;
import com.ml.tblandroidtxt.KeyValueStore;
import com.ml.tblandroidtxt.UiAppearanceCore;

import java.util.List;
import java.util.Locale;

/**
 * The colour theme and text size of the PC window. The five light themes come from the shared core (the same codes as the phone); the
 * choice is kept in the PC preferences as a theme id and a text size in percent. {@link #style} is the inline style of the scene root:
 * it sets the {@code -tbl-*} variables that app.css reads, plus the base font size.
 */
public final class DesktopTheme {
    public static final int[] FONT_PERCENTS = {90, 100, 115, 130};
    public static final int DEFAULT_FONT_PERCENT = 100;
    public static final String DEFAULT_THEME_ID = "gray-light";
    static final double BASE_FONT_PX = 13.0;
    private static final String KEY_THEME = "theme", KEY_FONT = "font_percent";

    private DesktopTheme() {}

    public static List<UiAppearanceCore.Palette> themes() { return UiAppearanceCore.lightFive(); }

    public static UiAppearanceCore.Palette theme(String id) {
        for (UiAppearanceCore.Palette p : themes()) if (p.id.equals(id)) return p;
        return UiAppearanceCore.find(DEFAULT_THEME_ID);
    }

    public static int nearestFontPercent(int percent) {
        int best = FONT_PERCENTS[0];
        for (int p : FONT_PERCENTS) if (Math.abs(p - percent) < Math.abs(best - percent)) best = p;
        return best;
    }

    public static String savedThemeId(KeyValueStore prefs) { return theme(prefs.getString(KEY_THEME, DEFAULT_THEME_ID)).id; }

    public static int savedFontPercent(KeyValueStore prefs) { try { return nearestFontPercent(Integer.parseInt(prefs.getString(KEY_FONT, String.valueOf(DEFAULT_FONT_PERCENT)).trim())); }
        catch (NumberFormatException e) { return DEFAULT_FONT_PERCENT; }
    }

    public static void save(KeyValueStore prefs, String themeId, int fontPercent) {
        prefs.edit().putString(KEY_THEME, theme(themeId).id).putString(KEY_FONT, String.valueOf(nearestFontPercent(fontPercent))).commit();
    }

    private static String hex(int argb) { return ColorMath.hex(argb); }

    /** Inline style for the scene root. */
    public static String style(UiAppearanceCore.Palette p, int fontPercent) {
        double px = BASE_FONT_PX * nearestFontPercent(fontPercent) / 100.0;
        return "-tbl-bg: " + hex(p.bg) + "; -tbl-surface: " + hex(p.panel) + "; -tbl-raised: " + hex(p.card)
                + "; -tbl-text: " + hex(p.text) + "; -tbl-text2: " + hex(p.muted) + "; -tbl-divider: " + hex(UiAppearanceCore.divider(p))
                + "; -tbl-outline: " + hex(p.border) + "; -tbl-accent: " + hex(p.accent) + "; -tbl-on-accent: " + hex(p.onAccent)
                + "; -tbl-ok: " + hex(p.ok) + "; -tbl-warn: " + hex(p.warn) + "; -tbl-err: " + hex(p.bad)
                + "; -fx-font-size: " + String.format(Locale.ROOT, "%.2f", px) + "px;";
    }
}
