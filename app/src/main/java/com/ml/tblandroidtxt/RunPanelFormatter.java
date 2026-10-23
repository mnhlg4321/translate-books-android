package com.ml.tblandroidtxt;

import java.util.Locale;

/** Pure text and numbers of the one-screen progress panel (a run of one file or of a whole chapter set). */
public final class RunPanelFormatter {
    private RunPanelFormatter() {}

    /** Vietnamese name of a run state for the panel title. */
    public static String stateLabel(String state) {
        String s = state == null ? "" : state.trim().toUpperCase(Locale.ROOT);
        switch (s) {
            case "PAUSED": return "T\u1ea1m d\u1eebng";
            case "COMPLETED": return "Xong";
            case "FAILED": return "L\u1ed7i";
            case "CANCELLED": return "\u0110\u00e3 h\u1ee7y";
            case "IDLE": return "\u0110\u00e3 d\u1eebng";
            case "VALIDATING": case "READY": return "\u0110ang chu\u1ea9n b\u1ecb";
            default: return "\u0110ang d\u1ecbch";
        }
    }

    /** "Đang dịch · Tập ATE_JP_VOL6" for a chapter-set run, "Đang dịch · 004.txt" for one file. */
    public static String title(String state, boolean set, String setName, String file) {
        String what;
        if (set) what = "T\u1eadp" + (setName == null || setName.trim().isEmpty() ? "" : " " + setName.trim());
        else what = file == null || file.trim().isEmpty() ? "" : file.trim();
        return stateLabel(state) + (what.isEmpty() ? "" : " \u00b7 " + what);
    }

    /** What the status chip at the top shows: the state of the run in progress when there is one, else the last saved state. */
    public static String chipState(boolean serviceActive, String tracked, String durable) {
        String t = tracked == null ? "" : tracked.trim().toUpperCase(Locale.ROOT);
        String d = durable == null ? "" : durable.trim().toUpperCase(Locale.ROOT);
        if (serviceActive) return t.equals("PAUSED") || t.equals("VALIDATING") || t.equals("READY") ? t : "RUNNING";
        return d.isEmpty() ? "IDLE" : d;
    }

    /** "Chương 4/14 · 3 xong · 0 lỗi". {@code index} is the 1-based chapter being worked on. */
    public static String setLine(int index, int total, int done, int failed, int chapterNo) {
        String which = chapterNo >= 0 ? "Chương " + chapterNo + " (" + Math.min(Math.max(index, 0), total) + "/" + total + ")" : "Chương " + Math.min(Math.max(index, 0), total) + "/" + total;
        return which + " · " + done + " xong · " + failed + " lỗi";
    }

    /** "chunk 5/6 · 004_ATE_JP_VOL6.txt". */
    public static String chapterLine(int chunk, int totalChunks, String file) {
        return totalChunks <= 0 ? "Chưa có chunk" : "Chunk " + Math.min(Math.max(chunk, 1), totalChunks) + "/" + totalChunks;
    }

    /** Share of the whole run done, in thousandths: finished (or failed) chapters plus the finished part of the current one. */
    public static int setPermille(int total, int done, int failed, int chunksDone, int chunksTotal) {
        if (total <= 0) return 0;
        double current = chunksTotal <= 0 ? 0 : Math.min(1d, Math.max(0d, chunksDone / (double) chunksTotal));
        int finished = Math.max(0, done) + Math.max(0, failed);
        double value = (Math.min(finished, total) + (finished >= total ? 0 : current)) / total;
        return (int) Math.round(Math.min(1d, Math.max(0d, value)) * 1000);
    }

    public static int chunkPermille(int chunksDone, int chunksTotal) {
        if (chunksTotal <= 0) return 0;
        return (int) Math.round(Math.min(1d, Math.max(0d, chunksDone / (double) chunksTotal)) * 1000);
    }

    /** Time left from the elapsed time and the share done; -1 until enough has been done to say. */
    public static long remainingMs(long elapsedMs, int permille) {
        if (permille < 20 || elapsedMs <= 0) return -1L;
        if (permille >= 1000) return 0L;
        return Math.round(elapsedMs * (1000 - permille) / (double) permille);
    }

    /** Explicit units, including seconds; never an unexplained MM:SS value. */
    public static String clock(long ms) {
        long seconds = Math.max(0L, ms) / 1000;
        long hours = seconds / 3600, minutes = (seconds % 3600) / 60, rest = seconds % 60;
        return hours > 0 ? hours + " giờ " + minutes + " phút " + rest + " s"
                : minutes > 0 ? minutes + " phút " + rest + " s" : rest + " s";
    }

    public static String remaining(long ms) { return ms < 0 ? "Đang ước tính" : clock(ms); }

    /**
     * Durable wall-clock elapsed time for a whole translation session. The six-argument
     * form is used when a paused elapsed value is already stored; the seven-argument
     * form additionally uses the current pause marker.
     */
    public static long runElapsedMs(String state, long nowWallMs, long sessionStartMs,
                                    long sessionEndMs, long storedElapsedMs, long pausedTotalMs) {
        return runElapsedMs(state, nowWallMs, sessionStartMs, sessionEndMs, 0L,
                storedElapsedMs, pausedTotalMs);
    }

    public static long runElapsedMs(String state, long nowWallMs, long sessionStartMs,
                                    long sessionEndMs, long pausedAtMs, long storedElapsedMs,
                                    long pausedTotalMs) {
        long stored = Math.max(0L, storedElapsedMs);
        long pauses = Math.max(0L, pausedTotalMs);
        if (sessionStartMs <= 0L) return stored;
        String normalized = state == null ? "" : state.trim().toUpperCase(Locale.ROOT);
        if ("PAUSED".equals(normalized)) {
            if (pausedAtMs > 0L) return Math.max(0L, pausedAtMs - sessionStartMs - pauses);
            return stored;
        }
        if (sessionEndMs > 0L || "COMPLETED".equals(normalized)
                || "FAILED".equals(normalized) || "CANCELLED".equals(normalized)) {
            return sessionEndMs > 0L
                    ? Math.max(0L, sessionEndMs - sessionStartMs - pauses)
                    : stored;
        }
        return Math.max(0L, Math.max(nowWallMs, sessionStartMs) - sessionStartMs - pauses);
    }

    public static String tokens(long value) {
        return java.text.NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(Math.max(0L, value)) + " token";
    }

    public static String money(double value) { return String.format(Locale.US, "$%.4f", Math.max(0d, value)); }

    public static String errors(int failedChunks) { return Math.max(0, failedChunks) + " chunk lỗi"; }

    /** Counts belong to the current request. A missing measurement is not a zero. */
    public static String rules(int glossary, int pronoun) {
        return "Glossaries: " + ruleCount(glossary) + " · Pronoun: " + ruleCount(pronoun);
    }

    private static String ruleCount(int count) { return count < 0 ? "chưa đo" : count + " luật"; }

    public static String chipLabel(String state) {
        if ("VALIDATING".equals(state) || "READY".equals(state)) return "Chuẩn bị";
        if ("COMPLETED".equals(state)) return "Hoàn tất";
        if ("FAILED".equals(state)) return "Có lỗi";
        if ("CANCELLED".equals(state)) return "Đã hủy";
        if ("IDLE".equals(state)) return "Sẵn sàng";
        return stateLabel(state);
    }

    /** Planned cost and chapter-local usage are explicitly distinguished. */
    public static String estimateLine(boolean set, double estimatedCost, long chapterTokens, double chapterCost) {
        String planned = estimatedCost > 0 ? String.format(Locale.US, "$%.2f", estimatedCost) : "chưa có ước tính";
        return (set ? "Dự kiến cả lượt: " : "Dự kiến: ") + planned + "\n"
                + (set ? "Chương này: " : "Đã dùng: ") + tokens(chapterTokens) + " · Chi phí "
                + String.format(Locale.US, "$%.3f", Math.max(0d, chapterCost));
    }

    /** One line for the collapsed source and settings while a run is going. */
    public static String collapsedSource(String setName, int chapters, boolean set, String fileName, int files) {
        if (set) return "Tập " + (setName == null || setName.isEmpty() ? "" : setName + " · ") + chapters + " chương · ▸";
        return (files > 1 ? files + " tệp" : fileName == null || fileName.isEmpty() ? "1 tệp" : fileName) + " · ▸";
    }
}
