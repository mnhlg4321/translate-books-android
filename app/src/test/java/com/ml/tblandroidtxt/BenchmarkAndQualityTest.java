package com.ml.tblandroidtxt;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class BenchmarkAndQualityTest {
    @Test public void dryRunUsesIdenticalChunksAndOutputPolicy() {
        AppSettings s = new AppSettings(); s.optimizationPreset="balanced"; s.maxOutputTokens=2048;
        BenchmarkReport report = BenchmarkReport.dryRun(Arrays.asList(new Chunk(0,"before","Alice said hello.","after")), s);
        assertEquals(1, report.rows.size()); assertEquals(2048, report.rows.get(0).baselineMaxOutput);
        assertEquals(report.rows.get(0).baselineMaxOutput, report.rows.get(0).optimizedMaxOutput);
        assertTrue(report.baselineInputTokens > report.optimizedInputTokens);
    }
    @Test public void qualityChecksFindEmptyTruncatedFormattingAndDuplication() {
        assertTrue(TranslationQualityChecks.inspect("source", "", "").issues.contains("EMPTY_OUTPUT"));
        String longSource = "x".repeat(400); assertTrue(TranslationQualityChecks.inspect(longSource,"short"," ").issues.contains("POSSIBLE_OMISSION_OR_TRUNCATION"));
        assertTrue(TranslationQualityChecks.inspect("「text」","「broken","").issues.contains("SYMBOL_OR_FORMAT_LOSS"));
        String repeated="This boundary sentence is deliberately longer than forty characters.";
        assertTrue(TranslationQualityChecks.inspect("source",repeated,repeated).issues.contains("DUPLICATE_BOUNDARY_TEXT"));
    }
}
