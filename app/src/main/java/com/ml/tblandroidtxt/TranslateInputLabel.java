package com.ml.tblandroidtxt;

/** User-facing labels for the two one-chapter input pickers. */
final class TranslateInputLabel {
    private TranslateInputLabel() {}

    static String empty(boolean library) {
        return library ? "Chưa chọn RAW" : "Chưa chọn file TXT";
    }

    static String librarySelection(String rawName, String chapterLabel) {
        String name = rawName == null ? "" : rawName.trim();
        String chapter = chapterLabel == null ? "" : chapterLabel.trim();
        if (chapter.isEmpty()) return "📄 " + name;
        if (name.isEmpty()) return "📄 " + chapter;
        return "📄 " + name + " · " + chapter;
    }
}
