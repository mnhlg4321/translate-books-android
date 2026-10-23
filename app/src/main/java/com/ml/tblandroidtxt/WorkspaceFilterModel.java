package com.ml.tblandroidtxt;

/** Pure presentation rules for the persistent Jobs workspace filter chip. */
public final class WorkspaceFilterModel {
    private WorkspaceFilterModel() {}

    public static boolean active(String setId) {
        return setId != null && !setId.trim().isEmpty();
    }

    /** Never calls an unresolved active filter "Tất cả" while the workspace is loading. */
    public static String label(String setId, String resolvedName) {
        if (!active(setId)) return "Tất cả";
        if (resolvedName != null && !resolvedName.trim().isEmpty()) return resolvedName.trim();
        return "Đang lọc";
    }

    public static String chipText(String setId, String resolvedName) {
        return "Tập: " + label(setId, resolvedName) + (active(setId) ? "  ×" : "");
    }
}
