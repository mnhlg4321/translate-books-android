package com.ml.tblandroidtxt;

import java.util.EnumSet;
import java.util.Set;

/**
 * Machine-enforced lifecycle for the V5 editorial workflow. This class is intentionally
 * independent of translation jobs: a chapter must retain its evidence and context barriers.
 */
public final class EditorialWorkflowV5 {
    public enum AssetRole { RAW, DRAFT, GLOSSARY, PRONOUN, REPORT_L1, VI_L2, FINAL_QA }
    public enum Gate { COVERAGE, FIDELITY, REFERENCE_VOICE, CONTINUITY_STRUCTURE, CHANGE }
    public enum GateStatus { OPEN, CLOSED, NOT_APPLICABLE }
    public enum ChapterState {
        DRAFT_INPUT,
        L1_READY, L1_RUNNING, L1_CLOSED,
        L2_READY, L2_RAW_MAPPING, L2_RUNNING, L2_CLOSED,
        L3_READY, L3_INDEPENDENT_RUNNING, L3_REPORT_REVIEW, L3_RUNNING,
        RELEASE_READY, RELEASED, STALE, FAILED
    }
    public enum ContextPhase { L1_AUDIT, L2_RAW_MAPPING, L2_EDIT, L3_INDEPENDENT, L3_REPORT_REVIEW }

    private static final EnumSet<Gate> BASE_GATES = EnumSet.of(
            Gate.COVERAGE, Gate.FIDELITY, Gate.REFERENCE_VOICE, Gate.CONTINUITY_STRUCTURE);

    public static EnumSet<AssetRole> requiredInputs(ContextPhase phase) {
        switch (phase) {
            case L1_AUDIT:
                return EnumSet.of(AssetRole.RAW, AssetRole.DRAFT, AssetRole.GLOSSARY, AssetRole.PRONOUN);
            case L2_RAW_MAPPING:
                return EnumSet.of(AssetRole.RAW, AssetRole.GLOSSARY, AssetRole.PRONOUN);
            case L2_EDIT:
                return EnumSet.of(AssetRole.RAW, AssetRole.DRAFT, AssetRole.GLOSSARY, AssetRole.PRONOUN, AssetRole.REPORT_L1);
            case L3_INDEPENDENT:
                return EnumSet.of(AssetRole.RAW, AssetRole.VI_L2, AssetRole.GLOSSARY, AssetRole.PRONOUN);
            case L3_REPORT_REVIEW:
                return EnumSet.of(AssetRole.RAW, AssetRole.VI_L2, AssetRole.GLOSSARY, AssetRole.PRONOUN, AssetRole.REPORT_L1);
            default:
                return EnumSet.noneOf(AssetRole.class);
        }
    }

    /** Returns a stable, user-readable reason when a context package contains a forbidden role. */
    public static String validateContext(ContextPhase phase, Set<AssetRole> actual) {
        if (actual == null) return "Context package is missing";
        EnumSet<AssetRole> required = requiredInputs(phase);
        for (AssetRole role : required) if (!actual.contains(role)) return "Missing required input: " + role;
        for (AssetRole role : actual) if (!required.contains(role)) return "Forbidden input for " + phase + ": " + role;
        return null;
    }

    public static boolean baseGatesClosed(java.util.Map<Gate, GateStatus> statuses) {
        if (statuses == null) return false;
        for (Gate gate : BASE_GATES) if (statuses.get(gate) != GateStatus.CLOSED) return false;
        return true;
    }

    public static boolean mayRelease(java.util.Map<Gate, GateStatus> statuses,
                                     boolean crossSceneVoiceAuditClosed,
                                     boolean finalReadThroughClosed) {
        return baseGatesClosed(statuses)
                && statuses != null && statuses.get(Gate.CHANGE) == GateStatus.CLOSED
                && crossSceneVoiceAuditClosed && finalReadThroughClosed;
    }

    public static boolean canTransition(ChapterState from, ChapterState to) {
        if (from == null || to == null || from == to) return false;
        if (to == ChapterState.STALE || to == ChapterState.FAILED) return from != ChapterState.RELEASED;
        switch (from) {
            case DRAFT_INPUT: return to == ChapterState.L1_READY;
            case L1_READY: return to == ChapterState.L1_RUNNING;
            case L1_RUNNING: return to == ChapterState.L1_CLOSED;
            case L1_CLOSED: return to == ChapterState.L2_READY;
            case L2_READY: return to == ChapterState.L2_RAW_MAPPING;
            case L2_RAW_MAPPING: return to == ChapterState.L2_RUNNING;
            case L2_RUNNING: return to == ChapterState.L2_CLOSED;
            case L2_CLOSED: return to == ChapterState.L3_READY;
            case L3_READY: return to == ChapterState.L3_INDEPENDENT_RUNNING;
            case L3_INDEPENDENT_RUNNING: return to == ChapterState.L3_REPORT_REVIEW;
            case L3_REPORT_REVIEW: return to == ChapterState.L3_RUNNING;
            case L3_RUNNING: return to == ChapterState.RELEASE_READY;
            case RELEASE_READY: return to == ChapterState.RELEASED;
            case STALE: return to == ChapterState.L1_READY;
            case FAILED: return to == ChapterState.L1_READY || to == ChapterState.L2_READY || to == ChapterState.L3_READY;
            default: return false;
        }
    }

    private EditorialWorkflowV5() {}
}
