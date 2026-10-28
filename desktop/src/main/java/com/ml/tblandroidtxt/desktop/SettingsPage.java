package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.AppSettings;
import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.DesktopSettingsModel;
import com.ml.tblandroidtxt.DpapiSecrets;
import com.ml.tblandroidtxt.ModelCatalog;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Cài đặt: the groups of the phone's Settings (Kết nối Provider, Mức chất lượng, Chunk, Chi phí, Gọi lại và thời gian chờ, Đầu ra, Instruction,
 * Log và chẩn đoán, Chuyển dữ liệu). Every edit is saved at once ("Đã lưu tự động khi sửa", as on the phone), so there is no unsaved state.
 * Buttons that send a request to a provider ask first and say so.
 */
final class SettingsPage {
    private final DesktopApp app;
    private final DesktopRuntime runtime;
    private final Map<String, Runnable> refreshers = new HashMap<>();
    private final Map<String, Label> notes = new HashMap<>();
    private final Label globalNote = new Label();
    private final Label savedMark = new Label("Đã lưu tự động khi sửa");
    private final TextField modelSearch = new TextField();
    private final ListView<String> modelList = new ListView<>();
    private final PasswordField keyField = new PasswordField();
    private final TextField keyVisible = new TextField();
    private final ToggleButton keyToggle = new ToggleButton("Hiện/ẩn");
    private final Label keyMasked = new Label();
    private final Label instructionLabel = new Label();
    private final TextField sourceLang = new TextField();
    private final TextField targetLang = new TextField();
    private final Label recommendationBox = new Label();
    private final Button applyRecommendation = new Button("Áp dụng");
    private final Button undoRecommendation = new Button("Hoàn tác");
    private final Label diagnostics = new Label();
    private final Button profileUndo = new Button("Hoàn tác lần nhập gần nhất");
    private final CheckBox unlimited = new CheckBox("Không giới hạn");
    private boolean loading;
    private final Node root;

    SettingsPage(DesktopApp app, DesktopRuntime runtime) {
        this.app = app;
        this.runtime = runtime;
        this.root = build();
        reload();
    }

    Node node() { return root; }

    // ---- layout ----

    private Node build() {
        VBox connection = card(DesktopSettingsModel.CONNECTION);
        TextField providerBase = null;
        addField(connection, "provider", true);
        addField(connection, "baseUrl", false);
        addField(connection, "model", false);
        modelSearch.setPromptText("Tìm model…");
        modelList.setPrefHeight(120);
        modelSearch.textProperty().addListener((o, a, b) -> fillModels());
        modelList.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null && !loading) { commit("model", b); reload(); }
        });
        keyField.setPromptText("API key (không hiển thị lại)");
        keyField.setPrefWidth(190);
        keyVisible.setPrefWidth(190);
        keyVisible.setManaged(false);
        keyVisible.setVisible(false);
        keyVisible.textProperty().bindBidirectional(keyField.textProperty());
        keyToggle.getStyleClass().add("secondary");
        keyToggle.setMinWidth(Region.USE_PREF_SIZE);
        keyToggle.setOnAction(e -> {
            boolean show = keyToggle.isSelected();
            keyVisible.setVisible(show); keyVisible.setManaged(show);
            keyField.setVisible(!show); keyField.setManaged(!show);
        });
        Button saveKey = secondary("Lưu key");
        saveKey.setMinWidth(Region.USE_PREF_SIZE);
        saveKey.setOnAction(e -> saveKey());
        keyMasked.setMinWidth(Region.USE_PREF_SIZE);
        Button saveConnection = secondary("Lưu kết nối");
        saveConnection.setOnAction(e -> { say(DesktopSettingsModel.CONNECTION, "Đã lưu kết nối (các ô được lưu ngay khi sửa).", false); });
        Button testApi = secondary("Kiểm tra API");
        testApi.setOnAction(e -> testApi());
        Button refreshCatalog = secondary("Làm mới catalog");
        refreshCatalog.setOnAction(e -> refreshCatalog());
        connection.getChildren().addAll(modelSearch, modelList, row("API key", new HBox(8, keyField, keyVisible, keyToggle, saveKey, keyMasked)),
                new FlowPane(8, 8, saveConnection, testApi, refreshCatalog), note(DesktopSettingsModel.CONNECTION));

        VBox quality = card(DesktopSettingsModel.QUALITY);
        addField(quality, "optimizationPreset", true);
        applyRecommendation.getStyleClass().add("primary");
        undoRecommendation.getStyleClass().add("secondary");
        recommendationBox.setWrapText(true);
        applyRecommendation.setOnAction(e -> applyRecommendation());
        undoRecommendation.setOnAction(e -> undoRecommendation());
        quality.getChildren().addAll(new Label("Cài đặt khuyến nghị"), recommendationBox, new HBox(8, applyRecommendation, undoRecommendation));
        addField(quality, "reasoningEffort", true);
        addField(quality, "temperature", false);
        quality.getChildren().add(note(DesktopSettingsModel.QUALITY));

        VBox chunk = card(DesktopSettingsModel.CHUNK);
        for (DesktopSettingsModel.Field f : DesktopSettingsModel.fieldsOf(DesktopSettingsModel.CHUNK)) addField(chunk, f.key, false);
        chunk.getChildren().add(note(DesktopSettingsModel.CHUNK));

        VBox cost = card(DesktopSettingsModel.COST);
        addField(cost, "costLimitUsd", false);
        unlimited.setOnAction(e -> unlimitedChanged());
        for (String k : new String[]{"stopOnCostLimit"}) addField(cost, k, false);
        cost.getChildren().add(unlimited);
        addField(cost, "costWarningUsd", false);
        addField(cost, "stopWhenPricingUnknown", false);
        cost.getChildren().add(note(DesktopSettingsModel.COST));

        VBox retry = card(DesktopSettingsModel.RETRY);
        for (DesktopSettingsModel.Field f : DesktopSettingsModel.fieldsOf(DesktopSettingsModel.RETRY)) addField(retry, f.key, false);
        retry.getChildren().add(note(DesktopSettingsModel.RETRY));

        VBox output = card(DesktopSettingsModel.OUTPUT);
        for (DesktopSettingsModel.Field f : DesktopSettingsModel.fieldsOf(DesktopSettingsModel.OUTPUT)) addField(output, f.key, false);
        output.getChildren().add(label("Tên tệp đầu ra được đặt ở trang Dịch."));
        output.getChildren().add(note(DesktopSettingsModel.OUTPUT));

        Button chooseInstruction = secondary("Chọn instruction YAML…");
        chooseInstruction.setOnAction(e -> chooseInstruction());
        Button clearInstruction = secondary("Bỏ chọn");
        clearInstruction.setOnAction(e -> { runtime.setInstructionPath(""); reload(); app.translatePage().refreshProfiles(); });
        sourceLang.textProperty().addListener((o, a, b) -> saveLanguages());
        targetLang.textProperty().addListener((o, a, b) -> saveLanguages());
        VBox instruction = card("Instruction và profile", row("Instruction YAML", new HBox(8, chooseInstruction, clearInstruction, instructionLabel)),
                row("Ngôn ngữ nguồn", sourceLang), row("Ngôn ngữ đích", targetLang));

        Button clearLog = secondary("Xóa log runtime");
        clearLog.setOnAction(e -> {
            if (!app.confirm("Xóa log runtime?", "Log hoạt động của app sẽ bị xóa khỏi máy này. Bản dịch và job không bị ảnh hưởng.")) return;
            globalSay(runtime.clearLog() ? "Đã xóa log runtime." : "Không xóa được log.", false);
        });
        Button exportLog = secondary("Xuất toàn bộ log…");
        exportLog.setOnAction(e -> exportLog());
        Button diag = secondary("Chẩn đoán model");
        diag.setOnAction(e -> diagnostics.setText(runtime.modelDiagnostics() + "\nChẩn đoán chỉ đọc catalog trên máy; không gửi request."));
        diagnostics.setWrapText(true);
        VBox logBox = card("Log và chẩn đoán", new FlowPane(8, 8, clearLog, exportLog, diag), diagnostics);

        Button importProfile = secondary("Nhập hồ sơ cài đặt từ điện thoại…");
        Button exportProfile = secondary("Xuất hồ sơ cài đặt…");
        profileUndo.getStyleClass().add("secondary");
        importProfile.setOnAction(e -> app.importProfile());
        exportProfile.setOnAction(e -> app.exportProfile());
        profileUndo.setOnAction(e -> {
            try {
                runtime.undoProfile();
                reload();
                globalSay("Đã hoàn tác lần nhập hồ sơ.", false);
            } catch (Exception ex) {
                globalSay("Không hoàn tác được: " + ex.getMessage(), true);
            }
        });
        VBox transfer = card("Chuyển dữ liệu với điện thoại", new FlowPane(8, 8, importProfile, exportProfile, profileUndo),
                label("Hồ sơ chỉ chứa các mục dùng chung; API key, thư mục output và Biên tập không đi theo."));

        savedMark.getStyleClass().add("ok");
        globalNote.setWrapText(true);
        VBox left = new VBox(14, connection, quality, cost, instruction);
        VBox right = new VBox(14, chunk, retry, output, logBox, transfer);
        HBox cols = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setMinWidth(380);
        right.setMinWidth(380);
        ScrollPane scroll = new ScrollPane(new VBox(12, new HBox(12, savedMark, globalNote), cols));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return DesktopApp.page(NavModel.title(NavModel.SETTINGS), scroll);
    }

    private static Button secondary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("secondary");
        return b;
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.getStyleClass().add("muted");
        return l;
    }

    private static VBox card(String title, Node... more) {
        Label t = new Label(title);
        t.setStyle("-fx-font-weight: bold;");
        VBox box = DesktopApp.card(t);
        box.getChildren().addAll(more);
        box.setUserData(title);
        return box;
    }

    private Label note(String group) {
        Label l = new Label();
        l.setWrapText(true);
        notes.put(group, l);
        return l;
    }

    private static HBox row(String text, Node field) {
        Label l = new Label(text);
        l.setMinWidth(190);
        l.getStyleClass().add("muted");
        l.setWrapText(true);
        HBox h = new HBox(10, l, field);
        h.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        if (field instanceof Region) HBox.setHgrow(field, Priority.ALWAYS);
        return h;
    }

    private void say(String group, String text, boolean error) {
        Label l = notes.get(group);
        if (l == null) { globalSay(text, error); return; }
        l.setText(text);
        l.getStyleClass().removeAll("err", "ok");
        l.getStyleClass().add(error ? "err" : "ok");
    }

    private void globalSay(String text, boolean error) {
        globalNote.setText(text);
        globalNote.getStyleClass().removeAll("err", "ok");
        globalNote.getStyleClass().add(error ? "err" : "ok");
    }

    // ---- generic fields ----

    private void addField(VBox card, String key, boolean unused) {
        DesktopSettingsModel.Field f = DesktopSettingsModel.field(key);
        switch (f.kind) {
            case BOOL: {
                CheckBox box = new CheckBox(f.label);
                box.setOnAction(e -> { if (!loading) commit(key, box.isSelected() ? "true" : "false"); });
                refreshers.put(key, () -> box.setSelected(Boolean.parseBoolean(f.get(runtime.loadSettings()))));
                card.getChildren().add(box);
                break;
            }
            case CHOICE: {
                ComboBox<String[]> combo = new ComboBox<>();
                combo.setConverter(new StringConverter<String[]>() {
                    @Override public String toString(String[] v) { return v == null ? "" : v[1]; }
                    @Override public String[] fromString(String s) { return null; }
                });
                combo.getItems().setAll(f.choices);
                combo.setOnAction(e -> {
                    if (loading || combo.getValue() == null) return;
                    String value = combo.getValue()[0];
                    if (f.needsConfirm(value) && !app.confirm(f.confirmTitle, f.confirmText)) { reload(); return; }
                    commit(key, value);
                });
                refreshers.put(key, () -> {
                    String cur = f.get(runtime.loadSettings());
                    for (String[] c : f.choices) if (c[0].equals(cur)) combo.setValue(c);
                });
                card.getChildren().add(row(f.label, combo));
                break;
            }
            default: {
                TextField field = new TextField();
                Runnable apply = () -> { if (!loading) commit(key, field.getText()); };
                field.setOnAction(e -> apply.run());
                field.focusedProperty().addListener((o, a, focused) -> { if (!focused) apply.run(); });
                refreshers.put(key, () -> field.setText(f.get(runtime.loadSettings())));
                card.getChildren().add(row(f.label, field));
            }
        }
    }

    private void commit(String key, String value) {
        DesktopSettingsModel.Field f = DesktopSettingsModel.field(key);
        try {
            String current = f.get(runtime.loadSettings());
            if (current.equals(value == null ? "" : value.trim())) return;
            String problem = runtime.applySetting(key, value);
            if (problem != null) {
                say(f.group, f.label + ": " + problem, true);
                loading = true;
                refreshers.get(key).run();
                loading = false;
                return;
            }
            say(f.group, "Đã lưu: " + f.label, false);
            if (key.equals("provider")) { /* the base URL is the person's to change; the provider list only names the service */ }
            afterChange();
        } catch (Exception e) {
            say(f.group, "Không lưu được: " + e.getMessage(), true);
        }
    }

    private void afterChange() {
        app.translatePage().refreshEstimate();
        app.volumePage().refreshEstimate(false);
        refreshRecommendations();
    }

    // ---- special controls ----

    void reload() {
        loading = true;
        for (Runnable r : refreshers.values()) r.run();
        AppSettings s = runtime.loadSettings();
        sourceLang.setText(s.sourceLanguage);
        targetLang.setText(s.targetLanguage);
        unlimited.setSelected(runtime.unlimitedCost());
        keyMasked.setText(runtime.secrets().masked());
        String ip = runtime.instructionPath();
        instructionLabel.setText(ip.isEmpty() ? "chưa chọn" : Path.of(ip).getFileName().toString());
        profileUndo.setDisable(!runtime.canUndoProfile());
        loading = false;
        fillModels();
        refreshRecommendations();
    }

    private void fillModels() {
        String q = modelSearch.getText() == null ? "" : modelSearch.getText().toLowerCase(Locale.ROOT).trim();
        List<String> ids = new ArrayList<>();
        for (ModelCatalog.ModelInfo m : runtime.models()) if (q.isEmpty() || m.id.toLowerCase(Locale.ROOT).contains(q)) ids.add(m.id);
        boolean was = loading;
        loading = true;
        modelList.getItems().setAll(ids.size() > 300 ? ids.subList(0, 300) : ids);
        loading = was;
    }

    private void refreshRecommendations() {
        List<String[]> diffs = runtime.recommendationDiffs();
        if (diffs.isEmpty()) recommendationBox.setText("Cài đặt hiện tại đã khớp khuyến nghị.");
        else {
            StringBuilder sb = new StringBuilder();
            for (String[] d : diffs) sb.append(d[1]).append(": hiện ").append(d[2]).append(" → khuyến nghị ").append(d[3]).append("\n   ").append(d[4]).append('\n');
            recommendationBox.setText(sb.toString().trim());
        }
        applyRecommendation.setDisable(diffs.isEmpty());
        undoRecommendation.setDisable(!runtime.canUndoRecommendation());
    }

    private void applyRecommendation() {
        List<String[]> diffs = runtime.recommendationDiffs();
        if (diffs.isEmpty()) return;
        if (!app.confirm("Áp dụng cài đặt khuyến nghị?", diffs.size() + " mục sẽ đổi (không đụng provider, API key hay giới hạn chi phí). Có thể Hoàn tác.")) return;
        try {
            runtime.applyRecommendation();
            reload();
            say(DesktopSettingsModel.QUALITY, "Đã áp dụng cài đặt khuyến nghị.", false);
            afterChange();
        } catch (Exception e) {
            say(DesktopSettingsModel.QUALITY, "Không áp dụng được: " + e.getMessage(), true);
        }
    }

    private void undoRecommendation() {
        try {
            runtime.undoRecommendation();
            reload();
            say(DesktopSettingsModel.QUALITY, "Đã hoàn tác cài đặt khuyến nghị.", false);
            afterChange();
        } catch (Exception e) {
            say(DesktopSettingsModel.QUALITY, "Không hoàn tác được: " + e.getMessage(), true);
        }
    }

    private void unlimitedChanged() {
        if (loading) return;
        if (unlimited.isSelected()) {
            if (!app.confirm("Bỏ trần chi phí?", "Lượt dịch có thể phát sinh chi phí không giới hạn.")) { unlimited.setSelected(false); return; }
        }
        runtime.setUnlimitedCost(unlimited.isSelected());
        say(DesktopSettingsModel.COST, unlimited.isSelected() ? "Đã bỏ trần chi phí cho các lượt dịch sau." : "Đã bật lại trần chi phí.", false);
    }

    private void saveKey() {
        String shape = DpapiSecrets.validate(runtime.loadSettings().provider, keyField.getText());
        if (shape != null) { say(DesktopSettingsModel.CONNECTION, shape, true); return; }
        try {
            runtime.secrets().save(keyField.getText());
            keyField.clear();
            keyMasked.setText(runtime.secrets().masked());
            say(DesktopSettingsModel.CONNECTION, "Đã lưu key (mã hóa theo tài khoản Windows này).", false);
        } catch (Exception ex) {
            say(DesktopSettingsModel.CONNECTION, "Không lưu được key: " + ex.getMessage(), true);
        }
    }

    private void testApi() {
        AppSettings s = runtime.loadSettings();
        if (!app.confirm("Kiểm tra API?", "Sẽ gọi provider (" + s.provider + " / " + s.model + ") với một yêu cầu rất ngắn và tốn một ít tiền thật.")) return;
        runtime.confirmSpend();
        say(DesktopSettingsModel.CONNECTION, "Đang kiểm tra API…", false);
        UiTasks.run(app, "Đang kiểm tra API…", runtime::testApi, r -> say(DesktopSettingsModel.CONNECTION, r, r.startsWith("API lỗi")),
                e -> say(DesktopSettingsModel.CONNECTION, "API lỗi: " + e.getMessage(), true));
    }

    private void refreshCatalog() {
        if (!app.confirm("Làm mới catalog?", "App sẽ tải danh sách model và giá từ openrouter.ai (không gửi API key, không gọi provider dịch).")) return;
        runtime.refreshCatalog();
        say(DesktopSettingsModel.CONNECTION, "Đang tải catalog… mở lại trang sau vài giây.", false);
    }

    private void chooseInstruction() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML / TXT", "*.yaml", "*.yml", "*.txt"));
        File f = fc.showOpenDialog(app.stage());
        if (f == null) return;
        runtime.setInstructionPath(f.getAbsolutePath());
        reload();
        app.translatePage().refreshProfiles();
        app.translatePage().refreshEstimate();
    }

    private void saveLanguages() {
        if (loading) return;
        try {
            AppSettings s = runtime.loadSettings();
            if (!sourceLang.getText().trim().isEmpty()) s.sourceLanguage = sourceLang.getText().trim();
            if (!targetLang.getText().trim().isEmpty()) s.targetLanguage = targetLang.getText().trim();
            runtime.saveSettings(s);
            app.translatePage().refreshProfiles();
        } catch (Exception e) {
            globalSay("Không lưu được ngôn ngữ: " + e.getMessage(), true);
        }
    }

    private void exportLog() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Xuất toàn bộ log");
        fc.setInitialFileName("tbl_runtime.log");
        File f = fc.showSaveDialog(app.stage());
        if (f == null) return;
        try {
            runtime.exportLog(f.toPath());
            globalSay("Đã xuất log (không có văn bản sách và API key).", false);
        } catch (Exception e) {
            globalSay("Không xuất được log: " + e.getMessage(), true);
        }
    }

    void message(String text) { globalSay(text, false); }

    /** After a profile was imported or undone from outside this page. */
    void profileChanged() { reload(); }
}
