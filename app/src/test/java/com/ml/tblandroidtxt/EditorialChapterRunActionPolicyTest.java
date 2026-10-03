package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialContractRevision;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class EditorialChapterRunActionPolicyTest {
    private static EditorialChapterFinalCoordinator.Inspection beforeL1() {
        return new EditorialChapterFinalCoordinator.Inspection(
                EditorialChapterProgress.derive(false, null, null), null, "NOT_APPLICABLE", "");
    }

    private static EditorialChapterFinalCoordinator.Inspection afterL1(String revision) {
        return new EditorialChapterFinalCoordinator.Inspection(
                EditorialChapterProgress.derive(true, null, null), null, "NOT_APPLICABLE", revision);
    }

    @Test public void missingL1IsTheOnlyStateThatStartsTheLedgerEntryPoint() {
        EditorialChapterFinalCoordinator.Inspection inspection = beforeL1();
        assertEquals(EditorialChapterRunActionPolicy.Action.START_FROM_L1,
                EditorialChapterRunActionPolicy.action(inspection));
        assertEquals("Chạy L1 → L2 → L3 (cần cấp phép)",
                EditorialChapterRunActionPolicy.buttonLabel(inspection));
    }

    @Test public void legacyL1OnlyOffersContinuationAndExplainsHowToStartContractV2() {
        EditorialChapterFinalCoordinator.Inspection inspection = afterL1(EditorialContractRevision.LEGACY_V1);
        assertEquals(EditorialChapterRunActionPolicy.Action.CONTINUE_L2_L3,
                EditorialChapterRunActionPolicy.action(inspection));
        assertEquals("Chạy tiếp L2 → L3 (cần cấp phép)",
                EditorialChapterRunActionPolicy.buttonLabel(inspection));
        assertTrue(EditorialChapterRunActionPolicy.legacyNotice(inspection)
                .contains("Tạo binding mới để chạy contract v2"));
    }

    @Test public void committedLedgerL1OnlyOffersL2L3Continuation() {
        EditorialChapterFinalCoordinator.Inspection inspection = afterL1(EditorialContractRevision.L1_LEDGER_V2);
        assertEquals(EditorialChapterRunActionPolicy.Action.CONTINUE_L2_L3,
                EditorialChapterRunActionPolicy.action(inspection));
        assertFalse(EditorialChapterRunActionPolicy.legacyL1(inspection));
        assertFalse(EditorialChapterRunActionPolicy.buttonLabel(inspection).contains("L1"));
    }

    @Test public void recoveryAndFinishedStatesDoNotOfferARunAction() {
        EditorialChapterFinalCoordinator.Inspection unknown = new EditorialChapterFinalCoordinator.Inspection(
                EditorialChapterProgress.derive(true,
                        new EditorialChapterProgress.StageRow("CLAIMED", "", false), null),
                null, "NOT_APPLICABLE", EditorialContractRevision.L1_LEDGER_V2);
        assertEquals(EditorialChapterRunActionPolicy.Action.NONE,
                EditorialChapterRunActionPolicy.action(unknown));
        assertEquals(EditorialChapterRunActionPolicy.Action.NONE,
                EditorialChapterRunActionPolicy.action(null));
    }

    @Test public void unreadableStateNeverOffersANewL1Run() {
        EditorialChapterFinalCoordinator.Inspection unreadable = new EditorialChapterFinalCoordinator.Inspection(
                EditorialChapterProgress.derive(false, null, null), null,
                "NOT_APPLICABLE", "", false);
        assertEquals(EditorialChapterRunActionPolicy.Action.NONE,
                EditorialChapterRunActionPolicy.action(unreadable));
        assertFalse(EditorialChapterRunActionPolicy.legacyL1(unreadable));
    }
}
