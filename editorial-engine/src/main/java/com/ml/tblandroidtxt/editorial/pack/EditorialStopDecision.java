package com.ml.tblandroidtxt.editorial.pack;

import java.util.List;
import java.util.Objects;

/** Typed run disposition and recovery receipt for SAFE4; independent of chapter state. */
public final class EditorialStopDecision {
    public enum StopClass { INPUT_REQUIRED, REPAIR_REQUIRED, RETRY_REQUIRED, CONTENT_BLOCKED }

    public enum Disposition { CONTINUE, PRESERVE_DRAFT, STOP }

    public record StopReceipt(StopClass stopClass, String reasonCode, String phase,
                              String blockingGate, List<String> evidenceRefs,
                              String affectedScope, String recoveryAction,
                              String resumeFrom, boolean retryable) {
        public StopReceipt {
            Objects.requireNonNull(stopClass, "stopClass");
            requireText(reasonCode, "reasonCode");
            requireText(phase, "phase");
            requireText(blockingGate, "blockingGate");
            evidenceRefs = List.copyOf(evidenceRefs == null ? List.of() : evidenceRefs);
            requireText(affectedScope, "affectedScope");
            requireText(recoveryAction, "recoveryAction");
            requireText(resumeFrom, "resumeFrom");
            if (stopClass == StopClass.CONTENT_BLOCKED && evidenceRefs.isEmpty()) {
                throw new IllegalArgumentException("CONTENT_BLOCKED requires evidence references");
            }
        }

        public boolean isTyped() { return stopClass != null; }
    }

    public record Decision(Disposition disposition, StopReceipt stopReceipt,
                           String reasonCode, String phase, String blockingGate,
                           List<String> evidenceRefs, String affectedScope,
                           String recoveryAction, String resumeFrom) {
        public Decision {
            Objects.requireNonNull(disposition, "disposition");
            requireText(reasonCode, "reasonCode");
            requireText(phase, "phase");
            requireText(blockingGate, "blockingGate");
            evidenceRefs = List.copyOf(evidenceRefs == null ? List.of() : evidenceRefs);
            requireText(affectedScope, "affectedScope");
            requireText(recoveryAction, "recoveryAction");
            requireText(resumeFrom, "resumeFrom");
            if (disposition == Disposition.STOP && stopReceipt == null) {
                throw new IllegalArgumentException("STOP requires a typed stop receipt");
            }
            if (disposition != Disposition.STOP && stopReceipt != null) {
                throw new IllegalArgumentException("Only STOP may carry a stop receipt");
            }
        }

        public boolean isStop() { return disposition == Disposition.STOP; }
        public boolean isPreserveDraft() { return disposition == Disposition.PRESERVE_DRAFT; }
    }

    private EditorialStopDecision() { }

    public static Decision inputRequired(String reasonCode, String phase, String gate,
                                         String affectedScope, String recoveryAction,
                                         String resumeFrom) {
        return stop(new StopReceipt(StopClass.INPUT_REQUIRED, reasonCode, phase, gate,
                List.of(), affectedScope, recoveryAction, resumeFrom, true));
    }

    public static Decision repairRequired(String reasonCode, String phase, String gate,
                                          List<String> evidenceRefs, String affectedScope,
                                          String recoveryAction, String resumeFrom) {
        return stop(new StopReceipt(StopClass.REPAIR_REQUIRED, reasonCode, phase, gate,
                evidenceRefs, affectedScope, recoveryAction, resumeFrom, true));
    }

    public static Decision retryRequired(String reasonCode, String phase, String gate,
                                         String affectedScope, String recoveryAction,
                                         String resumeFrom) {
        return stop(new StopReceipt(StopClass.RETRY_REQUIRED, reasonCode, phase, gate,
                List.of(), affectedScope, recoveryAction, resumeFrom, true));
    }

    public static Decision contentBlocked(String reasonCode, String phase, String gate,
                                          List<String> evidenceRefs, String affectedScope,
                                          String recoveryAction, String resumeFrom) {
        return stop(new StopReceipt(StopClass.CONTENT_BLOCKED, reasonCode, phase, gate,
                evidenceRefs, affectedScope, recoveryAction, resumeFrom, false));
    }

    public static Decision preserveDraft(String reasonCode, String phase, String gate,
                                         List<String> evidenceRefs, String affectedScope,
                                         String recoveryAction, String resumeFrom) {
        return new Decision(Disposition.PRESERVE_DRAFT, null, reasonCode, phase, gate,
                evidenceRefs, affectedScope, recoveryAction, resumeFrom);
    }

    public static Decision continueWithoutStop(String phase, String gate, String reasonCode) {
        return new Decision(Disposition.CONTINUE, null, reasonCode, phase, gate,
                List.of(), "NONE", "No recovery action required", phase);
    }

    public static boolean repairAttemptAllowed(int priorRepairAttempts) {
        return priorRepairAttempts >= 0 && priorRepairAttempts < 1;
    }

    private static Decision stop(StopReceipt receipt) {
        return new Decision(Disposition.STOP, receipt, receipt.reasonCode(), receipt.phase(),
                receipt.blockingGate(), receipt.evidenceRefs(), receipt.affectedScope(),
                receipt.recoveryAction(), receipt.resumeFrom());
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    }
}
