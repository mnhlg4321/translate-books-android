package com.ml.tblandroidtxt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * A PopupMenu anchored to the whole content view (android.R.id.content) opened with zero height at the bottom of the screen on a real phone
 * (R0, 2026-10-29): "Sao lưu thư viện" could not be seen or tapped. Every menu must be anchored to the control that was tapped.
 */
public class PopupMenuAnchorSourceTest {
    private static File sourceDir() {
        File dir = new File("src/main/java/com/ml/tblandroidtxt");
        if (!dir.isDirectory()) dir = new File("app/src/main/java/com/ml/tblandroidtxt");
        return dir;
    }

    @Test public void noPopupMenuIsAnchoredToTheContentView() throws Exception {
        File[] files = sourceDir().listFiles((d, n) -> n.endsWith(".java"));
        assertTrue("source folder found", files != null && files.length > 50);
        for (File f : files) {
            String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            for (String line : text.split("\n")) {
                if (line.contains("PopupMenu(") && line.contains("android.R.id.content")) {
                    throw new AssertionError(f.getName() + " anchors a PopupMenu to the content view: " + line.trim());
                }
            }
        }
    }

    @Test public void theLibraryMenuTakesTheTappedButtonAsItsAnchor() throws Exception {
        String main = new String(Files.readAllBytes(new File(sourceDir(), "MainActivity.java").toPath()), StandardCharsets.UTF_8);
        assertTrue(main.contains("void showLibraryMenu(View anchor)"));
        assertTrue(main.contains("v->showLibraryMenu(v)"));
        assertFalse(main.contains("showLibraryMenu()"));
    }
}
