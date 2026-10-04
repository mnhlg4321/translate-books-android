package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Offline Z2 runner over saved live responses. Usage:
 * {@code <RAW.txt> <DRAFT.txt> <rawResponse.json> <reconcileResponse.json> [more rawResponse reconcileResponse pairs]}.
 * Prints a JSON report (no model text, only mutation names, safe CODE:path values and counts) and exits 2 when a baseline
 * does not parse or any variant behaves against its expectation. It reads local files only and never calls a provider.
 */
public final class EditorialWireMutationTool {
    private EditorialWireMutationTool() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 4 || args.length % 2 != 0) {
            System.err.println("usage: RAW.txt DRAFT.txt rawResponse reconcileResponse [rawResponse reconcileResponse ...]");
            System.exit(64);
        }
        byte[] rawBytes = Files.readAllBytes(Path.of(args[0]));
        byte[] draftBytes = Files.readAllBytes(Path.of(args[1]));
        EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(rawBytes);
        List<String> draftLines = EditorialL1Ledger.draftLines(draftBytes);
        Map<String, String> labels = EditorialWireMutationEngine.labels();
        List<Object> cases = new ArrayList<>();
        int defects = 0;
        for (int pair = 2; pair < args.length; pair += 2) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("rawResponse", args[pair]);
            entry.put("reconcileResponse", args[pair + 1]);
            byte[] rawResponse = Files.readAllBytes(Path.of(args[pair]));
            byte[] reconcileResponse = Files.readAllBytes(Path.of(args[pair + 1]));
            entry.put("rawResponseSha256", EditorialCanonicalJson.sha256Hex(rawResponse));
            entry.put("reconcileResponseSha256", EditorialCanonicalJson.sha256Hex(reconcileResponse));
            String rawAttempt = (String) EditorialCanonicalJson.parseObject(rawResponse).get("attemptIdentity");
            String reconcileAttempt = (String) EditorialCanonicalJson.parseObject(reconcileResponse).get("attemptIdentity");
            EditorialL1Ledger.RawPass rawPass;
            EditorialL1Ledger.ReconcilePass pass;
            try {
                rawPass = EditorialL1Ledger.parseRawPass(rawResponse, rawAttempt, inventory);
            } catch (RuntimeException invalid) {
                entry.put("baseline", "RAW REJECTED " + WireViolation.safeMessage(invalid, "L1_WIRE_PARSE_FAILED"));
                cases.add(entry);
                defects++;
                continue;
            }
            try {
                pass = EditorialL1Ledger.parseReconcile(reconcileResponse, reconcileAttempt, inventory, draftLines, rawPass.candidates());
            } catch (RuntimeException invalid) {
                entry.put("baseline", "RECONCILE REJECTED " + WireViolation.safeMessage(invalid, "L1_WIRE_PARSE_FAILED"));
                cases.add(entry);
                defects++;
                continue;
            }
            entry.put("baseline", "PASS");
            entry.put("baselineNotes", new ArrayList<Object>(pass.bookkeepingNotes()));
            entry.put("speakerRecordsDropped", BigDecimal.valueOf(pass.speakerRecordsDropped().size()));

            int blank = 0;
            Set<Integer> unitLines = new TreeSet<>();
            for (EditorialRawInventory.Unit unit : inventory.units()) unitLines.add(unit.line());
            for (int line = 1; line <= inventory.physicalLines(); line++) if (!unitLines.contains(line)) { blank = line; break; }
            String goodUnit = "L" + inventory.units().get(0).line();
            Set<Integer> occupied = new TreeSet<>();
            for (EditorialL1Ledger.Finding finding : pass.findings()) {
                if ("OPEN".equals(finding.disposition()) && "LINES".equals(finding.draft().kind())) {
                    for (int line = finding.draft().start(); line <= finding.draft().end(); line++) occupied.add(line);
                }
            }
            final String rawAttemptId = rawAttempt;
            final String reconcileAttemptId = reconcileAttempt;
            List<EditorialWireMutationEngine.Outcome> outcomes = new ArrayList<>();
            outcomes.addAll(EditorialWireMutationEngine.runAll(rawResponse,
                    EditorialWireMutationEngine.l1Raw(EditorialWireMutationEngine.copy(rawResponse)),
                    wire -> {
                        EditorialL1Ledger.parseRawPass(wire, rawAttemptId, inventory);
                        return WireNotes.drain();
                    }, labels));
            outcomes.addAll(EditorialWireMutationEngine.runAll(reconcileResponse,
                    EditorialWireMutationEngine.l1Reconcile(EditorialWireMutationEngine.copy(reconcileResponse), draftLines.size(),
                            blank, inventory.physicalLines(), goodUnit, occupied, quote -> {
                                int hits = 0;
                                for (EditorialRawInventory.Unit unit : inventory.units()) {
                                    if (EditorialQuoteMatcher.containsRaw(unit.text(), quote)) hits++;
                                }
                                return hits;
                            }),
                    wire -> EditorialL1Ledger.parseReconcile(wire, reconcileAttemptId, inventory, draftLines,
                            rawPass.candidates()).bookkeepingNotes(), labels));
            List<Object> rows = new ArrayList<>();
            int normalizedOk = 0;
            int refusedOk = 0;
            for (EditorialWireMutationEngine.Outcome outcome : outcomes) {
                if (outcome.ok()) {
                    if (outcome.expect() == EditorialWireMutationEngine.Expect.NORMALIZED) normalizedOk++;
                    else refusedOk++;
                    continue;
                }
                defects++;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("mutation", outcome.name());
                row.put("expect", outcome.expect().name());
                row.put("why", outcome.why());
                rows.add(row);
            }
            entry.put("variants", BigDecimal.valueOf(outcomes.size()));
            entry.put("normalizedParsed", BigDecimal.valueOf(normalizedOk));
            entry.put("refusedAsExpected", BigDecimal.valueOf(refusedOk));
            entry.put("defects", rows);
            cases.add(entry);
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schemaVersion", BigDecimal.ONE);
        report.put("cases", cases);
        report.put("defectCount", BigDecimal.valueOf(defects));
        report.put("conclusion", defects == 0 ? "PASS" : "FAIL");
        System.out.println(EditorialCanonicalJson.canonicalize(report));
        if (defects != 0) System.exit(2);
    }
}
