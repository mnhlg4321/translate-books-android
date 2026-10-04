package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * How an L2 edit answers every finding of a ledger-contract REPORT_L1. The model states a status per finding;
 * the app verifies the claim against the declared change rows, the preserved rows, the RAW text and the actual
 * reconstruction: a fix must name changes that carry the finding's id, sit on its DRAFT anchor and survive the
 * reconstruction; every other place the finding was reported must point at a change or a preserved row; a
 * rejection must quote RAW; nothing is accepted because the model said so. A finding that stays open is a
 * typed stop, never a silent pass.
 */
final class EditorialL2Findings {
    static final Set<String> STATUSES = Set.of("FIXED", "REJECTED", "PRESERVED", "UNRESOLVED");
    static final int MAX_REFS = EditorialFieldSpec.MAX_CHANGE_REFS;
    static final int MAX_OCCURRENCES = EditorialL1Ledger.MAX_OCCURRENCE_UNITS;

    record Occurrence(String unitId, String ref) { }

    record Resolution(String errorId, String status, List<String> changeIds, List<String> preserveIds,
                      List<Occurrence> occurrences, String evidenceQuote, String reason) { }

    /** Result of the app's check: structural problems, findings that stay open, and the effective status of each. */
    record Verdict(List<String> issues, List<String> unresolved, Map<String, String> effective) { }

    private EditorialL2Findings() { }

    /** Strict parse of the {@code findingResolutions} array; completeness is judged by {@link #verify}. */
    static List<Resolution> parse(Object rows, Set<String> knownErrorIds, EditorialRawInventory.Inventory inventory) {
        List<Object> values;
        try { values = EditorialCanonicalJson.array(rows, "findingResolutions"); }
        catch (RuntimeException invalid) { throw WireViolation.from(invalid, "L2_WIRE_ARRAY_INVALID", "findingResolutions"); }
        if (values.size() > EditorialL1Ledger.MAX_FINDINGS_PER_CALL) throw bad("L2_WIRE_ROW_LIMIT_EXCEEDED", "findingResolutions");
        List<Resolution> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String path = "findingResolutions." + index;
            Map<String, Object> row = EditorialL2Execution.object(values.get(index), path);
            EditorialL2Execution.keys(row, Set.of("errorId", "status", "changeIds", "preserveIds", "occurrences",
                    "evidenceQuote", "reason"), path);
            String errorId = EditorialL2Execution.text(row, "errorId", path + ".errorId");
            if (!knownErrorIds.contains(errorId) || !seen.add(errorId)) throw bad("L2_WIRE_FINDING_ID_INVALID", path + ".errorId");
            String status = EditorialL2Execution.text(row, "status", path + ".status");
            if (!STATUSES.contains(status)) throw bad("L2_WIRE_FINDING_STATUS_INVALID", path + ".status");
            List<String> changeIds = ids(row.get("changeIds"), MAX_REFS, path + ".changeIds");
            List<String> preserveIds = ids(row.get("preserveIds"), MAX_REFS, path + ".preserveIds");
            List<Occurrence> occurrences = new ArrayList<>();
            List<Object> occurrenceRows = EditorialCanonicalJson.array(row.get("occurrences"), path + ".occurrences");
            for (int occurrenceIndex = 0; occurrenceIndex < occurrenceRows.size(); occurrenceIndex++) {
                String occurrencePath = path + ".occurrences." + occurrenceIndex;
                Map<String, Object> occ = EditorialL2Execution.object(occurrenceRows.get(occurrenceIndex), occurrencePath);
                EditorialL2Execution.keys(occ, Set.of("unitId", "ref"), occurrencePath);
                occurrences.add(new Occurrence(EditorialUnitReference.resolve(occ.get("unitId"), inventory, occurrencePath + ".unitId"),
                        EditorialL2Execution.text(occ, "ref", occurrencePath + ".ref")));
            }
            if (occurrences.size() > MAX_OCCURRENCES) throw bad("L2_WIRE_ROW_LIMIT_EXCEEDED", path + ".occurrences");
            String quote = EditorialL2Execution.text(row, "evidenceQuote", path + ".evidenceQuote");
            String reason = EditorialL2Execution.text(row, "reason", path + ".reason");
            if (quote.length() > EditorialL1Ledger.MAX_QUOTE || reason.length() > EditorialL1Ledger.MAX_TEXT) {
                throw bad("L2_WIRE_TEXT_INVALID", path);
            }
            result.add(new Resolution(errorId, status, changeIds, preserveIds, occurrences, quote, reason));
        }
        return List.copyOf(result);
    }

    static Verdict verify(EditorialL1Ledger.Body l1, EditorialRawInventory.Inventory inventory, List<Resolution> resolutions,
                          List<EditorialChangeMapReconstructor.ChangeRow> changes,
                          List<EditorialChangeMapReconstructor.PreservedRow> preserved,
                          EditorialChangeMapReconstructor.Result reconstruction) {
        List<String> issues = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        Map<String, String> effective = new LinkedHashMap<>();
        Map<String, Resolution> byId = new HashMap<>();
        for (Resolution r : resolutions) byId.put(r.errorId(), r);
        Map<String, EditorialChangeMapReconstructor.ChangeRow> changeById = new HashMap<>();
        for (EditorialChangeMapReconstructor.ChangeRow row : changes) changeById.put(row.changeId(), row);
        Set<String> applied = new HashSet<>();
        for (EditorialChangeMapReconstructor.AppliedChange a : reconstruction.applied()) applied.add(a.row().changeId());
        Map<String, EditorialChangeMapReconstructor.PreservedRow> preservedById = new HashMap<>();
        for (EditorialChangeMapReconstructor.PreservedRow row : preserved) preservedById.put(row.preserveId(), row);
        Set<String> l1Ids = new HashSet<>();
        for (EditorialL1Ledger.Finding f : l1.findings()) l1Ids.add(f.errorId());

        // every change belongs to a known L1 finding or to a finding L2 opened itself
        for (EditorialChangeMapReconstructor.ChangeRow row : changes) {
            if (!l1Ids.contains(row.errorId()) && !row.errorId().startsWith("L2-")) {
                issues.add("L2_CHANGE_ERROR_ID_UNKNOWN:" + row.changeId());
            }
        }

        for (EditorialL1Ledger.Finding f : l1.findings()) {
            String id = f.errorId();
            Resolution r = byId.get(id);
            if (r == null) {
                issues.add("L2_FINDING_NOT_RESOLVED:" + id);
                continue;
            }
            String status = r.status();
            Set<String> closedForFinding = new TreeSet<>();
            for (EditorialChangeMapReconstructor.ChangeRow row : changes) {
                if (id.equals(row.errorId()) && row.status() == EditorialChangeMapReconstructor.DeclaredStatus.CLOSED) {
                    closedForFinding.add(row.changeId());
                }
            }
            switch (status) {
                case "FIXED" -> status = verifyFixed(f, r, changeById, applied, preservedById, closedForFinding, issues);
                case "REJECTED" -> verifyRejected(f, r, inventory, closedForFinding, issues);
                case "PRESERVED" -> verifyPreserved(f, r, preservedById, closedForFinding, issues);
                default -> {
                    if (r.reason().isBlank()) issues.add("L2_FINDING_UNRESOLVED_WITHOUT_REASON:" + id);
                }
            }
            effective.put(id, status);
            if ("UNRESOLVED".equals(status)) unresolved.add(id);
        }
        return new Verdict(List.copyOf(issues), List.copyOf(unresolved), effective);
    }

    private static String verifyFixed(EditorialL1Ledger.Finding f, Resolution r,
                                      Map<String, EditorialChangeMapReconstructor.ChangeRow> changeById,
                                      Set<String> applied,
                                      Map<String, EditorialChangeMapReconstructor.PreservedRow> preservedById,
                                      Set<String> closedForFinding, List<String> issues) {
        String id = f.errorId();
        final int issuesBefore = issues.size();
        boolean open = false;
        if (r.changeIds().isEmpty()) issues.add("L2_FINDING_FIXED_WITHOUT_CHANGE:" + id);
        boolean anchored = false;
        for (String cid : r.changeIds()) {
            EditorialChangeMapReconstructor.ChangeRow row = changeById.get(cid);
            if (row == null) {
                issues.add("L2_FINDING_CHANGE_UNKNOWN:" + id + ":" + cid);
            } else if (!id.equals(row.errorId())) {
                issues.add("L2_FINDING_CHANGE_ERROR_MISMATCH:" + id + ":" + cid);
            } else if (row.status() != EditorialChangeMapReconstructor.DeclaredStatus.CLOSED) {
                issues.add("L2_FINDING_CHANGE_NOT_CLOSED:" + id + ":" + cid);
            } else if (!applied.contains(cid)) {
                open = true;   // the app reverted it (protected span, missing speaker proof): the defect is still there
            } else {
                anchored |= onAnchor(f, row);
            }
        }
        if (!open && !r.changeIds().isEmpty() && !anchored && issues.size() == issuesBefore) {
            issues.add("L2_FINDING_FIX_OFF_ANCHOR:" + id);
        }
        for (String cid : closedForFinding) {
            if (!r.changeIds().contains(cid)) issues.add("L2_FINDING_CHANGE_NOT_LISTED:" + id + ":" + cid);
        }
        // every other place the finding was reported must be tied to a change or a preserved row
        Set<String> expected = new TreeSet<>(f.occurrenceUnits());
        Set<String> mapped = new TreeSet<>();
        for (Occurrence occ : r.occurrences()) {
            if (!expected.contains(occ.unitId()) || !mapped.add(occ.unitId())) {
                issues.add("L2_FINDING_OCCURRENCE_UNKNOWN:" + id + ":" + occ.unitId());
                continue;
            }
            boolean viaChange = r.changeIds().contains(occ.ref());
            boolean viaPreserve = r.preserveIds().contains(occ.ref()) && preservedById.containsKey(occ.ref());
            if (!viaChange && !viaPreserve) issues.add("L2_FINDING_OCCURRENCE_UNBACKED:" + id + ":" + occ.unitId());
            if (viaChange && !applied.contains(occ.ref())) open = true;
        }
        for (String unit : expected) if (!mapped.contains(unit)) issues.add("L2_FINDING_OCCURRENCE_MISSING:" + id + ":" + unit);
        return open ? "UNRESOLVED" : "FIXED";
    }

    private static boolean onAnchor(EditorialL1Ledger.Finding f, EditorialChangeMapReconstructor.ChangeRow row) {
        EditorialL1Ledger.DraftAnchor anchor = f.draft();
        int line = row.lineNumber();
        boolean merge = row.op() == EditorialChangeMapReconstructor.Op.MERGE_WITH_NEXT;
        if ("LINES".equals(anchor.kind())) {
            if (row.op() == EditorialChangeMapReconstructor.Op.INSERT_AFTER) return line >= anchor.start() - 1 && line <= anchor.end();
            return (line >= anchor.start() && line <= anchor.end()) || (merge && line + 1 >= anchor.start() && line + 1 <= anchor.end());
        }
        return row.op() == EditorialChangeMapReconstructor.Op.INSERT_AFTER && Math.abs(line - anchor.after()) <= 2;
    }

    private static void verifyRejected(EditorialL1Ledger.Finding f, Resolution r, EditorialRawInventory.Inventory inventory,
                                       Set<String> closedForFinding, List<String> issues) {
        String id = f.errorId();
        if (r.reason().isBlank()) issues.add("L2_FINDING_REJECTED_WITHOUT_REASON:" + id);
        boolean quoted = false;
        if (!r.evidenceQuote().isEmpty()) {
            List<String> units = new ArrayList<>(f.rawUnits());
            units.addAll(f.occurrenceUnits());
            for (String unit : units) {
                EditorialRawInventory.Unit u = inventory.unit(unit);
                quoted |= u != null && EditorialQuoteMatcher.containsRaw(u.text(), r.evidenceQuote());
            }
        }
        if (!quoted) issues.add("L2_FINDING_REJECTED_WITHOUT_RAW_EVIDENCE:" + id);
        if (!r.changeIds().isEmpty() || !closedForFinding.isEmpty()) issues.add("L2_FINDING_REJECTED_BUT_CHANGED:" + id);
    }

    private static void verifyPreserved(EditorialL1Ledger.Finding f, Resolution r,
                                        Map<String, EditorialChangeMapReconstructor.PreservedRow> preservedById,
                                        Set<String> closedForFinding, List<String> issues) {
        String id = f.errorId();
        if (r.preserveIds().isEmpty()) issues.add("L2_FINDING_PRESERVED_WITHOUT_ROW:" + id);
        for (String pid : r.preserveIds()) {
            EditorialChangeMapReconstructor.PreservedRow row = preservedById.get(pid);
            if (row == null) {
                issues.add("L2_FINDING_PRESERVE_UNKNOWN:" + id + ":" + pid);
            } else if ("LINES".equals(f.draft().kind()) && (row.lineNumber() < f.draft().start() || row.lineNumber() > f.draft().end())) {
                issues.add("L2_FINDING_PRESERVE_OFF_ANCHOR:" + id + ":" + pid);
            }
        }
        if (!r.changeIds().isEmpty() || !closedForFinding.isEmpty()) issues.add("L2_FINDING_PRESERVED_BUT_CHANGED:" + id);
        Set<String> expected = new TreeSet<>(f.occurrenceUnits());
        Set<String> mapped = new TreeSet<>();
        for (Occurrence occ : r.occurrences()) {
            if (!expected.contains(occ.unitId()) || !mapped.add(occ.unitId()) || !r.preserveIds().contains(occ.ref())) {
                issues.add("L2_FINDING_OCCURRENCE_UNBACKED:" + id + ":" + occ.unitId());
            }
        }
        for (String unit : expected) if (!mapped.contains(unit)) issues.add("L2_FINDING_OCCURRENCE_MISSING:" + id + ":" + unit);
    }

    /** Canonical evidence stored in CHANGE_MAP_L2: what became of every L1 finding. */
    static Map<String, Object> evidence(EditorialL1Ledger.Body l1, Verdict verdict, List<Resolution> resolutions, byte[] reportL1Bytes) {
        Map<String, Resolution> byId = new HashMap<>();
        for (Resolution r : resolutions) byId.put(r.errorId(), r);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contractRevision", EditorialContractRevision.CURRENT_LEDGER);
        m.put("reportL1Sha256", EditorialCanonicalJson.sha256Hex(reportL1Bytes));
        Map<String, Object> counts = new LinkedHashMap<>();
        for (String s : new String[] {"FIXED", "REJECTED", "PRESERVED", "UNRESOLVED"}) counts.put(s, java.math.BigDecimal.ZERO);
        List<Object> rows = new ArrayList<>();
        for (EditorialL1Ledger.Finding f : l1.findings()) {
            String effective = verdict.effective().getOrDefault(f.errorId(), "UNRESOLVED");
            counts.merge(effective, java.math.BigDecimal.ONE, (a, b) -> ((java.math.BigDecimal) a).add((java.math.BigDecimal) b));
            Resolution r = byId.get(f.errorId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("errorId", f.errorId());
            row.put("type", f.type());
            row.put("severity", f.severity());
            row.put("l1Disposition", f.disposition());
            row.put("status", effective);
            row.put("claimedStatus", r == null ? "NONE" : r.status());
            row.put("changeIds", r == null ? new ArrayList<Object>() : new ArrayList<Object>(r.changeIds()));
            row.put("preserveIds", r == null ? new ArrayList<Object>() : new ArrayList<Object>(r.preserveIds()));
            row.put("occurrencesCovered", java.math.BigDecimal.valueOf(r == null ? 0 : r.occurrences().size()));
            row.put("occurrencesExpected", java.math.BigDecimal.valueOf(f.occurrenceUnits().size()));
            rows.add(row);
        }
        m.put("counts", counts);
        m.put("findings", rows);
        return m;
    }

    private static List<String> ids(Object value, int max, String path) {
        List<Object> values = EditorialCanonicalJson.array(value, path);
        if (values.size() > max) throw bad("L2_WIRE_ROW_LIMIT_EXCEEDED", path);
        List<String> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            Object o = values.get(index);
            if (!(o instanceof String) || !EditorialP5RawWireContract.token((String) o, EditorialP5RawWireContract.MAX_ID_LENGTH)) {
                throw bad("L2_WIRE_ID_INVALID", path + "." + index);
            }
            result.add((String) o);
        }
        return List.copyOf(result);
    }

    private static IllegalArgumentException bad(String code) { return WireViolation.at(code, "root"); }
    private static IllegalArgumentException bad(String code, String path) { return WireViolation.at(code, path); }
}
