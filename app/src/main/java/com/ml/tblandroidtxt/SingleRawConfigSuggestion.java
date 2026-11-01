package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;

/** Display-only same-chapter profile suggestions for the one-RAW Translate workflow. */
final class SingleRawConfigSuggestion {
    static String format(int chapterNo, List<String> glossaryNames, List<String> pronounNames) {
        if (chapterNo < 0) {
            return "Không xác định số chương từ tên RAW. Cấu hình đang chọn được giữ nguyên.";
        }
        return "Gợi ý cùng chương " + chapterNo
                + "\nGlossary: " + matching(chapterNo, glossaryNames)
                + "\nPronoun: " + matching(chapterNo, pronounNames)
                + "\nCấu hình đang chọn được giữ nguyên.";
    }

    private static String matching(int chapterNo, List<String> names) {
        ArrayList<String> matches = new ArrayList<>();
        if (names != null) {
            for (String name : names) if (ChapterNumber.parse(name) == chapterNo) matches.add(name);
        }
        if (matches.isEmpty()) return "không có";
        StringBuilder out = new StringBuilder();
        for (String match : matches) {
            if (out.length() > 0) out.append(" · ");
            out.append(match);
        }
        return out.toString();
    }

    private SingleRawConfigSuggestion() {}
}
