package com.ml.tblandroidtxt.desktop;

/** Plain main class (does not extend Application) so JavaFX can run from the classpath. */
public final class DesktopLauncher {
    private DesktopLauncher() {}

    public static void main(String[] args) {
        javafx.application.Application.launch(DesktopApp.class, args);
    }
}
