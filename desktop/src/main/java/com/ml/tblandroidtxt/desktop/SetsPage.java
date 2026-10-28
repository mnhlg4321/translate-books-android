package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.DesktopSetEditor;
import com.ml.tblandroidtxt.VolumePlan;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Thư viện · Tập: saved sets (shared with "Dịch cả tập") and the set editor with chapter matching. */
final class SetsPage {
    private static final String NOT_IN_ZIP = "Tập chỉ nằm trên máy này: gói thư viện (zip) không chứa Tập, nên nhập/xuất gói không mang Tập theo.";

    private final DesktopApp app;
    private final DesktopRuntime runtime;
    private final StackPane root = new StackPane();
    private final ListView<DesktopRuntime.SetCard> list = new ListView<>();
    private final Label listNote = new Label();
    private final Button open = new Button("Mở / Sửa");
    private final Button rename = new Button("Đổi tên");
    private final Button delete = new Button("Xóa");

    private final VBox editorPage;
    private final TextField nameField = new TextField();
    private final Label rawSlot = new Label();
    private final Label glossarySlot = new Label();
    private final Label pronounSlot = new Label();
    private final Label summary = new Label();
    private final VBox unusedBox = new VBox(4);
    private final TableView<VolumePlan.RowView> table = new TableView<>();
    private final Button saveButton = new Button("Lưu tập");
    private final Label editorNote = new Label();
    private DesktopSetEditor editor;
    private boolean loading;

    SetsPage(DesktopApp app, DesktopRuntime runtime, Node toolbar) {
        this.app = app;
        this.runtime = runtime;
        VBox listPage = buildList(toolbar);
        editorPage = buildEditor();
        editorPage.setVisible(false);
        root.getChildren().addAll(listPage, editorPage);
        app.registerSearch(NavModel.SETS, new TextField());
        app.registerLeaveGuard(NavModel.SETS, this::confirmLeave);
        reload();
    }

    Node node() { return root; }

    // ---- list ----

    private VBox buildList(Node toolbar) {
        Label desc = new Label("Một tập gồm một hoặc nhiều chương; mỗi chương là một bộ RAW – Glossary – Pronoun. Ở \"Dịch cả tập\" chọn một tập đã lưu để dịch nhiều chương, mỗi chương dùng đúng Glossary/Pronoun của nó.\n" + NOT_IN_ZIP);
        desc.setWrapText(true);
        desc.getStyleClass().add("muted");
        list.setPlaceholder(new Label("Chưa có tập. Nhập RAW trước, rồi bấm \"+ Tạo tập mới\"."));
        list.setCellFactory(l -> new ListCell<DesktopRuntime.SetCard>() {
            @Override protected void updateItem(DesktopRuntime.SetCard c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.name + "  ·  " + c.summary);
                getStyleClass().removeAll("err", "warn");
                if (c != null && !empty) getStyleClass().add(c.red > 0 ? "err" : c.yellow > 0 ? "warn" : "ok");
            }
        });
        list.setOnMouseClicked(e -> { if (e.getClickCount() == 2) openSelected(); });
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateListButtons());
        VBox.setVgrow(list, Priority.ALWAYS);
        Button create = new Button("+ Tạo tập mới");
        create.getStyleClass().add("primary");
        for (Button b : new Button[]{open, rename, delete}) b.getStyleClass().add("secondary");
        create.setOnAction(e -> startNew(new ArrayList<>()));
        open.setOnAction(e -> openSelected());
        rename.setOnAction(e -> renameSelected());
        delete.setOnAction(e -> deleteSelected());
        listNote.setWrapText(true);
        VBox card = DesktopApp.card(desc, new FlowPane(8, 8, create, open, rename, delete), list, listNote);
        VBox.setVgrow(card, Priority.ALWAYS);
        updateListButtons();
        return DesktopApp.page(NavModel.title(NavModel.SETS), toolbar, card);
    }

    void reload() {
        String keep = list.getSelectionModel().getSelectedItem() == null ? "" : list.getSelectionModel().getSelectedItem().id;
        list.getItems().setAll(runtime.setCards());
        for (int i = 0; i < list.getItems().size(); i++) if (list.getItems().get(i).id.equals(keep)) list.getSelectionModel().select(i);
        updateListButtons();
    }

    private void updateListButtons() {
        boolean none = list.getSelectionModel().getSelectedItem() == null;
        open.setDisable(none);
        rename.setDisable(none);
        delete.setDisable(none);
    }

    private void say(Label target, String text, boolean error) {
        target.setText(text);
        target.getStyleClass().removeAll("err", "ok");
        target.getStyleClass().add(error ? "err" : "ok");
    }

    private void openSelected() {
        DesktopRuntime.SetCard c = list.getSelectionModel().getSelectedItem();
        if (c == null) return;
        DesktopSetEditor e = runtime.openSet(c.id);
        if (e == null) { say(listNote, "Tập này không còn nữa.", true); reload(); return; }
        showEditor(e);
    }

    private void renameSelected() {
        DesktopRuntime.SetCard c = list.getSelectionModel().getSelectedItem();
        if (c == null) return;
        TextInputDialog d = new TextInputDialog(c.name);
        d.setTitle("Đổi tên tập");
        d.setHeaderText("Tên mới cho \"" + c.name + "\"");
        Optional<String> r = app.showDialog(d);
        if (!r.isPresent()) return;
        if (!runtime.renameSet(c.id, r.get())) { say(listNote, "Tên không hợp lệ", true); return; }
        say(listNote, "Đã đổi tên.", false);
        app.libraryChanged();
    }

    private void deleteSelected() {
        DesktopRuntime.SetCard c = list.getSelectionModel().getSelectedItem();
        if (c == null) return;
        List<String> ids = new ArrayList<>();
        ids.add(c.id);
        int rawOnly = runtime.rawOnlyInSets(ids).size();
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Xóa tập \"" + c.name + "\" (" + c.chapters + " chương). RAW, Glossary và Pronoun của các chương vẫn còn trong thư viện.", javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        a.setTitle("Xác nhận");
        a.setHeaderText("Xóa tập?");
        a.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        CheckBox alsoRaw = new CheckBox("Xóa cả " + rawOnly + " RAW chỉ thuộc tập này");
        alsoRaw.setDisable(rawOnly == 0);
        VBox content = new VBox(10, new Label(a.getContentText()), alsoRaw);
        ((Label) content.getChildren().get(0)).setWrapText(true);
        a.getDialogPane().setContent(content);
        Optional<javafx.scene.control.ButtonType> r = app.showDialog(a);
        if (!r.isPresent() || r.get() != javafx.scene.control.ButtonType.OK) return;
        if (alsoRaw.isSelected() && !app.confirm("Xóa cả RAW?", rawOnly + " RAW chỉ thuộc tập này sẽ bị xóa khỏi thư viện (RAW đang cần cho job dịch dở được giữ lại).")) return;
        int removed = runtime.deleteSets(ids, alsoRaw.isSelected());
        say(listNote, "Đã xóa tập \"" + c.name + "\"" + (removed > 0 ? " và " + removed + " RAW" : "") + ".", false);
        app.libraryChanged();
    }

    /** "Tạo tập mới" and the RAW page shortcut: opens the editor with these chapters in the RAW slot. */
    void startNew(List<String> rawIds) {
        if (!confirmLeave()) return;
        showEditor(runtime.newSet(rawIds));
    }

    // ---- editor ----

    private VBox buildEditor() {
        Label desc = new Label("Chọn tệp ở ba ô; app gợi ý ghép theo số chương ở đầu tên, chỉ trong các tệp bạn đã chọn. Bạn xem từng dòng và đổi chỗ sai.\n" + NOT_IN_ZIP);
        desc.setWrapText(true);
        desc.getStyleClass().add("muted");
        nameField.setPromptText("Tên tập");
        nameField.textProperty().addListener((o, a, b) -> { if (!loading && editor != null) editor.setName(b); });
        Button pickRaw = new Button("Chọn / Đổi…");
        Button pickGlossary = new Button("Chọn / Đổi…");
        Button pickPronoun = new Button("Chọn / Đổi…");
        for (Button b : new Button[]{pickRaw, pickGlossary, pickPronoun}) b.getStyleClass().add("secondary");
        pickRaw.setOnAction(e -> pickSlot(0));
        pickGlossary.setOnAction(e -> pickSlot(1));
        pickPronoun.setOnAction(e -> pickSlot(2));
        HBox raw = slotRow("RAW", rawSlot, pickRaw);
        HBox glossary = slotRow("Glossary", glossarySlot, pickGlossary);
        HBox pronoun = slotRow("Pronoun", pronounSlot, pickPronoun);
        summary.setWrapText(true);
        summary.getStyleClass().add("muted");

        buildTable();
        table.setPlaceholder(new Label("Chưa có RAW nào: chọn RAW ở ô trên."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        Button cancel = new Button("Hủy");
        cancel.getStyleClass().add("secondary");
        saveButton.getStyleClass().add("primary");
        cancel.setOnAction(e -> { if (confirmLeave()) closeEditor(); });
        saveButton.setOnAction(e -> save());
        editorNote.setWrapText(true);
        VBox card = DesktopApp.card(desc, nameField, raw, glossary, pronoun, unusedBox, summary, table, new HBox(10, cancel, saveButton, editorNote));
        VBox.setVgrow(card, Priority.ALWAYS);
        return DesktopApp.page(NavModel.title(NavModel.SETS), card);
    }

    private HBox slotRow(String label, Label value, Button pick) {
        Label l = new Label(label);
        l.setMinWidth(80);
        l.getStyleClass().add("muted");
        value.setWrapText(true);
        HBox.setHgrow(value, Priority.ALWAYS);
        value.setMaxWidth(Double.MAX_VALUE);
        HBox h = new HBox(10, l, value, pick);
        h.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return h;
    }

    private void buildTable() {
        TableColumn<VolumePlan.RowView, String> chapter = new TableColumn<>("Chương");
        chapter.setPrefWidth(80);
        chapter.setEditable(true);
        chapter.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().chapterNo >= 0 ? String.valueOf(cd.getValue().chapterNo) : ""));
        chapter.setCellFactory(TextFieldTableCell.forTableColumn());
        chapter.setOnEditCommit(ev -> { if (editor != null) { editor.setChapterText(ev.getRowValue().index, ev.getNewValue()); Platform.runLater(this::refresh); } });
        TableColumn<VolumePlan.RowView, String> raw = new TableColumn<>("RAW");
        raw.setPrefWidth(200);
        raw.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().rawName));
        TableColumn<VolumePlan.RowView, VolumePlan.RowView> gl = profileColumn("Glossary", true);
        TableColumn<VolumePlan.RowView, VolumePlan.RowView> pr = profileColumn("Pronoun", false);
        TableColumn<VolumePlan.RowView, String> status = new TableColumn<>("Trạng thái");
        status.setPrefWidth(220);
        status.setCellValueFactory(cd -> new SimpleStringProperty(statusText(cd.getValue())));
        TableColumn<VolumePlan.RowView, VolumePlan.RowView> skip = new TableColumn<>("Bỏ khỏi tập");
        skip.setPrefWidth(100);
        skip.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue()));
        skip.setCellFactory(c -> new TableCell<VolumePlan.RowView, VolumePlan.RowView>() {
            @Override protected void updateItem(VolumePlan.RowView row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setGraphic(null); return; }
                CheckBox box = new CheckBox();
                box.setSelected(row.excluded);
                box.setOnAction(e -> { if (editor != null) { editor.setExcluded(row.index, box.isSelected()); Platform.runLater(SetsPage.this::refresh); } });
                setGraphic(box);
            }
        });
        table.setEditable(true);
        table.getColumns().add(chapter);
        table.getColumns().add(raw);
        table.getColumns().add(gl);
        table.getColumns().add(pr);
        table.getColumns().add(status);
        table.getColumns().add(skip);
    }

    private TableColumn<VolumePlan.RowView, VolumePlan.RowView> profileColumn(String title, boolean glossary) {
        TableColumn<VolumePlan.RowView, VolumePlan.RowView> col = new TableColumn<>(title);
        col.setPrefWidth(200);
        col.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue()));
        col.setCellFactory(c -> new TableCell<VolumePlan.RowView, VolumePlan.RowView>() {
            @Override protected void updateItem(VolumePlan.RowView row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setGraphic(null); setText(null); return; }
                Button change = new Button("Đổi");
                change.getStyleClass().add("secondary");
                change.setDisable(row.excluded);
                change.setOnAction(e -> chooseForRow(row.index, glossary));
                Label l = new Label(glossary ? row.glossaryLabel : row.pronounLabel);
                HBox box = new HBox(8, l, change);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });
        return col;
    }

    private static String statusText(VolumePlan.RowView r) {
        if (r.excluded) return "— Bỏ khỏi tập";
        String icon = r.status.equals("RED") ? "✕ " : r.status.equals("YELLOW") ? "⚠ " : "✓ ";
        return icon + r.reason;
    }

    private void pickSlot(int slot) {
        if (editor == null) return;
        List<com.ml.tblandroidtxt.DesktopSetEditor.Choice> choices = slot == 0 ? editor.rawChoices() : slot == 1 ? editor.glossaryChoices() : editor.pronounChoices();
        String title = slot == 0 ? "Chọn RAW" : slot == 1 ? "Chọn Glossary" : "Chọn Pronoun";
        String hint = choices.isEmpty() ? "Thư viện chưa có mục nào: nhập ở các trang Thư viện." : slot == 0 ? "" : "Gợi ý: tệp cùng số chương với các RAW đã chọn";
        java.util.Set<String> current = slot == 0 ? editor.chosenRaw() : slot == 1 ? editor.chosenGlossaries() : editor.chosenPronouns();
        Optional<List<String>> r = ChoiceDialog.pick(app, title, hint, choices, current, true, slot == 0 ? "chưa chọn RAW nào" : "Không dùng");
        if (!r.isPresent()) return;
        if (slot == 0) editor.setRawSlot(r.get()); else if (slot == 1) editor.setGlossarySlot(r.get()); else editor.setPronounSlot(r.get());
        refresh();
    }

    private void chooseForRow(int index, boolean glossary) {
        if (editor == null) return;
        VolumePlan.RowView row = editor.rows().get(index);
        java.util.Set<String> current = new java.util.HashSet<>();
        String id = glossary ? row.glossaryId : row.pronounId;
        if (!id.isEmpty()) current.add(id);
        Optional<List<String>> r = ChoiceDialog.pick(app, (glossary ? "Glossary" : "Pronoun") + " cho " + (row.chapterNo >= 0 ? "Chương " + row.chapterNo : row.rawName),
                row.rawName, editor.rowChoices(index, glossary), current, false, "Không dùng");
        if (!r.isPresent()) return;
        String picked = r.get().isEmpty() ? "" : r.get().get(0);
        if (glossary) editor.chooseGlossary(index, picked); else editor.choosePronoun(index, picked);
        refresh();
    }

    private void showEditor(DesktopSetEditor e) {
        editor = e;
        loading = true;
        nameField.setText(e.name());
        loading = false;
        editorNote.setText("");
        root.getChildren().get(0).setVisible(false);
        editorPage.setVisible(true);
        refresh();
    }

    private void closeEditor() {
        editor = null;
        root.getChildren().get(0).setVisible(true);
        editorPage.setVisible(false);
        reload();
    }

    boolean editing() { return editor != null; }

    private void refresh() {
        if (editor == null) return;
        loading = true;
        if (!nameField.getText().equals(editor.name())) nameField.setText(editor.name());
        loading = false;
        rawSlot.setText(slotText(editor.chosenRaw().size(), "RAW", false));
        glossarySlot.setText(slotText(editor.chosenGlossaries().size(), "Glossary", true));
        pronounSlot.setText(slotText(editor.chosenPronouns().size(), "Pronoun", true));
        List<VolumePlan.RowView> rows = editor.rows();
        table.getItems().setAll(rows);
        summary.setText(editor.summary());
        summary.getStyleClass().removeAll("err", "warn", "ok", "muted");
        boolean red = false, yellow = false;
        for (VolumePlan.RowView r : rows) if (!r.excluded) { if (r.status.equals("RED")) red = true; else if (r.status.equals("YELLOW")) yellow = true; }
        summary.getStyleClass().add(rows.isEmpty() ? "muted" : red ? "err" : yellow ? "warn" : "ok");
        unusedBox.getChildren().clear();
        if (!editor.unusedGlossaries().isEmpty()) unusedBox.getChildren().add(unusedLine(editor.unusedGlossaries().size() + " Glossary không khớp chương nào: " + String.join(", ", editor.unusedGlossaries()), true));
        if (!editor.unusedPronouns().isEmpty()) unusedBox.getChildren().add(unusedLine(editor.unusedPronouns().size() + " Pronoun không khớp chương nào: " + String.join(", ", editor.unusedPronouns()), false));
        saveButton.setText("Lưu tập (" + editor.included() + " chương)");
    }

    private static String slotText(int n, String what, boolean optional) {
        return n == 0 ? (optional ? "Không dùng" : "Chưa chọn") : n + " " + (what.equals("RAW") ? "RAW" : what) + " đã chọn";
    }

    private Node unusedLine(String text, boolean glossary) {
        Label l = new Label("⚠ " + text);
        l.setWrapText(true);
        l.getStyleClass().add("warn");
        HBox.setHgrow(l, Priority.ALWAYS);
        Button drop = new Button("Bỏ khỏi ô");
        drop.getStyleClass().add("secondary");
        drop.setOnAction(e -> { editor.dropUnused(glossary); refresh(); });
        HBox h = new HBox(10, l, drop);
        h.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return h;
    }

    private void save() {
        if (editor == null) return;
        String reason = editor.blockReason();
        if (reason != null) { say(editorNote, reason, true); return; }
        String name = editor.name().trim().isEmpty() ? "Tổ hợp" : editor.name().trim();
        int n = editor.included();
        if (editor.save() == null) { say(editorNote, "Không lưu được tập.", true); return; }
        say(listNote, "Đã lưu tập \"" + name + "\" (" + n + " chương). Tập đã hiện trong \"Dịch cả tập\".", false);
        closeEditor();
        app.libraryChanged();
    }

    boolean confirmLeave() {
        if (editor == null || !editor.dirty()) return true;
        int answer = app.askSave("Tập \"" + editor.name() + "\" có thay đổi chưa lưu.");
        if (answer < 0) return false;
        if (answer == 1) {
            String reason = editor.blockReason();
            if (reason != null) { say(editorNote, reason, true); return false; }
            return editor.save() != null;
        }
        return true;
    }
}
