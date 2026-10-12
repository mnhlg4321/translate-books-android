package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlan;
import com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where a pair run gets its RAW and DRAFT rows. The first source is a translation job: a row of the job is the link between a
 * RAW chunk and its translation, and it is checked (contiguous offsets, hashes, completed status) before anything is built.
 * Two files are linked by the CS-1 chunk plan (alignment, chapter verdict, cuts); without a plan they carry the issue
 * {@code NO_EXPLICIT_MAPPING} and do not run by chunk. Nothing is written to the job. Pure.
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
    /** The chunk plan that produced the rows (two-file sources); {@code null} for a job source. */
    public final ChunkPlan plan;

    public EditorialPairSource(String kind, String ref, String label, String chapterId, List<String> rawRows, List<String> draftRows,
                               List<String> lineageIssues) {
        this(kind, ref, label, chapterId, rawRows, draftRows, lineageIssues, null);
    }

    public EditorialPairSource(String kind, String ref, String label, String chapterId, List<String> rawRows, List<String> draftRows,
                               List<String> lineageIssues, ChunkPlan plan) {
        this.kind = kind;
        this.ref = ref;
        this.label = label == null ? "" : label;
        this.chapterId = chapterId == null ? "" : chapterId;
        this.rawRows = Collections.unmodifiableList(new ArrayList<>(rawRows));
        this.draftRows = Collections.unmodifiableList(new ArrayList<>(draftRows));
        this.lineageIssues = Collections.unmodifiableList(new ArrayList<>(lineageIssues));
        this.plan = plan;
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
     * Two files cut by the CS-1 chunk plan: row i is chunk i, the exact text of its RAW lines and of its DRAFT lines (blank lines
     * included), so the rows concatenate back to each file. A plan the chapter verdict blocked has no rows and the issue
     * {@code CHAPTER_BLOCKED}; the person has to choose other files.
     */
    public static EditorialPairSource fromPlan(String label, ChunkPlanner.Planned planned) {
        ChunkPlan plan = planned.plan();
        if (plan.blocked() || plan.chunks.isEmpty()) {
            return new EditorialPairSource(EditorialPairModels.SOURCE_FILES, "", label, "", List.of(), List.of(), List.of("CHAPTER_BLOCKED"), plan);
        }
        return new EditorialPairSource(EditorialPairModels.SOURCE_FILES, "", label, "", planned.rawRows(), planned.draftRows(), List.of(), plan);
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
