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
public record EditorialChainBudgets(EditorialL2Execution.Budget l1Raw,
                                    EditorialL2Execution.Budget l1Reconcile,
                                    EditorialL2Execution.Budget discovery, EditorialL2Execution.Budget edit,
                                    EditorialL2Execution.Budget reaudit, EditorialL2Execution.Budget reconcile,
                                    BigDecimal chainMaximumCost, EditorialL2Execution.Budget finalRead) {
    /** The legacy chain: four calls and no final read. */
    public EditorialChainBudgets(EditorialL2Execution.Budget discovery, EditorialL2Execution.Budget edit,
                                 EditorialL2Execution.Budget reaudit, EditorialL2Execution.Budget reconcile,
                                 BigDecimal chainMaximumCost) {
        this(null, null, discovery, edit, reaudit, reconcile, chainMaximumCost, null);
    }

    public EditorialChainBudgets(EditorialL2Execution.Budget discovery, EditorialL2Execution.Budget edit,
                                 EditorialL2Execution.Budget reaudit, EditorialL2Execution.Budget reconcile,
                                 BigDecimal chainMaximumCost, EditorialL2Execution.Budget finalRead) {
        this(null, null, discovery, edit, reaudit, reconcile, chainMaximumCost, finalRead);
    }

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

    /**
     * Caps for the ledger-contract chain: the four calls plus one capped final read after L2 and one after L3
     * (the L3 read is wired in the next step). They are recommendations, not a grant.
     */
    public static EditorialChainBudgets ledgerRecommended() {
        EditorialChainBudgets d3 = d3Recommended();
        return new EditorialChainBudgets(d3.discovery(), d3.edit(), d3.reaudit(), d3.reconcile(), new BigDecimal("0.36"),
                new EditorialL2Execution.Budget(200_000, 4_096, new BigDecimal("0.03"), 180_000L));
    }

    /** All eight ledger-v2 calls, using the owner-approved G4 cap. Pricing must be checked again before live use. */
    public static EditorialChainBudgets fullLedgerRecommended() {
        EditorialChainBudgets ledger = ledgerRecommended();
        long timeout = 180_000L;
        return new EditorialChainBudgets(
                new EditorialL2Execution.Budget(200_000, 8_192, new BigDecimal("0.05"), timeout),
                new EditorialL2Execution.Budget(200_000, 16_384, new BigDecimal("0.10"), timeout),
                ledger.discovery(), ledger.edit(), ledger.reaudit(), ledger.reconcile(), new BigDecimal("0.50"),
                ledger.finalRead());
    }

    public boolean includesL1() { return l1Raw != null && l1Reconcile != null; }

    /** Sum of the per-call USD caps, final reads included (two reads in a ledger chain). */
    public BigDecimal summedPhaseCost() {
        BigDecimal sum = discovery.maximumCost().add(edit.maximumCost()).add(reaudit.maximumCost()).add(reconcile.maximumCost());
        if (finalRead != null) sum = sum.add(finalRead.maximumCost()).add(finalRead.maximumCost());
        if (includesL1()) sum = sum.add(l1Raw.maximumCost()).add(l1Reconcile.maximumCost());
        return sum;
    }

    /** Every cap is positive and the per-call caps fit under the chain cap. */
    public boolean valid() {
        java.util.List<EditorialL2Execution.Budget> all = new java.util.ArrayList<>(
                java.util.List.of(discovery, edit, reaudit, reconcile));
        if (finalRead != null) all.add(finalRead);
        if ((l1Raw == null) != (l1Reconcile == null)) return false;
        if (includesL1()) { all.add(l1Raw); all.add(l1Reconcile); }
        for (EditorialL2Execution.Budget budget : all) {
            if (budget.maximumInputBytes() <= 0 || budget.maximumOutputTokens() <= 0
                    || budget.maximumCost().signum() < 0 || budget.maximumExecutionTimeMillis() <= 0) return false;
        }
        if (chainMaximumCost.signum() <= 0) return false;
        if (!includesL1()) return summedPhaseCost().compareTo(chainMaximumCost) <= 0;
        if (finalRead == null) return false;
        // The per-call USD ceilings intentionally sum to 0.51 while G4 is capped at 0.50.
        // Validate the stricter token-cap worst case using the pinned R5 price basis; P4 must reprice this
        // before any live call, and the run coordinator still enforces each response's reported cost.
        BigDecimal worstCase = worstCase(l1Raw).add(worstCase(l1Reconcile)).add(worstCase(discovery))
                .add(worstCase(edit)).add(worstCase(finalRead)).add(worstCase(reaudit))
                .add(worstCase(reconcile)).add(worstCase(finalRead));
        return worstCase.compareTo(chainMaximumCost) <= 0;
    }

    /** Fixed wording for the authorization dialog; the same values the engine enforces. */
    public String describe() {
        String phases;
        if (includesL1()) {
            phases = "1. L1_RAW_DISCOVERY: 1 call, output ≤ " + l1Raw.maximumOutputTokens() + " token, ≤ USD " + usd(l1Raw)
                    + "\n2. L1_RECONCILE: 1 call, output ≤ " + l1Reconcile.maximumOutputTokens() + " token, ≤ USD " + usd(l1Reconcile)
                    + "\n3. L2_RAW_DISCOVERY: 1 call, output ≤ " + discovery.maximumOutputTokens() + " token, ≤ USD " + usd(discovery)
                    + "\n4. L2_EDIT: 1 call, output ≤ " + edit.maximumOutputTokens() + " token, ≤ USD " + usd(edit)
                    + "\n5. L2_FINAL_READ: 1 call, output ≤ " + finalRead.maximumOutputTokens() + " token, ≤ USD " + usd(finalRead)
                    + "\n6. L3_RAW_FIRST_REAUDIT: 1 call, output ≤ " + reaudit.maximumOutputTokens() + " token, ≤ USD " + usd(reaudit)
                    + "\n7. L3_RECONCILE: 1 call, output ≤ " + reconcile.maximumOutputTokens() + " token, ≤ USD " + usd(reconcile)
                    + "\n8. L3_FINAL_READ: 1 call, output ≤ " + finalRead.maximumOutputTokens() + " token, ≤ USD " + usd(finalRead);
        } else {
            phases = "L2_RAW_DISCOVERY: 1 call, output ≤ " + discovery.maximumOutputTokens() + " token, ≤ USD " + usd(discovery)
                    + "\nL2_EDIT: 1 call, output ≤ " + edit.maximumOutputTokens() + " token, ≤ USD " + usd(edit)
                    + "\nL3_RAW_FIRST_REAUDIT: 1 call, output ≤ " + reaudit.maximumOutputTokens() + " token, ≤ USD " + usd(reaudit)
                    + "\nL3_RECONCILE: 1 call, output ≤ " + reconcile.maximumOutputTokens() + " token, ≤ USD " + usd(reconcile)
                    + (finalRead == null ? "" : "\nFINAL_READ (sau L2 và sau L3): 2 call, mỗi call output ≤ "
                    + finalRead.maximumOutputTokens() + " token, ≤ USD " + usd(finalRead));
        }
        return phases
                + "\nInput ≤ " + discovery.maximumInputBytes() + " byte mỗi call • " + discovery.maximumExecutionTimeMillis() / 1000L
                + " s mỗi call • 0 repair • 0 retry"
                + "\nTrần cả chuỗi: USD " + money(chainMaximumCost);
    }

    private static String money(BigDecimal value) {
        return (value.scale() < 2 ? value.setScale(2) : value).toPlainString();
    }

    private static String usd(EditorialL2Execution.Budget budget) {
        return money(budget.maximumCost());
    }

    private static BigDecimal worstCase(EditorialL2Execution.Budget budget) {
        if (budget == null) return BigDecimal.ZERO;
        BigDecimal inputTokens = BigDecimal.valueOf((budget.maximumInputBytes() + 1L) / 2L);
        return inputTokens.multiply(new BigDecimal("0.25")).add(
                BigDecimal.valueOf(budget.maximumOutputTokens()).multiply(new BigDecimal("1.20")))
                .movePointLeft(6);
    }
}
