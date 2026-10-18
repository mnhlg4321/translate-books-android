package com.ml.tblandroidtxt;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Library backup as one ZIP: {@code library.json} is the only part read back; the CSV/TXT copies and README are for people.
 * It holds Glossary and Pronoun profiles only, never settings, API keys, jobs or Editorial data. Import only adds.
 */
final class LibraryArchive {
    static final String FORMAT = "tbl-library";
    static final int FORMAT_VERSION = 1;
    static final String MANIFEST = "library.json";
    static final long MAX_MANIFEST_BYTES = 50L * 1024 * 1024;
    static final int MAX_ENTRIES = 20000;
    static final int MAX_ITEMS = 10000;

    /** What a backup holds after its checks; items that failed their hash are counted in {@link #bad}. */
    static final class Contents {
        final List<GlossaryStore.Glossary> glossaries = new ArrayList<>();
        final List<PronounStore.Profile> pronouns = new ArrayList<>();
        String activeGlossaryId = "";
        String activePronounId = "";
        String appVersion = "";
        long createdAt;
        final List<String> bad = new ArrayList<>();
    }

    /** Items to add (front of each list) and the counts the preview shows. */
    static final class MergePlan {
        final List<GlossaryStore.Glossary> addGlossaries = new ArrayList<>();
        final List<PronounStore.Profile> addPronouns = new ArrayList<>();
        int existing;
        int duplicate;
        int renamed;
        final List<String> bad = new ArrayList<>();

        int addedCount() { return addGlossaries.size() + addPronouns.size(); }
    }

    static byte[] build(List<GlossaryStore.Glossary> glossaries, List<PronounStore.Profile> pronouns,
                        String activeGlossaryId, String activePronounId, String appVersion, long createdAt) throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("formatVersion", FORMAT_VERSION);
        root.put("appVersion", safe(appVersion));
        root.put("createdAt", createdAt);
        JSONArray g = new JSONArray();
        for (GlossaryStore.Glossary item : glossaries) g.put(new JSONObject().put("sha256", integrity(item)).put("item", GlossaryStore.toJson(item)));
        JSONArray p = new JSONArray();
        for (PronounStore.Profile item : pronouns) p.put(new JSONObject().put("sha256", integrity(item)).put("item", PronounStore.toJson(item)));
        root.put("glossaries", g);
        root.put("pronouns", p);
        root.put("active", new JSONObject().put("glossaryId", safe(activeGlossaryId)).put("pronounId", safe(activePronounId)));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            put(zip, MANIFEST, root.toString(2));
            put(zip, "README.txt", readme(glossaries.size(), pronouns.size()));
            for (int i = 0; i < glossaries.size(); i++) {
                GlossaryStore.Glossary item = glossaries.get(i);
                put(zip, "glossaries/" + number(i) + "_" + safeFileName(item.name) + ".csv", csv(item));
            }
            for (int i = 0; i < pronouns.size(); i++) {
                PronounStore.Profile item = pronouns.get(i);
                put(zip, "pronouns/" + number(i) + "_" + safeFileName(item.name) + ".txt", safe(item.text));
            }
        }
        return bytes.toByteArray();
    }

    /** Reads only {@code library.json}; a missing or foreign manifest is an {@link IllegalArgumentException} with a readable reason. */
    static Contents read(InputStream in) throws IOException {
        String manifest = null;
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) throw new IllegalArgumentException("Tệp có quá nhiều mục, không phải bản sao lưu thư viện.");
                if (!entry.isDirectory() && MANIFEST.equals(entry.getName())) {
                    if (manifest != null) throw new IllegalArgumentException("Tệp có hai library.json.");
                    manifest = readLimited(zip);
                }
            }
        }
        if (manifest == null) throw new IllegalArgumentException("Tệp không có library.json: không phải bản sao lưu thư viện của app.");
        return parseManifest(manifest);
    }

    static Contents parseManifest(String manifest) {
        JSONObject root;
        try {
            root = new JSONObject(manifest);
        } catch (Exception e) {
            throw new IllegalArgumentException("library.json không đọc được.");
        }
        if (!FORMAT.equals(root.optString("format"))) throw new IllegalArgumentException("library.json không phải định dạng " + FORMAT + ".");
        int version = root.optInt("formatVersion", -1);
        if (version < 1) throw new IllegalArgumentException("library.json thiếu formatVersion.");
        if (version > FORMAT_VERSION) throw new IllegalArgumentException("Bản sao lưu do phiên bản app mới hơn tạo (định dạng " + version + ").");
        Contents out = new Contents();
        out.appVersion = root.optString("appVersion", "");
        out.createdAt = root.optLong("createdAt", 0L);
        JSONObject active = root.optJSONObject("active");
        if (active != null) {
            out.activeGlossaryId = active.optString("glossaryId", "");
            out.activePronounId = active.optString("pronounId", "");
        }
        JSONArray g = root.optJSONArray("glossaries");
        JSONArray p = root.optJSONArray("pronouns");
        int total = (g == null ? 0 : g.length()) + (p == null ? 0 : p.length());
        if (total > MAX_ITEMS) throw new IllegalArgumentException("Bản sao lưu có quá nhiều mục (" + total + ").");
        if (g != null) for (int i = 0; i < g.length(); i++) {
            try {
                JSONObject wrapper = g.getJSONObject(i);
                GlossaryStore.Glossary item = GlossaryStore.fromJson(wrapper.getJSONObject("item"));
                if (!integrity(item).equals(wrapper.optString("sha256"))) { out.bad.add("Glossary " + safe(item.name) + ": sai mã kiểm"); continue; }
                out.glossaries.add(item);
            } catch (Exception e) {
                out.bad.add("Glossary #" + (i + 1) + ": không đọc được");
            }
        }
        if (p != null) for (int i = 0; i < p.length(); i++) {
            try {
                JSONObject wrapper = p.getJSONObject(i);
                PronounStore.Profile item = PronounStore.fromJson(wrapper.getJSONObject("item"));
                if (!integrity(item).equals(wrapper.optString("sha256"))) { out.bad.add("Pronoun " + safe(item.name) + ": sai mã kiểm"); continue; }
                out.pronouns.add(item);
            } catch (Exception e) {
                out.bad.add("Pronoun #" + (i + 1) + ": không đọc được");
            }
        }
        return out;
    }

    /**
     * Adds only: a new id keeps its id (Editorial combos refer to profiles by id); the same id with the same content is
     * already there; the same id with other content is added as a copy with a new id and a dated name; content equal to
     * another profile is a duplicate. Nothing in the current library is changed or removed.
     */
    static MergePlan plan(Contents in, List<GlossaryStore.Glossary> currentGlossaries, List<PronounStore.Profile> currentPronouns, String dateLabel) {
        MergePlan plan = new MergePlan();
        plan.bad.addAll(in.bad);
        Map<String, String> glossaryContentById = new HashMap<>();
        Set<String> glossaryContents = new HashSet<>();
        for (GlossaryStore.Glossary g : currentGlossaries) { glossaryContentById.put(g.id, content(g)); glossaryContents.add(content(g)); }
        for (GlossaryStore.Glossary g : in.glossaries) {
            String key = content(g);
            String sameId = glossaryContentById.get(g.id);
            if (sameId != null && sameId.equals(key)) { plan.existing++; continue; }
            if (glossaryContents.contains(key)) { plan.duplicate++; continue; }
            if (sameId != null) {
                g.id = UUID.randomUUID().toString();
                g.name = safe(g.name) + " (nhập " + dateLabel + ")";
                plan.renamed++;
            }
            plan.addGlossaries.add(g);
            glossaryContentById.put(g.id, key);
            glossaryContents.add(key);
        }
        Map<String, String> pronounContentById = new HashMap<>();
        Set<String> pronounContents = new HashSet<>();
        for (PronounStore.Profile p : currentPronouns) { pronounContentById.put(p.id, content(p)); pronounContents.add(content(p)); }
        for (PronounStore.Profile p : in.pronouns) {
            String key = content(p);
            String sameId = pronounContentById.get(p.id);
            if (sameId != null && sameId.equals(key)) { plan.existing++; continue; }
            if (pronounContents.contains(key)) { plan.duplicate++; continue; }
            if (sameId != null) {
                p.id = UUID.randomUUID().toString();
                p.name = safe(p.name) + " (nhập " + dateLabel + ")";
                plan.renamed++;
            }
            plan.addPronouns.add(p);
            pronounContentById.put(p.id, key);
            pronounContents.add(key);
        }
        return plan;
    }

    /** Content identity: everything the person sees, without id, timestamps or the source file reference. */
    static String content(GlossaryStore.Glossary g) {
        StringBuilder sb = new StringBuilder("glossary");
        field(sb, g.name); field(sb, g.sourceLang); field(sb, g.targetLang);
        if (g.terms != null) for (GlossaryStore.Term t : g.terms) { field(sb, t.source); field(sb, t.target); field(sb, t.category); field(sb, t.note); }
        return HashUtil.sha256(sb.toString());
    }

    static String content(PronounStore.Profile p) {
        StringBuilder sb = new StringBuilder("pronoun");
        field(sb, p.name); field(sb, p.text);
        return HashUtil.sha256(sb.toString());
    }

    /** Integrity of a stored item: its content plus the fields import keeps. */
    static String integrity(GlossaryStore.Glossary g) {
        StringBuilder sb = new StringBuilder(content(g));
        field(sb, g.id); field(sb, String.valueOf(g.createdAt)); field(sb, String.valueOf(g.updatedAt));
        return HashUtil.sha256(sb.toString());
    }

    static String integrity(PronounStore.Profile p) {
        StringBuilder sb = new StringBuilder(content(p));
        field(sb, p.id); field(sb, p.uri); field(sb, String.valueOf(p.createdAt)); field(sb, String.valueOf(p.updatedAt));
        return HashUtil.sha256(sb.toString());
    }

    static String csv(GlossaryStore.Glossary g) {
        StringBuilder sb = new StringBuilder("source,target,category,note\n");
        if (g.terms != null) for (GlossaryStore.Term t : g.terms) {
            sb.append(csvCell(t.source)).append(',').append(csvCell(t.target)).append(',')
                    .append(csvCell(t.category)).append(',').append(csvCell(t.note)).append('\n');
        }
        return sb.toString();
    }

    static String csvCell(String value) {
        String v = safe(value);
        if (v.indexOf(',') < 0 && v.indexOf('"') < 0 && v.indexOf('\n') < 0 && v.indexOf('\r') < 0) return v;
        return '"' + v.replace("\"", "\"\"") + '"';
    }

    static String safeFileName(String name) {
        String cleaned = safe(name).trim().replaceAll("[^\\p{L}\\p{N}._-]+", "_").replaceAll("^[._]+", "");
        if (cleaned.isEmpty()) cleaned = "unnamed";
        return cleaned.length() > 60 ? cleaned.substring(0, 60) : cleaned;
    }

    static String suggestedFileName(String timestamp) {
        return "TranslateBooks-library-" + timestamp + ".zip";
    }

    private static String readme(int glossaries, int pronouns) {
        return "Translate Books - sao lưu thư viện\n\n"
                + "Tệp này có " + glossaries + " glossary và " + pronouns + " pronoun profile.\n"
                + "library.json là phần app đọc khi nhập lại (Library > Files > Nhập thư viện).\n"
                + "Thư mục glossaries/ (CSV: source,target,category,note) và pronouns/ (TXT) chỉ để xem; sửa chúng không đổi nội dung khi nhập.\n"
                + "Tệp không chứa API key, cài đặt, job dịch hay dữ liệu Biên tập.\n"
                + "Nhập lại chỉ thêm mục mới, không ghi đè hay xóa mục đang có, không đổi Glossary/Pronoun đang dùng.\n";
    }

    private static void put(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(safe(text).getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buffer)) > 0) {
            total += n;
            if (total > MAX_MANIFEST_BYTES) throw new IllegalArgumentException("library.json quá lớn.");
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void field(StringBuilder sb, String value) {
        String v = safe(value);
        sb.append('\u0001').append(v.length()).append(':').append(v);
    }

    private static String number(int index) { return String.format(Locale.ROOT, "%03d", index + 1); }

    private static String safe(String s) { return s == null ? "" : s; }

    private LibraryArchive() {}
}
