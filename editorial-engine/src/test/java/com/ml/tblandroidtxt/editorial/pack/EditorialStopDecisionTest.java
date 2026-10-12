package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EditorialStopDecisionTest {
    @Test public void inputMissingIsTypedAndHasRecovery() {
        EditorialStopDecision.Decision decision = EditorialStopDecision.inputRequired(
                "INPUT_DRAFT_FILE_MISSING", "L1_SOURCE_PREFLIGHT", "SOURCE_INPUTS",
                "chapter-1", "Select DRAFT source", "L1_SOURCE_PREFLIGHT");
        assertEquals(EditorialStopDecision.Disposition.STOP, decision.disposition());
        assertEquals(EditorialStopDecision.StopClass.INPUT_REQUIRED, decision.stopReceipt().stopClass());
        assertTrue(decision.stopReceipt().retryable());
        assertFalse(decision.reasonCode().equals("BLOCKED"));
    }

    @Test public void schemaFailureIsRepairNotContentConflict() {
        EditorialStopDecision.Decision decision = EditorialStopDecision.repairRequired(
                "REPAIR_RECEIPT_SCHEMA_INVALID", "L2_EDIT", "RECEIPT_SCHEMA",
                List.of("receipt-1"), "chapter-1", "Repair schema and revalidate once", "L2_EDIT");
        assertEquals(EditorialStopDecision.StopClass.REPAIR_REQUIRED, decision.stopReceipt().stopClass());
        assertTrue(decision.stopReceipt().retryable());
        assertTrue(EditorialStopDecision.repairAttemptAllowed(0));
        assertFalse(EditorialStopDecision.repairAttemptAllowed(1));
    }

    @Test public void truncatedOutputIsRetryAndContentConflictNeedsEvidence() {
        EditorialStopDecision.Decision retry = EditorialStopDecision.retryRequired(
                "RETRY_OUTPUT_TRUNCATED", "L2_EDIT", "OUTPUT_COMPLETE",
                "chapter-1", "Retry the same deterministic input", "L2_EDIT");
        assertEquals(EditorialStopDecision.StopClass.RETRY_REQUIRED, retry.stopReceipt().stopClass());
        assertTrue(retry.stopReceipt().retryable());

        EditorialStopDecision.Decision blocked = EditorialStopDecision.contentBlocked(
                "CONTENT_CONFLICT_PROVEN", "L1_RECONCILE", "SEMANTIC_FIDELITY",
                List.of("raw-anchor-4", "report-1"), "chapter-1", "Resolve cited conflict", "L1_RECONCILE");
        assertEquals(EditorialStopDecision.StopClass.CONTENT_BLOCKED, blocked.stopReceipt().stopClass());
        assertFalse(blocked.stopReceipt().retryable());
    }

    @Test public void uncertaintyPreservesDraftWithoutStopReceiptOrCanon() {
        EditorialStopDecision.Decision decision = EditorialStopDecision.preserveDraft(
                "PRESERVE_DRAFT_SEMANTIC_UNCERTAINTY", "L1_RECONCILE", "SEMANTIC_FIDELITY",
                List.of("raw-anchor-4"), "chapter-1", "Keep draft for explicit review", "L1_RECONCILE");
        assertEquals(EditorialStopDecision.Disposition.PRESERVE_DRAFT, decision.disposition());
        assertTrue(decision.isPreserveDraft());
        assertFalse(decision.isStop());
        assertTrue(decision.stopReceipt() == null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void contentBlockedWithoutEvidenceIsRejected() {
        EditorialStopDecision.contentBlocked("CONTENT_CONFLICT_PROVEN", "L1_RECONCILE",
                "SEMANTIC_FIDELITY", List.of(), "chapter-1", "Resolve conflict", "L1_RECONCILE");
    }
}
