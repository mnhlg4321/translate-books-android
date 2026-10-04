package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Shared wire-field constraints used by parser validation, L1 response schemas, and provider prompts. */
public final class EditorialFieldSpec {
    public static final int MAX_ID_LENGTH = 48;
    public static final int MAX_UNIT_REFERENCE_LENGTH = 64;
    public static final int MAX_ATTEMPT_IDENTITY_LENGTH = 128;
    public static final int MAX_RANGES = 120;
    public static final int MAX_L1_CANDIDATES = 400;
    public static final int MAX_L2_CANDIDATES = 300;
    public static final int MAX_L3_CANDIDATES = 400;
    public static final int MAX_FINDINGS = 48;
    public static final int MAX_RAW_UNITS_PER_FINDING = 6;
    public static final int MAX_OCCURRENCES = 40;
    public static final int MAX_SPEAKERS = 200;
    public static final int MAX_PROTECTED_SPANS = 100;
    public static final int MAX_TEXT = 240;
    public static final int MAX_QUOTE = 80;
    public static final int MAX_EVIDENCE_REFS = 4;
    public static final int MAX_CANDIDATE_REFS = 8;
    public static final int MAX_CHANGE_REFS = 20;
    public static final int MAX_L2_CHANGES = 200;
    public static final int MAX_L2_PRESERVED = 100;
    public static final int MAX_PROBES = 40;
    public static final int MAX_PROBE_TEXT = 120;
    public static final int MAX_FINAL_READ_PROBES = 3;
    public static final int MAX_FINAL_READ_DEFECTS = 40;
    public static final int MAX_FINAL_READ_TAIL = 12;
    public static final int MAX_MODEL_TEXT = 2_000;
    public static final int MAX_CANDIDATE_NOTE = 80;
    public static final int MAX_SPEAKER_LABEL = 80;

    public static final String L1_RAW_DISCOVERY = "L1_RAW_DISCOVERY";
    public static final String L1_RECONCILE = "L1_RECONCILE";
    public static final String L2_RAW_DISCOVERY = "L2_RAW_DISCOVERY";
    public static final String L2_EDIT = "L2_EDIT";
    public static final String L2_FINAL_READ = "L2_FINAL_READ";
    public static final String L3_RAW_FIRST_REAUDIT = "L3_RAW_FIRST_REAUDIT";
    public static final String L3_RECONCILE = "L3_RECONCILE";
    public static final String L3_FINAL_READ = "L3_FINAL_READ";

    public enum Kind { STRING, ENUM, ARRAY, INTEGER, BOOLEAN, OBJECT }
    public enum Requirement { MUST, MAY, CONDITIONAL }

    public record Field(String phase, String path, Kind kind, Requirement requirement, boolean keyRequired,
                        int minLength, int maxLength, int minItems, int maxItems, String pattern,
                        List<String> values, String condition) {
        public Field {
            values = List.copyOf(values);
            condition = condition == null ? "" : condition;
        }
    }

    private static final String ID_PATTERN = "^[A-Za-z0-9][A-Za-z0-9._:/-]*$";
    private static final String OPTIONAL_ID_PATTERN = "^(?:[A-Za-z0-9][A-Za-z0-9._:/-]*)?$";
    public static final String UNIT_REFERENCE_PATTERN = "^L[1-9][0-9]*$";
    private static final String UNIT_PATTERN = UNIT_REFERENCE_PATTERN;
    private static final String NON_BLANK_PATTERN = "^[\\s\\S]*\\S[\\s\\S]*$";
    private static final String OPTIONAL_REASON_PATTERN = "^(?:[A-Za-z0-9][A-Za-z0-9 ._:/;()/-]{0,31})?$";
    private static final List<Field> FIELDS = build();
    private static final Map<String, List<Field>> BY_PHASE = index();

    private EditorialFieldSpec() { }

    public static List<Field> fields(String phase) { return BY_PHASE.getOrDefault(phase, List.of()); }

    public static Set<String> phases() { return BY_PHASE.keySet(); }

    public static Field find(String phase, String path) {
        String normalized = normalize(path);
        for (Field field : fields(phase)) if (field.path().equals(normalized)) return field;
        return null;
    }

    /** Builds the string/enum leaf schema directly from this table. */
    public static Map<String, Object> schema(String phase, String path) {
        Field field = find(phase, path);
        if (field == null || (field.kind() != Kind.STRING && field.kind() != Kind.ENUM)) {
            throw new IllegalArgumentException("EDITORIAL_FIELD_SPEC_MISSING:" + phase + ":" + normalize(path));
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        schema.put("minLength", BigDecimal.valueOf(field.minLength()));
        schema.put("maxLength", BigDecimal.valueOf(field.maxLength()));
        if (!field.pattern().isEmpty()) schema.put("pattern", field.pattern());
        if (!field.values().isEmpty()) schema.put("enum", new ArrayList<>(field.values()));
        return schema;
    }

    public static int minItems(String phase, String path) { return require(phase, path, Kind.ARRAY).minItems(); }
    public static int maxItems(String phase, String path) { return require(phase, path, Kind.ARRAY).maxItems(); }

    /** Validates string shape from the shared spec while leaving CONDITIONAL emptiness to its status-aware parser. */
    public static String validateString(String phase, String path, Object value, String typeCode,
                                        String requiredCode, String lengthCode, String patternCode) {
        Field field = find(phase, path);
        if (field == null || (field.kind() != Kind.STRING && field.kind() != Kind.ENUM)) {
            throw WireViolation.at("EDITORIAL_FIELD_SPEC_MISSING", phase + "." + normalize(path));
        }
        if (!(value instanceof String text)) throw WireViolation.at(typeCode, path);
        if (text.length() > field.maxLength()) throw WireViolation.at(lengthCode, path);
        if (field.requirement() == Requirement.MUST && text.isBlank()) {
            throw WireViolation.at(requiredCode, path);
        }
        if (!field.pattern().isEmpty() && !Pattern.matches(field.pattern(), text)) {
            throw WireViolation.at(patternCode, path);
        }
        return text;
    }

    /** Compact per-phase contract text; the prompts and the L1 schema draw from the same field rows. */
    public static String promptRules(String phase) {
        List<Field> phaseFields = fields(phase);
        if (phaseFields.isEmpty()) return "";
        StringBuilder result = new StringBuilder("\nFIELD SPECIFICATION (shared with the app parser and response schema):\n");
        result.append("Keys marked MAY omit can be absent. For keys that are present, MUST strings cannot be blank, "
                + "MAY strings can be empty, and CONDITIONAL values follow the stated condition.\n");
        for (Field field : phaseFields) {
            if (field.kind() == Kind.STRING || field.kind() == Kind.ENUM) {
                if (field.keyRequired()) result.append("- ").append(field.requirement()).append(' ').append(field.path());
                else result.append("- MAY omit ").append(field.path()).append("; when present its value MUST");
                if (field.kind() == Kind.ENUM) result.append(" ∈ {").append(String.join("|", field.values())).append('}');
                else result.append(" length ").append(field.minLength()).append("..").append(field.maxLength());
                if (!field.pattern().isEmpty()) result.append("; pattern ").append(field.pattern());
                if (!field.condition().isEmpty()) result.append("; ").append(field.condition());
                result.append('\n');
            } else if (field.kind() == Kind.ARRAY) {
                result.append("- ").append(field.keyRequired() ? "MUST include " : "MAY omit ")
                        .append(field.path()).append(" array; items ").append(field.minItems()).append("..")
                        .append(field.maxItems()).append('\n');
            } else if (field.kind() == Kind.OBJECT) {
                result.append("- ").append(field.keyRequired() ? "MUST include " : "MAY omit ")
                        .append(field.path()).append(" object")
                        .append(field.condition().isEmpty() ? "" : "; " + field.condition()).append('\n');
            } else if (field.kind() == Kind.INTEGER || field.kind() == Kind.BOOLEAN) {
                result.append("- MUST ").append(field.path()).append(' ')
                        .append(field.kind() == Kind.INTEGER ? "integer" : "boolean").append('\n');
            }
        }
        return result.toString();
    }

    private static Field require(String phase, String path, Kind kind) {
        Field field = find(phase, path);
        if (field == null || field.kind() != kind) {
            throw new IllegalArgumentException("EDITORIAL_FIELD_SPEC_MISSING:" + phase + ":" + normalize(path));
        }
        return field;
    }

    private static String normalize(String path) {
        if (path == null) return "";
        return path.replaceAll("\\.(\\d+)(?=\\.|$)", "[]");
    }

    private static Map<String, List<Field>> index() {
        Map<String, List<Field>> result = new LinkedHashMap<>();
        for (Field field : FIELDS) result.computeIfAbsent(field.phase(), ignored -> new ArrayList<>()).add(field);
        Map<String, List<Field>> frozen = new LinkedHashMap<>();
        result.forEach((phase, fields) -> frozen.put(phase, List.copyOf(fields)));
        return Map.copyOf(frozen);
    }

    private static List<Field> build() {
        List<Field> fields = new ArrayList<>();
        rawPhase(fields, L1_RAW_DISCOVERY, false, true);
        l1Reconcile(fields);
        rawPhase(fields, L2_RAW_DISCOVERY, false, false);
        l2Edit(fields);
        finalRead(fields, L2_FINAL_READ);
        rawPhase(fields, L3_RAW_FIRST_REAUDIT, true, false);
        l3Reconcile(fields);
        finalRead(fields, L3_FINAL_READ);
        return List.copyOf(fields);
    }

    private static void rawPhase(List<Field> fields, String phase, boolean l3, boolean l1Strict) {
        s(fields, phase, "attemptIdentity", Requirement.MUST, 1, MAX_ATTEMPT_IDENTITY_LENGTH, "", "");
        e(fields, phase, "wireSchemaVersion", l3 ? List.of("safe4.l3.reaudit.wire.v3")
                : l1Strict ? List.of("safe4.l1.raw-ledger.wire.v3")
                : List.of("safe4.l2.raw-discovery.wire.v3"), "");
        a(fields, phase, "coverage", 0, MAX_RANGES, Requirement.MUST);
        s(fields, phase, "coverage[].from", Requirement.MUST, 3, MAX_UNIT_REFERENCE_LENGTH, UNIT_PATTERN, "");
        s(fields, phase, "coverage[].to", Requirement.MUST, 3, MAX_UNIT_REFERENCE_LENGTH, UNIT_PATTERN, "");
        e(fields, phase, "coverage[].status", List.of("PROCESSED", "PRESERVED"), "");
        a(fields, phase, "candidates", 0, l1Strict ? MAX_L1_CANDIDATES : l3 ? MAX_L3_CANDIDATES : MAX_L2_CANDIDATES, Requirement.MAY);
        s(fields, phase, "candidates[].candidateId", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        e(fields, phase, "candidates[].ledger", List.of("PAIR", "RC", "SPEAKER", "SR", "TG", "UNIT"), "");
        s(fields, phase, "candidates[].unitId", Requirement.MUST, 3, MAX_UNIT_REFERENCE_LENGTH, UNIT_PATTERN, "");
        if (l3) {
            i(fields, phase, "candidates[].viLine");
            e(fields, phase, "candidates[].status", List.of("CONFLICT", "PRESERVED", "PROCESSED", "UNPROCESSED"), "");
        }
        s(fields, phase, "candidates[].note", Requirement.MAY, 0, MAX_CANDIDATE_NOTE, "", "");
    }

    private static void l1Reconcile(List<Field> fields) {
        String phase = L1_RECONCILE;
        s(fields, phase, "attemptIdentity", Requirement.MUST, 1, MAX_ATTEMPT_IDENTITY_LENGTH, "", "");
        e(fields, phase, "wireSchemaVersion", List.of("safe4.l1.reconcile-ledger.wire.v3"), "");
        a(fields, phase, "coverage", 0, MAX_RANGES, Requirement.MUST);
        s(fields, phase, "coverage[].from", Requirement.MUST, 3, MAX_UNIT_REFERENCE_LENGTH, UNIT_PATTERN, "");
        s(fields, phase, "coverage[].to", Requirement.MUST, 3, MAX_UNIT_REFERENCE_LENGTH, UNIT_PATTERN, "");
        e(fields, phase, "coverage[].status", List.of("PROCESSED", "PRESERVED"), "");
        a(fields, phase, "resolutions", 0, MAX_L1_CANDIDATES, Requirement.MAY);
        s(fields, phase, "resolutions[].candidateId", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        e(fields, phase, "resolutions[].status", List.of("CONFLICT", "PRESERVED", "PROCESSED", "UNPROCESSED"), "");
        s(fields, phase, "resolutions[].findingRef", Requirement.MAY, 0, 48, OPTIONAL_ID_PATTERN, "");
        a(fields, phase, "findings", 0, 48, Requirement.MAY);
        s(fields, phase, "findings[].errorId", Requirement.MUST, 1, 48, ID_PATTERN, "");
        e(fields, phase, "findings[].type", List.of("ADDRESS_PROFILE", "ADDITION", "GLOSSARY", "MEANING", "NEGATION",
                "NUMBER", "OMISSION", "SPEAKER_LISTENER", "STRUCTURE", "UNTRANSLATED"), "");
        e(fields, phase, "findings[].severity", List.of("CRITICAL", "MAJOR", "MINOR"), "");
        a(fields, phase, "findings[].rawUnits", 1, MAX_RAW_UNITS_PER_FINDING, Requirement.MUST);
        s(fields, phase, "findings[].rawUnits[]", Requirement.MUST, 3, MAX_UNIT_REFERENCE_LENGTH, UNIT_PATTERN, "");
        e(fields, phase, "findings[].draft.kind", List.of("LINES", "MISSING"), "");
        n(fields, phase, "findings[].draft.start");
        n(fields, phase, "findings[].draft.end");
        n(fields, phase, "findings[].draft.after");
        s(fields, phase, "findings[].rawQuote", Requirement.MUST, 1, MAX_QUOTE, NON_BLANK_PATTERN, "");
        s(fields, phase, "findings[].draftQuote", Requirement.CONDITIONAL, 0, MAX_QUOTE, "",
                "non-empty for draft.kind=LINES; empty for draft.kind=MISSING");
        s(fields, phase, "findings[].observation", Requirement.MUST, 1, MAX_TEXT, NON_BLANK_PATTERN, "");
        s(fields, phase, "findings[].expectedMeaning", Requirement.MUST, 1, MAX_TEXT, NON_BLANK_PATTERN, "");
        a(fields, phase, "findings[].evidenceRefs", 0, MAX_EVIDENCE_REFS, Requirement.MAY);
        s(fields, phase, "findings[].evidenceRefs[]", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        a(fields, phase, "findings[].candidateIds", 0, MAX_CANDIDATE_REFS, Requirement.MAY);
        s(fields, phase, "findings[].candidateIds[]", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        a(fields, phase, "findings[].occurrenceUnits", 0, MAX_OCCURRENCES, Requirement.MAY);
        s(fields, phase, "findings[].occurrenceUnits[]", Requirement.MUST, 3, MAX_UNIT_REFERENCE_LENGTH, UNIT_PATTERN, "");
        e(fields, phase, "findings[].disposition", List.of("OPEN", "PRESERVED"), "");
        s(fields, phase, "findings[].evidenceLimit", Requirement.MAY, 0, MAX_TEXT, "", "");
        a(fields, phase, "speakerRecords", 0, MAX_SPEAKERS, Requirement.MAY);
        s(fields, phase, "speakerRecords[].unitId", Requirement.MUST, 3, MAX_UNIT_REFERENCE_LENGTH, UNIT_PATTERN, "");
        s(fields, phase, "speakerRecords[].speaker", Requirement.MUST, 1, MAX_SPEAKER_LABEL, NON_BLANK_PATTERN, "");
        s(fields, phase, "speakerRecords[].listener", Requirement.MAY, 0, MAX_SPEAKER_LABEL, "", "empty or UNKNOWN is allowed");
        s(fields, phase, "speakerRecords[].basis", Requirement.MUST, 1, MAX_TEXT, NON_BLANK_PATTERN, "");
        a(fields, phase, "protectedSpans", 0, MAX_PROTECTED_SPANS, Requirement.MAY);
        s(fields, phase, "protectedSpans[].spanId", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        n(fields, phase, "protectedSpans[].start");
        n(fields, phase, "protectedSpans[].end");
        e(fields, phase, "protectedSpans[].source", List.of("GLOSSARY_ROW", "L1_PROOF", "PRONOUN_ROW", "SPEAKER_PROOF"), "");
        s(fields, phase, "protectedSpans[].reason", Requirement.MUST, 1, MAX_TEXT, NON_BLANK_PATTERN, "");
        e(fields, phase, "disposition.disposition", List.of("CONTINUE", "PRESERVE_DRAFT", "STOP"), "");
        s(fields, phase, "disposition.reasonCode", Requirement.CONDITIONAL, 0, 32, OPTIONAL_REASON_PATTERN,
                "required when disposition=STOP; empty is allowed for CONTINUE/PRESERVE_DRAFT");
        e(fields, phase, "disposition.stopClass", List.of("CONTENT_BLOCKED", "INPUT_REQUIRED", "NONE"), "");
    }

    private static void l2Edit(List<Field> fields) {
        String phase = L2_EDIT;
        s(fields, phase, "attemptIdentity", Requirement.MUST, 1, MAX_ATTEMPT_IDENTITY_LENGTH, "", "");
        e(fields, phase, "wireSchemaVersion", List.of("safe4.l2.edit.wire.v3"), "");
        a(fields, phase, "resolutions", 0, MAX_L2_CANDIDATES, Requirement.MAY);
        s(fields, phase, "resolutions[].candidateId", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        e(fields, phase, "resolutions[].status", List.of("CONFLICT", "PRESERVED", "PROCESSED", "UNPROCESSED"), "");
        a(fields, phase, "findingResolutions", 0, 48, Requirement.MAY);
        s(fields, phase, "findingResolutions[].errorId", Requirement.MUST, 1, 48, ID_PATTERN, "");
        e(fields, phase, "findingResolutions[].status", List.of("FIXED", "PRESERVED", "REJECTED", "UNRESOLVED"), "");
        a(fields, phase, "findingResolutions[].changeIds", 0, MAX_CHANGE_REFS, Requirement.MAY);
        s(fields, phase, "findingResolutions[].changeIds[]", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        a(fields, phase, "findingResolutions[].preserveIds", 0, MAX_CHANGE_REFS, Requirement.MAY);
        s(fields, phase, "findingResolutions[].preserveIds[]", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        s(fields, phase, "findingResolutions[].evidenceQuote", Requirement.CONDITIONAL, 0, MAX_QUOTE, "",
                "must quote the draft anchor when status=REJECTED");
        s(fields, phase, "findingResolutions[].reason", Requirement.CONDITIONAL, 0, MAX_TEXT, "",
                "required when status=REJECTED or UNRESOLVED");
        a(fields, phase, "findingResolutions[].occurrences", 0, 40, Requirement.MAY);
        s(fields, phase, "findingResolutions[].occurrences[].unitId", Requirement.MUST, 3, MAX_UNIT_REFERENCE_LENGTH, UNIT_PATTERN, "");
        s(fields, phase, "findingResolutions[].occurrences[].ref", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        a(fields, phase, "changes", 0, MAX_L2_CHANGES, Requirement.MAY);
        s(fields, phase, "changes[].changeId", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        s(fields, phase, "changes[].errorId", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        n(fields, phase, "changes[].line");
        sOptional(fields, phase, "changes[].before", Requirement.CONDITIONAL, 0, MAX_MODEL_TEXT, "",
                "app reconstructs it from line; if supplied, it must match a source-line substring after NFC/trim (mismatch is a warning)");
        s(fields, phase, "changes[].after", Requirement.CONDITIONAL, 0, MAX_MODEL_TEXT, "", "empty only for DELETE");
        s(fields, phase, "changes[].reason", Requirement.MUST, 1, MAX_MODEL_TEXT, NON_BLANK_PATTERN, "");
        b(fields, phase, "changes[].dialogue");
        e(fields, phase, "changes[].status", List.of("CLOSED", "REVERTED"), "");
        eOptional(fields, phase, "changes[].op", List.of("DELETE", "INSERT_AFTER", "MERGE_WITH_NEXT", "REPLACE"),
                "defaults to REPLACE when omitted");
        s(fields, phase, "changes[].speakerProof.speaker", Requirement.CONDITIONAL, 0, MAX_MODEL_TEXT, "", "required when dialogue=true");
        s(fields, phase, "changes[].speakerProof.listener", Requirement.CONDITIONAL, 0, MAX_MODEL_TEXT, "", "required when dialogue=true");
        s(fields, phase, "changes[].speakerProof.anchorBefore", Requirement.CONDITIONAL, 0, MAX_MODEL_TEXT, "", "required when dialogue=true");
        s(fields, phase, "changes[].speakerProof.anchorAfter", Requirement.CONDITIONAL, 0, MAX_MODEL_TEXT, "", "required when dialogue=true");
        o(fields, phase, "changes[].speakerProof", false, "required when dialogue=true");
        a(fields, phase, "preserved", 0, MAX_L2_PRESERVED, Requirement.MAY);
        s(fields, phase, "preserved[].preserveId", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        n(fields, phase, "preserved[].line");
        s(fields, phase, "preserved[].before", Requirement.CONDITIONAL, 0, MAX_MODEL_TEXT, "", "exact current line; may be empty only when the anchored line is empty");
        s(fields, phase, "preserved[].evidenceLimit", Requirement.MUST, 1, MAX_MODEL_TEXT, NON_BLANK_PATTERN, "");
        disposition(fields, phase);
    }

    private static void l3Reconcile(List<Field> fields) {
        String phase = L3_RECONCILE;
        s(fields, phase, "attemptIdentity", Requirement.MUST, 1, MAX_ATTEMPT_IDENTITY_LENGTH, "", "");
        e(fields, phase, "wireSchemaVersion", List.of("safe4.l3.reconcile.wire.v3"), "");
        a(fields, phase, "resolutions", 0, MAX_L3_CANDIDATES, Requirement.MAY);
        s(fields, phase, "resolutions[].candidateId", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        e(fields, phase, "resolutions[].status", List.of("CONFLICT", "PRESERVED", "PROCESSED", "UNPROCESSED"), "");
        a(fields, phase, "carriedResolutions", 0, 40, Requirement.MAY);
        n(fields, phase, "carriedResolutions[].index");
        e(fields, phase, "carriedResolutions[].status", List.of("FIXED", "PRESERVED", "REJECTED", "UNRESOLVED"), "");
        a(fields, phase, "carriedResolutions[].changeIds", 0, MAX_CHANGE_REFS, Requirement.MAY);
        s(fields, phase, "carriedResolutions[].changeIds[]", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        a(fields, phase, "carriedResolutions[].preserveIds", 0, MAX_CHANGE_REFS, Requirement.MAY);
        s(fields, phase, "carriedResolutions[].preserveIds[]", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        s(fields, phase, "carriedResolutions[].evidenceQuote", Requirement.CONDITIONAL, 0, MAX_QUOTE, "",
                "must quote the visible line when status=REJECTED");
        s(fields, phase, "carriedResolutions[].reason", Requirement.CONDITIONAL, 0, MAX_TEXT, "",
                "required when status=REJECTED or UNRESOLVED");
        // Change and preserve row fields use the same constraints as L2_EDIT.
        copyRows(fields, phase);
        a(fields, phase, "probes", 0, MAX_PROBES, Requirement.MAY);
        s(fields, phase, "probes[].probeId", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        e(fields, phase, "probes[].kind", List.of("COVERAGE", "REGRESSION"), "");
        a(fields, phase, "probes[].rawUnits", 1, MAX_RAW_UNITS_PER_FINDING, Requirement.MUST);
        s(fields, phase, "probes[].rawUnits[]", Requirement.MUST, 3, MAX_UNIT_REFERENCE_LENGTH, UNIT_PATTERN, "");
        n(fields, phase, "probes[].viStart");
        n(fields, phase, "probes[].viEnd");
        s(fields, phase, "probes[].scope", Requirement.MUST, 1, MAX_PROBE_TEXT, NON_BLANK_PATTERN, "");
        s(fields, phase, "probes[].contrast", Requirement.MUST, 1, MAX_PROBE_TEXT, NON_BLANK_PATTERN, "");
        s(fields, phase, "probes[].rawQuote", Requirement.MUST, 1, MAX_QUOTE, NON_BLANK_PATTERN, "");
        s(fields, phase, "probes[].viQuote", Requirement.MUST, 1, MAX_QUOTE, NON_BLANK_PATTERN, "");
        e(fields, phase, "probes[].verdict", List.of("CONFLICT", "DEFECT_FOUND", "NO_DEFECT", "PRESERVED"), "");
        s(fields, phase, "probes[].action", Requirement.MUST, 1, MAX_ID_LENGTH + 9, "^(?:NONE|CHANGE:[A-Za-z0-9][A-Za-z0-9._:/-]*|PRESERVE:[A-Za-z0-9][A-Za-z0-9._:/-]*)$", "");
        disposition(fields, phase);
    }

    private static void copyRows(List<Field> fields, String phase) {
        a(fields, phase, "changes", 0, MAX_L2_CHANGES, Requirement.MAY);
        s(fields, phase, "changes[].changeId", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        s(fields, phase, "changes[].errorId", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        n(fields, phase, "changes[].line");
        sOptional(fields, phase, "changes[].before", Requirement.CONDITIONAL, 0, MAX_MODEL_TEXT, "",
                "app reconstructs it from line; if supplied, it must match a source-line substring after NFC/trim (mismatch is a warning)");
        s(fields, phase, "changes[].after", Requirement.CONDITIONAL, 0, MAX_MODEL_TEXT, "", "empty only for DELETE");
        s(fields, phase, "changes[].reason", Requirement.MUST, 1, MAX_MODEL_TEXT, NON_BLANK_PATTERN, "");
        b(fields, phase, "changes[].dialogue");
        e(fields, phase, "changes[].status", List.of("CLOSED", "REVERTED"), "");
        eOptional(fields, phase, "changes[].op", List.of("DELETE", "INSERT_AFTER", "MERGE_WITH_NEXT", "REPLACE"),
                "defaults to REPLACE when omitted");
        s(fields, phase, "changes[].speakerProof.speaker", Requirement.CONDITIONAL, 0, MAX_MODEL_TEXT, "", "required when dialogue=true");
        s(fields, phase, "changes[].speakerProof.listener", Requirement.CONDITIONAL, 0, MAX_MODEL_TEXT, "", "required when dialogue=true");
        s(fields, phase, "changes[].speakerProof.anchorBefore", Requirement.CONDITIONAL, 0, MAX_MODEL_TEXT, "", "required when dialogue=true");
        s(fields, phase, "changes[].speakerProof.anchorAfter", Requirement.CONDITIONAL, 0, MAX_MODEL_TEXT, "", "required when dialogue=true");
        o(fields, phase, "changes[].speakerProof", false, "required when dialogue=true");
        a(fields, phase, "preserved", 0, MAX_L2_PRESERVED, Requirement.MAY);
        s(fields, phase, "preserved[].preserveId", Requirement.MUST, 1, MAX_ID_LENGTH, ID_PATTERN, "");
        n(fields, phase, "preserved[].line");
        s(fields, phase, "preserved[].before", Requirement.CONDITIONAL, 0, MAX_MODEL_TEXT, "", "exact current line; may be empty only when the anchored line is empty");
        s(fields, phase, "preserved[].evidenceLimit", Requirement.MUST, 1, MAX_MODEL_TEXT, NON_BLANK_PATTERN, "");
    }

    private static void finalRead(List<Field> fields, String phase) {
        s(fields, phase, "attemptIdentity", Requirement.MUST, 1, MAX_ATTEMPT_IDENTITY_LENGTH, "", "");
        e(fields, phase, "wireSchemaVersion", List.of(EditorialFinalRead.WIRE), "");
        s(fields, phase, "readSha256", Requirement.MUST, 64, 64, "^[0-9a-f]{64}$", "");
        a(fields, phase, "probeTails", 0, MAX_FINAL_READ_PROBES, Requirement.MAY);
        n(fields, phase, "probeTails[].line");
        s(fields, phase, "probeTails[].tail", Requirement.MUST, 1, MAX_FINAL_READ_TAIL, "", "must echo the app-provided non-empty line tail");
        e(fields, phase, "verdict", List.of("CLEAN", "DEFECTS"), "");
        a(fields, phase, "defects", 0, MAX_FINAL_READ_DEFECTS, Requirement.MAY);
        n(fields, phase, "defects[].line");
        s(fields, phase, "defects[].quote", Requirement.MUST, 1, MAX_QUOTE, NON_BLANK_PATTERN, "");
        e(fields, phase, "defects[].type", List.of("ADDRESS_PROFILE", "ADDITION", "GLOSSARY", "MEANING", "NEGATION",
                "NUMBER", "OMISSION", "SPEAKER_LISTENER", "STRUCTURE", "UNTRANSLATED"), "");
        s(fields, phase, "defects[].note", Requirement.MAY, 0, MAX_TEXT, "", "");
    }

    private static void disposition(List<Field> fields, String phase) {
        e(fields, phase, "disposition.disposition", List.of("CONTINUE", "PRESERVE_DRAFT", "STOP"), "");
        s(fields, phase, "disposition.reasonCode", Requirement.MUST, 1, 32,
                "^[A-Za-z0-9][A-Za-z0-9 ._:/;()/-]{0,31}$", "");
        e(fields, phase, "disposition.stopClass", List.of("CONTENT_BLOCKED", "INPUT_REQUIRED", "NONE"), "");
    }

    private static void s(List<Field> fields, String phase, String path, Requirement requirement,
                          int min, int max, String pattern, String condition) {
        fields.add(new Field(phase, path, Kind.STRING, requirement, true, min, max, -1, -1,
                pattern, List.of(), condition));
    }

    private static void sOptional(List<Field> fields, String phase, String path, Requirement requirement,
                                  int min, int max, String pattern, String condition) {
        fields.add(new Field(phase, path, Kind.STRING, requirement, false, min, max, -1, -1,
                pattern, List.of(), condition));
    }

    private static void e(List<Field> fields, String phase, String path, List<String> values, String condition) {
        fields.add(new Field(phase, path, Kind.ENUM, Requirement.MUST, true, 1, 240, -1, -1,
                "", values, condition));
    }

    private static void eOptional(List<Field> fields, String phase, String path, List<String> values, String condition) {
        fields.add(new Field(phase, path, Kind.ENUM, Requirement.MUST, false, 1, 240, -1, -1,
                "", values, condition));
    }

    private static void a(List<Field> fields, String phase, String path, int minItems, int maxItems, Requirement requirement) {
        fields.add(new Field(phase, path, Kind.ARRAY, requirement, true, -1, -1, minItems, maxItems,
                "", List.of(), ""));
    }

    private static void n(List<Field> fields, String phase, String path) {
        fields.add(new Field(phase, path, Kind.INTEGER, Requirement.MUST, true, -1, -1, -1, -1,
                "", List.of(), ""));
    }

    private static void i(List<Field> fields, String phase, String path) { n(fields, phase, path); }

    private static void b(List<Field> fields, String phase, String path) {
        fields.add(new Field(phase, path, Kind.BOOLEAN, Requirement.MUST, true, -1, -1, -1, -1,
                "", List.of(), ""));
    }

    private static void o(List<Field> fields, String phase, String path, boolean keyRequired, String condition) {
        fields.add(new Field(phase, path, Kind.OBJECT, Requirement.CONDITIONAL, keyRequired, -1, -1, -1, -1,
                "", List.of(), condition));
    }
}
