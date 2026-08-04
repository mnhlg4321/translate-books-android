package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;

import java.util.ArrayList;
import java.util.Collections;

/** Centralizes user-facing, fail-closed wording for ZIP import outcomes. */
public final class EditorialPackImportResultMapper {
    private EditorialPackImportResultMapper() { }

    public static EditorialPackImportUiState progress(EditorialPackImportState state) {
        if (state == EditorialPackImportState.SNAPSHOTTED) {
            return EditorialPackImportUiState.phase(EditorialPackImportUiState.Phase.INTEGRITY_CHECKING,
                    "Đang kiểm tra integrity", "Snapshot đã hoàn tất; đang kiểm tra manifest, file role và hash.");
        }
        if (state == EditorialPackImportState.INTEGRITY_VALIDATED) {
            return EditorialPackImportUiState.phase(EditorialPackImportUiState.Phase.COMPATIBILITY_CHECKING,
                    "Đang đánh giá compatibility", "Đang đối chiếu machine contract và capability đã tin cậy.");
        }
        if (state == EditorialPackImportState.COMPATIBILITY_EVALUATED) {
            return EditorialPackImportUiState.phase(EditorialPackImportUiState.Phase.STORING,
                    "Đang lưu bất biến", "Đang commit storage content-addressed và registry.");
        }
        if (state == EditorialPackImportState.STORED_READY_FOR_CERTIFICATION) {
            return EditorialPackImportUiState.phase(EditorialPackImportUiState.Phase.READY_FOR_CERTIFICATION,
                    "Đã nhập và lưu bất biến • chờ chứng nhận", "");
        }
        if (state == EditorialPackImportState.STORED_BLOCKED) {
            return EditorialPackImportUiState.phase(EditorialPackImportUiState.Phase.STORED_BLOCKED,
                    "Pack đã được lưu nhưng đang bị khóa", "");
        }
        return EditorialPackImportUiState.phase(EditorialPackImportUiState.Phase.SNAPSHOTTING,
                "Đang snapshot", "Đang đọc stream một lần vào pipeline import riêng của ứng dụng.");
    }

    public static EditorialPackImportUiState result(EditorialPackImportResult result) {
        if (result == null) return EditorialPackImportUiState.phase(EditorialPackImportUiState.Phase.ERROR,
                "Không thể nhập pack", "Importer không trả về kết quả.");
        if (result.state() == EditorialPackImportState.STORED_READY_FOR_CERTIFICATION
                && result.compatibilityClass() == EditorialPackCompatibilityClass.DATA_COMPATIBLE) {
            String detail = identity(result) + "\nDATA_COMPATIBLE\n"
                    + (result.alreadyExisted() ? "Không tạo bản mutable mới; registry giữ nguyên.\n" : "")
                    + "Pack chưa được chứng nhận và chưa thể chạy biên tập";
            return EditorialPackImportUiState.result(EditorialPackImportUiState.Phase.READY_FOR_CERTIFICATION,
                    result.alreadyExisted() ? "Pack đã tồn tại" : "Đã nhập và lưu bất biến • chờ chứng nhận", detail, result);
        }
        if (result.state() == EditorialPackImportState.STORED_BLOCKED) {
            String detail = identity(result) + "\n" + classLabel(result.compatibilityClass())
                    + "\n" + reason(result.blockedReason())
                    + missing(result)
                    + "\nPack chưa được chứng nhận và chưa thể chạy biên tập";
            return EditorialPackImportUiState.result(EditorialPackImportUiState.Phase.STORED_BLOCKED,
                    "Pack đã được lưu nhưng đang bị khóa", detail, result);
        }
        String detail = reason(result.error() + (result.blockedReason().isEmpty() ? "" : ": " + result.blockedReason()));
        return EditorialPackImportUiState.result(EditorialPackImportUiState.Phase.INVALID,
                "Không thể nhập pack", detail, result);
    }

    private static String identity(EditorialPackImportResult result) {
        return "packId=" + safe(result.packId()) + " • version=" + safe(result.version())
                + " • canonical hash=" + safe(result.canonicalPackHash());
    }

    private static String classLabel(EditorialPackCompatibilityClass compatibility) {
        if (compatibility == EditorialPackCompatibilityClass.ADAPTER_REQUIRED) return "ADAPTER_REQUIRED • cần adapter, không thể kích hoạt";
        if (compatibility == EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED) return "ENGINE_UPGRADE_REQUIRED • cần nâng engine/APK, không thể kích hoạt";
        if (compatibility == EditorialPackCompatibilityClass.DATA_COMPATIBLE) return "DATA_COMPATIBLE • tương thích contract nhưng chưa chứng nhận";
        if (compatibility == EditorialPackCompatibilityClass.INVALID) return "INVALID • không hợp lệ";
        return compatibility == null ? "BLOCKED • bị khóa" : compatibility.name() + " • bị khóa";
    }

    private static String reason(String value) { return value == null || value.isBlank() ? "Lý do chưa được cung cấp; fail-closed." : value; }
    private static String missing(EditorialPackImportResult result) {
        if (result.missingCapabilities() == null || result.missingCapabilities().isEmpty()) return "";
        ArrayList<String> values = new ArrayList<>(result.missingCapabilities());
        Collections.sort(values);
        return "\nMissing capabilities: " + String.join(", ", values);
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
