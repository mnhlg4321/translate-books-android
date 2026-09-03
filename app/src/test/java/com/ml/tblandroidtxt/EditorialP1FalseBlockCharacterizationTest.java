package com.ml.tblandroidtxt;

import org.junit.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * P1 characterization of the existing SAFE4 workflow seam. The test records
 * which false-block protections already exist and which typed dispositions
 * have no runtime representation yet.
 */
public class EditorialP1FalseBlockCharacterizationTest {
    @Test public void phaseScopedHiddenAssetsAndOptionalPairContextAreNotMissing() {
        EnumSet<EditorialSafe4Workflow.AssetRole> visibleInL1 = EnumSet.of(
                EditorialSafe4Workflow.AssetRole.RAW,
                EditorialSafe4Workflow.AssetRole.GLOSSARY);

        assertFalse(EditorialSafe4Workflow.requiredInputs(
                EditorialSafe4Workflow.ContextPhase.L1_RAW_DISCOVERY,
                EditorialSafe4Workflow.PronounStatus.NONE,
                false).contains(EditorialSafe4Workflow.AssetRole.DRAFT));
        assertNull(EditorialSafe4Workflow.validateContext(
                EditorialSafe4Workflow.ContextPhase.L1_RAW_DISCOVERY,
                EditorialSafe4Workflow.PronounStatus.NONE,
                false,
                visibleInL1));
    }

    @Test public void pronounStatusAndPairContextAreExplicitNotSilentlyInferred() {
        EnumSet<EditorialSafe4Workflow.AssetRole> base = EnumSet.of(
                EditorialSafe4Workflow.AssetRole.RAW,
                EditorialSafe4Workflow.AssetRole.GLOSSARY);
        assertNull(EditorialSafe4Workflow.validateContext(
                EditorialSafe4Workflow.ContextPhase.L1_RAW_DISCOVERY,
                EditorialSafe4Workflow.PronounStatus.NONE,
                false,
                base));

        String missingPronoun = EditorialSafe4Workflow.validateContext(
                EditorialSafe4Workflow.ContextPhase.L1_RAW_DISCOVERY,
                EditorialSafe4Workflow.PronounStatus.AVAILABLE,
                false,
                base);
        assertTrue(missingPronoun.contains("PRONOUN"));

        EnumSet<EditorialSafe4Workflow.AssetRole> unexpectedPronoun = EnumSet.copyOf(base);
        unexpectedPronoun.add(EditorialSafe4Workflow.AssetRole.PRONOUN);
        String quarantined = EditorialSafe4Workflow.validateContext(
                EditorialSafe4Workflow.ContextPhase.L1_RAW_DISCOVERY,
                EditorialSafe4Workflow.PronounStatus.NONE,
                false,
                unexpectedPronoun);
        assertTrue(quarantined.contains("Forbidden"));
        assertTrue(quarantined.contains("PRONOUN"));
    }

    @Test public void zeroPopulationAndZeroEditCountersAreNotFailuresButReleaseRemainsLocked() {
        EditorialSafe4Workflow.ReleaseNumbers numbers =
                new EditorialSafe4Workflow.ReleaseNumbers(0, 0, 0, 0, 0);
        assertTrue(numbers.allZero());

        Map<EditorialSafe4Workflow.Gate, EditorialSafe4Workflow.GateStatus> gates =
                new EnumMap<>(EditorialSafe4Workflow.Gate.class);
        for (EditorialSafe4Workflow.Gate gate : EditorialSafe4Workflow.Gate.values()) {
            gates.put(gate, EditorialSafe4Workflow.GateStatus.PASS);
        }
        assertFalse(EditorialSafe4Pack.executionEnabled());
        assertFalse(EditorialSafe4Workflow.mayRelease(gates, numbers, true, true, true));
    }

    @Test public void typedOutputStopRecoveryAndPreserveDraftDispositionsAreNotRepresentedYet() {
        assertFalse(hasConstant(EditorialSafe4Workflow.ChapterState.class, "PRESERVE_DRAFT"));
        assertFalse(hasConstant(EditorialSafe4Workflow.ChapterState.class, "REPAIR_REQUIRED"));
        assertFalse(hasConstant(EditorialSafe4Workflow.ChapterState.class, "RETRY_REQUIRED"));
        assertFalse(hasConstant(EditorialSafe4Workflow.ChapterState.class, "INPUT_REQUIRED"));
        assertFalse(hasConstant(EditorialSafe4Workflow.ChapterState.class, "CONTENT_BLOCKED"));
        assertTrue(hasConstant(EditorialSafe4Workflow.GateStatus.class, "BLOCKED"));
        assertFalse(hasConstant(EditorialSafe4Workflow.GateStatus.class, "REPAIR_REQUIRED"));
    }

    @Test public void modelCannotSelfCertifyOrChangeChapterStateThroughExistingGate() {
        assertFalse(EditorialSafe4Workflow.canTransition(
                EditorialSafe4Workflow.ChapterState.SAFE4_READY,
                EditorialSafe4Workflow.ChapterState.L1_RAW_DISCOVERY));
        assertFalse(EditorialSafe4Workflow.mayRelease(null,
                new EditorialSafe4Workflow.ReleaseNumbers(0, 0, 0, 0, 0), true, true, true));
    }

    private static boolean hasConstant(Class<? extends Enum<?>> type, String name) {
        return Arrays.stream(type.getEnumConstants()).anyMatch(value -> value.name().equals(name));
    }
}
