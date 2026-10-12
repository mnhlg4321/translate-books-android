package com.ml.tblandroidtxt.editorial.api.chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * CS-1 step S6: checks every chunk offline before anything is sent. A chunk fails when
 * <ul>
 * <li>LENGTH - its DRAFT/RAW character ratio is more than 35 % off the chapter's own ratio;</li>
 * <li>DIALOGUE - the counts of lines opening with 「 or 『 differ by more than one;</li>
 * <li>NAME - at least 5 of its 1-1 pairs carry glossary terms and fewer than 35 % of them agree.</li>
 * </ul>
 * A failing chunk is merged with its neighbour and checked again; if it still fails it stays and is marked uncertain. If more
 * than 20 % of the chunks fail at first, the chapter verdict is raised to WARN by the caller.
 */
public final class ChunkChecks {
    public static final double LENGTH_TOLERANCE = 0.35;
    public static final int DIALOGUE_TOLERANCE = 1;
    public static final int MIN_NAME_PAIRS = 5;
    public static final double NAME_MIN = 0.35;
    public static final double FAILURE_SHARE_FOR_WARN = 0.20;

    /** Beads [beadFrom, beadTo] inclusive, the flags that came with the cut, and the outcome of the checks. */
    public record Checked(int beadFrom, int beadTo, Set<String> flags, boolean uncertain, List<String> failures) { }

    public record Result(List<Checked> chunks, int initialChunks, int initialFailures) {
        public double failureShare() { return initialChunks == 0 ? 0 : (double) initialFailures / initialChunks; }

        public boolean raisesWarn() { return failureShare() > FAILURE_SHARE_FOR_WARN; }
    }

    private ChunkChecks() { }

    public static Result apply(LineAligner.Alignment alignment, List<String> raw, List<String> draft, List<Anchors.Term> terms,
                               List<ChunkCutter.Cut> cuts) {
        Evaluator ev = new Evaluator(alignment, raw, draft, terms);
        List<Checked> ranges = new ArrayList<>();
        int from = 0;
        for (ChunkCutter.Cut c : cuts) {
            ranges.add(new Checked(from, c.endBead(), c.flags(), false, List.of()));
            from = c.endBead() + 1;
        }
        int initialFailures = 0;
        for (Checked c : ranges) if (!ev.failures(c.beadFrom(), c.beadTo()).isEmpty()) initialFailures++;
        List<Checked> out = new ArrayList<>();
        int i = 0;
        while (i < ranges.size()) {
            Checked cur = ranges.get(i);
            List<String> fails = ev.failures(cur.beadFrom(), cur.beadTo());
            if (fails.isEmpty()) { out.add(cur); i++; continue; }
            if (i + 1 < ranges.size()) {
                Checked next = ranges.get(i + 1);
                if (ev.failures(cur.beadFrom(), next.beadTo()).isEmpty()) {
                    out.add(new Checked(cur.beadFrom(), next.beadTo(), union(cur.flags(), next.flags(), "MERGED"), false, List.of()));
                    i += 2;
                    continue;
                }
            } else if (!out.isEmpty()) {
                Checked prev = out.get(out.size() - 1);
                if (ev.failures(prev.beadFrom(), cur.beadTo()).isEmpty()) {
                    out.set(out.size() - 1, new Checked(prev.beadFrom(), cur.beadTo(), union(prev.flags(), cur.flags(), "MERGED"), false, List.of()));
                    i++;
                    continue;
                }
            }
            out.add(new Checked(cur.beadFrom(), cur.beadTo(), cur.flags(), true, Collections.unmodifiableList(fails)));
            i++;
        }
        return new Result(Collections.unmodifiableList(out), ranges.size(), initialFailures);
    }

    private static Set<String> union(Set<String> a, Set<String> b, String extra) {
        Set<String> out = new LinkedHashSet<>(a);
        out.addAll(b);
        out.add(extra);
        return Collections.unmodifiableSet(out);
    }

    private static final class Evaluator {
        private final LineAligner.Alignment alignment;
        private final List<String> raw;
        private final List<String> draft;
        private final Set<String> targets;

        Evaluator(LineAligner.Alignment alignment, List<String> raw, List<String> draft, List<Anchors.Term> terms) {
            this.alignment = alignment;
            this.raw = raw;
            this.draft = draft;
            this.targets = Anchors.targets(terms);
        }

        List<String> failures(int from, int to) {
            List<LineAligner.Bead> beads = alignment.beads();
            int rawFrom = beads.get(from).rawStart();
            int rawTo = beads.get(to).rawEnd();
            int draftFrom = beads.get(from).draftStart();
            int draftTo = beads.get(to).draftEnd();
            long rawChars = 0;
            long draftChars = 0;
            int rawDialogue = 0;
            int draftDialogue = 0;
            for (int i = rawFrom; i < rawTo; i++) {
                rawChars += raw.get(i).codePointCount(0, raw.get(i).length());
                if (opensDialogue(raw.get(i))) rawDialogue++;
            }
            for (int j = draftFrom; j < draftTo; j++) {
                draftChars += draft.get(j).codePointCount(0, draft.get(j).length());
                if (opensDialogue(draft.get(j))) draftDialogue++;
            }
            List<String> out = new ArrayList<>();
            if (rawChars == 0 || alignment.ratio() <= 0
                    || Math.abs((double) draftChars / rawChars / alignment.ratio() - 1) > LENGTH_TOLERANCE) out.add("LENGTH");
            if (Math.abs(rawDialogue - draftDialogue) > DIALOGUE_TOLERANCE) out.add("DIALOGUE");
            int withTerms = 0;
            int agree = 0;
            for (int k = from; k <= to; k++) {
                LineAligner.Bead b = beads.get(k);
                if (!b.oneToOne()) continue;
                Set<String> a = alignment.rawAnchors().get(b.rawStart());
                Set<String> d = alignment.draftAnchors().get(b.draftStart());
                boolean any = false;
                boolean shared = false;
                for (String s : a) if (targets.contains(s)) { any = true; if (d.contains(s)) shared = true; }
                if (!any) for (String s : d) if (targets.contains(s)) { any = true; break; }
                if (any) { withTerms++; if (shared) agree++; }
            }
            if (withTerms >= MIN_NAME_PAIRS && (double) agree / withTerms < NAME_MIN) out.add("NAME");
            return out;
        }

        private static boolean opensDialogue(String line) {
            char c = line.charAt(0);
            return c == '「' || c == '『';
        }
    }
}
