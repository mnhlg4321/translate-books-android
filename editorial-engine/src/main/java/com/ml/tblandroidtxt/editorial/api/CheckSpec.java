package com.ml.tblandroidtxt.editorial.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The single description of the check answer. The prompt text that tells the model what to write, the strict JSON schema
 * the provider enforces and the keys the parser reads all come from {@link #FIELDS}, so they cannot drift apart (the same
 * principle as EditorialFieldSpec for the SAFE4 wires).
 */
public final class CheckSpec {
    public enum Type { STRING, ENUM }

    /** {@code parent} is empty for a top-level key and {@code "issues"} for a key of one issue. */
    public record Field(String parent, String name, Type type, int minLength, int maxLength, List<String> values, String meaning) {
        public Field {
            values = List.copyOf(values);
        }

        public String path() { return parent.isEmpty() ? name : parent + "[]." + name; }
    }

    public static final String VERDICT = "verdict";
    public static final String WRONG_PAIR_EVIDENCE = "wrong_pair_evidence";
    public static final String ISSUES = "issues";
    public static final String EDITED_QUOTE = "edited_quote";
    public static final String RAW_QUOTE = "raw_quote";
    public static final String KIND = "kind";
    public static final String FIX = "fix";

    public static final List<String> VERDICTS = List.of("PASS", "ISSUES", "WRONG_PAIR");

    public static final List<Field> FIELDS;

    static {
        List<String> kinds = new ArrayList<>();
        for (EditorialApiContract.IssueKind kind : EditorialApiContract.IssueKind.values()) kinds.add(kind.name());
        List<Field> fields = new ArrayList<>();
        fields.add(new Field("", VERDICT, Type.ENUM, 0, 0, VERDICTS,
                "PASS when the EDITED text has no real problem; ISSUES when it has; WRONG_PAIR only when RAW and EDITED are clearly different chapters"));
        fields.add(new Field("", WRONG_PAIR_EVIDENCE, Type.STRING, 0, EditorialApiContract.MAX_WRONG_PAIR_EVIDENCE_CHARS, List.of(),
                "one sentence with a short quote from each text when the verdict is WRONG_PAIR, otherwise an empty string"));
        fields.add(new Field(ISSUES, EDITED_QUOTE, Type.STRING, 1, EditorialApiContract.MAX_QUOTE_CHARS, List.of(),
                "an exact quote of the problem passage in the EDITED text, at most 120 characters"));
        fields.add(new Field(ISSUES, RAW_QUOTE, Type.STRING, 0, EditorialApiContract.MAX_QUOTE_CHARS, List.of(),
                "the matching passage of RAW (empty when there is none), at most 120 characters"));
        fields.add(new Field(ISSUES, KIND, Type.ENUM, 0, 0, kinds, "what kind of problem it is"));
        fields.add(new Field(ISSUES, FIX, Type.STRING, 0, EditorialApiContract.MAX_FIX_CHARS, List.of(),
                "the text that should replace exactly the edited_quote, or an empty string when you only report the problem"));
        FIELDS = List.copyOf(fields);
    }

    private CheckSpec() { }

    public static List<Field> topLevel() {
        List<Field> out = new ArrayList<>();
        for (Field field : FIELDS) if (field.parent().isEmpty()) out.add(field);
        return out;
    }

    public static List<Field> issueFields() {
        List<Field> out = new ArrayList<>();
        for (Field field : FIELDS) if (ISSUES.equals(field.parent())) out.add(field);
        return out;
    }

    /** Strict schema: every key is required, nothing else is allowed; an inapplicable value is the empty string. */
    public static Map<String, Object> schema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Field field : topLevel()) properties.put(field.name(), leaf(field));
        Map<String, Object> issueProperties = new LinkedHashMap<>();
        for (Field field : issueFields()) issueProperties.put(field.name(), leaf(field));
        Map<String, Object> array = new LinkedHashMap<>();
        array.put("type", "array");
        array.put("items", object(issueProperties));
        properties.put(ISSUES, array);
        return object(properties);
    }

    private static Map<String, Object> object(Map<String, Object> properties) {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("type", "object");
        object.put("additionalProperties", Boolean.FALSE);
        object.put("required", new ArrayList<Object>(properties.keySet()));
        object.put("properties", properties);
        return object;
    }

    private static Map<String, Object> leaf(Field field) {
        Map<String, Object> leaf = new LinkedHashMap<>();
        leaf.put("type", "string");
        if (field.type() == Type.ENUM) {
            leaf.put("enum", new ArrayList<Object>(field.values()));
        } else {
            leaf.put("minLength", BigDecimal.valueOf(field.minLength()));
            leaf.put("maxLength", BigDecimal.valueOf(field.maxLength()));
        }
        return leaf;
    }

    /** The answer format as the prompt states it, generated from the same fields as the schema. */
    public static String promptDescription() {
        StringBuilder out = new StringBuilder("ANSWER FORMAT\nReturn one JSON object and nothing else. Every key below is present.\n");
        for (Field field : topLevel()) out.append(line(field, "- "));
        out.append("- ").append(ISSUES).append(": an array (empty when verdict is PASS); each item has every key below.\n");
        for (Field field : issueFields()) out.append(line(field, "  - "));
        return out.toString();
    }

    private static String line(Field field, String bullet) {
        StringBuilder out = new StringBuilder(bullet).append(field.name()).append(": ");
        if (field.type() == Type.ENUM) out.append("one of ").append(String.join(" | ", field.values())).append("; ");
        else out.append("text, ").append(field.minLength()).append("..").append(field.maxLength()).append(" characters; ");
        return out.append(field.meaning()).append(".\n").toString();
    }
}
