package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairMaps;
import com.ml.tblandroidtxt.editorial.api.fix.FixPoint.Type;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * v4.19 X3: where the CP-IMPL-14 name-call rule contradicts owner decision D-A (2026-10-13). D-A: when a row's call is a name,
 * "cậu" stays a valid second-person pronoun if the row's self is "mình" or "tớ"; the name is required only at a direct vocative.
 * Rows with another self (tôi, em, ...) keep the old rule.
 *
 * <p>Tests named {@code knownDaContradiction...} pin what the shipped code (CP-IMPL-15) does today; the commit that implements
 * D-A must flip them. {@code control...} tests must keep passing. Invented names (Aki, Rina, Ken) only; no book text, no provider.
 */
public final class NameCallRuleDaContradictionTest {
    private static final String HEADER = "from,speaker,target,self,call,scope,note\n";
    /** Aki and Rina are friends (mình / name); Ken is addressed by Aki with tôi / Ken, a self outside D-A. */
    private static final String ROWS = "リナ,Aki,Rina,mình,Rina,*,x\nアキ,Rina,Aki,mình,Aki,*,x\nケン,Aki,Ken,tôi,Ken,*,x\n";
    private static final List<AddressRow> GUARD_ROWS = List.of(
            new AddressRow("リナ", "Aki", "Rina", "mình", "Rina"),
            new AddressRow("アキ", "Rina", "Aki", "mình", "Aki"));

    private static List<FixPoint> find(String raw, String draft) {
        PairMap map = PairMaps.fromJobRows("CH900", "raw", "draft", List.of(raw), List.of(draft));
        return FixPointFinder.find(map, map.entries().get(0), List.of(), HEADER + ROWS, null, null).points();
    }

    private static boolean flagsAddress(List<FixPoint> points) {
        return points.stream().anyMatch(p -> p.types().contains(Type.ADDRESS));
    }

    private static String guard(String draft, String raw, String candidate) {
        FixPoint point = new FixPoint(1, Set.of(Type.ADDRESS), 0, draft, raw, List.of("ADDRESS synthetic rule"), GUARD_ROWS);
        return AddressEditGuard.rejection(point, candidate);
    }

    // ---- finder: a D-A-valid "cậu" still becomes a point, so the model is asked to change it ----

    /**
     * Rina asks Aki a question without calling her by name: under D-A "cậu" is correct, yet the finder flags the line. The name
     * appears in the chunk as a topic (アキは), so the rows are projected by their cue, but the quoted line has no vocative.
     */
    @Test public void knownDaContradictionFinderFlagsCauOfAMinhRowOutsideAVocative() {
        assertTrue(flagsAddress(find("アキは振り向いた。「疲れてない？」", "Aki quay lại. 「Cậu không mệt à?」")));
    }

    /** The vocative case must stay a point after D-A: RAW calls Rina by name and the DRAFT has only "cậu" there. */
    @Test public void controlVocativeNameReplacedByCauIsFlagged() {
        assertTrue(flagsAddress(find("「リナ、疲れてない？」", "「Cậu không mệt à?」")));
    }

    // ---- prompt: the call-name rule has no self or vocative condition ----

    @Test public void knownDaContradictionPromptReplacesEveryAddressedCauWithTheCallName() {
        FixPoint point = new FixPoint(1, Set.of(Type.ADDRESS), 0, "「Cậu không mệt à?」", "「疲れてない？」",
                List.of("ADDRESS Rina -> Aki: self mình, call Aki"), GUARD_ROWS);
        String system = TargetedFixPrompt.build("Vietnamese", "", "「疲れてない？」", "", "", "「Cậu không mệt à?」", "", List.of(point)).system();
        assertTrue(system.contains("replace that word with the row's call name; do not keep the generic word the row replaces"));
        // D-A: no exception for self mình/tớ and no restriction to a direct vocative exists in the shipped rule
        assertFalse(system.contains("mình"));
        assertFalse(system.contains("tớ"));
        assertFalse(system.contains("only at a direct vocative"));
    }

    // ---- guard: "cậu" is not a form of a mình row, so a D-A-valid answer is refused ----

    /** The model replaces a generic "bạn" with "cậu" for Rina speaking to Aki: valid under D-A, refused by the guard today. */
    @Test public void knownDaContradictionGuardRefusesCauForAMinhRow() {
        assertEquals("ADDRESS_FORM_NOT_IN_APPLICABLE_ROW", guard("「Bạn không mệt à?」", "「疲れてない？」", "「Cậu không mệt à?」"));
    }

    /** Replacing "cậu" with the call name stays allowed after D-A (optional outside a vocative, required at one). */
    @Test public void controlCauToTheCallNameIsAccepted() {
        assertNull(guard("「Cậu không mệt à?」", "「疲れてない？」", "「Aki không mệt à?」"));
        assertNull(guard("「Cậu không mệt à?」", "「アキ、疲れてない？」", "「Aki không mệt à?」"));
    }

    /** Fixing a wrong self word while keeping "cậu" is accepted today and must stay accepted. */
    @Test public void controlSelfRepairThatKeepsCauIsAccepted() {
        assertNull(guard("「Cảm ơn cậu vì đã lo cho tớ」", "「心配してくれてありがと」", "「Cảm ơn cậu vì đã lo cho mình」"));
    }
}
