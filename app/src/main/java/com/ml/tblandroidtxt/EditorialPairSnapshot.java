package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairMaps;
import com.ml.tblandroidtxt.editorial.api.PairPromptBuilder;
import com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlan;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The frozen inputs of a run rebuilt from its row: rows, references and the pair map, checked against the stored map hash. */
public final class EditorialPairSnapshot {
    public static final class Mismatch extends RuntimeException {
        public Mismatch(String detail) { super("SNAPSHOT_MISMATCH: " + detail); }
    }

    public final List<String> rawRows;
    public final List<String> draftRows;
    public final List<EditInputs.GlossaryEntry> glossary;
    public final String pronounText;
    public final Set<String> cueFields;
    public final PairMap map;
    /** Reference-only context around each pair: from the chunk plan (Performance setting) or the default. */
    public final PairPromptBuilder.ContextPolicy context;
    /** Persisted CS-1 alignment when the source was planned from paired files. */
    public final ChunkPlan chunkPlan;

    private EditorialPairSnapshot(List<String> rawRows, List<String> draftRows, List<EditInputs.GlossaryEntry> glossary, String pronounText,
                                  Set<String> cueFields, PairMap map, PairPromptBuilder.ContextPolicy context, ChunkPlan chunkPlan) {
        this.rawRows = rawRows;
        this.draftRows = draftRows;
        this.glossary = glossary;
        this.pronounText = pronounText;
        this.cueFields = cueFields;
        this.map = map;
        this.context = context;
        this.chunkPlan = chunkPlan;
    }

    public static EditorialPairSnapshot of(PairRun run) {
        List<String> raw = rows(run.rawRowsJson);
        List<String> draft = rows(run.draftRowsJson);
        PairMap map = PairMaps.fromJobRows(run.chapterId, "raw:" + run.sourceRef, "draft:" + run.sourceRef, raw, draft);
        if (!run.mapHash.isEmpty() && !run.mapHash.equals(map.mapHash())) throw new Mismatch("map hash differs from the stored run");
        PairPromptBuilder.ContextPolicy context = PairPromptBuilder.ContextPolicy.DEFAULT;
        ChunkPlan chunkPlan = null;
        if (run.chunkPlanJson != null && !run.chunkPlanJson.isEmpty()) {
            chunkPlan = ChunkPlan.fromJson(run.chunkPlanJson);
            context = new PairPromptBuilder.ContextPolicy(chunkPlan.limits.contextChars(), context.minLines());
        }
        return new EditorialPairSnapshot(raw, draft, glossaryFrom(run.glossaryText), run.pronounText, cues(run.cueFields), map, context, chunkPlan);
    }

    public static String rowsJson(List<String> rows) {
        JSONArray array = new JSONArray();
        for (String r : rows) array.put(r == null ? org.json.JSONObject.NULL : r);
        return array.toString();
    }

    static List<String> rows(String json) {
        try {
            JSONArray array = new JSONArray(json == null || json.isEmpty() ? "[]" : json);
            List<String> out = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) out.add(array.isNull(i) ? null : array.getString(i));
            return out;
        } catch (JSONException broken) {
            throw new Mismatch("rows are not readable");
        }
    }

    static List<EditInputs.GlossaryEntry> glossaryFrom(String text) {
        List<EditInputs.GlossaryEntry> entries = new ArrayList<>();
        for (String line : (text == null ? "" : text).split("\n", -1)) {
            if (line.isEmpty()) continue;
            String[] parts = line.split("\t", -1);
            entries.add(new EditInputs.GlossaryEntry(parts.length > 0 ? parts[0] : "", parts.length > 1 ? parts[1] : "",
                    parts.length > 2 ? parts[2] : "", parts.length > 3 ? parts[3] : ""));
        }
        return entries;
    }

    static Set<String> cues(String csv) {
        Set<String> out = new LinkedHashSet<>();
        for (String c : (csv == null ? "" : csv).split(",")) if (!c.trim().isEmpty()) out.add(c.trim());
        return out.isEmpty() ? null : out;
    }
}
