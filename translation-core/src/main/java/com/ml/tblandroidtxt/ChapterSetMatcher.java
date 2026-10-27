package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Suggests, never decides: pairs each RAW with the Glossary and Pronoun profiles that start with the same chapter number, and rates
 * every row so the owner can see what needs a look. The owner confirms (and may change) every row before a set is saved.
 *
 * <pre>
 * GREEN  one RAW number, exactly one Glossary and exactly one Pronoun chosen
 * YELLOW the RAW is fine but a Glossary or a Pronoun is missing ("Không dùng" counts as chosen on purpose)
 * RED    the RAW has no chapter number, shares its number with another included RAW, or several profiles match and none is chosen
 * </pre>
 */
final class ChapterSetMatcher {
    enum Status { GREEN, YELLOW, RED }

    /** A named library item (RAW, Glossary or Pronoun) as the matcher sees it. */
    static final class Named {
        final String id;
        final String name;
        Named(String id, String name) { this.id = id; this.name = name == null ? "" : name; }
    }

    /** One editable row of the confirmation table. */
    static final class Row {
        String rawId = "";
        String rawName = "";
        int chapterNo = ChapterNumber.NONE;
        String glossaryId = "";
        String pronounId = "";
        /** The owner picked this field (even "Không dùng"), so a new chapter number does not re-suggest it. */
        boolean glossaryChosen;
        boolean pronounChosen;
        boolean excluded;
        /** Set when an opened set points at a RAW / profile that no longer exists; the row shows it as missing instead of breaking. */
        /** The whole slot (Glossary or Pronoun) is empty, so the row uses none on purpose and is not a warning. */
        boolean glossaryOff;
        /** A lone RAW may go without a chapter number (its pairing does not need one). */
        boolean numberOptional;
        boolean pronounOff;
        boolean rawMissing;
        boolean glossaryMissing;
        boolean pronounMissing;
        final List<String> glossaryCandidates = new ArrayList<>();
        final List<String> pronounCandidates = new ArrayList<>();
        Status status = Status.RED;
        String reason = "";
    }

    private ChapterSetMatcher() {}

    /** Rows for the RAW items, in the given (chapter) order, with suggestions and ratings filled in. */
    static List<Row> suggest(List<Named> raws, List<Integer> chapterNumbers, List<Named> glossaries, List<Named> pronouns) {
        ArrayList<Row> rows = new ArrayList<>();
        for (int i = 0; i < raws.size(); i++) {
            Row r = new Row();
            r.rawId = raws.get(i).id;
            r.rawName = raws.get(i).name;
            r.chapterNo = chapterNumbers.get(i);
            rows.add(r);
        }
        resuggest(rows, glossaries, pronouns);
        rate(rows);
        return rows;
    }

    /** Recomputes the candidates of every row and fills fields the owner has not chosen yet. */
    static void resuggest(List<Row> rows, List<Named> glossaries, List<Named> pronouns) {
        for (Row r : rows) {
            r.glossaryCandidates.clear();
            r.pronounCandidates.clear();
            if (r.chapterNo >= 0) {
                for (Named g : glossaries) if (ChapterNumber.parse(g.name) == r.chapterNo) r.glossaryCandidates.add(g.id);
                for (Named p : pronouns) if (ChapterNumber.parse(p.name) == r.chapterNo) r.pronounCandidates.add(p.id);
            }
            if (!r.glossaryChosen) r.glossaryId = r.glossaryCandidates.size() == 1 ? r.glossaryCandidates.get(0) : "";
            if (!r.pronounChosen) r.pronounId = r.pronounCandidates.size() == 1 ? r.pronounCandidates.get(0) : "";
        }
    }

    /** Sets {@code status} and {@code reason} on every row from its current values. Excluded rows are ignored (and count for nothing). */
    static void rate(List<Row> rows) {
        Map<Integer, Integer> perNumber = new HashMap<>();
        for (Row r : rows) if (!r.excluded && r.chapterNo >= 0) perNumber.merge(r.chapterNo, 1, Integer::sum);
        for (Row r : rows) {
            if (r.excluded) { r.status = Status.GREEN; r.reason = "Bỏ khỏi bộ"; continue; }
            if (r.rawMissing) { r.status = Status.RED; r.reason = "RAW đã bị xóa khỏi thư viện: bỏ dòng hoặc nhập lại RAW"; continue; }
            if (r.chapterNo < 0 && !r.numberOptional) { r.status = Status.RED; r.reason = "RAW không có số chương: nhập số chương hoặc bỏ dòng"; continue; }
            if (r.chapterNo >= 0 && perNumber.get(r.chapterNo) > 1) { r.status = Status.RED; r.reason = "Hai RAW cùng số chương " + r.chapterNo + ": đổi số hoặc bỏ một dòng"; continue; }
            if (!r.glossaryChosen && r.glossaryCandidates.size() > 1) { r.status = Status.RED; r.reason = r.glossaryCandidates.size() + " Glossary cùng chương " + r.chapterNo + " — chọn một"; continue; }
            if (!r.pronounChosen && r.pronounCandidates.size() > 1) { r.status = Status.RED; r.reason = r.pronounCandidates.size() + " Pronoun cùng chương " + r.chapterNo + " — chọn một"; continue; }
            boolean noGlossary = (r.glossaryId.isEmpty() && !r.glossaryChosen && !r.glossaryOff) || r.glossaryMissing;
            boolean noPronoun = (r.pronounId.isEmpty() && !r.pronounChosen && !r.pronounOff) || r.pronounMissing;
            if (noGlossary || noPronoun) {
                r.status = Status.YELLOW;
                String g = r.glossaryMissing ? "Glossary đã bị xóa" : "Thiếu Glossary";
                String p = r.pronounMissing ? "Pronoun đã bị xóa" : "Thiếu Pronoun";
                boolean plainBoth = noGlossary && noPronoun && !r.glossaryMissing && !r.pronounMissing;
                r.reason = plainBoth ? "Thiếu Glossary và Pronoun" : noGlossary && noPronoun ? g + "; " + p : noGlossary ? g : p;
                continue;
            }
            r.status = Status.GREEN;
            r.reason = "Đủ";
        }
    }

    static boolean hasRed(List<Row> rows) {
        for (Row r : rows) if (!r.excluded && r.status == Status.RED) return true;
        return false;
    }

    static int count(List<Row> rows, Status status) {
        int n = 0;
        for (Row r : rows) if (!r.excluded && r.status == status) n++;
        return n;
    }

    /** Default set name: the part of the RAW file names that all of them share after the chapter number ("025_Kumo7_RAW.txt" and "026_Kumo7_RAW.txt" give "Kumo7_RAW"). */
    static String defaultName(List<String> rawNames) {
        if (rawNames.isEmpty()) return "Tổ hợp";
        ArrayList<String> stems = new ArrayList<>();
        for (String n : rawNames) {
            String s = n == null ? "" : n.trim();
            if (s.toLowerCase(java.util.Locale.ROOT).endsWith(".txt")) s = s.substring(0, s.length() - 4);
            stems.add(s.replaceAll("^[0-9]+[_\\-\\s.]*", "").trim());
        }
        String prefix = stems.get(0);
        for (String s : stems) {
            int k = 0;
            while (k < prefix.length() && k < s.length() && prefix.charAt(k) == s.charAt(k)) k++;
            prefix = prefix.substring(0, k);
        }
        String trimmed = prefix.replaceAll("[_\\-\\s.]+$", "").trim();
        if (trimmed.isEmpty()) trimmed = stems.get(0).isEmpty() ? "Tổ hợp" : stems.get(0);
        return trimmed;
    }
}
