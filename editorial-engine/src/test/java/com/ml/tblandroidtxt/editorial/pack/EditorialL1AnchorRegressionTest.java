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

/** Synthetic anchor matrix: the validator checks the declared RAW/DRAFT range and never searches for a new anchor. */
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

    @Test public void exactAnchorAcceptsAndNeighborAnchorRejects() {
        assertEquals(1, parse("raw 1\nraw 2", "draft 1\ndraft 2",
                finding(1, 1, 1, "raw 1", "draft 1")).findings().size());
        expectCode("L1_DRAFT_QUOTE_NOT_IN_ANCHOR", "findings.0.draftQuote",
                () -> parse("raw 1\nraw 2", "draft 1\ndraft 2", finding(1, 1, 1, "raw 1", "draft 2")));
    }

    @Test public void duplicateQuoteStillUsesDeclaredAnchor() {
        expectCode("L1_DRAFT_QUOTE_NOT_IN_ANCHOR", "findings.0.draftQuote",
                () -> parse("raw 1\nraw 2\nraw 3", "same\nsame\nother",
                        finding(1, 3, 3, "raw 1", "same")));
    }

    @Test public void rawAndDraftNamespacesAreIndependent() {
        expectCode("L1_RAW_QUOTE_NOT_IN_ANCHOR", "findings.0.rawQuote",
                () -> parse("raw 1", "draft 1", finding(1, 1, 1, "draft 1", "draft 1")));
        expectCode("L1_DRAFT_QUOTE_NOT_IN_ANCHOR", "findings.0.draftQuote",
                () -> parse("raw 1", "draft 1", finding(1, 1, 1, "raw 1", "raw 1")));
    }

    @Test public void rangeAndMissingTargetRulesAreExplicit() {
        assertEquals(1, parse("raw 1\nraw 2\nraw 3", "draft 1\ndraft 2\ndraft 3",
                finding(1, 1, 2, "raw 1", "draft 2")).findings().size());
        expectCode("L1_DRAFT_QUOTE_NOT_IN_ANCHOR", "findings.0.draftQuote",
                () -> parse("raw 1\nraw 2\nraw 3", "draft 1\ndraft 2\ndraft 3",
                        finding(1, 1, 2, "raw 1", "draft 3")));
        assertEquals(1, parse("raw 1", "draft 1", missingFinding(1, 0, "")).findings().size());
        expectCode("L1_DRAFT_QUOTE_FORBIDDEN_FOR_MISSING", "findings.0.draftQuote",
                () -> parse("raw 1", "draft 1", missingFinding(1, 0, "draft 1")));
        expectCode("L1_DRAFT_QUOTE_REQUIRED", "findings.0.draftQuote",
                () -> parse("raw 1", "draft 1", finding(1, 1, 1, "raw 1", "")));
    }

    @Test public void normalizationRubyAndEmptyCitationRulesApplyWithoutReanchoring() {
        assertEquals(1, parse("cafe\u0301《よみ》", "café", finding(1, 1, 1, " cafe\u0301 ", " café ")).findings().size());
        assertEquals(1, parse("揃《そろ》えても", "draft", finding(1, 1, 1, "揃えても", "draft")).findings().size());
        expectCode("L1_RAW_QUOTE_NOT_IN_ANCHOR", "findings.0.rawQuote",
                () -> parse("揃《そろ》えても", "draft", finding(1, 1, 1, "そろ", "draft")));
        expectCode("L1_DRAFT_QUOTE_NOT_IN_ANCHOR", "findings.0.draftQuote",
                () -> parse("raw 1", "draft", finding(1, 1, 1, "raw 1", " 《よみ》 ")));
    }

    @Test public void crlfTrimAndOutOfRangeAreBoundedToDeclaredLines() {
        assertEquals(1, parse("raw 1\r\nraw 2", "  draft 1  \r\ndraft 2\r\n",
                finding(1, 1, 1, " raw 1 ", " draft 1 ")).findings().size());
        expectCode("L1_DRAFT_ANCHOR_OUT_OF_RANGE", "findings.0.draft",
                () -> parse("raw 1", "draft 1", finding(1, 2, 2, "raw 1", "draft 1")));
    }

    @Test public void savedU6ShapeKeepsDraft99AndRejectsQuoteFrom101() {
        List<String> raw = new ArrayList<>();
        List<String> draft = new ArrayList<>();
        for (int line = 1; line <= 101; line++) {
            raw.add("raw " + line);
            draft.add("draft " + line);
        }
        expectCode("L1_DRAFT_QUOTE_NOT_IN_ANCHOR", "findings.0.draftQuote",
                () -> parse(String.join("\n", raw), String.join("\n", draft),
                        finding(99, 99, 99, "raw 99", "draft 101")));
    }
}
