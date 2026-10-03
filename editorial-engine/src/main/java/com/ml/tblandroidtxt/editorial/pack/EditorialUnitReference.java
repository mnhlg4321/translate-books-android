package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Wire v3 uses physical RAW line references; durable artifacts keep inventory ids. */
public final class EditorialUnitReference {
    private static final Set<String> SCALAR_KEYS = Set.of("unitId", "from", "to");
    private static final Set<String> LIST_KEYS = Set.of("rawUnits", "occurrenceUnits");

    private EditorialUnitReference() { }

    public static String of(EditorialRawInventory.Unit unit) { return "L" + unit.line(); }

    public static String resolve(Object reference, EditorialRawInventory.Inventory inventory) {
        if (!(reference instanceof String) || !((String) reference).matches("^L[1-9][0-9]*$")) {
            throw new IllegalArgumentException("L1_UNIT_REF_INVALID");
        }
        int line;
        try { line = Integer.parseInt(((String) reference).substring(1)); }
        catch (NumberFormatException outside) { throw new IllegalArgumentException("L1_UNIT_UNKNOWN"); }
        if (line > inventory.physicalLines()) throw new IllegalArgumentException("L1_UNIT_UNKNOWN");
        for (EditorialRawInventory.Unit unit : inventory.units()) if (unit.line() == line) return unit.id();
        throw new IllegalArgumentException("L1_UNIT_LINE_NOT_A_UNIT");
    }

    public static List<String> resolveAll(List<String> references, EditorialRawInventory.Inventory inventory) {
        List<String> ids = new ArrayList<>();
        for (String reference : references) ids.add(resolve(reference, inventory));
        return List.copyOf(ids);
    }

    public static List<String> resolveList(Object value, String path, int maximum,
                                           EditorialRawInventory.Inventory inventory) {
        List<Object> references = EditorialCanonicalJson.array(value, path);
        if (references.size() > maximum) throw new IllegalArgumentException("L1_LIST_TOO_LONG");
        List<String> ids = new ArrayList<>();
        for (Object reference : references) ids.add(resolve(reference, inventory));
        return List.copyOf(ids);
    }

    /** Convert an app-owned full id to its wire reference, without changing arbitrary source text. */
    public static String fromId(String id) {
        if (!id.matches("u:[1-9][0-9]*:[0-9a-f]{8}")) throw new IllegalArgumentException("L1_UNIT_REF_INVALID");
        return "L" + id.substring(2, id.indexOf(':', 2));
    }

    /** A separate model-facing view. Never use this view as the persisted artifact. */
    public static Object wireView(Object value) { return wireView(value, ""); }

    private static Object wireView(Object value, String key) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String childKey = (String) entry.getKey();
                result.put(childKey, wireView(entry.getValue(), childKey));
            }
            return result;
        }
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>();
            for (Object item : list) result.add(wireView(item, LIST_KEYS.contains(key) ? "unitId" : ""));
            return result;
        }
        if (value instanceof String text && SCALAR_KEYS.contains(key) && text.matches("u:[1-9][0-9]*:[0-9a-f]{8}")) {
            return fromId(text);
        }
        return value;
    }
}
