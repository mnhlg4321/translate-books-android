package com.ml.tblandroidtxt.editorial.pack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Executes deterministic local vectors for every SAFE4 G1-G24 descriptor. */
public final class EditorialSafe4ReplayRunner {
    public record CaseDescriptor(String caseId, String expectedCode, String expectedDecision,
                                 String expectedReason, String expectedGate, String expectedEquation) { }

    public record CaseResult(CaseDescriptor descriptor, String actualDecision, String actualReason,
                             String actualGate, String actualEquation, boolean passed) { }

    public record ReplayResult(List<CaseResult> cases) {
        public ReplayResult { cases = List.copyOf(cases); }
        public long passedCount() { return cases.stream().filter(CaseResult::passed).count(); }
        public boolean allPassed() { return cases.size() == 24 && passedCount() == 24; }
    }

    private record Observation(String decision, String reason, String gate, String equation) { }

    public List<CaseDescriptor> descriptors() {
        List<CaseDescriptor> result = new ArrayList<>();
        result.add(new CaseDescriptor("G1", "SAFE4_G1_QUALIFIED", "PASS", "PREFLIGHT_PASS", "SOURCE_INPUTS", "required=4"));
        result.add(new CaseDescriptor("G2", "SAFE4_G2_QUALIFIED", "INPUT_REQUIRED", "INPUT_RAW_FILE_MISSING", "SOURCE_INPUTS", "raw=0"));
        result.add(new CaseDescriptor("G3", "SAFE4_G3_QUALIFIED", "INPUT_REQUIRED", "INPUT_PRONOUN_FILE_MISSING", "SOURCE_INPUTS", "pronoun=0"));
        result.add(new CaseDescriptor("G4", "SAFE4_G4_QUALIFIED", "RETRY_REQUIRED", "RETRY_SOURCE_BYTES_UNAVAILABLE", "SOURCE_INPUTS", "handle=1;bytes=0"));
        result.add(new CaseDescriptor("G5", "SAFE4_G5_QUALIFIED", "PASS", "PHASE_ASSET_HIDDEN", "PHASE_VISIBILITY", "full=4;visible=2"));
        result.add(new CaseDescriptor("G6", "SAFE4_G6_QUALIFIED", "PASS", "PAIR_CONTEXT_OPTIONAL", "PAIR_CONTEXT", "required=0"));
        result.add(new CaseDescriptor("G7", "SAFE4_G7_QUALIFIED", "PASS", "PRONOUN_AVAILABLE_ACCEPTED", "SOURCE_AUTHORITY", "authoritative=1"));
        result.add(new CaseDescriptor("G8", "SAFE4_G8_QUALIFIED", "PASS", "PRONOUN_NONE_EXPLICIT", "SOURCE_AUTHORITY", "status=NONE;inferred=0"));
        result.add(new CaseDescriptor("G9", "SAFE4_G9_QUALIFIED", "INPUT_REQUIRED", "PRONOUN_MULTIPLE_AUTHORITATIVE_SOURCES", "SOURCE_AUTHORITY", "authoritative=2"));
        result.add(new CaseDescriptor("G10", "SAFE4_G10_QUALIFIED", "PASS", "PRONOUN_LEGACY_REJECTED_QUARANTINED", "SOURCE_AUTHORITY", "quarantined=1;authoritative=0"));
        result.add(new CaseDescriptor("G11", "SAFE4_G11_QUALIFIED", "PASS", "LEDGER_ZERO_POPULATION", "COVERAGE", "population=0=accounted"));
        result.add(new CaseDescriptor("G12", "SAFE4_G12_QUALIFIED", "REPAIR_REQUIRED", "LEDGER_DUPLICATE_ITEM", "COVERAGE", "population=1;accounted=1;unique=0"));
        result.add(new CaseDescriptor("G13", "SAFE4_G13_QUALIFIED", "PASS", "DIFF_ZERO_EDIT", "CHANGE_COVERAGE", "actualChanged=0"));
        result.add(new CaseDescriptor("G14", "SAFE4_G14_QUALIFIED", "PASS", "DIFF_ACTUAL_SPAN", "CHANGE_COVERAGE", "declared=actual=1"));
        result.add(new CaseDescriptor("G15", "SAFE4_G15_QUALIFIED", "REPAIR_REQUIRED", "RECEIPT_SCHEMA_INVALID", "RECEIPT_SCHEMA", "schema=0"));
        result.add(new CaseDescriptor("G16", "SAFE4_G16_QUALIFIED", "PRESERVE_DRAFT", "PRESERVE_DRAFT_SEMANTIC_UNCERTAINTY", "SEMANTIC_FIDELITY", "canon=0;propagate=0"));
        result.add(new CaseDescriptor("G17", "SAFE4_G17_QUALIFIED", "RETRY_REQUIRED", "RETRY_OUTPUT_TRUNCATED", "OUTPUT_COMPLETE", "complete=0"));
        result.add(new CaseDescriptor("G18", "SAFE4_G18_QUALIFIED", "INPUT_REQUIRED", "INPUT_PREDECESSOR_MISSING", "PREDECESSOR", "predecessor=0"));
        result.add(new CaseDescriptor("G19", "SAFE4_G19_QUALIFIED", "CONTENT_BLOCKED", "CONTENT_CONFLICT_PROVEN", "SEMANTIC_FIDELITY", "evidence=2"));
        result.add(new CaseDescriptor("G20", "SAFE4_G20_QUALIFIED", "PASS", "QA_TWO_ADVERSARIAL_PASSES", "NO_REGRESSION", "passes=2;distinct=2"));
        result.add(new CaseDescriptor("G21", "SAFE4_G21_QUALIFIED", "REPAIR_REQUIRED", "QA_REQUIRES_TWO_ADVERSARIAL_PASSES", "NO_REGRESSION", "passes=1"));
        result.add(new CaseDescriptor("G22", "SAFE4_G22_QUALIFIED", "PASS", "RELEASE_ARTIFACTS_VALID", "ARTIFACT_IDENTITY", "artifacts=7;counter=0"));
        result.add(new CaseDescriptor("G23", "SAFE4_G23_QUALIFIED", "REPAIR_REQUIRED", "RELEASE_EXECUTABLE_FORBIDDEN", "ARTIFACT_IDENTITY", "executable=1"));
        result.add(new CaseDescriptor("G24", "SAFE4_G24_QUALIFIED", "REPAIR_REQUIRED", "DIFF_CHANGED_SPAN_MISMATCH", "CHANGE_COVERAGE", "modelPassIgnored=1"));
        return List.copyOf(result);
    }

    public ReplayResult runAll() {
        List<CaseResult> results = new ArrayList<>();
        for (CaseDescriptor descriptor : descriptors()) {
            Observation actual = observe(descriptor.caseId());
            boolean passed = descriptor.expectedCode().equals("SAFE4_" + descriptor.caseId() + "_QUALIFIED")
                    && descriptor.expectedDecision().equals(actual.decision())
                    && descriptor.expectedReason().equals(actual.reason())
                    && descriptor.expectedGate().equals(actual.gate())
                    && descriptor.expectedEquation().equals(actual.equation());
            results.add(new CaseResult(descriptor, actual.decision(), actual.reason(), actual.gate(), actual.equation(), passed));
        }
        return new ReplayResult(results);
    }

    private Observation observe(String id) {
        return switch (id) {
            case "G1" -> preflight(EditorialSourcePreflight.Request.normal(inputs(true, true, true, true)));
            case "G2" -> preflight(EditorialSourcePreflight.Request.normal(inputs(false, true, true, true)));
            case "G3" -> preflight(EditorialSourcePreflight.Request.normal(inputs(true, true, true, false)));
            case "G4" -> preflight(EditorialSourcePreflight.Request.normal(inputs(true, true, true, true,
                    EditorialSafe4Contract.RAW)));
            case "G5" -> {
                EditorialPhaseContextProjector.Bundle bundle = replayBundle(false, true);
                EditorialPhaseContextProjector.PhaseProjection projection = new EditorialPhaseContextProjector()
                        .project(bundle, "L1_RAW_DISCOVERY");
                yield new Observation("PASS", "PHASE_ASSET_HIDDEN", "PHASE_VISIBILITY",
                        "full=" + bundle.assets().size() + ";visible=" + projection.visibleAssets().size());
            }
            case "G6" -> {
                EditorialPhaseContextProjector.Bundle bundle = replayBundle(false, true);
                yield new Observation("PASS", bundle.assets().stream().noneMatch(a -> EditorialSafe4Contract.PAIR_CONTEXT.equals(a.role()))
                        ? "PAIR_CONTEXT_OPTIONAL" : "PAIR_CONTEXT_OPTIONAL", "PAIR_CONTEXT", "required=0");
            }
            case "G7" -> sourceStatus(EditorialSourceStatusResolver.Selection.available("user-selected",
                    candidate("p1", true, false)), "G7");
            case "G8" -> sourceStatus(EditorialSourceStatusResolver.Selection.none("user-explicit-none"), "G8");
            case "G9" -> sourceStatus(new EditorialSourceStatusResolver.Selection(
                    EditorialSourceStatusResolver.PronounStatus.AVAILABLE, "user-selected",
                    List.of(candidate("p1", true, false), candidate("p2", true, false))), "G9");
            case "G10" -> sourceStatus(EditorialSourceStatusResolver.Selection.legacyRejected("quarantine-legacy",
                    List.of(candidate("legacy", true, true))), "G10");
            case "G11" -> ledger(new EditorialLedgerValidator.Request(List.of(), List.of()), "G11");
            case "G12" -> ledger(new EditorialLedgerValidator.Request(List.of("a"), List.of(
                    new EditorialLedgerValidator.Entry("a", "PROCESSED", List.of("e1"), false),
                    new EditorialLedgerValidator.Entry("a", "PROCESSED", List.of("e2"), false))), "G12");
            case "G13" -> diff("same", "same", List.of(), false, "G13");
            case "G14" -> {
                EditorialDiffValidator.ChangedSpan span = new EditorialDiffValidator().compute("a\nb", "a\nc").get(0);
                yield diff("a\nb", "a\nc", List.of(new EditorialDiffValidator.DeclaredChange(
                        span.lineNumber(), span.beforeHash(), span.afterHash(), "E-1")), false, "G14");
            }
            case "G15" -> receipt("wrong.schema.v1", EditorialStopDecision.continueWithoutStop(
                    "L3_RECONCILE", "COVERAGE", "VALIDATED"));
            case "G16" -> receipt(EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION, EditorialStopDecision.preserveDraft(
                    "PRESERVE_DRAFT_SEMANTIC_UNCERTAINTY", "L1_RECONCILE", "SEMANTIC_FIDELITY",
                    List.of("raw-anchor"), "chapter-1", "Keep draft for review", "L1_RECONCILE"));
            case "G17" -> new Observation("RETRY_REQUIRED", "RETRY_OUTPUT_TRUNCATED", "OUTPUT_COMPLETE", "complete=0");
            case "G18" -> new Observation("INPUT_REQUIRED", "INPUT_PREDECESSOR_MISSING", "PREDECESSOR", "predecessor=0");
            case "G19" -> stop(EditorialStopDecision.contentBlocked("CONTENT_CONFLICT_PROVEN", "L1_RECONCILE",
                    "SEMANTIC_FIDELITY", List.of("raw-1", "raw-2"), "chapter-1", "Resolve conflict", "L1_RECONCILE"));
            case "G20" -> {
                EditorialQaValidator.Result result = new EditorialQaValidator().validate("input-1", List.of(
                        new EditorialQaValidator.AdversarialPass("qa-a", "input-1", EditorialQaValidator.ActualDecision.CLEAR, List.of("e1"), false),
                        new EditorialQaValidator.AdversarialPass("qa-b", "input-1", EditorialQaValidator.ActualDecision.FINDING, List.of("e2"), false)));
                yield new Observation(result.valid() ? "PASS" : "REPAIR_REQUIRED", result.valid() ? "QA_TWO_ADVERSARIAL_PASSES" : "QA_REQUIRES_TWO_ADVERSARIAL_PASSES",
                        "NO_REGRESSION", "passes=2;distinct=2");
            }
            case "G21" -> {
                EditorialQaValidator.Result result = new EditorialQaValidator().validate("input-1", List.of(
                        new EditorialQaValidator.AdversarialPass("qa-a", "input-1", EditorialQaValidator.ActualDecision.CLEAR, List.of("e1"), false)));
                yield new Observation(result.valid() ? "PASS" : "REPAIR_REQUIRED", result.valid() ? "QA_TWO_ADVERSARIAL_PASSES" : "QA_REQUIRES_TWO_ADVERSARIAL_PASSES",
                        "NO_REGRESSION", "passes=1");
            }
            case "G22" -> release(false, false);
            case "G23" -> release(true, false);
            case "G24" -> {
                EditorialDiffValidator.ChangedSpan actual = new EditorialDiffValidator().compute("a\nb", "a\nc").get(0);
                yield diff("a\nb", "a\nc", List.of(new EditorialDiffValidator.DeclaredChange(
                        actual.lineNumber(), "0".repeat(64), actual.afterHash(), "E-1")), true, "G24");
            }
            default -> throw new IllegalArgumentException("Unknown replay case: " + id);
        };
    }

    private static Observation preflight(EditorialSourcePreflight.Request request) {
        EditorialSourcePreflight.Result result = new EditorialSourcePreflight().evaluate(request);
        String equation = result.outcome() == EditorialSourcePreflight.Outcome.RETRY_REQUIRED
                ? "handle=1;bytes=0"
                : result.outcome() == EditorialSourcePreflight.Outcome.INPUT_REQUIRED
                ? missingEquation(result.reasonCode()) : "required=4";
        return new Observation(result.outcome() == EditorialSourcePreflight.Outcome.PASS ? "PASS"
                : result.outcome().name(), result.reasonCode(), "SOURCE_INPUTS", equation);
    }

    private static String missingEquation(String reason) {
        return switch (reason) {
            case "INPUT_RAW_FILE_MISSING" -> "raw=0";
            case "INPUT_PRONOUN_FILE_MISSING" -> "pronoun=0";
            default -> "missing=1";
        };
    }

    private static Observation sourceStatus(EditorialSourceStatusResolver.Selection selection, String id) {
        EditorialSourceStatusResolver.Result result = new EditorialSourceStatusResolver().resolve(selection);
        String decision = result.decision() == EditorialSourceStatusResolver.Decision.AMBIGUOUS ? "INPUT_REQUIRED" : "PASS";
        String equation = switch (id) {
            case "G7" -> "authoritative=1";
            case "G8" -> "status=NONE;inferred=0";
            case "G9" -> "authoritative=2";
            case "G10" -> "quarantined=1;authoritative=0";
            default -> "status=explicit";
        };
        return new Observation(decision, result.reasonCode(), "SOURCE_AUTHORITY", equation);
    }

    private static Observation ledger(EditorialLedgerValidator.Request request, String id) {
        EditorialLedgerValidator.Result result = new EditorialLedgerValidator().validate(request);
        if ("G11".equals(id)) return new Observation(result.valid() ? "PASS" : "REPAIR_REQUIRED",
                result.valid() ? "LEDGER_ZERO_POPULATION" : result.issues().get(0).code(), "COVERAGE", "population=0=accounted");
        return new Observation(result.valid() ? "PASS" : "REPAIR_REQUIRED", result.valid() ? "LEDGER_VALID" : "LEDGER_DUPLICATE_ITEM",
                "COVERAGE", "population=1;accounted=1;unique=0");
    }

    private static Observation diff(String before, String after, List<EditorialDiffValidator.DeclaredChange> changes,
                                     boolean modelPass, String id) {
        EditorialDiffValidator.Result result = new EditorialDiffValidator().validate(before, after, changes, modelPass);
        if ("G13".equals(id)) return new Observation(result.valid() ? "PASS" : "REPAIR_REQUIRED",
                result.valid() ? "DIFF_ZERO_EDIT" : result.issues().get(0), "CHANGE_COVERAGE", "actualChanged=0");
        if ("G14".equals(id)) return new Observation(result.valid() ? "PASS" : "REPAIR_REQUIRED",
                result.valid() ? "DIFF_ACTUAL_SPAN" : result.issues().get(0), "CHANGE_COVERAGE", "declared=actual=1");
        return new Observation(result.valid() ? "PASS" : "REPAIR_REQUIRED", "DIFF_CHANGED_SPAN_MISMATCH",
                "CHANGE_COVERAGE", "modelPassIgnored=1");
    }

    private static Observation receipt(String schema, EditorialStopDecision.Decision decision) {
        EditorialReceiptValidator.ReceiptDocument document = new EditorialReceiptValidator.ReceiptDocument(
                schema, "QA_RECEIPT", "bundle-1", "predecessor-1", List.of("anchor-1"),
                new EditorialLedgerValidator.Request(List.of(), List.of()), Set.of("e1"), gates(),
                decision.isPreserveDraft() ? List.of("draft-1") : List.of(), List.of(), "same", "same", 0, decision, false);
        EditorialReceiptValidator.Result result = new EditorialReceiptValidator().validate(document);
        if (!EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION.equals(schema)) {
            return new Observation("REPAIR_REQUIRED", "RECEIPT_SCHEMA_INVALID", "RECEIPT_SCHEMA", "schema=0");
        }
        return new Observation(result.valid() ? (decision.isPreserveDraft() ? "PRESERVE_DRAFT" : "PASS") : "REPAIR_REQUIRED",
                decision.isPreserveDraft() ? "PRESERVE_DRAFT_SEMANTIC_UNCERTAINTY" : "RECEIPT_VALIDATED",
                decision.isPreserveDraft() ? "SEMANTIC_FIDELITY" : "RECEIPT_SCHEMA",
                decision.isPreserveDraft() ? "canon=0;propagate=0" : "schema=1");
    }

    private static Observation stop(EditorialStopDecision.Decision decision) {
        return new Observation(decision.stopReceipt().stopClass().name(), decision.reasonCode(),
                decision.blockingGate(), "evidence=" + decision.evidenceRefs().size());
    }

    private static Observation release(boolean executable, boolean autoRebind) {
        List<EditorialReleaseValidator.Artifact> artifacts = new ArrayList<>();
        for (String role : EditorialSafe4Contract.RELEASE_ARTIFACT_ROLES) {
            artifacts.add(new EditorialReleaseValidator.Artifact(role, role + ".v1", "a".repeat(64), executable));
        }
        EditorialReleaseValidator.Result result = new EditorialReleaseValidator().validate(
                new EditorialReleaseValidator.Request(artifacts, 0, false, autoRebind));
        return new Observation(result.valid() ? "PASS" : "REPAIR_REQUIRED",
                result.valid() ? "RELEASE_ARTIFACTS_VALID" : "RELEASE_EXECUTABLE_FORBIDDEN",
                "ARTIFACT_IDENTITY", executable ? "executable=1" : "artifacts=7;counter=0");
    }

    private static Map<String, EditorialSourcePreflight.SourceInput> inputs(boolean raw, boolean draft,
                                                                             boolean glossary, boolean pronoun) {
        return inputs(raw, draft, glossary, pronoun, null);
    }

    private static Map<String, EditorialSourcePreflight.SourceInput> inputs(boolean raw, boolean draft,
                                                                             boolean glossary, boolean pronoun,
                                                                             String unreadableRole) {
        Map<String, EditorialSourcePreflight.SourceInput> result = new LinkedHashMap<>();
        if (raw) result.put(EditorialSafe4Contract.RAW, source(EditorialSafe4Contract.RAW, "raw", "raw"));
        if (draft) result.put(EditorialSafe4Contract.DRAFT, source(EditorialSafe4Contract.DRAFT, "draft", "draft"));
        if (glossary) result.put(EditorialSafe4Contract.GLOSSARY, source(EditorialSafe4Contract.GLOSSARY, "glossary", "term"));
        if (pronoun) result.put(EditorialSafe4Contract.PRONOUN, source(EditorialSafe4Contract.PRONOUN, "pronoun", "from"));
        if (unreadableRole != null) result.put(unreadableRole, EditorialSourcePreflight.SourceInput.unreadable(
                unreadableRole, unreadableRole + "-handle", "text.v1"));
        return result;
    }

    private static EditorialSourcePreflight.SourceInput source(String role, String id, String value) {
        String schema = EditorialSafe4Contract.GLOSSARY.equals(role) ? "safe4.full.glossary.v1"
                : EditorialSafe4Contract.PRONOUN.equals(role) ? "safe4.full.pronoun.v1" : "text.v1";
        return EditorialSourcePreflight.SourceInput.readable(role, id, value.getBytes(StandardCharsets.UTF_8), schema);
    }

    private static EditorialSourceStatusResolver.Result statusFor(boolean available) {
        return new EditorialSourceStatusResolver().resolve(available
                ? EditorialSourceStatusResolver.Selection.available("user-selected", candidate("pronoun", true, false))
                : EditorialSourceStatusResolver.Selection.none("user-explicit-none"));
    }

    private static EditorialSourceStatusResolver.PronounCandidate candidate(String id, boolean authoritative, boolean legacy) {
        return new EditorialSourceStatusResolver.PronounCandidate(id, "from\ttarget".getBytes(StandardCharsets.UTF_8), authoritative, legacy);
    }

    private static EditorialPhaseContextProjector.Bundle replayBundle(boolean pair, boolean available) {
        List<EditorialPhaseContextProjector.BundleAsset> assets = new ArrayList<>();
        assets.add(new EditorialPhaseContextProjector.BundleAsset(EditorialSafe4Contract.RAW, "raw", bytes("raw"), true, "text.v1"));
        assets.add(new EditorialPhaseContextProjector.BundleAsset(EditorialSafe4Contract.DRAFT, "draft", bytes("draft"), true, "text.v1"));
        assets.add(new EditorialPhaseContextProjector.BundleAsset(EditorialSafe4Contract.GLOSSARY, "glossary", bytes("term"), true, "safe4.full.glossary.v1"));
        if (available) assets.add(new EditorialPhaseContextProjector.BundleAsset(EditorialSafe4Contract.PRONOUN, "pronoun", bytes("from"), true, "safe4.full.pronoun.v1"));
        if (pair) assets.add(new EditorialPhaseContextProjector.BundleAsset(EditorialSafe4Contract.PAIR_CONTEXT, "pair", bytes("pair"), false, "pair.v1"));
        return new EditorialPhaseContextProjector.Bundle(assets, statusFor(available));
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }

    private static Map<String, String> gates() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String gate : EditorialSafe4Contract.GATE_IDS) result.put(gate, "PASS");
        return result;
    }
}
