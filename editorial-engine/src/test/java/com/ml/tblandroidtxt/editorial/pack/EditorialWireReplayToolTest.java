package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class EditorialWireReplayToolTest {
    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }

    private static byte[] json(Map<String, Object> value) {
        return EditorialCanonicalJson.canonicalize(value).getBytes(StandardCharsets.UTF_8);
    }

    @Test public void replaysCapturedReconcileAndReportsTheSameSafePath() throws Exception {
        Path root = Files.createTempDirectory("p6-wire-replay-");
        try {
            Path raw = root.resolve("RAW.txt");
            Path draft = root.resolve("DRAFT.txt");
            Path rawResponse = root.resolve("001-L1_RAW_DISCOVERY.json");
            Path reconcileResponse = root.resolve("002-L1_RECONCILE.json");
            Files.writeString(raw, "雨が降る。\n彼は歩いた。", StandardCharsets.UTF_8);
            Files.writeString(draft, "Troi mua.\nAnh di.", StandardCharsets.UTF_8);

            Map<String, Object> rawWire = map(
                    "wireSchemaVersion", EditorialL1Ledger.RAW_WIRE,
                    "attemptIdentity", "raw-attempt",
                    "coverage", List.of(map("from", "L1", "to", "L2", "status", "PROCESSED")),
                    "candidates", List.of());
            Files.write(rawResponse, json(rawWire));

            Map<String, Object> finding = map(
                    "errorId", "E1", "type", "MEANING", "severity", "MAJOR",
                    "rawUnits", List.of("L1"),
                    "draft", map("kind", "LINES", "start", BigDecimal.ONE,
                            "end", BigDecimal.ONE, "after", BigDecimal.ZERO),
                    "rawQuote", "雨", "draftQuote", "Troi",
                    "observation", "", "expectedMeaning", "expected",
                    "evidenceRefs", List.of(), "candidateIds", List.of(), "occurrenceUnits", List.of(),
                    "disposition", "OPEN", "evidenceLimit", "");
            Map<String, Object> reconcileWire = map(
                    "wireSchemaVersion", EditorialL1Ledger.RECONCILE_WIRE,
                    "attemptIdentity", "reconcile-attempt",
                    "coverage", List.of(map("from", "L1", "to", "L2", "status", "PROCESSED")),
                    "resolutions", List.of(), "findings", List.of(finding),
                    "speakerRecords", List.of(), "protectedSpans", List.of(),
                    "disposition", map("disposition", "CONTINUE", "reasonCode", "OK", "stopClass", "NONE"));
            Files.write(reconcileResponse, json(reconcileWire));

            try {
                EditorialWireReplayTool.run(new String[]{"L1_RECONCILE", reconcileResponse.toString(),
                        raw.toString(), draft.toString(), rawResponse.toString()});
                fail("expected a parser refusal");
            } catch (WireViolation refusal) {
                assertTrue(WireViolation.safeMessage(refusal, "REPLAY_FAILED")
                        .startsWith("L1_TEXT_REQUIRED:findings.0.observation"));
            }
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (java.io.IOException ignored) { }
                });
            }
        }
    }
}
