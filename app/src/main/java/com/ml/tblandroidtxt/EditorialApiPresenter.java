package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.EditPromptBuilder;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;
import com.ml.tblandroidtxt.editorial.api.LineDiff;
import com.ml.tblandroidtxt.editorial.api.SourceCheck;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Everything the Biên tập screens say, as plain Vietnamese and without Android types, so the wording and the decisions
 * (what blocks, what only warns, what the cost estimate is) are covered by JVM tests. The views only place these values.
 */
final class EditorialApiPresenter {
    /** The one place a legacy term may appear in front of the person: the collapsed developer section. */
    static final String LEGACY_SECTION_TITLE = "Công cụ dev — SAFE4 legacy";
    static final String TAB_TITLE = "Biên tập";

    static final int HEAD_LINES = 3;
    static final int HEAD_LINE_CHARS = 90;
    static final int MAX_DIFF_ROWS = 40;

    private EditorialApiPresenter() { }

    // ---- list ----

    static final class ComboRow {
        final long comboId;
        final String title;
        final String status;
        final boolean hasResult;

        ComboRow(long comboId, String title, String status, boolean hasResult) {
            this.comboId = comboId;
            this.title = title;
            this.status = status;
            this.hasResult = hasResult;
        }
    }

    static ComboRow row(EditorialApiCombo combo, EditorialApiRun latest) {
        boolean has = latest != null && latest.finished();
        return new ComboRow(combo.id, combo.name.isEmpty() ? "(chưa đặt tên)" : combo.name, statusLabel(latest), has);
    }

    static String statusLabel(EditorialApiRun run) {
        if (run == null) return "Chưa chạy";
        switch (run.state) {
            case RUNNING: return EditorialApiRunService.isInterrupted(run) ? "Bị gián đoạn — hãy chạy lại" : "Đang chạy…";
            case FINAL_OK: return "Xong";
            case FINAL_NOTES: return "Xong — có mục cần xem";
            case RETRY_REQUIRED: return "Chưa xong — cần chạy lại";
            case WRONG_PAIR: return "RAW và DRAFT có vẻ khác chương";
            case CANCELLED: return "Đã hủy";
            default: return "";
        }
    }

    static String modeLabel(EditorialApiContract.Mode mode) {
        return mode == EditorialApiContract.Mode.THOROUGH ? "Kỹ" : "Nhanh";
    }

    // ---- combo screen ----

    /** The name offered when the person has not typed one. */
    static String suggestedName(EditorialApiCombo combo, String glossaryName, String pronounName) {
        return EditorialApiCombo.suggestName(combo.rawName, combo.draftName, glossaryName, pronounName);
    }

    /** What stops "Tiếp tục": the two files are mandatory, the references are not. Empty = can continue. */
    static String missingForConfirmation(EditorialApiCombo combo) {
        if (EditorialPairModels.SOURCE_JOB.equals(combo.sourceKind)) return combo.jobId > 0 ? "" : "Chọn một job Dịch.";
        if (combo.rawUri.isEmpty() && combo.draftUri.isEmpty()) return "Chọn file RAW và file DRAFT.";
        if (combo.rawUri.isEmpty()) return "Chọn file RAW.";
        if (combo.draftUri.isEmpty()) return "Chọn file DRAFT.";
        return "";
    }

    // ---- confirmation ----

    static final class Confirmation {
        String comboName = "";
        String rawName = "";
        String draftName = "";
        String rawHead = "";
        String draftHead = "";
        int rawChars;
        int draftChars;
        String glossaryLine = "";
        String pronounLine = "";
        String modeLine = "";
        String modelLine = "";
        String costLine = "";
        final List<String> warnings = new ArrayList<>();
        final List<String> blockers = new ArrayList<>();

        boolean canRun() { return blockers.isEmpty(); }
    }

    static Confirmation confirmation(EditorialApiCombo combo, EditorialApiSources sources, String glossaryName, String pronounName,
                                     String model, BigDecimal inputPerToken, BigDecimal outputPerToken) {
        return confirmation(combo, sources, glossaryName, pronounName, model, inputPerToken, outputPerToken, true);
    }

    static Confirmation confirmation(EditorialApiCombo combo, EditorialApiSources sources, String glossaryName, String pronounName,
                                     String model, BigDecimal inputPerToken, BigDecimal outputPerToken, boolean priceKnown) {
        Confirmation c = new Confirmation();
        EditorialApiCombo.Settings settings = combo.settings();
        c.comboName = combo.name;
        c.rawName = combo.rawName.isEmpty() ? "RAW" : combo.rawName;
        c.draftName = combo.draftName.isEmpty() ? "DRAFT" : combo.draftName;
        c.rawChars = sources.raw.length();
        c.draftChars = sources.draft.length();
        c.rawHead = head(sources.raw);
        c.draftHead = head(sources.draft);
        c.modeLine = "Chế độ: " + modeLabel(settings.mode) + (settings.mode == EditorialApiContract.Mode.THOROUGH
                ? " (biên tập, kiểm, kiểm lại nếu có sửa)" : " (chỉ biên tập, 1 lượt gọi)");
        c.modelLine = "Model: " + (model.isEmpty() ? "(chưa chọn trong Cài đặt)" : model);

        if (sources.hasGlossary()) {
            c.glossaryLine = "Glossary: " + (glossaryName.isEmpty() ? "đã chọn" : glossaryName) + " — " + sources.glossary.size() + " mục";
        } else {
            c.glossaryLine = "Không dùng Glossary";
            c.warnings.add("Không có Glossary: tên riêng có thể không thống nhất.");
        }
        if (sources.hasPronoun()) {
            c.pronounLine = "Pronoun: " + (pronounName.isEmpty() ? "đã chọn" : pronounName);
        } else {
            c.pronounLine = "Không dùng Pronoun";
            c.warnings.add("Không có Pronoun: cách xưng hô sẽ giữ như trong DRAFT.");
        }

        int referenceChars = sources.glossaryText.length() + sources.pronounText.length();
        for (SourceCheck.Problem problem : SourceCheck.check(sources.raw, sources.draft, sources.hasGlossary(), sources.hasPronoun(), referenceChars)) {
            String text = problemText(problem);
            if (text.isEmpty()) continue;
            if (problem.blocking()) c.blockers.add(text);
            else if (problem != SourceCheck.Problem.NO_GLOSSARY && problem != SourceCheck.Problem.NO_PRONOUN) c.warnings.add(text);
        }
        if (model.isEmpty()) c.blockers.add("Chưa chọn model. Vào Cài đặt để chọn model và nhập khóa API.");
        c.costLine = costLine(sources, settings, inputPerToken, outputPerToken);
        if (!priceKnown && !model.isEmpty()) {
            c.warnings.add("Chưa có giá của model này trong danh mục: ước tính theo mức cao, và lần chạy có thể dừng sớm vì chạm trần chi phí. Nâng trần hoặc chọn model có giá.");
        }
        return c;
    }

    static String problemText(SourceCheck.Problem problem) {
        switch (problem) {
            case RAW_EMPTY: return "File RAW trống.";
            case DRAFT_EMPTY: return "File DRAFT trống.";
            case RAW_BROKEN_ENCODING: return "File RAW bị lỗi mã hóa (nhiều ký tự ‘�’). Hãy lưu lại file dạng UTF-8.";
            case DRAFT_BROKEN_ENCODING: return "File DRAFT bị lỗi mã hóa (nhiều ký tự ‘�’). Hãy lưu lại file dạng UTF-8.";
            case TOO_LONG: return "Chương quá dài để biên tập trong một lần. Hãy tách chương.";
            case LENGTH_RATIO: return "Độ dài DRAFT so với RAW khác thường (ngoài 0,5–4 lần). Kiểm tra xem hai file có đúng cặp không.";
            case SAME_TEXT: return "DRAFT giống hệt RAW — có thể chưa phải bản dịch.";
            default: return "";
        }
    }

    /** First lines of a text, each cut short, so the person can recognise the file. */
    static String head(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\\R", -1)) {
            if (line.isBlank()) continue;
            String trimmed = line.strip();
            lines.add(trimmed.length() > HEAD_LINE_CHARS ? trimmed.substring(0, HEAD_LINE_CHARS) + "…" : trimmed);
            if (lines.size() == HEAD_LINES) break;
        }
        return String.join("\n", lines);
    }

    /**
     * A rounded estimate of one run: the edit sends the sources and returns about the size of the draft, the check sends them
     * again with the edited text. Thorough adds one more check. Prices are per token; the cap is enforced separately.
     */
    static String costLine(EditorialApiSources sources, EditorialApiCombo.Settings settings, BigDecimal inputPerToken, BigDecimal outputPerToken) {
        EditInputs inputs = sources.inputs();
        long editIn = EditPromptBuilder.build(inputs).estimatedInputTokens();
        long draftTokens = Math.max(1L, sources.draft.length() / 2L);
        long checkIn = editIn + draftTokens;
        long checkOut = 600L;
        int checks = settings.mode == EditorialApiContract.Mode.THOROUGH ? 2 : 0;
        BigDecimal usd = inputPerToken.multiply(BigDecimal.valueOf(editIn + checks * checkIn))
                .add(outputPerToken.multiply(BigDecimal.valueOf(draftTokens + checks * checkOut)));
        return "Ước tính chi phí: khoảng " + usd(usd.setScale(3, RoundingMode.UP)) + " (tối đa " + usd(settings.maxUsdPerChapter) + " cho chương này)";
    }

    static String usd(BigDecimal value) { return "USD " + value.stripTrailingZeros().toPlainString(); }

    // ---- progress ----

    static String progressLine(EditorialApiContract.Step step, int attempt) {
        String name = step == EditorialApiContract.Step.EDIT ? "Đang biên tập"
                : step == EditorialApiContract.Step.CHECK ? "Đang kiểm" : "Đang kiểm lại";
        return attempt > 1 ? name + " (thử lại)…" : name + "…";
    }

    static String spentLine(EditorialApiRun run) {
        return "Đã dùng " + usd(run.usd) + " • " + run.calls + " lượt gọi";
    }

    // ---- result ----

    static final class Review {
        final String kind;
        final String viQuote;
        final String rawQuote;
        final String fix;
        final String reason;

        Review(String kind, String viQuote, String rawQuote, String fix, String reason) {
            this.kind = kind;
            this.viQuote = viQuote;
            this.rawQuote = rawQuote;
            this.fix = fix;
            this.reason = reason;
        }
    }

    static final class Result {
        String headline = "";
        String detail = "";
        String finalText = "";
        boolean canExport;
        boolean canCompare;
        final List<String> staleParts = new ArrayList<>();
        final List<Review> review = new ArrayList<>();
        final List<String> flags = new ArrayList<>();
        final List<String> notes = new ArrayList<>();
        String technical = "";
    }

    static Result result(EditorialApiRun run, List<String> changedParts) {
        Result r = new Result();
        r.finalText = run.finalText;
        boolean finished = run.state == EditorialApiContract.RunState.FINAL_OK || run.state == EditorialApiContract.RunState.FINAL_NOTES;
        r.canExport = finished && !run.finalText.isEmpty();
        r.canCompare = finished;
        switch (run.state) {
            case FINAL_OK:
                r.headline = "Đã biên tập xong";
                r.detail = run.mode == EditorialApiContract.Mode.QUICK
                        ? "Chế độ Nhanh chỉ biên tập, không có bước kiểm riêng. Không có cờ nào cần lưu ý."
                        : "Bản cuối đã qua bước kiểm. Không có mục nào cần xem thêm.";
                break;
            case FINAL_NOTES:
                r.headline = "Đã biên tập xong — có mục cần xem";
                r.detail = "Bản cuối dùng được. Một số chỗ bên dưới nên được người đọc lại.";
                break;
            case WRONG_PAIR:
                r.headline = "RAW và DRAFT có vẻ không cùng một chương";
                r.detail = run.wrongPairEvidence.isEmpty() ? "Hãy kiểm tra lại hai file rồi chạy lại." : run.wrongPairEvidence;
                break;
            case RETRY_REQUIRED:
                r.headline = "Chưa biên tập được";
                r.detail = "Nhà cung cấp không trả về kết quả dùng được hoặc chi phí chạm trần. Bản DRAFT được giữ nguyên. Hãy chạy lại.";
                if (!run.costKnown) r.detail += " Có thể một lượt gọi đã bị tính phí nhưng chưa rõ số tiền; hãy xem bảng giá của nhà cung cấp trước khi chạy lại.";
                if ("COST_BOUND_EXCEEDED".equals(run.error)) r.detail += " Một lượt gọi tốn hơn mức dự tính nên không gửi thêm.";
                break;
            case CANCELLED:
                r.headline = "Đã hủy";
                r.detail = "Bản DRAFT được giữ nguyên.";
                break;
            default:
                r.headline = EditorialApiRunService.isInterrupted(run) ? "Lần chạy bị gián đoạn" : "Đang chạy…";
                r.detail = EditorialApiRunService.isInterrupted(run) ? "Ứng dụng đã đóng giữa chừng. Không có gì được gửi lại; hãy chạy lại." : "";
        }
        for (String part : changedParts) r.staleParts.add(partName(part));
        for (String[] item : EditorialApiRunCodec.reviewItems(run)) r.review.add(new Review(kindLabel(item[0]), item[1], item[2], item[3], item[4]));
        for (String flag : EditorialApiRunCodec.guardFlags(run)) r.flags.add(flagText(flag));
        for (String[] note : EditorialApiRunCodec.notes(run)) r.notes.add("“" + note[0] + "” — " + note[2]);
        r.technical = technical(run);
        return r;
    }

    static String partName(String part) {
        switch (part) {
            case "RAW": return "file RAW";
            case "DRAFT": return "file DRAFT";
            case "GLOSSARY": return "Glossary";
            case "PRONOUN": return "Pronoun";
            default: return part;
        }
    }

    static String staleMessage(List<String> parts) {
        return parts.isEmpty() ? "" : "Nguồn đã thay đổi sau lần chạy này (" + String.join(", ", parts) + "). Chạy lại?";
    }

    static String kindLabel(String kind) {
        switch (kind) {
            case "MEANING": return "Sai nghĩa";
            case "OMISSION": return "Thiếu ý";
            case "ADDITION": return "Thừa ý";
            case "NUMBER": return "Sai số";
            case "NEGATION": return "Sai phủ định";
            case "SPEAKER": return "Sai người nói";
            case "PRONOUN": return "Xưng hô";
            case "GLOSSARY": return "Thuật ngữ";
            case "REGRESSION": return "Sửa làm hỏng chỗ đúng";
            default: return "Khác";
        }
    }

    static String flagText(String flag) {
        String code = flag.contains(":") ? flag.substring(0, flag.indexOf(':')) : flag;
        String detail = flag.contains(":") ? flag.substring(flag.indexOf(':') + 1).trim() : "";
        switch (code) {
            case "STRUCTURE_WARN": return "Số dòng khác DRAFT đáng kể" + suffix(detail);
            case "REWRITE_WARN": return "Bản cuối thay đổi nhiều so với DRAFT" + suffix(detail);
            case "SYMBOL_WARN": return "Ký hiệu khung (「」『』…) khác DRAFT" + suffix(detail);
            case "GLOSSARY_WARN": return "Chưa thấy trong bản cuối: " + detail;
            case "META_LEAK": return "Đã bỏ các dòng giải thích lẫn trong văn bản";
            case "META_UNSEPARABLE": return "Có lời giải thích lẫn trong văn bản";
            default: return flag;
        }
    }

    private static String suffix(String detail) { return detail.isEmpty() ? "" : " (" + detail + ")"; }

    static String technical(EditorialApiRun run) {
        StringBuilder out = new StringBuilder();
        out.append("Model: ").append(run.model).append('\n');
        out.append("Chế độ: ").append(modeLabel(run.mode)).append('\n');
        out.append("Hợp đồng: ").append(run.contractRevision).append('\n');
        out.append("Chuẩn chất lượng sha256: ").append(run.qualityCoreSha256).append('\n');
        out.append("Lượt gọi: ").append(run.calls).append(" • token vào/ra: ").append(run.inputTokens).append('/').append(run.outputTokens)
                .append(" • ").append(usd(run.usd)).append(run.costKnown ? "" : " (chưa rõ giá)").append('\n');
        out.append("RAW sha256: ").append(run.rawSha256).append('\n');
        out.append("DRAFT sha256: ").append(run.draftSha256).append('\n');
        if (!run.glossarySha256.isEmpty()) out.append("Glossary sha256: ").append(run.glossarySha256).append(" (").append(run.glossaryEntries).append(" mục dùng)\n");
        if (!run.pronounSha256.isEmpty()) out.append("Pronoun sha256: ").append(run.pronounSha256).append(" (").append(run.pronounRows).append(" dòng dùng)\n");
        out.append("Bản cuối sha256: ").append(HashUtil.sha256(run.finalText));
        if (!run.error.isEmpty()) out.append("\nGhi chú kỹ thuật: ").append(run.error);
        return out.toString();
    }

    // ---- comparison ----

    /** Changed passages between the draft and the final text, as "DRAFT" / "BẢN CUỐI" pairs, capped for the screen. */
    static List<String[]> diffRows(String draft, String finalText) {
        List<String[]> rows = new ArrayList<>();
        for (LineDiff.Segment segment : LineDiff.segments(draft, finalText)) {
            if (rows.size() == MAX_DIFF_ROWS) break;
            rows.add(new String[] {"Dòng " + (segment.draftStartLine() + 1), segment.before(), segment.after()});
        }
        return rows;
    }

    static String diffSummary(String draft, String finalText) {
        List<LineDiff.Segment> segments = LineDiff.segments(draft, finalText);
        if (segments.isEmpty()) return "Bản cuối giống hệt DRAFT.";
        String more = segments.size() > MAX_DIFF_ROWS ? String.format(Locale.ROOT, " Chỉ hiện %d chỗ đầu.", MAX_DIFF_ROWS) : "";
        return segments.size() + " chỗ khác DRAFT." + more;
    }
}
