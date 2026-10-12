package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Z1: every rejection code of a model-facing parser, receipt or validator is classified once
 * ({@code rejection-classification.csv}, rendered as docs/P6_R6_VALIDATION_RULE_CLASSIFICATION.md). The test makes the
 * table complete by construction: a new code literal that is not in the table fails the build, and a code classified
 * BOOKKEEPING must no longer be a refusal anywhere in the sources.
 */
public final class EditorialRejectionClassificationTest {
    private static final Set<String> LABELS = Set.of("SEMANTIC", "BOOKKEEPING", "PROTOCOL", "APP", "MIXED", "NOT_A_CODE");
    private static final List<String> FILES = List.of("EditorialL1Ledger", "EditorialL1LedgerRun", "EditorialL2Execution",
            "EditorialL2Findings", "EditorialL3Ledger", "EditorialL3Execution", "EditorialFinalRead",
            "EditorialChangeMapReconstructor", "EditorialUnitReference", "EditorialRawInventory",
            "EditorialP5RawWireContract", "EditorialP5RawWireResponse", "EditorialReceiptValidator",
            "EditorialLedgerValidator", "EditorialDiffValidator", "EditorialQaReceiptValidator", "EditorialQaValidator",
            "EditorialQuoteMatcher", "EditorialReferenceNormalization", "EditorialCanonicalJson", "EditorialFieldSpec");
    private static final Pattern LITERAL = Pattern.compile("\"([A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+)(:?)\"");

    private static Path sources() {
        Path[] candidates = {Paths.get("src/main/java/com/ml/tblandroidtxt/editorial/pack"),
                Paths.get("editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack")};
        for (Path candidate : candidates) if (Files.isDirectory(candidate)) return candidate;
        throw new AssertionError("editorial pack sources not found from " + Paths.get("").toAbsolutePath());
    }

    private static Map<String, String[]> table() throws IOException {
        Map<String, String[]> rows = new TreeMap<>();
        try (var in = EditorialRejectionClassificationTest.class.getResourceAsStream("/rejection-classification.csv")) {
            assertTrue("classification table missing", in != null);
            List<String> lines = List.of(new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n"));
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] parts = line.split(",", 4);
                assertEquals(line, 4, parts.length);
                assertTrue("duplicate row " + parts[0], rows.put(parts[0], parts) == null);
            }
        }
        return rows;
    }

    private static Map<String, Set<String>> literals() throws IOException {
        Map<String, Set<String>> found = new TreeMap<>();
        for (String file : FILES) {
            Path path = sources().resolve(file + ".java");
            assertTrue("missing source " + path, Files.exists(path));
            Matcher matcher = LITERAL.matcher(Files.readString(path));
            while (matcher.find()) found.computeIfAbsent(matcher.group(1), k -> new TreeSet<>()).add(file);
        }
        return found;
    }

    @Test public void everyCodeLiteralOfTheParsersIsClassified() throws IOException {
        Map<String, String[]> table = table();
        Set<String> missing = new TreeSet<>();
        for (String code : literals().keySet()) if (!table.containsKey(code)) missing.add(code);
        assertTrue("codes missing from rejection-classification.csv (add a row, then regenerate the doc): " + missing,
                missing.isEmpty());
        for (String[] row : table.values()) assertTrue("unknown label " + row[1] + " for " + row[0], LABELS.contains(row[1]));
    }

    @Test public void aBookkeepingCodeIsNoLongerARefusalAnywhere() throws IOException {
        Map<String, Set<String>> found = literals();
        Set<String> stillThrown = new TreeSet<>();
        for (String[] row : table().values()) {
            if (!"BOOKKEEPING".equals(row[1]) || !found.containsKey(row[0])) continue;
            // a warning-only literal is allowed; its action says so
            if (!row[3].contains("warning")) stillThrown.add(row[0]);
        }
        assertTrue("BOOKKEEPING codes still present in the parsers: " + stillThrown, stillThrown.isEmpty());
    }

    @Test public void theTableHasNoStaleRowsForLiveCodes() throws IOException {
        // every non-removed row names a literal that really exists; removed rows say so in their action
        Map<String, Set<String>> found = literals();
        for (String[] row : table().values()) {
            if (found.containsKey(row[0])) continue;
            assertTrue(row[0] + " is in the table but not in the sources and is not marked removed",
                    "BOOKKEEPING".equals(row[1]) && row[3].contains("removed"));
        }
    }
}
