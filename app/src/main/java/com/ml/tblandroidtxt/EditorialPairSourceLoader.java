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
