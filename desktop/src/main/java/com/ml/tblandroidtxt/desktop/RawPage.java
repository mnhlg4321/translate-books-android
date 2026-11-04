package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.DesktopLibraryBulk;
import com.ml.tblandroidtxt.DesktopRawBulk;
import com.ml.tblandroidtxt.DesktopRawStore;
import com.ml.tblandroidtxt.DesktopRuntime;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/** Thư viện · RAW: tick boxes, search, rename and replace of the focused row, bulk delete with undo, export, create set, drag and drop import. */
final class RawPage {
    private final DesktopApp app;
    private final DesktopRuntime runtime;
    private final ObservableList<DesktopRawStore.Item> all = FXCollections.observableArrayList();
    private final FilteredList<DesktopRawStore.Item> shown = new FilteredList<>(all, i -> true);
    private final ListView<DesktopRawStore.Item> list = new ListView<>(shown);
    private final TextField search = new TextField();
    private final Label count = new Label();
    private final Label message = new Label();
    private final TickList<DesktopRawStore.Item> ticks;
    private final UndoBar undo = new UndoBar();
    private final Button rename = new Button("Đổi tên");
    private final Button replace = new Button("Thay tệp…");
    private final Button delete = new Button("Xóa (0)");
    private final Button makeSet = new Button("Tạo tập (0)");
    private final Button export = new Button("Xuất (0)…");
    private final HBox bar = new HBox(8, delete, export, makeSet);
    private final Node root;

    RawPage(DesktopApp app, DesktopRuntime runtime, Node toolbar) {
        this.app = app;
        this.runtime = runtime;
        this.ticks = new TickList<>(list, i -> i.id, this::rowText, this::updateButtons);
        this.root = build(toolbar);
        app.registerSearch(NavModel.RAW, search);
        reload();
    }

    Node node() { return root; }

    private Node build(Node toolbar) {
        search.setPromptText("Tìm theo tên…");
        search.textProperty().addListener((o, a, b) -> applyFilter());
        list.setPlaceholder(new Label("Chưa có RAW. Bấm \"Thêm RAW\" hoặc kéo tệp .txt vào đây."));
        VBox.setVgrow(list, Priority.ALWAYS);
        list.setOnDragOver(this::dragOver);
        list.setOnDragDropped(this::dropped);
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateButtons());

        Button add = new Button("Thêm RAW (TXT)…");
        for (Button b : new Button[]{add, rename, replace, delete, makeSet, export}) b.getStyleClass().add("secondary");
        add.setOnAction(e -> chooseAndAdd());
        rename.setOnAction(e -> renameFocused());
        replace.setOnAction(e -> replaceFocused());
        delete.setOnAction(e -> deleteTicked());
        makeSet.setOnAction(e -> app.createSetFromRaws(ticks.ids()));
        export.setOnAction(e -> exportTicked());
        message.setWrapText(true);
        count.getStyleClass().add("muted");
        bar.setAlignment(Pos.CENTER_LEFT);

        FlowPane single = new FlowPane(8, 8, add, rename, replace);
        VBox card = DesktopApp.card(search, single, ticks.header(), count, list, bar, new HBox(10, message, undo.node()));
        VBox.setVgrow(card, Priority.ALWAYS);
        updateButtons();
        return DesktopApp.page(NavModel.title(NavModel.RAW), toolbar, card);
    }

    /** One RAW row: chapter, name, size and the encoding it was read with. */
    private String rowText(DesktopRawStore.Item item) {
        String enc = item.encoding == null || item.encoding.isEmpty() ? "" : "  ·  " + item.encoding;
        return item.chapterLabel() + "  ·  " + item.name + "  ·  " + item.chars + " ký tự" + enc;
    }

    void reload() {
        all.setAll(runtime.raws().list());
        ticks.retainOnly(all);
        applyFilter();
        updateButtons();
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        shown.setPredicate(i -> q.isEmpty() || i.name.toLowerCase(Locale.ROOT).contains(q) || i.originalName.toLowerCase(Locale.ROOT).contains(q));
        count.setText(all.size() + " RAW" + (q.isEmpty() ? "" : " · đang hiện " + shown.size()));
        ticks.changed();
    }

    private void updateButtons() {
        int n = ticks.count();
        boolean focused = list.getSelectionModel().getSelectedItem() != null;
        rename.setDisable(!focused);
        replace.setDisable(!focused);
        bar.setVisible(n > 0);
        bar.setManaged(n > 0);
        delete.setText("Xóa (" + n + ")");
        makeSet.setText("Tạo tập (" + n + ")");
        export.setText("Xuất (" + n + ")…");
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

    // ---- one RAW (the focused row) ----

    private void renameFocused() {
        DesktopRawStore.Item item = list.getSelectionModel().getSelectedItem();
        if (item == null) return;
        TextInputDialog d = new TextInputDialog(item.name);
        d.setTitle("Đổi tên RAW");
        d.setHeaderText("Tên mới cho \"" + item.name + "\"");
        d.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Optional<String> r = app.showDialog(d);
        if (!r.isPresent()) return;
        if (!runtime.raws().rename(item.id, r.get())) { say("Tên không hợp lệ", true); return; }
        say("Đã đổi tên.", false);
        app.libraryChanged();
    }

    private void replaceFocused() {
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

    // ---- bulk delete with undo ----

    /** QA: ticks every visible RAW and asks for the delete, the same path as the button. */
    void qaDelete() {
        for (DesktopRawStore.Item i : shown) if (!ticks.isTicked(i)) ticks.tick(i.id);
        deleteTicked();
    }

    /** QA: ticks the first n visible RAW rows. */
    void qaTickFirst(int n) { ticks.tickFirst(n); }

    /** QA: unticks everything. */
    void qaClearTicks() { ticks.clear(); }

    private void deleteTicked() {
        List<String> ids = ticks.ids();
        if (ids.isEmpty()) return;
        DesktopLibraryBulk.Plan plan = runtime.rawBulk().plan(ids);
        if (plan.deletableCount() == 0) { say(plan.firstMessage(), true); return; }
        if (!app.confirm("Xóa " + plan.deletableCount() + " RAW?", plan.firstMessage())) return;
        if (plan.needsSecondConfirm() && !app.confirmWithLabel("Xác nhận lần hai", plan.secondMessage(), "Vẫn xóa")) return;
        undo.settle();
        DesktopRawBulk.Pending pending = runtime.rawBulk().delete(plan);
        ticks.clear();
        int n = pending.count();
        say("Đã xóa " + n + " RAW" + (plan.blockedCount() > 0 ? " · bỏ qua " + plan.blockedCount() + " (còn job dịch dở)" : ""), false);
        undo.show("Đã xóa " + n + " RAW", () -> {
            int back = runtime.rawBulk().undo(pending);
            say("Đã khôi phục " + back + " RAW.", false);
            app.libraryChanged();
        }, () -> runtime.rawBulk().expire(pending));
        app.libraryChanged();
    }

    /** Ends an open undo window: the files of the removed RAW go for good. */
    void settle() {
        undo.settle();
    }

    // ---- export ----

    private void exportTicked() {
        List<String> ids = ticks.ids();
        if (ids.isEmpty()) return;
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Xuất " + ids.size() + " RAW ra thư mục");
        File dir = dc.showDialog(app.stage());
        if (dir == null) return;
        UiTasks.run(app, "Đang xuất RAW…", () -> runtime.rawBulk().export(ids, dir.toPath()),
                n -> say("Đã xuất " + n + " tệp .txt vào " + dir.getName() + " (tệp trùng tên được thêm số, không ghi đè)", false),
                e -> say("Không xuất được: " + e.getMessage(), true));
    }
}
