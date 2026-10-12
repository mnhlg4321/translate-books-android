package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the merged DRAFT-side text from accepted candidates. The separators come only from the {@link BoundaryPlan} that
 * the app holds, never from a response, and a candidate that still carries edge whitespace is refused instead of being
 * trimmed silently: {@code edgeLead + cand0 + boundary0 + cand1 + ... + edgeTrail}. Context is never part of a candidate.
 * Pure; the receipt lets the app check the output by hash after writing it.
 */
public final class ChunkMerge {
    public record Receipt(String contractRevision, String mapRevision, String mapHash, String rawCoverageHash, String draftCoverageHash,
                          String boundaryPlanHash, String edgePlanHash, List<String> orderedPairIds, List<String> fallbackPairIds,
                          String outputSha256) { }

    public record Error(String code, String detail) { }

    public record Result(boolean ok, String text, Receipt receipt, List<Error> errors) {
        public Result {
            errors = List.copyOf(errors);
        }
    }

    /** The separators of the DRAFT side: before the first pair, between pairs, after the last. */
    public record BoundaryPlan(String edgeLead, List<String> boundaries, String edgeTrail) {
        public BoundaryPlan {
            boundaries = List.copyOf(boundaries);
        }

        public String hash() { return PairText.sha256("B|" + String.join("\u0001", boundaries)); }

        public String edgeHash() { return PairText.sha256("E|" + edgeLead + "\u0001" + edgeTrail); }
    }

    private ChunkMerge() { }

    /** The plan of a map whose pairs all have a DRAFT range; {@code null} when a pair is missing or the draft ranges are unusable. */
    public static BoundaryPlan boundaryPlan(PairMap map) {
        List<int[]> ranges = new ArrayList<>();
        for (PairMap.Entry e : map.entries()) {
            int[] d = map.draftRange(e);
            if (d[0] < 0) return null;
            ranges.add(d);
        }
        if (ranges.isEmpty()) return null;
        String text = map.draft.text;
        List<String> boundaries = new ArrayList<>();
        for (int i = 0; i + 1 < ranges.size(); i++) {
            if (ranges.get(i)[1] > ranges.get(i + 1)[0]) return null;
            boundaries.add(text.substring(ranges.get(i)[1], ranges.get(i + 1)[0]));
        }
        return new BoundaryPlan(text.substring(0, ranges.get(0)[0]), boundaries, text.substring(ranges.get(ranges.size() - 1)[1]));
    }

    /**
     * @param accepted pairId to the candidate text of an accepted pair
     * @param fallbackToDraft a pair without an accepted candidate keeps its own DRAFT text and is listed in the receipt; the
     *                        merged text is then a provisional copy, never a result of the model
     */
    public static Result merge(PairMap map, Map<String, String> accepted, boolean fallbackToDraft) {
        List<Error> errors = new ArrayList<>();
        for (DocManifest.Issue i : map.verify()) errors.add(new Error("MERGE_MAP_INVALID", i.code() + " " + i.detail()));
        if (!errors.isEmpty()) return new Result(false, "", null, errors);
        BoundaryPlan plan = boundaryPlan(map);
        if (plan == null) return new Result(false, "", null, List.of(new Error("MERGE_PLAN_UNAVAILABLE", "a pair has no DRAFT range")));
        Set<String> known = new HashSet<>();
        for (PairMap.Entry e : map.entries()) known.add(e.pairId());
        for (String id : accepted.keySet()) if (!known.contains(id)) errors.add(new Error("MERGE_UNKNOWN_PAIR", id));
        StringBuilder out = new StringBuilder(plan.edgeLead());
        List<String> order = new ArrayList<>();
        List<String> fallback = new ArrayList<>();
        List<PairMap.Entry> entries = map.entries();
        for (int i = 0; i < entries.size(); i++) {
            PairMap.Entry e = entries.get(i);
            String text = accepted.get(e.pairId());
            if (text == null) {
                if (!fallbackToDraft) { errors.add(new Error("MERGE_PAIR_NOT_ACCEPTED", e.pairId())); continue; }
                text = map.draftText(e);
                fallback.add(e.pairId());
            } else {
                if (PairText.isBlank(text)) { errors.add(new Error("MERGE_CANDIDATE_EMPTY", e.pairId())); continue; }
                if (!PairText.trim(text).equals(text)) { errors.add(new Error("MERGE_CANDIDATE_NOT_TRIMMED", e.pairId())); continue; }
                if (text.contains(PairContract.LABEL_OPEN)) { errors.add(new Error("MERGE_LABEL_LEAK", e.pairId())); continue; }
            }
            order.add(e.pairId());
            out.append(text);
            if (i + 1 < entries.size()) out.append(plan.boundaries().get(i));
        }
        if (!errors.isEmpty()) return new Result(false, "", null, errors);
        out.append(plan.edgeTrail());
        String text = out.toString();
        Receipt receipt = new Receipt(PairContract.REVISION, map.mapRevision, map.mapHash(), map.raw.coverageHash(), map.draft.coverageHash(),
                plan.hash(), plan.edgeHash(), order, fallback, PairText.sha256(text));
        return new Result(true, text, receipt, List.of());
    }
}
