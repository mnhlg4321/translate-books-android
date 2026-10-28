package com.ml.tblandroidtxt;

import android.content.Context;

/** The product palettes live in {@link UiAppearanceCore} (shared with the PC); this adds the per-phone choice. */
public final class UiAppearance extends UiAppearanceCore {
    public static Palette load(Context c) { return find(c.getSharedPreferences("appearance", Context.MODE_PRIVATE).getString("palette", "default")); }
    public static void save(Context c, String id) { c.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putString("palette", find(id).id).apply(); }
    private UiAppearance() {}
}
