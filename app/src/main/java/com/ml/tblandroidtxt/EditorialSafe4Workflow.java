package com.ml.tblandroidtxt;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Fail-closed machine contract for V5-SAFE.4. No model-provided PASS can bypass evidence. */
public final class EditorialSafe4Workflow {
    public enum AssetRole {
        RAW, DRAFT, GLOSSARY, PRONOUN, PAIR_CONTEXT,
        REPORT_L1, VI_L2, CHANGE_MAP_L2, FINAL_QA, QA_RECEIPT, PAIR_DELTA_QA
    }

    public enum PronounStatus { AVAILABLE, NONE, LEGACY_REJECTED }

    public enum Gate {
        ARTIFACT_IDENTITY,
        COVERAGE,
        TITLE_GLOSSARY,
        SEMANTIC_FIDELITY,
        RELATION_PAIR_PROOF,
        SPEAKER,
        CHANGE_COVERAGE,
        NO_REGRESSION,
        CONTINUITY_STRUCTURE_TECHNICAL
    }

    public enum GateStatus { OPEN, PASS, BLOCKED, NOT_APPLICABLE }

    public enum ChapterState {
        INPUT_DRAFT,
        SAFE4_BLOCKED,
        SAFE4_READY,
        L1_RAW_DISCOVERY,
        L1_RECONCILE,
        L1_CLOSED,
        L2_RAW_DISCOVERY,
        L2_EDITING,
        L2_CLOSED,
        L3_BLIND_QA,
        L3_RECONCILE,
        L3_ADVERSARIAL_COVERAGE,
        L3_ADVERSARIAL_REGRESSION,
        RELEASE_READY,
        RELEASED,
        STALE,
        FAILED,
        LEGACY_V5_READ_ONLY
    }

    public enum ContextPhase {
        L1_RAW_DISCOVERY,
        L1_RECONCILE,
        L2_RAW_DISCOVERY,
        L2_EDIT,
        L3_BLIND,
        L3_RECONCILE,
        L3_ADVERSARIAL_COVERAGE,
        L3_ADVERSARIAL_REGRESSION
    }

    public static final class ReleaseNumbers {
        public final int unprocessedRawUnits;
        public final int uncoveredTitleGlossaryOrSemanticOccurrences;
        public final int unresolvedLocalConflicts;
        public final int unaccountedChangedAnchors;
        public final int protectedSpanRegressions;

        public ReleaseNumbers(int raw, int occurrences, int conflicts, int changes, int protectedSpans) {
            unprocessedRawUnits = raw;
            uncoveredTitleGlossaryOrSemanticOccurrences = occurrences;
            unresolvedLocalConflicts = conflicts;
            unaccountedChangedAnchors = changes;
            protectedSpanRegressions = protectedSpans;
        }

        public boolean allZero() {
            return unprocessedRawUnits == 0
                    && uncoveredTitleGlossaryOrSemanticOccurrences == 0
                    && unresolvedLocalConflicts == 0
                    && unaccountedChangedAnchors == 0
                    && protectedSpanRegressions == 0;
        }
    }

    public static EnumSet<AssetRole> requiredInputs(ContextPhase phase, PronounStatus pronounStatus,
                                                     boolean pairContextAvailable) {
        if (phase == null || pronounStatus == null) throw new IllegalArgumentException("SAFE 4 phase and PRONOUN_STATUS are required");
        EnumSet<AssetRole> roles;
        switch (phase) {
            case L1_RAW_DISCOVERY:
            case L2_RAW_DISCOVERY:
                roles = EnumSet.of(AssetRole.RAW, AssetRole.GLOSSARY);
                break;
            case L1_RECONCILE:
                roles = EnumSet.of(AssetRole.RAW, AssetRole.DRAFT, AssetRole.GLOSSARY);
                break;
            case L2_EDIT:
                roles = EnumSet.of(AssetRole.RAW, AssetRole.DRAFT, AssetRole.GLOSSARY, AssetRole.REPORT_L1);
                break;
            case L3_BLIND:
                roles = EnumSet.of(AssetRole.RAW, AssetRole.GLOSSARY, AssetRole.VI_L2);
                break;
            default:
                roles = EnumSet.of(AssetRole.RAW, AssetRole.DRAFT, AssetRole.GLOSSARY,
                        AssetRole.REPORT_L1, AssetRole.VI_L2, AssetRole.CHANGE_MAP_L2);
                break;
        }
        if (pronounStatus == PronounStatus.AVAILABLE) roles.add(AssetRole.PRONOUN);
        if (pairContextAvailable) roles.add(AssetRole.PAIR_CONTEXT);
        return roles;
    }

    public static String validateContext(ContextPhase phase, PronounStatus pronounStatus,
                                         boolean pairContextAvailable, Set<AssetRole> actual) {
        if (actual == null) return "Context package is missing";
        EnumSet<AssetRole> required = requiredInputs(phase, pronounStatus, pairContextAvailable);
        for (AssetRole role : required) if (!actual.contains(role)) return "Missing required SAFE 4 input: " + role;
        for (AssetRole role : actual) if (!required.contains(role)) return "Forbidden SAFE 4 input for " + phase + ": " + role;
        if (pronounStatus != PronounStatus.AVAILABLE && actual.contains(AssetRole.PRONOUN)) {
            return "PRONOUN must be quarantined when status is " + pronounStatus;
        }
        return null;
    }

    public static boolean mayRelease(Map<Gate, GateStatus> gates, ReleaseNumbers numbers,
                                     boolean qaReceiptValid, boolean adversarialCoveragePass,
                                     boolean adversarialRegressionPass) {
        if (!EditorialSafe4Pack.executionEnabled() || gates == null || numbers == null || !numbers.allZero()
                || !qaReceiptValid || !adversarialCoveragePass || !adversarialRegressionPass) return false;
        for (Gate gate : Gate.values()) if (gates.get(gate) != GateStatus.PASS) return false;
        return true;
    }

    public static boolean canTransition(ChapterState from, ChapterState to) {
        if (!EditorialSafe4Pack.executionEnabled() || from == null || to == null || from == to) return false;
        switch (from) {
            case SAFE4_READY: return to == ChapterState.L1_RAW_DISCOVERY;
            case L1_RAW_DISCOVERY: return to == ChapterState.L1_RECONCILE || to == ChapterState.FAILED;
            case L1_RECONCILE: return to == ChapterState.L1_CLOSED || to == ChapterState.FAILED;
            case L1_CLOSED: return to == ChapterState.L2_RAW_DISCOVERY;
            case L2_RAW_DISCOVERY: return to == ChapterState.L2_EDITING || to == ChapterState.FAILED;
            case L2_EDITING: return to == ChapterState.L2_CLOSED || to == ChapterState.FAILED;
            case L2_CLOSED: return to == ChapterState.L3_BLIND_QA;
            case L3_BLIND_QA: return to == ChapterState.L3_RECONCILE || to == ChapterState.FAILED;
            case L3_RECONCILE: return to == ChapterState.L3_ADVERSARIAL_COVERAGE || to == ChapterState.FAILED;
            case L3_ADVERSARIAL_COVERAGE: return to == ChapterState.L3_ADVERSARIAL_REGRESSION || to == ChapterState.FAILED;
            case L3_ADVERSARIAL_REGRESSION: return to == ChapterState.RELEASE_READY || to == ChapterState.FAILED;
            case RELEASE_READY: return to == ChapterState.RELEASED;
            case STALE: return to == ChapterState.SAFE4_READY;
            case FAILED: return to == ChapterState.SAFE4_READY;
            default: return false;
        }
    }

    public static ChapterState persistedState(String value) {
        if (value == null || value.trim().isEmpty()) return ChapterState.LEGACY_V5_READ_ONLY;
        try { return ChapterState.valueOf(value); }
        catch (Exception ignored) { return ChapterState.LEGACY_V5_READ_ONLY; }
    }

    private EditorialSafe4Workflow() { }
}
