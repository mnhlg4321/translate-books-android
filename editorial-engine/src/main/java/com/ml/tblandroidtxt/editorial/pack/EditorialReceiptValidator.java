package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Validates typed receipt objects and derives canon/propagation disposition locally. */
public final class EditorialReceiptValidator {
    public static final Set<String> RECEIPT_ARTIFACT_TYPES = Set.of(
            "REPORT_L1", "VI_L2", "CHANGE_MAP_L2", "FINAL_QA", "QA_RECEIPT");

    public record ReceiptDocument(String schemaVersion, String artifactType,
                                  String bundleIdentity, String predecessorIdentity,
                                  List<String> stableAnchors,
                                  EditorialLedgerValidator.Request ledger,
                                  Set<String> evidenceRefs, Map<String, String> gates,
                                  List<String> preservedInventory,
                                  List<EditorialDiffValidator.DeclaredChange> declaredChanges,
                                  String beforeText, String afterText,
                                  int releaseAttemptCount,
                                  EditorialStopDecision.Decision disposition,
                                  boolean modelDeclaredPass) {
        public ReceiptDocument {
            Objects.requireNonNull(schemaVersion, "schemaVersion");
            Objects.requireNonNull(artifactType, "artifactType");
            Objects.requireNonNull(bundleIdentity, "bundleIdentity");
            Objects.requireNonNull(predecessorIdentity, "predecessorIdentity");
            stableAnchors = List.copyOf(stableAnchors == null ? List.of() : stableAnchors);
            evidenceRefs = Set.copyOf(evidenceRefs == null ? Set.of() : evidenceRefs);
            gates = Map.copyOf(gates == null ? Map.of() : gates);
            preservedInventory = List.copyOf(preservedInventory == null ? List.of() : preservedInventory);
            declaredChanges = List.copyOf(declaredChanges == null ? List.of() : declaredChanges);
            Objects.requireNonNull(disposition, "disposition");
        }
    }

    public record Result(boolean valid, boolean canonAllowed, boolean propagationAllowed,
                         List<String> issues, String receiptFingerprint) {
        public Result {
            issues = List.copyOf(issues == null ? List.of() : issues);
            Objects.requireNonNull(receiptFingerprint, "receiptFingerprint");
        }
    }

    public Result validate(ReceiptDocument document) {
        if (document == null) return invalid(List.of("RECEIPT_MISSING"));
        List<String> issues = new ArrayList<>();
        if (!EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION.equals(document.schemaVersion())) {
            issues.add("RECEIPT_SCHEMA_INVALID");
        }
        if (!RECEIPT_ARTIFACT_TYPES.contains(document.artifactType())) issues.add("RECEIPT_ARTIFACT_TYPE_INVALID");
        if (document.bundleIdentity().isBlank()) issues.add("RECEIPT_BUNDLE_IDENTITY_MISSING");
        if (document.predecessorIdentity().isBlank()) issues.add("RECEIPT_PREDECESSOR_IDENTITY_MISSING");
        if (document.stableAnchors().isEmpty()
                || document.stableAnchors().stream().anyMatch(value -> value == null || value.isBlank())
                || document.stableAnchors().size() != new HashSet<>(document.stableAnchors()).size()) {
            issues.add("RECEIPT_STABLE_ANCHOR_INVALID");
        }
        if (document.evidenceRefs().stream().anyMatch(value -> value == null || value.isBlank())) {
            issues.add("RECEIPT_EVIDENCE_REF_INVALID");
        }
        if (document.gates().size() != EditorialSafe4Contract.GATE_IDS.size()
                || !document.gates().keySet().equals(new HashSet<>(EditorialSafe4Contract.GATE_IDS))) {
            issues.add("RECEIPT_GATE_DEFINITION_INVALID");
        }
        for (String gate : EditorialSafe4Contract.GATE_IDS) {
            String value = document.gates().get(gate);
            if (value == null || !("PASS".equals(value) || "NOT_APPLICABLE".equals(value))) {
                issues.add("RECEIPT_GATE_STATUS_INVALID:" + gate);
            }
        }
        if (document.releaseAttemptCount() < 0) issues.add("RECEIPT_RELEASE_COUNTER_INVALID");

        EditorialLedgerValidator.Result ledger = new EditorialLedgerValidator().validate(document.ledger());
        if (!ledger.valid()) issues.addAll(ledger.issues().stream()
                .map(issue -> issue.code() + ":" + issue.itemId()).toList());
        EditorialDiffValidator.Result diff = new EditorialDiffValidator().validate(
                document.beforeText(), document.afterText(), document.declaredChanges(), document.modelDeclaredPass());
        if (!diff.valid()) issues.addAll(diff.issues());

        boolean preserve = document.disposition().isPreserveDraft();
        if (preserve && document.preservedInventory().isEmpty()) issues.add("RECEIPT_PRESERVED_INVENTORY_MISSING");
        if (preserve && document.disposition().isStop()) issues.add("RECEIPT_PRESERVE_STOP_OVERLAP");
        if (document.disposition().isStop() && document.disposition().stopReceipt() == null) {
            issues.add("RECEIPT_TYPED_STOP_MISSING");
        }
        String fingerprint = fingerprint(document);
        boolean valid = issues.isEmpty();
        return new Result(valid, valid && !preserve && !document.disposition().isStop(),
                valid && !preserve && !document.disposition().isStop(), issues, fingerprint);
    }

    private static Result invalid(List<String> issues) {
        return new Result(false, false, false, issues, "");
    }

    private static String fingerprint(ReceiptDocument document) {
        String canonical = document.schemaVersion() + "\n" + document.artifactType() + "\n"
                + document.bundleIdentity() + "\n" + document.predecessorIdentity() + "\n"
                + String.join("\n", new java.util.TreeSet<>(document.evidenceRefs())) + "\n"
                + new java.util.TreeMap<>(document.gates()) + "\n"
                + document.disposition().disposition();
        return EditorialCanonicalJson.sha256Hex(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
