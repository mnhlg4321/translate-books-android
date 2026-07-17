package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Recent/favorite model picker metadata. Selected model remains owned by SettingsStore. */
public final class ModelPreferences {
    private static final String PREF = "model_picker_v42";

    public static List<String> recent(Context context) {
        ArrayList<String> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .getString("recent", "[]"));
            for (int i = 0; i < array.length(); i++) result.add(array.optString(i, ""));
        } catch (Exception ignored) {}
        return result;
    }

    public static void recordRecent(Context context, String modelId) {
        String id = ModelCatalog.normalizeModelId(modelId);
        List<String> recent = recent(context);
        recent.remove(id);
        recent.add(0, id);
        while (recent.size() > 12) recent.remove(recent.size() - 1);
        JSONArray array = new JSONArray();
        for (String item : recent) array.put(item);
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString("recent", array.toString()).apply();
    }

    public static Set<String> favorites(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return new HashSet<>(preferences.getStringSet("favorites", new HashSet<>()));
    }

    public static boolean toggleFavorite(Context context, String modelId) {
        String id = ModelCatalog.normalizeModelId(modelId);
        Set<String> favorites = favorites(context);
        boolean nowFavorite;
        if (favorites.contains(id)) { favorites.remove(id); nowFavorite = false; }
        else { favorites.add(id); nowFavorite = true; }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putStringSet("favorites", favorites).apply();
        return nowFavorite;
    }

    private ModelPreferences() {}
}
