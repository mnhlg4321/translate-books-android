package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Process-level contract tests for the offline replay CLI and its bounded diagnostics. */
public final class EditorialWireReplayToolTest {
    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }

    private static byte[] json(Map<String, Object> value) {
        return EditorialCanonicalJson.canonicalize(value).getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, Object> finding(String id, int line, String draftQuote) {
        return map("errorId", id, "type", "MEANING", "severity", "MAJOR",
                "rawUnits", List.of("L" + line),
                "draft", map("kind", "LINES", "start", BigDecimal.valueOf(line),
                        "end", BigDecimal.valueOf(line), "after", BigDecimal.ZERO),
                "rawQuote", "raw " + line, "draftQuote", draftQuote,
                "observation", "observed", "expectedMeaning", "expected",
                "evidenceRefs", List.of(), "candidateIds", List.of(), "occurrenceUnits", List.of(),
                "disposition", "OPEN", "evidenceLimit", "");
    }

    private static Fixture fixture(Path root, List<Map<String, Object>> findings) throws IOException {
        Path raw = root.resolve("RAW.txt");
        Path draft = root.resolve("DRAFT.txt");
        Path rawResponse = root.resolve("001-L1_RAW_DISCOVERY.json");
        Path reconcileResponse = root.resolve("002-L1_RECONCILE.json");
        Files.writeString(raw, "raw 1\nraw 2\nraw 3", StandardCharsets.UTF_8);
        Files.writeString(draft, "draft 1\ndraft 2\ndraft 3", StandardCharsets.UTF_8);
        Files.write(rawResponse, json(map(
                "wireSchemaVersion", EditorialL1Ledger.RAW_WIRE,
                "attemptIdentity", "raw-attempt",
                "coverage", List.of(map("from", "L1", "to", "L3", "status", "PROCESSED")),
                "candidates", List.of())));
        Files.write(reconcileResponse, json(map(
                "wireSchemaVersion", EditorialL1Ledger.RECONCILE_WIRE,
                "attemptIdentity", "reconcile-attempt",
                "coverage", List.of(map("from", "L1", "to", "L3", "status", "PROCESSED")),
                "resolutions", List.of(), "findings", findings,
                "speakerRecords", List.of(), "protectedSpans", List.of(),
                "disposition", map("disposition", "CONTINUE", "reasonCode", "OK", "stopClass", "NONE"))));
        return new Fixture(raw, draft, rawResponse, reconcileResponse);
    }

    private record Fixture(Path raw, Path draft, Path rawResponse, Path reconcileResponse) { }

    private static Map<String, Object> caseRow(String id, String phase, Path response, Path raw, Path draft,
                                                String dependency, String expected, String hash, List<String> codes,
                                                boolean diagnostic) {
        Map<String, Object> row = map("id", id, "phase", phase, "response", response.toString(),
                "raw", raw.toString(), "expectedStatus", expected, "responseSha256", hash,
                "expectedCodes", codes, "diagnostic", diagnostic);
        if (draft != null) row.put("draft", draft.toString());
        if ("L1_RECONCILE".equals(phase)) {
            row.put("rawResponse", raw.getParent().resolve("001-L1_RAW_DISCOVERY.json").toString());
        }
        if (dependency != null) row.put("dependsOn", dependency);
        return row;
    }

    private static Path manifest(Path root, List<Map<String, Object>> cases) throws IOException {
        Path path = root.resolve("replay-manifest.json");
        Files.write(path, json(map("schemaVersion", BigDecimal.ONE, "cases", cases)));
        return path;
    }

    private record ProcessResult(int exit, String stdout, String stderr) { }

    private static ProcessResult process(String... args) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<String> command = new ArrayList<>();
        command.add(java);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(EditorialWireReplayTool.class.getName());
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).start();
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ProcessResult(process.waitFor(), stdout.trim(), stderr.trim());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> report(ProcessResult result) {
        assertFalse("replay output must be present", result.stdout().isBlank());
        return EditorialCanonicalJson.parseObject(result.stdout().getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> check(Map<String, Object> report, int index) {
        return (Map<String, Object>) ((List<Object>) report.get("checks")).get(index);
    }

    @Test public void validResponseIsPassAndExitZero() throws Exception {
        Path root = Files.createTempDirectory("p6-replay-process-");
        try {
            Fixture f = fixture(root, List.of(finding("E1", 1, "draft 1")));
            ProcessResult result = process("L1_RECONCILE", f.reconcileResponse().toString(), f.raw().toString(),
                    f.draft().toString(), f.rawResponse().toString());
            Map<String, Object> report = report(result);
            assertEquals(0, result.exit());
            assertEquals("PASS", report.get("conclusion"));
            assertEquals("PASS", check(report, 0).get("actualStatus"));
            assertEquals("PASS", check(report, 0).get("testConclusion"));
            assertTrue(result.stderr().isEmpty());
        } finally { delete(root); }
    }

    @Test public void unexpectedInvalidResponseFailsWithNonzeroExitAndProductionIsFailFast() throws Exception {
        Path root = Files.createTempDirectory("p6-replay-process-invalid-");
        try {
            Fixture f = fixture(root, List.of(finding("E1", 1, "absent 2"), finding("E2", 2, "absent 3")));
            ProcessResult result = process("L1_RECONCILE", f.reconcileResponse().toString(), f.raw().toString(),
                    f.draft().toString(), f.rawResponse().toString());
            Map<String, Object> report = report(result);
            assertEquals(2, result.exit());
            assertEquals("FAIL", report.get("conclusion"));
            Map<String, Object> item = check(report, 0);
            assertEquals("REJECTED", item.get("actualStatus"));
            assertEquals(List.of("L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote"), item.get("codes"));
            assertTrue(((List<Object>) item.get("skipped")).contains("remainingItems"));
        } finally { delete(root); }
    }

    @Test public void expectedRejectIsHashAndCodeBoundButDoesNotMakeResponseValid() throws Exception {
        Path root = Files.createTempDirectory("p6-replay-manifest-");
        try {
            Fixture f = fixture(root, List.of(finding("E1", 1, "absent 2")));
            String hash = EditorialCanonicalJson.sha256Hex(Files.readAllBytes(f.reconcileResponse()));
            Path m = manifest(root, List.of(caseRow("reconcile", "L1_RECONCILE", f.reconcileResponse(), f.raw(), f.draft(),
                    null, "EXPECTED_REJECT", hash, List.of("L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote"), true)));
            ProcessResult result = process("replay-all", m.toString());
            Map<String, Object> report = report(result);
            assertEquals(0, result.exit());
            Map<String, Object> item = check(report, 0);
            assertEquals("REJECTED", item.get("actualStatus"));
            assertEquals("PASS", item.get("testConclusion"));
            assertEquals("PASS", report.get("conclusion"));
        } finally { delete(root); }
    }

    @Test public void expectedPassOrWrongHashOrCodeFails() throws Exception {
        Path root = Files.createTempDirectory("p6-replay-expectation-");
        try {
            Fixture f = fixture(root, List.of(finding("E1", 1, "draft 1")));
            String hash = EditorialCanonicalJson.sha256Hex(Files.readAllBytes(f.reconcileResponse()));
            Path expectedReject = manifest(root, List.of(caseRow("pass-as-reject", "L1_RECONCILE", f.reconcileResponse(), f.raw(), f.draft(),
                    null, "EXPECTED_REJECT", hash, List.of("L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote"), true)));
            ProcessResult passAsReject = process("replay-all", expectedReject.toString());
            assertEquals(2, passAsReject.exit());
            assertEquals("FAIL", check(report(passAsReject), 0).get("testConclusion"));

            Path wrongHash = manifest(root, List.of(caseRow("wrong-hash", "L1_RECONCILE", f.reconcileResponse(), f.raw(), f.draft(),
                    null, "PASS", "0000000000000000000000000000000000000000000000000000000000000000", List.of(), true)));
            ProcessResult badHash = process("replay-all", wrongHash.toString());
            assertEquals(2, badHash.exit());
            assertTrue(((List<Object>) check(report(badHash), 0).get("expectationErrors"))
                    .contains("L1_REPLAY_EXPECTATION_HASH_MISMATCH:responseSha256"));

            Path wrongCode = manifest(root, List.of(caseRow("wrong-code", "L1_RECONCILE", f.reconcileResponse(), f.raw(), f.draft(),
                    null, "EXPECTED_REJECT", hash, List.of("L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote"), true)));
            ProcessResult badCode = process("replay-all", wrongCode.toString());
            assertEquals(2, badCode.exit());
            assertEquals("FAIL", check(report(badCode), 0).get("testConclusion"));

            Path badRoot = root.resolve("bad-case");
            Files.createDirectories(badRoot);
            Fixture bad = fixture(badRoot, List.of(finding("E1", 1, "absent 2")));
            String badResponseHash = EditorialCanonicalJson.sha256Hex(Files.readAllBytes(bad.reconcileResponse()));
            Path mismatchedCode = manifest(root.resolve("bad-case"), List.of(caseRow("mismatched-code", "L1_RECONCILE",
                    bad.reconcileResponse(), bad.raw(), bad.draft(), null, "EXPECTED_REJECT", badResponseHash,
                    List.of("L1_RAW_QUOTE_NOT_IN_ANCHOR:findings.0.rawQuote"), true)));
            ProcessResult badExpectedCode = process("replay-all", mismatchedCode.toString());
            assertEquals(2, badExpectedCode.exit());
            assertEquals("FAIL", check(report(badExpectedCode), 0).get("testConclusion"));
        } finally { delete(root); }
    }

    @Test public void diagnosticCollectsIndependentFindingsAndSkipsDependencies() throws Exception {
        Path root = Files.createTempDirectory("p6-replay-diagnostic-");
        try {
            Fixture f = fixture(root, List.of(finding("E1", 1, "absent 2"), finding("E2", 2, "absent 3")));
            String reconcileHash = EditorialCanonicalJson.sha256Hex(Files.readAllBytes(f.reconcileResponse()));
            Path m = manifest(root, List.of(caseRow("reconcile", "L1_RECONCILE", f.reconcileResponse(), f.raw(), f.draft(),
                    null, "EXPECTED_REJECT", reconcileHash,
                    List.of("L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote",
                            "L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.1.draftQuote"), true)));
            Map<String, Object> report = report(process("replay-all", m.toString()));
            assertEquals("PASS", report.get("conclusion"));
            assertEquals(List.of("L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote",
                    "L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.1.draftQuote"), check(report, 0).get("codes"));

            Path mixedRoot = root.resolve("mixed-case");
            Files.createDirectories(mixedRoot);
            Fixture mixed = fixture(mixedRoot, List.of(finding("E1", 1, "absent 2"), finding("E2", 2, "draft 2")));
            String mixedHash = EditorialCanonicalJson.sha256Hex(Files.readAllBytes(mixed.reconcileResponse()));
            Path mixedManifest = manifest(mixedRoot, List.of(caseRow("mixed", "L1_RECONCILE",
                    mixed.reconcileResponse(), mixed.raw(), mixed.draft(), null, "EXPECTED_REJECT", mixedHash,
                    List.of("L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote"), true)));
            Map<String, Object> mixedReport = report(process("replay-all", mixedManifest.toString()));
            assertEquals(List.of("L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote"), check(mixedReport, 0).get("codes"));
            assertTrue(((List<Object>) check(mixedReport, 0).get("completed")).contains("findings.1"));

            Path badRaw = root.resolve("bad-raw.json");
            Files.writeString(badRaw, "{bad", StandardCharsets.UTF_8);
            Path skipManifest = manifest(root, List.of(
                    caseRow("raw", "L1_RAW_DISCOVERY", badRaw, f.raw(), null, null, "UNSPECIFIED", null, List.of(), true),
                    caseRow("reconcile", "L1_RECONCILE", f.reconcileResponse(), f.raw(), f.draft(), "raw", "UNSPECIFIED", null, List.of(), true)));
            ProcessResult skippedProcess = process("replay-all", skipManifest.toString());
            Map<String, Object> skipped = report(skippedProcess);
            assertEquals(2, skippedProcess.exit());
            assertEquals("SKIPPED", check(skipped, 1).get("actualStatus"));
            assertTrue(((List<Object>) check(skipped, 1).get("skipped")).contains("dependency:raw"));
        } finally { delete(root); }
    }

    @Test public void malformedOrMissingInputIsTypedFailureAndOutputContainsNoSecrets() throws Exception {
        Path root = Files.createTempDirectory("p6-replay-errors-");
        try {
            Path raw = root.resolve("RAW.txt");
            Files.writeString(raw, "raw 1", StandardCharsets.UTF_8);
            ProcessResult missing = process("L1_RAW_DISCOVERY", root.resolve("missing.json").toString(), raw.toString());
            assertEquals(2, missing.exit());
            assertEquals("TOOL_ERROR", check(report(missing), 0).get("actualStatus"));
            Path broken = root.resolve("broken.json");
            Files.writeString(broken, "{bad", StandardCharsets.UTF_8);
            ProcessResult malformed = process("L1_RAW_DISCOVERY", broken.toString(), raw.toString());
            assertEquals(2, malformed.exit());
            assertTrue(((List<Object>) check(report(malformed), 0).get("codes"))
                    .contains("L1_WIRE_JSON_INVALID:root"));
            assertFalse(malformed.stdout().contains("apiKey"));
            assertFalse(malformed.stdout().contains("Authorization"));
            assertFalse(malformed.stdout().contains("model"));
            assertTrue(malformed.stderr().isEmpty());

            Path brokenReconcileRoot = root.resolve("broken-reconcile");
            Files.createDirectories(brokenReconcileRoot);
            Fixture brokenReconcile = fixture(brokenReconcileRoot, List.of(finding("E1", 1, "draft 1")));
            Files.writeString(brokenReconcile.reconcileResponse(), "{bad", StandardCharsets.UTF_8);
            Path diagnosticManifest = manifest(brokenReconcileRoot, List.of(caseRow("broken-reconcile",
                    "L1_RECONCILE", brokenReconcile.reconcileResponse(), brokenReconcile.raw(), brokenReconcile.draft(),
                    null, "UNSPECIFIED", null, List.of(), true)));
            ProcessResult malformedReconcile = process("replay-all", diagnosticManifest.toString());
            Map<String, Object> malformedReconcileReport = report(malformedReconcile);
            assertEquals(2, malformedReconcile.exit());
            assertTrue(((List<Object>) check(malformedReconcileReport, 0).get("skipped")).contains("structure"));
        } finally { delete(root); }
    }

    private static void delete(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        }
    }
}
