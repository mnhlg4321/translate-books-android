package com.ml.tblandroidtxt.editorial.api.chunk;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CS-1 step S8: the immutable result of planning one RAW/DRAFT pair, stored with the run. It holds the hashes of what was
 * read, the Performance limits it was cut with, the alignment in run-length form, the chunks as unit ranges (never text),
 * and the chapter verdict with its reasons. Reopening a run reads it back and does not plan again; if a source hash differs
 * the plan is stale and is recomputed. No book text is kept in it.
 */
public final class ChunkPlan {
    public static final String ALGORITHM = "CS-1";

    /** Units [rawFrom, rawTo) of RAW and [draftFrom, draftTo) of DRAFT form one chunk. */
    public record Chunk(int rawFrom, int rawTo, int draftFrom, int draftTo, List<String> flags, boolean uncertain) { }

    public record Reason(String level, String code, double value) { }

    public record Limits(String mode, int soft, int hard, int maxOutputTokens, int contextChars) { }

    public final String algorithm;
    public final String rawSha256;
    public final String draftSha256;
    public final String glossarySha256;
    public final String pronounSha256;
    public final Limits limits;
    public final double seriesRatio;
    public final int rawUnits;
    public final int draftUnits;
    /** Run-length alignment: {@code "11:57"} = 57 one-to-one beads; kinds are 11, 12, 21, 10 and 01. */
    public final List<String> beads;
    public final List<Chunk> chunks;
    public final String verdict;
    public final List<Reason> reasons;
    public final int splitGroups;
    public final int mergeGroups;
    public final int rawOnly;
    public final int draftOnly;
    public final int checkFailures;
    public final int uncertainChunks;

    public ChunkPlan(String rawSha256, String draftSha256, String glossarySha256, String pronounSha256, Limits limits, double seriesRatio,
                     int rawUnits, int draftUnits, List<String> beads, List<Chunk> chunks, String verdict, List<Reason> reasons,
                     int splitGroups, int mergeGroups, int rawOnly, int draftOnly, int checkFailures) {
        this.algorithm = ALGORITHM;
        this.rawSha256 = rawSha256;
        this.draftSha256 = draftSha256;
        this.glossarySha256 = glossarySha256;
        this.pronounSha256 = pronounSha256;
        this.limits = limits;
        this.seriesRatio = seriesRatio;
        this.rawUnits = rawUnits;
        this.draftUnits = draftUnits;
        this.beads = Collections.unmodifiableList(new ArrayList<>(beads));
        this.chunks = Collections.unmodifiableList(new ArrayList<>(chunks));
        this.verdict = verdict;
        this.reasons = Collections.unmodifiableList(new ArrayList<>(reasons));
        this.splitGroups = splitGroups;
        this.mergeGroups = mergeGroups;
        this.rawOnly = rawOnly;
        this.draftOnly = draftOnly;
        this.checkFailures = checkFailures;
        int u = 0;
        for (Chunk c : chunks) if (c.uncertain()) u++;
        this.uncertainChunks = u;
    }

    /** True when the plan was made from exactly these four inputs. */
    public boolean matches(String rawSha, String draftSha, String glossarySha, String pronounSha) {
        return rawSha256.equals(rawSha) && draftSha256.equals(draftSha) && glossarySha256.equals(glossarySha) && pronounSha256.equals(pronounSha);
    }

    public boolean blocked() { return "BLOCK".equals(verdict); }

    public boolean warned() { return "WARN".equals(verdict); }

    public Reason reason(String code) {
        for (Reason r : reasons) if (r.code().equals(code)) return r;
        return null;
    }

    // ---- run-length alignment

    public static List<String> encodeBeads(List<LineAligner.Bead> beads) {
        List<String> out = new ArrayList<>();
        String kind = null;
        int count = 0;
        for (LineAligner.Bead b : beads) {
            String k = "" + b.rawCount() + b.draftCount();
            if (k.equals(kind)) { count++; continue; }
            if (kind != null) out.add(kind + ":" + count);
            kind = k;
            count = 1;
        }
        if (kind != null) out.add(kind + ":" + count);
        return out;
    }

    public static List<LineAligner.Bead> decodeBeads(List<String> runs) {
        List<LineAligner.Bead> out = new ArrayList<>();
        int r = 0;
        int d = 0;
        for (String run : runs) {
            int colon = run.indexOf(':');
            if (colon != 2) throw new IllegalArgumentException("PLAN_BEADS_INVALID");
            int dr = run.charAt(0) - '0';
            int dd = run.charAt(1) - '0';
            int count = Integer.parseInt(run.substring(colon + 1));
            for (int i = 0; i < count; i++) {
                out.add(new LineAligner.Bead(r, r + dr, d, d + dd));
                r += dr;
                d += dd;
            }
        }
        return out;
    }

    // ---- JSON

    public String toJson() {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("algorithm", algorithm);
        o.put("rawSha256", rawSha256);
        o.put("draftSha256", draftSha256);
        o.put("glossarySha256", glossarySha256);
        o.put("pronounSha256", pronounSha256);
        Map<String, Object> l = new LinkedHashMap<>();
        l.put("mode", limits.mode());
        l.put("soft", BigDecimal.valueOf(limits.soft()));
        l.put("hard", BigDecimal.valueOf(limits.hard()));
        l.put("maxOutput", BigDecimal.valueOf(limits.maxOutputTokens()));
        l.put("contextChars", BigDecimal.valueOf(limits.contextChars()));
        o.put("limits", l);
        o.put("seriesRatio", Double.toString(seriesRatio));
        o.put("rawUnits", BigDecimal.valueOf(rawUnits));
        o.put("draftUnits", BigDecimal.valueOf(draftUnits));
        o.put("beads", new ArrayList<Object>(beads));
        List<Object> cs = new ArrayList<>();
        for (Chunk c : chunks) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("r", List.of(BigDecimal.valueOf(c.rawFrom()), BigDecimal.valueOf(c.rawTo())));
            m.put("d", List.of(BigDecimal.valueOf(c.draftFrom()), BigDecimal.valueOf(c.draftTo())));
            m.put("flags", new ArrayList<Object>(c.flags()));
            m.put("uncertain", c.uncertain());
            cs.add(m);
        }
        o.put("chunks", cs);
        o.put("verdict", verdict);
        List<Object> rs = new ArrayList<>();
        for (Reason r : reasons) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("level", r.level());
            m.put("code", r.code());
            m.put("value", Double.toString(r.value()));
            rs.add(m);
        }
        o.put("reasons", rs);
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("split", BigDecimal.valueOf(splitGroups));
        s.put("merge", BigDecimal.valueOf(mergeGroups));
        s.put("rawOnly", BigDecimal.valueOf(rawOnly));
        s.put("draftOnly", BigDecimal.valueOf(draftOnly));
        s.put("checkFailures", BigDecimal.valueOf(checkFailures));
        o.put("stats", s);
        return EditorialCanonicalJson.canonicalize(o);
    }

    @SuppressWarnings("unchecked")
    public static ChunkPlan fromJson(String json) {
        Map<String, Object> o = EditorialCanonicalJson.parseObject(json.getBytes(StandardCharsets.UTF_8));
        if (!ALGORITHM.equals(o.get("algorithm"))) throw new IllegalArgumentException("PLAN_ALGORITHM_UNKNOWN");
        Map<String, Object> l = (Map<String, Object>) o.get("limits");
        Limits limits = new Limits((String) l.get("mode"), num(l.get("soft")), num(l.get("hard")), num(l.get("maxOutput")),
                l.containsKey("contextChars") ? num(l.get("contextChars")) : 400);
        List<String> beads = new ArrayList<>();
        for (Object b : (List<Object>) o.get("beads")) beads.add((String) b);
        List<Chunk> chunks = new ArrayList<>();
        for (Object c : (List<Object>) o.get("chunks")) {
            Map<String, Object> m = (Map<String, Object>) c;
            List<Object> r = (List<Object>) m.get("r");
            List<Object> d = (List<Object>) m.get("d");
            List<String> flags = new ArrayList<>();
            for (Object f : (List<Object>) m.get("flags")) flags.add((String) f);
            chunks.add(new Chunk(num(r.get(0)), num(r.get(1)), num(d.get(0)), num(d.get(1)), Collections.unmodifiableList(flags), (Boolean) m.get("uncertain")));
        }
        List<Reason> reasons = new ArrayList<>();
        for (Object x : (List<Object>) o.get("reasons")) {
            Map<String, Object> m = (Map<String, Object>) x;
            reasons.add(new Reason((String) m.get("level"), (String) m.get("code"), Double.parseDouble((String) m.get("value"))));
        }
        Map<String, Object> s = (Map<String, Object>) o.get("stats");
        return new ChunkPlan((String) o.get("rawSha256"), (String) o.get("draftSha256"), (String) o.get("glossarySha256"), (String) o.get("pronounSha256"),
                limits, Double.parseDouble((String) o.get("seriesRatio")), num(o.get("rawUnits")), num(o.get("draftUnits")), beads, chunks,
                (String) o.get("verdict"), reasons, num(s.get("split")), num(s.get("merge")), num(s.get("rawOnly")), num(s.get("draftOnly")),
                num(s.get("checkFailures")));
    }

    private static int num(Object value) { return ((BigDecimal) value).intValueExact(); }
}
