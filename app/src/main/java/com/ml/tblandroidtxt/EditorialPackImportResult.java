package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;

import java.util.Collections;
import java.util.Set;

public record EditorialPackImportResult(
        String importId,
        EditorialPackImportState state,
        EditorialPackImportError error,
        String blockedReason,
        String packId,
        String version,
        String canonicalPackHash,
        EditorialPackCompatibilityClass compatibilityClass,
        String storageKey,
        Set<String> missingCapabilities,
        boolean alreadyExisted) {
    public EditorialPackImportResult(String importId, EditorialPackImportState state, EditorialPackImportError error,
                                     String blockedReason, String packId, String version, String canonicalPackHash,
                                     EditorialPackCompatibilityClass compatibilityClass, String storageKey) {
        this(importId, state, error, blockedReason, packId, version, canonicalPackHash, compatibilityClass, storageKey, Set.of());
    }

    public EditorialPackImportResult {
        missingCapabilities = Collections.unmodifiableSet(Set.copyOf(missingCapabilities == null ? Set.of() : missingCapabilities));
    }

    public EditorialPackImportResult(String importId, EditorialPackImportState state, EditorialPackImportError error,
                                     String blockedReason, String packId, String version, String canonicalPackHash,
                                     EditorialPackCompatibilityClass compatibilityClass, String storageKey,
                                     Set<String> missingCapabilities) {
        this(importId, state, error, blockedReason, packId, version, canonicalPackHash, compatibilityClass,
                storageKey, missingCapabilities, false);
    }
    public boolean stored() {
        return state == EditorialPackImportState.STORED_BLOCKED || state == EditorialPackImportState.STORED_READY_FOR_CERTIFICATION;
    }

    public boolean readyForCertification() {
        return state == EditorialPackImportState.STORED_READY_FOR_CERTIFICATION
                && compatibilityClass == EditorialPackCompatibilityClass.DATA_COMPATIBLE;
    }
}
