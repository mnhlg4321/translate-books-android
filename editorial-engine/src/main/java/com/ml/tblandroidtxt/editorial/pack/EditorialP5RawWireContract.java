package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The bounded provider wire contract for one L1 RAW discovery call.
 *
 * <p>This is deliberately not a replacement for REPORT_L1 or its receipt.
 * It is a small, model-facing DTO contract. The app owns the source bytes,
 * final identities and canonical report materialization.</p>
 */
public final class EditorialP5RawWireContract {
    public static final String SCHEMA_VERSION = "safe4.raw.discovery.wire.v1";
    public static final String SCHEMA_NAME = "safe4_raw_discovery_v1";
    public static final String FINAL_REPORT_SCHEMA = "safe4.full.report-l1.v1";
    public static final String FINAL_PHASE = "L1";
    public static final String REASONING_POLICY = "minimal";

    /** Effective output cap remains the existing P5 authorization cap. */
    public static final int OUTPUT_TOKEN_CAP = 4_096;

    /** Raw response bytes are bounded independently of the provider body limit. */
    public static final int MAX_WIRE_BYTES = 3_584;
    public static final int MAX_FINDINGS = 4;
    public static final int MAX_EVIDENCE_REFS = 8;
    public static final int MAX_ENTRY_EVIDENCE_REFS = 2;
    public static final int MAX_DISPOSITION_EVIDENCE_REFS = 4;
    public static final int MAX_PRESERVED_ITEMS = 4;
    public static final int MAX_ID_LENGTH = 48;
    public static final int MAX_REF_LENGTH = 48;
    public static final int MAX_DISPOSITION_TEXT_LENGTH = 32;

    private EditorialP5RawWireContract() { }

    /**
     * JSON Schema sent in OpenRouter's response_format. Every object is
     * closed so an accidental source-sized or unknown field cannot be
     * accepted by a structured-output provider.
     */
    public static Map<String, Object> jsonSchema() {
        LinkedHashMap<String, Object> root = object();
        root.put("type", "object");
        root.put("additionalProperties", false);

        LinkedHashMap<String, Object> properties = object();
        properties.put("wireSchemaVersion", stringSchema(64, null));
        properties.put("attemptIdentity", stringSchema(64, "[0-9a-f]{64}"));
        properties.put("requestEnvelopeHash", stringSchema(64, "[0-9a-f]{64}"));
        properties.put("findings", findingsSchema());
        properties.put("gateObservations", gatesSchema());
        properties.put("evidenceRefs", arraySchema(MAX_EVIDENCE_REFS,
                stringSchema(MAX_REF_LENGTH, tokenPattern(MAX_REF_LENGTH))));
        properties.put("preservedInventory", arraySchema(MAX_PRESERVED_ITEMS,
                stringSchema(MAX_ID_LENGTH, tokenPattern(MAX_ID_LENGTH))));
        properties.put("declaredChanges", emptyArraySchema());
        properties.put("disposition", dispositionSchema());
        properties.put("modelDeclaredPass", Map.of("type", "boolean"));
        root.put("properties", properties);
        root.put("required", List.of(
                "wireSchemaVersion", "attemptIdentity", "requestEnvelopeHash", "findings",
                "gateObservations", "evidenceRefs", "preservedInventory", "declaredChanges",
                "disposition", "modelDeclaredPass"));
        return root;
    }

    /** Deterministic size evidence for the declared worst-case compact wire. */
    public static int worstCaseWireBytes() {
        String idPrefix = "I".repeat(MAX_ID_LENGTH - 1);
        String refPrefix = "R".repeat(MAX_REF_LENGTH - 1);
        ArrayList<Object> findings = new ArrayList<>();
        for (int i = 0; i < MAX_FINDINGS; i++) {
            String id = idPrefix + i;
            String ref = refPrefix + i;
            findings.add(Map.of(
                    "itemId", id,
                    "disposition", "NOT_EVALUATED",
                    "evidenceRefs", List.of(ref, refPrefix + (i + MAX_FINDINGS)),
                    "modelDeclaredPass", false));
        }
        ArrayList<String> evidence = new ArrayList<>();
        for (int i = 0; i < MAX_EVIDENCE_REFS; i++) evidence.add(refPrefix + i);
        ArrayList<String> preserved = new ArrayList<>();
        for (int i = 0; i < MAX_PRESERVED_ITEMS; i++) preserved.add(idPrefix + i);

        LinkedHashMap<String, String> gates = new LinkedHashMap<>();
        for (String gate : EditorialSafe4Contract.GATE_IDS) gates.put(gate, "NOT_APPLICABLE");

        ArrayList<String> dispositionEvidence = new ArrayList<>();
        for (int i = 0; i < MAX_DISPOSITION_EVIDENCE_REFS; i++) {
            dispositionEvidence.add(refPrefix + i);
        }
        Map<String, Object> disposition = Map.of(
                "disposition", "CONTINUE",
                "reasonCode", "R".repeat(MAX_DISPOSITION_TEXT_LENGTH),
                "phase", FINAL_PHASE,
                "blockingGate", "CONTINUITY_STRUCTURE_TECHNICAL",
                "evidenceRefs", dispositionEvidence,
                "affectedScope", "A".repeat(MAX_DISPOSITION_TEXT_LENGTH),
                "recoveryAction", "C".repeat(MAX_DISPOSITION_TEXT_LENGTH),
                "resumeFrom", "S".repeat(MAX_DISPOSITION_TEXT_LENGTH),
                "stopClass", "NONE",
                "retryable", false);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("wireSchemaVersion", SCHEMA_VERSION);
        root.put("attemptIdentity", "a".repeat(64));
        root.put("requestEnvelopeHash", "b".repeat(64));
        root.put("findings", findings);
        root.put("gateObservations", gates);
        root.put("evidenceRefs", evidence);
        root.put("preservedInventory", preserved);
        root.put("declaredChanges", List.of());
        root.put("disposition", disposition);
        root.put("modelDeclaredPass", false);
        return EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8).length;
    }

    static List<String> validate(EditorialP5RawWireResponse response) {
        ArrayList<String> issues = new ArrayList<>();
        if (response == null) return List.of("RAW_WIRE_RESPONSE_MISSING");
        if (!SCHEMA_VERSION.equals(response.wireSchemaVersion())) issues.add("RAW_WIRE_SCHEMA_INVALID");
        if (!hash(response.attemptIdentity())) issues.add("RAW_WIRE_ATTEMPT_IDENTITY_INVALID");
        if (!hash(response.requestEnvelopeHash())) issues.add("RAW_WIRE_REQUEST_HASH_INVALID");
        if (response.evidenceRefs().size() > MAX_EVIDENCE_REFS) {
            issues.add("RAW_WIRE_EVIDENCE_LIMIT_EXCEEDED");
        }
        Set<String> declaredEvidenceRefs = new HashSet<>();
        for (String ref : response.evidenceRefs()) {
            if (!token(ref, MAX_REF_LENGTH)) issues.add("RAW_WIRE_EVIDENCE_INVALID");
            if (!declaredEvidenceRefs.add(ref)) issues.add("RAW_WIRE_DUPLICATE_EVIDENCE_REF");
        }
        if (response.findings().size() > MAX_FINDINGS) issues.add("RAW_WIRE_FINDINGS_LIMIT_EXCEEDED");
        Set<String> findingIds = new HashSet<>();
        for (EditorialLedgerValidator.Entry entry : response.findings()) {
            if (!token(entry.itemId(), MAX_ID_LENGTH)) issues.add("RAW_WIRE_ITEM_ID_INVALID");
            if (!findingIds.add(entry.itemId())) issues.add("RAW_WIRE_DUPLICATE_FINDING_ID");
            if (!List.of("PROCESSED", "PRESERVE_DRAFT", "NOT_EVALUATED").contains(entry.disposition())) {
                issues.add("RAW_WIRE_FINDING_DISPOSITION_INVALID");
            }
            if (entry.evidenceRefs().size() > MAX_ENTRY_EVIDENCE_REFS) {
                issues.add("RAW_WIRE_ENTRY_EVIDENCE_LIMIT_EXCEEDED");
            }
            Set<String> entryEvidenceRefs = new HashSet<>();
            for (String ref : entry.evidenceRefs()) {
                if (!token(ref, MAX_REF_LENGTH)) issues.add("RAW_WIRE_ENTRY_EVIDENCE_INVALID");
                if (!entryEvidenceRefs.add(ref)) issues.add("RAW_WIRE_DUPLICATE_EVIDENCE_REF");
                if (!declaredEvidenceRefs.contains(ref)) {
                    issues.add("RAW_WIRE_EVIDENCE_REF_NOT_DECLARED");
                }
            }
        }
        if (!exactGates(response.gateObservations())) issues.add("RAW_WIRE_GATES_INVALID");
        if (response.preservedInventory().size() > MAX_PRESERVED_ITEMS) {
            issues.add("RAW_WIRE_PRESERVED_LIMIT_EXCEEDED");
        }
        for (String item : response.preservedInventory()) {
            if (!token(item, MAX_ID_LENGTH)) issues.add("RAW_WIRE_PRESERVED_ITEM_INVALID");
        }
        if (response.declaredChangesCount() != 0) issues.add("RAW_DECLARED_CHANGES_FORBIDDEN");
        EditorialStopDecision.Decision disposition = response.disposition();
        if (disposition == null) {
            issues.add("RAW_WIRE_DISPOSITION_MISSING");
            return List.copyOf(issues);
        }
        if (!FINAL_PHASE.equals(disposition.phase())) issues.add("RAW_WIRE_DISPOSITION_PHASE_INVALID");
        if (!safeText(disposition.reasonCode()) || !safeText(disposition.affectedScope())
                || !safeText(disposition.recoveryAction()) || !safeText(disposition.resumeFrom())) {
            issues.add("RAW_WIRE_DISPOSITION_TEXT_INVALID");
        }
        if (!gateOrNone(disposition.blockingGate())) issues.add("RAW_WIRE_BLOCKING_GATE_INVALID");
        if (disposition.evidenceRefs().size() > MAX_DISPOSITION_EVIDENCE_REFS) {
            issues.add("RAW_WIRE_DISPOSITION_EVIDENCE_LIMIT_EXCEEDED");
        }
        Set<String> dispositionEvidenceRefs = new HashSet<>();
        for (String ref : disposition.evidenceRefs()) {
            if (!token(ref, MAX_REF_LENGTH)) issues.add("RAW_WIRE_DISPOSITION_EVIDENCE_INVALID");
            if (!dispositionEvidenceRefs.add(ref)) issues.add("RAW_WIRE_DUPLICATE_EVIDENCE_REF");
            if (!declaredEvidenceRefs.contains(ref)) {
                issues.add("RAW_WIRE_EVIDENCE_REF_NOT_DECLARED");
            }
        }
        if (response.wireBytes().length > MAX_WIRE_BYTES) issues.add("RAW_WIRE_BYTE_LIMIT_EXCEEDED");
        return List.copyOf(issues);
    }

    static boolean hash(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    public static boolean token(String value, int maxLength) {
        return value != null && value.length() >= 1 && value.length() <= maxLength
                && value.matches(tokenPattern(maxLength));
    }

    public static boolean safeText(String value) {
        return value != null && value.length() >= 1 && value.length() <= MAX_DISPOSITION_TEXT_LENGTH
                && value.matches("[A-Za-z0-9][A-Za-z0-9 ._:/;()/-]{0,"
                + (MAX_DISPOSITION_TEXT_LENGTH - 1) + "}");
    }

    public static boolean gateOrNone(String value) {
        return "NONE".equals(value) || EditorialSafe4Contract.GATE_IDS.contains(value);
    }

    private static boolean exactGates(Map<String, String> gates) {
        if (gates == null || gates.size() != EditorialSafe4Contract.GATE_IDS.size()
                || !gates.keySet().equals(new java.util.HashSet<>(EditorialSafe4Contract.GATE_IDS))) {
            return false;
        }
        return gates.values().stream().allMatch(value -> "PASS".equals(value)
                || "NOT_APPLICABLE".equals(value));
    }

    private static String tokenPattern(int maxLength) {
        return "[A-Za-z0-9][A-Za-z0-9._:/-]{0," + (maxLength - 1) + "}";
    }

    private static Map<String, Object> findingsSchema() {
        LinkedHashMap<String, Object> item = object();
        item.put("type", "object");
        item.put("additionalProperties", false);
        item.put("properties", Map.of(
                "itemId", stringSchema(MAX_ID_LENGTH, tokenPattern(MAX_ID_LENGTH)),
                "disposition", Map.of("type", "string",
                        "enum", List.of("PROCESSED", "PRESERVE_DRAFT", "NOT_EVALUATED")),
                "evidenceRefs", arraySchema(MAX_ENTRY_EVIDENCE_REFS,
                        stringSchema(MAX_REF_LENGTH, tokenPattern(MAX_REF_LENGTH))),
                "modelDeclaredPass", Map.of("type", "boolean")));
        item.put("required", List.of("itemId", "disposition", "evidenceRefs", "modelDeclaredPass"));
        return arraySchema(MAX_FINDINGS, item);
    }

    private static Map<String, Object> gatesSchema() {
        LinkedHashMap<String, Object> schema = object();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        LinkedHashMap<String, Object> properties = object();
        for (String gate : EditorialSafe4Contract.GATE_IDS) {
            properties.put(gate, Map.of("type", "string", "enum", List.of("PASS", "NOT_APPLICABLE")));
        }
        schema.put("properties", properties);
        schema.put("required", EditorialSafe4Contract.GATE_IDS);
        return schema;
    }

    private static Map<String, Object> dispositionSchema() {
        LinkedHashMap<String, Object> schema = object();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", Map.of(
                "disposition", Map.of("type", "string", "enum", List.of("CONTINUE", "PRESERVE_DRAFT", "STOP")),
                "reasonCode", stringSchema(MAX_DISPOSITION_TEXT_LENGTH, safeTextPattern()),
                // enum is the portable strict-schema form of this one-value
                // phase constraint across OpenAI-compatible providers.
                "phase", Map.of("type", "string", "enum", List.of(FINAL_PHASE)),
                "blockingGate", Map.of("type", "string", "enum", gateValues()),
                "evidenceRefs", arraySchema(MAX_DISPOSITION_EVIDENCE_REFS,
                        stringSchema(MAX_REF_LENGTH, tokenPattern(MAX_REF_LENGTH))),
                "affectedScope", stringSchema(MAX_DISPOSITION_TEXT_LENGTH, safeTextPattern()),
                "recoveryAction", stringSchema(MAX_DISPOSITION_TEXT_LENGTH, safeTextPattern()),
                "resumeFrom", stringSchema(MAX_DISPOSITION_TEXT_LENGTH, safeTextPattern()),
                "stopClass", Map.of("type", "string", "enum", List.of(
                        "NONE", "INPUT_REQUIRED", "REPAIR_REQUIRED", "RETRY_REQUIRED", "CONTENT_BLOCKED")),
                "retryable", Map.of("type", "boolean")));
        schema.put("required", List.of("disposition", "reasonCode", "phase", "blockingGate",
                "evidenceRefs", "affectedScope", "recoveryAction", "resumeFrom", "stopClass", "retryable"));
        return schema;
    }

    private static List<String> gateValues() {
        ArrayList<String> values = new ArrayList<>(EditorialSafe4Contract.GATE_IDS);
        values.add("NONE");
        return List.copyOf(values);
    }

    private static String safeTextPattern() {
        return "[A-Za-z0-9][A-Za-z0-9 ._:/;()/-]{0," + (MAX_DISPOSITION_TEXT_LENGTH - 1) + "}";
    }

    private static Map<String, Object> stringSchema(int maxLength, String pattern) {
        LinkedHashMap<String, Object> value = object();
        value.put("type", "string");
        value.put("minLength", BigDecimal.ONE);
        value.put("maxLength", BigDecimal.valueOf(maxLength));
        if (pattern != null) value.put("pattern", pattern);
        return value;
    }

    private static Map<String, Object> arraySchema(int maxItems, Object items) {
        LinkedHashMap<String, Object> value = object();
        value.put("type", "array");
        value.put("maxItems", BigDecimal.valueOf(maxItems));
        value.put("items", items);
        return value;
    }

    private static Map<String, Object> emptyArraySchema() {
        LinkedHashMap<String, Object> value = object();
        value.put("type", "array");
        value.put("maxItems", BigDecimal.ZERO);
        // Keep the item schema concrete for providers that reject an empty
        // schema object even though maxItems makes it unreachable.
        value.put("items", Map.of("type", "string", "maxLength", BigDecimal.ZERO));
        return value;
    }

    private static LinkedHashMap<String, Object> object() { return new LinkedHashMap<>(); }
}
