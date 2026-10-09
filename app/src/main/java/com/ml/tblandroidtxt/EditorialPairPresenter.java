package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.PairStates;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;
import com.ml.tblandroidtxt.editorial.api.PairStates.RunState;

import com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlan;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Every sentence of the chunk-pair screens, without Android types so the wording and the decisions are covered by JVM tests.
 * Three things are always kept apart: the structural check (is the answer safe to merge), the meaning (not measured here), and
 * whether the result can be saved and opened again. A merged text is never called "đạt" or "không còn lỗi". Pure.
 */
final class EditorialPairPresenter {
    static final String DIAGNOSTICS_TITLE = "Chẩn đoán kỹ thuật";

    private EditorialPairPresenter() { }

    // ---- why it cannot run, and what is only a warning

    static String blocker(String code) {
        String name = code;
        String arg = "";
        int colon = code.indexOf(':');
        if (colon > 0) { name = code.substring(0, colon); arg = code.substring(colon + 1); }
        switch (name) {
            case "SOURCE_NO_EXPLICIT_MAPPING":
                return "Hai file riêng chưa có liên kết đoạn RAW–DRAFT, nên chưa chạy theo từng đoạn được. Hãy chọn một job Dịch, hoặc chạy toàn chương.";
            case "SOURCE_CHAPTER_BLOCKED":
                return "RAW và DRAFT có vẻ không cùng chương, nên chưa biên tập. Hãy chọn lại file.";
            case "SOURCE_NO_ROWS": case "SOURCE_NO_ROWS_":
                return "Job chưa có đoạn nào để biên tập.";
            case "SOURCE_ROW_OFFSET_GAP":
                return "Đoạn " + arg + " của job không nối liền với đoạn trước (RAW bị hở hoặc trùng).";
            case "SOURCE_ROW_RANGE_MISMATCH":
                return "Đoạn " + arg + " của job có độ dài không khớp vùng của nó trong RAW.";
            case "SOURCE_ROW_HASH_MISMATCH":
                return "Nội dung RAW của đoạn " + arg + " không khớp mã kiểm tra mà job đã lưu.";
            case "SOURCE_ROW_EMPTY_SOURCE":
                return "Đoạn " + arg + " của job không có RAW.";
            case "MISSING_PAIRS":
                return "Còn " + arg + " đoạn chưa có bản dịch hoàn tất. Hãy dịch xong job trước khi biên tập.";
            case "TOO_LONG":
                return "Các đoạn số " + arg.replace("[", "").replace("]", "") + " quá dài cho một lượt gọi. Hãy dịch lại job với đoạn nhỏ hơn.";
            default:
                if (name.startsWith("MAP_")) return "Liên kết RAW–DRAFT không hợp lệ (" + name.substring(4) + ").";
                if (name.startsWith("PAIR_")) return "Một đoạn không thể tạo yêu cầu (" + name.substring(5) + ").";
                return "Chưa chạy được (" + code + ").";
        }
    }

    static String warning(String code) {
        String name = code;
        String arg = "";
        int colon = code.indexOf(':');
        if (colon > 0) { name = code.substring(0, colon); arg = code.substring(colon + 1); }
        switch (name) {
            case "CHAPTER_WARN": return "RAW và DRAFT khớp chưa chắc chắn; hãy xem lý do ở trên trước khi chạy.";
            case "UNCERTAIN_CHUNKS": return arg + " đoạn chưa chắc về cách ghép RAW–DRAFT (đánh dấu trong kết quả).";
            case "NO_GLOSSARY": return "Không dùng Glossary: tên riêng có thể không thống nhất giữa các đoạn.";
            case "NO_PRONOUN": return "Không dùng Pronoun: cách xưng hô giữ như trong DRAFT.";
            case "REFERENCE_CONFLICT": return arg + " chỗ có quy tắc xưng hô mâu thuẫn trong cùng phạm vi. Ứng dụng không tự chọn; model được dặn giữ cách xưng hô của DRAFT ở đó.";
            case "SCOPE_INVALID": return arg + " dòng Pronoun có phạm vi áp dụng sai nên bị bỏ qua.";
            default: return code;
        }
    }

    static List<String> blockers(EditorialPairPreview preview) {
        List<String> out = new ArrayList<>();
        for (String b : preview.blockers) out.add(blocker(b));
        return out;
    }

    static List<String> warnings(EditorialPairPreview preview) {
        List<String> out = new ArrayList<>();
        for (String w : preview.warnings) out.add(warning(w));
        return out;
    }

    // ---- the chunk plan of two files: verdict before anything is sent

    /** What the person reads first: the verdict of the pair of files. Plain words, no engineering terms. */
    static String verdictHeadline(ChunkPlan plan, String performanceLine) {
        switch (plan.verdict) {
            case "BLOCK": return "RAW và DRAFT có vẻ không cùng chương";
            case "WARN": return "RAW và DRAFT khớp chưa chắc chắn";
            default: return okSummary(plan, performanceLine);
        }
    }

    static String okSummary(ChunkPlan plan, String performanceLine) {
        StringBuilder sb = new StringBuilder("Chia " + plan.chunks.size() + " đoạn theo cài đặt Performance (" + performanceLine + ").");
        int regrouped = plan.splitGroups + plan.mergeGroups;
        List<String> parts = new ArrayList<>();
        if (regrouped > 0) parts.add("DRAFT tách/gộp dòng ở " + regrouped + " chỗ");
        if (plan.rawOnly > 0) parts.add(plan.rawOnly + " câu RAW chưa có trong DRAFT");
        if (!parts.isEmpty()) sb.append(' ').append(String.join("; ", parts)).append('.');
        if (plan.uncertainChunks > 0) sb.append(' ').append(plan.uncertainChunks).append(" đoạn chưa chắc, đã được đánh dấu.");
        return sb.toString();
    }

    /** At most two reasons with their numbers, the blocking ones first. */
    static List<String> verdictReasons(ChunkPlan plan) {
        List<ChunkPlan.Reason> ordered = new ArrayList<>();
        for (ChunkPlan.Reason r : plan.reasons) if ("BLOCK".equals(r.level())) ordered.add(r);
        for (ChunkPlan.Reason r : plan.reasons) if (!"BLOCK".equals(r.level())) ordered.add(r);
        List<String> out = new ArrayList<>();
        for (ChunkPlan.Reason r : ordered) {
            if (out.size() == 2) break;
            out.add(reasonText(plan, r));
        }
        return out;
    }

    static String reasonText(ChunkPlan plan, ChunkPlan.Reason r) {
        switch (r.code()) {
            case "NAME_MATCH": return "chỉ " + percent(r.value()) + "% tên riêng trong RAW có mặt ở dòng DRAFT tương ứng";
            case "EDGE_SYMBOLS": return percent(1 - r.value()) + "% dòng có dấu thoại/ký hiệu khác nhau";
            case "UNPAIRED_LINES": return (plan.rawOnly + plan.draftOnly) + " dòng không ghép được";
            case "LENGTH_RATIO": return "DRAFT " + (r.value() >= 0 ? "dài" : "ngắn") + " hơn mức thường " + percent(Math.abs(r.value())) + "%";
            case "CHUNK_CHECKS": return percent(r.value()) + "% đoạn có độ dài hoặc số câu thoại lệch bất thường";
            case "EMPTY_SOURCE": return "một trong hai file không có chữ nào";
            default: return r.code();
        }
    }

    private static long percent(double share) { return Math.round(share * 100); }

    /** 1-based ordinals of the chunks the plan marked uncertain; empty when the run has no plan or it does not match the chunk count. */
    static java.util.Set<Integer> uncertainOrdinals(String planJson, int itemCount) {
        java.util.Set<Integer> out = new java.util.HashSet<>();
        if (planJson == null || planJson.isEmpty()) return out;
        try {
            ChunkPlan plan = ChunkPlan.fromJson(planJson);
            if (plan.chunks.size() != itemCount) return out;
            for (int i = 0; i < plan.chunks.size(); i++) if (plan.chunks.get(i).uncertain()) out.add(i + 1);
        } catch (RuntimeException unreadable) {
            return new java.util.HashSet<>();
        }
        return out;
    }

    /** The Performance values the plan was cut with, as the person knows them from the settings screen. */
    static String performanceLine(AppSettings s) {
        String mode = "char".equalsIgnoreCase(s.chunkMode) ? "ký tự" : "token";
        return mode + " " + s.effectiveHardLimit() + " · mềm " + String.format(Locale.ROOT, "%.1f", s.softLimitRatio);
    }

    // ---- preview rows and cost

    static String previewRow(EditorialPairPreview.Row row) {
        String state = row.missing ? "Thiếu bản dịch" : row.tooLong ? "Quá dài" : "Sẵn sàng";
        return "Đoạn " + row.ordinal + " • RAW " + row.rawChars + " chữ • DRAFT " + row.draftChars + " chữ • " + state;
    }

    static String previewTotals(EditorialPairPreview preview) {
        int total = preview.rows.size();
        return total + " đoạn" + (preview.missingCount > 0 ? ", " + preview.missingCount + " thiếu bản dịch" : "")
                + (preview.runnable() ? "" : " • chưa thể chạy");
    }

    /** Rough estimate: every pair sends its prompt and gets back about its own text. The cap is enforced per call, not from this. */
    static String costLine(EditorialPairPreview preview, BigDecimal inputPerToken, BigDecimal outputPerToken, BigDecimal capUsd) {
        long in = 0;
        long out = 0;
        for (EditorialPairPreview.Row r : preview.rows) {
            if (r.missing) continue;
            in += r.estimatedInputTokens;
            out += Math.max(1, r.draftChars / 2);
        }
        BigDecimal usd = inputPerToken.multiply(BigDecimal.valueOf(in)).add(outputPerToken.multiply(BigDecimal.valueOf(out))).setScale(3, RoundingMode.UP);
        return "Ước tính chi phí: khoảng " + EditorialApiPresenter.usd(usd) + " cho " + preview.rows.size() + " lượt gọi (trần " + EditorialApiPresenter.usd(capUsd)
                + " cho cả lần chạy; mỗi lượt được giữ chỗ chi phí trước khi gửi)";
    }

    // ---- states

    static String pairState(PairState state) {
        switch (state) {
            case IMPORTED: return "Chờ gửi";
            case RESERVE_FAILED: return "Không gửi: hết hạn mức chi phí";
            case E_RESERVED: return "Đã giữ chỗ chi phí";
            case E_SENT: return "Đã gửi, chờ kết quả";
            case E_RECEIVED: return "Đã nhận";
            case STRUCTURE_BLOCKED: return "Bị chặn: cấu trúc không đáng tin";
            case WARN_REVIEW: return "Có cảnh báo cấu trúc: cần xem";
            case CHECK_PENDING: return "Chờ kiểm";
            case ACCEPTED: return "Đã nhận, qua kiểm cấu trúc";
            case REJECTED: return "Đã loại";
            case UNKNOWN: return "Không rõ kết quả: không gửi lại";
            case MISSING: return "Thiếu bản dịch";
            default: return "";
        }
    }

    static String runHeadline(PairRun run, boolean executing, boolean interrupted) {
        if (executing) return "Đang biên tập từng đoạn…";
        switch (run.state) {
            case PREPARED: return "Chưa chạy";
            case RUNNING: return interrupted ? "Lần chạy bị gián đoạn" : "Đang chạy…";
            case PAUSED: return "Đã tạm dừng";
            case INCOMPLETE: return "Chưa xong: còn đoạn chưa gửi";
            case UNKNOWN: return "Có đoạn không rõ kết quả";
            case FINAL_BLOCKED: return "Có đoạn bị chặn: chưa ghép được bản đủ";
            case FINAL_ELIGIBLE: return run.warnings > 0
                    ? "Đã ghép đủ các đoạn, có " + run.warnings + " cảnh báo cấu trúc"
                    : "Đã ghép đủ các đoạn";
            default: return "";
        }
    }

    /** What the headline does NOT say: the three questions stay apart. */
    static List<String> statusLines(PairRun run, List<PairItem> items) {
        List<String> out = new ArrayList<>();
        int accepted = 0;
        int warned = 0;
        int blocked = 0;
        int unknown = 0;
        for (PairItem i : items) {
            if (i.state == PairState.ACCEPTED) accepted++;
            else if (i.state == PairState.WARN_REVIEW) warned++;
            else if (i.state == PairState.STRUCTURE_BLOCKED || i.state == PairState.REJECTED) blocked++;
            else if (i.state == PairState.UNKNOWN) unknown++;
        }
        out.add("Cấu trúc: " + accepted + " đoạn đạt, " + warned + " có cảnh báo, " + blocked + " bị chặn, " + unknown + " không rõ, trên tổng " + items.size() + " đoạn.");
        out.add("Nghĩa: chưa được chấm. Ghép thành công không có nghĩa là bản dịch đúng.");
        out.add(run.state == RunState.FINAL_ELIGIBLE
                ? "Lưu và xuất: đã lưu, mở lại được; có thể xuất bản ghép."
                : "Lưu và xuất: tiến độ đã lưu, mở lại được; xuất chỉ cho bản tạm.");
        if (!run.costKnown) out.add("Chi phí: có lượt gọi chưa rõ chi phí; giữ chỗ ở mức tối đa, kiểm tra bảng giá nhà cung cấp.");
        if (run.costOverrun) out.add("Chi phí: một lượt gọi tốn hơn mức đã giữ chỗ nên không gửi thêm.");
        return out;
    }

    static boolean interrupted(PairRun run, List<PairItem> items) {
        if (EditorialPairRunService.isActive(run.id)) return false;
        for (PairItem i : items) if (i.state == PairState.E_RESERVED || i.state == PairState.E_SENT) return true;
        return false;
    }

    // ---- gate codes in plain words

    static String gateCode(String code) {
        switch (code) {
            case "CHARS_LOSS": return "Mất nhiều chữ so với DRAFT";
            case "CHARS_GROWTH": return "Thêm quá nhiều chữ so với DRAFT";
            case "CHARS_LOSS_WARN": return "Ít chữ hơn DRAFT khá nhiều";
            case "CHARS_GROWTH_WARN": return "Nhiều chữ hơn DRAFT khá nhiều";
            case "LINE_DELTA": return "Số dòng thay đổi";
            case "REFLOW_ONLY": return "Chỉ xuống dòng lại, chữ giữ nguyên";
            case "MARKER_MISMATCH": return "Dòng ký hiệu (như ◆) bị mất, thêm hoặc đổi chỗ";
            case "MARKER_NOT_OWN_LINE": return "Dòng ký hiệu không còn nằm riêng một dòng";
            case "BLANK_RUN_CHANGED": return "Dòng trống trong văn bản thay đổi";
            case "BOUNDARY_WS_TRIMMED": return "Khoảng trắng thừa ở hai đầu đã được cắt";
            case "META_LEAK": return "Lời giải thích lẫn trong văn bản đã được bỏ";
            case "META_UNSEPARABLE": return "Lời giải thích lẫn trong văn bản, không tách được";
            case "PARAGRAPH_LABEL_LEAK": return "Nhãn đoạn của RAW bị chép vào kết quả";
            case "CANDIDATE_EMPTY": return "Kết quả trống";
            case "PAIR_ID_MISMATCH": return "Phản hồi không khớp yêu cầu";
            case "FINISH_REASON": return "Phản hồi không kết thúc bình thường";
            case "FORMAT": return "Phản hồi không đúng định dạng";
            case "TRUNCATED": return "Phản hồi bị cắt";
            case "WRONG_PAIR": return "Model báo RAW và DRAFT có vẻ không cùng đoạn";
            case "PROVIDER_FAILURE": return "Nhà cung cấp lỗi trước khi nhận yêu cầu";
            case "HARD_LIMIT": return "Vượt giới hạn độ dài";
            default: return code;
        }
    }

    static List<String> gateLines(PairItem item) {
        List<String> out = new ArrayList<>();
        try {
            JSONArray codes = new JSONObject(item.gateJson).optJSONArray("codes");
            if (codes != null) {
                for (int i = 0; i < codes.length(); i++) {
                    JSONObject c = codes.getJSONObject(i);
                    String text = gateCode(c.optString("code"));
                    String detail = c.optString("detail");
                    boolean block = "BLOCK".equals(c.optString("severity"));
                    out.add((block ? "Chặn: " : "Cảnh báo: ") + text + (detail.isEmpty() || detail.length() > 80 ? "" : " (" + detail + ")"));
                }
            }
        } catch (Exception unreadable) {
            // a column that cannot be read shows no codes
        }
        return out;
    }

    // ---- export

    static String exportName(String comboName, EditorialPairRunService.ExportPlan plan) {
        String base = comboName == null || comboName.trim().isEmpty() ? "editorial" : comboName.trim();
        return FileUtil.sanitizeOutputName(base + plan.fileSuffix + ".txt");
    }

    static String detailsText(PairRun run, List<PairItem> items) {
        StringBuilder out = new StringBuilder();
        out.append("Model: ").append(run.model).append('\n');
        out.append("Hợp đồng: ").append(run.contractRevision).append('\n');
        out.append("Nguồn: ").append(run.sourceKind).append(' ').append(run.sourceLabel).append('\n');
        out.append("Liên kết: ").append(run.mapRevision).append(" • ").append(run.mapHash).append('\n');
        out.append("Lượt gọi: ").append(run.calls).append(" • token vào/ra: ").append(run.inputTokens).append('/').append(run.outputTokens)
                .append(" • ").append(EditorialApiPresenter.usd(run.usd)).append(run.costKnown ? "" : " (chưa rõ giá)").append('\n');
        if (!run.mergeReceiptJson.isEmpty()) out.append("Biên nhận ghép: ").append(run.mergeReceiptJson).append('\n');
        for (PairItem i : items) {
            out.append("Đoạn ").append(i.ordinal).append(": ").append(i.state).append(" • ").append(i.calls).append(" lượt • ")
                    .append(EditorialApiPresenter.usd(i.usd)).append(i.costKnown ? "" : " (chưa rõ)").append(' ').append(i.responseHash.isEmpty() ? "" : i.responseHash.substring(0, 12)).append('\n');
        }
        return out.toString();
    }
}
