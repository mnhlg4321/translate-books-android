package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure identity rules for the work currently selected on Translate.
 *
 * A pending job is useful context even when it belongs to another selection,
 * but it must not change the primary action for the selection that is open.
 */
final class SelectedWorkModel {
    enum Mode { TXT, SET }

    static final class Selection {
        final Mode mode;
        final String setId;
        final List<String> inputHashes;
        final String displayName;

        Selection(Mode mode, String setId, List<String> inputHashes, String displayName) {
            this.mode = mode == null ? Mode.TXT : mode;
            this.setId = safe(setId);
            this.inputHashes = clean(inputHashes);
            this.displayName = safe(displayName);
        }
    }

    static final class Pending {
        final Mode mode;
        final String setId;
        final String inputHash;
        final String fileName;

        Pending(Mode mode, String setId, String inputHash, String fileName) {
            this.mode = mode == null ? Mode.TXT : mode;
            this.setId = safe(setId);
            this.inputHash = safe(inputHash);
            this.fileName = safe(fileName);
        }
    }

    private SelectedWorkModel() {}

    static Selection txt(List<String> hashes, String displayName) {
        return new Selection(Mode.TXT, "", hashes, displayName);
    }

    static Selection set(String setId, List<String> hashes, String displayName) {
        return new Selection(Mode.SET, setId, hashes, displayName);
    }

    static boolean matches(Selection selection, Pending pending) {
        if (selection == null || pending == null || selection.mode != pending.mode) return false;
        if (selection.mode == Mode.SET && !selection.setId.equals(pending.setId)) return false;
        if (!pending.inputHash.isEmpty() && selection.inputHashes.contains(pending.inputHash)) return true;
        // Before preparation has produced a hash, a single TXT choice still
        // has a safe filename fallback. Never use that fallback for a set.
        return selection.mode == Mode.TXT && selection.inputHashes.isEmpty()
                && !selection.displayName.isEmpty()
                && selection.displayName.equalsIgnoreCase(pending.fileName);
    }

    static String key(Selection selection) {
        if (selection == null) return "";
        ArrayList<String> hashes = new ArrayList<>(selection.inputHashes);
        Collections.sort(hashes);
        return selection.mode.name() + "|" + selection.setId + "|" + String.join(",", hashes);
    }

    private static List<String> clean(List<String> values) {
        ArrayList<String> out = new ArrayList<>();
        if (values != null) for (String value : values) if (value != null && !value.trim().isEmpty()) out.add(value.trim());
        return Collections.unmodifiableList(out);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
