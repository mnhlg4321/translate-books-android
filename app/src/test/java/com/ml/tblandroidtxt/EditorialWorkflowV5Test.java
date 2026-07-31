package com.ml.tblandroidtxt;

import org.junit.Test;

import java.util.EnumMap;
import java.util.EnumSet;

import static org.junit.Assert.*;

public class EditorialWorkflowV5Test {
    @Test public void l3IndependentPackageCannotSeeReportOrL2Evidence() {
        EnumSet<EditorialWorkflowV5.AssetRole> allowed = EditorialWorkflowV5.requiredInputs(EditorialWorkflowV5.ContextPhase.L3_INDEPENDENT);
        assertFalse(allowed.contains(EditorialWorkflowV5.AssetRole.REPORT_L1));
        assertFalse(allowed.contains(EditorialWorkflowV5.AssetRole.DRAFT));
        assertNull(EditorialWorkflowV5.validateContext(EditorialWorkflowV5.ContextPhase.L3_INDEPENDENT, allowed));
        allowed.add(EditorialWorkflowV5.AssetRole.REPORT_L1);
        assertTrue(EditorialWorkflowV5.validateContext(EditorialWorkflowV5.ContextPhase.L3_INDEPENDENT, allowed).contains("Forbidden"));
    }

    @Test public void rawFirstPhasesRejectDraftAndReportUntilTheirBarrier() {
        EnumSet<EditorialWorkflowV5.AssetRole> l2 = EditorialWorkflowV5.requiredInputs(EditorialWorkflowV5.ContextPhase.L2_RAW_MAPPING);
        l2.add(EditorialWorkflowV5.AssetRole.DRAFT);
        assertTrue(EditorialWorkflowV5.validateContext(EditorialWorkflowV5.ContextPhase.L2_RAW_MAPPING, l2).contains("Forbidden"));
        EnumSet<EditorialWorkflowV5.AssetRole> l3 = EditorialWorkflowV5.requiredInputs(EditorialWorkflowV5.ContextPhase.L3_INDEPENDENT);
        l3.add(EditorialWorkflowV5.AssetRole.REPORT_L1);
        assertTrue(EditorialWorkflowV5.validateContext(EditorialWorkflowV5.ContextPhase.L3_INDEPENDENT, l3).contains("Forbidden"));
    }

    @Test public void stateMachineRequiresEveryV5Barrier() {
        assertTrue(EditorialWorkflowV5.canTransition(EditorialWorkflowV5.ChapterState.L1_CLOSED, EditorialWorkflowV5.ChapterState.L2_READY));
        assertFalse(EditorialWorkflowV5.canTransition(EditorialWorkflowV5.ChapterState.L1_CLOSED, EditorialWorkflowV5.ChapterState.L2_RUNNING));
        assertTrue(EditorialWorkflowV5.canTransition(EditorialWorkflowV5.ChapterState.L3_INDEPENDENT_RUNNING, EditorialWorkflowV5.ChapterState.L3_REPORT_REVIEW));
        assertFalse(EditorialWorkflowV5.canTransition(EditorialWorkflowV5.ChapterState.L3_INDEPENDENT_RUNNING, EditorialWorkflowV5.ChapterState.L3_RUNNING));
    }

    @Test public void releaseNeedsAllGatesAndBothFinalAudits() {
        EnumMap<EditorialWorkflowV5.Gate, EditorialWorkflowV5.GateStatus> gates = new EnumMap<>(EditorialWorkflowV5.Gate.class);
        for (EditorialWorkflowV5.Gate gate : EditorialWorkflowV5.Gate.values()) gates.put(gate, EditorialWorkflowV5.GateStatus.CLOSED);
        assertTrue(EditorialWorkflowV5.mayRelease(gates, true, true));
        gates.put(EditorialWorkflowV5.Gate.CHANGE, EditorialWorkflowV5.GateStatus.OPEN);
        assertFalse(EditorialWorkflowV5.mayRelease(gates, true, true));
    }

    @Test public void l1EvidenceRequiresAnchorsIssueEvidenceAndClosedGates() {
        String valid = "{\"schema\":\"editorial-v5-evidence-1\",\"run\":\"L1\",\"chapterId\":\"c12\","
                + "\"scenes\":[{\"id\":\"s1\",\"rawStart\":\"p1\",\"rawEnd\":\"p8\",\"coverage\":\"ALIGNED\",\"status\":\"CLOSED\"}],"
                + "\"issues\":[{\"id\":\"i1\",\"severity\":\"MAJOR\",\"rawAnchor\":\"p2\",\"draftAnchor\":\"p2\",\"currentMeaning\":\"A\",\"rawEvidence\":\"B\",\"deviationImpact\":\"C\",\"l2Scope\":\"scene\"}],"
                + "\"gates\":{\"coverage\":\"CLOSED\",\"fidelity\":\"CLOSED\",\"referenceVoice\":\"CLOSED\",\"continuityStructure\":\"CLOSED\"}}";
        assertNull(EditorialEvidenceValidator.validateL1Report(valid));
        assertTrue(EditorialEvidenceValidator.validateL1Report(valid.replace("\"rawEvidence\":\"B\",", "")).contains("Issue"));
        assertTrue(EditorialEvidenceValidator.validateL1Report(valid.replace("\"fidelity\":\"CLOSED\"", "\"fidelity\":\"OPEN\"")).contains("gate"));
    }

    @Test public void evidenceContractsCoverAllThreeV5Runs() {
        assertTrue(EditorialEvidenceSchema.l1ReportSchema().contains("\"run\":\"L1\""));
        assertTrue(EditorialEvidenceSchema.l2OutputSchema().contains("\"run\":\"L2\""));
        assertTrue(EditorialEvidenceSchema.l3OutputSchema().contains("\"run\":\"L3\""));
        assertTrue(EditorialEvidenceSchema.l3OutputSchema().contains("crossSceneVoiceAudit"));
    }
}
