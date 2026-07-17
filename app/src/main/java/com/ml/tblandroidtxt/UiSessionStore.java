package com.ml.tblandroidtxt;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/** Persists only UI selections; it never starts or resumes translation work. */
public final class UiSessionStore {
    private static final String PREF = "ui_session";
    private static final String INPUT_URIS = "inputUris";

    private UiSessionStore() {}

    public static void saveInputUris(Context context, List<Uri> uris) {
        if (context == null) return;
        JSONArray array = new JSONArray();
        if (uris != null) for (Uri uri : uris) if (uri != null) array.put(uri.toString());
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString(INPUT_URIS, array.toString())
                .apply();
    }

    public static List<String> loadInputUris(Context context) {
        ArrayList<String> out = new ArrayList<>();
        if (context == null) return out;
        try {
            JSONArray array = new JSONArray(context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .getString(INPUT_URIS, "[]"));
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "").trim();
                if (!value.isEmpty()) out.add(value);
            }
        } catch (Exception ignored) {}
        return out;
    }
}
