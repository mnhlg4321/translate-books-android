package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EditorialPackFixtures {
    static final String CODE86_PROMPT_SHA = "0b4c02573f46a91528a63262d3e52c655a5c7e31f2abbbfb01759d38e94f8e81";
    static final String DBE214_PROMPT_SHA = "dbe214d842d98fd76afd2e700747fcc2cf3d5b6038134b38ea8f220fed3bf273";

    static Fixture valid() { return create("com.example.editorial.safe4", "5.0.4", "prompt\n", "DATA_COMPATIBLE"); }

    static Fixture phaseChanged(String compatibilityClass) {
        Map<String, byte[]> data = new HashMap<>();
        data.put("project.txt", "project\n".getBytes(StandardCharsets.UTF_8));
        data.put("prompt.txt", "prompt\n".getBytes(StandardCharsets.UTF_8));
        data.put("workflow.txt", "workflow\n".getBytes(StandardCharsets.UTF_8));
        LinkedHashMap<String, Object> root = root("com.example.editorial.safe4", "5.0.5", compatibilityClass, data);
        root.put("phaseGraph", mapOf("profile", "changed-barrier-v2", "initialPhase", "L1", "terminalPhase", "RELEASED",
                "phases", List.of("L1", "RELEASED"), "edges", List.of(List.of("L1", "RELEASED"))));
        root.put("canonicalPackHash", canonicalHashWithout(root));
        byte[] manifestBytes = EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8);
        return new Fixture(manifestBytes, data, EditorialPackManifest.parse(manifestBytes));
    }

    static Fixture create(String packId, String version, String prompt, String compatibilityClass) {
        return create(packId, version, prompt.getBytes(StandardCharsets.UTF_8), compatibilityClass);
    }

    static Fixture create(String packId, String version, byte[] prompt, String compatibilityClass) {
        Map<String, byte[]> data = new HashMap<>();
        data.put("project.txt", "project\n".getBytes(StandardCharsets.UTF_8));
        data.put("prompt.txt", prompt.clone());
        data.put("workflow.txt", "workflow\n".getBytes(StandardCharsets.UTF_8));
        LinkedHashMap<String, Object> root = root(packId, version, compatibilityClass, data);
        root.put("canonicalPackHash", canonicalHashWithout(root));
        byte[] manifestBytes = EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8);
        return new Fixture(manifestBytes, data, EditorialPackManifest.parse(manifestBytes));
    }

    static LinkedHashMap<String, Object> root(String packId, String version, String compatibilityClass, Map<String, byte[]> data) {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("manifestFormat", "com.ml.tblandroidtxt.editorial-pack");
        root.put("manifestVersion", BigDecimal.ONE);
        root.put("packId", packId);
        root.put("version", version);
        root.put("displayName", "Test Editorial Pack");
        root.put("contractVersion", "contract.test.v1");
        root.put("schemaVersion", "evidence.test.v1");
        root.put("minimumEngineVersion", "1.0.0");
        root.put("compatibilityClass", compatibilityClass);
        root.put("adapterId", "adapter.test.v1");
        root.put("requiredCapabilities", List.of("context.test.v1", "ledger.test.v1"));
        List<Object> files = new ArrayList<>();
        files.add(file("PROJECT_INSTRUCTION", "project.txt", data.get("project.txt")));
        files.add(file("TURN_PROMPT", "prompt.txt", data.get("prompt.txt")));
        files.add(file("WORKFLOW", "workflow.txt", data.get("workflow.txt")));
        root.put("fileRoles", files);
        root.put("canonicalPackHash", "0000000000000000000000000000000000000000000000000000000000000000");
        root.put("inputRoles", List.of(
                mapOf("role", "RAW", "cardinality", "ONE", "required", true),
                mapOf("role", "DRAFT", "cardinality", "ONE", "required", true),
                mapOf("role", "GLOSSARY", "cardinality", "ONE", "required", true),
                mapOf("role", "PRONOUN", "cardinality", "ZERO_OR_ONE", "required", false, "requiredWhen", "pronounStatus == AVAILABLE")));
        root.put("pronounPolicy", mapOf("allowedStatuses", List.of("AVAILABLE", "NONE", "LEGACY_REJECTED"), "defaultStatus", "NONE",
                "availableRequiresRole", "PRONOUN", "noneForbidsRole", true, "legacyRejectedForbidsRole", true, "fallbackLookupAllowed", false));
        root.put("pairContextPolicy", mapOf("optional", true, "sameProjectRequired", true, "samePackHashRequired", true));
        root.put("phaseGraph", mapOf("profile", "test-linear-v1", "initialPhase", "L1", "terminalPhase", "RELEASED",
                "phases", List.of("L1", "RELEASED"), "edges", List.of(List.of("L1", "RELEASED"))));
        root.put("contextAllowList", mapOf("L1", mapOf("required", List.of("RAW"), "allowed", List.of("RAW"))));
        root.put("evidenceSchemas", List.of(mapOf("evidenceType", "LEDGER", "schemaId", "ledger.test.v1")));
        root.put("gateDefinitions", List.of(mapOf("gateId", "COVERAGE", "calculatorId", "coverage.test.v1", "requires", List.of("LEDGER"))));
        root.put("releaseArtifacts", mapOf("maximumFiles", BigDecimal.valueOf(2), "artifacts", List.of(mapOf("role", "FINAL", "required", true))));
        root.put("goldenReplayCases", List.of(mapOf("caseId", "G1", "fixtureId", "test/g1", "expectedCode", "OK")));
        root.put("createdAt", "2026-08-03T00:00:00Z");
        root.put("migrationPolicy", mapOf("automaticProjectUpgrade", false, "projectRebindAllowed", false));
        return root;
    }

    static String canonicalHashWithout(Map<String, Object> root) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>(root);
        copy.remove("canonicalPackHash");
        byte[] canonical = EditorialCanonicalJson.canonicalize(copy).getBytes(StandardCharsets.UTF_8);
        byte[] prefix = "EDITORIAL_PACK_CANONICAL_HASH_V1\n".getBytes(StandardCharsets.UTF_8);
        byte[] payload = Arrays.copyOf(prefix, prefix.length + canonical.length);
        System.arraycopy(canonical, 0, payload, prefix.length, canonical.length);
        return EditorialCanonicalJson.sha256Hex(payload);
    }

    static Map<String, Object> file(String role, String path, byte[] bytes) {
        return mapOf("role", role, "path", path, "mediaType", "text/plain", "charset", "UTF-8", "bom", "FORBIDDEN",
                "byteLength", BigDecimal.valueOf(bytes.length), "sha256", EditorialCanonicalJson.sha256Hex(bytes));
    }

    static Map<String, Object> mapOf(Object... values) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put((String) values[i], values[i + 1]);
        return map;
    }

    record Fixture(byte[] manifestBytes, Map<String, byte[]> dataFiles, EditorialPackManifest manifest) {
        Fixture {
            manifestBytes = manifestBytes.clone();
            dataFiles = new HashMap<>(dataFiles);
        }
    }
}
