package com.ml.tblandroidtxt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Pure navigation data used by the button-opened drawer. */
final class DrawerNavigationModel {
    static final class Entry {
        final String target;
        final String label;
        final String icon;

        Entry(String target, String label, String icon) {
            this.target = target;
            this.label = label;
            this.icon = icon;
        }
    }

    private DrawerNavigationModel() {}

    static List<Entry> destinations() {
        return Collections.unmodifiableList(Arrays.asList(
                new Entry("Translate", "Dịch", "↗"),
                new Entry("Jobs", "Công việc", "◷"),
                new Entry("Editorial", EditorialApiPresenter.TAB_TITLE, "✎"),
                new Entry("Library", "Thư viện", "▤")));
    }

    static boolean isSelected(String target, String page) {
        if ("Library".equals(target)) {
            return "Files".equals(page) || "RAW".equals(page) || "Glossaries".equals(page)
                    || "Pronouns".equals(page) || "ChapterSets".equals(page) || "Sample".equals(page);
        }
        if ("Jobs".equals(target)) return "Jobs".equals(page) || "JobsHistory".equals(page);
        return target != null && target.equals(page);
    }
}
