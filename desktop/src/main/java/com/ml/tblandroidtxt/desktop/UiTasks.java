package com.ml.tblandroidtxt.desktop;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Long work (reading many files, scanning a folder) runs off the window thread; the status bar shows that something is happening. */
final class UiTasks {
    private UiTasks() { }

    static <T> void run(DesktopApp app, String label, Callable<T> work, Consumer<T> done, Consumer<Exception> failed) {
        app.busy(label);
        Thread t = new Thread(() -> {
            T result = null;
            Exception error = null;
            try {
                result = work.call();
            } catch (Exception e) {
                error = e;
            }
            final T r = result;
            final Exception err = error;
            Platform.runLater(() -> {
                app.idle();
                if (err != null) failed.accept(err); else done.accept(r);
            });
        }, "tbl-task");
        t.setDaemon(true);
        t.start();
    }
}
