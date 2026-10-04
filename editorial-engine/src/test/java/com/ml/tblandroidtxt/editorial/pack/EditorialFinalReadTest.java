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

/** The final-read primitive: the model must echo the hash and the tails of app-chosen lines. Synthetic text. */
public final class EditorialFinalReadTest {
    private static final String ATT = "att-1";

    private static byte[] b(String s) { return s.getBytes(StandardCharsets.UTF_8); }

    private static String target() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 40; i++) sb.append("Dong so ").append(i).append(" co noi dung rieng so ").append(i * 7).append('\n');
        return sb.toString();
    }

    private static Map<String, Object> goodWire(byte[] target) {
        List<String> lines = EditorialFinalRead.lines(target);
        List<Object> tails = new ArrayList<>();
        for (Integer line : EditorialFinalRead.probeLines(target)) {
            String text = lines.get(line - 1);
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("line", BigDecimal.valueOf(line));
            t.put("tail", text.length() <= 12 ? text : text.substring(text.length() - 12));
            tails.add(t);
        }
        Map<String, Object> w = new LinkedHashMap<>();
        w.put("wireSchemaVersion", EditorialFinalRead.WIRE);
        w.put("attemptIdentity", ATT);
        w.put("readSha256", EditorialCanonicalJson.sha256Hex(target));
        w.put("probeTails", tails);
        w.put("verdict", "CLEAN");
        w.put("defects", new ArrayList<Object>());
        return w;
    }

    private static byte[] json(Map<String, Object> m) { return EditorialCanonicalJson.canonicalize(m).getBytes(StandardCharsets.UTF_8); }

    private static void expect(String code, byte[] response, byte[] target) {
        try {
            EditorialFinalRead.parse(response, ATT, target);
            fail("expected " + code);
        } catch (IllegalArgumentException e) {
            assertEquals(code, e.getMessage());
        }
    }

    @Test public void probeLinesAreDeterministicDistinctNonEmptyAndDependOnTheBytes() {
        byte[] t = b(target());
        List<Integer> a = EditorialFinalRead.probeLines(t);
        assertEquals(a, EditorialFinalRead.probeLines(t));
        assertEquals(3, a.size());
        assertEquals(3, new java.util.HashSet<>(a).size());
        List<String> lines = EditorialFinalRead.lines(t);
        for (Integer line : a) assertTrue(!lines.get(line - 1).isBlank());
        // blank lines are never probed; short texts probe what exists
        assertEquals(List.of(1, 3), EditorialFinalRead.probeLines(b("x\n\ny")));
        assertEquals(List.of(), EditorialFinalRead.probeLines(b("\n\n")));
    }

    @Test public void aCorrectEchoIsAccepted() {
        byte[] t = b(target());
        EditorialFinalRead.Result r = EditorialFinalRead.parse(json(goodWire(t)), ATT, t);
        assertEquals("CLEAN", r.verdict());
        assertEquals(41, r.lineCount());
        assertEquals(EditorialCanonicalJson.sha256Hex(t), r.targetSha256());
    }

    @Test public void crlfAndBomDoNotChangeTheLinesOrTheProbes() {
        byte[] lf = b("a1\nb2\nc3\nd4\ne5\n");
        byte[] crlf = b("﻿a1\r\nb2\r\nc3\r\nd4\r\ne5\r\n");
        assertEquals(EditorialFinalRead.lines(lf), EditorialFinalRead.lines(crlf));
        EditorialFinalRead.Result r = EditorialFinalRead.parse(json(goodWire(crlf)), ATT, crlf);
        assertEquals(6, r.lineCount());
    }

    @Test public void echoMismatchesAreTypedRefusals() {
        byte[] t = b(target());
        Map<String, Object> w = goodWire(t);
        w.put("readSha256", "0".repeat(64));
        expect("FINAL_READ_HASH_ECHO_MISMATCH", json(w), t);
        w = goodWire(t);
        w.put("attemptIdentity", "other");
        expect("FINAL_READ_ATTEMPT_ECHO_MISMATCH", json(w), t);
        w = goodWire(t);
        @SuppressWarnings("unchecked") List<Object> tails = (List<Object>) w.get("probeTails");
        @SuppressWarnings("unchecked") Map<String, Object> first = (Map<String, Object>) tails.get(0);
        first.put("tail", "wrong tail!!");
        expect("FINAL_READ_PROBE_TAIL_MISMATCH", json(w), t);
        w = goodWire(t);
        ((List<?>) w.get("probeTails")).remove(0);
        expect("FINAL_READ_PROBE_SET_MISMATCH", json(w), t);
        // a read of different bytes cannot be replayed against this target
        byte[] other = b(target() + "extra\n");
        expect("FINAL_READ_HASH_ECHO_MISMATCH", json(goodWire(t)), other);
    }

    @Test public void hashEchoFailureIncludesItsWireFieldPath() {
        byte[] t = b(target());
        Map<String, Object> wire = goodWire(t);
        wire.put("readSha256", "0".repeat(64));
        try {
            EditorialFinalRead.parse(json(wire), ATT, t);
            fail("expected hash echo rejection");
        } catch (RuntimeException invalid) {
            assertEquals("FINAL_READ_HASH_ECHO_MISMATCH:readSha256",
                    WireViolation.safeMessage(invalid, "FINAL_READ_PARSE_FAILED"));
        }
    }

    @Test public void defectsAreCheckedAgainstTheBuiltLines() {
        byte[] t = b(target());
        Map<String, Object> w = goodWire(t);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("line", BigDecimal.valueOf(5));
        d.put("quote", "co noi dung");
        d.put("type", "MEANING");
        d.put("note", "n");
        w.put("verdict", "DEFECTS");
        w.put("defects", new ArrayList<Object>(List.of(d)));
        EditorialFinalRead.Result r = EditorialFinalRead.parse(json(w), ATT, t);
        assertEquals(1, r.defects().size());
        // evidence round trip
        Map<String, Object> block = EditorialCanonicalJson.parseObject(
                EditorialCanonicalJson.canonicalize(EditorialFinalRead.evidence(EditorialFinalRead.L2_PHASE, r)).getBytes(StandardCharsets.UTF_8));
        assertEquals(r, EditorialFinalRead.parseEvidence(block));
        d.put("quote", "not in the line");
        expect("FINAL_READ_DEFECT_QUOTE_NOT_IN_LINE", json(w), t);
        d.put("quote", "co noi dung");
        d.put("line", BigDecimal.valueOf(99));
        expect("FINAL_READ_DEFECT_LINE_OUT_OF_RANGE", json(w), t);
        d.put("line", BigDecimal.valueOf(5));
        d.put("type", "VIBES");
        expect("FINAL_READ_DEFECT_TYPE_INVALID", json(w), t);
        d.put("type", "MEANING");
        w.put("verdict", "CLEAN");
        expect("FINAL_READ_VERDICT_DEFECT_MISMATCH", json(w), t);
        w.put("verdict", "DEFECTS");
        w.put("defects", new ArrayList<Object>());
        expect("FINAL_READ_VERDICT_DEFECT_MISMATCH", json(w), t);
    }

    @Test public void defectQuotesNormalizeNfcAndOuterWhitespaceWithinTheSelectedLine() {
        byte[] t = b("first line\nCafe\u0301 au lait\nthird line\nfourth line\nfifth line\n");
        Map<String, Object> wire = goodWire(t);
        wire.put("verdict", "DEFECTS");
        wire.put("defects", new ArrayList<Object>(List.of(Map.of(
                "line", BigDecimal.valueOf(2), "quote", " \u00e9 ", "type", "MEANING", "note", "n"))));
        EditorialFinalRead.Result result = EditorialFinalRead.parse(json(wire), ATT, t);
        assertEquals(1, result.defects().size());
        assertEquals(" \u00e9 ", result.defects().get(0).quote());
    }

    @Test public void unknownKeysAreIgnoredAndNotedButOversizeIsRefused() {
        byte[] t = b(target());
        Map<String, Object> w = goodWire(t);
        w.put("extra", "x");
        EditorialFinalRead.Result noted = EditorialFinalRead.parse(json(w), ATT, t);
        assertTrue(noted.bookkeepingNotes().contains("unknownKeyIgnored:root"));
        w.remove("extra");
        w.remove("verdict");
        expect("FINAL_READ_KEYS_INVALID", json(w), t);
        expect("FINAL_READ_WIRE_BYTE_LIMIT_EXCEEDED", new byte[EditorialFinalRead.MAX_WIRE_BYTES + 1], t);
    }

    @Test public void probeBlockCarriesHashAndLines() {
        byte[] t = b(target());
        Map<String, Object> block = EditorialCanonicalJson.parseObject(EditorialFinalRead.probeBlock(t));
        assertEquals(EditorialCanonicalJson.sha256Hex(t), block.get("targetSha256"));
        assertEquals(3, ((List<?>) block.get("probeLines")).size());
    }

    @Test @SuppressWarnings("unchecked") public void schemaRequiresExactlyTheWireKeys() {
        Map<String, Object> schema = EditorialFinalRead.jsonSchema();
        assertEquals(new java.util.HashSet<>(goodWire(b(target())).keySet()),
                new java.util.HashSet<>((List<Object>) schema.get("required")));
    }
}
