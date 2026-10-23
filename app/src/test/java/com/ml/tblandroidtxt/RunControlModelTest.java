package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pure state coverage for the one shared run-control bar. */
public class RunControlModelTest {
    @Test public void everyVisibleStateHasTheDocumentedControls() {
        assertEquals(RunControlModel.Action.START, RunControlModel.controls(RunControlModel.State.READY, 0).primaryAction);
        RunControlModel.Controls blocked = RunControlModel.controls(RunControlModel.State.BLOCKED, 0);
        assertEquals(RunControlModel.Action.START, blocked.primaryAction);
        assertFalse(blocked.primaryEnabled);
        assertEquals(RunControlModel.Action.STOP, RunControlModel.controls(RunControlModel.State.PREPARING, 0).secondaryAction);
        assertEquals("Đang dừng…", RunControlModel.controls(RunControlModel.State.STOPPING, 0).message);
        assertEquals(RunControlModel.Action.REVIEW, RunControlModel.controls(RunControlModel.State.UNKNOWN_DELIVERY, 0).primaryAction);
        assertEquals(RunControlModel.Action.PAUSE, RunControlModel.controls(RunControlModel.State.RUNNING, 0).primaryAction);
        assertEquals(RunControlModel.Action.RESUME, RunControlModel.controls(RunControlModel.State.PAUSED, 0).primaryAction);
        assertEquals("Dịch lại 3 chunk lỗi", RunControlModel.controls(RunControlModel.State.WORK_REMAINS, 3).primaryLabel);
        assertEquals(RunControlModel.Action.OPEN_RESULT, RunControlModel.controls(RunControlModel.State.COMPLETED, 0).primaryAction);
    }

    @Test public void theBarMappingDoesNotDependOnChapterSetMode() {
        for (RunControlModel.State state : RunControlModel.State.values()) {
            assertEquals(RunControlModel.controls(state, 2).signature(),
                    RunControlModel.controlsForMode(state, 2, false).signature());
            assertEquals(RunControlModel.controls(state, 2).signature(),
                    RunControlModel.controlsForMode(state, 2, true).signature());
        }
    }

    @Test public void runtimeResolutionPrioritizesSafetyStates() {
        assertEquals(RunControlModel.State.STOPPING,
                RunControlModel.resolve(true, "STOPPING", false, false, 0, true, ""));
        assertEquals(RunControlModel.State.UNKNOWN_DELIVERY,
                RunControlModel.resolve(false, "COMPLETED", true, true, 1, true, ""));
        assertEquals(RunControlModel.State.WORK_REMAINS,
                RunControlModel.resolve(false, "CANCELLED", false, true, 0, true, ""));
        assertEquals(RunControlModel.State.BLOCKED,
                RunControlModel.resolve(false, "", false, false, 0, false, "Chưa chọn TXT"));
        assertEquals(RunControlModel.State.READY,
                RunControlModel.resolve(false, "", false, false, 0, true, ""));
    }
}
