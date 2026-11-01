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
            boolean unnamed = name == null || name.trim().isEmpty();
            String suffix = detail == null || detail.trim().isEmpty() ? "" : " • " + detail.trim();
            // Nothing chosen: "Không sử dụng" once (the name and the status label would both say it).
            if (unnamed && status == Status.NOT_USED) return "Không sử dụng" + suffix;
            String shownName = unnamed ? "Không sử dụng" : name.trim();
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
            return new Item("Glossary", "", "", Status.NOT_USED);
        }
        Status status = warnings > 0 || terms <= 0 ? Status.WARNING : Status.VALID;
        return new Item("Glossary", safeName(name, "Glossary"), terms + " mục", status);
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

    /** Compact, valid-configuration summary used when the detail rows are collapsed. */
    public String compactSummary() {
        return compactSummary(false);
    }

    public String compactSummary(boolean perChapter) {
        String summary = "Glossary: " + compactCount(glossary)
                + " · Pronoun: " + compactCount(pronoun)
                + " · YAML: " + compactInstruction(instruction);
        return perChapter ? summary + " · theo từng chương của tập" : summary;
    }

    private static String compactCount(Item item) {
        if (item == null || item.status == Status.NOT_USED) return "Không dùng";
        String detail = item.detail == null ? "" : item.detail.trim();
        int split = detail.indexOf(' ');
        String count = split > 0 ? detail.substring(0, split) : detail;
        String value = compactName(item.name) + " (" + (count.isEmpty() ? "—" : count) + ")";
        return item.status == Status.VALID ? value : value + " !";
    }

    private static String compactInstruction(Item item) {
        if (item == null || item.status == Status.NOT_USED) return "Không dùng";
        return compactName(item.name) + (item.isValidForStart() ? " ✓" : " !");
    }

    /** Truncates long names in the middle and keeps the first chapter-number run visible. */
    private static String compactName(String raw) {
        String name = raw == null ? "" : raw.trim();
        final int max = 32;
        int points = name.codePointCount(0, name.length());
        if (points <= max) return name;

        int numberStart = -1;
        int numberEnd = -1;
        for (int i = 0; i < name.length(); ) {
            int cp = name.codePointAt(i);
            int next = i + Character.charCount(cp);
            if (Character.isDigit(cp)) {
                numberStart = i;
                numberEnd = next;
                while (numberEnd < name.length() && Character.isDigit(name.codePointAt(numberEnd))) {
                    numberEnd += Character.charCount(name.codePointAt(numberEnd));
                }
                break;
            }
            i = next;
        }

        if (numberStart < 0) return middle(name, max);
        String chapter = name.substring(numberStart, numberEnd);
        int chapterPoints = chapter.codePointCount(0, chapter.length());
        int room = max - chapterPoints - 2;
        if (room <= 0) return middle(name, max);
        int beforePoints = Math.min(name.codePointCount(0, numberStart), room / 2);
        int afterPoints = Math.min(name.codePointCount(numberEnd, name.length()), room - beforePoints);
        String before = takeStart(name.substring(0, numberStart), beforePoints);
        String after = takeEnd(name.substring(numberEnd), afterPoints);
        String leftMark = before.isEmpty() ? "" : "…";
        String rightMark = after.isEmpty() ? "" : "…";
        return before + leftMark + chapter + rightMark + after;
    }

    private static String middle(String value, int maxCodePoints) {
        int keep = Math.max(1, (maxCodePoints - 1) / 2);
        return takeStart(value, keep) + "…" + takeEnd(value, keep);
    }

    private static String takeStart(String value, int count) {
        int end = value.offsetByCodePoints(0, Math.min(count, value.codePointCount(0, value.length())));
        return value.substring(0, end);
    }

    private static String takeEnd(String value, int count) {
        int points = value.codePointCount(0, value.length());
        int start = value.offsetByCodePoints(0, Math.max(0, points - Math.max(0, count)));
        return value.substring(start);
    }
}
