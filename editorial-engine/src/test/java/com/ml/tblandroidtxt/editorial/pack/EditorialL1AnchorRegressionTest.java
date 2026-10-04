package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Synthetic anchor matrix: line numbers are hints and the app derives the DRAFT anchor from the quote. */
public final class EditorialL1AnchorRegressionTest {
    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }

    private static Map<String, Object> finding(int rawLine, int draftStart, int draftEnd,
                                                String rawQuote, String draftQuote) {
        return map("errorId", "E" + rawLine + "_" + draftStart, "type", "MEANING", "severity", "MAJOR",
                "rawUnits", List.of("L" + rawLine),
                "draft", map("kind", "LINES", "start", BigDecimal.valueOf(draftStart),
                        "end", BigDecimal.valueOf(draftEnd), "after", BigDecimal.ZERO),
                "rawQuote", rawQuote, "draftQuote", draftQuote,
                "observation", "observed", "expectedMeaning", "expected",
                "evidenceRefs", List.of(), "candidateIds", List.of(), "occurrenceUnits", List.of(),
                "disposition", "OPEN", "evidenceLimit", "");
    }

    private static Map<String, Object> missingFinding(int rawLine, int after, String draftQuote) {
        return map("errorId", "MISSING_" + rawLine + "_" + after, "type", "OMISSION", "severity", "MAJOR",
                "rawUnits", List.of("L" + rawLine),
                "draft", map("kind", "MISSING", "start", BigDecimal.ZERO, "end", BigDecimal.ZERO,
                        "after", BigDecimal.valueOf(after)),
                "rawQuote", "raw " + rawLine, "draftQuote", draftQuote,
                "observation", "observed", "expectedMeaning", "expected",
                "evidenceRefs", List.of(), "candidateIds", List.of(), "occurrenceUnits", List.of(),
                "disposition", "OPEN", "evidenceLimit", "");
    }

    private static EditorialL1Ledger.ReconcilePass parse(String raw, String draft, Map<String, Object> finding) {
        String[] rawRows = raw.split("\\n", -1);
        Map<String, Object> rawWire = map("wireSchemaVersion", EditorialL1Ledger.RAW_WIRE,
                "attemptIdentity", "raw", "coverage", List.of(map("from", "L1", "to", "L" + rawRows.length,
                        "status", "PROCESSED")), "candidates", List.of());
        EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(raw.getBytes(StandardCharsets.UTF_8));
        EditorialL1Ledger.RawPass rawPass = EditorialL1Ledger.parseRawPass(
                json(rawWire), "raw", inventory);
        String[] draftRows = draft.split("\\n", -1);
        Map<String, Object> reconcile = map("wireSchemaVersion", EditorialL1Ledger.RECONCILE_WIRE,
                "attemptIdentity", "reconcile", "coverage", List.of(map("from", "L1", "to", "L" + rawRows.length,
                        "status", "PROCESSED")), "resolutions", List.of(), "findings", List.of(finding),
                "speakerRecords", List.of(), "protectedSpans", List.of(),
                "disposition", map("disposition", "CONTINUE", "reasonCode", "OK", "stopClass", "NONE"));
        return EditorialL1Ledger.parseReconcile(json(reconcile), "reconcile", inventory,
                EditorialL1Ledger.draftLines(draft.getBytes(StandardCharsets.UTF_8)), rawPass.candidates());
    }

    private static byte[] json(Map<String, Object> value) {
        return EditorialCanonicalJson.canonicalize(value).getBytes(StandardCharsets.UTF_8);
    }

    private static void expectCode(String code, String path, Throwing action) {
        try {
            action.run();
            fail("expected " + code + ":" + path);
        } catch (WireViolation invalid) {
            assertEquals(code, invalid.code());
            assertEquals(path, invalid.path());
        }
    }

    @FunctionalInterface private interface Throwing { void run(); }

    private static List<String> numbered(String prefix, int count) {
        List<String> rows = new ArrayList<>();
        for (int line = 1; line <= count; line++) rows.add(prefix + " " + line);
        return rows;
    }

    @Test public void quoteOnTheHintedLineKeepsTheHintAndRecordsNothing() {
        EditorialL1Ledger.ReconcilePass pass = parse("raw 1\nraw 2", "draft 1\ndraft 2", finding(1, 1, 1, "raw 1", "draft 1"));
        assertEquals(EditorialL1Ledger.DraftAnchor.lines(1, 1), pass.findings().get(0).draft());
        assertEquals(0, pass.draftAnchorsDerivedFromQuote());
        assertEquals(0, pass.maxDraftAnchorDeviation());
    }

    @Test public void uniqueQuoteElsewhereMovesTheAnchorAndRecordsTheDeviation() {
        EditorialL1Ledger.ReconcilePass pass = parse("raw 1\nraw 2", "draft 1\ndraft 2", finding(1, 1, 1, "raw 1", "draft 2"));
        assertEquals(EditorialL1Ledger.DraftAnchor.lines(2, 2), pass.findings().get(0).draft());
        assertEquals(1, pass.draftAnchorsDerivedFromQuote());
        assertEquals(1, pass.maxDraftAnchorDeviation());
    }

    @Test public void savedU6ShapeIsAnchoredToTheQuoteLine() {
        List<String> raw = numbered("raw", 101);
        List<String> draft = numbered("draft", 101);
        EditorialL1Ledger.ReconcilePass pass = parse(String.join("\n", raw), String.join("\n", draft),
                finding(99, 99, 99, "raw 99", "draft 101"));
        assertEquals(EditorialL1Ledger.DraftAnchor.lines(101, 101), pass.findings().get(0).draft());
        assertEquals(1, pass.draftAnchorsDerivedFromQuote());
        assertEquals(2, pass.maxDraftAnchorDeviation());
    }

    @Test public void multiLineHintKeepsItsLengthFromTheQuoteLine() {
        EditorialL1Ledger.ReconcilePass pass = parse("raw 1\nraw 2\nraw 3", "draft 1\ndraft 2\ndraft 3\ndraft 4",
                finding(1, 1, 2, "raw 1", "draft 3"));
        assertEquals(EditorialL1Ledger.DraftAnchor.lines(3, 4), pass.findings().get(0).draft());
        // the quote line already inside the hinted span keeps the span as hinted
        assertEquals(EditorialL1Ledger.DraftAnchor.lines(1, 2), parse("raw 1\nraw 2\nraw 3", "draft 1\ndraft 2\ndraft 3",
                finding(1, 1, 2, "raw 1", "draft 2")).findings().get(0).draft());
        // a span that would run past the end is clamped to the last line
        assertEquals(EditorialL1Ledger.DraftAnchor.lines(3, 3), parse("raw 1\nraw 2\nraw 3", "draft 1\ndraft 2\ndraft 3",
                finding(1, 1, 2, "raw 1", "draft 3")).findings().get(0).draft());
    }

    @Test public void repeatedQuoteIsDisambiguatedByTheNearestLineInsideTheWindow() {
        // repeated on 2 and 6: a hint on line 5 is one line from 6 and three from 2
        EditorialL1Ledger.ReconcilePass pass = parse("raw 1\nraw 2\nraw 3\nraw 4\nraw 5\nraw 6\nraw 7",
                "x\nsame\ny\nz\nw\nsame\nv", finding(1, 5, 5, "raw 1", "same"));
        assertEquals(EditorialL1Ledger.DraftAnchor.lines(6, 6), pass.findings().get(0).draft());
        // a hint exactly on one of the repeats coincides with it
        assertEquals(EditorialL1Ledger.DraftAnchor.lines(2, 2), parse("raw 1\nraw 2\nraw 3\nraw 4\nraw 5\nraw 6\nraw 7",
                "x\nsame\ny\nz\nw\nsame\nv", finding(1, 2, 2, "raw 1", "same")).findings().get(0).draft());
    }

    @Test public void repeatedQuoteWithoutAUniqueNearestLineIsAmbiguous() {
        // equidistant from the hint
        expectCode("L1_DRAFT_QUOTE_AMBIGUOUS", "findings.0.draftQuote",
                () -> parse("raw 1\nraw 2\nraw 3", "same\nother\nsame", finding(1, 2, 2, "raw 1", "same")));
        // both repeats are outside the +-3 window
        List<String> draft = new ArrayList<>(numbered("draft", 12));
        draft.set(0, "same");
        draft.set(11, "same");
        expectCode("L1_DRAFT_QUOTE_AMBIGUOUS", "findings.0.draftQuote",
                () -> parse(String.join("\n", numbered("raw", 12)), String.join("\n", draft), finding(1, 6, 6, "raw 1", "same")));
    }

    @Test public void quoteMissingFromTheWholeDraftIsStillRejected() {
        expectCode("L1_DRAFT_QUOTE_NOT_IN_ANCHOR", "findings.0.draftQuote",
                () -> parse("raw 1\nraw 2", "draft 1\ndraft 2", finding(1, 1, 1, "raw 1", "draft 3")));
        expectCode("L1_DRAFT_QUOTE_NOT_IN_ANCHOR", "findings.0.draftQuote",
                () -> parse("raw 1", "draft 1", finding(1, 1, 1, "raw 1", "raw 1")));
    }

    @Test public void rawAndDraftNamespacesAreIndependent() {
        expectCode("L1_RAW_QUOTE_NOT_IN_ANCHOR", "findings.0.rawQuote",
                () -> parse("raw 1", "draft 1", finding(1, 1, 1, "draft 1", "draft 1")));
    }

    @Test public void missingTargetAndEmptyQuoteRulesAreUnchanged() {
        assertEquals(1, parse("raw 1", "draft 1", missingFinding(1, 0, "")).findings().size());
        assertEquals(EditorialL1Ledger.DraftAnchor.missingAfter(0),
                parse("raw 1", "draft 1", missingFinding(1, 0, "")).findings().get(0).draft());
        expectCode("L1_DRAFT_QUOTE_FORBIDDEN_FOR_MISSING", "findings.0.draftQuote",
                () -> parse("raw 1", "draft 1", missingFinding(1, 0, "draft 1")));
        expectCode("L1_DRAFT_ANCHOR_OUT_OF_RANGE", "findings.0.draft.after",
                () -> parse("raw 1", "draft 1", missingFinding(1, 5, "")));
        expectCode("L1_DRAFT_QUOTE_REQUIRED", "findings.0.draftQuote",
                () -> parse("raw 1", "draft 1", finding(1, 1, 1, "raw 1", "")));
    }

    @Test public void normalizationRubyAndEmptyCitationRulesApply() {
        assertEquals(1, parse("café《よみ》", "café", finding(1, 1, 1, " café ", " café ")).findings().size());
        assertEquals(1, parse("ab《cd》ef", "draft", finding(1, 1, 1, "abef", "draft")).findings().size());
        expectCode("L1_RAW_QUOTE_NOT_IN_ANCHOR", "findings.0.rawQuote",
                () -> parse("ab《cd》ef", "draft", finding(1, 1, 1, "cd", "draft")));
        // a quote that is empty once ruby markup and whitespace are gone has nothing to search for
        expectCode("L1_DRAFT_QUOTE_NOT_IN_ANCHOR", "findings.0.draftQuote",
                () -> parse("raw 1", "draft", finding(1, 1, 1, "raw 1", " 《よみ》 ")));
    }

    @Test public void crlfTrimAndOutOfRangeNumbersAreOnlyHints() {
        assertEquals(1, parse("raw 1\r\nraw 2", "  draft 1  \r\ndraft 2\r\n",
                finding(1, 1, 1, " raw 1 ", " draft 1 ")).findings().size());
        // a hint outside the draft is clamped; the quote still decides
        EditorialL1Ledger.ReconcilePass pass = parse("raw 1", "draft 1", finding(1, 2, 2, "raw 1", "draft 1"));
        assertEquals(EditorialL1Ledger.DraftAnchor.lines(1, 1), pass.findings().get(0).draft());
        assertEquals(0, pass.draftAnchorsDerivedFromQuote());
        EditorialL1Ledger.ReconcilePass zero = parse("raw 1\nraw 2", "draft 1\ndraft 2", finding(1, 0, 0, "raw 1", "draft 2"));
        assertEquals(EditorialL1Ledger.DraftAnchor.lines(2, 2), zero.findings().get(0).draft());
    }

    @Test public void derivationIsPersistedInTheReportBodyAndRestored() {
        EditorialRawInventory.Inventory inventory = EditorialRawInventory.build("raw 1\nraw 2".getBytes(StandardCharsets.UTF_8));
        EditorialL1Ledger.RawPass rawPass = new EditorialL1Ledger.RawPass(
                List.of(new EditorialRawInventory.Range("L1", "L2", "PROCESSED")), List.of());
        EditorialL1Ledger.ReconcilePass pass = parse("raw 1\nraw 2", "draft 1\ndraft 2", finding(1, 1, 1, "raw 1", "draft 2"));
        EditorialL1Ledger.Body body = EditorialL1Ledger.bodyOfReconcile(inventory, rawPass, pass);
        Map<String, Object> artifact = EditorialL1Ledger.bodyToMap(body);
        @SuppressWarnings("unchecked") Map<String, Object> normalizations = (Map<String, Object>) artifact.get("normalizations");
        assertEquals(1, ((BigDecimal) normalizations.get("draftAnchorDerivedFromQuote")).intValueExact());
        assertEquals(1, ((BigDecimal) normalizations.get("maxDraftAnchorDeviation")).intValueExact());
        EditorialL1Ledger.Body restored = EditorialL1Ledger.parseBody(artifact);
        assertEquals(1, restored.draftAnchorsDerivedFromQuote());
        assertEquals(1, restored.maxDraftAnchorDeviation());
        assertEquals(EditorialContractRevision.CURRENT_LEDGER, artifact.get("contractRevision"));
        assertTrue(EditorialContractRevision.isLedger(EditorialContractRevision.L1_LEDGER_V8));
    }
}
