package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * L1 Error Ledger, current contract revision {@code L1_LEDGER_V6}. Four notions stay apart: a RAW unit/occurrence is a
 * part of the source (the app's {@link EditorialRawInventory}); a candidate is a suspicion raised by the blind RAW
 * pass; a finding is a defect with evidence in the DRAFT; a change is an edit (L2/L3). The model supplies
 * judgement and the app checks everything checkable: every unit id exists, coverage ranges close over the whole
 * inventory exactly once, quotes occur in the anchored text, draft anchors are inside the DRAFT, ids are unique,
 * references resolve, every candidate is resolved. Nothing here is a statement that the meaning is right.
 */
public final class EditorialL1Ledger {
    public static final String RAW_WIRE = "safe4.l1.raw-ledger.wire.v3";
    public static final String RECONCILE_WIRE = "safe4.l1.reconcile-ledger.wire.v3";

    /**
     * Hard byte cap of one response. The output-token cap (16,384) is what bounds cost; at roughly 4 bytes per token
     * the largest response a provider can return is about 64 KiB, so a response above this cap is a typed failure
     * and is never truncated. The count and text limits below keep a realistic call far under it.
     */
    public static final int MAX_WIRE_BYTES = 65_536;
    public static final int MAX_FINDINGS_PER_CALL = EditorialFieldSpec.MAX_FINDINGS;
    public static final int MAX_CANDIDATES_PER_CALL = EditorialFieldSpec.MAX_L1_CANDIDATES;
    public static final int MAX_SPEAKER_RECORDS_PER_CALL = EditorialFieldSpec.MAX_SPEAKERS;
    public static final int MAX_PROTECTED_SPANS_PER_CALL = EditorialFieldSpec.MAX_PROTECTED_SPANS;
    public static final int MAX_RANGES = EditorialFieldSpec.MAX_RANGES;
    public static final int MAX_TEXT = EditorialFieldSpec.MAX_TEXT;
    public static final int MAX_QUOTE = EditorialFieldSpec.MAX_QUOTE;
    public static final int MAX_REFS = EditorialFieldSpec.MAX_EVIDENCE_REFS;
    public static final int MAX_RAW_UNITS_PER_FINDING = EditorialFieldSpec.MAX_RAW_UNITS_PER_FINDING;
    public static final int MAX_OCCURRENCE_UNITS = EditorialFieldSpec.MAX_OCCURRENCES;

    public static final Set<String> FINDING_TYPES = Set.of("UNTRANSLATED", "MEANING", "OMISSION", "ADDITION", "NUMBER",
            "NEGATION", "GLOSSARY", "ADDRESS_PROFILE", "SPEAKER_LISTENER", "STRUCTURE");
    public static final Set<String> SEVERITIES = Set.of("MINOR", "MAJOR", "CRITICAL");
    public static final Set<String> DISPOSITIONS = Set.of("OPEN", "PRESERVED");
    public static final Set<String> CANDIDATE_LEDGERS = Set.of("UNIT", "TG", "SR", "RC", "PAIR", "SPEAKER");
    public static final Set<String> CANDIDATE_STATUSES = Set.of("PROCESSED", "PRESERVED", "UNPROCESSED", "CONFLICT");
    public static final Set<String> RANGE_STATUSES = Set.of("PROCESSED", "PRESERVED");
    public static final Set<String> PROTECTED_SOURCES = Set.of("PRONOUN_ROW", "GLOSSARY_ROW", "L1_PROOF", "SPEAKER_PROOF");

    public record Candidate(String candidateId, String ledger, String unitId, String note) { }

    public record Resolution(String candidateId, String status, String findingRef) { }

    /** Where in the DRAFT: lines {@code start..end} (1-based), or a missing target inserted after line {@code after}. */
    public record DraftAnchor(String kind, int start, int end, int after) {
        public static DraftAnchor lines(int start, int end) { return new DraftAnchor("LINES", start, end, 0); }
        public static DraftAnchor missingAfter(int after) { return new DraftAnchor("MISSING", 0, 0, after); }
    }

    public record Finding(String errorId, String type, String severity, List<String> rawUnits, DraftAnchor draft,
                          String rawQuote, String draftQuote, String observation, String expectedMeaning,
                          List<String> evidenceRefs, List<String> candidateIds, List<String> occurrenceUnits,
                          String disposition, String evidenceLimit) {
        public Finding {
            rawUnits = List.copyOf(rawUnits);
            evidenceRefs = List.copyOf(evidenceRefs);
            candidateIds = List.copyOf(candidateIds);
            occurrenceUnits = List.copyOf(occurrenceUnits);
        }
    }

    public record SpeakerRecord(String unitId, String speaker, String listener, String basis) { }

    public record ProtectedSpan(String spanId, int start, int end, String source, String reason) { }

    public record Disposition(String kind, String reasonCode, String stopClass) { }

    public record RawPass(List<EditorialRawInventory.Range> coverage, List<Candidate> candidates) {
        public RawPass { coverage = List.copyOf(coverage); candidates = List.copyOf(candidates); }
    }

    public record ReconcilePass(List<EditorialRawInventory.Range> coverage, List<Resolution> resolutions,
                                List<Finding> findings, List<SpeakerRecord> speakerRecords,
                                List<ProtectedSpan> protectedSpans, Disposition disposition,
                                int duplicateReferencesRemoved, int draftAnchorsDerivedFromQuote,
                                int maxDraftAnchorDeviation, List<String> speakerRecordsDropped,
                                List<String> bookkeepingNotes) {
        public ReconcilePass(List<EditorialRawInventory.Range> coverage, List<Resolution> resolutions,
                             List<Finding> findings, List<SpeakerRecord> speakerRecords,
                             List<ProtectedSpan> protectedSpans, Disposition disposition,
                             int duplicateReferencesRemoved, int draftAnchorsDerivedFromQuote, int maxDraftAnchorDeviation,
                             List<String> speakerRecordsDropped) {
            this(coverage, resolutions, findings, speakerRecords, protectedSpans, disposition, duplicateReferencesRemoved,
                    draftAnchorsDerivedFromQuote, maxDraftAnchorDeviation, speakerRecordsDropped, List.of());
        }
        public ReconcilePass(List<EditorialRawInventory.Range> coverage, List<Resolution> resolutions,
                             List<Finding> findings, List<SpeakerRecord> speakerRecords,
                             List<ProtectedSpan> protectedSpans, Disposition disposition,
                             int duplicateReferencesRemoved, int draftAnchorsDerivedFromQuote, int maxDraftAnchorDeviation) {
            this(coverage, resolutions, findings, speakerRecords, protectedSpans, disposition, duplicateReferencesRemoved,
                    draftAnchorsDerivedFromQuote, maxDraftAnchorDeviation, List.of(), List.of());
        }
        public ReconcilePass(List<EditorialRawInventory.Range> coverage, List<Resolution> resolutions,
                             List<Finding> findings, List<SpeakerRecord> speakerRecords,
                             List<ProtectedSpan> protectedSpans, Disposition disposition) {
            this(coverage, resolutions, findings, speakerRecords, protectedSpans, disposition, 0, 0, 0);
        }
        public ReconcilePass(List<EditorialRawInventory.Range> coverage, List<Resolution> resolutions,
                             List<Finding> findings, List<SpeakerRecord> speakerRecords,
                             List<ProtectedSpan> protectedSpans, Disposition disposition, int duplicateReferencesRemoved) {
            this(coverage, resolutions, findings, speakerRecords, protectedSpans, disposition, duplicateReferencesRemoved, 0, 0);
        }
        public ReconcilePass {
            coverage = List.copyOf(coverage);
            resolutions = List.copyOf(resolutions);
            findings = List.copyOf(findings);
            speakerRecords = List.copyOf(speakerRecords);
            protectedSpans = List.copyOf(protectedSpans);
            speakerRecordsDropped = List.copyOf(speakerRecordsDropped);
            bookkeepingNotes = List.copyOf(bookkeepingNotes);
        }
    }

    /**
     * Bounded offline diagnostics for a reconcile response.  The production parser remains fail-fast; this result is
     * only used by replay tooling to inspect independent finding rows before dependent arrays are considered.
     */
    public record ReconcileDiagnostic(List<String> errors, List<String> skipped, List<String> completed,
                                      List<String> speakerRecordsDropped) {
        public ReconcileDiagnostic(List<String> errors, List<String> skipped, List<String> completed) {
            this(errors, skipped, completed, List.of());
        }
        public ReconcileDiagnostic {
            speakerRecordsDropped = List.copyOf(speakerRecordsDropped);
            errors = List.copyOf(errors);
            skipped = List.copyOf(skipped);
            completed = List.copyOf(completed);
        }
    }

    /**
     * Definitions: {@code uniqueFindingCount} counts findings (error ids), {@code occurrenceCount} counts every
     * place a finding applies (the anchor plus its extra occurrence units), candidates are never findings.
     */
    public record Metrics(int uniqueFindingCount, int occurrenceCount, int openFindingCount, int preservedFindingCount,
                          int candidateCount, int unprocessedCandidateCount, int conflictCandidateCount,
                          int speakerRecordCount, int protectedSpanCount, int unitCount, int excludedLineCount) { }

    private EditorialL1Ledger() { }

    // ---- DRAFT lines (same splitting rules as the RAW inventory) ----

    public static List<String> draftLines(byte[] draft) {
        String text = new String(draft, StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '﻿') text = text.substring(1);
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n", -1)) lines.add(line.endsWith("\r") ? line.substring(0, line.length() - 1) : line);
        return lines;
    }

    // ---- RAW pass ----

    public static RawPass parseRawPass(byte[] bytes, String attemptIdentity, EditorialRawInventory.Inventory inventory) {
        return parseRawPass(bytes, attemptIdentity, inventory, RAW_WIRE);
    }

    /**
     * Same grammar under another wire label: the blind RAW passes of L2 and L3 report coverage over the same
     * inventory and raise sparse candidates exactly like the L1 RAW pass.
     */
    public static RawPass parseRawPass(byte[] bytes, String attemptIdentity, EditorialRawInventory.Inventory inventory,
                                       String wireLabel) {
        WireNotes.begin();
        Map<String, Object> root = rootOf(bytes, attemptIdentity, wireLabel,
                Set.of("wireSchemaVersion", "attemptIdentity", "coverage", "candidates"), Set.of("candidates"));
        String phase = rawPhase(wireLabel);
        List<EditorialRawInventory.Range> coverage = coverage(root.get("coverage"), inventory, phase);
        List<Object> rows = EditorialCanonicalJson.array(root.get("candidates"), "candidates");
        if (rows.size() > MAX_CANDIDATES_PER_CALL) throw bad("L1_CANDIDATE_LIMIT_EXCEEDED");
        List<Candidate> candidates = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < rows.size(); index++) {
            String path = "candidates." + index;
            Map<String, Object> row = object(rows.get(index), path);
            keys(row, Set.of("candidateId", "ledger", "unitId", "note"), path, Set.of("note"));
            row = withDefaults(row, path, "note", "");
            String id = id(row, "candidateId", path + ".candidateId", phase);
            if (!ids.add(id)) throw bad("L1_CANDIDATE_ID_DUPLICATE", path + ".candidateId");
            String ledger = enumOf(row, "ledger", CANDIDATE_LEDGERS, path + ".ledger", phase);
            String unit = EditorialUnitReference.resolve(row.get("unitId"), inventory, path + ".unitId");
            String note = fieldStr(phase, row, "note", path + ".note");
            candidates.add(new Candidate(id, ledger, unit, note));
        }
        return new RawPass(coverage, candidates);
    }

    // ---- RECONCILE pass ----

    public static ReconcilePass parseReconcile(byte[] bytes, String attemptIdentity, EditorialRawInventory.Inventory inventory,
                                               List<String> draftLines, List<Candidate> rawCandidates) {
        WireNotes.begin();
        Map<String, Object> root = rootOf(bytes, attemptIdentity, RECONCILE_WIRE, Set.of("wireSchemaVersion", "attemptIdentity",
                "coverage", "resolutions", "findings", "speakerRecords", "protectedSpans", "disposition"),
                Set.of("resolutions", "findings", "speakerRecords", "protectedSpans"));
        Disposition disposition = disposition(object(root.get("disposition"), "disposition"), "disposition", EditorialFieldSpec.L1_RECONCILE);
        boolean stop = "STOP".equals(disposition.kind());
        List<EditorialRawInventory.Range> coverage = stop && EditorialCanonicalJson.array(root.get("coverage"), "coverage").isEmpty()
                ? List.of() : coverage(root.get("coverage"), inventory, EditorialFieldSpec.L1_RECONCILE);

        List<Finding> findings = new ArrayList<>();
        EditorialReferenceNormalization.Counter normalizations = new EditorialReferenceNormalization.Counter();
        Set<String> errorIds = new HashSet<>();
        Set<String> knownCandidates = new HashSet<>();
        for (Candidate candidate : rawCandidates) knownCandidates.add(candidate.candidateId());
        List<Object> findingRows = EditorialCanonicalJson.array(root.get("findings"), "findings");
        if (findingRows.size() > MAX_FINDINGS_PER_CALL) throw bad("L1_FINDING_LIMIT_EXCEEDED");
        for (int index = 0; index < findingRows.size(); index++) {
            String path = "findings." + index;
            Finding finding = finding(object(findingRows.get(index), path), inventory, draftLines, knownCandidates,
                    path, EditorialFieldSpec.L1_RECONCILE, normalizations);
            if (!errorIds.add(finding.errorId())) throw bad("L1_ERROR_ID_DUPLICATE", path + ".errorId");
            findings.add(finding);
        }

        Map<String, Resolution> resolved = new LinkedHashMap<>();
        List<Object> resolutionRows = EditorialCanonicalJson.array(root.get("resolutions"), "resolutions");
        for (int index = 0; index < resolutionRows.size(); index++) {
            String path = "resolutions." + index;
            Map<String, Object> row = object(resolutionRows.get(index), path);
            keys(row, Set.of("candidateId", "status", "findingRef"), path);
            String candidateId = id(row, "candidateId", path + ".candidateId", EditorialFieldSpec.L1_RECONCILE);
            if (!knownCandidates.contains(candidateId) || resolved.containsKey(candidateId)) throw bad("L1_RESOLUTION_ID_INVALID", path + ".candidateId");
            String status = enumOf(row, "status", CANDIDATE_STATUSES, path + ".status", EditorialFieldSpec.L1_RECONCILE);
            String ref = fieldStr(EditorialFieldSpec.L1_RECONCILE, row, "findingRef", path + ".findingRef");
            if (!ref.isEmpty() && !errorIds.contains(ref)) throw bad("L1_FINDING_REF_UNKNOWN", path + ".findingRef");
            resolved.put(candidateId, new Resolution(candidateId, status, ref));
        }
        if (!stop) {
            for (String id : knownCandidates) if (!resolved.containsKey(id)) throw bad("L1_CANDIDATE_UNRESOLVED", "resolutions");
        }

        List<SpeakerRecord> speakers = new ArrayList<>();
        List<Object> speakerRows = EditorialCanonicalJson.array(root.get("speakerRecords"), "speakerRecords");
        for (int index = 0; index < speakerRows.size(); index++) {
            String path = "speakerRecords." + index;
            // speakerRecords is an optional side note nothing downstream reads: a record that cannot be read as a whole
            // (bad reference, blank label, wrong shape) is dropped and counted with the path; the app never guesses
            if (speakers.size() >= MAX_SPEAKER_RECORDS_PER_CALL) {
                normalizations.speakerRecordDropped(path);
                continue;
            }
            try {
                Map<String, Object> row = withDefaults(object(speakerRows.get(index), path), path, "listener", "");
                keys(row, Set.of("unitId", "speaker", "listener", "basis"), path, Set.of("listener"));
                String unit = EditorialUnitReference.resolve(row.get("unitId"), inventory, path + ".unitId");
                speakers.add(new SpeakerRecord(unit,
                        fieldStr(EditorialFieldSpec.L1_RECONCILE, row, "speaker", path + ".speaker"),
                        fieldStr(EditorialFieldSpec.L1_RECONCILE, row, "listener", path + ".listener"),
                        fieldStr(EditorialFieldSpec.L1_RECONCILE, row, "basis", path + ".basis")));
            } catch (WireViolation invalid) {
                normalizations.speakerRecordDropped(invalid.path());
            }
        }

        List<ProtectedSpan> spans = new ArrayList<>();
        Set<String> spanIds = new HashSet<>();
        List<Object> spanRows = EditorialCanonicalJson.array(root.get("protectedSpans"), "protectedSpans");
        if (spanRows.size() > MAX_PROTECTED_SPANS_PER_CALL) throw bad("L1_PROTECTED_LIMIT_EXCEEDED");
        for (int index = 0; index < spanRows.size(); index++) {
            String path = "protectedSpans." + index;
            Map<String, Object> row = object(spanRows.get(index), path);
            keys(row, Set.of("spanId", "start", "end", "source", "reason"), path, Set.of("reason"));
            row = withDefaults(row, path, "reason", "");
            // Z1 BOOKKEEPING: the span id is a label, the range is clamped to the draft, a missing reason is stated as such
            String spanId;
            Object rawSpanId = row.get("spanId");
            if (rawSpanId instanceof String text && EditorialP5RawWireContract.token(text, EditorialFieldSpec.MAX_ID_LENGTH)) {
                spanId = text;
            } else {
                spanId = "SPAN-" + (index + 1);
                WireNotes.note("protectedSpanIdAssigned", path + ".spanId");
            }
            if (!spanIds.add(spanId)) {
                String candidate = spanId;
                int suffix = 2;
                while (!spanIds.add(candidate = spanId + "-" + suffix)) suffix++;
                spanId = candidate;
                WireNotes.note("protectedSpanIdAssigned", path + ".spanId");
            }
            int start = intOf(row, "start", path + ".start");
            int end = intOf(row, "end", path + ".end");
            if (end < start) {
                int swap = start;
                start = end;
                end = swap;
                WireNotes.note("protectedSpanRangeAdjusted", path);
            }
            if (end < 1 || start > draftLines.size()) {
                WireNotes.note("protectedSpanDropped", path);
                continue;
            }
            if (start < 1 || end > draftLines.size()) {
                start = Math.max(1, start);
                end = Math.min(draftLines.size(), end);
                WireNotes.note("protectedSpanRangeAdjusted", path);
            }
            String reasonText;
            if (!(row.get("reason") instanceof String given) || given.isBlank()) {
                reasonText = "reason not stated";
                WireNotes.note("protectedSpanReasonDefaulted", path + ".reason");
            } else {
                reasonText = fieldStr(EditorialFieldSpec.L1_RECONCILE, row, "reason", path + ".reason");
            }
            spans.add(new ProtectedSpan(spanId, start, end,
                    enumOf(row, "source", PROTECTED_SOURCES, path + ".source", EditorialFieldSpec.L1_RECONCILE),
                    reasonText));
        }
        // a line the report protects cannot also be the anchor of an open defect
        Set<Integer> protectedNumbers = protectedLines(spans);
        for (Finding finding : findings) {
            if (!"OPEN".equals(finding.disposition()) || !"LINES".equals(finding.draft().kind())) continue;
            for (int line = finding.draft().start(); line <= finding.draft().end(); line++) {
                if (protectedNumbers.contains(line)) throw bad("L1_PROTECTED_OVERLAPS_OPEN_FINDING");
            }
        }
        return new ReconcilePass(coverage, new ArrayList<>(resolved.values()), findings, speakers, spans, disposition,
                normalizations.duplicateReferencesRemoved(), normalizations.draftAnchorsDerivedFromQuote(),
                normalizations.maxDraftAnchorDeviation(), normalizations.speakerRecordsDropped(), WireNotes.drain());
    }

    /**
     * Diagnose independent {@code findings[]} rows while retaining the production validation rules.  This deliberately
     * has a narrow scope: root/disposition/coverage are checked first, each finding is checked independently through
     * {@link #finding(Map, EditorialRawInventory.Inventory, List, Set, String, String,
     * EditorialReferenceNormalization.Counter)}, and the dependent resolution/tail checks are skipped when any
     * finding is invalid.  When all findings pass, the complete production parser is invoked so no second validator
     * exists for the rest of the wire.
     */
    static ReconcileDiagnostic diagnoseReconcile(byte[] bytes, String attemptIdentity,
                                                 EditorialRawInventory.Inventory inventory, List<String> draftLines,
                                                 List<Candidate> rawCandidates) {
        Map<String, Object> root = rootOf(bytes, attemptIdentity, RECONCILE_WIRE, Set.of("wireSchemaVersion", "attemptIdentity",
                "coverage", "resolutions", "findings", "speakerRecords", "protectedSpans", "disposition"));
        Disposition disposition = disposition(object(root.get("disposition"), "disposition"), "disposition",
                EditorialFieldSpec.L1_RECONCILE);
        boolean stop = "STOP".equals(disposition.kind());
        if (stop && EditorialCanonicalJson.array(root.get("coverage"), "coverage").isEmpty()) {
            // A STOP response may omit review coverage.  The production rule is explicit and is completed here.
        } else {
            coverage(root.get("coverage"), inventory, EditorialFieldSpec.L1_RECONCILE);
        }

        List<String> completed = new ArrayList<>(List.of("root", "disposition", "coverage"));
        List<String> skipped = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<Object> findingRows = EditorialCanonicalJson.array(root.get("findings"), "findings");
        if (findingRows.size() > MAX_FINDINGS_PER_CALL) throw bad("L1_FINDING_LIMIT_EXCEEDED");

        EditorialReferenceNormalization.Counter normalizations = new EditorialReferenceNormalization.Counter();
        Set<String> errorIds = new HashSet<>();
        Set<String> knownCandidates = new HashSet<>();
        for (Candidate candidate : rawCandidates) knownCandidates.add(candidate.candidateId());
        for (int index = 0; index < findingRows.size(); index++) {
            String path = "findings." + index;
            try {
                Finding parsed = finding(object(findingRows.get(index), path), inventory, draftLines, knownCandidates,
                        path, EditorialFieldSpec.L1_RECONCILE, normalizations);
                if (!errorIds.add(parsed.errorId())) throw bad("L1_ERROR_ID_DUPLICATE", path + ".errorId");
                completed.add(path);
            } catch (RuntimeException invalid) {
                addDiagnosticError(errors, invalid, "L1_WIRE_PARSE_FAILED", path);
            }
        }
        completed.add("findings");
        if (!errors.isEmpty()) {
            // Resolutions reference finding ids and candidates; the remaining arrays are outside this bounded scope.
            skipped.add("resolutions");
            skipped.add("speakerRecords");
            skipped.add("protectedSpans");
            skipped.add("postFindingInvariants");
            return new ReconcileDiagnostic(errors, skipped, completed);
        }

        try {
            ReconcilePass pass = parseReconcile(bytes, attemptIdentity, inventory, draftLines, rawCandidates);
            completed.add("resolutions");
            completed.add("speakerRecords");
            completed.add("protectedSpans");
            completed.add("postFindingInvariants");
            return new ReconcileDiagnostic(List.of(), List.of(), completed, pass.speakerRecordsDropped());
        } catch (RuntimeException invalid) {
            addDiagnosticError(errors, invalid, "L1_WIRE_PARSE_FAILED", "root");
            skipped.add("remainingItems");
            return new ReconcileDiagnostic(errors, skipped, completed);
        }
    }

    private static void addDiagnosticError(List<String> errors, RuntimeException invalid, String fallbackCode, String fallbackPath) {
        String safe = WireViolation.safeMessage(invalid, fallbackCode);
        if (safe.indexOf(':') < 0) safe = fallbackCode + ":" + fallbackPath;
        if (!errors.contains(safe)) errors.add(safe);
    }

    /** DRAFT line numbers covered by the protected spans. */
    public static Set<Integer> protectedLines(List<ProtectedSpan> spans) {
        Set<Integer> lines = new java.util.TreeSet<>();
        for (ProtectedSpan span : spans) for (int line = span.start(); line <= span.end(); line++) lines.add(line);
        return lines;
    }

    static Finding finding(Map<String, Object> row, EditorialRawInventory.Inventory inventory,
                           List<String> draftLines, Set<String> knownCandidates, String path, String phase,
                           EditorialReferenceNormalization.Counter normalizations) {
        keys(row, Set.of("errorId", "type", "severity", "rawUnits", "draft", "rawQuote", "draftQuote",
                "observation", "expectedMeaning", "evidenceRefs", "candidateIds", "occurrenceUnits", "disposition",
                "evidenceLimit"), path, Set.of("evidenceRefs", "candidateIds", "occurrenceUnits", "evidenceLimit"));
        row = withDefaults(row, path, "evidenceRefs", new ArrayList<Object>(), "candidateIds", new ArrayList<Object>(),
                "occurrenceUnits", new ArrayList<Object>(), "evidenceLimit", "");
        String errorId = id(row, "errorId", path + ".errorId", phase);
        String type = enumOf(row, "type", FINDING_TYPES, path + ".type", phase);
        String severity = enumOf(row, "severity", SEVERITIES, path + ".severity", phase);
        List<String> rawUnits = EditorialUnitReference.resolveList(row.get("rawUnits"), path + ".rawUnits", MAX_RAW_UNITS_PER_FINDING, inventory);
        if (rawUnits.isEmpty()) throw bad("L1_FINDING_RAW_ANCHOR_REQUIRED", path + ".rawUnits");
        for (int index = 0; index < rawUnits.size(); index++) if (!inventory.has(rawUnits.get(index))) throw bad("L1_UNIT_UNKNOWN", path + ".rawUnits." + index);
        rawUnits = normalizations.distinct(rawUnits);

        Map<String, Object> draftRow = object(row.get("draft"), path + ".draft");
        String kind = fieldStr(phase, draftRow, "kind", path + ".draft.kind");
        DraftAnchor anchor;
        // one object shape for both kinds (a strict response schema cannot express a union); unused numbers are 0
        keys(draftRow, Set.of("kind", "start", "end", "after"), path + ".draft", Set.of("start", "end", "after"));
        if ("LINES".equals(kind)) {
            // Z1 BOOKKEEPING: start/end are hints and `after` belongs to MISSING; absent or unused values are ignored
            int start = draftRow.containsKey("start") ? intOf(draftRow, "start", path + ".draft.start") : 0;
            int end = draftRow.containsKey("end") ? intOf(draftRow, "end", path + ".draft.end") : 0;
            if (draftRow.containsKey("after") && intOf(draftRow, "after", path + ".draft.after") != 0) {
                WireNotes.note("unusedAnchorFieldIgnored", path + ".draft.after");
            }
            // the numbers are only a hint: the app derives the anchor from draftQuote below
            int hintStart = Math.max(1, Math.min(start, draftLines.size()));
            int hintEnd = Math.max(hintStart, Math.min(end, draftLines.size()));
            anchor = DraftAnchor.lines(hintStart, hintEnd);
        } else if ("MISSING".equals(kind)) {
            int after = intOf(draftRow, "after", path + ".draft.after");
            if (draftRow.containsKey("start") && intOf(draftRow, "start", path + ".draft.start") != 0) {
                WireNotes.note("unusedAnchorFieldIgnored", path + ".draft.start");
            }
            if (draftRow.containsKey("end") && intOf(draftRow, "end", path + ".draft.end") != 0) {
                WireNotes.note("unusedAnchorFieldIgnored", path + ".draft.end");
            }
            if (after < 0 || after > draftLines.size()) {
                // the insertion position is a hint about where the omission shows; keep it inside the draft
                after = Math.max(0, Math.min(after, draftLines.size()));
                WireNotes.note("anchorClamped", path + ".draft.after");
            }
            anchor = DraftAnchor.missingAfter(after);
        } else {
            throw bad("L1_DRAFT_ANCHOR_KIND_INVALID", path + ".draft.kind");
        }

        String rawQuote = fieldStr(phase, row, "rawQuote", path + ".rawQuote");
        boolean quoted = false;
        for (String unit : rawUnits) quoted |= EditorialQuoteMatcher.containsRaw(inventory.unit(unit).text(), rawQuote);
        if (!quoted) throw bad("L1_RAW_QUOTE_NOT_IN_ANCHOR", path + ".rawQuote");
        String draftQuote = fieldStr(phase, row, "draftQuote", path + ".draftQuote");
        if ("LINES".equals(anchor.kind())) {
            if (draftQuote.isEmpty()) throw bad("L1_DRAFT_QUOTE_REQUIRED", path + ".draftQuote");
            anchor = deriveDraftAnchor(anchor, draftLines, draftQuote, path + ".draftQuote", normalizations);
        } else if (!draftQuote.isEmpty()) {
            throw bad("L1_DRAFT_QUOTE_FORBIDDEN_FOR_MISSING", path + ".draftQuote");
        }

        String observation = fieldStr(phase, row, "observation", path + ".observation");
        String expected = fieldStr(phase, row, "expectedMeaning", path + ".expectedMeaning");
        List<String> refs = lenientTokens(row.get("evidenceRefs"), path + ".evidenceRefs",
                EditorialFieldSpec.MAX_EVIDENCE_REFS, EditorialFieldSpec.MAX_ID_LENGTH);
        refs = normalizations.distinct(refs);
        List<String> candidateIds = stringList(row.get("candidateIds"), path + ".candidateIds",
                EditorialFieldSpec.MAX_CANDIDATE_REFS, EditorialFieldSpec.MAX_ID_LENGTH);
        for (int index = 0; index < candidateIds.size(); index++) if (!knownCandidates.contains(candidateIds.get(index))) throw bad("L1_CANDIDATE_REF_UNKNOWN", path + ".candidateIds." + index);
        candidateIds = normalizations.distinct(candidateIds);
        List<String> occurrences = lenientUnits(row.get("occurrenceUnits"), path + ".occurrenceUnits",
                MAX_OCCURRENCE_UNITS, inventory);
        Set<String> seen = new HashSet<>(rawUnits);
        List<String> normalizedOccurrences = new ArrayList<>();
        for (String unit : occurrences) if (normalizations.addReference(seen, unit)) normalizedOccurrences.add(unit);
        occurrences = List.copyOf(normalizedOccurrences);
        String disposition = enumOf(row, "disposition", DISPOSITIONS, path + ".disposition", phase);
        String limit = fieldStr(phase, row, "evidenceLimit", path + ".evidenceLimit");
        if ("PRESERVED".equals(disposition) && limit.isEmpty()) throw bad("L1_PRESERVED_NEEDS_EVIDENCE_LIMIT", path + ".evidenceLimit");
        return new Finding(errorId, type, severity, rawUnits, anchor, rawQuote, draftQuote, observation, expected, refs,
                candidateIds, occurrences, disposition, limit);
    }

    /** Largest distance between the hinted line and the quote line that still disambiguates repeated quotes. */
    static final int DRAFT_HINT_WINDOW = 3;

    /**
     * The DRAFT anchor is a fact about the quote, not about the model's line counting: the line carrying
     * {@code draftQuote} is the anchor and the hinted numbers only choose between repeated occurrences. A hint that
     * already contains the quote line is kept as it is; otherwise the span moves to the quote line with the hinted length.
     */
    static DraftAnchor deriveDraftAnchor(DraftAnchor hint, List<String> draftLines, String draftQuote, String quotePath,
                                         EditorialReferenceNormalization.Counter normalizations) {
        int size = draftLines.size();
        int length = hint.end() - hint.start();
        List<Integer> lines = new ArrayList<>();
        for (int line = 1; line <= size; line++) {
            if (EditorialQuoteMatcher.contains(draftLines.get(line - 1), draftQuote)) lines.add(line);
        }
        if (lines.isEmpty() && length > 0) {
            // a quote that crosses a line break starts on the line where the hinted span would reach it
            for (int line = 1; line <= size; line++) {
                int last = Math.min(size, line + length);
                if (last == line) continue;
                StringBuilder window = new StringBuilder();
                for (int i = line; i <= last; i++) window.append(draftLines.get(i - 1)).append('\n');
                if (!EditorialQuoteMatcher.contains(window.toString(), draftQuote)) continue;
                StringBuilder tail = new StringBuilder();
                for (int i = line + 1; i <= last; i++) tail.append(draftLines.get(i - 1)).append('\n');
                if (!EditorialQuoteMatcher.contains(tail.toString(), draftQuote)) lines.add(line);
            }
        }
        if (lines.isEmpty()) throw bad("L1_DRAFT_QUOTE_NOT_IN_ANCHOR", quotePath);
        int chosen;
        if (lines.size() == 1) {
            chosen = lines.get(0);
        } else {
            int best = Integer.MAX_VALUE;
            int bestCount = 0;
            chosen = -1;
            for (int line : lines) {
                int distance = line < hint.start() ? hint.start() - line : (line > hint.end() ? line - hint.end() : 0);
                if (distance > DRAFT_HINT_WINDOW) continue;
                if (distance < best) { best = distance; bestCount = 1; chosen = line; }
                else if (distance == best) bestCount++;
            }
            if (chosen < 0 || bestCount != 1) throw bad("L1_DRAFT_QUOTE_AMBIGUOUS", quotePath);
        }
        if (chosen >= hint.start() && chosen <= hint.end()) return hint;
        normalizations.draftAnchorDerived(chosen < hint.start() ? hint.start() - chosen : chosen - hint.end());
        return DraftAnchor.lines(chosen, Math.min(size, chosen + length));
    }

    private static Disposition disposition(Map<String, Object> row, String path, String phase) {
        keys(row, Set.of("disposition", "reasonCode", "stopClass"), path, Set.of("reasonCode", "stopClass"));
        String kind = fieldStr(phase, row, "disposition", path + ".disposition");
        if ("CONTINUE".equals(kind) || "PRESERVE_DRAFT".equals(kind)) {
            // Z1 BOOKKEEPING: a continuing disposition has no stop class and its reason is only a label
            String reason = "";
            Object rawReason = row.get("reasonCode");
            if (rawReason instanceof String text && !text.isEmpty()) {
                if (text.length() <= 32 && EditorialP5RawWireContract.safeText(text)) reason = text;
                else WireNotes.note("dispositionReasonDropped", path + ".reasonCode");
            }
            Object rawClass = row.get("stopClass");
            if (!"NONE".equals(rawClass)) WireNotes.note("dispositionStopClassIgnored", path + ".stopClass");
            return new Disposition(kind, reason, "NONE");
        }
        String reason = fieldStr(phase, row, "reasonCode", path + ".reasonCode");
        String stopClass = fieldStr(phase, row, "stopClass", path + ".stopClass");
        if ("STOP".equals(kind)) {
            if (reason.isEmpty()) throw bad("L1_TEXT_REQUIRED", path + ".reasonCode");
            if (!EditorialP5RawWireContract.safeText(reason)) throw bad("L1_DISPOSITION_REASON_INVALID", path + ".reasonCode");
            if (!"CONTENT_BLOCKED".equals(stopClass) && !"INPUT_REQUIRED".equals(stopClass)) throw bad("L1_DISPOSITION_STOP_CLASS_INVALID");
        } else {
            throw bad("L1_DISPOSITION_INVALID");
        }
        return new Disposition(kind, reason, stopClass);
    }

    // ---- metrics ----

    public static Metrics metrics(EditorialRawInventory.Inventory inventory, List<Candidate> candidates, ReconcilePass pass) {
        int occurrences = 0;
        int open = 0;
        int preserved = 0;
        for (Finding finding : pass.findings()) {
            occurrences += 1 + finding.occurrenceUnits().size();
            if ("OPEN".equals(finding.disposition())) open++; else preserved++;
        }
        int unprocessed = 0;
        int conflict = 0;
        for (Resolution resolution : pass.resolutions()) {
            if ("UNPROCESSED".equals(resolution.status())) unprocessed++;
            if ("CONFLICT".equals(resolution.status())) conflict++;
        }
        return new Metrics(pass.findings().size(), occurrences, open, preserved, candidates.size(), unprocessed, conflict,
                pass.speakerRecords().size(), pass.protectedSpans().size(), inventory.units().size(), inventory.excluded().size());
    }

    // ---- report body (the engine adds identity fields around it) ----

    /** Everything the REPORT_L1 body carries; {@link #parseBody} restores it without loss. */
    public record Body(String phase, String inventoryRevision, int unitCount, int excludedLineCount, String inventorySha256,
                       String rawSha256, List<EditorialRawInventory.Range> rawCoverage, List<Candidate> candidates,
                       List<EditorialRawInventory.Range> reviewCoverage, List<Resolution> resolutions, List<Finding> findings,
                       List<SpeakerRecord> speakerRecords, List<ProtectedSpan> protectedSpans, Disposition disposition,
                       Metrics metrics, int duplicateReferencesRemoved, int draftAnchorsDerivedFromQuote,
                       int maxDraftAnchorDeviation, List<String> speakerRecordsDropped, List<String> bookkeepingNotes) {
        public Body {
            rawCoverage = List.copyOf(rawCoverage);
            candidates = List.copyOf(candidates);
            reviewCoverage = List.copyOf(reviewCoverage);
            resolutions = List.copyOf(resolutions);
            findings = List.copyOf(findings);
            speakerRecords = List.copyOf(speakerRecords);
            protectedSpans = List.copyOf(protectedSpans);
            speakerRecordsDropped = List.copyOf(speakerRecordsDropped);
            bookkeepingNotes = List.copyOf(bookkeepingNotes);
        }
    }

    public static Body bodyOfRawPass(EditorialRawInventory.Inventory inventory, RawPass raw) {
        Metrics metrics = new Metrics(0, 0, 0, 0, raw.candidates().size(), 0, 0, 0, 0, inventory.units().size(), inventory.excluded().size());
        return new Body("L1_RAW_DISCOVERY", EditorialRawInventory.REVISION, inventory.units().size(), inventory.excluded().size(),
                inventory.inventorySha256(), inventory.rawSha256(), raw.coverage(), raw.candidates(), List.of(), List.of(), List.of(),
                List.of(), List.of(), new Disposition("CONTINUE", "L1_RAW_LEDGER", "NONE"), metrics, 0, 0, 0, List.of(), List.of());
    }

    public static Body bodyOfReconcile(EditorialRawInventory.Inventory inventory, RawPass raw, ReconcilePass pass) {
        return new Body("L1_RECONCILE", EditorialRawInventory.REVISION, inventory.units().size(), inventory.excluded().size(),
                inventory.inventorySha256(), inventory.rawSha256(), raw.coverage(), raw.candidates(), pass.coverage(), pass.resolutions(),
                pass.findings(), pass.speakerRecords(), pass.protectedSpans(), pass.disposition(),
                metrics(inventory, raw.candidates(), pass), pass.duplicateReferencesRemoved(),
                pass.draftAnchorsDerivedFromQuote(), pass.maxDraftAnchorDeviation(), pass.speakerRecordsDropped(), pass.bookkeepingNotes());
    }

    public static Map<String, Object> bodyToMap(Body body) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contractRevision", EditorialContractRevision.CURRENT_LEDGER);
        m.put("phase", body.phase());
        Map<String, Object> normalizations = new LinkedHashMap<>();
        normalizations.put("duplicateReferencesRemoved", num(body.duplicateReferencesRemoved()));
        normalizations.put("draftAnchorDerivedFromQuote", num(body.draftAnchorsDerivedFromQuote()));
        normalizations.put("maxDraftAnchorDeviation", num(body.maxDraftAnchorDeviation()));
        normalizations.put("speakerRecordsDropped", num(body.speakerRecordsDropped().size()));
        normalizations.put("speakerRecordsDroppedPaths", new ArrayList<Object>(body.speakerRecordsDropped()));
        normalizations.put("bookkeepingNoteCount", num(body.bookkeepingNotes().size()));
        normalizations.put("bookkeepingNotes", new ArrayList<Object>(body.bookkeepingNotes()));
        m.put("normalizations", normalizations);
        Map<String, Object> inv = new LinkedHashMap<>();
        inv.put("revision", body.inventoryRevision());
        inv.put("unitCount", num(body.unitCount()));
        inv.put("excludedLineCount", num(body.excludedLineCount()));
        inv.put("inventorySha256", body.inventorySha256());
        inv.put("rawSha256", body.rawSha256());
        m.put("inventory", inv);
        m.put("rawCoverage", rangesToList(body.rawCoverage()));
        m.put("candidates", candidatesToList(body.candidates()));
        m.put("reviewCoverage", rangesToList(body.reviewCoverage()));
        List<Object> res = new ArrayList<>();
        for (Resolution r : body.resolutions()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("candidateId", r.candidateId());
            o.put("status", r.status());
            o.put("findingRef", r.findingRef());
            res.add(o);
        }
        m.put("resolutions", res);
        List<Object> fs = new ArrayList<>();
        for (Finding f : body.findings()) fs.add(findingToMap(f));
        m.put("findings", fs);
        List<Object> sp = new ArrayList<>();
        for (SpeakerRecord s : body.speakerRecords()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("unitId", s.unitId());
            o.put("speaker", s.speaker());
            o.put("listener", s.listener());
            o.put("basis", s.basis());
            sp.add(o);
        }
        m.put("speakerRecords", sp);
        List<Object> ps = new ArrayList<>();
        for (ProtectedSpan p : body.protectedSpans()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("spanId", p.spanId());
            o.put("start", num(p.start()));
            o.put("end", num(p.end()));
            o.put("source", p.source());
            o.put("reason", p.reason());
            ps.add(o);
        }
        m.put("protectedSpans", ps);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("disposition", body.disposition().kind());
        d.put("reasonCode", body.disposition().reasonCode());
        d.put("stopClass", body.disposition().stopClass());
        m.put("l1Disposition", d);
        Metrics x = body.metrics();
        Map<String, Object> mm = new LinkedHashMap<>();
        mm.put("uniqueFindingCount", num(x.uniqueFindingCount()));
        mm.put("occurrenceCount", num(x.occurrenceCount()));
        mm.put("openFindingCount", num(x.openFindingCount()));
        mm.put("preservedFindingCount", num(x.preservedFindingCount()));
        mm.put("candidateCount", num(x.candidateCount()));
        mm.put("unprocessedCandidateCount", num(x.unprocessedCandidateCount()));
        mm.put("conflictCandidateCount", num(x.conflictCandidateCount()));
        mm.put("speakerRecordCount", num(x.speakerRecordCount()));
        mm.put("protectedSpanCount", num(x.protectedSpanCount()));
        mm.put("unitCount", num(x.unitCount()));
        mm.put("excludedLineCount", num(x.excludedLineCount()));
        m.put("metrics", mm);
        return m;
    }

    @SuppressWarnings("unchecked")
    public static Body parseBody(Map<String, Object> report) {
        if (!EditorialContractRevision.isLedger((String) report.get("contractRevision"))) throw bad("L1_REPORT_NOT_LEDGER_V2");
        Map<String, Object> inv = (Map<String, Object>) report.get("inventory");
        List<EditorialRawInventory.Range> rawCoverage = rangesFromList(report.get("rawCoverage"));
        List<Candidate> candidates = new ArrayList<>();
        for (Object o : EditorialCanonicalJson.array(report.get("candidates"), "candidates")) {
            Map<String, Object> r = (Map<String, Object>) o;
            candidates.add(new Candidate((String) r.get("candidateId"), (String) r.get("ledger"), (String) r.get("unitId"), (String) r.get("note")));
        }
        List<Resolution> resolutions = new ArrayList<>();
        for (Object o : EditorialCanonicalJson.array(report.get("resolutions"), "resolutions")) {
            Map<String, Object> r = (Map<String, Object>) o;
            resolutions.add(new Resolution((String) r.get("candidateId"), (String) r.get("status"), (String) r.get("findingRef")));
        }
        List<Finding> findings = new ArrayList<>();
        for (Object o : EditorialCanonicalJson.array(report.get("findings"), "findings")) findings.add(findingFromMap((Map<String, Object>) o));
        List<SpeakerRecord> speakers = new ArrayList<>();
        for (Object o : EditorialCanonicalJson.array(report.get("speakerRecords"), "speakerRecords")) {
            Map<String, Object> r = (Map<String, Object>) o;
            speakers.add(new SpeakerRecord((String) r.get("unitId"), (String) r.get("speaker"), (String) r.get("listener"), (String) r.get("basis")));
        }
        List<ProtectedSpan> spans = new ArrayList<>();
        for (Object o : EditorialCanonicalJson.array(report.get("protectedSpans"), "protectedSpans")) {
            Map<String, Object> r = (Map<String, Object>) o;
            spans.add(new ProtectedSpan((String) r.get("spanId"), ((BigDecimal) r.get("start")).intValueExact(),
                    ((BigDecimal) r.get("end")).intValueExact(), (String) r.get("source"), (String) r.get("reason")));
        }
        Map<String, Object> d = (Map<String, Object>) report.get("l1Disposition");
        Map<String, Object> mm = (Map<String, Object>) report.get("metrics");
        Metrics metrics = new Metrics(i(mm, "uniqueFindingCount"), i(mm, "occurrenceCount"), i(mm, "openFindingCount"),
                i(mm, "preservedFindingCount"), i(mm, "candidateCount"), i(mm, "unprocessedCandidateCount"),
                i(mm, "conflictCandidateCount"), i(mm, "speakerRecordCount"), i(mm, "protectedSpanCount"), i(mm, "unitCount"),
                i(mm, "excludedLineCount"));
        int duplicateReferencesRemoved = 0;
        int draftAnchorsDerived = 0;
        int maxDraftDeviation = 0;
        List<String> droppedSpeakers = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        Object normalizationValue = report.get("normalizations");
        if (normalizationValue instanceof Map<?, ?> normalizationMap) {
            if (normalizationMap.get("duplicateReferencesRemoved") instanceof BigDecimal count) duplicateReferencesRemoved = count.intValueExact();
            if (normalizationMap.get("draftAnchorDerivedFromQuote") instanceof BigDecimal count) draftAnchorsDerived = count.intValueExact();
            if (normalizationMap.get("maxDraftAnchorDeviation") instanceof BigDecimal count) maxDraftDeviation = count.intValueExact();
            if (normalizationMap.get("bookkeepingNotes") instanceof List<?> noteList) {
                for (Object note : noteList) if (note instanceof String text) notes.add(text);
            }
            if (normalizationMap.get("speakerRecordsDroppedPaths") instanceof List<?> paths) {
                for (Object path : paths) if (path instanceof String text) droppedSpeakers.add(text);
            }
        }
        return new Body((String) report.get("phase"), (String) inv.get("revision"), i(inv, "unitCount"), i(inv, "excludedLineCount"),
                (String) inv.get("inventorySha256"), (String) inv.get("rawSha256"), rawCoverage, candidates,
                rangesFromList(report.get("reviewCoverage")), resolutions, findings, speakers, spans,
                new Disposition((String) d.get("disposition"), (String) d.get("reasonCode"), (String) d.get("stopClass")), metrics,
                duplicateReferencesRemoved, draftAnchorsDerived, maxDraftDeviation, droppedSpeakers, notes);
    }

    // ---- strict response schema (provider response_format); the parsers above stay the authority ----

    public static final String RAW_SCHEMA_NAME = "safe4_l1_raw_ledger_v4";
    public static final String RECONCILE_SCHEMA_NAME = "safe4_l1_reconcile_ledger_v4";

    /** JSON schema of the wire for a strict response format; every key is required, unused values are empty/0. */
    public static Map<String, Object> jsonSchema(boolean rawPass) {
        return jsonSchema(rawPass, rawPass ? RAW_WIRE : RECONCILE_WIRE);
    }

    /** The RAW-pass schema under another wire label (L2 discovery, L3 re-audit); RECONCILE keeps its own label. */
    public static Map<String, Object> jsonSchema(boolean rawPass, String wireLabel) {
        Map<String, Object> props = new LinkedHashMap<>();
        String phase = rawPass ? EditorialFieldSpec.L1_RAW_DISCOVERY : EditorialFieldSpec.L1_RECONCILE;
        Map<String, Object> wireVersion = fieldSchema(phase, "wireSchemaVersion");
        wireVersion.put("enum", new ArrayList<Object>(List.of(wireLabel)));
        props.put("wireSchemaVersion", wireVersion);
        props.put("attemptIdentity", fieldSchema(phase, "attemptIdentity"));
        props.put("coverage", fieldArraySchema(phase, "coverage", objectSchema(
                "from", fieldSchema(phase, "coverage[].from"), "to", fieldSchema(phase, "coverage[].to"),
                "status", fieldSchema(phase, "coverage[].status"))));
        if (rawPass) {
            props.put("candidates", fieldArraySchema(phase, "candidates", objectSchema(
                    "candidateId", fieldSchema(phase, "candidates[].candidateId"),
                    "ledger", fieldSchema(phase, "candidates[].ledger"),
                    "unitId", fieldSchema(phase, "candidates[].unitId"),
                    "note", fieldSchema(phase, "candidates[].note"))));
            return topSchema(props);
        }
        props.put("resolutions", fieldArraySchema(phase, "resolutions", objectSchema(
                "candidateId", fieldSchema(phase, "resolutions[].candidateId"),
                "status", fieldSchema(phase, "resolutions[].status"),
                "findingRef", fieldSchema(phase, "resolutions[].findingRef"))));
        Map<String, Object> draft = objectSchema("kind", fieldSchema(phase, "findings[].draft.kind"),
                "start", integerSchema(), "end", integerSchema(), "after", integerSchema());
        props.put("findings", fieldArraySchema(phase, "findings", objectSchema(
                "errorId", fieldSchema(phase, "findings[].errorId"),
                "type", fieldSchema(phase, "findings[].type"),
                "severity", fieldSchema(phase, "findings[].severity"),
                "rawUnits", fieldArraySchema(phase, "findings[].rawUnits", fieldSchema(phase, "findings[].rawUnits[]")),
                "draft", draft,
                "rawQuote", fieldSchema(phase, "findings[].rawQuote"), "draftQuote", fieldSchema(phase, "findings[].draftQuote"),
                "observation", fieldSchema(phase, "findings[].observation"), "expectedMeaning", fieldSchema(phase, "findings[].expectedMeaning"),
                "evidenceRefs", fieldArraySchema(phase, "findings[].evidenceRefs", fieldSchema(phase, "findings[].evidenceRefs[]")),
                "candidateIds", fieldArraySchema(phase, "findings[].candidateIds", fieldSchema(phase, "findings[].candidateIds[]")),
                "occurrenceUnits", fieldArraySchema(phase, "findings[].occurrenceUnits", fieldSchema(phase, "findings[].occurrenceUnits[]")),
                "disposition", fieldSchema(phase, "findings[].disposition"),
                "evidenceLimit", fieldSchema(phase, "findings[].evidenceLimit"))));
        props.put("speakerRecords", fieldArraySchema(phase, "speakerRecords", objectSchema(
                "unitId", fieldSchema(phase, "speakerRecords[].unitId"),
                "speaker", fieldSchema(phase, "speakerRecords[].speaker"),
                "listener", fieldSchema(phase, "speakerRecords[].listener"),
                "basis", fieldSchema(phase, "speakerRecords[].basis"))));
        props.put("protectedSpans", fieldArraySchema(phase, "protectedSpans", objectSchema(
                "spanId", fieldSchema(phase, "protectedSpans[].spanId"), "start", integerSchema(), "end", integerSchema(),
                "source", fieldSchema(phase, "protectedSpans[].source"),
                "reason", fieldSchema(phase, "protectedSpans[].reason"))));
        props.put("disposition", objectSchema("disposition", fieldSchema(phase, "disposition.disposition"),
                "reasonCode", fieldSchema(phase, "disposition.reasonCode"),
                "stopClass", fieldSchema(phase, "disposition.stopClass")));
        return topSchema(props);
    }

    private static Map<String, Object> topSchema(Map<String, Object> props) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "object");
        m.put("additionalProperties", Boolean.FALSE);
        m.put("required", new ArrayList<Object>(props.keySet()));
        m.put("properties", props);
        return m;
    }

    private static Map<String, Object> objectSchema(Object... kv) {
        Map<String, Object> props = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) props.put((String) kv[i], kv[i + 1]);
        return topSchema(props);
    }

    private static Map<String, Object> fieldSchema(String phase, String path) {
        return EditorialFieldSpec.schema(phase, path);
    }

    private static Map<String, Object> fieldArraySchema(String phase, String path, Map<String, Object> items) {
        EditorialFieldSpec.Field spec = EditorialFieldSpec.find(phase, path);
        if (spec == null || spec.kind() != EditorialFieldSpec.Kind.ARRAY) {
            throw new IllegalArgumentException("EDITORIAL_FIELD_SPEC_MISSING:" + phase + ":" + path);
        }
        return arraySchema(spec.minItems(), spec.maxItems(), items);
    }

    private static Map<String, Object> integerSchema() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "integer");
        return m;
    }

    private static Map<String, Object> enumSchema(List<String> values) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "string");
        m.put("enum", new ArrayList<Object>(values));
        return m;
    }

    private static Map<String, Object> arraySchema(int maxItems, Map<String, Object> items) {
        return arraySchema(0, maxItems, items);
    }

    private static Map<String, Object> arraySchema(int minItems, int maxItems, Map<String, Object> items) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "array");
        if (minItems > 0) m.put("minItems", num(minItems));
        m.put("maxItems", num(maxItems));
        m.put("items", items);
        return m;
    }

    // ---- helpers ----

    private static Map<String, Object> findingToMap(Finding f) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("errorId", f.errorId());
        o.put("type", f.type());
        o.put("severity", f.severity());
        o.put("rawUnits", new ArrayList<Object>(f.rawUnits()));
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("kind", f.draft().kind());
        if ("LINES".equals(f.draft().kind())) {
            d.put("start", num(f.draft().start()));
            d.put("end", num(f.draft().end()));
        } else {
            d.put("after", num(f.draft().after()));
        }
        o.put("draft", d);
        o.put("rawQuote", f.rawQuote());
        o.put("draftQuote", f.draftQuote());
        o.put("observation", f.observation());
        o.put("expectedMeaning", f.expectedMeaning());
        o.put("evidenceRefs", new ArrayList<Object>(f.evidenceRefs()));
        o.put("candidateIds", new ArrayList<Object>(f.candidateIds()));
        o.put("occurrenceUnits", new ArrayList<Object>(f.occurrenceUnits()));
        o.put("disposition", f.disposition());
        o.put("evidenceLimit", f.evidenceLimit());
        return o;
    }

    @SuppressWarnings("unchecked")
    private static Finding findingFromMap(Map<String, Object> o) {
        Map<String, Object> d = (Map<String, Object>) o.get("draft");
        DraftAnchor anchor = "LINES".equals(d.get("kind"))
                ? DraftAnchor.lines(i(d, "start"), i(d, "end")) : DraftAnchor.missingAfter(i(d, "after"));
        return new Finding((String) o.get("errorId"), (String) o.get("type"), (String) o.get("severity"), strings(o.get("rawUnits")),
                anchor, (String) o.get("rawQuote"), (String) o.get("draftQuote"), (String) o.get("observation"),
                (String) o.get("expectedMeaning"), strings(o.get("evidenceRefs")), strings(o.get("candidateIds")),
                strings(o.get("occurrenceUnits")), (String) o.get("disposition"), (String) o.get("evidenceLimit"));
    }

    private static List<String> strings(Object value) {
        List<String> out = new ArrayList<>();
        for (Object o : EditorialCanonicalJson.array(value, "list")) out.add((String) o);
        return out;
    }

    private static List<Object> rangesToList(List<EditorialRawInventory.Range> ranges) {
        List<Object> out = new ArrayList<>();
        for (EditorialRawInventory.Range r : ranges) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("from", r.fromId());
            o.put("to", r.toId());
            o.put("status", r.status());
            out.add(o);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<EditorialRawInventory.Range> rangesFromList(Object value) {
        List<EditorialRawInventory.Range> out = new ArrayList<>();
        for (Object o : EditorialCanonicalJson.array(value, "ranges")) {
            Map<String, Object> r = (Map<String, Object>) o;
            out.add(new EditorialRawInventory.Range((String) r.get("from"), (String) r.get("to"), (String) r.get("status")));
        }
        return out;
    }

    private static List<Object> candidatesToList(List<Candidate> candidates) {
        List<Object> out = new ArrayList<>();
        for (Candidate c : candidates) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("candidateId", c.candidateId());
            o.put("ledger", c.ledger());
            o.put("unitId", c.unitId());
            o.put("note", c.note());
            out.add(o);
        }
        return out;
    }

    static List<EditorialRawInventory.Range> coverage(Object value, EditorialRawInventory.Inventory inventory) {
        return coverage(value, inventory, EditorialFieldSpec.L1_RAW_DISCOVERY);
    }

    static List<EditorialRawInventory.Range> coverage(Object value, EditorialRawInventory.Inventory inventory, String phase) {
        List<Object> rows = EditorialCanonicalJson.array(value, "coverage");
        if (rows.size() > MAX_RANGES) throw bad("L1_COVERAGE_RANGE_LIMIT_EXCEEDED");
        List<EditorialRawInventory.Range> ranges = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            String path = "coverage." + index;
            Map<String, Object> row = object(rows.get(index), path);
            keys(row, Set.of("from", "to", "status"), path);
            ranges.add(new EditorialRawInventory.Range(EditorialUnitReference.resolve(row.get("from"), inventory, path + ".from"),
                    EditorialUnitReference.resolve(row.get("to"), inventory, path + ".to"),
                    enumOf(row, "status", RANGE_STATUSES, path + ".status", phase)));
        }
        List<String> issues = EditorialRawInventory.coverageIssues(inventory, ranges);
        if (!issues.isEmpty()) {
            // the most specific defect wins, so a reversed range is not reported as the gap it leaves behind
            for (String[] known : new String[][] {{"COVERAGE_UNKNOWN", "L1_COVERAGE_UNKNOWN_ID"},
                    {"COVERAGE_REVERSED", "L1_COVERAGE_REVERSED"}, {"COVERAGE_OVERLAP", "L1_COVERAGE_OVERLAP"},
                    {"COVERAGE_GAP", "L1_COVERAGE_GAP"}}) {
                for (String issue : issues) if (issue.startsWith(known[0])) throw bad(known[1], "coverage");
            }
            throw bad("L1_COVERAGE_EMPTY", "coverage");
        }
        return ranges;
    }

    static Map<String, Object> rootOf(byte[] bytes, String attemptIdentity, String wire, Set<String> keys) {
        return rootOf(bytes, attemptIdentity, wire, keys, Set.of());
    }

    /**
     * {@code arrayKeys} are the MAY arrays of the field specification: the prompt tells the model it may omit them, so
     * an absent one is an empty array (noted) rather than a rejection.
     */
    static Map<String, Object> rootOf(byte[] bytes, String attemptIdentity, String wire, Set<String> keys,
                                      Set<String> arrayKeys) {
        if (bytes == null || bytes.length > MAX_WIRE_BYTES) throw bad("L1_WIRE_BYTE_LIMIT_EXCEEDED", "root");
        Map<String, Object> root = EditorialCanonicalJson.parseObject(bytes);
        keys(root, keys, "root", arrayKeys);
        if (!arrayKeys.isEmpty()) {
            root = new LinkedHashMap<>(root);
            for (String key : new java.util.TreeSet<>(arrayKeys)) {
                if (!root.containsKey(key)) {
                    root.put(key, new ArrayList<Object>());
                    WireNotes.note("missingOptionalKeyDefaulted", key);
                }
            }
        }
        if (!wire.equals(root.get("wireSchemaVersion"))) throw bad("L1_WIRE_SCHEMA_INVALID", "wireSchemaVersion");
        if (!attemptIdentity.equals(root.get("attemptIdentity"))) throw bad("L1_WIRE_ATTEMPT_ECHO_MISMATCH", "attemptIdentity");
        return root;
    }

    static String id(Map<String, Object> row, String key) {
        return id(row, key, key);
    }

    static String id(Map<String, Object> row, String key, String path) {
        String value = str(row, key, EditorialFieldSpec.MAX_ID_LENGTH, true, path);
        if (!EditorialP5RawWireContract.token(value, EditorialFieldSpec.MAX_ID_LENGTH)) throw bad("L1_ID_INVALID", path);
        return value;
    }

    static String id(Map<String, Object> row, String key, String path, String phase) {
        String value = fieldStr(phase, row, key, path);
        if (!EditorialP5RawWireContract.token(value, EditorialFieldSpec.MAX_ID_LENGTH)) throw bad("L1_ID_INVALID", path);
        return value;
    }

    static String enumOf(Map<String, Object> row, String key, Set<String> allowed) {
        return enumOf(row, key, allowed, key);
    }

    static String enumOf(Map<String, Object> row, String key, Set<String> allowed, String path) {
        String value = str(row, key, 32, true, path);
        if (!allowed.contains(value)) throw bad("L1_ENUM_INVALID", path);
        return value;
    }

    static String enumOf(Map<String, Object> row, String key, Set<String> allowed, String path, String phase) {
        String value = fieldStr(phase, row, key, path);
        if (!allowed.contains(value)) throw bad("L1_ENUM_INVALID", path);
        return value;
    }

    /** Free-text notes the app only displays: control characters and overlong text are normalized, not rejected. */
    static final Set<String> NOTE_FIELDS = Set.of("findings[].observation", "findings[].expectedMeaning",
            "findings[].evidenceLimit", "candidates[].note", "speakerRecords[].speaker", "speakerRecords[].listener",
            "speakerRecords[].basis", "protectedSpans[].reason", "changes[].reason", "preserved[].evidenceLimit",
            "findingResolutions[].reason", "carriedResolutions[].reason", "probes[].scope", "probes[].contrast",
            "defects[].note");

    static String fieldStr(String phase, Map<String, Object> row, String key, String path) {
        Object value = row.get(key);
        EditorialFieldSpec.Field spec = EditorialFieldSpec.find(phase, path);
        if (value instanceof String note && spec != null && NOTE_FIELDS.contains(spec.path())) {
            value = sanitizeNote(note, spec.maxLength(), path);
        }
        String text = EditorialFieldSpec.validateString(phase, path, value, "L1_TEXT_INVALID",
                "L1_TEXT_REQUIRED", "L1_TEXT_TOO_LONG", "L1_TEXT_PATTERN_INVALID");
        for (int i = 0; i < text.length(); i++) if (Character.isISOControl(text.charAt(i))) {
            throw bad("L1_TEXT_CONTROL_CHARACTER", path);
        }
        return text;
    }

    static String sanitizeNote(String text, int maxLength, String path) {
        StringBuilder cleaned = new StringBuilder(text.length());
        boolean changed = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isISOControl(c)) { cleaned.append(' '); changed = true; }
            else cleaned.append(c);
        }
        if (changed) WireNotes.note("textControlCharactersReplaced", path);
        String result = cleaned.toString();
        if (result.length() > maxLength) {
            int end = maxLength;
            if (end > 0 && Character.isHighSurrogate(result.charAt(end - 1))) end--;
            result = result.substring(0, end);
            WireNotes.note("textTruncated", path);
        }
        return result;
    }

    /** Optional (MAY) keys that are absent take their empty value; the default is noted, never invented content. */
    static Map<String, Object> withDefaults(Map<String, Object> row, String path, Object... keyAndDefault) {
        Map<String, Object> copy = new LinkedHashMap<>(row);
        for (int i = 0; i < keyAndDefault.length; i += 2) {
            String key = (String) keyAndDefault[i];
            if (!copy.containsKey(key)) {
                copy.put(key, keyAndDefault[i + 1]);
                WireNotes.note("missingOptionalKeyDefaulted", path + "." + key);
            }
        }
        return copy;
    }

    /** A list of side references: unusable items are dropped and extra items beyond the cap are cut, each noted. */
    static List<String> lenientTokens(Object value, String path, int maxItems, int maxLength) {
        List<Object> rows;
        try { rows = EditorialCanonicalJson.array(value, path); }
        catch (RuntimeException invalid) { throw WireViolation.from(invalid, "L1_LIST_INVALID", path); }
        List<String> out = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            Object o = rows.get(index);
            if (!(o instanceof String text) || text.isBlank() || text.length() > maxLength
                    || !EditorialP5RawWireContract.token(text, EditorialP5RawWireContract.MAX_ID_LENGTH)) {
                WireNotes.note("listItemDropped", path + "." + index);
                continue;
            }
            if (out.size() >= maxItems) { WireNotes.note("listTruncated", path); break; }
            out.add(text);
        }
        return out;
    }

    /** Extra occurrence units: an unusable reference is dropped (noted); the primary anchor stays strict. */
    static List<String> lenientUnits(Object value, String path, int maxItems, EditorialRawInventory.Inventory inventory) {
        List<Object> rows;
        try { rows = EditorialCanonicalJson.array(value, path); }
        catch (RuntimeException invalid) { throw WireViolation.from(invalid, "L1_LIST_INVALID", path); }
        List<String> out = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            String id;
            try {
                id = EditorialUnitReference.resolve(rows.get(index), inventory, path + "." + index);
            } catch (WireViolation invalid) {
                WireNotes.note("unitReferenceDropped", path + "." + index);
                continue;
            }
            if (out.size() >= maxItems) { WireNotes.note("listTruncated", path); break; }
            out.add(id);
        }
        return out;
    }

    private static String rawPhase(String wireLabel) {
        if (EditorialL2Execution.DISCOVERY_WIRE_V3.equals(wireLabel)) return EditorialFieldSpec.L2_RAW_DISCOVERY;
        if (EditorialL3Execution.REAUDIT_WIRE_V3.equals(wireLabel)) return EditorialFieldSpec.L3_RAW_FIRST_REAUDIT;
        return EditorialFieldSpec.L1_RAW_DISCOVERY;
    }

    static String str(Map<String, Object> row, String key, int max, boolean required) {
        return str(row, key, max, required, key);
    }

    static String str(Map<String, Object> row, String key, int max, boolean required, String path) {
        Object value = row.get(key);
        if (!(value instanceof String)) throw bad("L1_TEXT_INVALID", path);
        String text = (String) value;
        if (text.length() > max) throw bad("L1_TEXT_TOO_LONG", path);
        if (required && text.isBlank()) throw bad("L1_TEXT_REQUIRED", path);
        for (int i = 0; i < text.length(); i++) if (Character.isISOControl(text.charAt(i))) throw bad("L1_TEXT_CONTROL_CHARACTER", path);
        return text;
    }

    static int intOf(Map<String, Object> row, String key) {
        return intOf(row, key, key);
    }

    static int intOf(Map<String, Object> row, String key, String path) {
        Object value = row.get(key);
        if (!(value instanceof BigDecimal)) throw bad("L1_INT_INVALID", path);
        try {
            return ((BigDecimal) value).intValueExact();
        } catch (ArithmeticException invalid) {
            throw bad("L1_INT_INVALID", path);
        }
    }

    static List<String> stringList(Object value, String path, int maxItems, int maxLength) {
        List<Object> rows;
        try { rows = EditorialCanonicalJson.array(value, path); }
        catch (RuntimeException invalid) { throw WireViolation.from(invalid, "L1_LIST_INVALID", path); }
        if (rows.size() > maxItems) throw bad("L1_LIST_TOO_LONG", path);
        List<String> out = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            Object o = rows.get(index);
            if (!(o instanceof String) || ((String) o).isBlank() || ((String) o).length() > maxLength) throw bad("L1_LIST_ITEM_INVALID", path + "." + index);
            out.add((String) o);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map)) throw bad("L1_OBJECT_EXPECTED", path);
        return (Map<String, Object>) value;
    }

    static void keys(Map<String, Object> value, Set<String> allowed, String path) {
        keys(value, allowed, path, Set.of());
    }

    static void keys(Map<String, Object> value, Set<String> allowed, String path, Set<String> optional) {
        // Z1 BOOKKEEPING: a key the contract does not define changes no verified claim; ignore it and say so
        for (String key : value.keySet()) if (!allowed.contains(key)) WireNotes.note("unknownKeyIgnored", path);
        for (String key : allowed) if (!optional.contains(key) && !value.containsKey(key)) throw bad("L1_MISSING_KEY", path + "." + key);
    }

    private static int i(Map<String, Object> m, String key) { return ((BigDecimal) m.get(key)).intValueExact(); }

    private static BigDecimal num(int value) { return BigDecimal.valueOf(value); }

    static IllegalArgumentException bad(String code) { return WireViolation.at(code, "root"); }

    static IllegalArgumentException bad(String code, String path) { return WireViolation.at(code, path); }

    /** Typed, allow-listed message of a parse failure; anything else collapses to a generic code. */
    public static String safeMessage(RuntimeException error) {
        return WireViolation.safeMessage(error, "L1_WIRE_PARSE_FAILED");
    }

    /** Sorted copy used by tests and reports to compare ledgers independent of insertion order. */
    public static Map<String, Integer> findingCountByType(List<Finding> findings) {
        Map<String, Integer> counts = new TreeMap<>();
        for (Finding f : findings) counts.merge(f.type(), 1, Integer::sum);
        return counts;
    }
}
