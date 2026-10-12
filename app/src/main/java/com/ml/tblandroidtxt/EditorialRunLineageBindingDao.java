package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.ml.tblandroidtxt.editorial.pack.EditorialRunLineageBinding;

import java.util.Objects;
import java.util.Optional;

/** Append/read-only one-to-one binding DAO with a package-private transaction seam. */
public final class EditorialRunLineageBindingDao {
    private static final String TABLE = "editorial_run_lineage_bindings";
    private static final String SELECT = "SELECT binding_identity,closed_run_identity,lineage_record_identity,"
            + "lineage_record_fingerprint,binding_contract_version,bound_at FROM " + TABLE;
    private final TranslationRepository database;

    public EditorialRunLineageBindingDao(TranslationRepository database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public EditorialIdentityAppendResult<EditorialRunLineageBinding> append(
            EditorialRunLineageBinding binding, long boundAt) {
        if (binding == null || boundAt < 0) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.INVALID_IMMUTABLE_RECORD,
                    null, binding == null ? "" : binding.bindingIdentity(), "binding-or-timestamp-invalid");
        }
        SQLiteDatabase db = database.editorialWritableDatabase();
        try {
            db.beginTransaction();
            try {
                EditorialIdentityAppendResult<EditorialRunLineageBinding> result =
                        appendInTransaction(db, binding, boundAt);
                if (result.code() == EditorialIdentityPersistenceCode.APPENDED
                        || result.code() == EditorialIdentityPersistenceCode.ALREADY_EXISTS) {
                    db.setTransactionSuccessful();
                }
                return result;
            } finally {
                db.endTransaction();
            }
        } catch (SQLiteConstraintException error) {
            Optional<EditorialRunLineageBinding> existing = findByIdentity(binding.bindingIdentity());
            if (existing.isPresent() && existing.get().equals(binding)) {
                return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.ALREADY_EXISTS,
                        existing.get(), binding.bindingIdentity(), "same-binding-after-race");
            }
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE,
                    null, binding.bindingIdentity(), "binding-transaction-rollback");
        } catch (SQLiteException | IllegalStateException error) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE,
                    null, binding.bindingIdentity(), "binding-transaction-rollback");
        }
    }

    /** Transaction-scoped operation; caller must own the open transaction and SQLite connection. */
    EditorialIdentityAppendResult<EditorialRunLineageBinding> appendInTransaction(
            SQLiteDatabase db, EditorialRunLineageBinding binding, long boundAt) {
        if (db == null || !db.inTransaction()) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE,
                    null, binding == null ? "" : binding.bindingIdentity(), "transaction-required");
        }
        Optional<EditorialRunLineageBinding> exact = findByIdentity(binding.bindingIdentity());
        if (exact.isPresent()) {
            return exact.get().equals(binding)
                    ? EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.ALREADY_EXISTS,
                    exact.get(), binding.bindingIdentity(), "same-binding")
                    : EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.DUPLICATE_IMMUTABLE_RECORD,
                    null, binding.bindingIdentity(), "binding-identity-content-changed");
        }
        Optional<EditorialRunLineageBinding> byRun = findByClosedRunIdentity(binding.closedRunIdentity());
        if (byRun.isPresent()) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.BINDING_MISMATCH,
                    null, binding.bindingIdentity(), "closed-run-already-bound");
        }
        Optional<EditorialRunLineageBinding> byLineage = findByLineageRecordIdentity(binding.lineageRecordIdentity());
        if (byLineage.isPresent()) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.BINDING_MISMATCH,
                    null, binding.bindingIdentity(), "lineage-record-already-bound");
        }
        if (!EditorialIdentityDaoSupport.hasRow(db, "editorial_closed_run_contexts", "closed_run_identity",
                binding.closedRunIdentity())) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.RUN_CONTEXT_REQUIRED,
                    null, binding.bindingIdentity(), "closed-run-not-found");
        }
        try (Cursor lineage = db.rawQuery(
                "SELECT record_fingerprint FROM editorial_lineage_records WHERE record_identity=?",
                new String[]{binding.lineageRecordIdentity()})) {
            if (!lineage.moveToFirst()) {
                return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.IDENTITY_UNATTESTED,
                        null, binding.bindingIdentity(), "lineage-record-not-found");
            }
            if (!binding.lineageRecordFingerprint().equals(lineage.getString(0))) {
                return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.BINDING_MISMATCH,
                        null, binding.bindingIdentity(), "lineage-fingerprint-mismatch");
            }
        }
        ContentValues row = new ContentValues();
        row.put("binding_identity", binding.bindingIdentity());
        row.put("closed_run_identity", binding.closedRunIdentity());
        row.put("lineage_record_identity", binding.lineageRecordIdentity());
        row.put("lineage_record_fingerprint", binding.lineageRecordFingerprint());
        row.put("binding_contract_version", binding.bindingContractVersion());
        row.put("bound_at", boundAt);
        db.insertOrThrow(TABLE, null, row);
        return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.APPENDED,
                binding, binding.bindingIdentity(), "appended");
    }

    public Optional<EditorialRunLineageBinding> findByIdentity(String identity) {
        if (identity == null || identity.isBlank()) return Optional.empty();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                SELECT + " WHERE binding_identity=?", new String[]{identity})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            return Optional.of(read(cursor));
        } catch (RuntimeException invalidStoredRow) { return Optional.empty(); }
    }

    public Optional<EditorialRunLineageBinding> findByClosedRunIdentity(String identity) {
        return findOne("closed_run_identity", identity);
    }

    public Optional<EditorialRunLineageBinding> findByLineageRecordIdentity(String identity) {
        return findOne("lineage_record_identity", identity);
    }

    private Optional<EditorialRunLineageBinding> findOne(String column, String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                SELECT + " WHERE " + column + "=?", new String[]{value})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            return Optional.of(read(cursor));
        } catch (RuntimeException invalidStoredRow) { return Optional.empty(); }
    }

    private EditorialRunLineageBinding read(Cursor cursor) {
        EditorialRunLineageBinding binding = new EditorialRunLineageBinding(
                cursor.getString(4), cursor.getString(1), cursor.getString(2), cursor.getString(3));
        if (!binding.bindingIdentity().equals(cursor.getString(0))) {
            throw new IllegalArgumentException("stored binding canonical bytes mismatch");
        }
        return binding;
    }
}
