package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Strict, bounded JSON-to-model parser for trusted engine contract profiles. */
public final class EditorialEngineContractProfileParser {
    public static final int MAX_PROFILE_BYTES = 256 * 1024;
    public static final int MAX_COLLECTION_SIZE = 256;
    public static final int MAX_PHASE_COUNT = 128;
    public static final int MAX_CONTEXT_COUNT = 128;
    public static final int MAX_STRING_LENGTH = 1024;

    public enum Code {
        INPUT_NULL,
        SIZE_LIMIT,
        PARSE_ERROR,
        UNKNOWN_FIELD,
        MISSING_FIELD,
        WRONG_TYPE,
        COLLECTION_LIMIT,
        STRING_LIMIT
    }

    public static final class ParseException extends IllegalArgumentException {
        private final Code code;

        ParseException(Code code, String message) {
            super(message);
            this.code = code;
        }

        public Code code() { return code; }
    }

    private static final Set<String> ROOT_FIELDS = Set.of(
            "profileFormat", "profileFormatVersion", "engineProfileId", "engineProfileVersion",
            "engineVersion", "minimumSupportedContractVersion", "maximumSupportedContractVersion",
            "supportedSchemaVersions", "supportedInputRoles", "supportedPhaseGraph",
            "contextAllowListByPhase", "evidenceSchemaFingerprints", "gateDefinitionFingerprints",
            "releaseArtifactFingerprints", "implementedCapabilities", "explicitlyMissingCapabilities",
            "capabilityEvidence", "bundledAdapterIds", "adapterDescriptors",
            "machineContractFingerprint", "canonicalProfileHash", "createdAt", "buildSourceCommit",
            "deprecationPolicy");

    private static final Set<String> INPUT_ROLE_FIELDS = Set.of("role", "cardinality", "required");
    private static final Set<String> PHASE_GRAPH_FIELDS = Set.of("phases", "edges");
    private static final Set<String> PHASE_EDGE_FIELDS = Set.of("from", "to");
    private static final Set<String> CONTEXT_FIELDS = Set.of("requiredRoles", "allowedRoles");
    private static final Set<String> FINGERPRINT_FIELDS = Set.of("id", "fingerprint");
    private static final Set<String> EVIDENCE_FIELDS = Set.of(
            "capabilityId", "sourceCommit", "evidenceFingerprint", "evidenceClass");
    private static final Set<String> ADAPTER_FIELDS = Set.of(
            "adapterId", "adapterVersion", "sourceContractVersion", "sourceSchemaVersion",
            "sourceMachineContractFingerprint", "requiredCapabilities");
    private static final Set<String> DEPRECATION_FIELDS = Set.of(
            "state", "replacementProfileId", "replacementVersion", "automaticReplacement",
            "automaticProjectRebind");

    public EditorialEngineContractProfile parse(byte[] profileBytes) {
        if (profileBytes == null) throw new ParseException(Code.INPUT_NULL, "Profile bytes are null");
        if (profileBytes.length > MAX_PROFILE_BYTES) {
            throw new ParseException(Code.SIZE_LIMIT, "Profile exceeds maximum byte length");
        }
        final Map<String, Object> root;
        try {
            root = EditorialCanonicalJson.parseObject(profileBytes);
        } catch (IllegalArgumentException e) {
            throw new ParseException(Code.PARSE_ERROR, "Profile JSON is invalid");
        }
        requireExactFields(root, ROOT_FIELDS, "profile");
        return new EditorialEngineContractProfile(
                string(root, "profileFormat"),
                intValue(root, "profileFormatVersion"),
                string(root, "engineProfileId"),
                string(root, "engineProfileVersion"),
                string(root, "engineVersion"),
                nullableString(root, "minimumSupportedContractVersion"),
                nullableString(root, "maximumSupportedContractVersion"),
                stringList(root.get("supportedSchemaVersions"), "supportedSchemaVersions"),
                inputRoles(root.get("supportedInputRoles")),
                phaseGraph(root.get("supportedPhaseGraph")),
                contextAllowLists(root.get("contextAllowListByPhase")),
                fingerprintList(root.get("evidenceSchemaFingerprints"), "evidenceSchemaFingerprints"),
                fingerprintList(root.get("gateDefinitionFingerprints"), "gateDefinitionFingerprints"),
                fingerprintList(root.get("releaseArtifactFingerprints"), "releaseArtifactFingerprints"),
                stringList(root.get("implementedCapabilities"), "implementedCapabilities"),
                stringList(root.get("explicitlyMissingCapabilities"), "explicitlyMissingCapabilities"),
                capabilityEvidence(root.get("capabilityEvidence")),
                stringList(root.get("bundledAdapterIds"), "bundledAdapterIds"),
                adapters(root.get("adapterDescriptors")),
                string(root, "machineContractFingerprint"),
                string(root, "canonicalProfileHash"),
                string(root, "createdAt"),
                string(root, "buildSourceCommit"),
                deprecationPolicy(root.get("deprecationPolicy")));
    }

    private static void requireExactFields(Map<String, Object> object, Set<String> expected, String path) {
        for (String field : expected) {
            if (!object.containsKey(field)) throw new ParseException(Code.MISSING_FIELD, path + " is missing required field");
        }
        for (String field : object.keySet()) {
            if (!expected.contains(field)) throw new ParseException(Code.UNKNOWN_FIELD, path + " contains an unknown field");
        }
    }

    private static String string(Map<String, Object> object, String key) {
        return stringValue(object.get(key), key);
    }

    private static String stringValue(Object value, String path) {
        if (!(value instanceof String)) throw new ParseException(Code.WRONG_TYPE, path + " must be a string");
        String string = (String) value;
        if (string.length() > MAX_STRING_LENGTH) throw new ParseException(Code.STRING_LIMIT, path + " exceeds maximum length");
        return string;
    }

    private static String nullableString(Map<String, Object> object, String key) {
        Object value = object.get(key);
        return value == null ? null : stringValue(value, key);
    }

    private static int intValue(Map<String, Object> object, String key) {
        try {
            long value = EditorialCanonicalJson.integer(object.get(key), key);
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw new ArithmeticException();
            return (int) value;
        } catch (RuntimeException e) {
            throw new ParseException(Code.WRONG_TYPE, key + " must be a bounded integer");
        }
    }

    private static boolean booleanValue(Object value, String path) {
        if (!(value instanceof Boolean)) throw new ParseException(Code.WRONG_TYPE, path + " must be boolean");
        return (Boolean) value;
    }

    private static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map)) throw new ParseException(Code.WRONG_TYPE, path + " must be an object");
        @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) value;
        return map;
    }

    private static List<Object> array(Object value, String path, int maximum) {
        if (!(value instanceof List)) throw new ParseException(Code.WRONG_TYPE, path + " must be an array");
        @SuppressWarnings("unchecked") List<Object> list = (List<Object>) value;
        if (list.size() > maximum) throw new ParseException(Code.COLLECTION_LIMIT, path + " exceeds maximum item count");
        return list;
    }

    private static List<String> stringList(Object value, String path) {
        List<Object> raw = array(value, path, MAX_COLLECTION_SIZE);
        ArrayList<String> result = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) result.add(stringValue(raw.get(i), path + "[" + i + "]"));
        return result;
    }

    private static List<EditorialEngineContractProfile.InputRole> inputRoles(Object value) {
        List<Object> raw = array(value, "supportedInputRoles", MAX_COLLECTION_SIZE);
        ArrayList<EditorialEngineContractProfile.InputRole> result = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            String path = "supportedInputRoles[" + i + "]";
            Map<String, Object> item = object(raw.get(i), path);
            requireExactFields(item, INPUT_ROLE_FIELDS, path);
            result.add(new EditorialEngineContractProfile.InputRole(
                    string(item, "role"), string(item, "cardinality"), booleanValue(item.get("required"), path + ".required")));
        }
        return result;
    }

    private static EditorialEngineContractProfile.PhaseGraph phaseGraph(Object value) {
        Map<String, Object> graph = object(value, "supportedPhaseGraph");
        requireExactFields(graph, PHASE_GRAPH_FIELDS, "supportedPhaseGraph");
        List<String> phases = stringList(graph.get("phases"), "supportedPhaseGraph.phases");
        if (phases.size() > MAX_PHASE_COUNT) throw new ParseException(Code.COLLECTION_LIMIT, "supportedPhaseGraph.phases exceeds maximum");
        List<Object> rawEdges = array(graph.get("edges"), "supportedPhaseGraph.edges", MAX_COLLECTION_SIZE);
        ArrayList<EditorialEngineContractProfile.PhaseEdge> edges = new ArrayList<>(rawEdges.size());
        for (int i = 0; i < rawEdges.size(); i++) {
            String path = "supportedPhaseGraph.edges[" + i + "]";
            Map<String, Object> edge = object(rawEdges.get(i), path);
            requireExactFields(edge, PHASE_EDGE_FIELDS, path);
            edges.add(new EditorialEngineContractProfile.PhaseEdge(string(edge, "from"), string(edge, "to")));
        }
        return new EditorialEngineContractProfile.PhaseGraph(phases, edges);
    }

    private static Map<String, EditorialEngineContractProfile.ContextAllowList> contextAllowLists(Object value) {
        Map<String, Object> raw = object(value, "contextAllowListByPhase");
        if (raw.size() > MAX_CONTEXT_COUNT) throw new ParseException(Code.COLLECTION_LIMIT, "contextAllowListByPhase exceeds maximum");
        LinkedHashMap<String, EditorialEngineContractProfile.ContextAllowList> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String path = "contextAllowListByPhase." + entry.getKey();
            Map<String, Object> item = object(entry.getValue(), path);
            requireExactFields(item, CONTEXT_FIELDS, path);
            result.put(entry.getKey(), new EditorialEngineContractProfile.ContextAllowList(
                    stringList(item.get("requiredRoles"), path + ".requiredRoles"),
                    stringList(item.get("allowedRoles"), path + ".allowedRoles")));
        }
        return result;
    }

    private static List<EditorialEngineContractProfile.FingerprintDescriptor> fingerprintList(Object value, String path) {
        List<Object> raw = array(value, path, MAX_COLLECTION_SIZE);
        ArrayList<EditorialEngineContractProfile.FingerprintDescriptor> result = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            String itemPath = path + "[" + i + "]";
            Map<String, Object> item = object(raw.get(i), itemPath);
            requireExactFields(item, FINGERPRINT_FIELDS, itemPath);
            result.add(new EditorialEngineContractProfile.FingerprintDescriptor(
                    string(item, "id"), string(item, "fingerprint")));
        }
        return result;
    }

    private static List<EditorialEngineContractProfile.CapabilityEvidence> capabilityEvidence(Object value) {
        List<Object> raw = array(value, "capabilityEvidence", MAX_COLLECTION_SIZE);
        ArrayList<EditorialEngineContractProfile.CapabilityEvidence> result = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            String path = "capabilityEvidence[" + i + "]";
            Map<String, Object> item = object(raw.get(i), path);
            requireExactFields(item, EVIDENCE_FIELDS, path);
            result.add(new EditorialEngineContractProfile.CapabilityEvidence(
                    string(item, "capabilityId"), string(item, "sourceCommit"),
                    string(item, "evidenceFingerprint"), string(item, "evidenceClass")));
        }
        return result;
    }

    private static List<EditorialEngineContractProfile.AdapterDescriptor> adapters(Object value) {
        List<Object> raw = array(value, "adapterDescriptors", MAX_COLLECTION_SIZE);
        ArrayList<EditorialEngineContractProfile.AdapterDescriptor> result = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            String path = "adapterDescriptors[" + i + "]";
            Map<String, Object> item = object(raw.get(i), path);
            requireExactFields(item, ADAPTER_FIELDS, path);
            result.add(new EditorialEngineContractProfile.AdapterDescriptor(
                    string(item, "adapterId"), string(item, "adapterVersion"),
                    string(item, "sourceContractVersion"), string(item, "sourceSchemaVersion"),
                    string(item, "sourceMachineContractFingerprint"),
                    stringList(item.get("requiredCapabilities"), path + ".requiredCapabilities")));
        }
        return result;
    }

    private static EditorialEngineContractProfile.DeprecationPolicy deprecationPolicy(Object value) {
        Map<String, Object> item = object(value, "deprecationPolicy");
        requireExactFields(item, DEPRECATION_FIELDS, "deprecationPolicy");
        return new EditorialEngineContractProfile.DeprecationPolicy(
                string(item, "state"), nullableString(item, "replacementProfileId"),
                nullableString(item, "replacementVersion"),
                booleanValue(item.get("automaticReplacement"), "deprecationPolicy.automaticReplacement"),
                booleanValue(item.get("automaticProjectRebind"), "deprecationPolicy.automaticProjectRebind"));
    }
}
