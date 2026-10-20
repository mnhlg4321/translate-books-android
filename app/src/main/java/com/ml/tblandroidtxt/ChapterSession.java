package com.ml.tblandroidtxt;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * The per-Start snapshot of a chapter-set run: one {@link ChapterBinding} per prepared input, in input order, saved as
 * {@code <dir>/<startSessionId>.json} when Start is pressed. The intent carries only the session id, so the Glossary / Pronoun
 * texts never travel in an intent, and the run keeps using exactly these texts even if the Library changes afterwards.
 */
final class ChapterSession {
    static final String DIR = "translate_sessions";

    final String sessionId;
    String setId = "";
    String setName = "";
    long createdAt = System.currentTimeMillis();
    final List<ChapterBinding> chapters = new ArrayList<>();
    /** Ids of earlier unfinished jobs this run continued instead of starting new ones (read by the Jobs tab to group runs). */
    final List<Long> resumedJobs = new ArrayList<>();

    ChapterSession(String sessionId) { this.sessionId = sessionId == null ? "" : sessionId; }

    static File file(File dir, String sessionId) throws IOException {
        if (sessionId == null || !sessionId.matches("[A-Za-z0-9-]{1,64}")) throw new IOException("Start session id không hợp lệ");
        return new File(dir, sessionId + ".json");
    }

    void write(File dir) throws Exception {
        JSONArray rows = new JSONArray();
        for (ChapterBinding b : chapters) rows.put(b.toJson());
        JSONArray resumed = new JSONArray();
        for (Long id : resumedJobs) resumed.put(id.longValue());
        JSONObject root = new JSONObject().put("sessionId", sessionId).put("setId", setId).put("setName", setName).put("createdAt", createdAt).put("chapters", rows).put("resumedJobs", resumed);
        AtomicFileCommit.write(file(dir, sessionId).toPath(), root.toString());
    }

    /** Reads and checks a snapshot; throws with a readable reason when it is missing, damaged or not for this session. */
    static ChapterSession read(File dir, String sessionId) throws Exception {
        File f = file(dir, sessionId);
        if (!f.isFile()) throw new IllegalStateException("Thiếu ảnh chụp bộ chương của lượt chạy này; hãy bấm Start lại.");
        JSONObject root = new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        if (!sessionId.equals(root.optString("sessionId"))) throw new IllegalStateException("Ảnh chụp bộ chương không thuộc lượt chạy này.");
        ChapterSession s = new ChapterSession(sessionId);
        s.setId = root.optString("setId");
        s.setName = root.optString("setName");
        s.createdAt = root.optLong("createdAt");
        JSONArray resumed = root.optJSONArray("resumedJobs");
        if (resumed != null) for (int i = 0; i < resumed.length(); i++) s.resumedJobs.add(resumed.optLong(i));
        JSONArray rows = root.getJSONArray("chapters");
        for (int i = 0; i < rows.length(); i++) {
            ChapterBinding b = ChapterBinding.fromJson(rows.getJSONObject(i));
            if (!b.intact()) throw new IllegalStateException("Ảnh chụp bộ chương bị hỏng ở chương " + (i + 1) + ".");
            s.chapters.add(b);
        }
        return s;
    }

    /** Name, time, chapter count and continued jobs of every snapshot in {@code dir}; a damaged file is skipped. Read only. */
    static List<RunGroups.SessionInfo> readInfos(File dir) {
        ArrayList<RunGroups.SessionInfo> out = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return out;
        for (File f : files) {
            if (!f.getName().endsWith(".json")) continue;
            try {
                JSONObject root = new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
                RunGroups.SessionInfo info = new RunGroups.SessionInfo();
                info.sessionId = root.optString("sessionId");
                if (info.sessionId.isEmpty()) continue;
                info.setId = root.optString("setId");
                info.setName = root.optString("setName");
                info.createdAt = root.optLong("createdAt");
                JSONArray rows = root.optJSONArray("chapters");
                info.chapterCount = rows == null ? 0 : rows.length();
                JSONArray resumed = root.optJSONArray("resumedJobs");
                if (resumed != null) for (int i = 0; i < resumed.length(); i++) info.resumedJobs.add(resumed.optLong(i));
                out.add(info);
            } catch (Exception skip) { }
        }
        return out;
    }

    /** The snapshot must describe exactly the prepared inputs, in the same order. */
    void requireMatches(List<String> preparedInputUris) {
        if (chapters.size() != preparedInputUris.size()) throw new IllegalStateException("Ảnh chụp bộ chương có " + chapters.size() + " chương nhưng kế hoạch có " + preparedInputUris.size() + ".");
        for (int i = 0; i < chapters.size(); i++) {
            if (!chapters.get(i).rawUri().equals(preparedInputUris.get(i))) throw new IllegalStateException("Ảnh chụp bộ chương không khớp kế hoạch ở chương " + (i + 1) + ".");
        }
    }

    /** Removes snapshot files older than {@code maxAgeMs}; returns how many were removed. */
    static int purgeOlderThan(File dir, long maxAgeMs, long now) {
        File[] files = dir.listFiles();
        if (files == null) return 0;
        int removed = 0;
        for (File f : files) if (now - f.lastModified() > maxAgeMs && f.delete()) removed++;
        return removed;
    }
}
