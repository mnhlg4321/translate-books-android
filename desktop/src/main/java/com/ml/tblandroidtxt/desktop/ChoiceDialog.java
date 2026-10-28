package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.DesktopSetEditor;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Pick one or many library items for a set slot or a set row. Empty selection means "Không dùng". */
final class ChoiceDialog {
    private ChoiceDialog() { }

    /** @return the chosen ids (possibly empty), or empty when the person cancelled */
    static Optional<List<String>> pick(DesktopApp app, String title, String hint, List<DesktopSetEditor.Choice> choices, Set<String> selected,
                                       boolean multi, String emptyMeansLabel) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(title);
        d.setHeaderText(title);
        ListView<DesktopSetEditor.Choice> list = new ListView<>();
        list.getItems().setAll(choices);
        list.getSelectionModel().setSelectionMode(multi ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
        list.setCellFactory(l -> new ListCell<DesktopSetEditor.Choice>() {
            @Override protected void updateItem(DesktopSetEditor.Choice c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.label + (c.meta.isEmpty() ? "" : "  ·  " + c.meta));
            }
        });
        for (int i = 0; i < choices.size(); i++) if (selected.contains(choices.get(i).id)) list.getSelectionModel().select(i);
        list.setPrefSize(560, 300);
        Label note = new Label((hint == null || hint.isEmpty() ? "" : hint + "\n") + (multi ? "Ctrl/Shift để chọn nhiều. " : "")
                + "Bỏ chọn hết = " + emptyMeansLabel + ".");
        note.setWrapText(true);
        note.setMinHeight(Region.USE_PREF_SIZE);
        d.getDialogPane().setContent(new VBox(8, note, list));
        ButtonType ok = new ButtonType("Xong", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        Optional<ButtonType> r = app.showDialog(d);
        if (!r.isPresent() || r.get() != ok) return Optional.empty();
        List<String> ids = new ArrayList<>();
        for (DesktopSetEditor.Choice c : list.getSelectionModel().getSelectedItems()) ids.add(c.id);
        return Optional.of(ids);
    }
}
