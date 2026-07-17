package com.ml.tblandroidtxt;

/** Pure presentation state derived from the same AppSettings snapshot used by a job. */
public final class TranslationConfigState {
    public enum Status { NOT_USED, VALID, WARNING, INVALID }

    public static final class Item {
        public final String title;
        public final String name;
        public final String detail;
        public final Status status;

        Item(String title, String name, String detail, Status status) {
            this.title = title;
            this.name = name;
            this.detail = detail;
            this.status = status;
        }

        public boolean isSelected() { return status != Status.NOT_USED; }
        public boolean isValidForStart() { return status != Status.INVALID; }
        public String statusLabel() {
            if (status == Status.VALID) return "Hợp lệ";
            if (status == Status.WARNING) return "Có cảnh báo";
            if (status == Status.INVALID) return "Không hợp lệ";
            return "Không sử dụng";
        }
        public String displayText() {
            String shownName = name == null || name.trim().isEmpty() ? "Không sử dụng" : name.trim();
            String suffix = detail == null || detail.trim().isEmpty() ? "" : " • " + detail.trim();
            return shownName + "\n" + statusLabel() + suffix;
        }
    }

    public final Item glossary;
    public final Item pronoun;
    public final Item instruction;
    public final boolean fromActiveJob;
    public final long jobId;

    public TranslationConfigState(Item glossary, Item pronoun, Item instruction,
                                  boolean fromActiveJob, long jobId) {
        this.glossary = glossary;
        this.pronoun = pronoun;
        this.instruction = instruction;
        this.fromActiveJob = fromActiveJob;
        this.jobId = jobId;
    }

    public static AppSettings selectSettings(AppSettings editable, AppSettings jobSnapshot, boolean serviceActive) {
        if (serviceActive && jobSnapshot != null) return jobSnapshot;
        return editable == null ? new AppSettings() : editable;
    }

    public static Item glossary(String name, int terms, int warnings) {
        if ((name == null || name.trim().isEmpty()) && terms <= 0) {
            return new Item("Glossaries", "", "", Status.NOT_USED);
        }
        Status status = warnings > 0 || terms <= 0 ? Status.WARNING : Status.VALID;
        return new Item("Glossaries", safeName(name, "Glossary"), terms + " mục", status);
    }

    public static Item pronoun(String name, int rules, int warnings) {
        if ((name == null || name.trim().isEmpty()) && rules <= 0) {
            return new Item("Pronoun", "", "", Status.NOT_USED);
        }
        Status status = warnings > 0 || rules <= 0 ? Status.WARNING : Status.VALID;
        return new Item("Pronoun", safeName(name, "Pronoun rules"), rules + " quy tắc", status);
    }

    public static Item instruction(String name, boolean hasTranslation, boolean hasRefinement,
                                   boolean refineEnabled, String fatalError) {
        boolean selected = (name != null && !name.trim().isEmpty()) || hasTranslation || hasRefinement
                || (fatalError != null && !fatalError.trim().isEmpty());
        if (!selected) return new Item("Instruction YAML", "", "", Status.NOT_USED);
        if (fatalError != null && !fatalError.trim().isEmpty()) {
            return new Item("Instruction YAML", safeName(name, "Instruction profile"), fatalError, Status.INVALID);
        }
        if (!hasTranslation && !hasRefinement) {
            return new Item("Instruction YAML", safeName(name, "Instruction profile"), "Không có translation/refinement", Status.INVALID);
        }
        String detail;
        if (hasTranslation && hasRefinement) detail = "translation + refinement";
        else if (hasTranslation) detail = "translation";
        else detail = refineEnabled ? "refinement" : "refinement chưa bật";
        return new Item("Instruction YAML", safeName(name, "Instruction profile"), detail, Status.VALID);
    }

    private static String safeName(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
