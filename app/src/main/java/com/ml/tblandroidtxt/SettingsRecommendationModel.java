package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pure, non-secret safe runtime recommendation table. It never changes provider, credentials or money limits. */
final class SettingsRecommendationModel {
    static final class Item {
        final String key;
        final String current;
        final String recommended;
        final String reason;

        Item(String key, String current, String recommended, String reason) {
            this.key = key; this.current = current; this.recommended = recommended; this.reason = reason;
        }
    }

    private SettingsRecommendationModel() { }

    static List<Item> diffs(AppSettings source) {
        AppSettings s = source == null ? new AppSettings() : source;
        ArrayList<Item> out = new ArrayList<>();
        if (s.maxOutputTokens < 8192) out.add(new Item("maxOutputTokens", String.valueOf(s.maxOutputTokens), "8192", "Dành chỗ cho phần suy luận để bản dịch dài ít bị cắt."));
        if (s.timeoutSeconds < 300) out.add(new Item("timeoutSeconds", String.valueOf(s.timeoutSeconds), "300", "Cho phép lượt gọi chậm hoàn tất trong thời gian hợp lý."));
        if (s.maxAttempts < 3) out.add(new Item("maxAttempts", String.valueOf(s.maxAttempts), "3", "Có đủ lần thử lại cho lỗi mạng tạm thời."));
        if (s.contextChars < 400) out.add(new Item("contextChars", String.valueOf(s.contextChars), "400", "Giữ đủ ngữ cảnh hai phía khi ghép chunk."));
        if (s.softLimitRatio < 0.8f) out.add(new Item("softLimitRatio", fmt(s.softLimitRatio), "0.8", "Giữ chunk dưới trần cứng để giảm nguy cơ vượt giới hạn."));
        return out;
    }

    static AppSettings apply(AppSettings source) {
        AppSettings out = source == null ? new AppSettings() : source.copy();
        if (out.maxOutputTokens < 8192) out.maxOutputTokens = 8192;
        if (out.timeoutSeconds < 300) out.timeoutSeconds = 300;
        if (out.maxAttempts < 3) out.maxAttempts = 3;
        if (out.contextChars < 400) out.contextChars = 400;
        if (out.softLimitRatio < 0.8f) out.softLimitRatio = 0.8f;
        return out;
    }

    private static String fmt(float value) { return String.format(Locale.US, "%.3f", value); }
}
