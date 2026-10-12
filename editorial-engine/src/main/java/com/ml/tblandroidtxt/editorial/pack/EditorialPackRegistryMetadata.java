package com.ml.tblandroidtxt.editorial.pack;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Immutable persisted facts exposed by the read-only pack registry. */
public final class EditorialPackRegistryMetadata {
    public enum StorageState {
        STORED_READY_FOR_CERTIFICATION,
        STORED_BLOCKED,
        UNKNOWN
    }

    public enum IntegrityState {
        VALID,
        INVALID,
        MISSING_STORAGE,
        UNAVAILABLE
    }

    public record CompatibilitySnapshot(
            EditorialPackCompatibilityClass classification,
            EditorialPackCompatibilityClass requiredClass,
            String machineContractFingerprint,
            String engineVersionUsed,
            String blockedReason,
            Set<String> missingCapabilities,
            long evaluatedAt) {
        public CompatibilitySnapshot {
            classification = classification == null ? EditorialPackCompatibilityClass.BLOCKED : classification;
            requiredClass = requiredClass == null ? EditorialPackCompatibilityClass.BLOCKED : requiredClass;
            machineContractFingerprint = safe(machineContractFingerprint);
            engineVersionUsed = safe(engineVersionUsed);
            blockedReason = safe(blockedReason);
            missingCapabilities = immutable(missingCapabilities);
        }
    }

    private final StorageState storageState;
    private final String storageKey;
    private final String blockedReason;
    private final long createdAt;
    private final long validatedAt;
    private final String engineVersionUsed;
    private final IntegrityState integrityState;
    private final String integrityReason;
    private final CompatibilitySnapshot latestCompatibility;

    public EditorialPackRegistryMetadata(StorageState storageState, String storageKey, String blockedReason,
                                         long createdAt, long validatedAt, String engineVersionUsed,
                                         IntegrityState integrityState, String integrityReason,
                                         CompatibilitySnapshot latestCompatibility) {
        this.storageState = storageState == null ? StorageState.UNKNOWN : storageState;
        this.storageKey = safe(storageKey);
        this.blockedReason = safe(blockedReason);
        this.createdAt = createdAt;
        this.validatedAt = validatedAt;
        this.engineVersionUsed = safe(engineVersionUsed);
        this.integrityState = integrityState == null ? IntegrityState.UNAVAILABLE : integrityState;
        this.integrityReason = safe(integrityReason);
        this.latestCompatibility = latestCompatibility;
    }

    public StorageState storageState() { return storageState; }
    public String storageKey() { return storageKey; }
    public String blockedReason() { return blockedReason; }
    public long createdAt() { return createdAt; }
    public long validatedAt() { return validatedAt; }
    public String engineVersionUsed() { return engineVersionUsed; }
    public IntegrityState integrityState() { return integrityState; }
    public String integrityReason() { return integrityReason; }
    public CompatibilitySnapshot latestCompatibility() { return latestCompatibility; }

    public Set<String> missingCapabilities() {
        return latestCompatibility == null ? Set.of() : latestCompatibility.missingCapabilities();
    }

    private static Set<String> immutable(Set<String> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values == null ? Set.of() : values));
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
