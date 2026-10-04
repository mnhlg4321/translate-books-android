package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Ledger-contract ({@code L1_LEDGER_V4}) side of an L1 phase: prepares the app-owned inventory and the
 * RAW-pass block, interprets the model's wire strictly, and builds the typed output, report and metrics. The
 * model supplies judgement rows only; the app owns identity, inventory, coverage checks, gates and the report.
 */
final class EditorialL1LedgerRun {
    static final String EVIDENCE = "ledger:l1";
    static final String RAW_CANDIDATES_ROLE = "L1_RAW_CANDIDATES";
    /** One call carries the whole chapter up to this many units; above it the run stops typed (no truncation). */
    static final int MAX_UNITS_SINGLE_CALL = 600;

    private EditorialL1LedgerRun() { }

    /** Everything derived from the pinned bytes before any provider call. */
    static final class Context {
        final boolean reconcile;
        final EditorialRawInventory.Inventory inventory;
        final List<String> draftLines;
        final EditorialL1Ledger.Body rawBody;
        final byte[] rawCandidateBlock;

        private Context(boolean reconcile, EditorialRawInventory.Inventory inventory, List<String> draftLines,
                        EditorialL1Ledger.Body rawBody, byte[] rawCandidateBlock) {
            this.reconcile = reconcile;
            this.inventory = inventory;
            this.draftLines = draftLines;
            this.rawBody = rawBody;
            this.rawCandidateBlock = rawCandidateBlock;
        }
    }

    /** Result of interpreting one provider response. */
    static final class Outcome {
        final EditorialP5L1Output output;
        final EditorialL1Ledger.Body body;

        private Outcome(EditorialP5L1Output output, EditorialL1Ledger.Body body) {
            this.output = output;
            this.body = body;
        }
    }

    /** Typed input errors are IllegalArgumentException with an allow-listed code. */
    static Context prepare(EditorialP5PilotRequest request) {
        EditorialP5PilotRequest.SourceBytes raw = request.source(EditorialSafe4Contract.RAW);
        if (raw == null || raw.bytes() == null) throw new IllegalArgumentException("INPUT_RAW_MISSING");
        EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(raw.bytes());
        if (inventory.units().isEmpty()) throw new IllegalArgumentException("INPUT_RAW_HAS_NO_UNITS");
        if (inventory.units().size() > MAX_UNITS_SINGLE_CALL) throw new IllegalArgumentException("L1_UNIT_LIMIT_EXCEEDED");
        boolean reconcile = "L1_RECONCILE".equals(request.phase());
        if (!reconcile) return new Context(false, inventory, List.of(), null, new byte[0]);

        EditorialP5PilotRequest.SourceBytes draft = request.source(EditorialSafe4Contract.DRAFT);
        if (draft == null || draft.bytes() == null) throw new IllegalArgumentException("INPUT_DRAFT_MISSING");
        List<String> draftLines = EditorialL1Ledger.draftLines(draft.bytes());
        byte[] predecessor = request.predecessorReport();
        if (predecessor == null) throw new IllegalArgumentException("INPUT_RAW_LEDGER_REPORT_MISSING");
        if (!EditorialContractRevision.eligiblePredecessor(EditorialContractRevision.ofReportBytes(predecessor), request.contractRevision())) {
            throw new IllegalArgumentException("INPUT_REPORT_L1_LEGACY_CONTRACT");
        }
        EditorialL1Ledger.Body body;
        try {
            body = EditorialL1Ledger.parseBody(EditorialCanonicalJson.parseObject(predecessor));
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("INPUT_RAW_LEDGER_REPORT_INVALID");
        }
        if (!"L1_RAW_DISCOVERY".equals(body.phase())) throw new IllegalArgumentException("INPUT_RAW_LEDGER_PHASE_INVALID");
        if (!inventory.inventorySha256().equals(body.inventorySha256()) || !inventory.rawSha256().equals(body.rawSha256())) {
            throw new IllegalArgumentException("INPUT_RAW_LEDGER_STALE");
        }
        return new Context(true, inventory, draftLines, body, candidateBlock(body.candidates()));
    }

    /** App-owned text block the RECONCILE model reads; one candidate per line, tab separated. */
    static byte[] candidateBlock(List<EditorialL1Ledger.Candidate> candidates) {
        StringBuilder out = new StringBuilder("candidateId\tledger\tunitId\tnote\n");
        for (EditorialL1Ledger.Candidate c : candidates) {
            out.append(c.candidateId()).append('\t').append(c.ledger()).append('\t').append(EditorialUnitReference.fromId(c.unitId())).append('\t')
                    .append(c.note().replace('\t', ' ')).append('\n');
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    static Outcome interpret(EditorialP5PilotRequest request, EditorialPhaseContextProjector.PhaseProjection projection,
                             Context context, byte[] responseBytes) {
        String attempt = request.attemptIdentity();
        EditorialL1Ledger.Body body;
        Map<String, String> gates = gates(true);
        EditorialStopDecision.Decision decision;
        if (!context.reconcile) {
            EditorialL1Ledger.RawPass pass = EditorialL1Ledger.parseRawPass(responseBytes, attempt, context.inventory);
            body = EditorialL1Ledger.bodyOfRawPass(context.inventory, pass);
            decision = EditorialStopDecision.continueWithoutStop(EditorialP5RawWireContract.FINAL_PHASE, "NONE", "L1_RAW_LEDGER");
        } else {
            EditorialL1Ledger.RawPass raw = new EditorialL1Ledger.RawPass(context.rawBody.rawCoverage(), context.rawBody.candidates());
            EditorialL1Ledger.ReconcilePass pass = EditorialL1Ledger.parseReconcile(responseBytes, attempt,
                    context.inventory, context.draftLines, raw.candidates());
            body = EditorialL1Ledger.bodyOfReconcile(context.inventory, raw, pass);
            decision = decisionOf(pass.disposition());
            if (decision.isStop()) gates = gates(false);
        }
        String baseText = new String(request.source(context.reconcile ? EditorialSafe4Contract.DRAFT
                : EditorialSafe4Contract.RAW).bytes(), StandardCharsets.UTF_8);
        List<EditorialLedgerValidator.Entry> entries = new ArrayList<>();
        boolean preserve = decision.isPreserveDraft();
        for (String id : request.populationIds()) {
            entries.add(new EditorialLedgerValidator.Entry(id, preserve ? "PRESERVE_DRAFT" : "PROCESSED",
                    List.of(EVIDENCE), false));
        }
        List<String> preserved = preserve ? request.populationIds() : List.of();
        EditorialP5L1Output output = new EditorialP5L1Output(EditorialContractRevision.REPORT_SCHEMA_V2,
                EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION, request.binding().bindingIdentity(),
                request.manifestFingerprint(), request.chapterKey(), EditorialP5RawWireContract.FINAL_PHASE,
                projection.bundleIdentity(), request.predecessorIdentity(), request.stableAnchors(),
                new EditorialLedgerValidator.Request(request.populationIds(), entries), gates, preserved, List.of(),
                baseText, baseText, 0, decision, Set.of(EVIDENCE), false);
        return new Outcome(output, body);
    }

    private static EditorialStopDecision.Decision decisionOf(EditorialL1Ledger.Disposition d) {
        String phase = EditorialP5RawWireContract.FINAL_PHASE;
        List<String> evidence = List.of(EVIDENCE);
        switch (d.kind()) {
            case "CONTINUE":
                return EditorialStopDecision.continueWithoutStop(phase, "NONE", d.reasonCode());
            case "PRESERVE_DRAFT":
                return EditorialStopDecision.preserveDraft(d.reasonCode(), phase, "NONE", evidence, "CHAPTER",
                        "Keep the draft", "L1_RECONCILE");
            default:
                return "CONTENT_BLOCKED".equals(d.stopClass())
                        ? EditorialStopDecision.contentBlocked(d.reasonCode(), phase, "NONE", evidence, "CHAPTER",
                        "Review the blocked content", "L1_RECONCILE")
                        : EditorialStopDecision.inputRequired(d.reasonCode(), phase, "NONE", "CHAPTER",
                        "Provide the required input", "L1_RECONCILE");
        }
    }

    /** App-derived gates: structural checks the app verified pass; nothing semantic is certified here. */
    private static Map<String, String> gates(boolean coverageVerified) {
        Map<String, String> gates = new TreeMap<>();
        for (String gate : EditorialSafe4Contract.GATE_IDS) gates.put(gate, "NOT_APPLICABLE");
        gates.put("ARTIFACT_IDENTITY", "PASS");
        if (coverageVerified) gates.put("COVERAGE", "PASS");
        return gates;
    }

    /** REPORT_L1 v2: the v1 identity keys stay, the ledger body is added under the v2 schema. */
    static byte[] reportBytes(EditorialP5PilotRequest request, EditorialPhaseContextProjector.PhaseProjection projection,
                              EditorialP5L1Output output, int populationTotal, int accountedTotal, Outcome outcome) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schemaVersion", EditorialContractRevision.REPORT_SCHEMA_V2);
        report.put("artifactType", "REPORT_L1");
        report.put("bindingIdentity", request.binding().bindingIdentity());
        report.put("manifestFingerprint", request.manifestFingerprint());
        report.put("canonicalPackHash", request.binding().canonicalPackHash());
        report.put("canonicalProfileHash", request.binding().canonicalProfileHash());
        report.put("compatibilityEvaluationId", request.binding().compatibilityEvaluationId());
        report.put("chapterKey", request.chapterKey());
        report.put("phase", request.phase());
        report.put("bundleIdentity", projection.bundleIdentity());
        report.put("predecessorIdentity", output.predecessorIdentity());
        report.put("stableAnchors", output.stableAnchors());
        report.put("populationTotal", BigDecimal.valueOf(populationTotal));
        report.put("accountedTotal", BigDecimal.valueOf(accountedTotal));
        report.put("actualChangedSpans", BigDecimal.ZERO);
        report.put("gates", new TreeMap<>(output.gates()));
        report.put("evidenceRefs", output.evidenceRefs().stream().sorted().toList());
        report.put("preservedInventory", output.preservedInventory());
        report.put("disposition", output.disposition().disposition().name());
        report.put("modelDeclaredPassRecordedOnly", false);
        Map<String, Object> body = EditorialL1Ledger.bodyToMap(outcome.body);
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            if ("phase".equals(entry.getKey())) continue;
            report.put(entry.getKey(), entry.getValue());
        }
        return EditorialCanonicalJson.canonicalize(report).getBytes(StandardCharsets.UTF_8);
    }
}
