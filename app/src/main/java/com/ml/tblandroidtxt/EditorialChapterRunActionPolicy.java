package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialContractRevision;

/** Chooses a chapter-card action from persisted L1 contract state without dispatching work. */
final class EditorialChapterRunActionPolicy {
    enum Action { START_FROM_L1, CONTINUE_L2_L3, NONE }

    private EditorialChapterRunActionPolicy() { }

    static Action action(EditorialChapterFinalCoordinator.Inspection inspection) {
        if (inspection == null || !inspection.stateReadable() || inspection.progress() == null) return Action.NONE;
        return switch (inspection.progress().next()) {
            case L1_REQUIRED -> inspection.l1ContractRevision().isBlank() ? Action.START_FROM_L1 : Action.NONE;
            case RUN_STAGE_WITH_AUTHORIZATION -> inspection.l1ContractRevision().isBlank()
                    ? Action.NONE : Action.CONTINUE_L2_L3;
            default -> Action.NONE;
        };
    }

    static boolean legacyL1(EditorialChapterFinalCoordinator.Inspection inspection) {
        return inspection != null && inspection.stateReadable()
                && EditorialContractRevision.LEGACY_V1.equals(inspection.l1ContractRevision());
    }

    static String buttonLabel(EditorialChapterFinalCoordinator.Inspection inspection) {
        return action(inspection) == Action.START_FROM_L1
                ? "Chạy L1 → L2 → L3 (cần cấp phép)"
                : action(inspection) == Action.CONTINUE_L2_L3
                ? "Chạy tiếp L2 → L3 (cần cấp phép)" : "";
    }

    static String legacyNotice(EditorialChapterFinalCoordinator.Inspection inspection) {
        return legacyL1(inspection)
                ? "L1 của binding này thuộc chuỗi legacy. Tạo binding mới để chạy contract v2; lần chạy này chỉ tiếp tục L2/L3 theo contract đã lưu."
                : "";
    }
}
