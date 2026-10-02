package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ml.tblandroidtxt.EditorialChapterProgress.NextAction;
import com.ml.tblandroidtxt.EditorialChapterProgress.Progress;
import com.ml.tblandroidtxt.EditorialChapterProgress.Stage;
import com.ml.tblandroidtxt.EditorialChapterProgress.StageRow;
import com.ml.tblandroidtxt.EditorialChapterProgress.StageState;
import com.ml.tblandroidtxt.EditorialChapterProgress.StopClass;

public final class EditorialChapterProgressTest {
    private static StageRow committed() { return new StageRow("COMMITTED", "", true); }

    @Test public void withoutCommittedL1NothingElseIsOffered() {
        Progress p = EditorialChapterProgress.derive(false, committed(), committed());
        assertEquals(Stage.L1_INCOMPLETE, p.stage());
        assertEquals(NextAction.L1_REQUIRED, p.next());
        assertFalse(p.finalReady());
        assertEquals("INPUT_REPORT_L1_NOT_COMMITTED", p.reasonCode());
    }

    @Test public void freshChapterOffersL2OnlyWithAuthorization() {
        Progress p = EditorialChapterProgress.derive(true, null, null);
        assertEquals(Stage.L2, p.stage());
        assertEquals(StageState.PENDING, p.l2());
        assertEquals(NextAction.RUN_STAGE_WITH_AUTHORIZATION, p.next());
        assertEquals(StopClass.NONE, p.stopClass());
        assertTrue(EditorialChapterProgress.describe(p).contains("cấp phép"));
    }

    @Test public void committedL2OffersL3AndAStrayL3RowDoesNotAdvanceAnUnfinishedL2() {
        Progress p = EditorialChapterProgress.derive(true, committed(), null);
        assertEquals(Stage.L3, p.stage());
        assertEquals(NextAction.RUN_STAGE_WITH_AUTHORIZATION, p.next());

        Progress stray = EditorialChapterProgress.derive(true, new StageRow("CLAIMED", "", false), committed());
        assertEquals(Stage.L2, stray.stage());
        assertEquals(StageState.PENDING, stray.l3());
        assertFalse(stray.finalReady());
    }

    @Test public void bothStagesCommittedAndIntactMeansFinalReady() {
        Progress p = EditorialChapterProgress.derive(true, committed(), committed());
        assertEquals(Stage.FINAL, p.stage());
        assertTrue(p.finalReady());
        assertEquals(NextAction.VIEW_AND_EXPORT, p.next());
        assertEquals(StopClass.NONE, p.stopClass());
    }

    @Test public void claimedRowAfterProcessDeathIsUnknownStateNeverARetry() {
        Progress p = EditorialChapterProgress.derive(true, committed(), new StageRow("CLAIMED", "", false));
        assertEquals(Stage.L3, p.stage());
        assertEquals(StageState.IN_FLIGHT_UNKNOWN, p.l3());
        assertEquals("RETRY_L3_CALL_STATE_UNKNOWN", p.reasonCode());
        assertEquals(StopClass.RETRY_REQUIRED, p.stopClass());
        assertEquals(NextAction.OWNER_RECOVERY_DECISION, p.next());
        assertFalse(p.finalReady());
    }

    @Test public void recoveryRequiredKeepsTheStoredTypedReason() {
        Progress p = EditorialChapterProgress.derive(true,
                new StageRow("RECOVERY_REQUIRED", "REPAIR_L2_OUTPUT_SCHEMA_INVALID", false), null);
        assertEquals(Stage.L2, p.stage());
        assertEquals("REPAIR_L2_OUTPUT_SCHEMA_INVALID", p.reasonCode());
        assertEquals(StopClass.REPAIR_REQUIRED, p.stopClass());
        assertEquals(NextAction.OWNER_RECOVERY_DECISION, p.next());

        Progress blank = EditorialChapterProgress.derive(true, committed(), new StageRow("RECOVERY_REQUIRED", null, false));
        assertEquals("STOP_L3_EXTERNAL_CALL_STATE_UNRESOLVED", blank.reasonCode());
        assertEquals(StopClass.RETRY_REQUIRED, blank.stopClass());
    }

    @Test public void committedRowWhoseBytesNoLongerHashIsNeverFinal() {
        Progress p = EditorialChapterProgress.derive(true, committed(), new StageRow("COMMITTED", "", false));
        assertEquals(StageState.CORRUPT, p.l3());
        assertFalse(p.finalReady());
        assertEquals("RETRY_L3_COMMITTED_RESULT_UNAVAILABLE", p.reasonCode());
        assertEquals(NextAction.OWNER_RECOVERY_DECISION, p.next());

        assertEquals(StageState.CORRUPT, EditorialChapterProgress.state(new StageRow("SOMETHING_ELSE", "", true)));
    }

    @Test public void reasonCodesAreClassifiedLikeTheExecutionBoundariesWriteThem() {
        assertEquals(StopClass.BUDGET_EXCEEDED, EditorialChapterProgress.classify("L2_TOKEN_OR_COST_BUDGET_EXCEEDED"));
        assertEquals(StopClass.BUDGET_EXCEEDED, EditorialChapterProgress.classify("L3_TOKEN_OR_COST_BUDGET_EXCEEDED"));
        assertEquals(StopClass.INPUT_REQUIRED, EditorialChapterProgress.classify("INPUT_L2_BASE_INVALID"));
        assertEquals(StopClass.REPAIR_REQUIRED, EditorialChapterProgress.classify("REPAIR_L2_CHANGE_MAP_INVALID"));
        assertEquals(StopClass.RETRY_REQUIRED, EditorialChapterProgress.classify("RETRY_L2_OUTPUT_TRUNCATED"));
        assertEquals(StopClass.RETRY_REQUIRED, EditorialChapterProgress.classify("STOP_L2_EXTERNAL_CALL_STATE_UNRESOLVED"));
        assertEquals(StopClass.CONTENT_BLOCKED, EditorialChapterProgress.classify("CONTENT_CONFLICT"));
        assertEquals(StopClass.UNCLASSIFIED, EditorialChapterProgress.classify("model supplied text"));
        assertEquals(StopClass.UNCLASSIFIED, EditorialChapterProgress.classify(null));
    }

    @Test public void descriptionNeverContainsModelTextOnlyFixedWordingAndCodes() {
        Progress p = EditorialChapterProgress.derive(true, committed(), new StageRow("CLAIMED", "", false));
        String text = EditorialChapterProgress.describe(p);
        assertTrue(text, text.contains("RETRY_L3_CALL_STATE_UNKNOWN"));
        assertTrue(text, text.contains("không tự gọi lại"));
        assertTrue(EditorialChapterProgress.describe(EditorialChapterProgress.derive(true, committed(), committed()))
                .contains("xuất TXT"));    }
}
