package com.ml.tblandroidtxt;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DebugTraceStore {
    private static final String FILE_NAME = "tbl_debug_trace.log";
    private static final long MAX_BYTES = 700_000L;

    public static void request(Context ctx, String phase, int chunkIndexOneBased, PromptPair prompt, AppSettings s) {
        if (ctx == null || prompt == null) return;
        String header = "API REQUEST • " + safe(phase) + " • chunk " + chunkIndexOneBased
                + " • provider=" + safe(s == null ? "" : s.provider)
                + " • model=" + safe(s == null ? "" : s.model)
                + " • baseUrl=" + safe(s == null ? "" : s.baseUrl);
        String body = "systemChars=" + prompt.system.length()
                + " systemTokensApprox=" + Chunker.approxTokens(prompt.system)
                + " systemHash=" + HashUtil.sha256(prompt.system)
                + "\nuserChars=" + prompt.user.length()
                + " userTokensApprox=" + Chunker.approxTokens(prompt.user)
                + " userHash=" + HashUtil.sha256(prompt.user)
                + "\nContent is intentionally omitted for privacy.";
        appendBlock(ctx, header, body);
    }

    public static void response(Context ctx, String phase, int chunkIndexOneBased, OpenAICompatibleClient.ChatResult r, String extracted) {
        if (ctx == null) return;
        String header = "API RESPONSE • " + safe(phase) + " • chunk " + chunkIndexOneBased
                + " • tokens=" + (r == null ? 0 : r.totalTokens)
                + " • extractedChars=" + (extracted == null ? 0 : extracted.length());
        String raw = r == null ? "" : r.content;
        String body = "rawChars=" + raw.length() + " rawHash=" + HashUtil.sha256(raw)
                + "\nextractedChars=" + (extracted == null ? 0 : extracted.length())
                + " extractedHash=" + HashUtil.sha256(extracted == null ? "" : extracted)
                + "\nContent is intentionally omitted for privacy.";
        appendBlock(ctx, header, body);
    }

    public static void error(Context ctx, String phase, int chunkIndexOneBased, Throwable t) {
        if (ctx == null) return;
        appendBlock(ctx, "API ERROR • " + safe(phase) + " • chunk " + chunkIndexOneBased,
                ApiErrorParser.describe(t));
    }

    public static String read(Context ctx) {
        if (ctx == null) return "";
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

    public static void clear(Context ctx) {
        if (ctx == null) return;
        try {
            File f = new File(ctx.getFilesDir(), FILE_NAME);
            if (f.exists()) f.delete();
        } catch (Exception ignored) {}
    }

    private static void appendBlock(Context ctx, String title, String body) {
        try {
            File f = new File(ctx.getFilesDir(), FILE_NAME);
            try (FileOutputStream out = new FileOutputStream(f, true)) {
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                String block = "\n===== [" + time + "] " + safe(title) + " =====\n" + safe(body) + "\n";
                out.write(block.getBytes(StandardCharsets.UTF_8));
            }
            trimIfNeeded(f);
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

    private static String safe(String v) { return v == null ? "" : v; }
}
