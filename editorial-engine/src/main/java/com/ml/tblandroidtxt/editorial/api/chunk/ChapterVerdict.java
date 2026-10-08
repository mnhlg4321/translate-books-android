package com.ml.tblandroidtxt.editorial.api.chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * CS-1 step S4: decides, before any API call, whether RAW and DRAFT look like the same chapter of the same edition.
 * Four signals, each with a WARN and a BLOCK threshold (measured on 113 correct pairs, 55 wrong-chapter pairs and 11
 * other-edition pairs; a wrong chapter was blocked 55/55 and a correct pair never):
 * <ul>
 * <li>NAME_MATCH - share of 1-1 pairs carrying glossary terms in which a term agrees; only with a glossary and at least 15
 *     such pairs. OK from 0.6, WARN from 0.35, BLOCK below.</li>
 * <li>EDGE_SYMBOLS - share of 1-1 pairs with the same opening and closing mark. OK from 0.97, WARN from 0.95, BLOCK below.</li>
 * <li>UNPAIRED_LINES - share of beads that are 1-0 or 0-1. OK up to 1.5 %, WARN up to 5 %, BLOCK above.</li>
 * <li>LENGTH_RATIO - deviation of DRAFT/RAW characters from the series ratio (default 2.54 for Japanese to Vietnamese). OK up
 *     to 20 %, WARN up to 35 %, BLOCK above.</li>
 * </ul>
 */
public final class ChapterVerdict {
    public static final double DEFAULT_SERIES_RATIO = 2.54;
    public static final int MIN_NAME_PAIRS = 15;

    public enum Level { OK, WARN, BLOCK }

    /** One reason: its own level, a code for the message and the measured value (a share, or a share of deviation). */
    public record Reason(Level level, String code, double value) { }

    public record Result(Level level, List<Reason> reasons, int oneToOne, int splitGroups, int mergeGroups, int rawOnly, int draftOnly) {
        public boolean has(String code) {
            for (Reason r : reasons) if (r.code().equals(code)) return true;
            return false;
        }
    }

    private ChapterVerdict() { }

    public static Result evaluate(List<String> raw, List<String> draft, LineAligner.Alignment alignment, List<Anchors.Term> terms,
                                  double seriesRatio) {
        List<LineAligner.Bead> beads = alignment.beads();
        int one = 0;
        int split = 0;
        int merge = 0;
        int rawOnly = 0;
        int draftOnly = 0;
        for (LineAligner.Bead b : beads) {
            if (b.oneToOne()) one++;
            else if (b.rawCount() == 1 && b.draftCount() == 2) split++;
            else if (b.rawCount() == 2 && b.draftCount() == 1) merge++;
            else if (b.draftCount() == 0) rawOnly++;
            else if (b.rawCount() == 0) draftOnly++;
        }
        List<Reason> reasons = new ArrayList<>();
        if (raw.isEmpty() || draft.isEmpty()) {
            reasons.add(new Reason(Level.BLOCK, "EMPTY_SOURCE", 0));
            return new Result(Level.BLOCK, Collections.unmodifiableList(reasons), one, split, merge, rawOnly, draftOnly);
        }
        Set<String> targets = Anchors.targets(terms);
        int withTerms = 0;
        int agree = 0;
        int sameEdges = 0;
        for (LineAligner.Bead b : beads) {
            if (!b.oneToOne()) continue;
            Set<String> a = alignment.rawAnchors().get(b.rawStart());
            Set<String> d = alignment.draftAnchors().get(b.draftStart());
            if (hasTerm(a, d, targets)) {
                withTerms++;
                if (sharesTerm(a, d, targets)) agree++;
            }
            if (LineAligner.openMark(raw.get(b.rawStart())) == LineAligner.openMark(draft.get(b.draftStart()))
                    && LineAligner.closeMark(raw.get(b.rawStart())) == LineAligner.closeMark(draft.get(b.draftStart()))) {
                sameEdges++;
            }
        }
        if (withTerms >= MIN_NAME_PAIRS) {
            double name = (double) agree / withTerms;
            if (name < 0.35) reasons.add(new Reason(Level.BLOCK, "NAME_MATCH", name));
            else if (name < 0.6) reasons.add(new Reason(Level.WARN, "NAME_MATCH", name));
        }
        double edge = (double) sameEdges / Math.max(1, one);
        if (edge < 0.95) reasons.add(new Reason(Level.BLOCK, "EDGE_SYMBOLS", edge));
        else if (edge < 0.97) reasons.add(new Reason(Level.WARN, "EDGE_SYMBOLS", edge));
        double skips = (double) (rawOnly + draftOnly) / Math.max(1, beads.size());
        if (skips > 0.05) reasons.add(new Reason(Level.BLOCK, "UNPAIRED_LINES", skips));
        else if (skips > 0.015) reasons.add(new Reason(Level.WARN, "UNPAIRED_LINES", skips));
        long rawChars = 0;
        long draftChars = 0;
        for (String s : raw) rawChars += s.codePointCount(0, s.length());
        for (String s : draft) draftChars += s.codePointCount(0, s.length());
        double deviation = Math.abs((double) draftChars / Math.max(1L, rawChars) / seriesRatio - 1);
        if (deviation > 0.35) reasons.add(new Reason(Level.BLOCK, "LENGTH_RATIO", deviation));
        else if (deviation > 0.2) reasons.add(new Reason(Level.WARN, "LENGTH_RATIO", deviation));
        Level level = Level.OK;
        for (Reason r : reasons) {
            if (r.level() == Level.BLOCK) level = Level.BLOCK;
            else if (level == Level.OK) level = Level.WARN;
        }
        return new Result(level, Collections.unmodifiableList(reasons), one, split, merge, rawOnly, draftOnly);
    }

    private static boolean hasTerm(Set<String> a, Set<String> d, Set<String> targets) {
        for (String s : a) if (targets.contains(s)) return true;
        for (String s : d) if (targets.contains(s)) return true;
        return false;
    }

    private static boolean sharesTerm(Set<String> a, Set<String> d, Set<String> targets) {
        for (String s : a) if (targets.contains(s) && d.contains(s)) return true;
        return false;
    }
}
