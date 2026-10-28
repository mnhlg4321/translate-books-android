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
        addPage(navBox, "Dịch", buildTranslatePage());
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

        Scene scene = new Scene(root, 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/desktop/app.css").toExternalForm());
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
        if (index == 1) refreshJobs();
        if (index == 2) refreshLibrary();
        if (index == 3) keyMasked.setText(runtime.secrets().masked());
    }

    Stage stage() { return stage; }

    DesktopRuntime runtime() { return runtime; }

    int pageCount() { return pages.size(); }

    String pageName(int i) { return navButtons.get(i).getText(); }

    private static VBox page(String title, Node... body) {
        VBox box = new VBox(12);
        box.getStyleClass().add("page");
        Label t = new Label(title);
        t.getStyleClass().add("page-title");
        box.getChildren().add(t);
        box.getChildren().addAll(body);
        return box;
    }

    private static VBox card(Node... children) {
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

    private String describeEstimate(CostEstimator.Estimate e) {
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

    private void openOutputFolder() {
        try {
            if (outputDir != null) java.awt.Desktop.getDesktop().open(outputDir.toFile());
        } catch (Exception e) {
            alert("Không mở được thư mục: " + e.getMessage());
        }
    }

    // ---- Công việc ----

    private Node buildJobsPage() {
        jobsTable.getColumns().add(column("Tệp", 300, j -> j.fileName));
        jobsTable.getColumns().add(column("Trạng thái", 130, j -> j.status));
        jobsTable.getColumns().add(column("Chunk xong", 90, j -> j.doneChunks + "/" + j.totalChunks));
        jobsTable.getColumns().add(column("Lỗi", 60, j -> String.valueOf(j.failedChunks)));
        jobsTable.getColumns().add(column("Chi phí", 90, j -> Double.isNaN(DesktopRuntime.jobCost(j)) ? "" : CostEstimator.money(DesktopRuntime.jobCost(j))));
        jobsTable.getColumns().add(column("Cập nhật", 170, j -> new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(j.updatedAt))));
        jobsTable.setPlaceholder(new Label("Chưa có job nào"));
        VBox.setVgrow(jobsTable, Priority.ALWAYS);
        jobResumeButton.getStyleClass().add("primary");
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
        return page("Công việc", jobsTable, new HBox(10, jobResumeButton, jobRetryButton));
    }

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
        List<TranslationRepositoryCore.JobSummary> list = runtime.jobs();
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
        VBox left = card(new Label("Glossary"), importGlossary, glossaryList, new HBox(8, useGlossary, deleteGlossary));
        VBox right = card(new Label("Pronoun"), importPronoun, pronounList, new HBox(8, usePronoun, deletePronoun));
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        HBox both = new HBox(14, left, right);
        VBox.setVgrow(both, Priority.ALWAYS);
        return page("Thư viện", both);
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
                modelSearch, modelList, refreshModels, row("API key", new HBox(8, keyField, saveKey, keyMasked)));
        VBox behaviour = card(new Label("Dịch"), row("Reasoning", reasoning), row("Preset", preset), row("Temperature", temperature),
                row("maxTokensPerChunk", maxTokensPerChunk), row("maxOutputTokens", maxOutputTokens), row("contextChars", contextChars),
                row("Trần chi phí mỗi lượt (USD)", costCap), row("Instruction", new HBox(8, chooseInstruction, instructionLabel)));
        HBox cols = new HBox(14, connection, behaviour);
        HBox.setHgrow(connection, Priority.ALWAYS);
        HBox.setHgrow(behaviour, Priority.ALWAYS);
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
        updateControls();
    }

    private void updateControls() {
        boolean running = runtime != null && runtime.isRunning();
        boolean paused = running && runtime.runner().isPaused();
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

    private boolean confirm(String header, String text) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Xác nhận");
        a.setHeaderText(header);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    private void alert(String text) {
        Alert a = new Alert(Alert.AlertType.WARNING, text, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
