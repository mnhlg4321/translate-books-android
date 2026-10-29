package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.DesktopRawBulk;
import com.ml.tblandroidtxt.DesktopSetEditor;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * QA helper (TBL_SCREENSHOT_DIR): renders the real window to PNG files, then exits. Nothing is started and no key is shown. The data
 * folder is a throw-away TBL_DATA_DIR already filled by QaSeed (TBL_QA_SEED_DIR holds its files); dialogs are shown without blocking,
 * photographed, and closed as if cancelled, so no provider-calling button is ever confirmed. Every page and every kind of dialog is
 * photographed in a light and a dark palette.
 */
final class Screenshots {
    private final DesktopApp app;
    private final Path dir;
    private final Deque<Runnable> steps = new ArrayDeque<>();
    private String dialogName = "hop-thoai";
    private int number;

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
            app.libraryChanged();
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
                    save(d.getDialogPane().snapshot(null, null), dir.resolve(String.format("%03d-%s.png", ++number, name)));
                } catch (Exception ex) {
                    System.err.println("dialog screenshot failed: " + ex);
                }
                d.close();
            });
            w.play();
        });
        for (String mode : new String[]{"sang", "toi"}) {
            final boolean dark = mode.equals("toi");
            steps.add(() -> setLook(dark ? "default" : "gray-light", 100));
            for (String key : app.pageKeys()) {
                final String page = key;
                steps.add(() -> {
                    app.select(page);
                    if (page.equals(NavModel.VOLUME)) prepareVolume(seed);
                    if (page.equals(NavModel.JOBS)) app.jobsPage().refresh();
                    if (page.equals(NavModel.TRANSLATE) && seed != null) prepareTranslate(seed);
                });
                steps.add(() -> shot(mode, "trang-" + page));
            }
            steps.add(() -> { app.select(NavModel.GLOSSARY); var l = app.runtime().glossaries(); if (!l.isEmpty()) app.glossaryPage().openEditor(l.get(0)); });
            steps.add(() -> shot(mode, "glossary-bang-thuat-ngu"));
            steps.add(() -> app.glossaryPage().qaClose());
            steps.add(() -> { app.select(NavModel.PRONOUN); var l = app.runtime().pronouns(); if (!l.isEmpty()) app.pronounPage().openEditor(l.get(0)); });
            steps.add(() -> shot(mode, "pronoun-bang-quy-tac"));
            steps.add(() -> app.pronounPage().qaClose());
            steps.add(() -> {
                app.select(NavModel.SETS);
                List<String> ids = new ArrayList<>();
                for (var r : app.runtime().raws().list()) if (r.chapterNo <= 2) ids.add(r.id);
                app.setsPage().startNew(ids);
            });
            steps.add(() -> shot(mode, "tap-man-sua"));
            steps.add(() -> app.setsPage().qaClose());
            steps.add(() -> { app.select(NavModel.JOBS); app.jobsPage().showHistory(); app.historyPanel().selectFirst(); });
            steps.add(() -> shot(mode, "cong-viec-lich-su"));
            steps.add(() -> app.select(NavModel.TRANSLATE));
        }
        if (seed != null) {
            steps.add(() -> setLook("gray-light", 100));
            dialogs(seed);
            dialogs2();
        }
        steps.add(() -> app.select(NavModel.VOLUME));
        for (var theme : DesktopTheme.themes()) {
            for (int percent : new int[]{100, 130}) {
                steps.add(() -> setLook(theme.id, percent));
                steps.add(() -> shot("tema", "sang-" + theme.id + "-" + percent));
            }
        }
        steps.add(() -> app.select(NavModel.APPEARANCE));
        for (var theme : DesktopAppearance.darkPalettes()) {
            steps.add(() -> setLook(theme.id, 100));
            steps.add(() -> shot("tema", "toi-" + theme.id));
        }
        steps.add(() -> setLook(DesktopTheme.DEFAULT_THEME_ID, DesktopTheme.DEFAULT_FONT_PERCENT));
        steps.add(Platform::exit);
    }

    private void setLook(String paletteId, int percent) { app.setAppearance(paletteId, percent); }

    private void dialogs(Path seed) {
        steps.add(() -> app.select(NavModel.SETTINGS));
        steps.add(() -> {
            try {
                dialogName = "hop-thoai-nhap-ho-so";
                String json = new String(Files.readAllBytes(seed.resolve("QA_profile_phone.json")), java.nio.charset.StandardCharsets.UTF_8);
                app.showProfilePreview(app.runtime().previewProfile(json));
            } catch (Exception e) {
                System.err.println("profile preview failed: " + e);
            }
        });
        steps.add(() -> { });
        steps.add(() -> app.select(NavModel.RAW));
        steps.add(() -> { dialogName = "hop-thoai-nhap-goi-thu-vien"; app.importLibraryArchive(seed.resolve("QA_library_phone.zip"), true); });
        steps.add(() -> { });
        steps.add(() -> { dialogName = "hop-thoai-xoa-raw-lan-1"; app.rawPage().qaDelete(); });
        steps.add(() -> { });
        steps.add(() -> {
            dialogName = "hop-thoai-xoa-raw-lan-2";
            List<String> ids = new ArrayList<>();
            for (var r : app.runtime().raws().list()) ids.add(r.id);
            DesktopRawBulk.Plan plan = app.runtime().rawBulk().plan(ids);
            app.confirm("Xác nhận lần hai", plan.secondMessage());
        });
        steps.add(() -> { });
        steps.add(() -> { dialogName = "hop-thoai-doi-ten"; app.showDialog(new TextInputDialog("QA_ten_moi")); });
        steps.add(() -> { });
        steps.add(() -> {
            app.select(NavModel.VOLUME);
            prepareVolume(seed);
            app.volumePage().buildPlan();
            app.volumePage().plan().resolveMissing("", "");
            app.volumePage().refreshTable();
            app.volumePage().refreshEstimate(false);
            dialogName = "hop-thoai-xac-nhan-bat-dau-ca-tap";
            app.volumePage().startForQa();
        });
        steps.add(() -> { });
        steps.add(() -> {
            app.select(NavModel.TRANSLATE);
            prepareTranslate(seed);
            dialogName = "hop-thoai-xac-nhan-bat-dau";
            app.translatePage().startForQa();
        });
        steps.add(() -> { });
        steps.add(() -> { dialogName = "hop-thoai-xem-truoc-prompt"; app.translatePage().showPromptPreview(); });
        steps.add(() -> { });
        steps.add(() -> { dialogName = "hop-thoai-chon-profile-ngon-ngu"; app.translatePage().pickLanguageProfile(); });
        steps.add(() -> { });
    }

    private void dialogs2() {
        steps.add(() -> { dialogName = "hop-thoai-bao-loi"; app.alert("Không đọc được thư mục: QA_thu_muc_khong_co. Kiểm tra đường dẫn rồi thử lại."); });
        steps.add(() -> { });
        steps.add(() -> { dialogName = "hop-thoai-thay-doi-chua-luu"; app.askSave("Glossary \"QA_glossary\" có thay đổi chưa lưu."); });
        steps.add(() -> { });
        steps.add(() -> { dialogName = "hop-thoai-bat-muc-cao"; app.confirm("Bật mức Cao?", "Mức suy luận Cao có thể dùng nhiều token hơn và tăng chi phí lượt dịch."); });
        steps.add(() -> { });
        steps.add(() -> { dialogName = "hop-thoai-bo-tran-chi-phi"; app.confirm("Bỏ trần chi phí?", "Lượt dịch có thể phát sinh chi phí không giới hạn."); });
        steps.add(() -> { });
        steps.add(() -> { dialogName = "hop-thoai-kiem-tra-api"; app.confirm("Kiểm tra API?", "Sẽ gọi provider (openrouter / qa-model) với một yêu cầu rất ngắn và tốn một ít tiền thật."); });
        steps.add(() -> { });
        steps.add(() -> { dialogName = "hop-thoai-van-ban-kiem-tra"; app.showText("Kiểm tra Glossary / Pronoun", "Glossary terms: 2\nPronoun rules: 1\nNo obvious glossary/pronoun format problems detected.\n\n" + com.ml.tblandroidtxt.DesktopGlossaryEditor.PRIORITY_RULE); });
        steps.add(() -> { });
        steps.add(() -> {
            dialogName = "hop-thoai-chon-glossary-cho-dong";
            DesktopSetEditor ed = app.runtime().newSet(new ArrayList<String>());
            ChoiceDialog.pick(app, "Glossary cho Chương 1", "001_QA_chuong.txt", ed.glossaryChoices(), new java.util.HashSet<String>(), false, "Không dùng");
        });
        steps.add(() -> { });
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

    private void prepareTranslate(Path seed) {
        try {
            app.translatePage().setRaw(seed.resolve("volume").resolve("raw").resolve("001_QA_chuong.txt"));
            app.translatePage().setOutputDir(seed.resolve("out"));
            var jobs = app.runtime().jobs();
            if (!jobs.isEmpty()) app.translatePage().loadResults(jobs.get(jobs.size() - 1).id);
        } catch (Exception e) {
            System.err.println("translate prepare failed: " + e);
        }
    }

    private void shot(String group, String name) {
        try {
            Files.createDirectories(dir);
            save(app.stage().getScene().getRoot().snapshot(null, null), dir.resolve(String.format("%03d-%s-%s.png", ++number, group, name)));
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
