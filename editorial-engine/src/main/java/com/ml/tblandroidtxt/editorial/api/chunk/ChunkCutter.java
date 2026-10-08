package com.ml.tblandroidtxt.editorial.api.chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * CS-1 step S5: cuts the aligned chapter into chunks that RAW and DRAFT share. A cut falls only between two beads that each
 * have {@value #GUARD} consecutive 1-1 beads on both sides of it, so a split or merged group, an unpaired line or the stretch
 * around one never straddles a cut (measured: 133 of 133 cuts correct on 18 chapters with an answer derived from FINAL; with
 * a guard of one, 1 of 136 was wrong). Sizes come from the caller: the app passes the translation flow's own measure
 * ({@code Chunker.measure} in the Performance chunk mode) and the Performance limits, so Edit and Translate cut alike.
 * <ul>
 * <li>Cut at the first safe point once the RAW chunk reaches {@code soft}; never past {@code hard} when a safe point exists
 *     inside it (then the chunk is shorter than {@code soft}).</li>
 * <li>The estimated output (DRAFT chunk measure times {@code outputFactor}) must stay within {@code maxOutputTokens}.</li>
 * <li>A line longer than {@code hard} is a chunk of its own.</li>
 * <li>With no safe point inside {@code hard}, look up to {@value #OVERSIZE_FACTOR} times {@code hard} for a safe point
 *     (flag {@code OVERSIZE}); failing that cut at the 1-1 pair nearest to {@code hard} with a guard of one (flag
 *     {@code WEAK_CUT}); failing that keep the rest as one chunk (flag {@code NO_SAFE_CUT}).</li>
 * </ul>
 */
public final class ChunkCutter {
    public static final int GUARD = 2;
    public static final int WEAK_GUARD = 1;
    public static final int OVERSIZE_FACTOR = 3;

    /** @param soft where a cut becomes allowed; @param hard the ceiling; @param maxOutputTokens 0 = no output check */
    public record Limits(int soft, int hard, int maxOutputTokens, double outputFactor,
                         ToIntFunction<String> measureRaw, ToIntFunction<String> measureDraft) { }

    /** The chunk ends after bead {@code endBead} (inclusive). */
    public record Cut(int endBead, Set<String> flags) { }

    private final List<LineAligner.Bead> beads;
    private final List<String> raw;
    private final List<String> draft;
    private final Limits limits;

    private ChunkCutter(LineAligner.Alignment alignment, List<String> raw, List<String> draft, Limits limits) {
        this.beads = alignment.beads();
        this.raw = raw;
        this.draft = draft;
        this.limits = limits;
    }

    public static List<Cut> cut(LineAligner.Alignment alignment, List<String> raw, List<String> draft, Limits limits) {
        ChunkCutter cutter = new ChunkCutter(alignment, raw, draft, limits);
        List<Cut> out = new ArrayList<>();
        int start = 0;
        while (start < cutter.beads.size()) {
            Cut c = cutter.next(start);
            out.add(c);
            start = c.endBead() + 1;
        }
        return Collections.unmodifiableList(out);
    }

    /** True when beads [from, to] are the stretch the chunk-size checks look at: RAW text and DRAFT text joined by LF. */
    String rawText(int from, int to) { return join(raw, beads.get(from).rawStart(), beads.get(to).rawEnd()); }

    String draftText(int from, int to) { return join(draft, beads.get(from).draftStart(), beads.get(to).draftEnd()); }

    private static String join(List<String> lines, int a, int b) {
        StringBuilder sb = new StringBuilder();
        for (int i = a; i < b; i++) {
            if (i > a) sb.append('\n');
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    private boolean safe(int k, int guard) {
        int n = beads.size();
        if (k + guard >= n) return false;
        for (int j = Math.max(0, k - guard + 1); j <= k; j++) if (!beads.get(j).oneToOne()) return false;
        for (int j = k + 1; j <= k + guard; j++) if (!beads.get(j).oneToOne()) return false;
        return true;
    }

    private boolean outputFits(int s, int k) {
        if (limits.maxOutputTokens() <= 0) return true;
        double estimate = limits.measureDraft().applyAsInt(draftText(s, k)) * limits.outputFactor();
        return Math.ceil(estimate) <= limits.maxOutputTokens();
    }

    private static Cut cutAt(int k, String... flags) {
        Set<String> f = new LinkedHashSet<>();
        for (String s : flags) f.add(s);
        return new Cut(k, Collections.unmodifiableSet(f));
    }

    private Cut next(int s) {
        final int n = beads.size();
        int lastSafe = -1;
        int firstExceed = -1;
        for (int k = s; k < n; k++) {
            int size = limits.measureRaw().applyAsInt(rawText(s, k));
            boolean fits = size <= limits.hard() && outputFits(s, k);
            if (!fits) {
                if (k == s && safe(k, GUARD)) return cutAt(k, size > limits.hard() ? "LONG_LINE" : "OUTPUT_LIMIT");
                firstExceed = k;
                break;
            }
            if (safe(k, GUARD)) {
                if (size >= limits.soft()) return cutAt(k);
                lastSafe = k;
            }
        }
        if (firstExceed < 0) return cutAt(n - 1);
        if (lastSafe >= 0) return cutAt(lastSafe);
        long ceiling = (long) limits.hard() * OVERSIZE_FACTOR;
        for (int k = firstExceed; k < n; k++) {
            if (limits.measureRaw().applyAsInt(rawText(s, k)) > ceiling) break;
            if (safe(k, GUARD)) return cutAt(k, "OVERSIZE");
        }
        int best = -1;
        long bestDistance = Long.MAX_VALUE;
        for (int k = s; k < n; k++) {
            int size = limits.measureRaw().applyAsInt(rawText(s, k));
            if (safe(k, WEAK_GUARD)) {
                long distance = Math.abs((long) size - limits.hard());
                if (distance < bestDistance) { best = k; bestDistance = distance; }
            }
            if (size > ceiling && best >= 0) break;
        }
        if (best >= 0) return cutAt(best, "WEAK_CUT");
        return cutAt(n - 1, "NO_SAFE_CUT");
    }
}
