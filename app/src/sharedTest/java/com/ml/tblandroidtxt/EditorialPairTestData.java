package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.PairText;

import java.util.ArrayList;
import java.util.List;

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
        int a = user.indexOf(PART_HEADER);
        int start;
        if (a >= 0) start = a + PART_HEADER.length();
        else start = user.indexOf("# DRAFT\n") + "# DRAFT\n".length();
        int end = user.indexOf("\n\n# DRAFT CONTEXT AFTER", start);
        String part = end >= 0 ? user.substring(start, end) : user.substring(start);
        return PairText.trim(part);
    }
}
