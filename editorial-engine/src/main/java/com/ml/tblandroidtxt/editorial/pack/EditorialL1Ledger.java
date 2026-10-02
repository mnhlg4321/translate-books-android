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
 * L1 Error Ledger, contract revision {@code L1_LEDGER_V2}. Four notions stay apart: a RAW unit/occurrence is a
 * part of the source (the app's {@link EditorialRawInventory}); a candidate is a suspicion raised by the blind RAW
 * pass; a finding is a defect with evidence in the DRAFT; a change is an edit (L2/L3). The model supplies
 * judgement and the app checks everything checkable: every unit id exists, coverage ranges close over the whole
 * inventory exactly once, quotes occur in the anchored text, draft anchors are inside the DRAFT, ids are unique,
 * references resolve, every candidate is resolved. Nothing here is a statement that the meaning is right.
 */
public final class EditorialL1Ledger {
    public static final String RAW_WIRE = "safe4.l1.raw-ledger.wire.v2";
    public static final String RECONCILE_WIRE = "safe4.l1.reconcile-ledger.wire.v2";

    /**
     * Hard byte cap of one response. The output-token cap (16,384) is what bounds cost; at roughly 4 bytes per token
     * the largest response a provider can return is about 64 KiB, so a response above this cap is a typed failure
     * and is never truncated. The count and text limits below keep a realistic call far under it.
     */
    public static final int MAX_WIRE_BYTES = 65_536;
    public static final int MAX_FINDINGS_PER_CALL = 48;
    public static final int MAX_CANDIDATES_PER_CALL = 400;
    public static final int MAX_SPEAKER_RECORDS_PER_CALL = 200;
    public static final int MAX_PROTECTED_SPANS_PER_CALL = 100;
    public static final int MAX_RANGES = 120;
    public static final int MAX_TEXT = 240;
    public static final int MAX_QUOTE = 80;
    public static final int MAX_REFS = 4;
    public static final int MAX_RAW_UNITS_PER_FINDING = 6;
    public static final int MAX_OCCURRENCE_UNITS = 40;

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
                                List<ProtectedSpan> protectedSpans, Disposition disposition) {
        public ReconcilePass {
            coverage = List.copyOf(coverage);
            resolutions = List.copyOf(resolutions);
            findings = List.copyOf(findings);
            speakerRecords = List.copyOf(speakerRecords);
            protectedSpans = List.copyOf(protectedSpans);
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
        Map<String, Object> root = rootOf(bytes, attemptIdentity, RAW_WIRE, Set.of("wireSchemaVersion", "attemptIdentity", "coverage", "candidates"));
        List<EditorialRawInventory.Range> coverage = coverage(root.get("coverage"), inventory);
        List<Object> rows = EditorialCanonicalJson.array(root.get("candidates"), "candidates");
        if (rows.size() > MAX_CANDIDATES_PER_CALL) throw bad("L1_CANDIDATE_LIMIT_EXCEEDED");
        List<Candidate> candidates = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (Object value : rows) {
            Map<String, Object> row = object(value, "candidate");
            keys(row, Set.of("candidateId", "ledger", "unitId", "note"), "candidate", Set.of("note"));
            String id = id(row, "candidateId");
            if (!ids.add(id)) throw bad("L1_CANDIDATE_ID_DUPLICATE");
            String ledger = enumOf(row, "ledger", CANDIDATE_LEDGERS);
            String unit = str(row, "unitId", 64, true);
            if (!inventory.has(unit)) throw bad("L1_UNIT_UNKNOWN");
            String note = row.containsKey("note") ? str(row, "note", 80, false) : "";
            candidates.add(new Candidate(id, ledger, unit, note));
        }
        return new RawPass(coverage, candidates);
    }

    // ---- RECONCILE pass ----

    public static ReconcilePass parseReconcile(byte[] bytes, String attemptIdentity, EditorialRawInventory.Inventory inventory,
                                               List<String> draftLines, List<Candidate> rawCandidates) {
        Map<String, Object> root = rootOf(bytes, attemptIdentity, RECONCILE_WIRE, Set.of("wireSchemaVersion", "attemptIdentity",
                "coverage", "resolutions", "findings", "speakerRecords", "protectedSpans", "disposition"));
        Disposition disposition = disposition(object(root.get("disposition"), "disposition"));
        boolean stop = "STOP".equals(disposition.kind());
        List<EditorialRawInventory.Range> coverage = stop && EditorialCanonicalJson.array(root.get("coverage"), "coverage").isEmpty()
                ? List.of() : coverage(root.get("coverage"), inventory);

        List<Finding> findings = new ArrayList<>();
        Set<String> errorIds = new HashSet<>();
        Set<String> knownCandidates = new HashSet<>();
        for (Candidate candidate : rawCandidates) knownCandidates.add(candidate.candidateId());
        List<Object> findingRows = EditorialCanonicalJson.array(root.get("findings"), "findings");
        if (findingRows.size() > MAX_FINDINGS_PER_CALL) throw bad("L1_FINDING_LIMIT_EXCEEDED");
        for (Object value : findingRows) {
            Finding finding = finding(object(value, "finding"), inventory, draftLines, knownCandidates);
            if (!errorIds.add(finding.errorId())) throw bad("L1_ERROR_ID_DUPLICATE");
            findings.add(finding);
        }

        Map<String, Resolution> resolved = new LinkedHashMap<>();
        for (Object value : EditorialCanonicalJson.array(root.get("resolutions"), "resolutions")) {
            Map<String, Object> row = object(value, "resolution");
            keys(row, Set.of("candidateId", "status", "findingRef"), "resolution", Set.of("findingRef"));
            String candidateId = id(row, "candidateId");
            if (!knownCandidates.contains(candidateId) || resolved.containsKey(candidateId)) throw bad("L1_RESOLUTION_ID_INVALID");
            String status = enumOf(row, "status", CANDIDATE_STATUSES);
            String ref = row.containsKey("findingRef") ? str(row, "findingRef", 48, false) : "";
            if (!ref.isEmpty() && !errorIds.contains(ref)) throw bad("L1_FINDING_REF_UNKNOWN");
            resolved.put(candidateId, new Resolution(candidateId, status, ref));
        }
        if (!stop) {
            for (String id : knownCandidates) if (!resolved.containsKey(id)) throw bad("L1_CANDIDATE_UNRESOLVED");
        }

        List<SpeakerRecord> speakers = new ArrayList<>();
        List<Object> speakerRows = EditorialCanonicalJson.array(root.get("speakerRecords"), "speakerRecords");
        if (speakerRows.size() > MAX_SPEAKER_RECORDS_PER_CALL) throw bad("L1_SPEAKER_LIMIT_EXCEEDED");
        for (Object value : speakerRows) {
            Map<String, Object> row = object(value, "speakerRecord");
            keys(row, Set.of("unitId", "speaker", "listener", "basis"), "speakerRecord");
            String unit = str(row, "unitId", 64, true);
            if (!inventory.has(unit)) throw bad("L1_UNIT_UNKNOWN");
            speakers.add(new SpeakerRecord(unit, str(row, "speaker", 80, true), str(row, "listener", 80, true),
                    str(row, "basis", MAX_TEXT, true)));
        }

        List<ProtectedSpan> spans = new ArrayList<>();
        Set<String> spanIds = new HashSet<>();
        List<Object> spanRows = EditorialCanonicalJson.array(root.get("protectedSpans"), "protectedSpans");
        if (spanRows.size() > MAX_PROTECTED_SPANS_PER_CALL) throw bad("L1_PROTECTED_LIMIT_EXCEEDED");
        for (Object value : spanRows) {
            Map<String, Object> row = object(value, "protectedSpan");
            keys(row, Set.of("spanId", "start", "end", "source", "reason"), "protectedSpan");
            String spanId = id(row, "spanId");
            if (!spanIds.add(spanId)) throw bad("L1_PROTECTED_ID_DUPLICATE");
            int start = intOf(row, "start");
            int end = intOf(row, "end");
            if (start < 1 || end < start || end > draftLines.size()) throw bad("L1_PROTECTED_RANGE_INVALID");
            spans.add(new ProtectedSpan(spanId, start, end, enumOf(row, "source", PROTECTED_SOURCES), str(row, "reason", MAX_TEXT, true)));
        }
        return new ReconcilePass(coverage, new ArrayList<>(resolved.values()), findings, speakers, spans, disposition);
    }

    private static Finding finding(Map<String, Object> row, EditorialRawInventory.Inventory inventory,
                                   List<String> draftLines, Set<String> knownCandidates) {
        keys(row, Set.of("errorId", "type", "severity", "rawUnits", "draft", "rawQuote", "draftQuote",
                "observation", "expectedMeaning", "evidenceRefs", "candidateIds", "occurrenceUnits", "disposition",
                "evidenceLimit"), "finding", Set.of("candidateIds", "occurrenceUnits", "evidenceLimit", "evidenceRefs"));
        String errorId = id(row, "errorId");
        String type = enumOf(row, "type", FINDING_TYPES);
        String severity = enumOf(row, "severity", SEVERITIES);
        List<String> rawUnits = stringList(row.get("rawUnits"), "rawUnits", MAX_RAW_UNITS_PER_FINDING, 64);
        if (rawUnits.isEmpty()) throw bad("L1_FINDING_RAW_ANCHOR_REQUIRED");
        if (new HashSet<>(rawUnits).size() != rawUnits.size()) throw bad("L1_FINDING_RAW_ANCHOR_DUPLICATE");
        for (String unit : rawUnits) if (!inventory.has(unit)) throw bad("L1_UNIT_UNKNOWN");

        Map<String, Object> draftRow = object(row.get("draft"), "draft");
        String kind = str(draftRow, "kind", 16, true);
        DraftAnchor anchor;
        if ("LINES".equals(kind)) {
            keys(draftRow, Set.of("kind", "start", "end"), "draft");
            int start = intOf(draftRow, "start");
            int end = intOf(draftRow, "end");
            if (start < 1 || end < start || end > draftLines.size()) throw bad("L1_DRAFT_ANCHOR_OUT_OF_RANGE");
            anchor = DraftAnchor.lines(start, end);
        } else if ("MISSING".equals(kind)) {
            keys(draftRow, Set.of("kind", "after"), "draft");
            int after = intOf(draftRow, "after");
            if (after < 0 || after > draftLines.size()) throw bad("L1_DRAFT_ANCHOR_OUT_OF_RANGE");
            anchor = DraftAnchor.missingAfter(after);
        } else {
            throw bad("L1_DRAFT_ANCHOR_KIND_INVALID");
        }

        String rawQuote = str(row, "rawQuote", MAX_QUOTE, true);
        boolean quoted = false;
        for (String unit : rawUnits) quoted |= inventory.unit(unit).text().contains(rawQuote);
        if (!quoted) throw bad("L1_RAW_QUOTE_NOT_IN_ANCHOR");
        String draftQuote = str(row, "draftQuote", MAX_QUOTE, false);
        if ("LINES".equals(anchor.kind())) {
            if (draftQuote.isEmpty()) throw bad("L1_DRAFT_QUOTE_REQUIRED");
            StringBuilder span = new StringBuilder();
            for (int i = anchor.start(); i <= anchor.end(); i++) span.append(draftLines.get(i - 1)).append('\n');
            if (!span.toString().contains(draftQuote)) throw bad("L1_DRAFT_QUOTE_NOT_IN_ANCHOR");
        } else if (!draftQuote.isEmpty()) {
            throw bad("L1_DRAFT_QUOTE_FORBIDDEN_FOR_MISSING");
        }

        String observation = str(row, "observation", MAX_TEXT, true);
        String expected = str(row, "expectedMeaning", MAX_TEXT, true);
        List<String> refs = row.containsKey("evidenceRefs") ? stringList(row.get("evidenceRefs"), "evidenceRefs", MAX_REFS, 48) : List.of();
        for (String ref : refs) if (!EditorialP5RawWireContract.token(ref, EditorialP5RawWireContract.MAX_ID_LENGTH)) throw bad("L1_EVIDENCE_REF_INVALID");
        List<String> candidateIds = row.containsKey("candidateIds") ? stringList(row.get("candidateIds"), "candidateIds", 8, 48) : List.of();
        for (String candidate : candidateIds) if (!knownCandidates.contains(candidate)) throw bad("L1_CANDIDATE_REF_UNKNOWN");
        List<String> occurrences = row.containsKey("occurrenceUnits")
                ? stringList(row.get("occurrenceUnits"), "occurrenceUnits", MAX_OCCURRENCE_UNITS, 64) : List.of();
        Set<String> seen = new HashSet<>(rawUnits);
        for (String unit : occurrences) {
            if (!inventory.has(unit)) throw bad("L1_UNIT_UNKNOWN");
            if (!seen.add(unit)) throw bad("L1_OCCURRENCE_DUPLICATE");
        }
        String disposition = enumOf(row, "disposition", DISPOSITIONS);
        String limit = row.containsKey("evidenceLimit") ? str(row, "evidenceLimit", MAX_TEXT, false) : "";
        if ("PRESERVED".equals(disposition) && limit.isEmpty()) throw bad("L1_PRESERVED_NEEDS_EVIDENCE_LIMIT");
        return new Finding(errorId, type, severity, rawUnits, anchor, rawQuote, draftQuote, observation, expected, refs,
                candidateIds, occurrences, disposition, limit);
    }

    private static Disposition disposition(Map<String, Object> row) {
        keys(row, Set.of("disposition", "reasonCode", "stopClass"), "disposition");
        String kind = str(row, "disposition", 24, true);
        String reason = str(row, "reasonCode", 32, true);
        String stopClass = str(row, "stopClass", 24, true);
        if (!EditorialP5RawWireContract.safeText(reason)) throw bad("L1_DISPOSITION_REASON_INVALID");
        if ("CONTINUE".equals(kind) || "PRESERVE_DRAFT".equals(kind)) {
            if (!"NONE".equals(stopClass)) throw bad("L1_DISPOSITION_STOP_CLASS_INVALID");
        } else if ("STOP".equals(kind)) {
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
                       Metrics metrics) {
        public Body {
            rawCoverage = List.copyOf(rawCoverage);
            candidates = List.copyOf(candidates);
            reviewCoverage = List.copyOf(reviewCoverage);
            resolutions = List.copyOf(resolutions);
            findings = List.copyOf(findings);
            speakerRecords = List.copyOf(speakerRecords);
            protectedSpans = List.copyOf(protectedSpans);
        }
    }

    public static Body bodyOfRawPass(EditorialRawInventory.Inventory inventory, RawPass raw) {
        Metrics metrics = new Metrics(0, 0, 0, 0, raw.candidates().size(), 0, 0, 0, 0, inventory.units().size(), inventory.excluded().size());
        return new Body("L1_RAW_DISCOVERY", EditorialRawInventory.REVISION, inventory.units().size(), inventory.excluded().size(),
                inventory.inventorySha256(), inventory.rawSha256(), raw.coverage(), raw.candidates(), List.of(), List.of(), List.of(),
                List.of(), List.of(), new Disposition("CONTINUE", "L1_RAW_LEDGER", "NONE"), metrics);
    }

    public static Body bodyOfReconcile(EditorialRawInventory.Inventory inventory, RawPass raw, ReconcilePass pass) {
        return new Body("L1_RECONCILE", EditorialRawInventory.REVISION, inventory.units().size(), inventory.excluded().size(),
                inventory.inventorySha256(), inventory.rawSha256(), raw.coverage(), raw.candidates(), pass.coverage(), pass.resolutions(),
                pass.findings(), pass.speakerRecords(), pass.protectedSpans(), pass.disposition(),
                metrics(inventory, raw.candidates(), pass));
    }

    public static Map<String, Object> bodyToMap(Body body) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contractRevision", EditorialContractRevision.L1_LEDGER_V2);
        m.put("phase", body.phase());
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
        if (!EditorialContractRevision.L1_LEDGER_V2.equals(report.get("contractRevision"))) throw bad("L1_REPORT_NOT_LEDGER_V2");
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
        return new Body((String) report.get("phase"), (String) inv.get("revision"), i(inv, "unitCount"), i(inv, "excludedLineCount"),
                (String) inv.get("inventorySha256"), (String) inv.get("rawSha256"), rawCoverage, candidates,
                rangesFromList(report.get("reviewCoverage")), resolutions, findings, speakers, spans,
                new Disposition((String) d.get("disposition"), (String) d.get("reasonCode"), (String) d.get("stopClass")), metrics);
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

    private static List<EditorialRawInventory.Range> coverage(Object value, EditorialRawInventory.Inventory inventory) {
        List<Object> rows = EditorialCanonicalJson.array(value, "coverage");
        if (rows.size() > MAX_RANGES) throw bad("L1_COVERAGE_RANGE_LIMIT_EXCEEDED");
        List<EditorialRawInventory.Range> ranges = new ArrayList<>();
        for (Object o : rows) {
            Map<String, Object> row = object(o, "range");
            keys(row, Set.of("from", "to", "status"), "range");
            ranges.add(new EditorialRawInventory.Range(str(row, "from", 64, true), str(row, "to", 64, true),
                    enumOf(row, "status", RANGE_STATUSES)));
        }
        List<String> issues = EditorialRawInventory.coverageIssues(inventory, ranges);
        if (!issues.isEmpty()) {
            // the most specific defect wins, so a reversed range is not reported as the gap it leaves behind
            for (String[] known : new String[][] {{"COVERAGE_UNKNOWN", "L1_COVERAGE_UNKNOWN_ID"},
                    {"COVERAGE_REVERSED", "L1_COVERAGE_REVERSED"}, {"COVERAGE_OVERLAP", "L1_COVERAGE_OVERLAP"},
                    {"COVERAGE_GAP", "L1_COVERAGE_GAP"}}) {
                for (String issue : issues) if (issue.startsWith(known[0])) throw bad(known[1]);
            }
            throw bad("L1_COVERAGE_EMPTY");
        }
        return ranges;
    }

    private static Map<String, Object> rootOf(byte[] bytes, String attemptIdentity, String wire, Set<String> keys) {
        if (bytes == null || bytes.length > MAX_WIRE_BYTES) throw bad("L1_WIRE_BYTE_LIMIT_EXCEEDED");
        Map<String, Object> root = EditorialCanonicalJson.parseObject(bytes);
        keys(root, keys, "root");
        if (!wire.equals(root.get("wireSchemaVersion"))) throw bad("L1_WIRE_SCHEMA_INVALID");
        if (!attemptIdentity.equals(root.get("attemptIdentity"))) throw bad("L1_WIRE_ATTEMPT_ECHO_MISMATCH");
        return root;
    }

    private static String id(Map<String, Object> row, String key) {
        String value = str(row, key, EditorialP5RawWireContract.MAX_ID_LENGTH, true);
        if (!EditorialP5RawWireContract.token(value, EditorialP5RawWireContract.MAX_ID_LENGTH)) throw bad("L1_ID_INVALID");
        return value;
    }

    private static String enumOf(Map<String, Object> row, String key, Set<String> allowed) {
        String value = str(row, key, 32, true);
        if (!allowed.contains(value)) throw bad("L1_ENUM_INVALID");
        return value;
    }

    private static String str(Map<String, Object> row, String key, int max, boolean required) {
        Object value = row.get(key);
        if (!(value instanceof String)) throw bad("L1_TEXT_INVALID");
        String text = (String) value;
        if (text.length() > max) throw bad("L1_TEXT_TOO_LONG");
        if (required && text.isBlank()) throw bad("L1_TEXT_REQUIRED");
        for (int i = 0; i < text.length(); i++) if (Character.isISOControl(text.charAt(i))) throw bad("L1_TEXT_CONTROL_CHARACTER");
        return text;
    }

    private static int intOf(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (!(value instanceof BigDecimal)) throw bad("L1_INT_INVALID");
        try {
            return ((BigDecimal) value).intValueExact();
        } catch (ArithmeticException invalid) {
            throw bad("L1_INT_INVALID");
        }
    }

    private static List<String> stringList(Object value, String path, int maxItems, int maxLength) {
        List<Object> rows = EditorialCanonicalJson.array(value, path);
        if (rows.size() > maxItems) throw bad("L1_LIST_TOO_LONG");
        List<String> out = new ArrayList<>();
        for (Object o : rows) {
            if (!(o instanceof String) || ((String) o).isBlank() || ((String) o).length() > maxLength) throw bad("L1_LIST_ITEM_INVALID");
            out.add((String) o);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map)) throw bad("L1_OBJECT_EXPECTED");
        return (Map<String, Object>) value;
    }

    private static void keys(Map<String, Object> value, Set<String> allowed, String path) {
        keys(value, allowed, path, Set.of());
    }

    private static void keys(Map<String, Object> value, Set<String> allowed, String path, Set<String> optional) {
        for (String key : value.keySet()) if (!allowed.contains(key)) throw bad("L1_UNKNOWN_KEY");
        for (String key : allowed) if (!optional.contains(key) && !value.containsKey(key)) throw bad("L1_MISSING_KEY");
    }

    private static int i(Map<String, Object> m, String key) { return ((BigDecimal) m.get(key)).intValueExact(); }

    private static BigDecimal num(int value) { return BigDecimal.valueOf(value); }

    private static IllegalArgumentException bad(String code) { return new IllegalArgumentException(code); }

    /** Typed, allow-listed message of a parse failure; anything else collapses to a generic code. */
    public static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message != null && message.matches("[A-Z0-9_:./-]{1,96}") ? message : "L1_WIRE_PARSE_FAILED";
    }

    /** Sorted copy used by tests and reports to compare ledgers independent of insertion order. */
    public static Map<String, Integer> findingCountByType(List<Finding> findings) {
        Map<String, Integer> counts = new TreeMap<>();
        for (Finding f : findings) counts.merge(f.type(), 1, Integer::sum);
        return counts;
    }
}
