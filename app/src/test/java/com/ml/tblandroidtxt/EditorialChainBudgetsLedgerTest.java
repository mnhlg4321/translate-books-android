package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** The legacy chain keeps its numbers; the ledger chain adds two capped final reads under a higher chain cap. */
public final class EditorialChainBudgetsLedgerTest {
    @Test public void legacyBudgetsAreUnchanged() {
        EditorialChainBudgets d3 = EditorialChainBudgets.d3Recommended();
        assertNull(d3.finalRead());
        assertTrue(d3.valid());
        assertEquals(new BigDecimal("0.30"), d3.summedPhaseCost());
        assertFalse(d3.describe().contains("FINAL_READ"));
    }

    @Test public void ledgerBudgetsCountTwoFinalReadsAndStayWithinTheChainCap() {
        EditorialChainBudgets ledger = EditorialChainBudgets.ledgerRecommended();
        assertTrue(ledger.valid());
        assertEquals(new BigDecimal("0.36"), ledger.summedPhaseCost());
        assertEquals(new BigDecimal("0.36"), ledger.chainMaximumCost());
        assertTrue(ledger.describe().contains("FINAL_READ"));
        assertTrue(ledger.describe().contains("2 call"));
        // the same four calls under the legacy chain cap are over budget once the reads are added
        EditorialChainBudgets tight = new EditorialChainBudgets(ledger.discovery(), ledger.edit(), ledger.reaudit(),
                ledger.reconcile(), new BigDecimal("0.30"), ledger.finalRead());
        assertFalse(tight.valid());
        EditorialL2Execution.Budget zero = new EditorialL2Execution.Budget(0, 4_096, new BigDecimal("0.03"), 180_000L);
        assertFalse(new EditorialChainBudgets(ledger.discovery(), ledger.edit(), ledger.reaudit(), ledger.reconcile(),
                new BigDecimal("0.36"), zero).valid());
    }

    @Test public void fullLedgerBudgetListsEightCallCapsAndFitsG4AtThePinnedWorstCase() {
        EditorialChainBudgets full = EditorialChainBudgets.fullLedgerRecommended();
        assertTrue(full.includesL1());
        assertTrue(full.valid());
        assertEquals(new BigDecimal("0.51"), full.summedPhaseCost());
        assertEquals(new BigDecimal("0.50"), full.chainMaximumCost());
        String description = full.describe();
        assertTrue(description.contains("1. L1_RAW_DISCOVERY: 1 call, output ≤ 8192 token, ≤ USD 0.05"));
        assertTrue(description.contains("2. L1_RECONCILE: 1 call, output ≤ 16384 token, ≤ USD 0.10"));
        assertTrue(description.contains("8. L3_FINAL_READ: 1 call, output ≤ 4096 token, ≤ USD 0.03"));
        assertFalse(description.contains("FINAL_READ (sau L2 và sau L3): 2 call"));
    }
}
