package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Pair maps whose link is a fact of the source: one row of a translation job is one RAW unit and the DRAFT unit of the same
 * row. Nothing is matched by position of two independent texts; a row without a translation stays a missing pair. Pure.
 */
public final class PairMaps {
    public static final String JOB_ROW_REVISION = "job-row-1";

    private PairMaps() { }

    /**
     * @param rawRows   the RAW text of every row, in order (a blank row has no unit and no pair)
     * @param draftRows the DRAFT text of the same rows; {@code null} or blank where the row has no usable translation
     */
    public static PairMap fromJobRows(String chapterId, String rawDocId, String draftDocId, List<String> rawRows, List<String> draftRows) {
        if (rawRows.size() != draftRows.size()) throw new IllegalArgumentException("rows differ: " + rawRows.size() + " vs " + draftRows.size());
        DocManifest raw = DocManifest.fromRows(DocManifest.Kind.RAW, rawDocId, chapterId, rawRows, false);
        DocManifest draft = DocManifest.fromRows(DocManifest.Kind.DRAFT, draftDocId, chapterId, draftRows, true);
        List<PairMap.Spec> specs = new ArrayList<>();
        int rawIndex = 0;
        int draftIndex = 0;
        for (int i = 0; i < rawRows.size(); i++) {
            boolean hasRaw = !PairText.isBlank(PairText.normalize(rawRows.get(i)));
            boolean hasDraft = !PairText.isBlank(PairText.normalize(draftRows.get(i)));
            if (hasRaw) {
                specs.add(PairMap.Spec.one(raw.units().get(rawIndex).id(), hasDraft ? draft.units().get(draftIndex).id() : null, JOB_ROW_REVISION));
                rawIndex++;
            }
            // a translation without a source row cannot be linked to anything: it stays unmapped and the map reports it
            if (hasDraft) draftIndex++;
        }
        return PairMap.build(JOB_ROW_REVISION, raw, draft, specs);
    }
}
