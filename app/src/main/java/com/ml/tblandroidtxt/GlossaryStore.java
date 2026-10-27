package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.List;

/** Android face of GlossaryStore: same signatures over SharedPreferences "glossary_store"; the logic and static caches live in GlossaryStoreCore. */
public class GlossaryStore extends GlossaryStoreCore {
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE); }

    private static KeyValueStore store(Context c) { return new AndroidPrefsStore(prefs(c)); }

    public static List<Glossary> loadAll(Context c) { return GlossaryStoreCore.loadAll(store(c)); }
    public static boolean saveAll(Context c, List<Glossary> list) { return GlossaryStoreCore.saveAll(store(c), list); }
    public static boolean isCorrupt(Context c) { return GlossaryStoreCore.isCorrupt(store(c)); }
    public static String corruptText(Context c) { return GlossaryStoreCore.corruptText(store(c)); }
    public static Glossary create(Context c, String name) { return GlossaryStoreCore.create(store(c), name); }
    public static boolean upsert(Context c, Glossary g) { return GlossaryStoreCore.upsert(store(c), g); }
    public static boolean delete(Context c, String id) { return GlossaryStoreCore.delete(store(c), id); }
    public static Glossary find(Context c, String id) { return GlossaryStoreCore.find(store(c), id); }
    public static String getSelectedId(Context c) { return GlossaryStoreCore.getSelectedId(store(c)); }
    public static boolean setSelectedId(Context c, String id) { return GlossaryStoreCore.setSelectedId(store(c), id); }
    public static Glossary selected(Context c) { return GlossaryStoreCore.selected(store(c)); }
    public static String selectedPromptText(Context c) { return GlossaryStoreCore.selectedPromptText(store(c)); }
    static List<Glossary> loadAll(SharedPreferences p) { return GlossaryStoreCore.loadAll(new AndroidPrefsStore(p)); }
    static boolean saveAll(SharedPreferences p, List<Glossary> list) { return GlossaryStoreCore.saveAll(new AndroidPrefsStore(p), list); }
    /** Declared here (not only inherited) because tests reach the JSON mapping reflectively through this class. */
    static JSONObject toJson(Glossary x) throws Exception { return GlossaryStoreCore.toJson(x); }
    static Glossary fromJson(JSONObject o) throws Exception { return GlossaryStoreCore.fromJson(o); }
}
