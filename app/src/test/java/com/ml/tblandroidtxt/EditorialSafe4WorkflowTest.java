package com.ml.tblandroidtxt;

import org.junit.Test;

import java.util.EnumMap;
import java.util.EnumSet;

import static org.junit.Assert.*;

public class EditorialSafe4WorkflowTest {
    @Test public void bundledPackHasStableIdentityAndRemainsFailClosed() {
        assertEquals("V5-SAFE.4", EditorialSafe4Pack.VERSION);
        assertEquals(64, EditorialSafe4Pack.PACK_HASH.length());
        assertTrue(EditorialSafe4Pack.implementedCapabilities().contains(EditorialSafe4Pack.Capability.PACK_INTEGRITY));
        assertTrue(EditorialSafe4Pack.missingCapabilities().contains(EditorialSafe4Pack.Capability.GOLDEN_REPLAY_G1_TO_G10));
        assertFalse(EditorialSafe4Pack.executionEnabled());
    }

    @Test public void noneAndLegacyRejectedNeverRequireOrAllowPronoun() {
        EnumSet<EditorialSafe4Workflow.AssetRole> none = EditorialSafe4Workflow.requiredInputs(
                EditorialSafe4Workflow.ContextPhase.L1_RECONCILE,
                EditorialSafe4Workflow.PronounStatus.NONE, false);
        assertFalse(none.contains(EditorialSafe4Workflow.AssetRole.PRONOUN));
        none.add(EditorialSafe4Workflow.AssetRole.PRONOUN);
        assertTrue(EditorialSafe4Workflow.validateContext(EditorialSafe4Workflow.ContextPhase.L1_RECONCILE,
                EditorialSafe4Workflow.PronounStatus.LEGACY_REJECTED, false, none).contains("Forbidden"));
    }

    @Test public void blindL3ForbidsDraftReportAndChangeMap() {
        EnumSet<EditorialSafe4Workflow.AssetRole> blind = EditorialSafe4Workflow.requiredInputs(
                EditorialSafe4Workflow.ContextPhase.L3_BLIND,
                EditorialSafe4Workflow.PronounStatus.NONE, false);
        assertFalse(blind.contains(EditorialSafe4Workflow.AssetRole.DRAFT));
        assertFalse(blind.contains(EditorialSafe4Workflow.AssetRole.REPORT_L1));
        assertFalse(blind.contains(EditorialSafe4Workflow.AssetRole.CHANGE_MAP_L2));
    }

    @Test public void releaseCannotOpenBeforeAllCapabilitiesAndEvidenceExist() {
        EnumMap<EditorialSafe4Workflow.Gate, EditorialSafe4Workflow.GateStatus> gates =
                new EnumMap<>(EditorialSafe4Workflow.Gate.class);
        for (EditorialSafe4Workflow.Gate gate : EditorialSafe4Workflow.Gate.values()) {
            gates.put(gate, EditorialSafe4Workflow.GateStatus.PASS);
        }
        EditorialSafe4Workflow.ReleaseNumbers zero = new EditorialSafe4Workflow.ReleaseNumbers(0, 0, 0, 0, 0);
        assertFalse(EditorialSafe4Workflow.mayRelease(gates, zero, true, true, true));
        assertFalse(EditorialSafe4Workflow.canTransition(EditorialSafe4Workflow.ChapterState.SAFE4_READY,
                EditorialSafe4Workflow.ChapterState.L1_RAW_DISCOVERY));
    }

    @Test public void legacyV5StatesBecomeReadOnly() {
        assertEquals(EditorialSafe4Workflow.ChapterState.LEGACY_V5_READ_ONLY,
                EditorialSafe4Workflow.persistedState("L2_RUNNING"));
        assertEquals(EditorialSafe4Workflow.ChapterState.RELEASED,
                EditorialSafe4Workflow.persistedState("RELEASED"));
    }
}
