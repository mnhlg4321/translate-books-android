package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Deterministic RAW-only anchors shared by all runs; it contains no model interpretation. */
public final class EditorialRawMap {
    public static final class Anchor {
        public final String id;
        public final int start;
        public final int end;
        public final String text;
        public final boolean structuralMarker;

        Anchor(String id, int start, int end, String text) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.text = text;
            this.structuralMarker = isStructural(text);
        }
    }

    public final String normalizedRaw;
    public final String sha256;
    public final List<Anchor> anchors;

    private EditorialRawMap(String normalizedRaw, List<Anchor> anchors) {
        this.normalizedRaw = normalizedRaw;
        this.sha256 = HashUtil.sha256(normalizedRaw);
        this.anchors = Collections.unmodifiableList(anchors);
    }

    public static EditorialRawMap create(String raw) {
        String normalized = raw == null ? "" : raw.replace("\r\n", "\n").replace('\r', '\n');
        ArrayList<Anchor> anchors = new ArrayList<>();
        int start = 0;
        int ordinal = 1;
        for (int i = 0; i < normalized.length(); i++) {
            if (normalized.charAt(i) == '\n') {
                int end = i + 1;
                anchors.add(new Anchor(id(ordinal++), start, end, normalized.substring(start, end)));
                start = end;
            }
        }
        if (start < normalized.length() || normalized.isEmpty()) {
            anchors.add(new Anchor(id(ordinal), start, normalized.length(), normalized.substring(start)));
        }
        return new EditorialRawMap(normalized, anchors);
    }

    public String reconstruct() {
        StringBuilder out = new StringBuilder(normalizedRaw.length());
        for (Anchor anchor : anchors) out.append(anchor.text);
        return out.toString();
    }

    private static String id(int ordinal) { return String.format(java.util.Locale.US, "p%06d", ordinal); }
    private static boolean isStructural(String raw) {
        String text = raw == null ? "" : raw.trim();
        return text.matches("[◇◆＊*─—-]{3,}") || text.startsWith("【") || text.startsWith("[");
    }
}
