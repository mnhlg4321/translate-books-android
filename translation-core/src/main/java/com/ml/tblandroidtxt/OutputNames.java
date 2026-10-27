package com.ml.tblandroidtxt;

import java.util.Locale;

/** Output file name rules shared by the app and the core; FileUtil.sanitizeOutputName delegates here. */
public final class OutputNames {
    private OutputNames() {}

    public static String sanitizeOutputName(String raw) {
        String s = raw == null || raw.trim().isEmpty() ? "translated.txt" : raw.trim();
        s = s.replace('\u0000', '_')
                .replace('/', '_')
                .replace('\\', '_')
                .replace(':', '_')
                .replace('*', '_')
                .replace('?', '_')
                .replace('"', '_')
                .replace('<', '_')
                .replace('>', '_')
                .replace('|', '_');
        while (s.contains("  ")) s = s.replace("  ", " ");
        s = s.trim();
        if (s.isEmpty()) s = "translated.txt";
        if (!s.toLowerCase(Locale.ROOT).endsWith(".txt")) s += ".txt";
        if (s.length() > 180) {
            int dot = s.toLowerCase(Locale.ROOT).lastIndexOf(".txt");
            s = s.substring(0, Math.min(176, dot > 0 ? dot : s.length())) + ".txt";
        }
        return s;
    }
}
