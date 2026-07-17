package com.ml.tblandroidtxt;

public class YamlInstructionParser {
    public static CustomInstructions parse(String filename, String text) {
        CustomInstructions out = new CustomInstructions();
        if (text == null) return out;
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".txt")) {
            String v = text.trim();
            out.translation = v;
            out.refinement = v;
            return out;
        }
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        out.translation = readYamlBlock(normalized, "translation");
        out.refinement = readYamlBlock(normalized, "refinement");
        return out;
    }

    private static String readYamlBlock(String text, String key) {
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        boolean active = false;
        int blockIndent = -1;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!active) {
                if (trimmed.equals(key + ":") || trimmed.startsWith(key + ": |") || trimmed.startsWith(key + ": >") || trimmed.startsWith(key + ": |-")) {
                    active = true;
                    blockIndent = -1;
                    String after = trimmed.length() > key.length() + 1 ? trimmed.substring(key.length() + 1).trim() : "";
                    if (!after.isEmpty() && !after.startsWith("|") && !after.startsWith(">")) {
                        return stripQuotes(after);
                    }
                }
                continue;
            }
            if (trimmed.matches("^[A-Za-z0-9_\\-]+:.*") && leadingSpaces(line) == 0) break;
            if (trimmed.isEmpty()) {
                sb.append('\n');
                continue;
            }
            int indent = leadingSpaces(line);
            if (blockIndent < 0) blockIndent = indent;
            int cut = Math.min(blockIndent, line.length());
            sb.append(line.substring(cut)).append('\n');
        }
        String v = sb.toString().trim();
        return v.isEmpty() ? null : v;
    }

    private static int leadingSpaces(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') i++;
        return i;
    }

    private static String stripQuotes(String s) {
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1).trim();
        }
        return s.trim();
    }
}
