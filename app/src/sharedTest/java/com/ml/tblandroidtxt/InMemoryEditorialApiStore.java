package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Store double for JVM tests; mirrors the SQLite store, including the cascade of a combo's runs. */
public final class InMemoryEditorialApiStore implements EditorialApiStore {
    private final Map<Long, EditorialApiCombo> combos = new LinkedHashMap<>();
    private final Map<Long, EditorialApiRun> runs = new LinkedHashMap<>();
    private long nextCombo = 1;
    private long nextRun = 1;
    private long clock = 1_000;
    public int runWrites;

    @Override public long insertCombo(EditorialApiCombo combo) {
        combo.id = nextCombo++;
        combo.createdAt = ++clock;
        combo.updatedAt = clock;
        combos.put(combo.id, combo.copy());
        return combo.id;
    }

    @Override public void updateCombo(EditorialApiCombo combo) {
        combo.updatedAt = ++clock;
        combos.put(combo.id, combo.copy());
    }

    @Override public void deleteCombo(long id) {
        combos.remove(id);
        runs.values().removeIf(run -> run.comboId == id);
    }

    @Override public List<EditorialApiCombo> listCombos() {
        List<EditorialApiCombo> out = new ArrayList<>();
        for (EditorialApiCombo combo : combos.values()) out.add(combo.copy());
        out.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return out;
    }

    @Override public EditorialApiCombo getCombo(long id) {
        EditorialApiCombo combo = combos.get(id);
        return combo == null ? null : combo.copy();
    }

    @Override public long insertRun(EditorialApiRun run) {
        if (!combos.containsKey(run.comboId)) throw new IllegalStateException("FOREIGN KEY constraint failed");
        run.id = nextRun++;
        run.createdAt = ++clock;
        run.updatedAt = clock;
        runs.put(run.id, run.copy());
        runWrites++;
        return run.id;
    }

    @Override public void updateRun(EditorialApiRun run) {
        run.updatedAt = ++clock;
        runs.put(run.id, run.copy());
        runWrites++;
    }

    @Override public EditorialApiRun getRun(long id) {
        EditorialApiRun run = runs.get(id);
        return run == null ? null : run.copy();
    }

    @Override public EditorialApiRun latestRun(long comboId) {
        EditorialApiRun best = null;
        for (EditorialApiRun run : runs.values()) {
            if (run.comboId == comboId && (best == null || run.id > best.id)) best = run;
        }
        return best == null ? null : best.copy();
    }

    @Override public List<EditorialApiRun> runsOf(long comboId) {
        List<EditorialApiRun> out = new ArrayList<>();
        for (EditorialApiRun run : runs.values()) if (run.comboId == comboId) out.add(run.copy());
        out.sort((a, b) -> Long.compare(b.id, a.id));
        return out;
    }
}
