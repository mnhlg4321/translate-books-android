package com.ml.tblandroidtxt;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogStore {
    private static final String FILE_NAME = "tbl_runtime.log";
    private static final long MAX_BYTES = 512_000L;

    public static String formatLine(String msg) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        return "[" + time + "] " + (msg == null ? "" : msg) + "\n";
    }

    /**
     * Lines are written by one background thread, in the order they were appended: a caller on the UI thread (every catalog lookup, every
     * progress broadcast) no longer waits for a file open, a write and, past the size limit, a full read and rewrite of the log.
     * {@link #read}, {@link #clear} and {@link #flush} wait for the lines queued before them.
     */
    private static final java.util.concurrent.ExecutorService WRITER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "log-writer");
        t.setDaemon(true);
        return t;
    });
    private static volatile File logFile;

    private static File file(Context ctx) {
        File f = logFile;
        if (f == null) { f = new File(ctx.getFilesDir(), FILE_NAME); logFile = f; }
        return f;
    }

    public static void append(Context ctx, String msg) {
        if (ctx == null) return;
        try {
            final File f = file(ctx);
            final byte[] line = formatLine(msg).getBytes(StandardCharsets.UTF_8);
            WRITER.execute(() -> {
                try {
                    try (FileOutputStream out = new FileOutputStream(f, true)) { out.write(line); }
                    trimIfNeeded(f);
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    /** Waits (at most two seconds) until every line appended so far is on disk. */
    public static void flush() {
        try { WRITER.submit(() -> { }).get(2, java.util.concurrent.TimeUnit.SECONDS); } catch (Exception ignored) {}
    }

    public static String read(Context ctx) {
        if (ctx == null) return "";
        flush();
        try {
            File f = new File(ctx.getFilesDir(), FILE_NAME);
            if (!f.exists()) return "";
            long len = f.length();
            int size = (int)Math.min(len, MAX_BYTES);
            byte[] data = new byte[size];
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                long skip = Math.max(0, len - size);
                while (skip > 0) {
                    long skipped = in.skip(skip);
                    if (skipped <= 0) break;
                    skip -= skipped;
                }
                int n = in.read(data);
                return n <= 0 ? "" : new String(data, 0, n, StandardCharsets.UTF_8);
            }
        } catch (Exception e) { return ""; }
    }

    public static String exportBundle(Context ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("TBL Android TXT Debug Log Export\n");
        sb.append("Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
        sb.append("App version: ").append(AppBuildInfo.exportVersionLine()).append("\n\n");
        sb.append("===== LAST RUNTIME STATE =====\n");
        sb.append(RuntimeStateStore.describe(ctx)).append("\n\n");
        sb.append("===== RUNTIME LOG =====\n");
        String runtime = read(ctx);
        sb.append(runtime == null || runtime.trim().isEmpty() ? "No runtime log yet.\n" : runtime);
        sb.append("\n===== API DEBUG TRACE =====\n");
        String trace = DebugTraceStore.read(ctx);
        sb.append(trace == null || trace.trim().isEmpty() ? "No API debug trace yet.\n" : trace);
        return sb.toString();
    }

    public static void clear(Context ctx) {
        if (ctx == null) return;
        flush();
        try {
            File f = new File(ctx.getFilesDir(), FILE_NAME);
            if (f.exists()) f.delete();
        } catch (Exception ignored) {}
    }

    private static void trimIfNeeded(File f) {
        try {
            if (f == null || !f.exists() || f.length() <= MAX_BYTES) return;
            byte[] data = new byte[(int)MAX_BYTES];
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                long skip = f.length() - MAX_BYTES;
                while (skip > 0) {
                    long skipped = in.skip(skip);
                    if (skipped <= 0) break;
                    skip -= skipped;
                }
                int n = in.read(data);
                try (FileOutputStream out = new FileOutputStream(f, false)) {
                    if (n > 0) out.write(data, 0, n);
                }
            }
        } catch (Exception ignored) {}
    }
}
