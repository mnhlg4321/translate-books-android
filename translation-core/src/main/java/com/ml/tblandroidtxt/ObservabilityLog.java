package com.ml.tblandroidtxt;

import java.util.Locale;

/** Structured, privacy-safe runtime events used by model and cost diagnostics. */
public final class ObservabilityLog {
    /** Destination for formatted event lines; the app installs one that writes to its log store. */
    public interface Sink { void append(String line); }

    private static volatile Sink sink;

    public static void install(Sink next) {
        if (next != null) sink = next;
    }

    public static void event(String name, Object... fields) {
        Sink current = sink;
        if (current == null) return;
        current.append(formatEvent(name, fields));
    }

    /** Formats the privacy-safe event line so offline tests can inspect its exact fields. */
    static String formatEvent(String name, Object... fields) {
        StringBuilder line = new StringBuilder("OBS event=").append(safe(name));
        if (fields != null) {
            for (int i = 0; i + 1 < fields.length; i += 2) {
                line.append(' ').append(safe(String.valueOf(fields[i]))).append('=')
                        .append(safeValue(fields[i + 1]));
            }
        }
        return line.toString();
    }

    private static String safeValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Double) {
            double number = (Double) value;
            if (Double.isNaN(number)) return "unknown";
            return String.format(Locale.US, "%.9g", number);
        }
        return safe(String.valueOf(value));
    }

    private static String safe(String value) {
        if (value == null) return "";
        return value.replaceAll("[^A-Za-z0-9_./:@+-]", "_");
    }

    private ObservabilityLog() {}
}
