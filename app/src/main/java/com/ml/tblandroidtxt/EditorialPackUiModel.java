package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackRegistryMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Pure data used by the read-only Editorial Pack list/detail surface. */
public final class EditorialPackUiModel {
    public record FileRow(String role, String path, long byteLength, String sha256) {}

    private final String displayName;
    private final String packId;
    private final String version;
    private final String canonicalHash;
    private final String shortHash;
    private final String contractVersion;
    private final String schemaVersion;
    private final String minimumEngineVersion;
    private final EditorialPackCompatibilityClass compatibilityClass;
    private final String compatibilityLabel;
    private final EditorialPackRegistryMetadata.StorageState storageState;
    private final String storageLabel;
    private final String blockedReason;
    private final Set<String> requiredCapabilities;
    private final Set<String> missingCapabilities;
    private final String machineContractFingerprint;
    private final String engineVersionUsed;
    private final String integrityLabel;
    private final String integrityReason;
    private final long createdAt;
    private final long validatedAt;
    private final long compatibilityEvaluatedAt;
    private final List<FileRow> files;

    EditorialPackUiModel(String displayName, String packId, String version, String canonicalHash, String shortHash,
                         String contractVersion, String schemaVersion, String minimumEngineVersion,
                         EditorialPackCompatibilityClass compatibilityClass, String compatibilityLabel,
                         EditorialPackRegistryMetadata.StorageState storageState, String storageLabel,
                         String blockedReason, Set<String> requiredCapabilities, Set<String> missingCapabilities,
                         String machineContractFingerprint, String engineVersionUsed, String integrityLabel,
                         String integrityReason, long createdAt, long validatedAt, long compatibilityEvaluatedAt,
                         List<FileRow> files) {
        this.displayName = safe(displayName); this.packId = safe(packId); this.version = safe(version);
        this.canonicalHash = safe(canonicalHash); this.shortHash = safe(shortHash);
        this.contractVersion = safe(contractVersion); this.schemaVersion = safe(schemaVersion);
        this.minimumEngineVersion = safe(minimumEngineVersion);
        this.compatibilityClass = compatibilityClass == null ? EditorialPackCompatibilityClass.BLOCKED : compatibilityClass;
        this.compatibilityLabel = safe(compatibilityLabel);
        this.storageState = storageState == null ? EditorialPackRegistryMetadata.StorageState.UNKNOWN : storageState;
        this.storageLabel = safe(storageLabel); this.blockedReason = safe(blockedReason);
        this.requiredCapabilities = immutable(requiredCapabilities); this.missingCapabilities = immutable(missingCapabilities);
        this.machineContractFingerprint = safe(machineContractFingerprint); this.engineVersionUsed = safe(engineVersionUsed);
        this.integrityLabel = safe(integrityLabel); this.integrityReason = safe(integrityReason);
        this.createdAt = createdAt; this.validatedAt = validatedAt; this.compatibilityEvaluatedAt = compatibilityEvaluatedAt;
        this.files = List.copyOf(files == null ? List.of() : files);
    }

    public String displayName() { return displayName; }
    public String packId() { return packId; }
    public String version() { return version; }
    public String canonicalHash() { return canonicalHash; }
    public String shortHash() { return shortHash; }
    public String contractVersion() { return contractVersion; }
    public String schemaVersion() { return schemaVersion; }
    public String minimumEngineVersion() { return minimumEngineVersion; }
    public EditorialPackCompatibilityClass compatibilityClass() { return compatibilityClass; }
    public String compatibilityLabel() { return compatibilityLabel; }
    public EditorialPackRegistryMetadata.StorageState storageState() { return storageState; }
    public String storageLabel() { return storageLabel; }
    public String blockedReason() { return blockedReason; }
    public Set<String> requiredCapabilities() { return requiredCapabilities; }
    public Set<String> missingCapabilities() { return missingCapabilities; }
    public String machineContractFingerprint() { return machineContractFingerprint; }
    public String engineVersionUsed() { return engineVersionUsed; }
    public String integrityLabel() { return integrityLabel; }
    public String integrityReason() { return integrityReason; }
    public long createdAt() { return createdAt; }
    public long validatedAt() { return validatedAt; }
    public long compatibilityEvaluatedAt() { return compatibilityEvaluatedAt; }
    public List<FileRow> files() { return files; }

    /** Deliberately empty: this model exposes no mutation action or callback. */
    public List<String> mutationActions() { return Collections.emptyList(); }
    public boolean readOnly() { return true; }

    public static String shortHashOf(String hash) {
        String value = safe(hash);
        return value.length() <= 12 ? value : value.substring(0, 12) + "…";
    }

    private static Set<String> immutable(Set<String> values) { return Set.copyOf(values == null ? Set.of() : values); }
    private static String safe(String value) { return value == null ? "" : value; }
}
