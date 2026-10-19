package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.List;

/**
 * One-time repair of jobs wrongly marked INPUT_CONFLICT by the line-ending hash mismatch (v4.21 to v4.23). Only jobs made by a prepared
 * run, whose file can still be read and whose normalized text hashes to the stored value, go back to "paused": a match means the text
 * is identical and only the line endings differed. Runs on a background thread; the flag keeps it from running again.
 */
final class InputConflictRepair {
    private static final String PREFS = "input_conflict_repair";
    private static final String DONE = "done_v423_crlf";

    private InputConflictRepair() {}

    /** Number of jobs put back, or -1 when the repair had already run. */
    static int runOnce(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(DONE, false)) return -1;
        int repaired = 0;
        TranslationRepository repo = new TranslationRepository(context);
        try {
            List<TranslationRepository.ConflictJob> jobs = repo.getRepairableInputConflicts();
            for (TranslationRepository.ConflictJob j : jobs) {
                try {
                    String text = FileUtil.readText(context, Uri.parse(j.inputUri));
                    InputIdentity.Match m = InputIdentity.match(j.inputHash, text);
                    if (m == InputIdentity.Match.NORMALIZED) {
                        repo.touchJob(j.id, "paused");
                        repaired++;
                        ObservabilityLog.event("input_conflict_repaired", "job", j.id, "match", m.name());
                    }
                } catch (Exception unreadable) {
                    ObservabilityLog.event("input_conflict_repair_skipped", "job", j.id, "reason", "unreadable");
                }
            }
        } finally { repo.close(); }
        prefs.edit().putBoolean(DONE, true).apply();
        return repaired;
    }
}
