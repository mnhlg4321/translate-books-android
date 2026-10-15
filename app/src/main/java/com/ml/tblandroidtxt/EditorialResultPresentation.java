package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.PairStates;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/**
 * Read-only result projection. A FIXED proposal in a blocked chunk is not an applied edit. A line the checks refused keeps its
 * proposal ("proposed") until the person applies it; it is counted as blocked, never as a plain "kept".
 */
final class EditorialResultPresentation {
    static String reason(String code) {
        switch (code) {
            case "MODEL_KEEP": case "UNCHANGED": return "Giữ nguyên bản nháp";
            case "ID_NOT_RETURNED": return "Model chưa trả lời điểm này";
            case "EMPTY_INSERTION": return "Chưa bổ sung câu bị thiếu";
            case "INSERTED": return "Đã bổ sung câu bị thiếu";
            case "REPLACED": case "REPLACED_WITH_DRAFT_TERMINATOR": return "Đã sửa dòng";
            case "OWNER_APPLIED": return "Bạn đã áp dụng đề xuất của model";
            case "GLOSSARY_TARGET_REMOVED": return "Giữ thuật ngữ đúng đã có trong bản nháp";
            case "PROTECTED_SYMBOL_CHANGE": return "Đề xuất đổi dấu được bảo vệ nên giữ bản nháp";
            case "EMPTY_CANDIDATE": return "Đề xuất trống";
            case "MULTILINE_CANDIDATE": return "Đề xuất vượt quá một dòng";
            case "META_OR_TAG": return "Đề xuất lẫn lời giải thích hoặc thẻ";
            case "NEW_KANA_OR_HAN": return "Đề xuất thêm chữ Nhật không được phép";
            case "LINE_LENGTH_RATIO": return "Độ dài đề xuất lệch quá nhiều";
            case "INSERT_ANCHOR_OUT_OF_RANGE": case "DRAFT_LINE_OUT_OF_RANGE": return "Vị trí dòng không hợp lệ";
            case "ADDRESS_LINE_TOO_LONG": return "Dòng xưng hô quá dài để kiểm an toàn";
            case "ADDRESS_SCOPE_EXCEEDED": return "Đề xuất sửa ngoài phần xưng hô";
            case "ADDRESS_ROW_NOT_CITED": return "Chưa chỉ rõ hàng Pronoun áp dụng";
            case "ADDRESS_ROW_CITATION_INVALID": return "Hàng Pronoun được trích không hợp lệ";
            case "ADDRESS_AMBIGUOUS_AUDIENCE": return "Chưa rõ người nghe trong lời nói với nhóm";
            case "ADDRESS_NAME_SWAPPED": return "Đề xuất đổi sang tên nhân vật khác";
            case "ADDRESS_NAME_COMBINED": return "Đề xuất ghép tên nhân vật";
            case "ADDRESS_CITED_ROW_EXCLUDED": return "Hàng Pronoun bị ngữ cảnh loại trừ";
            case "ADDRESS_CITED_ROW_NOT_ADDRESSED": return "Hàng Pronoun không khớp người được gọi";
            case "ADDRESS_FORM_NOT_IN_CITED_ROW": return "Dạng xưng hô không thuộc hàng đã trích";
            case "ADDRESS_VALID_FORM_CHANGED": return "Đề xuất đổi dạng xưng hô vốn đúng";
            case "ADDRESS_FORM_NOT_IN_APPLICABLE_ROW": return "Dạng xưng hô không thuộc hàng đang áp dụng";
            case "ADDRESS_BREAKS_CONSISTENT_ROW": return "Đề xuất trộn cách xưng hô của nhiều hàng";
            case "ADDRESS_PLURAL_PHRASE": case "ADDRESS_PLURAL_SELF": case "ADDRESS_PLURAL_ADDRESSEE": return "Đề xuất đổi cách xưng hô số nhiều";
            default: return "Lý do khác";
        }
    }
    static JSONArray points(PairItem item) {
        try { JSONArray p = new JSONObject(item.gateJson).optJSONArray("fixPoints"); return p == null ? new JSONArray() : p; }
        catch (Exception invalid) { return new JSONArray(); }
    }
    static boolean applied(PairItem item, JSONObject point) {
        if ("OWNER_APPLIED".equals(point.optString("status"))) return PairStates.mergeable(item.state);
        return item.state == PairState.ACCEPTED && "FIXED".equals(point.optString("status"))
                && !point.optString("before").equals(point.optString("after"));
    }
    /** The model proposed a line, a check refused it, and the person has not applied it yet. */
    static boolean awaiting(PairItem item, JSONObject point) {
        return !point.optString("proposed").isEmpty() && !"OWNER_APPLIED".equals(point.optString("status"));
    }
    /** What the person reads next to a refused proposal: why it was refused and how the row can carry the form next time. */
    static String proposalHint(JSONObject point) {
        String code = point.optString("reason");
        String row = point.optString("citedRowText");
        String why = "Bị chặn vì: " + reason(code) + (row.isEmpty() ? "" : " (hàng " + row + ")") + ".";
        if ("ADDRESS_FORM_NOT_IN_CITED_ROW".equals(code) || "ADDRESS_FORM_NOT_IN_APPLICABLE_ROW".equals(code))
            return why + " Nếu đề xuất đúng, bấm Áp dụng. Để lần sau app tự nhận, thêm dạng đó vào cột self hoặc call của hàng,"
                    + " nhiều dạng cách nhau bằng /, ví dụ Yuki-chan/em.";
        return why + " App không biết chắc ai đang nói nên không tự áp dụng; đọc so sánh rồi quyết.";
    }
    static String summary(PairRun run, List<PairItem> items, int actualChangedLines) {
        int count=0, kept=0, blocked=0, pending=0;
        for(PairItem item:items) {
            if(item.state == PairState.IMPORTED) pending++;
            JSONArray points=points(item);
            for(int i=0;i<points.length();i++) {
                JSONObject p=points.optJSONObject(i); if(p==null) continue; count++;
                if(item.state == PairState.STRUCTURE_BLOCKED || item.state == PairState.REJECTED
                        || "REJECTED".equals(p.optString("status")) || awaiting(item,p)) blocked++;
                else if(!applied(item,p)) kept++;
            }
        }
        return actualChangedLines+" dòng đã sửa · "+count+" điểm nghi ngờ · "+kept+" giữ nguyên · "+blocked
                +" bị chặn · "+EditorialApiPresenter.usd(run.usd)
                +(pending>0 ? " · còn "+pending+" đoạn chưa chạy" : "");
    }
    static List<String> readablePoints(PairItem item, int filter) {
        List<String> out=new ArrayList<>(); JSONArray points=points(item);
        for(int i=0;i<points.length();i++) {
            JSONObject p=points.optJSONObject(i); if(p==null) continue;
            boolean changed=applied(item,p),blocked=item.state==PairState.STRUCTURE_BLOCKED||item.state==PairState.REJECTED||"REJECTED".equals(p.optString("status"))||awaiting(item,p);
            if((filter==0&&!changed)||(filter==1&&(changed||blocked))||(filter==2&&!blocked)) continue;
            String why=blocked&&"FIXED".equals(p.optString("status"))?"Đề xuất chưa vào đầu ra vì đoạn bị chặn":reason(p.optString("reason"));
            if(awaiting(item,p)) why=reason(p.optString("reason"))+"; model đề xuất: "+p.optString("proposed");
            out.add("Điểm "+p.optInt("id")+": "+(changed?"Đã sửa":blocked?"Bị chặn":"Giữ nguyên")+" — "+why);
        }
        return out;
    }
}
