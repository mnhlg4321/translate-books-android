package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Validates two independent L3 adversarial observations from local evidence. */
public final class EditorialQaValidator {
    public enum ActualDecision { CLEAR, FINDING }

    public record AdversarialPass(String passId, String inputIdentity,
                                  ActualDecision actualDecision,
                                  List<String> evidenceRefs, boolean modelDeclaredPass) {
        public AdversarialPass {
            Objects.requireNonNull(passId, "passId");
            Objects.requireNonNull(inputIdentity, "inputIdentity");
            Objects.requireNonNull(actualDecision, "actualDecision");
            evidenceRefs = List.copyOf(evidenceRefs == null ? List.of() : evidenceRefs);
        }
    }

    public record Result(boolean valid, List<String> issues) {
        public Result { issues = List.copyOf(issues == null ? List.of() : issues); }
    }

    public Result validate(String expectedInputIdentity, List<AdversarialPass> passes) {
        List<String> issues = new ArrayList<>();
        if (expectedInputIdentity == null || expectedInputIdentity.isBlank()) issues.add("QA_INPUT_IDENTITY_MISSING");
        if (passes == null || passes.size() != 2) issues.add("QA_REQUIRES_TWO_ADVERSARIAL_PASSES");
        if (passes != null) {
            Set<String> ids = new HashSet<>();
            for (AdversarialPass pass : passes) {
                if (pass == null) { issues.add("QA_PASS_MISSING"); continue; }
                if (!ids.add(pass.passId())) issues.add("QA_PASS_ID_DUPLICATE:" + pass.passId());
                if (!pass.inputIdentity().equals(expectedInputIdentity)) issues.add("QA_INPUT_IDENTITY_MISMATCH:" + pass.passId());
                if (pass.evidenceRefs().isEmpty() || pass.evidenceRefs().stream().anyMatch(value -> value == null || value.isBlank())) {
                    issues.add("QA_EVIDENCE_MISSING:" + pass.passId());
                }
            }
        }
        return new Result(issues.isEmpty(), issues);
    }
}
