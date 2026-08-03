package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;

public record EditorialPackImportResult(
        String importId,
        EditorialPackImportState state,
        EditorialPackImportError error,
        String blockedReason,
        String packId,
        String version,
        String canonicalPackHash,
        EditorialPackCompatibilityClass compatibilityClass,
        String storageKey) {
    public boolean stored() {
        return state == EditorialPackImportState.STORED_BLOCKED || state == EditorialPackImportState.STORED_READY_FOR_CERTIFICATION;
    }

    public boolean readyForCertification() {
        return state == EditorialPackImportState.STORED_READY_FOR_CERTIFICATION
                && compatibilityClass == EditorialPackCompatibilityClass.DATA_COMPATIBLE;
    }
}
