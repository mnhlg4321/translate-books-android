package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.DesktopLibraryBulk;
import com.ml.tblandroidtxt.DesktopPronounEditor;
import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.PronounStoreCore;

import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.nio.charset.StandardCharsets;
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
    private final Button bulkDelete = new Button("Xóa (0)");
    private final Button bulkExport = new Button("Xuất (0)…");
    private final Button bulkUse = new Button("Dùng");
    private final HBox bulkBar = new HBox(8, bulkDelete, bulkExport, bulkUse);
    private final Label count = new Label();
    private final UndoBar undo = new UndoBar();
    private final TickList<PronounStoreCore.Profile> ticks = new TickList<>(list, p -> p.id, this::rowText, this::updateListButtons);

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
        list.setOnMouseClicked(e -> { if (e.getClickCount() == 2) openSelected(); });
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateListButtons());
        VBox.setVgrow(list, Priority.ALWAYS);
        Button importButton = new Button("+ Nhập Pronoun…");
        for (Button b : new Button[]{importButton, open, use, replaceFile, rename, bulkDelete, bulkExport, bulkUse}) b.getStyleClass().add("secondary");
        importButton.setOnAction(e -> chooseAndImport());
        open.setOnAction(e -> openSelected());
        use.setOnAction(e -> useSelected());
        replaceFile.setOnAction(e -> replaceSelected(list.getSelectionModel().getSelectedItem()));
        rename.setOnAction(e -> renameProfile(list.getSelectionModel().getSelectedItem()));
        bulkDelete.setOnAction(e -> deleteTicked());
        bulkExport.setOnAction(e -> exportTicked());
        bulkUse.setOnAction(e -> useTicked());
        listNote.setWrapText(true);
        count.getStyleClass().add("muted");
        bulkBar.setAlignment(Pos.CENTER_LEFT);
        VBox card = DesktopApp.card(new FlowPane(8, 8, importButton, open, use, replaceFile, rename), ticks.header(), count, list, bulkBar,
                new HBox(10, listNote, undo.node()));
        VBox.setVgrow(card, Priority.ALWAYS);
        updateListButtons();
        return DesktopApp.page(NavModel.title(NavModel.PRONOUN), toolbar, card);
    }

    /** One Pronoun row: active marker, name, rule count and conflict warnings. */
    private String rowText(PronounStoreCore.Profile p) {
        int warnings = DesktopPronounEditor.conflictCount(p);
        return (p.id.equals(runtime.selectedPronounId()) ? "● Đang dùng   " : "") + p.name + "  ·  " + p.count() + " quy tắc"
                + (warnings == 0 ? "" : "  ·  cảnh báo " + warnings);
    }

    void reload() {
        String keep = list.getSelectionModel().getSelectedItem() == null ? "" : list.getSelectionModel().getSelectedItem().id;
        list.getItems().setAll(runtime.pronouns());
        for (int i = 0; i < list.getItems().size(); i++) if (list.getItems().get(i).id.equals(keep)) list.getSelectionModel().select(i);
        ticks.retainOnly(list.getItems());
        count.setText(list.getItems().size() + " Pronoun");
        list.refresh();
        updateListButtons();
    }

    private void updateListButtons() {
        boolean none = list.getSelectionModel().getSelectedItem() == null;
        for (Button b : new Button[]{open, use, replaceFile, rename}) b.setDisable(none);
        int n = ticks.count();
        bulkBar.setVisible(n > 0);
        bulkBar.setManaged(n > 0);
        bulkDelete.setText("Xóa (" + n + ")");
        bulkExport.setText("Xuất (" + n + ")…");
        bulkUse.setDisable(n != 1);
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

    /** "Dùng" of the bar: only when exactly one Pronoun is ticked. */
    private void useTicked() {
        List<String> ids = ticks.ids();
        if (ids.size() != 1) return;
        PronounStoreCore.Profile p = runtime.pronoun(ids.get(0));
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

    private void deleteTicked() { deleteIds(ticks.ids()); }

    /** Bulk delete: the usage list, a second confirmation for a Pronoun in use, then a 10 s undo that restores the stored objects. */
    private void deleteIds(List<String> ids) {
        if (ids.isEmpty()) return;
        DesktopLibraryBulk.Plan plan = runtime.pronounPlan(ids);
        if (plan.deletableCount() == 0) { say(listNote, plan.firstMessage(), true); return; }
        if (!app.confirm("Xóa " + plan.deletableCount() + " Pronoun?", plan.firstMessage())) return;
        if (plan.needsSecondConfirm() && !app.confirmWithLabel("Xác nhận lần hai", plan.secondMessage(), "Vẫn xóa")) return;
        undo.settle();
        DesktopLibraryBulk.Undo removed = runtime.deletePronouns(plan.deletableIds());
        if (removed == null) { say(listNote, "Không xóa được: kho Pronoun đang bị khóa.", true); return; }
        if (editor != null && ids.contains(editor.id())) { editor = null; showList(); }
        ticks.clear();
        int n = removed.count();
        say(listNote, "Đã xóa " + n + " Pronoun.", false);
        undo.show("Đã xóa " + n + " Pronoun", () -> {
            int back = runtime.restoreDeleted(removed);
            say(listNote, "Đã khôi phục " + back + " Pronoun.", false);
            app.libraryChanged();
        }, () -> runtime.expireDeleted(removed));
        app.libraryChanged();
    }

    /** Export of the bar: one .csv per ticked Pronoun with its stored text; a name that is taken gets a number. */
    private void exportTicked() {
        List<String> ids = ticks.ids();
        if (ids.isEmpty()) return;
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Xuất " + ids.size() + " Pronoun ra thư mục");
        File dir = dc.showDialog(app.stage());
        if (dir == null) return;
        UiTasks.run(app, "Đang xuất Pronoun…", () -> {
            int written = 0;
            for (String id : ids) {
                PronounStoreCore.Profile p = runtime.pronoun(id);
                if (p == null) continue;
                DesktopLibraryBulk.writeNew(dir.toPath(), p.name + ".csv", p.text.getBytes(StandardCharsets.UTF_8));
                written++;
            }
            return written;
        }, n -> say(listNote, "Đã xuất " + n + " Pronoun (CSV) vào " + dir.getName() + ". Tệp trùng tên được thêm số, không ghi đè.", false),
                e -> say(listNote, "Không xuất được: " + e.getMessage(), true));
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
        del.setOnAction(e -> { if (editor != null) deleteIds(java.util.Collections.singletonList(editor.id())); });

        table.setPlaceholder(new Label("Chưa có quy tắc nào được đọc từ văn bản bên dưới."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.widthProperty().addListener((o, a, b) -> fitColumns());
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

    /** Columns share the table width by their weight (user data), with a floor so a short column never collapses to "…". */
    private void fitColumns() {
        double total = 0;
        for (TableColumn<String[], ?> c : table.getColumns()) total += (Double) c.getUserData();
        if (total <= 0) return;
        double usable = Math.max(0, table.getWidth() - 24);
        for (TableColumn<String[], ?> c : table.getColumns()) {
            double w = usable * (Double) c.getUserData() / total;
            c.setMinWidth(Math.min(70, w));
            c.setPrefWidth(w);
        }
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
                // The rule text columns ("from", "note") take twice the share of the short ones; fitColumns() sizes them to the table width.
                c.setUserData(cols[i].equals("from") || cols[i].equals("note") ? 2.0 : 1.0);
                table.getColumns().add(c);
            }
        }
        fitColumns();
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

    /** QA: ticks the first n Pronouns of the list. */
    void qaTickFirst(int n) { ticks.tickFirst(n); }

    /** QA: ticks the first two Pronouns and asks for the delete, the same path as the button; the dialog is photographed and cancelled. */
    void qaDelete() {
        ticks.tickFirst(2);
        deleteTicked();
    }

    void qaClearTicks() { ticks.clear(); }

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
