package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ml.tblandroidtxt.editorial.pack.EditorialWireMutationEngine.Expect;
import static com.ml.tblandroidtxt.editorial.pack.EditorialWireMutationEngine.Mutation;
import static com.ml.tblandroidtxt.editorial.pack.EditorialWireMutationEngine.Outcome;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Z2 on synthetic wires: every bookkeeping variant parses, every semantic variant is refused with a code that the
 * classification table calls SEMANTIC, PROTOCOL or MIXED. The same engine runs offline over the saved live responses
 * (EditorialWireMutationTool), where the baselines are real model output.
 */
public final class EditorialWireMutationSuiteTest {
    // physical lines: 1 text, 2 blank, 3 text, 4 text, 5 blank, 6 text ... 12 text
    private static final String RAW = "raw 1\n\nraw 3\nraw 4\n\nraw 6\nraw 7\nraw 8\nraw 9\nraw 10\nraw 11\nraw 12";
    private static final String DRAFT = "draft 1\n\ndraft 3\ndraft 4\n\ndraft 6\ndraft 7\ndraft 8\ndraft 9\ndraft 10\ndraft 11\ndraft 12";
    private static final Map<String, String> LABELS = EditorialWireMutationEngine.labels();

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }

    private static List<Object> list(Object... values) { return new ArrayList<>(List.of(values)); }

    private static byte[] json(Map<String, Object> value) { return EditorialWireMutationEngine.bytes(value); }

    private static void assertAllOk(String family, List<Outcome> outcomes) {
        StringBuilder bad = new StringBuilder();
        for (Outcome outcome : outcomes) if (!outcome.ok()) bad.append("\n  ").append(outcome.name()).append(": ").append(outcome.why());
        assertTrue(family + " mutation defects:" + bad, bad.length() == 0);
        assertTrue(family + " ran " + outcomes.size(), outcomes.size() >= 5);
    }

    // ---- L1 --------------------------------------------------------------------------------------------------

    private static EditorialRawInventory.Inventory inventory() {
        return EditorialRawInventory.build(RAW.getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, Object> rawBaseline() {
        return map("wireSchemaVersion", EditorialL1Ledger.RAW_WIRE, "attemptIdentity", "raw",
                "coverage", list(map("from", "L1", "to", "L12", "status", "PROCESSED")),
                "candidates", list(map("candidateId", "c1", "ledger", "UNIT", "unitId", "L3", "note", "synthetic"),
                        map("candidateId", "c2", "ledger", "TG", "unitId", "L4", "note", "synthetic")));
    }

    private static Map<String, Object> reconcileBaseline() {
        Map<String, Object> lines = map("errorId", "E1", "type", "MEANING", "severity", "MAJOR",
                "rawUnits", list("L3"),
                "draft", map("kind", "LINES", "start", BigDecimal.valueOf(3), "end", BigDecimal.valueOf(3), "after", BigDecimal.ZERO),
                "rawQuote", "raw 3", "draftQuote", "draft 3", "observation", "observed", "expectedMeaning", "expected",
                "evidenceRefs", list("ref-1"), "candidateIds", list("c1"), "occurrenceUnits", list(),
                "disposition", "OPEN", "evidenceLimit", "");
        Map<String, Object> missing = map("errorId", "E2", "type", "OMISSION", "severity", "MINOR",
                "rawUnits", list("L8"),
                "draft", map("kind", "MISSING", "start", BigDecimal.ZERO, "end", BigDecimal.ZERO, "after", BigDecimal.valueOf(7)),
                "rawQuote", "raw 8", "draftQuote", "", "observation", "observed", "expectedMeaning", "expected",
                "evidenceRefs", list(), "candidateIds", list("c2"), "occurrenceUnits", list(),
                "disposition", "OPEN", "evidenceLimit", "");
        return map("wireSchemaVersion", EditorialL1Ledger.RECONCILE_WIRE, "attemptIdentity", "reconcile",
                "coverage", list(map("from", "L1", "to", "L12", "status", "PROCESSED")),
                "resolutions", list(map("candidateId", "c1", "status", "PROCESSED", "findingRef", "E1"),
                        map("candidateId", "c2", "status", "PROCESSED", "findingRef", "E2")),
                "findings", list(lines, missing),
                "speakerRecords", list(map("unitId", "L4", "speaker", "A", "listener", "B", "basis", "synthetic")),
                "protectedSpans", list(map("spanId", "p1", "start", BigDecimal.valueOf(10), "end", BigDecimal.valueOf(10),
                        "source", "PRONOUN_ROW", "reason", "synthetic")),
                "disposition", map("disposition", "CONTINUE", "reasonCode", "OK", "stopClass", "NONE"));
    }

    @Test public void l1RawVariants() {
        EditorialRawInventory.Inventory inventory = inventory();
        List<Outcome> outcomes = EditorialWireMutationEngine.runAll(json(rawBaseline()),
                EditorialWireMutationEngine.l1Raw(rawBaseline()), wire -> {
                    EditorialL1Ledger.parseRawPass(wire, "raw", inventory);
                    return WireNotes.drain();
                }, LABELS);
        assertAllOk("L1 RAW", outcomes);
    }

    @Test public void l1ReconcileVariants() {
        EditorialRawInventory.Inventory inventory = inventory();
        EditorialL1Ledger.RawPass rawPass = EditorialL1Ledger.parseRawPass(json(rawBaseline()), "raw", inventory);
        List<String> draft = EditorialL1Ledger.draftLines(DRAFT.getBytes(StandardCharsets.UTF_8));
        EditorialWireMutationEngine.Parser parser = wire ->
                EditorialL1Ledger.parseReconcile(wire, "reconcile", inventory, draft, rawPass.candidates()).bookkeepingNotes();
        // the baseline itself must parse
        parser.parse(json(reconcileBaseline()));
        Set<Integer> occupied = Set.of(3, 8);
        List<Outcome> outcomes = EditorialWireMutationEngine.runAll(json(reconcileBaseline()),
                EditorialWireMutationEngine.l1Reconcile(reconcileBaseline(), draft.size(), 2, 12, "L3", occupied, quote -> {
                    int hits = 0;
                    for (EditorialRawInventory.Unit unit : inventory.units()) {
                        if (EditorialQuoteMatcher.containsRaw(unit.text(), quote)) hits++;
                    }
                    return hits;
                }),
                parser, LABELS);
        assertAllOk("L1 RECONCILE", outcomes);
        // the variants that touch bookkeeping really leave a note
        int noted = 0;
        for (Outcome outcome : outcomes) if (outcome.expect() == Expect.NORMALIZED && !outcome.notes().isEmpty()) noted++;
        assertTrue("noted variants " + noted, noted >= 18);
    }

    // ---- L2 edit (row grammar shared with L3) ------------------------------------------------------------------

    private static Map<String, Object> l2Baseline() {
        Map<String, Object> change = map("changeId", "c-1", "errorId", "E1", "line", BigDecimal.ONE, "before", "draft 1",
                "after", "draft one", "reason", "synthetic reason", "dialogue", Boolean.FALSE, "status", "CLOSED");
        Map<String, Object> preserved = map("preserveId", "p-1", "line", BigDecimal.valueOf(3), "before", "draft 3",
                "evidenceLimit", "synthetic limit");
        return map("wireSchemaVersion", EditorialL2Execution.WIRE_SCHEMA_VERSION, "attemptIdentity", "att",
                "resolutions", list(map("candidateId", "u1", "status", "PROCESSED")),
                "changes", list(change), "preserved", list(preserved),
                "disposition", map("disposition", "CONTINUE", "reasonCode", "OK", "stopClass", "NONE"));
    }

    @Test public void l2EditVariants() {
        List<EditorialL2Execution.Candidate> candidates = List.of(new EditorialL2Execution.Candidate("u1", "UNIT", 1));
        EditorialWireMutationEngine.Parser parser = wire -> {
            EditorialL2Execution.EditWire parsed = EditorialL2Execution.parseEditWire(wire, "att", candidates);
            List<String> notes = new ArrayList<>();
            for (String warning : parsed.rows().warnings()) if (warning.startsWith("BOOKKEEPING:")) notes.add(warning.substring(12));
            return notes;
        };
        parser.parse(json(l2Baseline()));
        List<Mutation> m = new ArrayList<>();
        m.add(new Mutation("root: unknown key", Expect.NORMALIZED, r -> r.put("zz", BigDecimal.ONE)));
        m.add(new Mutation("change: unknown key", Expect.NORMALIZED, r -> EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "changes").get(0)).put("zz", BigDecimal.ONE)));
        m.add(new Mutation("preserved: unknown key", Expect.NORMALIZED, r -> EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "preserved").get(0)).put("zz", BigDecimal.ONE)));
        m.add(new Mutation("MAY arrays omitted", Expect.NORMALIZED, r -> {
            r.remove("resolutions");
            r.remove("changes");
            r.remove("preserved");
        }));
        m.add(new Mutation("disposition: stop class and unsafe reason on CONTINUE", Expect.NORMALIZED, r -> {
            Map<String, Object> d = EditorialWireMutationEngine.obj(r.get("disposition"));
            d.put("stopClass", "INPUT_REQUIRED");
            d.put("reasonCode", "bad reason!!");
        }));
        m.add(new Mutation("disposition: empty reason on CONTINUE", Expect.NORMALIZED, r -> EditorialWireMutationEngine.obj(r.get("disposition")).put("reasonCode", "")));
        m.add(new Mutation("disposition: reason and stop class omitted on CONTINUE", Expect.NORMALIZED, r -> {
            EditorialWireMutationEngine.obj(r.get("disposition")).remove("reasonCode");
            EditorialWireMutationEngine.obj(r.get("disposition")).remove("stopClass");
        }));
        m.add(new Mutation("change reason: control characters and overlong text", Expect.NORMALIZED, r ->
                EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "changes").get(0)).put("reason", "a\nb\t" + "r".repeat(5000))));
        m.add(new Mutation("preserved evidenceLimit: control characters and overlong text", Expect.NORMALIZED, r ->
                EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "preserved").get(0)).put("evidenceLimit", "a\nb" + "e".repeat(5000))));
        m.add(new Mutation("root: wrong attempt echo", Expect.REFUSED, r -> r.put("attemptIdentity", "other")));
        m.add(new Mutation("root: wrong wire schema", Expect.REFUSED, r -> r.put("wireSchemaVersion", "safe4.l2.edit.wire.v0")));
        m.add(new Mutation("resolutions: unknown candidate", Expect.REFUSED, r -> EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "resolutions").get(0)).put("candidateId", "nope")));
        m.add(new Mutation("resolutions: forged status", Expect.REFUSED, r -> EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "resolutions").get(0)).put("status", "DONE")));
        m.add(new Mutation("change: forged status", Expect.REFUSED, r -> EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "changes").get(0)).put("status", "OPEN")));
        m.add(new Mutation("change: line not a number", Expect.REFUSED, r -> EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "changes").get(0)).put("line", "one")));
        m.add(new Mutation("disposition: forged kind", Expect.REFUSED, r -> EditorialWireMutationEngine.obj(r.get("disposition")).put("disposition", "PASS")));
        assertAllOk("L2 edit", EditorialWireMutationEngine.runAll(json(l2Baseline()), m, parser, LABELS));
    }

    // ---- L3 reconcile -----------------------------------------------------------------------------------------

    private static Map<String, Object> l3Baseline() {
        return map("wireSchemaVersion", EditorialL3Ledger.RECONCILE_WIRE_V3, "attemptIdentity", "att",
                "resolutions", list(map("candidateId", "t1", "status", "PROCESSED")),
                "carriedResolutions", list(map("index", BigDecimal.ZERO, "status", "UNRESOLVED", "changeIds", list(),
                        "preserveIds", list(), "evidenceQuote", "", "reason", "synthetic")),
                "changes", list(), "preserved", list(),
                "probes", list(map("probeId", "pr-1", "kind", "COVERAGE", "rawUnits", list("L3"),
                        "viStart", BigDecimal.valueOf(3), "viEnd", BigDecimal.valueOf(3), "scope", "synthetic scope",
                        "contrast", "synthetic contrast", "rawQuote", "raw 3", "viQuote", "draft 3", "verdict", "NO_DEFECT",
                        "action", "NONE")),
                "disposition", map("disposition", "CONTINUE", "reasonCode", "OK", "stopClass", "NONE"));
    }

    @Test public void l3ReconcileVariants() {
        EditorialRawInventory.Inventory inventory = inventory();
        List<EditorialL3Ledger.Candidate> candidates = List.of(new EditorialL3Ledger.Candidate("t1", "UNIT",
                inventory.units().get(0).id(), 1, "PROCESSED", "n"));
        byte[] base = DRAFT.getBytes(StandardCharsets.UTF_8);
        EditorialWireMutationEngine.Parser parser = wire -> {
            EditorialL3Ledger.ReconcileWire parsed = EditorialL3Ledger.parseReconcile(wire, "att", candidates, 1, inventory, base);
            List<String> notes = new ArrayList<>();
            for (String warning : parsed.rows().warnings()) if (warning.startsWith("BOOKKEEPING:")) notes.add(warning.substring(12));
            return notes;
        };
        parser.parse(json(l3Baseline()));
        List<Mutation> m = new ArrayList<>();
        m.add(new Mutation("root: unknown key", Expect.NORMALIZED, r -> r.put("zz", BigDecimal.ONE)));
        m.add(new Mutation("MAY arrays omitted", Expect.NORMALIZED, r -> {
            r.remove("resolutions");
            r.remove("carriedResolutions");
            r.remove("changes");
            r.remove("preserved");
            r.remove("probes");
        }));
        m.add(new Mutation("carried: optional keys omitted", Expect.NORMALIZED, r -> {
            Map<String, Object> c = EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "carriedResolutions").get(0));
            c.remove("changeIds");
            c.remove("preserveIds");
            c.remove("evidenceQuote");
        }));
        m.add(new Mutation("carried: duplicated change ids", Expect.NORMALIZED, r -> {
            Map<String, Object> c = EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "carriedResolutions").get(0));
            c.put("changeIds", list("a", "a", "a"));
        }));
        m.add(new Mutation("carried: overlong reason", Expect.NORMALIZED, r ->
                EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "carriedResolutions").get(0)).put("reason", "r\n".repeat(400))));
        m.add(new Mutation("probe: overlong scope and contrast with control characters", Expect.NORMALIZED, r -> {
            Map<String, Object> p = EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "probes").get(0));
            p.put("scope", "s\n" + "x".repeat(3000));
            p.put("contrast", "c\t" + "y".repeat(3000));
        }));
        m.add(new Mutation("probe: viStart/viEnd far outside the text", Expect.NORMALIZED, r -> {
            Map<String, Object> p = EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "probes").get(0));
            p.put("viStart", BigDecimal.valueOf(900));
            p.put("viEnd", BigDecimal.valueOf(950));
        }));
        m.add(new Mutation("probe: unknown key", Expect.NORMALIZED, r ->
                EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "probes").get(0)).put("zz", BigDecimal.ONE)));
        m.add(new Mutation("disposition: stop class and unsafe reason on CONTINUE", Expect.NORMALIZED, r -> {
            Map<String, Object> d = EditorialWireMutationEngine.obj(r.get("disposition"));
            d.put("stopClass", "CONTENT_BLOCKED");
            d.put("reasonCode", "###");
        }));
        m.add(new Mutation("root: wrong attempt echo", Expect.REFUSED, r -> r.put("attemptIdentity", "other")));
        m.add(new Mutation("resolutions: unknown candidate", Expect.REFUSED, r ->
                EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "resolutions").get(0)).put("candidateId", "nope")));
        m.add(new Mutation("carried: index outside", Expect.REFUSED, r ->
                EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "carriedResolutions").get(0)).put("index", BigDecimal.valueOf(9))));
        m.add(new Mutation("probe: forged verdict", Expect.REFUSED, r ->
                EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "probes").get(0)).put("verdict", "MAYBE")));
        assertAllOk("L3 reconcile", EditorialWireMutationEngine.runAll(json(l3Baseline()), m, parser, LABELS));
    }

    // ---- final read -------------------------------------------------------------------------------------------

    @Test public void finalReadVariants() {
        byte[] target = "alpha line\nbeta line\ngamma line\ndelta line".getBytes(StandardCharsets.UTF_8);
        List<Object> tails = new ArrayList<>();
        for (Integer line : EditorialFinalRead.probeLines(target)) {
            String text = EditorialFinalRead.lines(target).get(line - 1);
            tails.add(map("line", BigDecimal.valueOf(line), "tail", text.length() <= EditorialFinalRead.TAIL_LENGTH
                    ? text : text.substring(text.length() - EditorialFinalRead.TAIL_LENGTH)));
        }
        Map<String, Object> baseline = map("wireSchemaVersion", EditorialFinalRead.WIRE, "attemptIdentity", "att",
                "readSha256", EditorialCanonicalJson.sha256Hex(target), "probeTails", tails, "verdict", "DEFECTS",
                "defects", list(map("line", BigDecimal.ONE, "quote", "alpha", "type", "MEANING", "note", "synthetic note")));
        EditorialWireMutationEngine.Parser parser = wire ->
                EditorialFinalRead.parse(wire, "att", target, EditorialFinalRead.L3_PHASE).bookkeepingNotes();
        parser.parse(json(baseline));
        List<Mutation> m = new ArrayList<>();
        m.add(new Mutation("root: unknown key", Expect.NORMALIZED, r -> r.put("zz", BigDecimal.ONE)));
        m.add(new Mutation("defect: unknown key", Expect.NORMALIZED, r -> EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "defects").get(0)).put("zz", BigDecimal.ONE)));
        m.add(new Mutation("defect: overlong note with control characters", Expect.NORMALIZED, r ->
                EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "defects").get(0)).put("note", "n\n" + "z".repeat(3000))));
        m.add(new Mutation("CLEAN with defects key omitted", Expect.NORMALIZED, r -> {
            r.put("verdict", "CLEAN");
            r.remove("defects");
        }));
        m.add(new Mutation("defect list beyond the cap", Expect.NORMALIZED, r -> {
            List<Object> defects = EditorialWireMutationEngine.list(r, "defects");
            for (int i = 0; i < EditorialFinalRead.MAX_DEFECTS + 4; i++) defects.add(new LinkedHashMap<>(EditorialWireMutationEngine.obj(defects.get(0))));
        }));
        m.add(new Mutation("root: wrong attempt echo", Expect.REFUSED, r -> r.put("attemptIdentity", "other")));
        m.add(new Mutation("root: wrong hash echo", Expect.REFUSED, r -> r.put("readSha256", "0".repeat(64))));
        m.add(new Mutation("probe tail altered", Expect.REFUSED, r ->
                EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "probeTails").get(0)).put("tail", "zzz")));
        m.add(new Mutation("verdict missing", Expect.REFUSED, r -> r.remove("verdict")));
        m.add(new Mutation("defect: quote not on its line", Expect.REFUSED, r ->
                EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "defects").get(0)).put("quote", "not-here")));
        m.add(new Mutation("defect: line outside", Expect.REFUSED, r ->
                EditorialWireMutationEngine.obj(EditorialWireMutationEngine.list(r, "defects").get(0)).put("line", BigDecimal.valueOf(99))));
        m.add(new Mutation("CLEAN while a defect is listed", Expect.REFUSED, r -> r.put("verdict", "CLEAN")));
        assertAllOk("final read", EditorialWireMutationEngine.runAll(json(baseline), m, parser, LABELS));
    }

    @Test public void noBookkeepingMutationIsRefusedWithAClassifiedBookkeepingCode() {
        // the engine itself: a refusal classified BOOKKEEPING counts as a defect even for a REFUSED expectation
        Map<String, String> labels = new LinkedHashMap<>(LABELS);
        labels.put("L1_WIRE_ATTEMPT_ECHO_MISMATCH", "BOOKKEEPING");
        EditorialRawInventory.Inventory inventory = inventory();
        Outcome outcome = EditorialWireMutationEngine.run(json(rawBaseline()),
                new Mutation("echo", Expect.REFUSED, r -> r.put("attemptIdentity", "other")),
                wire -> {
                    EditorialL1Ledger.parseRawPass(wire, "raw", inventory);
                    return List.of();
                }, labels);
        assertEquals(false, outcome.ok());
    }
}
