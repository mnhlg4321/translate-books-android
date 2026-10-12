package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EditorialSafe4ReplayRunnerTest {
    @Test public void G1ThroughG24ExecuteWithDeterministicExpectedDecisions() {
        EditorialSafe4ReplayRunner.ReplayResult result = new EditorialSafe4ReplayRunner().runAll();
        assertEquals(24, result.cases().size());
        assertEquals(24L, result.passedCount());
        assertTrue(result.allPassed());
    }

    @Test public void replayCaseRequiresDecisionReasonGateAndEquation() {
        EditorialSafe4ReplayRunner runner = new EditorialSafe4ReplayRunner();
        EditorialSafe4ReplayRunner.CaseDescriptor descriptor = runner.descriptors().get(23);
        assertEquals("G24", descriptor.caseId());
        assertEquals("REPAIR_REQUIRED", descriptor.expectedDecision());
        assertEquals("CHANGE_COVERAGE", descriptor.expectedGate());
        assertEquals("modelPassIgnored=1", descriptor.expectedEquation());
    }
}
