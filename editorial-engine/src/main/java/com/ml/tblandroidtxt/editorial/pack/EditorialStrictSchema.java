package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Z2: strict response schemas for the L2 and L3 phases, generated from {@link EditorialFieldSpec} so that the prompt rules,
 * the schema the decoder enforces and the parser share one table. Every property of an object is required and no other
 * property is allowed (the form a strict provider schema needs); a value that does not apply is the empty value the
 * field table already allows. The L1 phases keep their own generator in {@link EditorialL1Ledger}.
 */
public final class EditorialStrictSchema {
    /** A provider response-format description: a name and a JSON schema. */
    public record Spec(String name, Map<String, Object> schema) { }

    private EditorialStrictSchema() { }

    /**
     * The strict schema for a request, or {@code null} when the wire is not one of the generated ones (L1, legacy wires):
     * the caller then keeps its previous response format.
     */
    public static Spec forRequest(String outputSchemaId, String requestPhase) {
        if (outputSchemaId == null) return null;
        String found = null;
        for (String phase : EditorialFieldSpec.phases()) {
            if (phase.startsWith("L1_")) continue;
            EditorialFieldSpec.Field wire = EditorialFieldSpec.find(phase, "wireSchemaVersion");
            if (wire == null || !wire.values().contains(outputSchemaId)) continue;
            if (found == null || phase.equals(requestPhase)) found = phase;
        }
        if (found == null) return null;
        return new Spec("safe4_" + found.toLowerCase(java.util.Locale.ROOT) + "_strict", forPhase(found));
    }

    public static Map<String, Object> forPhase(String phase) {
        return objectOf(phase, "");
    }

    private static Map<String, Object> schemaOf(String phase, String path) {
        EditorialFieldSpec.Field field = EditorialFieldSpec.find(phase, path);
        if (field == null) return objectOf(phase, path);
        switch (field.kind()) {
            case STRING, ENUM:
                return EditorialFieldSpec.schema(phase, path);
            case INTEGER:
                return typed("integer");
            case BOOLEAN:
                return typed("boolean");
            case ARRAY: {
                Map<String, Object> array = new LinkedHashMap<>();
                array.put("type", "array");
                if (field.minItems() > 0) array.put("minItems", BigDecimal.valueOf(field.minItems()));
                array.put("maxItems", BigDecimal.valueOf(field.maxItems()));
                String element = path + "[]";
                array.put("items", EditorialFieldSpec.find(phase, element) != null ? schemaOf(phase, element) : objectOf(phase, element));
                return array;
            }
            case OBJECT:
            default:
                return objectOf(phase, path);
        }
    }

    private static Map<String, Object> objectOf(String phase, String prefix) {
        List<String> names = new ArrayList<>();
        String start = prefix.isEmpty() ? "" : prefix + ".";
        for (EditorialFieldSpec.Field field : EditorialFieldSpec.fields(phase)) {
            String path = field.path();
            if (!path.startsWith(start)) continue;
            String rest = path.substring(start.length());
            int dot = rest.indexOf('.');
            String segment = dot < 0 ? rest : rest.substring(0, dot);
            if (segment.endsWith("[]")) segment = segment.substring(0, segment.length() - 2);
            if (segment.isEmpty() || names.contains(segment)) continue;
            names.add(segment);
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String name : names) properties.put(name, schemaOf(phase, start + name));
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("type", "object");
        object.put("additionalProperties", Boolean.FALSE);
        object.put("required", new ArrayList<Object>(properties.keySet()));
        object.put("properties", properties);
        return object;
    }

    private static Map<String, Object> typed(String type) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        return map;
    }
}
