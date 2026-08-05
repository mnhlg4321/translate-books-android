package com.ml.tblandroidtxt;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.ml.tblandroidtxt.editorial.pack.EditorialLineageRecord;
import com.ml.tblandroidtxt.editorial.pack.EditorialRunLineageBinding;

import java.util.Objects;
import java.util.Optional;

/** Application transaction seam: one SQLite connection for lineage append and binding. */
public final class EditorialLineageAndBindingTransactionService {
    private final TranslationRepository database;
    private final EditorialLineageDao lineageDao;
    private final EditorialRunLineageBindingDao bindingDao;

    public EditorialLineageAndBindingTransactionService(TranslationRepository database) {
        this.database = Objects.requireNonNull(database, "database");
        this.lineageDao = new EditorialLineageDao(database);
        this.bindingDao = new EditorialRunLineageBindingDao(database);
    }

    public synchronized EditorialLineageAndBindingResult appendAndBind(
            EditorialLineageRecord record,
            EditorialRunLineageBinding binding,
            long lineageAuditTimestamp,
            long boundAt) {
        if (record == null || binding == null
                || !record.recordIdentity().equals(binding.lineageRecordIdentity())) {
            return failure(EditorialIdentityPersistenceCode.BINDING_MISMATCH,
                    null, null, "lineage-binding-identity-mismatch");
        }
        SQLiteDatabase db = database.editorialWritableDatabase();
        try {
            db.beginTransaction();
            try {
                Optional<EditorialLineageRecord> before = lineageDao.findByRecordIdentity(record.recordIdentity());
                EditorialLineageAppendResult lineageResult = lineageDao.appendInTransaction(
                        db, record, lineageAuditTimestamp);
                if (lineageResult.code() != EditorialLineagePersistenceCode.APPENDED
                        && lineageResult.code() != EditorialLineagePersistenceCode.ALREADY_EXISTS) {
                    return failure(EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE,
                            lineageResult, null, "lineage-append-rejected");
                }
                Optional<EditorialLineageRecord> readback = lineageDao.findByRecordIdentity(record.recordIdentity());
                if (readback.isEmpty()
                        || !record.recordIdentity().equals(readback.get().recordIdentity())
                        || !record.recordFingerprint().equals(readback.get().recordFingerprint())) {
                    return failure(EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE,
                            lineageResult, null, "lineage-fingerprint-readback-mismatch");
                }
                EditorialIdentityAppendResult<?> bindingResult = bindingDao.appendInTransaction(
                        db, binding, boundAt);
                if (bindingResult.code() != EditorialIdentityPersistenceCode.APPENDED
                        && bindingResult.code() != EditorialIdentityPersistenceCode.ALREADY_EXISTS) {
                    return failure(bindingResult.code(), lineageResult, bindingResult,
                            before.isPresent() ? "existing-lineage-binding-rollback" : "new-lineage-binding-rollback");
                }
                db.setTransactionSuccessful();
                EditorialIdentityPersistenceCode code = bindingResult.code()
                        == EditorialIdentityPersistenceCode.ALREADY_EXISTS
                        ? EditorialIdentityPersistenceCode.ALREADY_EXISTS
                        : EditorialIdentityPersistenceCode.APPENDED;
                return new EditorialLineageAndBindingResult(code, lineageResult, bindingResult,
                        true, before.isPresent() ? "existing-lineage-bound" : "lineage-and-binding-committed");
            } finally {
                db.endTransaction();
            }
        } catch (SQLiteException | IllegalStateException error) {
            return failure(EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE,
                    null, null, "shared-transaction-rollback");
        }
    }

    private EditorialLineageAndBindingResult failure(
            EditorialIdentityPersistenceCode code,
            EditorialLineageAppendResult lineageResult,
            EditorialIdentityAppendResult<?> bindingResult,
            String detail) {
        return new EditorialLineageAndBindingResult(code, lineageResult, bindingResult, false, detail);
    }
}
