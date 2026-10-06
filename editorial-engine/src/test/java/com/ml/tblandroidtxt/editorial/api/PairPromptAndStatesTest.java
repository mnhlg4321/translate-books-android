package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;
import com.ml.tblandroidtxt.editorial.api.PairStates.Recovery;
import com.ml.tblandroidtxt.editorial.api.PairStates.RunState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** PROMPT-01 (what a pair request contains), the pair/run state machine and the recovery of half-done pairs (RES-01, pure part). */
public final class PairPromptAndStatesTest {
    private static final List<String> RAW_ROWS = List.of(
            "Mở đầu RAW một.\n\nMercedes nói với Basil.\n\n", "Đoạn giữa RAW hai có Cain.\n\nĐoạn ba.\n\n", "Đoạn bốn RAW.\n\nĐoạn năm kết thúc.\n");
    private static final List<String> DRAFT_ROWS = List.of(
            "Bản nháp một.\n\nMercedes nói với Basil.\n\n", "Bản nháp hai có Cain.\n\nBản nháp ba.\n\n", "Bản nháp bốn.\n\nBản nháp năm kết thúc.\n");
    private static final String HEADER = "from,speaker,target,self,call,scope,note\n";

    private static PairMap map() {
        DocManifest raw = DocManifest.fromRows(DocManifest.Kind.RAW, "raw", "CH004", RAW_ROWS, false);
        DocManifest draft = DocManifest.fromRows(DocManifest.Kind.DRAFT, "draft", "CH004", DRAFT_ROWS, true);
        List<PairMap.Spec> specs = new ArrayList<>();
        for (int i = 0; i < 3; i++) specs.add(PairMap.Spec.one(raw.units().get(i).id(), draft.units().get(i).id(), "job-row"));
        return PairMap.build("m1", raw, draft, specs);
    }

    private static final List<EditInputs.GlossaryEntry> GLOSSARY = List.of(
            new EditInputs.GlossaryEntry("Mercedes", "Mercedes", "name", ""), new EditInputs.GlossaryEntry("Cain", "Cain", "name", ""));

    // ---- PROMPT-01

    @Test public void aPairRequestHasItsOwnMainTextAndOnlyBoundedLabelledContext() {
        PairMap map = map();
        ApiPrompt p = PairPromptBuilder.buildPair(map, map.entries().get(1), "Vietnamese", GLOSSARY, "", null);
        assertTrue(p.user().contains("# RAW (this part)\nĐoạn giữa RAW hai có Cain."));
        assertTrue(p.user().contains("# DRAFT (this part - return the edited version of exactly this text)\nBản nháp hai có Cain."));
        assertTrue(p.user().contains("# RAW CONTEXT BEFORE (REFERENCE ONLY"));
        assertTrue(p.user().contains("# RAW CONTEXT AFTER (REFERENCE ONLY"));
        assertTrue(p.user().contains("# DRAFT CONTEXT BEFORE (REFERENCE ONLY"));
        // the mains of the other pairs appear only inside the labelled context windows, never as a part to return
        assertFalse(p.user().contains("# RAW (this part)\nMở đầu"));
        assertTrue(p.system().contains("ONE part of a longer chapter"));
        assertTrue(p.system().contains("<EDITED>"));
        assertEquals(EditorialApiContract.Step.EDIT, p.step());
        assertEquals(QualityCore.editSha256(), p.qualityCoreSha256());
    }

    @Test public void theFirstAndLastPairHaveContextOnOneSideOnly() {
        PairMap map = map();
        ApiPrompt first = PairPromptBuilder.buildPair(map, map.entries().get(0), "Vietnamese", List.of(), "", null);
        assertFalse(first.user().contains("RAW CONTEXT BEFORE"));
        assertTrue(first.user().contains("RAW CONTEXT AFTER"));
        ApiPrompt last = PairPromptBuilder.buildPair(map, map.entries().get(2), "Vietnamese", List.of(), "", null);
        assertTrue(last.user().contains("RAW CONTEXT BEFORE"));
        assertFalse(last.user().contains("RAW CONTEXT AFTER"));
    }

    @Test public void theWholeChapterIsOnePseudoPairWithTheSamePolicyAndNoContext() {
        PairMap map = map();
        ApiPrompt w = PairPromptBuilder.buildWhole(map, "Vietnamese", GLOSSARY, "", null);
        assertTrue(w.user().startsWith("# RAW\nMở đầu RAW một."));
        assertTrue(w.user().contains("Đoạn năm kết thúc."));
        assertTrue(w.user().contains("# DRAFT\nBản nháp một."));
        assertFalse(w.user().contains("REFERENCE ONLY"));
        assertTrue(w.system().contains("whole chapter"));
        assertEquals(2, w.glossaryEntries());
    }

    @Test public void glossaryComesFromTheMainTextOnlyAndNoReferenceIsAValidConfiguration() {
        PairMap map = map();
        ApiPrompt middle = PairPromptBuilder.buildPair(map, map.entries().get(1), "Vietnamese", GLOSSARY, "", null);
        assertEquals(1, middle.glossaryEntries()); // Cain is in the main text of pair 2; Mercedes only in pair 1
        assertTrue(middle.system().contains("Cain | Cain"));
        assertFalse(middle.system().contains("Mercedes | Mercedes"));
        ApiPrompt none = PairPromptBuilder.buildPair(map, map.entries().get(1), "Vietnamese", List.of(), "", null);
        assertEquals(0, none.glossaryEntries());
        assertEquals(0, none.pronounRows());
        assertTrue(none.system().contains(EditPromptBuilder.NO_REFERENCE));
        // pronouns are never made from the glossary
        assertFalse(none.system().contains("# PRONOUNS"));
    }

    @Test public void aFullRowCarriesNoTagAndNoLabelsButAPartialRowTagsItselfAndLabelsTheRawParagraphs() {
        PairMap map = map();
        PairMap.Entry second = map.entries().get(1); // RAW paragraphs 3-4
        assertEquals(3, map.rawParagraphStart(second));
        assertEquals(4, map.rawParagraphEnd(second));
        String full = HEADER + "Cain,Cain,Basil,ta,ngươi,*,đầy đủ\n";
        ApiPrompt a = PairPromptBuilder.buildPair(map, second, "Vietnamese", List.of(), full, null);
        assertEquals(1, a.pronounRows());
        assertFalse(a.system().contains("áp dụng đoạn"));
        assertFalse(a.user().contains(PairContract.LABEL_OPEN));
        String partial = HEADER + "Cain,Cain,Basil,ta,ngươi,p004-p004,một phần\n";
        ApiPrompt b = PairPromptBuilder.buildPair(map, second, "Vietnamese", List.of(), partial, null);
        assertTrue(b.system().contains("[áp dụng đoạn P004]"));
        assertTrue(b.user().contains(PairContract.paragraphLabel(3) + " Đoạn giữa RAW hai có Cain."));
        assertTrue(b.user().contains(PairContract.paragraphLabel(4) + " Đoạn ba."));
        // labels exist only in the RAW section; the DRAFT section and the raw scope cell never reach the model
        String draftSection = b.user().substring(b.user().indexOf("# DRAFT (this part"));
        assertFalse(draftSection.contains(PairContract.LABEL_OPEN));
        assertFalse(b.system().contains("p004-p004"));
        String pronounSection = b.system().substring(b.system().indexOf("# PRONOUNS"));
        assertFalse(pronounSection.contains("scope"));
    }

    @Test public void conflictingRowsAreBothShownWithTheirScopeAndTheModelIsToldNotToChoose() {
        PairMap map = map();
        String csv = HEADER + "Cain,Cain,Basil,ta,ngươi,p003-p004,r1\nCain,Cain,Basil,tao,mày,p004-p005,r2\n";
        ApiPrompt p = PairPromptBuilder.buildPair(map, map.entries().get(1), "Vietnamese", List.of(), csv, null);
        assertEquals(2, p.pronounRows());
        assertTrue(p.system().contains("ta | ngươi"));
        assertTrue(p.system().contains("tao | mày"));
        assertTrue(p.system().contains("REFERENCE_CONFLICT đoạn P004"));
        assertTrue(p.system().contains("Do not choose between them"));
    }

    @Test public void aMissingDraftCannotBecomeARequestAndLabelCountMustAddUp() {
        DocManifest raw = DocManifest.fromRows(DocManifest.Kind.RAW, "raw", "CH004", RAW_ROWS, false);
        DocManifest draft = DocManifest.fromRows(DocManifest.Kind.DRAFT, "draft", "CH004", DRAFT_ROWS, true);
        PairMap missing = PairMap.build("m1", raw, draft, List.of(PairMap.Spec.one(raw.units().get(0).id(), null, "job-row")));
        try {
            PairPromptBuilder.buildPair(missing, missing.entries().get(0), "Vietnamese", List.of(), "", null);
            throw new AssertionError("a pair without a DRAFT must not be sent");
        } catch (PairPromptBuilder.PromptException expected) {
            assertEquals("PAIR_MISSING_DRAFT", expected.code);
        }
        try {
            PairPromptBuilder.labelled("Một.\n\nHai.", 3, 9);
            throw new AssertionError("paragraph numbers that do not add up must not be labelled");
        } catch (PairPromptBuilder.PromptException expected) {
            assertEquals("LABEL_COUNT_MISMATCH", expected.code);
        }
        assertEquals(PairContract.paragraphLabel(3) + " Một.\n\n" + PairContract.paragraphLabel(4) + " Hai.", PairPromptBuilder.labelled("Một.\n\nHai.", 3, 4));
    }

    // ---- states

    @Test public void onlyTheDocumentedMovesAreAllowed() {
        assertTrue(PairStates.canMove(PairState.IMPORTED, PairState.E_RESERVED));
        assertTrue(PairStates.canMove(PairState.IMPORTED, PairState.RESERVE_FAILED));
        assertTrue(PairStates.canMove(PairState.E_RESERVED, PairState.E_SENT));
        assertTrue(PairStates.canMove(PairState.E_RESERVED, PairState.IMPORTED));
        assertTrue(PairStates.canMove(PairState.E_SENT, PairState.E_RECEIVED));
        assertTrue(PairStates.canMove(PairState.E_SENT, PairState.UNKNOWN));
        assertTrue(PairStates.canMove(PairState.E_RECEIVED, PairState.ACCEPTED));
        assertTrue(PairStates.canMove(PairState.WARN_REVIEW, PairState.REJECTED));
        assertFalse(PairStates.canMove(PairState.E_SENT, PairState.IMPORTED)); // a sent request is never taken back
        assertFalse(PairStates.canMove(PairState.UNKNOWN, PairState.E_RESERVED)); // unknown never resends
        assertFalse(PairStates.canMove(PairState.STRUCTURE_BLOCKED, PairState.ACCEPTED));
        assertFalse(PairStates.canMove(PairState.ACCEPTED, PairState.E_RESERVED));
        for (PairState s : new PairState[] {PairState.ACCEPTED, PairState.REJECTED, PairState.STRUCTURE_BLOCKED, PairState.UNKNOWN, PairState.MISSING, PairState.RESERVE_FAILED}) {
            assertTrue(s.name(), PairStates.terminal(s));
        }
        assertEquals(PairState.STRUCTURE_BLOCKED, PairStates.afterReceive(StructuralGate.Status.BLOCK));
        assertEquals(PairState.WARN_REVIEW, PairStates.afterReceive(StructuralGate.Status.WARN));
        assertEquals(PairState.ACCEPTED, PairStates.afterReceive(StructuralGate.Status.PASS));
        assertTrue(PairStates.mergeable(PairState.WARN_REVIEW) && PairStates.mergeable(PairState.ACCEPTED));
        assertFalse(PairStates.mergeable(PairState.STRUCTURE_BLOCKED));
    }

    @Test public void theRunStateComesFromThePairsAndWarningsNeverMakeItBlockedOrClean() {
        assertEquals(RunState.PREPARED, PairStates.derive(List.of(PairState.IMPORTED, PairState.IMPORTED), false, false, false));
        assertEquals(RunState.RUNNING, PairStates.derive(List.of(PairState.ACCEPTED, PairState.E_SENT), true, true, false));
        assertEquals(RunState.INCOMPLETE, PairStates.derive(List.of(PairState.ACCEPTED, PairState.IMPORTED), true, false, false));
        assertEquals(RunState.PAUSED, PairStates.derive(List.of(PairState.ACCEPTED, PairState.IMPORTED), true, false, true));
        assertEquals(RunState.INCOMPLETE, PairStates.derive(List.of(PairState.ACCEPTED, PairState.RESERVE_FAILED), true, false, false));
        assertEquals(RunState.INCOMPLETE, PairStates.derive(List.of(PairState.ACCEPTED, PairState.MISSING), true, false, false));
        assertEquals(RunState.UNKNOWN, PairStates.derive(List.of(PairState.ACCEPTED, PairState.UNKNOWN, PairState.IMPORTED), true, false, false));
        assertEquals(RunState.FINAL_BLOCKED, PairStates.derive(List.of(PairState.ACCEPTED, PairState.STRUCTURE_BLOCKED), true, false, false));
        assertEquals(RunState.FINAL_ELIGIBLE, PairStates.derive(List.of(PairState.ACCEPTED, PairState.WARN_REVIEW), true, false, false));
        assertEquals(1, PairStates.warnings(List.of(PairState.ACCEPTED, PairState.WARN_REVIEW)));
        assertEquals(RunState.PREPARED, PairStates.derive(List.of(), false, false, false));
    }

    // ---- RES-01 (pure): what recovery does in each window

    @Test public void recoveryWindowsFollowTheReservationJournalOrder() {
        // died after RESERVE but before the journal knew: the reservation is orphaned
        assertEquals(Recovery.MARK_UNKNOWN, PairStates.recover(PairState.IMPORTED, true));
        // nothing reserved: nothing to do
        assertEquals(Recovery.NONE, PairStates.recover(PairState.IMPORTED, false));
        // journalled as reserved, never marked sent: provably not dispatched
        assertEquals(Recovery.SETTLE_ZERO_NOT_DISPATCHED, PairStates.recover(PairState.E_RESERVED, true));
        // marked sent: may have been billed, never resent
        assertEquals(Recovery.MARK_UNKNOWN, PairStates.recover(PairState.E_SENT, true));
        // a finished pair with an open reservation breaks the transaction rule: unknown, not clean
        assertEquals(Recovery.MARK_UNKNOWN, PairStates.recover(PairState.ACCEPTED, true));
        assertEquals(Recovery.NONE, PairStates.recover(PairState.ACCEPTED, false));
        assertEquals(Recovery.NONE, PairStates.recover(PairState.UNKNOWN, true));
        assertNotNull(PairContract.REVISION);
    }
}
