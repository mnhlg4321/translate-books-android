package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.DesktopRawBulk;
import com.ml.tblandroidtxt.DesktopRawStore;
import com.ml.tblandroidtxt.DesktopRuntime;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/** Thư viện · RAW: list, search, rename, replace, multi-select delete with undo, export, drag and drop import. */
final class RawPage {
    private final DesktopApp app;
    private final DesktopRuntime runtime;
    private final ObservableList<DesktopRawStore.Item> all = FXCollections.observableArrayList();
    private final FilteredList<DesktopRawStore.Item> shown = new FilteredList<>(all, i -> true);
    private final ListView<DesktopRawStore.Item> list = new ListView<>(shown);
    private final TextField search = new TextField();
    private final Label message = new Label();
    private final Button undoButton = new Button("Hoàn tác");
    private final Button rename = new Button("Đổi tên");
    private final Button replace = new Button("Thay tệp…");
    private final Button delete = new Button("Xóa");
    private final Button makeSet = new Button("Tạo tập");
    private final Button export = new Button("Xuất…");
    private final Node root;
    private DesktopRawBulk.Pending pending;
    private PauseTransition undoTimer;

    RawPage(DesktopApp app, DesktopRuntime runtime, Node toolbar) {
        this.app = app;
        this.runtime = runtime;
        this.root = build(toolbar);
        app.registerSearch(NavModel.RAW, search);
        reload();
    }

    Node node() { return root; }

    private Node build(Node toolbar) {
        search.setPromptText("Tìm theo tên…");
        search.textProperty().addListener((o, a, b) -> applyFilter());
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        list.setPlaceholder(new Label("Chưa có RAW. Bấm \"Thêm RAW\" hoặc kéo tệp .txt vào đây."));
        list.setCellFactory(l -> new ListCell<DesktopRawStore.Item>() {
            @Override protected void updateItem(DesktopRawStore.Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.chapterLabel() + "  ·  " + item.name + "  ·  " + item.chars + " ký tự");
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);
        list.setOnDragOver(this::dragOver);
        list.setOnDragDropped(this::dropped);
        list.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<DesktopRawStore.Item>) c -> updateButtons());

        Button add = new Button("Thêm RAW (TXT)…");
        Button selectAll = new Button("Chọn tất cả");
        for (Button b : new Button[]{add, selectAll, rename, replace, delete, makeSet, export, undoButton}) b.getStyleClass().add("secondary");
        undoButton.getStyleClass().remove("secondary");
        undoButton.getStyleClass().add("primary");
        undoButton.setVisible(false);
        undoButton.setManaged(false);
        add.setOnAction(e -> chooseAndAdd());
        selectAll.setOnAction(e -> list.getSelectionModel().selectAll());
        rename.setOnAction(e -> renameSelected());
        replace.setOnAction(e -> replaceSelected());
        delete.setOnAction(e -> deleteSelected());
        makeSet.setOnAction(e -> app.createSetFromRaws(selectedIds()));
        export.setOnAction(e -> exportSelected());
        undoButton.setOnAction(e -> undoNow());
        message.setWrapText(true);

        HBox actions = new HBox(8, add, selectAll, rename, replace, delete, makeSet, export);
        javafx.scene.layout.FlowPane flow = new javafx.scene.layout.FlowPane(8, 8, add, selectAll, rename, replace, delete, makeSet, export);
        VBox card = DesktopApp.card(search, flow, list, new HBox(10, message, undoButton));
        VBox.setVgrow(card, Priority.ALWAYS);
        updateButtons();
        return DesktopApp.page(NavModel.title(NavModel.RAW), toolbar, card);
    }

    void reload() {
        List<String> keep = selectedIds();
        all.setAll(runtime.raws().list());
        applyFilter();
        for (int i = 0; i < shown.size(); i++) if (keep.contains(shown.get(i).id)) list.getSelectionModel().select(i);
        updateButtons();
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        shown.setPredicate(i -> q.isEmpty() || i.name.toLowerCase(Locale.ROOT).contains(q) || i.originalName.toLowerCase(Locale.ROOT).contains(q));
    }

    private List<String> selectedIds() {
        return list.getSelectionModel().getSelectedItems().stream().map(i -> i.id).collect(Collectors.toList());
    }

    private void updateButtons() {
        int n = list.getSelectionModel().getSelectedItems().size();
        rename.setDisable(n != 1);
        replace.setDisable(n != 1);
        delete.setDisable(n == 0);
        makeSet.setDisable(n == 0);
        export.setDisable(n == 0);
    }

    private void say(String text, boolean error) {
        message.setText(text);
        message.getStyleClass().removeAll("err", "ok");
        message.getStyleClass().add(error ? "err" : "ok");
    }

    // ---- import ----

    private void chooseAndAdd() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Thêm RAW (.txt)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Văn bản", "*.txt"));
        List<File> files = fc.showOpenMultipleDialog(app.stage());
        if (files != null) importPaths(files.stream().map(File::toPath).collect(Collectors.toList()));
    }

    void importPaths(List<Path> paths) {
        UiTasks.run(app, "Đang nhập RAW…", () -> runtime.importRawFiles(paths), n -> {
            say("Đã thêm " + n[0] + " · bỏ qua " + n[1] + " (trùng/lỗi)", false);
            app.libraryChanged();
        }, e -> say("Không thêm được RAW: " + e.getMessage(), true));
    }

    private void dragOver(DragEvent e) {
        if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY);
        e.consume();
    }

    private void dropped(DragEvent e) {
        List<Path> txt = new ArrayList<>();
        if (e.getDragboard().hasFiles()) {
            for (File f : e.getDragboard().getFiles()) if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".txt")) txt.add(f.toPath());
        }
        e.setDropCompleted(!txt.isEmpty());
        e.consume();
        if (txt.isEmpty()) say("Chỉ nhận tệp .txt.", true);
        else importPaths(txt);
    }

    // ---- one RAW ----

    private void renameSelected() {
        DesktopRawStore.Item item = list.getSelectionModel().getSelectedItem();
        if (item == null) return;
        TextInputDialog d = new TextInputDialog(item.name);
        d.setTitle("Đổi tên RAW");
        d.setHeaderText("Tên mới cho \"" + item.name + "\"");
        d.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        Optional<String> r = app.showDialog(d);
        if (!r.isPresent()) return;
        if (!runtime.raws().rename(item.id, r.get())) { say("Tên không hợp lệ", true); return; }
        say("Đã đổi tên.", false);
        app.libraryChanged();
    }

    private void replaceSelected() {
        DesktopRawStore.Item item = list.getSelectionModel().getSelectedItem();
        if (item == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Thay nội dung của \"" + item.name + "\" bằng tệp .txt");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Văn bản", "*.txt"));
        File f = fc.showOpenDialog(app.stage());
        if (f == null) return;
        if (!app.confirm("Thay tệp?", "Nội dung của \"" + item.name + "\" sẽ bị ghi đè bằng " + f.getName() + ". Các tập đang dùng RAW này vẫn trỏ tới nó.")) return;
        UiTasks.run(app, "Đang thay tệp…", () -> runtime.replaceRaw(item.id, f.toPath()), problem -> {
            if (problem != null) say("Không thay được: " + problem, true);
            else { say("Đã thay nội dung của " + item.name + ".", false); app.libraryChanged(); }
        }, e -> say("Không thay được: " + e.getMessage(), true));
    }

    // ---- delete with undo ----

    void qaDelete() { list.getSelectionModel().selectAll(); deleteSelected(); }

    private void deleteSelected() {
        List<String> ids = selectedIds();
        if (ids.isEmpty()) return;
        DesktopRawBulk.Plan plan = runtime.rawBulk().plan(ids);
        if (plan.deletableCount() == 0) { say(plan.firstMessage(), true); return; }
        if (!app.confirm("Xóa " + plan.deletableCount() + " RAW?", plan.firstMessage())) return;
        if (plan.needsSecondConfirm() && !app.confirm("Xác nhận lần hai", plan.secondMessage())) return;
        settle();
        pending = runtime.rawBulk().delete(plan);
        int n = pending.count();
        say("Đã xóa " + n + " RAW" + (plan.blockedCount() > 0 ? " · bỏ qua " + plan.blockedCount() + " (còn job dịch dở)" : "") + " · còn 10 giây để Hoàn tác", false);
        undoButton.setVisible(true);
        undoButton.setManaged(true);
        undoTimer = new PauseTransition(Duration.millis(DesktopRawBulk.UNDO_MS));
        undoTimer.setOnFinished(e -> { settle(); message.setText(""); });
        undoTimer.play();
        app.libraryChanged();
    }

    /** Ends an open undo window: the files of the removed RAW go for good. */
    void settle() {
        if (undoTimer != null) undoTimer.stop();
        undoTimer = null;
        if (pending != null) runtime.rawBulk().expire(pending);
        pending = null;
        undoButton.setVisible(false);
        undoButton.setManaged(false);
    }

    private void undoNow() {
        if (pending == null) return;
        if (undoTimer != null) undoTimer.stop();
        int back = runtime.rawBulk().undo(pending);
        pending = null;
        undoButton.setVisible(false);
        undoButton.setManaged(false);
        say("Đã khôi phục " + back + " RAW.", false);
        app.libraryChanged();
    }

    // ---- export ----

    private void exportSelected() {
        List<String> ids = selectedIds();
        if (ids.isEmpty()) return;
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Xuất " + ids.size() + " RAW ra thư mục");
        File dir = dc.showDialog(app.stage());
        if (dir == null) return;
        UiTasks.run(app, "Đang xuất RAW…", () -> runtime.rawBulk().export(ids, dir.toPath()),
                n -> say("Đã xuất " + n + " tệp .txt vào " + dir.getName(), false), e -> say("Không xuất được: " + e.getMessage(), true));
    }
}
