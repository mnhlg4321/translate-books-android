package com.ml.tblandroidtxt.editorial.pack;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Z2: derives wire variants from a response that already parses and states what each variant must do. A variant that
 * only moves a value the app derives or never uses must parse and leave a note (NORMALIZED); a variant that
 * contradicts the source must be refused with a code classified SEMANTIC, PROTOCOL or MIXED. A refusal with a code
 * classified BOOKKEEPING, APP or not classified at all is always a defect. The engine holds no book text: it only
 * edits structure of whatever baseline it is given (synthetic in the unit test, saved live responses offline).
 */
final class EditorialWireMutationEngine {
    enum Expect { NORMALIZED, REFUSED }

    record Mutation(String name, Expect expect, Consumer<Map<String, Object>> apply) { }

    record Outcome(String name, Expect expect, boolean parsed, String code, List<String> notes, boolean ok, String why) { }

    interface Parser { List<String> parse(byte[] wire); }

    private static final Set<String> REFUSABLE = Set.of("SEMANTIC", "PROTOCOL", "MIXED");

    private EditorialWireMutationEngine() { }

    static Map<String, Object> copy(byte[] baseline) {
        return EditorialCanonicalJson.parseObject(baseline);
    }

    static byte[] bytes(Map<String, Object> value) {
        return EditorialCanonicalJson.canonicalize(value).getBytes(StandardCharsets.UTF_8);
    }

    static Outcome run(byte[] baseline, Mutation mutation, Parser parser, Map<String, String> labels) {
        Map<String, Object> variant = copy(baseline);
        mutation.apply().accept(variant);
        boolean parsed = false;
        String code = "";
        List<String> notes = List.of();
        try {
            notes = parser.parse(bytes(variant));
            parsed = true;
        } catch (RuntimeException refused) {
            code = WireViolation.safeMessage(refused, "UNCLASSIFIED_PARSE_FAILURE");
            if (!(refused instanceof WireViolation)) {
                // not an app-authored refusal: keep the exception type visible so the defect can be located
                code = code + "[" + refused.getClass().getSimpleName() + " at "
                        + (refused.getStackTrace().length > 0 ? refused.getStackTrace()[0] : "?") + "]";
            }
        }
        String bare = code.contains(":") ? code.substring(0, code.indexOf(':')) : code;
        boolean ok;
        String why = "";
        if (mutation.expect() == Expect.NORMALIZED) {
            ok = parsed;
            if (!ok) why = "bookkeeping variant refused with " + code + " (" + labels.getOrDefault(bare, "UNCLASSIFIED") + ")";
        } else {
            String label = labels.getOrDefault(bare, "UNCLASSIFIED");
            ok = !parsed && REFUSABLE.contains(label);
            if (parsed) why = "semantic variant was accepted";
            else if (!ok) why = "refused with " + code + " classified " + label;
        }
        return new Outcome(mutation.name(), mutation.expect(), parsed, code, notes, ok, why);
    }

    static List<Outcome> runAll(byte[] baseline, List<Mutation> mutations, Parser parser, Map<String, String> labels) {
        List<Outcome> outcomes = new ArrayList<>();
        for (Mutation mutation : mutations) outcomes.add(run(baseline, mutation, parser, labels));
        return outcomes;
    }

    static Map<String, String> labels() {
        Map<String, String> labels = new LinkedHashMap<>();
        try (var in = EditorialWireMutationEngine.class.getResourceAsStream("/rejection-classification.csv")) {
            if (in == null) throw new IllegalStateException("rejection-classification.csv missing");
            String[] lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n");
            for (int i = 1; i < lines.length; i++) {
                if (lines[i].isBlank()) continue;
                String[] parts = lines[i].split(",", 4);
                labels.put(parts[0], parts[1]);
            }
        } catch (IOException unreadable) {
            throw new IllegalStateException(unreadable);
        }
        return labels;
    }

    // ---- structure helpers -------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    static List<Object> list(Map<String, Object> root, String key) { return (List<Object>) root.get(key); }

    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(Object value) { return (Map<String, Object>) value; }

    static BigDecimal num(int value) { return BigDecimal.valueOf(value); }

    // ---- L1 RECONCILE ------------------------------------------------------------------------------------------

    /**
     * @param draftLines   number of DRAFT lines
     * @param blankRawLine physical RAW line number that is not a unit (blank), or 0 when there is none
     * @param rawLines     number of physical RAW lines
     * @param goodUnit     a valid unit reference ("L<n>") for speaker records
     * @param occupied     DRAFT lines anchoring an OPEN finding (a protected span there is a real contradiction)
     */
    static List<Mutation> l1Reconcile(Map<String, Object> baseline, int draftLines, int blankRawLine, int rawLines,
                                      String goodUnit, Set<Integer> occupied, java.util.function.ToIntFunction<String> rawHits) {
        List<Mutation> m = new ArrayList<>();
        int free = 1;
        while (free <= draftLines && occupied.contains(free)) free++;
        final int freeLine = free > draftLines ? 0 : free;
        boolean hasFinding = !list(baseline, "findings").isEmpty();
        // ---- bookkeeping: must parse
        m.add(new Mutation("root: unknown key", Expect.NORMALIZED, r -> r.put("zz-extra", "x")));
        m.add(new Mutation("disposition: unknown key", Expect.NORMALIZED, r -> obj(r.get("disposition")).put("zz-extra", BigDecimal.ONE)));
        m.add(new Mutation("root: optional arrays omitted", Expect.NORMALIZED, r -> {
            r.remove("speakerRecords");
            r.remove("protectedSpans");
        }));
        m.add(new Mutation("disposition: CONTINUE with stop class and unsafe reason", Expect.NORMALIZED, r -> {
            Map<String, Object> d = obj(r.get("disposition"));
            if ("STOP".equals(d.get("disposition"))) return;
            d.put("stopClass", "CONTENT_BLOCKED");
            d.put("reasonCode", "not a safe label !!!");
        }));
        m.add(new Mutation("disposition: CONTINUE without reason or stop class keys", Expect.NORMALIZED, r -> {
            Map<String, Object> d = obj(r.get("disposition"));
            if ("STOP".equals(d.get("disposition"))) return;
            d.remove("reasonCode");
            d.remove("stopClass");
        }));
        if (hasFinding) {
            for (int delta : new int[] {-2, -1, 1, 2, 60, -200}) {
                m.add(new Mutation("finding.draft: start/end shifted by " + delta, Expect.NORMALIZED, r -> {
                    Map<String, Object> d = obj(obj(list(r, "findings").get(0)).get("draft"));
                    if (!"LINES".equals(d.get("kind"))) return;
                    d.put("start", num(((BigDecimal) d.get("start")).intValue() + delta));
                    d.put("end", num(((BigDecimal) d.get("end")).intValue() + delta));
                }));
            }
            m.add(new Mutation("finding.draft: after filled on LINES (W4)", Expect.NORMALIZED, r -> {
                for (Object f : list(r, "findings")) {
                    Map<String, Object> d = obj(obj(f).get("draft"));
                    if ("LINES".equals(d.get("kind"))) d.put("after", d.get("start"));
                }
            }));
            m.add(new Mutation("finding.draft: start/end filled on MISSING", Expect.NORMALIZED, r -> {
                for (Object f : list(r, "findings")) {
                    Map<String, Object> d = obj(obj(f).get("draft"));
                    if ("MISSING".equals(d.get("kind"))) {
                        d.put("start", num(3));
                        d.put("end", num(4));
                    }
                }
            }));
            m.add(new Mutation("finding.draft: RAW line number copied into the DRAFT hint", Expect.NORMALIZED, r -> {
                Map<String, Object> f = obj(list(r, "findings").get(0));
                Map<String, Object> d = obj(f.get("draft"));
                if (!"LINES".equals(d.get("kind"))) return;
                String unit = (String) list(f, "rawUnits").get(0);
                int raw = Integer.parseInt(unit.substring(1));
                d.put("start", num(raw));
                d.put("end", num(raw));
            }));
            m.add(new Mutation("finding.draft: start/end absent", Expect.NORMALIZED, r -> {
                Map<String, Object> d = obj(obj(list(r, "findings").get(0)).get("draft"));
                if ("LINES".equals(d.get("kind"))) {
                    d.remove("start");
                    d.remove("end");
                    d.remove("after");
                }
            }));
            // Z2: RAW unit numbers are hints; they only move when the quote occurs in exactly one unit
            Map<String, Object> first = obj(list(baseline, "findings").get(0));
            boolean uniqueQuote = first.get("rawQuote") instanceof String q && rawHits.applyAsInt(q) == 1;
            if (uniqueQuote) {
                for (int delta : new int[] {-2, -1, 1, 2}) {
                    m.add(new Mutation("finding.rawUnits: neighbouring unit " + delta, Expect.NORMALIZED, r -> {
                        List<Object> units = list(obj(list(r, "findings").get(0)), "rawUnits");
                        String unit = (String) units.get(0);
                        units.set(0, "L" + Math.max(1, Integer.parseInt(unit.substring(1)) + delta));
                    }));
                }
                m.add(new Mutation("finding.rawUnits: unit past the text", Expect.NORMALIZED,
                        r -> list(obj(list(r, "findings").get(0)), "rawUnits").set(0, "L" + (rawLines + 500))));
                m.add(new Mutation("finding.rawUnits: blank line", Expect.NORMALIZED, r -> {
                    if (blankRawLine > 0) list(obj(list(r, "findings").get(0)), "rawUnits").set(0, "L" + blankRawLine);
                }));
                m.add(new Mutation("finding.rawUnits: empty", Expect.NORMALIZED,
                        r -> list(obj(list(r, "findings").get(0)), "rawUnits").clear()));
            }
            m.add(new Mutation("finding: unknown key", Expect.NORMALIZED, r -> obj(list(r, "findings").get(0)).put("zz-extra", "x")));
            m.add(new Mutation("finding: optional keys omitted", Expect.NORMALIZED, r -> {
                Map<String, Object> f = obj(list(r, "findings").get(0));
                f.remove("evidenceRefs");
                f.remove("candidateIds");
                f.remove("occurrenceUnits");
                if ("OPEN".equals(f.get("disposition"))) f.remove("evidenceLimit");
            }));
            m.add(new Mutation("finding: observation with control characters and overlong text", Expect.NORMALIZED, r -> {
                Map<String, Object> f = obj(list(r, "findings").get(0));
                f.put("observation", "line one\nline two\t" + "x".repeat(4000));
                f.put("expectedMeaning", "ok\r\n" + "y".repeat(4000));
            }));
            m.add(new Mutation("finding: duplicated rawUnits / evidenceRefs / candidateIds", Expect.NORMALIZED, r -> {
                Map<String, Object> f = obj(list(r, "findings").get(0));
                List<Object> units = list(f, "rawUnits");
                units.add(units.get(0));
                List<Object> refs = list(f, "evidenceRefs");
                refs.add("ref-a");
                refs.add("ref-a");
                List<Object> candidates = list(f, "candidateIds");
                if (!candidates.isEmpty()) candidates.add(candidates.get(0));
            }));
            m.add(new Mutation("finding: unusable evidenceRefs items and overlong list", Expect.NORMALIZED, r -> {
                Map<String, Object> f = obj(list(r, "findings").get(0));
                List<Object> refs = list(f, "evidenceRefs");
                refs.add("");
                refs.add("not a token!");
                refs.add("x".repeat(200));
                for (int i = 0; i < 40; i++) refs.add("ref-" + i);
            }));
            m.add(new Mutation("finding: unusable occurrenceUnits entries", Expect.NORMALIZED, r -> {
                Map<String, Object> f = obj(list(r, "findings").get(0));
                List<Object> occ = list(f, "occurrenceUnits");
                occ.add("L99999");
                occ.add("X1");
                occ.add("");
                if (blankRawLine > 0) occ.add("L" + blankRawLine);
            }));
            m.add(new Mutation("finding: draftQuote with NFD and outer whitespace", Expect.NORMALIZED, r -> {
                Map<String, Object> f = obj(list(r, "findings").get(0));
                Object q = f.get("draftQuote");
                if (q instanceof String s && !s.isEmpty()) {
                    String decomposed = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
                    String padded = "  " + decomposed + "  ";
                    // stay inside the 80-character quote limit: the variant tests normalization, not length
                    if (padded.length() <= 80) f.put("draftQuote", padded);
                    else if (decomposed.length() <= 80) f.put("draftQuote", decomposed);
                }
            }));
        }
        m.add(new Mutation("speakerRecords: reference to a blank line", Expect.NORMALIZED, r -> speakerRecord(r, blankRawLine > 0 ? "L" + blankRawLine : "L" + (rawLines + 5))));
        m.add(new Mutation("speakerRecords: reference past the text", Expect.NORMALIZED, r -> speakerRecord(r, "L" + (rawLines + 500))));
        m.add(new Mutation("speakerRecords: malformed reference", Expect.NORMALIZED, r -> speakerRecord(r, "X7")));
        m.add(new Mutation("speakerRecords: blank speaker label", Expect.NORMALIZED, r -> {
            speakerRecord(r, goodUnit);
            obj(list(r, "speakerRecords").get(list(r, "speakerRecords").size() - 1)).put("speaker", "");
        }));
        m.add(new Mutation("speakerRecords: listener omitted", Expect.NORMALIZED, r -> {
            speakerRecord(r, goodUnit);
            obj(list(r, "speakerRecords").get(list(r, "speakerRecords").size() - 1)).remove("listener");
        }));
        m.add(new Mutation("speakerRecords: far more records than the cap", Expect.NORMALIZED, r -> {
            for (int i = 0; i < EditorialL1Ledger.MAX_SPEAKER_RECORDS_PER_CALL + 5; i++) speakerRecord(r, goodUnit);
        }));
        if (freeLine > 0) {
            m.add(new Mutation("protectedSpans: valid span on a free line", Expect.NORMALIZED,
                    r -> protectedSpan(r, "ps-ok", freeLine, freeLine, "reason")));
            m.add(new Mutation("protectedSpans: duplicate id", Expect.NORMALIZED, r -> {
                protectedSpan(r, "ps-dup", freeLine, freeLine, "reason");
                protectedSpan(r, "ps-dup", freeLine, freeLine, "reason");
            }));
            m.add(new Mutation("protectedSpans: invalid id", Expect.NORMALIZED, r -> protectedSpan(r, "bad id!", freeLine, freeLine, "reason")));
            m.add(new Mutation("protectedSpans: empty reason", Expect.NORMALIZED, r -> protectedSpan(r, "ps-empty", freeLine, freeLine, "")));
            m.add(new Mutation("protectedSpans: reason key omitted", Expect.NORMALIZED, r -> {
                protectedSpan(r, "ps-nokey", freeLine, freeLine, "x");
                obj(list(r, "protectedSpans").get(list(r, "protectedSpans").size() - 1)).remove("reason");
            }));
        }
        if (draftLines >= 1 && !occupied.contains(draftLines)) {
            m.add(new Mutation("protectedSpans: range past the draft end is clamped", Expect.NORMALIZED,
                    r -> protectedSpan(r, "ps-tail", draftLines, draftLines + 40, "reason")));
            m.add(new Mutation("protectedSpans: reversed range past the end", Expect.NORMALIZED,
                    r -> protectedSpan(r, "ps-rev2", draftLines + 3, draftLines, "reason")));
        }
        m.add(new Mutation("protectedSpans: range wholly outside is dropped", Expect.NORMALIZED,
                r -> protectedSpan(r, "ps-out", draftLines + 10, draftLines + 20, "reason")));
        // ---- semantic: must be refused with a SEMANTIC/PROTOCOL/MIXED code
        m.add(new Mutation("root: wrong attempt echo", Expect.REFUSED, r -> r.put("attemptIdentity", "0".repeat(64))));
        m.add(new Mutation("root: wrong wire schema", Expect.REFUSED, r -> r.put("wireSchemaVersion", "safe4.l1.reconcile.wire.v0")));
        m.add(new Mutation("disposition: forged kind", Expect.REFUSED, r -> obj(r.get("disposition")).put("disposition", "PASS")));
        m.add(new Mutation("coverage: dropped", Expect.REFUSED, r -> list(r, "coverage").clear()));
        if (hasFinding) {
            m.add(new Mutation("finding: rawQuote absent from the RAW unit", Expect.REFUSED,
                    r -> obj(list(r, "findings").get(0)).put("rawQuote", "\u0001-not-in-raw-\u0002".replace('\u0001', '#').replace('\u0002', '#'))));
            m.add(new Mutation("finding: draftQuote absent from the whole DRAFT", Expect.REFUSED, r -> {
                Map<String, Object> f = obj(list(r, "findings").get(0));
                if ("LINES".equals(obj(f.get("draft")).get("kind"))) f.put("draftQuote", "#-not-in-draft-#");
                else f.put("draftQuote", "#-not-in-draft-#");
            }));
            m.add(new Mutation("finding: forged type", Expect.REFUSED, r -> obj(list(r, "findings").get(0)).put("type", "FANTASY")));
            m.add(new Mutation("finding: duplicate errorId", Expect.REFUSED, r -> {
                List<Object> findings = list(r, "findings");
                findings.add(new LinkedHashMap<>(obj(findings.get(0))));
            }));
            m.add(new Mutation("finding: blank observation", Expect.REFUSED, r -> obj(list(r, "findings").get(0)).put("observation", "")));
        }
        if (!list(baseline, "resolutions").isEmpty()) {
            m.add(new Mutation("resolutions: one candidate left unresolved", Expect.REFUSED, r -> list(r, "resolutions").remove(0)));
            m.add(new Mutation("resolutions: unknown candidate", Expect.REFUSED, r -> obj(list(r, "resolutions").get(0)).put("candidateId", "no-such-candidate")));
        }
        return m;
    }

    private static void speakerRecord(Map<String, Object> root, String unit) {
        if (!root.containsKey("speakerRecords")) root.put("speakerRecords", new ArrayList<Object>());
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("unitId", unit);
        record.put("speaker", "A");
        record.put("listener", "B");
        record.put("basis", "synthetic basis");
        list(root, "speakerRecords").add(record);
    }

    private static void protectedSpan(Map<String, Object> root, String id, int start, int end, String reason) {
        if (!root.containsKey("protectedSpans")) root.put("protectedSpans", new ArrayList<Object>());
        Map<String, Object> span = new LinkedHashMap<>();
        span.put("spanId", id);
        span.put("start", num(start));
        span.put("end", num(end));
        span.put("source", "PRONOUN_ROW");
        span.put("reason", reason);
        list(root, "protectedSpans").add(span);
    }

    // ---- L1 RAW ------------------------------------------------------------------------------------------------

    static List<Mutation> l1Raw(Map<String, Object> baseline) {
        List<Mutation> m = new ArrayList<>();
        m.add(new Mutation("root: unknown key", Expect.NORMALIZED, r -> r.put("zz-extra", "x")));
        m.add(new Mutation("candidates omitted", Expect.NORMALIZED, r -> r.remove("candidates")));
        if (!list(baseline, "candidates").isEmpty()) {
            m.add(new Mutation("candidate: unknown key", Expect.NORMALIZED, r -> obj(list(r, "candidates").get(0)).put("zz-extra", BigDecimal.ONE)));
            m.add(new Mutation("candidate: note omitted", Expect.NORMALIZED, r -> obj(list(r, "candidates").get(0)).remove("note")));
            m.add(new Mutation("candidate: note with control characters and overlong text", Expect.NORMALIZED,
                    r -> obj(list(r, "candidates").get(0)).put("note", "a\nb\t" + "z".repeat(3000))));
            m.add(new Mutation("candidate: unit reference past the text", Expect.REFUSED,
                    r -> obj(list(r, "candidates").get(0)).put("unitId", "L99999")));
            m.add(new Mutation("candidate: duplicate candidate id", Expect.REFUSED, r -> {
                List<Object> c = list(r, "candidates");
                c.add(new LinkedHashMap<>(obj(c.get(0))));
            }));
        }
        m.add(new Mutation("coverage: first range removed", Expect.REFUSED, r -> list(r, "coverage").remove(0)));
        m.add(new Mutation("root: wrong attempt echo", Expect.REFUSED, r -> r.put("attemptIdentity", "0".repeat(64))));
        return m;
    }
}
