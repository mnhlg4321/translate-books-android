package com.ml.tblandroidtxt;

/** The Vietnamese label of a job status, as the jobs list shows it on the phone and on the PC. */
public final class JobStatusText {
    private JobStatusText() {}

    public static String of(String raw) {
        switch (JobStatus.classify(raw)) {
            case FINISHED: return "Xong";
            case CANCELLED: return "Đã hủy";
            case DISMISSED: return "Đã bỏ";
            case NEEDS_REVIEW: return "Cần xem lại";
            case CONFLICT: return "Input đã đổi";
            case CONTINUABLE: return "error".equalsIgnoreCase(raw) ? "Lỗi" : "Tạm dừng";
            case ACTIVE: return "Đang dịch";
            default: return raw == null || raw.trim().isEmpty() ? "Không rõ" : raw.trim();
        }
    }
}
