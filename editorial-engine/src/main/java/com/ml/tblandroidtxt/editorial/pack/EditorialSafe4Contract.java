package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    /** Exact machine-contract projection shared with Pack Manifest v1. */
    public static Map<String, Object> machineContractProjection() {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("contractVersion", CONTRACT_VERSION);
        root.put("schemaVersion", RECEIPT_SCHEMA_VERSION);
        root.put("requiredCapabilities", IMPLEMENTED_CAPABILITY_IDS);
        root.put("inputRoles", List.of(
                Map.of("role", RAW, "cardinality", "ONE", "required", true),
                Map.of("role", DRAFT, "cardinality", "ONE", "required", true),
                Map.of("role", GLOSSARY, "cardinality", "ONE", "required", true),
                Map.of("role", PRONOUN, "cardinality", "ONE", "required", true)));
        root.put("pronounPolicy", Map.of(
                "allowedStatuses", List.of("AVAILABLE", "NONE", "LEGACY_REJECTED"),
                "defaultStatus", "NONE", "availableRequiresRole", PRONOUN,
                "noneForbidsRole", true, "legacyRejectedForbidsRole", true,
                "fallbackLookupAllowed", false));
        root.put("pairContextPolicy", Map.of(
                "optional", true, "source", PAIR_CONTEXT, "qaConfirmedRequired", false,
                "sameProjectRequired", true, "samePackHashRequired", true,
                "scopeRequired", false, "boundaryRequired", false));
        root.put("phaseGraph", Map.of(
                "profile", CONTRACT_VERSION, "initialPhase", PHASES.get(0),
                "terminalPhase", PHASES.get(PHASES.size() - 1), "phases", PHASES,
                "edges", PHASE_EDGES));

        LinkedHashMap<String, Object> contexts = new LinkedHashMap<>();
        contexts.put("L1_SOURCE_PREFLIGHT", context(List.of(RAW, DRAFT, GLOSSARY, PRONOUN), List.of(RAW, DRAFT, GLOSSARY, PRONOUN)));
        contexts.put("L1_RAW_DISCOVERY", context(List.of(RAW, GLOSSARY), List.of(RAW, GLOSSARY)));
        contexts.put("L1_RECONCILE", context(List.of(RAW, DRAFT, GLOSSARY), List.of(RAW, DRAFT, GLOSSARY, PRONOUN, PAIR_CONTEXT)));
        contexts.put("L2_RAW_DISCOVERY", context(List.of(RAW, GLOSSARY), List.of(RAW, GLOSSARY)));
        contexts.put("L2_EDIT", context(List.of(RAW, DRAFT, GLOSSARY, "REPORT_L1"), List.of(RAW, DRAFT, GLOSSARY, "REPORT_L1", PRONOUN, PAIR_CONTEXT)));
        contexts.put("L3_RAW_FIRST_REAUDIT", context(List.of(RAW, GLOSSARY, "VI_L2"), List.of(RAW, GLOSSARY, "VI_L2")));
        contexts.put("L3_RECONCILE", context(List.of(RAW, DRAFT, GLOSSARY, "REPORT_L1", "VI_L2", "CHANGE_MAP_L2"),
                List.of(RAW, DRAFT, GLOSSARY, "REPORT_L1", "VI_L2", "CHANGE_MAP_L2", PRONOUN, PAIR_CONTEXT)));
        root.put("contextAllowList", contexts);

        List<Object> evidence = new ArrayList<>();
        evidence.add(Map.of("evidenceType", "REPORT_L1", "schemaId", "safe4.full.report-l1.v1"));
        evidence.add(Map.of("evidenceType", "VI_L2", "schemaId", "safe4.full.vi-l2.v1"));
        evidence.add(Map.of("evidenceType", "CHANGE_MAP_L2", "schemaId", "safe4.full.change-map-l2.v1"));
        evidence.add(Map.of("evidenceType", "FINAL_QA", "schemaId", "safe4.full.final-qa.v1"));
        evidence.add(Map.of("evidenceType", "QA_RECEIPT", "schemaId", RECEIPT_SCHEMA_VERSION));
        root.put("evidenceSchemas", evidence);

        List<Object> gates = new ArrayList<>();
        for (String gate : GATE_IDS) {
            gates.add(Map.of("gateId", gate, "calculatorId", "safe4.full.gate." + gate.toLowerCase(java.util.Locale.ROOT),
                    "requires", List.of("QA_RECEIPT")));
        }
        root.put("gateDefinitions", gates);

        List<Object> artifacts = new ArrayList<>();
        for (String role : RELEASE_ARTIFACT_ROLES) {
            String contract = switch (role) {
                case "REPORT_L1" -> "safe4.full.report_l1.v1";
                case "VI_L2" -> "safe4.full.vi_l2.v1";
                case "CHANGE_MAP_L2" -> "safe4.full.change_map_l2.v1";
                case "FINAL_QA" -> "safe4.full.final_qa.v1";
                case "QA_RECEIPT" -> "safe4.full.qa_receipt.v1";
                case "SHA256_MANIFEST" -> "safe4.full.sha256_manifest.v1";
                case "PROJECT_SOURCE" -> "safe4.full.project_source.v1";
                default -> throw new IllegalArgumentException("Unknown release role: " + role);
            };
            artifacts.add(Map.of("role", role, "required", true, "nameTemplate", "{chapter}." + role.toLowerCase(java.util.Locale.ROOT) + ".json",
                    "contentContract", contract, "presentWhen", "RELEASE"));
        }
        root.put("releaseArtifacts", Map.of("maximumFiles", BigDecimal.valueOf(7), "artifacts", artifacts,
                "forbiddenArtifacts", List.of("EXECUTABLE_CODE", "REMOTE_SCRIPT")));
        return root;
    }

    public static String machineContractFingerprint() {
        return EditorialCanonicalJson.sha256Hex(EditorialCanonicalJson.canonicalize(machineContractProjection())
                .getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, Object> context(List<String> required, List<String> allowed) {
        return Map.of("required", required, "allowed", allowed);
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
