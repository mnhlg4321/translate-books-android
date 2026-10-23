package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;

/** Read-only data loaded off the UI thread for the Jobs Biên tập card list. */
final class JobsEditorialCardData {
    final EditorialApiCombo combo;
    final EditorialPairModels.PairRun run;
    final List<EditorialPairModels.PairItem> items;
    final boolean active;

    JobsEditorialCardData(EditorialApiCombo combo, EditorialPairModels.PairRun run,
                          List<EditorialPairModels.PairItem> items, boolean active) {
        this.combo = combo;
        this.run = run;
        this.items = items == null ? new ArrayList<EditorialPairModels.PairItem>() : new ArrayList<>(items);
        this.active = active;
    }
}
