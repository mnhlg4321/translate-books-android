package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.KeyValueStore;
import com.ml.tblandroidtxt.UiAppearanceCore;

import java.util.ArrayList;
import java.util.List;

/**
 * What the person chose in Cài đặt · Giao diện, the PC twin of the phone's AppearanceSettings (same choices: fixed or follow the system,
 * a light and a dark palette, 4 text sizes, 2 densities, 3 corner styles). The palettes come from the shared UiAppearanceCore. Kept in the
 * PC "appearance" preferences; the older keys of the first PC build ("theme", "font_percent") still load.
 */
public final class DesktopAppearance {
    public static final String MODE_FIXED = "fixed", MODE_SYSTEM = "system";
    public static final String DENSITY_COMFORTABLE = "comfortable", DENSITY_COMPACT = "compact";
    public static final String CORNERS_SQUARE = "square", CORNERS_MEDIUM = "medium", CORNERS_ROUND = "round";
    public static final int[] FONT_PERCENTS = {90, 100, 115, 130};
    public static final String DEFAULT_PALETTE = "gray-light";
    public static final String DEFAULT_DARK = "default";

    public String mode = MODE_FIXED;
    /** The palette when the mode is fixed (any of the 22 palettes). */
    public String palette = DEFAULT_PALETTE;
    /** Palettes used when the mode follows the system. */
    public String paletteLight = DEFAULT_PALETTE;
    public String paletteDark = DEFAULT_DARK;
    public int fontPercent = 100;
    public String density = DENSITY_COMFORTABLE;
    public String corners = CORNERS_MEDIUM;

    public static DesktopAppearance defaults() { return new DesktopAppearance(); }

    private static String known(String id, String fallback) {
        for (UiAppearanceCore.Palette p : UiAppearanceCore.all()) if (p.id.equals(id)) return id;
        return fallback;
    }

    public static int nearestFont(int percent) {
        int best = FONT_PERCENTS[0];
        for (int p : FONT_PERCENTS) if (Math.abs(p - percent) < Math.abs(best - percent)) best = p;
        return best;
    }

    public static DesktopAppearance load(KeyValueStore p) {
        DesktopAppearance a = new DesktopAppearance();
        a.mode = MODE_SYSTEM.equals(p.getString("mode", MODE_FIXED)) ? MODE_SYSTEM : MODE_FIXED;
        a.palette = known(p.getString("palette", p.getString("theme", DEFAULT_PALETTE)), DEFAULT_PALETTE);
        a.paletteLight = known(p.getString("paletteLight", DEFAULT_PALETTE), DEFAULT_PALETTE);
        a.paletteDark = known(p.getString("paletteDark", DEFAULT_DARK), DEFAULT_DARK);
        try { a.fontPercent = nearestFont(Integer.parseInt(p.getString("fontPercent", p.getString("font_percent", "100")).trim())); } catch (NumberFormatException e) { a.fontPercent = 100; }
        a.density = DENSITY_COMPACT.equals(p.getString("density", DENSITY_COMFORTABLE)) ? DENSITY_COMPACT : DENSITY_COMFORTABLE;
        String c = p.getString("corners", CORNERS_MEDIUM);
        a.corners = CORNERS_SQUARE.equals(c) || CORNERS_ROUND.equals(c) ? c : CORNERS_MEDIUM;
        return a;
    }

    public void save(KeyValueStore p) {
        p.edit().putString("mode", mode).putString("palette", palette).putString("paletteLight", paletteLight).putString("paletteDark", paletteDark)
                .putString("fontPercent", String.valueOf(nearestFont(fontPercent))).putString("density", density).putString("corners", corners)
                .putString("theme", palette).putString("font_percent", String.valueOf(nearestFont(fontPercent))).commit();
    }

    public boolean isDefault() {
        DesktopAppearance d = defaults();
        return mode.equals(d.mode) && palette.equals(d.palette) && paletteLight.equals(d.paletteLight) && paletteDark.equals(d.paletteDark)
                && fontPercent == d.fontPercent && density.equals(d.density) && corners.equals(d.corners);
    }

    /** The palette to draw with now. */
    public UiAppearanceCore.Palette resolve(boolean systemNight) {
        if (MODE_SYSTEM.equals(mode)) return UiAppearanceCore.find(systemNight ? paletteDark : paletteLight);
        return UiAppearanceCore.find(palette);
    }

    /** Style classes the window root carries for density and corners (app.css reads them). */
    public List<String> rootClasses() {
        List<String> out = new ArrayList<>();
        out.add(DENSITY_COMPACT.equals(density) ? "density-compact" : "density-comfortable");
        out.add("corners-" + corners);
        return out;
    }

    public static List<UiAppearanceCore.Palette> lightPalettes() { return UiAppearanceCore.light(); }

    public static List<UiAppearanceCore.Palette> darkPalettes() { return UiAppearanceCore.dark(); }

    /** True when Windows is set to dark apps (Settings > Personalization > Colors); light when it cannot be read. */
    public static boolean systemIsNight() {
        try {
            int v = com.sun.jna.platform.win32.Advapi32Util.registryGetIntValue(com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER,
                    "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "AppsUseLightTheme");
            return v == 0;
        } catch (Throwable e) {
            return false;
        }
    }
}
