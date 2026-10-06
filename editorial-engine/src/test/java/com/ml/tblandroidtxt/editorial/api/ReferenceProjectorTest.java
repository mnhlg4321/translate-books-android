package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** SCOPE-01…05 and the scope part of PROMPT-01: scope before cue, scope kept through dedupe, no inferred priority, W/C against an oracle. */
public final class ReferenceProjectorTest {
    private static final String HEADER = "from,speaker,target,self,call,scope,note\n";

    private static ReferenceProjector.Params main(int from, int to, String mainText, String before, String after) {
        return new ReferenceProjector.Params("CH004", from, to, mainText, before, after, null);
    }

    private static ReferenceProjector.Projection project(ReferenceProjector.Params p, String rows) {
        return ReferenceProjector.project(p, List.of(), HEADER + rows);
    }

    private static boolean dropped(ReferenceProjector.Projection pr, String reason) { return pr.count(reason) > 0; }

    // ---- SCOPE-01 / SCOPE-02

    @Test public void aRowOutsideTheMainParagraphsIsDroppedEvenWhenItsCueIsInTheContext() {
        ReferenceProjector.Params p = main(5, 6, "私は行く。 Basil", "Mercedes が来た。", "");
        ReferenceProjector.Projection pr = project(p, "Mercedes,Mercedes,Basil,ta,ngươi,p001-p004,context only scope\n");
        assertTrue(pr.pronouns().isEmpty());
        assertTrue(dropped(pr, "SCOPE_OUTSIDE_MAIN"));
    }

    @Test public void scopeIsCheckedBeforeTheCueSoABadScopeNeverReachesThePrompt() {
        ReferenceProjector.Params p = main(1, 3, "Mercedes と Basil", "", "");
        for (String bad : new String[] {"p000", "p5-p2", "CH4p1", "abc", "CHx:p1", "p1-", "q1"}) {
            ReferenceProjector.Projection pr = project(p, "Mercedes,Mercedes,Basil,ta,ngươi," + bad + ",x\n");
            assertTrue(bad, pr.pronouns().isEmpty());
            assertTrue(bad, dropped(pr, "SCOPE_INVALID"));
        }
    }

    @Test public void chapterPrefixMustMatchTheChapterAndFailsClosedWhenUnknown() {
        ReferenceProjector.Params p = main(1, 3, "Mercedes と Basil", "", "");
        assertEquals(1, project(p, "Mercedes,Mercedes,Basil,ta,ngươi,CH004:p1-p3,x\n").pronouns().size());
        assertEquals(1, project(p, "Mercedes,Mercedes,Basil,ta,ngươi,ch4:p2,x\n").pronouns().size());
        assertTrue(dropped(project(p, "Mercedes,Mercedes,Basil,ta,ngươi,CH005:p1-p3,x\n"), "SCOPE_CHAPTER_MISMATCH"));
        ReferenceProjector.Params unknown = new ReferenceProjector.Params("", 1, 3, "Mercedes と Basil", "", "", null);
        assertTrue(dropped(project(unknown, "Mercedes,Mercedes,Basil,ta,ngươi,CH004:p1-p3,x\n"), "SCOPE_CHAPTER_MISMATCH"));
        assertEquals(1, project(unknown, "Mercedes,Mercedes,Basil,ta,ngươi,p1-p3,x\n").pronouns().size());
    }

    @Test public void starAndEmptyScopeAreChapterWideAndAFullRowCarriesNoTag() {
        ReferenceProjector.Params p = main(4, 6, "Mercedes と Basil", "", "");
        ReferenceProjector.Projection pr = project(p, "Mercedes,Mercedes,Basil,ta,ngươi,*,a\nBasil,Basil,Mercedes,tôi,cô,,b\n");
        assertEquals(2, pr.pronouns().size());
        assertFalse(pr.hasPartial());
        for (String line : ReferenceProjector.renderPronounRows(pr)) assertFalse(line, line.contains("áp dụng"));
    }

    @Test public void aScopeThatCoversOnlyPartOfTheMainRangeIsPartialAndIsTaggedInTheSameNumbersAsTheLabels() {
        ReferenceProjector.Params p = main(3, 8, "Mercedes と Basil", "", "");
        ReferenceProjector.Projection pr = project(p, "Mercedes,Mercedes,Basil,ta,ngươi,p005-p006,a\nMercedes,Mercedes,Basil,ta,ngươi,p008-p012,a\n");
        assertEquals(1, pr.pronouns().size()); // same key: merged, scope accumulated
        ReferenceProjector.Row row = pr.pronouns().get(0);
        assertTrue(row.partial());
        assertEquals(2, row.effective().size());
        assertEquals("P005–P006, P008", ReferenceProjector.scopeText(row.effective()));
        String line = ReferenceProjector.renderPronounRows(pr).get(0);
        assertTrue(line, line.endsWith("[áp dụng đoạn P005–P006, P008]"));
        assertFalse("the raw scope cell is never shown to the model", line.contains("p005") || line.contains("p012"));
    }

    @Test public void adjacentAndOverlappingScopesMergeIntoOneRange() {
        ReferenceProjector.Params p = main(1, 12, "Mercedes と Basil", "", "");
        ReferenceProjector.Projection pr = project(p, "Mercedes,Mercedes,Basil,ta,ngươi,p001-p003,a\nMercedes,Mercedes,Basil,ta,ngươi,p004-p006,a\nMercedes,Mercedes,Basil,ta,ngươi,p005-p009,a\n");
        assertEquals(1, pr.pronouns().get(0).effective().size());
        assertEquals("P001–P009", ReferenceProjector.scopeText(pr.pronouns().get(0).effective()));
    }

    @Test public void cueInMainContextOnlyOrNowhere() {
        ReferenceProjector.Params p = main(1, 2, "本文に Basil だけ", "前に Mercedes", "後に Cain");
        ReferenceProjector.Projection pr = project(p,
                "Basil,Basil,Rex,ta,ngươi,*,main\n"
                + "Mercedes,Mercedes,Rex,tôi,cô,*,context\n"
                + "Cain,Cain,Rex,tao,mày,*,context after\n"
                + "Zed,Zed,Rex,a,b,*,nowhere\n");
        assertEquals(3, pr.pronouns().size());
        assertTrue(pr.pronouns().get(0).regions().contains("MAIN"));
        assertTrue(pr.pronouns().get(1).regions().contains("CONTEXT_ONLY"));
        assertTrue(pr.pronouns().get(2).regions().contains("CONTEXT_ONLY"));
        assertEquals(1, pr.count("NO_CUE_IN_REGION"));
        // a name that is only a target in the main text is a cue when target is a cue field, and not when only from is
        ReferenceProjector.Projection withTarget = project(p, "Zed,Zed,Basil,a,b,*,target in main\n");
        assertEquals(1, withTarget.pronouns().size());
        ReferenceProjector.Params fromOnly = new ReferenceProjector.Params("CH004", 1, 2, "本文に Basil だけ", "前に Mercedes", "後に Cain", Set.of("from"));
        assertEquals(0, project(fromOnly, "Zed,Zed,Basil,a,b,*,target in main\n").pronouns().size());
    }

    @Test public void aRowWithoutAnyCueIsGlobalAndScopeStillApplies() {
        ReferenceProjector.Params p = main(1, 2, "văn bản", "", "");
        ReferenceProjector.Projection pr = project(p, "*,*,*,,,*,quy tắc chung\n-,-,-,x,y,p009,ngoài\n");
        assertEquals(1, pr.pronouns().size());
        assertTrue(pr.pronouns().get(0).regions().contains("GLOBAL"));
        assertTrue(dropped(pr, "SCOPE_OUTSIDE_MAIN"));
    }

    @Test public void legacyThreeColumnRowsAreChapterWideAndKeyedByFrom() {
        ReferenceProjector.Params p = main(1, 2, "Mercedes がいた", "", "");
        ReferenceProjector.Projection pr = ReferenceProjector.project(p, List.of(), "Mercedes,Basil,ghi chú\nCain,Basil,khác\n");
        assertEquals(1, pr.pronouns().size());
        assertFalse(pr.pronouns().get(0).p3());
        assertFalse(pr.pronouns().get(0).partial());
        assertEquals("Mercedes,Basil,ghi chú", ReferenceProjector.renderPronounRows(pr).get(0));
    }

    @Test public void glossaryUsesOnlyTheMainTextAndIsNotLimited() {
        ReferenceProjector.Params p = main(1, 1, "勇者は魔王を倒した", "賢者が", "");
        List<EditInputs.GlossaryEntry> g = List.of(new EditInputs.GlossaryEntry("勇者", "Dũng giả", "", ""),
                new EditInputs.GlossaryEntry("魔王", "Ma vương", "", ""), new EditInputs.GlossaryEntry("賢者", "Hiền giả", "", ""),
                new EditInputs.GlossaryEntry("勇者", "Dũng giả", "", ""));
        ReferenceProjector.Projection pr = ReferenceProjector.project(p, g, "");
        assertEquals(2, pr.glossary().size());
    }

    // ---- SCOPE-03: dedupe key

    @Test public void rowsThatOnlyDifferInFromDoNotMerge() {
        ReferenceProjector.Params p = main(1, 20, "A と B", "", "");
        String rows = "A,S,T,ta,ngươi,p001-p010,n\nB,S,T,ta,ngươi,p011-p020,n\n";
        ReferenceProjector.Projection pr = project(p, rows);
        assertEquals(2, pr.pronouns().size());
        // what a key without from would lose: one row, one cue, the union scope
        ReferenceProjector.Projection merged = ReferenceProjector.project(p, List.of(), HEADER + rows, false);
        assertEquals(1, merged.pronouns().size());
        assertEquals("P001–P020", ReferenceProjector.scopeText(merged.pronouns().get(0).effective()));
    }

    // ---- SCOPE-04: conflicts keep every rule and rank none

    @Test public void disjointScopesAreNotAConflictAndEveryOverlapKindIsKeptWithBothRows() {
        ReferenceProjector.Params p = main(1, 20, "S と T", "", "");
        ReferenceProjector.Projection disjoint = project(p, "S,S,T,ta,ngươi,p001-p010,n\nS,S,T,tao,mày,p011-p020,n\n");
        assertEquals(2, disjoint.pronouns().size());
        assertTrue(disjoint.conflicts().isEmpty());
        String[][] cases = {
                {"p001-p012", "p008-p020"}, // partial overlap
                {"p005-p015", "p005-p015"}, // equal
                {"p001-p020", "p007-p009"}, // nested
        };
        for (String[] c : cases) {
            ReferenceProjector.Projection pr = project(p, "S,S,T,ta,ngươi," + c[0] + ",n\nS,S,T,tao,mày," + c[1] + ",n\n");
            assertEquals(c[0] + " " + c[1], 2, pr.pronouns().size());
            assertEquals(1, pr.conflicts().size());
            // no row is trimmed by the other: each keeps exactly its own scope
            List<int[]> a = ReferenceProjector.intersect(pr.pronouns().get(0).effective(), List.of(new int[] {1, 20}));
            assertEquals(pr.pronouns().get(0).effective().get(0)[0], a.get(0)[0]);
            assertEquals(pr.pronouns().get(0).effective().get(0)[1], a.get(0)[1]);
        }
        ReferenceProjector.Projection nested = project(p, "S,S,T,ta,ngươi,p001-p020,n\nS,S,T,tao,mày,p007-p009,n\n");
        assertEquals("the broad row is not reduced to make room for the narrow one", "P001–P020",
                ReferenceProjector.scopeText(nested.pronouns().get(0).effective()));
        assertEquals("P007–P009", ReferenceProjector.scopeText(nested.conflicts().get(0).overlap()));
        assertEquals("REFERENCE_CONFLICT đoạn P007–P009", ReferenceProjector.renderConflicts(nested).get(0));
    }

    // ---- SCOPE-04/05: W and C against an oracle of the unmerged rows

    private static final String CHAPTER_ROWS =
            "A,S,T,ta,ngươi,p001-p004,r1\n"            // cue A in paragraphs 1-2 only
            + "B,S,T,ta,ngươi,p005-p008,r1\n"          // same content as r1, cue B, other scope
            + "C,U,V,tôi,cậu,*,r3\n"                   // chapter-wide, cue C only in paragraph 6
            + "D,W,X,tao,mày,p003-p006,r4\n"           // cue D only in paragraph 2 (context of the pair 3-4)
            + "D,W,X,ta,ngươi,p004-p004,r5\n"          // conflict with r4 on paragraph 4
            + "E,Y,Z,a,b,p007-p008,r6\n";               // cue E in paragraph 8

    private static DocManifest chapter() {
        return DocManifest.fromRows(DocManifest.Kind.RAW, "raw", "CH004", List.of(
                "A một.\n\nA hai D.\n\n", "Ba.\n\nBốn.\n\n", "Năm.\n\nC sáu.\n\n", "Bảy.\n\nE tám.\n"), false);
    }

    /** (rowKey @ paragraph) for every unmerged row whose scope holds the paragraph and whose cue is in the cue text. */
    private static Set<String> oracle(String csv, int from, int to, String cueText, int firstPara, int lastPara) {
        Set<String> out = new TreeSet<>();
        for (String line : csv.split("\n")) {
            List<String> c = ReferenceFilter.cells(line);
            String key = String.join("|", c.get(0), c.get(1), c.get(2), c.get(3), c.get(4), c.get(6));
            if (!cueText.contains(c.get(0)) && !(cueText.contains(c.get(1)) && false)) continue;
            int lo = 1;
            int hi = 8;
            String scope = c.get(5);
            if (!scope.equals("*")) {
                String[] parts = scope.replace("p", "").split("-");
                lo = Integer.parseInt(parts[0]);
                hi = Integer.parseInt(parts[parts.length - 1]);
            }
            for (int p = Math.max(lo, from); p <= Math.min(hi, to); p++) if (p >= firstPara && p <= lastPara) out.add(key + "@" + p);
        }
        return out;
    }

    private static Set<String> observed(ReferenceProjector.Projection pr, int from, int to) {
        Set<String> out = new TreeSet<>();
        for (ReferenceProjector.Row r : pr.pronouns()) {
            for (int p = from; p <= to; p++) {
                if (r.covers(p)) out.add(String.join("|", r.from(), r.speaker(), r.target(), r.self(), r.call(), r.note()) + "@" + p);
            }
        }
        return out;
    }

    private static Set<String> conflictParagraphs(ReferenceProjector.Projection pr) {
        Set<String> out = new TreeSet<>();
        for (ReferenceProjector.Conflict c : pr.conflicts()) {
            for (int[] r : c.overlap()) for (int p = r[0]; p <= r[1]; p++) out.add(p + "");
        }
        return out;
    }

    @Test public void wholeChapterAndEveryPairApplyExactlyTheUnmergedRowsInTheirOwnCueUnits() {
        DocManifest raw = chapter();
        assertEquals(8, raw.paragraphCount());
        // W: one main range, cue anywhere in the chapter
        ReferenceProjector.Projection whole = ReferenceProjector.project(
                new ReferenceProjector.Params("CH004", 1, 8, raw.text, "", "", null), List.of(), HEADER + CHAPTER_ROWS);
        assertEquals(oracle(CHAPTER_ROWS, 1, 8, raw.text, 1, 8), observed(whole, 1, 8));
        Set<String> wholeSet = observed(whole, 1, 8);
        // C: one pair per unit, cue in main + bounded context of that pair
        for (int i = 0; i < raw.units().size(); i++) {
            DocManifest.Unit u = raw.units().get(i);
            String main = raw.main(u);
            String before = raw.contextBefore(u.start(), PairContract.CONTEXT_CHARS);
            String after = raw.contextAfter(u.end(), PairContract.CONTEXT_CHARS);
            ReferenceProjector.Projection pair = ReferenceProjector.project(
                    new ReferenceProjector.Params("CH004", u.paragraphStart(), u.paragraphEnd(), main, before, after, null), List.of(), HEADER + CHAPTER_ROWS);
            Set<String> expected = oracle(CHAPTER_ROWS, u.paragraphStart(), u.paragraphEnd(), main + "\n" + before + "\n" + after, u.paragraphStart(), u.paragraphEnd());
            assertEquals("pair " + i, expected, observed(pair, u.paragraphStart(), u.paragraphEnd()));
            // a pair never applies a rule to a paragraph that the whole chapter would not
            assertTrue("pair " + i, wholeSet.containsAll(observed(pair, u.paragraphStart(), u.paragraphEnd())));
            // a conflict seen by a pair is a conflict in the whole chapter on the same paragraphs
            for (String p : conflictParagraphs(pair)) assertTrue(conflictParagraphs(whole).contains(p));
        }
        assertEquals(Set.of("4"), conflictParagraphs(whole));
    }

    @Test public void theCounterExampleWithoutFromInTheKeyLosesARuleInTheWholeChapterThatTheChunkKeeps() {
        // two legacy rows about different people: the source name (from) is the only thing that tells them apart
        String legacy = "Mercedes,Basil,xưng ta\nCain,Basil,xưng ta\n";
        String both = "Mercedes xuất hiện.\n\nCain xuất hiện.";
        String onlyCain = "Cain xuất hiện.";
        ReferenceProjector.Params whole = new ReferenceProjector.Params("CH004", 1, 2, both, "", "", Set.of("from"));
        ReferenceProjector.Params pair = new ReferenceProjector.Params("CH004", 2, 2, onlyCain, "", "", Set.of("from"));
        // with from in the key: W keeps both rules and the pair that only has Cain keeps Cain's: they agree on paragraph 2
        ReferenceProjector.Projection w = ReferenceProjector.project(whole, List.of(), legacy);
        ReferenceProjector.Projection c = ReferenceProjector.project(pair, List.of(), legacy);
        assertEquals(2, w.pronouns().size());
        assertEquals(1, c.pronouns().size());
        assertTrue(ReferenceProjector.renderPronounRows(w).contains(ReferenceProjector.renderPronounRows(c).get(0)));
        // without from: W folds them into one row (the first), Cain's rule is gone from the whole-chapter prompt, the pair still has it
        ReferenceProjector.Projection wBad = ReferenceProjector.project(whole, List.of(), legacy, false);
        ReferenceProjector.Projection cBad = ReferenceProjector.project(pair, List.of(), legacy, false);
        assertEquals(1, wBad.pronouns().size());
        assertFalse(ReferenceProjector.renderPronounRows(wBad).contains(ReferenceProjector.renderPronounRows(cBad).get(0)));
    }

    @Test public void withScopedRowsADifferentFromIsKeptApartSoTheCueAndItsScopeStayTogether() {
        // R1 from=A scope p1-p10, R2 from=B scope p11-p20, everything else equal
        String rows = "A,S,T,ta,ngươi,p001-p010,n\nB,S,T,ta,ngươi,p011-p020,n\n";
        String firstHalf = "A " + "x\n\n".repeat(9) + "y";
        String secondHalf = "B " + "x\n\n".repeat(9) + "y";
        ReferenceProjector.Params whole = new ReferenceProjector.Params("CH004", 1, 20, firstHalf + "\n\n" + secondHalf, "", "", Set.of("from"));
        ReferenceProjector.Params pair = new ReferenceProjector.Params("CH004", 11, 20, secondHalf, "", "", Set.of("from"));
        ReferenceProjector.Projection w = ReferenceProjector.project(whole, List.of(), HEADER + rows);
        ReferenceProjector.Projection c = ReferenceProjector.project(pair, List.of(), HEADER + rows);
        assertEquals(observed(w, 11, 20), observed(c, 11, 20));
        assertTrue(observed(c, 11, 20).iterator().next().startsWith("B|"));
        // without from the two fold into one row that keeps the first cue (A) over p1-p20: the pair shows B, the whole chapter shows A
        ReferenceProjector.Projection wBad = ReferenceProjector.project(whole, List.of(), HEADER + rows, false);
        ReferenceProjector.Projection cBad = ReferenceProjector.project(pair, List.of(), HEADER + rows, false);
        assertEquals(1, wBad.pronouns().size());
        assertTrue(observed(wBad, 11, 20).iterator().next().startsWith("A|"));
        assertTrue(observed(cBad, 11, 20).iterator().next().startsWith("B|"));
        assertFalse(observed(wBad, 11, 20).equals(observed(cBad, 11, 20)));
    }

    @Test public void theProjectionIsIdenticalOnRepeatAndIgnoresRowOrderForTheSet() {
        ReferenceProjector.Params p = main(1, 8, "A D C E", "", "");
        ReferenceProjector.Projection a = project(p, CHAPTER_ROWS);
        ReferenceProjector.Projection b = project(p, CHAPTER_ROWS);
        assertEquals(ReferenceProjector.renderPronounRows(a), ReferenceProjector.renderPronounRows(b));
        List<String> reversed = new ArrayList<>(List.of(CHAPTER_ROWS.split("\n")));
        java.util.Collections.reverse(reversed);
        ReferenceProjector.Projection r = project(p, String.join("\n", reversed) + "\n");
        assertEquals(new TreeSet<>(ReferenceProjector.renderPronounRows(a)), new TreeSet<>(ReferenceProjector.renderPronounRows(r)));
    }
}
