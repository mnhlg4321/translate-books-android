package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic checks after the edit call (plan section 4.3). A guard returns a flag; no guard throws, and no flag turns
 * into a content verdict by itself - the flags go to the check call as "points to look at" and to the person in the result.
 */
public final class EditGuards {
    public enum Code { STRUCTURE_WARN, REWRITE_WARN, SYMBOL_WARN, GLOSSARY_WARN, META_LEAK, META_UNSEPARABLE,
        NORMALIZATION_APPLIED, CONTENT_LEAK, STATUS_LABEL_CASE_RESTORED }

    public record Flag(Code code, String detail) { }

    /** Thresholds in one place: structure 10 % or 5 lines, rewrite 35 %, and the frame symbols that must keep their count. */
    public record Config(double structureMaxFraction, int structureMaxLines, double rewriteMaxRatio, List<String> symbols) {
        public static final List<String> DEFAULT_SYMBOLS = List.of("「", "」", "『", "』", "◇", "◆", "＊", "──", "【", "】", "……");

        public Config {
            symbols = List.copyOf(symbols);
        }

        public static Config defaults() { return new Config(0.10, 5, 0.35, DEFAULT_SYMBOLS); }
    }

    public record Report(String cleaned, List<Flag> flags, int structureDelta, double rewriteRatio, int changedSegments) {
        public Report {
            flags = List.copyOf(flags);
        }

        public boolean has(Code code) {
            for (Flag flag : flags) if (flag.code() == code) return true;
            return false;
        }

        public boolean separable() { return !has(Code.META_UNSEPARABLE); }
    }

    private static final Pattern FENCE = Pattern.compile("^\\s*```.*$");
    private static final Pattern LEAD = Pattern.compile(
            "(?i)^\\s*(here is|here's|below is|dưới đây là|đây là bản|bản (đã )?(chỉnh|biên tập|sửa)).*[:：]\\s*$");
    private static final Pattern TRAIL = Pattern.compile(
            "(?i)^\\s*(\\*\\*)?\\s*(ghi chú|chú thích|notes?|lưu ý|explanation)\\s*(\\*\\*)?\\s*[:：].*$");

    private EditGuards() { }

    public static Report check(String raw, String draft, String edited, List<EditInputs.GlossaryEntry> glossary, Config config) {
        List<Flag> flags = new ArrayList<>();
        String cleaned = stripMeta(edited, flags);
        String normalized = RawAlignedNormalizer.normalize(raw, cleaned, glossary).text();
        if (!normalized.equals(cleaned)) {
            flags.add(new Flag(Code.NORMALIZATION_APPLIED, "RAW-aligned symbol normalization"));
            cleaned = normalized;
        }
        cleaned = blockAddedSourceScript(draft, cleaned, glossary, flags);
        cleaned = restoreStatusLabelCapitalization(draft, cleaned, flags);

        int draftLines = nonBlankLines(draft);
        int editedLines = nonBlankLines(cleaned);
        int delta = Math.abs(draftLines - editedLines);
        if (delta > config.structureMaxLines() || (draftLines > 0 && delta > config.structureMaxFraction() * draftLines)) {
            flags.add(new Flag(Code.STRUCTURE_WARN, "draft " + draftLines + " lines, edited " + editedLines));
        }

        List<LineDiff.Segment> segments = LineDiff.segments(draft, cleaned);
        double ratio = draft.isEmpty() ? 0.0 : (double) LineDiff.changedChars(segments) / draft.length();
        if (ratio > config.rewriteMaxRatio()) {
            flags.add(new Flag(Code.REWRITE_WARN, String.format(Locale.ROOT, "%.0f%% of the draft changed", ratio * 100.0)));
        }

        for (String symbol : config.symbols()) {
            int inDraft = count(draft, symbol);
            int inEdited = count(cleaned, symbol);
            // a change is explained when the edited text now matches what RAW has
            if (inEdited != inDraft && inEdited != count(raw, symbol)) {
                flags.add(new Flag(Code.SYMBOL_WARN, symbol + " draft " + inDraft + ", edited " + inEdited));
            }
        }

        for (EditInputs.GlossaryEntry entry : ReferenceFilter.glossary(raw, glossary)) {
            if (!cleaned.contains(entry.target().trim())) {
                flags.add(new Flag(Code.GLOSSARY_WARN, entry.source().trim() + " -> " + entry.target().trim()));
            }
        }
        return new Report(cleaned, flags, delta, ratio, segments.size());
    }

    /** A model must not introduce Japanese/Han into a Vietnamese DRAFT line by accident. */
    private static String blockAddedSourceScript(String draft, String edited,
                                                 List<EditInputs.GlossaryEntry> glossary, List<Flag> flags) {
        String[] before = LineDiff.lines(draft);
        String[] after = LineDiff.lines(edited);
        if (before.length != after.length) return edited;
        StringBuilder out = new StringBuilder(edited.length());
        int blocked = 0;
        for (int i = 0; i < after.length; i++) {
            String line = after[i];
            if (RawAlignedNormalizer.containsKanaOrHan(line)
                    && !RawAlignedNormalizer.containsKanaOrHan(before[i])
                    && !allowedGlossarySource(line, glossary)) {
                line = before[i];
                blocked++;
            }
            if (i > 0) out.append('\n');
            out.append(line);
        }
        if (blocked > 0) flags.add(new Flag(Code.CONTENT_LEAK, blocked + " line(s) introduced kana/Han; reverted to normalized draft"));
        return blocked == 0 ? edited : out.toString();
    }

    private static boolean allowedGlossarySource(String line, List<EditInputs.GlossaryEntry> glossary) {
        for (EditInputs.GlossaryEntry entry : glossary) {
            if (!entry.source().isBlank() && line.contains(entry.source().trim())) return true;
        }
        return false;
    }

    /** Restores only case-only changes to corresponding 【…】 labels on the same aligned line. */
    private static String restoreStatusLabelCapitalization(String draft, String edited, List<Flag> flags) {
        String[] before = LineDiff.lines(draft);
        String[] after = LineDiff.lines(edited);
        if (before.length != after.length) return edited;
        Pattern label = Pattern.compile("【([^】]*)】");
        StringBuilder out = new StringBuilder(edited.length());
        int restored = 0;
        for (int i = 0; i < after.length; i++) {
            Matcher draftMatcher = label.matcher(before[i]);
            Matcher editMatcher = label.matcher(after[i]);
            List<String> draftLabels = new ArrayList<>();
            List<int[]> editRanges = new ArrayList<>();
            List<String> editLabels = new ArrayList<>();
            while (draftMatcher.find()) draftLabels.add(draftMatcher.group(1));
            while (editMatcher.find()) { editLabels.add(editMatcher.group(1)); editRanges.add(new int[] {editMatcher.start(1), editMatcher.end(1)}); }
            String line = after[i];
            if (draftLabels.size() == editLabels.size() && !draftLabels.isEmpty()) {
                StringBuilder fixed = new StringBuilder(line);
                for (int j = editLabels.size() - 1; j >= 0; j--) {
                    String original = draftLabels.get(j);
                    String changed = editLabels.get(j);
                    if (!original.equals(changed) && original.equalsIgnoreCase(changed)) {
                        int[] range = editRanges.get(j);
                        fixed.replace(range[0], range[1], original);
                        restored++;
                    }
                }
                line = fixed.toString();
            }
            if (i > 0) out.append('\n');
            out.append(line);
        }
        if (restored > 0) flags.add(new Flag(Code.STATUS_LABEL_CASE_RESTORED, restored + " status label(s) restored to DRAFT capitalization"));
        return restored == 0 ? edited : out.toString();
    }

    /**
     * Removes explanation lines the model wrapped around the text (a leading "here is ..." line, a trailing "Notes: ..."
     * block, code fences). Explanation lines in the middle cannot be separated from the story and are flagged instead.
     */
    static String stripMeta(String edited, List<Flag> flags) {
        String[] lines = LineDiff.lines(edited);
        int from = 0;
        int to = lines.length;
        int removed = 0;
        while (from < to && (lines[from].isBlank() || FENCE.matcher(lines[from]).matches() || LEAD.matcher(lines[from]).matches())) {
            if (!lines[from].isBlank()) removed++;
            from++;
        }
        while (to > from && (lines[to - 1].isBlank() || FENCE.matcher(lines[to - 1]).matches())) {
            if (!lines[to - 1].isBlank()) removed++;
            to--;
        }
        int trailing = to;
        for (int i = from; i < to; i++) {
            if (TRAIL.matcher(lines[i]).matches()) { trailing = i; break; }
        }
        if (trailing < to) {
            // everything from the first explanation line down must be explanation, otherwise it is not separable
            boolean allMeta = true;
            for (int i = trailing; i < to; i++) {
                if (!lines[i].isBlank() && !TRAIL.matcher(lines[i]).matches() && !looksLikeNoteItem(lines[i])) allMeta = false;
            }
            if (allMeta) {
                removed += to - trailing;
                to = trailing;
            }
        }
        boolean unseparable = false;
        for (int i = from; i < to; i++) {
            if (FENCE.matcher(lines[i]).matches() || TRAIL.matcher(lines[i]).matches()) unseparable = true;
        }
        if (unseparable) flags.add(new Flag(Code.META_UNSEPARABLE, "explanation inside the text"));
        else if (removed > 0) flags.add(new Flag(Code.META_LEAK, removed + " explanation lines removed"));
        StringBuilder out = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (i > from) out.append('\n');
            out.append(lines[i]);
        }
        return removed == 0 && !unseparable ? edited : out.toString().strip();
    }

    private static boolean looksLikeNoteItem(String line) {
        String t = line.strip();
        return t.startsWith("-") || t.startsWith("•") || t.startsWith("*") || t.matches("^\\d+[.)].*");
    }

    static int nonBlankLines(String text) {
        int n = 0;
        for (String line : LineDiff.lines(text)) if (!line.isBlank()) n++;
        return n;
    }

    static int count(String text, String needle) {
        if (needle.isEmpty()) return 0;
        int n = 0;
        int at = 0;
        while ((at = text.indexOf(needle, at)) >= 0) { n++; at += needle.length(); }
        return n;
    }
}
