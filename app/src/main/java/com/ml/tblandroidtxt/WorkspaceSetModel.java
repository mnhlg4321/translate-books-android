package com.ml.tblandroidtxt;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/** Android face of WorkspaceSetModel: the pure model lives in WorkspaceSetModelCore; only the "last opened" prefs stay here. */
final class WorkspaceSetModel extends WorkspaceSetModelCore {
    private WorkspaceSetModel() {}

    static Map<String, Long> readLastOpened(Context context) {
        Map<String, ?> raw = context.getSharedPreferences(PREF_LAST_OPENED, Context.MODE_PRIVATE).getAll();
        Map<String, Long> out = new HashMap<>();
        for (Map.Entry<String, ?> e : raw.entrySet()) if (e.getValue() instanceof Long) out.put(e.getKey(), (Long)e.getValue());
        return out;
    }

    static void recordOpened(Context context, String setId) {
        if (blank(setId)) return;
        context.getSharedPreferences(PREF_LAST_OPENED, Context.MODE_PRIVATE).edit()
                .putLong(setId, System.currentTimeMillis()).apply();
    }
}
