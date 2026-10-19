package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UsageTotalsTest {
    @Test public void moneyCountsKnownPricesOnlyAndUnknownCallsAreCountedSeparately() {
        UsageTotals t = new UsageTotals();
        t.add("PROVIDER_REPORTED", 0.0100, 1000, 200, 300);
        t.add("USAGE_DERIVED", 0.0023, 500, 0, 100);
        t.add("UNKNOWN_USAGE", 0.5, 400, 0, 50);       // a price that is not trustworthy is not added
        t.add("UNKNOWN_PRICE", 0.9, 100, 0, 10);
        assertEquals(0.0123, t.costUsd, 1e-9);
        assertEquals(2, t.unknownPriceCalls);
        assertEquals(4, t.calls);
        assertEquals(2000, t.inputTokens);
        assertEquals(200, t.cachedInputTokens);
        assertEquals(460, t.outputTokens);
    }

    @Test public void aRetryIsRealMoneyAndCountsOncePerCall() {
        UsageTotals t = new UsageTotals();
        // chunk 3 failed after the provider billed it, then succeeded on the retry: two calls, two rows, two charges
        t.add("PROVIDER_REPORTED", 0.004, 800, 0, 0);
        t.add("PROVIDER_REPORTED", 0.005, 800, 0, 250);
        assertEquals(0.009, t.costUsd, 1e-9);
        assertEquals(2, t.calls);
    }

    @Test public void negativeValuesNeverReduceTheTotals() {
        UsageTotals t = new UsageTotals();
        t.add("PROVIDER_REPORTED", -1, -5, -5, -5);
        assertEquals(0d, t.costUsd, 0d);
        assertEquals(0, t.inputTokens);
    }

    @Test public void combiningTotalsAddsEveryField() {
        UsageTotals a = new UsageTotals(), b = new UsageTotals();
        a.add("PROVIDER_REPORTED", 0.01, 10, 1, 2); a.chaptersDone = 1;
        b.add("UNKNOWN_USAGE", 0, 5, 0, 1); b.chaptersDone = 2;
        a.add(b);
        assertEquals(15, a.inputTokens);
        assertEquals(3, a.chaptersDone);
        assertEquals(1, a.unknownPriceCalls);
        assertEquals(2, a.calls);
    }

    @Test public void theLineShowsTokensCostAndUnknownCalls() {
        UsageTotals t = new UsageTotals();
        t.add("PROVIDER_REPORTED", 0.0123, 12300, 4100, 2000);
        t.add("UNKNOWN_USAGE", 0, 0, 0, 0);
        String line = t.line();
        assertTrue(line, line.contains("12.3k vào"));
        assertTrue(line, line.contains("(4.1k cached)"));
        assertTrue(line, line.contains("2.0k ra"));
        assertTrue(line, line.contains("USD 0.0123"));
        assertTrue(line, line.contains("1 lượt chưa rõ giá"));
        assertFalse(new UsageTotals().line().contains("chưa rõ giá"));
    }
}
