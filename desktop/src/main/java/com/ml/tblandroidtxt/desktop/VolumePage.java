package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.AppSettings;
import com.ml.tblandroidtxt.CostEstimator;
import com.ml.tblandroidtxt.CostLimitPolicy;
import com.ml.tblandroidtxt.DesktopRawStore;
import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.RunSnapshot;
import com.ml.tblandroidtxt.VolumePlan;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * "Dịch cả tập": pick RAW chapters from the library, match each with its Glossary and Pronoun by chapter number, review and fix the
 * table, then start one run over every chapter. Starting always asks first.
 */
final class VolumePage {
    private static final String NONE_LABEL = "— Không dùng —";

    private final DesktopApp app;
    private final DesktopRuntime runtime;
    private VolumePlan plan;
    private Path outputDir;
    private CostEstimator.Estimate estimate;

    private final ListView<DesktopRawStore.Item> rawList = new ListView<>();
    private final TableView<VolumePlan.RowView> table = new TableView<>();
    private final Label summary = new Label("Thêm RAW vào thư viện, chọn các chương rồi bấm \"Ghép theo số chương\".");
    private final Label estimateLabel = new Label("");
    private final Label outputLabel = new Label("Chưa chọn thư mục output");
    private final Label progressLabel = new Label("");
    private final Label folderNote = new Label();
    private final Button startButton = new Button("Bắt đầu");
    private final Button pauseButton = new Button("Tạm dừng");
    private final Button resumeButton = new Button("Tiếp tục");
    private final Button cancelButton = new Button("Hủy");
    private final Button openOutputButton = new Button("Mở thư mục output");
    private final ComboBox<DesktopRuntime.SetCard> savedSets = new ComboBox<>();
    private final Label setNote = new Label();
    private final Node root;

    VolumePage(DesktopApp app, DesktopRuntime runtime) {
        this.app = app;
        this.runtime = runtime;
        this.root = build();
        refreshRaw();
    }

    Node node() { return root; }

    VolumePlan plan() { return plan; }

    private Node build() {
        Label rule = new Label(DesktopRuntime.FOLDER_RULE);
        rule.getStyleClass().add("muted");
        rule.setWrapText(true);
        folderNote.getStyleClass().add("warn");
        folderNote.setWrapText(true);
        Button addRaw = secondary("Thêm RAW từ thư mục…");
        Button addGlossary = secondary("Thêm Glossary từ thư mục…");
        Button addPronoun = secondary("Thêm Pronoun từ thư mục…");
        addRaw.setOnAction(e -> addFolder("RAW", runtime::importRawFolder));
        addGlossary.setOnAction(e -> addFolder("Glossary", runtime::importGlossaryFolder));
        addPronoun.setOnAction(e -> addFolder("Pronoun", runtime::importPronounFolder));
        rawList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        rawList.setPrefHeight(120);
        rawList.setMinHeight(100);
        rawList.setCellFactory(l -> new ListCell<DesktopRawStore.Item>() {
            @Override protected void updateItem(DesktopRawStore.Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.chapterLabel() + "  ·  " + item.originalName + "  ·  " + item.chars + " ký tự");
            }
        });
        Button selectAll = secondary("Chọn hết");
        selectAll.setOnAction(e -> rawList.getSelectionModel().selectAll());
        Button match = secondary("Ghép theo số chương");
        match.setOnAction(e -> buildPlan());
        savedSets.setPromptText("Chọn một tập đã lưu…");
        savedSets.setConverter(new StringConverter<DesktopRuntime.SetCard>() {
            @Override public String toString(DesktopRuntime.SetCard c) { return c == null ? "" : c.name + " (" + c.chapters + " chương)"; }
            @Override public DesktopRuntime.SetCard fromString(String s) { return null; }
        });
        Button loadSet = secondary("Nạp tập");
        loadSet.setOnAction(e -> loadSavedSet());
        setNote.setWrapText(true);
        setNote.getStyleClass().add("muted");
        VBox saved = card(new Label("Tập đã lưu (Thư viện · Tập)"), new HBox(8, savedSets, loadSet), setNote);
        VBox sources = card(new Label("Chương RAW trong thư viện"), new HBox(8, addRaw, addGlossary, addPronoun), rule, folderNote,
                rawList, new HBox(8, selectAll, match));

        buildTable();
        table.setPrefHeight(190);
        table.setMinHeight(150);
        Button oneGlossary = secondary("Dùng một Glossary cho mọi chương…");
        Button onePronoun = secondary("Dùng một Pronoun cho mọi chương…");
        oneGlossary.setOnAction(e -> pickForAll(true));
        onePronoun.setOnAction(e -> pickForAll(false));
        Button chooseOutput = secondary("Chọn thư mục output…");
        chooseOutput.setOnAction(e -> chooseOutput());
        Button reEstimate = secondary("Ước tính");
        reEstimate.setOnAction(e -> refreshEstimate(true));
        summary.setWrapText(true);
        outputLabel.getStyleClass().add("muted");
        VBox review = card(new Label("Bảng xem lại"), table, summary, new HBox(8, oneGlossary, onePronoun));

        estimateLabel.setWrapText(true);
        startButton.getStyleClass().add("primary");
        startButton.setOnAction(e -> confirmAndStart());
        for (Button b : new Button[]{pauseButton, resumeButton, cancelButton, openOutputButton}) b.getStyleClass().add("secondary");
        pauseButton.setOnAction(e -> runtime.pause());
        resumeButton.setOnAction(e -> runtime.resume());
        cancelButton.setOnAction(e -> {
            if (app.confirm("Hủy lượt dịch cả tập?", "App sẽ không tạo thêm request mới. Các chương đã dịch xong vẫn được giữ.")) runtime.cancel();
        });
        openOutputButton.setOnAction(e -> app.openFolder(outputDir));
        progressLabel.getStyleClass().add("muted");
        VBox run = card(new HBox(10, chooseOutput, outputLabel), new HBox(10, reEstimate, estimateLabel),
                new HBox(10, startButton, pauseButton, resumeButton, cancelButton, openOutputButton), progressLabel);
        VBox page = DesktopApp.page(NavModel.title(NavModel.VOLUME), saved, sources, review, run);
        updateControls();
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        return scroll;
    }

    private static Button secondary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("secondary");
        return b;
    }

    private static VBox card(Node... children) { return DesktopApp.card(children); }

    // ---- library and plan ----

    void refreshRaw() {
        List<DesktopRawStore.Item> items = runtime.raws().list();
        rawList.getItems().setAll(items);
        rawList.getSelectionModel().selectAll();
        refreshSets();
    }

    /** The saved sets are the ones of Thư viện · Tập: one store, so a set saved there shows here at once. */
    void refreshSets() {
        DesktopRuntime.SetCard keep = savedSets.getValue();
        savedSets.getItems().setAll(runtime.setCards());
        if (keep != null) for (DesktopRuntime.SetCard c : savedSets.getItems()) if (c.id.equals(keep.id)) savedSets.setValue(c);
        setNote.setText(savedSets.getItems().isEmpty() ? "Chưa có tập nào. Tạo ở Thư viện · Tập, hoặc chọn các chương RAW bên dưới." : "");
    }

    private void loadSavedSet() {
        DesktopRuntime.SetCard c = savedSets.getValue();
        if (c == null) { app.alert("Chọn một tập trước."); return; }
        VolumePlan loaded = runtime.volumePlanFromSet(c.id);
        if (loaded == null) { app.alert("Tập này không còn nữa."); refreshSets(); return; }
        plan = loaded;
        estimate = null;
        refreshTable();
        refreshEstimate(false);
        setNote.setText("Đã nạp tập \"" + c.name + "\". Lượt dịch này được ghi là của tập, nên Công việc lọc được theo tập.");
    }

    interface FolderAction { DesktopRuntime.FolderImport run(Path dir) throws Exception; }

    private void addFolder(String what, FolderAction action) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Chọn thư mục " + what + " (chỉ đọc tệp nằm trực tiếp trong đó)");
        File d = dc.showDialog(app.stage());
        if (d == null) return;
        importFolder(what, d.toPath(), action);
    }

    void importFolder(String what, Path dir, FolderAction action) {
        try {
            DesktopRuntime.FolderImport r = action.run(dir);
            folderNote.setText(r.note);
            summary.setText(what + ": thêm " + r.added + (r.skipped > 0 ? ", bỏ qua " + r.skipped + " (đã có)" : "") + ".");
            refreshRaw();
            app.refreshLibrary();
        } catch (Exception e) {
            app.alert("Không đọc được thư mục: " + e.getMessage());
        }
    }

    void importRawFolder(Path dir) { importFolder("RAW", dir, runtime::importRawFolder); }

    void importGlossaryFolder(Path dir) { importFolder("Glossary", dir, runtime::importGlossaryFolder); }

    void importPronounFolder(Path dir) { importFolder("Pronoun", dir, runtime::importPronounFolder); }

    void buildPlan() {
        List<String> ids = new ArrayList<>();
        for (DesktopRawStore.Item item : rawList.getSelectionModel().getSelectedItems()) ids.add(item.id);
        if (ids.isEmpty()) {
            app.alert("Chọn ít nhất một chương RAW.");
            return;
        }
        plan = runtime.newVolumePlan(ids);
        estimate = null;
        refreshTable();
        refreshEstimate(false);
    }

    private void buildTable() {
        TableColumn<VolumePlan.RowView, String> chapter = new TableColumn<>("Chương");
        chapter.setPrefWidth(80);
        chapter.setEditable(true);
        chapter.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().chapterNo >= 0 ? String.valueOf(cd.getValue().chapterNo) : ""));
        chapter.setCellFactory(TextFieldTableCell.forTableColumn());
        chapter.setOnEditCommit(ev -> {
            if (plan != null) plan.setChapterText(ev.getRowValue().index, ev.getNewValue());
            Platform.runLater(this::refreshTable);
        });
        TableColumn<VolumePlan.RowView, String> raw = new TableColumn<>("RAW");
        raw.setPrefWidth(250);
        raw.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().rawName));
        TableColumn<VolumePlan.RowView, VolumePlan.RowView> glossary = profileColumn("Glossary", true);
        TableColumn<VolumePlan.RowView, VolumePlan.RowView> pronoun = profileColumn("Pronoun", false);
        TableColumn<VolumePlan.RowView, VolumePlan.RowView> state = new TableColumn<>("Trạng thái");
        state.setPrefWidth(300);
        state.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        state.setCellFactory(c -> new TableCell<VolumePlan.RowView, VolumePlan.RowView>() {
            @Override protected void updateItem(VolumePlan.RowView r, boolean empty) {
                super.updateItem(r, empty);
                getStyleClass().removeAll("ok", "warn", "err");
                if (empty || r == null) { setText(null); return; }
                setText(r.reason);
                getStyleClass().add("GREEN".equals(r.status) ? "ok" : "YELLOW".equals(r.status) ? "warn" : "err");
            }
        });
        TableColumn<VolumePlan.RowView, VolumePlan.RowView> skip = new TableColumn<>("Bỏ chương");
        skip.setPrefWidth(90);
        skip.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        skip.setCellFactory(c -> new TableCell<VolumePlan.RowView, VolumePlan.RowView>() {
            private final CheckBox box = new CheckBox();
            @Override protected void updateItem(VolumePlan.RowView r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); return; }
                box.setOnAction(null);
                box.setSelected(r.excluded);
                box.setOnAction(e -> {
                    if (plan != null) plan.setExcluded(r.index, box.isSelected());
                    Platform.runLater(VolumePage.this::refreshTable);
                });
                setGraphic(box);
            }
        });
        table.getColumns().add(chapter);
        table.getColumns().add(raw);
        table.getColumns().add(glossary);
        table.getColumns().add(pronoun);
        table.getColumns().add(state);
        table.getColumns().add(skip);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Chưa có bảng ghép"));
    }

    private TableColumn<VolumePlan.RowView, VolumePlan.RowView> profileColumn(String title, boolean glossary) {
        TableColumn<VolumePlan.RowView, VolumePlan.RowView> col = new TableColumn<>(title);
        col.setPrefWidth(220);
        col.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        col.setCellFactory(c -> new TableCell<VolumePlan.RowView, VolumePlan.RowView>() {
            private final ComboBox<String[]> box = new ComboBox<>();
            {
                box.setMaxWidth(Double.MAX_VALUE);
                box.setConverter(new StringConverter<String[]>() {
                    @Override public String toString(String[] v) { return v == null ? "" : v[1]; }
                    @Override public String[] fromString(String s) { return null; }
                });
            }
            @Override protected void updateItem(VolumePlan.RowView r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null || plan == null) { setGraphic(null); return; }
                box.setOnAction(null);
                List<String[]> choices = new ArrayList<>();
                choices.add(new String[]{"", NONE_LABEL});
                choices.addAll(glossary ? plan.glossaryChoices() : plan.pronounChoices());
                box.getItems().setAll(choices);
                String id = glossary ? r.glossaryId : r.pronounId;
                String label = glossary ? r.glossaryLabel : r.pronounLabel;
                box.getSelectionModel().clearSelection();
                box.setPromptText(label.isEmpty() ? "(chưa có)" : label);
                if (!id.isEmpty() || "Không dùng".equals(label)) {
                    for (String[] choice : choices) if (choice[0].equals(id)) box.getSelectionModel().select(choice);
                }
                box.setOnAction(e -> {
                    String[] picked = box.getValue();
                    if (picked == null || plan == null) return;
                    if (glossary) plan.setGlossary(r.index, picked[0]); else plan.setPronoun(r.index, picked[0]);
                    Platform.runLater(VolumePage.this::refreshTable);
                });
                setGraphic(box);
            }
        });
        return col;
    }

    void refreshTable() {
        if (plan == null) {
            table.getItems().clear();
            return;
        }
        table.getItems().setAll(plan.views());
        summary.setText(plan.included() + " chương sẽ chạy · đủ " + plan.green() + " · thiếu " + plan.yellow() + " · lỗi " + plan.red()
                + (plan.hasRed() ? " — sửa các dòng đỏ trước khi bắt đầu" : ""));
        updateControls();
    }

    private void pickForAll(boolean glossary) {
        if (plan == null) return;
        List<String[]> choices = new ArrayList<>();
        choices.add(new String[]{"", NONE_LABEL});
        choices.addAll(glossary ? plan.glossaryChoices() : plan.pronounChoices());
        javafx.scene.control.ChoiceDialog<String[]> dialog = new javafx.scene.control.ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle(glossary ? "Một Glossary cho mọi chương" : "Một Pronoun cho mọi chương");
        dialog.setHeaderText(glossary ? "Chọn Glossary dùng cho mọi chương đang chạy" : "Chọn Pronoun dùng cho mọi chương đang chạy");
        dialog.setContentText(null);
        Optional<String[]> r = dialog.showAndWait();
        if (!r.isPresent()) return;
        if (glossary) plan.useGlossaryForAll(r.get()[0]); else plan.usePronounForAll(r.get()[0]);
        refreshTable();
        refreshEstimate(false);
    }

    private void chooseOutput() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Chọn thư mục output");
        File d = dc.showDialog(app.stage());
        if (d != null) setOutputDir(d.toPath());
    }

    void setOutputDir(Path dir) {
        outputDir = dir;
        outputLabel.setText(dir.getFileName() == null ? dir.toString() : dir.getFileName().toString());
        updateControls();
    }

    // ---- estimate and start ----

    void refreshEstimate(boolean showErrors) {
        estimate = null;
        estimateLabel.getStyleClass().remove("err");
        if (plan == null || plan.included() == 0) {
            estimateLabel.setText("");
            return;
        }
        try {
            estimate = runtime.estimateVolume(plan, app.currentSettings());
            estimateLabel.setText(plan.included() + " chương · " + app.describeEstimate(estimate));
        } catch (Exception e) {
            estimateLabel.setText("Không ước tính được: " + e.getMessage());
            if (!estimateLabel.getStyleClass().contains("err")) estimateLabel.getStyleClass().add("err");
            if (showErrors) app.alert("Không ước tính được: " + e.getMessage());
        }
        updateControls();
    }

    /** QA screenshots: the same path the Bắt đầu button takes (always asks first). */
    void startForQa() { confirmAndStart(); }

    private void confirmAndStart() {
        if (plan == null || runtime.isRunning()) return;
        if (plan.hasRed() || plan.included() == 0) {
            app.alert("Còn dòng đỏ hoặc không có chương nào để chạy. Sửa bảng rồi thử lại.");
            return;
        }
        if (outputDir == null) {
            app.alert("Chọn thư mục output trước.");
            return;
        }
        if (plan.hasMissingProfiles()) {
            ButtonType useSelected = new ButtonType("Dùng Glossary/Pronoun đang chọn");
            ButtonType skip = new ButtonType("Bỏ qua, không dùng");
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION,
                    plan.yellow() + " chương thiếu Glossary hoặc Pronoun. Với các chương đó, dùng mục đang chọn trong Thư viện hay chạy không có?",
                    useSelected, skip, ButtonType.CANCEL);
            a.setTitle("Chương thiếu Glossary/Pronoun");
            a.setHeaderText("Một số chương chưa có Glossary hoặc Pronoun");
            Optional<ButtonType> r = app.showDialog(a);
            if (!r.isPresent() || r.get() == ButtonType.CANCEL) return;
            if (r.get() == useSelected) plan.resolveMissing(runtime.selectedGlossaryId(), runtime.selectedPronounId());
            else plan.resolveMissing("", "");
            refreshTable();
        }
        refreshEstimate(true);
        AppSettings s = app.currentSettings();
        String message = plan.included() + " chương\nModel: " + s.model + "\nReasoning effort: " + s.reasoningEffort
                + "\nƯớc tính: " + (estimate == null ? "chưa có" : app.describeEstimate(estimate))
                + "\nTrần chi phí cho cả lượt: " + CostEstimator.money(s.stopOnCostLimit && s.costLimitUsd > 0 ? s.costLimitUsd : CostLimitPolicy.MIN_SAFE_CAP_USD)
                + "\nOutput: thư mục " + outputDir.getFileName() + "\n\nBắt đầu sẽ gọi provider và tốn tiền thật.";
        if (!app.confirm("Bắt đầu dịch cả tập?", message)) return;
        try {
            runtime.saveSettings(s);
        } catch (Exception e) {
            app.alert("Không lưu được cài đặt: " + e.getMessage());
            return;
        }
        if (!runtime.startVolume(plan, s, null, estimate, outputDir)) app.alert("Không bắt đầu được: đang có lượt khác chạy hoặc bảng còn dòng đỏ.");
        updateControls();
    }

    // ---- run state ----

    void applySnapshot(RunSnapshot s) {
        if (s.setTotal > 0) {
            progressLabel.setText("Chương " + Math.max(1, s.setIndex) + "/" + s.setTotal + " · xong " + s.setDone + " · lỗi " + s.setFailed
                    + " · chunk " + s.completed + "/" + s.totalChunks);
        }
        updateControls();
    }

    void updateControls() {
        boolean running = runtime != null && runtime.isRunning();
        boolean paused = running && runtime.runner().isPaused();
        startButton.setDisable(running || plan == null || plan.hasRed() || plan.included() == 0 || outputDir == null);
        pauseButton.setDisable(!running || paused);
        resumeButton.setDisable(!running || !paused);
        cancelButton.setDisable(!running);
        openOutputButton.setDisable(outputDir == null);
    }
}
