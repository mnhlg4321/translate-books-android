package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.AppSettings;
import com.ml.tblandroidtxt.CostEstimator;
import com.ml.tblandroidtxt.CostLimitPolicy;
import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.GlossaryStoreCore;
import com.ml.tblandroidtxt.ModelCatalog;
import com.ml.tblandroidtxt.PronounStoreCore;
import com.ml.tblandroidtxt.RunSnapshot;
import com.ml.tblandroidtxt.TranslationRepositoryCore;
import com.ml.tblandroidtxt.UiAppearanceCore;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** One window: navigation on the left (Dịch, Công việc, Thư viện, Cài đặt), the page on the right, run status at the bottom. */
public final class DesktopApp extends Application {
    private DesktopRuntime runtime;
    private FileLock instanceLock;
    private FileChannel lockChannel;
    private Stage stage;
    private final StackPane content = new StackPane();
    private final ToggleGroup nav = new ToggleGroup();
    private final java.util.Map<String, Node> pages = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, ToggleButton> navButtons = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, TextField> searchFields = new java.util.HashMap<>();
    private String currentKey = NavModel.TRANSLATE;
    private final java.util.Map<String, java.util.function.BooleanSupplier> leaveGuards = new java.util.HashMap<>();

    // status bar
    private final Label statusLabel = new Label("Sẵn sàng");
    private final Label metricsLabel = new Label("");
    private final ProgressBar progress = new ProgressBar(0);

    // translate page
    private TranslatePage translatePage;

    // jobs, library, settings
    private GlossaryPage glossaryPage;
    private PronounPage pronounPage;
    private SetsPage setsPage;
    private SettingsPage settingsPage;
    private final List<Label> libraryNotes = new ArrayList<>();
    private VolumePage volumePage;
    private HistoryPanel historyPanel;
    private JobsPage jobsPage;
    private RawPage rawPage;
    private AppearancePage appearancePage;
    private DesktopAppearance appearance = DesktopAppearance.defaults();
    private Scene scene;

    @Override public void start(Stage primary) throws Exception {
        this.stage = primary;
        Path dataDir = DesktopRuntime.defaultDataDir();
        Files.createDirectories(dataDir);
        if (!acquireInstanceLock(dataDir)) {
            new Alert(Alert.AlertType.INFORMATION, "Translate Books đang chạy ở một cửa sổ khác.").showAndWait();
            Platform.exit();
            return;
        }
        runtime = DesktopRuntime.open(dataDir, new FxRunListener(this));
        runtime.requireConfirmation(true);

        BorderPane root = new BorderPane();
        VBox navBox = new VBox();
        navBox.getStyleClass().add("nav");
        Label title = new Label("Translate Books");
        title.getStyleClass().add("nav-title");
        navBox.getChildren().add(title);
        volumePage = new VolumePage(this, runtime);
        historyPanel = new HistoryPanel(this, runtime);
        buildLibraryShared();
        java.util.Map<String, Node> built = new java.util.HashMap<>();
        translatePage = new TranslatePage(this, runtime);
        built.put(NavModel.TRANSLATE, translatePage.node());
        built.put(NavModel.VOLUME, volumePage.node());
        built.put(NavModel.JOBS, buildJobsPage());
        built.put(NavModel.RAW, buildRawPage());
        built.put(NavModel.GLOSSARY, buildGlossaryPage());
        built.put(NavModel.PRONOUN, buildPronounPage());
        built.put(NavModel.SETS, buildSetsPage());
        settingsPage = new SettingsPage(this, runtime);
        built.put(NavModel.SETTINGS, settingsPage.node());
        built.put(NavModel.APPEARANCE, buildAppearancePage());
        for (NavModel.Entry e : NavModel.entries()) {
            if (e.key.equals(NavModel.RAW)) addGroupHeading(navBox, "Thư viện", NavModel.LIBRARY);
            addPage(navBox, e, built.get(e.key));
        }
        root.setLeft(navBox);
        root.setCenter(content);
        HBox bar = new HBox(14, statusLabel, progress, metricsLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("statusbar");
        progress.setPrefWidth(160);
        root.setBottom(bar);

        scene = new Scene(root, 1280, 720);
        scene.getStylesheets().add(getClass().getResource("/desktop/app.css").toExternalForm());
        applyAppearance(DesktopAppearance.load(runtime.appearancePrefs()));
        primary.setTitle("Translate Books");
        primary.setScene(scene);
        restoreWindow(primary);
        primary.setOnCloseRequest(e -> { if (!confirmLeaveAll()) { e.consume(); return; } saveWindow(primary); if (rawPage != null) rawPage.settle(); runtime.close(); releaseInstanceLock(); });
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, this::shortcut);
        select(NavModel.restore(runtime.appearancePrefs()));
        refreshLibrary();
        refreshJobs();
        updateControls();
        primary.show();

        String shots = System.getenv("TBL_SCREENSHOT_DIR");
        if (shots != null && !shots.trim().isEmpty()) new Screenshots(this, Path.of(shots.trim())).run();
    }

    // ---- shell ----

    private boolean acquireInstanceLock(Path dataDir) {
        try {
            lockChannel = new RandomAccessFile(dataDir.resolve("app.lock").toFile(), "rw").getChannel();
            instanceLock = lockChannel.tryLock();
            return instanceLock != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void releaseInstanceLock() {
        try { if (instanceLock != null) instanceLock.release(); if (lockChannel != null) lockChannel.close(); } catch (Exception ignored) { }
    }

    private void addGroupHeading(VBox navBox, String text, String groupKey) {
        Button heading = new Button(text);
        heading.getStyleClass().add("nav-group");
        heading.setOnAction(e -> select(NavModel.lastLibrary(runtime.appearancePrefs())));
        navBox.getChildren().add(heading);
    }

    private void addPage(VBox navBox, NavModel.Entry entry, Node page) {
        ToggleButton b = new ToggleButton(entry.label);
        b.getStyleClass().add("nav-button");
        if (!entry.parent.isEmpty()) b.getStyleClass().add("nav-sub");
        b.setToggleGroup(nav);
        b.setOnAction(e -> select(entry.key));
        pages.put(entry.key, page);
        navButtons.put(entry.key, b);
        navBox.getChildren().add(b);
        page.setVisible(false);
        content.getChildren().add(page);
    }

    void select(String key) {
        if (!NavModel.isPage(key)) key = NavModel.TRANSLATE;
        if (!key.equals(currentKey)) {
            java.util.function.BooleanSupplier guard = leaveGuards.get(currentKey);
            if (guard != null && !guard.getAsBoolean()) {
                ToggleButton stay = navButtons.get(currentKey);
                if (stay != null) stay.setSelected(true);
                return;
            }
        }
        currentKey = key;
        fixWrapped(pages.get(key));
        for (java.util.Map.Entry<String, Node> e : pages.entrySet()) e.getValue().setVisible(e.getKey().equals(key));
        navButtons.get(key).setSelected(true);
        NavModel.remember(runtime.appearancePrefs(), key);
        if (key.equals(NavModel.VOLUME)) volumePage.refreshRaw();
        if (key.equals(NavModel.JOBS)) refreshJobs();
        if (NavModel.inLibrary(key)) refreshLibrary();
        if (key.equals(NavModel.SETTINGS)) settingsPage.reload();
    }

    /** A label that wraps keeps all of its lines (otherwise a narrow column cuts it with an ellipsis). */
    static void fixWrapped(Node n) {
        if (n instanceof Label && ((Label) n).isWrapText()) ((Label) n).setMinHeight(Region.USE_PREF_SIZE);
        if (n instanceof javafx.scene.control.ScrollPane && ((javafx.scene.control.ScrollPane) n).getContent() != null) fixWrapped(((javafx.scene.control.ScrollPane) n).getContent());
        if (n instanceof javafx.scene.control.TitledPane && ((javafx.scene.control.TitledPane) n).getContent() != null) fixWrapped(((javafx.scene.control.TitledPane) n).getContent());
        if (n instanceof javafx.scene.Parent) for (Node c : ((javafx.scene.Parent) n).getChildrenUnmodifiable()) fixWrapped(c);
    }

    String currentKey() { return currentKey; }

    /** A page with edits that are not saved yet registers a check; it returns false when the person cancels leaving. */
    void registerLeaveGuard(String key, java.util.function.BooleanSupplier guard) { leaveGuards.put(key, guard); }

    /** Runs every page's check (closing the window); true when nothing was cancelled. */
    boolean confirmLeaveAll() {
        for (java.util.function.BooleanSupplier g : leaveGuards.values()) if (!g.getAsBoolean()) return false;
        return true;
    }

    /** A page registers its search box so that Ctrl+F reaches it. */
    void registerSearch(String key, TextField field) { searchFields.put(key, field); }

    private void shortcut(javafx.scene.input.KeyEvent e) {
        if (!e.isShortcutDown() || e.isAltDown()) return;
        javafx.scene.input.KeyCode c = e.getCode();
        if (c == javafx.scene.input.KeyCode.F) {
            TextField f = searchFields.get(currentKey);
            if (f != null) { f.requestFocus(); f.selectAll(); e.consume(); }
            return;
        }
        if (c.isDigitKey()) {
            int n = c.ordinal() - javafx.scene.input.KeyCode.DIGIT0.ordinal();
            String target = NavModel.shortcutTarget(n, runtime.appearancePrefs());
            if (target != null) { select(target); e.consume(); }
        }
    }

    /** The window comes back where and how big it was (not while taking screenshots, which are always 1280 x 720). */
    private void restoreWindow(Stage s) {
        if (System.getenv("TBL_SCREENSHOT_DIR") != null) return;
        WindowBounds b = WindowBounds.load(runtime.appearancePrefs());
        if (b == null) return;
        List<double[]> screens = new ArrayList<>();
        for (javafx.stage.Screen sc : javafx.stage.Screen.getScreens()) {
            javafx.geometry.Rectangle2D r = sc.getVisualBounds();
            screens.add(new double[]{r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight()});
        }
        if (!b.visibleOn(screens)) return;
        s.setX(b.x); s.setY(b.y); s.setWidth(b.width); s.setHeight(b.height);
        s.setMaximized(b.maximized);
    }

    private void saveWindow(Stage s) {
        if (System.getenv("TBL_SCREENSHOT_DIR") != null) return;
        boolean max = s.isMaximized();
        WindowBounds old = WindowBounds.load(runtime.appearancePrefs());
        // while maximised the window's own size is the screen's: keep the last normal size
        double w = max && old != null ? old.width : s.getWidth(), h = max && old != null ? old.height : s.getHeight();
        double x = max && old != null ? old.x : s.getX(), y = max && old != null ? old.y : s.getY();
        new WindowBounds(x, y, w, h, max).save(runtime.appearancePrefs());
    }

    Stage stage() { return stage; }

    /** The status bar shows a working state while a task runs off the window thread. */
    void busy(String text) {
        statusLabel.setText(text);
        progress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
    }

    void idle() {
        statusLabel.setText("Sẵn sàng");
        progress.setProgress(0);
    }

    /** RAW, Glossary, Pronoun or Tập changed: every list that shows them is refreshed. */
    void libraryChanged() {
        refreshLibrary();
        if (translatePage != null) translatePage.refreshProfiles();
        volumePage.refreshRaw();
        volumePage.refreshSets();
    }

    /** "Tạo tập" on the RAW page: opens the set editor with these chapters. */
    void createSetFromRaws(List<String> rawIds) {
        select(NavModel.SETS);
        if (currentKey.equals(NavModel.SETS)) setsPage.startNew(rawIds);
    }

    DesktopRuntime runtime() { return runtime; }

    java.util.List<String> pageKeys() { return new ArrayList<>(pages.keySet()); }

    static VBox page(String title, Node... body) {
        VBox box = new VBox(12);
        box.getStyleClass().add("page");
        Label t = new Label(title);
        t.getStyleClass().add("page-title");
        box.getChildren().add(t);
        box.getChildren().addAll(body);
        return box;
    }

    static VBox card(Node... children) {
        VBox box = new VBox(8, children);
        box.getStyleClass().add("card");
        return box;
    }

    // ---- Dịch ----

    TranslatePage translatePage() { return translatePage; }

    SettingsPage settingsPage() { return settingsPage; }

    void openFolder(Path dir) {
        try {
            if (dir != null) java.awt.Desktop.getDesktop().open(dir.toFile());
        } catch (Exception e) {
            alert("Không mở được thư mục: " + e.getMessage());
        }
    }

    /** The stored settings: every page saves on each edit, so there is no separate form to read. */
    AppSettings currentSettings() { return runtime.loadSettings(); }

    VolumePage volumePage() { return volumePage; }

    HistoryPanel historyPanel() { return historyPanel; }

    // ---- Công việc ----

    private Node buildJobsPage() {
        jobsPage = new JobsPage(this, runtime, historyPanel);
        return jobsPage.node();
    }

    /** The jobs page on its Lịch sử tab. */
    void openJobsHistory() {
        select(NavModel.JOBS);
        jobsPage.showHistory();
    }

    /** QA: show the Lịch sử tab as if it was clicked. */
    void showHistoryTab() { jobsPage.showHistory(); }

    JobsPage jobsPage() { return jobsPage; }

    GlossaryPage glossaryPage() { return glossaryPage; }

    PronounPage pronounPage() { return pronounPage; }

    SetsPage setsPage() { return setsPage; }

    RawPage rawPage() { return rawPage; }

    /** Enables or disables the buttons that depend on whether a run is active. */
    void refreshControls() { updateControls(); }

    private static <T> TableColumn<T, String> column(String title, double width, java.util.function.Function<T, String> value) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(value.apply(cd.getValue())));
        return c;
    }

    void refreshJobs() {
        if (jobsPage != null) jobsPage.refresh();
        updateControls();
    }

    // ---- Thư viện ----

    /** Nothing is shared between the library pages any more; each page builds its own controls. */
    private void buildLibraryShared() { }

    /** The result of an archive import or export is shown next to the buttons on every library page. */
    private void libraryNote(String text, boolean error) {
        for (Label l : libraryNotes) {
            l.setText(text);
            l.getStyleClass().removeAll("err", "ok");
            l.getStyleClass().add(error ? "err" : "ok");
        }
    }

    /** Backup, import and export of the whole library sit on every library page. */
    private HBox libraryToolbar() {
        Button importArchive = new Button("Nhập gói thư viện…");
        Button exportArchive = new Button("Xuất gói thư viện…");
        importArchive.getStyleClass().add("secondary");
        exportArchive.getStyleClass().add("secondary");
        importArchive.setOnAction(e -> importLibraryArchive());
        exportArchive.setOnAction(e -> exportLibraryArchive());
        Label note = new Label();
        note.setWrapText(true);
        libraryNotes.add(note);
        HBox box = new HBox(10, importArchive, exportArchive, note);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Node buildRawPage() {
        rawPage = new RawPage(this, runtime, libraryToolbar());
        return rawPage.node();
    }

    private Node buildGlossaryPage() {
        glossaryPage = new GlossaryPage(this, runtime, libraryToolbar());
        return glossaryPage.node();
    }

    private Node buildPronounPage() {
        pronounPage = new PronounPage(this, runtime, libraryToolbar());
        return pronounPage.node();
    }

    private Node buildSetsPage() {
        setsPage = new SetsPage(this, runtime, libraryToolbar());
        return setsPage.node();
    }

    private Node buildAppearancePage() {
        appearancePage = new AppearancePage(this, runtime);
        return appearancePage.node();
    }

    void refreshLibrary() {
        if (glossaryPage != null) glossaryPage.reload();
        if (pronounPage != null) pronounPage.reload();
        if (setsPage != null) setsPage.reload();
        if (rawPage != null) rawPage.reload();
    }

    // ---- library archive and settings profile (moving data to and from the phone) ----

    private void importLibraryArchive() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Nhập gói thư viện (.zip) từ điện thoại");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Gói thư viện", "*.zip"));
        File f = fc.showOpenDialog(stage);
        if (f == null) return;
        importLibraryArchive(f.toPath(), true);
    }

    void importLibraryArchive(Path zip, boolean ask) {
        UiTasks.run(this, "Đang đọc gói thư viện…", () -> runtime.previewLibrary(zip), p -> afterLibraryPreview(p, ask),
                e -> {
                    if (e instanceof IllegalArgumentException) libraryNote("Tệp này không phải gói thư viện hợp lệ: " + e.getMessage(), true);
                    else libraryNote("Không nhập được gói: " + e.getMessage(), true);
                });
    }

    private void afterLibraryPreview(DesktopRuntime.LibraryPreview p, boolean ask) {
        String text = "Trong gói: " + p.glossariesInFile() + " glossary, " + p.pronounsInFile() + " pronoun, " + p.rawsInFile() + " RAW"
                + "\nSẽ thêm: " + p.addGlossaries() + " glossary, " + p.addPronouns() + " pronoun, " + p.addRaws() + " RAW"
                + "\nĐã có sẵn: " + p.existing() + " · trùng nội dung: " + p.duplicate()
                + "\nXung đột (cùng mã, khác nội dung — nhập thành bản đổi tên): " + p.renamed()
                + "\nHỏng mã toàn vẹn (không nhập): " + p.bad();
        if (p.addedCount() == 0) {
            if (ask) alert(text + "\n\nKhông có gì để thêm.");
            libraryNote("Không có gì để thêm từ gói này.", false);
            return;
        }
        if (ask && !confirm("Nhập gói thư viện?", text)) return;
        UiTasks.run(this, "Đang nhập gói thư viện…", () -> runtime.applyLibrary(p), ok -> {
            libraryChanged();
            if (!ok) libraryNote("Có kho không ghi được (Glossary/Pronoun đang bị khóa vì đọc lỗi). Chưa nhập gì thêm.", true);
            else libraryNote("Đã nhập " + p.addGlossaries() + " glossary, " + p.addPronouns() + " pronoun, " + p.addRaws() + " RAW", false);
        }, e -> libraryNote("Không nhập được gói: " + e.getMessage(), true));
    }

    private void exportLibraryArchive() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Xuất gói thư viện");
        fc.setInitialFileName(DesktopRuntime.suggestedLibraryFileName());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Gói thư viện", "*.zip"));
        File f = fc.showSaveDialog(stage);
        if (f == null) return;
        try {
            UiTasks.run(this, "Đang xuất gói thư viện…", () -> runtime.exportLibrary(f.toPath()),
                    n -> libraryNote("Đã xuất " + n[0] + " glossary, " + n[1] + " pronoun, " + n[2] + " RAW (không có API key, cài đặt, job hay Tập)", false),
                    e -> libraryNote("Không xuất được: " + e.getMessage(), true));
        } catch (Exception e) {
            libraryNote("Không xuất được: " + e.getMessage(), true);
        }
    }

    void importProfile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Nhập hồ sơ cài đặt từ điện thoại (.json)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Hồ sơ cài đặt", "*.json"));
        File f = fc.showOpenDialog(stage);
        if (f == null) return;
        try {
            String json = new String(Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            showProfilePreview(runtime.previewProfile(json));
        } catch (Exception e) {
            alert("Không đọc được hồ sơ: " + e.getMessage());
        }
    }

    void showProfilePreview(DesktopRuntime.ProfilePreview preview) {
        if (preview.differences.isEmpty()) {
            alert("Hồ sơ này giống hệt cài đặt hiện tại. Không có gì để áp dụng.");
            return;
        }
        TableView<String[]> diff = new TableView<>();
        diff.getColumns().add(column("Mục", 190, r -> r[0]));
        diff.getColumns().add(column("PC hiện tại", 230, r -> shorten(r[1])));
        diff.getColumns().add(column("Hồ sơ", 230, r -> shorten(r[2])));
        diff.getItems().setAll(preview.differences);
        diff.setPrefSize(680, 320);
        javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Nhập hồ sơ cài đặt");
        dialog.setHeaderText(preview.differences.size() + " mục khác nhau. API key, thư mục output và Biên tập trên PC được giữ nguyên.");
        dialog.getDialogPane().setContent(diff);
        ButtonType apply = new ButtonType("Áp dụng", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(apply, ButtonType.CANCEL);
        Optional<ButtonType> r = showDialog(dialog);
        if (!r.isPresent() || r.get() != apply) return;
        try {
            runtime.applyProfile(preview);
            settingsPage.profileChanged();
            settingsPage.message("Đã áp dụng " + preview.differences.size() + " mục. Bấm \"Hoàn tác lần nhập gần nhất\" để quay lại.");
            volumePage.refreshEstimate(false);
            translatePage.refreshProfiles();
        } catch (Exception e) {
            alert("Không áp dụng được: " + e.getMessage());
        }
    }

    private static String shorten(String v) {
        String one = v == null ? "" : v.replace('\n', ' ');
        return one.length() > 80 ? one.substring(0, 77) + "…" : one;
    }

    void exportProfile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Xuất hồ sơ cài đặt");
        fc.setInitialFileName("TranslateBooks-profile.json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Hồ sơ cài đặt", "*.json"));
        File f = fc.showSaveDialog(stage);
        if (f == null) return;
        try {
            Files.write(f.toPath(), runtime.exportProfileJson().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            settingsPage.message("Đã xuất hồ sơ (không có API key, output hay Biên tập)");
        } catch (Exception e) {
            alert("Không xuất được: " + e.getMessage());
        }
    }

    // ---- theme ----

    /** Paints the window with the chosen palette, text size, density and corners; dialogs opened later copy the same look. */
    void applyAppearance(DesktopAppearance a) {
        appearance = a;
        javafx.scene.Parent root = scene.getRoot();
        root.setStyle(DesktopTheme.style(a.resolve(a.mode.equals(DesktopAppearance.MODE_SYSTEM) && DesktopAppearance.systemIsNight()), a.fontPercent));
        root.getStyleClass().removeAll("density-compact", "density-comfortable", "corners-square", "corners-medium", "corners-round");
        root.getStyleClass().addAll(a.rootClasses());
    }

    DesktopAppearance appearance() { return appearance; }

    AppearancePage appearancePage() { return appearancePage; }

    /** QA: set a fixed palette and text size the way the controls do. */
    void setAppearance(String themeId, int fontPercent) {
        DesktopAppearance a = DesktopAppearance.load(runtime.appearancePrefs());
        a.mode = DesktopAppearance.MODE_FIXED;
        a.palette = themeId;
        a.fontPercent = DesktopAppearance.nearestFont(fontPercent);
        appearancePage.set(a);
    }

    // ---- Cài đặt ----

    private static HBox row(String label, Node field) {
        Label l = new Label(label);
        l.setMinWidth(170);
        l.getStyleClass().add("muted");
        HBox h = new HBox(10, l, field);
        h.setAlignment(Pos.CENTER_LEFT);
        if (field instanceof Region) HBox.setHgrow(field, Priority.ALWAYS);
        return h;
    }

    // ---- run events (called on the FX thread) ----

    void applySnapshot(RunSnapshot s) {
        statusLabel.setText(statusText(s));
        progress.setProgress(s.progress < 0 ? progress.getProgress() : s.progress / 100.0);
        metricsLabel.setText("chunk " + s.completed + "/" + s.totalChunks + " · " + s.totalTokens + " token · "
                + CostEstimator.money(s.sessionCost) + " · " + formatElapsed(s.elapsedMs));
        translatePage.applySnapshot(s);
        volumePage.applySnapshot(s);
        updateControls();
    }

    private static String statusText(RunSnapshot s) {
        switch (s.status) {
            case "paused": return "Đã tạm dừng";
            case "done": return "Hoàn tất";
            case "error": return "Lỗi";
            case "cancelled": return "Đã hủy";
            case "stopping": return "Đang dừng…";
            default: return s.phase == null || s.phase.isEmpty() ? "Đang chạy" : s.phase;
        }
    }

    static String formatElapsed(long ms) {
        long sec = Math.max(0, ms / 1000);
        return String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
    }

    void runEnded() {
        refreshJobs();
        translatePage.runEnded();
        volumePage.updateControls();
        updateControls();
    }

    private void updateControls() {
        boolean running = runtime != null && runtime.isRunning();
        boolean paused = running && runtime.runner().isPaused();
        if (volumePage != null) volumePage.updateControls();
        if (translatePage != null) translatePage.updateControls();
        if (jobsPage != null) jobsPage.updateButtons();
    }

    // ---- dialogs ----

    private java.util.function.Consumer<javafx.scene.control.Dialog<?>> dialogProbe;

    /** QA screenshots: dialogs are shown without blocking, handed to the probe, and count as cancelled. */
    void setDialogProbe(java.util.function.Consumer<javafx.scene.control.Dialog<?>> probe) { this.dialogProbe = probe; }

    /** A dialog is its own window, so it gets the stylesheet, the palette variables, the text size and the classes of the main window. */
    private void styleDialog(javafx.scene.control.Dialog<?> dialog) {
        javafx.scene.control.DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().setAll(scene.getStylesheets());
        pane.setStyle(scene.getRoot().getStyle());
        pane.getStyleClass().addAll(appearance.rootClasses());
        pane.getStyleClass().add("tbl-dialog");
        if (dialog.getOwner() == null && stage != null) dialog.initOwner(stage);
    }

    <T> Optional<T> showDialog(javafx.scene.control.Dialog<T> dialog) {
        styleDialog(dialog);
        if (dialogProbe != null) {
            dialog.show();
            dialogProbe.accept(dialog);
            return Optional.empty();
        }
        return dialog.showAndWait();
    }

    boolean confirm(String header, String text) {
        ButtonType ok = new ButtonType("Đồng ý", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, text, ok, cancel);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.setTitle("Xác nhận");
        a.setHeaderText(header);
        Optional<ButtonType> r = showDialog(a);
        return r.isPresent() && r.get() == ok;
    }

    /** Unsaved changes: 1 = save, 0 = leave without saving, -1 = stay. */
    int askSave(String text) {
        ButtonType save = new ButtonType("Lưu", javafx.scene.control.ButtonBar.ButtonData.YES);
        ButtonType discard = new ButtonType("Không lưu", javafx.scene.control.ButtonBar.ButtonData.NO);
        ButtonType stay = new ButtonType("Hủy", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, text, save, discard, stay);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.setTitle("Thay đổi chưa lưu");
        a.setHeaderText("Lưu thay đổi?");
        Optional<ButtonType> r = showDialog(a);
        if (!r.isPresent() || r.get() == stay) return -1;
        return r.get() == save ? 1 : 0;
    }

    /** A read-only text window with a copy button (health check, preview). */
    void showText(String title, String text) {
        javafx.scene.control.Dialog<ButtonType> d = new javafx.scene.control.Dialog<>();
        d.setTitle(title);
        d.setHeaderText(title);
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefSize(640, 360);
        d.getDialogPane().setContent(area);
        ButtonType copy = new ButtonType("Sao chép", javafx.scene.control.ButtonBar.ButtonData.LEFT);
        d.getDialogPane().getButtonTypes().addAll(copy, ButtonType.CLOSE);
        d.getDialogPane().lookupButton(copy).addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            javafx.scene.input.ClipboardContent c = new javafx.scene.input.ClipboardContent();
            c.putString(text);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(c);
            ev.consume();
        });
        showDialog(d);
    }

    void alert(String text) {
        Alert a = new Alert(Alert.AlertType.WARNING, text, new ButtonType("Đóng", javafx.scene.control.ButtonBar.ButtonData.OK_DONE));
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.setHeaderText(null);
        showDialog(a);
    }
}
