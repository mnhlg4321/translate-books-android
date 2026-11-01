package com.ml.tblandroidtxt;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Retention rules for private SAF-output recovery copies in filesDir. */
final class OutputRecoveryCopies {
    static final int MAX_PER_OUTPUT = 3;
    static final long MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L;
    private static final String PREFIX = "output-recovery-";
    private static final Pattern NAMED = Pattern.compile("^output-recovery-([0-9a-f]{64})-(\\d{13})\\.txt$");
    private static final Pattern LEGACY = Pattern.compile("^output-recovery-(\\d{13})\\.txt$");

    private static final class Entry {
        final File file;
        final String outputKey;
        final long timestamp;
        Entry(File file, String outputKey, long timestamp) { this.file = file; this.outputKey = outputKey; this.timestamp = timestamp; }
    }

    /** Removes expired copies, then trims each known output to its three newest copies. Legacy names share one conservative group. */
    static int prune(File directory, long now) {
        if (directory == null || !directory.isDirectory()) return 0;
        File[] files = directory.listFiles();
        if (files == null) return 0;
        int deleted = 0;
        Map<String, List<Entry>> groups = new HashMap<>();
        for (File file : files) {
            if (!file.isFile() || !file.getName().startsWith(PREFIX)) continue;
            if (file.getName().endsWith(".pending")) {
                if (file.delete()) deleted++;
                continue;
            }
            if (!file.getName().endsWith(".txt")) continue;
            Entry entry = parse(file);
            if (entry == null) entry = new Entry(file, "", file.lastModified());
            if (entry.timestamp < now - MAX_AGE_MS) {
                if (file.delete()) deleted++;
                continue;
            }
            groups.computeIfAbsent(entry.outputKey, ignored -> new ArrayList<>()).add(entry);
        }
        for (List<Entry> group : groups.values()) {
            Collections.sort(group, Comparator.comparingLong((Entry e) -> e.timestamp).reversed()
                    .thenComparing(e -> e.file.getName()));
            for (int i = MAX_PER_OUTPUT; i < group.size(); i++) if (group.get(i).file.delete()) deleted++;
        }
        return deleted;
    }

    static File namedCopy(File directory, String outputKey, long timestamp) {
        return new File(directory, PREFIX + outputKey + "-" + timestamp + ".txt");
    }

    private static Entry parse(File file) {
        Matcher named = NAMED.matcher(file.getName());
        if (named.matches()) return new Entry(file, named.group(1), parseTime(named.group(2), file));
        Matcher legacy = LEGACY.matcher(file.getName());
        if (legacy.matches()) return new Entry(file, "", parseTime(legacy.group(1), file));
        return null;
    }

    private static long parseTime(String value, File file) {
        try { return Long.parseLong(value); }
        catch (NumberFormatException ignored) { return file.lastModified(); }
    }

    private OutputRecoveryCopies() {}
}
