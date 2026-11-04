package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.DesktopLibraryBulk;

import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A Library list with a tick box in front of every row, the same as the phone's selection mode. The ticks are the source of truth for
 * bulk actions; the list's own highlight stays for the single-item buttons (Mở, Đổi tên…).
 */
final class TickList<T> {
    private final ListView<T> list;
    private final Function<T, String> idOf;
    private final Function<T, String> text;
    private final Runnable changed;
    private final Function<T, String> status;
    private final DesktopLibraryBulk.Selection selection = new DesktopLibraryBulk.Selection();
    private final CheckBox all = new CheckBox("Chọn tất cả");
    private final Label headline = new Label();
    private final Button cancel = new Button("Hủy");
    private final HBox header = new HBox(12);

    TickList(ListView<T> list, Function<T, String> idOf, Function<T, String> text, Runnable changed) {
        this(list, idOf, text, changed, null);
    }

    /** {@code status} gives the colour class of a row ("ok", "warn" or "err"); null for no colour. The tick box is kept in every case. */
    TickList(ListView<T> list, Function<T, String> idOf, Function<T, String> text, Runnable changed, Function<T, String> status) {
        this.list = list;
        this.idOf = idOf;
        this.text = text;
        this.changed = changed;
        this.status = status;
        cancel.getStyleClass().add("secondary");
        all.setOnAction(e -> {
            selection.selectAll(visibleIds());
            changed();
        });
        cancel.setOnAction(e -> {
            selection.clear();
            changed();
        });
        header.getChildren().addAll(all, headline, cancel);
        list.setCellFactory(cellFactory());
    }

    /** "Chọn tất cả", "n đã chọn" and "Hủy" in one row above the list. */
    HBox header() { return header; }

    boolean any() { return selection.count() > 0; }

    int count() { return selection.count(); }

    List<String> ids() { return selection.ids(); }

    boolean isTicked(T item) { return selection.isSelected(idOf.apply(item)); }

    /** Drops ticks of rows that were deleted; call after every reload with the complete source list. */
    void retainOnly(Collection<T> source) {
        selection.retainOnly(source.stream().map(idOf).collect(Collectors.toList()));
        refreshHeader();
    }

    void clear() {
        selection.clear();
        refreshHeader();
    }

    /** Ticks the first {@code n} rows shown; for the Screenshot mode. */
    void tickFirst(int n) {
        int i = 0;
        for (T item : list.getItems()) {
            if (i++ >= n) break;
            if (!selection.isSelected(idOf.apply(item))) selection.toggle(idOf.apply(item));
        }
        changed();
    }

    /** Ticks by id, for the Screenshot mode and tests; ignored for ids that are not shown. */
    void tick(String id) {
        if (!selection.isSelected(id)) selection.toggle(id);
        changed();
    }

    /** Refreshes the header and the rows after a tick changed. */
    void changed() {
        refreshHeader();
        list.refresh();
        changed.run();
    }

    private List<String> visibleIds() {
        ObservableList<T> items = list.getItems();
        return items.stream().map(idOf).collect(Collectors.toList());
    }

    private void refreshHeader() {
        headline.setText(selection.count() == 0 ? "" : selection.headline());
        List<String> visible = visibleIds();
        all.setSelected(!visible.isEmpty() && selection.ids().containsAll(visible));
        all.setDisable(visible.isEmpty());
        cancel.setDisable(selection.count() == 0);
    }

    private Callback<ListView<T>, javafx.scene.control.ListCell<T>> cellFactory() {
        return view -> new ListCell<T>() {
            private final CheckBox box = new CheckBox();
            private final HBox row = new HBox(8, box, new Label());

            {
                HBox.setHgrow(row, Priority.ALWAYS);
                box.setOnAction(e -> {
                    T item = getItem();
                    if (item != null) {
                        selection.toggle(idOf.apply(item));
                        changed();
                    }
                });
            }

            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                box.setSelected(selection.isSelected(idOf.apply(item)));
                Label label = (Label) row.getChildren().get(1);
                label.setText(text.apply(item));
                label.getStyleClass().removeAll("ok", "warn", "err");
                if (status != null) label.getStyleClass().add(status.apply(item));
                setText(null);
                setGraphic(row);
            }
        };
    }
}
