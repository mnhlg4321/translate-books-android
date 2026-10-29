package com.ml.tblandroidtxt.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/** The window icon PNGs are resources of the app, and the .ico handed to jpackage holds every size Windows asks for. */
public class DesktopIconsTest {
    @Test public void everyWindowIconSizeIsAResourceWithItsOwnSize() throws Exception {
        for (int size : DesktopIcons.SIZES) {
            try (InputStream in = DesktopIcons.class.getResourceAsStream(DesktopIcons.resourcePath(size))) {
                assertNotNull("missing " + DesktopIcons.resourcePath(size), in);
                BufferedImage img = javax.imageio.ImageIO.read(in);
                assertEquals(size, img.getWidth());
                assertEquals(size, img.getHeight());
                boolean opaque = false;
                for (int y = 0; y < img.getHeight() && !opaque; y++) for (int x = 0; x < img.getWidth(); x++) if ((img.getRGB(x, y) >>> 24) > 200) { opaque = true; break; }
                assertTrue("the logo is drawn, not empty: " + size, opaque);
            }
        }
    }

    @Test public void theIcoForTheInstallerHoldsAllSevenSizes() throws Exception {
        Path ico = Paths.get("packaging", "translate_books.ico");
        assertTrue("run scripts/make-desktop-icons.py", Files.exists(ico));
        ByteBuffer b = ByteBuffer.wrap(Files.readAllBytes(ico)).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, b.getShort(0));
        assertEquals("type 1 = icon", 1, b.getShort(2));
        int count = b.getShort(4);
        assertEquals(DesktopIcons.SIZES.length, count);
        Set<Integer> sizes = new HashSet<>();
        for (int i = 0; i < count; i++) {
            int w = b.get(6 + 16 * i) & 0xff;
            sizes.add(w == 0 ? 256 : w);
        }
        for (int s : DesktopIcons.SIZES) assertTrue("size " + s, sizes.contains(s));
    }
}
