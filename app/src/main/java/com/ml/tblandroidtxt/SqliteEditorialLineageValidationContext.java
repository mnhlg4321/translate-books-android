package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialLineageRecord;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageValidationContext;

import java.util.List;

/** Read-only SQLite adapter; the validator itself remains pure JVM. */
public final class SqliteEditorialLineageValidationContext implements EditorialLineageValidationContext {
    private final EditorialLineageDao dao;

    public SqliteEditorialLineageValidationContext(EditorialLineageDao dao) {
        if (dao == null) throw new IllegalArgumentException("dao is required");
        this.dao = dao;
    }

    @Override public List<EditorialLineageRecord> findByRecordIdentity(String recordIdentity) {
        try {
            java.util.Optional<EditorialLineageRecord> record = dao.findByRecordIdentity(recordIdentity);
            return record.isPresent() ? List.of(record.get()) : List.of();
        }
        catch (RuntimeException error) { return List.of(); }
    }

    @Override public List<EditorialLineageRecord> findByRunEvaluationIdentity(String runEvaluationIdentity) {
        try { return dao.listByRunEvaluationIdentity(runEvaluationIdentity); }
        catch (RuntimeException error) { return List.of(); }
    }

    @Override public List<EditorialLineageRecord> findChildrenByParentIdentity(String parentRecordIdentity) {
        try { return dao.listChildrenByParentIdentity(parentRecordIdentity); }
        catch (RuntimeException error) { return List.of(); }
    }
}
