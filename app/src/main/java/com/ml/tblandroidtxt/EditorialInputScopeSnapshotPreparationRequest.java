package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshotEntry;
import com.ml.tblandroidtxt.editorial.pack.EditorialRequiredInputRoleContract;

import java.util.List;

/** Immutable caller input for one explicit, complete input-scope snapshot. */
public record EditorialInputScopeSnapshotPreparationRequest(
        String projectRevisionIdentity,
        String scopeCanonicalVersion,
        String scopeKey,
        EditorialRequiredInputRoleContract requiredRoleContract,
        String manifestVersion,
        List<EditorialInputScopeSnapshotEntry> entries,
        Long sourceChapterRowId,
        long createdAt) {
    public EditorialInputScopeSnapshotPreparationRequest {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
