package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The explicit link between RAW units and DRAFT units. One entry binds one RAW range to one DRAFT range (or to a DRAFT range
 * that does not exist yet: a missing pair). Several units on one side form a composite pair only when the entry declares it
 * and lists the separators inside, so nothing is zipped, re-anchored or guessed by position, name or similarity. The identity
 * of a pair never depends on its display ordinal. Pure.
 */
public final class PairMap {
    public record Spec(List<String> rawUnitIds, List<String> draftUnitIds, String reason, boolean compositeDeclared,
                       List<String> rawInnerSeparators, List<String> draftInnerSeparators) {
        public Spec {
            rawUnitIds = List.copyOf(rawUnitIds);
            draftUnitIds = List.copyOf(draftUnitIds);
            reason = reason == null ? "" : reason;
            rawInnerSeparators = rawInnerSeparators == null ? List.of() : List.copyOf(rawInnerSeparators);
            draftInnerSeparators = draftInnerSeparators == null ? List.of() : List.copyOf(draftInnerSeparators);
        }

        public static Spec one(String rawUnitId, String draftUnitIdOrNull, String reason) {
            return new Spec(List.of(rawUnitId), draftUnitIdOrNull == null ? List.of() : List.of(draftUnitIdOrNull), reason, false, List.of(), List.of());
        }
    }

    public record Entry(String pairId, int displayOrdinal, List<String> rawUnitIds, List<String> draftUnitIds, String reason,
                        boolean compositeDeclared, List<String> rawInnerSeparators, List<String> draftInnerSeparators) {
        public boolean missingDraft() { return draftUnitIds.isEmpty(); }
    }

    public final String mapRevision;
    public final DocManifest raw;
    public final DocManifest draft;
    private final List<Entry> entries;

    private PairMap(String mapRevision, DocManifest raw, DocManifest draft, List<Entry> entries) {
        this.mapRevision = mapRevision == null ? "" : mapRevision;
        this.raw = raw;
        this.draft = draft;
        this.entries = List.copyOf(entries);
    }

    public static PairMap build(String mapRevision, DocManifest raw, DocManifest draft, List<Spec> specs) {
        List<Entry> built = new ArrayList<>();
        int ordinal = 1;
        for (Spec s : specs) {
            built.add(new Entry(pairIdOf(mapRevision, raw, draft, s.rawUnitIds(), s.draftUnitIds()), ordinal++, s.rawUnitIds(), s.draftUnitIds(),
                    s.reason(), s.compositeDeclared(), s.rawInnerSeparators(), s.draftInnerSeparators()));
        }
        return new PairMap(mapRevision, raw, draft, built);
    }

    /** Same entries with another display order: identity must not change (tests). */
    PairMap withEntries(List<Entry> replacement) { return new PairMap(mapRevision, raw, draft, replacement); }

    public List<Entry> entries() { return entries; }

    public Entry entry(String pairId) {
        for (Entry e : entries) if (e.pairId().equals(pairId)) return e;
        return null;
    }

    private static String pairIdOf(String revision, DocManifest raw, DocManifest draft, List<String> rawIds, List<String> draftIds) {
        int[] r = range(raw, rawIds);
        int[] d = range(draft, draftIds);
        return PairText.sha256(revision + "|" + raw.docId + "|" + draft.docId + "|" + String.join(",", rawIds) + "|" + String.join(",", draftIds)
                + "|" + r[0] + "-" + r[1] + "|" + d[0] + "-" + d[1]).substring(0, 32);
    }

    /** [start, end) from the first to the last unit named, or {-1, -1} when a unit is unknown or the list is empty. */
    private static int[] range(DocManifest doc, List<String> ids) {
        if (ids.isEmpty()) return new int[] {-1, -1};
        DocManifest.Unit first = doc.unit(ids.get(0));
        DocManifest.Unit last = doc.unit(ids.get(ids.size() - 1));
        if (first == null || last == null) return new int[] {-1, -1};
        return new int[] {first.start(), last.end()};
    }

    public int[] rawRange(Entry e) { return range(raw, e.rawUnitIds()); }

    /** {-1,-1} for a missing pair. */
    public int[] draftRange(Entry e) { return range(draft, e.draftUnitIds()); }

    public String rawText(Entry e) { int[] r = rawRange(e); return r[0] < 0 ? "" : raw.text.substring(r[0], r[1]); }

    /** The DRAFT text of the pair, or {@code null} when the pair is missing. */
    public String draftText(Entry e) { int[] r = draftRange(e); return r[0] < 0 ? null : draft.text.substring(r[0], r[1]); }

    public int rawParagraphStart(Entry e) { DocManifest.Unit u = raw.unit(e.rawUnitIds().get(0)); return u == null ? 0 : u.paragraphStart(); }

    public int rawParagraphEnd(Entry e) { DocManifest.Unit u = raw.unit(e.rawUnitIds().get(e.rawUnitIds().size() - 1)); return u == null ? 0 : u.paragraphEnd(); }

    public List<Entry> missingPairs() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) if (e.missingDraft()) out.add(e);
        return out;
    }

    /** Every defect that stops a pair from being sent or merged; empty = both sides are covered exactly and linked explicitly. */
    public List<DocManifest.Issue> verify() {
        List<DocManifest.Issue> issues = new ArrayList<>();
        for (DocManifest.Issue i : raw.verify()) issues.add(new DocManifest.Issue("RAW_" + i.code(), i.detail()));
        for (DocManifest.Issue i : draft.verify()) issues.add(new DocManifest.Issue("DRAFT_" + i.code(), i.detail()));
        Set<String> ids = new HashSet<>();
        Set<String> rawUsed = new HashSet<>();
        Set<String> draftUsed = new HashSet<>();
        int lastRaw = -1;
        int lastDraft = -1;
        for (Entry e : entries) {
            if (!ids.add(e.pairId())) issues.add(new DocManifest.Issue("DUPLICATE_PAIR", e.pairId()));
            if (!pairIdOf(mapRevision, raw, draft, e.rawUnitIds(), e.draftUnitIds()).equals(e.pairId())) {
                issues.add(new DocManifest.Issue("PAIR_ID_MISMATCH", e.pairId()));
            }
            checkSide(issues, "RAW", raw, e.rawUnitIds(), e, e.rawInnerSeparators(), rawUsed);
            if (e.missingDraft()) issues.add(new DocManifest.Issue("MISSING_DRAFT", e.pairId()));
            else checkSide(issues, "DRAFT", draft, e.draftUnitIds(), e, e.draftInnerSeparators(), draftUsed);
            int[] r = rawRange(e);
            int[] d = draftRange(e);
            if (r[0] >= 0) {
                if (r[0] < lastRaw) issues.add(new DocManifest.Issue("ORDER_MISMATCH", "raw " + e.pairId()));
                lastRaw = Math.max(lastRaw, r[0]);
            }
            if (d[0] >= 0) {
                if (d[0] < lastDraft) issues.add(new DocManifest.Issue("ORDER_MISMATCH", "draft " + e.pairId()));
                lastDraft = Math.max(lastDraft, d[0]);
            }
        }
        for (DocManifest.Unit u : raw.units()) if (!rawUsed.contains(u.id())) issues.add(new DocManifest.Issue("RAW_UNIT_UNMAPPED", u.id()));
        for (DocManifest.Unit u : draft.units()) if (!draftUsed.contains(u.id())) issues.add(new DocManifest.Issue("DRAFT_UNIT_UNMAPPED", u.id()));
        return issues;
    }

    private static void checkSide(List<DocManifest.Issue> issues, String side, DocManifest doc, List<String> unitIds, Entry e,
                                  List<String> declaredInner, Set<String> used) {
        if (unitIds.isEmpty()) {
            if (side.equals("RAW")) issues.add(new DocManifest.Issue("RAW_MISSING", e.pairId()));
            return;
        }
        int previous = -2;
        List<String> actualInner = new ArrayList<>();
        for (String id : unitIds) {
            int index = doc.indexOf(id);
            if (index < 0) { issues.add(new DocManifest.Issue(side + "_UNIT_UNKNOWN", id)); continue; }
            if (!used.add(id)) issues.add(new DocManifest.Issue(side + "_UNIT_REUSED", id));
            if (previous != -2) {
                if (index != previous + 1) issues.add(new DocManifest.Issue("COMPOSITE_NOT_CONTIGUOUS", e.pairId()));
                else actualInner.add(doc.boundaryAfter(previous));
            }
            previous = index;
        }
        if (unitIds.size() > 1) {
            if (!e.compositeDeclared()) issues.add(new DocManifest.Issue("COMPOSITE_UNDECLARED", e.pairId()));
            else if (!actualInner.equals(declaredInner)) issues.add(new DocManifest.Issue("COMPOSITE_SEPARATOR_MISMATCH", e.pairId()));
        }
    }

    /** Hash of everything that decides what a pair is: revision, both document hashes and every entry with its ranges. */
    public String mapHash() {
        StringBuilder sb = new StringBuilder(mapRevision).append('|').append(raw.textSha256).append('|').append(draft.textSha256);
        for (Entry e : entries) {
            int[] r = rawRange(e);
            int[] d = draftRange(e);
            sb.append('|').append(e.pairId()).append(':').append(r[0]).append('-').append(r[1]).append(':').append(d[0]).append('-').append(d[1]);
        }
        return PairText.sha256(sb.toString());
    }
}
