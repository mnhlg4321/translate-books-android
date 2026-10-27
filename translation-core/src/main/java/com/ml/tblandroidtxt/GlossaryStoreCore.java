package com.ml.tblandroidtxt;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class GlossaryStoreCore {
    static final String PREF = "glossary_store";
    static final String KEY_GLOSSARIES = "glossaries_json";
    static final String KEY_SELECTED = "selected_glossary_id";
    private static volatile String cachedJson;
    private static volatile List<Glossary> cachedGlossaries;
    private static volatile String corruptJson;

    public static class Term {
        public String source = "";
        public String target = "";
        public String category = "term";
        public String note = "";

        public Term() {}
        public Term(String s, String t, String c) { this(s, t, c, ""); }
        public Term(String s, String t, String c, String n) {
            source = safe(s);
            target = safe(t);
            category = safe(c).isEmpty() ? "term" : safe(c);
            note = safe(n);
        }
    }

    public static class Glossary {
        public String id = UUID.randomUUID().toString();
        public String name = "New glossary";
        public String sourceLang = "—";
        public String targetLang = "—";
        public long createdAt = System.currentTimeMillis();
        public long updatedAt = System.currentTimeMillis();
        public ArrayList<Term> terms = new ArrayList<>();

        public int count() { return terms == null ? 0 : terms.size(); }
    }



    /** An unreadable store is preserved under a new key and locked for writes; it never reads as an empty library. */
    static synchronized List<Glossary> loadAll(KeyValueStore p) {
        String json = p.getString(KEY_GLOSSARIES, "[]");
        if (cachedGlossaries != null && Objects.equals(cachedJson, json)) return new ArrayList<>(cachedGlossaries);
        List<Glossary> parsed = parse(json);
        if (parsed == null) {
            LibraryPrefs.preserveCorrupt(p, KEY_GLOSSARIES, json);
            corruptJson = json;
            cachedJson = null; cachedGlossaries = null;
            return new ArrayList<>();
        }
        corruptJson = null;
        cachedJson = json; cachedGlossaries = new ArrayList<>(parsed);
        return new ArrayList<>(parsed);
    }

    /** The stored list, or null when any part of it cannot be read. */
    static List<Glossary> parse(String json) {
        ArrayList<Glossary> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json == null ? "[]" : json);
            for (int i = 0; i < arr.length(); i++) list.add(fromJson(arr.getJSONObject(i)));
            return list;
        } catch (Exception e) {
            return null;
        }
    }

    static String serialize(List<Glossary> list) throws Exception {
        JSONArray arr = new JSONArray();
        for (Glossary g : list) arr.put(toJson(g));
        return arr.toString();
    }

    public static boolean isCorrupt(KeyValueStore store) { loadAll(store); return corruptJson != null; }

    /** The unreadable stored text while the store is locked, else null. */
    public static String corruptText(KeyValueStore store) { loadAll(store); return corruptJson; }


    /** Writes synchronously; refuses while the store is unreadable or when any glossary cannot be serialized. */
    static synchronized boolean saveAll(KeyValueStore p, List<Glossary> list) {
        loadAll(p);
        if (corruptJson != null || list == null) return false;
        String json;
        try { json = serialize(list); } catch (Exception e) { return false; }
        if (!p.edit().putString(KEY_GLOSSARIES, json).commit()) return false;
        cachedJson = json; cachedGlossaries = new ArrayList<>(list);
        return true;
    }

    /** The new glossary, or null when the store refused the write. */
    public static Glossary create(KeyValueStore store, String name) {
        Glossary g = new Glossary();
        if (!safe(name).isEmpty()) g.name = name.trim();
        ArrayList<Glossary> list = new ArrayList<>(loadAll(store));
        list.add(0, g);
        return saveAll(store, list) ? g : null;
    }

    public static boolean upsert(KeyValueStore store, Glossary g) {
        if (g == null) return false;
        g.updatedAt = System.currentTimeMillis();
        ArrayList<Glossary> list = new ArrayList<>(loadAll(store));
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i).id, g.id)) { list.set(i, g); found = true; break; }
        }
        if (!found) list.add(0, g);
        return saveAll(store, list);
    }

    public static boolean delete(KeyValueStore store, String id) {
        ArrayList<Glossary> list = new ArrayList<>(loadAll(store));
        for (int i = list.size() - 1; i >= 0; i--) if (Objects.equals(list.get(i).id, id)) list.remove(i);
        if (!saveAll(store, list)) return false;
        if (Objects.equals(getSelectedId(store), id)) setSelectedId(store, "");
        return true;
    }

    public static Glossary find(KeyValueStore store, String id) {
        if (safe(id).isEmpty()) return null;
        for (Glossary g : loadAll(store)) if (Objects.equals(g.id, id)) return g;
        return null;
    }

    public static String getSelectedId(KeyValueStore store) { return store.getString(KEY_SELECTED, ""); }
    public static boolean setSelectedId(KeyValueStore store, String id) { return store.edit().putString(KEY_SELECTED, safe(id)).commit(); }
    public static Glossary selected(KeyValueStore store) { return find(store, getSelectedId(store)); }

    public static String selectedPromptText(KeyValueStore store) {
        Glossary g = selected(store);
        return g == null ? "" : toPromptText(g);
    }

    private static final java.util.LinkedHashMap<String, String> PROMPT_TEXT_CACHE = new java.util.LinkedHashMap<String, String>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, String> eldest) { return size() > 24; }
    };

    /** Cheap identity of a glossary's content (no allocation per term); a prompt text is rebuilt only when this changes. */
    static String contentKey(Glossary g) {
        long h = 17;
        for (Term t : g.terms) {
            h = h * 31 + (t.source == null ? 0 : t.source.hashCode());
            h = h * 31 + (t.target == null ? 0 : t.target.hashCode());
            h = h * 31 + (t.category == null ? 0 : t.category.hashCode());
            h = h * 31 + (t.note == null ? 0 : t.note.hashCode());
        }
        return g.id + "|" + g.name + "|" + g.terms.size() + "|" + h;
    }

    public static String toPromptText(Glossary g) {
        if (g == null || g.terms == null || g.terms.isEmpty()) return "";
        String key = contentKey(g);
        synchronized (PROMPT_TEXT_CACHE) {
            String known = PROMPT_TEXT_CACHE.get(key);
            if (known != null) return known;
        }
        String built = buildPromptText(g);
        synchronized (PROMPT_TEXT_CACHE) { PROMPT_TEXT_CACHE.put(key, built); }
        return built;
    }

    private static String buildPromptText(Glossary g) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Glossary: ").append(g.name).append('\n');
        sb.append("Source, Target, Category\n");
        for (Term t : g.terms) {
            if (safe(t.source).isEmpty() || safe(t.target).isEmpty()) continue;
            sb.append(t.source.trim()).append(" => ").append(t.target.trim());
            if (!safe(t.category).isEmpty()) sb.append(" [").append(t.category.trim()).append("]");
            String note = normalizePromptNote(t.note);
            if (!note.isEmpty()) sb.append(" | ").append(note);
            sb.append('\n');
        }
        return sb.toString();
    }

    public static List<Term> parseTerms(String fileName, String text) {
        ArrayList<Term> terms = new ArrayList<>();
        PromptContextBuilder.ParseReport report = PromptContextBuilder.validate(text, "");
        for (PromptContextBuilder.TermEntry e : report.glossaryTerms) {
            if (safe(e.source).isEmpty() || safe(e.target).isEmpty()) continue;
            String category = safe(e.category).isEmpty() ? guessCategory(fileName) : e.category;
            terms.add(new Term(e.source, e.target, category, e.note));
        }
        return dedupe(terms);
    }

    public static void mergeTerms(Glossary g, List<Term> incoming) {
        if (g == null || incoming == null) return;
        Set<String> seen = new HashSet<>();
        for (Term t : g.terms) seen.add(key(t));
        for (Term t : incoming) {
            if (safe(t.source).isEmpty() || safe(t.target).isEmpty()) continue;
            String k = key(t);
            if (!seen.contains(k)) { g.terms.add(t); seen.add(k); }
        }
    }

    public static String suggestedImportName(String currentName, List<String> successfulFileNames) {
        String current = safe(currentName).trim();
        if (!current.isEmpty() && !"New glossary".equalsIgnoreCase(current)) return current;
        if (successfulFileNames == null || successfulFileNames.isEmpty()) {
            return current.isEmpty() ? "New glossary" : current;
        }
        String first = safe(successfulFileNames.get(0)).trim();
        if (first.isEmpty()) first = "Imported glossary";
        int additional = successfulFileNames.size() - 1;
        return additional <= 0 ? first : first + " +" + additional + " files";
    }

    private static List<Term> dedupe(List<Term> src) {
        ArrayList<Term> out = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (Term t : src) {
            String k = key(t);
            if (!seen.contains(k)) { out.add(t); seen.add(k); }
        }
        return out;
    }

    private static String key(Term t) { return safe(t.source).toLowerCase(Locale.ROOT) + "\u0000" + safe(t.target).toLowerCase(Locale.ROOT); }
    private static String guessCategory(String name) { return safe(name).toLowerCase(Locale.ROOT).contains("character") ? "character" : "term"; }

    private static String[] splitLine(String l) {
        if (l.contains("\t")) return clean(l.split("\t", -1));
        if (l.contains(",")) return clean(splitCsv(l));
        if (l.contains("=>")) return clean(l.split("=>", -1));
        if (l.contains("->")) return clean(l.split("->", -1));
        if (l.contains("→")) return clean(l.split("→", -1));
        return new String[]{l};
    }

    private static String[] splitCsv(String line) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') q = !q;
            else if (ch == ',' && !q) { out.add(cur.toString()); cur.setLength(0); }
            else cur.append(ch);
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private static String[] clean(String[] arr) {
        ArrayList<String> out = new ArrayList<>();
        for (String s : arr) out.add(safe(s).replace("\"", "").trim());
        return out.toArray(new String[0]);
    }

    static JSONObject toJson(Glossary g) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", g.id); o.put("name", g.name); o.put("sourceLang", g.sourceLang); o.put("targetLang", g.targetLang);
        o.put("createdAt", g.createdAt); o.put("updatedAt", g.updatedAt);
        JSONArray arr = new JSONArray();
        for (Term t : g.terms) {
            JSONObject x = new JSONObject();
            x.put("source", t.source); x.put("target", t.target); x.put("category", t.category);
            x.put("note", t.note);
            arr.put(x);
        }
        o.put("terms", arr);
        return o;
    }

    static Glossary fromJson(JSONObject o) throws Exception {
        Glossary g = new Glossary();
        g.id = o.optString("id", g.id); g.name = o.optString("name", g.name);
        g.sourceLang = o.optString("sourceLang", g.sourceLang); g.targetLang = o.optString("targetLang", g.targetLang);
        g.createdAt = o.optLong("createdAt", g.createdAt); g.updatedAt = o.optLong("updatedAt", g.updatedAt);
        JSONArray arr = o.optJSONArray("terms");
        if (arr != null) for (int i = 0; i < arr.length(); i++) {
            JSONObject x = arr.getJSONObject(i);
            g.terms.add(new Term(x.optString("source"), x.optString("target"),
                    x.optString("category", "term"), x.optString("note", "")));
        }
        return g;
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String normalizePromptNote(String s) { return safe(s).replace('\r', ' ').replace('\n', ' ').trim(); }
    private static String firstNonEmpty(String... vals) { for (String v : vals) if (!safe(v).trim().isEmpty()) return v.trim(); return ""; }

    /** Clears the in-memory caches so tests do not leak state through the static fields. */
    static synchronized void resetForTest() {
        cachedJson = null;
        cachedGlossaries = null;
        corruptJson = null;
        PROMPT_TEXT_CACHE.clear();
    }
}
