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
    private final List<Node> pages = new ArrayList<>();
    private final List<ToggleButton> navButtons = new ArrayList<>();

    // status bar
    private final Label statusLabel = new Label("Sẵn sàng");
    private final Label metricsLabel = new Label("");
    private final ProgressBar progress = new ProgressBar(0);

    // translate page
    private Path rawFile;
    private Path outputDir;
    private final Label rawLabel = new Label("Chưa chọn tệp RAW");
    private final Label outputLabel = new Label("Chưa chọn thư mục output");
    private final Label libraryLabel = new Label();
    private final Label estimateLabel = new Label("Chọn tệp RAW để xem ước tính");
    private CostEstimator.Estimate estimate;
    private final Button startButton = new Button("Bắt đầu");
    private final Button pauseButton = new Button("Tạm dừng");
    private final Button resumeButton = new Button("Tiếp tục");
    private final Button cancelButton = new Button("Hủy");
    private final Button openOutputButton = new Button("Mở thư mục output");
    private final TextArea previewArea = new TextArea();

    // jobs, library, settings
    private final TableView<TranslationRepositoryCore.JobSummary> jobsTable = new TableView<>();
    private final Button jobResumeButton = new Button("Tiếp tục");
    private final Button jobRetryButton = new Button("Dịch lại chunk lỗi");
    private final ListView<GlossaryStoreCore.Glossary> glossaryList = new ListView<>();
    private final ListView<PronounStoreCore.Profile> pronounList = new ListView<>();
    private final TextField providerBase = new TextField();
    private final ComboBox<String> provider = new ComboBox<>();
    private final TextField modelField = new TextField();
    private final TextField modelSearch = new TextField();
    private final ListView<String> modelList = new ListView<>();
    private final PasswordField keyField = new PasswordField();
    private final TextField keyVisible = new TextField();
    private final ToggleButton keyToggle = new ToggleButton("Hiện/ẩn");
    private final Label keyMasked = new Label();
    private final ComboBox<String> reasoning = new ComboBox<>();
    private final ComboBox<String> preset = new ComboBox<>();
    private final TextField temperature = new TextField();
    private final TextField maxTokensPerChunk = new TextField();
    private final TextField maxOutputTokens = new TextField();
    private final TextField contextChars = new TextField();
    private final TextField costCap = new TextField();
    private final Label instructionLabel = new Label();
    private final Label settingsNote = new Label();
    private VolumePage volumePage;
    private HistoryPanel historyPanel;
    private final ToggleButton activeTab = new ToggleButton("Đang làm");
    private final ToggleButton historyTab = new ToggleButton("Lịch sử");
    private final ListView<com.ml.tblandroidtxt.DesktopRawStore.Item> rawLibraryList = new ListView<>();
    private final Button profileUndoButton = new Button("Hoàn tác lần nhập gần nhất");
    private final ComboBox<UiAppearanceCore.Palette> themeBox = new ComboBox<>();
    private final ComboBox<Integer> fontBox = new ComboBox<>();
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

        BorderPane root = new BorderPane();
        VBox navBox = new VBox();
        navBox.getStyleClass().add("nav");
        Label title = new Label("Translate Books");
        title.getStyleClass().add("nav-title");
        navBox.getChildren().add(title);
        volumePage = new VolumePage(this, runtime);
        historyPanel = new HistoryPanel(this, runtime);
        addPage(navBox, "Dịch", buildTranslatePage());
        addPage(navBox, "Dịch cả tập", volumePage.node());
        addPage(navBox, "Công việc", buildJobsPage());
        addPage(navBox, "Thư viện", buildLibraryPage());
        addPage(navBox, "Cài đặt", buildSettingsPage());
        root.setLeft(navBox);
        root.setCenter(content);
        HBox bar = new HBox(14, statusLabel, progress, metricsLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("statusbar");
        progress.setPrefWidth(160);
        root.setBottom(bar);

        scene = new Scene(root, 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/desktop/app.css").toExternalForm());
        applyTheme(DesktopTheme.savedThemeId(runtime.appearancePrefs()), DesktopTheme.savedFontPercent(runtime.appearancePrefs()));
        primary.setTitle("Translate Books");
        primary.setScene(scene);
        primary.setOnCloseRequest(e -> { runtime.close(); releaseInstanceLock(); });
        select(0);
        loadSettingsIntoForm();
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

    private void addPage(VBox navBox, String name, Node page) {
        ToggleButton b = new ToggleButton(name);
        b.getStyleClass().add("nav-button");
        b.setToggleGroup(nav);
        int index = pages.size();
        b.setOnAction(e -> select(index));
        pages.add(page);
        navButtons.add(b);
        navBox.getChildren().add(b);
        page.setVisible(false);
        content.getChildren().add(page);
    }

    void select(int index) {
        for (int i = 0; i < pages.size(); i++) pages.get(i).setVisible(i == index);
        navButtons.get(index).setSelected(true);
        String name = navButtons.get(index).getText();
        if (name.equals("Dịch cả tập")) volumePage.refreshRaw();
        if (name.equals("Công việc")) refreshJobs();
        if (name.equals("Thư viện")) refreshLibrary();
        if (name.equals("Cài đặt")) { keyMasked.setText(runtime.secrets().masked()); profileUndoButton.setDisable(!runtime.canUndoProfile()); }
    }

    Stage stage() { return stage; }

    DesktopRuntime runtime() { return runtime; }

    int pageCount() { return pages.size(); }

    String pageName(int i) { return navButtons.get(i).getText(); }

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

    private Node buildTranslatePage() {
        Button chooseRaw = new Button("Chọn RAW…");
        chooseRaw.getStyleClass().add("secondary");
        chooseRaw.setOnAction(e -> chooseRaw());
        Button chooseOutput = new Button("Chọn thư mục output…");
        chooseOutput.getStyleClass().add("secondary");
        chooseOutput.setOnAction(e -> chooseOutput());
        rawLabel.getStyleClass().add("muted");
        outputLabel.getStyleClass().add("muted");
        libraryLabel.getStyleClass().add("muted");
        estimateLabel.setWrapText(true);
        startButton.getStyleClass().add("primary");
        startButton.setOnAction(e -> confirmAndStart());
        for (Button b : new Button[]{pauseButton, resumeButton, cancelButton, openOutputButton}) b.getStyleClass().add("secondary");
        pauseButton.setOnAction(e -> runtime.pause());
        resumeButton.setOnAction(e -> runtime.resume());
        cancelButton.setOnAction(e -> {
            if (confirm("Hủy lượt dịch đang chạy?", "App sẽ không tạo thêm request mới. Phần đã dịch xong vẫn được giữ.")) runtime.cancel();
        });
        openOutputButton.setOnAction(e -> openOutputFolder());
        previewArea.setEditable(false);
        previewArea.setWrapText(true);
        previewArea.setPromptText("Chunk vừa dịch xong sẽ hiện ở đây");
        VBox.setVgrow(previewArea, Priority.ALWAYS);
        VBox source = card(new Label("Nguồn"), new HBox(10, chooseRaw, rawLabel), new HBox(10, chooseOutput, outputLabel), libraryLabel);
        VBox plan = card(new Label("Ước tính"), estimateLabel);
        HBox actions = new HBox(10, startButton, pauseButton, resumeButton, cancelButton, openOutputButton);
        VBox preview = card(new Label("Chunk vừa xong"), previewArea);
        VBox.setVgrow(preview, Priority.ALWAYS);
        return page("Dịch", source, plan, actions, preview);
    }

    private void chooseRaw() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn tệp RAW (.txt)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Văn bản", "*.txt"));
        File f = fc.showOpenDialog(stage);
        if (f != null) setRaw(f.toPath());
    }

    void setRaw(Path file) {
        rawFile = file;
        rawLabel.setText(file.getFileName().toString());
        refreshEstimate();
    }

    private void chooseOutput() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Chọn thư mục output");
        File d = dc.showDialog(stage);
        if (d != null) setOutputDir(d.toPath());
    }

    void setOutputDir(Path dir) {
        outputDir = dir;
        outputLabel.setText(dir.getFileName() == null ? dir.toString() : dir.getFileName().toString());
        updateControls();
    }

    void refreshEstimate() {
        estimate = null;
        if (rawFile == null) return;
        try {
            estimate = runtime.estimate(rawFile, readSettingsFromForm());
            estimateLabel.setText(describeEstimate(estimate));
            estimateLabel.getStyleClass().remove("err");
        } catch (Exception e) {
            estimateLabel.setText("Không ước tính được: " + e.getMessage());
            if (!estimateLabel.getStyleClass().contains("err")) estimateLabel.getStyleClass().add("err");
        }
        updateControls();
    }

    String describeEstimate(CostEstimator.Estimate e) {
        String cost = e.pricingAvailable && !Double.isNaN(e.costHigh)
                ? "chi phí ước tính " + CostEstimator.money(e.costLow) + " – " + CostEstimator.money(e.costHigh)
                : "chưa có giá cho model này";
        return e.chunks + " chunk · ≈ " + CostEstimator.fmt(e.totalTokensHigh) + " token · " + cost;
    }

    private double effectiveCap(AppSettings s) {
        return s.stopOnCostLimit && s.costLimitUsd > 0 ? s.costLimitUsd : CostLimitPolicy.MIN_SAFE_CAP_USD;
    }

    private void confirmAndStart() {
        if (rawFile == null || outputDir == null || runtime.isRunning()) return;
        AppSettings s = readSettingsFromForm();
        String name = s.buildOutputName(rawFile.getFileName().toString());
        String message = "Model: " + s.model + "\nReasoning effort: " + s.reasoningEffort
                + "\nƯớc tính: " + (estimate == null ? "chưa có" : describeEstimate(estimate))
                + "\nTrần chi phí lượt này: " + CostEstimator.money(effectiveCap(s))
                + "\nOutput: " + name + "\n\nBắt đầu sẽ gọi provider và tốn tiền thật.";
        if (!confirm("Bắt đầu dịch?", message)) return;
        try {
            runtime.saveSettings(s);
        } catch (Exception e) {
            alert("Không lưu được cài đặt: " + e.getMessage());
            return;
        }
        if (!runtime.startNew(rawFile, outputDir.resolve(name), s, null, estimate)) alert("Đang có một lượt dịch khác chạy.");
        updateControls();
    }

    private void openOutputFolder() { openFolder(outputDir); }

    void openFolder(Path dir) {
        try {
            if (dir != null) java.awt.Desktop.getDesktop().open(dir.toFile());
        } catch (Exception e) {
            alert("Không mở được thư mục: " + e.getMessage());
        }
    }

    AppSettings currentSettings() { return readSettingsFromForm(); }

    VolumePage volumePage() { return volumePage; }

    HistoryPanel historyPanel() { return historyPanel; }

    // ---- Công việc ----

    private Node buildJobsPage() {
        jobsTable.getColumns().add(column("Tệp", 300, j -> j.fileName));
        jobsTable.getColumns().add(column("Trạng thái", 130, j -> j.status));
        jobsTable.getColumns().add(column("Chunk xong", 90, j -> j.doneChunks + "/" + j.totalChunks));
        jobsTable.getColumns().add(column("Lỗi", 60, j -> String.valueOf(j.failedChunks)));
        jobsTable.getColumns().add(column("Chi phí", 90, j -> Double.isNaN(DesktopRuntime.jobCost(j)) ? "" : CostEstimator.money(DesktopRuntime.jobCost(j))));
        jobsTable.getColumns().add(column("Cập nhật", 170, j -> new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(j.updatedAt))));
        jobsTable.setPlaceholder(new Label("Không có job nào đang dở"));
        VBox.setVgrow(jobsTable, Priority.ALWAYS);
        jobResumeButton.getStyleClass().add("primary");
        jobResumeButton.setDisable(true);
        jobRetryButton.setDisable(true);
        jobRetryButton.getStyleClass().add("secondary");
        jobsTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateControls());
        jobResumeButton.setOnAction(e -> {
            TranslationRepositoryCore.JobSummary j = jobsTable.getSelectionModel().getSelectedItem();
            if (j != null && confirmSpend("Tiếp tục job #" + j.id + "?", j)) runtime.resumeJob(j.id);
            updateControls();
        });
        jobRetryButton.setOnAction(e -> {
            TranslationRepositoryCore.JobSummary j = jobsTable.getSelectionModel().getSelectedItem();
            if (j != null && confirmSpend("Dịch lại chunk lỗi của job #" + j.id + "?", j)) runtime.retryFailed(j.id);
            updateControls();
        });
        ToggleGroup tabs = new ToggleGroup();
        activeTab.setToggleGroup(tabs);
        historyTab.setToggleGroup(tabs);
        activeTab.getStyleClass().add("nav-button");
        historyTab.getStyleClass().add("nav-button");
        activeTab.setSelected(true);
        VBox activePane = new VBox(10, jobsTable, new HBox(10, jobResumeButton, jobRetryButton));
        VBox.setVgrow(activePane, Priority.ALWAYS);
        Node historyNode = historyPanel.node();
        historyNode.setVisible(false);
        historyNode.setManaged(false);
        StackPane stack = new StackPane(activePane, historyNode);
        VBox.setVgrow(stack, Priority.ALWAYS);
        activeTab.setOnAction(e -> { activeTab.setSelected(true); activePane.setVisible(true); activePane.setManaged(true); historyNode.setVisible(false); historyNode.setManaged(false); refreshJobs(); });
        historyTab.setOnAction(e -> { historyTab.setSelected(true); activePane.setVisible(false); activePane.setManaged(false); historyNode.setVisible(true); historyNode.setManaged(true); historyPanel.reload(); });
        activeTab.setMinWidth(Region.USE_PREF_SIZE);
        historyTab.setMinWidth(Region.USE_PREF_SIZE);
        return page("Công việc", new HBox(8, activeTab, historyTab), stack);
    }

    /** QA: show the Lịch sử tab as if it was clicked. */
    void showHistoryTab() { historyTab.fire(); }

    private boolean confirmSpend(String title, TranslationRepositoryCore.JobSummary j) {
        AppSettings s = readSettingsFromForm();
        return confirm(title, "Model: " + s.model + "\nReasoning effort: " + s.reasoningEffort + "\nTrần chi phí: " + CostEstimator.money(effectiveCap(s))
                + "\nChunk xong " + j.doneChunks + "/" + j.totalChunks + ", lỗi " + j.failedChunks + "\n\nTiếp tục sẽ gọi provider và tốn tiền thật.");
    }

    private static <T> TableColumn<T, String> column(String title, double width, java.util.function.Function<T, String> value) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(value.apply(cd.getValue())));
        return c;
    }

    void refreshJobs() {
        List<TranslationRepositoryCore.JobSummary> list = new ArrayList<>();
        for (TranslationRepositoryCore.JobSummary j : runtime.jobs()) if (com.ml.tblandroidtxt.DesktopHistory.isUnfinished(j)) list.add(j);
        jobsTable.getItems().setAll(list);
        updateControls();
    }

    // ---- Thư viện ----

    private Node buildLibraryPage() {
        Button importGlossary = new Button("Nhập Glossary (CSV)…");
        Button importPronoun = new Button("Nhập Pronoun (CSV)…");
        importGlossary.getStyleClass().add("secondary");
        importPronoun.getStyleClass().add("secondary");
        importGlossary.setOnAction(e -> importFiles(true));
        importPronoun.setOnAction(e -> importFiles(false));
        glossaryList.setCellFactory(l -> new ListCell<GlossaryStoreCore.Glossary>() {
            @Override protected void updateItem(GlossaryStoreCore.Glossary g, boolean empty) {
                super.updateItem(g, empty);
                setText(empty || g == null ? null : (g.id.equals(runtime.selectedGlossaryId()) ? "● " : "   ") + g.name + "  ·  " + g.count() + " mục");
            }
        });
        pronounList.setCellFactory(l -> new ListCell<PronounStoreCore.Profile>() {
            @Override protected void updateItem(PronounStoreCore.Profile p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : (p.id.equals(runtime.selectedPronounId()) ? "● " : "   ") + p.name + "  ·  " + p.count() + " quy tắc");
            }
        });
        VBox.setVgrow(glossaryList, Priority.ALWAYS);
        VBox.setVgrow(pronounList, Priority.ALWAYS);
        Button useGlossary = new Button("Dùng mục này");
        Button deleteGlossary = new Button("Xóa");
        Button usePronoun = new Button("Dùng mục này");
        Button deletePronoun = new Button("Xóa");
        for (Button b : new Button[]{useGlossary, deleteGlossary, usePronoun, deletePronoun}) b.getStyleClass().add("secondary");
        useGlossary.setOnAction(e -> { GlossaryStoreCore.Glossary g = glossaryList.getSelectionModel().getSelectedItem(); if (g != null) { runtime.selectGlossary(g.id); refreshLibrary(); } });
        usePronoun.setOnAction(e -> { PronounStoreCore.Profile p = pronounList.getSelectionModel().getSelectedItem(); if (p != null) { runtime.selectPronoun(p.id); refreshLibrary(); } });
        deleteGlossary.setOnAction(e -> { GlossaryStoreCore.Glossary g = glossaryList.getSelectionModel().getSelectedItem(); if (g != null && confirm("Xóa Glossary \"" + g.name + "\"?", "Không hoàn tác được.")) { runtime.deleteGlossary(g.id); refreshLibrary(); } });
        deletePronoun.setOnAction(e -> { PronounStoreCore.Profile p = pronounList.getSelectionModel().getSelectedItem(); if (p != null && confirm("Xóa Pronoun \"" + p.name + "\"?", "Không hoàn tác được.")) { runtime.deletePronoun(p.id); refreshLibrary(); } });
        Button importArchive = new Button("Nhập gói thư viện…");
        Button exportArchive = new Button("Xuất gói thư viện…");
        importArchive.getStyleClass().add("secondary");
        exportArchive.getStyleClass().add("secondary");
        importArchive.setOnAction(e -> importLibraryArchive());
        exportArchive.setOnAction(e -> exportLibraryArchive());
        Button addRawFiles = new Button("Thêm RAW (TXT)…");
        Button deleteRaw = new Button("Xóa RAW");
        addRawFiles.getStyleClass().add("secondary");
        deleteRaw.getStyleClass().add("secondary");
        addRawFiles.setOnAction(e -> addRawFiles());
        deleteRaw.setOnAction(e -> deleteSelectedRaw());
        rawLibraryList.setCellFactory(l -> new ListCell<com.ml.tblandroidtxt.DesktopRawStore.Item>() {
            @Override protected void updateItem(com.ml.tblandroidtxt.DesktopRawStore.Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.chapterLabel() + "  ·  " + item.originalName);
            }
        });
        VBox.setVgrow(rawLibraryList, Priority.ALWAYS);
        VBox middle = card(new Label("RAW"), addRawFiles, rawLibraryList, new HBox(8, deleteRaw));
        HBox.setHgrow(middle, Priority.ALWAYS);
        VBox left = card(new Label("Glossary"), importGlossary, glossaryList, new HBox(8, useGlossary, deleteGlossary));
        VBox right = card(new Label("Pronoun"), importPronoun, pronounList, new HBox(8, usePronoun, deletePronoun));
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        HBox both = new HBox(14, left, middle, right);
        VBox.setVgrow(both, Priority.ALWAYS);
        return page("Thư viện", new HBox(10, importArchive, exportArchive), both);
    }

    private void importFiles(boolean glossary) {
        FileChooser fc = new FileChooser();
        fc.setTitle(glossary ? "Nhập Glossary" : "Nhập Pronoun");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV / TXT", "*.csv", "*.txt"));
        List<File> files = fc.showOpenMultipleDialog(stage);
        if (files == null) return;
        importPaths(glossary, files.stream().map(File::toPath).collect(java.util.stream.Collectors.toList()));
    }

    void importPaths(boolean glossary, List<Path> paths) {
        try {
            int n = glossary ? runtime.importGlossaries(paths) : runtime.importPronouns(paths);
            refreshLibrary();
            settingsNote.setText("Đã nhập " + n + " tệp");
        } catch (Exception e) {
            alert("Không nhập được: " + e.getMessage());
        }
    }

    void refreshLibrary() {
        glossaryList.getItems().setAll(runtime.glossaries());
        pronounList.getItems().setAll(runtime.pronouns());
        glossaryList.refresh();
        pronounList.refresh();
        String g = "chưa chọn", p = "chưa chọn";
        for (GlossaryStoreCore.Glossary x : runtime.glossaries()) if (x.id.equals(runtime.selectedGlossaryId())) g = x.name + " (" + x.count() + " mục)";
        for (PronounStoreCore.Profile x : runtime.pronouns()) if (x.id.equals(runtime.selectedPronounId())) p = x.name + " (" + x.count() + " quy tắc)";
        libraryLabel.setText("Glossary đang dùng: " + g + "   ·   Pronoun đang dùng: " + p);
        rawLibraryList.getItems().setAll(runtime.raws().list());
    }

    private void addRawFiles() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Thêm RAW (.txt)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Văn bản", "*.txt"));
        List<File> files = fc.showOpenMultipleDialog(stage);
        if (files == null) return;
        try {
            int[] n = runtime.importRawFiles(files.stream().map(File::toPath).collect(java.util.stream.Collectors.toList()));
            refreshLibrary();
            volumePage.refreshRaw();
            settingsNote.setText("RAW: thêm " + n[0] + ", bỏ qua " + n[1] + " (trùng hoặc lỗi)");
        } catch (Exception e) {
            alert("Không thêm được RAW: " + e.getMessage());
        }
    }

    private void deleteSelectedRaw() {
        com.ml.tblandroidtxt.DesktopRawStore.Item item = rawLibraryList.getSelectionModel().getSelectedItem();
        if (item == null) return;
        List<String> usage = runtime.raws().unfinishedJobNames(item.id);
        String extra = usage.isEmpty() ? "" : "\nCòn " + usage.size() + " job chưa xong đang cần RAW này; xóa thì không tiếp tục được.";
        if (!confirm("Xóa RAW \"" + item.originalName + "\"?", "Không hoàn tác được." + extra)) return;
        runtime.raws().delete(new ArrayList<>(java.util.Arrays.asList(item.id)));
        refreshLibrary();
        volumePage.refreshRaw();
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
        try {
            DesktopRuntime.LibraryPreview p = runtime.previewLibrary(zip);
            String text = "Trong gói: " + p.glossariesInFile() + " glossary, " + p.pronounsInFile() + " pronoun, " + p.rawsInFile() + " RAW"
                    + "\nSẽ thêm: " + p.addGlossaries() + " glossary, " + p.addPronouns() + " pronoun, " + p.addRaws() + " RAW"
                    + "\nĐã có sẵn: " + p.existing() + " · trùng nội dung: " + p.duplicate()
                    + "\nXung đột (cùng mã, khác nội dung — nhập thành bản đổi tên): " + p.renamed()
                    + "\nHỏng mã toàn vẹn (không nhập): " + p.bad();
            if (p.addedCount() == 0) {
                if (ask) alert(text + "\n\nKhông có gì để thêm.");
                return;
            }
            if (ask && !confirm("Nhập gói thư viện?", text)) return;
            boolean ok = runtime.applyLibrary(p);
            refreshLibrary();
            volumePage.refreshRaw();
            if (!ok) alert("Có kho không ghi được (Glossary/Pronoun đang bị khóa vì đọc lỗi). Chưa nhập gì thêm.");
            else settingsNote.setText("Đã nhập " + p.addGlossaries() + " glossary, " + p.addPronouns() + " pronoun, " + p.addRaws() + " RAW");
        } catch (IllegalArgumentException e) {
            alert("Tệp này không phải gói thư viện hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            alert("Không nhập được gói: " + e.getMessage());
        }
    }

    private void exportLibraryArchive() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Xuất gói thư viện");
        fc.setInitialFileName(DesktopRuntime.suggestedLibraryFileName());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Gói thư viện", "*.zip"));
        File f = fc.showSaveDialog(stage);
        if (f == null) return;
        try {
            int[] n = runtime.exportLibrary(f.toPath());
            settingsNote.setText("Đã xuất " + n[0] + " glossary, " + n[1] + " pronoun, " + n[2] + " RAW (không có API key, cài đặt hay job)");
        } catch (Exception e) {
            alert("Không xuất được: " + e.getMessage());
        }
    }

    private void importProfile() {
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
            loadSettingsIntoForm();
            profileUndoButton.setDisable(!runtime.canUndoProfile());
            settingsNote.setText("Đã áp dụng " + preview.differences.size() + " mục. Bấm \"Hoàn tác lần nhập gần nhất\" để quay lại.");
            volumePage.refreshEstimate(false);
        } catch (Exception e) {
            alert("Không áp dụng được: " + e.getMessage());
        }
    }

    private static String shorten(String v) {
        String one = v == null ? "" : v.replace('\n', ' ');
        return one.length() > 80 ? one.substring(0, 77) + "…" : one;
    }

    private void exportProfile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Xuất hồ sơ cài đặt");
        fc.setInitialFileName("TranslateBooks-profile.json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Hồ sơ cài đặt", "*.json"));
        File f = fc.showSaveDialog(stage);
        if (f == null) return;
        try {
            runtime.saveSettings(readSettingsFromForm());
            Files.write(f.toPath(), runtime.exportProfileJson().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            settingsNote.setText("Đã xuất hồ sơ (không có API key, output hay Biên tập)");
        } catch (Exception e) {
            alert("Không xuất được: " + e.getMessage());
        }
    }

    // ---- theme ----

    void applyTheme(String themeId, int fontPercent) {
        scene.getRoot().setStyle(DesktopTheme.style(DesktopTheme.theme(themeId), fontPercent));
    }

    private void themeChanged() {
        UiAppearanceCore.Palette p = themeBox.getValue();
        Integer font = fontBox.getValue();
        if (p == null || font == null) return;
        DesktopTheme.save(runtime.appearancePrefs(), p.id, font);
        applyTheme(p.id, font);
    }

    /** QA: set theme and text size the way the controls do. */
    void setAppearance(String themeId, int fontPercent) {
        themeBox.setValue(DesktopTheme.theme(themeId));
        fontBox.setValue(DesktopTheme.nearestFontPercent(fontPercent));
    }

    // ---- Cài đặt ----

    private Node buildSettingsPage() {
        provider.getItems().addAll("openrouter", "openai", "deepseek", "custom");
        reasoning.getItems().addAll("auto", "off", "low", "medium", "high");
        preset.getItems().addAll("quality", "balanced", "economy", "full");
        modelSearch.setPromptText("Tìm model…");
        modelList.setPrefHeight(150);
        modelSearch.textProperty().addListener((o, a, b) -> fillModels());
        modelList.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> { if (b != null) modelField.setText(b); });
        Button refreshModels = new Button("Cập nhật danh sách model");
        refreshModels.getStyleClass().add("secondary");
        refreshModels.setOnAction(e -> {
            if (confirm("Cập nhật danh sách model?", "App sẽ tải danh sách từ openrouter.ai (không gửi API key).")) { runtime.refreshCatalog(); settingsNote.setText("Đang tải danh sách model… mở lại trang sau vài giây"); }
        });
        Button saveKey = new Button("Lưu key");
        saveKey.getStyleClass().add("secondary");
        saveKey.setOnAction(e -> {
            String shape = com.ml.tblandroidtxt.DpapiSecrets.validate(provider.getValue(), keyField.getText());
            if (shape != null) {
                alert(shape);
                return;
            }
            try {
                runtime.secrets().save(keyField.getText());
                keyField.clear();
                keyMasked.setText(runtime.secrets().masked());
                settingsNote.setText("Đã lưu key (mã hóa theo tài khoản Windows này)");
            } catch (Exception ex) {
                alert("Không lưu được key: " + ex.getMessage());
            }
        });
        keyField.setPromptText("API key (không hiển thị lại)");
        keyField.setPrefWidth(170);
        keyVisible.setPrefWidth(170);
        keyVisible.setManaged(false);
        keyVisible.setVisible(false);
        keyVisible.textProperty().bindBidirectional(keyField.textProperty());
        keyToggle.getStyleClass().add("secondary");
        keyToggle.setMinWidth(Region.USE_PREF_SIZE);
        keyToggle.setOnAction(e -> {
            boolean show = keyToggle.isSelected();
            keyVisible.setVisible(show);
            keyVisible.setManaged(show);
            keyField.setVisible(!show);
            keyField.setManaged(!show);
        });
        saveKey.setMinWidth(Region.USE_PREF_SIZE);
        keyMasked.setMinWidth(Region.USE_PREF_SIZE);
        instructionLabel.setMinWidth(Region.USE_PREF_SIZE);
        Button chooseInstruction = new Button("Chọn instruction YAML…");
        chooseInstruction.getStyleClass().add("secondary");
        chooseInstruction.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML / TXT", "*.yaml", "*.yml", "*.txt"));
            File f = fc.showOpenDialog(stage);
            if (f != null) { runtime.setInstructionPath(f.getAbsolutePath()); instructionLabel.setText(f.getName()); }
        });
        Button save = new Button("Lưu cài đặt");
        save.getStyleClass().add("primary");
        save.setOnAction(e -> {
            try {
                runtime.saveSettings(readSettingsFromForm());
                settingsNote.setText("Đã lưu cài đặt");
                refreshEstimate();
            } catch (Exception ex) {
                alert("Không lưu được: " + ex.getMessage());
            }
        });
        VBox connection = card(new Label("Kết nối provider"), row("Provider", provider), row("Base URL", providerBase), row("Model", modelField),
                modelSearch, modelList, refreshModels, row("API key", new HBox(8, keyField, keyVisible, keyToggle, saveKey, keyMasked)));
        VBox behaviour = card(new Label("Dịch"), row("Reasoning", reasoning), row("Preset", preset), row("Temperature", temperature),
                row("maxTokensPerChunk", maxTokensPerChunk), row("maxOutputTokens", maxOutputTokens), row("contextChars", contextChars),
                row("Trần chi phí mỗi lượt (USD)", costCap), row("Instruction", new HBox(8, chooseInstruction, instructionLabel)));
        Button importProfile = new Button("Nhập hồ sơ cài đặt từ điện thoại…");
        Button exportProfile = new Button("Xuất hồ sơ cài đặt…");
        importProfile.getStyleClass().add("secondary");
        exportProfile.getStyleClass().add("secondary");
        profileUndoButton.getStyleClass().add("secondary");
        importProfile.setOnAction(e -> importProfile());
        exportProfile.setOnAction(e -> exportProfile());
        profileUndoButton.setDisable(!runtime.canUndoProfile());
        profileUndoButton.setOnAction(e -> {
            try {
                runtime.undoProfile();
                loadSettingsIntoForm();
                profileUndoButton.setDisable(true);
                settingsNote.setText("Đã hoàn tác lần nhập hồ sơ");
            } catch (Exception ex) {
                alert("Không hoàn tác được: " + ex.getMessage());
            }
        });
        themeBox.getItems().setAll(DesktopTheme.themes());
        themeBox.setConverter(new javafx.util.StringConverter<UiAppearanceCore.Palette>() {
            @Override public String toString(UiAppearanceCore.Palette p) { return p == null ? "" : p.name; }
            @Override public UiAppearanceCore.Palette fromString(String s) { return null; }
        });
        for (int percent : DesktopTheme.FONT_PERCENTS) fontBox.getItems().add(percent);
        fontBox.setConverter(new javafx.util.StringConverter<Integer>() {
            @Override public String toString(Integer v) { return v == null ? "" : v + "%"; }
            @Override public Integer fromString(String s) { return null; }
        });
        themeBox.setValue(DesktopTheme.theme(DesktopTheme.savedThemeId(runtime.appearancePrefs())));
        fontBox.setValue(DesktopTheme.savedFontPercent(runtime.appearancePrefs()));
        themeBox.setOnAction(e -> themeChanged());
        fontBox.setOnAction(e -> themeChanged());
        javafx.scene.layout.FlowPane profileButtons = new javafx.scene.layout.FlowPane(8, 8, importProfile, exportProfile, profileUndoButton);
        VBox profile = card(new Label("Chuyển dữ liệu với điện thoại"), profileButtons,
                new Label("Hồ sơ chỉ chứa các mục dùng chung; API key, thư mục output và Biên tập không đi theo."));
        VBox look = card(new Label("Giao diện"), row("Màu", themeBox), row("Cỡ chữ", fontBox));
        HBox cols = new HBox(14, new VBox(14, connection, profile), new VBox(14, behaviour, look));
        for (Node column : cols.getChildren()) HBox.setHgrow(column, Priority.ALWAYS);
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(new VBox(12, cols, new HBox(10, save, settingsNote)));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return page("Cài đặt", scroll);
    }

    private static HBox row(String label, Node field) {
        Label l = new Label(label);
        l.setMinWidth(170);
        l.getStyleClass().add("muted");
        HBox h = new HBox(10, l, field);
        h.setAlignment(Pos.CENTER_LEFT);
        if (field instanceof Region) HBox.setHgrow(field, Priority.ALWAYS);
        return h;
    }

    private void fillModels() {
        String q = modelSearch.getText() == null ? "" : modelSearch.getText().toLowerCase(Locale.ROOT).trim();
        List<String> ids = new ArrayList<>();
        for (ModelCatalog.ModelInfo m : runtime.models()) if (q.isEmpty() || m.id.toLowerCase(Locale.ROOT).contains(q)) ids.add(m.id);
        modelList.getItems().setAll(ids.size() > 300 ? ids.subList(0, 300) : ids);
    }

    private void loadSettingsIntoForm() {
        AppSettings s = runtime.loadSettings();
        provider.setValue(s.provider);
        providerBase.setText(s.baseUrl);
        modelField.setText(s.model);
        reasoning.setValue(s.reasoningEffort);
        preset.setValue(s.optimizationPreset);
        temperature.setText(String.valueOf(s.temperature));
        maxTokensPerChunk.setText(String.valueOf(s.maxTokensPerChunk));
        maxOutputTokens.setText(String.valueOf(s.maxOutputTokens));
        contextChars.setText(String.valueOf(s.contextChars));
        costCap.setText(s.stopOnCostLimit && s.costLimitUsd > 0 ? String.valueOf(s.costLimitUsd) : "");
        keyMasked.setText(runtime.secrets().masked());
        String ip = runtime.instructionPath();
        instructionLabel.setText(ip.isEmpty() ? "chưa chọn" : Path.of(ip).getFileName().toString());
        fillModels();
    }

    private AppSettings readSettingsFromForm() {
        AppSettings s = runtime.loadSettings();
        if (provider.getValue() != null) s.provider = provider.getValue();
        if (!providerBase.getText().trim().isEmpty()) s.baseUrl = providerBase.getText().trim();
        s.model = modelField.getText().trim();
        if (reasoning.getValue() != null) s.reasoningEffort = reasoning.getValue();
        if (preset.getValue() != null) s.optimizationPreset = preset.getValue();
        s.temperature = parseFloat(temperature.getText(), s.temperature);
        s.maxTokensPerChunk = parseInt(maxTokensPerChunk.getText(), s.maxTokensPerChunk);
        s.maxOutputTokens = parseInt(maxOutputTokens.getText(), s.maxOutputTokens);
        s.contextChars = parseInt(contextChars.getText(), s.contextChars);
        double cap = parseDouble(costCap.getText(), 0);
        s.stopOnCostLimit = cap > 0;
        s.costLimitUsd = cap > 0 ? cap : 0;
        return s;
    }

    private static int parseInt(String v, int d) { try { return Integer.parseInt(v.trim()); } catch (Exception e) { return d; } }

    private static float parseFloat(String v, float d) { try { return Float.parseFloat(v.trim()); } catch (Exception e) { return d; } }

    private static double parseDouble(String v, double d) { try { return Double.parseDouble(v.trim().replace(',', '.')); } catch (Exception e) { return d; } }

    // ---- run events (called on the FX thread) ----

    void applySnapshot(RunSnapshot s) {
        statusLabel.setText(statusText(s));
        progress.setProgress(s.progress < 0 ? progress.getProgress() : s.progress / 100.0);
        metricsLabel.setText("chunk " + s.completed + "/" + s.totalChunks + " · " + s.totalTokens + " token · "
                + CostEstimator.money(s.sessionCost) + " · " + formatElapsed(s.elapsedMs));
        if (s.preview != null && !s.preview.isEmpty()) previewArea.setText(s.preview);
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

    private static String formatElapsed(long ms) {
        long sec = Math.max(0, ms / 1000);
        return String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
    }

    void runEnded() {
        refreshJobs();
        volumePage.updateControls();
        updateControls();
    }

    private void updateControls() {
        boolean running = runtime != null && runtime.isRunning();
        boolean paused = running && runtime.runner().isPaused();
        if (volumePage != null) volumePage.updateControls();
        startButton.setDisable(running || rawFile == null || outputDir == null);
        pauseButton.setDisable(!running || paused);
        resumeButton.setDisable(!running || !paused);
        cancelButton.setDisable(!running);
        openOutputButton.setDisable(outputDir == null);
        TranslationRepositoryCore.JobSummary j = jobsTable.getSelectionModel().getSelectedItem();
        boolean open = j != null && !running;
        jobResumeButton.setDisable(!open || j.doneChunks >= j.totalChunks);
        jobRetryButton.setDisable(!open || j.failedChunks <= 0);
    }

    // ---- dialogs ----

    private java.util.function.Consumer<javafx.scene.control.Dialog<?>> dialogProbe;

    /** QA screenshots: dialogs are shown without blocking, handed to the probe, and count as cancelled. */
    void setDialogProbe(java.util.function.Consumer<javafx.scene.control.Dialog<?>> probe) { this.dialogProbe = probe; }

    <T> Optional<T> showDialog(javafx.scene.control.Dialog<T> dialog) {
        if (dialogProbe != null) {
            dialog.show();
            dialogProbe.accept(dialog);
            return Optional.empty();
        }
        return dialog.showAndWait();
    }

    boolean confirm(String header, String text) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.setTitle("Xác nhận");
        a.setHeaderText(header);
        Optional<ButtonType> r = showDialog(a);
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    void alert(String text) {
        Alert a = new Alert(Alert.AlertType.WARNING, text, ButtonType.OK);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.setHeaderText(null);
        showDialog(a);
    }
}
