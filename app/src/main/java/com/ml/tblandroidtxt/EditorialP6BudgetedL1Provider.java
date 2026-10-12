package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;

import java.util.Map;
import java.util.Objects;

/** Reserves group spend before every L1 dispatch and settles only known provider cost. */
final class EditorialP6BudgetedL1Provider implements EditorialP5PilotProvider {
    private final EditorialP5PilotProvider delegate;
    private final EditorialP6GroupSpendLedger ledger;
    private final Map<String, EditorialL2Execution.Budget> budgets;

    EditorialP6BudgetedL1Provider(EditorialP5PilotProvider delegate, EditorialP6GroupSpendLedger ledger,
                                  EditorialChainBudgets chainBudgets) {
        this.delegate = Objects.requireNonNull(delegate, "provider");
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.budgets = Map.of("L1_RAW_DISCOVERY", chainBudgets.l1Raw(),
                "L1_RECONCILE", chainBudgets.l1Reconcile());
    }

    @Override public void beginAttempt(long maximumExecutionTimeMillis) { delegate.beginAttempt(maximumExecutionTimeMillis); }

    @Override public Response call(Request request) throws Exception {
        EditorialL2Execution.Budget budget = budgets.get(request.phase());
        if (budget == null) throw new IllegalStateException("P6_SPEND_PHASE_NOT_ALLOWED");
        String callId = EditorialP6GroupSpendLedger.callId(request.attemptIdentity(), request.phase());
        ledger.reserve(callId, request.phase(), EditorialP6GroupSpendLedger.worstCase(budget));
        Response response = delegate.call(request);
        if (response.costKnown()) ledger.settle(callId, response.reportedCost());
        return response;
    }
}
