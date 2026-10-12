package com.ml.tblandroidtxt;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads what a pair run needs from the app: a translation job (rows only, never written) and the Library references. The
 * job is opened through the repository with read calls only; nothing here can change a row, a status, an output or a ledger.
 */
final class EditorialPairSourceLoader {
    /** A job the person can choose: how many of its rows already have a completed translation. */
    static final class JobChoice {
        final long id;
        final String fileName;
        final String status;
        final int total;
        final int done;

        JobChoice(long id, String fileName, String status, int total, int done) {
            this.id = id;
            this.fileName = fileName == null ? "" : fileName;
            this.status = status == null ? "" : status;
            this.total = total;
            this.done = done;
        }

        String label() { return (fileName.isEmpty() ? "Job " + id : fileName) + " • " + done + "/" + total + " đoạn đã dịch"; }
    }

    /** The glossary and pronoun the combo points at, as the texts that are frozen into the run; empty = not used. */
    static final class References {
        final String glossaryText;
        final String glossaryName;
        final String pronounText;
        final String pronounName;

        References(String glossaryText, String glossaryName, String pronounText, String pronounName) {
            this.glossaryText = glossaryText;
            this.glossaryName = glossaryName;
            this.pronounText = pronounText;
            this.pronounName = pronounName;
        }
    }

    private EditorialPairSourceLoader() { }

    static List<JobChoice> listJobs(Context context) {
        List<JobChoice> out = new ArrayList<>();
        TranslationRepository repository = new TranslationRepository(context.getApplicationContext());
        try {
            for (TranslationRepository.JobSummary s : repository.getJobSummaries(40)) {
                if (s.totalChunks > 0) out.add(new JobChoice(s.id, s.fileName, s.status, s.totalChunks, s.doneChunks));
            }
        } finally {
            repository.close();
        }
        return out;
    }

    static EditorialPairSource loadJob(Context context, long jobId) {
        TranslationRepository repository = new TranslationRepository(context.getApplicationContext());
        try {
            TranslationRepository.Job job = repository.getJob(jobId);
            if (job == null) {
                return new EditorialPairSource(EditorialPairModels.SOURCE_JOB, String.valueOf(jobId), "", "", List.of(), List.of(), List.of("NO_ROWS"));
            }
            return EditorialPairSource.fromJobRows(jobId, job.fileName, repository.getChunkRows(jobId));
        } finally {
            repository.close();
        }
    }

    /** The two files cut into chunks, with the first lines of each file for the confirmation screen (kept in memory only). */
    static final class FilesLoad {
        final EditorialPairSource source;
        final com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlan plan;
        final List<String> rawHead;
        final List<String> draftHead;

        FilesLoad(EditorialPairSource source, com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlan plan, List<String> rawHead, List<String> draftHead) {
            this.source = source;
            this.plan = plan;
            this.rawHead = rawHead;
            this.draftHead = draftHead;
        }
    }

    /**
     * Chunk-planner settings taken from Settings → Performance through the same functions the Translate flow uses: the mode
     * and its measure ({@code Chunker.measure}), the adaptive hard and soft limits, MAX OUTPUT and the context size.
     */
    static com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlanner.Settings plannerSettings(AppSettings s) {
        final String mode = "char".equalsIgnoreCase(s.chunkMode) ? "char" : "token";
        int hard = Chunker.adaptiveLimit(s, s.effectiveHardLimit());
        int soft = Math.min(hard, Chunker.adaptiveLimit(s, s.effectiveSoftLimit()));
        return new com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlanner.Settings(mode, soft, hard, s.maxOutputTokens, s.contextChars, 1.3,
                text -> Chunker.measure(text, mode), text -> Chunker.measure(text, "token"),
                com.ml.tblandroidtxt.editorial.api.chunk.ChapterVerdict.DEFAULT_SERIES_RATIO);
    }

    /** Reads RAW and DRAFT with the app's own text reader (UTF-8, UTF-16, Windows-31J) and plans the chunks. Nothing is sent. */
    static FilesLoad loadFiles(Context context, EditorialApiCombo combo, References refs, AppSettings settings) throws EditorialApiSourceLoader.SourceException {
        String raw = EditorialApiSourceLoader.readFile(context, combo.rawUri, "RAW");
        String draft = EditorialApiSourceLoader.readFile(context, combo.draftUri, "DRAFT");
        return planFiles(combo.rawName + " + " + combo.draftName, raw, draft, refs, settings);
    }

    static FilesLoad planFiles(String label, String raw, String draft, References refs, AppSettings settings) {
        com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlanner.Planned planned = com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlanner.plan(
                raw, draft, EditorialPairSnapshot.glossaryFrom(refs.glossaryText), refs.glossaryText, refs.pronounText, plannerSettings(settings));
        return new FilesLoad(EditorialPairSource.fromPlan(label, planned), planned.plan(), head(planned.raw()), head(planned.draft()));
    }

    private static List<String> head(com.ml.tblandroidtxt.editorial.api.chunk.LineUnits units) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(2, units.size()); i++) {
            String t = units.units().get(i).text();
            out.add(t.length() > 60 ? t.substring(0, 60) + "…" : t);
        }
        return out;
    }

    /** {@code null} problem = ok; a reference the combo names but the Library no longer has is reported, not silently dropped. */
    static References loadReferences(Context context, EditorialApiCombo combo) throws EditorialApiSourceLoader.SourceException {
        String glossaryText = "";
        String glossaryName = "";
        if (!combo.glossaryId.isEmpty()) {
            GlossaryStore.Glossary glossary = GlossaryStore.find(context, combo.glossaryId);
            if (glossary == null) throw new EditorialApiSourceLoader.SourceException("GLOSSARY", "Glossary đã chọn không còn trong thư viện.");
            List<com.ml.tblandroidtxt.editorial.api.EditInputs.GlossaryEntry> entries = new ArrayList<>();
            for (GlossaryStore.Term term : glossary.terms) {
                entries.add(new com.ml.tblandroidtxt.editorial.api.EditInputs.GlossaryEntry(term.source, term.target, term.category, term.note));
            }
            glossaryText = EditorialApiSources.glossaryAsText(entries);
            glossaryName = glossary.name;
        }
        String pronounText = "";
        String pronounName = "";
        if (!combo.pronounId.isEmpty()) {
            PronounStore.Profile profile = PronounStore.find(context, combo.pronounId);
            if (profile == null) throw new EditorialApiSourceLoader.SourceException("PRONOUN", "Pronoun đã chọn không còn trong thư viện.");
            pronounText = profile.text == null ? "" : profile.text;
            pronounName = profile.name;
        }
        return new References(glossaryText, glossaryName, pronounText, pronounName);
    }
}
