package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** F5: the run clock is based on durable lifecycle markers, not on screen reopen time. */
public class RuntimeFixF5Test {
    @Test public void runningContinuesFromTheSessionStart() {
        assertEquals(9_000L, RunPanelFormatter.runElapsedMs("RUNNING", 10_000L, 1_000L, 0L, 0L, 0L, 0L));
        assertEquals(9_000L, RunPanelFormatter.runElapsedMs("stopping", 10_000L, 1_000L, 0L, 0L, 0L, 0L));
    }

    @Test public void pausedFreezesAtThePauseMarkerAndSubtractsEarlierPauses() {
        assertEquals(2_000L, RunPanelFormatter.runElapsedMs("PAUSED", 18_000L, 1_000L, 0L, 4_000L, 0L, 1_000L));
        assertEquals(2_000L, RunPanelFormatter.runElapsedMs("PAUSED", 18_000L, 1_000L, 0L, 0L, 2_000L, 1_000L));
    }

    @Test public void terminalStatesStayFixedAfterEighteenHours() {
        long start = 1_000L;
        long end = 7_000L;
        long frozen = 5_000L;
        assertEquals(frozen, RunPanelFormatter.runElapsedMs("COMPLETED", end + 18 * 60 * 60 * 1000L, start, end, 0L, 0L, 1_000L));
        assertEquals(frozen, RunPanelFormatter.runElapsedMs("FAILED", end + 18 * 60 * 60 * 1000L, start, end, 0L, 0L, 1_000L));
        assertEquals(frozen, RunPanelFormatter.runElapsedMs("CANCELLED", end + 18 * 60 * 60 * 1000L, start, end, 0L, 0L, 1_000L));
    }

    @Test public void missingStartUsesTheStoredElapsedAndNeverInventsWallTime() {
        assertEquals(1_234L, RunPanelFormatter.runElapsedMs("RUNNING", 9_999_999L, 0L, 0L, 0L, 1_234L, 0L));
        assertEquals(1_234L, RunPanelFormatter.runElapsedMs("COMPLETED", 9_999_999L, 0L, 0L, 0L, 1_234L, 0L));
    }

    @Test public void snapshotRoundTripsSessionMarkers() {
        RuntimeStateSnapshot snapshot = new RuntimeStateSnapshot();
        snapshot.sessionEndMs = 7_000L;
        snapshot.pausedAtMs = 4_000L;
        snapshot.pausedTotalMs = 1_000L;
        RuntimeStateSnapshot restored = RuntimeStateSnapshot.fromMap(snapshot.toMap());
        assertEquals(7_000L, restored.sessionEndMs);
        assertEquals(4_000L, restored.pausedAtMs);
        assertEquals(1_000L, restored.pausedTotalMs);
    }
}
