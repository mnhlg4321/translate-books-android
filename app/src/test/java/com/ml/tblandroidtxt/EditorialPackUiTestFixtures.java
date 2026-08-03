package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackRegistryMetadata;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EditorialPackUiTestFixtures {
    static EditorialPackManifest pack(String packId, String version, EditorialPackCompatibilityClass classification,
                                      EditorialPackRegistryMetadata.StorageState state, Set<String> missing,
                                      String blockedReason, EditorialPackRegistryMetadata.IntegrityState integrity) {
        Map<String, byte[]> files = Map.of("project.txt", bytes("project\n"), "prompt.txt", bytes("prompt\n"), "workflow.txt", bytes("workflow\n"));
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("manifestFormat", "com.ml.tblandroidtxt.editorial-pack"); root.put("manifestVersion", BigDecimal.ONE);
        root.put("packId", packId); root.put("version", version); root.put("displayName", "UI " + packId + " " + version);
        root.put("contractVersion", "contract.test.v1"); root.put("schemaVersion", "schema.test.v1"); root.put("minimumEngineVersion", "1.0.0");
        root.put("compatibilityClass", classification.name()); root.put("adapterId", "adapter.test");
        root.put("requiredCapabilities", List.of("cap.required"));
        root.put("fileRoles", List.of(file("PROJECT_INSTRUCTION", "project.txt", files.get("project.txt")), file("TURN_PROMPT", "prompt.txt", files.get("prompt.txt")), file("WORKFLOW", "workflow.txt", files.get("workflow.txt"))));
        root.put("canonicalPackHash", "0".repeat(64));
        root.put("inputRoles", List.of(map("role", "RAW", "cardinality", "ONE", "required", true)));
        root.put("pronounPolicy", map("allowedStatuses", List.of("NONE"), "defaultStatus", "NONE", "availableRequiresRole", "PRONOUN", "noneForbidsRole", true, "legacyRejectedForbidsRole", true, "fallbackLookupAllowed", false));
        root.put("pairContextPolicy", map("optional", true, "sameProjectRequired", true));
        root.put("phaseGraph", map("profile", "test", "initialPhase", "L1", "terminalPhase", "DONE", "phases", List.of("L1", "DONE"), "edges", List.of(List.of("L1", "DONE"))));
        root.put("contextAllowList", map("L1", map("required", List.of("RAW"), "allowed", List.of("RAW"))));
        root.put("evidenceSchemas", List.of(map("evidenceType", "LEDGER", "schemaId", "ledger.test")));
        root.put("gateDefinitions", List.of(map("gateId", "GATE", "calculatorId", "gate.test", "requires", List.of("LEDGER"))));
        root.put("releaseArtifacts", map("maximumFiles", BigDecimal.ONE, "artifacts", List.of(map("role", "FINAL", "required", true))));
        root.put("goldenReplayCases", List.of(map("caseId", "G1", "fixtureId", "fixture", "expectedCode", "OK")));
        root.put("createdAt", "2026-08-03T00:00:00Z"); root.put("migrationPolicy", map("automaticProjectUpgrade", false, "projectRebindAllowed", false));
        root.put("canonicalPackHash", hashWithout(root));
        EditorialPackManifest manifest = EditorialPackManifest.parse(EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8));
        EditorialPackRegistryMetadata.CompatibilitySnapshot compatibility = new EditorialPackRegistryMetadata.CompatibilitySnapshot(
                classification, classification, manifest.machineContractFingerprint(), "1.0.0", blockedReason, missing, 123L);
        return manifest.withRegistryMetadata(new EditorialPackRegistryMetadata(state, "immutable/" + manifest.canonicalPackHash(), blockedReason,
                100L, 120L, "1.0.0", integrity, integrity == EditorialPackRegistryMetadata.IntegrityState.VALID ? "" : "storage issue", compatibility));
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static Map<String, Object> file(String role, String path, byte[] bytes) { return map("role", role, "path", path, "mediaType", "text/plain", "charset", "UTF-8", "bom", "FORBIDDEN", "byteLength", BigDecimal.valueOf(bytes.length), "sha256", EditorialCanonicalJson.sha256Hex(bytes)); }
    private static String hashWithout(Map<String, Object> root) { LinkedHashMap<String, Object> copy = new LinkedHashMap<>(root); copy.remove("canonicalPackHash"); byte[] canonical = EditorialCanonicalJson.canonicalize(copy).getBytes(StandardCharsets.UTF_8); byte[] domain = "EDITORIAL_PACK_CANONICAL_HASH_V1\n".getBytes(StandardCharsets.UTF_8); byte[] payload = Arrays.copyOf(domain, domain.length + canonical.length); System.arraycopy(canonical, 0, payload, domain.length, canonical.length); return EditorialCanonicalJson.sha256Hex(payload); }
    private static Map<String, Object> map(Object... values) { LinkedHashMap<String, Object> result = new LinkedHashMap<>(); for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]); return result; }
}
