package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.DesktopPronounEditor;
import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.PronounStoreCore;

import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Thư viện · Pronoun: the list of profiles, and for the open one a read-only rule table plus the raw text to edit. */
final class PronounPage {
    private final DesktopApp app;
    private final DesktopRuntime runtime;
    private final StackPane root = new StackPane();
    private final ListView<PronounStoreCore.Profile> list = new ListView<>();
    private final Label listNote = new Label();
    private final Button open = new Button("Mở / Sửa");
    private final Button use = new Button("Dùng cho bản dịch");
    private final Button replaceFile = new Button("Thay file…");
    private final Button rename = new Button("Đổi tên");
    private final Button delete = new Button("Xóa");

    private final VBox editorPage;
    private final Label status = new Label();
    private final TableView<String[]> table = new TableView<>();
    private final TextArea textArea = new TextArea();
    private final Label editorNote = new Label();
    private final Label dirtyMark = new Label();
    private DesktopPronounEditor editor;
    private boolean loading;
    private String shownColumns = "";
    private final PauseTransition reparse = new PauseTransition(Duration.millis(300));

    PronounPage(DesktopApp app, DesktopRuntime runtime, Node toolbar) {
        this.app = app;
        this.runtime = runtime;
        VBox listPage = buildList(toolbar);
        editorPage = buildEditor();
        editorPage.setVisible(false);
        root.getChildren().addAll(listPage, editorPage);
        app.registerSearch(NavModel.PRONOUN, new TextField());
        app.registerLeaveGuard(NavModel.PRONOUN, this::confirmLeave);
        reparse.setOnFinished(e -> refreshTable());
        reload();
    }

    Node node() { return root; }

    // ---- list ----

    private VBox buildList(Node toolbar) {
        list.setPlaceholder(new Label("Chưa có hồ sơ Pronoun. Bấm \"+ Nhập Pronoun\" để lấy từ tệp CSV."));
        list.setCellFactory(l -> new ListCell<PronounStoreCore.Profile>() {
            @Override protected void updateItem(PronounStoreCore.Profile p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); return; }
                int warnings = DesktopPronounEditor.conflictCount(p);
                setText((p.id.equals(runtime.selectedPronounId()) ? "● Đang dùng   " : "") + p.name + "  ·  " + p.count() + " quy tắc"
                        + (warnings == 0 ? "" : "  ·  cảnh báo " + warnings));
            }
        });
        list.setOnMouseClicked(e -> { if (e.getClickCount() == 2) openSelected(); });
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateListButtons());
        VBox.setVgrow(list, Priority.ALWAYS);
        Button importButton = new Button("+ Nhập Pronoun…");
        for (Button b : new Button[]{importButton, open, use, replaceFile, rename, delete}) b.getStyleClass().add("secondary");
        importButton.setOnAction(e -> chooseAndImport());
        open.setOnAction(e -> openSelected());
        use.setOnAction(e -> useSelected());
        replaceFile.setOnAction(e -> replaceSelected(list.getSelectionModel().getSelectedItem()));
        rename.setOnAction(e -> renameProfile(list.getSelectionModel().getSelectedItem()));
        delete.setOnAction(e -> deleteProfile(list.getSelectionModel().getSelectedItem()));
        listNote.setWrapText(true);
        VBox card = DesktopApp.card(new FlowPane(8, 8, importButton, open, use, replaceFile, rename, delete), list, listNote);
        VBox.setVgrow(card, Priority.ALWAYS);
        updateListButtons();
        return DesktopApp.page(NavModel.title(NavModel.PRONOUN), toolbar, card);
    }

    void reload() {
        String keep = list.getSelectionModel().getSelectedItem() == null ? "" : list.getSelectionModel().getSelectedItem().id;
        list.getItems().setAll(runtime.pronouns());
        for (int i = 0; i < list.getItems().size(); i++) if (list.getItems().get(i).id.equals(keep)) list.getSelectionModel().select(i);
        list.refresh();
        updateListButtons();
    }

    private void updateListButtons() {
        boolean none = list.getSelectionModel().getSelectedItem() == null;
        for (Button b : new Button[]{open, use, replaceFile, rename, delete}) b.setDisable(none);
    }

    private void say(Label target, String text, boolean error) {
        target.setText(text);
        target.getStyleClass().removeAll("err", "ok");
        target.getStyleClass().add(error ? "err" : "ok");
    }

    private void chooseAndImport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Nhập Pronoun");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV / TXT", "*.csv", "*.txt"));
        List<File> files = fc.showOpenMultipleDialog(app.stage());
        if (files == null) return;
        List<Path> paths = files.stream().map(File::toPath).collect(Collectors.toList());
        UiTasks.run(app, "Đang nhập Pronoun…", () -> runtime.importPronounFiles(paths), r -> {
            String text = "Đã nhập " + r.imported + " hồ sơ Pronoun.";
            if (!r.failed.isEmpty()) text += " Bỏ qua " + r.failed.size() + ": " + String.join("; ", r.failed);
            say(listNote, text, !r.failed.isEmpty() && r.imported == 0);
            app.libraryChanged();
        }, e -> say(listNote, "Không nhập được: " + e.getMessage(), true));
    }

    private void useSelected() {
        PronounStoreCore.Profile p = list.getSelectionModel().getSelectedItem();
        if (p == null) return;
        runtime.selectPronoun(p.id);
        say(listNote, "Đang dùng \"" + p.name + "\" cho bản dịch.", false);
        app.libraryChanged();
    }

    private void renameProfile(PronounStoreCore.Profile p) {
        if (p == null) return;
        TextInputDialog d = new TextInputDialog(p.name);
        d.setTitle("Đổi tên Pronoun");
        d.setHeaderText("Tên mới cho \"" + p.name + "\"");
        Optional<String> r = app.showDialog(d);
        if (!r.isPresent()) return;
        if (!runtime.renamePronoun(p.id, r.get())) { say(editor == null ? listNote : editorNote, "Tên không hợp lệ", true); return; }
        if (editor != null && editor.id().equals(p.id)) editor.setName(r.get());
        say(editor == null ? listNote : editorNote, "Đã đổi tên.", false);
        app.libraryChanged();
    }

    private void deleteProfile(PronounStoreCore.Profile p) {
        if (p == null) return;
        if (!app.confirm("Xóa Pronoun \"" + p.name + "\"?", "Không xóa file gốc trong máy.")) return;
        if (!runtime.deletePronoun(p.id)) { say(listNote, "Không xóa được: kho Pronoun đang bị khóa.", true); return; }
        if (editor != null && editor.id().equals(p.id)) { editor = null; showList(); }
        say(listNote, "Đã xóa \"" + p.name + "\".", false);
        app.libraryChanged();
    }

    private void replaceSelected(PronounStoreCore.Profile p) {
        if (p == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Thay nội dung của \"" + p.name + "\" bằng tệp");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV / TXT", "*.csv", "*.txt"));
        File f = fc.showOpenDialog(app.stage());
        if (f == null) return;
        if (!app.confirm("Thay file?", "Nội dung của \"" + p.name + "\" sẽ bị ghi đè bằng " + f.getName() + " và hồ sơ này thành hồ sơ đang dùng.")) return;
        Label target = editor == null ? listNote : editorNote;
        UiTasks.run(app, "Đang thay file…", () -> runtime.replacePronounFile(p.id, f.toPath()), problem -> {
            if (problem != null) { say(target, "Không thay được: " + problem, true); return; }
            say(target, "Đã thay file Pronoun: " + f.getName(), false);
            if (editor != null && editor.id().equals(p.id)) openEditor(runtime.pronoun(p.id));
            app.libraryChanged();
        }, e -> say(target, "Không thay được: " + e.getMessage(), true));
    }

    private void openSelected() {
        PronounStoreCore.Profile p = list.getSelectionModel().getSelectedItem();
        if (p != null) openEditor(p);
    }

    // ---- editor ----

    private VBox buildEditor() {
        Button back = new Button("← Danh sách");
        Button useBtn = new Button("Dùng cho bản dịch");
        Button save = new Button("Lưu");
        Button health = new Button("Kiểm tra dữ liệu");
        Button renameBtn = new Button("Đổi tên");
        Button replaceBtn = new Button("Thay file…");
        Button del = new Button("Xóa");
        for (Button b : new Button[]{back, useBtn, health, renameBtn, replaceBtn, del}) b.getStyleClass().add("secondary");
        save.getStyleClass().add("primary");
        back.setOnAction(e -> { if (confirmLeave()) { editor = null; showList(); reload(); } });
        useBtn.setOnAction(e -> { if (saveNow()) { runtime.selectPronoun(editor.id()); say(editorNote, "Đang dùng \"" + editor.name() + "\" cho bản dịch.", false); app.libraryChanged(); } });
        save.setOnAction(e -> { if (saveNow()) say(editorNote, "Đã lưu \"" + editor.name() + "\".", false); });
        health.setOnAction(e -> app.showText("Kiểm tra Glossary / Pronoun", editor.healthText(runtime.selectedGlossaryText())));
        renameBtn.setOnAction(e -> { if (editor != null) renameProfile(runtime.pronoun(editor.id())); });
        replaceBtn.setOnAction(e -> { if (editor != null) replaceSelected(runtime.pronoun(editor.id())); });
        del.setOnAction(e -> { if (editor != null) deleteProfile(runtime.pronoun(editor.id())); });

        table.setPlaceholder(new Label("Chưa có quy tắc nào được đọc từ văn bản bên dưới."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(230);
        textArea.setStyle("-fx-font-family: 'Consolas', monospace;");
        textArea.textProperty().addListener((o, a, b) -> {
            if (loading || editor == null) return;
            editor.setText(b);
            markDirty();
            reparse.playFromStart();
        });
        VBox.setVgrow(textArea, Priority.ALWAYS);
        status.setWrapText(true);
        editorNote.setWrapText(true);
        dirtyMark.getStyleClass().add("warn");
        Label tableHint = new Label("Bảng chỉ để xem cách app hiểu các dòng. Sửa ở ô văn bản bên dưới rồi bấm Lưu.");
        tableHint.getStyleClass().add("muted");
        VBox card = DesktopApp.card(new FlowPane(8, 8, back, useBtn, save, health, renameBtn, replaceBtn, del), status, table, tableHint,
                new Label("Văn bản Pronoun (lưu nguyên như bạn gõ)"), textArea, new HBox(12, dirtyMark, editorNote));
        VBox.setVgrow(card, Priority.ALWAYS);
        return DesktopApp.page(NavModel.title(NavModel.PRONOUN), card);
    }

    void openEditor(PronounStoreCore.Profile p) {
        editor = DesktopPronounEditor.open(p);
        loading = true;
        textArea.setText(editor.text());
        loading = false;
        editorNote.setText("");
        listVisible(false);
        refreshTable();
        markDirty();
    }

    private void refreshTable() {
        if (editor == null) return;
        String[] cols = editor.columns();
        String key = String.join(",", cols);
        if (!key.equals(shownColumns)) {
            shownColumns = key;
            table.getColumns().clear();
            for (int i = 0; i < cols.length; i++) {
                final int index = i;
                TableColumn<String[], String> c = new TableColumn<>(cols[i]);
                c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(index < cd.getValue().length ? cd.getValue()[index] : ""));
                table.getColumns().add(c);
            }
        }
        table.getItems().setAll(editor.rows());
        status.setText(editor.statusLine() + (editor.legacyOnly() ? " · định dạng cũ 3 cột (giữ nguyên khi lưu)" : ""));
        status.getStyleClass().removeAll("ok", "warn");
        status.getStyleClass().add(editor.warningCount() == 0 ? "ok" : "warn");
    }

    private void markDirty() {
        dirtyMark.setText(editor != null && editor.dirty() ? "● Có thay đổi chưa lưu" : "");
    }

    private void listVisible(boolean showList) {
        root.getChildren().get(0).setVisible(showList);
        editorPage.setVisible(!showList);
    }

    private void showList() { listVisible(true); }

    boolean editing() { return editor != null; }

    /** QA: closes the editor without asking. */
    void qaClose() { editor = null; showList(); reload(); }

    DesktopPronounEditor currentEditor() { return editor; }

    private boolean saveNow() {
        if (editor == null) return false;
        if (!editor.save(runtime)) { say(editorNote, "Không lưu được: kho Pronoun đang bị khóa vì đọc lỗi.", true); return false; }
        refreshTable();
        markDirty();
        reload();
        app.libraryChanged();
        return true;
    }

    boolean confirmLeave() {
        if (editor == null || !editor.dirty()) return true;
        int answer = app.askSave("Pronoun \"" + editor.name() + "\" có thay đổi chưa lưu.");
        if (answer < 0) return false;
        if (answer == 1) return saveNow();
        return true;
    }
}
