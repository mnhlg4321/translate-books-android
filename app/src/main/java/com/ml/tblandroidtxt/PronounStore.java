package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.List;

/** Android face of PronounStore: same signatures over SharedPreferences "pronoun_store"; the logic and static caches live in PronounStoreCore. */
public final class PronounStore extends PronounStoreCore {
    private PronounStore() {}
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE); }

    private static KeyValueStore store(Context c) { return new AndroidPrefsStore(prefs(c)); }

    public static List<Profile> loadAll(Context c) { return PronounStoreCore.loadAll(store(c)); }
    public static boolean saveAll(Context c, List<Profile> list) { return PronounStoreCore.saveAll(store(c), list); }
    public static boolean isCorrupt(Context c) { return PronounStoreCore.isCorrupt(store(c)); }
    public static String corruptText(Context c) { return PronounStoreCore.corruptText(store(c)); }
    public static Profile importProfile(Context c, String name, String uri, String text) { return PronounStoreCore.importProfile(store(c), name, uri, text); }
    public static Profile importAndSelectProfile(Context c, String name, String uri, String text) { return PronounStoreCore.importAndSelectProfile(store(c), name, uri, text); }
    public static boolean upsert(Context c, Profile p) { return PronounStoreCore.upsert(store(c), p); }
    public static boolean delete(Context c, String id) { return PronounStoreCore.delete(store(c), id); }
    public static Profile find(Context c, String id) { return PronounStoreCore.find(store(c), id); }
    public static String getSelectedId(Context c) { return PronounStoreCore.getSelectedId(store(c)); }
    public static boolean setSelectedId(Context c, String id) { return PronounStoreCore.setSelectedId(store(c), id); }
    public static boolean saveAndSelect(Context c, Profile p) { return PronounStoreCore.saveAndSelect(store(c), p); }
    public static Profile selected(Context c) { return PronounStoreCore.selected(store(c)); }
    public static String selectedPromptText(Context c) { return PronounStoreCore.selectedPromptText(store(c)); }
    public static void migrateLegacy(Context c, AppSettings s) { PronounStoreCore.migrateLegacy(store(c), s); }
    static List<Profile> loadAll(SharedPreferences pref) { return PronounStoreCore.loadAll(new AndroidPrefsStore(pref)); }
    static boolean saveAll(SharedPreferences pref, List<Profile> list) { return PronounStoreCore.saveAll(new AndroidPrefsStore(pref), list); }
    /** Declared here (not only inherited) because tests reach the JSON mapping reflectively through this class. */
    static JSONObject toJson(Profile x) throws Exception { return PronounStoreCore.toJson(x); }
    static Profile fromJson(JSONObject o) { return PronounStoreCore.fromJson(o); }
}
