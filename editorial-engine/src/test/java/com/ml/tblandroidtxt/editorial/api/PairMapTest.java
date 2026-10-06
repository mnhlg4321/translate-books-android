package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** MAP-*, ID-*, SEP-01 and MERGE-* of the chunk-pair package: manifests, explicit pair map, boundary plan and merge. */
public final class PairMapTest {
    private static final List<String> RAW_ROWS = List.of("第一章\n\n太郎は扉を開けた。\n", "花子が言った。「行こう」\n\n", "◆\n\n次郎は笑った。");
    private static final List<String> DRAFT_ROWS = List.of("Chương một\n\nTaro mở cửa.", "Hanako nói: 「Đi thôi」\n\n", "◆\n\nJiro cười.\n");

    private static DocManifest raw(List<String> rows) { return DocManifest.fromRows(DocManifest.Kind.RAW, "raw-1", "CH001", rows, false); }

    private static DocManifest draft(List<String> rows) { return DocManifest.fromRows(DocManifest.Kind.DRAFT, "draft-1", "CH001", rows, true); }

    private static PairMap rowMap(DocManifest raw, DocManifest draft, int rows) {
        List<PairMap.Spec> specs = new ArrayList<>();
        for (int i = 0; i < rows; i++) specs.add(PairMap.Spec.one(raw.units().get(i).id(), draft.units().get(i).id(), "job-row"));
        return PairMap.build("map-1", raw, draft, specs);
    }

    private static PairMap standard() { return rowMap(raw(RAW_ROWS), draft(DRAFT_ROWS), 3); }

    private static Map<String, String> ownDraftTexts(PairMap map) {
        Map<String, String> out = new HashMap<>();
        for (PairMap.Entry e : map.entries()) out.put(e.pairId(), map.draftText(e));
        return out;
    }

    private static boolean has(List<DocManifest.Issue> issues, String code) {
        for (DocManifest.Issue i : issues) if (i.code().equals(code)) return true;
        return false;
    }

    // ---- MAP-RAW-01 / MAP-DRAFT-01

    @Test public void manifestsCoverTheNormalizedTextExactlyAndAreStable() {
        DocManifest a = raw(RAW_ROWS);
        DocManifest b = raw(RAW_ROWS);
        assertTrue(a.verify().isEmpty());
        assertEquals(a.textSha256, b.textSha256);
        assertEquals(3, a.units().size());
        assertEquals(a.units().get(1).id(), b.units().get(1).id());
        // whole text = edgeLead + units + boundaries + edgeTrail
        StringBuilder rebuilt = new StringBuilder(a.edgeLead());
        for (int i = 0; i < a.units().size(); i++) {
            rebuilt.append(a.main(a.units().get(i)));
            rebuilt.append(i + 1 < a.units().size() ? a.boundaryAfter(i) : a.edgeTrail());
        }
        assertEquals(a.text, rebuilt.toString());
    }

    @Test public void crlfAndLfAreOneDocumentAndUnicodeIsNormalizedToNfc() {
        List<String> crlf = new ArrayList<>();
        for (String r : RAW_ROWS) crlf.add(r.replace("\n", "\r\n"));
        assertEquals(raw(RAW_ROWS).textSha256, raw(crlf).textSha256);
        DocManifest decomposed = DocManifest.fromRows(DocManifest.Kind.DRAFT, "d", "", List.of("Viet̂ nam"), true);
        DocManifest composed = DocManifest.fromRows(DocManifest.Kind.DRAFT, "d", "", List.of("Việt nam".replace("Việt", "Việt")), true);
        assertEquals(PairText.normalize("Viet̂ nam"), decomposed.text.substring(0, decomposed.text.length() - 1));
        assertNotNull(composed.text);
    }

    @Test public void trailingNewlineFollowsTheJobOutputContractOnTheDraftSideOnly() {
        DocManifest d = draft(List.of("A", "B\n"));
        assertEquals("A\nB\n", d.text); // newline appended after a row that does not end with one
        DocManifest r = raw(List.of("A", "B\n"));
        assertEquals("AB\n", r.text); // RAW chunks are contiguous: no separator is invented
        assertEquals("\n", d.edgeTrail());
    }

    @Test public void emptyRowsHaveNoUnitAndParagraphsAreNumberedOverTheWholeChapter() {
        DocManifest d = draft(java.util.Arrays.asList("Một.\n\n", "   ", "Hai.\n\nBa.\n\n", null, "Bốn."));
        assertEquals(3, d.units().size());
        assertEquals(1, d.units().get(0).paragraphStart());
        assertEquals(1, d.units().get(0).paragraphEnd());
        assertEquals(2, d.units().get(1).paragraphStart());
        assertEquals(3, d.units().get(1).paragraphEnd());
        assertEquals(4, d.units().get(2).paragraphStart());
        assertEquals(4, d.paragraphCount());
        // rows joined by a single newline share a paragraph: numbering follows the text, not the row index
        DocManifest joined = draft(List.of("Một.", "Hai."));
        assertEquals(1, joined.units().get(1).paragraphStart());
        assertEquals(1, joined.paragraphCount());
    }

    @Test public void aWrongUnitHashIsAProvenanceIssue() {
        DocManifest d = draft(DRAFT_ROWS);
        List<DocManifest.Unit> units = new ArrayList<>(d.units());
        DocManifest.Unit u = units.get(1);
        units.set(1, new DocManifest.Unit(u.id(), u.start(), u.end(), u.paragraphStart(), u.paragraphEnd(), "0".repeat(64)));
        assertTrue(has(d.withUnits(units).verify(), "UNIT_HASH_MISMATCH"));
    }

    @Test public void nonWhitespaceBetweenUnitsIsACoverageGap() {
        DocManifest d = DocManifest.fromText(DocManifest.Kind.DRAFT, "d", "", "AAA\nXXX\nBBB", new int[][] {{0, 3}, {8, 11}});
        assertTrue(has(d.verify(), "COVERAGE_GAP"));
        DocManifest overlap = DocManifest.fromText(DocManifest.Kind.DRAFT, "d", "", "AAAABBBB", new int[][] {{0, 5}, {4, 8}});
        assertTrue(has(overlap.verify(), "UNIT_OVERLAP"));
    }

    // ---- MAP-PAIR-01/02, ID-01

    @Test public void anExplicitOneToOneMapVerifiesAndRoundTripsItsHash() {
        PairMap a = standard();
        PairMap b = standard();
        assertTrue(a.verify().isEmpty());
        assertEquals(a.mapHash(), b.mapHash());
        assertEquals(a.entries().get(0).pairId(), b.entries().get(0).pairId());
        assertEquals(3, a.entries().size());
    }

    @Test public void identityDoesNotDependOnTheDisplayOrdinalButChangesWithContentOrRevision() {
        PairMap a = standard();
        List<PairMap.Entry> reordered = new ArrayList<>();
        for (PairMap.Entry e : a.entries()) {
            reordered.add(new PairMap.Entry(e.pairId(), 100 - e.displayOrdinal(), e.rawUnitIds(), e.draftUnitIds(), e.reason(),
                    e.compositeDeclared(), e.rawInnerSeparators(), e.draftInnerSeparators()));
        }
        PairMap b = a.withEntries(reordered);
        assertEquals(a.mapHash(), b.mapHash());
        assertTrue(b.verify().isEmpty());
        List<String> changed = new ArrayList<>(DRAFT_ROWS);
        changed.set(1, "Hanako nói: 「Đi nào」\n\n");
        PairMap c = rowMap(raw(RAW_ROWS), draft(changed), 3);
        assertNotEquals(a.entries().get(1).pairId(), c.entries().get(1).pairId());
        assertEquals(a.entries().get(0).pairId().length(), 32);
        PairMap d = PairMap.build("map-2", raw(RAW_ROWS), draft(DRAFT_ROWS), List.of(
                PairMap.Spec.one(a.raw.units().get(0).id(), a.draft.units().get(0).id(), "x"),
                PairMap.Spec.one(a.raw.units().get(1).id(), a.draft.units().get(1).id(), "x"),
                PairMap.Spec.one(a.raw.units().get(2).id(), a.draft.units().get(2).id(), "x")));
        assertNotEquals(a.entries().get(0).pairId(), d.entries().get(0).pairId());
    }

    @Test public void aTamperedPairIdIsRejected() {
        PairMap a = standard();
        List<PairMap.Entry> es = new ArrayList<>(a.entries());
        PairMap.Entry e = es.get(0);
        es.set(0, new PairMap.Entry("f".repeat(32), e.displayOrdinal(), e.rawUnitIds(), e.draftUnitIds(), e.reason(), false, List.of(), List.of()));
        assertTrue(has(a.withEntries(es).verify(), "PAIR_ID_MISMATCH"));
    }

    @Test public void gapsMissingPairsReuseAndOrderAreTypedRejections() {
        DocManifest raw = raw(RAW_ROWS);
        DocManifest draft = draft(DRAFT_ROWS);
        // a RAW unit with no pair
        PairMap fewer = rowMap(raw, draft, 2);
        List<DocManifest.Issue> issues = fewer.verify();
        assertTrue(has(issues, "RAW_UNIT_UNMAPPED"));
        assertTrue(has(issues, "DRAFT_UNIT_UNMAPPED"));
        // a pair whose DRAFT row does not exist yet
        PairMap missing = PairMap.build("map-1", raw, draft, List.of(
                PairMap.Spec.one(raw.units().get(0).id(), draft.units().get(0).id(), "r"),
                PairMap.Spec.one(raw.units().get(1).id(), null, "r"),
                PairMap.Spec.one(raw.units().get(2).id(), draft.units().get(2).id(), "r")));
        assertTrue(has(missing.verify(), "MISSING_DRAFT"));
        assertEquals(1, missing.missingPairs().size());
        assertNull(missing.draftText(missing.missingPairs().get(0)));
        // the same DRAFT unit used twice
        PairMap reused = PairMap.build("map-1", raw, draft, List.of(
                PairMap.Spec.one(raw.units().get(0).id(), draft.units().get(0).id(), "r"),
                PairMap.Spec.one(raw.units().get(1).id(), draft.units().get(0).id(), "r"),
                PairMap.Spec.one(raw.units().get(2).id(), draft.units().get(2).id(), "r")));
        assertTrue(has(reused.verify(), "DRAFT_UNIT_REUSED"));
        // crossed order: RAW 0->DRAFT 1 and RAW 1->DRAFT 0
        PairMap crossed = PairMap.build("map-1", raw, draft, List.of(
                PairMap.Spec.one(raw.units().get(0).id(), draft.units().get(1).id(), "r"),
                PairMap.Spec.one(raw.units().get(1).id(), draft.units().get(0).id(), "r"),
                PairMap.Spec.one(raw.units().get(2).id(), draft.units().get(2).id(), "r")));
        assertTrue(has(crossed.verify(), "ORDER_MISMATCH"));
        // an unknown unit id
        PairMap unknown = PairMap.build("map-1", raw, draft, List.of(PairMap.Spec.one("nope", draft.units().get(0).id(), "r")));
        assertTrue(has(unknown.verify(), "RAW_UNIT_UNKNOWN"));
    }

    @Test public void aCompositePairNeedsExplicitDeclarationAndItsSeparators() {
        DocManifest raw = raw(RAW_ROWS);
        DocManifest draft = draft(DRAFT_ROWS);
        List<String> rawIds = List.of(raw.units().get(0).id(), raw.units().get(1).id());
        List<String> draftIds = List.of(draft.units().get(0).id(), draft.units().get(1).id());
        PairMap.Spec last = PairMap.Spec.one(raw.units().get(2).id(), draft.units().get(2).id(), "r");
        PairMap undeclared = PairMap.build("map-1", raw, draft, List.of(new PairMap.Spec(rawIds, draftIds, "r", false, List.of(), List.of()), last));
        assertTrue(has(undeclared.verify(), "COMPOSITE_UNDECLARED"));
        PairMap wrong = PairMap.build("map-1", raw, draft, List.of(new PairMap.Spec(rawIds, draftIds, "r", true, List.of("\n\n"), List.of("\n\n")), last));
        assertTrue(has(wrong.verify(), "COMPOSITE_SEPARATOR_MISMATCH"));
        PairMap ok = PairMap.build("map-1", raw, draft, List.of(new PairMap.Spec(rawIds, draftIds, "r", true,
                List.of(raw.boundaryAfter(0)), List.of(draft.boundaryAfter(0))), last));
        assertTrue(ok.verify().toString(), ok.verify().isEmpty());
        assertEquals(2, ok.entries().size());
        // the composite range spans both members including the separator between them
        assertTrue(ok.draftText(ok.entries().get(0)).contains(draft.boundaryAfter(0)));
        PairMap gapped = PairMap.build("map-1", raw, draft, List.of(new PairMap.Spec(List.of(raw.units().get(0).id(), raw.units().get(2).id()),
                List.of(draft.units().get(0).id(), draft.units().get(2).id()), "r", true, List.of(), List.of())));
        assertTrue(has(gapped.verify(), "COMPOSITE_NOT_CONTIGUOUS"));
    }

    // ---- SEP-01, MERGE-01/02

    @Test public void aNoOpMergeRestoresTheNormalizedDraftExactly() {
        PairMap map = standard();
        ChunkMerge.Result result = ChunkMerge.merge(map, ownDraftTexts(map), false);
        assertTrue(result.errors().toString(), result.ok());
        assertEquals(map.draft.text, result.text());
        assertEquals(PairText.sha256(map.draft.text), result.receipt().outputSha256());
        assertEquals(PairContract.REVISION, result.receipt().contractRevision());
        assertEquals(3, result.receipt().orderedPairIds().size());
        assertTrue(result.receipt().fallbackPairIds().isEmpty());
    }

    @Test public void everySeparatorBelongsToTheBoundaryPlanAndEditsLeaveItUntouched() {
        PairMap map = standard();
        ChunkMerge.BoundaryPlan plan = ChunkMerge.boundaryPlan(map);
        assertNotNull(plan);
        assertEquals(2, plan.boundaries().size());
        assertEquals("\n", plan.edgeTrail().substring(plan.edgeTrail().length() - 1));
        Map<String, String> edited = ownDraftTexts(map);
        String first = map.entries().get(0).pairId();
        edited.put(first, "Chương một (sửa)\n\nTaro mở cửa ra.");
        ChunkMerge.Result result = ChunkMerge.merge(map, edited, false);
        assertTrue(result.ok());
        assertTrue(result.text().contains("Taro mở cửa ra." + plan.boundaries().get(0) + "Hanako"));
        assertEquals(plan.hash(), result.receipt().boundaryPlanHash());
        // the plan is the same whatever the candidates say: the receipt hash does not move
        assertEquals(ChunkMerge.merge(map, ownDraftTexts(map), false).receipt().boundaryPlanHash(), result.receipt().boundaryPlanHash());
    }

    @Test public void aCandidateCarryingEdgeWhitespaceOrALabelIsRefusedNotTrimmed() {
        PairMap map = standard();
        Map<String, String> edited = ownDraftTexts(map);
        String id = map.entries().get(1).pairId();
        edited.put(id, edited.get(id) + "\n\n");
        ChunkMerge.Result trailing = ChunkMerge.merge(map, edited, false);
        assertFalse(trailing.ok());
        assertEquals("MERGE_CANDIDATE_NOT_TRIMMED", trailing.errors().get(0).code());
        edited.put(id, PairContract.paragraphLabel(2) + " Hanako nói");
        assertEquals("MERGE_LABEL_LEAK", ChunkMerge.merge(map, edited, false).errors().get(0).code());
        edited.put(id, "  ");
        assertEquals("MERGE_CANDIDATE_EMPTY", ChunkMerge.merge(map, edited, false).errors().get(0).code());
    }

    @Test public void missingAcceptedPairsDuplicatesAndUnknownIdsAreTypedMergeErrors() {
        PairMap map = standard();
        Map<String, String> partial = ownDraftTexts(map);
        String dropped = map.entries().get(2).pairId();
        partial.remove(dropped);
        ChunkMerge.Result strict = ChunkMerge.merge(map, partial, false);
        assertFalse(strict.ok());
        assertEquals("MERGE_PAIR_NOT_ACCEPTED", strict.errors().get(0).code());
        ChunkMerge.Result provisional = ChunkMerge.merge(map, partial, true);
        assertTrue(provisional.ok());
        assertEquals(List.of(dropped), provisional.receipt().fallbackPairIds());
        partial.put("not-a-pair", "x");
        assertEquals("MERGE_UNKNOWN_PAIR", ChunkMerge.merge(map, partial, true).errors().get(0).code());
    }

    @Test public void aMissingDraftPairBlocksTheMergePlan() {
        DocManifest raw = raw(RAW_ROWS);
        DocManifest draft = draft(DRAFT_ROWS);
        PairMap missing = PairMap.build("map-1", raw, draft, List.of(
                PairMap.Spec.one(raw.units().get(0).id(), draft.units().get(0).id(), "r"),
                PairMap.Spec.one(raw.units().get(1).id(), null, "r"),
                PairMap.Spec.one(raw.units().get(2).id(), draft.units().get(2).id(), "r")));
        assertNull(ChunkMerge.boundaryPlan(missing));
        ChunkMerge.Result result = ChunkMerge.merge(missing, new HashMap<>(), true);
        assertFalse(result.ok());
        assertEquals("MERGE_MAP_INVALID", result.errors().get(0).code());
    }

    @Test public void rawAndDraftCoverageAreCheckedSeparatelyAndContextIsNeverOutput() {
        PairMap map = standard();
        assertNotEquals(map.raw.coverageHash(), map.draft.coverageHash());
        ChunkMerge.Result result = ChunkMerge.merge(map, ownDraftTexts(map), false);
        assertEquals(map.raw.coverageHash(), result.receipt().rawCoverageHash());
        assertEquals(map.draft.coverageHash(), result.receipt().draftCoverageHash());
        // context is bounded reference text outside the main range and never reaches the merged output of a changed pair
        DocManifest.Unit middle = map.draft.units().get(1);
        String before = map.draft.contextBefore(middle.start(), 5);
        assertEquals(5, before.length());
        Map<String, String> edited = ownDraftTexts(map);
        edited.put(map.entries().get(1).pairId(), "ĐÃ SỬA");
        assertEquals(1, countOccurrences(ChunkMerge.merge(map, edited, false).text(), "ĐÃ SỬA"));
    }

    private static int countOccurrences(String text, String needle) {
        int n = 0;
        int at = 0;
        while ((at = text.indexOf(needle, at)) >= 0) { n++; at += needle.length(); }
        return n;
    }
}
