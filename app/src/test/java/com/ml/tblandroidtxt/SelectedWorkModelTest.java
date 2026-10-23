package com.ml.tblandroidtxt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class SelectedWorkModelTest {
    @Test public void txtPendingFromAnotherFileDoesNotOwnThePrimaryBar() {
        SelectedWorkModel.Selection selected = SelectedWorkModel.txt(Collections.singletonList("hash-current"), "005.txt");
        assertFalse(SelectedWorkModel.matches(selected,
                new SelectedWorkModel.Pending(SelectedWorkModel.Mode.TXT, "", "hash-other", "020.txt")));
    }

    @Test public void txtPendingForTheSelectedFileMatches() {
        SelectedWorkModel.Selection selected = SelectedWorkModel.txt(Collections.singletonList("hash-current"), "005.txt");
        assertTrue(SelectedWorkModel.matches(selected,
                new SelectedWorkModel.Pending(SelectedWorkModel.Mode.TXT, "", "hash-current", "005.txt")));
    }

    @Test public void setPendingFromAnotherSetDoesNotMatchEvenWhenRawIsShared() {
        SelectedWorkModel.Selection selected = SelectedWorkModel.set("set-current", Arrays.asList("hash-shared", "hash-two"), "Tập hiện tại");
        assertFalse(SelectedWorkModel.matches(selected,
                new SelectedWorkModel.Pending(SelectedWorkModel.Mode.SET, "set-other", "hash-shared", "005.txt")));
    }

    @Test public void setPendingForTheSelectedSetAndRawMatches() {
        SelectedWorkModel.Selection selected = SelectedWorkModel.set("set-current", Arrays.asList("hash-one", "hash-two"), "Tập hiện tại");
        assertTrue(SelectedWorkModel.matches(selected,
                new SelectedWorkModel.Pending(SelectedWorkModel.Mode.SET, "set-current", "hash-two", "005.txt")));
    }

    @Test public void runControlIgnoresAnUnrelatedPendingJob() {
        RunControlModel.State state = RunControlModel.resolve(false, "", false,
                true, 1, true, "", false);
        assertTrue(state == RunControlModel.State.READY);
    }
}
