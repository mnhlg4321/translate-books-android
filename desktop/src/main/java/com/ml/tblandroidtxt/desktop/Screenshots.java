package com.ml.tblandroidtxt.desktop;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** QA helper (TBL_SCREENSHOT_DIR): renders each page of the real window to a PNG, then exits. Nothing is clicked or started. */
final class Screenshots {
    private final DesktopApp app;
    private final Path dir;

    Screenshots(DesktopApp app, Path dir) { this.app = app; this.dir = dir; }

    void run() {
        step(0);
    }

    private void step(int index) {
        if (index >= app.pageCount()) {
            Platform.exit();
            return;
        }
        app.select(index);
        PauseTransition wait = new PauseTransition(Duration.millis(700));
        wait.setOnFinished(e -> {
            try {
                Files.createDirectories(dir);
                save(app.stage().getScene().getRoot().snapshot(null, null), dir.resolve((index + 1) + "-" + app.pageName(index) + ".png"));
            } catch (Exception ex) {
                System.err.println("screenshot failed: " + ex);
            }
            step(index + 1);
        });
        wait.play();
    }

    private static void save(WritableImage image, Path file) throws Exception {
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        PixelReader reader = image.getPixelReader();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) out.setRGB(x, y, reader.getArgb(x, y));
        javax.imageio.ImageIO.write(out, "png", file.toFile());
    }
}
