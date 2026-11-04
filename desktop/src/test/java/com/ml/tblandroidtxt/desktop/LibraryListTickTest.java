package com.ml.tblandroidtxt.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ml.tblandroidtxt.ColorMath;
import com.ml.tblandroidtxt.DesktopLibraryBulk;
import com.ml.tblandroidtxt.UiAppearanceCore;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * v4.25 lib-a follow-up: the Tập rows must keep their tick boxes (the page must not replace TickList's cell factory), a single set
 * ticks exactly its own id, and the red "Xóa (n)" button has a readable text colour on every palette.
 */
public class LibraryListTickTest {

    @Test public void togglingOneSetTicksExactlyThatId() {
        DesktopLibraryBulk.Selection sel = new DesktopLibraryBulk.Selection();
        sel.toggle("tap-1");
        assertTrue(sel.isSelected("tap-1"));
        assertFalse(sel.isSelected("tap-2"));
        assertEquals(1, sel.count());
        sel.toggle("tap-1");
        assertEquals(0, sel.count());
    }

    /** A page that replaces the cell factory of its ticked list would hide the tick boxes; the four Library pages must not do that. */
    @Test public void libraryPagesDoNotReplaceTheTickListCellFactory() throws Exception {
        for (String page : Arrays.asList("RawPage", "GlossaryPage", "PronounPage", "SetsPage")) {
            Path source = Paths.get("src/main/java/com/ml/tblandroidtxt/desktop", page + ".java");
            String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
            assertFalse(page + " must not call list.setCellFactory (TickList owns the row cell)", text.contains("list.setCellFactory("));
        }
    }

    /** The "Xóa (n)" button uses the palette's error colour; its text must reach 4.5:1 on every palette, light and dark. */
    @Test public void dangerButtonTextReachesAARatioOnEveryPalette() {
        for (UiAppearanceCore.Palette p : UiAppearanceCore.all()) {
            int onBad = ColorMath.onColor(p.bad);
            double ratio = ColorMath.ratio(p.bad, onBad);
            assertTrue(p.id + " danger text contrast " + ratio, ratio >= 4.5);
        }
    }
}
