package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.PairContract;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Guards for the single-chunk experiment (docs/EDITORIAL_CHUNK_PLAN.md section 6.2): one chunk of a frozen chapter is sent, nothing
 * else. The run keeps the whole-chapter snapshot, so that chunk's RAW/DRAFT range, context, glossary and pronoun rows are exactly
 * what the full run would send. A selector names the chunk by ordinal and carries the hashes it expects; any mismatch is refused
 * before a reservation or a call exists. {@link OneCallProvider} then caps the dispatch count at one, whatever the caller does.
 */
public final class EditorialSingleChunkProbe {
    private EditorialSingleChunkProbe() { }

    /** Every field is required: a selector without a guard is refused, never defaulted. */
    public record Selector(int ordinal, String pairId, String mapHash, int firstParagraph, int lastParagraph,
                           String rawRangeSha256, String draftRangeSha256) { }

    public static final class SelectionException extends Exception {
        public final String code;

        SelectionException(String code, String detail) {
            super(code + ": " + detail);
            this.code = code;
        }
    }

    /** @return the pair id of the selected chunk, after every guard passed */
    public static String resolve(PairRun run, Selector s) throws SelectionException {
        if (run == null || s == null) throw new SelectionException("SELECTOR_MISSING", "run or selector");
        if (s.ordinal() < 1 || blank(s.pairId()) || blank(s.mapHash()) || s.firstParagraph() < 1 || s.lastParagraph() < s.firstParagraph()
                || blank(s.rawRangeSha256()) || blank(s.draftRangeSha256())) {
            throw new SelectionException("SELECTOR_INCOMPLETE", "ordinal, pair id, map hash, paragraph range and both range hashes are required");
        }
        if (!EditorialPairModels.ARM_CHUNK.equals(run.arm)) throw new SelectionException("SELECTOR_NEEDS_CHUNK_RUN", String.valueOf(run.arm));
        if (!PairContract.REVISION.equals(run.contractRevision)) throw new SelectionException("SELECTOR_CONTRACT_CHANGED", String.valueOf(run.contractRevision));
        PairMap map = EditorialPairSnapshot.of(run).map;
        if (!s.mapHash().equals(run.mapHash) || !s.mapHash().equals(map.mapHash())) throw new SelectionException("SELECTOR_MAP_HASH_MISMATCH", "stored/expected/recomputed");
        List<PairMap.Entry> hits = new ArrayList<>();
        for (PairMap.Entry e : map.entries()) if (e.displayOrdinal() == s.ordinal()) hits.add(e);
        if (hits.size() != 1) throw new SelectionException("SELECTOR_ORDINAL_NOT_UNIQUE", s.ordinal() + " matches " + hits.size());
        PairMap.Entry entry = hits.get(0);
        if (!entry.pairId().equals(s.pairId())) throw new SelectionException("SELECTOR_PAIR_ID_MISMATCH", "ordinal " + s.ordinal());
        if (entry.missingDraft()) throw new SelectionException("SELECTOR_PAIR_MISSING_DRAFT", entry.pairId());
        if (map.rawParagraphStart(entry) != s.firstParagraph() || map.rawParagraphEnd(entry) != s.lastParagraph()) {
            throw new SelectionException("SELECTOR_PARAGRAPH_RANGE_MISMATCH", map.rawParagraphStart(entry) + "-" + map.rawParagraphEnd(entry));
        }
        if (!PairText.sha256(map.rawText(entry)).equalsIgnoreCase(s.rawRangeSha256())) throw new SelectionException("SELECTOR_RAW_RANGE_HASH_MISMATCH", entry.pairId());
        if (!PairText.sha256(map.draftText(entry)).equalsIgnoreCase(s.draftRangeSha256())) throw new SelectionException("SELECTOR_DRAFT_RANGE_HASH_MISMATCH", entry.pairId());
        return entry.pairId();
    }

    /** Lets exactly one request through to the delegate; every later request is refused without touching it. */
    public static final class OneCallProvider implements EditorialApiProvider {
        public static final String LIMIT_ERROR = "SINGLE_CALL_LIMIT";
        private final EditorialApiProvider delegate;
        private final AtomicInteger attempts = new AtomicInteger();
        private final AtomicInteger refused = new AtomicInteger();

        public OneCallProvider(EditorialApiProvider delegate) {
            this.delegate = delegate;
        }

        @Override public EditorialApiFlow.StepResponse call(EditorialApiFlow.Request request, String model, int maxOutputTokens, long timeoutMillis) {
            if (attempts.incrementAndGet() > 1) {
                refused.incrementAndGet();
                return EditorialApiFlow.StepResponse.failure(LIMIT_ERROR);
            }
            return delegate.call(request, model, maxOutputTokens, timeoutMillis);
        }

        @Override public void cancel() { delegate.cancel(); }

        /** Requests that reached the delegate (0 or 1). */
        public int dispatched() { return Math.min(1, attempts.get()); }

        /** Requests that were refused because the single call was already used. */
        public int refused() { return refused.get(); }
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
}
