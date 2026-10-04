package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class EditorialFieldSpecTest {
    private static final Set<String> ALL_PHASES = Set.of(
            EditorialFieldSpec.L1_RAW_DISCOVERY, EditorialFieldSpec.L1_RECONCILE,
            EditorialFieldSpec.L2_RAW_DISCOVERY, EditorialFieldSpec.L2_EDIT, EditorialFieldSpec.L2_FINAL_READ,
            EditorialFieldSpec.L3_RAW_FIRST_REAUDIT, EditorialFieldSpec.L3_RECONCILE, EditorialFieldSpec.L3_FINAL_READ);

    @Test public void allEightPhasesHaveRequiredPromptRulesForEveryField() {
        assertEquals(ALL_PHASES, EditorialFieldSpec.phases());
        for (String phase : ALL_PHASES) {
            assertFalse("empty FieldSpec for " + phase, EditorialFieldSpec.fields(phase).isEmpty());
            String prompt = EditorialFieldSpec.promptRules(phase);
            assertTrue("prompt header missing for " + phase, prompt.contains("FIELD SPECIFICATION"));
            for (EditorialFieldSpec.Field field : EditorialFieldSpec.fields(phase)) {
                String rule;
                if (field.kind() == EditorialFieldSpec.Kind.ARRAY || field.kind() == EditorialFieldSpec.Kind.OBJECT) {
                    rule = "- " + (field.keyRequired() ? "MUST include " : "MAY omit ") + field.path();
                } else if (!field.keyRequired()) {
                    rule = "- MAY omit " + field.path();
                } else {
                    rule = "- " + field.requirement() + " " + field.path();
                }
                if (field.kind() == EditorialFieldSpec.Kind.INTEGER) rule += " integer";
                if (field.kind() == EditorialFieldSpec.Kind.BOOLEAN) rule += " boolean";
                assertTrue("prompt omitted " + phase + ":" + field.path(), prompt.contains(rule));
                if ((field.kind() == EditorialFieldSpec.Kind.STRING || field.kind() == EditorialFieldSpec.Kind.ENUM)
                        && field.requirement() == EditorialFieldSpec.Requirement.MUST) {
                    assertTrue("MUST value must be non-empty: " + phase + ":" + field.path(), field.minLength() >= 1);
                }
            }
        }
    }

    @Test public void l1StrictSchemasUseTheSameStringAndArrayBoundsAsFieldSpec() {
        assertSchemaMatches(EditorialFieldSpec.L1_RAW_DISCOVERY, EditorialL1Ledger.jsonSchema(true));
        assertSchemaMatches(EditorialFieldSpec.L1_RECONCILE, EditorialL1Ledger.jsonSchema(false));
        assertSchemaMatches(EditorialFieldSpec.L2_FINAL_READ, EditorialFinalRead.jsonSchema());
    }

    @SuppressWarnings("unchecked")
    private static void assertSchemaMatches(String phase, Map<String, Object> schema) {
        for (EditorialFieldSpec.Field field : EditorialFieldSpec.fields(phase)) {
            if (field.kind() != EditorialFieldSpec.Kind.STRING && field.kind() != EditorialFieldSpec.Kind.ENUM
                    && field.kind() != EditorialFieldSpec.Kind.ARRAY) continue;
            Map<String, Object> leaf = schemaAt(schema, field.path());
            assertNotNull("schema omitted " + phase + ":" + field.path(), leaf);
            if (field.kind() == EditorialFieldSpec.Kind.ARRAY) {
                assertEquals(field.minItems(), leaf.containsKey("minItems")
                        ? ((BigDecimal) leaf.get("minItems")).intValueExact() : 0);
                assertEquals(field.maxItems(), ((BigDecimal) leaf.get("maxItems")).intValueExact());
                assertEquals("array", leaf.get("type"));
                continue;
            }
            assertEquals("string", leaf.get("type"));
            assertEquals(field.minLength(), ((BigDecimal) leaf.get("minLength")).intValueExact());
            assertEquals(field.maxLength(), ((BigDecimal) leaf.get("maxLength")).intValueExact());
            if (field.pattern().isEmpty()) assertFalse(leaf.containsKey("pattern"));
            else assertEquals(field.pattern(), leaf.get("pattern"));
            if (field.values().isEmpty()) assertFalse(leaf.containsKey("enum"));
            else assertEquals(field.values(), leaf.get("enum"));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> schemaAt(Map<String, Object> root, String path) {
        Map<String, Object> current = root;
        for (String segment : path.split("\\.")) {
            int cursor = 0;
            int bracket = segment.indexOf('[');
            String name = bracket < 0 ? segment : segment.substring(0, bracket);
            Map<String, Object> properties = (Map<String, Object>) current.get("properties");
            current = (Map<String, Object>) properties.get(name);
            if (current == null) return null;
            cursor = name.length();
            while (cursor < segment.length()) {
                if (!segment.startsWith("[]", cursor)) throw new AssertionError("bad FieldSpec path " + path);
                current = (Map<String, Object>) current.get("items");
                if (current == null) return null;
                cursor += 2;
            }
        }
        return current;
    }
}
