package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;

/**
 * The confirmation table of a whole-volume run on the PC: one row per RAW chapter with the Glossary and Pronoun matched by chapter
 * number (the same matcher and rating as the phone's chapter sets). The screen reads the rows and edits them through this class;
 * nothing here touches the network or the stores.
 */
public final class VolumePlan {
    /** What the screen shows for one row. */
    public static final class RowView {
        public final int index;
        public final String rawId;
        public final String rawName;
        public final int chapterNo;
        public final String glossaryId;
        public final String glossaryLabel;
        public final String pronounId;
        public final String pronounLabel;
        /** GREEN, YELLOW or RED. */
        public final String status;
        public final String reason;
        public final boolean excluded;
        public final int glossaryCandidates;
        public final int pronounCandidates;

        RowView(int index, ChapterSetMatcher.Row r, String glossaryLabel, String pronounLabel) {
            this.index = index;
            this.rawId = r.rawId;
            this.rawName = r.rawName;
            this.chapterNo = r.chapterNo;
            this.glossaryId = r.glossaryId;
            this.glossaryLabel = glossaryLabel;
            this.pronounId = r.pronounId;
            this.pronounLabel = pronounLabel;
            this.status = r.status.name();
            this.reason = r.reason;
            this.excluded = r.excluded;
            this.glossaryCandidates = r.glossaryCandidates.size();
            this.pronounCandidates = r.pronounCandidates.size();
        }
    }

    final List<ChapterSetMatcher.Row> rows = new ArrayList<>();
    private final List<ChapterSetMatcher.Named> glossaryNames = new ArrayList<>();
    private final List<ChapterSetMatcher.Named> pronounNames = new ArrayList<>();
    final List<GlossaryStoreCore.Glossary> glossaries;
    final List<PronounStoreCore.Profile> pronouns;
    final String name;

    /** @param raws the chosen RAW chapters, already in chapter order */
    VolumePlan(List<DesktopRawStore.Item> raws, List<GlossaryStoreCore.Glossary> glossaries, List<PronounStoreCore.Profile> pronouns) {
        this.glossaries = glossaries;
        this.pronouns = pronouns;
        for (GlossaryStoreCore.Glossary g : glossaries) glossaryNames.add(new ChapterSetMatcher.Named(g.id, g.name));
        for (PronounStoreCore.Profile p : pronouns) pronounNames.add(new ChapterSetMatcher.Named(p.id, p.name));
        List<ChapterSetMatcher.Named> rawNames = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();
        List<String> originals = new ArrayList<>();
        for (DesktopRawStore.Item item : raws) {
            rawNames.add(new ChapterSetMatcher.Named(item.id, item.originalName));
            numbers.add(item.chapterNo);
            originals.add(item.originalName);
        }
        rows.addAll(ChapterSetMatcher.suggest(rawNames, numbers, glossaryNames, pronounNames));
        this.name = ChapterSetMatcher.defaultName(originals);
    }

    public String name() { return name; }

    public int size() { return rows.size(); }

    public RowView row(int index) {
        ChapterSetMatcher.Row r = rows.get(index);
        return new RowView(index, r, labelOfGlossary(r), labelOfPronoun(r));
    }

    public List<RowView> views() {
        List<RowView> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) out.add(row(i));
        return out;
    }

    public List<String[]> glossaryChoices() {
        List<String[]> out = new ArrayList<>();
        for (ChapterSetMatcher.Named n : glossaryNames) out.add(new String[]{n.id, n.name});
        return out;
    }

    public List<String[]> pronounChoices() {
        List<String[]> out = new ArrayList<>();
        for (ChapterSetMatcher.Named n : pronounNames) out.add(new String[]{n.id, n.name});
        return out;
    }

    private String labelOfGlossary(ChapterSetMatcher.Row r) {
        if (r.glossaryId.isEmpty()) return r.glossaryChosen ? "Không dùng" : "";
        for (ChapterSetMatcher.Named n : glossaryNames) if (n.id.equals(r.glossaryId)) return n.name;
        return "";
    }

    private String labelOfPronoun(ChapterSetMatcher.Row r) {
        if (r.pronounId.isEmpty()) return r.pronounChosen ? "Không dùng" : "";
        for (ChapterSetMatcher.Named n : pronounNames) if (n.id.equals(r.pronounId)) return n.name;
        return "";
    }

    // ---- edits ----

    /** The person types a chapter number: the suggestions for that row are recomputed unless they chose the profiles. */
    public void setChapterNo(int index, int chapterNo) {
        rows.get(index).chapterNo = chapterNo;
        ChapterSetMatcher.resuggest(rows, glossaryNames, pronounNames);
        ChapterSetMatcher.rate(rows);
    }

    /** An empty id means "Không dùng". */
    public void setGlossary(int index, String id) {
        ChapterSetMatcher.Row r = rows.get(index);
        r.glossaryId = id == null ? "" : id;
        r.glossaryChosen = true;
        ChapterSetMatcher.rate(rows);
    }

    public void setPronoun(int index, String id) {
        ChapterSetMatcher.Row r = rows.get(index);
        r.pronounId = id == null ? "" : id;
        r.pronounChosen = true;
        ChapterSetMatcher.rate(rows);
    }

    public void setExcluded(int index, boolean excluded) {
        rows.get(index).excluded = excluded;
        ChapterSetMatcher.rate(rows);
    }

    /** One Glossary for every included chapter (a shared file such as GLOSSARY_WORKING.csv). */
    public void useGlossaryForAll(String id) {
        for (ChapterSetMatcher.Row r : rows) if (!r.excluded) { r.glossaryId = id == null ? "" : id; r.glossaryChosen = true; }
        ChapterSetMatcher.rate(rows);
    }

    public void usePronounForAll(String id) {
        for (ChapterSetMatcher.Row r : rows) if (!r.excluded) { r.pronounId = id == null ? "" : id; r.pronounChosen = true; }
        ChapterSetMatcher.rate(rows);
    }

    /**
     * The answer to "some chapters have no Glossary/Pronoun": fill every empty slot with the profile selected in the Library, or
     * leave the slot unused. Only rows that are still unresolved change.
     */
    public void resolveMissing(String glossaryIdOrEmpty, String pronounIdOrEmpty) {
        for (ChapterSetMatcher.Row r : rows) {
            if (r.excluded) continue;
            if (r.glossaryId.isEmpty() && !r.glossaryChosen) { r.glossaryId = glossaryIdOrEmpty == null ? "" : glossaryIdOrEmpty; r.glossaryChosen = true; }
            if (r.pronounId.isEmpty() && !r.pronounChosen) { r.pronounId = pronounIdOrEmpty == null ? "" : pronounIdOrEmpty; r.pronounChosen = true; }
        }
        ChapterSetMatcher.rate(rows);
    }

    // ---- state ----

    public boolean hasRed() { return ChapterSetMatcher.hasRed(rows); }

    public int green() { return ChapterSetMatcher.count(rows, ChapterSetMatcher.Status.GREEN); }

    public int yellow() { return ChapterSetMatcher.count(rows, ChapterSetMatcher.Status.YELLOW); }

    public int red() { return ChapterSetMatcher.count(rows, ChapterSetMatcher.Status.RED); }

    public int included() {
        int n = 0;
        for (ChapterSetMatcher.Row r : rows) if (!r.excluded) n++;
        return n;
    }

    /** Rows that still have no Glossary or no Pronoun (the yellow ones). */
    public boolean hasMissingProfiles() { return yellow() > 0; }

    GlossaryStoreCore.Glossary glossary(String id) {
        for (GlossaryStoreCore.Glossary g : glossaries) if (g.id.equals(id)) return g;
        return null;
    }

    PronounStoreCore.Profile pronoun(String id) {
        for (PronounStoreCore.Profile p : pronouns) if (p.id.equals(id)) return p;
        return null;
    }

    /** The bindings of the included rows in order; the texts are copied now so later Library edits cannot change the run. */
    List<ChapterBinding> bindings() {
        List<ChapterBinding> out = new ArrayList<>();
        for (ChapterSetMatcher.Row r : rows) {
            if (r.excluded) continue;
            out.add(ChapterBinding.of(r.rawId, r.rawName, r.chapterNo, glossary(r.glossaryId), pronoun(r.pronounId)));
        }
        return out;
    }
}
