package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.DesktopLibraryBulk;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

/**
 * The "Hoàn tác" bar after a bulk delete. The removal is already done; the window is {@link DesktopLibraryBulk#UNDO_MS} long. When it
 * ends, or when another delete starts, the bar expires the previous removal for good (its files, for RAW).
 */
final class UndoBar {
    private final Label message = new Label();
    private final Button undo = new Button("Hoàn tác");
    private final HBox node = new HBox(10, message, undo);
    private final PauseTransition timer = new PauseTransition(Duration.millis(DesktopLibraryBulk.UNDO_MS));
    private Runnable onUndo;
    private Runnable onExpire;

    UndoBar() {
        undo.getStyleClass().add("primary");
        message.setWrapText(true);
        HBox.setHgrow(message, Priority.ALWAYS);
        node.setVisible(false);
        node.setManaged(false);
        timer.setOnFinished(e -> settle());
        undo.setOnAction(e -> undoNow());
    }

    Node node() { return node; }

    boolean open() { return onUndo != null; }

    /** Shows the bar. An earlier window that is still open is settled first. */
    void show(String text, Runnable undoAction, Runnable expireAction) {
        settle();
        onUndo = undoAction;
        onExpire = expireAction;
        message.setText(text + " · còn 10 giây để Hoàn tác");
        message.getStyleClass().removeAll("err", "ok");
        message.getStyleClass().add("ok");
        node.setVisible(true);
        node.setManaged(true);
        timer.playFromStart();
    }

    /** Ends the open window now: the removal becomes final. */
    void settle() {
        timer.stop();
        Runnable expire = onExpire;
        onUndo = null;
        onExpire = null;
        node.setVisible(false);
        node.setManaged(false);
        message.setText("");
        if (expire != null) expire.run();
    }

    /** Puts the removed items back by running the undo action; the window is closed either way. */
    void undoNow() {
        timer.stop();
        Runnable back = onUndo;
        onUndo = null;
        onExpire = null;
        node.setVisible(false);
        node.setManaged(false);
        message.setText("");
        if (back != null) back.run();
    }
}
