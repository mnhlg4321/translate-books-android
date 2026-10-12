package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairMaps;
import com.ml.tblandroidtxt.editorial.api.fix.FixPoint.Type;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * v4.19 characterization of the layer S1 gaps of the address finder, measured on the private 8-chapter corpus
 * (docs/V419_PRONOUN_S1_SUMMARY.json) and reproduced here with invented text. Tests named {@code knownGap...} pin what the shipped
 * finder does today and are expected to be flipped by the commit that closes the gap; {@code control...} tests must keep passing.
 * No book text, no provider.
 */
public final class PronounMissGapCharacterizationTest {
    private static final String HEADER = "from,speaker,target,self,call,scope,note\n";

    private static List<FixPoint> find(String raw, String draft, String rows) {
        PairMap map = PairMaps.fromJobRows("CH900", "raw", "draft", List.of(raw), List.of(draft));
        return FixPointFinder.find(map, map.entries().get(0), List.of(), HEADER + rows, null, null).points();
    }

    private static boolean flagsAddress(List<FixPoint> points) {
        return points.stream().anyMatch(p -> p.types().contains(Type.ADDRESS));
    }

    // ---- controls: what the finder already does right (the clean side of the suite) ----

    @Test public void controlLexiconWordOutsideTheRowIsFlagged() {
        assertTrue(flagsAddress(find("「AはBに言った。」", "「Tớ sẽ đi.」", "A,A,B,tôi,em,*,x\n")));
    }

    @Test public void controlLineThatUsesOnlyTheRowsOwnWordsIsNotFlagged() {
        assertFalse(flagsAddress(find("「AはBに言った。」", "「Tôi sẽ gặp em.」", "A,A,B,tôi,em,*,x\n")));
    }

    // ---- known gaps (layer S1): the finder is silent, so the model is never asked ----

    /** A row with a self word but no call word is skipped. The narrator row (@NARRATOR, self only) is of this kind. */
    @Test public void knownGapRowWithoutACallWordNeverProducesAPoint() {
        assertFalse(flagsAddress(find("「AはBに言った。」", "「Tớ sẽ đi.」", "A,A,B,tôi,,*,self only\n")));
    }

    /** The lexicon is fixed (AddressChecklist.WORDS): a first-person form outside it, here "mình", is invisible even when the row says "tôi". */
    @Test public void knownGapSelfFormOutsideTheFixedLexiconIsNotFlagged() {
        assertFalse(flagsAddress(find("「AはBに言った。」", "「Mình sẽ đi.」", "A,A,B,tôi,em,*,x\n")));
    }

    /** A wrong proper name or title where the row requires another call word is not an address word of the lexicon either. */
    @Test public void knownGapWrongNameOrTitleInsteadOfTheRowsCallIsNotFlagged() {
        assertFalse(flagsAddress(find("「AはBに言った。」", "「Hãy đi nào, Zeta.」", "A,A,B,tôi,Fuyu,*,x\n")));
        assertFalse(flagsAddress(find("「AはBに言った。」", "「Hãy đi nào, tiền bối Fuyu.」", "A,A,B,tôi,chị Fuyu,*,x\n")));
    }

    /** Allowed words used in the wrong role (the call word where the speaker's own self belongs) look correct to the finder. */
    @Test public void knownGapRowWordsSwappedBetweenSelfAndCallAreNotFlagged() {
        assertFalse(flagsAddress(find("「AはBに言った。」", "「Em sẽ gặp tôi.」", "A,A,B,tôi,em,*,x\n")));
    }
}
