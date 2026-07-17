package com.ml.tblandroidtxt;

import java.util.LinkedHashMap;
import java.util.Map;

public class EnvParser {
    public static Map<String, String> parse(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        if (text == null) return map;
        String[] lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            map.put(key, value);
        }
        return map;
    }
}
