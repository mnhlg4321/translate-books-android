package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** The Library tab row: labels keep room from the border, the four tabs are equal, the row is tall enough for the tabs, and it scrolls only when it must. */
public class LibraryTabLayoutModelTest {
    private static final int[] SCREENS_DP = {320, 361, 411};
    private static final float[] APP_SCALES = {0.9f, 1.0f, 1.15f, 1.3f};
    private static final float[] SYSTEM_SCALES = {1.0f, 1.3f};

    @Test public void labelsKeepAtLeastEightDpFromTheTabBorderTwiceTheOldFour() {
        assertTrue(LibraryTabLayoutModel.TAB_HORIZONTAL_PADDING_DP >= 8);
    }

    /** v4.24.0 clipped the bottom 2 dp of every tab: 4 + 6 dp of padding left a 46 dp row for 48 dp tabs. */
    @Test public void theRowIsTallEnoughForTheTabsAndTheMenuWithItsPadding() {
        assertEquals(LibraryTabLayoutModel.NAV_TOP_PADDING_DP + LibraryTabLayoutModel.ROW_HEIGHT_DP + LibraryTabLayoutModel.NAV_BOTTOM_PADDING_DP,
                LibraryTabLayoutModel.navHeightDp());
        assertTrue(LibraryTabLayoutModel.navHeightDp() - LibraryTabLayoutModel.NAV_TOP_PADDING_DP - LibraryTabLayoutModel.NAV_BOTTOM_PADDING_DP
                >= LibraryTabLayoutModel.ROW_HEIGHT_DP);
        assertTrue(LibraryTabLayoutModel.ROW_HEIGHT_DP >= 48);
    }

    @Test public void ownerPhoneAtDefaultSizesFitsWithoutScrolling() {
        assertFalse(LibraryTabLayoutModel.needsHorizontalScroll(1.0f, 1.0f, 361));
        assertFalse(LibraryTabLayoutModel.needsHorizontalScroll(0.9f, 1.0f, 361));
        assertFalse(LibraryTabLayoutModel.needsHorizontalScroll(1.0f, 1.0f, 411));
    }

    @Test public void largeTextOrNarrowScreensScroll() {
        assertTrue(LibraryTabLayoutModel.needsHorizontalScroll(1.3f, 1.0f, 361));
        assertTrue(LibraryTabLayoutModel.needsHorizontalScroll(1.15f, 1.0f, 361));
        assertTrue(LibraryTabLayoutModel.needsHorizontalScroll(1.0f, 1.3f, 320));
        assertTrue(LibraryTabLayoutModel.shouldUseHorizontalScroll(1.3f, 1.3f, 360));
    }

    @Test public void everyTabIsEqualAndKeepsTheMinimumTouchWidthAtEverySize() {
        for (float app : APP_SCALES) for (float system : SYSTEM_SCALES) {
            int equal = LibraryTabLayoutModel.equalTabWidthDp(app, system);
            assertTrue(equal >= LibraryTabLayoutModel.TAB_MIN_WIDTH_DP);
            for (String label : new String[]{"RAW", "Glossary", "Pronoun", "Tập"}) {
                assertTrue(LibraryTabLayoutModel.tabWidthDp(label, app, system) <= equal);
            }
        }
    }

    @Test public void theScrollDecisionNeverFlipsBackWhenTheTextGetsBiggerOrTheScreenNarrower() {
        for (int screen : SCREENS_DP) for (float system : SYSTEM_SCALES) {
            boolean scrolled = false;
            for (float app : APP_SCALES) {
                boolean now = LibraryTabLayoutModel.needsHorizontalScroll(app, system, screen);
                assertTrue("bigger text must not need less room", now || !scrolled);
                scrolled = now;
            }
        }
        for (float app : APP_SCALES) for (float system : SYSTEM_SCALES) {
            boolean narrowScrolls = false;
            for (int i = SCREENS_DP.length - 1; i >= 0; i--) {
                boolean now = LibraryTabLayoutModel.needsHorizontalScroll(app, system, SCREENS_DP[i]);
                assertTrue("a narrower screen must not need less room", now || !narrowScrolls);
                narrowScrolls = now;
            }
        }
    }

    /** The screen builds the row from the model's numbers; nothing in the builder may bring back a fixed 56 dp row or a 4 dp tab padding. */
    @Test public void theScreenUsesTheModelForRowHeightPaddingAndEqualTabs() throws Exception {
        String main = new String(Files.readAllBytes(Paths.get("src", "main", "java", "com", "ml", "tblandroidtxt", "MainActivity.java")), StandardCharsets.UTF_8);
        int from = main.indexOf("View buildLibraryDestination(String selected)");
        int to = main.indexOf("silentPersistCurrentUi", from);
        String body = main.substring(from, to);
        assertTrue(body.contains("LibraryTabLayoutModel.navHeightDp()"));
        assertTrue(body.contains("LibraryTabLayoutModel.ROW_HEIGHT_DP"));
        assertTrue(body.contains("setFillViewport(true)"));
        assertTrue("the chosen tab is scrolled into view", body.contains("tabScroll.scrollTo(overflow,0)"));
        assertTrue(body.contains("LibraryTabLayoutModel.TAB_HORIZONTAL_PADDING_DP"));
        assertFalse(body.contains("dp(56)"));
    }
}
