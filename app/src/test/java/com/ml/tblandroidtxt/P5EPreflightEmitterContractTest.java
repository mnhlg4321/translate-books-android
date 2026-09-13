package com.ml.tblandroidtxt;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Host-only contract tests for the actual AndroidTest status emitter source.
 * Parser fixtures remain parser-only; this class reads the implementation
 * mapping and verifies it against the parser's live contract sets.
 */
public final class P5EPreflightEmitterContractTest {
    private static final String EMITTER_RELATIVE_PATH =
            "app/src/androidTest/java/com/ml/tblandroidtxt/"
                    + "EditorialP5EFreshRawLiveInstrumentedTest.java";
    private static final String EXPECTED_EMITTER_PREFIX = "p5e.preflight.v2.";
    private static final Pattern PREFIX_DECLARATION = Pattern.compile(
            "PREFLIGHT_STATUS_PREFIX\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern MANIFEST_BOOLEAN = Pattern.compile(
            "manifest\\.put\\(\\s*\"([^\"]+)\"\\s*,\\s*(true|false)\\s*\\)");
    private static final Pattern MANIFEST_MAPPING = Pattern.compile(
            "putManifestString\\(\\s*status\\s*,\\s*manifest\\s*,\\s*\"([^\"]+)\"\\s*\\)");
    private static final Pattern DIRECT_STATUS_MAPPING = Pattern.compile(
            "status\\.putString\\(\\s*([^,]+?)\\s*,\\s*\"([^\"]*)\"\\s*\\)");

    @Test
    public void actualEmitterMapsEveryRequiredParserFieldExactlyOnce() throws IOException {
        ContractResult result = inspect(readEmitterSource());
        assertTrue(result.errors.toString(), result.errors.isEmpty());
        assertTrue(result.fields.contains("reconciliationCreated"));
    }

    @Test
    public void catchesManifestFieldThatEmitterDoesNotEmit() throws IOException {
        String source = readEmitterSource();
        String mapping = "putManifestString(status, manifest, \"reconciliationCreated\");";
        ContractResult result = inspect(source.replace(mapping + "\n", ""));
        assertTrue(result.errors.toString(),
                result.errors.contains("MISSING_EMITTER_FIELD:reconciliationCreated"));
    }

    @Test
    public void catchesDuplicateEmitterMapping() throws IOException {
        String source = readEmitterSource();
        String mapping = "        putManifestString(status, manifest, \"reconciliationCreated\");\n";
        ContractResult result = inspect(source.replace(mapping, mapping + mapping));
        assertTrue(result.errors.toString(),
                result.errors.contains("DUPLICATE_FIELD:reconciliationCreated:2"));
    }

    @Test
    public void catchesWrongEmitterPrefix() throws IOException {
        String source = readEmitterSource().replace(
                "PREFLIGHT_STATUS_PREFIX = \"p5e.preflight.v2.\"",
                "PREFLIGHT_STATUS_PREFIX = \"p5e.preflight.v1.\"");
        ContractResult result = inspect(source);
        assertTrue(result.errors.toString(),
                result.errors.contains("PREFIX_INVALID:p5e.preflight.v1."));
    }

    @Test
    public void catchesBooleanValueThatCannotBeConvertedStrictly() throws IOException {
        String source = readEmitterSource().replace(
                "status.putString(PREFLIGHT_STATUS_PREFIX + \"providerMatch\", \"true\");",
                "status.putString(PREFLIGHT_STATUS_PREFIX + \"providerMatch\", \"TRUE\");");
        ContractResult result = inspect(source);
        assertTrue(result.errors.toString(),
                result.errors.contains("BOOLEAN_INVALID:providerMatch:TRUE"));
    }

    private static ContractResult inspect(String source) {
        List<String> errors = new ArrayList<>();
        Map<String, List<Mapping>> mappings = new HashMap<>();
        Map<String, String> manifestBooleanValues = new HashMap<>();

        String prefix = findGroup(PREFIX_DECLARATION, source, 1);
        if (!EXPECTED_EMITTER_PREFIX.equals(prefix)
                || !P5EInstrumentationStatusParser.PREFIX.equals(prefix)) {
            errors.add("PREFIX_INVALID:" + prefix);
        }

        String manifestBody = methodBody(source,
                "private static JSONObject redactedPreflightManifest(",
                "private static Bundle redactedPreflightStatus(");
        String statusBody = methodBody(source,
                "private static Bundle redactedPreflightStatus(",
                "private static void putManifestString(");
        String helperBody = methodBody(source,
                "private static void putManifestString(",
                "private static JSONArray sourceInventory(");
        if (!helperBody.contains("PREFLIGHT_STATUS_PREFIX + key")) {
            errors.add("HELPER_PREFIX_INVALID");
        }

        Matcher manifestBooleanMatcher = MANIFEST_BOOLEAN.matcher(manifestBody);
        while (manifestBooleanMatcher.find()) {
            manifestBooleanValues.put(manifestBooleanMatcher.group(1),
                    manifestBooleanMatcher.group(2));
        }

        Matcher manifestMappingMatcher = MANIFEST_MAPPING.matcher(statusBody);
        while (manifestMappingMatcher.find()) {
            addMapping(mappings, manifestMappingMatcher.group(1),
                    Mapping.manifest(manifestBooleanValues.get(manifestMappingMatcher.group(1))));
        }

        Matcher directStatusMatcher = DIRECT_STATUS_MAPPING.matcher(statusBody);
        while (directStatusMatcher.find()) {
            String keyExpression = directStatusMatcher.group(1).trim();
            String fieldPrefix = "PREFLIGHT_STATUS_PREFIX + \"";
            if (!keyExpression.startsWith(fieldPrefix) || !keyExpression.endsWith("\"")) {
                errors.add("BAD_PREFIX_EXPRESSION:" + keyExpression);
                continue;
            }
            String field = keyExpression.substring(fieldPrefix.length(),
                    keyExpression.length() - 1);
            addMapping(mappings, field, Mapping.literal(directStatusMatcher.group(2)));
        }

        Set<String> required = P5EInstrumentationStatusParser.requiredFieldsForContract();
        Set<String> allowed = P5EInstrumentationStatusParser.allowedFieldsForContract();
        Set<String> booleanFields = P5EInstrumentationStatusParser.booleanFieldsForContract();
        Set<String> mappedFields = new HashSet<>(mappings.keySet());
        for (String field : mappedFields) {
            if (!allowed.contains(field)) errors.add("UNKNOWN_FIELD:" + field);
            List<Mapping> fieldMappings = mappings.get(field);
            if (fieldMappings.size() > 1) {
                errors.add("DUPLICATE_FIELD:" + field + ":" + fieldMappings.size());
            }
            if (booleanFields.contains(field) && fieldMappings.size() == 1) {
                String value = fieldMappings.get(0).booleanValue;
                if (!"true".equals(value) && !"false".equals(value)) {
                    errors.add("BOOLEAN_INVALID:" + field + ":" + value);
                }
            }
        }
        for (String field : required) {
            List<Mapping> fieldMappings = mappings.get(field);
            if (fieldMappings == null || fieldMappings.isEmpty()) {
                errors.add("MISSING_EMITTER_FIELD:" + field);
            }
        }
        return new ContractResult(mappedFields, errors);
    }

    private static void addMapping(Map<String, List<Mapping>> mappings,
                                   String field, Mapping mapping) {
        mappings.computeIfAbsent(field, ignored -> new ArrayList<>()).add(mapping);
    }

    private static String findGroup(Pattern pattern, String source, int group) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group(group) : null;
    }

    private static String methodBody(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        if (start < 0 || end < 0) {
            throw new IllegalArgumentException("Cannot isolate emitter method: " + startMarker);
        }
        return source.substring(start, end);
    }

    private static String readEmitterSource() throws IOException {
        Path current = Paths.get("").toAbsolutePath();
        for (Path root = current; root != null; root = root.getParent()) {
            Path candidate = root.resolve(EMITTER_RELATIVE_PATH);
            if (Files.isRegularFile(candidate)) {
                return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
            }
        }
        throw new IOException("Emitter source not found from " + current);
    }

    private static final class Mapping {
        private final String booleanValue;

        private Mapping(String booleanValue) {
            this.booleanValue = booleanValue;
        }

        private static Mapping manifest(String value) {
            return new Mapping(value);
        }

        private static Mapping literal(String value) {
            return new Mapping(value);
        }
    }

    private static final class ContractResult {
        private final Set<String> fields;
        private final List<String> errors;

        private ContractResult(Set<String> fields, List<String> errors) {
            this.fields = fields;
            this.errors = errors;
        }
    }
}
