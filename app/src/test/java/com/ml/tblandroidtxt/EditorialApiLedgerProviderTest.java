package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** The fixture runner's ledger wrapper: real charges are settled as they are, never clamped to the reservation. */
public final class EditorialApiLedgerProviderTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private static final EditorialApiRunService.Pricing PINNED = new EditorialApiRunService.Pricing() {
        @Override public BigDecimal inputPerToken(String model) { return new BigDecimal("0.00000025"); }

        @Override public BigDecimal outputPerToken(String model) { return new BigDecimal("0.0000012"); }
    };

    private EditorialP6GroupSpendLedger ledger() throws Exception {
        Path path = folder.newFolder().toPath().resolve("ledger.jsonl");
        return new EditorialP6GroupSpendLedger(path, "TEST-GROUP", new BigDecimal("1.00"));
    }

    private static EditorialApiFlow.Request request() {
        EditorialApiFlow flow = new EditorialApiFlow(new EditInputs("原文", "Bản nháp", "Vietnamese", List.of(), ""),
                EditorialApiFlow.Config.of(EditorialApiContract.Mode.QUICK));
        return flow.nextRequest();
    }

    private static EditorialApiFlow.StepResponse answer(String cost, boolean known) {
        return new EditorialApiFlow.StepResponse("<EDITED>x</EDITED>", "stop", 100, 50, new BigDecimal(cost), known, "m", "r", "");
    }

    private static EditorialApiLedgerProvider wrap(EditorialApiFlow.StepResponse response, EditorialP6GroupSpendLedger ledger, List<String> seen) {
        EditorialApiProvider delegate = new EditorialApiProvider() {
            @Override public EditorialApiFlow.StepResponse call(EditorialApiFlow.Request r, String model, int maxOutput, long timeoutMillis) { return response; }

            @Override public void cancel() { }
        };
        return new EditorialApiLedgerProvider(delegate, ledger, PINNED, "run|fx", "m", (r, a) -> seen.add(r.step().name()));
    }

    @Test public void aChargeInsideTheReservationIsSettledExactly() throws Exception {
        EditorialP6GroupSpendLedger ledger = ledger();
        List<String> seen = new ArrayList<>();
        wrap(answer("0.004", true), ledger, seen).call(request(), "m", 4096, 1000);
        EditorialP6GroupSpendLedger.Snapshot s = ledger.inspect();
        assertEquals(0, s.pendingCalls());
        assertEquals(0, new BigDecimal("0.004").compareTo(s.settledUsd()));
        assertEquals(List.of("EDIT"), seen);
    }

    @Test public void aChargeAboveTheReservationIsKeptAsEvidenceAndNeverClampedIntoTheLedger() throws Exception {
        EditorialP6GroupSpendLedger ledger = ledger();
        EditorialApiLedgerProvider provider = wrap(answer("5.0", true), ledger, new ArrayList<>());
        EditorialApiFlow.StepResponse back = provider.call(request(), "m", 4096, 1000);
        assertEquals(0, new BigDecimal("5.0").compareTo(back.cost()));
        assertEquals(1, provider.overruns().size());
        assertEquals(0, new BigDecimal("5.0").compareTo(provider.overruns().get(0).actual()));
        assertTrue(provider.overruns().get(0).reserved().compareTo(new BigDecimal("0.01")) < 0);
        EditorialP6GroupSpendLedger.Snapshot s = ledger.inspect();
        assertEquals("the reservation stays open so the group stops", 1, s.pendingCalls());
        assertEquals(0, BigDecimal.ZERO.compareTo(s.settledUsd()));
    }

    @Test public void anUnpricedAnswerOrAnUnknownOutcomeLeavesTheReservationPending() throws Exception {
        EditorialP6GroupSpendLedger unpriced = ledger();
        wrap(answer("0", false), unpriced, new ArrayList<>()).call(request(), "m", 4096, 1000);
        assertEquals(1, unpriced.inspect().pendingCalls());
        EditorialP6GroupSpendLedger unknown = ledger();
        wrap(EditorialApiFlow.StepResponse.unknownOutcome("SocketTimeoutException"), unknown, new ArrayList<>()).call(request(), "m", 4096, 1000);
        assertEquals(1, unknown.inspect().pendingCalls());
    }

    @Test public void aDefiniteFailureBeforeDispatchSettlesAtZero() throws Exception {
        EditorialP6GroupSpendLedger ledger = ledger();
        wrap(EditorialApiFlow.StepResponse.failure("ApiHttpException: 429"), ledger, new ArrayList<>()).call(request(), "m", 4096, 1000);
        assertEquals(0, ledger.inspect().pendingCalls());
        assertEquals(0, BigDecimal.ZERO.compareTo(ledger.inspect().settledUsd()));
    }
}
