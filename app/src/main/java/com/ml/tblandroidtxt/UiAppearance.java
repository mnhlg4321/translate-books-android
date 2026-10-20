package com.ml.tblandroidtxt;

import android.content.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Product UI palettes: workspace colours plus the status colours (success, warning, error) and the colour of text on the accent. Dark
 * palettes keep the status colours the app always had; the light palettes use darker ones that stay readable (4.5:1) on their own surfaces.
 */
public final class UiAppearance {
    public static final class Palette {
        public final String id, name;
        public final int bg, panel, card, field, border, text, muted, accent, accentAlt;
        /** True for a palette whose surfaces are light: the Activity then uses the light theme and dark system-bar icons. */
        public final boolean light;
        public final int ok, warn, bad;
        /** Text on a filled accent (primary button). */
        public final int onAccent;

        Palette(String id, String name, boolean light, String bg, String panel, String card, String field, String border, String text, String muted,
                String accent, String accentAlt, String ok, String warn, String bad, String onAccent) {
            this(id, name, light, c(bg), c(panel), c(card), c(field), c(border), c(text), c(muted), c(accent), c(accentAlt), c(ok), c(warn), c(bad),
                    onAccent == null ? c(text) : c(onAccent));
        }

        Palette(String id, String name, boolean light, int bg, int panel, int card, int field, int border, int text, int muted, int accent, int accentAlt,
                int ok, int warn, int bad, int onAccent) {
            this.id = id; this.name = name; this.light = light; this.bg = bg; this.panel = panel; this.card = card; this.field = field;
            this.border = border; this.text = text; this.muted = muted; this.accent = accent; this.accentAlt = accentAlt;
            this.ok = ok; this.warn = warn; this.bad = bad; this.onAccent = onAccent;
        }

        /** The same palette with another accent; the text on it is whichever of black and white reads better. */
        public Palette withAccent(int newAccent) {
            return new Palette(id, name, light, bg, panel, card, field, border, text, muted, newAccent, accentAlt, ok, warn, bad, ColorMath.onColor(newAccent));
        }
    }

    private static final String OK = "#2BCF7E", WARN = "#F5B443", BAD = "#EE4646";

    private static Palette dark(String id, String name, String bg, String panel, String card, String field, String border, String text, String muted, String accent, String accentAlt) {
        return new Palette(id, name, false, bg, panel, card, field, border, text, muted, accent, accentAlt, OK, WARN, BAD, null);
    }

    private static final List<Palette> PALETTES = Arrays.asList(
            dark("default", "Default", "#0E0F11", "#232324", "#2B2B2C", "#1B1B1C", "#434344", "#F5F7FA", "#A1A8B3", "#5C91EB", "#36B9C5"),
            dark("blue", "Ocean Blue", "#0D1219", "#182433", "#203044", "#131C28", "#344B66", "#F2F6FC", "#9EAEC0", "#4C9AFF", "#4BC0D9"),
            dark("teal", "Seafoam", "#0C1516", "#172728", "#203536", "#122021", "#345354", "#F1F8F7", "#9CB5B3", "#46B9AE", "#50C8C2"),
            dark("mint", "Mint", "#0D1512", "#192820", "#21352A", "#132019", "#365442", "#F2F8F4", "#9FB5A7", "#52B788", "#62C6A0"),
            dark("green", "Forest", "#10150F", "#20291E", "#2A3627", "#182016", "#45573F", "#F4F8F1", "#A7B4A2", "#6AAA64", "#72C085"),
            dark("gold", "Yellow Gold", "#17140D", "#2B2618", "#393220", "#211D12", "#5A5030", "#FAF7EC", "#BDB39A", "#D0A83A", "#E0BC55"),
            dark("orange", "Orange", "#18110D", "#302117", "#3D2A1D", "#251A12", "#61452E", "#FBF4EE", "#C0AA99", "#E1843D", "#E8A15C"),
            dark("rose", "Rose", "#180F13", "#2F1D25", "#3D2630", "#24171D", "#60404D", "#FAF1F5", "#BEA3AE", "#D36B91", "#E084A3"),
            dark("red", "Brick Red", "#180F0F", "#301D1D", "#3E2727", "#251717", "#624040", "#FAF1F1", "#BFA4A4", "#CC6464", "#DD7D72"),
            dark("violet", "Violet", "#130F19", "#261D31", "#322740", "#1D1725", "#514064", "#F6F2FA", "#AEA2BC", "#9871D8", "#B188E2"),
            dark("iris", "Iris Pastel", "#121119", "#242231", "#302E40", "#1B1A25", "#4C4963", "#F5F4FA", "#ABA8BD", "#8580C9", "#A39DE0"),
            dark("slate", "Blue Gray", "#101316", "#20262B", "#2A3239", "#181D21", "#424E57", "#F3F6F8", "#A3ADB5", "#7895AA", "#78B5C5"),
            dark("graphite", "Gray Dark", "#111111", "#242424", "#303030", "#1B1B1B", "#4B4B4B", "#F5F5F5", "#AAAAAA", "#8AA7D8", "#77B8B4"),
            dark("coffee", "Coffee", "#15110F", "#29211D", "#362B26", "#201916", "#55463E", "#F8F3F0", "#B6A8A0", "#B98568", "#C69B76"),
            dark("camo", "Camouflage", "#11140F", "#22271C", "#2D3425", "#191D15", "#48513C", "#F4F6F0", "#A8B09E", "#82945B", "#9DA66B"),
            new Palette("light", "Light", true, "#F4F5F7", "#FFFFFF", "#FFFFFF", "#EEF0F3", "#B9C0CB", "#14181F", "#4A5362", "#1D5FD1", "#0B6F82",
                    "#14703A", "#8A5300", "#C0262D", "#FFFFFF"),
            new Palette("paper", "Paper", true, "#F6EFE0", "#FBF6EA", "#FFFBF2", "#EFE6D2", "#C9BB9B", "#2A2118", "#5A4E3E", "#9A4A12", "#0E6B64",
                    "#2D6A1E", "#7A5000", "#A52222", "#FFFFFF")
    );

    public static List<Palette> all() { return PALETTES; }
    public static List<Palette> dark() { List<Palette> out = new ArrayList<>(); for (Palette p : PALETTES) if (!p.light) out.add(p); return out; }
    public static List<Palette> light() { List<Palette> out = new ArrayList<>(); for (Palette p : PALETTES) if (p.light) out.add(p); return out; }
    public static Palette find(String id) { for (Palette p : PALETTES) if (p.id.equals(id)) return p; return PALETTES.get(0); }
    public static Palette load(Context c) { return find(c.getSharedPreferences("appearance", Context.MODE_PRIVATE).getString("palette", "default")); }
    public static void save(Context c, String id) { c.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putString("palette", find(id).id).apply(); }
    private static int c(String s) { String v = s.startsWith("#") ? s.substring(1) : s; long n = Long.parseLong(v, 16); if (v.length() == 6) n |= 0xFF000000L; return (int) n; }
    private UiAppearance() {}
}
