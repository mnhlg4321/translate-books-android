package com.ml.tblandroidtxt;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class EditorialP6GroupCapPolicyTest {
    @Test public void runnerAcceptsApprovedQ2CapAndRejectsLargerOrNonPositive() {
        assertTrue(EditorialP6GroupSpendLedger.isValidRunnerGroupCap(new BigDecimal("6.00")));
        assertTrue(EditorialP6GroupSpendLedger.isValidRunnerGroupCap(new BigDecimal("2.00")));
        assertFalse(EditorialP6GroupSpendLedger.isValidRunnerGroupCap(new BigDecimal("6.01")));
        assertFalse(EditorialP6GroupSpendLedger.isValidRunnerGroupCap(BigDecimal.ZERO));
    }
}
