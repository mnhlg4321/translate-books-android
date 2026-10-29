package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.AppSettings;
import com.ml.tblandroidtxt.CostEstimator;
import com.ml.tblandroidtxt.CostLimitPolicy;
import com.ml.tblandroidtxt.DesktopPromptPreview;
import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.GlossaryStoreCore;
import com.ml.tblandroidtxt.PronounStoreCore;
import com.ml.tblandroidtxt.RunSnapshot;
import com.ml.tblandroidtxt.TranslationRepositoryCore;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Dịch: one chapter. Source and output, the translation set-up, estimate, prompt preview, start/pause/cancel, the translated text and the log. */
final class TranslatePage {
    private static final String NONE = "— Không dùng —";

    private final DesktopApp app;
    private final DesktopRuntime runtime;
    private Path rawFile;
    private Path outputDir;
    private CostEstimator.Estimate estimate;
    private long shownJob;
    private int shownDone = -1;
    private boolean loading;

    private final Label rawLabel = new Label("Chưa chọn tệp RAW");
    private final Label outputLabel = new Label("Chưa chọn nơi lưu bản dịch");
    private final Label estimateLabel = new Label("Chọn tệp RAW để xem ước tính");
    private final Label liveLabel = new Label("");
    private final Label appliedLabel = new Label("");
    private final Label note = new Label();
    private final Button startButton = new Button("Bắt đầu");
    private final Button pauseButton = new Button("Tạm dừng");
    private final Button resumeButton = new Button("Tiếp tục");
    private final Button cancelButton = new Button("Hủy");
    private final Button previewButton = new Button("Xem trước prompt…");
    private final Button openOutput = new Button("Mở output");
    private final Button openFolder = new Button("Mở thư mục");
    private final TextField sourceLang = new TextField();
    private final TextField targetLang = new TextField();
    private final TextField pattern = new TextField();
    private final CheckBox bilingual = new CheckBox("Xuất song ngữ");
    private final Label instructionLabel = new Label();
    private final ComboBox<String[]> glossaryBox = new ComboBox<>();
    private final ComboBox<String[]> pronounBox = new ComboBox<>();
    private final ComboBox<Integer> chunkBox = new ComboBox<>();
    private final CheckBox sideBySide = new CheckBox("Song song RAW | bản dịch");
    private final TextArea rawArea = new TextArea();
    private final TextArea translatedArea = new TextArea();
    private final Label resultNote = new Label("Bản dịch sẽ xuất hiện sau khi một chunk được chấp nhận.");
    private final TextArea logArea = new TextArea();
    private List<TranslationRepositoryCore.ChunkRow> shownRows = new ArrayList<>();
    private final Node root;

    TranslatePage(DesktopApp app, DesktopRuntime runtime) {
        this.app = app;
        this.runtime = runtime;
        this.root = build();
        refreshProfiles();
    }

    Node node() { return root; }

    static double effectiveCap(AppSettings s) {
        return s.stopOnCostLimit && s.costLimitUsd > 0 ? s.costLimitUsd : CostLimitPolicy.MIN_SAFE_CAP_USD;
    }

    static String describeEstimate(CostEstimator.Estimate e) {
        String cost = e.pricingAvailable && !Double.isNaN(e.costHigh)
                ? "chi phí ước tính " + CostEstimator.money(e.costLow) + " – " + CostEstimator.money(e.costHigh)
                : "chưa có giá cho model này";
        return e.chunks + " chunk · ≈ " + CostEstimator.fmt(e.totalTokensHigh) + " token · " + cost;
    }

    // ---- layout ----

    private Node build() {
        Button chooseRaw = secondary("Chọn RAW…");
        chooseRaw.setOnAction(e -> chooseRaw());
        Button folder = secondary("Thư mục (nhiều chương)");
        folder.setOnAction(e -> app.select(NavModel.VOLUME));
        Button chooseOutput = secondary("Nơi lưu bản dịch…");
        chooseOutput.setOnAction(e -> chooseOutput());
        rawLabel.getStyleClass().add("muted");
        outputLabel.getStyleClass().add("muted");
        VBox source = DesktopApp.card(new Label("Nguồn"), new HBox(10, chooseRaw, folder, rawLabel), new HBox(10, chooseOutput, outputLabel));

        // set-up (collapsible)
        Button languageProfile = secondary("Chọn profile ngôn ngữ");
        languageProfile.setOnAction(e -> pickLanguageProfile());
        sourceLang.setPromptText("Ngôn ngữ nguồn");
        targetLang.setPromptText("Ngôn ngữ đích");
        sourceLang.textProperty().addListener((o, a, b) -> saveSetup());
        targetLang.textProperty().addListener((o, a, b) -> saveSetup());
        pattern.setPromptText("Tên tệp đầu ra, ví dụ {name}_{targetLang}.txt");
        pattern.textProperty().addListener((o, a, b) -> saveSetup());
        bilingual.setOnAction(e -> saveSetup());
        Button chooseInstruction = secondary("Chọn instruction YAML…");
        chooseInstruction.setOnAction(e -> chooseInstruction());
        Button clearInstruction = secondary("Bỏ chọn");
        clearInstruction.setOnAction(e -> { runtime.setInstructionPath(""); refreshProfiles(); refreshEstimate(); });
        glossaryBox.setConverter(pairConverter());
        pronounBox.setConverter(pairConverter());
        glossaryBox.setOnAction(e -> { if (!loading && glossaryBox.getValue() != null) { runtime.selectGlossary(glossaryBox.getValue()[0]); app.libraryChanged(); refreshEstimate(); } });
        pronounBox.setOnAction(e -> { if (!loading && pronounBox.getValue() != null) { runtime.selectPronoun(pronounBox.getValue()[0]); app.libraryChanged(); refreshEstimate(); } });
        Button pricing = secondary("Cập nhật giá model");
        pricing.setOnAction(e -> updatePricing());
        appliedLabel.getStyleClass().add("muted");
        VBox setup = new VBox(8, languageProfile, row("Ngôn ngữ nguồn", sourceLang), row("Ngôn ngữ đích", targetLang),
                row("Instruction YAML", new HBox(8, chooseInstruction, clearInstruction, instructionLabel)),
                row("Glossary đang dùng", glossaryBox), row("Pronoun đang dùng", pronounBox), appliedLabel, bilingual,
                row("Tên tệp đầu ra", pattern), pricing);
        TitledPane setupPane = new TitledPane("Cấu hình bản dịch", setup);
        setupPane.setExpanded(true);

        estimateLabel.setWrapText(true);
        liveLabel.setWrapText(true);
        liveLabel.getStyleClass().add("muted");
        VBox plan = DesktopApp.card(new Label("Ước tính"), estimateLabel, liveLabel);

        startButton.getStyleClass().add("primary");
        for (Button b : new Button[]{pauseButton, resumeButton, cancelButton, previewButton}) b.getStyleClass().add("secondary");
        startButton.setOnAction(e -> confirmAndStart());
        pauseButton.setOnAction(e -> runtime.pause());
        resumeButton.setOnAction(e -> runtime.resume());
        cancelButton.setOnAction(e -> {
            if (app.confirm("Hủy lượt dịch đang chạy?", "App sẽ không tạo thêm request mới. Phần đã dịch xong vẫn được giữ.")) runtime.cancel();
        });
        previewButton.setOnAction(e -> showPromptPreview());
        note.setWrapText(true);
        FlowPane actions = new FlowPane(10, 8, startButton, pauseButton, resumeButton, cancelButton, previewButton);

        // translated text
        openOutput.getStyleClass().add("secondary");
        openFolder.getStyleClass().add("secondary");
        openOutput.setOnAction(e -> openOutputFile());
        openFolder.setOnAction(e -> app.openFolder(outputDir));
        Button prevDetail = secondary("Mở chi tiết lượt dịch trước");
        prevDetail.setOnAction(e -> app.openJobsHistory());
        chunkBox.setConverter(new StringConverter<Integer>() {
            @Override public String toString(Integer i) { return i == null ? "" : "Chunk " + (i + 1) + (i < shownRows.size() ? " · " + statusLabel(shownRows.get(i).status) : ""); }
            @Override public Integer fromString(String s) { return null; }
        });
        chunkBox.setOnAction(e -> showChunk());
        sideBySide.setOnAction(e -> layoutResults());
        rawArea.setEditable(false);
        rawArea.setWrapText(true);
        translatedArea.setEditable(false);
        translatedArea.setWrapText(true);
        rawArea.setPromptText("RAW của chunk");
        translatedArea.setPromptText("Bản dịch đã được chấp nhận của chunk");
        rawArea.scrollTopProperty().bindBidirectional(translatedArea.scrollTopProperty());
        resultNote.getStyleClass().add("muted");
        resultNote.setWrapText(true);
        resultBox = new HBox(10, translatedArea);
        HBox.setHgrow(translatedArea, Priority.ALWAYS);
        HBox.setHgrow(rawArea, Priority.ALWAYS);
        resultBox.setPrefHeight(230);
        VBox results = DesktopApp.card(new Label("Kết quả bản dịch"), new FlowPane(10, 8, chunkBox, sideBySide, openOutput, openFolder, prevDetail), resultBox, resultNote);

        // log (collapsible)
        logArea.setEditable(false);
        logArea.setPrefRowCount(8);
        Button exportLog = secondary("Xuất log…");
        Button clearLog = secondary("Xóa log");
        exportLog.setOnAction(e -> exportLog());
        clearLog.setOnAction(e -> {
            if (!app.confirm("Xóa log?", "Log hoạt động của app sẽ bị xóa khỏi máy này. Bản dịch và job không bị ảnh hưởng.")) return;
            boolean cleared = runtime.clearLog();
            say(cleared ? "Đã xóa log." : "Không xóa được log.", !cleared);
            refreshLog();
        });
        TitledPane logPane = new TitledPane("Log hoạt động", new VBox(8, logArea, new HBox(10, exportLog, clearLog)));
        logPane.setExpanded(false);
        logPane.expandedProperty().addListener((o, a, b) -> { if (b) refreshLog(); });

        VBox page = DesktopApp.page(NavModel.title(NavModel.TRANSLATE), source, setupPane, plan, actions, note, results, logPane);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        updateControls();
        return scroll;
    }

    private HBox resultBox;

    private static Button secondary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("secondary");
        return b;
    }

    private static HBox row(String label, Node field) {
        Label l = new Label(label);
        l.setMinWidth(160);
        l.getStyleClass().add("muted");
        HBox h = new HBox(10, l, field);
        h.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        if (field instanceof Region) HBox.setHgrow(field, Priority.ALWAYS);
        return h;
    }

    private static StringConverter<String[]> pairConverter() {
        return new StringConverter<String[]>() {
            @Override public String toString(String[] v) { return v == null ? "" : v[1]; }
            @Override public String[] fromString(String s) { return null; }
        };
    }

    private void say(String text, boolean error) {
        note.setText(text);
        note.getStyleClass().removeAll("err", "ok");
        note.getStyleClass().add(error ? "err" : "ok");
    }

    // ---- source, output, set-up ----

    private void chooseRaw() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn tệp RAW (.txt)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Văn bản", "*.txt"));
        File f = fc.showOpenDialog(app.stage());
        if (f != null) setRaw(f.toPath());
    }

    void setRaw(Path file) {
        rawFile = file;
        rawLabel.setText(file.getFileName().toString());
        refreshEstimate();
    }

    private void chooseOutput() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Nơi lưu bản dịch");
        File d = dc.showDialog(app.stage());
        if (d != null) setOutputDir(d.toPath());
    }

    void setOutputDir(Path dir) {
        outputDir = dir;
        outputLabel.setText(dir.getFileName() == null ? dir.toString() : dir.getFileName().toString());
        updateControls();
    }

    private void chooseInstruction() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML / TXT", "*.yaml", "*.yml", "*.txt"));
        File f = fc.showOpenDialog(app.stage());
        if (f == null) return;
        runtime.setInstructionPath(f.getAbsolutePath());
        refreshProfiles();
        refreshEstimate();
    }

    void pickLanguageProfile() {
        javafx.scene.control.ChoiceDialog<String> d = new javafx.scene.control.ChoiceDialog<>(LanguageProfiles.LABELS[0], LanguageProfiles.LABELS);
        d.setTitle("Chọn profile ngôn ngữ");
        d.setHeaderText("Chọn profile ngôn ngữ");
        d.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Optional<String> r = app.showDialog(d);
        if (!r.isPresent()) return;
        String[] pair = LanguageProfiles.languages(r.get());
        loading = true;
        if (pair != null) { sourceLang.setText(pair[0]); targetLang.setText(pair[1]); }
        loading = false;
        AppSettings s = runtime.loadSettings();
        if (pair != null) { s.sourceLanguage = pair[0]; s.targetLanguage = pair[1]; }
        s.languageProfile = r.get();
        save(s);
        say("Đã chọn profile: " + r.get(), false);
        refreshEstimate();
    }

    private void saveSetup() {
        if (loading) return;
        AppSettings s = runtime.loadSettings();
        s.sourceLanguage = sourceLang.getText().trim().isEmpty() ? s.sourceLanguage : sourceLang.getText().trim();
        s.targetLanguage = targetLang.getText().trim().isEmpty() ? s.targetLanguage : targetLang.getText().trim();
        if (!pattern.getText().trim().isEmpty()) s.outputFilenamePattern = pattern.getText().trim();
        s.bilingualOutput = bilingual.isSelected();
        save(s);
    }

    private void save(AppSettings s) {
        try { runtime.saveSettings(s); } catch (Exception e) { say("Không lưu được cài đặt: " + e.getMessage(), true); }
    }

    /** Pulls the stored set-up into the controls (page opened, settings imported, library changed). */
    void refreshProfiles() {
        loading = true;
        AppSettings s = runtime.loadSettings();
        sourceLang.setText(s.sourceLanguage);
        targetLang.setText(s.targetLanguage);
        pattern.setText(s.outputFilenamePattern);
        bilingual.setSelected(s.bilingualOutput);
        String ip = runtime.instructionPath();
        instructionLabel.setText(ip.isEmpty() ? "chưa chọn" : Path.of(ip).getFileName().toString());
        List<String[]> gl = new ArrayList<>();
        gl.add(new String[]{"", NONE});
        for (GlossaryStoreCore.Glossary g : runtime.glossaries()) gl.add(new String[]{g.id, g.name + " (" + g.count() + " mục)"});
        glossaryBox.getItems().setAll(gl);
        for (String[] p : gl) if (p[0].equals(runtime.selectedGlossaryId())) glossaryBox.setValue(p);
        List<String[]> pl = new ArrayList<>();
        pl.add(new String[]{"", NONE});
        for (PronounStoreCore.Profile p : runtime.pronouns()) pl.add(new String[]{p.id, p.name + " (" + p.count() + " quy tắc)"});
        pronounBox.getItems().setAll(pl);
        for (String[] p : pl) if (p[0].equals(runtime.selectedPronounId())) pronounBox.setValue(p);
        int g = 0, r = 0;
        GlossaryStoreCore.Glossary sg = runtime.glossary(runtime.selectedGlossaryId());
        PronounStoreCore.Profile sp = runtime.pronoun(runtime.selectedPronounId());
        if (sg != null) g = sg.count();
        if (sp != null) r = sp.count();
        appliedLabel.setText("Đã áp dụng: " + g + " mục Glossary · " + r + " quy tắc Pronoun");
        loading = false;
    }

    private void updatePricing() {
        if (!app.confirm("Cập nhật giá model?", "App sẽ tải bảng giá model từ openrouter.ai (không gửi API key, không gọi provider dịch).")) return;
        runtime.refreshCatalog();
        say("Đang tải bảng giá… ước tính sẽ tính lại sau vài giây.", false);
        PauseTransition wait = new PauseTransition(Duration.seconds(6));
        wait.setOnFinished(e -> refreshEstimate());
        wait.play();
    }

    // ---- estimate and start ----

    void refreshEstimate() {
        estimate = null;
        if (rawFile == null) return;
        try {
            estimate = runtime.estimate(rawFile, app.currentSettings());
            estimateLabel.setText(describeEstimate(estimate));
            estimateLabel.getStyleClass().remove("err");
        } catch (Exception e) {
            estimateLabel.setText("Không ước tính được: " + e.getMessage());
            if (!estimateLabel.getStyleClass().contains("err")) estimateLabel.getStyleClass().add("err");
        }
        updateControls();
    }

    private void confirmAndStart() {
        if (rawFile == null || outputDir == null || runtime.isRunning()) return;
        AppSettings s = app.currentSettings();
        String name = s.buildOutputName(rawFile.getFileName().toString());
        String message = "Model: " + s.model + "\nReasoning effort: " + s.reasoningEffort
                + "\nƯớc tính: " + (estimate == null ? "chưa có" : describeEstimate(estimate))
                + "\nTrần chi phí lượt này: " + CostEstimator.money(effectiveCap(s))
                + "\nOutput: " + name + "\n\nBắt đầu sẽ gọi provider và tốn tiền thật.";
        if (!app.confirm("Bắt đầu dịch?", message)) return;
        try {
            runtime.saveSettings(s);
        } catch (Exception e) {
            say("Không lưu được cài đặt: " + e.getMessage(), true);
            return;
        }
        runtime.confirmSpend();
        if (!runtime.startNew(rawFile, outputDir.resolve(name), s, null, estimate)) say("Đang có một lượt dịch khác chạy.", true);
        updateControls();
    }

    /** QA: shows the start confirmation without starting anything. */
    void startForQa() { confirmAndStart(); }

    // ---- prompt preview ----

    void showPromptPreview() {
        if (rawFile == null) { say("Chọn tệp RAW trước.", true); return; }
        PromptPreviewView view;
        try {
            view = new PromptPreviewView(runtime, rawFile, app::currentSettings);
        } catch (Exception e) {
            say("Không xem trước được: " + e.getMessage(), true);
            return;
        }
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Xem trước prompt");
        d.setHeaderText("Prompt sẽ gửi cho từng chunk (chưa gọi provider)");
        d.getDialogPane().setContent(view.node());
        ButtonType copyType = new ButtonType("Sao chép", ButtonBar.ButtonData.LEFT);
        ButtonType closeType = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        d.getDialogPane().getButtonTypes().addAll(copyType, closeType);
        d.getDialogPane().lookupButton(copyType).addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            javafx.scene.input.ClipboardContent c = new javafx.scene.input.ClipboardContent();
            c.putString(view.copyText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(c);
            ev.consume();
        });
        app.showDialog(d);
    }

    // ---- translated text ----

    private static String statusLabel(String status) {
        String s = status == null ? "" : status.toLowerCase(java.util.Locale.ROOT);
        if (s.equals("completed") || s.equals("done")) return "xong";
        if (s.contains("fail") || s.contains("error")) return "lỗi";
        if (s.isEmpty() || s.equals("pending")) return "chờ";
        return s;
    }

    void loadResults(long jobId) {
        shownJob = jobId;
        shownRows = jobId > 0 ? runtime.chunkRows(jobId) : new ArrayList<TranslationRepositoryCore.ChunkRow>();
        Integer keep = chunkBox.getValue();
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < shownRows.size(); i++) idx.add(i);
        loading = true;
        chunkBox.getItems().setAll(idx);
        if (keep != null && keep < idx.size()) chunkBox.setValue(keep); else if (!idx.isEmpty()) chunkBox.setValue(lastDone());
        loading = false;
        showChunk();
    }

    private int lastDone() {
        int last = 0;
        for (int i = 0; i < shownRows.size(); i++) if (shownRows.get(i).translated != null && !shownRows.get(i).translated.isEmpty()) last = i;
        return last;
    }

    private void showChunk() {
        if (loading) return;
        Integer i = chunkBox.getValue();
        if (i == null || i >= shownRows.size()) {
            rawArea.clear();
            translatedArea.clear();
            resultNote.setText("Bản dịch sẽ xuất hiện sau khi một chunk được chấp nhận.");
            layoutResults();
            return;
        }
        TranslationRepositoryCore.ChunkRow r = shownRows.get(i);
        rawArea.setText(r.source == null ? "" : r.source);
        translatedArea.setText(r.translated == null ? "" : r.translated);
        resultNote.setText("Chunk " + (i + 1) + "/" + shownRows.size() + " · " + statusLabel(r.status) + (r.error == null || r.error.isEmpty() ? "" : " · lỗi gần nhất: " + r.error));
        layoutResults();
    }

    private void layoutResults() {
        resultBox.getChildren().setAll(sideBySide.isSelected() ? new Node[]{rawArea, translatedArea} : new Node[]{translatedArea});
    }

    private void openOutputFile() {
        if (rawFile == null || outputDir == null) return;
        Path out = outputDir.resolve(runtime.loadSettings().buildOutputName(rawFile.getFileName().toString()));
        if (!Files.exists(out)) { say("Chưa có tệp output: " + out.getFileName(), true); return; }
        try { java.awt.Desktop.getDesktop().open(out.toFile()); } catch (Exception e) { say("Không mở được output: " + e.getMessage(), true); }
    }

    // ---- log ----

    private void refreshLog() {
        String text = runtime.logText();
        String[] lines = text.split("\n");
        int from = Math.max(0, lines.length - 300);
        logArea.setText(String.join("\n", java.util.Arrays.asList(lines).subList(from, lines.length)));
        logArea.positionCaret(logArea.getText().length());
    }

    private void exportLog() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Xuất log");
        fc.setInitialFileName("tbl_runtime.log");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Log", "*.log", "*.txt"));
        File f = fc.showSaveDialog(app.stage());
        if (f == null) return;
        try {
            runtime.exportLog(f.toPath());
            say("Đã xuất log (không có văn bản sách và API key).", false);
        } catch (Exception e) {
            say("Không xuất được log: " + e.getMessage(), true);
        }
    }

    // ---- run events (FX thread) ----

    void applySnapshot(RunSnapshot s) {
        if (s.activeJobId > 0 && (s.activeJobId != shownJob || s.completed != shownDone)) {
            shownDone = s.completed;
            loadResults(s.activeJobId);
        }
        liveLabel.setText("Còn lại (ước tính): " + (s.remainingMs > 0 ? DesktopApp.formatElapsed(s.remainingMs) : "—")
                + " · Token cả lượt: " + CostEstimator.fmt(s.sessionTokens));
        if (s.activeJobId > 0) appliedLabel.setText("Đã áp dụng (chunk " + Math.max(1, s.lockChunk) + "): " + s.glossaryLocks + " mục Glossary · " + s.pronounLocks + " quy tắc Pronoun");
        updateControls();
    }

    void runEnded() {
        loadResults(runtime.lastJobId());
        refreshLog();
        refreshProfiles();
        updateControls();
    }

    void updateControls() {
        boolean running = runtime.isRunning();
        boolean paused = running && runtime.runner().isPaused();
        startButton.setDisable(running || rawFile == null || outputDir == null);
        pauseButton.setDisable(!running || paused);
        resumeButton.setDisable(!running || !paused);
        cancelButton.setDisable(!running);
        previewButton.setDisable(rawFile == null);
        openOutput.setDisable(rawFile == null || outputDir == null);
        openFolder.setDisable(outputDir == null);
    }
}
