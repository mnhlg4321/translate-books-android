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
 * It holds Glossary and Pronoun profiles and (format 2) RAW chapters, never settings, API keys, jobs or Editorial data. Import only adds.
 * Format 1 files (no RAW) are still read.
 */
final class LibraryArchive {
    static final String FORMAT = "tbl-library";
    static final int FORMAT_VERSION = 2;
    static final long MAX_RAW_BYTES = 20L * 1024 * 1024;
    static final long MAX_RAW_TOTAL_BYTES = 400L * 1024 * 1024;
    static final String MANIFEST = "library.json";
    static final long MAX_MANIFEST_BYTES = 50L * 1024 * 1024;
    static final int MAX_ENTRIES = 20000;
    static final int MAX_ITEMS = 10000;

    /** One RAW chapter with its text (normalised, so {@code inputHash} is the hash of {@code text}). */
    static final class RawEntry {
        String id = UUID.randomUUID().toString();
        String name = "";
        String originalName = "";
        String encoding = "";
        String inputHash = "";
        String text = "";
        int chapterNo = ChapterNumber.NONE;
        int chars;
        long createdAt;
        long updatedAt;
    }

    /** What a backup holds after its checks; items that failed their hash are counted in {@link #bad}. */
    static final class Contents {
        final List<RawEntry> raws = new ArrayList<>();
        final List<GlossaryStoreCore.Glossary> glossaries = new ArrayList<>();
        final List<PronounStoreCore.Profile> pronouns = new ArrayList<>();
        String activeGlossaryId = "";
        String activePronounId = "";
        String appVersion = "";
        long createdAt;
        final List<String> bad = new ArrayList<>();
    }

    /** Items to add (front of each list) and the counts the preview shows. */
    static final class MergePlan {
        final List<GlossaryStoreCore.Glossary> addGlossaries = new ArrayList<>();
        final List<PronounStoreCore.Profile> addPronouns = new ArrayList<>();
        final List<RawEntry> addRaws = new ArrayList<>();
        int existing;
        int duplicate;
        int renamed;
        final List<String> bad = new ArrayList<>();

        int addedCount() { return addGlossaries.size() + addPronouns.size() + addRaws.size(); }
    }

    static byte[] build(List<GlossaryStoreCore.Glossary> glossaries, List<PronounStoreCore.Profile> pronouns,
                        String activeGlossaryId, String activePronounId, String appVersion, long createdAt) throws Exception {
        return build(glossaries, pronouns, new ArrayList<RawEntry>(), activeGlossaryId, activePronounId, appVersion, createdAt);
    }

    static byte[] build(List<GlossaryStoreCore.Glossary> glossaries, List<PronounStoreCore.Profile> pronouns, List<RawEntry> raws,
                        String activeGlossaryId, String activePronounId, String appVersion, long createdAt) throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("formatVersion", FORMAT_VERSION);
        root.put("appVersion", safe(appVersion));
        root.put("createdAt", createdAt);
        JSONArray g = new JSONArray();
        for (GlossaryStoreCore.Glossary item : glossaries) g.put(new JSONObject().put("sha256", integrity(item)).put("item", GlossaryStoreCore.toJson(item)));
        JSONArray p = new JSONArray();
        for (PronounStoreCore.Profile item : pronouns) p.put(new JSONObject().put("sha256", integrity(item)).put("item", PronounStoreCore.toJson(item)));
        JSONArray r = new JSONArray();
        for (int i = 0; i < raws.size(); i++) r.put(rawJson(raws.get(i), rawPath(i, raws.get(i))));
        root.put("glossaries", g);
        root.put("pronouns", p);
        root.put("raws", r);
        root.put("active", new JSONObject().put("glossaryId", safe(activeGlossaryId)).put("pronounId", safe(activePronounId)));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            put(zip, MANIFEST, root.toString(2));
            put(zip, "README.txt", readme(glossaries.size(), pronouns.size(), raws.size()));
            for (int i = 0; i < raws.size(); i++) put(zip, rawPath(i, raws.get(i)), raws.get(i).text);
            for (int i = 0; i < glossaries.size(); i++) {
                GlossaryStoreCore.Glossary item = glossaries.get(i);
                put(zip, "glossaries/" + number(i) + "_" + safeFileName(item.name) + ".csv", csv(item));
            }
            for (int i = 0; i < pronouns.size(); i++) {
                PronounStoreCore.Profile item = pronouns.get(i);
                put(zip, "pronouns/" + number(i) + "_" + safeFileName(item.name) + ".txt", safe(item.text));
            }
        }
        return bytes.toByteArray();
    }

    /** Reads only {@code library.json}; a missing or foreign manifest is an {@link IllegalArgumentException} with a readable reason. */
    static Contents read(InputStream in) throws IOException {
        String manifest = null;
        Map<String, String> rawFiles = new HashMap<>();
        long rawTotal = 0;
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) throw new IllegalArgumentException("Tệp có quá nhiều mục, không phải bản sao lưu thư viện.");
                if (!entry.isDirectory() && MANIFEST.equals(entry.getName())) {
                    if (manifest != null) throw new IllegalArgumentException("Tệp có hai library.json.");
                    manifest = readLimited(zip);
                } else if (!entry.isDirectory() && entry.getName().startsWith("raw/") && entry.getName().endsWith(".txt")) {
                    String text = readLimited(zip, MAX_RAW_BYTES);
                    rawTotal += text.length();
                    if (rawTotal > MAX_RAW_TOTAL_BYTES) throw new IllegalArgumentException("Các tệp RAW trong bản sao lưu quá lớn.");
                    rawFiles.put(entry.getName(), text);
                }
            }
        }
        if (manifest == null) throw new IllegalArgumentException("Tệp không có library.json: không phải bản sao lưu thư viện của app.");
        return parseManifest(manifest, rawFiles);
    }

    static Contents parseManifest(String manifest) { return parseManifest(manifest, new HashMap<String, String>()); }

    static Contents parseManifest(String manifest, Map<String, String> rawFiles) {
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
        JSONArray r = root.optJSONArray("raws");
        int total = (g == null ? 0 : g.length()) + (p == null ? 0 : p.length()) + (r == null ? 0 : r.length());
        if (total > MAX_ITEMS) throw new IllegalArgumentException("Bản sao lưu có quá nhiều mục (" + total + ").");
        if (g != null) for (int i = 0; i < g.length(); i++) {
            try {
                JSONObject wrapper = g.getJSONObject(i);
                GlossaryStoreCore.Glossary item = GlossaryStoreCore.fromJson(wrapper.getJSONObject("item"));
                if (!integrity(item).equals(wrapper.optString("sha256"))) { out.bad.add("Glossary " + safe(item.name) + ": sai mã kiểm"); continue; }
                out.glossaries.add(item);
            } catch (Exception e) {
                out.bad.add("Glossary #" + (i + 1) + ": không đọc được");
            }
        }
        if (p != null) for (int i = 0; i < p.length(); i++) {
            try {
                JSONObject wrapper = p.getJSONObject(i);
                PronounStoreCore.Profile item = PronounStoreCore.fromJson(wrapper.getJSONObject("item"));
                if (!integrity(item).equals(wrapper.optString("sha256"))) { out.bad.add("Pronoun " + safe(item.name) + ": sai mã kiểm"); continue; }
                out.pronouns.add(item);
            } catch (Exception e) {
                out.bad.add("Pronoun #" + (i + 1) + ": không đọc được");
            }
        }
        if (r != null) for (int i = 0; i < r.length(); i++) {
            String label = "RAW #" + (i + 1);
            try {
                JSONObject o = r.getJSONObject(i);
                RawEntry e = new RawEntry();
                e.id = o.getString("id");
                e.name = o.optString("name", "");
                e.originalName = o.optString("originalName", e.name);
                label = "RAW " + e.originalName;
                e.encoding = o.optString("encoding", "");
                e.chapterNo = o.has("chapterNo") && !o.isNull("chapterNo") ? o.getInt("chapterNo") : ChapterNumber.NONE;
                e.createdAt = o.optLong("createdAt", 0L);
                e.updatedAt = o.optLong("updatedAt", 0L);
                String text = rawFiles.get(o.getString("file"));
                if (text == null) { out.bad.add(label + ": thiếu tệp " + o.optString("file")); continue; }
                e.text = text;
                e.chars = text.length();
                e.inputHash = HashUtil.sha256(text);
                if (!e.inputHash.equals(o.optString("inputHash")) || !integrity(e).equals(o.optString("sha256"))) { out.bad.add(label + ": sai mã kiểm"); continue; }
                out.raws.add(e);
            } catch (Exception e) {
                out.bad.add(label + ": không đọc được");
            }
        }
        return out;
    }

    /**
     * Adds only: a new id keeps its id (Editorial combos refer to profiles by id); the same id with the same content is
     * already there; the same id with other content is added as a copy with a new id and a dated name; content equal to
     * another profile is a duplicate. Nothing in the current library is changed or removed.
     */
    static MergePlan plan(Contents in, List<GlossaryStoreCore.Glossary> currentGlossaries, List<PronounStoreCore.Profile> currentPronouns, String dateLabel) {
        return plan(in, currentGlossaries, currentPronouns, new ArrayList<RawEntry>(), dateLabel);
    }

    /** {@code currentRaws} need only id and inputHash. */
    static MergePlan plan(Contents in, List<GlossaryStoreCore.Glossary> currentGlossaries, List<PronounStoreCore.Profile> currentPronouns, List<RawEntry> currentRaws, String dateLabel) {
        MergePlan plan = new MergePlan();
        plan.bad.addAll(in.bad);
        Map<String, String> glossaryContentById = new HashMap<>();
        Set<String> glossaryContents = new HashSet<>();
        for (GlossaryStoreCore.Glossary g : currentGlossaries) { glossaryContentById.put(g.id, content(g)); glossaryContents.add(content(g)); }
        for (GlossaryStoreCore.Glossary g : in.glossaries) {
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
        for (PronounStoreCore.Profile p : currentPronouns) { pronounContentById.put(p.id, content(p)); pronounContents.add(content(p)); }
        for (PronounStoreCore.Profile p : in.pronouns) {
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
        Map<String, String> rawHashById = new HashMap<>();
        Set<String> rawHashes = new HashSet<>();
        for (RawEntry e : currentRaws) { rawHashById.put(e.id, e.inputHash); rawHashes.add(e.inputHash); }
        for (RawEntry e : in.raws) {
            String sameId = rawHashById.get(e.id);
            if (sameId != null && sameId.equals(e.inputHash)) { plan.existing++; continue; }
            if (rawHashes.contains(e.inputHash)) { plan.duplicate++; continue; }
            if (sameId != null) {
                e.id = UUID.randomUUID().toString();
                plan.renamed++;
            }
            plan.addRaws.add(e);
            rawHashById.put(e.id, e.inputHash);
            rawHashes.add(e.inputHash);
        }
        return plan;
    }

    /** Content identity: everything the person sees, without id, timestamps or the source file reference. */
    static String content(GlossaryStoreCore.Glossary g) {
        StringBuilder sb = new StringBuilder("glossary");
        field(sb, g.name); field(sb, g.sourceLang); field(sb, g.targetLang);
        if (g.terms != null) for (GlossaryStoreCore.Term t : g.terms) { field(sb, t.source); field(sb, t.target); field(sb, t.category); field(sb, t.note); }
        return HashUtil.sha256(sb.toString());
    }

    static String content(PronounStoreCore.Profile p) {
        StringBuilder sb = new StringBuilder("pronoun");
        field(sb, p.name); field(sb, p.text);
        return HashUtil.sha256(sb.toString());
    }

    /** Integrity of a stored item: its content plus the fields import keeps. */
    static String integrity(GlossaryStoreCore.Glossary g) {
        StringBuilder sb = new StringBuilder(content(g));
        field(sb, g.id); field(sb, String.valueOf(g.createdAt)); field(sb, String.valueOf(g.updatedAt));
        return HashUtil.sha256(sb.toString());
    }

    static String integrity(PronounStoreCore.Profile p) {
        StringBuilder sb = new StringBuilder(content(p));
        field(sb, p.id); field(sb, p.uri); field(sb, String.valueOf(p.createdAt)); field(sb, String.valueOf(p.updatedAt));
        return HashUtil.sha256(sb.toString());
    }

    static String integrity(RawEntry e) {
        StringBuilder sb = new StringBuilder("raw");
        field(sb, e.id); field(sb, e.name); field(sb, e.originalName); field(sb, e.encoding); field(sb, e.inputHash);
        field(sb, String.valueOf(e.chapterNo)); field(sb, String.valueOf(e.createdAt)); field(sb, String.valueOf(e.updatedAt));
        return HashUtil.sha256(sb.toString());
    }

    static String rawPath(int index, RawEntry e) {
        String base = e.originalName.isEmpty() ? e.name : e.originalName.replaceAll("(?i)\\.txt$", "");
        return "raw/" + number(index) + "_" + safeFileName(base) + ".txt";
    }

    private static JSONObject rawJson(RawEntry e, String file) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", e.id); o.put("name", e.name); o.put("originalName", e.originalName); o.put("encoding", e.encoding);
        o.put("inputHash", e.inputHash); o.put("chars", e.text.length());
        o.put("chapterNo", e.chapterNo >= 0 ? (Object) e.chapterNo : JSONObject.NULL);
        o.put("createdAt", e.createdAt); o.put("updatedAt", e.updatedAt);
        o.put("file", file); o.put("sha256", integrity(e));
        return o;
    }

    static String csv(GlossaryStoreCore.Glossary g) {
        StringBuilder sb = new StringBuilder("source,target,category,note\n");
        if (g.terms != null) for (GlossaryStoreCore.Term t : g.terms) {
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

    private static String readme(int glossaries, int pronouns, int raws) {
        return "Translate Books - sao lưu thư viện\n\n"
                + "Tệp này có " + glossaries + " glossary, " + pronouns + " pronoun profile và " + raws + " RAW.\n"
                + "Thư mục raw/ giữ nội dung từng RAW (app đọc lại khi nhập).\n"
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

    private static String readLimited(InputStream in) throws IOException { return readLimited(in, MAX_MANIFEST_BYTES); }

    private static String readLimited(InputStream in, long limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buffer)) > 0) {
            total += n;
            if (total > limit) throw new IllegalArgumentException("library.json quá lớn.");
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
