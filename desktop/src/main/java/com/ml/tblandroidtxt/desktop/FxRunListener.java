package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.RunSnapshot;

import javafx.application.Platform;

/** Moves every run event onto the JavaFX thread. A finished or cancelled run re-enables the buttons; it never closes the window. */
final class FxRunListener implements DesktopRuntime.UiEvents {
    private final DesktopApp app;

    FxRunListener(DesktopApp app) { this.app = app; }

    @Override public void onSnapshot(RunSnapshot snapshot) { Platform.runLater(() -> app.applySnapshot(snapshot)); }

    @Override public void onNotification(String text, int done, int total) { }

    @Override public void onRunEnded() { Platform.runLater(app::runEnded); }

    @Override public void onCancelledEnd(String message) { Platform.runLater(app::runEnded); }
}
