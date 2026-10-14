package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** Read-only result projection. A FIXED proposal in a blocked chunk is not an applied edit. */
final class EditorialResultPresentation {
    static String reason(String code) {
        switch (code) {
            case "MODEL_KEEP": case "UNCHANGED": return "Giữ nguyên bản nháp";
            case "ID_NOT_RETURNED": return "Model chưa trả lời điểm này";
            case "EMPTY_INSERTION": return "Chưa bổ sung câu bị thiếu";
            case "INSERTED": return "Đã bổ sung câu bị thiếu";
            case "REPLACED": case "REPLACED_WITH_DRAFT_TERMINATOR": return "Đã sửa dòng";
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
        return item.state == PairState.ACCEPTED && "FIXED".equals(point.optString("status"))
                && !point.optString("before").equals(point.optString("after"));
    }
    static String summary(PairRun run, List<PairItem> items, int actualChangedLines) {
        int count=0, kept=0, blocked=0, pending=0;
        for(PairItem item:items) {
            if(item.state == PairState.IMPORTED) pending++;
            JSONArray points=points(item);
            for(int i=0;i<points.length();i++) {
                JSONObject p=points.optJSONObject(i); if(p==null) continue; count++;
                if(item.state == PairState.STRUCTURE_BLOCKED || item.state == PairState.REJECTED
                        || "REJECTED".equals(p.optString("status"))) blocked++;
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
            boolean changed=applied(item,p),blocked=item.state==PairState.STRUCTURE_BLOCKED||item.state==PairState.REJECTED||"REJECTED".equals(p.optString("status"));
            if((filter==0&&!changed)||(filter==1&&(changed||blocked))||(filter==2&&!blocked)) continue;
            String why=blocked&&"FIXED".equals(p.optString("status"))?"Đề xuất chưa vào đầu ra vì đoạn bị chặn":reason(p.optString("reason"));
            out.add("Điểm "+p.optInt("id")+": "+(changed?"Đã sửa":blocked?"Bị chặn":"Giữ nguyên")+" — "+why);
        }
        return out;
    }
}
