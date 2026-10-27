package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Decides, for each picked TXT, whether it becomes a library RAW, using the same normalised hash as {@code jobs.input_hash}. */
final class RawImportPlanner {
    enum Verdict { ADD, DUPLICATE, INVALID }

    static final class Candidate {
        final String name;
        final String text;
        final String encoding;
        Candidate(String name, String text, String encoding) {
            this.name = name == null ? "" : name.trim();
            this.text = text == null ? "" : text;
            this.encoding = encoding == null ? "" : encoding;
        }
    }

    static final class Entry {
        final Candidate candidate;
        final Verdict verdict;
        final String normalized;
        final String hash;
        final int chapterNo;
        final String reason;
        Entry(Candidate c, Verdict v, String normalized, String hash, int chapterNo, String reason) {
            this.candidate = c; this.verdict = v; this.normalized = normalized; this.hash = hash; this.chapterNo = chapterNo; this.reason = reason;
        }
    }

    static final class Plan {
        final List<Entry> entries = new ArrayList<>();
        int added() { return count(Verdict.ADD); }
        int skipped() { return entries.size() - added(); }
        private int count(Verdict v) { int n = 0; for (Entry e : entries) if (e.verdict == v) n++; return n; }
        String summary() { return "Đã thêm " + added() + " RAW, bỏ qua " + skipped() + " (trùng/lỗi)"; }
    }

    /** {@code existingHashes} are the hashes already in the library; two picked files with equal text add only the first. */
    static Plan plan(List<Candidate> picked, Set<String> existingHashes) {
        Plan plan = new Plan();
        Set<String> seen = new HashSet<>(existingHashes == null ? new HashSet<String>() : existingHashes);
        if (picked == null) return plan;
        for (Candidate c : picked) {
            String normalized = Chunker.normalizeSource(c.text);
            String invalid = AppValidator.validateSourceText(normalized, c.name);
            if (invalid != null) { plan.entries.add(new Entry(c, Verdict.INVALID, normalized, "", ChapterNumber.NONE, invalid)); continue; }
            String hash = HashUtil.sha256(normalized);
            int chapter = ChapterNumber.parse(c.name);
            if (!seen.add(hash)) { plan.entries.add(new Entry(c, Verdict.DUPLICATE, normalized, hash, chapter, c.name + ": trùng nội dung với RAW đã có")); continue; }
            plan.entries.add(new Entry(c, Verdict.ADD, normalized, hash, chapter, ""));
        }
        return plan;
    }

    private RawImportPlanner() {}
}
