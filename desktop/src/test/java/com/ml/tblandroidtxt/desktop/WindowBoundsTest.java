package com.ml.tblandroidtxt.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.ml.tblandroidtxt.MemoryKeyValueStore;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class WindowBoundsTest {
    @Test public void sizeAndPlaceRoundTripAndNothingSavedMeansDefault() {
        MemoryKeyValueStore p = new MemoryKeyValueStore();
        assertNull(WindowBounds.load(p));
        new WindowBounds(120, 80, 1400, 800, true).save(p);
        WindowBounds b = WindowBounds.load(p);
        assertEquals(120, b.x, 0);
        assertEquals(1400, b.width, 0);
        assertTrue(b.maximized);
        p.edit().putString("win_w", "10").commit();
        assertNull("a window that small is not restored", WindowBounds.load(p));
        p.edit().putString("win_w", "abc").commit();
        assertNull(WindowBounds.load(p));
    }

    @Test public void aPlaceOnAnUnpluggedMonitorIsIgnored() {
        double[] main = {0, 0, 1920, 1080};
        double[] second = {1920, 0, 1920, 1080};
        WindowBounds onSecond = new WindowBounds(2100, 100, 1280, 720, false);
        assertTrue(onSecond.visibleOn(Arrays.asList(main, second)));
        assertFalse(onSecond.visibleOn(Collections.singletonList(main)));
        assertTrue(new WindowBounds(-100, 50, 1280, 720, false).visibleOn(Collections.singletonList(main)));
        assertFalse(new WindowBounds(5000, 5000, 1280, 720, false).visibleOn(Arrays.asList(main, second)));
    }
}
