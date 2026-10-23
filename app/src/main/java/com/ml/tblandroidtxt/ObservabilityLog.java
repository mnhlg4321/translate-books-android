package com.ml.tblandroidtxt;

import android.content.Context;

import java.util.Locale;

/** Structured, privacy-safe runtime events used by model and cost diagnostics. */
public final class ObservabilityLog {
    private static volatile Context appContext;

    public static void initialize(Context context) {
        if (context != null) appContext = context.getApplicationContext();
    }

    public static void event(String name, Object... fields) {
        Context context = appContext;
        if (context == null) return;
        LogStore.append(context, formatEvent(name, fields));
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
