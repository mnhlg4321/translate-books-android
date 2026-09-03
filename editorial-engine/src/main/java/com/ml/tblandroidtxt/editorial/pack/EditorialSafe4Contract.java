package com.ml.tblandroidtxt.editorial.pack;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Code-owned constants for the deterministic SAFE4 full contract. */
public final class EditorialSafe4Contract {
    public static final String CONTRACT_VERSION = "safe4.full.three-pass.v1";
    public static final String RECEIPT_SCHEMA_VERSION = "safe4.full.receipt.v1";
    public static final String NORMAL_MODE = "NORMAL_FOUR_SOURCE";
    public static final String ALTERNATE_MODE = "ALTERNATE_EXPLICIT";

    public static final String RAW = "RAW";
    public static final String DRAFT = "DRAFT";
    public static final String GLOSSARY = "GLOSSARY";
    public static final String PRONOUN = "PRONOUN";
    public static final String PAIR_CONTEXT = "PAIR_CONTEXT";

    public static final String PACK_INTEGRITY_SHA256_V1 = "pack.integrity.sha256.v1";
    public static final String SOURCE_PREFLIGHT_SAFE4_FULL_V1 = "source.preflight.safe4-full.v1";
    public static final String BUNDLE_PHASE_VISIBILITY_SAFE4_FULL_V1 = "bundle.phase-visibility.safe4-full.v1";
    public static final String STATUS_GLOSSARY_PRONOUN_SAFE4_FULL_V1 = "status.glossary-pronoun.safe4-full.v1";
    public static final String LEDGER_EXHAUSTIVE_SAFE4_FULL_V1 = "ledger.exhaustive.safe4-full.v1";
    public static final String PRESERVE_DRAFT_SAFE4_FULL_V1 = "preserve.draft.safe4-full.v1";
    public static final String STOP_TYPED_SAFE4_FULL_V1 = "stop.typed.safe4-full.v1";
    public static final String DIFF_CHANGE_COVERAGE_V1 = "diff.change-coverage.v1";
    public static final String QA_L3_TWO_ADVERSARIAL_V1 = "qa.l3-two-adversarial.v1";
    public static final String RELEASE_SAFE4_FULL_V1 = "release.safe4-full.v1";
    public static final String REPLAY_SAFE4_G1_G24_V1 = "replay.safe4.g1-g24.v1";

    public static final List<String> IMPLEMENTED_CAPABILITY_IDS = List.of(
            PACK_INTEGRITY_SHA256_V1,
            SOURCE_PREFLIGHT_SAFE4_FULL_V1,
            BUNDLE_PHASE_VISIBILITY_SAFE4_FULL_V1,
            STATUS_GLOSSARY_PRONOUN_SAFE4_FULL_V1,
            LEDGER_EXHAUSTIVE_SAFE4_FULL_V1,
            PRESERVE_DRAFT_SAFE4_FULL_V1,
            STOP_TYPED_SAFE4_FULL_V1,
            DIFF_CHANGE_COVERAGE_V1,
            QA_L3_TWO_ADVERSARIAL_V1,
            RELEASE_SAFE4_FULL_V1,
            REPLAY_SAFE4_G1_G24_V1);

    public static final List<String> PHASES = List.of(
            "L1_SOURCE_PREFLIGHT",
            "L1_RAW_DISCOVERY",
            "L1_RECONCILE",
            "L2_RAW_DISCOVERY",
            "L2_EDIT",
            "L3_RAW_FIRST_REAUDIT",
            "L3_RECONCILE");

    public static final List<List<String>> PHASE_EDGES = List.of(
            List.of(PHASES.get(0), PHASES.get(1)),
            List.of(PHASES.get(1), PHASES.get(2)),
            List.of(PHASES.get(2), PHASES.get(3)),
            List.of(PHASES.get(3), PHASES.get(4)),
            List.of(PHASES.get(4), PHASES.get(5)),
            List.of(PHASES.get(5), PHASES.get(6)));

    public static final List<String> REQUIRED_SOURCE_ROLES = List.of(RAW, DRAFT, GLOSSARY, PRONOUN);
    public static final List<String> EVIDENCE_SCHEMA_IDS = List.of(
            "safe4.full.report-l1.v1",
            "safe4.full.vi-l2.v1",
            "safe4.full.change-map-l2.v1",
            "safe4.full.final-qa.v1",
            RECEIPT_SCHEMA_VERSION);
    public static final List<String> GATE_IDS = List.of(
            "ARTIFACT_IDENTITY", "COVERAGE", "TITLE_GLOSSARY", "SEMANTIC_FIDELITY",
            "RELATION_PAIR_PROOF", "SPEAKER", "CHANGE_COVERAGE", "NO_REGRESSION",
            "CONTINUITY_STRUCTURE_TECHNICAL");
    public static final List<String> RELEASE_ARTIFACT_ROLES = List.of(
            "REPORT_L1", "VI_L2", "CHANGE_MAP_L2", "FINAL_QA", "QA_RECEIPT",
            "SHA256_MANIFEST", "PROJECT_SOURCE");

    private static final String EVIDENCE_DOMAIN = "EDITORIAL_SAFE4_CAPABILITY_EVIDENCE_V1\n";

    private EditorialSafe4Contract() { }

    /** Stable code-owned fingerprint for a capability's contract/evidence descriptor. */
    public static String evidenceFingerprint(String capabilityId, String owner,
                                             String positiveTest, String negativeTest) {
        String descriptor = capabilityId + "\n" + owner + "\n" + positiveTest + "\n" + negativeTest;
        return EditorialCanonicalJson.sha256Hex((EVIDENCE_DOMAIN + descriptor)
                .getBytes(StandardCharsets.UTF_8));
    }

    /** Stable fingerprint for a named schema, gate, or release descriptor. */
    public static String descriptorFingerprint(String namespace, String id) {
        return EditorialCanonicalJson.sha256Hex((namespace + "\n" + id)
                .getBytes(StandardCharsets.UTF_8));
    }

    public static Map<String, String> phaseRequiredRoles() {
        return Map.of(
                "L1_SOURCE_PREFLIGHT", ListString.join(RAW, DRAFT, GLOSSARY, PRONOUN),
                "L1_RAW_DISCOVERY", ListString.join(RAW, GLOSSARY),
                "L1_RECONCILE", ListString.join(RAW, DRAFT, GLOSSARY),
                "L2_RAW_DISCOVERY", ListString.join(RAW, GLOSSARY),
                "L2_EDIT", ListString.join(RAW, DRAFT, GLOSSARY, "REPORT_L1"),
                "L3_RAW_FIRST_REAUDIT", ListString.join(RAW, GLOSSARY, "VI_L2"),
                "L3_RECONCILE", ListString.join(RAW, DRAFT, GLOSSARY, "REPORT_L1", "VI_L2", "CHANGE_MAP_L2"));
    }

    private static final class ListString {
        private ListString() { }
        static String join(String... values) { return String.join("\u0000", values); }
    }
}
