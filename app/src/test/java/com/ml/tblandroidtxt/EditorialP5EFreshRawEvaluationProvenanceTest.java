package com.ml.tblandroidtxt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test-first guard for the official fresh-pilot evaluation record. The
 * evaluation ID is import-scoped and must not be copied from the historical
 * P5D lineage or treated as a pack-level constant.
 */
public final class EditorialP5EFreshRawEvaluationProvenanceTest {
    private static final String OFFICIAL_FRESH_EVALUATION =
            "3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1";

    @Test public void freshRunnerPinsOfficialSetupEvaluationRecord() {
        assertEquals(OFFICIAL_FRESH_EVALUATION,
                EditorialP5EFreshRawLiveRunner.COMPATIBILITY_EVALUATION_ID);
    }
}
