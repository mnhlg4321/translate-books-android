package com.ml.tblandroidtxt.desktop;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** The Translate Books logo in the sizes Windows asks for (window title bar, taskbar, Alt+Tab, dialogs). Built from the phone logo by scripts/make-desktop-icons.py. */
final class DesktopIcons {
    static final int[] SIZES = {16, 24, 32, 48, 64, 128, 256};

    private DesktopIcons() { }

    static String resourcePath(int size) { return "/desktop/icons/logo-" + size + ".png"; }

    /** One image per size; a size whose file is missing is left out. */
    static List<Image> load() {
        List<Image> out = new ArrayList<>();
        for (int s : SIZES) {
            try (InputStream in = DesktopIcons.class.getResourceAsStream(resourcePath(s))) {
                if (in != null) out.add(new Image(in));
            } catch (Exception ignored) {
                // a missing icon must never stop the window from opening
            }
        }
        return out;
    }
}
