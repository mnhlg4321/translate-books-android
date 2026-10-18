package com.ml.tblandroidtxt;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure helpers for "an unfinished job already exists for this file": the question's text and which job each input continues. */
final class ResumePrompt {
    private ResumePrompt() {}

    /** "x/y chunk xong · z lỗi · m chưa dịch" for one job. */
    static String progressLine(TranslationRepository.JobProgress p) {
        StringBuilder sb = new StringBuilder(p.done + "/" + p.total + " chunk xong");
        if (p.error > 0) sb.append(" · ").append(p.error).append(" lỗi");
        if (p.unknown > 0) sb.append(" · ").append(p.unknown).append(" không rõ đã gửi");
        if (p.pending > 0) sb.append(" · ").append(p.pending).append(" chưa dịch");
        return sb.toString();
    }

    static String message(List<TranslationRepository.JobProgress> partial) {
        StringBuilder sb = new StringBuilder(partial.size() == 1 ? "Đã có job dở cho tệp này:" : "Đã có job dở cho " + partial.size() + " tệp:");
        for (TranslationRepository.JobProgress p : partial) sb.append("\n• ").append(p.fileName).append(" — ").append(progressLine(p));
        sb.append("\n\nTiếp tục job cũ chỉ dịch phần còn lại (không tốn tiền dịch lại phần đã xong). Dịch lại từ đầu tạo job mới và dịch toàn bộ.");
        return sb.toString();
    }

    /**
     * For each prepared input (by ordinal and input hash) the newest unfinished job with the same hash that can be continued (nothing of
     * unknown delivery). {@code partial} is newest first.
     */
    static Map<Integer, Long> chooseJobs(List<Integer> ordinals, List<String> hashes, List<TranslationRepository.JobProgress> partial) {
        Map<Integer, Long> out = new HashMap<>();
        for (int i = 0; i < ordinals.size(); i++) {
            for (TranslationRepository.JobProgress p : partial) {
                if (p.unknown == 0 && p.inputHash.equals(hashes.get(i))) { out.put(ordinals.get(i), p.id); break; }
            }
        }
        return out;
    }

    static String toJson(Map<Integer, Long> map) {
        JSONObject o = new JSONObject();
        try { for (Map.Entry<Integer, Long> e : map.entrySet()) o.put(String.valueOf(e.getKey()), e.getValue()); } catch (Exception ignored) { }
        return o.toString();
    }
}
