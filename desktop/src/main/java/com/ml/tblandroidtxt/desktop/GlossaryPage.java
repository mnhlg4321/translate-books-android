package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.DesktopGlossaryEditor;
import com.ml.tblandroidtxt.DesktopLibraryBulk;
import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.GlossaryStoreCore;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Thư viện · Glossary: the list of Glossaries, and a four-column term table for the one that is open. */
final class GlossaryPage {
    private static final String ALL = "Tất cả loại";

    private final DesktopApp app;
    private final DesktopRuntime runtime;
    private final StackPane root = new StackPane();
    private final ListView<GlossaryStoreCore.Glossary> list = new ListView<>();
    private final Label listNote = new Label();
    private final Button open = new Button("Mở");
    private final Button use = new Button("Dùng cho bản dịch");
    private final Button bulkDelete = new Button("Xóa (0)");
    private final Button bulkExport = new Button("Xuất (0)…");
    private final HBox bulkBar = new HBox(8, bulkDelete, bulkExport);
    private final Label count = new Label();
    private final UndoBar undo = new UndoBar();
    private final TickList<GlossaryStoreCore.Glossary> ticks = new TickList<>(list, g -> g.id, this::rowText, this::updateListButtons);

    private final VBox editorPage;
    private final TextField nameField = new TextField();
    private final TextField sourceLang = new TextField();
    private final TextField targetLang = new TextField();
    private final TextField termSearch = new TextField();
    private final ComboBox<String> categoryBox = new ComboBox<>();
    private final TableView<Integer> table = new TableView<>();
    private final ObservableList<Integer> rows = FXCollections.observableArrayList();
    private final Label editorNote = new Label();
    private final Label dirtyMark = new Label();
    private DesktopGlossaryEditor editor;
    private boolean updatingFilter;

    GlossaryPage(DesktopApp app, DesktopRuntime runtime, Node toolbar) {
        this.app = app;
        this.runtime = runtime;
        VBox listPage = buildList(toolbar);
        editorPage = buildEditor();
        editorPage.setVisible(false);
        root.getChildren().addAll(listPage, editorPage);
        app.registerSearch(NavModel.GLOSSARY, termSearch);
        app.registerLeaveGuard(NavModel.GLOSSARY, this::confirmLeave);
        reload();
    }

    Node node() { return root; }

    // ---- list ----

    private VBox buildList(Node toolbar) {
        list.setPlaceholder(new Label("Chưa có Glossary. Bấm \"+ Nhập Glossary\" để lấy từ tệp CSV, hoặc \"+ Tạo Glossary trống\"."));
        list.setOnMouseClicked(e -> { if (e.getClickCount() == 2) openSelected(); });
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateListButtons());
        VBox.setVgrow(list, Priority.ALWAYS);
        Button importButton = new Button("+ Nhập Glossary…");
        Button blankButton = new Button("+ Tạo Glossary trống");
        for (Button b : new Button[]{importButton, blankButton, open, use, bulkExport}) b.getStyleClass().add("secondary");
        bulkDelete.getStyleClass().add("danger");
        importButton.setOnAction(e -> chooseAndImport());
        blankButton.setOnAction(e -> createBlank());
        open.setOnAction(e -> openSelected());
        use.setOnAction(e -> useSelected());
        bulkDelete.setOnAction(e -> deleteTicked());
        bulkExport.setOnAction(e -> exportTicked());
        listNote.setWrapText(true);
        count.getStyleClass().add("muted");
        bulkBar.setAlignment(Pos.CENTER_LEFT);
        FlowPane buttons = new FlowPane(8, 8, importButton, blankButton, open, use);
        VBox card = DesktopApp.card(buttons, ticks.header(), count, list, bulkBar, new HBox(10, listNote, undo.node()));
        VBox.setVgrow(card, Priority.ALWAYS);
        updateListButtons();
        return DesktopApp.page(NavModel.title(NavModel.GLOSSARY), toolbar, card);
    }

    /** One Glossary row: active marker, name, term count and languages. */
    private String rowText(GlossaryStoreCore.Glossary g) {
        boolean active = g.id.equals(runtime.selectedGlossaryId());
        String langs = blank(g.sourceLang) && blank(g.targetLang) ? "" : "  ·  " + g.sourceLang + " → " + g.targetLang;
        return (active ? "● Đang dùng   " : "") + g.name + "  ·  " + g.count() + " mục" + langs;
    }

    void reload() {
        String keep = list.getSelectionModel().getSelectedItem() == null ? "" : list.getSelectionModel().getSelectedItem().id;
        list.getItems().setAll(runtime.glossaries());
        for (int i = 0; i < list.getItems().size(); i++) if (list.getItems().get(i).id.equals(keep)) list.getSelectionModel().select(i);
        ticks.retainOnly(list.getItems());
        count.setText(list.getItems().size() + " Glossary");
        list.refresh();
        updateListButtons();
    }

    private void updateListButtons() {
        boolean none = list.getSelectionModel().getSelectedItem() == null;
        open.setDisable(none);
        use.setDisable(useTarget() == null);
        int n = ticks.count();
        bulkBar.setVisible(n > 0);
        bulkBar.setManaged(n > 0);
        bulkDelete.setText("Xóa (" + n + ")");
        bulkExport.setText("Xuất (" + n + ")…");
    }

    private static boolean blank(String s) { return s == null || s.trim().isEmpty() || s.trim().equals("—"); }

    private void say(Label target, String text, boolean error) {
        target.setText(text);
        target.getStyleClass().removeAll("err", "ok");
        target.getStyleClass().add(error ? "err" : "ok");
    }

    private void chooseAndImport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Nhập Glossary");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV / TXT", "*.csv", "*.txt"));
        List<File> files = fc.showOpenMultipleDialog(app.stage());
        if (files == null) return;
        List<Path> paths = files.stream().map(File::toPath).collect(Collectors.toList());
        UiTasks.run(app, "Đang nhập Glossary…", () -> runtime.importGlossaries(paths), n -> {
            say(listNote, "Đã nhập " + n + " Glossary (mỗi tệp một Glossary mang tên tệp).", false);
            app.libraryChanged();
        }, e -> say(listNote, "Không nhập được: " + e.getMessage(), true));
    }

    private void createBlank() {
        TextInputDialog d = new TextInputDialog("Glossary mới");
        d.setTitle("Tạo Glossary trống");
        d.setHeaderText("Tên Glossary");
        Optional<String> r = app.showDialog(d);
        if (!r.isPresent()) return;
        GlossaryStoreCore.Glossary g = runtime.createGlossary(r.get());
        if (g == null) { say(listNote, "Không tạo được: kho Glossary đang bị khóa vì đọc lỗi.", true); return; }
        app.libraryChanged();
        openEditor(g);
    }

    /**
     * The one "Dùng" button: the Glossary ticked when exactly one is ticked, otherwise the focused row when nothing is ticked.
     * Null (button disabled) when several are ticked.
     */
    private GlossaryStoreCore.Glossary useTarget() {
        List<String> ids = ticks.ids();
        if (ids.size() == 1) return runtime.glossary(ids.get(0));
        return ids.isEmpty() ? list.getSelectionModel().getSelectedItem() : null;
    }

    private void useSelected() {
        GlossaryStoreCore.Glossary g = useTarget();
        if (g == null) return;
        runtime.selectGlossary(g.id);
        say(listNote, "Đang dùng \"" + g.name + "\" cho bản dịch.", false);
        app.libraryChanged();
    }

    /** Bulk delete: the usage list, a second confirmation for a Glossary in use, then a 10 s undo that restores the stored objects. */
    private void deleteTicked() {
        List<String> ids = ticks.ids();
        if (ids.isEmpty()) return;
        DesktopLibraryBulk.Plan plan = runtime.glossaryPlan(ids);
        if (plan.deletableCount() == 0) { say(listNote, plan.firstMessage(), true); return; }
        if (!app.confirmWithLabel("Xóa " + plan.deletableCount() + " Glossary?", plan.firstMessage(), "Xóa")) return;
        if (plan.needsSecondConfirm() && !app.confirmWithLabel("Xác nhận lần hai", plan.secondMessage(), "Vẫn xóa")) return;
        performDelete(plan);
    }

    /** QA: removes the first two Glossaries without the dialogs, so the undo bar can be photographed (throw-away data only). */
    void qaDeleteNoConfirm() {
        ticks.tickFirst(2);
        performDelete(runtime.glossaryPlan(ticks.ids()));
    }

    private void performDelete(DesktopLibraryBulk.Plan plan) {
        undo.settle();
        DesktopLibraryBulk.Undo removed = runtime.deleteGlossaries(plan.deletableIds());
        if (removed == null) { say(listNote, "Không xóa được: kho Glossary đang bị khóa.", true); return; }
        ticks.clear();
        int n = removed.count();
        // The undo bar says what was deleted; the note is cleared so the same sentence is not shown twice.
        say(listNote, "", false);
        undo.show("Đã xóa " + n + " Glossary", () -> {
            int back = runtime.restoreDeleted(removed);
            say(listNote, "Đã khôi phục " + back + " Glossary.", false);
            app.libraryChanged();
        }, () -> runtime.expireDeleted(removed));
        app.libraryChanged();
    }

    /** Export of the bar: one .csv per ticked Glossary, in the source format; a name that is taken gets a number. */
    private void exportTicked() {
        List<String> ids = ticks.ids();
        if (ids.isEmpty()) return;
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Xuất " + ids.size() + " Glossary ra thư mục");
        File dir = dc.showDialog(app.stage());
        if (dir == null) return;
        UiTasks.run(app, "Đang xuất Glossary…", () -> {
            int written = 0;
            for (String id : ids) {
                GlossaryStoreCore.Glossary g = runtime.glossary(id);
                if (g == null) continue;
                DesktopLibraryBulk.writeNew(dir.toPath(), g.name + ".csv", DesktopLibraryBulk.glossaryCsv(g).getBytes(StandardCharsets.UTF_8));
                written++;
            }
            return written;
        }, n -> say(listNote, "Đã xuất " + n + " Glossary (CSV) vào " + dir.getName() + ". Tệp trùng tên được thêm số, không ghi đè.", false),
                e -> say(listNote, "Không xuất được: " + e.getMessage(), true));
    }

    private void openSelected() {
        GlossaryStoreCore.Glossary g = list.getSelectionModel().getSelectedItem();
        if (g != null) openEditor(g);
    }

    // ---- editor ----

    private VBox buildEditor() {
        Button back = new Button("← Danh sách");
        Button useBtn = new Button("Dùng cho bản dịch");
        Button save = new Button("Lưu");
        Button health = new Button("Kiểm tra dữ liệu");
        Button preview = new Button("Xem trước");
        Button merge = new Button("Gộp file…");
        Button del = new Button("Xóa Glossary");
        for (Button b : new Button[]{back, useBtn, health, preview, merge, del}) b.getStyleClass().add("secondary");
        save.getStyleClass().add("primary");
        back.setOnAction(e -> { if (confirmLeave()) closeEditor(); });
        useBtn.setOnAction(e -> { if (saveNow()) { runtime.selectGlossary(editor.id()); say(editorNote, "Đang dùng \"" + editor.name() + "\" cho bản dịch.", false); app.libraryChanged(); } });
        save.setOnAction(e -> { if (saveNow()) say(editorNote, "Đã lưu Glossary \"" + editor.name() + "\".", false); });
        health.setOnAction(e -> app.showText("Kiểm tra Glossary / Pronoun", editor.healthText(runtime.selectedPronounText())));
        preview.setOnAction(e -> app.showText("Xem trước Glossary • " + editor.size() + " mục", editor.previewText()));
        merge.setOnAction(e -> mergeFiles());
        del.setOnAction(e -> {
            if (editor == null || !app.confirm("Xóa Glossary \"" + editor.name() + "\"?", "Không xóa file gốc trong máy.")) return;
            runtime.deleteGlossary(editor.id());
            editor = null;
            closeEditor();
            app.libraryChanged();
        });

        nameField.setPromptText("Tên");
        sourceLang.setPromptText("Ngôn ngữ nguồn");
        targetLang.setPromptText("Ngôn ngữ đích");
        nameField.textProperty().addListener((o, a, b) -> { if (editor != null && !updatingFilter) { editor.setName(b); markDirty(); } });
        sourceLang.textProperty().addListener((o, a, b) -> { if (editor != null && !updatingFilter) { editor.setSourceLang(b); markDirty(); } });
        targetLang.textProperty().addListener((o, a, b) -> { if (editor != null && !updatingFilter) { editor.setTargetLang(b); markDirty(); } });

        termSearch.setPromptText("Tìm source hoặc target…");
        termSearch.textProperty().addListener((o, a, b) -> refilter());
        categoryBox.setOnAction(e -> { if (!updatingFilter) refilter(); });
        Button add = new Button("+ Thêm thuật ngữ");
        Button remove = new Button("Xóa dòng");
        add.getStyleClass().add("secondary");
        remove.getStyleClass().add("secondary");
        add.setOnAction(e -> addRow());
        remove.setOnAction(e -> removeSelectedRows());

        table.setEditable(true);
        table.setItems(rows);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setPlaceholder(new Label("Chưa có thuật ngữ nào khớp. Bấm \"+ Thêm thuật ngữ\" hoặc \"Gộp file\"."));
        table.getColumns().add(column("source", DesktopGlossaryEditor.SOURCE, 220));
        table.getColumns().add(column("target", DesktopGlossaryEditor.TARGET, 220));
        table.getColumns().add(column("category", DesktopGlossaryEditor.CATEGORY, 120));
        table.getColumns().add(column("note", DesktopGlossaryEditor.NOTE, 300));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        editorNote.setWrapText(true);
        dirtyMark.getStyleClass().add("warn");

        FlowPane top = new FlowPane(8, 8, back, useBtn, save, health, preview, merge, del);
        HBox langs = new HBox(10, nameField, sourceLang, targetLang);
        HBox.setHgrow(nameField, Priority.ALWAYS);
        HBox filter = new HBox(10, termSearch, categoryBox, add, remove);
        HBox.setHgrow(termSearch, Priority.ALWAYS);
        VBox card = DesktopApp.card(top, langs, filter, table, new HBox(12, dirtyMark, editorNote));
        VBox.setVgrow(card, Priority.ALWAYS);
        return DesktopApp.page(NavModel.title(NavModel.GLOSSARY), card);
    }

    private TableColumn<Integer, String> column(String title, int field, double width) {
        TableColumn<Integer, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(editor == null ? "" : editor.field(cd.getValue(), field)));
        c.setCellFactory(TextFieldTableCell.forTableColumn());
        c.setOnEditCommit(e -> {
            if (editor == null) return;
            editor.setField(e.getRowValue(), field, e.getNewValue());
            markDirty();
            table.refresh();
        });
        return c;
    }

    void openEditor(GlossaryStoreCore.Glossary g) {
        editor = DesktopGlossaryEditor.open(g);
        updatingFilter = true;
        nameField.setText(editor.name());
        sourceLang.setText(editor.sourceLang());
        targetLang.setText(editor.targetLang());
        termSearch.clear();
        updatingFilter = false;
        editorNote.setText("");
        listVisible(false);
        refilter();
        markDirty();
    }

    private void closeEditor() {
        editor = null;
        listVisible(true);
        reload();
    }

    private void listVisible(boolean showList) {
        root.getChildren().get(0).setVisible(showList);
        editorPage.setVisible(!showList);
    }

    boolean editing() { return editor != null; }

    /** QA: closes the editor without asking. */
    void qaClose() { closeEditor(); }

    /** QA: ticks the first n Glossaries of the list. */
    void qaTickFirst(int n) { ticks.tickFirst(n); }

    /** QA: ticks the first two Glossaries and asks for the delete, the same path as the button; the dialog is photographed and cancelled. */
    void qaDelete() {
        ticks.tickFirst(2);
        deleteTicked();
    }

    void qaClearTicks() { ticks.clear(); }

    DesktopGlossaryEditor currentEditor() { return editor; }

    private void refilter() {
        if (editor == null) return;
        String keep = categoryBox.getValue();
        updatingFilter = true;
        List<String> options = new ArrayList<>();
        options.add(ALL);
        options.addAll(editor.categories());
        categoryBox.getItems().setAll(options);
        categoryBox.setValue(keep != null && options.contains(keep) ? keep : ALL);
        updatingFilter = false;
        String category = ALL.equals(categoryBox.getValue()) ? "" : categoryBox.getValue();
        rows.setAll(editor.visible(termSearch.getText(), category));
    }

    private void markDirty() {
        dirtyMark.setText(editor != null && editor.dirty() ? "● Có thay đổi chưa lưu" : "");
    }

    private void addRow() {
        if (editor == null) return;
        termSearch.clear();
        categoryBox.setValue(ALL);
        editor.addTerm("", "", "term", "");
        refilter();
        markDirty();
        table.getSelectionModel().clearAndSelect(0);
        table.edit(0, table.getColumns().get(0));
    }

    private void removeSelectedRows() {
        if (editor == null) return;
        List<Integer> picked = new ArrayList<>(table.getSelectionModel().getSelectedItems());
        if (picked.isEmpty()) { say(editorNote, "Chọn dòng cần xóa trước.", true); return; }
        if (!app.confirm("Xóa " + picked.size() + " dòng thuật ngữ?", "Dòng bị xóa khỏi bản đang sửa; bấm Lưu để ghi vào thư viện.")) return;
        editor.removeRows(picked);
        refilter();
        markDirty();
    }

    private boolean saveNow() {
        if (editor == null) return false;
        if (!editor.save(runtime)) { say(editorNote, "Không lưu được: kho Glossary đang bị khóa vì đọc lỗi.", true); return false; }
        updatingFilter = true;
        nameField.setText(editor.name());
        updatingFilter = false;
        refilter();
        markDirty();
        reload();
        app.libraryChanged();
        return true;
    }

    private void mergeFiles() {
        if (editor == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Gộp các tệp CSV vào Glossary này");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV / TXT", "*.csv", "*.txt"));
        List<File> files = fc.showOpenMultipleDialog(app.stage());
        if (files == null) return;
        List<Path> paths = files.stream().map(File::toPath).collect(Collectors.toList());
        UiTasks.run(app, "Đang gộp tệp…", () -> editor.mergeFiles(paths), r -> {
            updatingFilter = true;
            nameField.setText(editor.name());
            updatingFilter = false;
            refilter();
            markDirty();
            say(editorNote, "Đã đọc " + r[0] + " tệp, thêm " + r[1] + " thuật ngữ mới (chưa lưu).", false);
        }, e -> say(editorNote, "Không gộp được: " + e.getMessage(), true));
    }

    /** Leaving the page (or going back to the list) with unsaved edits asks first. True when it may go on. */
    boolean confirmLeave() {
        if (editor == null || !editor.dirty()) return true;
        int answer = app.askSave("Glossary \"" + editor.name() + "\" có thay đổi chưa lưu.");
        if (answer < 0) return false;
        if (answer == 1) return saveNow();
        return true;
    }
}
