package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The PC Settings page as data: groups, Vietnamese labels, how each value is read from and written to {@link AppSettings}, and the
 * check on what the person typed. The page draws itself from this list, and the tests change and read every field through it.
 * Nothing here is Editorial, refinement, or a field the phone marks "chưa có tác dụng".
 */
public final class DesktopSettingsModel {
    public enum Kind { TEXT, INT, DECIMAL, BOOL, CHOICE }

    public static final String CONNECTION = "Kết nối Provider", QUALITY = "Mức chất lượng", CHUNK = "Chunk", COST = "Chi phí",
            RETRY = "Gọi lại và thời gian chờ", OUTPUT = "Đầu ra";

    public static final class Field {
        public final String group;
        public final String key;
        public final String label;
        public final Kind kind;
        /** For CHOICE: stored value / shown label pairs. */
        public final String[][] choices;
        private final Getter getter;
        private final Setter setter;
        /** Setting this value needs the person to confirm first (reasoning "high", unlimited cost). */
        public final String confirmValue;
        public final String confirmTitle;
        public final String confirmText;

        Field(String group, String key, String label, Kind kind, String[][] choices, Getter g, Setter s, String confirmValue, String confirmTitle, String confirmText) {
            this.group = group; this.key = key; this.label = label; this.kind = kind; this.choices = choices; this.getter = g; this.setter = s;
            this.confirmValue = confirmValue; this.confirmTitle = confirmTitle; this.confirmText = confirmText;
        }

        public String get(AppSettings s) { return getter.get(s); }

        /** Writes the typed text; returns null on success or the reason it was refused (the settings stay as they were). */
        public String set(AppSettings s, String text) {
            try { return setter.set(s, text == null ? "" : text.trim()); } catch (NumberFormatException e) { return "Giá trị không hợp lệ: " + text; }
        }

        public boolean needsConfirm(String value) { return confirmValue != null && confirmValue.equals(value); }
    }

    interface Getter { String get(AppSettings s); }
    interface Setter { String set(AppSettings s, String v); }

    private static final List<Field> FIELDS = new ArrayList<>();

    private static void text(String g, String k, String l, Getter get, Setter set) { FIELDS.add(new Field(g, k, l, Kind.TEXT, null, get, set, null, null, null)); }
    private static void num(String g, String k, String l, Getter get, Setter set) { FIELDS.add(new Field(g, k, l, Kind.INT, null, get, set, null, null, null)); }
    private static void dec(String g, String k, String l, Getter get, Setter set) { FIELDS.add(new Field(g, k, l, Kind.DECIMAL, null, get, set, null, null, null)); }
    private static void bool(String g, String k, String l, Getter get, Setter set) { FIELDS.add(new Field(g, k, l, Kind.BOOL, null, get, set, null, null, null)); }
    private static void choice(String g, String k, String l, String[][] c, Getter get, Setter set, String confirmValue, String ct, String cx) {
        FIELDS.add(new Field(g, k, l, Kind.CHOICE, c, get, set, confirmValue, ct, cx));
    }

    private static String b(boolean v) { return v ? "true" : "false"; }

    private static String positiveInt(String v, int min, String what, java.util.function.IntConsumer apply) {
        int n = Integer.parseInt(v);
        if (n < min) return what + " phải từ " + min + " trở lên";
        apply.accept(n);
        return null;
    }

    static {
        text(CONNECTION, "provider", "Provider", s -> s.provider, (s, v) -> { if (v.isEmpty()) return "Chọn provider"; s.provider = v.toLowerCase(Locale.ROOT); return null; });
        text(CONNECTION, "baseUrl", "Base URL", s -> s.baseUrl, (s, v) -> { if (v.isEmpty()) return "Base URL không được để trống"; s.baseUrl = v; return null; });
        text(CONNECTION, "model", "Model", s -> s.model, (s, v) -> { if (v.isEmpty()) return "Model không được để trống"; s.model = v; return null; });

        choice(QUALITY, "optimizationPreset", "Mức chất lượng",
                new String[][]{{"economy", "Tiết kiệm"}, {"balanced", "Cân bằng"}, {"quality", "Chất lượng cao"}, {"full", "Đầy đủ (prompt gốc, tốn token hơn)"}},
                s -> s.optimizationPreset, (s, v) -> { s.optimizationPreset = v; return null; }, null, null, null);
        choice(QUALITY, "reasoningEffort", "Mức suy luận",
                new String[][]{{"auto", "Tự động (thấp với model suy luận)"}, {"off", "Tắt (không gửi)"}, {"low", "Thấp"}, {"medium", "Vừa"}, {"high", "Cao · có thể tốn hơn"}},
                s -> s.reasoningEffort, (s, v) -> { s.reasoningEffort = v; return null; }, "high", "Bật mức Cao?",
                "Mức suy luận Cao có thể dùng nhiều token hơn và tăng chi phí lượt dịch.");
        dec(QUALITY, "temperature", "Nhiệt độ", s -> trim(s.temperature), (s, v) -> {
            float f = Float.parseFloat(v.replace(',', '.'));
            if (f < 0 || f > 2) return "Nhiệt độ phải trong khoảng 0 đến 2";
            s.temperature = f; return null;
        });

        choice(CHUNK, "chunkMode", "Chế độ chunk", new String[][]{{"token", "Theo token"}, {"char", "Theo ký tự"}},
                s -> s.chunkMode, (s, v) -> { s.chunkMode = v; return null; }, null, null, null);
        num(CHUNK, "maxTokensPerChunk", "Số token/chunk", s -> String.valueOf(s.maxTokensPerChunk), (s, v) -> positiveInt(v, 50, "Số token/chunk", n -> s.maxTokensPerChunk = n));
        num(CHUNK, "maxCharsPerChunk", "Số ký tự/chunk", s -> String.valueOf(s.maxCharsPerChunk), (s, v) -> positiveInt(v, 0, "Số ký tự/chunk", n -> s.maxCharsPerChunk = n));
        dec(CHUNK, "softLimitRatio", "Tỷ lệ mềm", s -> trim(s.softLimitRatio), (s, v) -> {
            float f = Float.parseFloat(v.replace(',', '.'));
            if (f < 0.3f || f > 1f) return "Tỷ lệ mềm phải trong khoảng 0.3 đến 1";
            s.softLimitRatio = f; return null;
        });
        num(CHUNK, "maxOutputTokens", "Max output tokens", s -> String.valueOf(s.maxOutputTokens), (s, v) -> positiveInt(v, 256, "Max output tokens", n -> s.maxOutputTokens = n));
        num(CHUNK, "contextChars", "Ngữ cảnh (ký tự)", s -> String.valueOf(s.contextChars), (s, v) -> positiveInt(v, 0, "Ngữ cảnh", n -> s.contextChars = n));
        bool(CHUNK, "contextOverlapEnabled", "Ngữ cảnh chồng lấn", s -> b(s.contextOverlapEnabled), (s, v) -> { s.contextOverlapEnabled = Boolean.parseBoolean(v); return null; });
        num(CHUNK, "glossaryInjectLimit", "Số dòng Glossary/chunk", s -> String.valueOf(s.glossaryInjectLimit), (s, v) -> positiveInt(v, 0, "Số dòng Glossary", n -> s.glossaryInjectLimit = n));
        num(CHUNK, "pronounInjectLimit", "Số quy tắc Pronoun/chunk", s -> String.valueOf(s.pronounInjectLimit), (s, v) -> positiveInt(v, 0, "Số quy tắc Pronoun", n -> s.pronounInjectLimit = n));

        dec(COST, "costLimitUsd", "Trần chi phí mỗi lượt (USD)", s -> s.costLimitUsd > 0 ? trimD(s.costLimitUsd) : "", (s, v) -> {
            if (v.isEmpty()) { s.costLimitUsd = 0; return null; }
            double d = Double.parseDouble(v.replace(',', '.'));
            if (d < 0) return "Trần chi phí không được âm";
            s.costLimitUsd = d; return null;
        });
        bool(COST, "stopOnCostLimit", "Dừng khi chạm trần chi phí", s -> b(s.stopOnCostLimit), (s, v) -> { s.stopOnCostLimit = Boolean.parseBoolean(v); return null; });
        dec(COST, "costWarningUsd", "Cảnh báo chi phí (USD)", s -> s.costWarningUsd > 0 ? trimD(s.costWarningUsd) : "", (s, v) -> {
            if (v.isEmpty()) { s.costWarningUsd = 0; return null; }
            double d = Double.parseDouble(v.replace(',', '.'));
            if (d < 0) return "Mức cảnh báo không được âm";
            s.costWarningUsd = d; return null;
        });
        bool(COST, "stopWhenPricingUnknown", "Dừng khi chưa biết giá", s -> b(s.stopWhenPricingUnknown), (s, v) -> { s.stopWhenPricingUnknown = Boolean.parseBoolean(v); return null; });

        num(RETRY, "maxAttempts", "Số lần thử tối đa", s -> String.valueOf(s.maxAttempts), (s, v) -> positiveInt(v, 1, "Số lần thử", n -> s.maxAttempts = n));
        num(RETRY, "timeoutSeconds", "Thời gian chờ (giây)", s -> String.valueOf(s.timeoutSeconds), (s, v) -> positiveInt(v, 10, "Thời gian chờ", n -> s.timeoutSeconds = n));
        num(RETRY, "initialRetryDelayMs", "Độ trễ gọi lại (ms)", s -> String.valueOf(s.initialRetryDelayMs), (s, v) -> positiveInt(v, 0, "Độ trễ gọi lại", n -> s.initialRetryDelayMs = n));
        num(RETRY, "maxRetryDelayMs", "Độ trễ tối đa (ms)", s -> String.valueOf(s.maxRetryDelayMs), (s, v) -> positiveInt(v, 0, "Độ trễ tối đa", n -> s.maxRetryDelayMs = n));
        bool(RETRY, "retryOnEmpty", "Gọi lại khi phản hồi rỗng", s -> b(s.retryOnEmpty), (s, v) -> { s.retryOnEmpty = Boolean.parseBoolean(v); return null; });
        bool(RETRY, "retryOnTruncation", "Gọi lại khi có dấu hiệu bị cắt", s -> b(s.retryOnTruncation), (s, v) -> { s.retryOnTruncation = Boolean.parseBoolean(v); return null; });
        bool(RETRY, "retryOnValidationFailure", "Gọi lại khi kiểm tra thất bại", s -> b(s.retryOnValidationFailure), (s, v) -> { s.retryOnValidationFailure = Boolean.parseBoolean(v); return null; });

        bool(OUTPUT, "bilingualOutput", "Đầu ra song ngữ", s -> b(s.bilingualOutput), (s, v) -> { s.bilingualOutput = Boolean.parseBoolean(v); return null; });
        bool(OUTPUT, "savePartialOutput", "Lưu đầu ra từng phần", s -> b(s.savePartialOutput), (s, v) -> { s.savePartialOutput = Boolean.parseBoolean(v); return null; });
    }

    private DesktopSettingsModel() { }

    private static String trim(float f) { return f == (long) f ? String.valueOf((long) f) : String.valueOf(f); }

    private static String trimD(double d) { return d == (long) d ? String.valueOf((long) d) : String.valueOf(d); }

    public static List<Field> fields() { return FIELDS; }

    public static List<String> groups() {
        List<String> out = new ArrayList<>();
        for (Field f : FIELDS) if (!out.contains(f.group)) out.add(f.group);
        return out;
    }

    public static List<Field> fieldsOf(String group) {
        List<Field> out = new ArrayList<>();
        for (Field f : FIELDS) if (f.group.equals(group)) out.add(f);
        return out;
    }

    public static Field field(String key) {
        for (Field f : FIELDS) if (f.key.equals(key)) return f;
        return null;
    }

    // ---- "Cài đặt khuyến nghị" ----

    /** Settings that differ from the safe recommendation: {key, label, current, recommended, reason}. Never touches provider, key or money limits. */
    public static List<String[]> recommendationDiffs(AppSettings s) {
        List<String[]> out = new ArrayList<>();
        for (SettingsRecommendationModel.Item i : SettingsRecommendationModel.diffs(s)) {
            Field f = field(i.key);
            out.add(new String[]{i.key, f == null ? i.key : f.label, i.current, i.recommended, i.reason});
        }
        return out;
    }

    public static AppSettings applyRecommendation(AppSettings s) { return SettingsRecommendationModel.apply(s); }
}
