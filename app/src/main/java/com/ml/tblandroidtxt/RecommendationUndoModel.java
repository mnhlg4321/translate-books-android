package com.ml.tblandroidtxt;

/** Pure presentation state for the in-memory recommendation undo snapshot. */
final class RecommendationUndoModel {
    private RecommendationUndoModel() {}

    static boolean isEnabled(boolean hasSnapshot) {
        return hasSnapshot;
    }

    static String helperText(boolean hasSnapshot) {
        return hasSnapshot ? "" : "Hoàn tác dùng được sau khi bấm Áp dụng";
    }

    static String contentDescription(boolean hasSnapshot) {
        return hasSnapshot
                ? "Hoàn tác cài đặt khuyến nghị"
                : "Hoàn tác cài đặt khuyến nghị, chưa dùng được: bấm Áp dụng trước";
    }
}
