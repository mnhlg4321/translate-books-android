package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A combination being created or edited: three slots (RAW, Glossary, Pronoun) of chosen files and, below them, one row per RAW that
 * pairs it with a Glossary and a Pronoun taken only from the files chosen in the slots. The app suggests, the owner can change
 * any row; a row the owner has chosen is never re-paired, and a saved combination opens with exactly its saved choices.
 */
final class ComboDraft {
    /** A RAW as the draft needs it. */
    static final class RawRef {
        final String id;
        final String name;
        final int chapterNo;
        RawRef(String id, String name, int chapterNo) { this.id = id; this.name = name == null ? "" : name; this.chapterNo = chapterNo; }
    }

    String id = UUID.randomUUID().toString();
    String name = "";
    /** While true the name follows the shared part of the RAW names; typing a name turns it off. */
    boolean nameAuto = true;
    long createdAt = System.currentTimeMillis();
    boolean existing;
    boolean dirty;
    final LinkedHashSet<String> rawIds = new LinkedHashSet<>();
    final LinkedHashSet<String> glossaryIds = new LinkedHashSet<>();
    final LinkedHashSet<String> pronounIds = new LinkedHashSet<>();
    final List<ChapterSetMatcher.Row> rows = new ArrayList<>();
    /** Chosen Glossary / Pronoun files that no row uses, for the "không khớp chương nào" warning. */
    final List<String> unusedGlossaries = new ArrayList<>();
    final List<String> unusedPronouns = new ArrayList<>();

    /**
     * Recomputes the rows after a slot, a chapter number or a row choice changed.
     * {@code raws}: every RAW in the library (only those in {@link #rawIds} become rows); glossaries / pronouns: the whole library.
     */
    void rebuild(List<RawRef> raws, List<ChapterSetMatcher.Named> glossaries, List<ChapterSetMatcher.Named> pronouns) {
        Map<String, ChapterSetMatcher.Row> old = new HashMap<>();
        for (ChapterSetMatcher.Row r : rows) old.put(r.rawId, r);
        rows.clear();
        List<RawRef> picked = new ArrayList<>();
        for (RawRef r : raws) if (rawIds.contains(r.id)) picked.add(r);
        java.util.Collections.sort(picked, (x, y) -> ChapterNumber.compare(x.chapterNo, x.name, y.chapterNo, y.name));
        for (RawRef raw : picked) {
            ChapterSetMatcher.Row row = old.get(raw.id);
            if (row == null) {
                row = new ChapterSetMatcher.Row();
                row.rawId = raw.id;
                row.chapterNo = raw.chapterNo;
            }
            row.rawName = raw.name;
            rows.add(row);
        }
        List<ChapterSetMatcher.Named> slotG = inSlot(glossaries, glossaryIds), slotP = inSlot(pronouns, pronounIds);
        for (ChapterSetMatcher.Row r : rows) {
            r.glossaryCandidates.clear();
            r.pronounCandidates.clear();
            if (r.chapterNo >= 0) {
                for (ChapterSetMatcher.Named g : slotG) if (ChapterNumber.parse(g.name) == r.chapterNo) r.glossaryCandidates.add(g.id);
                for (ChapterSetMatcher.Named p : slotP) if (ChapterNumber.parse(p.name) == r.chapterNo) r.pronounCandidates.add(p.id);
            }
            boolean single = rows.size() == 1;
            r.numberOptional = single;
            r.glossaryOff = slotG.isEmpty();
            r.pronounOff = slotP.isEmpty();
            if (!r.glossaryChosen) r.glossaryId = single && slotG.size() == 1 ? slotG.get(0).id : r.glossaryCandidates.size() == 1 ? r.glossaryCandidates.get(0) : "";
            if (!r.pronounChosen) r.pronounId = single && slotP.size() == 1 ? slotP.get(0).id : r.pronounCandidates.size() == 1 ? r.pronounCandidates.get(0) : "";
            if (single && slotG.size() == 1) r.glossaryCandidates.add(slotG.get(0).id);
            if (single && slotP.size() == 1) r.pronounCandidates.add(slotP.get(0).id);
            dedupe(r.glossaryCandidates);
            dedupe(r.pronounCandidates);
        }
        ChapterSetMatcher.rate(rows);
        // The single-file rule needs no chapter number, so a lone pair with one candidate each is never red.
        unusedGlossaries.clear();
        unusedPronouns.clear();
        for (ChapterSetMatcher.Named g : slotG) if (!used(g.id, true)) unusedGlossaries.add(g.name);
        for (ChapterSetMatcher.Named p : slotP) if (!used(p.id, false)) unusedPronouns.add(p.name);
        if (nameAuto) {
            List<String> names = new ArrayList<>();
            for (ChapterSetMatcher.Row r : rows) names.add(r.rawName);
            name = names.isEmpty() ? "" : ChapterSetMatcher.defaultName(names);
        }
    }

    private boolean used(String id, boolean glossary) {
        for (ChapterSetMatcher.Row r : rows) if (!r.excluded && id.equals(glossary ? r.glossaryId : r.pronounId)) return true;
        return false;
    }

    private static void dedupe(List<String> l) {
        LinkedHashSet<String> s = new LinkedHashSet<>(l);
        l.clear();
        l.addAll(s);
    }

    private static List<ChapterSetMatcher.Named> inSlot(List<ChapterSetMatcher.Named> all, LinkedHashSet<String> ids) {
        List<ChapterSetMatcher.Named> out = new ArrayList<>();
        for (ChapterSetMatcher.Named n : all) if (ids.contains(n.id)) out.add(n);
        return out;
    }

    int included() {
        int n = 0;
        for (ChapterSetMatcher.Row r : rows) if (!r.excluded) n++;
        return n;
    }

    boolean canSave() { return included() > 0 && !ChapterSetMatcher.hasRed(rows); }

    /** Index of the first row that blocks saving, or -1. */
    int firstRedIndex() {
        for (int i = 0; i < rows.size(); i++) if (!rows.get(i).excluded && rows.get(i).status == ChapterSetMatcher.Status.RED) return i;
        return -1;
    }

    /** Status line shown in the list: "3 chương · ✓ 3 đủ" or "⚠ 1 thiếu …". */
    static String summaryOf(int total, int green, int yellow, int red) {
        StringBuilder sb = new StringBuilder(total + " chương · ");
        List<String> parts = new ArrayList<>();
        if (green > 0) parts.add("✓ " + green + " đủ");
        if (yellow > 0) parts.add("⚠ " + yellow + " thiếu");
        if (red > 0) parts.add("✕ " + red + " lỗi");
        sb.append(parts.isEmpty() ? "trống" : String.join(" · ", parts));
        return sb.toString();
    }

    /** "Ch.1–3" for consecutive chapters, "Ch.1, 3, 7" otherwise. */
    static String chapterRange(List<Integer> chapters) {
        List<Integer> c = new ArrayList<>();
        for (Integer x : chapters) if (x != null && x >= 0) c.add(x);
        java.util.Collections.sort(c);
        if (c.isEmpty()) return "";
        boolean consecutive = true;
        for (int i = 1; i < c.size(); i++) if (c.get(i) != c.get(i - 1) + 1) consecutive = false;
        if (c.size() == 1) return "Ch." + c.get(0);
        if (consecutive) return "Ch." + c.get(0) + "–" + c.get(c.size() - 1);
        StringBuilder sb = new StringBuilder("Ch.");
        for (int i = 0; i < c.size(); i++) sb.append(i == 0 ? "" : ", ").append(c.get(i));
        return sb.toString();
    }
}
