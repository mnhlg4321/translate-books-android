package com.ml.tblandroidtxt.desktop;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * QA helper (TBL_SCREENSHOT_DIR): renders the real window to PNG files, then exits. Nothing is started and no key is shown. The data
 * folder is a throw-away TBL_DATA_DIR already filled by QaSeed (TBL_QA_SEED_DIR holds its files); dialogs are shown without blocking,
 * photographed, and closed as if cancelled, so "Bắt đầu" is never confirmed.
 */
final class Screenshots {
    private final DesktopApp app;
    private final Path dir;
    private final Deque<Runnable> steps = new ArrayDeque<>();
    private String dialogName;

    Screenshots(DesktopApp app, Path dir) { this.app = app; this.dir = dir; }

    void run() {
        Path seed = seedDir();
        seed();
        queue(seed);
        next();
    }

    private static Path seedDir() {
        String s = System.getenv("TBL_QA_SEED_DIR");
        return s == null || s.trim().isEmpty() ? null : Path.of(s.trim());
    }

    private void seed() {
        try {
            app.runtime().secrets().save("TEST_KEY_NOT_SECRET");
            app.refreshLibrary();
            app.volumePage().refreshRaw();
        } catch (Exception e) {
            System.err.println("seed failed: " + e);
        }
    }

    private void queue(Path seed) {
        app.setDialogProbe(d -> {
            String name = dialogName;
            PauseTransition w = new PauseTransition(Duration.millis(500));
            w.setOnFinished(e -> {
                try {
                    Files.createDirectories(dir);
                    save(d.getDialogPane().snapshot(null, null), dir.resolve(name + ".png"));
                } catch (Exception ex) {
                    System.err.println("dialog screenshot failed: " + ex);
                }
                d.close();
            });
            w.play();
        });
        // one image per page, in the default theme
        for (int i = 0; i < app.pageCount(); i++) {
            final int index = i;
            steps.add(() -> {
                app.select(index);
                if (app.pageName(index).equals("Dịch cả tập")) prepareVolume(seed);
                if (app.pageName(index).equals("Công việc")) { app.showHistoryTab(); app.historyPanel().selectFirst(); }
            });
            steps.add(() -> shot((index + 1) + "-" + app.pageName(index)));
        }
        // the dialogs the owner meets: profile preview, library preview, the start confirmation (cancelled)
        if (seed != null) {
            steps.add(() -> app.select(4));
            steps.add(() -> {
                try {
                    dialogName = "dialog-nhap-ho-so";
                    String json = new String(Files.readAllBytes(seed.resolve("QA_profile_phone.json")), java.nio.charset.StandardCharsets.UTF_8);
                    app.showProfilePreview(app.runtime().previewProfile(json));
                } catch (Exception e) {
                    System.err.println("profile preview failed: " + e);
                }
            });
            steps.add(() -> { });
            steps.add(() -> app.select(3));
            steps.add(() -> {
                dialogName = "dialog-nhap-goi-thu-vien";
                app.importLibraryArchive(seed.resolve("QA_library_phone.zip"), true);
            });
            steps.add(() -> { });
            steps.add(() -> {
                app.select(1);
                app.volumePage().buildPlan();
                app.volumePage().plan().resolveMissing("", "");
                app.volumePage().refreshTable();
                app.volumePage().refreshEstimate(false);
                dialogName = "dialog-xac-nhan-bat-dau";
                app.volumePage().startForQa();
            });
            steps.add(() -> { });
        }
        // the five light themes at 100% and 130%, on the volume page (tables, status colours, buttons)
        steps.add(() -> app.select(1));
        for (var theme : DesktopTheme.themes()) {
            for (int percent : new int[]{100, 130}) {
                steps.add(() -> app.setAppearance(theme.id, percent));
                steps.add(() -> shot("tema-" + theme.id + "-" + percent));
            }
        }
        steps.add(() -> app.setAppearance(DesktopTheme.DEFAULT_THEME_ID, DesktopTheme.DEFAULT_FONT_PERCENT));
        steps.add(Platform::exit);
    }

    private void prepareVolume(Path seed) {
        try {
            app.volumePage().refreshRaw();
            app.volumePage().buildPlan();
            app.volumePage().setOutputDir(seed == null ? dir : seed.resolve("out"));
        } catch (Exception e) {
            System.err.println("volume prepare failed: " + e);
        }
    }

    private void shot(String name) {
        try {
            Files.createDirectories(dir);
            save(app.stage().getScene().getRoot().snapshot(null, null), dir.resolve(name + ".png"));
        } catch (Exception ex) {
            System.err.println("screenshot failed: " + ex);
        }
    }

    private void next() {
        Runnable step = steps.poll();
        if (step == null) return;
        try {
            step.run();
        } catch (Exception e) {
            System.err.println("step failed: " + e);
        }
        PauseTransition wait = new PauseTransition(Duration.millis(900));
        wait.setOnFinished(e -> next());
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
