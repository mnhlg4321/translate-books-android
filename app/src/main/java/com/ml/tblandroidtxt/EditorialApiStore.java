package com.ml.tblandroidtxt;

import java.util.List;

/** Persistence of combos and runs; the SQLite implementation is used by the app, an in-memory one by JVM tests. */
public interface EditorialApiStore {
    long insertCombo(EditorialApiCombo combo);

    void updateCombo(EditorialApiCombo combo);

    /** Removes the combo and, with it, its runs. */
    void deleteCombo(long id);

    List<EditorialApiCombo> listCombos();

    EditorialApiCombo getCombo(long id);

    long insertRun(EditorialApiRun run);

    void updateRun(EditorialApiRun run);

    EditorialApiRun getRun(long id);

    /** The most recent run of the combo, or {@code null}. */
    EditorialApiRun latestRun(long comboId);

    List<EditorialApiRun> runsOf(long comboId);
}
