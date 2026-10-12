package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;

/** Immutable state rendered by the ZIP import page. */
public final class EditorialPackImportUiState {
    public enum Phase {
        IDLE,
        PICKER_OPEN,
        SNAPSHOTTING,
        INTEGRITY_CHECKING,
        COMPATIBILITY_CHECKING,
        STORING,
        READY_FOR_CERTIFICATION,
        STORED_BLOCKED,
        INVALID,
        CANCELLED,
        ERROR
    }

    private final Phase phase;
    private final String title;
    private final String detail;
    private final String importId;
    private final String packId;
    private final String version;
    private final String canonicalHash;
    private final EditorialPackCompatibilityClass compatibilityClass;
    private final String blockedReason;

    private EditorialPackImportUiState(Phase phase, String title, String detail, String importId,
                                       String packId, String version, String canonicalHash,
                                       EditorialPackCompatibilityClass compatibilityClass,
                                       String blockedReason) {
        this.phase = phase;
        this.title = safe(title);
        this.detail = safe(detail);
        this.importId = safe(importId);
        this.packId = safe(packId);
        this.version = safe(version);
        this.canonicalHash = safe(canonicalHash);
        this.compatibilityClass = compatibilityClass;
        this.blockedReason = safe(blockedReason);
    }

    public static EditorialPackImportUiState phase(Phase phase, String title, String detail) {
        return new EditorialPackImportUiState(phase, title, detail, "", "", "", "", null, "");
    }

    public static EditorialPackImportUiState result(Phase phase, String title, String detail,
                                                    EditorialPackImportResult result) {
        return new EditorialPackImportUiState(phase, title, detail,
                result == null ? "" : result.importId(),
                result == null ? "" : result.packId(),
                result == null ? "" : result.version(),
                result == null ? "" : result.canonicalPackHash(),
                result == null ? null : result.compatibilityClass(),
                result == null ? "" : result.blockedReason());
    }

    public Phase phase() { return phase; }
    public String title() { return title; }
    public String detail() { return detail; }
    public String importId() { return importId; }
    public String packId() { return packId; }
    public String version() { return version; }
    public String canonicalHash() { return canonicalHash; }
    public EditorialPackCompatibilityClass compatibilityClass() { return compatibilityClass; }
    public String blockedReason() { return blockedReason; }

    private static String safe(String value) { return value == null ? "" : value; }
}
