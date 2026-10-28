package com.ml.tblandroidtxt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** logs/tbl_runtime.log; also the sink for ObservabilityLog events. Lines never carry the API key or book text (callers log counts). */
public final class FileRuntimeLog implements RuntimeLog, ObservabilityLog.Sink {
    private final Path file;

    public FileRuntimeLog(Path logsDir) {
        this.file = logsDir.resolve("tbl_runtime.log");
    }

    public Path file() { return file; }

    /** The whole log text ("" when there is none yet). */
    public synchronized String readAll() {
        try { return Files.exists(file) ? new String(Files.readAllBytes(file), StandardCharsets.UTF_8) : ""; } catch (IOException e) { return ""; }
    }

    public synchronized boolean clear() {
        try { Files.deleteIfExists(file); return true; } catch (IOException e) { return false; }
    }

    @Override public synchronized void append(String message) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.write(file, LogFormat.formatLine(message).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // logging must never stop a run
        }
    }

    @Override public void error(String tag, String message, Throwable error) {
        append("E " + tag + " " + message + " " + (error == null ? "" : error.getClass().getName()));
    }

    @Override public String stackTrace(Throwable error) {
        if (error == null) return "";
        java.io.StringWriter w = new java.io.StringWriter();
        error.printStackTrace(new java.io.PrintWriter(w));
        return w.toString();
    }
}
