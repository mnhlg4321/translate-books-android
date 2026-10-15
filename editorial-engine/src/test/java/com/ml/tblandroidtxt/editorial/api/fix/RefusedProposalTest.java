package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.AddressChecklist;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * v4.22 (Kumo ch 013 line 151, 2026-10-15): the model returned the right line, the edit lock refused it because the row's call cell held
 * only the name while its note said "gọi em", and the line silently stayed as the DRAFT. A refused proposal is now kept for the person,
 * and a cell may carry several forms. Synthetic characters only (Mika, Yuki).
 */
public final class RefusedProposalTest {
    private static final String DRAFT = "「Yuki-chan, tôi biết cô mệt, nhưng cô chờ thêm chút nữa nhé?」";
    private static final String RAW = "「ユキちゃん、疲れたよね、でももう少し待ってね？」";
    private static final String RIGHT = "「Yuki-chan, chị biết em mệt, nhưng em chờ thêm chút nữa nhé?」";

    private static FixPoint point(String call) {
        return new FixPoint(1, Set.of(FixPoint.Type.ADDRESS), 0, DRAFT, RAW, List.of("ADDRESS synthetic rule"),
                List.of(new AddressRow("ユキちゃん", "Mika", "Yuki", "chị", call, "Mika thân mật gọi Yuki-chan; khi nói với Yuki xưng chị, gọi em.")));
    }

    private static TargetedFixApplier.Result apply(FixPoint point, String answer) {
        return TargetedFixApplier.apply(point.draftText(), List.of(point), TargetedFixParser.parse(answer, List.of(point)), List.of());
    }

    @Test public void aRefusedAddressRepairKeepsTheDraftButNotLosesTheProposal() {
        TargetedFixApplier.Result result = apply(point("Yuki-chan"), "[1] R1 " + RIGHT);
        TargetedFixApplier.Outcome outcome = result.outcomes().get(0);
        assertEquals(TargetedFixApplier.Status.KEPT, outcome.status());
        assertEquals("ADDRESS_FORM_NOT_IN_CITED_ROW", outcome.reason());
        assertEquals("the output is unchanged until a person applies it", DRAFT, result.text());
        assertEquals(RIGHT, outcome.proposed());
        assertEquals(1, outcome.citedRow());
    }

    @Test public void aCellWithSeveralFormsSeparatedBySlashAcceptsEachForm() {
        TargetedFixApplier.Result result = apply(point("Yuki-chan/em"), "[1] R1 " + RIGHT);
        assertEquals(TargetedFixApplier.Status.FIXED, result.outcomes().get(0).status());
        assertEquals(RIGHT, result.text());
        assertTrue(AddressChecklist.rowForms("chị", "Yuki-chan/em").containsAll(Set.of("chị", "yuki", "chan", "em")));
    }

    @Test public void theFinderTakesEachSlashFormAsAFormOfTheRowAndKeepsANameCellWhole() {
        assertTrue(AddressChecklist.unexpectedWords(DRAFT.replace("cô", "em").replace("tôi", "chị"), AddressChecklist.cellForms("chị", "Yuki-chan/em")).isEmpty());
        assertEquals(Set.of("tôi", "cô"), AddressChecklist.unexpectedWords(DRAFT, AddressChecklist.cellForms("chị", "Yuki-chan/em")));
        // "chị Erika" stays one form: a lone "chị" still raises the point that lets a surname become the call name
        assertEquals(Set.of("chị"), AddressChecklist.unexpectedWords("「Chị Kuragane là chị gái hả」", AddressChecklist.cellForms("em", "chị Erika")));
    }

    @Test public void aMalformedAnswerIsNeverOffered() {
        FixPoint point = point("Yuki-chan");
        TargetedFixApplier.Outcome tagged = apply(point, "[1] R1 <b>" + RIGHT + "</b>").outcomes().get(0);
        assertEquals("META_OR_TAG", tagged.reason());
        assertEquals("", tagged.proposed());
        assertFalse(TargetedFixApplier.offerable("NEW_KANA_OR_HAN"));
        assertTrue(TargetedFixApplier.offerable("ADDRESS_CITED_ROW_EXCLUDED"));
        assertTrue(TargetedFixApplier.offerable("GLOSSARY_TARGET_REMOVED"));
    }

    @Test public void applyingAProposalReplacesOnlyItsLineAfterEarlierInsertions() {
        String chunk = "Dòng một.\nDòng chèn.\n\n" + DRAFT + "\r\nDòng cuối.";
        // the point was found on DRAFT line 1 (0-based, blank lines not counted); one insertion was placed before it
        String applied = TargetedFixApplier.applyProposal(chunk, 1, 1, DRAFT, RIGHT);
        assertEquals("Dòng một.\nDòng chèn.\n\n" + RIGHT + "\r\nDòng cuối.", applied);
        // the stored line took the RAW's marks: same words, still found
        String marked = chunk.replace("nhé?」", "nhé？」");
        assertEquals("Dòng một.\nDòng chèn.\n\n" + RIGHT + "\r\nDòng cuối.", TargetedFixApplier.applyProposal(marked, 1, 1, DRAFT, RIGHT));
        // a line that no longer carries the DRAFT words is not overwritten
        assertNull(TargetedFixApplier.applyProposal(chunk, 1, 0, DRAFT, RIGHT));
        assertNull(TargetedFixApplier.applyProposal(chunk, 1, 1, DRAFT, RIGHT + "\nthêm"));
        assertNull(TargetedFixApplier.applyProposal(chunk, 9, 0, DRAFT, RIGHT));
    }
}
