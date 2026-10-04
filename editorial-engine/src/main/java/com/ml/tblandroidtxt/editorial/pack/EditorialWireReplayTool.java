package com.ml.tblandroidtxt.editorial.pack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Offline replay of captured P6 fixture responses through the production L1 parsers. */
public final class EditorialWireReplayTool {
    private static final int REPORT_SCHEMA_VERSION = 2;
    private static final int MAX_MANIFEST_CASES = 64;
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern CODE_PATH = Pattern.compile(
            "[A-Z][A-Z0-9_]{1,95}:(?:[A-Za-z][A-Za-z0-9]*|[0-9]+)(?:\\.(?:[A-Za-z][A-Za-z0-9]*|[0-9]+))*");

    private enum ExpectedStatus { PASS, EXPECTED_REJECT, UNSPECIFIED }

    private record CaseSpec(String id, String phase, Path response, Path raw, Path draft, Path rawResponse,
                            String dependency, ExpectedStatus expected, String expectedHash,
                            List<String> expectedCodes, boolean diagnostic) {
        private CaseSpec {
            expectedCodes = List.copyOf(expectedCodes);
        }
    }

    private record Outcome(Map<String, Object> check, EditorialL1Ledger.RawPass rawPass) { }

    private static final class Report {
        private final String mode;
        private final String expectationSource;
        private final List<Map<String, Object>> checks = new ArrayList<>();
        private final List<String> toolErrors = new ArrayList<>();

        private Report(String mode, String expectationSource) {
            this.mode = mode;
            this.expectationSource = expectationSource;
        }

        private void add(Map<String, Object> check) { checks.add(check); }

        private void toolError(String codePath) {
            if (!toolErrors.contains(codePath)) toolErrors.add(codePath);
        }

        private boolean passed() {
            if (!toolErrors.isEmpty() || checks.isEmpty()) return false;
            for (Map<String, Object> check : checks) {
                if (!"PASS".equals(check.get("testConclusion"))) return false;
            }
            return true;
        }

        private String json() {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("schemaVersion", java.math.BigDecimal.valueOf(REPORT_SCHEMA_VERSION));
            root.put("mode", mode);
            root.put("expectationSource", expectationSource);
            root.put("checks", checks);
            root.put("toolErrors", toolErrors);
            root.put("conclusion", passed() ? "PASS" : "FAIL");
            return EditorialCanonicalJson.canonicalize(root);
        }

        private int exitCode() { return passed() ? 0 : 2; }
    }

    private EditorialWireReplayTool() { }

    public static void main(String[] args) {
        try {
            Report report = dispatch(args);
            System.out.println(report.json());
            if (report.exitCode() != 0) System.exit(report.exitCode());
        } catch (RuntimeException invalid) {
            Report report = toolFailure(WireViolation.safeMessage(invalid, "L1_WIRE_REPLAY_FAILED"));
            System.out.println(report.json());
            System.exit(report.exitCode());
        } catch (IOException invalid) {
            Report report = toolFailure("L1_WIRE_REPLAY_IO_FAILED:root");
            System.out.println(report.json());
            System.exit(report.exitCode());
        }
    }

    /** Run one response in production fail-fast mode, without an expectation. */
    public static String run(String[] args) throws IOException {
        return dispatch(args).json();
    }

    /**
     * Compatibility entry point. The six-argument form is intentionally diagnostic-only and has no hidden fx-a03
     * expectation; use {@code replay-all <manifest>} for hash-bound expectations.
     */
    public static String runAll(String[] args) throws IOException {
        if (args == null || args.length == 0 || !"replay-all".equals(args[0])) {
            throw WireViolation.at("L1_WIRE_REPLAY_ALL_USAGE", "root");
        }
        return dispatch(args).json();
    }

    private static Report dispatch(String[] args) throws IOException {
        if (args != null && args.length > 0 && "replay-all".equals(args[0])) {
            if (args.length == 2) return replayManifest(Path.of(args[1]));
            if (args.length == 6) return replayLegacy(args);
            throw WireViolation.at("L1_WIRE_REPLAY_ALL_USAGE", "root");
        }
        return replaySingle(args);
    }

    private static Report replaySingle(String[] args) throws IOException {
        if (args == null || args.length < 3) throw WireViolation.at("L1_WIRE_REPLAY_USAGE", "root");
        String phase = args[0];
        CaseSpec spec;
        if ("L1_RAW_DISCOVERY".equals(phase) && args.length == 3) {
            spec = new CaseSpec("single", phase, Path.of(args[1]), Path.of(args[2]), null, null,
                    null, ExpectedStatus.UNSPECIFIED, null, List.of(), false);
        } else if ("L1_RECONCILE".equals(phase) && args.length == 5) {
            spec = new CaseSpec("single", phase, Path.of(args[1]), Path.of(args[2]), Path.of(args[3]),
                    Path.of(args[4]), null, ExpectedStatus.UNSPECIFIED, null, List.of(), false);
        } else {
            throw WireViolation.at("L1_WIRE_REPLAY_ARGUMENTS_INVALID", "root");
        }
        return evaluate(List.of(spec), "single", "none");
    }

    private static Report replayLegacy(String[] args) {
        Path oldRawResponse = Path.of(args[1]);
        Path currentRawResponse = Path.of(args[2]);
        Path reconcileResponse = Path.of(args[3]);
        Path raw = Path.of(args[4]);
        Path draft = Path.of(args[5]);
        List<CaseSpec> cases = List.of(
                new CaseSpec("old-raw", "L1_RAW_DISCOVERY", oldRawResponse, raw, null, null,
                        null, ExpectedStatus.UNSPECIFIED, null, List.of(), true),
                new CaseSpec("current-raw", "L1_RAW_DISCOVERY", currentRawResponse, raw, null, null,
                        null, ExpectedStatus.UNSPECIFIED, null, List.of(), true),
                new CaseSpec("reconcile", "L1_RECONCILE", reconcileResponse, raw, draft, null,
                        "current-raw", ExpectedStatus.UNSPECIFIED, null, List.of(), true));
        return evaluate(cases, "replay-all", "legacy-arguments");
    }

    private static Report replayManifest(Path manifest) throws IOException {
        byte[] manifestBytes;
        try {
            manifestBytes = Files.readAllBytes(manifest);
        } catch (IOException missing) {
            throw WireViolation.at("L1_REPLAY_INPUT_MISSING", "manifest");
        }
        final Map<String, Object> root;
        try {
            root = EditorialCanonicalJson.parseObject(manifestBytes);
            requireKeys(root, Set.of("schemaVersion", "cases"), "root");
            if (EditorialCanonicalJson.integer(root.get("schemaVersion"), "schemaVersion") != 1) {
                throw WireViolation.at("L1_REPLAY_MANIFEST_VERSION_INVALID", "schemaVersion");
            }
        } catch (RuntimeException invalid) {
            throw WireViolation.from(invalid, "L1_REPLAY_MANIFEST_INVALID", "root");
        }
        List<Object> rows = EditorialCanonicalJson.array(root.get("cases"), "cases");
        if (rows.isEmpty() || rows.size() > MAX_MANIFEST_CASES) {
            throw WireViolation.at("L1_REPLAY_CASE_LIMIT_INVALID", "cases");
        }
        Path base = manifest.toAbsolutePath().normalize().getParent();
        List<CaseSpec> cases = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < rows.size(); index++) {
            String path = "cases." + index;
            try {
                Map<String, Object> row = EditorialCanonicalJson.object(rows.get(index), path);
                requireKeys(row, Set.of("id", "phase", "response", "raw", "draft", "rawResponse",
                        "dependsOn", "expectedStatus", "responseSha256", "expectedCodes", "diagnostic"), path);
                String id = requiredString(row, "id", path + ".id");
                if (!ids.add(id)) throw WireViolation.at("L1_REPLAY_CASE_ID_DUPLICATE", path + ".id");
                String phase = requiredString(row, "phase", path + ".phase");
                if (!"L1_RAW_DISCOVERY".equals(phase) && !"L1_RECONCILE".equals(phase)) {
                    throw WireViolation.at("L1_REPLAY_PHASE_INVALID", path + ".phase");
                }
                Path response = resolvePath(base, requiredString(row, "response", path + ".response"), path + ".response");
                Path raw = resolvePath(base, requiredString(row, "raw", path + ".raw"), path + ".raw");
                Path draft = optionalPath(base, row.get("draft"), path + ".draft");
                Path rawResponse = optionalPath(base, row.get("rawResponse"), path + ".rawResponse");
                String dependency = optionalString(row.get("dependsOn"), path + ".dependsOn");
                String expectedText = requiredString(row, "expectedStatus", path + ".expectedStatus");
                ExpectedStatus expected;
                try { expected = ExpectedStatus.valueOf(expectedText); }
                catch (IllegalArgumentException bad) { throw WireViolation.at("L1_REPLAY_EXPECTATION_INVALID", path + ".expectedStatus"); }
                String expectedHash = optionalString(row.get("responseSha256"), path + ".responseSha256");
                if (expectedHash != null && !HASH.matcher(expectedHash).matches()) {
                    throw WireViolation.at("L1_REPLAY_EXPECTATION_HASH_INVALID", path + ".responseSha256");
                }
                List<String> expectedCodes = stringList(row.get("expectedCodes"), path + ".expectedCodes");
                for (int codeIndex = 0; codeIndex < expectedCodes.size(); codeIndex++) {
                    if (!CODE_PATH.matcher(expectedCodes.get(codeIndex)).matches()) {
                        throw WireViolation.at("L1_REPLAY_EXPECTATION_CODE_INVALID", path + ".expectedCodes." + codeIndex);
                    }
                }
                if (expected != ExpectedStatus.UNSPECIFIED && expectedHash == null) {
                    throw WireViolation.at("L1_REPLAY_EXPECTATION_HASH_REQUIRED", path + ".responseSha256");
                }
                if (expected == ExpectedStatus.EXPECTED_REJECT && expectedCodes.isEmpty()) {
                    throw WireViolation.at("L1_REPLAY_EXPECTATION_CODES_REQUIRED", path + ".expectedCodes");
                }
                if (expected == ExpectedStatus.PASS && !expectedCodes.isEmpty()) {
                    throw WireViolation.at("L1_REPLAY_EXPECTATION_CODES_UNEXPECTED", path + ".expectedCodes");
                }
                boolean diagnostic = optionalBoolean(row.get("diagnostic"), path + ".diagnostic", true);
                if ("L1_RECONCILE".equals(phase) && draft == null) {
                    throw WireViolation.at("L1_REPLAY_INPUT_MISSING", path + ".draft");
                }
                cases.add(new CaseSpec(id, phase, response, raw, draft, rawResponse, dependency, expected,
                        expectedHash, expectedCodes, diagnostic));
            } catch (RuntimeException invalid) {
                throw WireViolation.from(invalid, "L1_REPLAY_MANIFEST_INVALID", path);
            }
        }
        Set<String> knownIds = new HashSet<>(ids);
        for (int index = 0; index < cases.size(); index++) {
            CaseSpec spec = cases.get(index);
            if (spec.dependency() != null && (!knownIds.contains(spec.dependency()) || indexOf(cases, spec.dependency()) >= index)) {
                throw WireViolation.at("L1_REPLAY_DEPENDENCY_INVALID", "cases." + index + ".dependsOn");
            }
        }
        return evaluate(cases, "replay-all", "manifest");
    }

    private static int indexOf(List<CaseSpec> cases, String id) {
        for (int i = 0; i < cases.size(); i++) if (cases.get(i).id().equals(id)) return i;
        return Integer.MAX_VALUE;
    }

    private static Report evaluate(List<CaseSpec> cases, String mode, String expectationSource) {
        Report report = new Report(mode, expectationSource);
        Map<String, Outcome> outcomes = new LinkedHashMap<>();
        for (CaseSpec spec : cases) {
            Outcome outcome = evaluateCase(spec, outcomes, report);
            outcomes.put(spec.id(), outcome);
            report.add(outcome.check());
        }
        return report;
    }

    private static Outcome evaluateCase(CaseSpec spec, Map<String, Outcome> outcomes, Report report) {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("id", spec.id());
        check.put("phase", spec.phase());
        check.put("response", spec.response().toString());
        check.put("expectedStatus", spec.expected().name());
        check.put("expectedResponseSha256", spec.expectedHash() == null ? "" : spec.expectedHash());
        check.put("expectedCodes", spec.expectedCodes());
        check.put("diagnosticScope", spec.diagnostic() ? "findings-independent-bounded" : "production-fail-fast");
        List<String> codes = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> completed = new ArrayList<>();
        List<String> expectationErrors = new ArrayList<>();
        byte[] responseBytes;
        try {
            responseBytes = Files.readAllBytes(spec.response());
        } catch (IOException missing) {
            String code = "L1_REPLAY_INPUT_MISSING:response";
            codes.add(code);
            check.put("actualStatus", "TOOL_ERROR");
            check.put("responseSha256", "");
            skipped.add("response");
            check.put("codes", codes);
            check.put("skipped", skipped);
            check.put("completed", completed);
            check.put("expectationErrors", expectationErrors);
            check.put("testConclusion", "FAIL");
            report.toolError(code);
            return new Outcome(check, null);
        }

        String responseHash = EditorialCanonicalJson.sha256Hex(responseBytes);
        check.put("responseSha256", responseHash);
        if (spec.expectedHash() != null && !spec.expectedHash().equals(responseHash)) {
            expectationErrors.add("L1_REPLAY_EXPECTATION_HASH_MISMATCH:responseSha256");
        }

        String actual = "TOOL_ERROR";
        EditorialL1Ledger.RawPass rawPass = null;
        try {
            EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(Files.readAllBytes(spec.raw()));
            if ("L1_RAW_DISCOVERY".equals(spec.phase())) {
                rawPass = EditorialL1Ledger.parseRawPass(responseBytes, attemptIdentity(responseBytes), inventory);
                actual = "PASS";
                completed.add("coverage");
                completed.add("candidates");
            } else {
                rawPass = prerequisiteRaw(spec, outcomes, inventory, skipped, codes);
                if (rawPass == null) {
                    actual = "SKIPPED";
                } else {
                    List<String> draftLines = EditorialL1Ledger.draftLines(Files.readAllBytes(spec.draft()));
                    if (spec.diagnostic()) {
                        EditorialL1Ledger.ReconcileDiagnostic diagnostic = EditorialL1Ledger.diagnoseReconcile(
                                responseBytes, attemptIdentity(responseBytes), inventory, draftLines, rawPass.candidates());
                        codes.addAll(diagnostic.errors());
                        skipped.addAll(diagnostic.skipped());
                        completed.addAll(diagnostic.completed());
                        actual = diagnostic.errors().isEmpty() ? "PASS" : "REJECTED";
                    } else {
                        EditorialL1Ledger.ReconcilePass parsed = EditorialL1Ledger.parseReconcile(
                                responseBytes, attemptIdentity(responseBytes), inventory, draftLines, rawPass.candidates());
                        actual = "PASS";
                        completed.add("reconcile");
                        completed.add("resolutions");
                        completed.add("findings");
                        completed.add("speakerRecords");
                        completed.add("protectedSpans");
                    }
                }
            }
        } catch (IOException missing) {
            actual = "TOOL_ERROR";
            codes.add("L1_REPLAY_INPUT_MISSING:input");
            skipped.add("input");
            report.toolError("L1_REPLAY_INPUT_MISSING:input");
        } catch (RuntimeException invalid) {
            actual = "REJECTED";
            String code = WireViolation.safeMessage(invalid, "L1_WIRE_REPLAY_FAILED");
            if (!codes.contains(code)) codes.add(code);
            if (!spec.diagnostic()) {
                skipped.add("remainingItems");
            } else if (completed.isEmpty()) {
                if ("L1_RECONCILE".equals(spec.phase())) {
                    skipped.add("structure");
                    skipped.add("findings");
                } else {
                    skipped.add("coverage");
                    skipped.add("candidates");
                }
                skipped.add("remainingItems");
            }
        }
        deduplicate(codes);
        deduplicate(skipped);
        deduplicate(completed);
        check.put("actualStatus", actual);
        check.put("codes", codes);
        check.put("skipped", skipped);
        check.put("completed", completed);
        check.put("expectationErrors", expectationErrors);
        check.put("testConclusion", conclusion(spec, actual, responseHash, codes, expectationErrors));
        if ("TOOL_ERROR".equals(actual)) report.toolError(codes.isEmpty() ? "L1_REPLAY_TOOL_ERROR:root" : codes.get(0));
        return new Outcome(check, rawPass);
    }

    private static EditorialL1Ledger.RawPass prerequisiteRaw(CaseSpec spec, Map<String, Outcome> outcomes,
                                                               EditorialRawInventory.Inventory inventory,
                                                               List<String> skipped, List<String> codes) throws IOException {
        if (spec.dependency() != null) {
            Outcome dependency = outcomes.get(spec.dependency());
            if (dependency == null || !"PASS".equals(dependency.check().get("actualStatus"))
                    || dependency.rawPass() == null) {
                skipped.add("dependency:" + spec.dependency());
                return null;
            }
            return dependency.rawPass();
        }
        if (spec.rawResponse() == null) {
            codes.add("L1_REPLAY_INPUT_MISSING:rawResponse");
            skipped.add("rawPrerequisite");
            return null;
        }
        try {
            byte[] rawBytes = Files.readAllBytes(spec.rawResponse());
            return EditorialL1Ledger.parseRawPass(rawBytes, attemptIdentity(rawBytes), inventory);
        } catch (RuntimeException invalid) {
            String code = WireViolation.safeMessage(invalid, "L1_WIRE_REPLAY_FAILED");
            skipped.add("rawPrerequisite:" + code);
            return null;
        }
    }

    private static String conclusion(CaseSpec spec, String actual, String responseHash,
                                     List<String> codes, List<String> expectationErrors) {
        if (!expectationErrors.isEmpty()) return "FAIL";
        return switch (spec.expected()) {
            case PASS -> "PASS".equals(actual) && codes.isEmpty() ? "PASS" : "FAIL";
            case EXPECTED_REJECT -> "REJECTED".equals(actual)
                    && responseHash.equals(spec.expectedHash()) && codes.equals(spec.expectedCodes()) ? "PASS" : "FAIL";
            case UNSPECIFIED -> "PASS".equals(actual) && codes.isEmpty() ? "PASS" : "FAIL";
        };
    }

    private static Report toolFailure(String codePath) {
        Report report = new Report("tool", "none");
        report.toolError(codePath);
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("id", "tool");
        check.put("phase", "TOOL");
        check.put("response", "");
        check.put("responseSha256", "");
        check.put("expectedResponseSha256", "");
        check.put("actualStatus", "TOOL_ERROR");
        check.put("expectedStatus", "UNSPECIFIED");
        check.put("expectedCodes", List.of());
        check.put("diagnosticScope", "tool-error");
        check.put("codes", List.of(codePath));
        check.put("skipped", List.of("input"));
        check.put("completed", List.of());
        check.put("expectationErrors", List.of());
        check.put("testConclusion", "FAIL");
        report.add(check);
        return report;
    }

    private static String attemptIdentity(byte[] response) {
        Map<String, Object> root;
        try { root = EditorialCanonicalJson.parseObject(response); }
        catch (RuntimeException invalid) { throw WireViolation.from(invalid, "L1_WIRE_JSON_INVALID", "root"); }
        Object value = root.get("attemptIdentity");
        if (!(value instanceof String identity) || identity.isBlank()) {
            throw WireViolation.at("L1_WIRE_ATTEMPT_IDENTITY_MISSING", "attemptIdentity");
        }
        return identity;
    }

    private static void requireKeys(Map<String, Object> row, Set<String> allowed, String path) {
        for (String key : row.keySet()) if (!allowed.contains(key)) throw WireViolation.at("L1_REPLAY_UNKNOWN_FIELD", path);
        for (String key : allowed) {
            if (("schemaVersion".equals(key) || "cases".equals(key)) && !row.containsKey(key)) {
                throw WireViolation.at("L1_REPLAY_FIELD_REQUIRED", path + "." + key);
            }
        }
    }

    private static String requiredString(Map<String, Object> row, String key, String path) {
        Object value = row.get(key);
        if (!(value instanceof String text) || text.isBlank()) throw WireViolation.at("L1_REPLAY_FIELD_REQUIRED", path);
        return text;
    }

    private static String optionalString(Object value, String path) {
        if (value == null) return null;
        if (!(value instanceof String text) || text.isBlank()) throw WireViolation.at("L1_REPLAY_FIELD_INVALID", path);
        return text;
    }

    private static boolean optionalBoolean(Object value, String path, boolean fallback) {
        if (value == null) return fallback;
        if (!(value instanceof Boolean flag)) throw WireViolation.at("L1_REPLAY_FIELD_INVALID", path);
        return flag;
    }

    private static List<String> stringList(Object value, String path) {
        if (value == null) return List.of();
        List<Object> values;
        try { values = EditorialCanonicalJson.array(value, path); }
        catch (RuntimeException invalid) { throw WireViolation.from(invalid, "L1_REPLAY_FIELD_INVALID", path); }
        if (values.size() > 256) throw WireViolation.at("L1_REPLAY_LIST_LIMIT_EXCEEDED", path);
        List<String> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            if (!(values.get(index) instanceof String text) || text.isBlank()) {
                throw WireViolation.at("L1_REPLAY_FIELD_INVALID", path + "." + index);
            }
            result.add(text);
        }
        return List.copyOf(result);
    }

    private static Path resolvePath(Path base, String value, String path) {
        try { return Path.of(value).isAbsolute() ? Path.of(value) : base.resolve(value).normalize(); }
        catch (RuntimeException invalid) { throw WireViolation.at("L1_REPLAY_PATH_INVALID", path); }
    }

    private static Path optionalPath(Path base, Object value, String path) {
        if (value == null) return null;
        if (!(value instanceof String text) || text.isBlank()) throw WireViolation.at("L1_REPLAY_FIELD_INVALID", path);
        return resolvePath(base, text, path);
    }

    private static void deduplicate(List<String> values) {
        Set<String> seen = new HashSet<>();
        values.removeIf(value -> !seen.add(value));
    }
}
