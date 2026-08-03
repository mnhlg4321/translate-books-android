package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable, parsed manifest. It contains declarations only and no executable hooks. */
public final class EditorialPackManifest {
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Set<String> ROOT_FIELDS = Set.of(
            "manifestFormat", "manifestVersion", "packId", "version", "displayName",
            "contractVersion", "schemaVersion", "minimumEngineVersion", "compatibilityClass",
            "adapterId", "requiredCapabilities", "fileRoles", "canonicalPackHash", "inputRoles",
            "pronounPolicy", "pairContextPolicy", "phaseGraph", "contextAllowList",
            "evidenceSchemas", "gateDefinitions", "releaseArtifacts", "goldenReplayCases",
            "createdAt", "migrationPolicy");

    private final Map<String, Object> root;
    private final List<FileEntry> fileEntries;
    private final Set<String> requiredCapabilities;
    private final String machineContractFingerprint;

    private EditorialPackManifest(Map<String, Object> root) {
        this.root = EditorialCanonicalJson.freezeObject(root);
        this.fileEntries = parseFileEntries(this.root.get("fileRoles"));
        this.requiredCapabilities = parseStringSet(this.root.get("requiredCapabilities"), "requiredCapabilities", true);
        this.machineContractFingerprint = EditorialCanonicalJson.sha256Hex(
                EditorialCanonicalJson.canonicalize(machineContractObject(this.root))
                        .getBytes(StandardCharsets.UTF_8));
    }

    public static EditorialPackManifest parse(byte[] manifestBytes) {
        if (manifestBytes == null) throw new IllegalArgumentException("manifest bytes are null");
        Map<String, Object> root;
        try {
            root = EditorialCanonicalJson.parseObject(manifestBytes);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid manifest JSON: " + e.getMessage(), e);
        }
        validateRoot(root);
        return new EditorialPackManifest(root);
    }

    public String manifestFormat() { return string("manifestFormat"); }
    public int manifestVersion() { return integer("manifestVersion"); }
    public String packId() { return string("packId"); }
    public String version() { return string("version"); }
    public String displayName() { return string("displayName"); }
    public String contractVersion() { return string("contractVersion"); }
    public String schemaVersion() { return string("schemaVersion"); }
    public String minimumEngineVersion() { return string("minimumEngineVersion"); }
    public EditorialPackCompatibilityClass declaredCompatibilityClass() {
        return EditorialPackCompatibilityClass.valueOf(string("compatibilityClass"));
    }
    public String adapterId() { return optionalString("adapterId"); }
    public String canonicalPackHash() { return string("canonicalPackHash"); }
    public Set<String> requiredCapabilities() { return requiredCapabilities; }
    public List<FileEntry> fileEntries() { return fileEntries; }
    public EditorialPronounPolicy pronounPolicy() {
        Object policy = root.get("pronounPolicy");
        String status;
        if (policy instanceof String) status = (String) policy;
        else status = EditorialCanonicalJson.string(EditorialCanonicalJson.object(policy, "pronounPolicy").get("defaultStatus"), "pronounPolicy.defaultStatus");
        return EditorialPronounPolicy.valueOf(status);
    }
    public String machineContractFingerprint() { return machineContractFingerprint; }
    public String canonicalJson() { return EditorialCanonicalJson.canonicalize(root); }
    public String canonicalJsonWithoutPackHash() {
        LinkedHashMap<String, Object> without = new LinkedHashMap<>(root);
        without.remove("canonicalPackHash");
        return EditorialCanonicalJson.canonicalize(without);
    }
    public String calculatedCanonicalPackHash() {
        byte[] canonical = canonicalJsonWithoutPackHash().getBytes(StandardCharsets.UTF_8);
        byte[] domain = "EDITORIAL_PACK_CANONICAL_HASH_V1\n".getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[domain.length + canonical.length];
        System.arraycopy(domain, 0, payload, 0, domain.length);
        System.arraycopy(canonical, 0, payload, domain.length, canonical.length);
        return EditorialCanonicalJson.sha256Hex(payload);
    }

    /** Deeply immutable canonical declarations for contract comparison only. */
    public Map<String, Object> declarations() { return root; }

    private static Map<String, Object> machineContractObject(Map<String, Object> root) {
        LinkedHashMap<String, Object> selected = new LinkedHashMap<>();
        for (String key : List.of("contractVersion", "schemaVersion", "requiredCapabilities", "inputRoles",
                "pronounPolicy", "pairContextPolicy", "phaseGraph", "contextAllowList", "evidenceSchemas",
                "gateDefinitions", "releaseArtifacts")) {
            selected.put(key, root.get(key));
        }
        return selected;
    }

    private static List<FileEntry> parseFileEntries(Object value) {
        List<Object> array = EditorialCanonicalJson.array(value, "fileRoles");
        ArrayList<FileEntry> result = new ArrayList<>();
        Set<EditorialPackFileRole> roles = new HashSet<>();
        Set<String> paths = new HashSet<>();
        for (int i = 0; i < array.size(); i++) {
            Map<String, Object> file = EditorialCanonicalJson.object(array.get(i), "fileRoles[" + i + "]");
            Set<String> allowed = Set.of("role", "path", "mediaType", "charset", "bom", "byteLength", "sha256");
            rejectUnknown(file, allowed, "fileRoles[" + i + "]");
            require(file, allowed, "role", "fileRoles[" + i + "]");
            require(file, allowed, "path", "fileRoles[" + i + "]");
            require(file, allowed, "mediaType", "fileRoles[" + i + "]");
            require(file, allowed, "charset", "fileRoles[" + i + "]");
            require(file, allowed, "bom", "fileRoles[" + i + "]");
            require(file, allowed, "byteLength", "fileRoles[" + i + "]");
            require(file, allowed, "sha256", "fileRoles[" + i + "]");
            EditorialPackFileRole role;
            try { role = EditorialPackFileRole.valueOf(EditorialCanonicalJson.string(file.get("role"), "fileRoles.role")); }
            catch (RuntimeException e) { throw new IllegalArgumentException("Invalid file role", e); }
            String path = EditorialCanonicalJson.string(file.get("path"), "fileRoles.path");
            if (!roles.add(role)) throw new IllegalArgumentException("Duplicate file role: " + role);
            if (!paths.add(path)) throw new IllegalArgumentException("Duplicate file path: " + path);
            if (!isRootFilePath(path)) throw new IllegalArgumentException("Invalid file path: " + path);
            String mediaType = EditorialCanonicalJson.string(file.get("mediaType"), "fileRoles.mediaType");
            String charset = EditorialCanonicalJson.string(file.get("charset"), "fileRoles.charset");
            String bom = EditorialCanonicalJson.string(file.get("bom"), "fileRoles.bom");
            long length = EditorialCanonicalJson.integer(file.get("byteLength"), "fileRoles.byteLength");
            String sha = EditorialCanonicalJson.string(file.get("sha256"), "fileRoles.sha256");
            if (length < 0 || !SHA256.matcher(sha).matches()) throw new IllegalArgumentException("Invalid file length/hash");
            if (!"text/plain".equals(mediaType) || !"UTF-8".equals(charset) || !"FORBIDDEN".equals(bom)) {
                throw new IllegalArgumentException("Pack files must be strict UTF-8 text/plain without BOM");
            }
            result.add(new FileEntry(role, path, mediaType, charset, bom, length, sha));
        }
        if (result.size() != 3 || roles.size() != 3 || !roles.containsAll(Set.of(EditorialPackFileRole.values()))) {
            throw new IllegalArgumentException("fileRoles must contain exactly the three required roles");
        }
        return Collections.unmodifiableList(result);
    }

    private static void validateRoot(Map<String, Object> root) {
        rejectUnknown(root, ROOT_FIELDS, "manifest");
        for (String key : ROOT_FIELDS) if (!"adapterId".equals(key) && !root.containsKey(key)) {
            throw new IllegalArgumentException("Missing manifest field: " + key);
        }
        if (!"com.ml.tblandroidtxt.editorial-pack".equals(EditorialCanonicalJson.string(root.get("manifestFormat"), "manifestFormat"))) {
            throw new IllegalArgumentException("Unsupported manifestFormat");
        }
        if (EditorialCanonicalJson.integer(root.get("manifestVersion"), "manifestVersion") != 1) throw new IllegalArgumentException("Unsupported manifestVersion");
        for (String key : List.of("packId", "version", "displayName", "contractVersion", "schemaVersion", "minimumEngineVersion", "createdAt")) {
            if (EditorialCanonicalJson.string(root.get(key), key).isBlank()) throw new IllegalArgumentException(key + " is blank");
        }
        try { EditorialPackCompatibilityClass.valueOf(EditorialCanonicalJson.string(root.get("compatibilityClass"), "compatibilityClass")); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Invalid compatibilityClass", e); }
        parseStringSet(root.get("requiredCapabilities"), "requiredCapabilities", true);
        List<Object> files = EditorialCanonicalJson.array(root.get("fileRoles"), "fileRoles");
        if (files.size() != 3) throw new IllegalArgumentException("fileRoles must contain exactly three entries");
        validateInputRoles(root.get("inputRoles"));
        validatePronounPolicy(root.get("pronounPolicy"));
        validatePairPolicy(root.get("pairContextPolicy"));
        validatePhaseGraph(root.get("phaseGraph"));
        validateContextAllowList(root.get("contextAllowList"));
        validateDescriptorArray(root.get("evidenceSchemas"), "evidenceSchemas", Set.of("evidenceType", "schemaId"), "evidenceType");
        validateGateArray(root.get("gateDefinitions"));
        validateReleaseArtifacts(root.get("releaseArtifacts"));
        validateGoldenCases(root.get("goldenReplayCases"));
        validateMigrationPolicy(root.get("migrationPolicy"));
        String hash = EditorialCanonicalJson.string(root.get("canonicalPackHash"), "canonicalPackHash");
        if (!SHA256.matcher(hash).matches()) throw new IllegalArgumentException("canonicalPackHash must be lowercase SHA-256");
    }

    private static void validateInputRoles(Object value) {
        List<Object> array = EditorialCanonicalJson.array(value, "inputRoles");
        if (array.isEmpty()) throw new IllegalArgumentException("inputRoles is empty");
        Set<String> roles = new HashSet<>();
        for (int i = 0; i < array.size(); i++) {
            Map<String, Object> role = EditorialCanonicalJson.object(array.get(i), "inputRoles[" + i + "]");
            Set<String> allowed = Set.of("role", "cardinality", "required", "requiredWhen");
            rejectUnknown(role, allowed, "inputRoles[" + i + "]");
            require(role, allowed, "role", "inputRoles[" + i + "]");
            require(role, allowed, "cardinality", "inputRoles[" + i + "]");
            require(role, allowed, "required", "inputRoles[" + i + "]");
            String name = EditorialCanonicalJson.string(role.get("role"), "inputRoles.role");
            if (!roles.add(name)) throw new IllegalArgumentException("Duplicate input role: " + name);
            String cardinality = EditorialCanonicalJson.string(role.get("cardinality"), "inputRoles.cardinality");
            if (!Set.of("ONE", "ZERO_OR_ONE", "MANY", "ZERO_OR_MORE").contains(cardinality)) throw new IllegalArgumentException("Invalid input cardinality");
            if (role.containsKey("required")) EditorialCanonicalJson.bool(role.get("required"), "inputRoles.required");
            if (role.containsKey("requiredWhen")) EditorialCanonicalJson.string(role.get("requiredWhen"), "inputRoles.requiredWhen");
        }
    }

    private static void validatePronounPolicy(Object value) {
        if (value instanceof String) {
            try { EditorialPronounPolicy.valueOf((String) value); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid pronounPolicy", e); }
            return;
        }
        Map<String, Object> policy = EditorialCanonicalJson.object(value, "pronounPolicy");
        Set<String> allowedFields = Set.of("allowedStatuses", "defaultStatus", "availableRequiresRole", "noneForbidsRole", "legacyRejectedForbidsRole", "fallbackLookupAllowed");
        rejectUnknown(policy, allowedFields, "pronounPolicy");
        for (String field : allowedFields) require(policy, allowedFields, field, "pronounPolicy");
        String status = EditorialCanonicalJson.string(policy.get("defaultStatus"), "pronounPolicy.defaultStatus");
        try { EditorialPronounPolicy.valueOf(status); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid pronoun defaultStatus", e); }
        List<Object> statuses = EditorialCanonicalJson.array(policy.get("allowedStatuses"), "pronounPolicy.allowedStatuses");
        Set<String> seen = new HashSet<>();
        for (Object item : statuses) {
            String allowed = EditorialCanonicalJson.string(item, "pronounPolicy.allowedStatuses[]");
            try { EditorialPronounPolicy.valueOf(allowed); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid pronoun status", e); }
            seen.add(allowed);
        }
        if (!seen.contains(status)) throw new IllegalArgumentException("Pronoun defaultStatus is not explicitly allowed");
        EditorialCanonicalJson.bool(policy.get("fallbackLookupAllowed"), "pronounPolicy.fallbackLookupAllowed");
    }

    private static void validatePairPolicy(Object value) {
        Map<String, Object> policy = EditorialCanonicalJson.object(value, "pairContextPolicy");
        rejectUnknown(policy, Set.of("optional", "source", "qaConfirmedRequired", "sameProjectRequired", "samePackHashRequired", "scopeRequired", "boundaryRequired"), "pairContextPolicy");
        if (!policy.containsKey("optional")) throw new IllegalArgumentException("pairContextPolicy.optional is required");
        EditorialCanonicalJson.bool(policy.get("optional"), "pairContextPolicy.optional");
    }

    private static void validatePhaseGraph(Object value) {
        Map<String, Object> graph = EditorialCanonicalJson.object(value, "phaseGraph");
        rejectUnknown(graph, Set.of("profile", "initialPhase", "terminalPhase", "phases", "edges"), "phaseGraph");
        require(graph, Set.of("profile", "initialPhase", "terminalPhase", "phases", "edges"), "profile", "phaseGraph");
        require(graph, Set.of("profile", "initialPhase", "terminalPhase", "phases", "edges"), "initialPhase", "phaseGraph");
        require(graph, Set.of("profile", "initialPhase", "terminalPhase", "phases", "edges"), "terminalPhase", "phaseGraph");
        Set<String> phases = new LinkedHashSet<>();
        for (Object item : EditorialCanonicalJson.array(graph.get("phases"), "phaseGraph.phases")) {
            String phase = EditorialCanonicalJson.string(item, "phaseGraph.phases[]");
            if (!phases.add(phase)) throw new IllegalArgumentException("Duplicate phase: " + phase);
        }
        String initial = EditorialCanonicalJson.string(graph.get("initialPhase"), "phaseGraph.initialPhase");
        String terminal = EditorialCanonicalJson.string(graph.get("terminalPhase"), "phaseGraph.terminalPhase");
        if (!phases.contains(initial) || !phases.contains(terminal) || phases.isEmpty()) throw new IllegalArgumentException("Phase endpoints are not declared");
        Set<String> edges = new HashSet<>();
        for (Object edgeValue : EditorialCanonicalJson.array(graph.get("edges"), "phaseGraph.edges")) {
            List<Object> edge = EditorialCanonicalJson.array(edgeValue, "phaseGraph.edges[]");
            if (edge.size() != 2) throw new IllegalArgumentException("Each phase edge must have two endpoints");
            String from = EditorialCanonicalJson.string(edge.get(0), "phaseGraph.edges.from");
            String to = EditorialCanonicalJson.string(edge.get(1), "phaseGraph.edges.to");
            if (!phases.contains(from) || !phases.contains(to) || !edges.add(from + "\u0000" + to)) throw new IllegalArgumentException("Invalid or duplicate phase edge");
        }
    }

    private static void validateContextAllowList(Object value) {
        Map<String, Object> contexts = EditorialCanonicalJson.object(value, "contextAllowList");
        if (contexts.isEmpty()) throw new IllegalArgumentException("contextAllowList is empty");
        for (Map.Entry<String, Object> entry : contexts.entrySet()) {
            Map<String, Object> rule = EditorialCanonicalJson.object(entry.getValue(), "contextAllowList." + entry.getKey());
            rejectUnknown(rule, Set.of("required", "allowed", "sameAs"), "contextAllowList." + entry.getKey());
            if (!rule.containsKey("sameAs")) {
                require(rule, Set.of("required", "allowed"), "required", "contextAllowList." + entry.getKey());
                require(rule, Set.of("required", "allowed"), "allowed", "contextAllowList." + entry.getKey());
                for (Object role : EditorialCanonicalJson.array(rule.get("required"), "context.required")) EditorialCanonicalJson.string(role, "context.required[]");
                for (Object role : EditorialCanonicalJson.array(rule.get("allowed"), "context.allowed")) EditorialCanonicalJson.string(role, "context.allowed[]");
            } else EditorialCanonicalJson.string(rule.get("sameAs"), "context.sameAs");
        }
    }

    private static void validateDescriptorArray(Object value, String field, Set<String> required, String uniqueField) {
        List<Object> array = EditorialCanonicalJson.array(value, field);
        if (array.isEmpty()) throw new IllegalArgumentException(field + " is empty");
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < array.size(); i++) {
            Map<String, Object> item = EditorialCanonicalJson.object(array.get(i), field + "[" + i + "]");
            rejectUnknown(item, required, field + "[" + i + "]");
            require(item, required, uniqueField, field + "[" + i + "]");
            String id = EditorialCanonicalJson.string(item.get(uniqueField), field + "." + uniqueField);
            if (!seen.add(id)) throw new IllegalArgumentException("Duplicate " + field + " " + id);
        }
    }

    private static void validateGateArray(Object value) {
        List<Object> array = EditorialCanonicalJson.array(value, "gateDefinitions");
        if (array.isEmpty()) throw new IllegalArgumentException("gateDefinitions is empty");
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < array.size(); i++) {
            Map<String, Object> gate = EditorialCanonicalJson.object(array.get(i), "gateDefinitions[" + i + "]");
            rejectUnknown(gate, Set.of("gateId", "calculatorId", "requires"), "gateDefinitions[" + i + "]");
            require(gate, Set.of("gateId", "calculatorId", "requires"), "gateId", "gateDefinitions[]");
            require(gate, Set.of("gateId", "calculatorId", "requires"), "calculatorId", "gateDefinitions[]");
            require(gate, Set.of("gateId", "calculatorId", "requires"), "requires", "gateDefinitions[]");
            String id = EditorialCanonicalJson.string(gate.get("gateId"), "gate.gateId");
            if (!seen.add(id)) throw new IllegalArgumentException("Duplicate gateId: " + id);
            EditorialCanonicalJson.string(gate.get("calculatorId"), "gate.calculatorId");
            if (EditorialCanonicalJson.array(gate.get("requires"), "gate.requires").isEmpty()) throw new IllegalArgumentException("Gate has no evidence requirements");
        }
    }

    private static void validateReleaseArtifacts(Object value) {
        Map<String, Object> release = EditorialCanonicalJson.object(value, "releaseArtifacts");
        rejectUnknown(release, Set.of("maximumFiles", "artifacts", "forbiddenArtifacts"), "releaseArtifacts");
        require(release, Set.of("maximumFiles", "artifacts"), "maximumFiles", "releaseArtifacts");
        require(release, Set.of("maximumFiles", "artifacts"), "artifacts", "releaseArtifacts");
        if (EditorialCanonicalJson.integer(release.get("maximumFiles"), "releaseArtifacts.maximumFiles") < 1) throw new IllegalArgumentException("maximumFiles must be positive");
        List<Object> artifacts = EditorialCanonicalJson.array(release.get("artifacts"), "releaseArtifacts.artifacts");
        if (artifacts.isEmpty()) throw new IllegalArgumentException("releaseArtifacts.artifacts is empty");
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < artifacts.size(); i++) {
            Map<String, Object> artifact = EditorialCanonicalJson.object(artifacts.get(i), "releaseArtifacts.artifacts[" + i + "]");
            Set<String> allowed = Set.of("role", "required", "nameTemplate", "contentContract", "presentWhen");
            rejectUnknown(artifact, allowed, "releaseArtifacts.artifacts[" + i + "]");
            require(artifact, allowed, "role", "releaseArtifacts.artifacts[" + i + "]");
            require(artifact, allowed, "required", "releaseArtifacts.artifacts[" + i + "]");
            String role = EditorialCanonicalJson.string(artifact.get("role"), "releaseArtifacts.artifacts.role");
            if (!seen.add(role)) throw new IllegalArgumentException("Duplicate release artifact role: " + role);
            EditorialCanonicalJson.bool(artifact.get("required"), "releaseArtifacts.artifacts.required");
        }
    }

    private static void validateGoldenCases(Object value) {
        List<Object> cases = EditorialCanonicalJson.array(value, "goldenReplayCases");
        if (cases.isEmpty()) throw new IllegalArgumentException("goldenReplayCases is empty");
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < cases.size(); i++) {
            Map<String, Object> item = EditorialCanonicalJson.object(cases.get(i), "goldenReplayCases[" + i + "]");
            Set<String> allowed = Set.of("caseId", "fixtureId", "expectedCode");
            rejectUnknown(item, allowed, "goldenReplayCases[" + i + "]");
            for (String field : allowed) require(item, allowed, field, "goldenReplayCases[" + i + "]");
            String id = EditorialCanonicalJson.string(item.get("caseId"), "goldenReplayCases.caseId");
            if (!seen.add(id)) throw new IllegalArgumentException("Duplicate golden case: " + id);
        }
    }

    private static void validateMigrationPolicy(Object value) {
        Map<String, Object> migration = EditorialCanonicalJson.object(value, "migrationPolicy");
        Set<String> allowed = Set.of("automaticProjectUpgrade", "projectRebindAllowed", "existingProjectAction", "newProjectPolicy", "copyToDifferentPackRequiresNewProject");
        rejectUnknown(migration, allowed, "migrationPolicy");
        require(migration, allowed, "automaticProjectUpgrade", "migrationPolicy");
        require(migration, allowed, "projectRebindAllowed", "migrationPolicy");
        if (EditorialCanonicalJson.bool(migration.get("automaticProjectUpgrade"), "migrationPolicy.automaticProjectUpgrade")) throw new IllegalArgumentException("Automatic project upgrade is forbidden");
        if (EditorialCanonicalJson.bool(migration.get("projectRebindAllowed"), "migrationPolicy.projectRebindAllowed")) throw new IllegalArgumentException("Project rebind is forbidden");
    }

    private static Set<String> parseStringSet(Object value, String path, boolean nonEmpty) {
        List<Object> array = EditorialCanonicalJson.array(value, path);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Object item : array) {
            String string = EditorialCanonicalJson.string(item, path + "[]");
            if (!result.add(string)) throw new IllegalArgumentException("Duplicate " + path + " entry: " + string);
        }
        if (nonEmpty && result.isEmpty()) throw new IllegalArgumentException(path + " is empty");
        return Collections.unmodifiableSet(result);
    }

    private String string(String key) { return EditorialCanonicalJson.string(root.get(key), key); }
    private String optionalString(String key) { return root.containsKey(key) ? EditorialCanonicalJson.string(root.get(key), key) : ""; }
    private int integer(String key) {
        long value = EditorialCanonicalJson.integer(root.get(key), key);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw new IllegalArgumentException(key + " out of range");
        return (int) value;
    }

    private static void rejectUnknown(Map<String, Object> map, Set<String> allowed, String path) {
        for (String key : map.keySet()) if (!allowed.contains(key)) throw new IllegalArgumentException("Unknown " + path + " field: " + key);
    }

    private static void require(Map<String, Object> map, Set<String> allowed, String key, String path) {
        if (!map.containsKey(key)) throw new IllegalArgumentException("Missing " + path + "." + key);
    }

    private static boolean isRootFilePath(String path) {
        return path != null && !path.isBlank() && !path.startsWith("/") && !path.startsWith("\\")
                && !path.contains("/") && !path.contains("\\") && !path.equals(".") && !path.equals("..")
                && !path.contains("..") && !path.contains(":");
    }

    public static final class FileEntry {
        private final EditorialPackFileRole role;
        private final String path;
        private final String mediaType;
        private final String charset;
        private final String bom;
        private final long byteLength;
        private final String sha256;

        private FileEntry(EditorialPackFileRole role, String path, String mediaType, String charset, String bom, long byteLength, String sha256) {
            this.role = role; this.path = path; this.mediaType = mediaType; this.charset = charset; this.bom = bom; this.byteLength = byteLength; this.sha256 = sha256;
        }
        public EditorialPackFileRole role() { return role; }
        public String path() { return path; }
        public String mediaType() { return mediaType; }
        public String charset() { return charset; }
        public String bom() { return bom; }
        public long byteLength() { return byteLength; }
        public String sha256() { return sha256; }
    }
}
