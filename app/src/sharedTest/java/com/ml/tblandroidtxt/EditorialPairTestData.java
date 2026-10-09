package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.PairText;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Synthetic rows and prompt reading shared by the host tests and the instrumented tests of the chunk-pair flow. */
public final class EditorialPairTestData {
    private static final String PART_HEADER = "# DRAFT (this part - return the edited version of exactly this text)\n";

    private EditorialPairTestData() { }

    public static String rawRow(int i) {
        return "第" + i + "章の本文です。花子は太郎に言った。\n「今日は静かな夜だね。」と花子は笑った。\n太郎は黙って頷き、遠くの灯りを見た。\n\n";
    }

    public static String draftRow(int i) {
        StringBuilder sb = new StringBuilder();
        for (int j = 1; j <= 4; j++) sb.append("Hàng ").append(i).append(", dòng ").append(j).append(": nội dung bản nháp có đủ chữ để cổng cấu trúc đo.\n");
        return sb.append('\n').toString();
    }

    public static EditorialPairSource source(int rows) {
        List<String> raw = new ArrayList<>();
        List<String> draft = new ArrayList<>();
        for (int i = 1; i <= rows; i++) { raw.add(rawRow(i)); draft.add(draftRow(i)); }
        return new EditorialPairSource(EditorialPairModels.SOURCE_JOB, "7", "truyen.txt", "", raw, draft, List.of());
    }

    /** The draft text of the pair a request asks to edit, exactly as the prompt carries it. */
    public static String draftPart(EditorialApiFlow.Request request) {
        String user = request.prompt().user();
        if (user.contains("# NUMBERED FIX POINTS\n")) {
            Matcher m = Pattern.compile("(?m)^DRAFT: (.*)$").matcher(user.substring(user.indexOf("# NUMBERED FIX POINTS\n")));
            return m.find() ? m.group(1) : "";
        }
        int a = user.indexOf(PART_HEADER);
        int start;
        if (a >= 0) start = a + PART_HEADER.length();
        else start = user.indexOf("# DRAFT\n") + "# DRAFT\n".length();
        int end = user.indexOf("\n\n# DRAFT CONTEXT AFTER", start);
        String part = end >= 0 ? user.substring(start, end) : user.substring(start);
        return PairText.trim(part);
    }

    /** Script a response in the CP-IMPL-6 one-line-per-point format without copying any other chunk text. */
    public static String targetedAnswer(EditorialApiFlow.Request request, int callIndex, BiFunction<Integer, String, String> edit) {
        String user = request.prompt().user();
        int start = user.indexOf("# NUMBERED FIX POINTS\n");
        if (start < 0) return "";
        String[] lines = user.substring(start).split("\n");
        StringBuilder out = new StringBuilder();
        for (int i = 1; i < lines.length; i++) {
            Matcher id = Pattern.compile("^\\[(\\d+)] TYPES: .*$").matcher(lines[i]);
            if (!id.matches()) continue;
            String draft = "";
            for (int j = i + 1; j < lines.length && !lines[j].startsWith("["); j++) {
                if (lines[j].startsWith("DRAFT: ")) { draft = lines[j].substring("DRAFT: ".length()); break; }
            }
            if (out.length() > 0) out.append('\n');
            out.append('[').append(id.group(1)).append("] ").append(edit.apply(callIndex, draft));
        }
        return out.toString();
    }
}
