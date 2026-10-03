package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialL3Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialFinalRead;

import java.util.Map;
import java.util.Objects;

/** Reserves group spend before each L2/L3/final-read call and settles only known provider cost. */
final class EditorialP6BudgetedPhaseProvider implements EditorialL2Execution.Provider {
    private final EditorialL2Execution.Provider delegate;
    private final EditorialP6GroupSpendLedger ledger;
    private final Map<String, EditorialL2Execution.Budget> budgets;

    EditorialP6BudgetedPhaseProvider(EditorialL2Execution.Provider delegate, EditorialP6GroupSpendLedger ledger,
                                     EditorialChainBudgets chainBudgets, boolean l2) {
        this.delegate = Objects.requireNonNull(delegate, "provider");
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.budgets = l2
                ? Map.of(EditorialL2Execution.DISCOVERY_PHASE, chainBudgets.discovery(),
                        EditorialL2Execution.PHASE, chainBudgets.edit(),
                        EditorialFinalRead.L2_PHASE, chainBudgets.finalRead())
                : Map.of(EditorialL3Execution.REAUDIT_PHASE, chainBudgets.reaudit(),
                        EditorialL3Execution.RECONCILE_PHASE, chainBudgets.reconcile(),
                        EditorialFinalRead.L3_PHASE, chainBudgets.finalRead());
    }

    @Override public Response call(Request request) throws Exception {
        EditorialL2Execution.Budget budget = budgets.get(request.phase());
        if (budget == null) throw new IllegalStateException("P6_SPEND_PHASE_NOT_ALLOWED");
        ledger.reserve(request.attemptIdentity(), request.phase(), EditorialP6GroupSpendLedger.worstCase(budget));
        Response response = delegate.call(request);
        if (response.costKnown()) ledger.settle(request.attemptIdentity(), response.cost());
        return response;
    }
}
