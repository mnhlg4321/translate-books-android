package com.ml.tblandroidtxt;

/** Last accepted chunk of a run, with its own source identity across chapter transitions. */
final class AcceptedTranslationPreview {
    private String currentFile = "";
    private int currentTotal;
    String text = "", file = "";
    int chunk, total;

    void beginRun() {
        text = file = currentFile = "";
        chunk = total = currentTotal = 0;
    }

    void beginFile(String name, int count) {
        currentFile = name == null ? "" : name;
        currentTotal = count;
        // A chapter beginning (or failing) is not a new accepted translation.
    }

    void accept(String translated, int index, int count) {
        if (translated == null || translated.trim().isEmpty()) return;
        int start = Math.max(0, translated.length() - 1200);
        if (start > 0 && Character.isLowSurrogate(translated.charAt(start))) start--;
        text = translated.substring(start);
        file = currentFile;
        chunk = index;
        total = count > 0 ? count : currentTotal;
    }
}
