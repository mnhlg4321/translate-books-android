package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialRawInventory;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class OpenRouterLedgerCoverageFactsTest {
    private static final byte[] RAW = ("first line\n\n[IMAGE: cover]\nsecond line\r\n　\nthird line")
            .getBytes(StandardCharsets.UTF_8);

    @Test public void factsNameTheExactUnitCountAndTheFirstAndLastIdsOfTheValidatorInventory() {
        List<EditorialRawInventory.Unit> units = EditorialRawInventory.build(RAW).units();
        String facts = OpenRouterEditorialP5PilotProvider.coverageFacts(RAW);

        assertEquals(3, units.size());
        assertTrue(facts.contains("exactly 3 units"));
        assertTrue(facts.contains("the first is " + units.get(0).id()));
        assertTrue(facts.contains("the last is " + units.get(2).id()));
        assertTrue(facts.contains("never skip or repeat a unit"));
        assertTrue(facts.contains("at most 80 characters"));
    }

    @Test public void factsAreEmptyWithoutRawOrUnits() {
        assertEquals("", OpenRouterEditorialP5PilotProvider.coverageFacts(null));
        assertEquals("", OpenRouterEditorialP5PilotProvider.coverageFacts("\n\n".getBytes(StandardCharsets.UTF_8)));
    }
}
