package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where a pair run gets its RAW and DRAFT rows. The first source is a translation job: a row of the job is the link between a
 * RAW chunk and its translation, and it is checked (contiguous offsets, hashes, completed status) before anything is built.
 * Two independent files have no such link; they are kept as a combo and previewed, but they carry the issue
 * {@code NO_EXPLICIT_MAPPING} and never run by chunk. Nothing is written to the job. Pure.
 */
public final class EditorialPairSource {
    public final String kind;
    public final String ref;
    public final String label;
    public final String chapterId;
    public final List<String> rawRows;
    /** Same length as {@link #rawRows}; {@code null} where the row has no completed translation. */
    public final List<String> draftRows;
    /** Defects of the source itself, typed; an empty list means the lineage of every row was checked. */
    public final List<String> lineageIssues;

    public EditorialPairSource(String kind, String ref, String label, String chapterId, List<String> rawRows, List<String> draftRows,
                               List<String> lineageIssues) {
        this.kind = kind;
        this.ref = ref;
        this.label = label == null ? "" : label;
        this.chapterId = chapterId == null ? "" : chapterId;
        this.rawRows = Collections.unmodifiableList(new ArrayList<>(rawRows));
        this.draftRows = Collections.unmodifiableList(new ArrayList<>(draftRows));
        this.lineageIssues = Collections.unmodifiableList(new ArrayList<>(lineageIssues));
    }

    static boolean completed(String status) { return "done".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status); }

    /** Rows of one job, read-only: the lists are copies and the repository is never asked to change anything. */
    public static EditorialPairSource fromJobRows(long jobId, String fileName, List<TranslationRepository.ChunkRow> rows) {
        List<String> raw = new ArrayList<>();
        List<String> draft = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            issues.add("NO_ROWS");
            return new EditorialPairSource(EditorialPairModels.SOURCE_JOB, String.valueOf(jobId), fileName, "", raw, draft, issues);
        }
        List<TranslationRepository.ChunkRow> sorted = new ArrayList<>(rows);
        sorted.sort((a, b) -> Integer.compare(a.index, b.index));
        int previousEnd = -1;
        for (int i = 0; i < sorted.size(); i++) {
            TranslationRepository.ChunkRow r = sorted.get(i);
            String source = r.source == null ? "" : r.source;
            if (source.trim().isEmpty()) issues.add("ROW_EMPTY_SOURCE:" + r.index);
            if (r.startOffset >= 0 && r.endOffset >= 0) {
                if (previousEnd >= 0 && r.startOffset != previousEnd) issues.add("ROW_OFFSET_GAP:" + r.index);
                if (r.endOffset - r.startOffset != source.length()) issues.add("ROW_RANGE_MISMATCH:" + r.index);
                previousEnd = r.endOffset;
            }
            if (r.sourceHash != null && !r.sourceHash.isEmpty() && !HashUtil.sha256(source).equals(r.sourceHash)) issues.add("ROW_HASH_MISMATCH:" + r.index);
            raw.add(source);
            boolean usable = completed(r.status) && r.translated != null && !r.translated.trim().isEmpty();
            draft.add(usable ? r.translated : null);
        }
        return new EditorialPairSource(EditorialPairModels.SOURCE_JOB, String.valueOf(jobId), fileName, "", raw, draft, issues);
    }

    /**
     * Two files whose non-blank lines correspond one to one (same count): line i of RAW is the source of line i of DRAFT, as
     * in the owner's chapter files. Consecutive lines are grouped into chunks of at most {@code maxRawChars} RAW characters
     * (a longer single line is a chunk of its own), and the same line ranges cut the DRAFT. Each row keeps its exact text
     * including the blank lines after it, so the rows concatenate back to each file. Different counts give no rows and the
     * issue {@code LINE_COUNT_MISMATCH:raw/draft}: the link is not proven, so the caller falls back to the whole chapter.
     */
    public static EditorialPairSource fromAlignedFiles(String label, String rawText, String draftText, int maxRawChars) {
        List<String> raw = lineSegments(rawText);
        List<String> draft = lineSegments(draftText);
        if (raw.isEmpty() || raw.size() != draft.size()) {
            return new EditorialPairSource(EditorialPairModels.SOURCE_FILES, "", label, "", List.of(), List.of(),
                    List.of("LINE_COUNT_MISMATCH:" + raw.size() + "/" + draft.size()));
        }
        int budget = Math.max(1, maxRawChars);
        List<String> rawRows = new ArrayList<>();
        List<String> draftRows = new ArrayList<>();
        StringBuilder r = new StringBuilder();
        StringBuilder d = new StringBuilder();
        int chars = 0;
        for (int i = 0; i < raw.size(); i++) {
            int size = raw.get(i).trim().length();
            if (r.length() > 0 && chars + size > budget) {
                rawRows.add(r.toString());
                draftRows.add(d.toString());
                r.setLength(0);
                d.setLength(0);
                chars = 0;
            }
            r.append(raw.get(i));
            d.append(draft.get(i));
            chars += size;
        }
        rawRows.add(r.toString());
        draftRows.add(d.toString());
        return new EditorialPairSource(EditorialPairModels.SOURCE_FILES, "", label, "", rawRows, draftRows, List.of());
    }

    /** One segment per non-blank line: the line plus the blank lines after it; leading blank lines go with the first. */
    static List<String> lineSegments(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        StringBuilder lead = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int end = text.indexOf('\n', i);
            end = end < 0 ? text.length() : end + 1;
            String line = text.substring(i, end);
            if (line.trim().isEmpty()) {
                if (out.isEmpty()) lead.append(line);
                else out.set(out.size() - 1, out.get(out.size() - 1) + line);
            } else {
                out.add(out.isEmpty() ? lead + line : line);
            }
            i = end;
        }
        return out;
    }

    /** Two independent files: a source without any link between them. It can be saved and previewed, not run by chunk. */
    public static EditorialPairSource unmappedFiles(String label) {
        return new EditorialPairSource(EditorialPairModels.SOURCE_FILES, "", label, "", List.of(), List.of(), List.of("NO_EXPLICIT_MAPPING"));
    }

    public int missingRows() {
        int n = 0;
        for (int i = 0; i < draftRows.size(); i++) if (draftRows.get(i) == null) n++;
        return n;
    }
}
