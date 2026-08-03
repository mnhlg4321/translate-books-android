package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackRegistryMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Maps a registry manifest plus its persisted metadata into presentation-only data. */
public final class EditorialPackUiMapper {
    public EditorialPackUiModel map(EditorialPackManifest manifest) {
        if (manifest == null) throw new IllegalArgumentException("Pack manifest is required");
        EditorialPackRegistryMetadata metadata = manifest.registryMetadata().orElse(null);
        EditorialPackRegistryMetadata.CompatibilitySnapshot compatibility = metadata == null ? null : metadata.latestCompatibility();
        EditorialPackCompatibilityClass compatibilityClass = compatibility == null
                ? manifest.declaredCompatibilityClass() : compatibility.classification();
        String blockedReason = firstNonBlank(metadata == null ? "" : metadata.blockedReason(), compatibility == null ? "" : compatibility.blockedReason());
        Set<String> missing = compatibility == null ? Set.of() : compatibility.missingCapabilities();
        List<EditorialPackUiModel.FileRow> files = new ArrayList<>();
        for (EditorialPackManifest.FileEntry file : manifest.fileEntries()) {
            files.add(new EditorialPackUiModel.FileRow(file.role().name(), file.path(), file.byteLength(), file.sha256()));
        }
        String machineFingerprint = compatibility == null ? manifest.machineContractFingerprint() : compatibility.machineContractFingerprint();
        String engineVersion = compatibility == null ? (metadata == null ? "" : metadata.engineVersionUsed()) : compatibility.engineVersionUsed();
        long compatibilityEvaluatedAt = compatibility == null ? 0L : compatibility.evaluatedAt();
        return new EditorialPackUiModel(manifest.displayName(), manifest.packId(), manifest.version(), manifest.canonicalPackHash(),
                EditorialPackUiModel.shortHashOf(manifest.canonicalPackHash()), manifest.contractVersion(), manifest.schemaVersion(),
                manifest.minimumEngineVersion(), compatibilityClass, compatibilityLabel(compatibilityClass),
                metadata == null ? EditorialPackRegistryMetadata.StorageState.UNKNOWN : metadata.storageState(),
                storageLabel(metadata == null ? EditorialPackRegistryMetadata.StorageState.UNKNOWN : metadata.storageState()),
                blockedReason, manifest.requiredCapabilities(), missing, machineFingerprint, engineVersion,
                integrityLabel(metadata == null ? EditorialPackRegistryMetadata.IntegrityState.UNAVAILABLE : metadata.integrityState()),
                metadata == null ? "" : metadata.integrityReason(), metadata == null ? 0L : metadata.createdAt(),
                metadata == null ? 0L : metadata.validatedAt(), compatibilityEvaluatedAt, files);
    }

    public static String compatibilityLabel(EditorialPackCompatibilityClass value) {
        if (value == EditorialPackCompatibilityClass.DATA_COMPATIBLE) return "Tương thích contract • chưa chứng nhận";
        if (value == EditorialPackCompatibilityClass.ADAPTER_REQUIRED) return "Cần adapter • không thể kích hoạt";
        if (value == EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED) return "Cần nâng engine/APK • không thể kích hoạt";
        if (value == EditorialPackCompatibilityClass.INVALID) return "Không hợp lệ";
        return "Bị khóa";
    }

    public static String storageLabel(EditorialPackRegistryMetadata.StorageState value) {
        if (value == EditorialPackRegistryMetadata.StorageState.STORED_READY_FOR_CERTIFICATION) return "Đã lưu • chờ chứng nhận";
        if (value == EditorialPackRegistryMetadata.StorageState.STORED_BLOCKED) return "Đã lưu • bị khóa";
        return "Trạng thái lưu trữ chưa xác định";
    }

    private static String integrityLabel(EditorialPackRegistryMetadata.IntegrityState value) {
        if (value == EditorialPackRegistryMetadata.IntegrityState.VALID) return "Integrity: hợp lệ";
        if (value == EditorialPackRegistryMetadata.IntegrityState.MISSING_STORAGE) return "Integrity: thiếu immutable storage";
        if (value == EditorialPackRegistryMetadata.IntegrityState.INVALID) return "Integrity: không hợp lệ";
        return "Integrity: chưa có kết quả";
    }

    private static String firstNonBlank(String first, String second) { return first != null && !first.isBlank() ? first : (second == null ? "" : second); }
}
