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
            System.out.println(run(args));
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
