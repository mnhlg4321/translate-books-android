package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class GlossaryStore {
    private static final String PREF = "glossary_store";
    private static final String KEY_GLOSSARIES = "glossaries_json";
    private static final String KEY_SELECTED = "selected_glossary_id";
    private static volatile String cachedJson;
    private static volatile List<Glossary> cachedGlossaries;

    public static class Term {
        public String source = "";
        public String target = "";
        public String category = "term";

        public Term() {}
        public Term(String s, String t, String c) { source = safe(s); target = safe(t); category = safe(c).isEmpty() ? "term" : safe(c); }
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

    public static synchronized List<Glossary> loadAll(Context c) {
        ArrayList<Glossary> list = new ArrayList<>();
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String json = p.getString(KEY_GLOSSARIES, "[]");
        if (cachedGlossaries != null && TextUtils.equals(cachedJson, json)) return new ArrayList<>(cachedGlossaries);
        try {
            JSONArray arr = new JSONArray(json == null ? "[]" : json);
            for (int i = 0; i < arr.length(); i++) list.add(fromJson(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
        cachedJson=json;cachedGlossaries=new ArrayList<>(list);return list;
    }

    public static synchronized void saveAll(Context c, List<Glossary> list) {
        JSONArray arr = new JSONArray();
        try { for (Glossary g : list) arr.put(toJson(g)); } catch (Exception ignored) {}
        String json=arr.toString();cachedJson=json;cachedGlossaries=new ArrayList<>(list);
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_GLOSSARIES, json).apply();
    }

    public static Glossary create(Context c, String name) {
        Glossary g = new Glossary();
        if (!safe(name).isEmpty()) g.name = name.trim();
        ArrayList<Glossary> list = new ArrayList<>(loadAll(c));
        list.add(0, g);
        saveAll(c, list);
        return g;
    }

    public static void upsert(Context c, Glossary g) {
        if (g == null) return;
        g.updatedAt = System.currentTimeMillis();
        ArrayList<Glossary> list = new ArrayList<>(loadAll(c));
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (TextUtils.equals(list.get(i).id, g.id)) { list.set(i, g); found = true; break; }
        }
        if (!found) list.add(0, g);
        saveAll(c, list);
    }

    public static void delete(Context c, String id) {
        ArrayList<Glossary> list = new ArrayList<>(loadAll(c));
        for (int i = list.size() - 1; i >= 0; i--) if (TextUtils.equals(list.get(i).id, id)) list.remove(i);
        saveAll(c, list);
        if (TextUtils.equals(getSelectedId(c), id)) setSelectedId(c, "");
    }

    public static Glossary find(Context c, String id) {
        if (safe(id).isEmpty()) return null;
        for (Glossary g : loadAll(c)) if (TextUtils.equals(g.id, id)) return g;
        return null;
    }

    public static String getSelectedId(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_SELECTED, ""); }
    public static void setSelectedId(Context c, String id) { c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_SELECTED, safe(id)).apply(); }
    public static Glossary selected(Context c) { return find(c, getSelectedId(c)); }

    public static String selectedPromptText(Context c) {
        Glossary g = selected(c);
        return g == null ? "" : toPromptText(g);
    }

    public static String toPromptText(Glossary g) {
        if (g == null || g.terms == null || g.terms.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("# Glossary: ").append(g.name).append('\n');
        sb.append("Source, Target, Category\n");
        for (Term t : g.terms) {
            if (safe(t.source).isEmpty() || safe(t.target).isEmpty()) continue;
            sb.append(t.source.trim()).append(" => ").append(t.target.trim());
            if (!safe(t.category).isEmpty()) sb.append(" [").append(t.category.trim()).append("]");
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
            terms.add(new Term(e.source, e.target, category));
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

    private static JSONObject toJson(Glossary g) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", g.id); o.put("name", g.name); o.put("sourceLang", g.sourceLang); o.put("targetLang", g.targetLang);
        o.put("createdAt", g.createdAt); o.put("updatedAt", g.updatedAt);
        JSONArray arr = new JSONArray();
        for (Term t : g.terms) {
            JSONObject x = new JSONObject();
            x.put("source", t.source); x.put("target", t.target); x.put("category", t.category);
            arr.put(x);
        }
        o.put("terms", arr);
        return o;
    }

    private static Glossary fromJson(JSONObject o) throws Exception {
        Glossary g = new Glossary();
        g.id = o.optString("id", g.id); g.name = o.optString("name", g.name);
        g.sourceLang = o.optString("sourceLang", g.sourceLang); g.targetLang = o.optString("targetLang", g.targetLang);
        g.createdAt = o.optLong("createdAt", g.createdAt); g.updatedAt = o.optLong("updatedAt", g.updatedAt);
        JSONArray arr = o.optJSONArray("terms");
        if (arr != null) for (int i = 0; i < arr.length(); i++) {
            JSONObject x = arr.getJSONObject(i);
            g.terms.add(new Term(x.optString("source"), x.optString("target"), x.optString("category", "term")));
        }
        return g;
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String firstNonEmpty(String... vals) { for (String v : vals) if (!safe(v).trim().isEmpty()) return v.trim(); return ""; }
}
