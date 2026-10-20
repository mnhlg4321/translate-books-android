package com.ml.tblandroidtxt;

/** WCAG 2.x relative luminance and contrast ratio, and the "text colour that sits best on this colour" rule. */
public final class ColorMath {
    private ColorMath() {}

    private static double channel(int v) {
        double s = v / 255d;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    public static double luminance(int argb) {
        return 0.2126 * channel((argb >> 16) & 255) + 0.7152 * channel((argb >> 8) & 255) + 0.0722 * channel(argb & 255);
    }

    public static double ratio(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        double hi = Math.max(la, lb), lo = Math.min(la, lb);
        return (hi + 0.05) / (lo + 0.05);
    }

    public static final int WHITE = 0xFFFFFFFF, BLACK = 0xFF000000;

    /** Black or white, whichever has the higher contrast on {@code background}. */
    public static int onColor(int background) {
        return ratio(WHITE, background) >= ratio(BLACK, background) ? WHITE : BLACK;
    }

    /** {@code t} = 0 gives {@code a}, 1 gives {@code b}. Alpha is opaque. */
    public static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** "#RRGGBB" or "RRGGBB" (also 3 digits) to an opaque colour; null when it is not a colour. */
    public static Integer parseHex(String text) {
        if (text == null) return null;
        String v = text.trim();
        if (v.startsWith("#")) v = v.substring(1);
        if (v.length() == 3) v = "" + v.charAt(0) + v.charAt(0) + v.charAt(1) + v.charAt(1) + v.charAt(2) + v.charAt(2);
        if (v.length() != 6) return null;
        try { return 0xFF000000 | (int) Long.parseLong(v, 16); } catch (NumberFormatException e) { return null; }
    }

    public static String hex(int argb) { return String.format(java.util.Locale.US, "#%06X", argb & 0xFFFFFF); }
}
