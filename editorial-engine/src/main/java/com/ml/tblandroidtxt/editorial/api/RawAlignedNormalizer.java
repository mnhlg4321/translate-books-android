package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic repairs that are safe only when a DRAFT line is strongly
 * aligned with its RAW line.  It never translates prose and never invents a
 * quote; unresolved content is reported as a detection for the editor.
 */
public final class RawAlignedNormalizer {
    public record Repair(int draftLine, String kind) { }
    public record Detection(int draftLine, String kind, String detail) { }
    /** How the RAW and DRAFT sequences were proven to correspond. */
    public enum AlignmentMode { NONE, PHYSICAL, NONBLANK }
    public record Result(String text, List<Repair> repairs, List<Detection> detections,
                         boolean alignmentStrong, int rawNonBlankLines, int draftNonBlankLines,
                         AlignmentMode alignmentMode) {
        public Result(String text, List<Repair> repairs, List<Detection> detections,
                      boolean alignmentStrong, int rawNonBlankLines, int draftNonBlankLines) {
            this(text, repairs, detections, alignmentStrong, rawNonBlankLines, draftNonBlankLines,
                    alignmentStrong ? AlignmentMode.NONBLANK : AlignmentMode.NONE);
        }
        public Result {
            repairs = List.copyOf(repairs);
            detections = List.copyOf(detections);
            alignmentMode = alignmentMode == null ? AlignmentMode.NONE : alignmentMode;
        }
    }

    private static final Pattern RUBY = Pattern.compile("《[\\u3040-\\u30ff]+》");

    private RawAlignedNormalizer() { }

    public static Result normalize(String raw, String draft) {
        return normalize(raw, draft, List.of());
    }

    public static Result normalize(String raw, String draft, List<EditInputs.GlossaryEntry> glossary) {
        String[] rawLines = lines(raw);
        String[] draftLines = lines(draft);
        int rawCount = nonBlank(rawLines);
        int draftCount = nonBlank(draftLines);
        // Physical alignment is strongest.  When only blank lines differ, the
        // non-blank sequences are still a deterministic one-to-one mapping;
        // blanks are formatting and are skipped by the pairing loop below.
        boolean physical = rawCount > 0 && draftCount > 0 && rawLines.length == draftLines.length;
        if (physical) {
            for (int i = 0; i < rawLines.length; i++) {
                if (rawLines[i].isBlank() != draftLines[i].isBlank()) { physical = false; break; }
            }
        }
        AlignmentMode alignmentMode = physical ? AlignmentMode.PHYSICAL
                : (rawCount > 0 && rawCount == draftCount ? AlignmentMode.NONBLANK : AlignmentMode.NONE);
        boolean strong = alignmentMode != AlignmentMode.NONE;
        List<Repair> repairs = new ArrayList<>();
        List<Detection> detections = new ArrayList<>();
        StringBuilder output = new StringBuilder();
        int rawIndex = 0;
        int draftLineNumber = 0;
        for (String draftLine : draftLines) {
            draftLineNumber++;
            if (draftLineNumber > 1) output.append('\n');
            if (draftLine.isBlank()) { output.append(draftLine); continue; }
            while (rawIndex < rawLines.length && rawLines[rawIndex].isBlank()) rawIndex++;
            String rawLine = rawIndex < rawLines.length ? rawLines[rawIndex++] : "";
            String current = draftLine;
            // Physical alignment has the same line/blank layout as the
            // source.  Nonblank alignment is deliberately diagnostic only:
            // the 28-chapter Q2.1 measurement found every attempted symbol
            // repair could move a line farther from FINAL under shifted
            // blanks, so it must not mutate text.
            if (alignmentMode == AlignmentMode.PHYSICAL && !rawLine.isBlank()) {
                String repaired = repairSymbols(rawLine, current, repairs, draftLineNumber);
                current = repaired;
            }
            if (containsKanaOrHan(current)) {
                detections.add(new Detection(draftLineNumber, "UNTRANSLATED", "kana_or_han_remains"));
            }
            for (EditInputs.GlossaryEntry entry : glossary == null ? List.<EditInputs.GlossaryEntry>of() : glossary) {
                if (!entry.source().isBlank() && rawLine.contains(entry.source().trim())
                        && !entry.target().isBlank() && !current.contains(entry.target().trim())) {
                    detections.add(new Detection(draftLineNumber, "GLOSSARY", "target_missing"));
                }
            }
            output.append(current);
        }
        if (!strong && rawCount != draftCount) {
            detections.add(new Detection(0, "LINE_OFFSET", rawCount + "_raw_vs_" + draftCount + "_draft"));
        }
        return new Result(output.toString(), repairs, detections, strong, rawCount, draftCount, alignmentMode);
    }

    private static String repairSymbols(String raw, String draft, List<Repair> repairs, int line) {
        String out = draft;
        String before = out;
        if (raw.contains("《") && raw.contains("》") && out.contains("【") && out.contains("】")) {
            out = out.replace('【', '《').replace('】', '》');
            if (!out.equals(before)) repairs.add(new Repair(line, "FRAME_FROM_RAW"));
        }
        before = out;
        if (raw.contains("〝") && raw.contains("〟")) {
            out = out.replace('“', '〝').replace('”', '〟');
            if (!out.equals(before)) repairs.add(new Repair(line, "QUOTE_FROM_RAW"));
        }
        before = out;
        if (raw.contains("……") && !out.contains("……") && out.contains("…")) {
            out = out.replace("…", "……");
            if (!out.equals(before)) repairs.add(new Repair(line, "ELLIPSIS_FROM_RAW"));
        }
        before = out;
        if (containsHalfWidthStatus(raw)) {
            out = halfWidthStatus(out);
            if (!out.equals(before)) repairs.add(new Repair(line, "STATUS_WIDTH_FROM_RAW"));
        }
        before = out;
        Matcher matcher = RUBY.matcher(out);
        StringBuffer stripped = new StringBuffer();
        while (matcher.find()) {
            if (raw.contains(matcher.group())) matcher.appendReplacement(stripped, "");
            else matcher.appendReplacement(stripped, Matcher.quoteReplacement(matcher.group()));
        }
        matcher.appendTail(stripped);
        out = stripped.toString();
        if (!out.equals(before)) repairs.add(new Repair(line, "REMOVE_COPIED_RUBY"));
        before = out;
        String normalized = halfWidthAscii(out);
        if (containsAsciiLetterOrDigit(raw) && !normalized.equals(out)) {
            out = normalized;
            repairs.add(new Repair(line, "FULLWIDTH_ASCII_FROM_RAW"));
        }
        return out;
    }

    private static boolean containsHalfWidthStatus(String raw) {
        return raw.indexOf('?') >= 0 || raw.indexOf('!') >= 0 || raw.indexOf(':') >= 0 || raw.indexOf('/') >= 0
                || raw.matches(".*[0-9].*");
    }

    private static String halfWidthStatus(String value) {
        return value.replace('？', '?').replace('！', '!').replace('：', ':').replace('／', '/')
                .replace('０', '0').replace('１', '1').replace('２', '2').replace('３', '3').replace('４', '4')
                .replace('５', '5').replace('６', '6').replace('７', '7').replace('８', '8').replace('９', '9');
    }

    private static boolean containsAsciiLetterOrDigit(String value) {
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) < 128 && Character.isLetterOrDigit(value.charAt(i))) return true;
        return false;
    }

    private static String halfWidthAscii(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '！' && c <= '～') out.append((char) (c - '！' + '!'));
            else if (c == '　') out.append(' ');
            else out.append(c);
        }
        return out.toString();
    }

    static boolean containsKanaOrHan(String value) {
        for (int i = 0; i < value.length(); ) {
            int cp = value.codePointAt(i);
            Character.UnicodeScript script = Character.UnicodeScript.of(cp);
            if (script == Character.UnicodeScript.HIRAGANA || script == Character.UnicodeScript.KATAKANA
                    || script == Character.UnicodeScript.HAN) return true;
            i += Character.charCount(cp);
        }
        return false;
    }

    private static String[] lines(String text) {
        return (text == null ? "" : text).replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }

    private static int nonBlank(String[] lines) {
        int count = 0;
        for (String line : lines) if (!line.isBlank()) count++;
        return count;
    }
}
