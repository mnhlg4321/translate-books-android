package com.ml.tblandroidtxt;

/**
 * Chapter number from the start of a file or profile name: {@code 025_RAW_x.txt} and {@code 25-x} are chapter 25.
 * Digits in the middle of a name ({@code VOL08_013}, {@code Ex2}, {@code 第3章}) are never read, so a volume is not mistaken for a chapter.
 */
final class ChapterNumber {
    static final int NONE = -1;

    /** The leading integer of the name, or {@link #NONE} when the name does not start with a digit. */
    static int parse(String name) {
        if (name == null) return NONE;
        String s = name.trim();
        int end = 0;
        while (end < s.length() && s.charAt(end) >= '0' && s.charAt(end) <= '9') end++;
        if (end == 0 || end > 9) return NONE;
        return Integer.parseInt(s.substring(0, end));
    }

    /** Order by chapter number as a number (9 < 13 < 100); names without a number go last, then by name. */
    static int compare(int chapterA, String nameA, int chapterB, String nameB) {
        boolean noneA = chapterA < 0, noneB = chapterB < 0;
        if (noneA != noneB) return noneA ? 1 : -1;
        if (!noneA && chapterA != chapterB) return Integer.compare(chapterA, chapterB);
        return String.valueOf(nameA).compareToIgnoreCase(String.valueOf(nameB));
    }

    private ChapterNumber() {}
}
