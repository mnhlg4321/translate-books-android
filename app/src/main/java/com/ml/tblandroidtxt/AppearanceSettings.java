package com.ml.tblandroidtxt;

import android.content.SharedPreferences;

/**
 * What the owner chose in Settings > Appearance, kept in the {@code appearance} preferences. The old {@code palette} key stays and still
 * means "the palette" (the dark one when the system mode is followed); everything else is new and defaults to today's look.
 */
public final class AppearanceSettings {
    public static final String MODE_FIXED = "fixed", MODE_SYSTEM = "system";
    public static final String DENSITY_COMFORTABLE = "comfortable", DENSITY_COMPACT = "compact";
    public static final String CORNERS_SQUARE = "square", CORNERS_MEDIUM = "medium", CORNERS_ROUND = "round";
    public static final int[] FONT_PERCENTS = {90, 100, 115, 130};
    /** Accents offered as buttons; any other colour can be typed as hex. */
    public static final String[] PRESET_ACCENTS = {"#5C91EB", "#2F8FD8", "#1F9E89", "#3E9B4F", "#D08A1E", "#D8603A", "#C2457A", "#8A63D2"};

    public String mode = MODE_FIXED;
    /** The palette when the mode is fixed, and the dark palette when the system mode is followed. */
    public String palette = "default";
    public String paletteLight = "light";
    /** "#RRGGBB" or empty for the palette's own accent. */
    public String accent = "";
    public int fontPercent = 100;
    public String density = DENSITY_COMFORTABLE;
    public String corners = CORNERS_MEDIUM;

    public static AppearanceSettings load(SharedPreferences p) {
        AppearanceSettings s = new AppearanceSettings();
        s.mode = MODE_SYSTEM.equals(p.getString("mode", MODE_FIXED)) ? MODE_SYSTEM : MODE_FIXED;
        s.palette = UiAppearance.find(p.getString("palette", "default")).id;
        s.paletteLight = UiAppearance.find(p.getString("paletteLight", "light")).id;
        String accent = p.getString("accent", "");
        Integer parsed = ColorMath.parseHex(accent);
        s.accent = parsed == null ? "" : ColorMath.hex(parsed);
        int percent = p.getInt("fontPercent", 100);
        s.fontPercent = 100;
        for (int allowed : FONT_PERCENTS) if (allowed == percent) s.fontPercent = percent;
        s.density = DENSITY_COMPACT.equals(p.getString("density", DENSITY_COMFORTABLE)) ? DENSITY_COMPACT : DENSITY_COMFORTABLE;
        String corners = p.getString("corners", CORNERS_MEDIUM);
        s.corners = CORNERS_SQUARE.equals(corners) || CORNERS_ROUND.equals(corners) ? corners : CORNERS_MEDIUM;
        return s;
    }

    public void save(SharedPreferences p) {
        p.edit().putString("mode", mode).putString("palette", palette).putString("paletteLight", paletteLight).putString("accent", accent)
                .putInt("fontPercent", fontPercent).putString("density", density).putString("corners", corners).apply();
    }

    /** The default look: dark "Default" palette, no accent override, 100 percent, comfortable, medium corners. */
    public static AppearanceSettings defaults() { return new AppearanceSettings(); }

    public boolean isDefault() {
        AppearanceSettings d = defaults();
        return mode.equals(d.mode) && palette.equals(d.palette) && paletteLight.equals(d.paletteLight) && accent.equals(d.accent)
                && fontPercent == d.fontPercent && density.equals(d.density) && corners.equals(d.corners);
    }

    /** The palette to draw with now, with the chosen accent applied. */
    public UiAppearance.Palette resolve(boolean systemNight) {
        UiAppearance.Palette base;
        if (MODE_SYSTEM.equals(mode)) base = systemNight ? UiAppearance.find(palette) : UiAppearance.find(paletteLight);
        else base = UiAppearance.find(palette);
        Integer custom = ColorMath.parseHex(accent);
        return custom == null ? base : base.withAccent(custom);
    }

    public float fontScale() { return fontPercent / 100f; }

    /** Multiplier for paddings, margins and gaps. Buttons keep their 44 dp minimum whatever this is. */
    public float densityScale() { return DENSITY_COMPACT.equals(density) ? 0.75f : 1f; }

    /** Multiplier for corner radii. */
    public float cornerFactor() {
        if (CORNERS_SQUARE.equals(corners)) return 0f;
        return CORNERS_ROUND.equals(corners) ? 1.8f : 1f;
    }

    /** Smallest contrast an accent must have with the palette's surfaces: it is also used as text colour. */
    public static final double MIN_ACCENT_CONTRAST = 4.5;

    /** Why {@code accent} cannot be used with {@code palette}, or null when it can. */
    public static String accentError(Integer accent, UiAppearance.Palette palette) {
        if (accent == null) return "Mã màu không hợp lệ. Nhập dạng #RRGGBB, ví dụ #2F8FD8.";
        double onBg = ColorMath.ratio(accent, palette.bg), onCard = ColorMath.ratio(accent, palette.card), onField = ColorMath.ratio(accent, palette.field);
        double worst = Math.min(onBg, Math.min(onCard, onField));
        if (worst >= MIN_ACCENT_CONTRAST) return null;
        return String.format(java.util.Locale.US, "Màu %s khó đọc trên nền %s (tương phản %.1f:1, cần từ %.1f:1 để chữ nhấn đạt chuẩn AA). Hãy chọn màu %s hơn.",
                ColorMath.hex(accent), palette.name, worst, MIN_ACCENT_CONTRAST, palette.light ? "đậm" : "sáng");
    }
}
