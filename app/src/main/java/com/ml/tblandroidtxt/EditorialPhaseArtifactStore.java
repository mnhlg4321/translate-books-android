package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Durable L2_EDIT store (schema v25 {@code editorial_phase_artifacts}). VI_L2 and
 * CHANGE_MAP_L2 are written by one UPDATE of one claimed row, so a crash leaves
 * either the complete pair or no pair. Readback re-hashes both blobs; a row
 * whose bytes do not match its recorded hashes is never returned as committed.
 */
public final class EditorialPhaseArtifactStore implements EditorialL2Execution.Store {
    static final String TABLE = "editorial_phase_artifacts";
    static final String L2_PHASE = "L2_EDIT";
    static final String L3_PHASE = "L3_FINAL";

    private final TranslationRepository database;
    private final String chapterKey;
    private final String phase;

    public EditorialPhaseArtifactStore(TranslationRepository database, String chapterKey) {
        this(database, chapterKey, L2_PHASE);
    }

    /** The same one-row pair store for L3: FINAL text + QA_RECEIPT. */
    public EditorialPhaseArtifactStore(TranslationRepository database, String chapterKey, String phase) {
        if (!L2_PHASE.equals(phase) && !L3_PHASE.equals(phase)) throw new IllegalArgumentException("phase is invalid");
        this.phase = phase;
        this.database = Objects.requireNonNull(database, "database");
        if (chapterKey == null || chapterKey.isBlank()) throw new IllegalArgumentException("chapter key is required");
        this.chapterKey = chapterKey;
    }

    @Override public synchronized Claim claim(String attemptIdentity, String predecessorIdentity,
                                              String bundleIdentity) {
        requireHash(attemptIdentity, "attempt identity");
        requireHash(predecessorIdentity, "predecessor identity");
        requireHash(bundleIdentity, "bundle identity");
        Row existing = findRow(attemptIdentity);
        if (existing == null) {
            long now = System.currentTimeMillis();
            ContentValues values = new ContentValues();
            values.put("attempt_identity", attemptIdentity);
            values.put("phase", phase);
            values.put("chapter_key", chapterKey);
            values.put("predecessor_identity", predecessorIdentity);
            values.put("bundle_identity", bundleIdentity);
            values.put("status", "CLAIMED");
            values.putNull("text_bytes");
            values.putNull("evidence_bytes");
            values.put("created_at", now);
            values.put("updated_at", now);
            try {
                database.editorialWritableDatabase().insertOrThrow(TABLE, null, values);
                return Claim.ACQUIRED;
            } catch (SQLiteConstraintException race) {
                existing = findRow(attemptIdentity);
                if (existing == null) throw race;
            }
        }
        if (!existing.phase.equals(phase) || !existing.chapterKey.equals(chapterKey)
                || !existing.predecessorIdentity.equals(predecessorIdentity)
                || !existing.bundleIdentity.equals(bundleIdentity)) {
            throw new IllegalStateException("L2 attempt identity facts changed");
        }
        switch (existing.status) {
            case "COMMITTED": return Claim.ALREADY_COMMITTED;
            case "CLAIMED": return Claim.IN_FLIGHT;
            case "RECOVERY_REQUIRED": return Claim.RECOVERY_REQUIRED;
            default: throw new IllegalStateException("L2 attempt status is invalid");
        }
    }

    @Override public synchronized void commit(EditorialL2Execution.Committed value) {
        Objects.requireNonNull(value, "value");
        if (!EditorialCanonicalJson.sha256Hex(value.viL2Bytes()).equals(value.viL2Sha256())
                || !EditorialCanonicalJson.sha256Hex(value.changeMapBytes()).equals(value.changeMapSha256())) {
            throw new IllegalArgumentException("L2 commit hashes do not match their bytes");
        }
        Row current = findRow(value.attemptIdentity());
        if (current == null || !"CLAIMED".equals(current.status)
                || !current.predecessorIdentity.equals(value.predecessorIdentity())
                || !current.bundleIdentity.equals(value.bundleIdentity())) {
            throw new IllegalStateException("L2 attempt is not the claimed exact attempt");
        }
        ContentValues update = new ContentValues();
        update.put("status", "COMMITTED");
        update.put("text_bytes", value.viL2Bytes());
        update.put("text_sha256", value.viL2Sha256());
        update.put("evidence_bytes", value.changeMapBytes());
        update.put("evidence_sha256", value.changeMapSha256());
        update.put("recovery_reason_code", "");
        update.put("updated_at", System.currentTimeMillis());
        if (database.editorialWritableDatabase().update(TABLE, update,
                "attempt_identity=? AND status='CLAIMED'", new String[]{value.attemptIdentity()}) != 1) {
            throw new IllegalStateException("L2 atomic commit lost the claim");
        }
    }

    @Override public synchronized Optional<EditorialL2Execution.Committed> findCommitted(String attemptIdentity) {
        Row row = findRow(attemptIdentity);
        if (row == null || !"COMMITTED".equals(row.status) || row.textBytes == null || row.evidenceBytes == null) {
            return Optional.empty();
        }
        if (!EditorialCanonicalJson.sha256Hex(row.textBytes).equals(row.textSha256)
                || !EditorialCanonicalJson.sha256Hex(row.evidenceBytes).equals(row.evidenceSha256)) {
            return Optional.empty();
        }
        return Optional.of(new EditorialL2Execution.Committed(row.attemptIdentity, row.predecessorIdentity,
                row.bundleIdentity, row.textBytes, row.textSha256, row.evidenceBytes, row.evidenceSha256));
    }

    /**
     * Read-only status of one attempt row for progress display; never claims or mutates. An empty
     * result means the attempt was never claimed. {@code intact} re-hashes both blobs of a COMMITTED row.
     */
    public synchronized Optional<EditorialChapterProgress.StageRow> inspect(String attemptIdentity) {
        requireHash(attemptIdentity, "attempt identity");
        Row row = findRow(attemptIdentity);
        if (row == null) return Optional.empty();
        String reason = "";
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT recovery_reason_code FROM " + TABLE + " WHERE attempt_identity=?", new String[]{attemptIdentity})) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) reason = cursor.getString(0);
        }
        boolean intact = row.textBytes != null && row.evidenceBytes != null
                && EditorialCanonicalJson.sha256Hex(row.textBytes).equals(row.textSha256)
                && EditorialCanonicalJson.sha256Hex(row.evidenceBytes).equals(row.evidenceSha256);
        return Optional.of(new EditorialChapterProgress.StageRow(row.status, reason, intact));
    }

    @Override public synchronized void markRecoveryRequired(String attemptIdentity, String reasonCode) {
        requireHash(attemptIdentity, "attempt identity");
        if (reasonCode == null || reasonCode.isBlank()) throw new IllegalArgumentException("reason code is required");
        ContentValues update = new ContentValues();
        update.put("status", "RECOVERY_REQUIRED");
        update.put("recovery_reason_code", reasonCode);
        update.put("updated_at", System.currentTimeMillis());
        database.editorialWritableDatabase().update(TABLE, update,
                "attempt_identity=? AND status='CLAIMED'", new String[]{attemptIdentity});
    }

    private Row findRow(String attemptIdentity) {
        SQLiteDatabase db = database.editorialReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT attempt_identity,phase,chapter_key,predecessor_identity,"
                + "bundle_identity,status,text_bytes,text_sha256,evidence_bytes,evidence_sha256 FROM "
                + TABLE + " WHERE attempt_identity=?", new String[]{attemptIdentity})) {
            if (!cursor.moveToFirst()) return null;
            return new Row(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                    cursor.getString(4), cursor.getString(5),
                    cursor.isNull(6) ? null : cursor.getBlob(6), cursor.getString(7),
                    cursor.isNull(8) ? null : cursor.getBlob(8), cursor.getString(9));
        }
    }

    private static void requireHash(String value, String label) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(label + " must be a lowercase SHA-256 hex value");
        }
    }

    private static final class Row {
        final String attemptIdentity;
        final String phase;
        final String chapterKey;
        final String predecessorIdentity;
        final String bundleIdentity;
        final String status;
        final byte[] textBytes;
        final String textSha256;
        final byte[] evidenceBytes;
        final String evidenceSha256;

        Row(String attemptIdentity, String phase, String chapterKey, String predecessorIdentity,
            String bundleIdentity, String status, byte[] textBytes, String textSha256,
            byte[] evidenceBytes, String evidenceSha256) {
            this.attemptIdentity = attemptIdentity;
            this.phase = phase;
            this.chapterKey = chapterKey;
            this.predecessorIdentity = predecessorIdentity;
            this.bundleIdentity = bundleIdentity;
            this.status = status;
            this.textBytes = textBytes == null ? null : Arrays.copyOf(textBytes, textBytes.length);
            this.textSha256 = textSha256;
            this.evidenceBytes = evidenceBytes == null ? null : Arrays.copyOf(evidenceBytes, evidenceBytes.length);
            this.evidenceSha256 = evidenceSha256;
        }
    }
}
