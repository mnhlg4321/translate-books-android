package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ml.tblandroidtxt.EditorialChapterProgress.StageRow;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class EditorialChapterRunServiceTest {
    private static final EditorialChainBudgets BUDGETS = EditorialChainBudgets.d3Recommended();

    private static EditorialChapterFinalCoordinator.Inspection state(boolean l1, StageRow l2, StageRow l3) {
        return new EditorialChapterFinalCoordinator.Inspection(EditorialChapterProgress.derive(l1, l2, l3), null);
    }

    private static final class FakeChain implements EditorialChapterRunService.Chain {
        EditorialChapterFinalCoordinator.Inspection inspection;
        String preflight;
        final AtomicInteger runs = new AtomicInteger();
        EditorialChainBudgets lastBudgets;
        CountDownLatch inside;
        CountDownLatch release;
        RuntimeException failure;

        FakeChain(EditorialChapterFinalCoordinator.Inspection inspection) { this.inspection = inspection; }

        @Override public EditorialChapterFinalCoordinator.Inspection inspect() { return inspection; }

        @Override public String preflightIssue() { return preflight; }

        @Override public EditorialChapterFinalCoordinator.Result run(EditorialChainBudgets budgets) {
            runs.incrementAndGet();
            lastBudgets = budgets;
            if (inside != null) inside.countDown();
            try {
                if (release != null) release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            if (failure != null) throw failure;
            return new EditorialChapterFinalCoordinator.Result(EditorialChapterFinalCoordinator.Stage.FINAL, true,
                    "L3_FINAL_COMMITTED", null, 4);
        }
    }

    @Test public void runsOnceWithTheExactCapsWhenConfirmedAndNeverAttempted() {
        FakeChain chain = new FakeChain(state(true, null, null));
        EditorialChapterRunService.Outcome outcome = EditorialChapterRunService.run("1:001", chain, BUDGETS, true);
        assertTrue(outcome.started());
        assertEquals("L3_FINAL_COMMITTED", outcome.reasonCode());
        assertEquals(1, chain.runs.get());
        assertEquals(BUDGETS, chain.lastBudgets);
        assertFalse(EditorialChapterRunService.isRunning("1:001"));
    }

    @Test public void resumesWhenL2IsCommittedAndL3WasNeverAttempted() {
        FakeChain chain = new FakeChain(state(true, new StageRow("COMMITTED", "", true), null));
        assertTrue(EditorialChapterRunService.run("1:002", chain, BUDGETS, true).started());
        assertEquals(1, chain.runs.get());
    }

    @Test public void fullLedgerConsentCanStartAtL1WhenNoReportIsCommitted() {
        FakeChain chain = new FakeChain(state(false, null, null));
        EditorialChainBudgets full = EditorialChainBudgets.fullLedgerRecommended();
        EditorialChapterRunService.Outcome outcome = EditorialChapterRunService.run("1:001-new", chain, full, true);
        assertTrue(outcome.started());
        assertEquals(1, chain.runs.get());
        assertEquals(full, chain.lastBudgets);
    }

    @Test public void withoutExplicitConfirmationNothingRuns() {
        FakeChain chain = new FakeChain(state(true, null, null));
        EditorialChapterRunService.Outcome outcome = EditorialChapterRunService.run("1:003", chain, BUDGETS, false);
        assertFalse(outcome.started());
        assertEquals("RUN_NOT_CONFIRMED", outcome.reasonCode());
        assertEquals(0, chain.runs.get());
    }

    @Test public void invalidOrMissingCapsNeverRun() {
        FakeChain chain = new FakeChain(state(true, null, null));
        EditorialL2Execution.Budget zero = new EditorialL2Execution.Budget(0, 8_192, new BigDecimal("0.05"), 180_000L);
        EditorialChainBudgets zeroInput = new EditorialChainBudgets(zero, BUDGETS.edit(), BUDGETS.reaudit(),
                BUDGETS.reconcile(), new BigDecimal("0.30"));
        EditorialChainBudgets overChain = new EditorialChainBudgets(BUDGETS.discovery(), BUDGETS.edit(),
                BUDGETS.reaudit(), BUDGETS.reconcile(), new BigDecimal("0.10"));
        EditorialChainBudgets noChainCap = new EditorialChainBudgets(BUDGETS.discovery(), BUDGETS.edit(),
                BUDGETS.reaudit(), BUDGETS.reconcile(), BigDecimal.ZERO);
        for (EditorialChainBudgets bad : new EditorialChainBudgets[]{null, zeroInput, overChain, noChainCap}) {
            EditorialChapterRunService.Outcome outcome = EditorialChapterRunService.run("1:004", chain, bad, true);
            assertFalse(outcome.started());
            assertEquals("RUN_BUDGET_INVALID", outcome.reasonCode());
        }
        assertEquals(0, chain.runs.get());
        assertTrue(BUDGETS.valid());
        assertEquals(new BigDecimal("0.30"), BUDGETS.summedPhaseCost());
    }

    @Test public void setupProblemIsRefusedBeforeAnyClaim() {
        FakeChain chain = new FakeChain(state(true, null, null));
        chain.preflight = "P6_ROUTE_SETTINGS_MISMATCH";
        EditorialChapterRunService.Outcome outcome = EditorialChapterRunService.run("1:005", chain, BUDGETS, true);
        assertFalse(outcome.started());
        assertEquals("RUN_PREFLIGHT:P6_ROUTE_SETTINGS_MISMATCH", outcome.reasonCode());
        assertEquals(0, chain.runs.get());
        assertFalse(EditorialChapterRunService.isRunning("1:005"));
    }

    @Test public void unknownRecoveryAndFinishedStatesAreNeverRerun() {
        StageRow committed = new StageRow("COMMITTED", "", true);
        Object[][] cases = {
                {state(false, null, null), "RUN_NOT_ALLOWED:INPUT_REPORT_L1_NOT_COMMITTED"},
                {state(true, new StageRow("CLAIMED", "", false), null), "RUN_NOT_ALLOWED:RETRY_L2_CALL_STATE_UNKNOWN"},
                {state(true, new StageRow("RECOVERY_REQUIRED", "REPAIR_L2_OUTPUT_SCHEMA_INVALID", false), null),
                        "RUN_NOT_ALLOWED:REPAIR_L2_OUTPUT_SCHEMA_INVALID"},
                {state(true, committed, new StageRow("CLAIMED", "", false)), "RUN_NOT_ALLOWED:RETRY_L3_CALL_STATE_UNKNOWN"},
                {state(true, committed, new StageRow("COMMITTED", "", false)),
                        "RUN_NOT_ALLOWED:RETRY_L3_COMMITTED_RESULT_UNAVAILABLE"},
                {state(true, committed, committed), "RUN_NOT_ALLOWED:FINAL_COMMITTED"},
        };
        for (Object[] c : cases) {
            FakeChain chain = new FakeChain((EditorialChapterFinalCoordinator.Inspection) c[0]);
            EditorialChapterRunService.Outcome outcome = EditorialChapterRunService.run("1:006", chain, BUDGETS, true);
            assertFalse((String) c[1], outcome.started());
            assertEquals(c[1], outcome.reasonCode());
            assertEquals(0, chain.runs.get());
        }
    }

    @Test public void doubleTapStartsOneRunAndTheSecondIsRefusedWhileTheFirstIsInside() throws Exception {
        FakeChain chain = new FakeChain(state(true, null, null));
        chain.inside = new CountDownLatch(1);
        chain.release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<EditorialChapterRunService.Outcome> first = pool.submit(
                    () -> EditorialChapterRunService.run("1:007", chain, BUDGETS, true));
            assertTrue(chain.inside.await(5, TimeUnit.SECONDS));
            assertTrue(EditorialChapterRunService.isRunning("1:007"));
            EditorialChapterRunService.Outcome second = EditorialChapterRunService.run("1:007", chain, BUDGETS, true);
            assertFalse(second.started());
            assertEquals("RUN_ALREADY_ACTIVE", second.reasonCode());
            // Another chapter is independent.
            assertTrue(EditorialChapterRunService.run("2:001", new FakeChain(state(true, null, null)), BUDGETS, true).started());
            chain.release.countDown();
            assertTrue(first.get(5, TimeUnit.SECONDS).started());
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, chain.runs.get());
        assertFalse(EditorialChapterRunService.isRunning("1:007"));
    }

    @Test public void unexpectedFailureReleasesTheLockAndReportsStateMustBeReinspected() {
        FakeChain chain = new FakeChain(state(true, null, null));
        chain.failure = new IllegalStateException("boom");
        EditorialChapterRunService.Outcome outcome = EditorialChapterRunService.run("1:008", chain, BUDGETS, true);
        assertTrue(outcome.started());
        assertEquals("RUN_UNEXPECTED_ERROR", outcome.reasonCode());
        assertNull(outcome.result());
        assertFalse(EditorialChapterRunService.isRunning("1:008"));
        assertEquals(1, chain.runs.get());
    }

    @Test public void dialogWordingCarriesTheExactEnforcedNumbers() {
        String text = BUDGETS.describe();
        assertTrue(text, text.contains("L2_RAW_DISCOVERY: 1 call, output ≤ 8192 token, ≤ USD 0.05"));
        assertTrue(text, text.contains("L2_EDIT: 1 call, output ≤ 16384 token, ≤ USD 0.10"));
        assertTrue(text, text.contains("L3_RAW_FIRST_REAUDIT: 1 call, output ≤ 8192 token, ≤ USD 0.05"));
        assertTrue(text, text.contains("L3_RECONCILE: 1 call, output ≤ 16384 token, ≤ USD 0.10"));
        assertTrue(text, text.contains("Input ≤ 200000 byte mỗi call • 180 s mỗi call • 0 repair • 0 retry"));
        assertTrue(text, text.contains("Trần cả chuỗi: USD 0.30"));
    }

    @Test public void lockKeyIsPerProjectAndChapter() {
        assertEquals("7:001", EditorialChapterRunService.lockKey(7L, "001"));
        assertFalse(EditorialChapterRunService.lockKey(7L, "001").equals(EditorialChapterRunService.lockKey(8L, "001")));
    }
}
