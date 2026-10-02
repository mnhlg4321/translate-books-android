package com.ml.tblandroidtxt;

import java.util.Objects;

/**
 * Read-only progress of one chapter on the REPORT_L1 -> L2 -> L3 -> FINAL chain, derived only from
 * durable rows. It never claims, dispatches or repairs anything: a stage whose external state is
 * unknown (CLAIMED after a process death) or marked RECOVERY_REQUIRED is reported as such and is
 * never offered as an automatic retry. Pure Java so it is covered by JVM tests.
 */
public final class EditorialChapterProgress {
    public enum Stage { L1_INCOMPLETE, L2, L3, FINAL }

    public enum StageState { PENDING, IN_FLIGHT_UNKNOWN, RECOVERY_REQUIRED, COMMITTED, CORRUPT }

    public enum StopClass { NONE, INPUT_REQUIRED, REPAIR_REQUIRED, RETRY_REQUIRED, CONTENT_BLOCKED, BUDGET_EXCEEDED, UNCLASSIFIED }

    public enum NextAction {
        /** REPORT_L1 is not committed for this chapter; nothing to do from this panel. */
        L1_REQUIRED,
        /** The stage has never been attempted; running it needs an explicit user authorization. */
        RUN_STAGE_WITH_AUTHORIZATION,
        /** External state unknown or recovery required: no automatic retry exists. */
        OWNER_RECOVERY_DECISION,
        VIEW_AND_EXPORT
    }

    /** One {@code editorial_phase_artifacts} row as stored; {@code intact} = bytes re-hash to the recorded hashes. */
    public record StageRow(String status, String recoveryReasonCode, boolean intact) {
        public StageRow {
            Objects.requireNonNull(status, "status");
            recoveryReasonCode = recoveryReasonCode == null ? "" : recoveryReasonCode;
        }
    }

    public record Progress(Stage stage, StageState l2, StageState l3, String reasonCode, StopClass stopClass,
                    NextAction next, boolean finalReady) { }

    private EditorialChapterProgress() { }

    public static Progress derive(boolean l1Committed, StageRow l2, StageRow l3) {
        if (!l1Committed) {
            return new Progress(Stage.L1_INCOMPLETE, StageState.PENDING, StageState.PENDING,
                    "INPUT_REPORT_L1_NOT_COMMITTED", StopClass.INPUT_REQUIRED, NextAction.L1_REQUIRED, false);
        }
        StageState l2State = state(l2);
        StageState l3State = state(l3);
        if (l2State != StageState.COMMITTED) {
            // L3 is only ever attempted on a committed L2; a stray L3 row must not make the chain look further along.
            return blocked(Stage.L2, l2State, StageState.PENDING, l2);
        }
        if (l3State != StageState.COMMITTED) return blocked(Stage.L3, l2State, l3State, l3);
        return new Progress(Stage.FINAL, l2State, l3State, "FINAL_COMMITTED", StopClass.NONE,
                NextAction.VIEW_AND_EXPORT, true);
    }

    private static Progress blocked(Stage stage, StageState l2State, StageState l3State, StageRow row) {
        StageState current = stage == Stage.L2 ? l2State : l3State;
        String prefix = stage.name() + "_";
        switch (current) {
            case PENDING:
                return new Progress(stage, l2State, l3State, prefix + "NOT_STARTED", StopClass.NONE,
                        NextAction.RUN_STAGE_WITH_AUTHORIZATION, false);
            case IN_FLIGHT_UNKNOWN:
                return new Progress(stage, l2State, l3State, "RETRY_" + prefix + "CALL_STATE_UNKNOWN",
                        StopClass.RETRY_REQUIRED, NextAction.OWNER_RECOVERY_DECISION, false);
            case CORRUPT:
                return new Progress(stage, l2State, l3State, "RETRY_" + prefix + "COMMITTED_RESULT_UNAVAILABLE",
                        StopClass.RETRY_REQUIRED, NextAction.OWNER_RECOVERY_DECISION, false);
            default:
                String reason = row == null || row.recoveryReasonCode().isEmpty()
                        ? "STOP_" + prefix + "EXTERNAL_CALL_STATE_UNRESOLVED" : row.recoveryReasonCode();
                return new Progress(stage, l2State, l3State, reason, classify(reason),
                        NextAction.OWNER_RECOVERY_DECISION, false);
        }
    }

    static StageState state(StageRow row) {
        if (row == null) return StageState.PENDING;
        switch (row.status()) {
            case "COMMITTED": return row.intact() ? StageState.COMMITTED : StageState.CORRUPT;
            case "CLAIMED": return StageState.IN_FLIGHT_UNKNOWN;
            case "RECOVERY_REQUIRED": return StageState.RECOVERY_REQUIRED;
            default: return StageState.CORRUPT;
        }
    }

    /** Class of a stored recovery reason; the same prefixes the execution boundaries write. */
    static StopClass classify(String reason) {
        if (reason == null || reason.isEmpty()) return StopClass.UNCLASSIFIED;
        if (reason.contains("BUDGET")) return StopClass.BUDGET_EXCEEDED;
        if (reason.startsWith("INPUT")) return StopClass.INPUT_REQUIRED;
        if (reason.startsWith("REPAIR")) return StopClass.REPAIR_REQUIRED;
        if (reason.startsWith("RETRY") || reason.startsWith("STOP")) return StopClass.RETRY_REQUIRED;
        if (reason.startsWith("CONTENT")) return StopClass.CONTENT_BLOCKED;
        return StopClass.UNCLASSIFIED;
    }

    /** Vietnamese one-paragraph status for the chapter card; no model text, only fixed wording and codes. */
    public static String describe(Progress progress) {
        switch (progress.stage()) {
            case L1_INCOMPLETE:
                return "Editorial L1: chưa có REPORT_L1 đã commit cho chương này (" + progress.reasonCode() + ").";
            case FINAL:
                return "Editorial L1 ✓ • L2 ✓ • L3 ✓ — bản biên tập cuối đã lưu; có thể xem và xuất TXT.";
            default:
        }
        String done = progress.stage() == Stage.L2 ? "L1 ✓" : "L1 ✓ • L2 ✓";
        String stage = progress.stage().name();
        switch (progress.next()) {
            case RUN_STAGE_WITH_AUTHORIZATION:
                return "Editorial " + done + " • " + stage + " chưa chạy. Chạy cần cấp phép riêng (trần call/token/USD).";
            default:
                return "Editorial " + done + " • " + stage + " dừng: " + progress.reasonCode() + " (" + progress.stopClass().name()
                        + "). Trạng thái ngoài chưa xác định hoặc cần phục hồi; app không tự gọi lại.";
        }
    }
}
