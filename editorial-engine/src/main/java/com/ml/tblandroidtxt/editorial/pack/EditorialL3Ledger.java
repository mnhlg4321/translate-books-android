package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The ledger-contract side of L3: a blind re-audit that reports coverage over the app's RAW inventory with
 * sparse candidates anchored on a RAW unit and a VI_L2 line, anchored probes in place of free-text
 * "no defect" sentences, and a resolution for every defect the L2 final read left behind. The model states
 * judgement; the app checks every anchor, quote, id and action against the exact bytes and against the
 * changes it actually applied.
 */
final class EditorialL3Ledger {
    static final String REAUDIT_WIRE_V3 = "safe4.l3.reaudit.wire.v3";
    static final String RECONCILE_WIRE_V3 = "safe4.l3.reconcile.wire.v3";
    static final String CARRIED_ROLE = "L3_CARRIED_DEFECTS";
    static final Set<String> CANDIDATE_STATUSES = Set.of("PROCESSED", "PRESERVED", "UNPROCESSED", "CONFLICT");
    static final Set<String> PROBE_KINDS = Set.of("COVERAGE", "REGRESSION");
    static final Set<String> PROBE_VERDICTS = Set.of("NO_DEFECT", "DEFECT_FOUND", "PRESERVED", "CONFLICT");
    static final Set<String> CARRIED_STATUSES = Set.of("FIXED", "REJECTED", "PRESERVED", "UNRESOLVED");
    static final int MIN_PROBES_PER_KIND = 3;
    static final int MAX_PROBES = EditorialFieldSpec.MAX_PROBES;
    static final int MAX_PROBE_TEXT = EditorialFieldSpec.MAX_PROBE_TEXT;
    static final int MAX_CANDIDATES = EditorialFieldSpec.MAX_L3_CANDIDATES;

    record Candidate(String candidateId, String ledger, String unitId, int viLine, String status, String note) { }

    record ReauditPass(List<EditorialRawInventory.Range> coverage, List<Candidate> candidates) { }

    /** A defect the final read of VI_L2 reported; L3 must answer each one. */
    record Carried(int index, int line, String quote, String type, String note) { }

    record CarriedResolution(int index, String status, List<String> changeIds, List<String> preserveIds,
                             String evidenceQuote, String reason) { }

    record Probe(String probeId, String kind, List<String> rawUnits, int viStart, int viEnd, String scope,
                 String contrast, String rawQuote, String viQuote, String verdict, String action) { }

    record ReconcileWire(Map<String, String> resolutions, List<CarriedResolution> carried,
                         EditorialL2Execution.Wire rows, List<Probe> probes) { }

    private EditorialL3Ledger() { }

    // ---- blind re-audit ----

    static ReauditPass parseReaudit(byte[] bytes, String attemptIdentity, EditorialRawInventory.Inventory inventory,
                                    int viLineCount) {
        Map<String, Object> root = EditorialL1Ledger.rootOf(bytes, attemptIdentity, REAUDIT_WIRE_V3,
                Set.of("wireSchemaVersion", "attemptIdentity", "coverage", "candidates"));
        List<EditorialRawInventory.Range> coverage = EditorialL1Ledger.coverage(root.get("coverage"), inventory,
                EditorialFieldSpec.L3_RAW_FIRST_REAUDIT);
        List<Object> rows = EditorialCanonicalJson.array(root.get("candidates"), "candidates");
        if (rows.size() > MAX_CANDIDATES) throw EditorialL1Ledger.bad("L3_CANDIDATE_LIMIT_EXCEEDED");
        List<Candidate> candidates = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < rows.size(); index++) {
            String path = "candidates." + index;
            Map<String, Object> row = EditorialL1Ledger.object(rows.get(index), path);
            EditorialL1Ledger.keys(row, Set.of("candidateId", "ledger", "unitId", "viLine", "status", "note"), path);
            String id = EditorialL1Ledger.id(row, "candidateId", path + ".candidateId", EditorialFieldSpec.L3_RAW_FIRST_REAUDIT);
            if (!ids.add(id)) throw EditorialL1Ledger.bad("L3_CANDIDATE_ID_DUPLICATE", path + ".candidateId");
            String ledger = EditorialL1Ledger.enumOf(row, "ledger", EditorialL1Ledger.CANDIDATE_LEDGERS,
                    path + ".ledger", EditorialFieldSpec.L3_RAW_FIRST_REAUDIT);
            String unit = EditorialUnitReference.resolve(row.get("unitId"), inventory, path + ".unitId");
            int viLine = EditorialL1Ledger.intOf(row, "viLine", path + ".viLine");
            if (viLine < 0 || viLine > viLineCount) throw EditorialL1Ledger.bad("L3_VI_LINE_OUT_OF_RANGE", path + ".viLine");
            String status = EditorialL1Ledger.enumOf(row, "status", CANDIDATE_STATUSES, path + ".status",
                    EditorialFieldSpec.L3_RAW_FIRST_REAUDIT);
            candidates.add(new Candidate(id, ledger, unit, viLine, status, EditorialL1Ledger.fieldStr(
                    EditorialFieldSpec.L3_RAW_FIRST_REAUDIT, row, "note", path + ".note")));
        }
        return new ReauditPass(coverage, List.copyOf(candidates));
    }

    /** The app-owned block the reconcile call reads. */
    static byte[] candidateBlock(List<Candidate> candidates) {
        List<Object> rows = new ArrayList<>();
        for (Candidate c : candidates) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("candidateId", c.candidateId());
            row.put("ledger", c.ledger());
            row.put("unitId", EditorialUnitReference.fromId(c.unitId()));
            row.put("viLine", BigDecimal.valueOf(c.viLine()));
            row.put("status", c.status());
            row.put("note", c.note());
            rows.add(row);
        }
        return EditorialCanonicalJson.canonicalize(Map.of("candidates", rows)).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ---- carried defects of the L2 final read ----

    /** Defects recorded in a CHANGE_MAP_L2 by the final read of VI_L2 (empty for clean reads and legacy maps). */
    @SuppressWarnings("unchecked")
    static List<Carried> carriedFrom(byte[] changeMapL2) {
        Map<String, Object> map = EditorialCanonicalJson.parseObject(changeMapL2);
        Object block = map.get("finalRead");
        if (!(block instanceof Map)) return List.of();
        EditorialFinalRead.Result read = EditorialFinalRead.parseEvidence((Map<String, Object>) block);
        List<Carried> result = new ArrayList<>();
        for (int i = 0; i < read.defects().size(); i++) {
            EditorialFinalRead.Defect d = read.defects().get(i);
            result.add(new Carried(i, d.line(), d.quote(), d.type(), d.note()));
        }
        return List.copyOf(result);
    }

    static byte[] carriedBlock(List<Carried> carried) {
        List<Object> rows = new ArrayList<>();
        for (Carried c : carried) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("index", BigDecimal.valueOf(c.index()));
            row.put("line", BigDecimal.valueOf(c.line()));
            row.put("quote", c.quote());
            row.put("type", c.type());
            row.put("note", c.note());
            rows.add(row);
        }
        return EditorialCanonicalJson.canonicalize(Map.of("defects", rows)).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ---- reconcile ----

    static ReconcileWire parseReconcile(byte[] bytes, String attemptIdentity, List<Candidate> candidates, int carriedCount,
                                        EditorialRawInventory.Inventory inventory, byte[] baseBytes) {
        Map<String, Object> root = EditorialL1Ledger.rootOf(bytes, attemptIdentity, RECONCILE_WIRE_V3,
                Set.of("wireSchemaVersion", "attemptIdentity", "resolutions", "carriedResolutions", "changes", "preserved",
                        "probes", "disposition"));
        Map<String, Object> shape = new LinkedHashMap<>();
        shape.put("wireSchemaVersion", EditorialL2Execution.WIRE_SCHEMA_VERSION);
        shape.put("attemptIdentity", attemptIdentity);
        shape.put("changes", root.get("changes"));
        shape.put("preserved", root.get("preserved"));
        shape.put("disposition", root.get("disposition"));
        EditorialL2Execution.Wire rows = EditorialL2Execution.parseWire(
                EditorialCanonicalJson.canonicalize(shape).getBytes(java.nio.charset.StandardCharsets.UTF_8), attemptIdentity, true,
                EditorialFieldSpec.L3_RECONCILE, baseBytes, true);

        Set<String> known = new HashSet<>();
        for (Candidate candidate : candidates) known.add(candidate.candidateId());
        Map<String, String> resolutions = new java.util.TreeMap<>();
        List<Object> resolutionRows = EditorialCanonicalJson.array(root.get("resolutions"), "resolutions");
        for (int index = 0; index < resolutionRows.size(); index++) {
            String path = "resolutions." + index;
            Map<String, Object> row = EditorialL1Ledger.object(resolutionRows.get(index), path);
            EditorialL1Ledger.keys(row, Set.of("candidateId", "status"), path);
            String id = EditorialL1Ledger.id(row, "candidateId", path + ".candidateId", EditorialFieldSpec.L3_RECONCILE);
            if (!known.contains(id) || resolutions.containsKey(id)) throw EditorialL1Ledger.bad("L3_RESOLUTION_ID_INVALID", path + ".candidateId");
            resolutions.put(id, EditorialL1Ledger.enumOf(row, "status", CANDIDATE_STATUSES, path + ".status",
                    EditorialFieldSpec.L3_RECONCILE));
        }

        List<CarriedResolution> carried = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        List<Object> carriedRows = EditorialCanonicalJson.array(root.get("carriedResolutions"), "carriedResolutions");
        for (int index = 0; index < carriedRows.size(); index++) {
            String path = "carriedResolutions." + index;
            Map<String, Object> row = EditorialL1Ledger.object(carriedRows.get(index), path);
            EditorialL1Ledger.keys(row, Set.of("index", "status", "changeIds", "preserveIds", "evidenceQuote", "reason"), path);
            int carriedIndex = EditorialL1Ledger.intOf(row, "index", path + ".index");
            if (carriedIndex < 0 || carriedIndex >= carriedCount || !seen.add(carriedIndex)) throw EditorialL1Ledger.bad("L3_CARRIED_INDEX_INVALID", path + ".index");
            carried.add(new CarriedResolution(carriedIndex, EditorialL1Ledger.enumOf(row, "status", CARRIED_STATUSES,
                    path + ".status", EditorialFieldSpec.L3_RECONCILE),
                    refs(row.get("changeIds"), path + ".changeIds"), refs(row.get("preserveIds"), path + ".preserveIds"),
                    EditorialL1Ledger.fieldStr(EditorialFieldSpec.L3_RECONCILE, row, "evidenceQuote", path + ".evidenceQuote"),
                    EditorialL1Ledger.fieldStr(EditorialFieldSpec.L3_RECONCILE, row, "reason", path + ".reason")));
        }

        List<Object> probeRows = EditorialCanonicalJson.array(root.get("probes"), "probes");
        if (probeRows.size() > MAX_PROBES) throw EditorialL1Ledger.bad("L3_PROBE_LIMIT_EXCEEDED");
        List<Probe> probes = new ArrayList<>();
        Set<String> probeIds = new HashSet<>();
        for (int index = 0; index < probeRows.size(); index++) {
            String path = "probes." + index;
            Map<String, Object> row = EditorialL1Ledger.object(probeRows.get(index), path);
            EditorialL1Ledger.keys(row, Set.of("probeId", "kind", "rawUnits", "viStart", "viEnd", "scope", "contrast",
                    "rawQuote", "viQuote", "verdict", "action"), path);
            String id = EditorialL1Ledger.id(row, "probeId", path + ".probeId", EditorialFieldSpec.L3_RECONCILE);
            if (!probeIds.add(id)) throw EditorialL1Ledger.bad("L3_PROBE_ID_DUPLICATE", path + ".probeId");
            probes.add(new Probe(id, EditorialL1Ledger.enumOf(row, "kind", PROBE_KINDS, path + ".kind", EditorialFieldSpec.L3_RECONCILE),
                    EditorialUnitReference.resolveList(row.get("rawUnits"), path + ".rawUnits", EditorialL1Ledger.MAX_RAW_UNITS_PER_FINDING, inventory),
                    EditorialL1Ledger.intOf(row, "viStart", path + ".viStart"), EditorialL1Ledger.intOf(row, "viEnd", path + ".viEnd"),
                    EditorialL1Ledger.fieldStr(EditorialFieldSpec.L3_RECONCILE, row, "scope", path + ".scope"),
                    EditorialL1Ledger.fieldStr(EditorialFieldSpec.L3_RECONCILE, row, "contrast", path + ".contrast"),
                    EditorialL1Ledger.fieldStr(EditorialFieldSpec.L3_RECONCILE, row, "rawQuote", path + ".rawQuote"),
                    EditorialL1Ledger.fieldStr(EditorialFieldSpec.L3_RECONCILE, row, "viQuote", path + ".viQuote"),
                    EditorialL1Ledger.enumOf(row, "verdict", PROBE_VERDICTS, path + ".verdict", EditorialFieldSpec.L3_RECONCILE),
                    EditorialL1Ledger.fieldStr(EditorialFieldSpec.L3_RECONCILE, row, "action", path + ".action")));
        }
        return new ReconcileWire(Map.copyOf(resolutions), List.copyOf(carried), rows, List.copyOf(probes));
    }

    private static List<String> refs(Object value, String path) {
        List<String> ids = EditorialL1Ledger.stringList(value, path, EditorialL2Findings.MAX_REFS, EditorialP5RawWireContract.MAX_ID_LENGTH);
        for (int index = 0; index < ids.size(); index++) if (!EditorialP5RawWireContract.token(ids.get(index), EditorialP5RawWireContract.MAX_ID_LENGTH)) throw EditorialL1Ledger.bad("L3_ID_INVALID", path + "." + index);
        return ids;
    }

    // ---- verification against the exact bytes and the applied changes ----

    /** Probe problems; empty when every probe is anchored, quoted and coherent with what was applied. */
    static List<String> verifyProbes(List<Probe> probes, EditorialRawInventory.Inventory inventory, List<String> viLines,
                                     List<EditorialChangeMapReconstructor.ChangeRow> changes,
                                     List<EditorialChangeMapReconstructor.PreservedRow> preserved,
                                     EditorialChangeMapReconstructor.Result reconstruction, int[] conflictsOut) {
        List<String> issues = new ArrayList<>();
        Map<String, EditorialChangeMapReconstructor.ChangeRow> changeById = new HashMap<>();
        for (EditorialChangeMapReconstructor.ChangeRow row : changes) changeById.put(row.changeId(), row);
        Set<String> applied = new HashSet<>();
        for (EditorialChangeMapReconstructor.AppliedChange a : reconstruction.applied()) applied.add(a.row().changeId());
        Map<String, EditorialChangeMapReconstructor.PreservedRow> preservedById = new HashMap<>();
        for (EditorialChangeMapReconstructor.PreservedRow row : preserved) preservedById.put(row.preserveId(), row);

        int coverage = 0;
        int regression = 0;
        Set<String> anchors = new HashSet<>();
        Set<String> units = new TreeSet<>();
        int conflicts = 0;
        for (Probe p : probes) {
            String id = p.probeId();
            if ("COVERAGE".equals(p.kind())) coverage++; else regression++;
            if (p.rawUnits().isEmpty()) issues.add("L3_PROBE_RAW_ANCHOR_REQUIRED:" + id);
            boolean rawOk = !p.rawUnits().isEmpty();
            boolean quoted = false;
            for (String unit : p.rawUnits()) {
                EditorialRawInventory.Unit u = inventory.unit(unit);
                if (u == null) {
                    issues.add("L3_PROBE_UNIT_UNKNOWN:" + id);
                    rawOk = false;
                } else {
                    quoted |= u.text().contains(p.rawQuote());
                    units.add(unit);
                }
            }
            if (rawOk && !quoted) issues.add("L3_PROBE_RAW_QUOTE_NOT_IN_ANCHOR:" + id);
            if (p.viStart() < 1 || p.viEnd() < p.viStart() || p.viEnd() > viLines.size()) {
                issues.add("L3_PROBE_VI_ANCHOR_OUT_OF_RANGE:" + id);
            } else {
                StringBuilder span = new StringBuilder();
                for (int i = p.viStart(); i <= p.viEnd(); i++) span.append(viLines.get(i - 1)).append('\n');
                if (!span.toString().contains(p.viQuote())) issues.add("L3_PROBE_VI_QUOTE_NOT_IN_ANCHOR:" + id);
                List<String> sorted = new ArrayList<>(p.rawUnits());
                java.util.Collections.sort(sorted);
                if (!anchors.add(sorted + "|" + p.viStart() + "|" + p.viEnd())) issues.add("L3_PROBE_ANCHOR_DUPLICATE:" + id);
            }
            String action = p.action();
            switch (p.verdict()) {
                case "NO_DEFECT", "CONFLICT" -> {
                    if (!"NONE".equals(action)) issues.add("L3_PROBE_ACTION_INVALID:" + id);
                    if ("CONFLICT".equals(p.verdict())) conflicts++;
                }
                case "DEFECT_FOUND" -> {
                    String changeId = action.startsWith("CHANGE:") ? action.substring(7) : "";
                    EditorialChangeMapReconstructor.ChangeRow row = changeById.get(changeId);
                    if (row == null || row.status() != EditorialChangeMapReconstructor.DeclaredStatus.CLOSED) {
                        issues.add("L3_PROBE_ACTION_CHANGE_UNKNOWN:" + id);
                    } else if (!applied.contains(changeId)) {
                        conflicts++;   // the app reverted the fix: the defect the probe found is still there
                    } else if (!touches(row, p.viStart(), p.viEnd())) {
                        issues.add("L3_PROBE_ACTION_OFF_ANCHOR:" + id);
                    }
                }
                default -> {
                    String preserveId = action.startsWith("PRESERVE:") ? action.substring(9) : "";
                    EditorialChangeMapReconstructor.PreservedRow row = preservedById.get(preserveId);
                    if (row == null) issues.add("L3_PROBE_ACTION_PRESERVE_UNKNOWN:" + id);
                    else if (row.lineNumber() < p.viStart() || row.lineNumber() > p.viEnd()) issues.add("L3_PROBE_ACTION_OFF_ANCHOR:" + id);
                }
            }
        }
        if (coverage < MIN_PROBES_PER_KIND) issues.add("L3_PROBES_COVERAGE_TOO_FEW");
        if (regression < MIN_PROBES_PER_KIND) issues.add("L3_PROBES_REGRESSION_TOO_FEW");
        if (units.size() < Math.min(6, inventory.units().size())) issues.add("L3_PROBES_BREADTH_LOW");
        conflictsOut[0] = conflicts;
        return List.copyOf(issues);
    }

    private static boolean touches(EditorialChangeMapReconstructor.ChangeRow row, int start, int end) {
        int line = row.lineNumber();
        return switch (row.op()) {
            case INSERT_AFTER -> line >= start - 1 && line <= end;
            case MERGE_WITH_NEXT -> (line >= start - 1 && line <= end) || (line + 1 >= start && line + 1 <= end);
            default -> line >= start - 1 && line <= end + 1;
        };
    }

    record CarriedVerdict(List<String> issues, List<String> unresolved, Map<Integer, String> effective) { }

    static CarriedVerdict verifyCarried(List<Carried> carried, List<CarriedResolution> resolutions, List<String> viLines,
                                        List<EditorialChangeMapReconstructor.ChangeRow> changes,
                                        List<EditorialChangeMapReconstructor.PreservedRow> preserved,
                                        EditorialChangeMapReconstructor.Result reconstruction) {
        List<String> issues = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        Map<Integer, String> effective = new LinkedHashMap<>();
        Map<Integer, CarriedResolution> byIndex = new HashMap<>();
        for (CarriedResolution r : resolutions) byIndex.put(r.index(), r);
        Map<String, EditorialChangeMapReconstructor.ChangeRow> changeById = new HashMap<>();
        for (EditorialChangeMapReconstructor.ChangeRow row : changes) changeById.put(row.changeId(), row);
        Set<String> applied = new HashSet<>();
        for (EditorialChangeMapReconstructor.AppliedChange a : reconstruction.applied()) applied.add(a.row().changeId());
        Map<String, EditorialChangeMapReconstructor.PreservedRow> preservedById = new HashMap<>();
        for (EditorialChangeMapReconstructor.PreservedRow row : preserved) preservedById.put(row.preserveId(), row);

        for (Carried c : carried) {
            CarriedResolution r = byIndex.get(c.index());
            String key = Integer.toString(c.index());
            if (r == null) {
                issues.add("L3_CARRIED_NOT_RESOLVED:" + key);
                continue;
            }
            String status = r.status();
            switch (status) {
                case "FIXED" -> {
                    boolean any = false;
                    boolean open = false;
                    boolean anchored = false;
                    for (String cid : r.changeIds()) {
                        EditorialChangeMapReconstructor.ChangeRow row = changeById.get(cid);
                        if (row == null || row.status() != EditorialChangeMapReconstructor.DeclaredStatus.CLOSED) {
                            issues.add("L3_CARRIED_CHANGE_UNKNOWN:" + key + ":" + cid);
                        } else if (!applied.contains(cid)) {
                            open = true;
                        } else {
                            any = true;
                            anchored |= touches(row, c.line(), c.line());
                        }
                    }
                    if (r.changeIds().isEmpty()) issues.add("L3_CARRIED_FIXED_WITHOUT_CHANGE:" + key);
                    else if (any && !anchored) issues.add("L3_CARRIED_FIX_OFF_ANCHOR:" + key);
                    if (open) status = "UNRESOLVED";
                }
                case "REJECTED" -> {
                    boolean quoted = !r.evidenceQuote().isEmpty() && c.line() >= 1 && c.line() <= viLines.size()
                            && viLines.get(c.line() - 1).contains(r.evidenceQuote());
                    if (!quoted || r.reason().isBlank()) issues.add("L3_CARRIED_REJECTED_WITHOUT_EVIDENCE:" + key);
                    if (!r.changeIds().isEmpty()) issues.add("L3_CARRIED_REJECTED_BUT_CHANGED:" + key);
                }
                case "PRESERVED" -> {
                    if (r.preserveIds().isEmpty()) issues.add("L3_CARRIED_PRESERVED_WITHOUT_ROW:" + key);
                    for (String pid : r.preserveIds()) {
                        EditorialChangeMapReconstructor.PreservedRow row = preservedById.get(pid);
                        if (row == null) issues.add("L3_CARRIED_PRESERVE_UNKNOWN:" + key + ":" + pid);
                        else if (row.lineNumber() != c.line()) issues.add("L3_CARRIED_PRESERVE_OFF_ANCHOR:" + key + ":" + pid);
                    }
                    if (!r.changeIds().isEmpty()) issues.add("L3_CARRIED_PRESERVED_BUT_CHANGED:" + key);
                }
                default -> {
                    if (r.reason().isBlank()) issues.add("L3_CARRIED_UNRESOLVED_WITHOUT_REASON:" + key);
                }
            }
            effective.put(c.index(), status);
            if ("UNRESOLVED".equals(status)) unresolved.add(key);
        }
        return new CarriedVerdict(List.copyOf(issues), List.copyOf(unresolved), effective);
    }

    // ---- receipt blocks ----

    static List<Object> probeRows(List<Probe> probes) {
        List<Object> rows = new ArrayList<>();
        for (Probe p : probes) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("probeId", p.probeId());
            row.put("kind", p.kind());
            row.put("rawUnits", new ArrayList<Object>(p.rawUnits()));
            row.put("viStart", BigDecimal.valueOf(p.viStart()));
            row.put("viEnd", BigDecimal.valueOf(p.viEnd()));
            row.put("scope", p.scope());
            row.put("contrast", p.contrast());
            row.put("rawQuote", p.rawQuote());
            row.put("viQuote", p.viQuote());
            row.put("verdict", p.verdict());
            row.put("action", p.action());
            rows.add(row);
        }
        return rows;
    }

    static Map<String, Object> carriedEvidence(List<Carried> carried, CarriedVerdict verdict, List<CarriedResolution> resolutions) {
        Map<Integer, CarriedResolution> byIndex = new HashMap<>();
        for (CarriedResolution r : resolutions) byIndex.put(r.index(), r);
        List<Object> rows = new ArrayList<>();
        for (Carried c : carried) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("index", BigDecimal.valueOf(c.index()));
            row.put("line", BigDecimal.valueOf(c.line()));
            row.put("type", c.type());
            row.put("status", verdict.effective().getOrDefault(c.index(), "UNRESOLVED"));
            CarriedResolution r = byIndex.get(c.index());
            row.put("changeIds", r == null ? new ArrayList<Object>() : new ArrayList<Object>(r.changeIds()));
            row.put("preserveIds", r == null ? new ArrayList<Object>() : new ArrayList<Object>(r.preserveIds()));
            rows.add(row);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", BigDecimal.valueOf(carried.size()));
        m.put("defects", rows);
        return m;
    }

    static Map<String, Object> coverageEvidence(EditorialRawInventory.Inventory inventory, ReauditPass pass) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("inventorySha256", inventory.inventorySha256());
        m.put("unitCount", BigDecimal.valueOf(inventory.units().size()));
        List<Object> ranges = new ArrayList<>();
        for (EditorialRawInventory.Range range : pass.coverage()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("from", range.fromId());
            row.put("to", range.toId());
            row.put("status", range.status());
            ranges.add(row);
        }
        m.put("ranges", ranges);
        m.put("candidateCount", BigDecimal.valueOf(pass.candidates().size()));
        return m;
    }
}
