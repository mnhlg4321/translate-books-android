package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.ml.tblandroidtxt.editorial.pack.EditorialLineageInputEntry;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageParentReference;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageRecord;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageValidationCode;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageValidationContext;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageValidationResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Append/read-only SQLite v16 boundary for exact-parent lineage records. */
public final class EditorialLineageDao {
    private static final String RECORD_TABLE = "editorial_lineage_records";
    private static final String ENTRY_TABLE = "editorial_lineage_input_entries";
    private static final String RECORD_SELECT =
            "SELECT record_identity,record_fingerprint,canonical_pack_hash,trusted_profile_id," +
            "trusted_profile_version,canonical_profile_hash,machine_contract_fingerprint,contract_version," +
            "schema_version,project_identity,input_scope_identity,run_evaluation_identity,input_manifest_version," +
            "input_manifest_fingerprint,node_kind,parent_record_identity,parent_record_fingerprint,created_at " +
            "FROM " + RECORD_TABLE;
    private final TranslationRepository database;
    private final EditorialLineageValidator validator;

    public EditorialLineageDao(TranslationRepository database) {
        this(database, new EditorialLineageValidator());
    }

    EditorialLineageDao(TranslationRepository database, EditorialLineageValidator validator) {
        this.database = Objects.requireNonNull(database, "database");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    /** Appends a record and every manifest entry in one transaction. */
    public EditorialLineageAppendResult append(EditorialLineageRecord record) {
        return append(record, System.currentTimeMillis());
    }

    /** Timestamp is audit metadata only and is not part of the lineage fingerprint. */
    public synchronized EditorialLineageAppendResult append(EditorialLineageRecord record, long auditTimestamp) {
        if (record == null) return result(EditorialLineagePersistenceCode.INVALID_LINEAGE, "", "record-required");
        if (auditTimestamp < 0) return result(EditorialLineagePersistenceCode.INVALID_LINEAGE,
                record.recordIdentity(), "audit-timestamp-negative");

        Optional<EditorialLineageRecord> existing = findByRecordIdentity(record.recordIdentity());
        if (existing.isPresent()) return classifyExisting(existing.get(), record);

        EditorialLineageValidationContext context = new SqliteEditorialLineageValidationContext(this);
        EditorialLineageValidationResult validation = validator.validate(record, context);
        if (!validation.isValid()) return classifyValidation(record, context, validation);

        try {
            SQLiteDatabase db = database.editorialWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues row = recordValues(record, auditTimestamp);
                long rowId = db.insertOrThrow(RECORD_TABLE, null, row);
                for (EditorialLineageInputEntry entry : record.identity().inputManifest().entries()) {
                    db.insertOrThrow(ENTRY_TABLE, null, entryValues(record.recordIdentity(), entry));
                }
                db.setTransactionSuccessful();
                return EditorialLineageAppendResult.of(EditorialLineagePersistenceCode.APPENDED,
                        record.recordIdentity(), rowId, "appended");
            } finally {
                db.endTransaction();
            }
        } catch (SQLiteConstraintException error) {
            Optional<EditorialLineageRecord> raced = findByRecordIdentity(record.recordIdentity());
            if (raced.isPresent()) return classifyExisting(raced.get(), record);
            return result(EditorialLineagePersistenceCode.PERSISTENCE_FAILURE,
                    record.recordIdentity(), "sqlite-constraint-rollback");
        } catch (SQLiteException | IllegalStateException error) {
            return result(EditorialLineagePersistenceCode.PERSISTENCE_FAILURE,
                    record.recordIdentity(), "sqlite-append-rollback");
        }
    }

    public Optional<EditorialLineageRecord> findByRecordIdentity(String recordIdentity) {
        if (recordIdentity == null || recordIdentity.isBlank()) return Optional.empty();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                RECORD_SELECT + " WHERE record_identity=?", new String[]{recordIdentity})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            return Optional.of(readRecord(cursor));
        }
    }

    public List<EditorialLineageRecord> listByRunEvaluationIdentity(String runEvaluationIdentity) {
        if (runEvaluationIdentity == null || runEvaluationIdentity.isBlank()) return List.of();
        return list(RECORD_SELECT + " WHERE run_evaluation_identity=? ORDER BY record_identity ASC",
                new String[]{runEvaluationIdentity});
    }

    public List<EditorialLineageRecord> listChildrenByParentIdentity(String parentRecordIdentity) {
        if (parentRecordIdentity == null || parentRecordIdentity.isBlank()) return List.of();
        return list(RECORD_SELECT + " WHERE parent_record_identity=? ORDER BY record_identity ASC",
                new String[]{parentRecordIdentity});
    }

    private List<EditorialLineageRecord> list(String sql, String[] args) {
        ArrayList<EditorialLineageRecord> records = new ArrayList<>();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(sql, args)) {
            while (cursor.moveToNext()) records.add(readRecord(cursor));
        }
        return List.copyOf(records);
    }

    private EditorialLineageRecord readRecord(Cursor cursor) {
        return EditorialLineageRecordRowMapper.readRecord(cursor,
                readInputEntries(cursor.getString(0)));
    }

    private List<EditorialLineageInputEntry> readInputEntries(String recordIdentity) {
        ArrayList<EditorialLineageInputEntry> entries = new ArrayList<>();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT record_identity,role,ordinal,input_hash,byte_count,item_count FROM " + ENTRY_TABLE
                        + " WHERE record_identity=? ORDER BY role ASC,ordinal ASC",
                new String[]{recordIdentity})) {
            while (cursor.moveToNext()) entries.add(EditorialLineageRecordRowMapper.readInputEntry(cursor));
        }
        return List.copyOf(entries);
    }

    private static ContentValues recordValues(EditorialLineageRecord record, long auditTimestamp) {
        ContentValues values = new ContentValues();
        values.put("record_identity", record.recordIdentity());
        values.put("record_fingerprint", record.recordFingerprint());
        values.put("canonical_pack_hash", record.identity().canonicalPackHash());
        values.put("trusted_profile_id", record.identity().trustedProfileId());
        values.put("trusted_profile_version", record.identity().trustedProfileVersion());
        values.put("canonical_profile_hash", record.identity().canonicalProfileHash());
        values.put("machine_contract_fingerprint", record.identity().machineContractFingerprint());
        values.put("contract_version", record.identity().contractVersion());
        values.put("schema_version", record.identity().schemaVersion());
        values.put("project_identity", record.identity().projectIdentity());
        values.put("input_scope_identity", record.identity().inputScopeIdentity());
        values.put("run_evaluation_identity", record.identity().runEvaluationIdentity());
        values.put("input_manifest_version", record.identity().inputManifest().manifestVersion());
        values.put("input_manifest_fingerprint", record.inputManifestFingerprint());
        values.put("node_kind", record.nodeKind().name());
        EditorialLineageParentReference parent = record.parentReference();
        if (parent == null) {
            values.putNull("parent_record_identity");
            values.putNull("parent_record_fingerprint");
        } else {
            values.put("parent_record_identity", parent.parentRecordIdentity());
            values.put("parent_record_fingerprint", parent.parentRecordFingerprint());
        }
        values.put("created_at", auditTimestamp);
        return values;
    }

    private static ContentValues entryValues(String recordIdentity, EditorialLineageInputEntry entry) {
        ContentValues values = new ContentValues();
        values.put("record_identity", recordIdentity);
        values.put("role", entry.role());
        values.put("ordinal", entry.ordinal());
        values.put("input_hash", entry.inputHash());
        values.put("byte_count", entry.byteCount());
        values.put("item_count", entry.itemCount());
        return values;
    }

    private EditorialLineageAppendResult classifyExisting(EditorialLineageRecord existing,
                                                           EditorialLineageRecord incoming) {
        if (existing.equals(incoming)) {
            return result(EditorialLineagePersistenceCode.ALREADY_EXISTS, incoming.recordIdentity(),
                    "same-immutable-record");
        }
        if (!Objects.equals(existing.parentReference(), incoming.parentReference())) {
            return result(EditorialLineagePersistenceCode.REPARENT_ATTEMPT, incoming.recordIdentity(),
                    "parent-reference-changed");
        }
        return result(EditorialLineagePersistenceCode.DUPLICATE_LINEAGE, incoming.recordIdentity(),
                "identity-content-or-fingerprint-changed");
    }

    private EditorialLineageAppendResult classifyValidation(EditorialLineageRecord record,
                                                             EditorialLineageValidationContext context,
                                                             EditorialLineageValidationResult validation) {
        List<EditorialLineageValidationCode> codes = validation.failureCodes();
        if (codes.contains(EditorialLineageValidationCode.PARENT_MISMATCH)) {
            return result(EditorialLineagePersistenceCode.PARENT_MISMATCH, record.recordIdentity(),
                    EditorialLineageValidationCode.PARENT_MISMATCH.name());
        }
        if (record.nodeKind() == EditorialLineageNodeKind.CHILD && record.parentReference() != null
                && context.findByRecordIdentity(record.parentReference().parentRecordIdentity()).isEmpty()
                && codes.contains(EditorialLineageValidationCode.MISSING_PARENT)) {
            return result(EditorialLineagePersistenceCode.ORPHAN_LINEAGE, record.recordIdentity(),
                    EditorialLineageValidationCode.ORPHAN_LINEAGE.name());
        }
        if (codes.contains(EditorialLineageValidationCode.AMBIGUOUS_PARENT)) {
            return result(EditorialLineagePersistenceCode.AMBIGUOUS_PARENT, record.recordIdentity(),
                    EditorialLineageValidationCode.AMBIGUOUS_PARENT.name());
        }
        if (codes.contains(EditorialLineageValidationCode.REPARENT_ATTEMPT)) {
            return result(EditorialLineagePersistenceCode.REPARENT_ATTEMPT, record.recordIdentity(),
                    EditorialLineageValidationCode.REPARENT_ATTEMPT.name());
        }
        if (codes.contains(EditorialLineageValidationCode.DUPLICATE_LINEAGE)) {
            return result(EditorialLineagePersistenceCode.DUPLICATE_LINEAGE, record.recordIdentity(),
                    EditorialLineageValidationCode.DUPLICATE_LINEAGE.name());
        }
        if (codes.contains(EditorialLineageValidationCode.ORPHAN_LINEAGE)) {
            return result(EditorialLineagePersistenceCode.ORPHAN_LINEAGE, record.recordIdentity(),
                    EditorialLineageValidationCode.ORPHAN_LINEAGE.name());
        }
        return result(EditorialLineagePersistenceCode.INVALID_LINEAGE, record.recordIdentity(),
                validation.code().name());
    }

    private static EditorialLineageAppendResult result(EditorialLineagePersistenceCode code,
                                                        String recordIdentity,
                                                        String detail) {
        return EditorialLineageAppendResult.of(code, recordIdentity, null, detail);
    }
}
