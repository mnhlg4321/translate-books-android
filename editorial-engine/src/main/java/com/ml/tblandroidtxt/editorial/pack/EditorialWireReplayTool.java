package com.ml.tblandroidtxt.editorial.pack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Offline replay of a captured P6 fixture response through the production L1 parsers. */
public final class EditorialWireReplayTool {
    private EditorialWireReplayTool() { }

    public static void main(String[] args) {
        try {
            System.out.println(args != null && args.length > 0 && "replay-all".equals(args[0])
                    ? runAll(args) : run(args));
        } catch (RuntimeException invalid) {
            System.err.println(WireViolation.safeMessage(invalid, "L1_WIRE_REPLAY_FAILED"));
            System.exit(2);
        } catch (IOException invalid) {
            System.err.println("L1_WIRE_REPLAY_IO_FAILED:root");
            System.exit(2);
        }
    }

    public static String run(String[] args) throws IOException {
        if (args == null || args.length < 3) {
            throw WireViolation.at("L1_WIRE_REPLAY_USAGE", "root");
        }
        String phase = args[0];
        Path response = Path.of(args[1]);
        Path raw = Path.of(args[2]);
        if ("L1_RAW_DISCOVERY".equals(phase) && args.length == 3) {
            EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(Files.readAllBytes(raw));
            byte[] bytes = Files.readAllBytes(response);
            String attempt = attemptIdentity(bytes);
            EditorialL1Ledger.RawPass pass = EditorialL1Ledger.parseRawPass(bytes, attempt, inventory);
            return "PASS L1_RAW_DISCOVERY candidates=" + pass.candidates().size()
                    + " coverageRanges=" + pass.coverage().size();
        }
        if ("L1_RECONCILE".equals(phase) && args.length == 5) {
            Path draft = Path.of(args[3]);
            Path rawResponse = Path.of(args[4]);
            EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(Files.readAllBytes(raw));
            byte[] rawBytes = Files.readAllBytes(rawResponse);
            EditorialL1Ledger.RawPass rawPass = EditorialL1Ledger.parseRawPass(
                    rawBytes, attemptIdentity(rawBytes), inventory);
            byte[] reconcileBytes = Files.readAllBytes(response);
            List<String> draftLines = EditorialL1Ledger.draftLines(Files.readAllBytes(draft));
            EditorialL1Ledger.ReconcilePass parsed = EditorialL1Ledger.parseReconcile(
                    reconcileBytes, attemptIdentity(reconcileBytes), inventory, draftLines, rawPass.candidates());
            return "PASS L1_RECONCILE findings=" + parsed.findings().size()
                    + " resolutions=" + parsed.resolutions().size();
        }
        throw WireViolation.at("L1_WIRE_REPLAY_ARGUMENTS_INVALID", "root");
    }

    /** Replay the known-invalid old RAW capture and the exact current RAW/RECONCILE pair independently. */
    public static String runAll(String[] args) throws IOException {
        if (args == null || args.length != 6 || !"replay-all".equals(args[0])) {
            throw WireViolation.at("L1_WIRE_REPLAY_ALL_USAGE", "root");
        }
        Path oldRawResponse = Path.of(args[1]);
        Path currentRawResponse = Path.of(args[2]);
        Path reconcileResponse = Path.of(args[3]);
        Path raw = Path.of(args[4]);
        Path draft = Path.of(args[5]);
        EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(Files.readAllBytes(raw));
        List<String> output = new java.util.ArrayList<>();

        boolean expectedOldRawRefusal = false;
        try {
            byte[] bytes = Files.readAllBytes(oldRawResponse);
            EditorialL1Ledger.RawPass pass = EditorialL1Ledger.parseRawPass(bytes, attemptIdentity(bytes), inventory);
            output.add("ERROR L1_RAW_OLD_EXPECTED_INVALID_NOT_REPRODUCED response=" + oldRawResponse);
        } catch (RuntimeException refusal) {
            String diagnostic = EditorialL1Ledger.safeMessage(refusal);
            expectedOldRawRefusal = "L1_UNIT_UNKNOWN:coverage.0.from".equals(diagnostic);
            output.add((expectedOldRawRefusal ? "EXPECTED " : "CODE ") + diagnostic + " response=" + oldRawResponse);
        }

        EditorialL1Ledger.RawPass rawPass = null;
        try {
            byte[] bytes = Files.readAllBytes(currentRawResponse);
            rawPass = EditorialL1Ledger.parseRawPass(bytes, attemptIdentity(bytes), inventory);
            output.add("PASS L1_RAW_DISCOVERY candidates=" + rawPass.candidates().size()
                    + " coverageRanges=" + rawPass.coverage().size() + " response=" + currentRawResponse);
        } catch (RuntimeException refusal) {
            output.add("CODE " + EditorialL1Ledger.safeMessage(refusal) + " response=" + currentRawResponse);
        }

        boolean reconcilePass = false;
        if (rawPass == null) {
            output.add("SKIP L1_RECONCILE raw prerequisite failed response=" + reconcileResponse);
        } else {
            try {
                byte[] bytes = Files.readAllBytes(reconcileResponse);
                EditorialL1Ledger.ReconcilePass pass = EditorialL1Ledger.parseReconcile(bytes, attemptIdentity(bytes), inventory,
                        EditorialL1Ledger.draftLines(Files.readAllBytes(draft)), rawPass.candidates());
                reconcilePass = true;
                output.add("PASS L1_RECONCILE findings=" + pass.findings().size()
                        + " resolutions=" + pass.resolutions().size()
                        + " duplicateReferencesRemoved=" + pass.duplicateReferencesRemoved()
                        + " response=" + reconcileResponse);
            } catch (RuntimeException refusal) {
                output.add("CODE " + EditorialL1Ledger.safeMessage(refusal) + " response=" + reconcileResponse);
            }
        }
        output.add(expectedOldRawRefusal && rawPass != null && reconcilePass ? "REPLAY_ALL PASS" : "REPLAY_ALL FAIL");
        return String.join(System.lineSeparator(), output);
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
}
