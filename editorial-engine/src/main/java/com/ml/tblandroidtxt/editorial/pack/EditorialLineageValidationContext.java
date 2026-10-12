package com.ml.tblandroidtxt.editorial.pack;

import java.util.List;
import java.util.Optional;

/**
 * Read-only parent/index seam for pure-JVM validation. It has no persistence,
 * filesystem, network or mutation operation.
 */
public interface EditorialLineageValidationContext {
    List<EditorialLineageRecord> findByRecordIdentity(String recordIdentity);

    List<EditorialLineageRecord> findByRunEvaluationIdentity(String runEvaluationIdentity);

    List<EditorialLineageRecord> findChildrenByParentIdentity(String parentRecordIdentity);

    default Optional<EditorialLineageTrustContext> expectedTrustContext() {
        return Optional.empty();
    }

    default boolean forkAllowed() {
        return false;
    }
}
