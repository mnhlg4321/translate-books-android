package com.ml.tblandroidtxt.editorial.api;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads the check answer leniently. Only an unreadable answer is a failure (the flow retries it once); everything else that
 * is irregular - extra keys, a missing optional value, an issue without a quote, an unknown kind - is counted and the rest of
 * the answer is used. There is no rejection code for a bookkeeping slip.
 */
public final class CheckResponseParser {
    public enum Status { OK, UNREADABLE }

    public record Issue(String editedQuote, String rawQuote, EditorialApiContract.IssueKind kind, String fix) { }

    public record Parsed(Status status, String verdict, String wrongPairEvidence, List<Issue> issues, Counters counters) {
        public Parsed {
            issues = List.copyOf(issues);
        }
    }

    /** What was irregular; shown in technical details and used by the measurement, never a stop. */
    public record Counters(int issuesDropped, int unknownKinds, int unknownKeys, int missingKeys, int oversizeValues) {
        public int total() { return issuesDropped + unknownKinds + unknownKeys + missingKeys + oversizeValues; }
    }

    private CheckResponseParser() { }

    @SuppressWarnings("unchecked")
    public static Parsed parse(String content) {
        Map<String, Object> root = readObject(content);
        if (root == null) return new Parsed(Status.UNREADABLE, "", "", List.of(), new Counters(0, 0, 0, 0, 0));
        int unknownKeys = 0;
        int missingKeys = 0;
        int oversize = 0;
        int dropped = 0;
        int unknownKinds = 0;
        for (String key : root.keySet()) {
            if (!key.equals(CheckSpec.VERDICT) && !key.equals(CheckSpec.WRONG_PAIR_EVIDENCE) && !key.equals(CheckSpec.ISSUES)) unknownKeys++;
        }
        String verdict = text(root.get(CheckSpec.VERDICT)).strip().toUpperCase(java.util.Locale.ROOT);
        if (!root.containsKey(CheckSpec.VERDICT)) missingKeys++;
        String evidence = text(root.get(CheckSpec.WRONG_PAIR_EVIDENCE));
        if (evidence.length() > EditorialApiContract.MAX_WRONG_PAIR_EVIDENCE_CHARS) {
            evidence = evidence.substring(0, EditorialApiContract.MAX_WRONG_PAIR_EVIDENCE_CHARS);
            oversize++;
        }
        List<Issue> issues = new ArrayList<>();
        Object rawIssues = root.get(CheckSpec.ISSUES);
        if (rawIssues instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) { dropped++; continue; }
                Map<String, Object> row = (Map<String, Object>) map;
                for (String key : row.keySet()) {
                    if (!List.of(CheckSpec.EDITED_QUOTE, CheckSpec.RAW_QUOTE, CheckSpec.KIND, CheckSpec.FIX).contains(key)) unknownKeys++;
                }
                String editedQuote = text(row.get(CheckSpec.EDITED_QUOTE));
                if (editedQuote.isBlank()) { dropped++; continue; }
                if (editedQuote.length() > EditorialApiContract.MAX_QUOTE_CHARS) oversize++;
                String rawQuote = text(row.get(CheckSpec.RAW_QUOTE));
                String fix = text(row.get(CheckSpec.FIX));
                if (!row.containsKey(CheckSpec.FIX) || !row.containsKey(CheckSpec.RAW_QUOTE)) missingKeys++;
                EditorialApiContract.IssueKind kind;
                try {
                    kind = EditorialApiContract.IssueKind.valueOf(text(row.get(CheckSpec.KIND)).strip().toUpperCase(java.util.Locale.ROOT));
                } catch (IllegalArgumentException unknown) {
                    kind = EditorialApiContract.IssueKind.TECHNICAL;
                    unknownKinds++;
                }
                issues.add(new Issue(editedQuote, rawQuote, kind, fix));
            }
        } else if (root.containsKey(CheckSpec.ISSUES)) {
            dropped++;
        } else {
            missingKeys++;
        }
        if (!CheckSpec.VERDICTS.contains(verdict)) {
            // an answer that lists problems is ISSUES; one that lists none is PASS
            verdict = issues.isEmpty() ? "PASS" : "ISSUES";
            if (root.containsKey(CheckSpec.VERDICT)) unknownKinds++;
        }
        return new Parsed(Status.OK, verdict, evidence, issues, new Counters(dropped, unknownKinds, unknownKeys, missingKeys, oversize));
    }

    private static String text(Object value) {
        return value instanceof String s ? s : "";
    }

    /** The first JSON object of the answer, tolerating a code fence or a sentence around it. */
    private static Map<String, Object> readObject(String content) {
        if (content == null) return null;
        int from = content.indexOf('{');
        int to = content.lastIndexOf('}');
        if (from < 0 || to <= from) return null;
        try {
            return EditorialCanonicalJson.parseObject(content.substring(from, to + 1).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (RuntimeException unreadable) {
            return null;
        }
    }
}
