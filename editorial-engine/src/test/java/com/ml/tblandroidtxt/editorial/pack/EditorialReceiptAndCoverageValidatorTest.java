package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EditorialReceiptAndCoverageValidatorTest {
    @Test public void zeroPopulationAndZeroEditAreValid() {
        EditorialReceiptValidator.Result result = new EditorialReceiptValidator().validate(validReceipt(
                new EditorialLedgerValidator.Request(List.of(), List.of()), "same", List.of(),
                EditorialStopDecision.continueWithoutStop("L1_RECONCILE", "COVERAGE", "NO_CHANGES")));
        assertTrue(result.valid());
        assertTrue(result.canonAllowed());
        assertTrue(result.propagationAllowed());
    }

    @Test public void ledgerIsExhaustiveAndDoesNotTrustModelPass() {
        EditorialLedgerValidator validator = new EditorialLedgerValidator();
        EditorialLedgerValidator.Result valid = validator.validate(new EditorialLedgerValidator.Request(
                List.of("a", "b"), List.of(
                new EditorialLedgerValidator.Entry("a", "PROCESSED", List.of("e-a"), true),
                new EditorialLedgerValidator.Entry("b", "PRESERVE_DRAFT", List.of("e-b"), true))));
        assertTrue(valid.valid());
        EditorialLedgerValidator.Result invalid = validator.validate(new EditorialLedgerValidator.Request(
                List.of("a"), List.of(new EditorialLedgerValidator.Entry("a", "PASS", List.of(), true))));
        assertFalse(invalid.valid());
        assertTrue(invalid.issues().stream().anyMatch(issue -> issue.code().contains("MODEL_PASS")));
    }

    @Test public void actualDiffMustMatchDeclaredErrorMapping() {
        EditorialDiffValidator validator = new EditorialDiffValidator();
        EditorialDiffValidator.ChangedSpan actual = validator.compute("a\nb", "a\nc").get(0);
        EditorialDiffValidator.Result valid = validator.validate("a\nb", "a\nc", List.of(
                new EditorialDiffValidator.DeclaredChange(actual.lineNumber(), actual.beforeHash(), actual.afterHash(), "E-1")), true);
        assertTrue(valid.valid());
        EditorialDiffValidator.Result invalid = validator.validate("a\nb", "a\nc", List.of(
                new EditorialDiffValidator.DeclaredChange(2, "0".repeat(64), actual.afterHash(), "E-1")), true);
        assertFalse(invalid.valid());
    }

    @Test public void preserveDraftCannotBecomeCanonOrPropagation() {
        EditorialReceiptValidator.Result result = new EditorialReceiptValidator().validate(validReceipt(
                new EditorialLedgerValidator.Request(List.of("a"), List.of(
                        new EditorialLedgerValidator.Entry("a", "PRESERVE_DRAFT", List.of("raw-1"), false))),
                "same", List.of("a"), EditorialStopDecision.preserveDraft(
                        "PRESERVE_DRAFT_SEMANTIC_UNCERTAINTY", "L1_RECONCILE", "SEMANTIC_FIDELITY",
                        List.of("raw-1"), "chapter-1", "Keep draft for review", "L1_RECONCILE")));
        assertTrue(result.valid());
        assertFalse(result.canonAllowed());
        assertFalse(result.propagationAllowed());
    }

    @Test public void schemaAndGateFailuresAreTypedValidationFailures() {
        EditorialReceiptValidator.ReceiptDocument invalid = validDocument(
                EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION, "same", "pred");
        EditorialReceiptValidator.ReceiptDocument wrongSchema = new EditorialReceiptValidator.ReceiptDocument(
                "wrong.schema.v1", invalid.artifactType(), invalid.bundleIdentity(), invalid.predecessorIdentity(),
                invalid.stableAnchors(), invalid.ledger(), invalid.evidenceRefs(), invalid.gates(), invalid.preservedInventory(),
                invalid.declaredChanges(), invalid.beforeText(), invalid.afterText(), invalid.releaseAttemptCount(), invalid.disposition(), false);
        assertFalse(new EditorialReceiptValidator().validate(wrongSchema).valid());
        Map<String, String> badGates = new LinkedHashMap<>(invalid.gates());
        badGates.put("COVERAGE", "BLOCKED");
        EditorialReceiptValidator.ReceiptDocument wrongGate = new EditorialReceiptValidator.ReceiptDocument(
                invalid.schemaVersion(), invalid.artifactType(), invalid.bundleIdentity(), invalid.predecessorIdentity(),
                invalid.stableAnchors(), invalid.ledger(), invalid.evidenceRefs(), badGates, invalid.preservedInventory(),
                invalid.declaredChanges(), invalid.beforeText(), invalid.afterText(), invalid.releaseAttemptCount(), invalid.disposition(), false);
        assertFalse(new EditorialReceiptValidator().validate(wrongGate).valid());
    }

    @Test public void qaRequiresTwoIndependentPassesAndReleaseIsFailClosed() {
        EditorialQaValidator qa = new EditorialQaValidator();
        assertTrue(qa.validate("input-1", List.of(
                new EditorialQaValidator.AdversarialPass("qa-a", "input-1", EditorialQaValidator.ActualDecision.CLEAR, List.of("e1"), true),
                new EditorialQaValidator.AdversarialPass("qa-b", "input-1", EditorialQaValidator.ActualDecision.FINDING, List.of("e2"), true))).valid());
        assertFalse(qa.validate("input-1", List.of(
                new EditorialQaValidator.AdversarialPass("qa-a", "input-1", EditorialQaValidator.ActualDecision.CLEAR, List.of("e1"), false))).valid());

        List<EditorialReleaseValidator.Artifact> artifacts = new ArrayList<>();
        for (String role : EditorialSafe4Contract.RELEASE_ARTIFACT_ROLES) {
            artifacts.add(new EditorialReleaseValidator.Artifact(role, role + ".v1", "a".repeat(64), false));
        }
        assertTrue(new EditorialReleaseValidator().validate(new EditorialReleaseValidator.Request(artifacts, 0, false, false)).valid());
        assertFalse(new EditorialReleaseValidator().validate(new EditorialReleaseValidator.Request(artifacts, 1, false, false)).valid());
    }

    private static EditorialReceiptValidator.ReceiptDocument validDocument(String schema, String bundle, String predecessor) {
        return new EditorialReceiptValidator.ReceiptDocument(schema, "QA_RECEIPT", bundle, predecessor,
                List.of("anchor-1"), new EditorialLedgerValidator.Request(List.of(), List.of()),
                Set.of("evidence-1"), gates(), List.of(), List.of(), "same", "same", 0,
                EditorialStopDecision.continueWithoutStop("L3_RECONCILE", "COVERAGE", "VALIDATED"), false);
    }

    private static EditorialReceiptValidator.ReceiptDocument validReceipt(EditorialLedgerValidator.Request ledger,
                                                                          String after, List<String> preserved,
                                                                          EditorialStopDecision.Decision decision) {
        EditorialReceiptValidator.ReceiptDocument base = validDocument(
                EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION, "bundle-1", "predecessor-1");
        return new EditorialReceiptValidator.ReceiptDocument(base.schemaVersion(), base.artifactType(), base.bundleIdentity(),
                base.predecessorIdentity(), base.stableAnchors(), ledger, base.evidenceRefs(), base.gates(), preserved,
                List.of(), "same", after, 0, decision, false);
    }

    private static Map<String, String> gates() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String gate : EditorialSafe4Contract.GATE_IDS) result.put(gate, "PASS");
        return result;
    }
}
