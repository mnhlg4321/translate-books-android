package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Per-phase caps for one L2/L3 chain of a chapter: {@code L2_RAW_DISCOVERY}, {@code L2_EDIT},
 * {@code L3_RAW_FIRST_REAUDIT} and {@code L3_RECONCILE}, one call each, no repair, no retry, plus a
 * chain-wide USD cap. The authorization dialog shows exactly these numbers and the coordinator enforces
 * exactly these numbers; nothing else grants a provider call.
 */
public record EditorialChainBudgets(EditorialL2Execution.Budget discovery, EditorialL2Execution.Budget edit,
                                    EditorialL2Execution.Budget reaudit, EditorialL2Execution.Budget reconcile,
                                    BigDecimal chainMaximumCost) {
    public EditorialChainBudgets {
        Objects.requireNonNull(discovery, "discovery");
        Objects.requireNonNull(edit, "edit");
        Objects.requireNonNull(reaudit, "reaudit");
        Objects.requireNonNull(reconcile, "reconcile");
        Objects.requireNonNull(chainMaximumCost, "chainMaximumCost");
    }

    /** The caps recommended in docs/P6_G5_G6_WORK_REQUEST_20261002.md D3; they are not a grant. */
    public static EditorialChainBudgets d3Recommended() {
        long timeout = 180_000L;
        return new EditorialChainBudgets(
                new EditorialL2Execution.Budget(200_000, 8_192, new BigDecimal("0.05"), timeout),
                new EditorialL2Execution.Budget(200_000, 16_384, new BigDecimal("0.10"), timeout),
                new EditorialL2Execution.Budget(200_000, 8_192, new BigDecimal("0.05"), timeout),
                new EditorialL2Execution.Budget(200_000, 16_384, new BigDecimal("0.10"), timeout),
                new BigDecimal("0.30"));
    }

    /** Sum of the four per-call USD caps. */
    public BigDecimal summedPhaseCost() {
        return discovery.maximumCost().add(edit.maximumCost()).add(reaudit.maximumCost()).add(reconcile.maximumCost());
    }

    /** Every cap is positive and the per-call caps fit under the chain cap. */
    public boolean valid() {
        for (EditorialL2Execution.Budget budget : new EditorialL2Execution.Budget[]{discovery, edit, reaudit, reconcile}) {
            if (budget.maximumInputBytes() <= 0 || budget.maximumOutputTokens() <= 0
                    || budget.maximumCost().signum() < 0 || budget.maximumExecutionTimeMillis() <= 0) return false;
        }
        return chainMaximumCost.signum() > 0 && summedPhaseCost().compareTo(chainMaximumCost) <= 0;
    }

    /** Fixed wording for the authorization dialog; the same values the engine enforces. */
    public String describe() {
        return "L2_RAW_DISCOVERY: 1 call, output ≤ " + discovery.maximumOutputTokens() + " token, ≤ USD " + usd(discovery)
                + "\nL2_EDIT: 1 call, output ≤ " + edit.maximumOutputTokens() + " token, ≤ USD " + usd(edit)
                + "\nL3_RAW_FIRST_REAUDIT: 1 call, output ≤ " + reaudit.maximumOutputTokens() + " token, ≤ USD " + usd(reaudit)
                + "\nL3_RECONCILE: 1 call, output ≤ " + reconcile.maximumOutputTokens() + " token, ≤ USD " + usd(reconcile)
                + "\nInput ≤ " + discovery.maximumInputBytes() + " byte mỗi call • " + discovery.maximumExecutionTimeMillis() / 1000L
                + " s mỗi call • 0 repair • 0 retry"
                + "\nTrần cả chuỗi: USD " + chainMaximumCost.stripTrailingZeros().toPlainString();
    }

    private static String usd(EditorialL2Execution.Budget budget) {
        return budget.maximumCost().stripTrailingZeros().toPlainString();
    }
}
